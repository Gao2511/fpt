package ai.dto;

import java.io.Serializable;

/**
 * Kết quả trả về chuẩn hóa từ mọi LLM Provider
 */
public class ChatResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;
    private String provider;
    private String model;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private String finishReason;
    private long latencyMs;

    public ChatResponse() {
    }

    public ChatResponse(String content, String provider, String model, long latencyMs) {
        this.content = content;
        this.provider = provider;
        this.model = model;
        this.latencyMs = latencyMs;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
