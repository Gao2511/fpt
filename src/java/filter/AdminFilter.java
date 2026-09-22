package filter;

import dto.UserDTO;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;

/**
 * AdminFilter - Chặn user không phải admin truy cập /admin/*
 */
@WebFilter(urlPatterns = {"/admin/*"})
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);

        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        // Chưa đăng nhập → về login
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Không phải admin → báo lỗi 403
        if (!"admin".equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền truy cập trang này");
            return;
        }

        chain.doFilter(req, res);
    }
}