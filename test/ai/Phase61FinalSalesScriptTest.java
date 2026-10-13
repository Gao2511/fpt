package ai;

import ai.consultation.*;
import ai.dto.*;
import ai.session.ChatSessionData;
import cms.SiteContent;
import dto.PackageDTO;
import java.nio.file.Paths;
import java.util.*;
import rag.files.FileKnowledgeCatalog;

/** Focused offline checks for the final sales-script integration. */
public final class Phase61FinalSalesScriptTest {
    private static int passed;
    private interface Case { void run() throws Exception; }
    private static void test(String name, Case body) throws Exception { body.run(); passed++; System.out.println("PASS " + name); }
    private static void check(boolean value, String detail) { if (!value) throw new AssertionError(detail); }
    private static ProductCatalog catalog() {
        int[] prices={195,205,220,230,239,249}, speeds={300,300,300,300,1000,1000};
        List<PackageDTO> rows=new ArrayList<>();
        for(int i=0;i<prices.length;i++){
            PackageDTO p=new PackageDTO();p.setId(i+1);p.setName("Gói "+prices[i]+"K");p.setPackageCode(prices[i]+"K");
            p.setPrice(prices[i]*1000L);p.setSpeedMbps(speeds[i]);p.setStandardInstallationFee(300000L);p.setActive(true);
            p.setDescription("Modem WiFi 6"+(i==1||i==3||i==5?", 1 Camera":"")+(i>=2?", TV 180 kênh, nội dung Ngoại Hạng Anh":""));rows.add(p);
        }
        return new ProductCatalog(rows, SiteContent.defaults());
    }
    private static ChatResponse ask(String text, ChatSessionData session, ProductCatalog catalog)throws Exception{
        return new ConsultationEngine().chat(text,session,catalog,null,null,new ChatOptions(.3,1200),ConsultationPrompt.SYSTEM);
    }
    public static void main(String[] args)throws Exception{
        ProductCatalog catalog=catalog();
        test("verified public business defaults",()->{
            Map<String,String> b=SiteContent.defaults();
            check("Tư vấn viên AI của Phúc".equals(b.get("business.assistantName")),"assistant");
            check("https://fpt.vn/sale/phucbn2-18".equals(b.get("business.registrationUrl")),"registration url");
            check("07:00–22:00".equals(b.get("business.humanHours"))&&"24/24".equals(b.get("business.chatbotHours")),"hours");
        });
        test("confirmed F2 six-month promotion",()->{
            ChatSessionData s=new ChatSessionData("p61-f2-6");
            String c=ask("Đóng trước 6 tháng có giảm phí lắp F2 không?",s,catalog).getContent();
            check(c.contains("700.000đ")&&c.contains("500.000đ/lần")&&c.contains("13/10/2026")&&!c.contains("chưa được xác nhận"),c);
        });
        test("confirmed F1 twelve-month promotion",()->{
            ChatSessionData s=new ChatSessionData("p61-f1-12");
            String c=ask("Trả trước 12 tháng cho F1 thì sao?",s,catalog).getContent();
            check(c.contains("500.000đ")&&c.contains("300.000đ/lần")&&c.contains("tặng 1 tháng"),c);
        });
        test("standard six-month installation total",()->{
            ChatSessionData s=new ChatSessionData("p61-standard-6");
            String c=ask("Đóng trước 6 tháng thì phí lắp Internet còn bao nhiêu?",s,catalog).getContent();
            check(c.contains("300.000đ")&&c.contains("100.000đ/lần"),c);
        });
        test("ambiguous 230K is not silently converted to combo",()->{
            ChatSessionData s=new ChatSessionData("p61-ambiguous");s.getRequirements().meshInterest="F1";
            ChatResponse r=ask("Tôi muốn chốt gói 230K.",s,catalog);
            check("none".equals(r.getAction())&&r.getContent().contains("hai phương án")&&r.getContent().contains("gói độc lập")&&r.getContent().contains("bản Mesh"),r.getContent());
        });
        test("selected 220K plus F1 remains transparent",()->{
            ChatSessionData s=new ChatSessionData("p61-combo");s.getRequirements().selectedPackageId=3;s.getRequirements().meshInterest="F1";
            ChatResponse r=ask("Tôi muốn chốt gói 230K.",s,catalog);String c=r.getContent();
            check("open_registration".equals(r.getAction())&&r.getRecommendedPackageId()==3,c);
            check(c.contains("Gói nền: Gói 220K")&&c.contains("220.000đ + 10.000đ = 230.000đ/tháng")&&c.contains("không gồm camera"),c);
            check(c.contains("https://fpt.vn/sale/phucbn2-18")&&c.contains("0932 079 469")&&c.contains("chưa chuyển thông tin")&&!c.contains("đã chuyển"),c);
        });
        test("standalone 230K keeps camera identity",()->{
            ChatResponse r=ask("Tôi muốn đăng ký gói 230K.",new ChatSessionData("p61-standalone"),catalog);String c=r.getContent();
            check("open_registration".equals(r.getAction())&&r.getRecommendedPackageId()==4,c);
            check(c.contains("Gói nền: Gói 230K")&&c.contains("1 Camera")&&!c.contains("Mesh F1"),c);
        });
        test("human handoff remains honest",()->{
            String c=ask("Tôi muốn gặp tư vấn viên.",new ChatSessionData("p61-handoff"),catalog).getContent();
            check(c.contains("24/24")&&c.contains("07:00–22:00")&&c.contains("chưa có cơ chế tự động chuyển")&&!c.contains("đã ghi nhận"),c);
        });
        test("prepayment context changes selected solution only once",()->{
            ChatSessionData s=new ChatSessionData("p61-context");s.getRequirements().selectedPackageId=5;s.getRequirements().meshInterest="F2";
            ask("Tôi chọn đóng trước 12 tháng.",s,catalog);
            ChatResponse r=ask("Anh chốt gói này.",s,catalog);String c=r.getContent();
            check(c.contains("239.000đ + 20.000đ = 259.000đ/tháng")&&c.contains("sau ưu đãi trả trước còn 500.000đ/lần"),c);
        });
        test("markdown catalog remains valid",()->check(FileKnowledgeCatalog.load(Paths.get("knowledge")).files.size()>=18,"catalog not loaded"));
        System.out.println("Phase61FinalSalesScriptTest: "+passed+" passed; external API calls=0; database writes=0");
    }
}
