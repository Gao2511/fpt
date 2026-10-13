package ai;

import ai.consultation.ConsultationEngine;
import ai.consultation.ConsultationPrompt;
import ai.consultation.ProductCatalog;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.session.ChatSessionData;
import cms.SiteContent;
import dto.PackageDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Focused Phase 5.9 offline behavior tests. */
public final class CombinedSolutionRecommendationTest {
    private static int passed;
    private static final ProductCatalog CATALOG = catalog(true);

    private interface Case { void run() throws Exception; }
    private static void test(String name, Case body) throws Exception {
        body.run(); passed++; System.out.println("PASS " + name);
    }
    private static void check(boolean value, String detail) {
        if (!value) throw new AssertionError(detail);
    }
    private static ChatResponse ask(String text, ChatSessionData session, ProductCatalog catalog) throws Exception {
        return new ConsultationEngine().chat(text, session, catalog, null, null,
            new ChatOptions(0.3, 1200), ConsultationPrompt.SYSTEM);
    }
    private static ProductCatalog catalog(boolean completeMeshPrice) {
        int[] prices = {195,205,220,230,239,249};
        int[] speeds = {300,300,500,500,1000,1000};
        List<PackageDTO> rows = new ArrayList<>();
        for (int i=0;i<prices.length;i++) {
            PackageDTO p = new PackageDTO();
            p.setId(i+1); p.setName("Gói " + prices[i] + "K"); p.setPackageCode(prices[i] + "K");
            p.setPrice(prices[i] * 1000L); p.setSpeedMbps(speeds[i]); p.setActive(true);
            p.setStandardInstallationFee(300000L);
            p.setDescription("Modem WiFi 6" + (i==1 || i==3 || i==5 ? ", 1 Camera" : "")
                + (i>=2 ? ", TV 180 kênh, nội dung Ngoại Hạng Anh" : ""));
            rows.add(p);
        }
        Map<String,String> business = SiteContent.defaults();
        // Non-default prices prove that the recommendation reads approved configuration.
        business.put("business.meshF1Monthly", "17000");
        business.put("business.meshF1Fee", "610000");
        business.put("business.meshF2Monthly", "29000");
        business.put("business.meshF2Fee", "830000");
        if (!completeMeshPrice) business.remove("business.meshF2Monthly");
        return new ProductCatalog(rows, business);
    }
    private static int occurrences(String text, String needle) {
        int count=0, at=0;
        while ((at=text.indexOf(needle,at))>=0) { count++; at+=needle.length(); }
        return count;
    }
    private static void onePrimary(ChatResponse response) {
        check(response.getRecommendedPackageId()!=null, response.getContent());
        check(occurrences(response.getContent(), "Gói ")==1, "Package spam/duplicate: " + response.getContent());
    }

    public static void main(String[] args) throws Exception {
        test("case 1: three floors gets a preliminary combined solution", () -> {
            ChatResponse r=ask("Nhà anh 3 tầng.",new ChatSessionData("phase59-c1"),CATALOG);
            onePrimary(r); check(r.getRecommendedPackageId()==1 && r.getContent().contains("Mesh WiFi F2")
                && r.getContent().contains("224.000đ/tháng") && r.getContent().contains("số người"),r.getContent());
        });
        test("case 2: three floors and eight people gets one capacity plus coverage solution", () -> {
            ChatResponse r=ask("Nhà anh 3 tầng, 8 người.",new ChatSessionData("phase59-c2"),CATALOG);
            onePrimary(r); check(r.getRecommendedPackageId()==5 && r.getContent().contains("239.000đ/tháng — 1000Mbps")
                && r.getContent().contains("268.000đ/tháng") && !r.getContent().contains("220.000đ/tháng"),r.getContent());
        });
        test("case 3: two floors, five people and streaming gets one primary solution", () -> {
            ChatResponse r=ask("Nhà anh 2 tầng, 5 người, xem phim nhiều.",new ChatSessionData("phase59-c3"),CATALOG);
            onePrimary(r); check(r.getRecommendedPackageId()==3 && r.getContent().contains("Mesh WiFi F1")
                && r.getContent().contains("237.000đ/tháng"),r.getContent());
        });
        test("case 4: context evolves to Internet plus Mesh plus five-camera guidance", () -> {
            ChatSessionData s=new ChatSessionData("phase59-c4");
            ask("Nhà anh 3 tầng.",s,CATALOG); ask("Khoảng 8 người.",s,CATALOG);
            ChatResponse r=ask("5 camera.",s,CATALOG);
            onePrimary(r); check(r.getRecommendedPackageId()==6 && r.getContent().contains("278.000đ/tháng")
                && r.getContent().contains("5 camera") && r.getContent().contains("chỉ xác nhận kèm 1 Camera"),r.getContent());
        });
        test("case 5: gaming guidance avoids an absolute latency promise", () -> {
            ChatResponse r=ask("Anh chơi Valorant, nhà 3 tầng.",new ChatSessionData("phase59-c5"),CATALOG);
            onePrimary(r); String c=r.getContent();
            check(r.getRecommendedPackageId()==3 && c.contains("249.000đ/tháng") && c.contains("LAN")
                && c.contains("không bảo đảm ping") && !c.contains("không lag"),c);
        });
        test("case 6: explicit Internet-only preference is respected", () -> {
            ChatResponse r=ask("Anh chỉ muốn gói Internet, không cần Mesh.",new ChatSessionData("phase59-c6"),CATALOG);
            onePrimary(r); check(r.getRecommendedPackageId()==1 && !r.getContent().contains("Mesh WiFi")
                && r.getContent().contains("195.000đ/tháng"),r.getContent());
        });
        test("monthly and one-time totals use separate approved data", () -> {
            ChatResponse r=ask("Nhà anh 3 tầng.",new ChatSessionData("phase59-prices"),CATALOG);
            String c=r.getContent();
            check(c.contains("195.000đ/tháng") && c.contains("+29.000đ/tháng") && c.contains("224.000đ/tháng"),c);
            check(c.contains("300.000đ/lần") && c.contains("830.000đ/lần") && c.contains("1.130.000đ"),c);
            check(!c.contains("215.000đ/tháng") && !c.contains("700.000đ/lần"),"Hardcoded price leaked: "+c);
        });
        test("unknown Mesh price is not invented or aggregated", () -> {
            ChatResponse r=ask("Nhà anh 3 tầng.",new ChatSessionData("phase59-unknown"),catalog(false));
            String c=r.getContent();
            check(c.contains("giá hiện chưa có dữ liệu được xác nhận") && c.contains("chưa thể cộng chính xác")
                && !c.contains("224.000đ/tháng") && !c.contains("215.000đ/tháng"),c);
        });
        test("remembered floors do not hijack an unrelated social turn", () -> {
            ChatSessionData s=new ChatSessionData("phase59-social"); ask("Nhà anh 3 tầng.",s,CATALOG);
            ChatResponse r=ask("Cảm ơn em.",s,CATALOG);
            check(!r.getContent().contains("Tổng hàng tháng") && r.getContent().contains("vẫn ở đây"),r.getContent());
        });
        test("coverage request without floor count asks one necessary question", () -> {
            ChatResponse r=ask("Nhà nhiều phòng và WiFi tầng trên yếu.",new ChatSessionData("phase59-missing-floor"),CATALOG);
            check(r.getRecommendedPackageId()==null && r.getContent().contains("bao nhiêu tầng")
                && !r.getContent().contains("Tổng hàng tháng"),r.getContent());
        });
        System.out.println("CombinedSolutionRecommendationTest: " + passed + " passed");
    }
}
