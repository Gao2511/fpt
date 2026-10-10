package cms;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import javax.servlet.*;
import javax.servlet.http.*;
import java.nio.file.*;
import java.util.*;
/** Loopback-only test app, synthetic JDBC and identities. Never packaged into production WAR. */
public class CmsBrowserServer {
    public static void main(String[] args)throws Exception {
        FakeDatabase db=new FakeDatabase();db.install();Tomcat server=new Tomcat();server.setBaseDir(Files.createTempDirectory("fpt-cms-browser-").toString());server.setPort(0);server.getConnector().setProperty("address","127.0.0.1");
        Context context=server.addWebapp("",Paths.get("build/web").toAbsolutePath().toString());context.setIgnoreAnnotations(true);context.setParentClassLoader(CmsBrowserServer.class.getClassLoader());org.apache.catalina.loader.WebappLoader loader=new org.apache.catalina.loader.WebappLoader();loader.setDelegate(true);context.setLoader(loader);
        servlet(context,"fixture",new HttpServlet(){protected void doGet(HttpServletRequest r,HttpServletResponse p)throws java.io.IOException{
            if(r.getServletPath().equals("/fixture-login")){dto.UserDTO u=new dto.UserDTO();u.setId(101);u.setRole("admin");u.setUsername("test-admin");u.setFullName("Test administrator");u.setActive(true);r.getSession().setAttribute("user",u);p.sendRedirect("/admin/content");}
            else if(r.getServletPath().equals("/fixture-image")){p.setContentType("image/png");javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20,10,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",p.getOutputStream());}
            else {p.setContentType("application/json");com.google.gson.JsonObject j=new com.google.gson.JsonObject();j.addProperty("writes",db.writes);j.addProperty("privateReads",db.privateReads);p.getWriter().write(j.toString());}
        }},"/fixture-login","/fixture-stats","/fixture-image");
        servlet(context,"login",new controller.LoginServlet(),"/login");servlet(context,"logout",new controller.LogoutServlet(),"/logout");
        servlet(context,"home",new controller.HomeServlet(),"/home");servlet(context,"detail",new controller.PackageDetailServlet(),"/package-detail");
        servlet(context,"content",new controller.AdminContentServlet(),"/admin/content","/admin/business");
        servlet(context,"packages",new controller.AdminPackageServlet(),"/admin/packages");
        servlet(context,"settings",new controller.AdminSettingsServlet(),"/admin/settings");
        // Demo routes are intercepted by the filter. Sentinel ensures they never reach live controllers.
        servlet(context,"sensitive",new HttpServlet(){protected void service(HttpServletRequest r,HttpServletResponse p)throws java.io.IOException{p.sendError(500,"Unexpected live controller execution");}},"/admin/dashboard","/admin/customers","/admin/customer-detail","/admin/users","/admin/email-logs","/admin/ai-playground","/ContactServlet","/profile","/change-password","/register","/reset-password","/ai-chat");
        Wrapper media=servlet(context,"media",new controller.MediaServlet(),"/admin/media","/media");media.setMultipartConfigElement(new MultipartConfigElement("",2097152,2200000,0));
        servlet(context,"main",new controller.MainController(),"/MainController","/main");
        addFilter(context,"admin",new filter.AdminFilter());addFilter(context,"content",new filter.SiteContentFilter());
        server.start();System.out.println("FPT_CMS_TEST_PORT="+server.getConnector().getLocalPort());System.out.flush();server.getServer().await();
    }
    static Wrapper servlet(Context c,String name,HttpServlet servlet,String... paths){Wrapper w=Tomcat.addServlet(c,name,servlet);for(String path:paths)c.addServletMappingDecoded(path,name);return w;}
    static void addFilter(Context c,String name,Filter filter){FilterDef def=new FilterDef();def.setFilterName(name);def.setFilter(filter);c.addFilterDef(def);FilterMap map=new FilterMap();map.setFilterName(name);map.addURLPattern("/*");map.setDispatcher("REQUEST");map.setDispatcher("FORWARD");map.setDispatcher("ERROR");c.addFilterMap(map);}
}
