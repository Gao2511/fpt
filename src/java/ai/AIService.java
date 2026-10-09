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
        public String activeModel = "";
        public String fallbackProvider = "";
        public String fallbackModel = "";
        public String systemPrompt = "";
        public double temperature = 0.3;
        public int maxTokens = 1200;
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
            // Reading configuration must not migrate/write production settings.

            Map<String, String> map = settingsDAO.getAllAsMap();
            map.entrySet().removeIf(entry -> entry.getValue() == null);
            ConfigCache cfg = new ConfigCache();
            cfg.rawSettings = new ConcurrentHashMap<>(map);

            cfg.activeProvider = map.getOrDefault("ai_active_provider", "gemini");
            cfg.activeModel = map.getOrDefault("ai_active_model", map.getOrDefault("gemini_model", ""));
            cfg.fallbackProvider = map.getOrDefault("ai_fallback_provider", "");
            cfg.fallbackModel = map.getOrDefault("ai_fallback_model", "");
            cfg.systemPrompt = ai.consultation.ConsultationPrompt.SYSTEM;

            try {
                cfg.temperature = Double.parseDouble(map.getOrDefault("ai_temperature",
                        map.getOrDefault("gemini_temperature", "0.3")));
            } catch (Exception e) { cfg.temperature = 0.3; }
            if (!Double.isFinite(cfg.temperature)) cfg.temperature = 0.3;
            cfg.temperature = Math.max(0, Math.min(0.5, cfg.temperature));

            try {
                cfg.maxTokens = Integer.parseInt(map.getOrDefault("ai_max_tokens",
                        map.getOrDefault("gemini_max_tokens", "1200")));
            } catch (Exception e) { cfg.maxTokens = 1200; }
            cfg.maxTokens = Math.max(1200, Math.min(2400, cfg.maxTokens));

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

    /** A one-shot compatibility call never shares customer memory. */
    public static ChatResponse askAI(String question) throws AIException {
        String id = java.util.UUID.randomUUID().toString();
        try { return askAI(question, id); }
        finally { ai.session.ChatSessionManager.removeSession(id); }
    }

    public static ChatResponse askAI(String question, String sessionId) throws AIException {
        ConfigCache cfg = getConfig();
        if (!cfg.chatbotEnabled) throw new AIException(AIException.ErrorType.SERVER_ERROR,
            "Chatbot đang bảo trì. Anh/chị có thể gửi biểu mẫu tư vấn trên trang.", "System");
        ai.consultation.ProductCatalog catalog = new ai.consultation.ProductCatalog(new dao.PackageDAO().getAll());
        ai.consultation.ConsultationEngine.Target primary = target(cfg, cfg.activeProvider, cfg.activeModel);
        ai.consultation.ConsultationEngine.Target fallback = target(cfg, cfg.fallbackProvider, cfg.fallbackModel);
        if (primary != null && fallback != null && primary.provider.getId().equals(fallback.provider.getId()) && primary.model.equals(fallback.model)) fallback = null;
        ChatOptions options = new ChatOptions(cfg.temperature, cfg.maxTokens);
        return new ai.consultation.ConsultationEngine().chat(question,
            ai.session.ChatSessionManager.getOrCreate(sessionId), catalog, primary, fallback, options);
    }

    private static ai.consultation.ConsultationEngine.Target target(ConfigCache cfg, String providerId, String model) {
        LLMProvider provider = ProviderRegistry.get(providerId);
        if (provider == null || model == null || model.trim().isEmpty()) return null;
        String key = cfg.rawSettings.get("ai_provider_" + provider.getId() + "_key");
        if ((key == null || key.isEmpty()) && "gemini".equals(provider.getId())) key = cfg.rawSettings.get("gemini_api_key");
        return new ai.consultation.ConsultationEngine.Target(provider, CryptoUtil.decrypt(key), model.trim(), cfg.rawSettings.get("ai_provider_" + provider.getId() + "_base_url"));
    }
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
            settingsDAO.update("ai_active_model", (oldModel != null && !oldModel.isEmpty()) ? oldModel : "");

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
