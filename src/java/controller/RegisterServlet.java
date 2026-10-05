package controller;

import dao.UserDAO;
import dto.UserDTO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    /** Hiển thị trang đăng ký */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/view/register.jsp").forward(request, response);
    }

    /** Xử lý đăng ký */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String fullName = request.getParameter("fullName");
        String phone    = request.getParameter("phone");
        String email    = request.getParameter("email");
        String password = request.getParameter("password");
        String confirm  = request.getParameter("confirmPassword");
        String agree    = request.getParameter("agree");

        // Giữ lại dữ liệu đã nhập
        request.setAttribute("fullName", fullName);
        request.setAttribute("phone", phone);
        request.setAttribute("email", email);

        // Chuẩn hóa
        if (fullName != null) fullName = fullName.trim();
        if (phone != null)    phone = phone.trim();
        if (email != null)    email = email.trim().toLowerCase();  // ⭐ lowercase email

        // ===== VALIDATE =====

        // 1. Họ tên
        if (fullName == null || fullName.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập Họ và tên.");
            forward(request, response);
            return;
        }

        // 2. Email (Bắt buộc để đăng nhập)
        if (email == null || email.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập địa chỉ Email.");
            forward(request, response);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            request.setAttribute("error", "Địa chỉ Email không đúng định dạng.");
            forward(request, response);
            return;
        }

        // 3. Số điện thoại (Bắt buộc)
        if (phone == null || phone.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập Số điện thoại liên hệ.");
            forward(request, response);
            return;
        }

        String cleanPhone = phone.replaceAll("[\\s.\\-]", "");
        if (!cleanPhone.matches("^[0-9]{10,11}$")) {
            request.setAttribute("error", "Số điện thoại không hợp lệ (phải gồm 10-11 chữ số).");
            forward(request, response);
            return;
        }
        phone = cleanPhone;

        // 4. Mật khẩu
        if (password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập mật khẩu.");
            forward(request, response);
            return;
        }

        if (password.length() < 8) {
            request.setAttribute("error", "Mật khẩu phải có ít nhất 8 ký tự.");
            forward(request, response);
            return;
        }

        if (!password.equals(confirm)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            forward(request, response);
            return;
        }

        // 5. Điều khoản
        if (agree == null) {
            request.setAttribute("error", "Bạn cần đồng ý với Điều khoản dịch vụ & Chính sách của FPT.");
            forward(request, response);
            return;
        }

        // =========================================================
        // ⭐ KIỂM TRA TRÙNG EMAIL — PHÂN BIỆT RÕ TÀI KHOẢN GOOGLE
        // =========================================================
        UserDTO existingByEmail = userDAO.getByEmail(email);
        if (existingByEmail != null) {
            if (existingByEmail.isGoogleUser()) {
                request.setAttribute("error",
                    "Email này đã được đăng ký bằng tài khoản Google. Vui lòng đăng nhập bằng Google hoặc sử dụng email khác.");
            } else {
                request.setAttribute("error",
                    "Email này đã được đăng ký tài khoản. Vui lòng đăng nhập hoặc sử dụng email khác.");
            }
            forward(request, response);
            return;
        }

        // ⭐ KIỂM TRA TRÙNG SỐ ĐIỆN THOẠI
        UserDTO existingByPhone = userDAO.getByPhone(phone);
        if (existingByPhone != null) {
            request.setAttribute("error",
                "Số điện thoại này đã được đăng ký. Vui lòng sử dụng số điện thoại khác.");
            forward(request, response);
            return;
        }

        // ===== XÁC ĐỊNH USERNAME =====
        String username = phone;

        // ⭐ KIỂM TRA TRÙNG USERNAME
        UserDTO existingByUsername = userDAO.getByUsername(username);
        if (existingByUsername != null) {
            if (existingByUsername.isGoogleUser()) {
                request.setAttribute("error",
                    "Tài khoản này đã được đăng ký bằng Google. Vui lòng đăng nhập bằng Google.");
            } else {
                request.setAttribute("error",
                    "Số điện thoại này đã được đăng ký tài khoản. Vui lòng đăng nhập.");
            }
            forward(request, response);
            return;
        }

        // Kiểm tra thêm nếu username là email đã từng được lưu
        UserDTO existingByUsernameEmail = userDAO.getByUsername(email);
        if (existingByUsernameEmail != null) {
            if (existingByUsernameEmail.isGoogleUser()) {
                request.setAttribute("error",
                    "Email này đã được đăng ký bằng tài khoản Google. Vui lòng đăng nhập bằng Google.");
            } else {
                request.setAttribute("error",
                    "Email này đã được đăng ký. Vui lòng đăng nhập.");
            }
            forward(request, response);
            return;
        }

        // ===== TIẾN HÀNH ĐĂNG KÝ TÀI KHOẢN LOCAL =====
        int newUserId = userDAO.registerFull(
            username,
            password,
            fullName,
            phone,
            email
        );

        if (newUserId > 0) {
            // Thành công → chuyển về trang login kèm thông báo
            request.setAttribute("success",
                "Đăng ký tài khoản thành công! Vui lòng đăng nhập bằng Email " + email);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Đăng ký thất bại do lỗi hệ thống. Vui lòng thử lại sau.");
            forward(request, response);
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/view/register.jsp").forward(request, response);
    }
}