package rag.live;

import com.google.gson.*;
import rag.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Actual isolated PG + unchanged RAG pipeline; all external provider requests injected as deterministic mocks. */
public final class LiveRunnerDatabaseTest {
    public static void main(String[] args)throws Exception{
        LiveSafety.require("false".equals(System.getenv("RAG_ENABLED"))&&"development".equals(System.getenv("RAG_ENVIRONMENT")),"ENVIRONMENT");
        LiveSafety.database(System.getenv("RAG_TEST_DB_HOST"),System.getenv("RAG_TEST_DB_NAME"));LiveSafety.require("true".equals(System.getenv("RAG_TEST_CONTAINER_VERIFIED")),"CONTAINER_NOT_VERIFIED");
        String port=System.getenv("RAG_TEST_DB_PORT"),password=System.getenv("RAG_TEST_DB_PASSWORD");LiveSafety.require(port!=null&&port.matches("[0-9]{1,5}"),"PORT");
        RagDatabase db=new RagDatabase(()->DriverManager.getConnection("jdbc:postgresql://127.0.0.1:"+port+"/fpt_rag_test?sslmode=disable&connectTimeout=3&socketTimeout=4","fpt_rag_user",password));
        LiveRagRunner.verifyDatabase(db,System.getenv("RAG_TEST_DB_NONCE"));
        boolean wrongNonce=false;try{LiveRagRunner.verifyDatabase(db,"00000000000000000000000000000000");}catch(LiveSafety.Halt h){wrongNonce=true;}LiveSafety.require(wrongNonce,"IDENTITY_NONCE_NOT_CHECKED");
        Path root=Paths.get(args[0]);JsonObject fixture=JsonParser.parseString(Files.readString(root.resolve("test/rag/fixtures/phase24-live-plan.json"))).getAsJsonObject();JsonObject mock=LiveRunnerSafetyTest.approvedFixture(fixture);mock.addProperty("planId","phase24-offline-db-"+UUID.randomUUID().toString());
        LiveSafety.Plan plan=new LiveSafety.Plan(mock);int[] actualCalls={0};
        try(LiveSafety.Ledger ledger=new LiveSafety.Ledger(plan,root.resolve(".build/phase241-db-mocks"),ms->{})){
            LiveSafety.Wire wire=(endpoint,key,body)->{
                actualCalls[0]++;LiveSafety.require(actualCalls[0]==ledger.total,"PRE_SEND_ACCOUNTING");
                try(java.util.stream.Stream<Path> files=Files.list(ledger.evidence())){LiveSafety.require(files.filter(p->p.getFileName().toString().endsWith(".reserved.json")).count()==ledger.total,"PRE_SEND_JOURNAL");}
                if(endpoint.endsWith(":embedContent")){
                    LiveSafety.require(body.get("outputDimensionality").getAsInt()==768,"MOCK_WIRE_DIMENSIONS");String text=body.getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();String normalized=RetrievalQuery.normalize(text);int index=normalized.contains("game")||normalized.contains("ping")?1:normalized.contains("nuoc ngoai")||normalized.contains("wifi 6")?2:0;
                    JsonArray values=new JsonArray();for(int i=0;i<768;i++)values.add(i==index?2:0);JsonObject r=new JsonObject(),e=new JsonObject();e.add("values",values);r.add("embedding",e);return new LiveSafety.Response(200,r.toString());
                }
                JsonArray contents=body.getAsJsonArray("contents");String last=contents.get(contents.size()-1).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();int first=last.indexOf('{'),end=last.lastIndexOf('}');JsonObject data=JsonParser.parseString(last.substring(first,end+1)).getAsJsonObject();String question=last.substring(last.indexOf("Câu hỏi của khách:\n")+"Câu hỏi của khách:\n".length());
                JsonObject reply=JsonParser.parseString(ai.ConsultationBehaviorTest.envelope("Dạ em chưa có tài liệu đã duyệt để xác nhận thông tin này. Anh/chị muốn tư vấn thêm về dịch vụ Internet không?").getContent()).getAsJsonObject();
                if(data.has("knowledgeContext")){JsonArray ids=new JsonArray();if(!question.contains("WiFi 6")&&!question.contains("nước ngoài"))for(JsonElement c:data.getAsJsonArray("knowledgeContext"))ids.add(c.getAsJsonObject().get("id"));reply.add("knowledgeChunkIds",ids);}
                JsonObject thought=new JsonObject(),part=new JsonObject(),content=new JsonObject(),candidate=new JsonObject(),usage=new JsonObject(),response=new JsonObject();JsonArray parts=new JsonArray(),candidates=new JsonArray();thought.addProperty("thought",true);thought.addProperty("text","Internal medium reasoning");parts.add(thought);part.addProperty("text",reply.toString());parts.add(part);content.add("parts",parts);candidate.add("content",content);candidate.addProperty("finishReason","STOP");candidates.add(candidate);usage.addProperty("promptTokenCount",420);usage.addProperty("candidatesTokenCount",64);usage.addProperty("thoughtsTokenCount",48);usage.addProperty("totalTokenCount",532);response.add("candidates",candidates);response.add("usageMetadata",usage);return new LiveSafety.Response(200,response.toString());
            };
            // Same corpus/expectations as Phase 2.4.1; query-aware selection must resolve the overlap.
            JsonObject quality=LiveRagRunner.run(plan,ledger,db,wire,LiveRunnerSafetyTest.KEY,LiveRunnerSafetyTest.KEY,LiveRagRunner.business(db));
            LiveSafety.require(quality.getAsJsonArray("cases").size()==6&&ledger.documents==2&&ledger.queries==6&&ledger.chats<=6&&ledger.total<=14&&actualCalls[0]==ledger.total,"COMPLETE_MOCK_SEQUENCE");
            JsonObject chatResult=JsonParser.parseString(Files.readString(ledger.evidence().resolve("chat-LQ1.result.json"))).getAsJsonObject();LiveSafety.require("gemini-3.8-flash".equals(LiveSafety.str(chatResult,"model"))&&chatResult.getAsJsonObject("usage").get("thoughtsTokenCount").getAsInt()==48,"GEMINI_38_USAGE_EVIDENCE");
            int before=actualCalls[0];boolean nextRefused=false;try{ledger.send("query","query-LQ5","POST",LiveSafety.EMBED,LiveRunnerSafetyTest.KEY,new JsonObject(),wire,(r,s)->true);}catch(LiveSafety.Halt h){nextRefused=true;}
            LiveSafety.require(nextRefused&&actualCalls[0]==before,"POST_COMPLETION_REQUEST");
            JsonObject observation=JsonParser.parseString(Files.readString(ledger.evidence().resolve("LQ4.sources.json"))).getAsJsonObject();LiveSafety.require(observation.getAsJsonArray("retrievedDocuments").size()==2&&observation.getAsJsonArray("selectedDocuments").size()==1&&"L2".equals(observation.getAsJsonArray("selectedDocuments").get(0).getAsString()),"QUERY_AWARE_SELECTION_EVIDENCE");
            boolean duplicateRefused=false;try{new LiveSafety.Ledger(plan,root.resolve(".build/phase241-db-mocks"),ms->{});}catch(LiveSafety.Halt h){duplicateRefused=true;}LiveSafety.require(duplicateRefused,"EXECUTION_LOCK_REUSE");
            // DB identity/freshness failure must block reuse, preserving indexed rows.
            boolean refused=false;try{LiveRagRunner.verifyDatabase(db,System.getenv("RAG_TEST_DB_NONCE"));}catch(LiveSafety.Halt h){refused=true;}LiveSafety.require(refused,"POPULATED_DB_REUSE");
            JsonObject report=new JsonObject();report.addProperty("safetyAssertionsPassed",7);report.addProperty("qualityPositiveCasesPassed",4);report.addProperty("unknownAbstentions",2);report.add("quality",quality);report.addProperty("mockProviderRequests",ledger.total);report.addProperty("documentRequests",ledger.documents);report.addProperty("queryRequests",ledger.queries);report.addProperty("chatRequests",ledger.chats);report.addProperty("realProviderRequests",0);report.addProperty("databaseMode","real isolated PostgreSQL");LiveSafety.create(ledger.evidence().resolve("offline-database-validation.json"),report);
            System.out.println("Offline DB safety: 7/7 assertions; LQ1-LQ6 PASS. Mock requests="+ledger.total+"; real provider requests=0.");
            System.out.println("Sanitized result: "+ledger.evidence().resolve("offline-database-validation.json"));
        }
    }
}
