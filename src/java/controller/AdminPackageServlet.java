package controller;

import dao.PackageDAO;
import dto.PackageDTO;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * AdminPackageServlet - Quản lý gói cước
 * URL: /admin/packages
 */
@WebServlet("/admin/packages")
public class AdminPackageServlet extends HttpServlet {

    private final PackageDAO packageDAO = new PackageDAO();
    private static final int PAGE_SIZE = 8;

    // =========================================================
    // DO GET
    // =========================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        System.out.println("🔵 [GET] action = " + action);

        // ===== SỬA GÓI CƯỚC =====
        if ("edit".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    PackageDTO pkg = packageDAO.getById(id);
                    if (pkg != null) {
                        request.setAttribute("pkg", pkg);
                        request.setAttribute("mode", "edit");
                        request.getRequestDispatcher("/view/admin/package-form.jsp")
                                .forward(request, response);
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }
            request.getSession().setAttribute("message", "✗ Không tìm thấy gói cước");
            request.getSession().setAttribute("messageType", "error");
            response.sendRedirect(request.getContextPath() + "/admin/packages");
            return;
        }

        // ===== CẬP NHẬT BADGE TYPE (từ danh sách) =====
        if ("updateBadge".equals(action)) {
            String idStr = request.getParameter("id");
            String badgeType = request.getParameter("badgeType");

            System.out.println("🔵 [updateBadge] id = " + idStr + ", badgeType = " + badgeType);

            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    // Chuẩn hóa: rỗng → null
                    if (badgeType == null || badgeType.trim().isEmpty()) {
                        badgeType = null;
                    }
                    boolean ok = packageDAO.updateBadgeType(id, badgeType);

                    if (ok) {
                        String label = badgeType == null ? "Bình thường"
                                : badgeType.equals("hot") ? "HOT"
                                : badgeType.equals("featured") ? "NỔI BẬT"
                                : "Không xác định";
                        request.getSession().setAttribute("message",
                                "✓ Đã cập nhật loại gói thành '" + label + "'");
                        request.getSession().setAttribute("messageType", "success");
                    } else {
                        request.getSession().setAttribute("message", "✗ Cập nhật thất bại");
                        request.getSession().setAttribute("messageType", "error");
                    }
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/admin/packages");
            return;
        }

        // ===== THÊM GÓI CƯỚC =====
        if ("add".equals(action)) {
            request.setAttribute("mode", "add");
            request.getRequestDispatcher("/view/admin/package-form.jsp")
                   .forward(request, response);
            return;
        }

        // ===== XÓA GÓI CƯỚC =====
        if ("delete".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    boolean ok = packageDAO.delete(id);
                    if (ok) {
                        request.getSession().setAttribute("message",
                            "✓ Đã xóa gói cước #" + id + " thành công");
                        request.getSession().setAttribute("messageType", "success");
                    } else {
                        request.getSession().setAttribute("message",
                            "✗ Xóa thất bại. Có thể gói cước đang có khách hàng sử dụng.");
                        request.getSession().setAttribute("messageType", "error");
                    }
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/admin/packages");
            return;
        }

        // ===== DANH SÁCH GÓI CƯỚC =====
        String keyword = request.getParameter("keyword");
        String sortBy = request.getParameter("sortBy");

        int page = 1;
        String pageStr = request.getParameter("page");
        if (pageStr != null) {
            try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;

        List<PackageDTO> packages = packageDAO.search(keyword, sortBy, page, PAGE_SIZE);
        int total = packageDAO.countSearch(keyword);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;

        int totalAll = packageDAO.getAll().size();
        int totalHot = packageDAO.countHot();

        request.setAttribute("packages", packages);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", total);
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("keyword", keyword);
        request.setAttribute("sortBy", sortBy);
        request.setAttribute("totalAll", totalAll);
        request.setAttribute("totalHot", totalHot);

        HttpSession session = request.getSession();
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/admin/package-list.jsp")
               .forward(request, response);
    }

    // =========================================================
    // DO POST
    // =========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        
        System.out.println("🟢 [POST] action = " + action);
        System.out.println("🟢 [POST] badgeType = " + request.getParameter("badgeType"));
        System.out.println("🟢 [POST] id = " + request.getParameter("id"));

        // ===== THÊM GÓI MỚI =====
        if ("insert".equals(action)) {
            String packageCode     = request.getParameter("packageCode");
            String name            = request.getParameter("name");
            String priceStr        = request.getParameter("price");
            String speedStr        = request.getParameter("speedMbps");
            String description     = request.getParameter("description");
            String longDescription = request.getParameter("longDescription");
            String badgeType       = request.getParameter("badgeType");

            if (packageCode == null || packageCode.trim().isEmpty()
                || name == null || name.trim().isEmpty()
                || priceStr == null || priceStr.trim().isEmpty()) {
                request.getSession().setAttribute("message", "✗ Vui lòng nhập đầy đủ Mã, Tên và Giá");
                request.getSession().setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/admin/packages?action=add");
                return;
            }

            try {
                long price = Long.parseLong(priceStr.trim());
                int speed = speedStr != null && !speedStr.trim().isEmpty()
                            ? Integer.parseInt(speedStr.trim()) : 0;

                if (packageDAO.isCodeExists(packageCode.trim(), 0)) {
                    request.getSession().setAttribute("message", "✗ Mã gói '" + packageCode + "' đã tồn tại");
                    request.getSession().setAttribute("messageType", "error");
                    response.sendRedirect(request.getContextPath() + "/admin/packages?action=add");
                    return;
                }

                PackageDTO p = new PackageDTO();
                p.setPackageCode(packageCode.trim());
                p.setName(name.trim());
                p.setPrice(price);
                p.setSpeedMbps(speed);
                p.setDescription(description != null ? description.trim() : "");
                p.setLongDescription(longDescription != null ? longDescription.trim() : "");

                if (badgeType == null || badgeType.trim().isEmpty()) {
                    p.setBadgeType(null);
                    p.setHot(false);
                } else {
                    p.setBadgeType(badgeType);
                    p.setHot("hot".equals(badgeType));
                }

                boolean ok = packageDAO.insert(p);
                if (ok) {
                    request.getSession().setAttribute("message",
                        "✓ Đã thêm gói cước '" + name + "' thành công");
                    request.getSession().setAttribute("messageType", "success");
                    response.sendRedirect(request.getContextPath() + "/admin/packages");
                } else {
                    request.getSession().setAttribute("message", "✗ Thêm gói cước thất bại");
                    request.getSession().setAttribute("messageType", "error");
                    response.sendRedirect(request.getContextPath() + "/admin/packages?action=add");
                }
            } catch (NumberFormatException e) {
                request.getSession().setAttribute("message", "✗ Giá và tốc độ phải là số");
                request.getSession().setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/admin/packages?action=add");
            }
            return;
        }

        // ===== CẬP NHẬT GÓI =====
        if ("update".equals(action)) {
            String idStr           = request.getParameter("id");
            String packageCode     = request.getParameter("packageCode");
            String name            = request.getParameter("name");
            String priceStr        = request.getParameter("price");
            String speedStr        = request.getParameter("speedMbps");
            String description     = request.getParameter("description");
            String longDescription = request.getParameter("longDescription");
            String badgeType       = request.getParameter("badgeType");

            if (idStr == null || packageCode == null || packageCode.trim().isEmpty()
                || name == null || name.trim().isEmpty()
                || priceStr == null || priceStr.trim().isEmpty()) {
                request.getSession().setAttribute("message", "✗ Vui lòng nhập đầy đủ thông tin");
                request.getSession().setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/admin/packages");
                return;
            }

            try {
                int id = Integer.parseInt(idStr);
                long price = Long.parseLong(priceStr.trim());
                int speed = speedStr != null && !speedStr.trim().isEmpty()
                            ? Integer.parseInt(speedStr.trim()) : 0;

                if (packageDAO.isCodeExists(packageCode.trim(), id)) {
                    request.getSession().setAttribute("message", "✗ Mã gói '" + packageCode + "' đã tồn tại");
                    request.getSession().setAttribute("messageType", "error");
                    response.sendRedirect(request.getContextPath() + "/admin/packages?action=edit&id=" + id);
                    return;
                }

                PackageDTO p = new PackageDTO();
                p.setId(id);
                p.setPackageCode(packageCode.trim());
                p.setName(name.trim());
                p.setPrice(price);
                p.setSpeedMbps(speed);
                p.setDescription(description != null ? description.trim() : "");
                p.setLongDescription(longDescription != null ? longDescription.trim() : "");

                if (badgeType == null || badgeType.trim().isEmpty()) {
                    p.setBadgeType(null);
                    p.setHot(false);
                } else {
                    p.setBadgeType(badgeType);
                    p.setHot("hot".equals(badgeType));
                }

                System.out.println("🟢 [UPDATE] id=" + id + ", badgeType=" + badgeType);

                boolean ok = packageDAO.update(p);
                if (ok) {
                    request.getSession().setAttribute("message",
                        "✓ Đã cập nhật gói cước '" + name + "' thành công");
                    request.getSession().setAttribute("messageType", "success");
                    response.sendRedirect(request.getContextPath() + "/admin/packages");
                } else {
                    request.getSession().setAttribute("message", "✗ Cập nhật thất bại");
                    request.getSession().setAttribute("messageType", "error");
                    response.sendRedirect(request.getContextPath() + "/admin/packages?action=edit&id=" + id);
                }
            } catch (NumberFormatException e) {
                request.getSession().setAttribute("message", "✗ Giá và tốc độ phải là số");
                request.getSession().setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/admin/packages");
            }
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/packages");
    }
}