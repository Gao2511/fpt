package rag.live;

import com.google.gson.*;
import rag.VectorValues;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

/** Test tool only. Immutable CREATE_NEW journal; no recovery/resume and no production defaults. */
public final class LiveSafety {
    public static final String EMBED="https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";
    // Deliberately bypass catch(Exception) in the runtime's retry/fallback code. Caught only at runner boundary.
    public static final class Halt extends Error { public Halt(String code){super(code);} }
    public static void require(boolean ok,String code){if(!ok)throw new Halt(code);}
    public static boolean yes(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull()&&o.get(k).getAsBoolean();}
    public static String str(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():"";}
    public static int num(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsInt():-1;}
    public static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    public static final class Plan {
        public final JsonObject json, approval; public final String id,model,hash;
        public Plan(JsonObject json){
            try {
                this.json=json.deepCopy();id=str(json,"planId");model=str(json,"chatModel");hash=VectorValues.hash(json.toString());
                require(id.matches("phase24-[a-zA-Z0-9-]{1,100}"),"PLAN_ID");
                require(yes(json,"approved")&&yes(json,"executionAuthorized"),"APPROVAL_MISSING");
                require(yes(json,"runnerImplemented")&&yes(json.getAsJsonObject("readiness"),"executable")&&json.getAsJsonObject("readiness").getAsJsonArray("blockers").size()==0,"RUNNER_NOT_READY");
                require("development".equals(str(json,"environment"))&&json.has("productionRagEnabled")&&!yes(json,"productionRagEnabled"),"ENVIRONMENT");
                require("gemini-embedding-001".equals(str(json,"embeddingModel"))&&num(json,"dimensions")==768,"EMBEDDING_CONFIGURATION");
                require("RAG_EMBEDDING_API_KEY".equals(str(json,"embeddingKeyVariable"))&&"RAG_CHAT_TEST_API_KEY".equals(str(json,"chatKeyVariable")),"KEY_VARIABLES");
                JsonObject db=json.getAsJsonObject("database");database(str(db,"host"),str(db,"name"));
                require(db.has("productionAllowed")&&!yes(db,"productionAllowed")&&yes(db,"requireNewDedicatedContainer"),"DATABASE_POLICY");
                JsonObject b=json.getAsJsonObject("requestBudget");
                require(num(b,"documentEmbeddingPosts")==2&&num(b,"queryEmbeddingPosts")==6&&num(b,"chatGenerationPosts")==6&&num(b,"maximumExternalRequests")==14,"BUDGET");
                for(String k:Arrays.asList("metadataGets","countTokensRequests","retries","redirects"))require(num(b,k)==0,"FORBIDDEN_REQUEST_POLICY");
                require("gemini-3.8-flash".equals(model),"CHAT_MODEL_MISSING");
                JsonObject cost=json.getAsJsonObject("cost");
                require(yes(cost,"currentProjectTierVerifiedForThisPlan")&&cost.has("paidRequestsAllowed")&&!yes(cost,"paidRequestsAllowed")&&cost.get("maximumApprovedPaidCostUsd").getAsBigDecimal().signum()==0&&cost.get("maximumEstimatedCostUsd").getAsBigDecimal().signum()==0&&yes(cost,"thinkingCostVerified"),"COST_UNVERIFIED");
                require(yes(json.getAsJsonObject("quota"),"verifiedForThisPlan"),"QUOTA_UNVERIFIED");
                approval=json.getAsJsonObject("liveApproval");require(approval!=null,"LIVE_APPROVAL_MISSING");
                long now=System.currentTimeMillis(),verified=approval.get("verifiedAtEpochMs").getAsLong(),expires=approval.get("expiresAtEpochMs").getAsLong();
                require(verified<=now&&expires>now&&expires-verified<=1800000,"APPROVAL_EXPIRED");
                require(model.equals(str(approval,"chatModel"))&&"gemini-embedding-001".equals(str(approval,"embeddingModel")),"MODEL_APPROVAL_MISMATCH");
                require(yes(approval,"dedicatedTestKeysConfirmed")&&yes(approval,"metadataEvidenceReviewed")&&yes(approval,"chatRestConfigurationReviewed"),"ACCOUNT_OR_MODEL_UNVERIFIED");
                for(String k:Arrays.asList("embeddingTier","chatTier"))require("Free Tier".equals(str(approval,k)),"PAID_TIER_FORBIDDEN");
                require(num(approval,"embeddingRpdRemaining")>=8&&num(approval,"chatRpdRemaining")>=6&&num(approval,"embeddingRpmRemaining")>=1&&num(approval,"chatRpmRemaining")>=1,"QUOTA_INSUFFICIENT");
                require(num(approval,"embeddingTpmRemaining")>=2048&&num(approval,"chatInputByteCap")>0&&num(approval,"chatInputByteCap")<=16384&&num(approval,"chatMaxOutputTokens")==1200,"TOKEN_LIMITS");
                // Gemini 3.8 Flash requires thinkingConfig={"thinkingLevel":"medium"}; thinkingBudget=0 and other levels are forbidden.
                JsonObject thinking=approval.getAsJsonObject("thinkingConfig");
                require(thinking!=null&&thinking.size()==1&&"medium".equals(str(thinking,"thinkingLevel"))&&!thinking.has("thinkingBudget")&&!yes(approval,"thinkingDisabledSupported"),"THINKING_CONFIGURATION");
                require(num(approval,"chatTpmRemaining")>=num(approval,"chatInputByteCap")+1200+64&&num(approval,"spacingMs")>=65000,"TOKEN_QUOTA_OR_SCHEDULE");
                require(num(approval,"chatInputTokenLimit")>=num(approval,"chatInputByteCap")+64&&num(approval,"chatOutputTokenLimit")>=1200,"CHAT_MODEL_TOKEN_LIMITS_UNVERIFIED");
                if(json.has("backupModelPolicy")){
                    JsonObject bm=json.getAsJsonObject("backupModelPolicy");
                    JsonObject bt=bm!=null?bm.getAsJsonObject("thinkingConfig"):null;
                    require(bm!=null&&"gemini-3.5-flash-lite".equals(str(bm,"backupModel"))&&!model.equals(str(bm,"backupModel"))
                        &&bt!=null&&bt.size()==1&&("low".equals(str(bt,"thinkingLevel"))||"minimal".equals(str(bt,"thinkingLevel")))&&!bt.has("thinkingBudget")
                        &&num(bm,"maxOutputTokens")==1200
                        &&bm.has("enabledDuring14RequestLiveRun")&&!yes(bm,"enabledDuring14RequestLiveRun")
                        &&num(bm,"extraBudgetRequests")==0
                        &&yes(bm,"noKeyOrProjectRotation")
                        &&yes(bm,"separatePlanApprovalRequiredForLiveFailover"),"BACKUP_MODEL_POLICY");
                }
                require(json.getAsJsonArray("documents").size()==2&&json.getAsJsonArray("questions").size()==6,"CORPUS_SIZE");
                for(int i=0;i<2;i++){JsonObject d=json.getAsJsonArray("documents").get(i).getAsJsonObject();require(("L"+(i+1)).equals(str(d,"id"))&&yes(d,"approvedForLive")&&num(d,"expectedChunks")==1,"DOCUMENT_APPROVAL");}
                for(int i=0;i<6;i++)require(("LQ"+(i+1)).equals(str(json.getAsJsonArray("questions").get(i).getAsJsonObject(),"id")),"QUESTION_ALLOWLIST");
            } catch(Halt h){throw h;}catch(Exception e){throw new Halt("INVALID_PLAN");}
        }
        public String endpoint(String kind){return "chat".equals(kind)?"https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent":EMBED;}
        public void beforeSend(){require(System.currentTimeMillis()<approval.get("expiresAtEpochMs").getAsLong(),"APPROVAL_EXPIRED");}
    }
    public static void database(String host,String name){require("127.0.0.1".equals(host)&&"fpt_rag_test".equals(name),"DATABASE_NOT_ISOLATED");}
    public static void key(String value){require(value!=null&&value.matches("[!-~]{7,512}"),"TEST_KEY_MISSING_OR_INVALID");}
    public interface Wire { Response post(String endpoint,String key,JsonObject body)throws Exception; }
    public static final class Response { public final int status;public final String body;public Response(int status,String body){this.status=status;this.body=body;} }
    public interface Validator<T>{T validate(Response response,JsonObject summary)throws Exception;}
    public interface Pause {void waitMillis(long millis)throws InterruptedException;}
    public static final class Ledger implements AutoCloseable {
        private final Plan plan;private final Path dir;private final FileChannel lock;
        private final Set<String> used=new HashSet<>();private final Map<String,Long> last=new HashMap<>();
        private final Pause pause; private boolean failed,closed; public int documents,queries,chats,total;public long quotaWaitMillis;
        private final Map<String,String> queryHashes=new HashMap<>();
        public Ledger(Plan plan,Path base,Pause pause)throws IOException{
            this.plan=plan;this.pause=pause;Files.createDirectories(base);
            require(!Files.isSymbolicLink(base),"EVIDENCE_SYMLINK");
            try(java.util.stream.Stream<Path> paths=Files.list(base)){require(!paths.anyMatch(p->p.getFileName().toString().startsWith(plan.id+".")),"PREVIOUS_EVIDENCE_OR_UNKNOWN_STATE");}
            // Exclusive CREATE_NEW is the cross-process arbitration; the file is NEVER removed.
            lock=FileChannel.open(base.resolve(plan.id+".request.lock"),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            dir=base.resolve(plan.id+".evidence");
            try{Files.createDirectory(dir);JsonObject first=counts();first.addProperty("planHash",plan.hash);first.addProperty("state","claimed_no_resume");write(lock,first);}
            catch(Throwable e){lock.close();throw e;}
        }
        public synchronized JsonObject counts(){JsonObject o=new JsonObject();o.addProperty("documentEmbedding",documents);o.addProperty("queryEmbedding",queries);o.addProperty("chat",chats);o.addProperty("total",total);return o;}
        public synchronized void healthy(){require(!failed&&!closed,"PLAN_HALTED");}
        public synchronized void awaitModel(String kind)throws InterruptedException{
            healthy();String model="chat".equals(kind)?plan.model:"gemini-embedding-001";
            long remaining=last.getOrDefault(model,0L)+num(plan.approval,"spacingMs")-System.currentTimeMillis();
            if(remaining>0){long start=System.currentTimeMillis();pause.waitMillis(remaining);quotaWaitMillis+=System.currentTimeMillis()-start;}plan.beforeSend();
        }
        public synchronized void freezeQueries(Map<String,String> texts)throws IOException{
            healthy();require(total==0&&queryHashes.isEmpty()&&texts.size()==6,"QUERY_FREEZE_STATE");
            rag.GeminiEmbeddingService builder=new rag.GeminiEmbeddingService(new rag.RagConfig(true,true,true,"gemini-embedding-001",768),null);
            JsonObject evidence=new JsonObject();
            for(int i=1;i<=6;i++){String id="LQ"+i,text=texts.get(id);require(text!=null&&!text.isEmpty(),"QUERY_ALLOWLIST");String hash=VectorValues.hash(builder.requestBody(null,text,"RETRIEVAL_QUERY").toString());queryHashes.put(id,hash);evidence.addProperty(id,hash);}
            create(dir.resolve("frozen-query-payload-hashes.json"),evidence);
        }
        public synchronized void abort(String code){failed=true;try{event("halt-"+UUID.randomUUID(),code);}catch(Exception ignored){}}
        public synchronized void event(String name,String code)throws IOException{JsonObject o=counts();o.addProperty("state",code);create(dir.resolve(name+".json"),o);}
        public synchronized <T>T send(String kind,String operation,String method,String endpoint,String apiKey,JsonObject body,Wire wire,Validator<T> validator){
            healthy();FileChannel result=null;JsonObject summary=new JsonObject();long started=0;
            try {
                plan.beforeSend();key(apiKey);
                require("POST".equals(method)&&plan.endpoint(kind).equals(endpoint),"UNPLANNED_ENDPOINT_OR_METHOD");
                require(Arrays.asList("document","query","chat").contains(kind),"UNPLANNED_KIND");
                require(operation.matches("document-L[12]|query-LQ[1-6]|chat-LQ[1-6]")&&operation.startsWith(kind+"-"),"UNPLANNED_OPERATION");
                require(!used.contains(operation),"OPERATION_ALREADY_RESERVED");
                require(total<14&&("document".equals(kind)?documents<2:"query".equals(kind)?queries<6:chats<6),"BUDGET_EXCEEDED");
                if(!"chat".equals(kind)){
                    require("models/gemini-embedding-001".equals(str(body,"model"))&&body.has("outputDimensionality")&&"768".equals(body.get("outputDimensionality").toString())&&!body.has("embedContentConfig"),"WIRE_EMBEDDING_CONFIGURATION");
                    require(("document".equals(kind)?"RETRIEVAL_DOCUMENT":"RETRIEVAL_QUERY").equals(str(body,"taskType")),"WIRE_TASK");
                    String text=body.getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
                    require(bytes(text).length+bytes(str(body,"title")).length+64<=2048,"EMBEDDING_INPUT_LIMIT");
                    if("document".equals(kind)){
                        JsonObject d=plan.json.getAsJsonArray("documents").get(operation.endsWith("1")?0:1).getAsJsonObject();
                        require(text.equals(str(d,"content"))&&str(body,"title").equals(str(d,"title")),"UNAPPROVED_DOCUMENT_BODY");
                        rag.GeminiEmbeddingService builder=new rag.GeminiEmbeddingService(new rag.RagConfig(true,true,true,"gemini-embedding-001",768),null);
                        require(body.equals(builder.requestBody(str(d,"title"),str(d,"content"),"RETRIEVAL_DOCUMENT")),"UNAPPROVED_DOCUMENT_FIELDS");
                    }else {
                        require(!body.has("title"),"QUERY_TITLE");
                        require(VectorValues.hash(body.toString()).equals(queryHashes.get(operation.substring("query-".length()))),"UNAPPROVED_QUERY_BODY");
                    }
                }else{
                    JsonObject g=body.getAsJsonObject("generationConfig");JsonArray c=body.getAsJsonArray("contents");
                    require(g!=null&&num(g,"maxOutputTokens")==1200&&g.has("thinkingConfig")&&g.getAsJsonObject("thinkingConfig").equals(plan.approval.getAsJsonObject("thinkingConfig"))&&"application/json".equals(str(g,"responseMimeType"))&&g.has("responseJsonSchema")&&!g.has("temperature")&&!g.has("topP")&&!g.has("topK")&&!g.has("candidateCount")&&!g.has("thinkingBudget"),"CHAT_CONFIGURATION");
                    require(c!=null&&c.size()>0&&"user".equals(str(c.get(c.size()-1).getAsJsonObject(),"role")),"CHAT_TURN_ROLE");
                    require(bytes(body.toString()).length+64<=num(plan.approval,"chatInputByteCap"),"CHAT_INPUT_BOUND");
                }
                String model="chat".equals(kind)?plan.model:"gemini-embedding-001";
                awaitModel(kind);
                // Reserve writable result storage before recording intent to send.
                result=FileChannel.open(dir.resolve(operation+".result.json"),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
                used.add(operation);total++;if("document".equals(kind))documents++;else if("query".equals(kind))queries++;else chats++;
                summary=counts();summary.addProperty("operation",operation);summary.addProperty("model",model);summary.addProperty("bodyHash",VectorValues.hash(body.toString()));summary.addProperty("state","reserved_unknown_until_result");summary.addProperty("planHash",plan.hash);
                if(!"chat".equals(kind)){summary.addProperty("taskType",str(body,"taskType"));summary.addProperty("expectedDimensions",768);summary.addProperty("vectorValid",false);}
                // A forced immutable journal entry exists before ANY transport connect/send.
                create(dir.resolve(String.format(Locale.ROOT,"%02d-%s.reserved.json",total,operation)),summary);
                last.put(model,System.currentTimeMillis());started=System.nanoTime();
                Response r=wire.post(endpoint,apiKey,body);summary.addProperty("httpStatus",r.status);summary.addProperty("latencyMs",(System.nanoTime()-started)/1e6);
                require(r.status==200,"HTTP_"+r.status);
                summary.addProperty("latencyTargetMs","chat".equals(kind)?10000:5000);
                require(summary.get("latencyMs").getAsDouble()<=("chat".equals(kind)?10000:5000),"REQUEST_LATENCY_TARGET");
                T checked=validator.validate(r,summary);
                summary.addProperty("state","validated");write(result,summary);return checked;
            }catch(Throwable e){
                failed=true;summary.addProperty("state","halted_no_retry");String code=e instanceof Halt?e.getMessage():e instanceof java.net.SocketTimeoutException?"TIMEOUT":e instanceof IOException?"CONNECTION_OR_EVIDENCE_ERROR":"RESPONSE_INVALID";
                summary.addProperty("errorCode",code);if(started!=0)summary.addProperty("latencyMs",(System.nanoTime()-started)/1e6);
                try{if(result!=null)write(result,summary);event("halt-"+UUID.randomUUID(),code);}catch(Exception ignored){}
                throw new Halt(code);
            }finally{try{if(result!=null)result.close();}catch(IOException e){failed=true;throw new Halt("EVIDENCE_CLOSE_ERROR");}}
        }
        public synchronized void finish(JsonObject quality)throws IOException{healthy();JsonObject out=counts();out.add("quality",quality);out.addProperty("state","complete");out.addProperty("estimatedFreeTierCostUsd",0);out.addProperty("quotaWaitMillis",quotaWaitMillis);create(dir.resolve("summary.json"),out);}
        public Path evidence(){return dir;}
        public synchronized void close()throws IOException{closed=true;lock.close();}
    }
    public static void create(Path path,JsonObject data)throws IOException{try(FileChannel f=FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){write(f,data);}}
    private static void write(FileChannel f,JsonObject data)throws IOException{ByteBuffer b=ByteBuffer.wrap(bytes(data.toString()));while(b.hasRemaining())f.write(b);f.force(true);}
}
