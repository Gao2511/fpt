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

    /**
     * Gửi tin nhắn chat đến AI (Tự động kích hoạt Fallback khi model chính gặp sự cố)
     */
    public static ChatResponse askAI(String userQuestion) throws AIException {
        ConfigCache cfg = getConfig();

        if (!cfg.chatbotEnabled) {
            throw new AIException(AIException.ErrorType.SERVER_ERROR,
                    "Chatbot AI hiện đang tạm bảo trì để nâng cấp.", "System");
        }

        List<ChatMessage> messages = new ArrayList<>();
        if (cfg.systemPrompt != null && !cfg.systemPrompt.trim().isEmpty()) {
            messages.add(ChatMessage.system(cfg.systemPrompt.trim()));
        }
        messages.add(ChatMessage.user(userQuestion));

        ChatOptions options = new ChatOptions(cfg.temperature, cfg.maxTokens);

        // 1. Thử gọi Provider chính
        String primaryProviderId = cfg.activeProvider;
        String primaryModel = cfg.activeModel;
        LLMProvider primaryProvider = ProviderRegistry.get(primaryProviderId);

        if (primaryProvider == null) {
            throw new AIException(AIException.ErrorType.INVALID_REQUEST,
                    "Không tìm thấy nhà cung cấp AI '" + primaryProviderId + "'.", "System");
        }

        String primaryApiKey = getDecryptedApiKey(primaryProviderId);
        String primaryBaseUrl = getBaseUrl(primaryProviderId);

        try {
            ChatResponse response = primaryProvider.chat(messages, primaryApiKey, primaryModel, primaryBaseUrl, options);
            // Áp dụng bộ lọc Guardrail đầu ra
            response.setContent(applyOutputGuardrails(response.getContent(), primaryApiKey));
            return response;
        } catch (AIException e) {
            System.err.println("⚠️ [AI Fallback Trigger] Lỗi từ Provider chính (" + primaryProviderId + "/" + primaryModel + "): " + e.getMessage());

            // 2. Kiểm tra xem có cấu hình Fallback khả dụng không
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
                        ChatResponse fbResp = fbProvider.chat(messages, fbApiKey, fbModel, fbBaseUrl, options);
                        fbResp.setContent(applyOutputGuardrails(fbResp.getContent(), fbApiKey));
                        return fbResp;
                    } catch (Exception fbEx) {
                        System.err.println("❌ [AI Fallback Failed] Cả model dự phòng cũng gặp lỗi: " + fbEx.getMessage());
                    }
                }
            }

            // Ném ngoại lệ ban đầu nếu không fallback được
            throw e;
        }
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
