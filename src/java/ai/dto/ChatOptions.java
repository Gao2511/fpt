package ai.dto;

import java.io.Serializable;

/**
 * Các tham số sinh chữ khi gọi Model
 */
public class ChatOptions implements Serializable {
    private static final long serialVersionUID = 1L;

    private double temperature = 0.7;
    private int maxTokens = 600;
    private Double topP = 0.95;

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
