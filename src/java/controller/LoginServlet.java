package controller;

import dao.UserDAO;
import dto.UserDTO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/view/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        // ⭐ Đổi tên param từ "username" → "username" nhưng cho phép nhập SĐT/Email
        String input    = request.getParameter("username");
        String password = request.getParameter("password");

        // ===== VALIDATE =====
        if (input == null || input.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            request.setAttribute("username", input);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
            return;
        }

        input = input.trim();

        // ⭐ ĐĂNG NHẬP BẰNG SĐT HOẶC EMAIL HOẶC USERNAME
        UserDTO user = userDAO.checkLoginByPhoneOrEmail(input, password);

        if (user == null) {
            request.setAttribute("error", "Số điện thoại / Email hoặc mật khẩu không đúng!");
            request.setAttribute("username", input);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
            return;
        }

        // ===== ĐĂNG NHẬP THÀNH CÔNG =====
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(30 * 60);

        System.out.println("==========================================");
        System.out.println("✅ [Login] Đăng nhập thành công: " + user.getUsername());
        System.out.println("   - Input nhập: " + input);
        System.out.println("   - Role: " + user.getRole());

        // ===== XỬ LÝ REDIRECT =====
        String returnUrl = request.getParameter("returnUrl");
        Object redirectUrl = session.getAttribute("redirectAfterLogin");

        String finalRedirect = null;

        if (returnUrl != null && !returnUrl.trim().isEmpty()) {
            finalRedirect = returnUrl;
            session.removeAttribute("redirectAfterLogin");
            System.out.println("   → Dùng returnUrl param");
        } else if (redirectUrl != null) {
            finalRedirect = (String) redirectUrl;
            session.removeAttribute("redirectAfterLogin");
            System.out.println("   → Dùng redirectAfterLogin session");
        } else {
            finalRedirect = request.getContextPath() + "/dashboard";
            System.out.println("   → Dùng mặc định /dashboard");
        }

        System.out.println("   ➡️ FINAL REDIRECT: " + finalRedirect);
        System.out.println("==========================================");

        response.sendRedirect(finalRedirect);
    }
}