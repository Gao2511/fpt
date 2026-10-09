package ai.consultation;

import ai.dto.ChatResponse;
import java.util.*;

/** Server-owned business decisions and factual rendering also used during outages. */
public final class ConsultationPolicy {
    private ConsultationPolicy() {}
    public static boolean registrationRequested(String text) {
        String q = ProductCatalog.normalize(text);
        return q.matches("(?s).*(?:toi|minh|anh|chi|em)\\s+(?:muon|can|xin|se)\\s+(?:dang ky|mua|lap)(?:\\s|$).*") ||
            q.matches("^(?:cho (?:toi|minh|anh|chi|em) )?dang ky\\s+(?:goi|internet|mang).*" );
    }
    public static ChatResponse known(String question, CustomerRequirements memory, ProductCatalog catalog) {
        String q = ProductCatalog.normalize(question);
        List<ProductCatalog.Product> mentioned = catalog.mentioned(question);
        if (q.matches("(?s).*(phi (?:lap|hoa)|gia lap|lap dat.*bao nhieu|khuyen mai|uu dai|giam gia|hop dong|vung phu|ha tang|bao lau.*lap).*"))
            return reply("Em chưa có dữ liệu xác nhận về phí lắp đặt, ưu đãi, điều khoản hoặc hạ tầng tại địa chỉ của anh/chị. Anh/chị có thể gửi yêu cầu tư vấn qua biểu mẫu để được kiểm tra; em chưa thể báo chi phí hay cam kết lịch lắp đặt.");
        if (q.contains("ban chay") || q.contains("pho bien nhat"))
            return reply("Em chưa có số liệu bán hàng để xác định gói bán chạy nhất. Anh/chị cho em biết ngân sách, số người sử dụng và nhu cầu chính để em chọn gói phù hợp nhé.");
        if (q.contains("so sanh") || mentioned.size() >= 2 || q.contains("khac nhau") || q.contains("tat ca") || q.contains("6 goi")) {
            List<ProductCatalog.Product> compare = mentioned.size() >= 2 && !q.contains("tat ca") && !q.contains("6 goi") ? mentioned : catalog.all();
            if (compare.isEmpty()) return unavailable();
            StringBuilder text = new StringBuilder("Em gửi anh/chị thông tin các gói để so sánh:\n");
            for (ProductCatalog.Product p : compare) text.append(p.display()).append('\n');
            if (compare.size() == 2) {
                ProductCatalog.Product a = compare.get(0), b = compare.get(1);
                text.append("Chênh lệch giá: ").append(String.format(Locale.US, "%,d", Math.abs(a.price - b.price)).replace(',', '.')).append("đ/tháng; tốc độ lần lượt ").append(a.speed).append("Mbps và ").append(b.speed).append("Mbps.");
            }
            return reply(text.toString().trim());
        }
        if (registrationRequested(question)) {
            boolean explicitPackage = q.matches("(?s).*goi\\s+[0-9]+.*");
            ProductCatalog.Product p = mentioned.size() == 1 ? mentioned.get(0) : explicitPackage || mentioned.size() > 1 || memory.selectedPackageId == null ? null : catalog.byId(memory.selectedPackageId);
            if (p == null) return reply("Anh/chị muốn đăng ký gói nào? Em sẽ mở biểu mẫu của gói đã chọn; chỉ khi anh/chị kiểm tra và bấm gửi thì yêu cầu mới được tiếp nhận.");
            memory.selectedPackageId = p.id;
            ChatResponse r = reply("Anh/chị muốn đăng ký " + p.display() + "\nAnh/chị vui lòng mở biểu mẫu bên dưới, kiểm tra thông tin và bấm gửi yêu cầu tư vấn. Em chưa gửi đăng ký hoặc xác nhận lắp đặt.");
            r.setAction("open_registration"); r.setRecommendedPackageId(p.id); return r;
        }
        if (mentioned.size() == 1) {
            ProductCatalog.Product p = mentioned.get(0); memory.selectedPackageId = p.id;
            ChatResponse r = reply(p.display() + "\nAnh/chị cho em biết số người sử dụng và nhu cầu chính để em tư vấn thêm nhé.");
            r.setRecommendedPackageId(p.id); return r;
        }
        if (q.matches("(?s).*(sinh vien|ngan sach|toi co|minh co|nguoi|game|phong tro|o tro|goi nao|tu van|chon goi).*")) {
            ProductCatalog.Product best = choose(memory, catalog);
            if (best == null) return catalog.all().isEmpty() ? unavailable() : reply("Em chưa tìm thấy gói phù hợp với ngân sách và quyền lợi anh/chị cần trong danh mục hiện có. Anh/chị có thể điều chỉnh ngân sách hoặc nhu cầu để em so sánh thêm nhé.");
            memory.selectedPackageId = best.id;
            String text = "Em đề xuất gói có chi phí thấp nhất phù hợp với ngân sách và quyền lợi đã nêu: " + best.display();
            if (memory.people != null) text += "\nEm đã ghi nhận " + memory.people + " người sử dụng.";
            if (memory.gaming) text += "\nVới nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế.";
            text += "\nAnh/chị có cần thêm truyền hình hoặc camera không?";
            ChatResponse r = reply(text); r.setRecommendedPackageId(best.id); return r;
        }
        return null;
    }
    public static ProductCatalog.Product choose(CustomerRequirements memory, ProductCatalog catalog) {
        for (ProductCatalog.Product p : catalog.all()) {
            if (memory.budget != null && p.price > memory.budget) continue;
            if (memory.camera && !p.camera) continue;
            if (memory.television && !p.tv180) continue;
            return p;
        }
        return null;
    }
    public static ChatResponse render(ConsultationReply reply, String question, CustomerRequirements memory, ProductCatalog catalog) {
        // Model actions never establish customer consent.
        if (!"none".equals(reply.action)) throw new IllegalArgumentException("Unsolicited action");
        if ("registration".equals(reply.intent)) throw new IllegalArgumentException("Unsolicited registration");
        String text = reply.message;
        LinkedHashSet<Integer> ids = new LinkedHashSet<>(reply.packageIds);
        if (reply.recommendedPackageId != null) ids.add(reply.recommendedPackageId);
        if (ids.size() > 0 && memory.budget != null && reply.recommendedPackageId != null && catalog.byId(reply.recommendedPackageId).price > memory.budget)
            throw new IllegalArgumentException("Over budget");
        if (reply.recommendedPackageId != null) {
            ProductCatalog.Product selected = catalog.byId(reply.recommendedPackageId);
            if (memory.camera && !selected.camera || memory.television && !selected.tv180) throw new IllegalArgumentException("Missing requested benefit");
        }
        for (int id : ids) text += "\n" + catalog.byId(id).display();
        ChatResponse r = reply(text); r.setRecommendedPackageId(reply.recommendedPackageId);
        if (reply.recommendedPackageId != null) memory.selectedPackageId = reply.recommendedPackageId;
        return r;
    }
    public static ChatResponse fallback(String question, CustomerRequirements memory, ProductCatalog catalog) {
        ChatResponse known = known(question, memory, catalog);
        if (known != null) return known;
        return reply("Em chưa xử lý được câu hỏi này đầy đủ. Anh/chị có thể cho em biết ngân sách, số người sử dụng hoặc gói Internet cần tìm hiểu; em sẽ tư vấn theo thông tin hiện có.");
    }
    private static ChatResponse unavailable() { return reply("Em chưa tải được danh mục gói cước hiện tại nên chưa thể xác nhận giá và quyền lợi. Anh/chị vui lòng thử lại hoặc gửi biểu mẫu tư vấn trên trang để được hỗ trợ."); }
    public static ChatResponse reply(String message) {
        ChatResponse r = new ChatResponse(message, "catalog", "", 0); r.setFinishReason("stop"); return r;
    }
}
