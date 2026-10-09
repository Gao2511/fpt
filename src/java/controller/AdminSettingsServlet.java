package controller;

import ai.AIService;
import ai.LLMProvider;
import ai.ProviderRegistry;
import ai.dto.ChatResponse;
import ai.dto.ModelInfo;
import ai.exception.AIException;
import ai.security.CryptoUtil;
import ai.security.SSRFValidator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.SettingsDAO;
import dto.UserDTO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller quản trị cấu hình AI Đa Nhà Cung Cấp (Multi-Provider Admin Console)
 * Được bảo vệ bởi AdminFilter (/admin/*)
 */
@WebServlet("/admin/settings")
public class AdminSettingsServlet extends HttpServlet {

    private final SettingsDAO settingsDAO = new SettingsDAO();

    // =========================================================
    // DO GET: Hiển thị trang cài đặt AI
    // =========================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Đảm bảo dữ liệu cũ đã được migrate an toàn sang cấu trúc đa nhà cung cấp
        AIService.ensureMigration();

        Map<String, String> settingsMap = settingsDAO.getAllAsMap();
        request.setAttribute("settingsMap", settingsMap);

        // Danh sách tất cả các provider đã đăng ký trong hệ thống
        List<LLMProvider> providers = ProviderRegistry.getAll();
        request.setAttribute("providers", providers);

        // Chuẩn bị Map chứa API Key đã che mờ (Masked Key) cho từng provider để render an toàn
        Map<String, String> maskedKeys = new HashMap<>();
        Map<String, String> baseUrls = new HashMap<>();
        Map<String, Boolean> keyStatus = new HashMap<>();

        for (LLMProvider p : providers) {
            String pid = p.getId();
            String decrypted = AIService.getDecryptedApiKey(pid);
            boolean hasKey = (decrypted != null && !decrypted.trim().isEmpty());
            keyStatus.put(pid, hasKey);
            maskedKeys.put(pid, hasKey ? CryptoUtil.maskApiKey(decrypted) : "");

            String bUrl = AIService.getBaseUrl(pid);
            if (bUrl == null || bUrl.isEmpty()) bUrl = p.getDefaultBaseUrl();
            baseUrls.put(pid, bUrl != null ? bUrl : "");
        }

        request.setAttribute("maskedKeys", maskedKeys);
        request.setAttribute("baseUrls", baseUrls);
        request.setAttribute("keyStatus", keyStatus);

        // Flash message
        HttpSession session = request.getSession();
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/admin/settings.jsp").forward(request, response);
    }

    // =========================================================
    // DO POST: Xử lý cập nhật cấu hình hoặc kiểm tra kết nối API
    // =========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ===== 1. KIỂM TRA KẾT NỐI API TRỰC TIẾP (TEST CONNECTION) =====
        if ("testConnection".equals(action)) {
            response.setContentType("application/json;charset=UTF-8");
            String providerId = request.getParameter("provider");
            String model = request.getParameter("model");
            String apiKey = request.getParameter("apiKey");
            String baseUrl = request.getParameter("baseUrl");

            JsonObject result = handleTestConnection(providerId, model, apiKey, baseUrl);
            response.getWriter().write(result.toString());
            return;
        }

        // ===== 2. ĐỒNG BỘ DANH SÁCH MODEL TỪ TÀI KHOẢN API (FETCH MODELS) =====
        if ("fetchModels".equals(action)) {
            response.setContentType("application/json;charset=UTF-8");
            String providerId = request.getParameter("provider");
            String apiKey = request.getParameter("apiKey");
            String baseUrl = request.getParameter("baseUrl");

            JsonObject result = handleFetchModels(providerId, apiKey, baseUrl);
            response.getWriter().write(result.toString());
            return;
        }

        // ===== 3. LẤY CẤU HÌNH NHANH CỦA MỘT PROVIDER (SWITCH PROVIDER AJAX) =====
        if ("getProviderData".equals(action)) {
            response.setContentType("application/json;charset=UTF-8");
            String providerId = request.getParameter("provider");
            JsonObject result = handleGetProviderData(providerId);
            response.getWriter().write(result.toString());
            return;
        }

        // ===== 4. LƯU TOÀN DIỆN CẤU HÌNH AI =====
        if ("updateAI".equals(action)) {
            handleSaveSettings(request);
            response.sendRedirect(request.getContextPath() + "/admin/settings");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/settings");
    }

    // =========================================================
    // HELPER: TEST KẾT NỐI
    // =========================================================
    private JsonObject handleTestConnection(String providerId, String model, String apiKey, String baseUrl) {
        JsonObject result = new JsonObject();

        if (providerId == null || providerId.trim().isEmpty()) providerId = "gemini";
        providerId = providerId.trim().toLowerCase();

        LLMProvider provider = ProviderRegistry.get(providerId);
        if (provider == null) {
            result.addProperty("success", false);
            result.addProperty("message", "Không tìm thấy nhà cung cấp: " + providerId);
            return result;
        }

        // Lấy API Key
        if (apiKey == null || apiKey.trim().isEmpty() || CryptoUtil.isMasked(apiKey)) {
            apiKey = AIService.getDecryptedApiKey(providerId);
        } else {
            apiKey = apiKey.trim();
        }

        if (apiKey == null || apiKey.isEmpty()) {
            result.addProperty("success", false);
            result.addProperty("message", "Vui lòng nhập API Key cho " + provider.getDisplayName() + " trước khi kiểm tra!");
            return result;
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = AIService.getBaseUrl(providerId);
            if (baseUrl == null || baseUrl.isEmpty()) baseUrl = provider.getDefaultBaseUrl();
        }

        if (model == null || model.trim().isEmpty()) {
            List<ModelInfo> presets = provider.getPresetModels();
            model = !presets.isEmpty() ? presets.get(0).getId() : "default";
        }

        try {
            ChatResponse chatResp = provider.testConnection(apiKey, model, baseUrl);
            result.addProperty("success", true);
            result.addProperty("message", "Kết nối thành công tới " + provider.getDisplayName()
                    + " (Model: " + chatResp.getModel() + ", Độ trễ: " + chatResp.getLatencyMs() + "ms)!");
            result.addProperty("latencyMs", chatResp.getLatencyMs());
            result.addProperty("model", chatResp.getModel());
        } catch (AIException aie) {
            result.addProperty("success", false);
            result.addProperty("message", aie.getMessage());
            result.addProperty("errorType", aie.getErrorType().name());
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("message", "Lỗi kết nối mạng: " + e.getMessage());
        }

        return result;
    }

    // =========================================================
    // HELPER: ĐỒNG BỘ DANH SÁCH MODEL
    // =========================================================
    private JsonObject handleFetchModels(String providerId, String apiKey, String baseUrl) {
        JsonObject result = new JsonObject();

        if (providerId == null || providerId.trim().isEmpty()) providerId = "gemini";
        providerId = providerId.trim().toLowerCase();

        LLMProvider provider = ProviderRegistry.get(providerId);
        if (provider == null) {
            result.addProperty("success", false);
            result.addProperty("message", "Không tìm thấy nhà cung cấp: " + providerId);
            return result;
        }

        if (apiKey == null || apiKey.trim().isEmpty() || CryptoUtil.isMasked(apiKey)) {
            apiKey = AIService.getDecryptedApiKey(providerId);
        } else {
            apiKey = apiKey.trim();
        }

        if (apiKey == null || apiKey.isEmpty()) {
            result.addProperty("success", false);
            result.addProperty("message", "Vui lòng nhập API Key trước khi đồng bộ danh sách Model!");
            return result;
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = AIService.getBaseUrl(providerId);
            if (baseUrl == null || baseUrl.isEmpty()) baseUrl = provider.getDefaultBaseUrl();
        }

        try {
            List<ModelInfo> models = provider.listModels(apiKey, baseUrl);
            JsonArray modelArr = new JsonArray();
            for (ModelInfo m : models) {
                JsonObject item = new JsonObject();
                item.addProperty("id", m.getId());
                item.addProperty("displayName", m.getName() != null ? m.getName() : m.getId());
                item.addProperty("description", m.getDescription() != null ? m.getDescription() : "");
                modelArr.add(item);
            }

            result.addProperty("success", true);
            result.addProperty("message", "Đã đồng bộ thành công " + models.size() + " models từ " + provider.getDisplayName() + "!");
            result.add("models", modelArr);
        } catch (AIException aie) {
            result.addProperty("success", false);
            result.addProperty("message", aie.getMessage());

            // Dự phòng danh sách preset
            JsonArray presetArr = new JsonArray();
            for (ModelInfo p : provider.getPresetModels()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", p.getId());
                item.addProperty("displayName", p.getName());
                item.addProperty("description", p.getDescription());
                presetArr.add(item);
            }
            result.add("presetModels", presetArr);
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("message", "Lỗi: " + e.getMessage());
        }

        return result;
    }

    // =========================================================
    // HELPER: LẤY CẤU HÌNH KHI ADMIN SWITCH PROVIDER
    // =========================================================
    private JsonObject handleGetProviderData(String providerId) {
        JsonObject res = new JsonObject();
        if (providerId == null || providerId.trim().isEmpty()) providerId = "gemini";
        providerId = providerId.trim().toLowerCase();

        LLMProvider provider = ProviderRegistry.get(providerId);
        if (provider == null) {
            res.addProperty("success", false);
            return res;
        }

        String decrypted = AIService.getDecryptedApiKey(providerId);
        boolean hasKey = (decrypted != null && !decrypted.trim().isEmpty());
        String masked = hasKey ? CryptoUtil.maskApiKey(decrypted) : "";

        String baseUrl = AIService.getBaseUrl(providerId);
        if (baseUrl == null || baseUrl.isEmpty()) baseUrl = provider.getDefaultBaseUrl();

        JsonArray presets = new JsonArray();
        for (ModelInfo m : provider.getPresetModels()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", m.getId());
            item.addProperty("displayName", m.getName());
            item.addProperty("description", m.getDescription());
            presets.add(item);
        }

        res.addProperty("success", true);
        res.addProperty("provider", providerId);
        res.addProperty("displayName", provider.getDisplayName());
        res.addProperty("hasKey", hasKey);
        res.addProperty("maskedKey", masked);
        res.addProperty("baseUrl", baseUrl != null ? baseUrl : "");
        res.add("presetModels", presets);
        return res;
    }

    // =========================================================
    // HELPER: LƯU CẤU HÌNH VÀ GHI AUDIT LOG
    // =========================================================
    private void handleSaveSettings(HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        String adminName = (currentUser != null && currentUser.getFullName() != null)
                ? currentUser.getFullName() : "Admin";

        String activeProvider = request.getParameter("ai_active_provider");
        String activeModel = request.getParameter("ai_active_model");
        String fallbackProvider = request.getParameter("ai_fallback_provider");
        String fallbackModel = request.getParameter("ai_fallback_model");

        String apiKey = request.getParameter("current_api_key");
        String baseUrl = request.getParameter("current_base_url");

        String systemPrompt = request.getParameter("ai_system_prompt");
        String temperature = request.getParameter("ai_temperature");
        String maxTokens = request.getParameter("ai_max_tokens");
        String chatbotEnabled = request.getParameter("ai_chatbot_enabled");

        if (activeProvider == null || activeProvider.trim().isEmpty()) {
            activeProvider = "gemini";
        }
        activeProvider = activeProvider.trim().toLowerCase();

        // 1. Lưu API Key của Provider hiện tại nếu có thay đổi
        if (apiKey != null && !apiKey.trim().isEmpty() && !CryptoUtil.isMasked(apiKey)) {
            AIService.saveProviderApiKey(activeProvider, apiKey.trim());
        }

        // 2. Lưu Base URL (có SSRF Validation)
        if (baseUrl != null) {
            String trimmedUrl = baseUrl.trim();
            if (!trimmedUrl.isEmpty() && !"gemini".equals(activeProvider) && !"claude".equals(activeProvider)) {
                try {
                    boolean allowLocal = "custom".equals(activeProvider) || trimmedUrl.contains("localhost") || trimmedUrl.contains("127.0.0.1");
                    SSRFValidator.validateBaseUrl(trimmedUrl, allowLocal);
                    settingsDAO.update("ai_provider_" + activeProvider + "_base_url", trimmedUrl);
                } catch (IllegalArgumentException iae) {
                    session.setAttribute("message", "⚠️ Cảnh báo: Base URL không hợp lệ: " + iae.getMessage());
                    session.setAttribute("messageType", "warning");
                    return;
                }
            } else if (trimmedUrl.isEmpty()) {
                settingsDAO.update("ai_provider_" + activeProvider + "_base_url", "");
            }
        }

        // 3. Cập nhật Active & Fallback Provider / Model
        String oldActiveProvider = settingsDAO.getValue("ai_active_provider");
        String oldActiveModel = settingsDAO.getValue("ai_active_model");

        settingsDAO.update("ai_active_provider", activeProvider);
        if (activeModel != null && !activeModel.trim().isEmpty()) {
            settingsDAO.update("ai_active_model", activeModel.trim());
            if ("gemini".equals(activeProvider)) {
                settingsDAO.update("gemini_model", activeModel.trim());
            }
        }

        if (fallbackProvider != null) {
            settingsDAO.update("ai_fallback_provider", fallbackProvider.trim().toLowerCase());
        }
        if (fallbackModel != null) {
            settingsDAO.update("ai_fallback_model", fallbackModel.trim());
        }

        // 4. Cập nhật System Prompt, Temperature, Max Tokens
        if (systemPrompt != null) {
            settingsDAO.update("ai_system_prompt", systemPrompt.trim());
            settingsDAO.update("gemini_system_prompt", systemPrompt.trim());
        }

        if (temperature != null && !temperature.trim().isEmpty()) {
            settingsDAO.update("ai_temperature", temperature.trim());
            settingsDAO.update("gemini_temperature", temperature.trim());
        }

        if (maxTokens != null && !maxTokens.trim().isEmpty()) {
            settingsDAO.update("ai_max_tokens", maxTokens.trim());
            settingsDAO.update("gemini_max_tokens", maxTokens.trim());
        }

        // 5. Trạng thái Chatbot Enabled
        if ("true".equalsIgnoreCase(chatbotEnabled) || "on".equalsIgnoreCase(chatbotEnabled)) {
            settingsDAO.update("ai_chatbot_enabled", "true");
        } else {
            settingsDAO.update("ai_chatbot_enabled", "false");
        }

        // 6. Ghi Audit Log an toàn (không ghi API key)
        System.out.println(String.format(
                "📋 [AI Audit Log] Admin '%s' đã cập nhật cấu hình AI: Provider [%s -> %s], Model [%s -> %s], Fallback [%s/%s]",
                adminName, oldActiveProvider, activeProvider, oldActiveModel, activeModel, fallbackProvider, fallbackModel
        ));

        // 7. Làm mới Cache in-memory
        AIService.invalidateCache();

        session.setAttribute("message", "✓ Đã lưu thành công cấu hình AI Đa Nhà Cung Cấp! Thay đổi áp dụng ngay lập tức.");
        session.setAttribute("messageType", "success");
    }
}