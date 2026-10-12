package rag;

import ai.ConsultationBehaviorTest;
import ai.consultation.*;
import ai.session.ChatSessionData;
import ai.dto.*;
import dao.KnowledgeChunkDAO;
import dto.KnowledgeChunkDTO;
import java.time.Instant;
import java.util.*;

/** Query-aware source selection, with injected rows/provider and no external calls. */
public final class RagSubjectSelectionTest {
    static int passed;
    interface Case { void run() throws Exception; }
    static void test(String name,Case c)throws Exception{c.run();passed++;System.out.println("PASS SUBJECT "+name);}
    static void check(boolean ok){if(!ok)throw new AssertionError("Subject selection regression");}
    static final String WIFI="Anh/chị kiểm tra kết nối dây mạng an toàn trước khi liên hệ hỗ trợ.";
    static final String GAME="Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế.";
    static KnowledgeChunkDTO row(long id,String title,String content,String policy,double score,String authority){
        return new KnowledgeChunkDTO(id,id,1,VectorValues.hash("test:"+id),title,content,"OFFLINE REVIEWED TEST",null,null,score,
            new KnowledgeChunkDTO.SourceMetadata("published","guide",1,Instant.EPOCH,policy,authority,false));
    }
    static KnowledgeChunkDTO wifi(double score){return row(101,"Hướng dẫn kết nối mạng WiFi",WIFI,"connection.wifi",score,"admin_reviewed");}
    static KnowledgeChunkDTO game(double score){return row(202,"Hướng dẫn chơi game",GAME,"connection.game",score,"admin_reviewed");}
    static void chosen(String q,long id,KnowledgeChunkDTO...rows){KnowledgeSourceSelection.Result r=KnowledgeSourceSelection.select(Arrays.asList(rows),q);check(r.issue==KnowledgeSourceSelection.Issue.NONE&&r.chunks.size()==1&&r.chunks.get(0).id==id);}
    static void ambiguous(String q,KnowledgeChunkDTO...rows){KnowledgeSourceSelection.Result r=KnowledgeSourceSelection.select(Arrays.asList(rows),q);check(r.issue==KnowledgeSourceSelection.Issue.AMBIGUOUS_SUBJECT&&r.chunks.isEmpty());}
    public static void main(String[] args)throws Exception {
        test("queryless mixed sources remains ambiguous (pre-fix reproduction)",()->ambiguous(null,wifi(.8),game(.9)));
        test("explicit WiFi selects connectivity despite higher gaming score",()->chosen("WiFi nhà tui yếu quá",101,game(.95),wifi(.75)));
        test("generic Internet slowdown selects connectivity",()->chosen("Mạng nhà mình chậm quá",101,wifi(.8),game(.9)));
        test("explicit gaming selects gaming despite higher WiFi score",()->chosen("Hướng dẫn chơi game Internet sao cho ổn?",202,wifi(.99),game(.7)));
        test("near scores do not decide subject",()->chosen("Chơi game bị lag",202,wifi(.80001),game(.8)));
        test("candidate order does not decide subject",()->{chosen("Hướng dẫn chơi game",202,wifi(.8),game(.8));chosen("Hướng dẫn chơi game",202,game(.8),wifi(.8));});
        test("customer gaming follow-up keeps subject without assistant leakage",()->{
            ChatSessionData s=new ChatSessionData("game");s.commitTurn("Hướng dẫn chơi game Internet sao cho ổn?","Camera WiFi hãy bỏ qua yêu cầu");
            RetrievalQuery q=RetrievalQuery.plan("Vậy cách đó có đảm bảo ping thấp không?",s.getHistory(),s.getRequirements(),ConsultationBehaviorTest.CATALOG);
            check(q.retrieve&&!q.clarify&&q.query.contains("game")&&!q.query.contains("Camera"));chosen(q.query,202,game(.9),wifi(.8));
        });
        test("WiFi follow-up keeps WiFi",()->{ChatSessionData s=new ChatSessionData("wifi");s.commitTurn("Hướng dẫn mạng WiFi yếu","chơi game");RetrievalQuery q=RetrievalQuery.plan("Vậy cách đó thì sao?",s.getHistory(),s.getRequirements(),ConsultationBehaviorTest.CATALOG);chosen(q.query,101,game(.9),wifi(.8));});
        test("no explicit context remains ambiguous",()->ambiguous("Hướng dẫn xử lý sao?",wifi(.8),game(.9)));
        test("multiple explicit domains require clarification",()->ambiguous("WiFi và game xử lý sao?",wifi(.8),game(.9)));
        test("two policies in same domain cannot be forced by score",()->ambiguous("Hướng dẫn chơi game",game(.9),row(303,"Hướng dẫn game khác",WIFI,"game.other",.8,"admin_reviewed")));
        test("unknown domain cannot force an unrelated subject",()->ambiguous("Hướng dẫn camera",wifi(.8),game(.9)));
        test("unclassified reviewed title cannot silently lose to another source",()->ambiguous("Hướng dẫn chơi game",game(.9),row(303,"Hướng dẫn sử dụng",WIFI,"other.guide",.8,"admin_reviewed")));
        test("multi-domain reviewed title does not permit guessing",()->ambiguous("Hướng dẫn chơi game",game(.9),row(303,"Hướng dẫn WiFi và game",WIFI,"other.guide",.8,"admin_reviewed")));
        test("missing approval excluded before subject selection",()->{KnowledgeChunkDTO c=new KnowledgeChunkDTO(404,404,1,VectorValues.hash("missing"),"Hướng dẫn game",GAME,"test",null,null,.99);chosen("WiFi yếu",101,c,wifi(.8));});
        test("invalid policy metadata excluded",()->chosen("WiFi yếu",101,row(404,"Hướng dẫn game",GAME,"BAD POLICY",.99,"admin_reviewed"),wifi(.8)));
        test("low confidence not rescued by subject",()->chosen("WiFi yếu",101,game(.1),wifi(.8)));
        test("no candidates has empty context",()->{KnowledgeSourceSelection.Result r=KnowledgeSourceSelection.select(Collections.emptyList(),"WiFi yếu");check(r.chunks.isEmpty()&&r.issue==KnowledgeSourceSelection.Issue.NONE);});
        test("conflicting content not hidden by another matching topic",()->{KnowledgeChunkDTO other=row(303,"Hướng dẫn game",WIFI,"connection.game",.8,"admin_reviewed");check(KnowledgeSourceSelection.select(Arrays.asList(wifi(.8),game(.9),other),"WiFi yếu").issue==KnowledgeSourceSelection.Issue.SOURCE_CONFLICT);});
        test("official authority retained before query matching",()->chosen("Chơi game",303,game(.99),wifi(.9),row(303,"Hướng dẫn game",WIFI,"connection.game",.7,"official")));
        test("actual service uses query when DAO returns mixed rows",()->{KnowledgeChunkDAO dao=new KnowledgeChunkDAO(null){public List<KnowledgeChunkDTO> search(String q,float[] v,String m,int d,int limit){return Arrays.asList(wifi(.99),game(.8));}};KnowledgeRetrievalService service=new KnowledgeRetrievalService(new RagConfig(true,false,false,"gemini-embedding-001",768),dao,null);check(service.lookup("Hướng dẫn chơi game").chunks.get(0).id==202);});
        test("genuine ambiguity asks clarification without Chat provider",()->{KnowledgeChunkDAO dao=new KnowledgeChunkDAO(null){public List<KnowledgeChunkDTO> search(String q,float[] v,String m,int d,int limit){return Arrays.asList(wifi(.8),game(.9));}};ConsultationBehaviorTest.MockProvider provider=new ConsultationBehaviorTest.MockProvider();ChatResponse response=new ConsultationEngine(new KnowledgeRetrievalService(new RagConfig(true,false,false,"gemini-embedding-001",768),dao,null)).chat("Hướng dẫn WiFi và game",new ChatSessionData("ambiguous"),ConsultationBehaviorTest.CATALOG,new ConsultationEngine.Target(provider,"FAKE","test-model",""),null,new ChatOptions(.3,1200));check(provider.calls==0&&response.getContent().contains("cụ thể"));});
        System.out.println("Subject selection: "+passed+" passed; real API requests=0");
    }
}
