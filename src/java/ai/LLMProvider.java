package ai;

import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.dto.ModelInfo;
import ai.dto.StreamCallback;
import ai.exception.AIException;

import java.util.List;

/**
 * Interface chung cho tất cả các nhà cung cấp mô hình ngôn ngữ lớn (LLM Provider).
 * Áp dụng Adapter Pattern để chuẩn hóa hành vi gọi API giữa Google Gemini, OpenAI,
 * DeepSeek, Anthropic Claude và các dịch vụ tương thích khác.
 */
public interface LLMProvider {

    /** Định danh duy nhất của Provider (ví dụ: gemini, openai, deepseek, claude, custom) */
    String getId();

    /** Tên hiển thị thân thiện trên giao diện */
    String getDisplayName();

    /** Base URL mặc định của hãng (nếu có) */
    String getDefaultBaseUrl();

    /** Liệt kê danh sách các model khả dụng từ tài khoản API của hãng */
    List<ModelInfo> listModels(String apiKey, String baseUrl) throws AIException;

    /** Danh sách model gợi ý sẵn có (fallback khi chưa gọi API) */
    List<ModelInfo> getPresetModels();

    /** Thực hiện yêu cầu sinh câu trả lời đồng bộ */
    ChatResponse chat(List<ChatMessage> messages, String apiKey, String model,
                      String baseUrl, ChatOptions options) throws AIException;

    /** Thực hiện yêu cầu sinh câu trả lời dạng Streaming (Server-Sent Events) */
    void chatStream(List<ChatMessage> messages, String apiKey, String model,
                    String baseUrl, ChatOptions options, StreamCallback callback);

    /** Kiểm tra kết nối nhanh (ping test) và đo độ trễ mạng */
    ChatResponse testConnection(String apiKey, String model, String baseUrl) throws AIException;
}
