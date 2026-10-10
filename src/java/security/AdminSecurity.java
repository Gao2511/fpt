package security;
import dto.UserDTO;
import javax.servlet.http.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class AdminSecurity {
    private AdminSecurity() {}
    public static boolean demo(UserDTO u){return u!=null && "demo_admin".equals(u.getRole()) && u.isActive();}
    public static boolean admin(UserDTO u){return u!=null && "admin".equals(u.getRole()) && u.isActive();}
    public static String token(HttpSession s){synchronized(s){String t=(String)s.getAttribute("adminCsrf");if(t==null){byte[] b=new byte[32];new SecureRandom().nextBytes(b);t=Base64.getUrlEncoder().withoutPadding().encodeToString(b);s.setAttribute("adminCsrf",t);}return t;}}
    public static boolean csrf(HttpServletRequest r){HttpSession s=r.getSession(false);if(s==null)return false;String e=(String)s.getAttribute("adminCsrf"),a=r.getHeader("X-CSRF-Token");if(a==null)a=r.getParameter("csrf_token");return e!=null&&a!=null&&MessageDigest.isEqual(e.getBytes(StandardCharsets.UTF_8),a.getBytes(StandardCharsets.UTF_8));}
    public static boolean mutation(String a){return Arrays.asList("delete","deleteAll","deleteMultiple","lock","unlock","updateBadge").contains(a);}
    public static String redirect(String v,String context,String fallback){if(v==null||v.contains("\\")||v.contains("\r")||v.contains("\n")||v.contains("%")||v.contains(".."))return fallback;String p=v.startsWith(context+"/")?v.substring(context.length()):v;if(!p.matches("/(?:home|contact|package-detail|my-orders|profile|admin/(?:dashboard|packages|customers|content|business|settings))(?:[?#][A-Za-z0-9_=&?#.-]*)?"))return fallback;return context+p;}
}
