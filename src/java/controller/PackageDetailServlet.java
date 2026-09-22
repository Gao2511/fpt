package controller;

import dao.PackageDAO;
import dto.PackageDTO;
import dto.UserDTO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * PackageDetailServlet - Trang chi tiết gói cước
 * CHỈ CHO THÀNH VIÊN ĐÃ ĐĂNG NHẬP
 * URL: /package-detail?id=x
 */
@WebServlet("/package-detail")
public class PackageDetailServlet extends HttpServlet {

    private final PackageDAO packageDAO = new PackageDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ===== LẤY ID GÓI =====
        String idStr = request.getParameter("id");
        
        // ⭐ DEBUG
        System.out.println("==========================================");
        System.out.println("🔵 [PackageDetail] Vào servlet");
        System.out.println("   - idStr = " + idStr);

        if (idStr == null || idStr.trim().isEmpty()) {
            System.out.println("   ❌ idStr NULL hoặc rỗng → về /home");
            response.sendRedirect(request.getContextPath() + "/home#packages");
            return;
        }

        int pkgId;
        try {
            pkgId = Integer.parseInt(idStr);
            System.out.println("   - pkgId = " + pkgId);
        } catch (NumberFormatException e) {
            System.out.println("   ❌ idStr không phải số → về /home");
            response.sendRedirect(request.getContextPath() + "/home#packages");
            return;
        }

        // ===== KIỂM TRA ĐĂNG NHẬP =====
        HttpSession session = request.getSession(false);
        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        System.out.println("   - session = " + (session != null ? "có" : "null"));
        System.out.println("   - user = " + (user != null ? user.getUsername() : "null"));

        if (user == null) {
            // ⭐ TẠO URL ĐẦY ĐỦ (đã có context path)
            String originalUrl = request.getContextPath() + "/package-detail?id=" + pkgId;
            
            System.out.println("   🔒 Chưa login → Lưu redirectAfterLogin = " + originalUrl);

            session = request.getSession(true);
            session.setAttribute("redirectAfterLogin", originalUrl);
            session.setAttribute("message", "Vui lòng đăng nhập để xem chi tiết gói cước");
            session.setAttribute("messageType", "info");

            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ===== LẤY GÓI CƯỚC =====
        PackageDTO pkg = packageDAO.getById(pkgId);
        
        System.out.println("   - pkg = " + (pkg != null ? pkg.getName() : "null"));

        if (pkg == null) {
            System.out.println("   ❌ Không tìm thấy gói → về /home");
            response.sendRedirect(request.getContextPath() + "/home#packages");
            return;
        }

        System.out.println("   ✅ Forward đến package-detail.jsp");
        System.out.println("==========================================");

        request.setAttribute("pkg", pkg);
        request.setAttribute("currentUser", user);
        request.getRequestDispatcher("/view/customer/package-detail.jsp")
               .forward(request, response);
    }
}