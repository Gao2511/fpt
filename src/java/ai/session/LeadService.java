package ai.session;

import com.google.gson.JsonObject;
import dao.CustomerDAO;
import dao.SettingsDAO;
import dto.CustomerDTO;
import utils.EmailUtility;

import java.util.regex.Pattern;

/**
 * Service xử lý tự động tạo Khách hàng tiềm năng (Lead) khi AI thu thập đủ thông tin
 */
public class LeadService {

    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final SettingsDAO settingsDAO = new SettingsDAO();

    // Regex kiểm tra số điện thoại Việt Nam hợp lệ (10 số, đầu 03, 05, 07, 08, 09 hoặc +84)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(0|\\+84)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$"
    );

    /**
     * Chuẩn hóa và làm sạch chuỗi số điện thoại
     */
    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null) return "";
        String cleaned = rawPhone.replaceAll("[^0-9+]", "").trim();
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        }
        return cleaned;
    }

    /**
     * Kiểm tra số điện thoại có hợp lệ không
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String normalized = normalizePhone(phone);
        return PHONE_PATTERN.matcher(normalized).matches();
    }

    /**
     * Kiểm tra và tự động tạo Lead nếu state đã đủ 4 thông tin: ten, sdt, dia_chi, goi_de_xuat
     */
    public static boolean checkAndCreateLead(ChatSessionData sessionData) {
        if (sessionData == null || sessionData.isLeadCreated()) {
            return false;
        }

        JsonObject state = sessionData.getState();
        String ten = state.has("ten") ? state.get("ten").getAsString().trim() : "";
        String sdt = state.has("sdt") ? state.get("sdt").getAsString().trim() : "";
        String diaChi = state.has("dia_chi") ? state.get("dia_chi").getAsString().trim() : "";
        String goiDeXuat = state.has("goi_de_xuat") ? state.get("goi_de_xuat").getAsString().trim() : "";

        // Kiểm tra điều kiện đủ 4 thông tin bắt buộc
        if (ten.isEmpty() || sdt.isEmpty() || diaChi.isEmpty() || goiDeXuat.isEmpty()) {
            return false;
        }

        String normalizedPhone = normalizePhone(sdt);
        if (!isValidPhone(normalizedPhone)) {
            System.out.println("⚠️ [LeadService] Số điện thoại chưa hợp lệ (" + sdt + "), bỏ qua tạo lead.");
            return false;
        }

        // Chống tạo trùng lặp: Kiểm tra trong DB xem SĐT này đã có đơn tư vấn chưa
        if (existsRecentLead(normalizedPhone, goiDeXuat)) {
            System.out.println("ℹ️ [LeadService] Khách hàng " + normalizedPhone + " đã tồn tại lead gần đây. Bỏ qua tạo trùng.");
            sessionData.setLeadCreated(true);
            return false;
        }

        try {
            CustomerDTO customer = new CustomerDTO();
            customer.setFullName(ten);
            customer.setPhone(normalizedPhone);
            customer.setAddress(diaChi);
            customer.setPackageInterest(goiDeXuat);
            customer.setStatus("Mới");
            customer.setNote("🤖 Khách hàng được AI Chatbot tư vấn tự động trên website fpt.gialai.vn (Session: " + sessionData.getSessionId() + ")");

            int newId = customerDAO.insert(customer);
            if (newId > 0) {
                sessionData.setLeadCreated(true);
                System.out.println(String.format(
                        "🎉 [AI Lead Generator] ĐÃ TỰ ĐỘNG TẠO LEAD THÀNH CÔNG! ID: %d | Tên: %s | SĐT: %s | Gói: %s | Địa chỉ: %s",
                        newId, ten, normalizedPhone, goiDeXuat, diaChi
                ));

                // Gửi thông báo email cho Admin
                notifyAdminNewLead(newId, ten, normalizedPhone, diaChi, goiDeXuat);
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ [LeadService] Lỗi khi tạo Lead: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Kiểm tra chống tạo trùng lead cho cùng một số điện thoại
     */
    private static boolean existsRecentLead(String phone, String packageInterest) {
        try {
            // Sử dụng hàm search sẵn có trong CustomerDAO
            java.util.List<CustomerDTO> existing = customerDAO.search(phone, null, null, null, 1, 5);
            if (existing != null && !existing.isEmpty()) {
                for (CustomerDTO c : existing) {
                    if (phone.equals(c.getPhone())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Gửi thông báo đến Admin khi có Lead mới từ AI Chatbot
     */
    private static void notifyAdminNewLead(int leadId, String name, String phone, String address, String packageInterest) {
        try {
            String adminEmail = settingsDAO.getValue("admin_notification_email");
            if (adminEmail == null || adminEmail.trim().isEmpty()) {
                adminEmail = settingsDAO.getValue("smtp_username"); // fallback
            }
            if (adminEmail == null || adminEmail.trim().isEmpty()) {
                adminEmail = "caoky2k5@gmail.com"; // default fallback
            }

            String subject = "🔔 [FPT AI Chatbot] Khách hàng mới đăng ký lắp đặt: " + name + " (" + phone + ")";
            String textBody = String.format(
                    "Xin chào Admin FPT Telecom,\n\n" +
                    "AI Chatbot vừa tiếp nhận và chốt thành công thông tin khách hàng mới từ website fpt.gialai.vn:\n" +
                    "- Mã khách hàng: #%d\n" +
                    "- Họ tên: %s\n" +
                    "- Số điện thoại: %s\n" +
                    "- Địa chỉ lắp đặt: %s\n" +
                    "- Gói cước đề xuất: %s\n" +
                    "- Trạng thái: Mới\n\n" +
                    "Vui lòng đăng nhập hệ thống quản trị để liên hệ tư vấn và khảo sát hạ tầng kịp thời cho khách hàng.\n\n" +
                    "Trân trọng,\nFPT Sale AI Assistant",
                    leadId, name, phone, address, packageInterest
            );

            EmailUtility.sendEmail(adminEmail, subject, textBody);
        } catch (Exception e) {
            System.err.println("⚠️ [LeadService] Lỗi gửi email thông báo lead cho admin: " + e.getMessage());
        }
    }
}
