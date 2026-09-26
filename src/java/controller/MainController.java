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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * MainController - Chỉ chứa các action để test hệ thống.
 */
public class MainController {

    public static void main(String[] args) {
        // ⭐ Test kết nối Supabase chi tiết
        testSupabaseConnection();

        // ⭐ Test lấy dữ liệu (bỏ comment nếu muốn test)
        // testGetAllPackages();
        // testGetAllCustomers();
        // testGetAllUsers();
        // testGetAllEmailLogs();
    }

    /**
     * ⭐ Test kết nối Supabase chi tiết
     */
    public static void testSupabaseConnection() {
        System.out.println("🚀 Bắt đầu test kết nối Supabase...\n");

        try (Connection conn = DBUtils.getConnection()) {
            System.out.println("✅ Kết nối Supabase thành công!");
            System.out.println("📌 Database: " + conn.getCatalog());
            System.out.println("📌 User:     " + conn.getMetaData().getUserName());
            System.out.println("📌 Driver:   " + conn.getMetaData().getDriverName()
                             + " v" + conn.getMetaData().getDriverVersion());
            System.out.println("📌 URL:      " + conn.getMetaData().getURL());
            System.out.println();

            // ⭐ Đếm số lượng trong các bảng
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) System.out.println("👥 Users:     " + rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM packages");
            if (rs.next()) System.out.println("📦 Packages:  " + rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM customers");
            if (rs.next()) System.out.println("👤 Customers: " + rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM email_logs");
            if (rs.next()) System.out.println("📧 EmailLogs: " + rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM settings");
            if (rs.next()) System.out.println("⚙️  Settings:  " + rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM api_key_history");
            if (rs.next()) System.out.println("🔑 ApiKey:    " + rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM password_reset_token");
            if (rs.next()) System.out.println("🔐 ResetToken:" + rs.getInt(1));

            System.out.println("\n🎉 TEST KẾT NỐI THÀNH CÔNG!");

        } catch (ClassNotFoundException e) {
            System.out.println("❌ Không tìm thấy driver PostgreSQL!");
            System.out.println("👉 Kiểm tra: đã add file postgresql-42.7.4.jar vào Libraries chưa?");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("❌ Lỗi kết nối: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test kết nối DB đơn giản (giữ lại cho tương thích)
     */
    public static void testConnection() {
        try (Connection conn = DBUtils.getConnection()) {
            System.out.println("✅ Kết nối Supabase PostgreSQL thành công! Database: " + conn.getCatalog());
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
        for (PackageDTO p : list) {
            System.out.println("   → " + p.getPackageCode() + " - " + p.getName()
                             + " - " + p.getPrice() + "đ - " + p.getSpeedMbps() + "Mbps");
        }
        return list;
    }

    public static List<CustomerDTO> testGetAllCustomers() {
        List<CustomerDTO> list = new CustomerDAO().getAll();
        System.out.println("👥 Tổng số khách hàng: " + list.size());
        for (CustomerDTO c : list) {
            System.out.println("   → #" + c.getId() + " - " + c.getFullName()
                             + " - " + c.getPhone() + " - " + c.getStatus());
        }
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
        for (UserDTO u : list) {
            System.out.println("   → #" + u.getId() + " - " + u.getUsername()
                             + " - " + u.getRole() + " - " + (u.isActive() ? "Active" : "Locked"));
        }
        return list;
    }

    /**
     * Thêm khách hàng mới
     */
    public static int testInsertCustomer(String fullName, String phone, String address,
                                         String email, String note, Integer consultantId) {
        CustomerDTO c = new CustomerDTO();
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setAddress(address);
        c.setEmail(email);
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

        String emailTo = email;
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