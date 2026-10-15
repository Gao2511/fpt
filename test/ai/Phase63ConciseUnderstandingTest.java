package ai;

import ai.consultation.*;
import ai.dto.*;
import ai.session.ChatSessionData;
import cms.SiteContent;
import dto.PackageDTO;
import java.util.*;

/** Regression coverage for concise, context-aware conversations observed in Phase 6.3. */
public final class Phase63ConciseUnderstandingTest {
    private static int passed;
    private interface Case { void run() throws Exception; }
    private static void test(String name, Case body) throws Exception { body.run(); passed++; System.out.println("PASS " + name); }
    private static void check(boolean value, String detail) { if (!value) throw new AssertionError(detail); }
    private static int count(String text, String needle) { int n=0,p=0; while((p=text.indexOf(needle,p))>=0){n++;p+=needle.length();} return n; }

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

    private static ChatResponse ask(String text, ChatSessionData session, ProductCatalog catalog) throws Exception {
        return new ConsultationEngine().chat(text, session, catalog, null, null, new ChatOptions(.3,1200), ConsultationPrompt.SYSTEM);
    }

    public static void main(String[] args) throws Exception {
        ProductCatalog catalog=catalog();
        test("installation opening is direct and asks one useful question",()->{
            String c=ask("tôi muốn lắp mạng",new ChatSessionData("p63-open"),catalog).getContent();
            check(c.contains("nhà cấp 4")&&count(c,"?")==1&&!c.contains("Tư vấn viên AI")&&!c.contains("Gia Lai"),c);
        });
        test("floor and people are confirmed without losing either fact",()->{
            ChatSessionData s=new ChatSessionData("p63-needs"); ask("tôi muốn lắp mạng",s,catalog);
            String c=ask("nhà 3 tầng, có 5 người đều sử dụng thiết bị điện tử",s,catalog).getContent();
            check(c.contains("3 tầng")&&c.contains("5 người")&&count(c,"?")==1,c);
        });
        test("shorthand camera rejection is remembered",()->{
            ChatSessionData s=new ChatSessionData("p63-negation"); ask("tôi muốn lắp mạng",s,catalog); ask("nhà 3 tầng, có 5 người",s,catalog);
            String c=ask("có tivi thông minh muốn xem bóng đá, k lấy camera",s,catalog).getContent();
            CustomerRequirements m=s.getRequirements();
            check(m.television&&m.football&&m.cameraKnown&&!m.camera,c+"\n"+m.json());
            check(c.contains("xem bóng đá")&&c.contains("không camera")&&count(c,"?")==1,c);
        });
        test("location completes one grounded primary recommendation",()->{
            ChatSessionData s=new ChatSessionData("p63-full"); ask("tôi muốn lắp mạng",s,catalog); ask("nhà 3 tầng, có 5 người",s,catalog); ask("có tivi thông minh muốn xem bóng đá, k lấy camera",s,catalog);
            ChatResponse r=ask("xã Tuy Phước",s,catalog); String c=r.getContent();
            check(r.getRecommendedPackageId()==3&&c.contains("Gói 220K + Mesh WiFi F2")&&c.contains("240.000đ/tháng")&&c.contains("700.000đ/lần"),c);
            check(c.contains("xem bóng đá")&&c.contains("không camera")&&!c.contains("trả trước")&&!c.contains("Gói 230K"),c);
        });
        test("1000Mbps price uses remembered F2 without unrelated promotion",()->{
            ChatSessionData s=new ChatSessionData("p63-gigabit"); ask("nhà 3 tầng, 5 người, xem bóng đá, k lấy camera, xã Tuy Phước",s,catalog);
            String c=ask("1000 Mbps thì giá sao",s,catalog).getContent();
            check(c.startsWith("Dạ Gói 239K")&&c.contains("259.000đ/tháng")&&!c.contains("trả trước")&&!c.contains("249K"),c);
        });
        test("259K registration resolves to 239K plus F2 honestly",()->{
            ChatSessionData s=new ChatSessionData("p63-register"); ask("nhà 3 tầng, 5 người, xem bóng đá, k lấy camera",s,catalog); ask("1000 Mbps thì giá sao",s,catalog);
            ChatResponse r=ask("tôi muốn chốt gói 259k",s,catalog); String c=r.getContent();
            check("open_registration".equals(r.getAction())&&r.getRecommendedPackageId()==5,c);
            check(c.contains("Gói 239K + Mesh F2")&&c.contains("259.000đ/tháng")&&c.contains("chưa gửi")&&!c.contains("1–2 phút"),c);
        });
        test("installation objection answers only the relevant cost",()->{
            ChatSessionData s=new ChatSessionData("p63-fee"); s.getRequirements().meshInterest="F2"; s.getRequirements().floors=3;
            String c=ask("phí lắp 700k cao thế",s,catalog).getContent();
            check(c.contains("tổng phí lắp Mesh F2")&&c.contains("F1 hoặc Internet tiêu chuẩn")&&!c.contains("trả trước")&&!c.contains("TV 180"),c);
        });
        test("competitor comparison remains factual and concise",()->{
            String c=ask("bên Viettel cũng có thiết bị phụ mà chỉ tốn 400k thôi",new ChatSessionData("p63-competitor"),catalog).getContent();
            check(c.contains("thông tin anh/chị cung cấp")&&c.contains("không khẳng định")&&!c.contains("Phúc")&&!c.contains("Ngoại Hạng Anh"),c);
        });
        test("single-storey and ground-floor forms are parsed",()->{
            CustomerRequirements a=new CustomerRequirements(); a.update("nhà cấp 4, có 2 người");
            CustomerRequirements b=new CustomerRequirements(); b.update("nhà 1 trệt 2 lầu, 5 người");
            check(Integer.valueOf(1).equals(a.floors)&&"SINGLE_STOREY".equals(a.dwellingType),a.json().toString());
            check(Integer.valueOf(3).equals(b.floors)&&"GROUND_AND_UPPER_FLOORS".equals(b.dwellingType),b.json().toString());
        });
        test("lost connection gives one safe troubleshooting action",()->{
            String c=ask("mạng nhà tôi bị mất, không vào mạng được",new ChatSessionData("p63-outage"),catalog).getContent();
            check(c.contains("rút nguồn modem khoảng 1 phút")&&c.contains("Nếu vẫn mất kết nối")&&!c.contains("Gói "),c);
        });
        test("generic support wording is not mistaken for a rental home",()->{
            CustomerRequirements m=new CustomerRequirements(); m.update("Bạn hỗ trợ được những gì vậy?");
            check(!m.rentalHome, m.json().toString());
            check(ConversationIntent.classify("Bạn hỗ trợ được những gì vậy?",m,catalog)!=ConversationIntent.RENTAL_CONSULTATION,"misclassified support question");
        });
        test("explicit name is used but no name is invented",()->{
            CustomerRequirements named=new CustomerRequirements(); named.update("Tôi tên là Phúc.");
            CustomerRequirements unnamed=new CustomerRequirements(); unnamed.update("Tôi ở xã Tuy Phước.");
            check(ConversationSignals.address(named).contains("Phúc"),ConversationSignals.address(named));
            check(!ConversationSignals.address(unnamed).contains("Phúc"),ConversationSignals.address(unnamed));
        });
        test("prompt protocol enforces concise non-pushy behavior",()->{
            String p=ConsultationPrompt.instructions(ConsultationPrompt.SYSTEM);
            check(p.contains("Không bắt buộc mỗi câu trả lời phải có câu hỏi")&&p.contains("Không tự động giới thiệu ưu đãi trả trước")&&p.contains("không suy đoán tuổi hoặc tên khách"),p);
        });
        System.out.println("Phase63ConciseUnderstandingTest: "+passed+" passed; external API calls=0; database writes=0");
    }
}
