package controller;

import dto.UserDTO;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/dashboard")
public class DashboardRouterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Admin → vào dashboard admin
        if ("admin".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        }
        // Customer → về trang chủ (nhưng đã có session → hiển thị banner chào mừng)
        else {
            response.sendRedirect(request.getContextPath() + "/home");
        }
    }
}