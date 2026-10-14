package ai;

import ai.consultation.*;
import ai.dto.*;
import ai.session.ChatSessionData;
import cms.SiteContent;
import dto.PackageDTO;
import java.nio.file.Paths;
import java.util.*;
import rag.files.FileKnowledgeCatalog;

/** Twelve deterministic Phase 6.2 conversations; all provider and database access stays disabled. */
public final class Phase62ContextAwareConversationTest {
    private static int passed;
    private interface Case { void run() throws Exception; }
    private static void test(String name, Case body) throws Exception { body.run(); passed++; System.out.println("PASS " + name); }
    private static void check(boolean value, String detail) { if (!value) throw new AssertionError(detail); }
    private static ProductCatalog catalog() {
        int[] prices={195,205,220,230,239,249}, speeds={300,300,300,300,1000,1000};
        List<PackageDTO> rows=new ArrayList<>();
        for(int i=0;i<prices.length;i++){
            PackageDTO p=new PackageDTO(); p.setId(i+1); p.setName("Gói "+prices[i]+"K"); p.setPackageCode(prices[i]+"K");
            p.setPrice(prices[i]*1000L); p.setSpeedMbps(speeds[i]); p.setStandardInstallationFee(300000L); p.setActive(true);
            p.setDescription("Modem WiFi 6"+(i==1||i==3||i==5?", 1 Camera":"")+(i>=2?", TV 180 kênh, nội dung Ngoại Hạng Anh":"")); rows.add(p);
        }
        return new ProductCatalog(rows, SiteContent.defaults());
    }
    private static ChatResponse ask(String text, ChatSessionData session, ProductCatalog catalog)throws Exception {
        return new ConsultationEngine().chat(text,session,catalog,null,null,new ChatOptions(.3,1200),ConsultationPrompt.SYSTEM);
    }
    private static int count(String text,String needle){int n=0,p=0;while((p=text.indexOf(needle,p))>=0){n++;p+=needle.length();}return n;}

    public static void main(String[] args)throws Exception {
        ProductCatalog catalog=catalog();
        test("A simple F2 price answers first",()->{
            String c=ask("F2 bao nhiêu?",new ChatSessionData("p62-a"),catalog).getContent();
            check(c.startsWith("Dạ Mesh F2")&&c.contains("20.000đ/tháng")&&c.contains("700.000đ/lần")&&c.contains("không cộng thêm phí"),c);
        });
        test("B multi-floor recommendation asks only useful missing preference",()->{
            ChatResponse r=ask("Nhà 3 tầng, 5 người.",new ChatSessionData("p62-b"),catalog); String c=r.getContent();
            check(c.contains("Mesh WiFi F2")&&c.contains("truyền hình hoặc camera")&&count(c,"?")==1,c);
        });
        test("C full context yields one primary 220K plus F2 solution",()->{
            ChatSessionData s=new ChatSessionData("p62-c");
            ChatResponse r=ask("Nhà 3 tầng, 5 người, xem bóng đá, không camera.",s,catalog); String c=r.getContent();
            check(r.getRecommendedPackageId()==3&&c.contains("Gói 220K + Mesh WiFi F2")&&c.contains("240.000đ/tháng")&&c.contains("700.000đ/lần")&&!c.contains("Gói 230K"),c);
        });
        test("D 1000Mbps uses remembered floors TV and no-camera",()->{
            ChatSessionData s=new ChatSessionData("p62-d");
            ask("Nhà 3 tầng, 5 người, xem bóng đá, không camera.",s,catalog);
            ChatResponse r=ask("1000Mbps thì bao nhiêu?",s,catalog); String c=r.getContent();
            check(r.getRecommendedPackageId()==5&&c.startsWith("Dạ Gói 239K")&&c.contains("259.000đ/tháng")&&!c.contains("249K")&&!c.contains("Camera"),c);
        });
        test("E installation objection explains F2 and alternatives",()->{
            String c=ask("F2 lắp 700K cao quá.",new ChatSessionData("p62-e"),catalog).getContent();
            check(c.contains("tổng phí lắp Mesh F2")&&c.contains("2 Access Point")&&c.contains("không cộng 300.000đ")&&c.contains("F1 hoặc Internet tiêu chuẩn")&&!c.contains("TV 180"),c);
        });
        test("F monthly objection lowers to 220K plus F2",()->{
            ChatSessionData s=new ChatSessionData("p62-f");
            ask("Nhà 3 tầng, 5 người, xem bóng đá, không camera.",s,catalog);
            String c=ask("259K/tháng hơi mắc.",s,catalog).getContent();
            check(c.contains("Gói 220K + Mesh F2 = 240.000đ/tháng")&&c.contains("300Mbps thay vì 1000Mbps")&&c.contains("không camera"),c);
        });
        test("G competitor amount remains customer-supplied",()->{
            String c=ask("Viettel 400K thôi.",new ChatSessionData("p62-g"),catalog).getContent();
            check(c.contains("thông tin anh/chị cung cấp")&&c.contains("so sánh công bằng")&&c.contains("không khẳng định")&&!c.contains("Viettel kém"),c);
        });
        test("H rejected prepayment is remembered and not repeated",()->{
            ChatSessionData s=new ChatSessionData("p62-h");
            ask("Không muốn đóng trước.",s,catalog);
            String c=ask("Nhà 3 tầng, 5 người, xem bóng đá, không camera.",s,catalog).getContent();
            check(s.getRequirements().prepaymentRejected&&!c.contains("trả trước")&&!c.contains("Ưu đãi"),c);
        });
        test("I AI identity has no package advertisement",()->{
            String c=ask("Em là người hay AI?",new ChatSessionData("p62-i"),catalog).getContent();
            check(c.contains("Tư vấn viên AI")&&!c.contains("Gói ")&&!c.contains("Mbps"),c);
        });
        test("J infrastructure remains explicitly unverified",()->{
            String c=ask("Khu vực Tuy Phước lắp được không?",new ChatSessionData("p62-j"),catalog).getContent();
            check(c.contains("chưa có kết quả kiểm tra hạ tầng")&&c.contains("chưa thể xác nhận")&&!c.contains("lắp được ạ"),c);
        });
        test("K registration opens the existing form honestly",()->{
            ChatSessionData s=new ChatSessionData("p62-k"); s.getRequirements().meshInterest="F2"; s.getRequirements().selectedPackageId=5;
            ChatResponse r=ask("Anh chốt gói 239K + F2.",s,catalog); String c=r.getContent();
            check("open_registration".equals(r.getAction())&&r.getRecommendedPackageId()==5&&c.contains("259.000đ/tháng")&&c.contains("https://fpt.vn/sale/phucbn2-18")&&c.contains("chưa gửi"),c);
        });
        test("L human handoff gives verified contact without claiming transfer",()->{
            String c=ask("Cho anh gặp Phúc.",new ChatSessionData("p62-l"),catalog).getContent();
            check(c.contains("0932 079 469")&&c.contains("07:00–22:00")&&c.contains("chưa có cơ chế tự động chuyển")&&!c.contains("đã chuyển"),c);
        });
        test("renderer suppresses model package spam on unrelated intent",()->{
            ConsultationReply model=new ConsultationReply(); model.message="Dạ em là tư vấn viên AI ạ."; model.intent="consultation"; model.action="none"; model.recommendedPackageId=1; model.packageIds.add(2);
            ChatResponse r=ConsultationPolicy.render(model,"Em là người hay AI?",new CustomerRequirements(),catalog);
            check(r.getRecommendedPackageId()==null&&!r.getContent().contains("Gói 195K")&&!r.getContent().contains("Gói 205K"),r.getContent());
        });
        test("intent memory and repository Markdown remain valid",()->{
            CustomerRequirements m=new CustomerRequirements(); m.update("Tôi tên là Minh. Nhà 3 tầng, WiFi tầng trên yếu. Không muốn đóng trước.");
            check("Minh".equals(m.customerName)&&"WIFI_WEAK_COVERAGE".equals(m.coverageIssue)&&m.prepaymentRejected&&"F2".equals(m.meshInterest),m.json().toString());
            check(FileKnowledgeCatalog.load(Paths.get("knowledge")).files.size()>=18,"knowledge parse failed");
        });
        System.out.println("Phase62ContextAwareConversationTest: "+passed+" passed; external API calls=0; database writes=0");
    }
}
