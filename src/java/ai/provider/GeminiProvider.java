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
 * Adapter cho Google Gemini API (v1beta)
 */
public class GeminiProvider implements LLMProvider {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    @Override
    public String getId() {
        return "gemini";
    }

    @Override
    public String getDisplayName() {
        return "Google Gemini";
    }

    @Override
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    public List<ModelInfo> getPresetModels() {
        List<ModelInfo> list = new ArrayList<>();
        list.add(new ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", "Khuyên dùng: Phản hồi siêu nhanh, cân bằng, tối ưu chi phí"));
        list.add(new ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", "Thế hệ mới nhất, tốc độ cực nhanh, hỗ trợ đa nhiệm"));
        list.add(new ModelInfo("gemini-1.5-flash-8b", "Gemini 1.5 Flash-8B", "Bản tinh gọn 8B tham số, ít nghẽn tải, độ trễ cực thấp"));
        list.add(new ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", "Lý luận chuyên sâu, ngữ cảnh 2 triệu token, độ chính xác cao"));
        list.add(new ModelInfo("gemini-1.0-pro", "Gemini 1.0 Pro", "Bản tiền nhiệm cơ bản"));
        return list;
    }

    @Override
    public List<ModelInfo> listModels(String apiKey, String baseUrl) throws AIException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AIException(AIException.ErrorType.INVALID_KEY, 401,
                    "Vui lòng nhập Google Gemini API Key trước khi đồng bộ model.", getDisplayName());
        }

        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : DEFAULT_BASE_URL;
        String endpoint = base + "/v1beta/models?key=" + apiKey.trim();

        try {
            HttpURLConnection conn = openConnection(endpoint, "GET", 10000, 15000);
            int code = conn.getResponseCode();

            String responseBody = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (code < 200 || code >= 300) {
                throw AIException.fromHttp(code, responseBody, getDisplayName(), "models");
            }

            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray models = root.getAsJsonArray("models");
            List<ModelInfo> result = new ArrayList<>();

            if (models != null) {
                for (JsonElement el : models) {
                    JsonObject m = el.getAsJsonObject();
                    boolean canGenerate = false;
                    if (m.has("supportedGenerationMethods")) {
                        for (JsonElement method : m.getAsJsonArray("supportedGenerationMethods")) {
                            if ("generateContent".equals(method.getAsString())) {
                                canGenerate = true;
                                break;
                            }
                        }
                    }

                    if (canGenerate) {
                        String rawName = m.get("name").getAsString(); // e.g. "models/gemini-1.5-flash"
                        String id = rawName.startsWith("models/") ? rawName.substring(7) : rawName;
                        String displayName = m.has("displayName") ? m.get("displayName").getAsString() : id;
                        String desc = m.has("description") ? m.get("description").getAsString() : "";

                        // Lọc bỏ model không dùng để chat (embedding, aqa, etc.)
                        if (!id.contains("embedding") && !id.contains("aqa")) {
                            result.add(new ModelInfo(id, displayName, desc));
                        }
                    }
                }
            }
            return result;
        } catch (AIException aie) {
            throw aie;
        } catch (Exception e) {
            throw new AIException(AIException.ErrorType.NETWORK_ERROR, "Lỗi kết nối tới Google Gemini: " + e.getMessage(), getDisplayName(), e);
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
            throw new AIException(AIException.ErrorType.INVALID_KEY, 401, "Thiếu Gemini API Key.", getDisplayName());
        }
        if (model == null || model.trim().isEmpty()) {
            model = "gemini-1.5-flash";
        }

        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : DEFAULT_BASE_URL;
        String endpoint = base + "/v1beta/models/" + model.trim() + ":generateContent?key=" + apiKey.trim();

        long start = System.currentTimeMillis();
        try {
            JsonObject requestBody = buildRequestBody(messages, options);

            HttpURLConnection conn = openConnection(endpoint, "POST", 15000, 30000);
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
            String replyText = "";
            String finishReason = "STOP";

            if (root.has("candidates")) {
                JsonArray candidates = root.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject first = candidates.get(0).getAsJsonObject();
                    if (first.has("finishReason")) {
                        finishReason = first.get("finishReason").getAsString();
                    }
                    if (first.has("content")) {
                        JsonObject c = first.getAsJsonObject("content");
                        if (c.has("parts")) {
                            JsonArray parts = c.getAsJsonArray("parts");
                            if (parts.size() > 0 && parts.get(0).getAsJsonObject().has("text")) {
                                replyText = parts.get(0).getAsJsonObject().get("text").getAsString();
                            }
                        }
                    }
                }
            }

            ChatResponse response = new ChatResponse(replyText, getId(), model, latency);
            response.setFinishReason(finishReason);

            if (root.has("usageMetadata")) {
                JsonObject usage = root.getAsJsonObject("usageMetadata");
                if (usage.has("promptTokenCount")) response.setPromptTokens(usage.get("promptTokenCount").getAsInt());
                if (usage.has("candidatesTokenCount")) response.setCompletionTokens(usage.get("candidatesTokenCount").getAsInt());
                if (usage.has("totalTokenCount")) response.setTotalTokens(usage.get("totalTokenCount").getAsInt());
            }

            return response;
        } catch (AIException aie) {
            throw aie;
        } catch (Exception e) {
            throw new AIException(AIException.ErrorType.NETWORK_ERROR, "Lỗi kết nối gọi Gemini API: " + e.getMessage(), getDisplayName(), e);
        }
    }

    @Override
    public void chatStream(List<ChatMessage> messages, String apiKey, String model,
                           String baseUrl, ChatOptions options, StreamCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new AIException(AIException.ErrorType.INVALID_KEY, 401, "Thiếu Gemini API Key.", getDisplayName()));
            return;
        }
        if (model == null || model.trim().isEmpty()) model = "gemini-1.5-flash";

        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : DEFAULT_BASE_URL;
        String endpoint = base + "/v1beta/models/" + model.trim() + ":streamGenerateContent?alt=sse&key=" + apiKey.trim();

        long start = System.currentTimeMillis();
        StringBuilder accumulated = new StringBuilder();

        try {
            JsonObject requestBody = buildRequestBody(messages, options);
            HttpURLConnection conn = openConnection(endpoint, "POST", 15000, 45000);
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
                        String jsonPart = line.substring(6).trim();
                        if (jsonPart.isEmpty() || "[DONE]".equals(jsonPart)) continue;

                        try {
                            JsonObject chunk = JsonParser.parseString(jsonPart).getAsJsonObject();
                            if (chunk.has("candidates")) {
                                JsonArray cands = chunk.getAsJsonArray("candidates");
                                if (cands.size() > 0) {
                                    JsonObject cand = cands.get(0).getAsJsonObject();
                                    if (cand.has("content")) {
                                        JsonArray parts = cand.getAsJsonObject("content").getAsJsonArray("parts");
                                        if (parts != null && parts.size() > 0) {
                                            String token = parts.get(0).getAsJsonObject().get("text").getAsString();
                                            accumulated.append(token);
                                            callback.onToken(token);
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
            callback.onError(new AIException(AIException.ErrorType.NETWORK_ERROR, "Lỗi streaming Gemini: " + e.getMessage(), getDisplayName(), e));
        }
    }

    @Override
    public ChatResponse testConnection(String apiKey, String model, String baseUrl) throws AIException {
        List<ChatMessage> testMsgs = Collections.singletonList(ChatMessage.user("Ping"));
        ChatOptions opt = new ChatOptions(0.1, 15);
        return chat(testMsgs, apiKey, model, baseUrl, opt);
    }

    private JsonObject buildRequestBody(List<ChatMessage> messages, ChatOptions options) {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        String systemInstructionText = null;

        for (ChatMessage msg : messages) {
            if ("system".equalsIgnoreCase(msg.getRole())) {
                systemInstructionText = msg.getContent();
            } else {
                String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user";
                JsonObject item = new JsonObject();
                item.addProperty("role", role);
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", msg.getContent() != null ? msg.getContent() : "");
                parts.add(part);
                item.add("parts", parts);
                contents.add(item);
            }
        }

        if (systemInstructionText != null && !systemInstructionText.trim().isEmpty()) {
            JsonObject sysInst = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject p = new JsonObject();
            p.addProperty("text", systemInstructionText.trim());
            parts.add(p);
            sysInst.add("parts", parts);
            body.add("system_instruction", sysInst);
        }

        body.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", options != null ? options.getTemperature() : 0.7);
        genConfig.addProperty("maxOutputTokens", options != null ? options.getMaxTokens() : 600);
        if (options != null && options.getTopP() != null) {
            genConfig.addProperty("topP", options.getTopP());
        }
        body.add("generationConfig", genConfig);

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

    private HttpURLConnection openConnection(String urlStr, String method, int connectTimeout, int readTimeout) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
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
