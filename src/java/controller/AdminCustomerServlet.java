package controller;

import dao.CustomerDAO;
import dto.CustomerDTO;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * AdminCustomerServlet - Trang quản lý danh sách khách hàng
 * URL: /admin/customers
 */
@WebServlet("/admin/customers")
public class AdminCustomerServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private static final int PAGE_SIZE = 8;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ===== XÓA KHÁCH HÀNG =====
        if ("delete".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    boolean ok = customerDAO.delete(id);
                    if (ok) {
                        request.getSession().setAttribute("message",
                            "✓ Đã xóa khách hàng #KH-" + id + " thành công");
                        request.getSession().setAttribute("messageType", "success");
                    } else {
                        request.getSession().setAttribute("message", "✗ Xóa khách hàng thất bại");
                        request.getSession().setAttribute("messageType", "error");
                    }
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/admin/customers");
            return;
        }

        // ===== SỬA KHÁCH HÀNG =====
        if ("edit".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    CustomerDTO customer = customerDAO.getById(id);
                    if (customer != null) {
                        request.setAttribute("customer", customer);
                        request.getRequestDispatcher("/view/admin/customer-edit.jsp")
                               .forward(request, response);
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }
            request.getSession().setAttribute("message", "✗ Không tìm thấy khách hàng");
            request.getSession().setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/admin/customers");
            return;
        }

        // ===== LẤY THAM SỐ FILTER =====
        String keyword = request.getParameter("keyword");
        String status = request.getParameter("status");
        String dateRange = request.getParameter("dateRange");

        String dateFrom = null, dateTo = null;
        if (dateRange != null && dateRange.contains(" - ")) {
            String[] parts = dateRange.split(" - ");
            if (parts.length == 2) {
                dateFrom = convertDate(parts[0].trim());
                dateTo   = convertDate(parts[1].trim());
            }
        }

        // ===== PHÂN TRANG =====
        int page = 1;
        String pageStr = request.getParameter("page");
        if (pageStr != null) {
            try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;

        // ===== LẤY DỮ LIỆU =====
        List<CustomerDTO> customers = customerDAO.search(
            keyword, status, dateFrom, dateTo, page, PAGE_SIZE);

        int total = customerDAO.countSearch(keyword, status, dateFrom, dateTo);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;

        // ===== THỐNG KÊ NHANH =====
        int countAll = customerDAO.getAll().size();
        int countNew = customerDAO.countByStatus("Mới");
        int countContacted = customerDAO.countByStatus("Đã liên hệ");
        int countSigned = customerDAO.countByStatus("Đã ký HĐ");
        int countCancelled = customerDAO.countByStatus("Hủy");

        // ===== ĐẨY SANG JSP =====
        request.setAttribute("customers", customers);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", total);
        request.setAttribute("pageSize", PAGE_SIZE);

        request.setAttribute("keyword", keyword);
        request.setAttribute("status", status);
        request.setAttribute("dateRange", dateRange);

        request.setAttribute("countAll", countAll);
        request.setAttribute("countNew", countNew);
        request.setAttribute("countContacted", countContacted);
        request.setAttribute("countSigned", countSigned);
        request.setAttribute("countCancelled", countCancelled);

        HttpSession session = request.getSession();
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/admin/customer-list.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ===== CẬP NHẬT TOÀN BỘ THÔNG TIN KHÁCH HÀNG =====
        if ("update".equals(action)) {
            String idStr     = request.getParameter("id");
            String fullName  = request.getParameter("fullName");
            String phone     = request.getParameter("phone");
            String address   = request.getParameter("address");
            String email     = request.getParameter("email");
            String note      = request.getParameter("note");
            String status    = request.getParameter("status");

            if (idStr == null || fullName == null || fullName.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
                request.getSession().setAttribute("message", "✗ Vui lòng nhập đầy đủ Họ tên và SĐT");
                request.getSession().setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/admin/customers?action=edit&id=" + idStr);
                return;
            }

            try {
                int id = Integer.parseInt(idStr);
                CustomerDTO c = new CustomerDTO();
                c.setId(id);
                c.setFullName(fullName.trim());
                c.setPhone(phone.trim());
                c.setAddress(address != null ? address.trim() : "");
                c.setEmail(email != null ? email.trim() : "");
                c.setNote(note);
                c.setStatus(status != null ? status : "Mới");

                boolean ok = customerDAO.update(c);
                if (ok) {
                    request.getSession().setAttribute("message",
                        "✓ Đã cập nhật khách hàng #KH-" + id + " thành công");
                    request.getSession().setAttribute("messageType", "success");
                    response.sendRedirect(request.getContextPath() + "/admin/customer-detail?id=" + id);
                } else {
                    request.getSession().setAttribute("message", "✗ Cập nhật thất bại");
                    request.getSession().setAttribute("messageType", "error");
                    response.sendRedirect(request.getContextPath() + "/admin/customers?action=edit&id=" + id);
                }
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/admin/customers");
            }
            return;
        }

        // ===== CẬP NHẬT TRẠNG THÁI NHANH =====
        if ("updateStatus".equals(action)) {
            String idStr = request.getParameter("id");
            String status = request.getParameter("status");
            if (idStr != null && status != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    boolean ok = customerDAO.updateStatus(id, status);
                    if (ok) {
                        request.getSession().setAttribute("message",
                            "✓ Đã cập nhật trạng thái khách hàng #KH-" + id);
                        request.getSession().setAttribute("messageType", "success");
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // ===== XÓA NHIỀU KHÁCH HÀNG =====
        if ("deleteMultiple".equals(action)) {
            String[] ids = request.getParameterValues("ids");
            if (ids != null && ids.length > 0) {
                int count = 0;
                for (String idStr : ids) {
                    try {
                        int id = Integer.parseInt(idStr);
                        if (customerDAO.delete(id)) count++;
                    } catch (NumberFormatException ignored) {}
                }
                request.getSession().setAttribute("message",
                    "✓ Đã xóa " + count + " khách hàng");
                request.getSession().setAttribute("messageType", "success");
            }
        }

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin/customers")) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
        }
    }

    private String convertDate(String ddMMyyyy) {
        try {
            String[] parts = ddMMyyyy.split("/");
            if (parts.length == 3) {
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        } catch (Exception ignored) {}
        return null;
    }
}