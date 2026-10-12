package rag.runtime;
import rag.*;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.UUID;

/** Existing Gemini adapter behind an exact-payload allowlist and durable cost gate. */
public final class RuntimeEmbeddingService implements EmbeddingService {
    private final GeminiEmbeddingService delegate;
    public RuntimeEmbeddingService(GeminiRuntimeConfig cfg,ApiRequestLedger ledger){this(cfg,ledger,GeminiEmbeddingService.httpTransport());}
    public RuntimeEmbeddingService(GeminiRuntimeConfig cfg,ApiRequestLedger ledger,GeminiEmbeddingService.Transport wire){
        delegate=new GeminiEmbeddingService(cfg.rag(),cfg.embeddingKey(),(method,endpoint,key,body,deadline)->{
            cfg.current();
            if(!"POST".equals(method)||!endpoint.equals("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent")||!cfg.embeddingKey().equals(key)||body==null||body.get("outputDimensionality").getAsInt()!=768)throw new RagException(RagException.Code.EXECUTION_BLOCKED);
            String task=body.get("taskType").getAsString();String group="RETRIEVAL_DOCUMENT".equals(task)?"DOCUMENT":"RETRIEVAL_QUERY".equals(task)?"QUERY":"";
            if(group.isEmpty())throw new RagException(RagException.Code.EXECUTION_BLOCKED);cfg.payload(group,body);
            String input=body.getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
            int tokens=input.getBytes(StandardCharsets.UTF_8).length+(body.has("title")?body.get("title").getAsString().getBytes(StandardCharsets.UTF_8).length:0);
            if(tokens>2048)throw new RagException(RagException.Code.INVALID_DOCUMENT);
            String operation=group.equals("DOCUMENT")?"document:"+VectorValues.hash(body.toString()):"query:"+UUID.randomUUID();
            UUID ticket=ledger.reserve(group,operation,"gemini-embedding-001",Math.max(1,tokens));long start=System.nanoTime();
            GeminiEmbeddingService.Result result;
            try{result=wire.request(method,endpoint,key,body,deadline);}catch(IOException e){ledger.finish(ticket,"UNCERTAIN",0,"NETWORK_OR_TIMEOUT",ms(start),null,null,null);throw new IOException("Gemini embedding transport unavailable");}
            boolean valid=false;Integer usage=null;
            if(result.status==200)try{
                JsonObject root=JsonParser.parseString(result.body).getAsJsonObject();JsonArray values=root.getAsJsonObject("embedding").getAsJsonArray("values");
                if(values.size()!=768)throw new IllegalArgumentException();float[] v=new float[768];for(int i=0;i<768;i++){if(!values.get(i).getAsJsonPrimitive().isNumber())throw new IllegalArgumentException();v[i]=values.get(i).getAsFloat();}VectorValues.normalized(v,768);valid=true;
                if(root.has("usageMetadata")&&root.getAsJsonObject("usageMetadata").has("promptTokenCount")){
                    JsonPrimitive count=root.getAsJsonObject("usageMetadata").getAsJsonPrimitive("promptTokenCount");
                    if(!count.isNumber()||!count.toString().matches("[0-9]+")||count.getAsLong()>Integer.MAX_VALUE)throw new IllegalArgumentException();usage=count.getAsInt();
                }
            }catch(Exception ignored){valid=false;}
            if(usage!=null&&(usage<0||usage>tokens)){ledger.finish(ticket,"UNCERTAIN",result.status,"TOKEN_BOUND",ms(start),null,null,null);throw new RagException(RagException.Code.EXECUTION_BLOCKED);}
            ledger.finish(ticket,valid?"COMPLETED":"FAILED",result.status,valid?null:result.status==429?"RATE_LIMIT":result.status==200?"INVALID_VECTOR":"PROVIDER",ms(start),usage,null,usage);
            if(result.status==200&&!valid)throw new RagException(RagException.Code.INVALID_VECTOR);
            if(result.status>=201&&result.status<300)throw new RagException(RagException.Code.PROVIDER);
            return result;
        },true,1);
    }
    static long ms(long start){return (System.nanoTime()-start)/1000000;}
    public String model(){return delegate.model();}public int dimensions(){return 768;}public String providerKind(){return "GEMINI";}
    public float[] document(String title,String text)throws RagException{return delegate.document(title,text);}public float[] query(String text)throws RagException{return delegate.query(text);}
}
