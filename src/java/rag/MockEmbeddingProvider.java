package rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic offline 768-dimensional embedding provider for local development and testing.
 * Performs zero external network calls and requires no API keys.
 */
public final class MockEmbeddingProvider implements EmbeddingService {
    public static final String MODEL_NAME = "gemini-embedding-001";
    public static final int DIMENSIONS = 768;

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public String model() {
        return MODEL_NAME;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    public boolean isOfflineMock() {
        return true;
    }

    public int calls() {
        return callCount.get();
    }

    @Override
    public float[] document(String title, String content) throws RagException {
        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            throw new RagException(RagException.Code.INVALID_DOCUMENT);
        }
        callCount.incrementAndGet();
        return embedInternal(title.trim() + "\n" + content.trim(), true);
    }

    @Override
    public float[] query(String text) throws RagException {
        if (text == null || text.trim().isEmpty()) {
            throw new RagException(RagException.Code.INVALID_DOCUMENT);
        }
        callCount.incrementAndGet();
        return embedInternal(text.trim(), false);
    }

    private static float[] embedInternal(String raw, boolean isDocument) throws RagException {
        float[] vec = new float[DIMENSIONS];
        String normalized = RetrievalQuery.normalize(raw)
                .replaceAll("(?<=\\d)\\s+(?=k\\b)", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .trim();

        Set<String> stop = new HashSet<>(Arrays.asList(
                "toi", "minh", "anh", "chi", "em", "co", "khong", "the", "nao", "gi", "la",
                "cho", "cua", "voi", "va", "muon", "biet", "xin", "hoi", "ve", "duoc", "can",
                "hay", "tai", "thi", "neu", "lam", "shop", "oi", "qua", "nha", "tui", "bro",
                "sao", "vay", "nay", "da", "theo", "tai", "lieu", "huong", "dan", "duyet"));

        List<String> tokens = new ArrayList<>();
        if (!normalized.isEmpty()) {
            for (String w : normalized.split("\\s+")) {
                if (w.length() > 1 && !stop.contains(w)) {
                    tokens.add(w);
                }
            }
        }
        if (tokens.isEmpty() && !normalized.isEmpty()) {
            for (String w : normalized.split("\\s+")) {
                if (!w.isEmpty()) tokens.add(w);
            }
        }

        // Project lexical tokens and bigrams into dimensions 0..703 so matching topics have high cosine similarity.
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            projectFeature(vec, "tok:" + token, 1.0f, 704);
            if (i + 1 < tokens.size()) {
                projectFeature(vec, "bi:" + token + "_" + tokens.get(i + 1), 0.65f, 704);
            }
        }

        // Project topic buckets strongly so domain queries align closely with matching documents.
        Set<String> topics = RetrievalQuery.topics(raw);
        for (String topic : topics) {
            projectFeature(vec, "topic:" + topic, 2.2f, 704);
        }

        // Deterministic low-weight tail (704..767) from SHA-256 of normalized text so vectors are never zero.
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((normalized.isEmpty() ? raw : normalized).getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < 64; i++) {
                int b = digest[i % digest.length] & 0xFF;
                float val = ((b / 127.5f) - 1.0f) * (tokens.isEmpty() ? 1.0f : 0.04f);
                vec[704 + i] += val;
            }
            if (isDocument) {
                vec[767] += 0.01f;
            }
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }

        return VectorValues.normalized(vec, DIMENSIONS);
    }

    private static void projectFeature(float[] vec, String feature, float weight, int bucketCount) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(feature.getBytes(StandardCharsets.UTF_8));
            for (int h = 0; h < 6; h++) {
                int offset = h * 4;
                int hash = ((d[offset] & 0xFF) << 24)
                        | ((d[offset + 1] & 0xFF) << 16)
                        | ((d[offset + 2] & 0xFF) << 8)
                        | (d[offset + 3] & 0xFF);
                int idx = Math.abs(hash % bucketCount);
                float sign = ((d[24 + h] & 1) == 0) ? 1.0f : -1.0f;
                vec[idx] += sign * weight;
            }
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
