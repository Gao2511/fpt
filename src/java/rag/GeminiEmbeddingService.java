package rag;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Google embedContent adapter, independent of the unchanged Gemini chat adapter. */
public final class GeminiEmbeddingService implements EmbeddingService {
    public interface Transport {
        Result request(String method,String endpoint,String apiKey,JsonObject body,long deadline) throws IOException,RagException;
    }
    public static final class Result {
        public final int status;public final String body;
        public Result(int status,String body){this.status=status;this.body=body;}
    }
    private final RagConfig config;
    private final String key;
    private final Transport transport;
    private volatile boolean accountVerified;
    private final int maxAttempts;
    public GeminiEmbeddingService(RagConfig config,String apiKey) {this(config,apiKey,new HttpTransport());}
    public GeminiEmbeddingService(RagConfig config,String apiKey,Transport transport) {this(config,apiKey,transport,false,3);}
    /** Runtime metadata attestation is external; never performs an implicit GET or retry. */
    public GeminiEmbeddingService(RagConfig config,String apiKey,Transport transport,boolean verified,int attempts) {
        if(attempts<1||attempts>3)throw new IllegalArgumentException("Invalid attempt bound");
        this.config=config;key=apiKey;this.transport=transport;accountVerified=verified;maxAttempts=attempts;
    }
    public static Transport httpTransport(){return new HttpTransport();}
    public String providerKind(){return "GEMINI";}
    public String model(){return config.model;}
    public int dimensions(){return config.dimensions;}
    public float[] document(String title,String content) throws RagException {return embed(title,content,"RETRIEVAL_DOCUMENT");}
    public float[] query(String text) throws RagException {return embed(null,text,"RETRIEVAL_QUERY");}
    /** Non-billable metadata check; invoked only by an explicitly configured embedding caller. */
    public synchronized void verifyAccount(long deadline) throws RagException {
        if(accountVerified)return;
        // Metadata verification is allowed before cost/model approval; it generates no vectors.
        if(key==null || key.trim().isEmpty())throw new RagException(RagException.Code.CONFIGURATION);
        Result result=send("GET",null,deadline);
        try {
            JsonObject metadata=JsonParser.parseString(result.body).getAsJsonObject();
            if(!("models/"+config.model).equals(metadata.get("name").getAsString()))throw new IllegalArgumentException();
            boolean supported=false;
            for(JsonElement method:metadata.getAsJsonArray("supportedGenerationMethods"))if("embedContent".equals(method.getAsString()))supported=true;
            if(!supported)throw new IllegalArgumentException();accountVerified=true;
        }catch(Exception e){throw new RagException(RagException.Code.CONFIGURATION);}
    }
    private float[] embed(String title,String text,String task) throws RagException {
        if(!config.callsApproved || !config.modelVerified || text==null || text.trim().isEmpty() || text.length()>2000)
            throw new RagException(RagException.Code.CONFIGURATION);
        long deadline=System.currentTimeMillis()+10000;
        verifyAccount(deadline);
        JsonObject body=requestBody(title,text,task);
        Result result=send("POST",body,deadline);
        try {
            JsonArray values=JsonParser.parseString(result.body).getAsJsonObject().getAsJsonObject("embedding").getAsJsonArray("values");
            if(values.size()!=dimensions())throw new IllegalArgumentException();float[] vector=new float[values.size()];
            for(int i=0;i<vector.length;i++) {
                JsonElement v=values.get(i);if(!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException();
                vector[i]=v.getAsFloat();
            }return VectorValues.normalized(vector,dimensions());
        }catch(Exception e){throw new RagException(RagException.Code.INVALID_VECTOR);}
    }
    public JsonObject requestBody(String title,String text,String task) {
        JsonObject body=new JsonObject();body.addProperty("model","models/"+model());
        JsonObject content=new JsonObject(),part=new JsonObject();JsonArray parts=new JsonArray();part.addProperty("text",text);parts.add(part);content.add("parts",parts);body.add("content",content);
        // Retain the documented legacy REST fields validated in Phase 2.3; no silent payload migration.
        body.addProperty("taskType",task);body.addProperty("outputDimensionality",dimensions());
        if(title!=null && "RETRIEVAL_DOCUMENT".equals(task))body.addProperty("title",title);
        return body;
    }
    private Result send(String method,JsonObject body,long deadline) throws RagException {
        RagException.Code last=RagException.Code.PROVIDER;
        String endpoint="https://generativelanguage.googleapis.com/v1beta/models/"+model()+("POST".equals(method)?":embedContent":"");
        for(int attempt=0;attempt<maxAttempts && System.currentTimeMillis()<deadline;attempt++) {
            try {
                Result r=transport.request(method,endpoint,key,body,deadline);
                if(r.status>=200 && r.status<300)return r;
                if(r.status==401 || r.status==403 || r.status==404 || r.status==400)throw new RagException(RagException.Code.CONFIGURATION);
                last=r.status==429?RagException.Code.RATE_LIMIT:RagException.Code.PROVIDER;
                if(r.status!=429 && r.status<500)throw new RagException(last);
            }catch(SocketTimeoutException e){last=RagException.Code.TIMEOUT;}
            catch(IOException e){last=RagException.Code.PROVIDER;}
            if(attempt<maxAttempts-1)try {Thread.sleep(Math.min(200L*(attempt+1),Math.max(0,deadline-System.currentTimeMillis())));}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new RagException(RagException.Code.TIMEOUT);}
        }
        throw new RagException(System.currentTimeMillis()>=deadline?RagException.Code.TIMEOUT:last);
    }
    private static final class HttpTransport implements Transport {
        private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"rag-http-deadline");t.setDaemon(true);return t;});
        public Result request(String method,String endpoint,String apiKey,JsonObject body,long deadline) throws IOException {
            HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
            int remaining=(int)Math.max(1,Math.min(5000,deadline-System.currentTimeMillis()));
            c.setConnectTimeout(remaining);c.setReadTimeout(remaining);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);
            c.setRequestProperty("x-goog-api-key",apiKey);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            ScheduledFuture<?> timeout=TIMER.schedule(c::disconnect,Math.max(1,deadline-System.currentTimeMillis()),TimeUnit.MILLISECONDS);
            try {
                if(body!=null){byte[] payload=body.toString().getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setFixedLengthStreamingMode(payload.length);try(OutputStream out=c.getOutputStream()){out.write(payload);}}
                int status=c.getResponseCode();if(status<200 || status>=300)return new Result(status,"");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()) {byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>65536)throw new IOException("Response size exceeded");out.write(buffer,0,n);}}
                return new Result(status,new String(out.toByteArray(),StandardCharsets.UTF_8));
            }finally {timeout.cancel(false);c.disconnect();}
        }
    }
}
