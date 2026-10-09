package ai;

import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.dto.StreamCallback;
import ai.exception.AIException;
import ai.security.CryptoUtil;
import dao.SettingsDAO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Service trung tâm:
 * - Cache cấu hình in-memory
 * - Giải mã API Key an toàn
 * - Tự động Fallback sang model dự phòng khi model chính lỗi
 * - Output Guardrails: lọc rò rỉ prompt / key / dữ liệu nhạy cảm
 * - Ghi Audit Log chuyển đổi mô hình
 */
public class AIService {

    private static final SettingsDAO settingsDAO = new SettingsDAO();

    // Cache cấu hình tránh query DB liên tục mỗi request chat của khách
    private static volatile ConfigCache cachedConfig = null;
    private static long cacheExpiresAt = 0L;
    private static final long CACHE_TTL_MS = 60000L; // 60s

    public static class ConfigCache {
        public String activeProvider = "gemini";
        public String activeModel = "gemini-1.5-flash";
        public String fallbackProvider = "";
        public String fallbackModel = "";
        public String systemPrompt = "";
        public double temperature = 0.7;
        public int maxTokens = 600;
        public boolean chatbotEnabled = true;
        public Map<String, String> rawSettings = new ConcurrentHashMap<>();
    }

    /**
     * Làm mới cache cấu hình ngay lập tức (gọi sau khi Admin bấm Lưu)
     */
    public static synchronized void invalidateCache() {
        cachedConfig = null;
        cacheExpiresAt = 0L;
    }

    /**
     * Lấy cấu hình AI hiện tại (từ Cache hoặc nạp từ DB)
     */
    public static ConfigCache getConfig() {
        long now = System.currentTimeMillis();
        if (cachedConfig != null && now < cacheExpiresAt) {
            return cachedConfig;
        }

        synchronized (AIService.class) {
            if (cachedConfig != null && now < cacheExpiresAt) {
                return cachedConfig;
            }

            // Đảm bảo migration cấu hình cũ sang mới nếu cần
            ensureMigration();

            Map<String, String> map = settingsDAO.getAllAsMap();
            ConfigCache cfg = new ConfigCache();
            cfg.rawSettings = new ConcurrentHashMap<>(map);

            cfg.activeProvider = map.getOrDefault("ai_active_provider", "gemini");
            cfg.activeModel = map.getOrDefault("ai_active_model", "gemini-1.5-flash");
            cfg.fallbackProvider = map.getOrDefault("ai_fallback_provider", "");
            cfg.fallbackModel = map.getOrDefault("ai_fallback_model", "");
            cfg.systemPrompt = map.getOrDefault("ai_system_prompt",
                    map.getOrDefault("gemini_system_prompt",
                            "Bạn là chuyên viên tư vấn AI thông minh của FPT Telecom. Hotline: 0932 079 469."));

            try {
                cfg.temperature = Double.parseDouble(map.getOrDefault("ai_temperature",
                        map.getOrDefault("gemini_temperature", "0.7")));
            } catch (Exception e) { cfg.temperature = 0.7; }

            try {
                cfg.maxTokens = Integer.parseInt(map.getOrDefault("ai_max_tokens",
                        map.getOrDefault("gemini_max_tokens", "600")));
            } catch (Exception e) { cfg.maxTokens = 600; }

            String enabledStr = map.getOrDefault("ai_chatbot_enabled", "true");
            cfg.chatbotEnabled = !"false".equalsIgnoreCase(enabledStr);

            cachedConfig = cfg;
            cacheExpiresAt = now + CACHE_TTL_MS;
            return cfg;
        }
    }

    /**
     * Lấy API Key thực tế (đã giải mã) của 1 provider từ DB
     */
    public static String getDecryptedApiKey(String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) return "";
        String p = providerId.trim().toLowerCase();

        // 1. Thử lấy theo key mới: ai_provider_{name}_key
        String encKey = settingsDAO.getValue("ai_provider_" + p + "_key");

        // 2. Nếu là gemini và chưa có key mới, lấy từ gemini_api_key cũ
        if ((encKey == null || encKey.isEmpty()) && "gemini".equals(p)) {
            encKey = settingsDAO.getValue("gemini_api_key");
        }

        if (encKey == null || encKey.isEmpty()) return "";
        return CryptoUtil.decrypt(encKey);
    }

    /**
     * Lấy Base URL của provider từ DB
     */
    public static String getBaseUrl(String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) return "";
        String p = providerId.trim().toLowerCase();
        return settingsDAO.getValue("ai_provider_" + p + "_base_url");
    }

    /**
     * Lưu API Key của 1 provider (tự động mã hóa AES-256-GCM)
     */
    public static void saveProviderApiKey(String providerId, String rawApiKey) {
        if (providerId == null || rawApiKey == null) return;
        String p = providerId.trim().toLowerCase();
        String trimmedKey = rawApiKey.trim();

        // Nếu admin không sửa và ô hiển thị đang là masked thì bỏ qua không ghi đè
        if (CryptoUtil.isMasked(trimmedKey)) {
            return;
        }

        if (trimmedKey.isEmpty()) {
            settingsDAO.update("ai_provider_" + p + "_key", "");
            if ("gemini".equals(p)) settingsDAO.update("gemini_api_key", "");
            return;
        }

        String encrypted = CryptoUtil.encrypt(trimmedKey);
        settingsDAO.update("ai_provider_" + p + "_key", encrypted);
        if ("gemini".equals(p)) {
            settingsDAO.update("gemini_api_key", encrypted);
        }
    }

    // Chỉ dẫn giao thức Sổ tay khách hàng và cập nhật state JSON
    private static final String STATE_INSTRUCTION_APPENDIX =
        "\n\n=== QUY TẮC BỘ NHỚ VÀ SỔ TAY THÔNG TIN KHÁCH HÀNG (BẮT BUỘC) ===\n" +
        "1. Trong mỗi lượt trò chuyện, bạn sẽ nhận được thông tin khách hàng hiện có trong thẻ <sotay_hientai>{...}</sotay_hientai>.\n" +
        "2. Hãy ghi nhớ các thông tin khách hàng đã chia sẻ (Tên, Số điện thoại, Địa chỉ lắp đặt, Gói cước quan tâm). TUYỆT ĐỐI KHÔNG HỎI LẠI những thông tin đã có trong sổ tay!\n" +
        "3. Ở CUỐI CÙNG của mỗi câu trả lời, bạn BẮT BUỘC phải đính kèm khối JSON cập nhật thông tin trong thẻ <state>{...}</state>.\n" +
        "Định dạng chuẩn:\n" +
        "<state>{\"ten\":\"...\",\"sdt\":\"...\",\"dia_chi\":\"...\",\"goi_de_xuat\":\"...\"}</state>\n" +
        "Nếu chưa biết trường nào, hãy để chuỗi rỗng \"\" hoặc giữ nguyên giá trị cũ, tuyệt đối không bịa đặt thông tin.";

    /**
     * Gửi tin nhắn chat đến AI (Tương thích ngược khi không truyền sessionId)
     */
    public static ChatResponse askAI(String userQuestion) throws AIException {
        return askAI(userQuestion, "guest_default");
    }

    /**
     * Gửi tin nhắn chat đến AI duy trì ngữ cảnh hội thoại & sổ tay thông tin theo Session
     */
    public static ChatResponse askAI(String userQuestion, String sessionId) throws AIException {
        ConfigCache cfg = getConfig();

        if (!cfg.chatbotEnabled) {
            throw new AIException(AIException.ErrorType.SERVER_ERROR,
                    "Chatbot AI hiện đang tạm bảo trì để nâng cấp.", "System");
        }

        // Lấy session data quản lý lịch sử hội thoại và sổ tay
        ai.session.ChatSessionData sessionData = ai.session.ChatSessionManager.getOrCreate(sessionId);

        // 1. Xác định Provider & Model chính
        String primaryProviderId = cfg.activeProvider;
        String primaryModel = cfg.activeModel;
        LLMProvider primaryProvider = ProviderRegistry.get(primaryProviderId);

        if (primaryProvider == null) {
            throw new AIException(AIException.ErrorType.INVALID_REQUEST,
                    "Không tìm thấy nhà cung cấp AI '" + primaryProviderId + "'.", "System");
        }

        String primaryApiKey = getDecryptedApiKey(primaryProviderId);
        String primaryBaseUrl = getBaseUrl(primaryProviderId);

        // 2. Tóm tắt lịch sử cũ nếu vượt quá 20 lượt
        ai.session.ChatSessionManager.summarizeAndTrimIfNeeded(
                sessionData, primaryProvider, primaryApiKey, primaryModel, primaryBaseUrl
        );

        // 3. Xây dựng danh sách tin nhắn gửi sang LLM (Đúng thứ tự, đúng role)
        List<ChatMessage> messages = new ArrayList<>();

        // System Instruction kèm quy tắc Sổ tay
        String fullSystemPrompt = (cfg.systemPrompt != null ? cfg.systemPrompt.trim() : "") + STATE_INSTRUCTION_APPENDIX;
        messages.add(ChatMessage.system(fullSystemPrompt));

        // Thêm lịch sử hội thoại của session (tối đa 20 lượt gần nhất)
        List<ChatMessage> history = sessionData.getHistory();
        for (ChatMessage hMsg : history) {
            messages.add(new ChatMessage(hMsg.getRole(), hMsg.getContent()));
        }

        // Tin nhắn mới nhất của người dùng: Chèn <sotay_hientai>{state}</sotay_hientai> vào đầu
        String wrappedUserMsg = ai.session.ChatSessionManager.wrapUserMessageWithState(sessionData, userQuestion);
        messages.add(ChatMessage.user(wrappedUserMsg));

        ChatOptions options = new ChatOptions(cfg.temperature, cfg.maxTokens);

        // 4. Gọi LLM chính hoặc tự động Fallback
        ChatResponse response;
        String activeApiKeyUsed = primaryApiKey;

        try {
            response = primaryProvider.chat(messages, primaryApiKey, primaryModel, primaryBaseUrl, options);
        } catch (AIException e) {
            System.err.println("⚠️ [AI Fallback Trigger] Lỗi từ Provider chính (" + primaryProviderId + "/" + primaryModel + "): " + e.getMessage());

            // Fallback sang Provider dự phòng
            if (cfg.fallbackProvider != null && !cfg.fallbackProvider.trim().isEmpty()
                    && !cfg.fallbackProvider.equalsIgnoreCase(primaryProviderId)) {

                String fbProviderId = cfg.fallbackProvider.trim().toLowerCase();
                String fbModel = (cfg.fallbackModel != null && !cfg.fallbackModel.trim().isEmpty())
                        ? cfg.fallbackModel.trim() : "default";

                LLMProvider fbProvider = ProviderRegistry.get(fbProviderId);
                String fbApiKey = getDecryptedApiKey(fbProviderId);
                String fbBaseUrl = getBaseUrl(fbProviderId);

                if (fbProvider != null && fbApiKey != null && !fbApiKey.isEmpty()) {
                    System.out.println("🔄 [AI Fallback] Chuyển tiếp request sang Provider dự phòng: "
                            + fbProviderId + " (Model: " + fbModel + ")...");

                    try {
                        response = fbProvider.chat(messages, fbApiKey, fbModel, fbBaseUrl, options);
                        activeApiKeyUsed = fbApiKey;
                    } catch (Exception fbEx) {
                        System.err.println("❌ [AI Fallback Failed] Cả model dự phòng cũng gặp lỗi: " + fbEx.getMessage());
                        throw e;
                    }
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }

        // 5. Trích xuất <state>, cập nhật sổ tay, kiểm tra tạo Lead, và XÓA thẻ <state> khỏi câu trả lời
        String rawContent = response.getContent();
        String cleanContent = ai.session.ChatSessionManager.extractAndUpdateState(sessionData, rawContent);

        // 6. Lưu tin nhắn mới vào lịch sử hội thoại của session
        sessionData.addMessage(ChatMessage.user(userQuestion));
        sessionData.addMessage(ChatMessage.assistant(cleanContent));

        // 7. Áp dụng bộ lọc Guardrail đầu ra
        cleanContent = applyOutputGuardrails(cleanContent, activeApiKeyUsed);
        response.setContent(cleanContent);

        return response;
    }

    /**
     * Bộ lọc Guardrail: Chặn rò rỉ prompt nhạy cảm, API key, hoặc token ra ngoài cho khách
     */
    public static String applyOutputGuardrails(String rawReply, String activeApiKey) {
        if (rawReply == null) return "";
        String filtered = rawReply;

        // Xóa sạch nếu AI vô tình in chuỗi key của chính nó
        if (activeApiKey != null && activeApiKey.length() > 6) {
            filtered = filtered.replace(activeApiKey, "[PROTECTED]");
        }

        // Lọc các từ khóa nhạy cảm hệ thống
        filtered = CryptoUtil.sanitizeForLog(filtered);

        // Kiểm tra xem phản hồi có cố ý lộ System Instruction không
        String lower = filtered.toLowerCase();
        if (lower.contains("system prompt của tôi là") || lower.contains("chỉ dẫn hệ thống là:") || lower.contains("my system prompt is")) {
            return "Em là trợ lý ảo FPT Telecom. Em rất vui được hỗ trợ tư vấn các gói cước Internet, Truyền hình FPT Play cho anh/chị!";
        }

        return filtered;
    }

    /**
     * Tự động chuyển đổi (migration) cấu hình cũ sang cấu trúc đa nhà cung cấp
     */
    public static synchronized void ensureMigration() {
        String active = settingsDAO.getValue("ai_active_provider");
        if (active == null || active.trim().isEmpty()) {
            System.out.println("📦 [AI Migration] Đang tự động chuyển đổi cấu hình sang Multi-provider...");

            // 1. Đặt provider đang kích hoạt là gemini
            settingsDAO.update("ai_active_provider", "gemini");

            // 2. Chuyển model
            String oldModel = settingsDAO.getValue("gemini_model");
            settingsDAO.update("ai_active_model", (oldModel != null && !oldModel.isEmpty()) ? oldModel : "gemini-1.5-flash");

            // 3. Chuyển prompt
            String oldPrompt = settingsDAO.getValue("gemini_system_prompt");
            if (oldPrompt != null && !oldPrompt.isEmpty()) {
                settingsDAO.update("ai_system_prompt", oldPrompt);
            }

            // 4. Chuyển và mã hóa API Key nếu key cũ còn dạng plain text
            String oldKey = settingsDAO.getValue("gemini_api_key");
            if (oldKey != null && !oldKey.trim().isEmpty()) {
                if (!oldKey.startsWith("enc:gcm:")) {
                    String enc = CryptoUtil.encrypt(oldKey.trim());
                    settingsDAO.update("ai_provider_gemini_key", enc);
                    settingsDAO.update("gemini_api_key", enc);
                } else {
                    settingsDAO.update("ai_provider_gemini_key", oldKey);
                }
            }

            System.out.println("✅ [AI Migration] Hoàn tất chuyển đổi cấu hình an toàn.");
        }
    }
}
