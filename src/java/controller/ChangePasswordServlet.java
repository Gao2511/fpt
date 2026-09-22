package controller;

import dao.UserDAO;
import dto.UserDTO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * ChangePasswordServlet - Đổi mật khẩu
 * URL: /change-password
 */
@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/customer/change-password.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String currentPwd = request.getParameter("currentPassword");
        String newPwd     = request.getParameter("newPassword");
        String confirmPwd = request.getParameter("confirmPassword");

        // ===== VALIDATE =====
        if (currentPwd == null || currentPwd.trim().isEmpty()
            || newPwd == null || newPwd.trim().isEmpty()
            || confirmPwd == null || confirmPwd.trim().isEmpty()) {
            session.setAttribute("message", "✗ Vui lòng nhập đầy đủ thông tin");
            session.setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        if (!userDAO.checkPassword(user.getId(), currentPwd)) {
            session.setAttribute("message", "✗ Mật khẩu hiện tại không đúng");
            session.setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        if (newPwd.length() < 8) {
            session.setAttribute("message", "✗ Mật khẩu mới phải có ít nhất 8 ký tự");
            session.setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            session.setAttribute("message", "✗ Xác nhận mật khẩu không khớp");
            session.setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        if (newPwd.equals(currentPwd)) {
            session.setAttribute("message", "✗ Mật khẩu mới phải khác mật khẩu hiện tại");
            session.setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        // ===== UPDATE =====
        boolean ok = userDAO.changePassword(user.getId(), newPwd);

        if (ok) {
            user.setPassword(newPwd);
            session.setAttribute("user", user);

            session.setAttribute("message", "✓ Đổi mật khẩu thành công!");
            session.setAttribute("messageType", "success");
            response.sendRedirect(request.getContextPath() + "/profile");
        } else {
            session.setAttribute("message", "✗ Đổi mật khẩu thất bại. Vui lòng thử lại.");
            session.setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/change-password");
        }
    }
}