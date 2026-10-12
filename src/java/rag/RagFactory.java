package rag;
import dao.KnowledgeChunkDAO;
public final class RagFactory {
    private RagFactory() {}
    public static KnowledgeRetrievalService consultation(String currentGeminiKey) {
        // Do not initialize DB or decrypt keys when the feature is off.
        if(!rag.runtime.GeminiRuntimeConfig.requested(System.getenv()))return KnowledgeRetrievalService.disabled();
        try {
            return rag.runtime.GeminiRuntime.local().retrieval;
        }catch(Exception e){System.err.println("[RAG] Invalid configuration; feature disabled");return KnowledgeRetrievalService.disabled();}
    }
}
