package cms;
import dto.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.IOException;
/** No DAO dependency: all demo customer data is synthetic. */
public final class DemoData {
    private DemoData(){}
    public static List<PackageDTO> packages(){List<PackageDTO> rows=new ArrayList<>();int[] prices={195,205,220,230,239,249},speeds={300,300,300,300,1000,1000};for(int i=0;i<6;i++){PackageDTO p=new PackageDTO();p.setId(i+1);p.setPackageCode(prices[i]+"K");p.setName("Gói "+prices[i]+"K");p.setPrice(prices[i]*1000L);p.setSpeedMbps(speeds[i]);p.setStandardInstallationFee(300000L);p.setActive(true);p.setDisplayOrder(i+1);p.setDescription("Modem WiFi 6"+(i%2==1?", 1 Camera":"")+(i>=2?", TV 180 kênh, Ngoại Hạng Anh":""));rows.add(p);}return rows;}
    public static List<CustomerDTO> customers(){List<CustomerDTO> rows=new ArrayList<>();for(int i=1;i<=3;i++){CustomerDTO c=new CustomerDTO();c.setId(i);c.setFullName("Khách minh họa "+i);c.setPhone("Số điện thoại giả");c.setAddress("Địa chỉ minh họa");c.setEmail("demo"+i+"@example.invalid");c.setStatus("Mới");c.setPackageInterest("Gói 195K (minh họa)");c.setCreatedAt(new Date(1700000000000L+i));rows.add(c);}return rows;}
    public static boolean render(HttpServletRequest r,HttpServletResponse p,String path)throws IOException,ServletException{
        String jsp=null,a=r.getParameter("action");r.setAttribute("cmsValues",SiteContent.defaults());
        if(path.equals("/admin/dashboard")){jsp="dashboard.jsp";r.setAttribute("packages",packages());r.setAttribute("recentCustomers",customers());for(String k:Arrays.asList("totalCustomers","countNew"))r.setAttribute(k,3);r.setAttribute("totalPackages",6);for(String k:Arrays.asList("totalUsers","totalEmails","countHot","countFeatured","countActiveUsers","countContacted","countSigned","countCancelled"))r.setAttribute(k,0);r.setAttribute("chartLabels",Arrays.asList("Demo"));r.setAttribute("chartData",Arrays.asList(3));}
        else if(path.equals("/admin/packages")){jsp="package-list.jsp";r.setAttribute("packages",packages());r.setAttribute("totalAll",6);r.setAttribute("totalActive",6);r.setAttribute("totalHot",0);r.setAttribute("totalFeatured",0);if("edit".equals(a)||"add".equals(a)){jsp="package-form.jsp";r.setAttribute("mode","add".equals(a)?"add":"edit");if(!"add".equals(a)){int id=number(r.getParameter("id"));if(id<1||id>6){p.sendError(404);return true;}r.setAttribute("pkg",packages().get(id-1));}}}
        else if(path.equals("/admin/customers")){jsp="customer-list.jsp";r.setAttribute("customers",customers());for(String k:Arrays.asList("countAll","countNew"))r.setAttribute(k,3);for(String k:Arrays.asList("countContacted","countSigned","countCancelled"))r.setAttribute(k,0);if("edit".equals(a)){jsp="customer-edit.jsp";int id=number(r.getParameter("id"));if(id<1||id>3){p.sendError(404);return true;}r.setAttribute("customer",customers().get(id-1));}}
        else if(path.equals("/admin/customer-detail")){int id=number(r.getParameter("id"));if(id<1||id>3){p.sendError(404);return true;}jsp="customer-detail.jsp";r.setAttribute("customer",customers().get(id-1));r.setAttribute("emailLogs",Collections.emptyList());}
        else if(path.equals("/admin/content")||path.equals("/admin/business")){jsp="content.jsp";r.setAttribute("cmsFields",SiteContent.fields(path.endsWith("business")));r.setAttribute("cmsSections",EditorLayout.sections(path.endsWith("business")));r.setAttribute("cmsValues",SiteContent.defaults());r.setAttribute("business",path.endsWith("business"));}
        if(jsp==null)return false;r.setAttribute("currentPage",1);r.setAttribute("totalPages",1);r.setAttribute("totalRecords",3);r.setAttribute("demoMode",true);r.setAttribute("demoRender",true);r.getRequestDispatcher("/view/admin/"+jsp).forward(r,p);return true;
    }
    private static int number(String v){try{return Integer.parseInt(v);}catch(Exception e){return -1;}}
}
