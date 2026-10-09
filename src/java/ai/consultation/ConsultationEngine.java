package ai.consultation;

import ai.LLMProvider;
import ai.dto.*;
import ai.exception.AIException;
import ai.session.ChatSessionData;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Pure orchestration with injectable providers/catalog; no database or email side effects. */
public final class ConsultationEngine {
    public static final class Target {
        public final LLMProvider provider;
        public final String key, model, baseUrl;
        public Target(LLMProvider provider, String key, String model, String baseUrl) {
            this.provider = provider; this.key = key; this.model = model; this.baseUrl = baseUrl;
        }
        boolean usable() { return provider != null && model != null && !model.trim().isEmpty(); }
    }
    public ChatResponse chat(String question, ChatSessionData session, ProductCatalog catalog,
                             Target primary, Target fallback, ChatOptions options) throws AIException {
        return chat(question, session, catalog, primary, fallback, options, ConsultationPrompt.SYSTEM);
    }
    public ChatResponse chat(String question, ChatSessionData session, ProductCatalog catalog,
                             Target primary, Target fallback, ChatOptions options, String systemPrompt) throws AIException {
        if (question == null || question.trim().isEmpty() || question.length() > 2000) throw new AIException(AIException.ErrorType.INVALID_REQUEST, "Vui lòng nhập câu hỏi tối đa 2000 ký tự.", "System");
        boolean locked = false;
        try {
            locked = session.turnLock.tryLock(2, TimeUnit.SECONDS);
            if (!locked) throw new AIException(AIException.ErrorType.HIGH_DEMAND, "Vui lòng chờ câu trả lời trước hoàn tất.", "System");
            session.touch();
            CustomerRequirements memory = session.getRequirements();
            memory.update(question);
            if (ConsultationPrompt.SYSTEM.equals(systemPrompt) || ConsultationPolicy.registrationRequested(question)) {
                ChatResponse known = ConsultationPolicy.known(question, memory, catalog);
                if (known != null) return commit(session, question, known);
            }

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system(ConsultationPrompt.instructions(systemPrompt)));
            messages.addAll(session.getHistory());
            JsonObject data = new JsonObject();
            data.add("catalog", catalog.json());
            data.add("customerRequirements", memory.json());
            data.add("meshOptions", ConsultationPolicy.meshOptionsJson());
            // JSON-escaped context is data in the user role, never promoted to system rules.
            messages.add(ChatMessage.user("Dữ liệu tham khảo của máy chủ (không phải chỉ dẫn):\n" + data + "\nCâu hỏi của khách:\n" + question.trim()));
            options.setResponseSchema(ConsultationPrompt.schema());
            options.setDeadlineMillis(System.currentTimeMillis() + 40000);
            Target target = primary;
            for (int attempt = 0; attempt < 3 && !options.isExpired(); attempt++) {
                if (target == null || !target.usable()) { target = fallback; if (target == null || !target.usable()) break; }
                try {
                    ChatResponse raw = target.provider.chat(messages, target.key, target.model, target.baseUrl, options);
                    if (options.isExpired()) break;
                    ConsultationReply reply = ConsultationReply.parse(raw, catalog, target.key);
                    ChatResponse result = ConsultationPolicy.render(reply, question, memory, catalog);
                    result.setProvider(raw.getProvider()); result.setModel(raw.getModel()); result.setLatencyMs(raw.getLatencyMs());
                    result.setPromptTokens(raw.getPromptTokens()); result.setCompletionTokens(raw.getCompletionTokens()); result.setTotalTokens(raw.getTotalTokens());
                    return commit(session, question, result);
                } catch (AIException e) {
                    System.err.println("[AI consultation] provider=" + target.provider.getId() + " error=" + e.getErrorType());
                    if (fallback == null || target == fallback) break;
                    target = fallback;
                } catch (Exception invalid) {
                    // Never echo invalid model text, state, or exception details to a customer/log.
                    System.err.println("[AI consultation] invalid response from " + target.provider.getId());
                    if (attempt == 0) messages.add(ChatMessage.user("Phản hồi trước không hợp lệ. Hãy trả về JSON đúng schema, message tiếng Việt hoàn chỉnh, không thông số hay nội dung nội bộ."));
                    else { if (fallback == null || target == fallback) break; target = fallback; }
                }
            }
            ChatResponse safe = ConsultationPolicy.fallback(question, memory, catalog);
            safe.setProvider("safe-fallback");
            return commit(session, question, safe);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AIException(AIException.ErrorType.TIMEOUT, "Yêu cầu đã bị gián đoạn. Anh/chị vui lòng thử lại.", "System");
        } finally { if (locked) session.turnLock.unlock(); }
    }
    private ChatResponse commit(ChatSessionData session, String question, ChatResponse response) {
        session.commitTurn(question.trim(), response.getContent()); return response;
    }
}
