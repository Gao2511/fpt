package controller;

import dao.UserDAO;

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
        if (email != null)    email = email.trim();

        // ===== VALIDATE =====

        // 1. Họ tên
        if (fullName == null || fullName.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập Họ và tên");
            forward(request, response);
            return;
        }

        // 2. ⭐ BẮT BUỘC: SĐT HOẶC EMAIL (ít nhất 1 trong 2)
        boolean hasPhone = (phone != null && !phone.isEmpty());
        boolean hasEmail = (email != null && !email.isEmpty());

        if (!hasPhone && !hasEmail) {
            request.setAttribute("error",
                "Bạn phải nhập ít nhất 1 trong 2: Số điện thoại HOẶC Email");
            forward(request, response);
            return;
        }

        // 3. Validate SĐT nếu có nhập
        if (hasPhone) {
            String cleanPhone = phone.replaceAll("[\\s.\\-]", "");
            if (!cleanPhone.matches("^[0-9]{10,11}$")) {
                request.setAttribute("error", "Số điện thoại không hợp lệ (phải là 10-11 chữ số)");
                forward(request, response);
                return;
            }
            phone = cleanPhone;
        }

        // 4. Validate Email nếu có nhập
        if (hasEmail) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                request.setAttribute("error", "Email không hợp lệ");
                forward(request, response);
                return;
            }
        }

        // 5. Mật khẩu
        if (password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập mật khẩu");
            forward(request, response);
            return;
        }

        if (password.length() < 8) {
            request.setAttribute("error", "Mật khẩu phải có ít nhất 8 ký tự");
            forward(request, response);
            return;
        }

        if (!password.equals(confirm)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp");
            forward(request, response);
            return;
        }

        // 6. Điều khoản
        if (agree == null) {
            request.setAttribute("error", "Bạn phải đồng ý với Điều khoản dịch vụ");
            forward(request, response);
            return;
        }

        // ===== XÁC ĐỊNH USERNAME =====
        // ⭐ Ưu tiên SĐT → nếu không có SĐT thì dùng Email
        String username;
        if (hasPhone) {
            username = phone;
        } else {
            username = email;
        }

        // ===== KIỂM TRA TRÙNG USERNAME =====
        if (userDAO.getByUsername(username) != null) {
            String field = hasPhone ? "Số điện thoại" : "Email";
            request.setAttribute("error",
                field + " này đã được đăng ký. Vui lòng đăng nhập hoặc dùng thông tin khác.");
            forward(request, response);
            return;
        }

        // ⭐ KIỂM TRA TRÙNG EMAIL (nếu có email và username là SĐT)
        if (hasEmail && hasPhone) {
            // Kiểm tra email có bị trùng không
            if (userDAO.getByEmail(email) != null) {
                request.setAttribute("error",
                    "Email này đã được đăng ký. Vui lòng dùng email khác.");
                forward(request, response);
                return;
            }
        }

        // ⭐ KIỂM TRA TRÙNG PHONE (nếu có phone và username là email)
        if (hasPhone && hasEmail && !hasPhone) {
            // Trường hợp này không xảy ra vì hasPhone = true
        }

        // ===== ĐĂNG KÝ =====
        String emailToSave = hasEmail ? email : null;
        String phoneToSave = hasPhone ? phone : null;

        int consultantId = userDAO.registerFull(
            username,
            password,
            fullName,
            phoneToSave,
            emailToSave
        );

        if (consultantId > 0) {
            // Thành công → chuyển về login
            request.setAttribute("success",
                "Đăng ký thành công! Vui lòng đăng nhập bằng "
                + (hasPhone ? "SĐT " + phone : "Email " + email));
            request.setAttribute("username", username);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Đăng ký thất bại. Vui lòng thử lại sau.");
            forward(request, response);
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/view/register.jsp").forward(request, response);
    }
}