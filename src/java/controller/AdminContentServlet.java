package controller;
import cms.SiteContent;
import dto.UserDTO;
import dao.SettingsDAO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.util.*;
@WebServlet(urlPatterns={"/admin/content","/admin/business"})
public class AdminContentServlet extends HttpServlet {
    protected SettingsDAO store(){return new SettingsDAO();}
    protected void doGet(HttpServletRequest r,HttpServletResponse p)throws IOException,ServletException{
        boolean business=r.getServletPath().endsWith("business");r.setAttribute("business",business);r.setAttribute("cmsFields",SiteContent.fields(business));r.setAttribute("cmsSections",cms.EditorLayout.sections(business));
        if(r.getAttribute("cmsValues")==null)r.setAttribute("cmsValues",SiteContent.from(store().getCms()));r.setAttribute("revisions",store().revisions(business?"business":"content"));r.getRequestDispatcher("/view/admin/content.jsp").forward(r,p);
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException,ServletException{
        r.setCharacterEncoding("UTF-8");
        UserDTO u=(UserDTO)r.getSession().getAttribute("user");if(!security.AdminSecurity.admin(u)||!security.AdminSecurity.csrf(r)){p.sendError(403);return;}
        boolean business=r.getServletPath().endsWith("business");String kind=business?"business":"content";
        try{
            boolean ok;
            if("rollback".equals(r.getParameter("action")))ok=store().rollbackLatest(Integer.parseInt(r.getParameter("revisionId")),u.getId(),kind);
            else {
                Map<String,String> values=new LinkedHashMap<>(),input=SiteContent.defaults();
                for(SiteContent.Field f:SiteContent.fields(business)){String raw=r.getParameter(f.getKey());if(raw!=null)input.put(f.getKey(),raw);}
                r.setAttribute("cmsValues",input);
                for(SiteContent.Field f:SiteContent.fields(business)){String v=SiteContent.validate(f,r.getParameter(f.getKey()));values.put("cms."+f.getKey(),v);input.put(f.getKey(),v);}
                String start=input.get("business.promotionStart"),end=input.get("business.promotionEnd");if(!start.isEmpty()&&!end.isEmpty()&&start.compareTo(end)>0)throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
                if(business&&!input.get("business.promotionPackageIds").isEmpty()){Set<Integer> ids=new HashSet<>();for(dto.PackageDTO pack:new dao.PackageDAO().getAll())ids.add(pack.getId());for(String id:input.get("business.promotionPackageIds").split(","))if(!ids.contains(Integer.parseInt(id)))throw new IllegalArgumentException("Gói ưu đãi không tồn tại");}
                ok=store().saveVersioned(values,u.getId(),kind);
            }
            if(!ok)throw new IllegalArgumentException("Không thể lưu hoặc bản sửa đã thay đổi. Kiểm tra migration và thử lại.");
            ai.AIService.invalidateCache();r.getSession().setAttribute("cmsMessage","Đã lưu và xuất bản. Website cập nhật ngay.");p.sendRedirect(r.getContextPath()+r.getServletPath());
        }catch(Exception e){r.setAttribute("cmsError",e instanceof IllegalArgumentException?e.getMessage():"Không thể lưu dữ liệu.");p.setStatus(400);doGet(r,p);}
    }
}
