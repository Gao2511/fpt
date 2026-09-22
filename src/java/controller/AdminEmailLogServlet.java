package controller;

import dao.EmailLogDAO;
import dto.EmailLogDTO;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * AdminEmailLogServlet - Quản lý thông báo / Lịch sử email
 * URL: /admin/email-logs
 */
@WebServlet("/admin/email-logs")
public class AdminEmailLogServlet extends HttpServlet {

    private final EmailLogDAO emailLogDAO = new EmailLogDAO();
    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ===== XÓA 1 LOG =====
        if ("delete".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    boolean ok = emailLogDAO.delete(id);
                    if (ok) {
                        request.getSession().setAttribute("message",
                            "✓ Đã xóa log #" + id + " thành công");
                        request.getSession().setAttribute("messageType", "success");
                    } else {
                        request.getSession().setAttribute("message", "✗ Xóa log thất bại");
                        request.getSession().setAttribute("messageType", "error");
                    }
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/admin/email-logs");
            return;
        }

        // ===== XÓA TẤT CẢ =====
        if ("deleteAll".equals(action)) {
            int count = emailLogDAO.deleteAll();
            request.getSession().setAttribute("message",
                "✓ Đã xóa toàn bộ " + count + " log email");
            request.getSession().setAttribute("messageType", "success");
            response.sendRedirect(request.getContextPath() + "/admin/email-logs");
            return;
        }

        // ===== DANH SÁCH =====
        String keyword = request.getParameter("keyword");
        String status = request.getParameter("status");
        String dateRange = request.getParameter("dateRange");

        String dateFrom = null, dateTo = null;
        if (dateRange != null && dateRange.contains(" - ")) {
            String[] parts = dateRange.split(" - ");
            if (parts.length == 2) {
                dateFrom = convertDate(parts[0].trim());
                dateTo = convertDate(parts[1].trim());
            }
        }

        int page = 1;
        String pageStr = request.getParameter("page");
        if (pageStr != null) {
            try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;

        List<EmailLogDTO> logs = emailLogDAO.search(keyword, status, dateFrom, dateTo, page, PAGE_SIZE);
        int total = emailLogDAO.countSearch(keyword, status, dateFrom, dateTo);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;

        // Thống kê
        int totalAll = emailLogDAO.countAll();
        int totalSuccess = emailLogDAO.countByStatus("Success");
        int totalFailed = emailLogDAO.countByStatus("Failed");
        int successRate = totalAll > 0 ? (totalSuccess * 100 / totalAll) : 0;

        request.setAttribute("logs", logs);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", total);
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("keyword", keyword);
        request.setAttribute("status", status);
        request.setAttribute("dateRange", dateRange);

        request.setAttribute("totalAll", totalAll);
        request.setAttribute("totalSuccess", totalSuccess);
        request.setAttribute("totalFailed", totalFailed);
        request.setAttribute("successRate", successRate);

        // Flash message
        HttpSession session = request.getSession();
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/admin/email-log.jsp")
               .forward(request, response);
    }

    /** Chuyển dd/MM/yyyy -> yyyy-MM-dd */
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