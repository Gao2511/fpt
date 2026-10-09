package ai;

import ai.provider.AnthropicProvider;
import ai.provider.GeminiProvider;
import ai.provider.OpenAICompatibleProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trung tâm quản lý và đăng ký các nhà cung cấp AI (Provider Registry).
 * Cho phép bổ sung các hãng AI mới một cách linh hoạt mà không phải sửa code nghiệp vụ.
 */
public class ProviderRegistry {

    private static final Map<String, LLMProvider> PROVIDERS = new LinkedHashMap<>();

    static {
        // 1. Google Gemini
        register(new GeminiProvider());

        // 2. OpenAI
        register(new OpenAICompatibleProvider(
                "openai", "OpenAI (ChatGPT)", "https://api.openai.com/v1", false
        ));

        // 3. DeepSeek
        register(new OpenAICompatibleProvider(
                "deepseek", "DeepSeek", "https://api.deepseek.com", false
        ));

        // 4. Anthropic Claude
        register(new AnthropicProvider());

        // 5. OpenRouter
        register(new OpenAICompatibleProvider(
                "openrouter", "OpenRouter", "https://openrouter.ai/api/v1", false
        ));

        // 6. Groq
        register(new OpenAICompatibleProvider(
                "groq", "Groq (LPU Speed)", "https://api.groq.com/openai/v1", false
        ));

        // 7. Custom OpenAI-compatible (Admin tự nhập Base URL, hỗ trợ Ollama, LM Studio, Mistral, xAI...)
        register(new OpenAICompatibleProvider(
                "custom", "Tùy chỉnh (OpenAI-compatible / Local Ollama)", "", true
        ));
    }

    public static synchronized void register(LLMProvider provider) {
        if (provider != null) {
            PROVIDERS.put(provider.getId().toLowerCase(), provider);
        }
    }

    public static LLMProvider get(String id) {
        if (id == null) return null;
        return PROVIDERS.get(id.toLowerCase().trim());
    }

    public static boolean exists(String id) {
        if (id == null) return false;
        return PROVIDERS.containsKey(id.toLowerCase().trim());
    }

    public static List<LLMProvider> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(PROVIDERS.values()));
    }
}
