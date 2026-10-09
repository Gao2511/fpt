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

    public com.google.gson.JsonObject getResponseSchema() { return responseSchema; }
    public void setResponseSchema(com.google.gson.JsonObject schema) { responseSchema = schema; }
    public void setDeadlineMillis(long deadline) { deadlineMillis = deadline; }
    public int remainingMillis(int maximum) {
        if (deadlineMillis == 0) return maximum;
        return (int) Math.max(1, Math.min(maximum, deadlineMillis - System.currentTimeMillis()));
    }
    public boolean isExpired() { return deadlineMillis > 0 && System.currentTimeMillis() >= deadlineMillis; }

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
