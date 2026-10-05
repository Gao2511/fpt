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

        // ⭐ NHẬN IDENTIFIER (EMAIL / TÊN ĐĂNG NHẬP / SĐT) + PASSWORD
        String identifier = request.getParameter("identifier");
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = request.getParameter("email");
        }
        String password = request.getParameter("password");

        // ===== VALIDATE RỖNG =====
        if (identifier == null || identifier.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            request.setAttribute("identifier", identifier);
            request.setAttribute("email", identifier);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
            return;
        }

        identifier = identifier.trim();

        // ===== BƯỚC 1: Kiểm tra tài khoản có tồn tại không =====
        UserDTO byUser = userDAO.getByIdentifier(identifier);

        if (byUser == null) {
            request.setAttribute("error", "Tài khoản hoặc email không tồn tại trong hệ thống!");
            request.setAttribute("identifier", identifier);
            request.setAttribute("email", identifier);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
            return;
        }

        // ===== BƯỚC 2: ⭐ CHẶN TÀI KHOẢN GOOGLE =====
        if (byUser.isGoogleUser() && (byUser.getPassword() == null || byUser.getPassword().trim().isEmpty())) {
            request.setAttribute("error",
                "Tài khoản này được đăng ký bằng Google. Vui lòng nhấn \"Continue with Google\" để đăng nhập.");
            request.setAttribute("identifier", identifier);
            request.setAttribute("email", identifier);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
            return;
        }

        // Kiểm tra tài khoản bị khóa
        if (!byUser.isActive()) {
            request.setAttribute("error", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ bộ phận hỗ trợ.");
            request.setAttribute("identifier", identifier);
            request.setAttribute("email", identifier);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
            return;
        }

        // ===== BƯỚC 3: Login tài khoản LOCAL =====
        UserDTO user = userDAO.checkLoginLocalByIdentifier(identifier, password);

        if (user == null) {
            request.setAttribute("error", "Mật khẩu không đúng!");
            request.setAttribute("identifier", identifier);
            request.setAttribute("email", identifier);
            request.getRequestDispatcher("/view/login.jsp").forward(request, response);
            return;
        }

        // ===== ĐĂNG NHẬP THÀNH CÔNG =====
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(30 * 60);

        System.out.println("==========================================");
        System.out.println("✅ [Login] Đăng nhập thành công: " + user.getUsername());
        System.out.println("   - Email: " + user.getEmail());
        System.out.println("   - Role: " + user.getRole());

        // ===== XỬ LÝ REDIRECT =====
        String returnUrl = request.getParameter("returnUrl");
        Object redirectUrl = session.getAttribute("redirectAfterLogin");

        String finalRedirect;

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