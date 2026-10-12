package rag;

import java.util.Map;

/** Fail closed: activation, account verification and embedding-cost approval are separate. */
public final class RagConfig {
    public final boolean enabled, modelVerified, callsApproved;
    public final String model;
    public final int dimensions;
    public RagConfig(boolean enabled, boolean verified, boolean approved, String model, int dimensions) {
        if (!"gemini-embedding-001".equals(model) || dimensions != 768)
            throw new IllegalArgumentException("MVP requires verified gemini-embedding-001 / 768; changing space requires reindexing");
        this.enabled=enabled; this.modelVerified=verified; this.callsApproved=approved;
        this.model=model; this.dimensions=dimensions;
    }
    public static RagConfig from(Map<String,String> env) {
        return new RagConfig("true".equalsIgnoreCase(env.get("RAG_ENABLED")),
            "true".equalsIgnoreCase(env.get("RAG_EMBEDDING_MODEL_VERIFIED")),
            "true".equalsIgnoreCase(env.get("RAG_EMBEDDING_CALLS_APPROVED")),
            env.getOrDefault("RAG_EMBEDDING_MODEL","gemini-embedding-001"),
            Integer.parseInt(env.getOrDefault("RAG_EMBEDDING_DIMENSIONS","768")));
    }
    public static RagConfig disabled() { return new RagConfig(false,false,false,"gemini-embedding-001",768); }
}
