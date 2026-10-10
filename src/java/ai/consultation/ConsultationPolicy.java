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
    public static com.google.gson.JsonArray meshOptionsJson() {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        com.google.gson.JsonObject f1 = new com.google.gson.JsonObject();
        f1.addProperty("name", "Mesh WiFi F1");
        f1.addProperty("floors", "Nhà 2 tầng");
        f1.addProperty("devices", "1 Modem WiFi 6 + 1 Access Point");
        f1.addProperty("installationFeeVnd", 500000);
        f1.addProperty("monthlyAddonVnd", 10000);
        f1.addProperty("note", "Lựa chọn mở rộng vùng phủ sóng, cộng thêm +10.000đ/tháng vào tiền cước Internet hàng tháng");
        arr.add(f1);

        com.google.gson.JsonObject f2 = new com.google.gson.JsonObject();
        f2.addProperty("name", "Mesh WiFi F2");
        f2.addProperty("floors", "Nhà 3 tầng");
        f2.addProperty("devices", "1 Modem WiFi 6 + 2 Access Point");
        f2.addProperty("installationFeeVnd", 700000);
        f2.addProperty("monthlyAddonVnd", 10000);
        f2.addProperty("note", "Lựa chọn mở rộng vùng phủ sóng, cộng thêm +10.000đ/tháng vào tiền cước Internet hàng tháng");
        arr.add(f2);
        return arr;
    }

    public static ChatResponse known(String question, CustomerRequirements memory, ProductCatalog catalog) {
        String q = ProductCatalog.normalize(question);
        List<ProductCatalog.Product> mentioned = catalog.mentioned(question);

        // 1. Tốc độ gói cước (đặc biệt gói 239K = 1000 Mbps)
        boolean askingSpeed = q.contains("toc do") || q.contains("bang thong") || q.contains("mbps") || q.contains("gbps");
        if (askingSpeed && (q.contains("239") || (mentioned.size() == 1 && mentioned.get(0).price == 239000))) {
            return reply("Dạ gói 239K có tốc độ chính xác là 1000 Mbps (1 Gbps). Gói đi kèm 01 Modem WiFi 6, tài khoản truyền hình FPT Play 180 kênh và nội dung Ngoại Hạng Anh, giá cước 239.000đ/tháng ạ.");
        }
        if (askingSpeed && mentioned.size() == 1) {
            ProductCatalog.Product p = mentioned.get(0);
            return reply("Dạ gói " + p.label() + " có tốc độ công bố là " + p.speed + " Mbps" + (p.speed >= 1000 ? " (1 Gbps)" : "") + ", giá cước " + String.format(Locale.US, "%,d", p.price).replace(',', '.') + "đ/tháng.\n" + p.display());
        }

        // 2. Tư vấn Mesh WiFi theo số tầng
        if ((q.contains("3 tang") || q.contains("ba tang")) && (q.contains("f1") || q.contains("dung f1") || q.contains("duoc khong") || q.contains("on khong") || q.contains("co duoc khong"))) {
            return reply("Dạ với nhà 3 tầng, gói Mesh WiFi F1 (gồm 1 Modem WiFi 6 + 1 Access Point) vốn được thiết kế tối ưu cho nhà 2 tầng, nên có thể sẽ bị sóng yếu hoặc không phủ kín các phòng/tầng nếu có vật cản hoặc tường dày.\n"
                + "Để đảm bảo phủ sóng tốt và ổn định hơn cho nhà 3 tầng, em đề xuất anh/chị lựa chọn giải pháp Mesh WiFi F2:\n"
                + "- Thiết bị: 1 Modem WiFi 6 + 2 Access Point.\n"
                + "- Phí lắp đặt: 700.000 VNĐ.\n"
                + "- Phí cộng thêm hàng tháng: +10.000đ/tháng (cộng thêm vào tiền cước gói Internet đã chọn).\n"
                + "Lưu ý: Mesh WiFi F2 là giải pháp mở rộng phủ sóng kết hợp với gói Internet FPT, và không cam kết 100% phủ sóng mọi ngóc ngách nếu chưa kiểm tra vị trí đặt thiết bị và vật cản thực tế. Anh/chị cho em biết thêm diện tích mỗi tầng và nhu cầu sử dụng để em tư vấn gói cước Internet kết hợp tối ưu nhé!");
        }
        if (q.contains("2 tang") || q.contains("hai tang") || (q.contains("f1") && !q.contains("f2"))) {
            return reply("Dạ với nhà 2 tầng, em đề xuất anh/chị lựa chọn giải pháp mở rộng vùng phủ sóng Mesh WiFi F1:\n"
                + "- Phù hợp: Nhà 2 tầng, giúp loại bỏ góc chết WiFi giữa các tầng.\n"
                + "- Thiết bị: 1 Modem WiFi 6 + 1 Access Point.\n"
                + "- Phí lắp đặt: 500.000 VNĐ.\n"
                + "- Phí cộng thêm hàng tháng: +10.000đ/tháng (cộng thêm vào tiền cước gói Internet).\n"
                + "Lưu ý: Mesh WiFi F1 là lựa chọn mở rộng phủ sóng kết hợp cùng gói cước Internet FPT, không phải gói độc lập (Tổng cước = Giá gói Internet + 10.000đ/tháng). Anh/chị cho em biết thêm diện tích nhà, số người dùng và nhu cầu sử dụng Internet để em tư vấn gói cước nền phù hợp nhất nhé!");
        }
        if (q.contains("3 tang") || q.contains("ba tang") || q.contains("f2")) {
            return reply("Dạ với nhà 3 tầng, em đề xuất anh/chị lựa chọn giải pháp Mesh WiFi F2 để đảm bảo phủ sóng đều các tầng:\n"
                + "- Thiết bị: 1 Modem WiFi 6 + 2 Access Point.\n"
                + "- Phí lắp đặt: 700.000 VNĐ.\n"
                + "- Phí cộng thêm hàng tháng: +10.000đ/tháng (cộng thêm vào tiền cước gói Internet đã chọn).\n"
                + "Lưu ý: Mesh WiFi F2 là giải pháp mở rộng phủ sóng kết hợp cùng gói cước Internet FPT. Anh/chị cho em biết thêm diện tích mỗi tầng, số lượng thiết bị và nhu cầu để em tư vấn gói Internet nền kết hợp phù hợp nhé!");
        }
        if (q.contains("mesh")) {
            return reply("Dạ FPT hiện có 2 giải pháp mở rộng vùng phủ sóng Mesh WiFi cho nhà nhiều tầng (kết hợp cùng gói cước Internet FPT):\n"
                + "1. Mesh WiFi F1 (Phù hợp nhà 2 tầng): 1 Modem WiFi 6 + 1 Access Point. Phí lắp đặt 500.000 VNĐ, phí cộng thêm +10.000đ/tháng.\n"
                + "2. Mesh WiFi F2 (Phù hợp nhà 3 tầng): 1 Modem WiFi 6 + 2 Access Point. Phí lắp đặt 700.000 VNĐ, phí cộng thêm +10.000đ/tháng.\n"
                + "Lưu ý: Phí hàng tháng của Mesh WiFi là phí cộng thêm vào tiền cước gói Internet hàng tháng. Anh/chị cho em biết số tầng và diện tích nhà để em tư vấn chi tiết nhé!");
        }

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
