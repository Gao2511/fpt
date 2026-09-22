package controller;

import dao.ApiKeyHistoryDAO;
import dao.SettingsDAO;
import dto.ApiKeyHistoryDTO;
import dto.SettingsDTO;
import dto.UserDTO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/admin/settings")
public class AdminSettingsServlet extends HttpServlet {

    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final ApiKeyHistoryDAO historyDAO = new ApiKeyHistoryDAO();

    // =========================================================
    // DO GET
    // =========================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ===== XÓA 1 DÒNG LỊCH SỬ =====
        if ("deleteHistory".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    boolean ok = historyDAO.delete(id);
                    if (ok) {
                        request.getSession().setAttribute("message",
                            "✓ Đã xóa lịch sử #" + id + " thành công");
                        request.getSession().setAttribute("messageType", "success");
                    } else {
                        request.getSession().setAttribute("message", "✗ Xóa thất bại");
                        request.getSession().setAttribute("messageType", "error");
                    }
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/admin/settings");
            return;
        }

        // ===== XÓA TOÀN BỘ LỊCH SỬ =====
        if ("deleteAllHistory".equals(action)) {
            int count = historyDAO.deleteAll();
            request.getSession().setAttribute("message",
                "✓ Đã xóa toàn bộ " + count + " dòng lịch sử");
            request.getSession().setAttribute("messageType", "success");
            response.sendRedirect(request.getContextPath() + "/admin/settings");
            return;
        }

        // ===== LOAD DỮ LIỆU =====
        List<SettingsDTO> settings = settingsDAO.getAll();
        Map<String, String> settingsMap = settingsDAO.getAllAsMap();

        // Load lịch sử API key (20 dòng gần nhất)
        List<ApiKeyHistoryDTO> apiKeyHistory = historyDAO.getAll(20);
        int totalHistory = historyDAO.countAll();

        request.setAttribute("settings", settings);
        request.setAttribute("settingsMap", settingsMap);
        request.setAttribute("apiKeyHistory", apiKeyHistory);
        request.setAttribute("totalHistory", totalHistory);

        // Flash message
        HttpSession session = request.getSession();
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/admin/settings.jsp")
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

        if ("updateAI".equals(action)) {
            String apiKey = request.getParameter("gemini_api_key");
            String model = request.getParameter("gemini_model");
            String systemPrompt = request.getParameter("gemini_system_prompt");
            String note = request.getParameter("change_note");

            // Lấy thông tin admin đang đổi
            HttpSession session = request.getSession();
            UserDTO currentAdmin = (UserDTO) session.getAttribute("user");

            // Lấy giá trị cũ trước khi update
            String oldApiKey = settingsDAO.getValue("gemini_api_key");
            String oldModel = settingsDAO.getValue("gemini_model");

            boolean ok = true;
            boolean apiKeyChanged = false;

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                String newApiKey = apiKey.trim();
                ok &= settingsDAO.update("gemini_api_key", newApiKey);

                // Nếu API Key thực sự đổi → đánh dấu để ghi log
                if (oldApiKey == null || !newApiKey.equals(oldApiKey)) {
                    apiKeyChanged = true;
                }
            }

            if (model != null && !model.trim().isEmpty()) {
                ok &= settingsDAO.update("gemini_model", model.trim());
            }

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                ok &= settingsDAO.update("gemini_system_prompt", systemPrompt.trim());
            }

            // ===== GHI LOG LỊCH SỬ nếu API Key thay đổi =====
            if (ok && apiKeyChanged && currentAdmin != null) {
                String newApiKey = settingsDAO.getValue("gemini_api_key");
                String newModel = settingsDAO.getValue("gemini_model");

                String logNote = (note != null && !note.trim().isEmpty())
                               ? note.trim()
                               : "Đổi API Key từ " + maskKey(oldApiKey) + " → " + maskKey(newApiKey);

                historyDAO.log(
                    currentAdmin.getId(),
                    currentAdmin.getDisplayName(),
                    newApiKey,
                    newModel,
                    "UPDATE",
                    logNote
                );
            }

            if (ok) {
                session.setAttribute("message", "✓ Đã cập nhật cấu hình AI thành công");
                session.setAttribute("messageType", "success");
            } else {
                session.setAttribute("message", "✗ Cập nhật thất bại");
                session.setAttribute("messageType", "error");
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/settings");
    }

    /** Helper: che API key khi hiện trong note */
    private String maskKey(String key) {
        if (key == null || key.length() < 15) return key;
        return key.substring(0, 10) + "..." + key.substring(key.length() - 5);
    }
}