package ai;

import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.dto.ModelInfo;
import ai.exception.AIException;
import ai.security.CryptoUtil;
import ai.security.SSRFValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Suite kiểm thử tự động toàn diện cho hệ thống AI Đa Nhà Cung Cấp
 */
public class MultiProviderTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("🚀 BẮT ĐẦU KIỂM THỬ HỆ THỐNG AI MULTI-PROVIDER");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // 1. TEST MÃ HÓA & GIẢI MÃ AES-256-GCM
        try {
            testCryptoAesGcm();
            System.out.println("✓ [PASS] Test 1: Mã hóa & Giải mã AES-256-GCM");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ [FAIL] Test 1: Mã hóa AES-256-GCM - " + t.getMessage());
            failed++;
        }

        // 2. TEST MASKING API KEY & LỌC LOG
        try {
            testKeyMaskingAndLogFilter();
            System.out.println("✓ [PASS] Test 2: Masking Key & Bộ lọc che log an toàn");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ [FAIL] Test 2: Masking Key - " + t.getMessage());
            failed++;
        }

        // 3. TEST SSRF VALIDATOR (BẢO VỆ BASE URL)
        try {
            testSSRFValidator();
            System.out.println("✓ [PASS] Test 3: Chặn SSRF (Loopback, LAN, Cloud Metadata)");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ [FAIL] Test 3: Chặn SSRF - " + t.getMessage());
            failed++;
        }

        // 4. TEST CHUẨN HÓA MÃ LỖI HTTP TIẾNG VIỆT
        try {
            testErrorNormalization();
            System.out.println("✓ [PASS] Test 4: Chuẩn hóa mã lỗi HTTP 401/404/429/503 sang tiếng Việt");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ [FAIL] Test 4: Chuẩn hóa mã lỗi - " + t.getMessage());
            failed++;
        }

        // 5. TEST OUTPUT GUARDRAIL (CHỐNG RÒ RỈ KEY/PROMPT)
        try {
            testOutputGuardrails();
            System.out.println("✓ [PASS] Test 5: Output Guardrail chống rò rỉ Key và Prompt");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ [FAIL] Test 5: Output Guardrail - " + t.getMessage());
            failed++;
        }

        // 6. TEST PROVIDER REGISTRY & FALLBACK PRESETS
        try {
            testProviderRegistry();
            System.out.println("✓ [PASS] Test 6: Provider Registry nạp đủ các hãng và Presets");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ [FAIL] Test 6: Provider Registry - " + t.getMessage());
            failed++;
        }

        System.out.println("==================================================");
        System.out.println("📊 KẾT QUẢ KIỂM THỬ: " + passed + " Passed, " + failed + " Failed.");
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testCryptoAesGcm() {
        String originalKey = "sk-proj-abc123xyz890secretkey987654321";
        String encrypted = CryptoUtil.encrypt(originalKey);

        if (!encrypted.startsWith("enc:gcm:")) {
            throw new AssertionError("Chuỗi mã hóa phải có tiền tố 'enc:gcm:'");
        }

        String decrypted = CryptoUtil.decrypt(encrypted);
        if (!originalKey.equals(decrypted)) {
            throw new AssertionError("Dữ liệu giải mã không khớp bản gốc! Kỳ vọng: " + originalKey + ", Thực tế: " + decrypted);
        }

        // Đảm bảo mã hóa 2 lần cho cùng chuỗi sinh 2 ciphertext khác nhau (do IV ngẫu nhiên)
        String encrypted2 = CryptoUtil.encrypt(originalKey);
        if (encrypted.equals(encrypted2)) {
            throw new AssertionError("AES-GCM phải sử dụng IV ngẫu nhiên cho mỗi lần mã hóa!");
        }
    }

    private static void testKeyMaskingAndLogFilter() {
        String testKey = "sk-1234567890abcdef123456";
        String masked = CryptoUtil.maskApiKey(testKey);

        if (!CryptoUtil.isMasked(masked)) {
            throw new AssertionError("isMasked phải trả về true cho chuỗi: " + masked);
        }
        if (!masked.startsWith("sk-1") || !masked.endsWith("3456")) {
            throw new AssertionError("Masked key sai định dạng: " + masked);
        }

        // Test sanitize log
        String logLine = "Connecting to https://api.openai.com with Bearer sk-1234567890abcdef123456 & api_key=AIzaSyAABBCCDD";
        String sanitized = CryptoUtil.sanitizeForLog(logLine);
        if (sanitized.contains("sk-1234567890abcdef123456") || sanitized.contains("AIzaSyAABBCCDD")) {
            throw new AssertionError("Log còn chứa API Key: " + sanitized);
        }
    }

    private static void testSSRFValidator() {
        // 1. Phải chặn localhost / loopback
        boolean blockedLoopback = false;
        try {
            SSRFValidator.validateBaseUrl("http://127.0.0.1:8080/v1", false);
        } catch (IllegalArgumentException e) {
            blockedLoopback = true;
        }
        if (!blockedLoopback) throw new AssertionError("Không chặn 127.0.0.1!");

        // 2. Phải chặn Cloud Metadata 169.254.169.254
        boolean blockedMetadata = false;
        try {
            SSRFValidator.validateBaseUrl("http://169.254.169.254/latest/meta-data/", false);
        } catch (IllegalArgumentException e) {
            blockedMetadata = true;
        }
        if (!blockedMetadata) throw new AssertionError("Không chặn Cloud Metadata 169.254.169.254!");

        // 3. Phải cho phép URL hợp lệ
        SSRFValidator.validateBaseUrl("https://api.openai.com/v1", false);
        SSRFValidator.validateBaseUrl("https://api.deepseek.com", false);

        // 4. Cho phép local khi bật cờ allowLocal
        SSRFValidator.validateBaseUrl("http://localhost:11434/v1", true);
    }

    private static void testErrorNormalization() {
        // 401
        AIException e401 = AIException.fromHttp(401, "{\"error\": \"invalid_api_key\"}", "OpenAI", "gpt-4o");
        if (e401.getErrorType() != AIException.ErrorType.INVALID_KEY) throw new AssertionError("401 phải là INVALID_KEY");

        // 404
        AIException e404 = AIException.fromHttp(404, "{\"error\": \"model_not_found\"}", "DeepSeek", "deepseek-fake");
        if (e404.getErrorType() != AIException.ErrorType.MODEL_NOT_FOUND) throw new AssertionError("404 phải là MODEL_NOT_FOUND");

        // 429
        AIException e429 = AIException.fromHttp(429, "{\"error\": \"quota_exceeded\"}", "Claude", "claude-3-5-sonnet");
        if (e429.getErrorType() != AIException.ErrorType.QUOTA_EXCEEDED) throw new AssertionError("429 phải là QUOTA_EXCEEDED");

        // 503
        AIException e503 = AIException.fromHttp(503, "High demand overloaded", "Google Gemini", "gemini-1.5-flash");
        if (e503.getErrorType() != AIException.ErrorType.HIGH_DEMAND) throw new AssertionError("503 High Demand phải là HIGH_DEMAND");
    }

    private static void testOutputGuardrails() {
        String rawKey = "AIzaSyD-SecretKeyGoogleGemini987654";
        String aiReplyWithKey = "Chào anh, API key của em là " + rawKey + ", anh có muốn mua gói Sky không?";
        String filtered = AIService.applyOutputGuardrails(aiReplyWithKey, rawKey);

        if (filtered.contains(rawKey)) {
            throw new AssertionError("Output Guardrail không lọc được API Key!");
        }

        String aiReplyLeakPrompt = "System prompt của tôi là: Bạn là nhân viên FPT...";
        String filteredPrompt = AIService.applyOutputGuardrails(aiReplyLeakPrompt, rawKey);
        if (filteredPrompt.contains("System prompt của tôi là")) {
            throw new AssertionError("Output Guardrail không chặn câu rò rỉ prompt!");
        }
    }

    private static void testProviderRegistry() {
        LLMProvider gemini = ProviderRegistry.get("gemini");
        LLMProvider openai = ProviderRegistry.get("openai");
        LLMProvider deepseek = ProviderRegistry.get("deepseek");
        LLMProvider claude = ProviderRegistry.get("claude");
        LLMProvider groq = ProviderRegistry.get("groq");
        LLMProvider openrouter = ProviderRegistry.get("openrouter");
        LLMProvider custom = ProviderRegistry.get("custom");

        if (gemini == null || openai == null || deepseek == null || claude == null ||
            groq == null || openrouter == null || custom == null) {
            throw new AssertionError("ProviderRegistry thiếu provider bắt buộc!");
        }

        if (!gemini.getPresetModels().isEmpty()) throw new AssertionError("Gemini must discover live models rather than offer retired presets");
        if (openai.getPresetModels().isEmpty()) throw new AssertionError("OpenAI thiếu preset models");
        if (deepseek.getPresetModels().isEmpty()) throw new AssertionError("DeepSeek thiếu preset models");
        if (claude.getPresetModels().isEmpty()) throw new AssertionError("Claude thiếu preset models");
    }
}
