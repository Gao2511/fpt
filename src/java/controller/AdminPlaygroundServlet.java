package controller;

import ai.LLMProvider;
import ai.ProviderRegistry;
import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.dto.StreamCallback;
import ai.exception.AIException;
import ai.security.CryptoUtil;
import ai.session.ChatSessionData;
import ai.session.ChatSessionManager;
import com.google.gson.JsonObject;
import dao.SettingsDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet dành riêng cho Admin thử nghiệm mô hình AI (Live Playground)
 * Được bảo vệ bởi AdminFilter (/admin/*)
 * Duy trì ngữ cảnh hội thoại, sổ tay trạng thái theo session, và hỗ trợ nút Reset hội thoại
 */
@WebServlet("/admin/ai-playground")
public class AdminPlaygroundServlet extends HttpServlet {

    private final SettingsDAO settingsDAO = new SettingsDAO();

    private static final String STATE_INSTRUCTION_APPENDIX =
        "\n\n=== QUY TẮC BỘ NHỚ VÀ SỔ TAY THÔNG TIN KHÁCH HÀNG (BẮT BUỘC) ===\n" +
        "1. Trong mỗi lượt trò chuyện, bạn sẽ nhận được thông tin khách hàng hiện có trong thẻ <sotay_hientai>{...}</sotay_hientai>.\n" +
        "2. Hãy ghi nhớ các thông tin khách hàng đã chia sẻ (Tên, Số điện thoại, Địa chỉ lắp đặt, Gói cước quan tâm). TUYỆT ĐỐI KHÔNG HỎI LẠI những thông tin đã có trong sổ tay!\n" +
        "3. Ở CUỐI CÙNG của mỗi câu trả lời, bạn BẮT BUỘC phải đính kèm khối JSON cập nhật thông tin trong thẻ <state>{...}</state>.\n" +
        "Định dạng chuẩn:\n" +
        "<state>{\"ten\":\"...\",\"sdt\":\"...\",\"dia_chi\":\"...\",\"goi_de_xuat\":\"...\"}</state>\n" +
        "Nếu chưa biết trường nào, hãy để chuỗi rỗng \"\" hoặc giữ nguyên giá trị cũ, tuyệt đối không bịa đặt thông tin.";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "chat";
        }

        HttpSession httpSession = request.getSession(true);
        String playgroundSessionId = "playground_" + httpSession.getId();
        ChatSessionData sessionData = ChatSessionManager.getOrCreate(playgroundSessionId);

        // ===== ACTION: XÓA LỊCH SỬ / RESET SỔ TAY PLAYGROUND =====
        if ("clearHistory".equalsIgnoreCase(action) || "resetSession".equalsIgnoreCase(action)) {
            response.setContentType("application/json;charset=UTF-8");
            ChatSessionManager.clearSession(playgroundSessionId);
            JsonObject res = new JsonObject();
            res.addProperty("success", true);
            res.addProperty("message", "Đã xóa toàn bộ lịch sử và làm mới sổ tay hội thoại!");
            res.add("state", sessionData.getState());
            response.getWriter().write(res.toString());
            return;
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

        // Xử lý API Key
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

        // Tóm tắt ngữ cảnh cũ nếu vượt quá 20 lượt
        ChatSessionManager.summarizeAndTrimIfNeeded(sessionData, provider, apiKey, model, baseUrl);

        // Xây dựng danh sách tin nhắn gửi sang LLM (Đúng thứ tự, đúng role)
        List<ChatMessage> messages = new ArrayList<>();

        // System Instruction kèm quy tắc Sổ tay
        String fullSystemPrompt = (systemPrompt != null ? systemPrompt.trim() : "") + STATE_INSTRUCTION_APPENDIX;
        messages.add(ChatMessage.system(fullSystemPrompt));

        // Thêm lịch sử hội thoại của session (tối đa 20 lượt gần nhất)
        for (ChatMessage hMsg : sessionData.getHistory()) {
            messages.add(new ChatMessage(hMsg.getRole(), hMsg.getContent()));
        }

        // Tin nhắn mới nhất của Admin: Chèn <sotay_hientai>{state}</sotay_hientai> vào đầu
        String wrappedUserMsg = ChatSessionManager.wrapUserMessageWithState(sessionData, userMessage.trim());
        messages.add(ChatMessage.user(wrappedUserMsg));

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
            final StringBuilder accumulatedFull = new StringBuilder();

            provider.chatStream(messages, apiKey, model, baseUrl, options, new StreamCallback() {
                @Override
                public void onToken(String token) {
                    accumulatedFull.append(token);
                    // Lọc không bắn trực tiếp thẻ <state> ra giao diện stream
                    if (!token.contains("<state>") && !token.contains("</state>")) {
                        JsonObject data = new JsonObject();
                        data.addProperty("type", "token");
                        data.addProperty("content", token);
                        out.write("data: " + data.toString() + "\n\n");
                        out.flush();
                    }
                }

                @Override
                public void onComplete(ChatResponse resp) {
                    // Trích xuất state và xóa sạch <state> khỏi câu trả lời
                    String cleanReply = ChatSessionManager.extractAndUpdateState(sessionData, accumulatedFull.toString());

                    // Lưu vào lịch sử hội thoại
                    sessionData.addMessage(ChatMessage.user(userMessage.trim()));
                    sessionData.addMessage(ChatMessage.assistant(cleanReply));

                    JsonObject data = new JsonObject();
                    data.addProperty("type", "complete");
                    data.addProperty("provider", resp.getProvider());
                    data.addProperty("model", resp.getModel());
                    data.addProperty("latencyMs", resp.getLatencyMs());
                    data.addProperty("promptTokens", resp.getPromptTokens());
                    data.addProperty("completionTokens", resp.getCompletionTokens());
                    data.addProperty("totalTokens", resp.getTotalTokens());
                    data.add("state", sessionData.getState());
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

            // Trích xuất <state>, cập nhật sổ tay và XÓA thẻ <state> khỏi câu trả lời
            String rawReply = chatResp.getContent();
            String cleanReply = ChatSessionManager.extractAndUpdateState(sessionData, rawReply);

            // Lưu vào lịch sử hội thoại
            sessionData.addMessage(ChatMessage.user(userMessage.trim()));
            sessionData.addMessage(ChatMessage.assistant(cleanReply));

            json.addProperty("success", true);
            json.addProperty("reply", cleanReply);
            json.addProperty("provider", chatResp.getProvider());
            json.addProperty("model", chatResp.getModel());
            json.addProperty("latencyMs", chatResp.getLatencyMs());
            json.addProperty("promptTokens", chatResp.getPromptTokens());
            json.addProperty("completionTokens", chatResp.getCompletionTokens());
            json.addProperty("totalTokens", chatResp.getTotalTokens());
            json.addProperty("finishReason", chatResp.getFinishReason());
            json.add("state", sessionData.getState());
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
