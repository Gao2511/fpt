package controller;

import dao.UserDAO;
import dto.UserDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * ProfileServlet - Xem & chỉnh sửa hồ sơ cá nhân
 * URL: /profile
 * 
 * ĐÃ BỎ HẾT RÀNG BUỘC — không bắt buộc nhập gì cả
 */
@WebServlet("/profile")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,
    maxFileSize       = 1024 * 1024 * 5,
    maxRequestSize    = 1024 * 1024 * 10
)
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private static final String UPLOAD_DIR = "assets/uploads/avatars";

    // =====================================================
    // GET: Hiển thị form profile
    // =====================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Reload từ DB để lấy data mới nhất
        UserDTO freshUser = userDAO.getById(user.getId());
        if (freshUser != null) {
            freshUser.setPassword(null);
            session.setAttribute("user", freshUser);
        }

        // Flash message
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.setAttribute("csrfToken",security.AdminSecurity.token(session));
        request.getRequestDispatcher("/view/customer/profile.jsp")
               .forward(request, response);
    }

    // =====================================================
    // POST: Cập nhật profile — KHÔNG RÀNG BUỘC GÌ CẢ
    // =====================================================
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

        if(!security.AdminSecurity.csrf(request)){response.sendError(403);return;}
        // ===== LẤY PARAMS =====
        String newUsername = request.getParameter("username");
        String fullName    = request.getParameter("fullName");
        String phone       = request.getParameter("phone");
        String email       = request.getParameter("email");

        // ===== CHUẨN HÓA (null → "") =====
        if (newUsername == null) newUsername = "";
        if (fullName    == null) fullName    = "";
        if (phone       == null) phone       = "";
        if (email       == null) email       = "";

        newUsername = newUsername.trim();
        fullName    = fullName.trim();
        phone       = phone.trim();
        email       = email.trim();

        // ===== USERNAME =====
        // Nếu không nhập → giữ nguyên username cũ
        if (newUsername.isEmpty()) {
            newUsername = user.getUsername();
        }

        // ===== EMAIL =====
        // Nếu không nhập → giữ nguyên email cũ
        if (email.isEmpty()) {
            email = user.getEmail();
        }

        // ===== XỬ LÝ UPLOAD AVATAR =====
        String avatarUrl = user.getAvatarUrl();
        Part filePart = request.getPart("avatar");

        if (filePart != null && filePart.getSize() > 0) {
            try(java.io.InputStream input=filePart.getInputStream()) {avatarUrl=new cms.MediaStore().save(cms.MediaStore.validate(input),user.getId()).substring(1);}
            catch(Exception invalid){session.setAttribute("message","Không thể lưu ảnh. Chọn PNG/JPEG tối đa 2 MB và kiểm tra kho ảnh.");response.sendRedirect(request.getContextPath()+"/profile");return;}
        }

        // ===== UPDATE DB =====
        boolean ok = userDAO.updateProfile(
            user.getId(),
            newUsername,
            fullName,
            phone,
            email,
            avatarUrl
        );

        if (ok) {
            UserDTO updated = userDAO.getById(user.getId());
            if(updated!=null)updated.setPassword(null);
            session.setAttribute("user", updated);
            session.setAttribute("message", "✓ Cập nhật hồ sơ thành công!");
            session.setAttribute("messageType", "success");
        } else {
            session.setAttribute("message", "✗ Cập nhật thất bại. Vui lòng thử lại.");
            session.setAttribute("messageType", "error");
        }

        response.sendRedirect(request.getContextPath() + "/profile");
    }
}
