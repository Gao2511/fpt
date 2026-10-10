package ai;

import ai.consultation.*;
import ai.dto.*;
import ai.exception.AIException;
import ai.session.*;
import java.util.*;
import static ai.ConsultationBehaviorTest.*;

/** V4 scenarios exercise the real engine and validator with deterministic providers. */
public class AdaptiveConversationTest {
    static int passed;
    interface Case { void run() throws Exception; }
    static void test(String name, Case scenario) throws Exception { scenario.run(); passed++; System.out.println("PASS " + name); }
    static ChatResponse custom(String q, ChatSessionData s, MockProvider p) throws Exception {
        return new ConsultationEngine().chat(q,s,CATALOG,new ConsultationEngine.Target(p,"fake-test-key","mock-model",""),null,new ChatOptions(0.3,1200),"Trả lời tự nhiên, lịch sự.");
    }
    public static void main(String[] args) throws Exception {
        test("casual and attention greetings are available without provider calls",()->{
            for(String greeting:Arrays.asList("Hi","Hi a","Hello shop","Alo","Ê shop","Shop ơi","Có ai ở đây không?","Hí","Chào em","Good morning")) {
                MockProvider p=new MockProvider();ChatResponse r=ask(greeting,new ChatSessionData(greeting),p,null);
                check(p.calls==0 && r.getContent().contains("em đây") && !r.getContent().contains("0932") && !r.getContent().contains("chỉ có thể"),r.getContent());
            }
        });
        test("casual budget query uses friendly speech and verified cheapest product",()->{
            ChatSessionData s=new ChatSessionData("casual");ChatResponse r=ask("Shop ơi có gói nào ngon bổ rẻ hong?",s,new MockProvider(),null);
            check("casual".equals(s.getRequirements().style) && r.getContent().contains("nhé") && r.getRecommendedPackageId()==1 && r.getContent().contains("300Mbps"),r.getContent());
        });
        test("playful gaming gets humor without a performance guarantee",()->{
            ChatResponse r=ask("Lắp xong em thành pro player không?",new ChatSessionData("joke"),new MockProvider(),null);
            check(r.getContent().contains("Haha") && r.getContent().contains("ping") && !r.getContent().contains("chỉ có thể") && "none".equals(r.getAction()),r.getContent());
        });
        test("formal consultation and switching away from slang follow current language",()->{
            ChatSessionData s=new ChatSessionData("formal");ask("Hi a",s,new MockProvider(),null);
            ChatResponse r=ask("Tôi muốn tìm hiểu dịch vụ Internet cho gia đình.",s,new MockProvider(),null);
            check("formal".equals(s.getRequirements().style) && r.getContent().contains("195.000") && !r.getContent().contains("nhé!"),r.getContent());
        });
        test("direct price and abbreviated comparison avoid another sales question",()->{
            ChatResponse r=ask("Giá 220K?",new ChatSessionData("price"),new MockProvider(),null);
            check(r.getContent().equals(CATALOG.byId(3).display()),r.getContent());
            r=ask("So sánh 195 với 239.",new ChatSessionData("short-compare"),new MockProvider(),null);
            check(r.getContent().contains("195K") && r.getContent().contains("239K") && !r.getContent().contains("205K"),r.getContent());
        });
        test("feature follow-up combines household devices and both amenities",()->{
            ChatSessionData s=new ChatSessionData("features");ask("Nhà 4 người dùng khoảng 5 thiết bị.",s,new MockProvider(),null);
            ChatResponse r=ask("Có truyền hình và camera.",s,new MockProvider(),null);
            check(s.getRequirements().people==4 && s.getRequirements().devices==5 && r.getRecommendedPackageId()==4 && r.getContent().contains("230.000") && r.getContent().contains("500Mbps") && !r.getContent().contains("có cần thêm"),r.getContent());
            r=ask("Không cần camera nữa.",s,new MockProvider(),null);
            check(!s.getRequirements().camera && s.getRequirements().television && r.getRecommendedPackageId()==3,r.getContent());
        });
        test("single person, number words, device ranges and short replies update memory",()->{
            ChatSessionData s=new ChatSessionData("counts");ask("Em đang ở một mình.",s,new MockProvider(),null);ask("2 máy.",s,new MockProvider(),null);
            check(s.getRequirements().people==1 && s.getRequirements().devices==2,"Short answer lost");
            ask("Gia đình tôi có bốn người, 4–5 thiết bị.",s,new MockProvider(),null);
            check(s.getRequirements().people==4 && s.getRequirements().devices==5,"Word/range lost");
        });
        test("budget increase retains context and lists newly affordable packages",()->{
            ChatSessionData s=new ChatSessionData("budget-update");ask("Ngân sách 200 nghìn.",s,new MockProvider(),null);
            ChatResponse r=ask("Nếu tăng lên 250 nghìn thì sao?",s,new MockProvider(),null);
            check(s.getRequirements().budget==250000L && r.getContent().contains("205K") && r.getContent().contains("249K") && !r.getContent().contains("cho em biết ngân sách"),r.getContent());
        });
        test("availability question does not silently establish camera purchase preference",()->{
            ChatSessionData s=new ChatSessionData("availability");ask("Giá 195K?",s,new MockProvider(),null);
            ChatResponse r=ask("Có camera không?",s,new MockProvider(),null);
            check(!s.getRequirements().camera && !s.getRequirements().cameraKnown && r.getContent().contains("chưa có camera") && r.getContent().contains("195K"),r.getContent());
        });
        test("gaming follow-up keeps the selected package and states limitations",()->{
            ChatSessionData s=new ChatSessionData("gaming-follow");ask("Giá 220K?",s,new MockProvider(),null);
            ChatResponse r=ask("Mạng này chiến game ổn áp ko?",s,new MockProvider(),null);
            check(r.getRecommendedPackageId()==3 && r.getContent().contains("LAN") && r.getContent().contains("không bảo đảm"),r.getContent());
        });
        test("implicit closing opens the selected form; negation and unknown packages never submit",()->{
            ChatSessionData s=new ChatSessionData("close");ask("Giá 249K?",s,new MockProvider(),null);
            ChatResponse r=ask("Oke chốt luôn",s,new MockProvider(),null);
            check("open_registration".equals(r.getAction()) && r.getRecommendedPackageId()==6 && s.getRequirements().registrationFormOpened && !s.isLeadCreated(),r.getContent());
            check("none".equals(ask("Tôi không muốn đăng ký gói 249K.",s,new MockProvider(),null).getAction()),"Negated consent");
            check("none".equals(ask("Đăng ký 999K.",s,new MockProvider(),null).getAction()),"Unknown selection");
            r=ask("Đăng ký 249K.",new ChatSessionData("direct-register"),new MockProvider(),null);
            check("open_registration".equals(r.getAction()) && r.getRecommendedPackageId()==6,r.getContent());
        });
        test("ambiguous reference asks one clarification and does not invent a package",()->{
            ChatResponse r=ask("Cho cái kia.",new ChatSessionData("ambiguous"),new MockProvider(),null);
            check(r.getContent().contains("gói nào") && r.getRecommendedPackageId()==null && "none".equals(r.getAction()),r.getContent());
        });
        test("closing after multiple options clarifies instead of reusing a stale selection",()->{
            ChatSessionData s=new ChatSessionData("compare-close");ask("Giá 195K?",s,new MockProvider(),null);
            ask("So sánh 195 với 239.",s,new MockProvider(),null);
            ChatResponse r=ask("Oke chốt luôn",s,new MockProvider(),null);
            check("none".equals(r.getAction()) && r.getContent().contains("gói nào"),r.getContent());
            r=ask("Đăng ký 239K.",s,new MockProvider(),null);check("open_registration".equals(r.getAction()) && r.getRecommendedPackageId()==5,r.getContent());
        });
        test("speed answer uses catalog rather than an embedded product override",()->{
            dto.PackageDTO row=new dto.PackageDTO();row.setId(50);row.setPrice(239000);row.setSpeedMbps(777);row.setDescription("Modem WiFi 6");
            ChatResponse r=new ConsultationEngine().chat("Gói 239K tốc độ bao nhiêu?",new ChatSessionData("authoritative-speed"),new ProductCatalog(Arrays.asList(row)),null,null,new ChatOptions(0.3,1200));
            check(r.getContent().contains("777 Mbps") && !r.getContent().contains("1000") && !r.getContent().contains("180"),r.getContent());
        });
        test("human and missing data handoffs never claim a callback was sent",()->{
            ChatResponse r=ask("Tôi muốn nói chuyện với người thật.",new ChatSessionData("human"),new MockProvider(),null);
            check(r.getContent().contains("0932 079 469") && r.getContent().contains("chưa gửi") && "none".equals(r.getAction()),r.getContent());
            r=ask("Giá lắp mạng bao nhiêu?",new ChatSessionData("missing-fee"),new MockProvider(),null);
            check(r.getContent().contains("chưa có dữ liệu") && !r.getContent().contains("miễn phí"),r.getContent());
        });
        test("substantive unrelated request remains distinct from social conversation",()->{
            ChatSessionData s=new ChatSessionData("scope");ask("Nhà 4 người.",s,new MockProvider(),null);
            ChatResponse r=ask("Viết cho tôi code Java.",s,new MockProvider(),null);
            check(r.getContent().contains("chưa thể làm") && !r.getContent().contains("0932") && r.getRecommendedPackageId()==null,r.getContent());
            r=ask("Haha đùa thôi",s,new MockProvider(),null);check(r.getContent().contains("đùa") && !r.getContent().contains("chưa thể làm"),r.getContent());
        });
        test("explicit communication preference and address survive unrelated turns",()->{
            ChatSessionData s=new ChatSessionData("preference");ask("Gọi mình là bạn. Trả lời ngắn gọn.",s,new MockProvider(),null);
            ask("Nhà tôi có bốn người.",s,new MockProvider(),null);
            check("bạn".equals(s.getRequirements().address) && "direct".equals(s.getRequirements().style),"Preference lost");
            ChatResponse r=ask("Chào em",s,new MockProvider(),null);check(r.getContent().contains("bạn cần") && !r.getContent().contains("anh/chị"),r.getContent());
        });
        test("custom prompt incorrect scope is repaired without rejecting valid follow-up",()->{
            ChatSessionData s=new ChatSessionData("custom-scope");ask("Nhà 4 người, 5 thiết bị.",s,new MockProvider(),null);
            MockProvider p=new MockProvider();p.outputs.add(raw(envelope("Em chưa thể hỗ trợ yêu cầu này.").getContent().replace("consultation","out_of_scope"),"stop"));
            p.outputs.add(raw(envelope("Em gợi ý lựa chọn đáp ứng cả hai tiện ích mình cần.").getContent().replace("\"recommendedPackageId\":null","\"recommendedPackageId\":4"),"stop"));
            ChatResponse r=custom("Có truyền hình và camera.",s,p);
            check(p.calls==2 && r.getRecommendedPackageId()==4 && r.getContent().contains("230.000") && !r.getContent().contains("chưa thể hỗ trợ"),r.getContent());
        });
        test("benefit questions validate but unverified assertions and metadata do not",()->{
            ConsultationReply.parse(envelope("Anh/chị có cần thêm camera hoặc truyền hình không?"),CATALOG,"");
            for(String message:Arrays.asList("Gói này có camera.","State Tracking: No.","Đăng ký thành công.")) {
                boolean rejected=false;try{ConsultationReply.parse(envelope(message),CATALOG,"");}catch(Exception expected){rejected=true;}
                check(rejected,"Unsafe assertion accepted: "+message);
            }
        });
        test("provider and malformed failures give technical contact rather than scope refusal",()->{
            for(boolean malformed:Arrays.asList(false,true)) {
                MockProvider p=new MockProvider();if(malformed){p.outputs.add(raw("broken","stop"));p.outputs.add(raw("","length"));}
                else p.outputs.add(new AIException(AIException.ErrorType.TIMEOUT,"offline","Mock"));
                ChatResponse r=ask("Bạn hỗ trợ được những gì vậy?",new ChatSessionData("fail-"+malformed),p,null);
                check(r.getContent().contains("trục trặc") && r.getContent().contains("0932 079 469") && !r.getContent().contains("ngoài phạm vi") && p.calls==(malformed?2:1),r.getContent());
            }
        });
        test("typed device and preference memory survives trim and stays session private",()->{
            ChatSessionData a=new ChatSessionData("extended-memory"),b=new ChatSessionData("other");MockProvider p=new MockProvider();
            ask("Nhà bốn người, 5 thiết bị. Có camera và truyền hình.",a,p,null);
            for(int i=0;i<15;i++)ask("Bạn hỗ trợ được những gì vậy?",a,p,null);
            check(a.getHistory().size()==20 && a.getRequirements().devices==5 && a.getRequirements().camera && a.getRequirements().television,"Lost typed memory");
            check(p.lastMessages.get(p.lastMessages.size()-1).getContent().contains("\"devices\":5"),"Device count not sent");
            ask("Hi a",b,new MockProvider(),null);check(b.getRequirements().devices==null && !b.getRequirements().camera,"Cross-session leak");
            a.clearHistory();check(a.getRequirements().devices==null && a.getRequirements().stylePreference==null && !a.getRequirements().registrationFormOpened,"Reset incomplete");
        });
        System.out.println("AdaptiveConversationTest: "+passed+" passed");
    }
}
