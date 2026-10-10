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
        // GET must not write settings or migrate secrets.

        Map<String, String> settingsMap = settingsDAO.getAllAsMap();
        settingsMap.put("ai_system_prompt", ai.consultation.ConsultationPrompt.resolve(settingsMap));
        request.setAttribute("defaultSystemPrompt", ai.consultation.ConsultationPrompt.SYSTEM);
        AIService.ConfigCache consultationConfig = AIService.getConfig();
        settingsMap.put("ai_temperature", Double.toString(consultationConfig.temperature));
        settingsMap.put("ai_max_tokens", Integer.toString(consultationConfig.maxTokens));
        request.setAttribute("settingsMap", settingsMap);
        request.setAttribute("promptRevisions",settingsDAO.revisions("prompt"));

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
        if("rollbackPrompt".equals(action)) {
            try {UserDTO u=(UserDTO)request.getSession().getAttribute("user");boolean ok=settingsDAO.rollbackLatest(Integer.parseInt(request.getParameter("revisionId")),u.getId(),"prompt");if(ok)AIService.invalidateCache();request.getSession().setAttribute("message",ok?"Đã khôi phục prompt":"Không thể khôi phục bản này");}catch(Exception e){request.getSession().setAttribute("message","Không thể khôi phục prompt");}
            response.sendRedirect(request.getContextPath()+"/admin/settings");return;
        }


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
            SSRFValidator.validateBaseUrl(baseUrl,SSRFValidator.localAllowed(baseUrl));
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
            SSRFValidator.validateBaseUrl(baseUrl,SSRFValidator.localAllowed(baseUrl));
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
        HttpSession session=request.getSession();UserDTO user=(UserDTO)session.getAttribute("user");
        try {
            if(!security.AdminSecurity.admin(user)||!security.AdminSecurity.csrf(request))throw new IllegalArgumentException("Không có quyền lưu");
            String provider=request.getParameter("ai_active_provider"),model=request.getParameter("ai_active_model"),fallback=request.getParameter("ai_fallback_provider"),fallbackModel=request.getParameter("ai_fallback_model");
            double temperature=Double.parseDouble(request.getParameter("ai_temperature"));int tokens=Integer.parseInt(request.getParameter("ai_max_tokens"));
            if(!ProviderRegistry.exists(provider)||model==null||!model.matches("[A-Za-z0-9._:/-]{1,160}")||!Double.isFinite(temperature)||temperature<0||temperature>0.5||tokens<1200||tokens>2400)throw new IllegalArgumentException("Kiểm tra provider, model, temperature và token");
            if(fallback!=null&&!fallback.isEmpty()&&(!ProviderRegistry.exists(fallback)||fallbackModel==null||!fallbackModel.matches("[A-Za-z0-9._:/-]{1,160}")))throw new IllegalArgumentException("Provider dự phòng không hợp lệ");
            String prompt=request.getParameter("ai_system_prompt");if(prompt==null||prompt.length()>12000||prompt.indexOf((char)0)>=0)throw new IllegalArgumentException("Prompt tối đa 12000 ký tự");
            String url=request.getParameter("current_base_url");if(url!=null&&!url.trim().isEmpty())SSRFValidator.validateBaseUrl(url.trim(),SSRFValidator.localAllowed(url.trim()));
            Map<String,String> updates=new java.util.LinkedHashMap<>();updates.put("ai_active_provider",provider);updates.put("ai_active_model",model);updates.put("ai_fallback_provider",fallback==null?"":fallback);updates.put("ai_fallback_model",fallbackModel==null?"":fallbackModel);
            updates.put("ai_system_prompt",prompt);updates.put("gemini_system_prompt",prompt);updates.put("ai_temperature",Double.toString(temperature));updates.put("gemini_temperature",Double.toString(temperature));updates.put("ai_max_tokens",Integer.toString(tokens));updates.put("gemini_max_tokens",Integer.toString(tokens));updates.put("ai_chatbot_enabled","true".equals(request.getParameter("ai_chatbot_enabled"))||"on".equals(request.getParameter("ai_chatbot_enabled"))?"true":"false");
            if("gemini".equals(provider))updates.put("gemini_model",model);if(url!=null)updates.put("ai_provider_"+provider+"_base_url",url.trim());
            String key=request.getParameter("current_api_key");if(key!=null&&!key.trim().isEmpty()&&!CryptoUtil.isMasked(key)){String encrypted=CryptoUtil.encrypt(key.trim());updates.put("ai_provider_"+provider+"_key",encrypted);if("gemini".equals(provider))updates.put("gemini_api_key",encrypted);}
            if(!settingsDAO.saveVersioned(updates,user.getId(),"prompt"))throw new IllegalArgumentException("Không thể lưu. Kiểm tra migration; cấu hình cũ được giữ nguyên.");
            AIService.invalidateCache();session.setAttribute("message","Đã lưu cấu hình và phiên bản prompt.");session.setAttribute("messageType","success");
        }catch(Exception e){session.setAttribute("message",e instanceof IllegalArgumentException?e.getMessage():"Cấu hình không hợp lệ.");session.setAttribute("messageType","error");}
    }
}
