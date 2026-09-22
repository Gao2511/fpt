package controller;

import dao.CustomerDAO;
import dto.CustomerDTO;
import dto.UserDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * MyOrdersServlet - Xem đơn đăng ký tư vấn của khách hàng
 * URL: /my-orders
 * 
 * CÓ PHÂN TRANG — mỗi trang 5 đơn
 */
@WebServlet("/my-orders")
public class MyOrdersServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private static final int PAGE_SIZE = 5;   // ← Số đơn / trang

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ===== KIỂM TRA ĐĂNG NHẬP =====
        HttpSession session = request.getSession(false);
        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;

        if (user == null) {
            session = request.getSession(true);
            session.setAttribute("redirectAfterLogin",
                request.getContextPath() + "/my-orders");
            session.setAttribute("message", "Vui lòng đăng nhập để xem đơn đăng ký");
            session.setAttribute("messageType", "info");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ===== LẤY TẤT CẢ ĐƠN =====
        List<CustomerDTO> allOrders = new ArrayList<>();

        if (user.getId() > 0) {
            allOrders = customerDAO.getByUserId(user.getId());
        }

        // Fallback: nếu không có đơn và username là SĐT → lọc theo phone
        if (allOrders.isEmpty()) {
            List<CustomerDTO> ordersByPhone = customerDAO.getByPhone(user.getUsername());
            if (!ordersByPhone.isEmpty()) {
                allOrders = ordersByPhone;
            }
        }

        // ===== PHÂN TRANG =====
        int totalRecords = allOrders.size();
        int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;

        int page = 1;
        String pageStr = request.getParameter("page");
        if (pageStr != null) {
            try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalRecords);

        List<CustomerDTO> pageOrders = (totalRecords > 0)
            ? allOrders.subList(fromIndex, toIndex)
            : new ArrayList<>();

        // ===== ĐẨY SANG JSP =====
        request.setAttribute("orders", pageOrders);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", totalRecords);
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("currentUser", user);

        request.getRequestDispatcher("/view/customer/my-orders.jsp").forward(request, response);
    }
}