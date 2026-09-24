package controller;

import dao.CustomerDAO;
import dao.EmailLogDAO;
import dto.CustomerDTO;
import dto.UserDTO;
import ultis.EmailUtility;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * ContactServlet - Xử lý form đăng ký tư vấn
 * URL: /ContactServlet
 */
@WebServlet("/ContactServlet")
public class ContactServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final EmailLogDAO emailLogDAO = new EmailLogDAO();

    // ====================================================
    // MAIL ADMIN - Gmail dùng để NHẬN thông báo
    // ====================================================
    private static final String ADMIN_EMAIL = "nguyenhacaoky@gmail.com";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        // ===== 1. LẤY PARAMS =====
        String userName    = request.getParameter("user_name");
        String userPhone   = request.getParameter("user_phone");
        String userEmail   = request.getParameter("user_email");
        String userAddress = request.getParameter("user_address");
        String userNote    = request.getParameter("user_note");
        String userPackage = request.getParameter("user_package");

        System.out.println("[ContactServlet] Nhan form:");
        System.out.println("   - user_name    = " + userName);
        System.out.println("   - user_phone   = " + userPhone);
        System.out.println("   - user_email   = " + userEmail);

        // ===== 2. VALIDATE =====
        if (userName == null || userName.trim().isEmpty()
                || userPhone == null || userPhone.trim().isEmpty()) {
            request.getSession().setAttribute("message",
                "Vui lòng nhập đầy đủ Họ tên và Số điện thoại");
            request.getSession().setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/home#contact");
            return;
        }

        // ===== 3. LẤY CONSULTANT ID =====
        HttpSession session = request.getSession(false);
        UserDTO currentUser = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        Integer userId = null;
if (currentUser != null) {
    userId = currentUser.getId();
}

        // ===== 4. LƯU VÀO DB =====
        CustomerDTO customer = new CustomerDTO();
        customer.setFullName(userName.trim());
        customer.setPhone(userPhone.trim());
        customer.setEmail(userEmail != null ? userEmail.trim() : "");
        customer.setAddress(userAddress != null ? userAddress.trim() : "");
        customer.setNote(userNote != null ? userNote.trim() : "");
        customer.setConsultantId(userId);
        customer.setStatus("Mới");
        customer.setPackageInterest(userPackage);

        int customerId = customerDAO.insert(customer);

        if (customerId <= 0) {
            request.getSession().setAttribute("message",
                "Có lỗi xảy ra. Vui lòng thử lại sau.");
            request.getSession().setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/home#contact");
            return;
        }

        System.out.println("Da luu khach hang #KH-" + customerId);

        // ===== 5. GỬI EMAIL CHO ADMIN =====
        String subject = "Khách hàng mới đăng ký tư vấn: " + userName;

        String emailDisplay   = (userEmail != null && !userEmail.trim().isEmpty()) 
                                ? userEmail.trim() : "(Khách không cung cấp email)";
        String addressDisplay = (userAddress != null && !userAddress.trim().isEmpty()) 
                                ? userAddress.trim() : "(Khách không cung cấp địa chỉ)";
        String packageDisplay = (userPackage != null && !userPackage.trim().isEmpty()) 
                                ? userPackage.trim() : "(Khách không chọn gói)";
        String noteDisplay    = (userNote != null && !userNote.trim().isEmpty()) 
                                ? userNote.trim() : "(Khách không để lại ghi chú)";

        String body = "Xin chào Admin,\n\n"
                    + "Có khách hàng mới đăng ký tư vấn trên website:\n\n"
                    + "------------------------------\n"
                    + "Mã khách hàng: #KH-" + customerId + "\n"
                    + "Họ tên: " + userName + "\n"
                    + "Số điện thoại: " + userPhone + "\n"
                    + "Email: " + emailDisplay + "\n"
                    + "Địa chỉ: " + addressDisplay + "\n"
                    + "Gói quan tâm: " + packageDisplay + "\n"
                    + "Ghi chú: " + noteDisplay + "\n"
                    + "Thời gian: " + new java.util.Date() + "\n"
                    + "------------------------------\n\n"
                    + "Vui lòng liên hệ khách hàng sớm nhất có thể.\n\n"
                    + "Trân trọng,\n"
                    + "Hệ thống FPT Sale Manager";

        boolean emailSent = EmailUtility.sendEmail(ADMIN_EMAIL, subject, body);

        // ===== 6. GHI LOG =====
        if (emailSent) {
            System.out.println("Da gui email thong bao den admin");
            emailLogDAO.insertSuccess(customerId, ADMIN_EMAIL, subject);
        } else {
            System.err.println("Gui email that bai");
            emailLogDAO.insertFailed(customerId, ADMIN_EMAIL, subject, "SMTP error");
        }

        // ===== 7. REDIRECT =====
        request.getSession().setAttribute("message",
            "Đăng ký thành công! Chúng tôi sẽ liên hệ bạn trong thời gian sớm nhất.");
        request.getSession().setAttribute("messageType", "success");

        response.sendRedirect(request.getContextPath() + "/home#contact");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/home");
    }
}