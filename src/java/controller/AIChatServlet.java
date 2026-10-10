package controller;

import ai.AIService;
import ai.dto.ChatResponse;
import ai.exception.AIException;
import com.google.gson.JsonObject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/ai-chat")
public class AIChatServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String message = request.getParameter("message");
        if (message == null || message.trim().isEmpty() || message.length() > 2000) {
            response.setStatus(400); write(response, "Vui lòng nhập câu hỏi từ 1 đến 2000 ký tự."); return;
        }
        HttpSession session = request.getSession(true);
        synchronized (session) {
            long now = System.currentTimeMillis();
            Long started = (Long) session.getAttribute("ai_last_chat_time");
            Integer count = (Integer) session.getAttribute("ai_chat_count");
            if (started == null || now - started >= 60000) { started = now; count = 0; }
            if (count != null && count >= 10) {
                response.setStatus(429); response.setHeader("Retry-After", "60");
                write(response, "Anh/chị đang gửi câu hỏi quá nhanh. Vui lòng đợi một phút rồi thử lại."); return;
            }
            session.setAttribute("ai_last_chat_time", started);
            session.setAttribute("ai_chat_count", (count == null ? 0 : count) + 1);
        }
        try {
            ChatResponse reply = consult(message.trim(), session.getId());
            JsonObject json = customerJson(reply);
            response.getWriter().write(json.toString());
        } catch (AIException e) {
            response.setStatus(e.getErrorType() == AIException.ErrorType.HIGH_DEMAND ? 429 : 503);
            System.err.println("[AI chat] error=" + e.getErrorType());
            write(response, e.getErrorType() == AIException.ErrorType.HIGH_DEMAND ? "Anh/chị vui lòng chờ câu trả lời trước hoàn tất rồi thử lại." : ai.consultation.ConsultationPolicy.TECHNICAL_FAILURE);
        } catch (Exception e) {
            response.setStatus(503);
            System.err.println("[AI chat] error=" + e.getClass().getSimpleName());
            write(response, ai.consultation.ConsultationPolicy.TECHNICAL_FAILURE);
        }
    }
    protected ChatResponse consult(String message, String sessionId) throws AIException { return AIService.askAI(message, sessionId); }
    public static JsonObject customerJson(ChatResponse reply) {
        JsonObject json = new JsonObject(); json.addProperty("reply", reply.getContent());
        json.addProperty("action", reply.getAction());
        if ("open_registration".equals(reply.getAction()) && reply.getRecommendedPackageId() != null)
            json.addProperty("registrationPath", "/package-detail?id=" + reply.getRecommendedPackageId() + "#registration");
        return json;
    }
    private void write(HttpServletResponse response, String message) throws IOException {
        JsonObject json = new JsonObject(); json.addProperty("reply", message); json.addProperty("action", "none");
        response.getWriter().write(json.toString());
    }
}
