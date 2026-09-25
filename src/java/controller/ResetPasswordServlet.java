package controller;

import dao.UserDAO;
import dto.UserDTO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        UserDTO user = userDAO.findByValidToken(token);
        if (user == null) {
            request.setAttribute("error",
                "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
            return;
        }

        request.setAttribute("token", token);
        request.getRequestDispatcher("/view/reset-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String token = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (token == null || token.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (newPassword == null || newPassword.length() < 6) {
            request.setAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự!");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/view/reset-password.jsp").forward(request, response);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp!");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/view/reset-password.jsp").forward(request, response);
            return;
        }

        try {
            UserDTO user = userDAO.findByValidToken(token);
            if (user == null) {
                request.setAttribute("error", "Link không hợp lệ hoặc đã hết hạn.");
                request.getRequestDispatcher("/view/forgot-password.jsp").forward(request, response);
                return;
            }

            userDAO.changePassword(user.getId(), newPassword);
            userDAO.markTokenAsUsed(token);

            response.sendRedirect(request.getContextPath() + "/login?reset=success");

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Có lỗi xảy ra. Vui lòng thử lại!");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/view/reset-password.jsp").forward(request, response);
        }
    }
}