package controller;

import dao.PackageDAO;
import dto.PackageDTO;
import dto.UserDTO;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    private final PackageDAO packageDAO = new PackageDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");

        HttpSession session = request.getSession(false);
        UserDTO user = (session != null) ? (UserDTO) session.getAttribute("user") : null;
        request.setAttribute("currentUser", user);

        // LẤY 4 GÓI TỪ DB
        List<PackageDTO> packages = packageDAO.getAll();
        request.setAttribute("packages", packages);
        request.setAttribute("registrationToken", utils.RegistrationSubmission.issue(request.getSession(true)));

        request.getRequestDispatcher("/view/home.jsp").forward(request, response);
    }
}
