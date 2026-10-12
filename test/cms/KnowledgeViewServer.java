package cms;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.Context;
import javax.servlet.*;
import javax.servlet.http.*;
import java.nio.file.*;
import java.util.*;
import dto.AdminKnowledgeDTO.*;

/** Real JSPs, synthetic request attributes, annotation scanning disabled, no DAO/AI calls. */
public final class KnowledgeViewServer {
    public static void main(String[] args)throws Exception {
        Tomcat server=new Tomcat();server.setBaseDir(args[1]);server.setPort(0);server.getConnector().setProperty("address","127.0.0.1");
        Context context=server.addWebapp("",Paths.get(args[0]).toAbsolutePath().toString());context.setIgnoreAnnotations(true);context.setParentClassLoader(KnowledgeViewServer.class.getClassLoader());org.apache.catalina.loader.WebappLoader loader=new org.apache.catalina.loader.WebappLoader();loader.setDelegate(true);context.setLoader(loader);
        Tomcat.addServlet(context,"kb-fixture",new HttpServlet(){protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,java.io.IOException {
            dto.UserDTO u=new dto.UserDTO();u.setId(101);u.setRole("admin");u.setUsername("fixture-admin");u.setActive(true);r.getSession().setAttribute("user",u);
            r.setAttribute("csrfToken","fixture-csrf");r.setAttribute("kbTab",r.getParameter("tab"));r.setAttribute("kbLifecycleAvailable",true);r.setAttribute("kbWorkerAuthorized",false);r.setAttribute("kbTextLimit",100000);r.setAttribute("kbExtractedLimit",500000);r.setAttribute("kbUploadLimit",10485760);
            boolean large="true".equals(r.getParameter("large"));String text="Kiểm tra vị trí modem và vật cản.";if(large)text=String.join("\n",Collections.nCopies(4000,text));
            ManagedDocument doc=new ManagedDocument(1,1,"Hướng dẫn WiFi <script>danger</script>","guide",text,"fixture:reviewed","draft","DRAFT",large?"UPLOAD":"MANUAL","","",rag.VectorValues.hash("fixture"),"pending",null,"wifi.fixture","admin_reviewed",null,null,null,0,null,101,101,0,null,"",400,0,java.time.Instant.EPOCH,java.time.Instant.EPOCH);
            r.setAttribute("documents",Collections.singletonList(doc));if("documents".equals(r.getParameter("tab")))r.setAttribute("selectedDoc",doc);
            r.setAttribute("metrics",new DashboardMetrics(1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,Collections.emptyList()));
            Map<String,Object> readiness=new HashMap<>();readiness.put("published",0);readiness.put("gemini",0);readiness.put("validChunks",0);readiness.put("failedPublishJobs",0);r.setAttribute("kbReadiness",readiness);
            r.setAttribute("docPage",1);r.setAttribute("docTotalPages",1);r.setAttribute("kbPublishJobs",Collections.emptyList());r.setAttribute("ragRequestEvidence",Collections.emptyList());
            r.getRequestDispatcher("/view/admin/knowledge-base.jsp").forward(r,p);
        }});
        context.addServletMappingDecoded("/admin/knowledge-base","kb-fixture");server.start();System.out.println("KB57_TEST_PORT="+server.getConnector().getLocalPort());System.out.flush();server.getServer().await();
    }
}
