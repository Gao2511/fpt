package controller;

import dao.CustomerDAO;
import dao.EmailLogDAO;
import dto.CustomerDTO;
import dto.UserDTO;
import utils.EmailUtility;
import utils.RegistrationSubmission;
import dao.PackageDAO;
import dto.PackageDTO;

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
    private static final String ADMIN_EMAIL = "nguyenphuc6403@gmail.com";

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

        // ===== 2. VALIDATE =====
        if (!RegistrationSubmission.validFields(userName, userPhone, userAddress, userEmail, userNote,
                request.getParameter("registration_consent"))) {
            request.getSession().setAttribute("message",
                "Vui lòng kiểm tra họ tên, số điện thoại, địa chỉ, email và đồng ý gửi thông tin tư vấn.");
            request.getSession().setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/home#contact");
            return;
        }

        // ===== 3. LẤY CONSULTANT ID =====
        HttpSession session = request.getSession(true);
        UserDTO currentUser = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        Integer userId = null;
if (currentUser != null) {
    userId = currentUser.getId();
}

        // ===== 4. LƯU VÀO DB =====
        CustomerDTO customer = new CustomerDTO();
        customer.setFullName(userName.trim());
        customer.setPhone(RegistrationSubmission.normalizedPhone(userPhone));
        customer.setEmail(userEmail != null ? userEmail.trim() : "");
        customer.setAddress(userAddress != null ? userAddress.trim() : "");
        customer.setNote(userNote != null ? userNote.trim() : "");
        customer.setConsultantId(userId);
        customer.setStatus("Mới");
        customer.setPackageInterest(userPackage);

        int customerId;
        synchronized (session) {
            RegistrationSubmission.Submission submission = RegistrationSubmission.find(session, request.getParameter("registration_token"));
            if (submission == null) {
                session.setAttribute("message", "Biểu mẫu đã hết hạn. Vui lòng tải lại trang và gửi lại.");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/home#contact"); return;
            }
            if (submission.customerId > 0) {
                session.setAttribute("message", "Yêu cầu tư vấn này đã được tiếp nhận. Không cần gửi lại.");
                session.setAttribute("messageType", "success");
                response.sendRedirect(request.getContextPath() + "/home#contact"); return;
            }
            String packageId = request.getParameter("user_package_id");
            if (packageId != null && !packageId.isEmpty()) {
                PackageDTO selected = packageId.matches("[1-9][0-9]{0,8}") ? findPackage(Integer.parseInt(packageId)) : null;
                if (selected == null) { invalidPackage(session, request, response); return; }
                userPackage = selected.getName();
            } else if (userPackage != null && !userPackage.trim().isEmpty()) {
                PackageDTO selected = null;
                for (PackageDTO p : new PackageDAO().getAll()) {
                    if (userPackage.trim().equals(p.getName()) || userPackage.trim().equals(p.getPackageCode())) { selected = p; break; }
                }
                if (selected == null) { invalidPackage(session, request, response); return; }
                userPackage = selected.getName();
            }
            customer.setPackageInterest(userPackage);
            customerId = saveCustomer(customer);
            if (customerId > 0) submission.customerId = customerId;
        }

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

        boolean emailSent = sendNotification(ADMIN_EMAIL, subject, body);

        // ===== 6. GHI LOG =====
        logNotification(emailSent, customerId, subject);

        // ===== 7. REDIRECT =====
        request.getSession().setAttribute("message",
            "Đã tiếp nhận yêu cầu tư vấn. FPT sẽ liên hệ để kiểm tra và tư vấn lắp đặt.");
        request.getSession().setAttribute("messageType", "success");

        response.sendRedirect(request.getContextPath() + "/home#contact");
    }

    protected int saveCustomer(CustomerDTO customer) { return customerDAO.insert(customer); }
    protected PackageDTO findPackage(int id) { return new PackageDAO().getById(id); }
    protected boolean sendNotification(String recipient, String subject, String body) { return EmailUtility.sendEmail(recipient, subject, body); }
    protected void logNotification(boolean sent, int customerId, String subject) {
        if (sent) emailLogDAO.insertSuccess(customerId, ADMIN_EMAIL, subject);
        else emailLogDAO.insertFailed(customerId, ADMIN_EMAIL, subject, "SMTP error");
    }

    private void invalidPackage(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        session.setAttribute("message", "Gói cước không hợp lệ. Vui lòng chọn lại trên bảng giá.");
        session.setAttribute("messageType", "error");
        response.sendRedirect(request.getContextPath() + "/home#contact");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/home");
    }
}
