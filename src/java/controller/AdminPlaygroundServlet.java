package controller;

import ai.LLMProvider;
import ai.ProviderRegistry;
import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.dto.StreamCallback;
import ai.exception.AIException;
import ai.security.CryptoUtil;
import com.google.gson.JsonObject;
import dao.SettingsDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet dành riêng cho Admin thử nghiệm mô hình AI (Live Playground)
 * Được bảo vệ bởi AdminFilter (/admin/*)
 * Cho phép thử nghiệm các thông số nháp (kể cả khi chưa bấm Lưu)
 * Hỗ trợ cả trả lời JSON thường và Server-Sent Events (SSE Streaming)
 */
@WebServlet("/admin/ai-playground")
public class AdminPlaygroundServlet extends HttpServlet {

    private final SettingsDAO settingsDAO = new SettingsDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "chat";
        }

        String userMessage = request.getParameter("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            JsonObject err = new JsonObject();
            err.addProperty("success", false);
            err.addProperty("message", "Vui lòng nhập câu hỏi thử nghiệm.");
            response.getWriter().write(err.toString());
            return;
        }

        String providerId = request.getParameter("provider");
        String model = request.getParameter("model");
        String apiKey = request.getParameter("apiKey");
        String baseUrl = request.getParameter("baseUrl");
        String systemPrompt = request.getParameter("systemPrompt");
        String tempStr = request.getParameter("temperature");
        String maxTokensStr = request.getParameter("maxTokens");

        if (providerId == null || providerId.trim().isEmpty()) {
            providerId = settingsDAO.getValue("ai_active_provider");
            if (providerId == null || providerId.isEmpty()) providerId = "gemini";
        }
        providerId = providerId.trim().toLowerCase();

        LLMProvider provider = ProviderRegistry.get(providerId);
        if (provider == null) {
            response.setContentType("application/json;charset=UTF-8");
            JsonObject err = new JsonObject();
            err.addProperty("success", false);
            err.addProperty("message", "Không tìm thấy Provider: " + providerId);
            response.getWriter().write(err.toString());
            return;
        }

        // Xử lý API Key: nếu client truyền lên chuỗi masked hoặc rỗng thì đọc từ DB
        if (apiKey == null || apiKey.trim().isEmpty() || CryptoUtil.isMasked(apiKey)) {
            apiKey = ai.AIService.getDecryptedApiKey(providerId);
        } else {
            apiKey = apiKey.trim();
        }

        // Xử lý Base URL
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = ai.AIService.getBaseUrl(providerId);
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                baseUrl = provider.getDefaultBaseUrl();
            }
        }

        if (model == null || model.trim().isEmpty()) {
            model = settingsDAO.getValue("ai_active_model");
            if (model == null || model.isEmpty()) model = "default";
        }

        if (systemPrompt == null) {
            systemPrompt = settingsDAO.getValue("ai_system_prompt");
        }

        double temp = 0.7;
        try { if (tempStr != null) temp = Double.parseDouble(tempStr.trim()); } catch (Exception ignored) {}

        int maxTokens = 600;
        try { if (maxTokensStr != null) maxTokens = Integer.parseInt(maxTokensStr.trim()); } catch (Exception ignored) {}

        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            messages.add(ChatMessage.system(systemPrompt.trim()));
        }
        messages.add(ChatMessage.user(userMessage.trim()));

        ChatOptions options = new ChatOptions(temp, maxTokens);

        // ===== 1. STREAMING SSE =====
        if ("stream".equalsIgnoreCase(action)) {
            response.setContentType("text/event-stream;charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("X-Accel-Buffering", "no");

            PrintWriter out = response.getWriter();
            final Object lock = new Object();
            final boolean[] completed = {false};

            provider.chatStream(messages, apiKey, model, baseUrl, options, new StreamCallback() {
                @Override
                public void onToken(String token) {
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "token");
                    data.addProperty("content", token);
                    out.write("data: " + data.toString() + "\n\n");
                    out.flush();
                }

                @Override
                public void onComplete(ChatResponse resp) {
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "complete");
                    data.addProperty("provider", resp.getProvider());
                    data.addProperty("model", resp.getModel());
                    data.addProperty("latencyMs", resp.getLatencyMs());
                    data.addProperty("promptTokens", resp.getPromptTokens());
                    data.addProperty("completionTokens", resp.getCompletionTokens());
                    data.addProperty("totalTokens", resp.getTotalTokens());
                    out.write("data: " + data.toString() + "\n\n");
                    out.write("data: [DONE]\n\n");
                    out.flush();
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notifyAll();
                    }
                }

                @Override
                public void onError(Throwable error) {
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "error");
                    data.addProperty("message", error.getMessage());
                    out.write("data: " + data.toString() + "\n\n");
                    out.flush();
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notifyAll();
                    }
                }
            });

            // Chờ streaming xong (tối đa 40s)
            synchronized (lock) {
                if (!completed[0]) {
                    try { lock.wait(40000); } catch (InterruptedException ignored) {}
                }
            }
            return;
        }

        // ===== 2. JSON RESPONSE THÔNG THƯỜNG =====
        response.setContentType("application/json;charset=UTF-8");
        JsonObject json = new JsonObject();
        try {
            ChatResponse chatResp = provider.chat(messages, apiKey, model, baseUrl, options);
            json.addProperty("success", true);
            json.addProperty("reply", chatResp.getContent());
            json.addProperty("provider", chatResp.getProvider());
            json.addProperty("model", chatResp.getModel());
            json.addProperty("latencyMs", chatResp.getLatencyMs());
            json.addProperty("promptTokens", chatResp.getPromptTokens());
            json.addProperty("completionTokens", chatResp.getCompletionTokens());
            json.addProperty("totalTokens", chatResp.getTotalTokens());
            json.addProperty("finishReason", chatResp.getFinishReason());
        } catch (AIException e) {
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
            json.addProperty("errorType", e.getErrorType().name());
        } catch (Exception e) {
            json.addProperty("success", false);
            json.addProperty("message", "Lỗi xử lý: " + e.getMessage());
        }
        response.getWriter().write(json.toString());
    }
}
