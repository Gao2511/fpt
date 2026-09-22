package controller;

import dao.UserDAO;
import dto.UserDTO;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * AdminUserServlet - Quản lý tài khoản
 * URL: /admin/users
 * 
 * Chức năng:
 * - Danh sách + tìm kiếm + lọc + phân trang
 * - Khóa tài khoản
 * - Mở lại tài khoản
 * - Xóa tài khoản
 * - Đổi mật khẩu
 */
@WebServlet("/admin/users")
public class AdminUserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private static final int PAGE_SIZE = 10;

    // =========================================================
    // DO GET
    // =========================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ⭐ KHÓA
        if ("lock".equals(action)) {
            handleLock(request, response);
            return;
        }

        // ⭐ MỞ LẠI
        if ("unlock".equals(action)) {
            handleUnlock(request, response);
            return;
        }

        // ⭐ XÓA
        if ("delete".equals(action)) {
            handleDelete(request, response);
            return;
        }

        // ⭐ FORM ĐỔI MẬT KHẨU
        if ("change-password".equals(action)) {
            handleChangePasswordForm(request, response);
            return;
        }

        // ===== DANH SÁCH =====
        showList(request, response);
    }

    // =========================================================
    // DO POST
    // =========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ⭐ XỬ LÝ ĐỔI MẬT KHẨU
        if ("update-password".equals(action)) {
            handleUpdatePassword(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    // =========================================================
    // ACTION HANDLERS
    // =========================================================

    private void handleLock(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String idStr = request.getParameter("id");
        if (idStr != null) {
            try {
                int id = Integer.parseInt(idStr);
                UserDTO target = userDAO.getById(id);

                UserDTO currentUser = (UserDTO) request.getSession().getAttribute("user");
                if (currentUser != null && currentUser.getId() == id) {
                    setMessage(request, "✗ Bạn không thể khóa tài khoản của chính mình", "error");
                } else if (userDAO.lockAccount(id)) {
                    setMessage(request, "✓ Đã khóa tài khoản #" + id + " (" + target.getDisplayName() + ")", "success");
                } else {
                    setMessage(request, "✗ Khóa tài khoản thất bại", "error");
                }
            } catch (NumberFormatException ignored) {}
        }
        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    private void handleUnlock(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String idStr = request.getParameter("id");
        if (idStr != null) {
            try {
                int id = Integer.parseInt(idStr);
                UserDTO target = userDAO.getById(id);
                if (userDAO.unlockAccount(id)) {
                    setMessage(request, "✓ Đã mở lại tài khoản #" + id + " (" + target.getDisplayName() + ")", "success");
                } else {
                    setMessage(request, "✗ Mở lại tài khoản thất bại", "error");
                }
            } catch (NumberFormatException ignored) {}
        }
        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String idStr = request.getParameter("id");
        if (idStr != null) {
            try {
                int id = Integer.parseInt(idStr);
                UserDTO target = userDAO.getById(id);

                UserDTO currentUser = (UserDTO) request.getSession().getAttribute("user");
                if (currentUser != null && currentUser.getId() == id) {
                    setMessage(request, "✗ Bạn không thể xóa tài khoản của chính mình", "error");
                } else if (target != null && "admin".equals(target.getRole())) {
                    setMessage(request, "✗ Không thể xóa tài khoản admin", "error");
                } else if (userDAO.delete(id)) {
                    setMessage(request, "✓ Đã xóa tài khoản #" + id + " (" + target.getDisplayName() + ")", "success");
                } else {
                    setMessage(request, "✗ Xóa tài khoản thất bại", "error");
                }
            } catch (NumberFormatException ignored) {}
        }
        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    private void handleChangePasswordForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.sendRedirect(request.getContextPath() + "/admin/users");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            UserDTO target = userDAO.getById(id);
            if (target == null) {
                setMessage(request, "✗ Không tìm thấy tài khoản", "error");
                response.sendRedirect(request.getContextPath() + "/admin/users");
                return;
            }

            request.setAttribute("targetUser", target);

            // Flash message
            HttpSession session = request.getSession();
            if (session.getAttribute("message") != null) {
                request.setAttribute("message", session.getAttribute("message"));
                request.setAttribute("messageType", session.getAttribute("messageType"));
                session.removeAttribute("message");
                session.removeAttribute("messageType");
            }

            request.getRequestDispatcher("/view/admin/user-change-password.jsp")
                   .forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/users");
        }
    }

    private void handleUpdatePassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String idStr = request.getParameter("id");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // Validate
        if (idStr == null || newPassword == null || confirmPassword == null
                || newPassword.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            setMessage(request, "✗ Vui lòng nhập đầy đủ thông tin", "error");
            response.sendRedirect(request.getContextPath() + "/admin/users?action=change-password&id=" + idStr);
            return;
        }

        if (newPassword.length() < 6) {
            setMessage(request, "✗ Mật khẩu mới phải có ít nhất 6 ký tự", "error");
            response.sendRedirect(request.getContextPath() + "/admin/users?action=change-password&id=" + idStr);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            setMessage(request, "✗ Xác nhận mật khẩu không khớp", "error");
            response.sendRedirect(request.getContextPath() + "/admin/users?action=change-password&id=" + idStr);
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            UserDTO target = userDAO.getById(id);
            if (target == null) {
                setMessage(request, "✗ Không tìm thấy tài khoản", "error");
                response.sendRedirect(request.getContextPath() + "/admin/users");
                return;
            }

            if (userDAO.changePassword(id, newPassword)) {
                setMessage(request, "✓ Đã đổi mật khẩu tài khoản #" + id + " (" + target.getDisplayName() + ")", "success");
                response.sendRedirect(request.getContextPath() + "/admin/users");
            } else {
                setMessage(request, "✗ Đổi mật khẩu thất bại", "error");
                response.sendRedirect(request.getContextPath() + "/admin/users?action=change-password&id=" + idStr);
            }

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/users");
        }
    }

    // =========================================================
    // DANH SÁCH
    // =========================================================

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String keyword = request.getParameter("keyword");
        String status = request.getParameter("status");

        int page = 1;
        String pageStr = request.getParameter("page");
        if (pageStr != null) {
            try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;

        List<UserDTO> users = userDAO.search(keyword, status, page, PAGE_SIZE);
        int total = userDAO.countSearch(keyword, status);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;

        int countAll = userDAO.countAll();
        int countActive = userDAO.countActive();
        int countLocked = userDAO.countLocked();

        request.setAttribute("users", users);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", total);
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("keyword", keyword);
        request.setAttribute("status", status);
        request.setAttribute("countAll", countAll);
        request.setAttribute("countActive", countActive);
        request.setAttribute("countLocked", countLocked);

        HttpSession session = request.getSession();
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/admin/user-list.jsp")
               .forward(request, response);
    }

    private void setMessage(HttpServletRequest request, String msg, String type) {
        request.getSession().setAttribute("message", msg);
        request.getSession().setAttribute("messageType", type);
    }
}