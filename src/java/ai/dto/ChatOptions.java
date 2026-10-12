package ai.dto;

import java.io.Serializable;

/**
 * Các tham số sinh chữ khi gọi Model
 */
public class ChatOptions implements Serializable {
    private static final long serialVersionUID = 1L;

    private double temperature = 0.3;
    private int maxTokens = 1200;
    private Double topP = 0.95;
    private com.google.gson.JsonObject responseSchema;
    private long deadlineMillis;
    private String thinkingLevel;
    private int maxProviderAttempts=3;
    public int getMaxProviderAttempts(){return maxProviderAttempts;}
    public void setMaxProviderAttempts(int value){if(value<1||value>3)throw new IllegalArgumentException();maxProviderAttempts=value;}

    public com.google.gson.JsonObject getResponseSchema() { return responseSchema; }
    public void setResponseSchema(com.google.gson.JsonObject schema) { responseSchema = schema; }
    public long getDeadlineMillis() { return deadlineMillis; }
    public void setDeadlineMillis(long deadline) { deadlineMillis = deadline; }
    public String getThinkingLevel() { return thinkingLevel; }
    public void setThinkingLevel(String thinkingLevel) { this.thinkingLevel = thinkingLevel; }
    public int remainingMillis(int maximum) {
        if (deadlineMillis == 0) return maximum;
        return (int) Math.max(1, Math.min(maximum, deadlineMillis - System.currentTimeMillis()));
    }
    public boolean isExpired() { return deadlineMillis > 0 && System.currentTimeMillis() >= deadlineMillis; }

    public ChatOptions copyForTarget(String targetThinkingLevel, int targetMaxTokens) {
        ChatOptions copy = new ChatOptions(this.temperature, targetMaxTokens > 0 ? targetMaxTokens : this.maxTokens);
        copy.setTopP(this.topP);
        copy.setResponseSchema(this.responseSchema);
        copy.setDeadlineMillis(this.deadlineMillis);
        copy.setMaxProviderAttempts(this.maxProviderAttempts);
        if (targetThinkingLevel != null && !targetThinkingLevel.trim().isEmpty()) {
            copy.setThinkingLevel(targetThinkingLevel.trim());
        } else {
            copy.setThinkingLevel(this.thinkingLevel);
        }
        return copy;
    }

    public ChatOptions() {
    }

    public ChatOptions(double temperature, int maxTokens) {
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }
}
