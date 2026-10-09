package ai.provider;

import ai.LLMProvider;
import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.dto.ModelInfo;
import ai.dto.StreamCallback;
import ai.exception.AIException;
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
 * Adapter cho Anthropic Claude Messages API (v1/messages)
 */
public class AnthropicProvider implements LLMProvider {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public String getId() {
        return "claude";
    }

    @Override
    public String getDisplayName() {
        return "Anthropic (Claude)";
    }

    @Override
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    public List<ModelInfo> getPresetModels() {
        List<ModelInfo> list = new ArrayList<>();
        list.add(new ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Khuyên dùng: Siêu nhanh, phản hồi tức thì, văn phong chuẩn mực"));
        list.add(new ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Mô hình thông minh xuất sắc nhất về lập luận và đối thoại"));
        list.add(new ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", "Mô hình cao cấp xử lý các bài toán phân tích cực khó"));
        list.add(new ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", "Phiên bản tốc độ cơ bản"));
        return list;
    }

    @Override
    public List<ModelInfo> listModels(String apiKey, String baseUrl) throws AIException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AIException(AIException.ErrorType.INVALID_KEY, 401,
                    "Vui lòng nhập Anthropic API Key trước khi đồng bộ model.", getDisplayName());
        }

        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : DEFAULT_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String endpoint = base + "/v1/models";

        try {
            HttpURLConnection conn = openConnection(endpoint, "GET", apiKey, 10000, 15000);
            int code = conn.getResponseCode();
            String responseBody = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (code < 200 || code >= 300) {
                // Một số tài khoản Anthropic chưa kích hoạt endpoint /v1/models thì fallback về preset
                if (code == 404 || code == 400) {
                    return getPresetModels();
                }
                throw AIException.fromHttp(code, responseBody, getDisplayName(), "models");
            }

            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray data = root.getAsJsonArray("data");
            List<ModelInfo> result = new ArrayList<>();

            if (data != null) {
                for (JsonElement el : data) {
                    JsonObject m = el.getAsJsonObject();
                    String id = m.has("id") ? m.get("id").getAsString() : "";
                    String displayName = m.has("display_name") ? m.get("display_name").getAsString() : id;
                    if (!id.isEmpty()) {
                        result.add(new ModelInfo(id, displayName, "Claude Model"));
                    }
                }
            }

            if (result.isEmpty()) {
                return getPresetModels();
            }
            return result;
        } catch (AIException aie) {
            throw aie;
        } catch (Exception e) {
            // Nếu lỗi mạng khi gọi models, trả về danh sách preset
            return getPresetModels();
        }
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, String apiKey, String model,
                             String baseUrl, ChatOptions options) throws AIException {
        return executeWithRetry(() -> doChatInternal(messages, apiKey, model, baseUrl, options));
    }

    private ChatResponse doChatInternal(List<ChatMessage> messages, String apiKey, String model,
                                        String baseUrl, ChatOptions options) throws AIException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AIException(AIException.ErrorType.INVALID_KEY, 401, "Thiếu Claude API Key.", getDisplayName());
        }
        if (model == null || model.trim().isEmpty()) model = "claude-3-5-haiku-20241022";

        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : DEFAULT_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String endpoint = base + "/v1/messages";

        long start = System.currentTimeMillis();
        try {
            JsonObject requestBody = buildRequestBody(messages, model, options, false);

            HttpURLConnection conn = openConnection(endpoint, "POST", apiKey, 15000, 35000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - start;

            String responseBody = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (code < 200 || code >= 300) {
                throw AIException.fromHttp(code, responseBody, getDisplayName(), model);
            }

            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            StringBuilder replyText = new StringBuilder();
            String finishReason = "end_turn";

            if (root.has("stop_reason") && !root.get("stop_reason").isJsonNull()) {
                finishReason = root.get("stop_reason").getAsString();
            }

            if (root.has("content")) {
                JsonArray contentArr = root.getAsJsonArray("content");
                for (JsonElement item : contentArr) {
                    JsonObject cObj = item.getAsJsonObject();
                    if ("text".equals(cObj.get("type").getAsString()) && cObj.has("text")) {
                        replyText.append(cObj.get("text").getAsString());
                    }
                }
            }

            ChatResponse response = new ChatResponse(replyText.toString(), getId(), model, latency);
            response.setFinishReason(finishReason);

            if (root.has("usage")) {
                JsonObject usage = root.getAsJsonObject("usage");
                if (usage.has("input_tokens")) response.setPromptTokens(usage.get("input_tokens").getAsInt());
                if (usage.has("output_tokens")) response.setCompletionTokens(usage.get("output_tokens").getAsInt());
                response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());
            }

            return response;
        } catch (AIException aie) {
            throw aie;
        } catch (Exception e) {
            throw new AIException(AIException.ErrorType.NETWORK_ERROR, "Lỗi khi gọi Claude API: " + e.getMessage(), getDisplayName(), e);
        }
    }

    @Override
    public void chatStream(List<ChatMessage> messages, String apiKey, String model,
                           String baseUrl, ChatOptions options, StreamCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new AIException(AIException.ErrorType.INVALID_KEY, 401, "Thiếu Claude API Key.", getDisplayName()));
            return;
        }
        if (model == null || model.trim().isEmpty()) model = "claude-3-5-haiku-20241022";

        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : DEFAULT_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String endpoint = base + "/v1/messages";

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

                        try {
                            JsonObject event = JsonParser.parseString(payload).getAsJsonObject();
                            String type = event.has("type") ? event.get("type").getAsString() : "";
                            if ("content_block_delta".equals(type) && event.has("delta")) {
                                JsonObject delta = event.getAsJsonObject("delta");
                                if ("text_delta".equals(delta.get("type").getAsString()) && delta.has("text")) {
                                    String text = delta.get("text").getAsString();
                                    accumulated.append(text);
                                    callback.onToken(text);
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
            callback.onError(new AIException(AIException.ErrorType.NETWORK_ERROR, "Lỗi streaming Claude: " + e.getMessage(), getDisplayName(), e));
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
        body.addProperty("model", model);

        // Anthropic bắt buộc max_tokens
        int maxTokens = (options != null && options.getMaxTokens() > 0) ? options.getMaxTokens() : 600;
        body.addProperty("max_tokens", maxTokens);

        if (options != null) {
            body.addProperty("temperature", options.getTemperature());
        }

        String systemPrompt = null;
        JsonArray msgArr = new JsonArray();

        for (ChatMessage m : messages) {
            if ("system".equalsIgnoreCase(m.getRole())) {
                systemPrompt = m.getContent();
            } else {
                JsonObject item = new JsonObject();
                item.addProperty("role", "assistant".equalsIgnoreCase(m.getRole()) ? "assistant" : "user");
                item.addProperty("content", m.getContent() != null ? m.getContent() : "");
                msgArr.add(item);
            }
        }

        // Với Claude, system là tham số riêng biệt ở root level
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            body.addProperty("system", systemPrompt.trim());
        }

        body.add("messages", msgArr);

        if (stream) {
            body.addProperty("stream", true);
        }

        return body;
    }

    @FunctionalInterface
    private interface SupplierWithAIException<T> {
        T get() throws AIException;
    }

    private <T> T executeWithRetry(SupplierWithAIException<T> action) throws AIException {
        int maxRetries = 2;
        long delayMs = 1000;
        AIException lastEx = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
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
                        } catch (InterruptedException ignored) {}
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
        conn.setRequestProperty("x-api-key", apiKey != null ? apiKey.trim() : "");
        conn.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);
        return conn;
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
