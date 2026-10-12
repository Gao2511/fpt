package rag;
public interface EmbeddingService {
    /** Unknown/test adapters are offline unless they explicitly attest real-provider provenance. */
    default String providerKind() { return "MOCK"; }
    String model();
    int dimensions();
    float[] document(String title, String content) throws RagException;
    float[] query(String text) throws RagException;
}
