package controller;

import dao.UserDAO;
import dto.UserDTO;
import utils.TokenUtil;
import ultis.EmailUtility;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        if (email != null) {
            request.setAttribute("email", email.trim());
        }
        request.setAttribute("step", "email");
        request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "send_otp";
        }

        switch (action) {
            case "send_otp":
            case "resend_otp":
                handleSendOtp(request, response, action.equals("resend_otp"));
                break;

            case "verify_otp":
                handleVerifyOtp(request, response);
                break;

            default:
                request.setAttribute("step", "email");
                request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
                break;
        }
    }

    /**
     * Bước 1: Gửi hoặc gửi lại mã OTP qua Email
     */
    private void handleSendOtp(HttpServletRequest request, HttpServletResponse response, boolean isResend)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            email = request.getParameter("identifier");
        }

        if (email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập địa chỉ email của bạn!");
            request.setAttribute("step", "email");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        email = email.trim();

        // Kiểm tra định dạng email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            request.setAttribute("error", "Địa chỉ email không đúng định dạng!");
            request.setAttribute("email", email);
            request.setAttribute("step", "email");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        UserDTO user = userDAO.getByEmail(email);

        if (user == null) {
            request.setAttribute("error", "Email này chưa được đăng ký trong hệ thống FPT Telecom!");
            request.setAttribute("email", email);
            request.setAttribute("step", "email");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        try {
            // Tạo mã OTP 6 chữ số ngẫu nhiên
            int randomCode = ThreadLocalRandom.current().nextInt(100000, 1000000);
            String otp = String.valueOf(randomCode);

            // Thời hạn hiệu lực: 5 phút
            Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));

            boolean saved = userDAO.saveResetToken(user.getId(), otp, expiresAt);
            if (!saved) {
                request.setAttribute("error", "Không thể tạo mã OTP lúc này. Vui lòng thử lại sau!");
                request.setAttribute("email", email);
                request.setAttribute("step", "email");
                request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
                return;
            }

            // Gửi email OTP
            EmailUtility.sendOtpEmail(user.getEmail(), user.getDisplayName(), otp);

            request.setAttribute("step", "otp");
            request.setAttribute("email", user.getEmail());
            if (isResend) {
                request.setAttribute("message", "Mã xác thực OTP mới đã được gửi đến email " + user.getEmail() + ". Vui lòng kiểm tra hộp thư!");
            } else {
                request.setAttribute("message", "Mã xác thực OTP gồm 6 chữ số đã được gửi đến " + user.getEmail() + ". Mã có hiệu lực trong 5 phút.");
            }

            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Có lỗi xảy ra trong quá trình gửi OTP. Vui lòng thử lại!");
            request.setAttribute("email", email);
            request.setAttribute("step", "email");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
        }
    }

    /**
     * Bước 2: Xác nhận mã OTP do khách hàng nhập
     */
    private void handleVerifyOtp(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String otp = request.getParameter("otp");

        if (email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Phiên xác thực không hợp lệ. Vui lòng nhập lại email!");
            request.setAttribute("step", "email");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        email = email.trim();

        if (otp == null || otp.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập mã OTP 6 chữ số!");
            request.setAttribute("email", email);
            request.setAttribute("step", "otp");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        otp = otp.trim().replaceAll("\\s+", "");

        // Kiểm tra OTP hợp lệ và còn hạn
        UserDTO user = userDAO.findByValidTokenAndEmail(otp, email);

        if (user == null) {
            request.setAttribute("error", "Mã OTP không chính xác hoặc đã hết hạn (5 phút). Vui lòng thử lại!");
            request.setAttribute("email", email);
            request.setAttribute("step", "otp");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        try {
            // Đánh dấu OTP đã sử dụng
            userDAO.markTokenAsUsed(otp);

            // Sinh token bảo mật chuyển hướng sang trang đổi mật khẩu mới (hiệu lực 15 phút)
            String resetToken = TokenUtil.generateToken();
            Timestamp resetExpires = Timestamp.valueOf(LocalDateTime.now().plusMinutes(15));
            userDAO.saveResetToken(user.getId(), resetToken, resetExpires);

            // Chuyển hướng sang servlet đặt lại mật khẩu với token
            response.sendRedirect(request.getContextPath() + "/reset-password?token=" + resetToken);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Có lỗi xảy ra khi xác thực OTP. Vui lòng thử lại!");
            request.setAttribute("email", email);
            request.setAttribute("step", "otp");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
        }
    }
}