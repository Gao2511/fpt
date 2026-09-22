package controller;

import dao.CustomerDAO;
import dao.PackageDAO;
import dao.UserDAO;
import dao.EmailLogDAO;
import dto.CustomerDTO;
import dto.PackageDTO;
import dto.UserDTO;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * AdminDashboardServlet - Trang chủ admin
 * URL: /admin/dashboard
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final PackageDAO packageDAO = new PackageDAO();
    private final UserDAO userDAO = new UserDAO();
    private final EmailLogDAO emailLogDAO = new EmailLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ===== 1. LẤY DỮ LIỆU =====
        List<CustomerDTO> customers = customerDAO.getAll();
        List<PackageDTO> packages = packageDAO.getAll();
        List<UserDTO> users = userDAO.getAll();

        // ===== 2. THỐNG KÊ CHUNG =====
        int totalCustomers = customers.size();
        int totalPackages = packages.size();
        int totalUsers = users.size();
        int totalEmails = emailLogDAO.getAll().size();

        // Đếm khách hàng mới (status = "Mới")
        long countNew = customers.stream()
                .filter(c -> "Mới".equals(c.getStatus()))
                .count();

        // Đếm gói HOT (badge_type = 'hot')
        long countHot = packages.stream()
                .filter(p -> "hot".equals(p.getBadgeType()))
                .count();

        // Đếm gói NỔI BẬT
        long countFeatured = packages.stream()
                .filter(p -> "featured".equals(p.getBadgeType()))
                .count();

        // Đếm user đang hoạt động
        long countActiveUsers = users.stream()
                .filter(UserDTO::isActive)
                .count();

        // ===== 3. BIỂU ĐỒ 7 NGÀY GẦN NHẤT =====
        Map<String, Integer> last7Days = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -6);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM");
        for (int i = 0; i < 7; i++) {
            last7Days.put(sdf.format(cal.getTime()), 0);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        java.text.SimpleDateFormat fullSdf = new java.text.SimpleDateFormat("dd/MM");
        for (CustomerDTO c : customers) {
            if (c.getCreatedAt() != null) {
                String day = fullSdf.format(c.getCreatedAt());
                if (last7Days.containsKey(day)) {
                    last7Days.put(day, last7Days.get(day) + 1);
                }
            }
        }

        List<String> chartLabels = new ArrayList<>(last7Days.keySet());
        List<Integer> chartData = new ArrayList<>(last7Days.values());

        // ===== 4. 5 KHÁCH HÀNG GẦN ĐÂY NHẤT =====
        // Sắp xếp theo createdAt giảm dần
        List<CustomerDTO> recentCustomers = customers.stream()
                .filter(c -> c.getCreatedAt() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        System.out.println("📊 Dashboard: TotalCustomers=" + totalCustomers
                         + ", TotalPackages=" + totalPackages
                         + ", RecentCustomers=" + recentCustomers.size());

        // ===== 5. ĐẨY SANG JSP =====
        request.setAttribute("totalCustomers", totalCustomers);
        request.setAttribute("totalPackages", totalPackages);
        request.setAttribute("totalUsers", totalUsers);
        request.setAttribute("totalEmails", totalEmails);
        request.setAttribute("countNew", countNew);
        request.setAttribute("countHot", countHot);
        request.setAttribute("countFeatured", countFeatured);
        request.setAttribute("countActiveUsers", countActiveUsers);

        request.setAttribute("chartLabels", chartLabels);
        request.setAttribute("chartData", chartData);

        request.setAttribute("packages", packages);
        request.setAttribute("recentCustomers", recentCustomers);

        request.getRequestDispatcher("/view/admin/dashboard.jsp")
               .forward(request, response);
    }
}