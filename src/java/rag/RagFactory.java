package rag;

import dao.KnowledgeChunkDAO;
import dto.KnowledgeChunkDTO;
import java.util.Collections;
import java.util.Map;

public final class RagFactory {
    private RagFactory() {}

    public static KnowledgeRetrievalService consultation(String currentGeminiKey) {
        return consultation(currentGeminiKey, Collections.<String, String>emptyMap());
    }

    public static KnowledgeRetrievalService consultation(String currentGeminiKey, Map<String, String> settings) {
        Map<String, String> env = System.getenv();
        if (rag.runtime.GeminiRuntimeConfig.smokePlanConfigured(env)) {
            try {
                return rag.runtime.GeminiRuntime.active().retrieval;
            } catch (Exception e) {
                System.err.println("[RAG] Invalid smoke plan configuration; feature disabled");
                return KnowledgeRetrievalService.disabled();
            }
        }
        Map<String, String> safeSettings = settings == null ? Collections.<String, String>emptyMap() : settings;
        String dbFlag = safeSettings.get("rag.production_rag_enabled");
        boolean enabled;
        if (dbFlag != null && !dbFlag.trim().isEmpty()) {
            enabled = "true".equalsIgnoreCase(dbFlag.trim());
        } else {
            enabled = env != null && "true".equalsIgnoreCase(env.get("RAG_ENABLED"));
        }
        if (!enabled) {
            return KnowledgeRetrievalService.disabled();
        }
        return buildHybridService(currentGeminiKey, env);
    }

    public static KnowledgeRetrievalService consultationForPlayground(String currentGeminiKey, Map<String, String> settings) {
        Map<String, String> safeSettings = settings == null ? Collections.<String, String>emptyMap() : settings;
        boolean prodEnabled = "true".equalsIgnoreCase(safeSettings.getOrDefault("rag.production_rag_enabled",
                System.getenv() != null && "true".equalsIgnoreCase(System.getenv().get("RAG_ENABLED")) ? "true" : "false"));
        boolean stagingEnabled = "true".equalsIgnoreCase(safeSettings.getOrDefault("rag.staging_rag_enabled", "false"));
        boolean localEnabled = !"false".equalsIgnoreCase(safeSettings.getOrDefault("rag.local_indexing_enabled", "true"));
        if (!prodEnabled && !stagingEnabled && !localEnabled) {
            return KnowledgeRetrievalService.disabled();
        }
        return buildHybridService(currentGeminiKey, System.getenv());
    }

    public static String resolveGeminiEmbeddingKey(String fallbackGeminiKey, Map<String, String> env) {
        if (env != null) {
            String embKey = env.get("RAG_EMBEDDING_API_KEY");
            if (embKey != null && !embKey.trim().isEmpty()) return embKey.trim();
            String geminiKey = env.get("GEMINI_API_KEY");
            if (geminiKey != null && !geminiKey.trim().isEmpty()) return geminiKey.trim();
        }
        if (fallbackGeminiKey != null && !fallbackGeminiKey.trim().isEmpty()) {
            return fallbackGeminiKey.trim();
        }
        try {
            String dbKey = ai.AIService.getDecryptedApiKey("gemini");
            if (dbKey != null && !dbKey.trim().isEmpty()) return dbKey.trim();
        } catch (Exception ignored) {}
        return "";
    }

    private static KnowledgeRetrievalService buildHybridService(String currentGeminiKey, Map<String, String> env) {
        try {
            RagDatabase db = new RagDatabase();
            RagConfig activeConfig = new RagConfig(true, true, true, "gemini-embedding-001", 768);
            String embKey = resolveGeminiEmbeddingKey(currentGeminiKey, env);
            KnowledgeRetrievalService geminiService = null;
            if (!embKey.isEmpty()) {
                EmbeddingService geminiEmb = new GeminiEmbeddingService(
                        activeConfig, embKey, GeminiEmbeddingService.httpTransport(), true, 2);
                geminiService = new KnowledgeRetrievalService(
                        activeConfig, new KnowledgeChunkDAO(db, "GEMINI"), geminiEmb, false);
            }
            KnowledgeRetrievalService mockService = new KnowledgeRetrievalService(
                    activeConfig, new KnowledgeChunkDAO(db, "MOCK"), new MockEmbeddingProvider(), false);
            return new HybridKnowledgeRetrievalService(activeConfig, geminiService, mockService);
        } catch (Exception e) {
            System.err.println("[RAG] Unable to initialize retrieval service: " + e.getClass().getSimpleName());
            return KnowledgeRetrievalService.disabled();
        }
    }

    private static final class HybridKnowledgeRetrievalService extends KnowledgeRetrievalService {
        private final KnowledgeRetrievalService geminiService;
        private final KnowledgeRetrievalService mockService;

        HybridKnowledgeRetrievalService(RagConfig config, KnowledgeRetrievalService geminiService, KnowledgeRetrievalService mockService) {
            super(config, null, null, false);
            this.geminiService = geminiService;
            this.mockService = mockService;
        }

        @Override
        public KnowledgeSourceSelection.Result lookup(String question) {
            if (geminiService != null) {
                KnowledgeSourceSelection.Result res = geminiService.lookup(question);
                if (res != null && (!res.chunks.isEmpty() || res.uncertain())) {
                    return res;
                }
            }
            if (mockService != null) {
                return mockService.lookup(question);
            }
            return KnowledgeSourceSelection.empty(KnowledgeSourceSelection.Issue.NONE);
        }

        @Override
        public boolean current(KnowledgeChunkDTO chunk) {
            if (geminiService != null && geminiService.current(chunk)) {
                return true;
            }
            return mockService != null && mockService.current(chunk);
        }
    }
}
