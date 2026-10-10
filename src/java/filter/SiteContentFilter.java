package filter;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
@WebFilter(urlPatterns={"/*"},dispatcherTypes={DispatcherType.REQUEST,DispatcherType.FORWARD})
public class SiteContentFilter implements Filter {
    public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain)throws IOException,ServletException{
        HttpServletRequest r=(HttpServletRequest)req;String path=r.getServletPath();if(r.getPathInfo()!=null)path+=r.getPathInfo();
        if(r.getAttribute("cms")==null&&(path.equals("/home")||path.equals("/contact")||path.equals("/package-detail")||path.equals("/profile")||path.equals("/my-orders")||path.equals("/ai-chat"))){
            HttpSession s=r.getSession(false);dto.UserDTO u=s==null?null:(dto.UserDTO)s.getAttribute("user");java.util.Map<String,String> values=security.AdminSecurity.demo(u)?cms.SiteContent.defaults():cms.SiteContent.load();for(int i=1;i<=2;i++)values.put("business.meshF"+i+"MonthlyDisplay",cms.SiteContent.amount(values,"business.meshF"+i+"Monthly"));r.setAttribute("cms",values);r.setAttribute("cmsPromotionVisible",cms.SiteContent.promotionVisible(values));
        }chain.doFilter(req,res);
    }
}
