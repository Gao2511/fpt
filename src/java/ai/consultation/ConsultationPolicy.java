package ai.consultation;

import ai.dto.ChatResponse;
import java.util.*;

/** Server-owned business decisions and factual rendering also used during outages. */
public final class ConsultationPolicy {
    private ConsultationPolicy() {}
    public static final String CONTACT = "điện thoại/Zalo 0932 079 469";
    public static final String TECHNICAL_FAILURE = "Dạ em đang gặp chút trục trặc nên chưa thể trả lời chính xác lúc này. Anh/chị có thể thử lại hoặc liên hệ tư vấn viên qua " + CONTACT + " để được hỗ trợ nhé ạ.";
    public static boolean registrationRequested(String text) {
        String q = ProductCatalog.normalize(text);
        if (q.matches("(?s).*(?:khong|chua)\\s+(?:muon\\s+)?(?:dang ky|mua|chot).*")) return false;
        return q.matches("(?s).*(?:toi|minh|anh|chi|em)\\s+(?:muon|can|xin|se)\\s+(?:dang ky|mua|lap)(?:\\s|$).*") ||
            q.matches("^(?:cho (?:toi|minh|anh|chi|em) )?dang ky\\s+(?:goi|internet|mang|[0-9]).*" ) ||
            q.trim().matches("(?:oke?\\s+)?chot(?:\\s+luon|\\s+goi\\s+[0-9]+k?)?[.! ]*");
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
        if (ConversationSignals.handoff(question)) return reply("Dạ " + ConversationSignals.address(memory) + " có thể liên hệ tư vấn viên qua " + CONTACT + ". Em chưa gửi tin nhắn hoặc yêu cầu gọi lại; tư vấn viên sẽ kiểm tra thông tin cụ thể khi mình liên hệ.");
        if (ConversationSignals.greeting(question)) return reply(ConversationSignals.opening(memory) + "em đây ạ! " + ConversationSignals.address(memory) + " cần tìm gói mạng hay muốn hỏi thêm điều gì?");
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

    /** Returns one server-priced complete solution for coverage-related consultation. */
    public static ChatResponse combinedSolution(String question, CustomerRequirements memory, ProductCatalog catalog) {
        if (registrationRequested(question) || ConversationSignals.handoff(question)) return null;
        String q = ProductCatalog.normalize(question);
        if (q.contains("so sanh") || q.contains("tat ca") || q.contains("6 goi") || catalog.mentioned(question).size() > 1) return null;
        boolean currentCoverageSignal = q.matches("(?s).*(?:[2-9]\\s*tang|hai tang|ba tang|bon tang|mesh|wifi yeu|song yeu|tang tren|nhieu phong|tuong day|tuong be tong|phong xa|goc chet).*" );
        boolean coverageSignal = currentCoverageSignal
            || memory.floors != null && memory.floors >= 2 && memory.requirementsChanged;
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
            reason, memory.people == null && memory.devices == null ? "preliminary" : "requirements_based", sources);
        memory.selectedPackageId = selected.id;
        memory.selectionAmbiguous = false;
        ChatResponse response = reply(renderSolution(solution, memory, coverageSignal));
        response.setRecommendedPackageId(selected.id);
        return response;
    }

    private static ProductCatalog.Product chooseForSolution(CustomerRequirements memory, ProductCatalog catalog) {
        int minimumSpeed = 0;
        if (memory.businessUse || memory.cameraCount != null && memory.cameraCount >= 4
                || memory.people != null && memory.people >= 8 || memory.devices != null && memory.devices >= 12) minimumSpeed = 1000;
        else if (memory.gaming || memory.streaming || memory.cameraCount != null && memory.cameraCount >= 2
                || memory.people != null && memory.people >= 5 || memory.devices != null && memory.devices >= 8) minimumSpeed = 500;
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

    private static String renderSolution(RecommendedSolution solution, CustomerRequirements memory, boolean coverageSignal) {
        StringBuilder out = new StringBuilder("Dạ với ").append(solution.reason).append(", em ưu tiên phương án ")
            .append("Gói ").append(solution.internetPackage.label());
        if (solution.wifiCoverageOption != null) out.append(" + ").append(solution.wifiCoverageOption.name);
        out.append(".\n\n- Internet: ").append(RecommendedSolution.money(solution.monthlyInternetPrice)).append("/tháng — ")
            .append(solution.internetPackage.speed).append("Mbps");
        if (solution.wifiCoverageOption != null) {
            out.append("\n- Phủ sóng: ").append(solution.wifiCoverageOption.name);
            if (solution.wifiCoverageOption.equipment != null && !solution.wifiCoverageOption.equipment.trim().isEmpty())
                out.append(" (").append(solution.wifiCoverageOption.equipment.trim()).append(")");
            if (solution.monthlyAddonPrice != null) out.append(" — +").append(RecommendedSolution.money(solution.monthlyAddonPrice)).append("/tháng");
            else out.append(" — giá hiện chưa có dữ liệu được xác nhận");
        }
        if (solution.totalMonthlyPrice != null) out.append("\n- Tổng hàng tháng: ").append(RecommendedSolution.money(solution.totalMonthlyPrice)).append("/tháng");
        else out.append("\n- Tổng hàng tháng: chưa thể cộng chính xác vì giá Mesh chưa được xác nhận");
        if (solution.internetInstallationFee != null) out.append("\n- Phí lắp Internet thông thường: ").append(RecommendedSolution.money(solution.internetInstallationFee)).append("/lần");
        if (solution.wifiCoverageOption != null) {
            if (solution.oneTimeDeviceFee != null) out.append("\n- Phí lắp Mesh: ").append(RecommendedSolution.money(solution.oneTimeDeviceFee)).append("/lần");
            else out.append("\n- Phí lắp Mesh: chưa có dữ liệu được xác nhận");
            if (solution.initialCost != null) out.append("\n- Tổng phí một lần: ").append(RecommendedSolution.money(solution.initialCost));
        }
        if (solution.internetInstallationFee != null || solution.oneTimeDeviceFee != null)
            out.append("\nPhí một lần ở trên là mức thông thường; chính sách miễn giảm chỉ áp dụng khi được hệ thống xác nhận.");
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
        return (m.budget == null || p.price <= m.budget) && (!m.camera || p.camera) && (!m.television || p.tv180);
    }
    private static ChatResponse registration(String question, CustomerRequirements memory, ProductCatalog catalog, List<ProductCatalog.Product> mentioned) {
        boolean explicitPackage = ProductCatalog.normalize(question).matches("(?s).*(?:goi\\s+|dang ky\\s+)[0-9]+.*");
        ProductCatalog.Product p = mentioned.size() == 1 ? mentioned.get(0) : explicitPackage || mentioned.size() > 1 || memory.selectedPackageId == null || memory.selectionAmbiguous ? null : catalog.byId(memory.selectedPackageId);
        if (p == null) return reply("Anh/chị muốn đăng ký gói nào? Em sẽ mở biểu mẫu để mình kiểm tra và tự gửi yêu cầu.");
        memory.selectedPackageId = p.id;
        memory.selectionAmbiguous = false;
        ChatResponse r = reply("Anh/chị chọn " + p.display() + "\nVui lòng mở biểu mẫu bên dưới, kiểm tra thông tin và bấm gửi yêu cầu tư vấn. Em chưa gửi đăng ký hoặc xác nhận lắp đặt.");
        r.setAction("open_registration"); r.setRecommendedPackageId(p.id); return r;
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
        String phone=v.get("business.phone"),zalo=v.get("business.zalo");
        String contact="https://zalo.me/".concat(phone).equals(zalo)?"điện thoại/Zalo ":"điện thoại ";
        contact+=phone.replaceFirst("([0-9]{4})([0-9]{3})([0-9]+)","$1 $2 $3");
        if(zalo!=null&&!zalo.isEmpty()&&!zalo.equals("https://zalo.me/"+phone))contact+=" hoặc Zalo "+zalo;
        replacements.put(CONTACT,contact);
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
