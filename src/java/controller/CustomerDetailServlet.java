package controller;

import dao.CustomerDAO;
import dao.EmailLogDAO;
import dto.CustomerDTO;
import dto.EmailLogDTO;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * CustomerDetailServlet - Xem chi tiết 1 khách hàng + lịch sử email
 * URL: /admin/customer-detail?id=xxx
 */
@WebServlet("/admin/customer-detail")
public class CustomerDetailServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final EmailLogDAO emailLogDAO = new EmailLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            CustomerDTO customer = customerDAO.getById(id);
            if (customer == null) {
                response.sendRedirect(request.getContextPath() + "/admin/customers");
                return;
            }

            List<EmailLogDTO> emailLogs = emailLogDAO.getByCustomerId(id);

            request.setAttribute("customer", customer);
            request.setAttribute("emailLogs", emailLogs);

            request.getRequestDispatcher("/view/admin/customer-detail.jsp")
                   .forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
        }
    }
}