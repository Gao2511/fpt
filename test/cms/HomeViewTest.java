package cms;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

/** Render the real JSP/includes on loopback, using only synthetic request data. No DAO or AI calls. */
public final class HomeViewTest {
    private static int passed;
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Specify staged web root and fresh evidence directory");
        Path evidence = Paths.get(args[1]);
        Files.createDirectory(evidence);
        Tomcat server = new Tomcat();
        server.setBaseDir(evidence.resolve("tomcat").toAbsolutePath().toString());
        server.setPort(0);
        server.getConnector().setProperty("address", "127.0.0.1");
        Context context = server.addWebapp("/fixture", Paths.get(args[0]).toAbsolutePath().toString());
        context.setIgnoreAnnotations(true);
        context.setParentClassLoader(HomeViewTest.class.getClassLoader());
        org.apache.catalina.loader.WebappLoader loader = new org.apache.catalina.loader.WebappLoader();
        loader.setDelegate(true);
        context.setLoader(loader);
        Tomcat.addServlet(context, "home-fixture", new HttpServlet() {
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                Map<String, String> values = SiteContent.defaults();
                values.put("benefit1.order", "4");
                values.put("benefit4.order", "1");
                values.put("device1.order", "4");
                values.put("device4.order", "1");
                for (int i = 1; i <= 2; i++) values.put("business.meshF" + i + "MonthlyDisplay",
                        SiteContent.amount(values, "business.meshF" + i + "Monthly"));
                request.setAttribute("cms", values);
                request.setAttribute("cmsPromotionVisible", false);
                request.setAttribute("registrationToken", "OFFLINE_FIXTURE_TOKEN");
                request.setAttribute("packages", "empty".equals(request.getParameter("case"))
                        ? Collections.emptyList() : DemoData.packages());
                if ("customer".equals(request.getParameter("case"))) {
                    dto.UserDTO user = new dto.UserDTO();
                    user.setRole("customer");
                    user.setUsername("offline-customer");
                    user.setFullName("Khách <b>Gia Lai</b> \"A\" & B");
                    user.setPhone("09\" data-test=\"phone");
                    user.setEmail("a\" data-test=\"email@example.invalid");
                    request.getSession().setAttribute("user", user);
                }
                request.getRequestDispatcher("/view/home.jsp").forward(request, response);
            }
        });
        context.addServletMappingDecoded("/home-test", "home-fixture");
        try {
            server.start();
            String base = "http://127.0.0.1:" + server.getConnector().getLocalPort() + "/fixture/home-test?case=";
            String anonymous = get(base + "anonymous");
            Files.write(evidence.resolve("anonymous.html"), anonymous.getBytes(StandardCharsets.UTF_8));
            check(anonymous.contains("id=\"aiChatWindow\"") && anonymous.contains("id=\"themeToggleBtn\""), "includes and chat render");
            check(anonymous.contains("/fixture/js/ai-chat.js") && anonymous.contains("/fixture/package-detail?id="), "context path retained");
            check(!anonymous.contains("left: px;") && !anonymous.contains("top: px;"), "JSP does not erase JS coordinates");
            check(anonymous.contains("class=\"feature\" data-cms-order=\"4\"")
                    && anonymous.contains("class=\"device\" data-cms-order=\"4\"")
                    && anonymous.contains("/fixture/js/home-order.js"), "CMS custom order and script render");
            String customer = get(base + "customer");
            Files.write(evidence.resolve("customer.html"), customer.getBytes(StandardCharsets.UTF_8));
            check(customer.contains("<span class=\"welcome-line-2\">Khách &lt;b&gt;Gia Lai&lt;/b&gt; &#034;A&#034; &amp; B!</span>"), "welcome escapes markup");
            check(input(customer, "user_name").contains("&lt;b&gt;") && input(customer, "user_name").contains("&#034;A&#034;"), "name preserves quoted text safely");
            check(input(customer, "user_phone").contains("09&#034; data-test=&#034;phone"), "phone cannot create an attribute");
            check(input(customer, "user_email").contains("a&#034; data-test=&#034;email@example.invalid"), "email cannot create an attribute");
            String empty = get(base + "empty");
            check(empty.contains("Chưa có gói cước nào."), "empty catalogue renders");
            System.out.println("HomeViewTest: " + passed + "/9 passed; evidence=" + evidence.toAbsolutePath());
        } finally { server.stop(); server.destroy(); }
    }
    private static String input(String html, String id) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("<input\\b[^>]*id=\"" + id + "\"[^>]*>").matcher(html);
        if (!matcher.find()) throw new AssertionError("Missing input " + id);
        return matcher.group();
    }
    private static String get(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(30000); connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode() != 200) throw new AssertionError("JSP returned HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally { connection.disconnect(); }
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        passed++;
    }
}
