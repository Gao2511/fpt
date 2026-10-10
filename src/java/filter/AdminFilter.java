package filter;

import dto.UserDTO;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;

/**
 * AdminFilter - Chặn user không phải admin truy cập /admin/*
 */
@WebFilter(urlPatterns={"/*"}, dispatcherTypes={DispatcherType.REQUEST,DispatcherType.FORWARD,DispatcherType.ERROR})
public class AdminFilter implements Filter {
    protected UserDTO verify(UserDTO user) { return new dao.UserDAO().getById(user.getId()); }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        // Set encoding before CSRF/action reads cause the container to parse the form.
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;
        // Servlet paths are decoded/normalized by Tomcat; raw URI prefixes can be bypassed with %61dmin.
        String path=request.getServletPath();
        if(request.getPathInfo()!=null)path+=request.getPathInfo();
        if (security.AdminSecurity.demo(user)) {
            response.setHeader("Cache-Control","no-store");
            if(request.getDispatcherType()!=DispatcherType.REQUEST && Boolean.TRUE.equals(request.getAttribute("demoRender")) && path.startsWith("/view/admin/")){chain.doFilter(req,res);return;}
            if("GET".equals(request.getMethod()) && !security.AdminSecurity.mutation(request.getParameter("action")) && cms.DemoData.render(request,response,path))return;
            if("GET".equals(request.getMethod()) && ((path.startsWith("/assets/")&&!path.startsWith("/assets/uploads/"))||path.startsWith("/js/")||path.equals("/logout")||path.equals("/login"))){chain.doFilter(req,res);return;}
            response.sendError(403,"Chức năng này không khả dụng trong chế độ Demo.");return;
        }
        if(!path.startsWith("/admin/") && !path.startsWith("/view/admin/")){chain.doFilter(req,res);return;}
        response.setHeader("Cache-Control","no-store");

        // Chưa đăng nhập → về login
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Không phải admin → báo lỗi 403
        if (!security.AdminSecurity.admin(user) || !security.AdminSecurity.admin(verify(user))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền truy cập trang này");
            return;
        }

        if(path.startsWith("/view/admin/") && request.getDispatcherType()==DispatcherType.REQUEST){response.sendError(403);return;}
        if("GET".equals(request.getMethod()) && security.AdminSecurity.mutation(request.getParameter("action"))){response.sendError(405,"Dùng POST để thay đổi dữ liệu.");return;}
        if(!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())){
            try{if(!security.AdminSecurity.csrf(request)){response.sendError(403,"Biểu mẫu hết hạn hoặc thiếu mã bảo vệ.");return;}}
            catch(IllegalStateException oversized){response.sendError(413,"Biểu mẫu hoặc ảnh vượt giới hạn.");return;}
        }
        request.setAttribute("csrfToken",security.AdminSecurity.token(session));
        chain.doFilter(req, res);
    }
}
