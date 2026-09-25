package controller;

import dao.UserDAO;
import dto.UserDTO;
import utils.TokenUtil;
import ultis.EmailUtility;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String identifier = request.getParameter("identifier");

        if (identifier == null || identifier.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập email hoặc số điện thoại!");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        identifier = identifier.trim();
        UserDTO user = userDAO.findByEmailOrPhone(identifier);

        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            request.setAttribute("message",
                "Nếu email/SĐT tồn tại trong hệ thống, chúng tôi đã gửi link đặt lại mật khẩu. "
                + "Vui lòng kiểm tra hộp thư (kể cả Spam).");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        try {
            String token = TokenUtil.generateToken();
            Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(15));

            boolean saved = userDAO.saveResetToken(user.getId(), token, expiresAt);

            if (!saved) {
                request.setAttribute("error", "Có lỗi xảy ra. Vui lòng thử lại sau!");
                request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
                return;
            }

            String resetLink = request.getScheme() + "://"
                    + request.getServerName() + ":"
                    + request.getServerPort()
                    + request.getContextPath()
                    + "/reset-password?token=" + token;

            EmailUtility.sendResetPasswordEmail(user.getEmail(), user.getDisplayName(), resetLink);

            System.out.println("📧 Reset link: " + resetLink);

            request.setAttribute("message",
                "Chúng tôi đã gửi link đặt lại mật khẩu đến email của bạn. "
                + "Link có hiệu lực trong 15 phút.");

            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Có lỗi xảy ra. Vui lòng thử lại sau!");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
        }
    }
}