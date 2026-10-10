package ai;

import ai.consultation.*;
import ai.dto.*;
import ai.session.ChatSessionData;
import dto.PackageDTO;
import static ai.ConsultationBehaviorTest.*;

/** Actual engine responses and catalog facts, with no live provider/database. */
public class InstallationFeeTest {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        for (ProductCatalog.Product plan : CATALOG.all()) {
            long price = plan.price; int speed = plan.speed;
            check(plan.standardInstallationFee == PackageDTO.STANDARD_INSTALLATION_FEE, "Missing shared fee");
            ChatResponse r = ask("Gói " + plan.label() + " phí lắp bao nhiêu?", new ChatSessionData("fee-"+plan.id), new MockProvider(), null);
            check(r.getContent().contains("300.000đ/lần") && r.getContent().contains(String.format(java.util.Locale.US,"%,d",price).replace(',','.')+"đ/tháng"), r.getContent());
            check(plan.price == price && plan.speed == speed && "none".equals(r.getAction()), "Monthly data/action changed");
            check(CATALOG.json().get(plan.id-1).getAsJsonObject().get("standardInstallationFeeVnd").getAsLong()==300000, "Provider data missing fee");
            passed++;
        }
        for (String q : new String[]{"Lắp WiFi FPT mất bao nhiêu tiền?", "Phí lắp mạng bao nhiêu?", "Gói 195K có mất phí lắp không?", "Giá lắp mạng bao nhiêu?"}) {
            MockProvider p = new MockProvider();
            ChatResponse r = new ConsultationEngine().chat(q,new ChatSessionData(q),CATALOG,new ConsultationEngine.Target(p,"fake","mock",""),null,new ChatOptions(0.3,1200),"Prompt tùy chỉnh.");
            check(r.getContent().contains("300.000đ/lần") && p.calls==0,r.getContent()); passed++;
        }
        ChatSessionData s = new ChatSessionData("fee-memory");
        ask("Tôi muốn gói 220K",s,new MockProvider(),null);
        ChatResponse r = ask("Phí lắp đặt bao nhiêu?",s,new MockProvider(),null);
        r = ask("300K là đóng mỗi tháng hả?",s,new MockProvider(),null);
        check(r.getContent().contains("một lần") && r.getContent().contains("không phải cước hàng tháng"),r.getContent());
        check(s.getRequirements().selectedPackageId==3,"Fee erased selection"); passed++;
        r=ask("Phí lắp đặt có được miễn phí theo ưu đãi không?",s,new MockProvider(),null);
        check(r.getContent().contains("300.000") && r.getContent().contains("chưa có dữ liệu xác nhận miễn giảm"),r.getContent()); passed++;
        r=ask("Phí lắp Mesh F1 bao nhiêu?",s,new MockProvider(),null);
        check(r.getContent().contains("500.000") && !r.getContent().contains("300.000"),r.getContent()); passed++;
        r=ask("Phí lắp Mesh F2 bao nhiêu?",s,new MockProvider(),null);
        check(r.getContent().contains("700.000") && !r.getContent().contains("300.000"),r.getContent()); passed++;
        PackageDTO other = new PackageDTO(); other.setPrice(999000); other.setSpeedMbps(1000);
        check(other.getStandardInstallationFee()==0,"Other service inherits fee"); passed++;
        check(ConsultationPolicy.installationFee("Phí lắp camera bao nhiêu?",s.getRequirements(),CATALOG)==null,"Camera inherited Internet fee"); passed++;
        check(ConsultationPolicy.installationFee("Phí lắp truyền hình bao nhiêu?",s.getRequirements(),CATALOG)==null,"TV inherited Internet fee"); passed++;
        System.out.println("Installation fee tests: " + passed + " passed.");
    }
}
