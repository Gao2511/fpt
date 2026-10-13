package ai.consultation;

import ai.dto.ChatResponse;
import java.util.*;

/** Server-owned business decisions and factual rendering also used during outages. */
public final class ConsultationPolicy {
    private ConsultationPolicy() {}
    public static final String CONTACT = "điện thoại/Zalo 0932 079 469";
    private static final String ASSISTANT_NAME = "Tư vấn viên AI của Phúc";
    private static final String CONSULTANT_NAME = "Phúc";
    private static final String REGISTRATION_URL = "https://fpt.vn/sale/phucbn2-18";
    private static final String HUMAN_HOURS = "07:00–22:00";
    private static final String CHATBOT_HOURS = "24/24";
    public static final String TECHNICAL_FAILURE = "Dạ em đang gặp chút trục trặc nên chưa thể trả lời chính xác lúc này. Anh/chị có thể thử lại hoặc liên hệ tư vấn viên qua " + CONTACT + " để được hỗ trợ nhé ạ.";
    public static boolean registrationRequested(String text) {
        String q = ProductCatalog.normalize(text);
        if (q.matches("(?s).*(?:khong|chua)\\s+(?:muon\\s+)?(?:dang ky|mua|chot).*")) return false;
        return q.matches("(?s).*(?:toi|minh|anh|chi|em)\\s+(?:muon|can|xin|se)\\s+(?:dang ky|mua)(?:\\s|$).*") ||
            q.matches("^(?:cho (?:toi|minh|anh|chi|em) )?dang ky\\s+(?:goi|internet|mang|[0-9]).*" ) ||
            q.trim().matches("(?:oke?\\s+)?chot(?:\\s+luon|\\s+goi\\s+[0-9]+k?)?[.! ]*") ||
            q.matches("^(?:toi|minh|anh|chi|em)\\s+chot(?:\\s|$).*") ||
            q.matches("(?s).*(?:toi|minh|anh|chi|em)\\s+(?:muon|xin)\\s+chot(?:\\s|$).*");
    }
    public static com.google.gson.JsonArray meshOptionsJson() {
        return meshOptionsJson(cms.SiteContent.defaults());
    }

    public static ChatResponse installationFee(String question, CustomerRequirements memory, ProductCatalog catalog) {
        String q = ProductCatalog.normalize(question);
        if (registrationRequested(question) || ConversationSignals.handoff(question)
                || q.matches("(?s).*(mesh|\\bf1\\b|\\bf2\\b|camera|truyen hinh|fpt play).*") ) return null;
        boolean monthly = q.matches("(?s).*300\\s*(k|nghin|ngan|[.,]000).*" )
                && (q.contains("thang") || q.contains("mot lan") || q.contains("1 lan"));
        boolean fee = q.matches("(?s).*(phi (lap|hoa mang)|gia lap|lap.*(bao nhieu|mat.*tien|ton.*tien|mat phi)).*");
        if (!fee && !monthly) return null;
        List<ProductCatalog.Product> mentioned = catalog.mentioned(question);
        if (mentioned.isEmpty() && q.matches("(?s).*goi\\s+[0-9]+.*")) return null;
        List<ProductCatalog.Product> plans = mentioned.isEmpty() ? catalog.all() : mentioned;
        if (plans.isEmpty()) return unavailable();
        if(!mentioned.isEmpty())for(ProductCatalog.Product p:mentioned)if(p.standardInstallationFee<=0)return reply("Dạ phí lắp đặt của gói đang hỏi chưa được xác nhận. Anh/chị vui lòng liên hệ tư vấn viên để kiểm tra chính sách áp dụng.");
        if(mentioned.size()>1){StringBuilder fees=new StringBuilder("Dạ phí lắp đặt trả một lần, cước hàng tháng tính riêng:\n");for(ProductCatalog.Product p:mentioned)fees.append("Gói ").append(p.label()).append(": ").append(String.format(Locale.US,"%,d",p.standardInstallationFee).replace(',','.')).append("đ/lần; cước ").append(String.format(Locale.US,"%,d",p.price).replace(',','.')).append("đ/tháng.\n");return reply(fees.append("Phí thực tế áp dụng theo chính sách được xác nhận cho từng trường hợp.").toString());}
        long feeAmount=mentioned.isEmpty()?Long.parseLong(catalog.business.get("business.standardFee")):mentioned.get(0).standardInstallationFee;
        String amount = String.format(Locale.US, "%,d", feeAmount).replace(',', '.');
        StringBuilder text = new StringBuilder(monthly ? "Dạ không ạ, " + amount + "đ là phí lắp đặt Internet thông thường trả một lần, không phải cước hàng tháng. " : "Dạ phí lắp đặt Internet FPT thông thường là " + amount + "đ/lần lắp đặt. ");
        for (ProductCatalog.Product p : mentioned) text.append("Gói ").append(p.label()).append(" có cước ")
                .append(String.format(Locale.US, "%,d", p.price).replace(',', '.')).append("đ/tháng. ");
        if (mentioned.isEmpty()) text.append("Cước hàng tháng tính riêng theo gói Internet mình chọn. ");
        text.append("Phí thực tế áp dụng theo chính sách ưu đãi được xác nhận; em chưa có dữ liệu xác nhận miễn giảm cho trường hợp của anh/chị.");
        return reply(text.toString());
    }

    public static ChatResponse known(String question, CustomerRequirements memory, ProductCatalog catalog) {
        ChatResponse result=knownOriginal(question,memory,catalog);
        if(result!=null)result.setContent(facts(result.getContent(),catalog.business));return result;
    }
    private static ChatResponse knownOriginal(String question, CustomerRequirements memory, ProductCatalog catalog) {
        ChatResponse installation = installationFee(question, memory, catalog);
        if (installation != null) return installation;
        String q = ProductCatalog.normalize(question);
        List<ProductCatalog.Product> mentioned = catalog.mentioned(question);
        if (registrationRequested(question)) return registration(question, memory, catalog, mentioned);
        if (ConversationSignals.handoff(question)) return reply("Dạ chatbot hỗ trợ " + CHATBOT_HOURS + "; " + CONSULTANT_NAME + " tư vấn trực tiếp trong khung " + HUMAN_HOURS + " (giờ Việt Nam). Anh/chị có thể chủ động liên hệ qua " + CONTACT + ". Hệ thống chưa có cơ chế tự động chuyển thông tin hoặc yêu cầu gọi lại cho " + CONSULTANT_NAME + ".");
        if (q.matches("(?s).*(?:co phai|la)\\s+(?:nguoi that|robot|ai).*")) return reply("Dạ em là " + ASSISTANT_NAME + ", hỗ trợ tư vấn trực tuyến " + CHATBOT_HOURS + ". Khi cần tư vấn trực tiếp, " + CONSULTANT_NAME + " hỗ trợ trong khung " + HUMAN_HOURS + " (giờ Việt Nam). Anh/chị đang muốn tìm hiểu gói cước hay cần hỗ trợ vấn đề mạng ạ?");
        if (ConversationSignals.greeting(question)) return reply(ConversationSignals.opening(memory) + "em là " + ASSISTANT_NAME + " đây ạ! " + ConversationSignals.address(memory) + " cần tìm gói mạng hay muốn hỏi thêm điều gì?");
        if (ConversationSignals.social(question)) return reply(q.contains("dua") || q.contains("haha") ? "Dạ, em hiểu mình đang đùa thôi nhé! Khi cần hỏi về mạng, em vẫn ở đây ạ." : "Dạ, em vẫn ở đây khi mình cần hỗ trợ thêm nhé!");
        if (ConversationSignals.playful(question)) return reply("Haha, mạng không tặng kèm phép thuật hay kỹ năng pro player đâu ạ! Chơi game còn phụ thuộc ping, kết nối và máy chủ game; mình chơi trên PC hay điện thoại?");
        if (ConversationSignals.outside(question)) return reply("Dạ em hỗ trợ tư vấn dịch vụ FPT Telecom nên chưa thể làm yêu cầu này. Khi cần tìm hiểu gói Internet, em sẵn sàng hỗ trợ ạ.");
        ChatResponse combined = combinedSolution(question, memory, catalog);
        if (combined != null) return combined;
        if (ConversationSignals.ambiguous(question)) {
            ProductCatalog.Product selected = memory.selectedPackageId == null || memory.selectionAmbiguous ? null : catalog.byId(memory.selectedPackageId);
            return reply(selected == null ? "Anh/chị đang nói đến gói nào ạ?" : selected.display() + "\nAnh/chị muốn tìm hiểu thêm hay đăng ký gói này?");
        }
        if (memory.previousBudget != null && memory.budget != null && memory.budget > memory.previousBudget) {
            StringBuilder extra = new StringBuilder(ConversationSignals.opening(memory) + "với ngân sách mới, mình có thêm các lựa chọn sau:\n");
            int count = 0;
            for (ProductCatalog.Product p : catalog.all()) if (p.price > memory.previousBudget && fits(p, memory)) { extra.append(p.display()).append('\n'); count++; }
            if (count > 0) { memory.selectionAmbiguous = true; return reply(extra.toString().trim()); }
        }
        ChatResponse cameraAddition=cameraAddition(question,memory,catalog);
        if(cameraAddition!=null)return cameraAddition;
        if (q.matches("(?s).*(?:co|combo|kem).*camera\\s*(?:khong|ko|hong|khum)[?! .]*")) {
            ProductCatalog.Product selected = memory.selectedPackageId == null ? null : catalog.byId(memory.selectedPackageId);
            if (selected != null) return reply((selected.camera ? "Gói đã chọn có camera. " : "Gói đã chọn chưa có camera. ") + selected.display());
            StringBuilder choices = new StringBuilder("Các gói có camera trong danh mục:\n");
            for (ProductCatalog.Product p : catalog.all()) if (p.camera) choices.append(p.display()).append('\n');
            return reply(choices.toString().trim());
        }
        if (q.matches("(?s).*(?:internet kem truyen hinh|combo.*truyen hinh).*")) {
            StringBuilder choices=new StringBuilder("Các gói Internet kèm truyền hình trong danh mục:\n");
            for(ProductCatalog.Product p:catalog.all())if(p.tv180)choices.append(p.display()).append('\n');
            return reply(choices.toString().trim());
        }

        // 1. Tốc độ gói cước (đặc biệt gói 239K = 1000 Mbps)
        boolean askingSpeed = q.contains("toc do") || q.contains("bang thong") || q.contains("mbps") || q.contains("gbps");
        if (askingSpeed && mentioned.size() == 1) {
            ProductCatalog.Product p = mentioned.get(0);
            return reply("Dạ gói " + p.label() + " có tốc độ công bố là " + p.speed + " Mbps" + (p.speed >= 1000 ? " (1 Gbps)" : "") + ", giá cước " + String.format(Locale.US, "%,d", p.price).replace(',', '.') + "đ/tháng.\n" + p.display());
        }
        if (q.contains("cam ket") && (q.contains("toc do") || q.contains("24/7")))
            return reply("Dạ em chưa có dữ liệu xác nhận cam kết tốc độ tuyệt đối hoặc liên tục trong mọi trường hợp. Tốc độ công bố của gói không phải bằng chứng cho cam kết này; anh/chị cần tư vấn viên kiểm tra điều kiện cụ thể.");

        if ((q.contains("uu dai")||q.contains("khuyen mai")) && cms.SiteContent.promotionVisible(catalog.business)) {
            String ids=catalog.business.get("business.promotionPackageIds");
            boolean applies=mentioned.isEmpty()||ids.isEmpty();
            for(ProductCatalog.Product p:mentioned)if(java.util.Arrays.asList(ids.split(",")).contains(Integer.toString(p.id)))applies=true;
            if(applies)return reply(catalog.business.get("business.promotionTitle")+"\n"+catalog.business.get("business.promotionDescription")+"\nĐiều kiện áp dụng theo thông tin trên; phí thực tế cần được xác nhận, không tự động tính miễn giảm.");
        }
        if (q.matches("(?s).*(phi (?:lap|hoa)|gia lap|lap dat.*bao nhieu|khuyen mai|uu dai|giam gia|hop dong|vung phu|ha tang|bao lau.*lap).*"))
            return reply("Em chưa có dữ liệu xác nhận về phí sau ưu đãi, điều khoản hoặc hạ tầng tại địa chỉ của anh/chị. Anh/chị có thể gửi biểu mẫu tư vấn hoặc liên hệ " + CONTACT + " để được kiểm tra; em chưa thể báo chi phí thực tế hay cam kết lịch lắp đặt.");
        if (q.contains("ban chay") || q.contains("pho bien nhat"))
            return reply("Em chưa có số liệu bán hàng để xác định gói bán chạy nhất. Anh/chị cho em biết ngân sách, số người sử dụng và nhu cầu chính để em chọn gói phù hợp nhé.");
        if (q.contains("so sanh") || mentioned.size() >= 2 || q.contains("khac nhau") || q.contains("tat ca") || q.contains("6 goi")) {
            List<ProductCatalog.Product> compare = mentioned.size() >= 2 && !q.contains("tat ca") && !q.contains("6 goi") ? mentioned : catalog.all();
            if (compare.isEmpty()) return unavailable();
            memory.selectionAmbiguous = compare.size() > 1;
            StringBuilder text = new StringBuilder("Em gửi anh/chị thông tin các gói để so sánh:\n");
            for (ProductCatalog.Product p : compare) text.append(p.display()).append('\n');
            if (compare.size() == 2) {
                ProductCatalog.Product a = compare.get(0), b = compare.get(1);
                text.append("Chênh lệch giá: ").append(String.format(Locale.US, "%,d", Math.abs(a.price - b.price)).replace(',', '.')).append("đ/tháng; tốc độ lần lượt ").append(a.speed).append("Mbps và ").append(b.speed).append("Mbps.");
            }
            return reply(text.toString().trim());
        }
        if (mentioned.size() == 1) {
            ProductCatalog.Product p = mentioned.get(0); memory.selectedPackageId = p.id; memory.selectionAmbiguous = false;
            ChatResponse r = reply(p.display() + ("direct".equals(memory.style) ? "" : "\nAnh/chị muốn tìm hiểu thêm điều gì về gói này?"));
            r.setRecommendedPackageId(p.id); return r;
        }
        if (memory.requirementsChanged || q.matches("(?s).*\\b(sinh vien|ngan sach|toi co|minh co|nguoi|dua|game|phong tro|o tro|goi nao|goi.*re|ngon bo re|tu van|chon goi|tim hieu.*internet)\\b.*")) {
            ProductCatalog.Product best = choose(memory, catalog);
            ProductCatalog.Product previous = memory.selectedPackageId == null ? null : catalog.byId(memory.selectedPackageId);
            if (previous != null && fits(previous, memory) && q.matches("(?s).*(?:mang nay|goi nay|mang da chon).*")) best = previous;
            if (best == null) return catalog.all().isEmpty() ? unavailable() : reply("Em chưa tìm thấy gói phù hợp với ngân sách và quyền lợi anh/chị cần trong danh mục hiện có. Anh/chị có thể điều chỉnh ngân sách hoặc nhu cầu để em so sánh thêm nhé.");
            memory.selectedPackageId = best.id;
            memory.selectionAmbiguous = false;
            String text = ConversationSignals.opening(memory) + "em gợi ý lựa chọn tiết kiệm đáp ứng ngân sách và tiện ích mình đã nêu: " + best.display();
            if (memory.people != null) text += "\nEm đã ghi nhận " + memory.people + " người sử dụng.";
            if (memory.gaming) text += "\nVới nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế.";
            if (memory.devices != null) text += "\nEm đã ghi nhận " + memory.devices + " thiết bị; mức sử dụng đồng thời còn ảnh hưởng trải nghiệm thực tế.";
            if (!"direct".equals(memory.style) && !memory.televisionKnown && !memory.cameraKnown && !memory.amenitiesAsked) {
                text += "\n" + ConversationSignals.address(memory) + " có cần thêm truyền hình hoặc camera không?";
                memory.amenitiesAsked = true;
            }
            ChatResponse r = reply(text); r.setRecommendedPackageId(best.id); return r;
        }
        return null;
    }
    public static ProductCatalog.Product choose(CustomerRequirements memory, ProductCatalog catalog) {
        for (ProductCatalog.Product p : catalog.all()) {
            if (fits(p, memory)) return p;
        }
        return null;
    }

    /** Deterministic multi-turn discovery and objection handling before any model call. */
    public static ChatResponse conversationFlow(String question, CustomerRequirements memory, ProductCatalog catalog) {
        String q = ProductCatalog.normalize(question);
        ChatResponse objection = objection(question, memory, catalog);
        if (objection != null) return objection;
        if (!memory.consultationStarted) return null;
        if (memory.floors == null) {
            return reply("Dạ em là " + ASSISTANT_NAME + ", em hỗ trợ mình chọn giải pháp phù hợp ạ. Nhà mình là nhà cấp 4 hay có mấy tầng vậy anh/chị?");
        }
        if (!memory.televisionKnown && !memory.cameraKnown) {
            memory.amenitiesAsked = true;
            return reply("Dạ em đã ghi nhận nhà " + memory.floors + " tầng" + (memory.people == null ? "" : ", " + memory.people + " người sử dụng") + ". Nhà mình có cần xem truyền hình, bóng đá hoặc sử dụng camera không ạ?");
        }
        if (memory.serviceArea == null) {
            memory.locationAsked = true;
            return reply("Dạ em đã ghi nhận nhu cầu truyền hình/bóng đá" + (memory.cameraKnown && !memory.camera ? " và không lấy camera" : "") + ". Anh/chị cho em biết khu vực xã/phường dự kiến lắp để tư vấn tiếp nhé; em chưa xác nhận hạ tầng tại địa chỉ này.");
        }
        return null;
    }

    private static ChatResponse objection(String question, CustomerRequirements memory, ProductCatalog catalog) {
        String q = ProductCatalog.normalize(question);
        if (q.matches("(?s).*(?:khong muon|khong can|khong thich).*dong truoc.*"))
            return reply("Dạ em tôn trọng lựa chọn của anh/chị ạ. Mình có thể tiếp tục xem phương án theo cước và phí lắp thông thường; em sẽ không tự áp dụng hay tiếp tục thuyết phục mình dùng ưu đãi trả trước.");
        if (q.matches("(?s).*(?:viettel|vnpt|ben khac|nha mang khac).*(?:re hon|chi|phi|gia|thiet bi).*") || q.matches("(?s).*(?:ben khac|nha mang khac) re hon.*"))
            return reply("Dạ nếu phương án bên khác có chi phí ban đầu thấp hơn thì mình cân nhắc là hợp lý ạ. Để so sánh công bằng, mình nên đối chiếu cùng số điểm phát WiFi, phí hàng tháng, điều kiện lắp và phạm vi hỗ trợ. Với FPT em có thể giúp anh/chị so sánh F1 và F2 theo đúng cấu hình hiện có; em không khẳng định nhà mạng nào tốt hơn khi chưa có dữ liệu tương đương.");
        if (q.matches("(?s).*(?:de|cho) (?:anh|chi|toi|minh) suy nghi.*")) {
            ProductCatalog.Product selected = memory.selectedPackageId == null ? null : catalog.byId(memory.selectedPackageId);
            return reply(selected == null ? "Dạ vâng ạ, anh/chị cứ cân nhắc thoải mái. Khi cần em sẽ tiếp tục từ các nhu cầu mình đã chia sẻ."
                : "Dạ vâng ạ, anh/chị cứ cân nhắc thoải mái. Em đang ghi nhận " + selected.label() + (memory.meshInterest == null ? "" : " + Mesh " + memory.meshInterest) + "; khi cần mình có thể tiếp tục từ phương án này.");
        }
        if (q.matches("(?s).*(?:6|sau|12|muoi hai)\\s*thang.*(?:tra truoc|dong truoc|uu dai|giam).*")
                || q.matches("(?s).*(?:tra truoc|dong truoc).*(?:6|sau|12|muoi hai)\\s*thang.*")
                || q.matches("(?s).*(?:uu dai|giam).*phi lap.*"))
            return promotionReply(memory, catalog);
        boolean installationConcern = q.matches("(?s).*(?:(?:phi lap|700\\s*k|700[.]?000|500\\s*k|500[.]?000).*(?:cao|dat qua|mac|giam|re|tiet kiem)|cach nao giam phi lap).*" );
        if (installationConcern) {
            String id = memory.meshInterest == null ? (memory.floors != null && memory.floors >= 3 ? "F2" : "F1") : memory.meshInterest;
            RecommendedSolution.WifiCoverageOption mesh = meshOption(id, catalog.business);
            String amount = mesh.installationFeeTotal == null ? "mức phí chưa được cấu hình" : RecommendedSolution.money(mesh.installationFeeTotal);
            return reply("Dạ em hiểu mình đang muốn tiết kiệm phí lắp ban đầu ạ. " + amount + " là tổng phí lắp của phương án Mesh " + id + ", thay cho phí Internet thông thường 300.000đ; hệ thống không cộng 300.000đ thêm lần nữa. Phương án này gồm " + (mesh.equipment == null ? "thiết bị theo cấu hình được duyệt" : mesh.equipment) + ". Nếu muốn giảm chi phí ban đầu, em có thể so sánh F1 hoặc Internet tiêu chuẩn, nhưng vùng phủ còn phụ thuộc cấu trúc nhà và vị trí lắp. Anh/chị muốn ưu tiên chi phí hay vùng phủ ạ?");
        }
        if (q.matches("(?s).*(?:nha .*tang.*f1|f1.*(?:duoc khong|on khong|thay f2)).*")) {
            RecommendedSolution.WifiCoverageOption f1 = meshOption("F1", catalog.business);
            return reply("Dạ F1 gồm " + (f1.equipment == null ? "1 điểm phát bổ sung" : f1.equipment) + (f1.monthlyAddonPrice == null ? "" : ", cộng " + RecommendedSolution.money(f1.monthlyAddonPrice) + "/tháng") + (f1.installationFeeTotal == null ? "" : ", tổng phí lắp " + RecommendedSolution.money(f1.installationFeeTotal)) + ". Nhà nhiều tầng vẫn có thể cân nhắc F1, nhưng em không thể bảo đảm vùng phủ khi chưa biết mặt bằng, vật cản và vị trí đặt thiết bị. Anh/chị muốn ưu tiên phí ban đầu thấp hơn hay phủ sóng nhiều tầng ạ?");
        }
        if (q.matches("(?s).*(?:gia|phi|cuoc).*(?:cao|dat qua).*"))
            return reply("Dạ em hiểu mình đang cân nhắc chi phí ạ. Anh/chị đang thấy cao ở cước hàng tháng hay phí lắp ban đầu để em so sánh đúng phương án tiết kiệm hơn?");
        return null;
    }

    /** Returns one server-priced complete solution for coverage-related consultation. */
    public static ChatResponse combinedSolution(String question, CustomerRequirements memory, ProductCatalog catalog) {
        if (registrationRequested(question) || ConversationSignals.handoff(question)) return null;
        String q = ProductCatalog.normalize(question);
        if (q.contains("so sanh") || q.contains("tat ca") || q.contains("6 goi") || catalog.mentioned(question).size() > 1) return null;
        boolean currentCoverageSignal = q.matches("(?s).*(?:[2-9]\\s*tang|hai tang|ba tang|bon tang|mesh|wifi yeu|song yeu|tang tren|nhieu phong|tuong day|tuong be tong|phong xa|goc chet).*" );
        boolean coverageSignal = currentCoverageSignal
            || memory.floors != null && memory.floors >= 2 && (memory.requirementsChanged || memory.consultationStarted);
        boolean internetOnly = memory.meshPreferenceKnown && !memory.meshWanted
            && q.matches("(?s).*(?:chi (?:muon|can) goi internet|khong can mesh|khong dung mesh).*" );
        if (!coverageSignal && !internetOnly) return null;
        if (coverageSignal && memory.floors == null && memory.meshInterest == null
                && (!memory.meshPreferenceKnown || memory.meshWanted))
            return reply("Dạ em đã ghi nhận nhu cầu mở rộng vùng phủ WiFi. Anh/chị cho em biết nhà có bao nhiêu tầng để em ghép đúng gói Internet với phương án Mesh và tính tổng chính xác nhé?");

        List<ProductCatalog.Product> mentioned = catalog.mentioned(question);
        ProductCatalog.Product selected = mentioned.size() == 1 ? mentioned.get(0) : chooseForSolution(memory, catalog);
        if (selected == null) return catalog.all().isEmpty() ? unavailable()
            : reply("Dạ em chưa tìm được một gói vừa đáp ứng nhu cầu đã chia sẻ vừa nằm trong ngân sách hiện tại. Anh/chị có thể cho em biết mức ngân sách có thể điều chỉnh không ạ?");

        RecommendedSolution.WifiCoverageOption coverage = null;
        if (!memory.meshPreferenceKnown || memory.meshWanted) {
            String option = memory.floors != null && memory.floors >= 3 ? "F2"
                : memory.floors != null && memory.floors == 2 ? "F1"
                : "F1".equals(memory.meshInterest) || "F2".equals(memory.meshInterest) ? memory.meshInterest : null;
            if (option != null) coverage = meshOption(option, catalog.business);
        }

        List<String> optional = new ArrayList<>();
        if (memory.camera) optional.add(memory.cameraCount == null ? "Camera" : memory.cameraCount + " camera");
        if (memory.television) optional.add("Truyền hình");
        List<String> sources = new ArrayList<>();
        sources.add("packages:" + selected.id);
        if (coverage != null) {
            sources.add("cms:business.mesh" + coverage.id + "Monthly");
            sources.add("cms:business.mesh" + coverage.id + "Fee");
        }
        Long internetFee = selected.standardInstallationFee > 0 ? selected.standardInstallationFee
            : configuredMoney(catalog.business, "business.standardFee");
        String reason = reason(memory, coverage);
        RecommendedSolution solution = new RecommendedSolution(selected, coverage, optional, internetFee,
            reason, memory.people == null && memory.devices == null ? "preliminary" : "requirements_based", sources,
            memory.prepaymentMonths, configuredMoney(catalog.business, "business.prepayInstallDiscount"),
            configuredInteger(catalog.business, "business.prepay12BonusMonths"), confirmedPrepayPromotion(catalog.business));
        memory.selectedPackageId = selected.id;
        memory.selectionAmbiguous = false;
        ProductCatalog.Product alternative = null;
        if (selected.speed < 1000 && memory.television && memory.cameraKnown && !memory.camera) {
            for (ProductCatalog.Product candidate : catalog.all()) {
                if (candidate.speed >= 1000 && fits(candidate, memory)) { alternative = candidate; break; }
            }
        }
        ChatResponse response = reply(renderSolution(solution, memory, coverageSignal, alternative, catalog.business));
        response.setRecommendedPackageId(selected.id);
        return response;
    }

    private static ProductCatalog.Product chooseForSolution(CustomerRequirements memory, ProductCatalog catalog) {
        int minimumSpeed = memory.requestedSpeedMbps == null ? 0 : memory.requestedSpeedMbps;
        if (memory.businessUse || memory.cameraCount != null && memory.cameraCount >= 4
                || memory.people != null && memory.people >= 8 || memory.devices != null && memory.devices >= 12) minimumSpeed = 1000;
        else if (minimumSpeed < 1000 && (memory.cameraCount != null && memory.cameraCount >= 2
                || memory.devices != null && memory.devices >= 8)) minimumSpeed = 300;
        ProductCatalog.Product current = memory.selectedPackageId == null ? null : catalog.byId(memory.selectedPackageId);
        if (current != null && current.speed >= minimumSpeed && fits(current, memory)) return current;
        for (ProductCatalog.Product product : catalog.all()) if (product.speed >= minimumSpeed && fits(product, memory)) return product;
        return null;
    }

    private static RecommendedSolution.WifiCoverageOption meshOption(String id, Map<String,String> business) {
        String prefix = "business.mesh" + id;
        String equipment = business.get(prefix + "Equipment");
        Long monthly = configuredMoney(business, prefix + "Monthly");
        Long fee = configuredMoney(business, prefix + "Fee");
        return new RecommendedSolution.WifiCoverageOption(id, equipment, monthly, fee);
    }

    private static Long configuredMoney(Map<String,String> values, String key) {
        try {
            String raw = values == null ? null : values.get(key);
            if (raw == null || !raw.matches("[0-9]{1,9}")) return null;
            long value = Long.parseLong(raw);
            return value >= 0 && value <= 100000000 ? value : null;
        } catch (RuntimeException invalid) { return null; }
    }

    private static Integer configuredInteger(Map<String,String> values, String key) {
        Long value = configuredMoney(values, key);
        return value == null || value > Integer.MAX_VALUE ? null : value.intValue();
    }

    private static boolean confirmedPrepayPromotion(Map<String,String> values) {
        return values != null && "true".equals(values.get("business.prepayPromotionConfirmed"))
            && values.get("business.prepayConfirmedDate") != null && !values.get("business.prepayConfirmedDate").isEmpty()
            && configuredMoney(values, "business.prepayInstallDiscount") != null;
    }

    private static String promotionDateDisplay(Map<String,String> values) {
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(values.get("business.prepayConfirmedDate"));
            return String.format(Locale.ROOT, "%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
        } catch (RuntimeException invalid) { return "chưa có ngày xác nhận"; }
    }

    private static ChatResponse promotionReply(CustomerRequirements memory, ProductCatalog catalog) {
        if (!confirmedPrepayPromotion(catalog.business))
            return reply("Em chưa có cấu hình xác nhận ưu đãi trả trước đang áp dụng. Anh/chị vui lòng liên hệ " + CONTACT + " để kiểm tra.");
        Long fee = configuredMoney(catalog.business, "business.standardFee");
        if (memory.meshInterest != null) fee = meshOption(memory.meshInterest, catalog.business).installationFeeTotal;
        Long discount = configuredMoney(catalog.business, "business.prepayInstallDiscount");
        if (memory.prepaymentMonths != null) {
            Long finalFee = CommercialPriceValidator.discountedInstallation(fee, discount, true);
            String plan = memory.prepaymentMonths == 12
                ? "Trả trước 12 tháng được giảm " + RecommendedSolution.money(discount) + " phí lắp và tặng " + configuredInteger(catalog.business, "business.prepay12BonusMonths") + " tháng dịch vụ"
                : "Trả trước 6 tháng được giảm " + RecommendedSolution.money(discount) + " phí lắp";
            return reply("Dạ " + plan + ". " + (memory.meshInterest == null ? "Phí lắp Internet thông thường" : "Phí lắp Mesh " + memory.meshInterest)
                + " từ " + RecommendedSolution.money(fee) + " còn " + RecommendedSolution.money(finalFee) + "/lần. Chính sách được xác nhận ngày " + promotionDateDisplay(catalog.business) + "; chưa có ngày hết hiệu lực được xác nhận.");
        }
        return reply("Dạ chính sách được xác nhận ngày " + promotionDateDisplay(catalog.business) + ": trả trước 6 tháng giảm " + RecommendedSolution.money(discount)
            + " phí lắp; trả trước 12 tháng giảm " + RecommendedSolution.money(discount) + " phí lắp và tặng "
            + configuredInteger(catalog.business, "business.prepay12BonusMonths") + " tháng dịch vụ. Ưu đãi áp dụng cả phí lắp Mesh F1 (500.000đ còn 300.000đ) và F2 (700.000đ còn 500.000đ). Chưa có ngày hết hiệu lực được xác nhận.");
    }

    private static String reason(CustomerRequirements memory, RecommendedSolution.WifiCoverageOption coverage) {
        List<String> parts = new ArrayList<>();
        if (memory.floors != null) parts.add("nhà " + memory.floors + " tầng");
        if (memory.people != null) parts.add(memory.people + " người dùng");
        if (memory.devices != null) parts.add(memory.devices + " thiết bị");
        if (memory.streaming) parts.add("nhu cầu xem phim nhiều");
        if (memory.gaming) parts.add("nhu cầu chơi game");
        if (memory.cameraCount != null) parts.add(memory.cameraCount + " camera");
        if (parts.isEmpty()) return coverage == null ? "yêu cầu chỉ dùng Internet" : "nhu cầu phủ sóng WiFi";
        return String.join(", ", parts);
    }

    private static String renderSolution(RecommendedSolution solution, CustomerRequirements memory, boolean coverageSignal,
                                         ProductCatalog.Product alternative, Map<String,String> business) {
        StringBuilder out = new StringBuilder("Dạ với ").append(solution.reason).append(", em ưu tiên phương án ")
            .append("Gói ").append(solution.internetPackage.label());
        if (solution.wifiCoverageOption != null) out.append(" + ").append(solution.wifiCoverageOption.name);
        out.append(".\n\n- Internet nền: ").append(RecommendedSolution.money(solution.monthlyInternetPrice)).append("/tháng — ")
            .append(solution.internetPackage.speed).append("Mbps");
        if (solution.internetPackage.wifi6) out.append(", modem WiFi 6");
        if (solution.internetPackage.camera) out.append(", 1 Camera");
        if (solution.internetPackage.tv180) out.append(", TV 180 kênh");
        if (solution.internetPackage.premierLeague) out.append(", nội dung Ngoại Hạng Anh");
        if (solution.wifiCoverageOption != null) {
            out.append("\n- Phủ sóng: ").append(solution.wifiCoverageOption.name);
            if (solution.wifiCoverageOption.equipment != null && !solution.wifiCoverageOption.equipment.trim().isEmpty())
                out.append(" (").append(solution.wifiCoverageOption.equipment.trim()).append(")");
            if (solution.monthlyAddonPrice != null) out.append(" — +").append(RecommendedSolution.money(solution.monthlyAddonPrice)).append("/tháng");
            else out.append(" — giá hiện chưa có dữ liệu được xác nhận");
        }
        if (solution.totalMonthlyPrice != null) {
            if (solution.wifiCoverageOption != null && solution.monthlyAddonPrice != null)
                out.append("\n- Phép tính hàng tháng: ").append(RecommendedSolution.money(solution.monthlyInternetPrice)).append(" + ")
                    .append(RecommendedSolution.money(solution.monthlyAddonPrice)).append(" = ").append(RecommendedSolution.money(solution.totalMonthlyPrice)).append("/tháng");
            else out.append("\n- Tổng hàng tháng: ").append(RecommendedSolution.money(solution.totalMonthlyPrice)).append("/tháng");
        }
        else out.append("\n- Tổng hàng tháng: chưa thể cộng chính xác vì giá Mesh chưa được xác nhận");
        if (solution.internetPackage.camera) out.append("\n- Camera: gói nền gồm 1 Camera");
        else if (solution.wifiCoverageOption != null) out.append("\n- Camera: không gồm camera");
        if (solution.installationFee != null) out.append("\n- Phí lắp trước ưu đãi trả trước: ").append(RecommendedSolution.money(solution.installationFee)).append("/lần");
        else out.append("\n- Phí lắp: chưa có dữ liệu được xác nhận");
        if (solution.wifiCoverageOption != null && solution.installationFee != null)
            out.append(" (đã thay cho phí Internet thông thường, không cộng thêm ")
                .append(solution.standardInternetInstallationFee == null ? "300.000đ" : RecommendedSolution.money(solution.standardInternetInstallationFee)).append(")");
        if (solution.confirmedPromotion != null && solution.finalInstallationFee != null)
            out.append("\n- Ưu đãi đã chọn: ").append(solution.confirmedPromotion).append("; phí lắp còn ").append(RecommendedSolution.money(solution.finalInstallationFee)).append("/lần.");
        else if (confirmedPrepayPromotion(business)) out.append("\nƯu đãi đã được xác nhận ngày ").append(promotionDateDisplay(business))
            .append(": trả trước 6 tháng giảm ").append(RecommendedSolution.money(configuredMoney(business,"business.prepayInstallDiscount")))
            .append(" phí lắp; trả trước 12 tháng giảm ").append(RecommendedSolution.money(configuredMoney(business,"business.prepayInstallDiscount")))
            .append(" phí lắp và tặng ").append(configuredInteger(business,"business.prepay12BonusMonths")).append(" tháng dịch vụ. Chưa có ngày hết hiệu lực được xác nhận.");
        else out.append("\nƯu đãi trả trước chưa có cấu hình xác nhận nên chưa được áp dụng vào tổng.");
        if (solution.internetPackage.premierLeague)
            out.append(" Nội dung Ngoại Hạng Anh áp dụng theo quyền phát sóng và điều kiện còn hiệu lực.");
        out.append("\n\n").append("Phương án này cân bằng băng thông và vùng phủ; chất lượng thực tế còn phụ thuộc vị trí thiết bị, vật cản và số thiết bị dùng đồng thời.");
        if (memory.gaming) out.append(" Với chơi game, nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế.");
        if (memory.cameraCount != null && memory.cameraCount > 0) {
            out.append(" Nhu cầu ").append(memory.cameraCount).append(" camera cần được kiểm tra thêm về vị trí và mức dùng đồng thời");
            if (solution.internetPackage.camera) out.append("; gói này chỉ xác nhận kèm 1 Camera trong danh mục");
            out.append(".");
        }
        if (coverageSignal && solution.wifiCoverageOption == null && (!memory.meshPreferenceKnown || memory.meshWanted))
            out.append(" Anh/chị cho em biết số tầng để chọn phương án phủ sóng phù hợp nhé?");
        else if (memory.people == null && memory.devices == null && !memory.meshPreferenceKnown)
            out.append(" Anh/chị cho em biết số người hoặc số thiết bị thường dùng cùng lúc nhé?");
        if (alternative != null && solution.wifiCoverageOption != null && solution.monthlyAddonPrice != null) {
            Long alternativeMonthly = CommercialPriceValidator.monthlyTotal(alternative.price, solution.monthlyAddonPrice);
            out.append("\n\nNếu ưu tiên 1000Mbps, phương án thay thế là Gói ").append(alternative.label()).append(" + ")
                .append(solution.wifiCoverageOption.name).append(": ")
                .append(alternativeMonthly == null ? "chưa đủ dữ liệu để cộng" : RecommendedSolution.money(alternativeMonthly) + "/tháng")
                .append(", tổng phí lắp ").append(solution.installationFee == null ? "chưa được xác nhận" : RecommendedSolution.money(solution.installationFee)).append(".");
        }
        out.append("\nAnh/chị nghiêng về phương án nào ạ?");
        return out.toString();
    }
    /** A benefit question is hypothetical; it does not mutate consent or the selected package. */
    private static ChatResponse cameraAddition(String question,CustomerRequirements memory,ProductCatalog catalog) {
        String q=rag.RetrievalQuery.normalize(question);
        if(!q.matches("(?s).*(?:them camera|camera (?:thi )?sao|vay camera).*"))return null;
        ProductCatalog.Product base=memory.selectedPackageId==null || memory.selectionAmbiguous ? null : catalog.byId(memory.selectedPackageId);
        if(base==null && !memory.televisionKnown && memory.people==null && memory.floors==null && !q.contains("them camera"))return null;
        if(base!=null && base.camera)return reply("Dạ gói đang chọn đã có camera:\n"+base.display());
        StringBuilder text=new StringBuilder("Dạ nếu muốn thêm camera, anh/chị có thể xem các gói sau theo danh mục hiện tại:\n");int count=0;
        if(memory.floors!=null)text.append("Em vẫn ghi nhận nhà ").append(memory.floors).append(" tầng.\n");
        for(ProductCatalog.Product p:catalog.all())if(p.camera && (!memory.television || p.tv180) && (base==null || p.speed==base.speed)) {text.append(p.display()).append('\n');count++;}
        if(count==0)return reply("Dạ danh mục hiện tại chưa có lựa chọn camera phù hợp với yêu cầu đã chia sẻ. Anh/chị muốn em so sánh các gói khác không?");
        if(memory.budget!=null)text.append("Mình cần đối chiếu cước của từng gói với ngân sách đã chia sẻ trước khi chọn nhé.");
        return reply(text.toString().trim());
    }
    private static boolean fits(ProductCatalog.Product p, CustomerRequirements m) {
        return (m.budget == null || p.price <= m.budget)
            && (!m.camera || p.camera) && (!m.cameraKnown || m.camera || !p.camera)
            && (!m.television || p.tv180);
    }
    private static ChatResponse registration(String question, CustomerRequirements memory, ProductCatalog catalog, List<ProductCatalog.Product> mentioned) {
        String q = ProductCatalog.normalize(question);
        boolean explicitPackage = q.matches("(?s).*(?:goi\\s+|dang ky\\s+)[0-9]+.*");
        ProductCatalog.Product composite = compositePackage(question, memory, catalog);
        ProductCatalog.Product standalone = mentioned.size() == 1 ? mentioned.get(0) : null;
        boolean explicitCombo = q.matches("(?s).*(?:mesh|\\bf1\\b|\\bf2\\b|ban mesh|tong).*" );
        if (composite != null && standalone != null && standalone.id != composite.id && !explicitCombo) {
            if (memory.selectedPackageId != null && memory.selectedPackageId == standalone.id) composite = null;
            else if (memory.selectedPackageId == null || memory.selectedPackageId != composite.id) {
                memory.selectionAmbiguous = true;
                return reply("Mức " + standalone.label() + " có thể là hai phương án khác nhau: " + standalone.display()
                    + " Hoặc Gói " + composite.label() + " + Mesh " + memory.meshInterest + " = " + standalone.label()
                    + "/tháng" + (composite.camera ? " có camera" : " không camera") + ". Anh/chị muốn chọn gói độc lập hay bản Mesh ạ?");
            }
        }
        ProductCatalog.Product p = composite != null ? composite : mentioned.size() == 1 ? mentioned.get(0) : explicitPackage || mentioned.size() > 1 || memory.selectedPackageId == null || memory.selectionAmbiguous ? null : catalog.byId(memory.selectedPackageId);
        if (p == null) return reply("Anh/chị muốn đăng ký gói nào? Em sẽ mở biểu mẫu để mình kiểm tra và tự gửi yêu cầu.");
        memory.selectedPackageId = p.id;
        memory.selectionAmbiguous = false;
        RecommendedSolution.WifiCoverageOption mesh = memory.meshInterest == null ? null : meshOption(memory.meshInterest, catalog.business);
        Long monthly = CommercialPriceValidator.monthlyTotal(p.price, mesh == null ? null : mesh.monthlyAddonPrice);
        Long standard = p.standardInstallationFee > 0 ? p.standardInstallationFee : configuredMoney(catalog.business, "business.standardFee");
        Long installation = CommercialPriceValidator.installationTotal(standard, mesh == null ? null : mesh.installationFeeTotal);
        Long discount = configuredMoney(catalog.business, "business.prepayInstallDiscount");
        Long finalInstallation = CommercialPriceValidator.discountedInstallation(installation, discount,
            confirmedPrepayPromotion(catalog.business) && (Integer.valueOf(6).equals(memory.prepaymentMonths) || Integer.valueOf(12).equals(memory.prepaymentMonths)));
        StringBuilder summary = new StringBuilder("Dạ em xác nhận phương án Gói ").append(p.label());
        if (mesh != null) summary.append(" + Mesh ").append(mesh.id);
        summary.append(" mà anh/chị chọn:\n- Gói nền: ").append(p.display());
        if (mesh != null) {
            summary.append("\n- Mesh ").append(mesh.id).append(": ").append(mesh.equipment)
                .append(" — +").append(RecommendedSolution.money(mesh.monthlyAddonPrice)).append("/tháng")
                .append(p.camera ? "" : " — không gồm camera");
            summary.append("\n- Phép tính: ").append(RecommendedSolution.money(p.price)).append(" + ")
                .append(RecommendedSolution.money(mesh.monthlyAddonPrice)).append(" = ").append(RecommendedSolution.money(monthly)).append("/tháng");
        } else summary.append("\n- Cước hàng tháng: ").append(RecommendedSolution.money(monthly)).append("/tháng");
        if (installation != null) summary.append("\n- Phí lắp: ").append(RecommendedSolution.money(installation)).append("/lần");
        if (finalInstallation != null && !finalInstallation.equals(installation)) summary.append("; sau ưu đãi trả trước còn ").append(RecommendedSolution.money(finalInstallation)).append("/lần");
        summary.append(".\n\nAnh/chị có thể đăng ký theo 3 cách:\n1. Mở biểu mẫu đăng ký trên website và điền tên, SĐT/Zalo, địa chỉ lắp cùng gói đã chọn.\n2. Dùng link chính thức của ")
            .append(CONSULTANT_NAME).append(": ").append(REGISTRATION_URL).append("\n3. Chủ động liên hệ ").append(CONTACT).append("; giờ tư vấn trực tiếp ").append(HUMAN_HOURS).append(" (giờ Việt Nam).")
            .append("\nEm chỉ mở biểu mẫu để anh/chị tự kiểm tra và gửi. Em chưa gửi đăng ký, chưa lưu, chưa chuyển thông tin cho ").append(CONSULTANT_NAME).append(" và chưa xác nhận đơn hàng.");
        ChatResponse r = reply(summary.toString());
        r.setAction("open_registration"); r.setRecommendedPackageId(p.id); return r;
    }

    private static ProductCatalog.Product compositePackage(String question, CustomerRequirements memory, ProductCatalog catalog) {
        if (memory.meshInterest == null) return null;
        java.util.regex.Matcher amount = java.util.regex.Pattern.compile("(?<![0-9])(\\d{3})\\s*k\\b").matcher(ProductCatalog.normalize(question));
        if (!amount.find()) return null;
        long requested = Long.parseLong(amount.group(1)) * 1000L;
        RecommendedSolution.WifiCoverageOption mesh = meshOption(memory.meshInterest, catalog.business);
        if (mesh.monthlyAddonPrice == null) return null;
        ProductCatalog.Product found = null;
        for (ProductCatalog.Product candidate : catalog.all()) {
            if (CommercialPriceValidator.matchesMonthlyCombination(candidate.price, mesh.monthlyAddonPrice, requested)) {
                if (found != null) return null;
                found = candidate;
            }
        }
        return found;
    }
    public static ChatResponse render(ConsultationReply reply, String question, CustomerRequirements memory, ProductCatalog catalog) {
        // Model actions never establish customer consent.
        if (!"none".equals(reply.action)) throw new IllegalArgumentException("Unsolicited action");
        if ("registration".equals(reply.intent)) throw new IllegalArgumentException("Unsolicited registration");
        if ("out_of_scope".equals(reply.intent) && (ConversationSignals.greeting(question) || ConversationSignals.social(question) || ConversationSignals.playful(question) || ConversationSignals.service(question, memory, catalog))) throw new IllegalArgumentException("Incorrect scope classification");
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
        if (reply.recommendedPackageId != null) { memory.selectedPackageId = reply.recommendedPackageId; memory.selectionAmbiguous = false; }
        else if (ids.size() > 1) memory.selectionAmbiguous = true;
        return r;
    }
    public static ChatResponse fallback(String question, CustomerRequirements memory, ProductCatalog catalog) {
        ChatResponse known = known(question, memory, catalog);
        if (known != null) return known;
        return reply(facts(TECHNICAL_FAILURE,catalog.business));
    }
    private static ChatResponse unavailable() { return reply("Em chưa tải được danh mục gói cước hiện tại nên chưa thể xác nhận giá và quyền lợi. Anh/chị vui lòng thử lại hoặc gửi biểu mẫu tư vấn trên trang để được hỗ trợ."); }
    public static ChatResponse reply(String message) {
        ChatResponse r = new ChatResponse(message, "catalog", "", 0); r.setFinishReason("stop"); return r;
    }
    public static String facts(String message,Map<String,String> v){
        Map<String,String> replacements=new LinkedHashMap<>();
        replacements.put(ASSISTANT_NAME,v.get("business.assistantName"));
        replacements.put(CONSULTANT_NAME,v.get("business.consultantName"));
        replacements.put(REGISTRATION_URL,v.get("business.registrationUrl"));
        replacements.put(HUMAN_HOURS,v.get("business.humanHours"));
        replacements.put(CHATBOT_HOURS,v.get("business.chatbotHours"));
        String phone=v.get("business.phone"),zalo=v.get("business.zalo");
        String contact="https://zalo.me/".concat(phone).equals(zalo)?"điện thoại/Zalo ":"điện thoại ";
        contact+=phone.replaceFirst("([0-9]{4})([0-9]{3})([0-9]+)","$1 $2 $3");
        if(zalo!=null&&!zalo.isEmpty()&&!zalo.equals("https://zalo.me/"+phone))contact+=" hoặc Zalo "+zalo;
        replacements.put(CONTACT,contact);
        replacements.values().removeIf(java.util.Objects::isNull);
        StringBuilder pattern=new StringBuilder("(?<![0-9])(?:");for(String key:replacements.keySet()){if(pattern.charAt(pattern.length()-1)!=':')pattern.append('|');pattern.append(java.util.regex.Pattern.quote(key));}pattern.append(")(?![0-9])");
        java.util.regex.Matcher match=java.util.regex.Pattern.compile(pattern.toString()).matcher(message);StringBuffer out=new StringBuffer();while(match.find())match.appendReplacement(out,java.util.regex.Matcher.quoteReplacement(replacements.get(match.group())));match.appendTail(out);return out.toString();
    }
    public static com.google.gson.JsonArray meshOptionsJson(Map<String,String> v){
        com.google.gson.JsonArray a=new com.google.gson.JsonArray();
        for(int i=1;i<=2;i++){
            String id="F"+i,k="business.mesh"+id;
            com.google.gson.JsonObject o=new com.google.gson.JsonObject();
            o.addProperty("name","Mesh WiFi "+id);o.addProperty("floors",i==1?"Nhà 2 tầng":"Nhà 3 tầng trở lên");
            String equipment=v==null?null:v.get(k+"Equipment");if(equipment!=null)o.addProperty("devices",equipment);
            Long fee=configuredMoney(v,k+"Fee"),monthly=configuredMoney(v,k+"Monthly");
            if(fee!=null)o.addProperty("installationFeeVnd",fee);if(monthly!=null)o.addProperty("monthlyAddonVnd",monthly);
            o.addProperty("priceAuthority","approved_cms_business_config");a.add(o);
        }
        return a;
    }
}
