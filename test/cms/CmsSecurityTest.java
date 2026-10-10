package cms;
import filter.AdminFilter;
import security.*;
import dto.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import static ai.ConsultationBehaviorTest.check;
public class CmsSecurityTest {
    static int passed;
    static class Exchange {
        final Map<String,Object> session=new HashMap<>(),attrs=new HashMap<>();final Map<String,String> params=new HashMap<>();String path,method="GET",forward,redirect,headerToken;int status=200,chains;DispatcherType dispatch=DispatcherType.REQUEST;
        final HttpSession s=FakeDatabase.proxy(HttpSession.class,(o,m,a)->{switch(m.getName()){case "getAttribute":return session.get(a[0]);case "setAttribute":session.put((String)a[0],a[1]);return null;case "getId":return "private-test-session";default:return FakeDatabase.value(m.getReturnType());}});
        final HttpServletRequest request=FakeDatabase.proxy(HttpServletRequest.class,(o,m,a)->{switch(m.getName()){case "getSession":return s;case "getRequestURI":return "/fpt-sale"+path;case "getContextPath":return "/fpt-sale";case "getServletPath":return path;case "getMethod":return method;case "getDispatcherType":return dispatch;case "getParameter":return params.get(a[0]);case "getHeader":return headerToken;case "setAttribute":attrs.put((String)a[0],a[1]);return null;case "getAttribute":return attrs.get(a[0]);case "getRequestDispatcher":return FakeDatabase.proxy(RequestDispatcher.class,(x,n,b)->{forward=(String)a[0];return null;});default:return FakeDatabase.value(m.getReturnType());}});
        final HttpServletResponse response=FakeDatabase.proxy(HttpServletResponse.class,(o,m,a)->{switch(m.getName()){case "sendError":case "setStatus":status=(int)a[0];return null;case "sendRedirect":status=302;redirect=(String)a[0];return null;default:return FakeDatabase.value(m.getReturnType());}});
        Exchange(String path,UserDTO u){this.path=path;if(u!=null)session.put("user",u);}
        void run(AdminFilter f)throws Exception{f.doFilter(request,response,(r,p)->chains++);}
    }
    static UserDTO user(String role){UserDTO u=new UserDTO();u.setId(101);u.setRole(role);u.setActive(true);return u;}
    static void pass(String label){passed++;System.out.println("PASS "+label);}
    public static void main(String[] args)throws Exception {
        FakeDatabase db=new FakeDatabase();db.install();AdminFilter filter=new AdminFilter();
        for(DispatcherType dispatch:new DispatcherType[]{DispatcherType.REQUEST,DispatcherType.FORWARD,DispatcherType.ERROR}){Exchange e=new Exchange("/admin/packages",null);e.dispatch=dispatch;e.run(filter);check(e.status==302&&e.chains==0,"Unauth route bypass");pass("unauthenticated "+dispatch+" is blocked");}
        Exchange e=new Exchange("/view/admin/package-form.jsp",user("admin"));e.run(filter);check(e.status==403&&e.chains==0,"Direct JSP accessible");pass("direct JSP is protected");
        e=new Exchange("/admin/packages",user("customer"));e.run(filter);check(e.status==403&&e.chains==0,"Customer promoted");pass("customer lacks admin permission");
        UserDTO locked=user("admin");locked.setActive(false);e=new Exchange("/admin/packages",locked);e.run(filter);check(e.status==403,"Locked session allowed");pass("locked account denied");
        e=new Exchange("/admin/packages",user("admin"));e.params.put("action","delete");e.run(filter);check(e.status==405&&e.chains==0,"GET writes permitted");pass("GET mutation is denied");
        e=new Exchange("/admin/content",user("admin"));e.method="POST";e.run(filter);check(e.status==403&&e.chains==0,"Missing CSRF accepted");pass("CSRF mandatory");
        e=new Exchange("/admin/content",user("admin"));e.method="POST";e.headerToken=AdminSecurity.token(e.s);e.run(filter);check(e.status==200&&e.chains==1,"Valid admin POST rejected");pass("verified admin with CSRF reaches controller");
        int reads=db.privateReads,writes=db.writes;
        for(String route:new String[]{"/admin/dashboard","/admin/packages","/admin/customers","/admin/content","/admin/business"}){e=new Exchange(route,user("demo_admin"));e.run(filter);check(e.status==200&&e.chains==0&&e.forward!=null&&Boolean.TRUE.equals(e.attrs.get("demoMode")),"Demo not isolated: "+route);pass("demo renders fixture "+route);}
        e=new Exchange("/admin/customer-detail",user("demo_admin"));e.params.put("id","1");e.run(filter);CustomerDTO c=(CustomerDTO)e.attrs.get("customer");check(c.getEmail().endsWith("example.invalid")&&c.getAddress().equals("Địa chỉ minh họa"),"Real customer data present");pass("demo customer privacy");
        for(String route:new String[]{"/admin/settings","/admin/users","/admin/email-logs","/admin/media","/admin/ai-playground","/view/admin/settings.jsp","/MainController","/main","/ContactServlet","/profile","/change-password","/reset-password","/register","/ai-chat"}){
            e=new Exchange(route,user("demo_admin"));e.method="POST";e.headerToken=AdminSecurity.token(e.s);e.params.put("role","admin");e.run(filter);check(e.status==403&&e.chains==0&&e.forward==null,"Demo write escaped: "+route);pass("demo direct API denied "+route);
        }
        e=new Exchange("/admin/packages",user("demo_admin"));e.params.put("action","delete");e.run(filter);check(e.status==403,"Demo GET write");pass("demo GET mutation denied");
        check(db.writes==writes&&db.privateReads==reads,"Demo touched production DAO");pass("all demo cases leave DB and private reads unchanged");
        check(DemoAccount.authenticate("anything","anything")==null,"Demo default enabled");pass("unprovisioned demo cannot log in");
        String hash=Passwords.hash("synthetic-secret-not-a-real-password");check(Passwords.verify("synthetic-secret-not-a-real-password",hash)&&!Passwords.verify("wrong",hash)&&Passwords.verify("legacy-fixture","legacy-fixture"),"Password verifier broken");pass("salted hash and legacy compatibility");
        check(AdminSecurity.redirect("https://attacker.invalid","/fpt-sale","/safe").equals("/safe")&&AdminSecurity.redirect("//attacker.invalid","/fpt-sale","/safe").equals("/safe"),"Unsafe redirect accepted");pass("safe local redirects");
        System.out.println("CmsSecurityTest: "+passed+" passed");
    }
}
