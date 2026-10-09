package ai.provider;

import ai.LLMProvider;
import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.dto.ModelInfo;
import ai.dto.StreamCallback;
import ai.exception.AIException;
import ai.security.SSRFValidator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter đa năng cho tất cả các dịch vụ tương thích chuẩn OpenAI Chat Completions:
 * OpenAI, DeepSeek, Groq, OpenRouter, Mistral, Together AI, xAI, Ollama và Custom.
 */
public class OpenAICompatibleProvider implements LLMProvider {

    private final String id;
    private final String displayName;
    private final String defaultBaseUrl;
    private final boolean allowCustomBaseUrl;

    public OpenAICompatibleProvider(String id, String displayName, String defaultBaseUrl, boolean allowCustomBaseUrl) {
        this.id = id;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.allowCustomBaseUrl = allowCustomBaseUrl;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public boolean isAllowCustomBaseUrl() {
        return allowCustomBaseUrl;
    }

    @Override
    public List<ModelInfo> getPresetModels() {
        List<ModelInfo> list = new ArrayList<>();
        if ("openai".equals(id)) {
            list.add(new ModelInfo("gpt-4o-mini", "GPT-4o Mini", "Khuyên dùng: Siêu nhanh, thông minh, tối ưu chi phí"));
            list.add(new ModelInfo("gpt-4o", "GPT-4o", "Mô hình flagship đa thể thức thông minh nhất của OpenAI"));
            list.add(new ModelInfo("gpt-4-turbo", "GPT-4 Turbo", "Mô hình lý luận mạnh mẽ"));
            list.add(new ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "Mô hình cơ bản tiêu chuẩn"));
        } else if ("deepseek".equals(id)) {
            list.add(new ModelInfo("deepseek-chat", "DeepSeek-V3 (Chat)", "Khuyên dùng: Siêu thông minh, phản hồi tự nhiên, chi phí cực thấp"));
            list.add(new ModelInfo("deepseek-reasoner", "DeepSeek-R1 (Reasoner)", "Mô hình tư duy chuỗi suy luận (CoT) chuyên sâu"));
        } else if ("groq".equals(id)) {
            list.add(new ModelInfo("llama-3.3-70b-versatile", "Llama 3.3 70B (Groq)", "Siêu tốc độ LPU, thông minh vượt trội"));
            list.add(new ModelInfo("llama-3.1-8b-instant", "Llama 3.1 8B Instant", "Tốc độ bàn thờ (>700 tokens/s), phản hồi tức thì"));
            list.add(new ModelInfo("mixtral-8x7b-32768", "Mixtral 8x7B (Groq)", "MoE chất lượng cao, ngữ cảnh lớn"));
        } else if ("openrouter".equals(id)) {
            list.add(new ModelInfo("deepseek/deepseek-chat", "DeepSeek V3 (OpenRouter)", "Định tuyến tự động tối ưu giá và tốc độ"));
            list.add(new ModelInfo("google/gemini-2.0-flash-001", "Gemini 2.0 Flash (OpenRouter)", "Hỗ trợ qua OpenRouter gateway"));
            list.add(new ModelInfo("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B (OpenRouter)", "Model mã nguồn mở đỉnh cao"));
        } else {
            // Custom
            list.add(new ModelInfo("default", "Default Model", "Model mặc định của server tương thích OpenAI"));
        }
        return list;
    }

    private String resolveBaseUrl(String providedUrl) {
        String base = (providedUrl != null && !providedUrl.trim().isEmpty()) ? providedUrl.trim() : defaultBaseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    @Override
    public List<ModelInfo> listModels(String apiKey, String baseUrl) throws AIException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AIException(AIException.ErrorType.INVALID_KEY, 401,
                    "Vui lòng nhập API Key của " + displayName + " trước khi đồng bộ model.", getDisplayName());
        }

        String base = resolveBaseUrl(baseUrl);
        validateUrlSafety(base);

        String endpoint = base + "/models";

        try {
            HttpURLConnection conn = openConnection(endpoint, "GET", apiKey, 10000, 15000);
            int code = conn.getResponseCode();
            String responseBody = readResponse(conn, code);

            if (code < 200 || code >= 300) {
                throw AIException.fromHttp(code, responseBody, getDisplayName(), "models");
            }

            JsonObject root = parseJsonLenient(responseBody);
            JsonArray data = root.has("data") ? root.getAsJsonArray("data") : root.getAsJsonArray("models");
            List<ModelInfo> result = new ArrayList<>();

            if (data != null) {
                for (JsonElement el : data) {
                    JsonObject m = el.getAsJsonObject();
                    String id = m.has("id") ? m.get("id").getAsString() : "";
                    if (id.isEmpty()) continue;

                    // Lọc bỏ các model không phải chat
                    String idLower = id.toLowerCase();
                    if (isChatModelId(idLower)) {
                        String name = m.has("name") ? m.get("name").getAsString() : id;
                        String desc = m.has("description") ? m.get("description").getAsString() : "";
                        result.add(new ModelInfo(id, name, desc));
                    }
                }
            }

            // Sắp xếp thứ tự ưu tiên
            Collections.sort(result, (a, b) -> a.getId().compareToIgnoreCase(b.getId()));
            return result;
        } catch (AIException aie) {
            throw aie;
        } catch (Exception e) {
            throw new AIException(AIException.ErrorType.NETWORK_ERROR, "Lỗi kết nối tới " + getDisplayName() + ": " + e.getMessage(), getDisplayName(), e);
        }
    }

    private boolean isChatModelId(String idLower) {
        if (idLower.contains("embed") || idLower.contains("whisper") || idLower.contains("tts") ||
            idLower.contains("dall-e") || idLower.contains("davinci") || idLower.contains("babbage") ||
            idLower.contains("moderation") || idLower.contains("realtime") || idLower.contains("transcribe") ||
            idLower.contains("deep-research") || idLower.contains("audio") || idLower.contains("image")) {
            return false;
        }
        return true;
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, String apiKey, String model,
                             String baseUrl, ChatOptions options) throws AIException {
        return executeWithRetry(options, () -> doChatInternal(messages, apiKey, model, baseUrl, options));
    }

    private ChatResponse doChatInternal(List<ChatMessage> messages, String apiKey, String model,
                                        String baseUrl, ChatOptions options) throws AIException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AIException(AIException.ErrorType.INVALID_KEY, 401, "Thiếu API Key của " + displayName, getDisplayName());
        }

        String base = resolveBaseUrl(baseUrl);
        validateUrlSafety(base);

        String endpoint = base + "/chat/completions";
        long start = System.currentTimeMillis();
        HttpURLConnection conn = null;
        java.util.concurrent.ScheduledFuture<?> deadline = null;

        try {
            JsonObject requestBody = buildRequestBody(messages, model, options, false);

            conn = openConnection(endpoint, "POST", apiKey, (options == null ? 15000 : options.remainingMillis(10000)), (options == null ? 35000 : options.remainingMillis(20000)));
            deadline = ProviderDeadline.watch(conn, options);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - start;

            String responseBody = readResponse(conn, code);

            if (code < 200 || code >= 300) {
                throw AIException.fromHttp(code, responseBody, getDisplayName(), model);
            }

            JsonObject root = parseJsonLenient(responseBody);
            String replyText = "";
            String finishReason = null;

            if (root.has("choices")) {
                JsonArray choices = root.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject first = choices.get(0).getAsJsonObject();
                    if (first.has("finish_reason") && !first.get("finish_reason").isJsonNull()) {
                        finishReason = first.get("finish_reason").getAsString();
                    }
                    if (first.has("message")) {
                        JsonObject msg = first.getAsJsonObject("message");
                        if (msg.has("content") && !msg.get("content").isJsonNull()) {
                            replyText = msg.get("content").getAsString();
                        }
                    }
                }
            }

            ChatResponse response = new ChatResponse(replyText, getId(), model, latency);
            response.setFinishReason(finishReason);

            if (root.has("usage")) {
                JsonObject usage = root.getAsJsonObject("usage");
                if (usage.has("prompt_tokens")) response.setPromptTokens(usage.get("prompt_tokens").getAsInt());
                if (usage.has("completion_tokens")) response.setCompletionTokens(usage.get("completion_tokens").getAsInt());
                if (usage.has("total_tokens")) response.setTotalTokens(usage.get("total_tokens").getAsInt());
            }

            return response;
        } catch (AIException aie) {
            throw aie;
        } catch (Exception e) {
            throw new AIException((e instanceof java.net.SocketTimeoutException || options != null && options.isExpired() ? AIException.ErrorType.TIMEOUT : AIException.ErrorType.NETWORK_ERROR), "Lỗi khi gọi API " + displayName + ": " + e.getMessage(), getDisplayName(), e);
        } finally {
            if (deadline != null) deadline.cancel(false);
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    public void chatStream(List<ChatMessage> messages, String apiKey, String model,
                           String baseUrl, ChatOptions options, StreamCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new AIException(AIException.ErrorType.INVALID_KEY, 401, "Thiếu API Key của " + displayName, getDisplayName()));
            return;
        }

        String base = resolveBaseUrl(baseUrl);
        try {
            validateUrlSafety(base);
        } catch (Exception e) {
            callback.onError(new AIException(AIException.ErrorType.INVALID_REQUEST, e.getMessage(), getDisplayName()));
            return;
        }

        String endpoint = base + "/chat/completions";
        long start = System.currentTimeMillis();
        StringBuilder accumulated = new StringBuilder();

        try {
            JsonObject requestBody = buildRequestBody(messages, model, options, true);

            HttpURLConnection conn = openConnection(endpoint, "POST", apiKey, 15000, 45000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String err = readStream(conn.getErrorStream());
                callback.onError(AIException.fromHttp(code, err, getDisplayName(), model));
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String payload = line.substring(6).trim();
                        if (payload.isEmpty()) continue;
                        if ("[DONE]".equals(payload)) break;

                        try {
                            JsonObject chunk = JsonParser.parseString(payload).getAsJsonObject();
                            if (chunk.has("choices")) {
                                JsonArray choices = chunk.getAsJsonArray("choices");
                                if (choices.size() > 0) {
                                    JsonObject first = choices.get(0).getAsJsonObject();
                                    if (first.has("delta")) {
                                        JsonObject delta = first.getAsJsonObject("delta");
                                        if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                            String piece = delta.get("content").getAsString();
                                            accumulated.append(piece);
                                            callback.onToken(piece);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            long latency = System.currentTimeMillis() - start;
            ChatResponse finalResp = new ChatResponse(accumulated.toString(), getId(), model, latency);
            callback.onComplete(finalResp);
        } catch (Exception e) {
            callback.onError(new AIException(AIException.ErrorType.NETWORK_ERROR, "Lỗi streaming từ " + displayName + ": " + e.getMessage(), getDisplayName(), e));
        }
    }

    @Override
    public ChatResponse testConnection(String apiKey, String model, String baseUrl) throws AIException {
        List<ChatMessage> testMsgs = Collections.singletonList(ChatMessage.user("Ping"));
        ChatOptions opt = new ChatOptions(0.1, 15);
        return chat(testMsgs, apiKey, model, baseUrl, opt);
    }

    private JsonObject buildRequestBody(List<ChatMessage> messages, String model, ChatOptions options, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", (model != null && !model.trim().isEmpty()) ? model.trim() : "gpt-4o-mini");

        JsonArray msgArr = new JsonArray();
        for (ChatMessage m : messages) {
            JsonObject item = new JsonObject();
            item.addProperty("role", m.getRole());
            item.addProperty("content", m.getContent() != null ? m.getContent() : "");
            msgArr.add(item);
        }
        body.add("messages", msgArr);
        if (options != null && options.getResponseSchema() != null && "openai".equals(id) && model != null &&
            (model.equals("gpt-4o") || model.equals("gpt-4o-mini") || model.equals("gpt-4o-mini-2024-07-18") ||
             model.equals("gpt-4o-2024-08-06") || model.equals("gpt-4o-2024-11-20") || model.startsWith("gpt-4.1"))) {
            JsonObject schema = new JsonObject(); schema.addProperty("name", "fpt_consultation");
            schema.addProperty("strict", true); schema.add("schema", options.getResponseSchema());
            JsonObject format = new JsonObject(); format.addProperty("type", "json_schema"); format.add("json_schema", schema);
            body.add("response_format", format);
        }

        if (options != null) {
            body.addProperty("temperature", options.getTemperature());
            body.addProperty("max_tokens", options.getMaxTokens());
            if (options.getTopP() != null) {
                body.addProperty("top_p", options.getTopP());
            }
        }
        if (stream) {
            body.addProperty("stream", true);
        }

        return body;
    }

    private void validateUrlSafety(String url) throws AIException {
        try {
            boolean allowLocal = "custom".equals(id) || url.contains("localhost") || url.contains("127.0.0.1") || url.contains("11434");
            SSRFValidator.validateBaseUrl(url, allowLocal);
        } catch (IllegalArgumentException iae) {
            throw new AIException(AIException.ErrorType.INVALID_REQUEST, iae.getMessage(), getDisplayName());
        }
    }

    @FunctionalInterface
    private interface SupplierWithAIException<T> {
        T get() throws AIException;
    }

    private <T> T executeWithRetry(ChatOptions options, SupplierWithAIException<T> action) throws AIException {
        int maxRetries = options != null && options.getResponseSchema() != null ? 0 : 2;
        long delayMs = 1000;
        AIException lastEx = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (options != null && options.isExpired()) throw new AIException(AIException.ErrorType.TIMEOUT, "Request deadline exceeded", getDisplayName());
            try {
                return action.get();
            } catch (AIException e) {
                lastEx = e;
                if (e.getErrorType() == AIException.ErrorType.QUOTA_EXCEEDED ||
                    e.getErrorType() == AIException.ErrorType.HIGH_DEMAND ||
                    e.getErrorType() == AIException.ErrorType.SERVER_ERROR) {
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(delayMs * (attempt + 1));
                        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AIException(AIException.ErrorType.TIMEOUT, "Interrupted", getDisplayName()); }
                        continue;
                    }
                }
                throw e;
            }
        }
        throw lastEx;
    }

    private HttpURLConnection openConnection(String urlStr, String method, String apiKey, int connectTimeout, int readTimeout) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setUseCaches(false);
        conn.setRequestProperty("Connection", "keep-alive");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        }
        if ("openrouter".equals(id)) {
            conn.setRequestProperty("HTTP-Referer", "https://fpt.gialai.vn");
            conn.setRequestProperty("X-Title", "FPT Telecom AI Assistant");
        }
        return conn;
    }

    private String readResponse(HttpURLConnection conn, int code) throws IOException {
        InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();
        if (stream == null) return "";

        String encoding = conn.getContentEncoding();
        if ("gzip".equalsIgnoreCase(encoding)) {
            stream = new java.util.zip.GZIPInputStream(stream);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
            stream = new java.util.zip.InflaterInputStream(stream);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private JsonObject parseJsonLenient(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new JsonObject();
        }
        com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new StringReader(jsonStr));
        reader.setLenient(true);
        return JsonParser.parseReader(reader).getAsJsonObject();
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
