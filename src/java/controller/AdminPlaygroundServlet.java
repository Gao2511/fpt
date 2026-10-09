package controller;

import ai.*;
import ai.consultation.*;
import ai.dto.*;
import ai.session.ChatSessionManager;
import ai.security.CryptoUtil;
import com.google.gson.JsonObject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/** Same validated consultation pipeline as customers; preview never persists leads. */
@WebServlet("/admin/ai-playground")
public class AdminPlaygroundServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8"); response.setHeader("Cache-Control", "no-store");
        String action = request.getParameter("action");
        String sessionId = "playground_" + request.getSession(true).getId();
        boolean stream = "stream".equals(action);
        response.setContentType(stream ? "text/event-stream;charset=UTF-8" : "application/json;charset=UTF-8");
        JsonObject json = new JsonObject();
        try {
            if ("clearHistory".equals(action) || "resetSession".equals(action)) {
                ChatSessionManager.clearSession(sessionId);
                json.addProperty("success", true); json.addProperty("message", "Đã xóa lịch sử tư vấn.");
            } else {
                AIService.ConfigCache cfg = AIService.getConfig();
                String providerId = value(request, "provider", cfg.activeProvider);
                String model = value(request, "model", cfg.activeModel);
                LLMProvider provider = ProviderRegistry.get(providerId);
                if (provider == null) throw new IllegalArgumentException("Invalid provider");
                String key = request.getParameter("apiKey");
                if (key == null || key.trim().isEmpty() || CryptoUtil.isMasked(key)) key = AIService.getDecryptedApiKey(providerId);
                String url = value(request, "baseUrl", AIService.getBaseUrl(providerId));
                double temperature = Double.parseDouble(value(request, "temperature", Double.toString(cfg.temperature)));
                int tokens = Integer.parseInt(value(request, "maxTokens", Integer.toString(cfg.maxTokens)));
                if (!Double.isFinite(temperature) || temperature < 0 || temperature > 0.5 || tokens < 1200 || tokens > 2400) throw new IllegalArgumentException("Invalid generation settings");
                String prompt = request.getParameter("systemPrompt");
                if (prompt == null) prompt = cfg.systemPrompt;
                ProductCatalog catalog = new ProductCatalog(new dao.PackageDAO().getAll());
                ChatResponse reply = new ConsultationEngine().chat(request.getParameter("message"), ChatSessionManager.getOrCreate(sessionId), catalog,
                    new ConsultationEngine.Target(provider, key, model, url), null, new ChatOptions(temperature, tokens), prompt);
                json.addProperty("success", true); json.addProperty("reply", reply.getContent());
                json.addProperty("provider", reply.getProvider()); json.addProperty("model", reply.getModel());
                json.addProperty("latencyMs", reply.getLatencyMs()); json.addProperty("totalTokens", reply.getTotalTokens());
                if (stream) {
                    // Preserve the SSE contract, but release text only after complete validation.
                    JsonObject token = new JsonObject(); token.addProperty("type", "token"); token.addProperty("content", reply.getContent());
                    response.getWriter().write("data: " + token + "\n\n"); json.addProperty("type", "complete");
                }
            }
        } catch (Exception e) {
            json.addProperty("success", false); json.addProperty("message", "Không thể xử lý tư vấn. Kiểm tra provider, model và tham số rồi thử lại.");
            if (stream) json.addProperty("type", "error");
            System.err.println("[AI preview] error=" + e.getClass().getSimpleName());
        }
        if (stream) response.getWriter().write("data: " + json + "\n\ndata: [DONE]\n\n");
        else response.getWriter().write(json.toString());
    }
    private String value(HttpServletRequest request, String name, String fallback) {
        String value = request.getParameter(name); return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
