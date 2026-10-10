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
        if (security.AdminSecurity.mutation(action) && !"POST".equals(request.getMethod())) { response.sendError(405); return; }
        System.out.println("🔵 [GET] action = " + action);

        java.util.Map<String, String> rawCms = new dao.SettingsDAO().getCms();
        java.util.Map<String, String> cmsValues = cms.SiteContent.from(rawCms);
        request.setAttribute("cmsValues", cmsValues);

        // ===== SỬA GÓI CƯỚC =====
        if ("edit".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    PackageDTO pkg = packageDAO.getById(id);
                    if (pkg != null) {
                        cms.SiteContent.products(java.util.Arrays.asList(pkg), rawCms, cmsValues, false);
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
                    if(!(badgeType==null || badgeType.equals("hot") || badgeType.equals("featured"))){response.sendError(400);return;}
                    boolean ok = packageDAO.updateBadgeType(id, badgeType);

                    if (ok) {
                        ai.AIService.invalidateCache();
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
                        ai.AIService.invalidateCache();
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
        cms.SiteContent.products(packages, rawCms, cmsValues, false);

        int total = packageDAO.countSearch(keyword);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;

        List<PackageDTO> allPackages = cms.SiteContent.products(packageDAO.getAll(), rawCms, cmsValues, false);
        int totalAll = allPackages.size();
        long totalActive = allPackages.stream().filter(PackageDTO::isActive).count();
        long totalHot = allPackages.stream().filter(p -> "hot".equals(p.getBadgeType())).count();
        long totalFeatured = allPackages.stream().filter(p -> "featured".equals(p.getBadgeType())).count();

        request.setAttribute("packages", packages);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", total);
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("keyword", keyword);
        request.setAttribute("sortBy", sortBy);
        request.setAttribute("totalAll", totalAll);
        request.setAttribute("totalActive", totalActive);
        request.setAttribute("totalHot", totalHot);
        request.setAttribute("totalFeatured", totalFeatured);

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
        if (security.AdminSecurity.mutation(action)) { doGet(request,response); return; }
        
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
                validateText(packageCode,name,description,longDescription);
                long price = Long.parseLong(priceStr.trim());
                if(price<=0 || price>100000000)throw new NumberFormatException();
                int speed = speedStr != null && !speedStr.trim().isEmpty()
                            ? Integer.parseInt(speedStr.trim()) : 0;

                if(speed<=0 || speed>100000 || !(badgeType==null || badgeType.isEmpty() || badgeType.equals("hot") || badgeType.equals("featured")))throw new NumberFormatException();
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

                applyMetadata(request,p);
                boolean ok = packageDAO.saveManaged(p,true,((dto.UserDTO)request.getSession().getAttribute("user")).getId());
                if (ok) {
                    ai.AIService.invalidateCache();
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
                request.getSession().setAttribute("message", "✗ Kiểm tra độ dài nội dung, giá, tốc độ và phí lắp đặt hợp lệ");
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
                if(id<=0)throw new NumberFormatException();
                validateText(packageCode,name,description,longDescription);
                long price = Long.parseLong(priceStr.trim());
                if(price<=0 || price>100000000)throw new NumberFormatException();
                int speed = speedStr != null && !speedStr.trim().isEmpty()
                            ? Integer.parseInt(speedStr.trim()) : 0;

                if(speed<=0 || speed>100000 || !(badgeType==null || badgeType.isEmpty() || badgeType.equals("hot") || badgeType.equals("featured")))throw new NumberFormatException();
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

                applyMetadata(request,p);
                boolean ok = packageDAO.saveManaged(p,false,((dto.UserDTO)request.getSession().getAttribute("user")).getId());
                if (ok) {
                    ai.AIService.invalidateCache();
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
                request.getSession().setAttribute("message", "✗ Kiểm tra độ dài nội dung, giá, tốc độ và phí lắp đặt hợp lệ");
                request.getSession().setAttribute("messageType", "error");
                response.sendRedirect(request.getContextPath() + "/admin/packages");
            }
            return;
        }

        // ===== CẬP NHẬT CẤU HÌNH MESH WIFI & PHÍ HÒA MẠNG CHUẨN =====
        if ("updateMesh".equals(action)) {
            dto.UserDTO u = (dto.UserDTO) request.getSession().getAttribute("user");
            if (!security.AdminSecurity.admin(u) || !security.AdminSecurity.csrf(request)) {
                response.sendError(403);
                return;
            }
            try {
                dao.SettingsDAO store = new dao.SettingsDAO();
                java.util.Map<String, String> current = cms.SiteContent.from(store.getCms());
                java.util.Map<String, String> updates = new java.util.LinkedHashMap<>();
                for (cms.SiteContent.Field f : cms.SiteContent.fields(true)) {
                    String rawParam = request.getParameter(f.getKey());
                    String val = (rawParam != null) ? cms.SiteContent.validate(f, rawParam) : current.get(f.getKey());
                    updates.put("cms." + f.getKey(), val);
                }
                boolean ok = store.saveVersioned(updates, u.getId(), "business");
                if (!ok) {
                    throw new IllegalArgumentException("Không thể lưu cấu hình Mesh WiFi. Vui lòng thử lại.");
                }
                ai.AIService.invalidateCache();
                request.getSession().setAttribute("message", "✓ Đã lưu cấu hình Mesh WiFi F1/F2 & Phí lắp đặt chuẩn thành công!");
                request.getSession().setAttribute("messageType", "success");
            } catch (Exception e) {
                request.getSession().setAttribute("message", "✗ " + (e instanceof IllegalArgumentException ? e.getMessage() : "Không thể lưu cấu hình Mesh WiFi."));
                request.getSession().setAttribute("messageType", "error");
            }
            response.sendRedirect(request.getContextPath() + "/admin/packages#mesh-settings-section");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/packages");
    }
    private static void validateText(String code,String name,String description,String detail) {
        if(code.length()>64 || name.length()>200 || (description!=null && description.length()>4000) || (detail!=null && detail.length()>12000))throw new NumberFormatException();
    }
    private static void applyMetadata(HttpServletRequest request,PackageDTO p) {
        p.setActive(!"false".equals(request.getParameter("active")));
        int order=Integer.parseInt(request.getParameter("displayOrder"));long fee=Long.parseLong(request.getParameter("installationFee"));
        if(order<0 || order>10000 || fee<0 || fee>100000000)throw new NumberFormatException();p.setDisplayOrder(order);p.setStandardInstallationFee(fee);
        p.setInstallationFeeInherited("true".equals(request.getParameter("inheritInstallationFee")));
    }

}
