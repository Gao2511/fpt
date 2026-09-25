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
 * FIX: Không bắt buộc username khi user Google cập nhật profile
 */
@WebServlet("/profile")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,   // 2MB
    maxFileSize       = 1024 * 1024 * 5,   // 5MB
    maxRequestSize    = 1024 * 1024 * 10   // 10MB
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
            session.setAttribute("user", freshUser);
        }

        // Flash message
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/customer/profile.jsp")
               .forward(request, response);
    }

    // =====================================================
    // POST: Cập nhật profile (KHÔNG RÀNG BUỘC USERNAME)
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

        // ===== ⭐ FIX: USERNAME =====
        // Nếu form KHÔNG có field username HOẶC rỗng → GIỮ NGUYÊN username cũ
        if (newUsername.isEmpty()) {
            newUsername = user.getUsername();
        }

        // Chỉ validate nếu user THỰC SỰ muốn đổi username
        boolean isChangingUsername = !newUsername.equals(user.getUsername());

        if (isChangingUsername) {
            // Validate độ dài
            if (newUsername.length() < 3 || newUsername.length() > 50) {
                session.setAttribute("message", "✗ Tên đăng nhập phải có 3-50 ký tự");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }

            // Không cho khoảng trắng
            if (newUsername.contains(" ")) {
                session.setAttribute("message", "✗ Tên đăng nhập không được chứa khoảng trắng");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }

            // Kiểm tra trùng username
            UserDTO existing = userDAO.getByUsername(newUsername);
            if (existing != null && existing.getId() != user.getId()) {
                session.setAttribute("message", "✗ Tên đăng nhập '" + newUsername + "' đã được sử dụng");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }
        }

        // ===== VALIDATE EMAIL (CHỈ validate nếu CÓ nhập) =====
        if (!email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                session.setAttribute("message", "✗ Email không hợp lệ");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }

            // ⭐ Kiểm tra email trùng với user khác
            UserDTO existing = userDAO.getByEmail(email);
            if (existing != null && existing.getId() != user.getId()) {
                session.setAttribute("message", "✗ Email này đã được sử dụng bởi tài khoản khác");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }
        }

        // ===== VALIDATE PHONE (CHỈ validate nếu CÓ nhập) =====
        if (!phone.isEmpty()) {
            String cleanPhone = phone.replaceAll("[\\s.\\-]", "");
            if (!cleanPhone.matches("^[0-9]{10,11}$")) {
                session.setAttribute("message", "✗ Số điện thoại không hợp lệ (10-11 số)");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }
        }

        // ===== XỬ LÝ UPLOAD AVATAR =====
        String avatarUrl = user.getAvatarUrl(); // giữ nguyên nếu không upload
        Part filePart = request.getPart("avatar");

        if (filePart != null && filePart.getSize() > 0) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String ext = "";
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx > 0) ext = fileName.substring(dotIdx).toLowerCase();

            // Chỉ cho phép ảnh
            if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
                session.setAttribute("message", "✗ Chỉ chấp nhận file ảnh (jpg, png, gif, webp)");
                session.setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }

            // Tạo tên file unique
            String newFileName = "avatar_" + user.getId() + "_" + UUID.randomUUID() + ext;

            // Đường dẫn thư mục thật
            String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            // Lưu file
            File saveFile = new File(uploadDir, newFileName);
            filePart.write(saveFile.getAbsolutePath());

            // Xóa avatar cũ nếu có
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                String oldPath = getServletContext().getRealPath("") + File.separator
                               + user.getAvatarUrl().replace("/", File.separator);
                File oldFile = new File(oldPath);
                if (oldFile.exists()) oldFile.delete();
            }

            avatarUrl = UPLOAD_DIR + "/" + newFileName;
        }

        // ===== UPDATE DB =====
        boolean ok = userDAO.updateProfile(
            user.getId(),
            newUsername,       // ⭐ Có thể là username cũ nếu không đổi
            fullName,
            phone,
            email,
            avatarUrl
        );

        if (ok) {
            // Reload user mới
            UserDTO updated = userDAO.getById(user.getId());
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