package controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ContactPageServlet - Trang liên hệ riêng
 * URL: /contact
 */
@WebServlet("/contact")
public class ContactPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Chuyển sang JSP
        request.getRequestDispatcher("/view/contact.jsp")
               .forward(request, response);
    }
}