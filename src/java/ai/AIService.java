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

    public static final String DEFAULT_PRIMARY_TEST_MODEL = "gemini-3.8-flash";
    public static final String DEFAULT_BACKUP_MODEL = "gemini-3.5-flash-lite";
    public static final String DEFAULT_PRIMARY_THINKING_LEVEL = "medium";
    public static final String DEFAULT_BACKUP_THINKING_LEVEL = "low";
    public static final int DEFAULT_BACKUP_MAX_TOKENS = 1200;

    public static class ConfigCache {
        public String activeProvider = "gemini";
        public String activeModel = "";
        public String thinkingLevel = DEFAULT_PRIMARY_THINKING_LEVEL;
        public boolean fallbackEnabled = false;
        public String fallbackProvider = "";
        public String fallbackModel = "";
        public String fallbackThinkingLevel = DEFAULT_BACKUP_THINKING_LEVEL;
        public int fallbackMaxTokens = DEFAULT_BACKUP_MAX_TOKENS;
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

    public static ConfigCache parseConfig(Map<String, String> sourceMap) {
        Map<String, String> map = new ConcurrentHashMap<>();
        if (sourceMap != null) {
            for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        }
        ConfigCache cfg = new ConfigCache();
        cfg.rawSettings = new ConcurrentHashMap<>(map);

        cfg.activeProvider = map.getOrDefault("ai_active_provider", "gemini").trim();
        cfg.activeModel = map.getOrDefault("ai_active_model", map.getOrDefault("gemini_model", "")).trim();
        cfg.thinkingLevel = resolveThinkingLevel(cfg.activeModel, map.get("ai_thinking_level"), DEFAULT_PRIMARY_THINKING_LEVEL);

        cfg.fallbackProvider = map.getOrDefault("ai_fallback_provider", "").trim();
        cfg.fallbackModel = map.getOrDefault("ai_fallback_model", "").trim();
        if (map.containsKey("ai_fallback_enabled")) {
            String fbFlag = map.get("ai_fallback_enabled").trim();
            cfg.fallbackEnabled = !"false".equalsIgnoreCase(fbFlag) && !"0".equals(fbFlag) && !fbFlag.isEmpty();
        } else {
            cfg.fallbackEnabled = !cfg.fallbackProvider.isEmpty() && !cfg.fallbackModel.isEmpty();
        }
        if (cfg.fallbackEnabled && "gemini".equalsIgnoreCase(cfg.fallbackProvider) && cfg.fallbackModel.isEmpty()) {
            cfg.fallbackModel = DEFAULT_BACKUP_MODEL;
        }
        cfg.fallbackThinkingLevel = resolveThinkingLevel(cfg.fallbackModel, map.get("ai_fallback_thinking_level"), DEFAULT_BACKUP_THINKING_LEVEL);

        try {
            cfg.fallbackMaxTokens = Integer.parseInt(map.getOrDefault("ai_fallback_max_tokens", String.valueOf(DEFAULT_BACKUP_MAX_TOKENS)).trim());
        } catch (Exception e) { cfg.fallbackMaxTokens = DEFAULT_BACKUP_MAX_TOKENS; }
        cfg.fallbackMaxTokens = Math.max(1200, Math.min(2400, cfg.fallbackMaxTokens));

        cfg.systemPrompt = ai.consultation.ConsultationPrompt.resolve(map);

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
        return cfg;
    }

    public static String resolveThinkingLevel(String model, String configuredLevel, String defaultGemini3Level) {
        if (!ai.provider.GeminiProvider.isGemini3Model(model)) {
            return "";
        }
        String fallbackLevel = (defaultGemini3Level != null && !defaultGemini3Level.trim().isEmpty())
                ? defaultGemini3Level.trim().toLowerCase(java.util.Locale.ROOT)
                : DEFAULT_PRIMARY_THINKING_LEVEL;
        String candidate = (configuredLevel != null && !configuredLevel.trim().isEmpty())
                ? configuredLevel.trim().toLowerCase(java.util.Locale.ROOT)
                : fallbackLevel;
        String normalized = ai.provider.GeminiProvider.normalizeThinkingLevel(model, candidate);
        return normalized != null ? normalized : fallbackLevel;
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
            ConfigCache cfg = parseConfig(map);

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
        if(rag.runtime.GeminiRuntimeConfig.smokePlanConfigured(System.getenv())){
            try{
                rag.runtime.GeminiRuntime runtime=rag.runtime.GeminiRuntime.active();
                ConfigCache local=getConfig();
                if(!local.chatbotEnabled||!"gemini".equals(local.activeProvider)||!"gemini-3.8-flash".equals(local.activeModel)||!"medium".equals(local.thinkingLevel)||
                    (runtime.config.backupEnabled&&(!local.fallbackEnabled||!"gemini".equals(local.fallbackProvider)||!"gemini-3.5-flash-lite".equals(local.fallbackModel)||!"low".equals(local.fallbackThinkingLevel))))throw new IllegalArgumentException();
                return runtime.chat(question,ai.session.ChatSessionManager.getOrCreate(sessionId),cms.SiteContent.publicCatalog(),local.systemPrompt);
            }catch(AIException e){throw e;}catch(Exception e){throw new AIException(AIException.ErrorType.INVALID_REQUEST,"Gemini runtime chưa được cấu hình/phê duyệt đầy đủ. Kiểm tra môi trường và kế hoạch riêng.","System");}
        }
        ConfigCache cfg = getConfig();
        if (!cfg.chatbotEnabled) throw new AIException(AIException.ErrorType.SERVER_ERROR,
            "Chatbot đang bảo trì. Anh/chị có thể gửi biểu mẫu tư vấn trên trang.", "System");
        ai.consultation.ProductCatalog catalog = cms.SiteContent.publicCatalog();
        ai.consultation.ConsultationEngine.Target primary = buildPrimaryTarget(cfg);
        ai.consultation.ConsultationEngine.Target fallback = buildFallbackTarget(cfg);
        ChatOptions options = new ChatOptions(cfg.temperature, cfg.maxTokens);
        options.setThinkingLevel(cfg.thinkingLevel);
        String geminiKey = getDecryptedApiKey("gemini");
        return new ai.consultation.ConsultationEngine(rag.RagFactory.consultation(geminiKey, cfg.rawSettings)).chat(question,
            ai.session.ChatSessionManager.getOrCreate(sessionId), catalog, primary, fallback, options, cfg.systemPrompt);
    }

    public static ai.consultation.ConsultationEngine.Target buildPrimaryTarget(ConfigCache cfg) {
        if (cfg == null) return null;
        return target(cfg, cfg.activeProvider, cfg.activeModel, cfg.thinkingLevel, cfg.maxTokens);
    }

    public static ai.consultation.ConsultationEngine.Target buildFallbackTarget(ConfigCache cfg) {
        if (cfg == null || !cfg.fallbackEnabled) return null;
        ai.consultation.ConsultationEngine.Target primary = buildPrimaryTarget(cfg);
        ai.consultation.ConsultationEngine.Target fallback = target(
                cfg, cfg.fallbackProvider, cfg.fallbackModel, cfg.fallbackThinkingLevel, cfg.fallbackMaxTokens);
        if (fallback == null || !fallback.usable()) return null;
        if (primary != null && ai.consultation.ConsultationEngine.isSameModelTarget(primary, fallback)) return null;
        if (primary != null && ai.consultation.ConsultationEngine.isKeyRotationAttempt(primary, fallback)) return null;
        return fallback;
    }

    private static ai.consultation.ConsultationEngine.Target target(
            ConfigCache cfg, String providerId, String model, String thinkingLevel, int maxTokens) {
        LLMProvider provider = ProviderRegistry.get(providerId);
        if (provider == null || model == null || model.trim().isEmpty()) return null;
        // Same provider always uses the single configured provider API key; automatic key/project rotation is forbidden.
        String key = cfg.rawSettings.get("ai_provider_" + provider.getId() + "_key");
        if ((key == null || key.isEmpty()) && "gemini".equals(provider.getId())) key = cfg.rawSettings.get("gemini_api_key");
        String decrypted = (key != null && !key.isEmpty()) ? CryptoUtil.decrypt(key) : "";
        if (decrypted.isEmpty() && "gemini".equals(provider.getId())) {
            String envKey = System.getenv("GEMINI_API_KEY");
            if (envKey != null && !envKey.trim().isEmpty()) {
                decrypted = envKey.trim();
            }
        }
        return new ai.consultation.ConsultationEngine.Target(
                provider,
                decrypted,
                model.trim(),
                cfg.rawSettings.get("ai_provider_" + provider.getId() + "_base_url"),
                thinkingLevel,
                maxTokens);
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
