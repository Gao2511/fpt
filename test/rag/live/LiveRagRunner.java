package rag.live;

import ai.ConsultationBehaviorTest;
import ai.consultation.*;
import ai.dto.*;
import ai.provider.GeminiProvider;
import ai.session.ChatSessionData;
import com.google.gson.*;
import dao.*;
import dto.*;
import rag.*;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** CLI test harness, not deployed in WAR. Reuses existing worker, SQL retrieval, source policy and Engine. */
public final class LiveRagRunner {
    static final RagConfig CONFIG=new RagConfig(true,true,true,"gemini-embedding-001",768);
    static void require(boolean ok,String c){LiveSafety.require(ok,c);}
    static class Embeddings implements EmbeddingService {
        final LiveSafety.Ledger ledger;final LiveSafety.Wire wire;final String key;final JsonObject plan;
        final Map<String,String> queries;final Map<String,float[]> cache=new HashMap<>();String caseId;double queryLatencyMs;
        final GeminiEmbeddingService payload=new GeminiEmbeddingService(CONFIG,null,(a,b,c,d,e)->{throw new LiveSafety.Halt("DEFAULT_EMBEDDING_TRANSPORT_FORBIDDEN");});
        Embeddings(LiveSafety.Ledger ledger,LiveSafety.Wire wire,String key,JsonObject plan,Map<String,String> queries){this.ledger=ledger;this.wire=wire;this.key=key;this.plan=plan;this.queries=queries;}
        public String model(){return CONFIG.model;}public int dimensions(){return 768;}
        public float[] document(String title,String text){
            String id=null;for(JsonElement el:plan.getAsJsonArray("documents")){JsonObject d=el.getAsJsonObject();if(title.equals(LiveSafety.str(d,"title"))&&text.equals(LiveSafety.str(d,"content")))id=LiveSafety.str(d,"id");}
            require(id!=null,"UNAPPROVED_DOCUMENT");return send("document",id,payload.requestBody(title,text,"RETRIEVAL_DOCUMENT"));
        }
        void queryFor(String id){caseId=id;String text=queries.get(id);require(text!=null,"UNAPPROVED_QUERY");cache.put(id,send("query",id,payload.requestBody(null,text,"RETRIEVAL_QUERY")));}
        public float[] query(String text){require(text.equals(queries.get(caseId))&&cache.containsKey(caseId),"UNPLANNED_QUERY_OR_RETRY");return cache.get(caseId).clone();}
        float[] send(String kind,String id,JsonObject body){return ledger.send(kind,kind+"-"+id,"POST",LiveSafety.EMBED,key,body,wire,(r,s)->{if("query".equals(kind))queryLatencyMs=s.get("latencyMs").getAsDouble();return vector(r,s);});}
    }
    public static float[] vector(LiveSafety.Response response,JsonObject summary)throws Exception {
        JsonArray a=JsonParser.parseString(response.body).getAsJsonObject().getAsJsonObject("embedding").getAsJsonArray("values");
        summary.addProperty("returnedDimensions",a.size());require(a.size()==768,"VECTOR_DIMENSIONS");float[] values=new float[768];
        for(int i=0;i<768;i++){require(a.get(i).isJsonPrimitive()&&a.get(i).getAsJsonPrimitive().isNumber(),"VECTOR_TYPE");values[i]=a.get(i).getAsFloat();}
        values=VectorValues.normalized(values,768);double norm=0;for(float v:values)norm+=(double)v*v;
        require(Math.abs(Math.sqrt(norm)-1)<.00001,"VECTOR_NORMALIZATION");summary.addProperty("vectorValid",true);summary.addProperty("normalizedL2",Math.sqrt(norm));usage(JsonParser.parseString(response.body).getAsJsonObject(),summary);return values;
    }
    static void usage(JsonObject root,JsonObject summary){
        JsonObject safe=new JsonObject();JsonObject data=root.getAsJsonObject("usageMetadata");
        for(String k:Arrays.asList("promptTokenCount","candidatesTokenCount","totalTokenCount","thoughtsTokenCount")){if(data!=null&&data.has(k)&&data.get(k).isJsonPrimitive()&&data.get(k).getAsJsonPrimitive().isNumber())safe.addProperty(k,data.get(k).getAsLong());else safe.add(k,JsonNull.INSTANCE);}summary.add("usage",safe);
    }
    static class Chunks extends KnowledgeChunkDAO {
        List<KnowledgeChunkDTO> candidates=Collections.emptyList();String query;final LiveSafety.Ledger ledger;
        Chunks(RagDatabase db,LiveSafety.Ledger ledger){super(db);this.ledger=ledger;}
        public List<KnowledgeChunkDTO> search(String q,float[] v,String m,int d,int limit){try{require(v!=null&&v.length==768&&m.equals(CONFIG.model)&&d==768,"RETRIEVAL_VECTOR_METADATA");query=q;return candidates=super.search(q,v,m,d,limit);}catch(Exception e){ledger.abort("RETRIEVAL_DATABASE_FAILURE");throw new LiveSafety.Halt("RETRIEVAL_DATABASE_FAILURE");}}
        public boolean current(KnowledgeChunkDTO chunk){try{return super.current(chunk);}catch(Exception e){ledger.abort("CURRENTNESS_DATABASE_FAILURE");throw new LiveSafety.Halt("CURRENTNESS_DATABASE_FAILURE");}}
    }
    static final class Chat extends GeminiProvider {
        final LiveSafety.Plan plan;final LiveSafety.Ledger ledger;final LiveSafety.Wire wire;final String key;final ProductCatalog catalog;final Chunks chunks;
        String caseId,question;List<Long> selected=new ArrayList<>();
        Chat(LiveSafety.Plan p,LiveSafety.Ledger l,LiveSafety.Wire w,String k,ProductCatalog c,Chunks chunks){plan=p;ledger=l;wire=w;key=k;catalog=c;this.chunks=chunks;}
        @Override public ChatResponse chat(List<ChatMessage> messages,String ignored,String model,String url,ChatOptions options){
            require(plan.model.equals(model)&&options.getMaxTokens()==1200&&options.getResponseSchema()!=null&&"".equals(url),"CHAT_OVERRIDE");
            require(messages.get(messages.size()-1).getContent().endsWith("Câu hỏi của khách:\n"+question),"UNPLANNED_CHAT_OR_REPAIR");
            require(messages.get(0).getContent().startsWith(ConsultationPrompt.instructions(ConsultationPrompt.SYSTEM)),"CHAT_TEMPLATE");
            JsonObject body=buildRequestBody(messages,options,model);JsonObject gen=body.getAsJsonObject("generationConfig");
            gen.addProperty("responseMimeType","application/json");gen.add("responseJsonSchema",options.getResponseSchema());gen.add("thinkingConfig",plan.approval.getAsJsonObject("thinkingConfig").deepCopy());
            return ledger.send("chat","chat-"+caseId,"POST",plan.endpoint("chat"),key,body,wire,(r,s)->{
                JsonObject root=JsonParser.parseString(r.body).getAsJsonObject();JsonArray candidates=root.getAsJsonArray("candidates");require(candidates!=null&&candidates.size()==1,"CHAT_CANDIDATES");JsonObject c=candidates.get(0).getAsJsonObject();
                require("STOP".equals(LiveSafety.str(c,"finishReason")),"CHAT_TRUNCATED_OR_BLOCKED");
                String text=customerText(c.getAsJsonObject("content").getAsJsonArray("parts"));
                ChatResponse response=new ChatResponse(text,"gemini",model,s.get("latencyMs").getAsLong());response.setFinishReason("STOP");
                RagContextBuilder context=new RagContextBuilder(KnowledgeSourceSelection.select(chunks.candidates,chunks.query).chunks);
                ConsultationReply reply=ConsultationReply.parse(response,catalog,key,context);
                require("none".equals(reply.action)&&reply.packageIds.isEmpty()&&reply.recommendedPackageId==null,"UNEXPECTED_BUSINESS_ACTION");selected=new ArrayList<>(reply.knowledgeChunkIds);
                if(context.empty()&&expectedSources(plan.json,caseId).isEmpty())require(unknownReplyAllowed(reply.message),"UNSUPPORTED_UNKNOWN_PROSE");
                usage(root,s);if(root.has("usageMetadata")){JsonObject u=root.getAsJsonObject("usageMetadata");if(u.has("promptTokenCount")&&!u.get("promptTokenCount").isJsonNull()){int count=u.get("promptTokenCount").getAsInt();require(count>=0&&count<=LiveSafety.num(plan.approval,"chatInputByteCap")+64,"CHAT_INPUT_USAGE_BOUND");response.setPromptTokens(count);}if(u.has("candidatesTokenCount")&&!u.get("candidatesTokenCount").isJsonNull()){int count=u.get("candidatesTokenCount").getAsInt();require(count>=0&&count<=1200,"CHAT_OUTPUT_USAGE_BOUND");response.setCompletionTokens(count);}int thoughts=0;if(u.has("thoughtsTokenCount")&&!u.get("thoughtsTokenCount").isJsonNull()){thoughts=u.get("thoughtsTokenCount").getAsInt();require(thoughts>=0&&thoughts<=1200,"CHAT_THINKING_USAGE_BOUND");}require(response.getCompletionTokens()+thoughts<=1200,"CHAT_TOTAL_OUTPUT_USAGE_BOUND");if(u.has("totalTokenCount")&&!u.get("totalTokenCount").isJsonNull()){int totalTokens=u.get("totalTokenCount").getAsInt();require(totalTokens>=0&&totalTokens<=LiveSafety.num(plan.approval,"chatInputByteCap")+1200+64,"CHAT_TOTAL_USAGE_BOUND");response.setTotalTokens(totalTokens);}}
                return response;
            });
        }
        @Override public List<ModelInfo> listModels(String k,String u){throw new LiveSafety.Halt("MODEL_LISTING_FORBIDDEN");}
        @Override public ChatResponse testConnection(String k,String m,String u){throw new LiveSafety.Halt("PROBE_FORBIDDEN");}
        @Override public void chatStream(List<ChatMessage> m,String k,String model,String u,ChatOptions o,StreamCallback c){throw new LiveSafety.Halt("STREAM_FORBIDDEN");}
    }
    static List<String> expectedSources(JsonObject plan,String id){for(JsonElement e:plan.getAsJsonArray("questions")){JsonObject q=e.getAsJsonObject();if(id.equals(LiveSafety.str(q,"id"))){List<String> out=new ArrayList<>();for(JsonElement s:q.getAsJsonArray("expectedSelectedDocuments"))out.add(s.getAsString());return out;}}throw new LiveSafety.Halt("UNPLANNED_CASE");}
    static boolean unknownReplyAllowed(String text){return Arrays.asList(
        "Dạ em chưa có tài liệu đã duyệt để xác nhận thông tin này. Anh/chị muốn tư vấn thêm về dịch vụ Internet không?",
        "Dạ em chưa có nguồn còn hiệu lực và thống nhất để xác nhận thông tin này. Anh/chị vui lòng liên hệ tư vấn viên để được kiểm tra trường hợp cụ thể."
    ).contains(text);}
    static void turnLatency(double millis){require(Double.isFinite(millis)&&millis>=0&&millis<=10000,"TURN_LATENCY_TARGET");}
    static Map<String,String> queryInputs(JsonObject plan,ProductCatalog catalog){
        Map<String,String> result=new LinkedHashMap<>();Map<String,ChatSessionData> sessions=new HashMap<>();
        for(JsonElement element:plan.getAsJsonArray("questions")){JsonObject q=element.getAsJsonObject();String id=LiveSafety.str(q,"id"),text=LiveSafety.str(q,"text"),session=q.has("session")?LiveSafety.str(q,"session"):id;ChatSessionData data=sessions.computeIfAbsent(session,ChatSessionData::new);data.getRequirements().update(text);
            RetrievalQuery rewrite=RetrievalQuery.plan(text,data.getHistory(),data.getRequirements(),catalog);String input=rewrite.query==null||rewrite.query.trim().isEmpty()?text:rewrite.query;result.put(id,input);data.commitTurn(text,"Dạ em đã nhận câu hỏi.");}
        return result;
    }
    static void verifyDatabase(RagDatabase db,String nonce)throws Exception{
        try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database(),current_user,(SELECT nonce FROM phase241_test_identity),(SELECT count(*) FROM knowledge_documents),(SELECT count(*) FROM knowledge_chunks)")){
            require(r.next()&&"fpt_rag_test".equals(r.getString(1))&&"fpt_rag_user".equals(r.getString(2))&&nonce.equals(r.getString(3))&&r.getLong(4)==0&&r.getLong(5)==0,"DATABASE_IDENTITY_OR_FRESHNESS");
        }
    }
    static ProductCatalog business(RagDatabase db)throws Exception{
        // Authoritative test business snapshot uses the same JDBC source pattern as Phase 2.4, never CMS production settings.
        try(Connection c=db.open();Statement s=c.createStatement()){
            s.execute("CREATE TABLE phase241_business_packages(id int primary key,price bigint,speed int,description text,fee bigint)");
            try(PreparedStatement p=c.prepareStatement("INSERT INTO phase241_business_packages VALUES (?,?,?,?,?)")){for(ProductCatalog.Product item:ConsultationBehaviorTest.CATALOG.all()){p.setInt(1,item.id);p.setLong(2,item.price);p.setInt(3,item.speed);p.setString(4,"Modem WiFi 6"+(item.camera?", 1 Camera":"")+(item.tv180?", TV 180 kênh, nội dung Ngoại Hạng Anh":""));p.setLong(5,item.standardInstallationFee);p.executeUpdate();}}
        }
        List<PackageDTO> rows=new ArrayList<>();try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM phase241_business_packages ORDER BY id")){while(r.next()){PackageDTO p=new PackageDTO();p.setId(r.getInt("id"));p.setPrice(r.getLong("price"));p.setSpeedMbps(r.getInt("speed"));p.setDescription(r.getString("description"));p.setStandardInstallationFee(r.getLong("fee"));rows.add(p);}}
        return new ProductCatalog(rows,cms.SiteContent.defaults());
    }
    public static JsonObject run(LiveSafety.Plan plan,LiveSafety.Ledger ledger,RagDatabase db,LiveSafety.Wire wire,String embeddingKey,String chatKey,ProductCatalog catalog)throws Exception{
        Map<String,String> inputs=queryInputs(plan.json,catalog);
        for(JsonElement el:plan.json.getAsJsonArray("documents")){JsonObject d=el.getAsJsonObject();require(new DocumentChunkingService().chunk(LiveSafety.str(d,"content")).size()==1,"DOCUMENT_CHUNK_BUDGET");require(LiveSafety.bytes(LiveSafety.str(d,"content")+LiveSafety.str(d,"title")).length+64<=2048,"EMBEDDING_INPUT_LIMIT");}
        for(String text:inputs.values())require(LiveSafety.bytes(text).length+64<=2048,"EMBEDDING_INPUT_LIMIT");
        // Freeze deterministic rewritten payloads before the first request; subsequent actual retrieval must match.
        ledger.freezeQueries(inputs);
        Embeddings emb=new Embeddings(ledger,wire,embeddingKey,plan.json,inputs);
        KnowledgeDocumentDAO docs=new KnowledgeDocumentDAO(db,CONFIG);KnowledgeIndexJobDAO jobs=new KnowledgeIndexJobDAO(db);Chunks chunks=new Chunks(db,ledger);
        KnowledgeIndexingService worker=new KnowledgeIndexingService(db,docs,chunks,jobs,emb);Map<Long,String> sourceNames=new HashMap<>();Map<String,String> literals=new HashMap<>();
        for(JsonElement el:plan.json.getAsJsonArray("documents")){JsonObject d=el.getAsJsonObject();String id=LiveSafety.str(d,"id"),content=LiveSafety.str(d,"content");require(new DocumentChunkingService().chunk(content).size()==1,"DOCUMENT_CHUNK_BUDGET");
            KnowledgeDocumentDTO saved=docs.save(new KnowledgeDocumentDTO(0,0,LiveSafety.str(d,"title"),"guide",content,"TEST ONLY: "+LiveSafety.str(d,"scope"),"published",null,null,"pending","phase241."+id.toLowerCase(Locale.ROOT),"admin_reviewed",null),1);sourceNames.put(saved.id,id);literals.put(id,content);
            require(worker.runNext(),"INDEX_JOB_MISSING");ledger.healthy();require("ready".equals(docs.find(saved.id).indexingStatus),"INDEXING_FAILURE");
        }
        // Stored vectors must come only from the current two successful requests, never an old/mock DB cache.
        try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*),min(vector_dims(embedding)),max(vector_dims(embedding)),count(*) FILTER (WHERE embedding_model='gemini-embedding-001' AND vector_dimensions=768) FROM knowledge_chunks")){require(r.next()&&r.getInt(1)==2&&r.getInt(2)==768&&r.getInt(3)==768&&r.getInt(4)==2&&ledger.documents==2,"STORED_VECTOR_METADATA");}
        JsonArray cases=new JsonArray();double precision=0;int positives=0,abstentions=0;Map<String,ChatSessionData> sessions=new HashMap<>();
        for(JsonElement element:plan.json.getAsJsonArray("questions")){JsonObject q=element.getAsJsonObject();String id=LiveSafety.str(q,"id"),text=LiveSafety.str(q,"text"),sessionId=q.has("session")?LiveSafety.str(q,"session"):id;
            emb.queryFor(id);chunks.candidates=Collections.emptyList();ChatSessionData session=sessions.computeIfAbsent(sessionId,ChatSessionData::new);Chat chat=new Chat(plan,ledger,wire,chatKey,catalog,chunks);chat.caseId=id;chat.question=text;
            // Quota delay happens outside the Engine's 40s deadline. Transport still independently paces every send.
            ledger.awaitModel("chat");long waitBefore=ledger.quotaWaitMillis;
            long start=System.nanoTime();ChatResponse reply=new ConsultationEngine(new KnowledgeRetrievalService(CONFIG,chunks,emb)).chat(text,session,catalog,new ConsultationEngine.Target(chat,"",plan.model,""),null,new ChatOptions(.3,1200));ledger.healthy();
            List<String> expected=new ArrayList<>();for(JsonElement e:q.getAsJsonArray("expectedSelectedDocuments"))expected.add(e.getAsString());
            Set<String> selected=new LinkedHashSet<>();for(Long chunkId:chat.selected)for(KnowledgeChunkDTO c:chunks.candidates)if(c.id==chunkId)selected.add(sourceNames.get(c.documentId));
            LinkedHashSet<String> top=new LinkedHashSet<>();for(KnowledgeChunkDTO c:chunks.candidates){top.add(sourceNames.get(c.documentId));if(top.size()==3)break;}
            JsonObject observation=new JsonObject();observation.addProperty("id",id);observation.add("expectedDocuments",new Gson().toJsonTree(expected));observation.add("retrievedDocuments",new Gson().toJsonTree(top));observation.add("selectedDocuments",new Gson().toJsonTree(selected));
            List<String> validated=new ArrayList<>();for(KnowledgeChunkDTO chunk:KnowledgeSourceSelection.select(chunks.candidates,chunks.query).chunks)validated.add(sourceNames.get(chunk.documentId));observation.add("validatedDocuments",new Gson().toJsonTree(validated));
            observation.addProperty("retrievalQuery",chunks.query);
            JsonArray ranked=new JsonArray();for(KnowledgeChunkDTO c:chunks.candidates){JsonObject item=new JsonObject();item.addProperty("document",sourceNames.get(c.documentId));item.addProperty("score",c.score);item.addProperty("title",c.title);item.addProperty("policyKey",c.metadata.policyKey);item.addProperty("authority",c.metadata.authority);ranked.add(item);}observation.add("rankedCandidates",ranked);
            LiveSafety.create(ledger.evidence().resolve(id+".sources.json"),observation);
            require(selected.equals(new LinkedHashSet<>(expected)),"SOURCE_SELECTION_"+id);
            if(!expected.isEmpty()){require(top.containsAll(expected),"RETRIEVAL_RECALL_"+id);int relevant=0;for(String s:top)if(expected.contains(s))relevant++;precision+=(double)relevant/top.size();positives++;
                String grounded="Dạ theo tài liệu hướng dẫn đã được duyệt:\n";for(String s:selected)grounded+=literals.get(s)+"\n";require(grounded.trim().equals(reply.getContent()),"GROUNDING_OR_CURRENTNESS_"+id);
            }else{require(unknownReplyAllowed(reply.getContent()),"UNSUPPORTED_CLAIM_"+id);abstentions++;}
            require("none".equals(reply.getAction())&&reply.getContent()!=null&&!reply.getContent().matches("(?is).*(knowledgeChunkIds|documentHash|State Tracking|Tag Constraint|api.?key).*"),"PUBLIC_SAFETY");
            require(!reply.getContent().contains(embeddingKey)&&!reply.getContent().contains(chatKey),"SECRET_LEAK");
            JsonObject publicReply=controller.AIChatServlet.customerJson(reply);require(publicReply.get("reply").getAsString().equals(reply.getContent())&&!publicReply.has("knowledgeContext"),"PUBLIC_PROJECTION");
            JsonObject row=observation;row.addProperty("customerReply",reply.getContent());double engineMillis=(System.nanoTime()-start)/1e6-(ledger.quotaWaitMillis-waitBefore);row.addProperty("engineLatencyMsExcludingEmbeddingAndQuotaWait",engineMillis);row.addProperty("turnLatencyMsExcludingQuotaWait",engineMillis+emb.queryLatencyMs);cases.add(row);LiveSafety.create(ledger.evidence().resolve(id+".quality.json"),row);turnLatency(engineMillis+emb.queryLatencyMs);
        }
        require(positives==4&&abstentions==2&&precision/positives>=.8,"RETRIEVAL_PRECISION_OR_CASE_COUNT");require(ledger.documents==2&&ledger.queries==6&&ledger.chats<=6&&ledger.total<=14,"FINAL_BUDGET");
        JsonObject quality=new JsonObject();quality.add("cases",cases);quality.addProperty("positiveRecallCases",positives);quality.addProperty("unknownAbstentions",abstentions);quality.addProperty("meanPrecisionAt3",precision/positives);quality.addProperty("unsupportedClaims",0);ledger.finish(quality);return quality;
    }
    public static void main(String[] args){
        LiveSafety.Ledger ledger=null;
        try{
            JsonObject raw=JsonParser.parseString(new String(Files.readAllBytes(Paths.get(args[1])),StandardCharsets.UTF_8)).getAsJsonObject();
            LiveSafety.Plan plan=new LiveSafety.Plan(raw);
            require("development".equals(System.getenv("RAG_ENVIRONMENT"))&&"false".equals(System.getenv("RAG_ENABLED")),"ENVIRONMENT");
            String embed=System.getenv("RAG_EMBEDDING_API_KEY"),chat=System.getenv("RAG_CHAT_TEST_API_KEY");LiveSafety.key(embed);LiveSafety.key(chat);
            if("--preflight".equals(args[0])){System.out.println("Preflight policy/key presence: PASS; no API or database access.");return;}
            require("--execute-approved".equals(args[0]),"EXECUTION_FLAG");
            LiveSafety.database(System.getenv("RAG_TEST_DB_HOST"),System.getenv("RAG_TEST_DB_NAME"));require("true".equals(System.getenv("RAG_TEST_CONTAINER_VERIFIED")),"CONTAINER_NOT_VERIFIED");
            String port=System.getenv("RAG_TEST_DB_PORT"),password=System.getenv("RAG_TEST_DB_PASSWORD"),nonce=System.getenv("RAG_TEST_DB_NONCE");
            require(port!=null&&port.matches("[0-9]{1,5}")&&Integer.parseInt(port)>0&&Integer.parseInt(port)<=65535&&password!=null&&!password.isEmpty()&&nonce!=null&&nonce.matches("[a-f0-9]{32}"),"TEST_DATABASE_CONFIGURATION");
            ledger=new LiveSafety.Ledger(plan,Paths.get(args[2]),Thread::sleep);final LiveSafety.Ledger guard=ledger;
            String url="jdbc:postgresql://127.0.0.1:"+port+"/fpt_rag_test?sslmode=disable&connectTimeout=3&socketTimeout=4";
            RagDatabase db=new RagDatabase(()->{try{return DriverManager.getConnection(url,"fpt_rag_user",password);}catch(Exception e){guard.abort("DATABASE_CONNECTION_FAILURE");throw new LiveSafety.Halt("DATABASE_CONNECTION_FAILURE");}});
            verifyDatabase(db,nonce);ProductCatalog catalog=business(db);run(plan,ledger,db,new SinglePostWire(),embed,chat,catalog);
            System.out.println("Sanitized result: "+ledger.evidence().resolve("summary.json"));
        }catch(Throwable e){String code=e instanceof LiveSafety.Halt?e.getMessage():"RUNNER_PREFLIGHT_OR_DATABASE_FAILURE";if(ledger!=null)ledger.abort(code);System.err.println("Live RAG stopped: "+code+"; no retry/resume.");if(ledger!=null)System.err.println("Sanitized evidence: "+ledger.evidence());System.exit(1);}
        finally{try{if(ledger!=null)ledger.close();}catch(Exception ignored){}}
    }
}
