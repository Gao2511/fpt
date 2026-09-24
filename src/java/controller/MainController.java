package controller;

import dao.CustomerDAO;
import dao.EmailLogDAO;
import dao.PackageDAO;
import dao.UserDAO;
import dto.CustomerDTO;
import dto.EmailLogDTO;
import dto.PackageDTO;
import dto.UserDTO;
import utils.DBUtils;
import ultis.EmailUtility;

import java.sql.Connection;
import java.util.List;

/**
 * MainController - Chỉ chứa các action để test hệ thống.
 */
public class MainController {

    public static void main(String[] args) {
        testConnection();
    }

    public static void testConnection() {
        try (Connection conn = DBUtils.getConnection()) {
            System.out.println("✅ Kết nối SQL Server thành công! Database: " + conn.getCatalog());
        } catch (Exception e) {
            System.out.println("❌ Kết nối thất bại: " + e.getMessage());
        }
    }

    public static boolean testSendEmail(String to, String subject, String body) {
        boolean ok = EmailUtility.sendEmail(to, subject, body);
        System.out.println(ok ? "✅ Gửi mail thành công" : "❌ Gửi mail thất bại");
        return ok;
    }

    public static List<PackageDTO> testGetAllPackages() {
        List<PackageDTO> list = new PackageDAO().getAll();
        System.out.println("📦 Tổng số gói cước: " + list.size());
        return list;
    }

    public static List<CustomerDTO> testGetAllCustomers() {
        List<CustomerDTO> list = new CustomerDAO().getAll();
        System.out.println("👥 Tổng số khách hàng: " + list.size());
        return list;
    }

    public static List<EmailLogDTO> testGetAllEmailLogs() {
        List<EmailLogDTO> list = new EmailLogDAO().getAll();
        System.out.println("📧 Tổng số log email: " + list.size());
        return list;
    }

    public static List<UserDTO> testGetAllUsers() {
        List<UserDTO> list = new UserDAO().getAll();
        System.out.println("🔑 Tổng số tài khoản: " + list.size());
        return list;
    }

    /**
     * Thêm khách hàng mới (đã bỏ packageInterest, thay bằng email)
     */
    public static int testInsertCustomer(String fullName, String phone, String address,
                                         String email, String note, Integer consultantId) {
        CustomerDTO c = new CustomerDTO();
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setAddress(address);
        c.setEmail(email);              // ← ĐÃ ĐỔI
        c.setNote(note);
        c.setConsultantId(consultantId);
        c.setStatus("Mới");

        int id = new CustomerDAO().insert(c);
        System.out.println(id > 0 ? "✅ Thêm khách hàng thành công, ID = " + id
                                  : "❌ Thêm khách hàng thất bại");
        return id;
    }

    public static boolean testFullFlow(String fullName, String phone, String address,
                                       String email, String note, Integer consultantId) {

        int customerId = testInsertCustomer(fullName, phone, address, email, note, consultantId);
        boolean isSaved = customerId > 0;

        String emailTo = email;  // ← Gửi cho chính khách
        String subject = "Khách hàng mới: " + fullName;
        String body = "Họ tên: " + fullName + "\n"
                    + "SĐT: " + phone + "\n"
                    + "Địa chỉ: " + address + "\n"
                    + "Email: " + email + "\n"
                    + "Ghi chú: " + note;

        boolean isSent = EmailUtility.sendEmail(emailTo, subject, body);
        System.out.println(isSent ? "✅ Gửi mail thành công" : "❌ Gửi mail thất bại");

        if (isSaved) {
            EmailLogDAO logDAO = new EmailLogDAO();
            boolean logOk = isSent
                    ? logDAO.insertSuccess(customerId, emailTo, subject)
                    : logDAO.insertFailed(customerId, emailTo, subject, "SMTP error");
            System.out.println(logOk ? "✅ Ghi log thành công" : "❌ Ghi log thất bại");
        }

        boolean result = isSaved && isSent;
        System.out.println(result ? "🎉 FULL FLOW THÀNH CÔNG!" : "⚠️ Có bước bị lỗi");
        return result;
    }
}