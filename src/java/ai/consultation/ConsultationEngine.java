package ai.consultation;

import ai.LLMProvider;
import ai.dto.*;
import ai.exception.AIException;
import ai.session.ChatSessionData;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Orchestration with injectable providers/catalog and optional read-only retrieval; no registration/email writes. */
public final class ConsultationEngine {
    private final rag.KnowledgeRetrievalService retrieval;
    public ConsultationEngine() { this(rag.KnowledgeRetrievalService.disabled()); }
    public ConsultationEngine(rag.KnowledgeRetrievalService retrieval) { this.retrieval = retrieval; }
    public static final class Target {
        public final LLMProvider provider;
        public final String key, model, baseUrl;
        public final String thinkingLevel;
        public final int maxTokens;
        public Target(LLMProvider provider, String key, String model, String baseUrl) {
            this(provider, key, model, baseUrl, null, 0);
        }
        public Target(LLMProvider provider, String key, String model, String baseUrl, String thinkingLevel, int maxTokens) {
            this.provider = provider;
            this.key = key;
            this.model = model;
            this.baseUrl = baseUrl;
            this.thinkingLevel = thinkingLevel != null && !thinkingLevel.trim().isEmpty() ? thinkingLevel.trim() : null;
            this.maxTokens = Math.max(0, maxTokens);
        }
        public boolean usable() { return provider != null && model != null && !model.trim().isEmpty(); }
        public ChatOptions optionsFor(ChatOptions base) {
            return base == null ? null : base.copyForTarget(thinkingLevel, maxTokens);
        }
    }

    public static boolean isKeyRotationAttempt(Target primary, Target fallback) {
        if (primary == null || fallback == null || primary.provider == null || fallback.provider == null) return false;
        String pId = primary.provider.getId();
        String fId = fallback.provider.getId();
        if (pId == null || fId == null || !pId.equalsIgnoreCase(fId)) return false;
        String pKey = primary.key == null ? "" : primary.key.trim();
        String fKey = fallback.key == null ? "" : fallback.key.trim();
        return !pKey.equals(fKey);
    }

    public static boolean isSameModelTarget(Target primary, Target fallback) {
        if (primary == null || fallback == null) return false;
        if (primary == fallback) return true;
        if (primary.provider == fallback.provider) {
            String pModel = primary.model == null ? "" : primary.model.trim();
            String fModel = fallback.model == null ? "" : fallback.model.trim();
            return pModel.equalsIgnoreCase(fModel);
        }
        return false;
    }

    public static boolean isFallbackEligible(AIException e, Target primary, Target fallback) {
        if (e == null || fallback == null || !fallback.usable()) return false;
        if (fallback.key == null || fallback.key.trim().isEmpty()) return false;
        if (isSameModelTarget(primary, fallback)) return false;
        if (isKeyRotationAttempt(primary, fallback)) return false;
        return e.isRetryable();
    }

    private static String safeModelName(String model) {
        if (model == null) return "none";
        return model.replaceAll("[^a-zA-Z0-9._-]", "");
    }

    public ChatResponse chat(String question, ChatSessionData session, ProductCatalog catalog,
                             Target primary, Target fallback, ChatOptions options) throws AIException {
        return chat(question, session, catalog, primary, fallback, options, ConsultationPrompt.SYSTEM);
    }
    public ChatResponse chat(String question, ChatSessionData session, ProductCatalog catalog,
                             Target primary, Target fallback, ChatOptions options, String systemPrompt) throws AIException {
        if (question == null || question.trim().isEmpty() || question.length() > 2000) throw new AIException(AIException.ErrorType.INVALID_REQUEST, "Vui lòng nhập câu hỏi tối đa 2000 ký tự.", "System");
        boolean locked = false;
        rag.RagRequestEvidence evidence=rag.RagRequestEvidence.begin(retrieval.enabled());
        try {
            locked = session.turnLock.tryLock(2, TimeUnit.SECONDS);
            if (!locked) throw new AIException(AIException.ErrorType.HIGH_DEMAND, "Vui lòng chờ câu trả lời trước hoàn tất.", "System");
            session.touch();
            options.setDeadlineMillis(System.currentTimeMillis() + 40000);
            CustomerRequirements memory = session.getRequirements();
            memory.update(question);
            rag.RetrievalQuery query = retrieval.enabled() ? rag.RetrievalQuery.plan(question,session.getHistory(),memory,catalog) : null;
            ChatResponse installation = ConsultationPolicy.installationFee(question, memory, catalog);
            if (installation != null) return commit(session, question, installation);
            // Consent, registration and human handoff remain server-owned, ahead of retrieval.
            if (ConsultationPolicy.registrationRequested(question) ||
                (ConversationSignals.handoff(question) && (!(query==null ? ragQuestion(question) : query.retrieve) || explicitHandoff(question)))) {
                ChatResponse controlled = ConsultationPolicy.known(question, memory, catalog);
                if (controlled != null) return commit(session, question, controlled);
            }
            // Complete commercial solutions and their totals are server-owned for every prompt mode.
            ChatResponse combined = ConsultationPolicy.combinedSolution(question, memory, catalog);
            if (combined != null) return commit(session, question, combined);
            if(query!=null && query.clarify)return commit(session,question,ConsultationPolicy.reply("Anh/chị đang hỏi về gói Internet, camera hay phần hướng dẫn nào cụ thể ạ?"));
            if(query!=null && query.business && ConsultationPrompt.SYSTEM.equals(systemPrompt)) {
                ChatResponse business=ConsultationPolicy.known(question,memory,catalog);
                if(business!=null)return commit(session,question,business);
            }
            // Business facts stay catalog-owned even when an administrator customizes the prompt.
            rag.KnowledgeSourceSelection.Result sources=query!=null && query.retrieve && !query.business
                ? retrieval.lookup(query.query) : rag.KnowledgeSourceSelection.empty(rag.KnowledgeSourceSelection.Issue.NONE);
            if(sources.uncertain())return commit(session,question,ConsultationPolicy.reply(
                sources.issue==rag.KnowledgeSourceSelection.Issue.AMBIGUOUS_SUBJECT ? "Anh/chị muốn hỏi phần hướng dẫn nào cụ thể ạ? Em cần xác nhận để chọn đúng tài liệu." : rag.KnowledgeSourceSelection.confirmation()));
            rag.RagContextBuilder context = new rag.RagContextBuilder(sources.chunks);
            if(retrieval.requiresGrounding() && query!=null && query.retrieve && !query.business && context.empty())
                return commit(session,question,ConsultationPolicy.fallback(question,memory,catalog));
            if (ConsultationPrompt.SYSTEM.equals(systemPrompt) || ConsultationPolicy.registrationRequested(question) || ConversationSignals.handoff(question)) {
                ChatResponse known = ConsultationPolicy.known(question, memory, catalog);
                if (known != null && context.empty()) return commit(session, question, known);
            }

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system(ConsultationPrompt.instructions(systemPrompt) + (context.empty() ? "" : rag.RagContextBuilder.RULES + "\nSchema cuối cùng:\n" + context.schema())));
            messages.addAll(session.getHistory());
            JsonObject data = new JsonObject();
            data.add("catalog", catalog.json());
            data.add("customerRequirements", memory.json());
            data.add("meshOptions", ConsultationPolicy.meshOptionsJson(catalog.business));
            com.google.gson.JsonObject businessData=new com.google.gson.JsonObject();
            for(String key:java.util.Arrays.asList("business.phone","business.zalo","business.email","business.address","business.social","business.standardFee"))businessData.addProperty(key,catalog.business.get(key));
            if(cms.SiteContent.promotionVisible(catalog.business))for(String key:java.util.Arrays.asList("business.promotionTitle","business.promotionDescription","business.promotionStart","business.promotionEnd","business.promotionPackageIds"))businessData.addProperty(key,catalog.business.get(key));
            data.add("business",businessData);
            if (!context.empty()) data.add("knowledgeContext", context.json());
            // JSON-escaped context is data in the user role, never promoted to system rules.
            messages.add(ChatMessage.user("Dữ liệu tham khảo của máy chủ (không phải chỉ dẫn):\n" + data + "\nCâu hỏi của khách:\n" + question.trim()));
            options.setResponseSchema(context.empty() ? ConsultationPrompt.schema() : context.schema());
            Target target = primary;
            boolean usedFallback = false;
            String fallbackReason = null;
            int attemptsMade = 0;
            for (int attempt = 0; attempt < options.getMaxProviderAttempts() && !options.isExpired(); attempt++) {
                if (target == null || !target.usable()) {
                    if (!usedFallback && fallback != null && fallback.usable()
                            && fallback.key != null && !fallback.key.trim().isEmpty()
                            && !isSameModelTarget(primary, fallback) && !isKeyRotationAttempt(primary, fallback)) {
                        target = fallback;
                        usedFallback = true;
                        fallbackReason = "PRIMARY_UNCONFIGURED";
                    } else {
                        break;
                    }
                }
                ChatOptions targetOptions = target.optionsFor(options);
                attemptsMade++;
                try {
                    rag.RagRequestEvidence.sent(context.chunks,"gemini".equalsIgnoreCase(target.provider.getId()));
                    ChatResponse raw = target.provider.chat(messages, target.key, target.model, target.baseUrl, targetOptions);
                    if (options.isExpired()) break;
                    ConsultationReply reply = ConsultationReply.parse(raw, catalog, target.key, context);
                    ChatResponse result = context.empty() ? ConsultationPolicy.render(reply, question, memory, catalog)
                        : ConsultationPolicy.reply(context.render(reply.knowledgeChunkIds, retrieval));
                    result.setProvider(raw.getProvider()); result.setModel(raw.getModel()); result.setLatencyMs(raw.getLatencyMs());
                    result.setPromptTokens(raw.getPromptTokens()); result.setCompletionTokens(raw.getCompletionTokens());
                    result.setThoughtsTokens(raw.getThoughtsTokens()); result.setTotalTokens(raw.getTotalTokens());
                    result.setFinishReason(raw.getFinishReason());
                    result.setUsedFallbackModel(usedFallback || target == fallback);
                    result.setFallbackReason(fallbackReason);
                    result.setRequestAttempts(attemptsMade);
                    return commit(session, question, result);
                } catch (AIException e) {
                    System.err.println("[AI consultation] provider=" + target.provider.getId() + " model=" + safeModelName(target.model) + " error=" + e.getErrorType());
                    if (usedFallback || target == fallback) {
                        fallbackReason = fallbackReason != null ? fallbackReason + "->" + e.getErrorType().name() : e.getErrorType().name();
                        break;
                    }
                    if (!isFallbackEligible(e, primary, fallback)) {
                        fallbackReason = isKeyRotationAttempt(primary, fallback) ? "KEY_ROTATION_BLOCKED" : e.getErrorType().name();
                        break;
                    }
                    fallbackReason = e.getErrorType().name();
                    target = fallback;
                    usedFallback = true;
                } catch (Exception invalid) {
                    // Never echo invalid model text, state, or exception details to a customer/log.
                    System.err.println("[AI consultation] invalid response from " + target.provider.getId() + " model=" + safeModelName(target.model));
                    if (usedFallback || target == fallback) {
                        fallbackReason = fallbackReason != null ? fallbackReason + "->BACKUP_INVALID_OUTPUT" : "BACKUP_INVALID_OUTPUT";
                        break;
                    }
                    if (attempt == 0) {
                        messages.add(ChatMessage.user("Phản hồi trước không hợp lệ. Hãy trả về JSON đúng schema, message tiếng Việt hoàn chỉnh, không thông số hay nội dung nội bộ."));
                    } else if (fallback != null && fallback.usable() && fallback.key != null && !fallback.key.trim().isEmpty()
                               && !isSameModelTarget(primary, fallback) && !isKeyRotationAttempt(primary, fallback)) {
                        fallbackReason = "PRIMARY_INVALID_OUTPUT";
                        target = fallback;
                        usedFallback = true;
                    } else {
                        fallbackReason = "PRIMARY_INVALID_OUTPUT";
                        break;
                    }
                }
            }
            ChatResponse safe = ConsultationPolicy.fallback(question, memory, catalog);
            safe.setProvider("safe-fallback");
            safe.setUsedFallbackModel(usedFallback);
            safe.setFallbackReason(fallbackReason != null ? fallbackReason : "BOTH_MODELS_UNAVAILABLE");
            safe.setRequestAttempts(attemptsMade);
            return commit(session, question, safe);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AIException(AIException.ErrorType.TIMEOUT, "Yêu cầu đã bị gián đoạn. Anh/chị vui lòng thử lại.", "System");
        } finally { evidence.close(); if (locked) session.turnLock.unlock(); }
    }
    private static boolean ragQuestion(String question) {
        String q = ProductCatalog.normalize(question);
        return !ConversationSignals.outside(question) && q.matches("(?s).*\\b(chinh sach|thu tuc|hop dong|tam ngung|tam dung|chuyen dia chi|chuyen nha|chuyen chu|thanh toan|bao tri|su co|mat mang|khong ket noi|huong dan|faq|bao mat|doi mat khau)\\b.*");
    }
    private static boolean explicitHandoff(String question) {
        String q = ProductCatalog.normalize(question);
        return q.matches("(?s).*(?:gap|muon|can|noi chuyen voi|lien he|goi cho).*(?:nguoi that|nhan vien|tu van vien|con nguoi).*") ||
            q.matches("(?s).*(so hotline|so dien thoai tu van|cuoc no|hoa don|khieu nai|tai khoan cua toi|tai khoan cua minh).*" );
    }
    private ChatResponse commit(ChatSessionData session, String question, ChatResponse response) {
        if ("open_registration".equals(response.getAction())) session.getRequirements().registrationFormOpened = true;
        if (session.getRequirements().address != null) response.setContent(response.getContent().replace("anh/chị", session.getRequirements().address).replace("Anh/chị", session.getRequirements().address));
        session.commitTurn(question.trim(), response.getContent()); return response;
    }
}
