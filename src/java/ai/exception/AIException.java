package ai.exception;

/**
 * Ngoại lệ chuẩn hóa từ mọi LLM Provider
 */
public class AIException extends Exception {
    private static final long serialVersionUID = 1L;

    public enum ErrorType {
        INVALID_KEY,      // 401 / 403: Sai API Key hoặc chưa cấp quyền
        QUOTA_EXCEEDED,   // 429: Hết hạn mức tiền / giới hạn số lượt gọi (Rate Limit)
        MODEL_NOT_FOUND,  // 404: Tên model không tồn tại
        HIGH_DEMAND,      // 503 / high demand: Máy chủ hãng quá tải tạm thời
        TIMEOUT,          // Quá thời gian chờ phản hồi
        NETWORK_ERROR,    // Lỗi kết nối mạng
        SERVER_ERROR,     // 500: Lỗi nội bộ từ máy chủ hãng
        INVALID_REQUEST,  // 400: Tham số hoặc định dạng không hợp lệ
        UNKNOWN           // Lỗi không xác định
    }

    private final ErrorType errorType;
    private final int statusCode;
    private final String providerName;

    public AIException(ErrorType errorType, int statusCode, String message, String providerName) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
        this.providerName = providerName;
    }

    public AIException(ErrorType errorType, String message, String providerName) {
        super(message);
        this.errorType = errorType;
        this.statusCode = 0;
        this.providerName = providerName;
    }

    public AIException(ErrorType errorType, String message, String providerName, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = 0;
        this.providerName = providerName;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getProviderName() {
        return providerName;
    }

    /**
     * Chuyển mã HTTP và thông báo từ API hãng thành thông báo tiếng Việt dễ hiểu cho Admin
     */
    public static AIException fromHttp(int statusCode, String rawBody, String providerName, String modelName) {
        String lower = (rawBody != null) ? rawBody.toLowerCase() : "";
        ErrorType type;
        String friendlyMessage;

        if (statusCode == 401 || statusCode == 403 || lower.contains("api_key_invalid") || lower.contains("invalid api key") || lower.contains("authentication")) {
            type = ErrorType.INVALID_KEY;
            friendlyMessage = "Khóa API Key của " + providerName + " không hợp lệ hoặc đã hết hạn. Vui lòng kiểm tra lại.";
        } else if (statusCode == 404 || lower.contains("model not found") || lower.contains("does not exist")) {
            type = ErrorType.MODEL_NOT_FOUND;
            friendlyMessage = "Không tìm thấy model '" + modelName + "' tại " + providerName + ". Vui lòng chọn hoặc đồng bộ lại model khác.";
        } else if (statusCode == 429 || lower.contains("quota") || lower.contains("rate limit") || lower.contains("insufficient_quota")) {
            type = ErrorType.QUOTA_EXCEEDED;
            friendlyMessage = "Tài khoản " + providerName + " đã vượt quá hạn mức truy vấn (Quota) hoặc hết số dư khả dụng.";
        } else if (statusCode == 503 || lower.contains("high demand") || lower.contains("overloaded") || lower.contains("capacity")) {
            type = ErrorType.HIGH_DEMAND;
            friendlyMessage = "Máy chủ " + providerName + " cho model '" + modelName + "' đang bị quá tải tạm thời (High Demand). Hệ thống sẽ tự động thử lại hoặc chuyển model dự phòng.";
        } else if (statusCode >= 500) {
            type = ErrorType.SERVER_ERROR;
            friendlyMessage = "Máy chủ " + providerName + " đang gặp sự cố nội bộ (HTTP " + statusCode + "). Vui lòng thử lại sau ít phút.";
        } else if (statusCode == 400) {
            type = ErrorType.INVALID_REQUEST;
            friendlyMessage = "Yêu cầu gửi đến " + providerName + " không hợp lệ: " + extractBriefError(rawBody);
        } else {
            type = ErrorType.UNKNOWN;
            friendlyMessage = "Lỗi phản hồi từ " + providerName + " (HTTP " + statusCode + "): " + extractBriefError(rawBody);
        }

        return new AIException(type, statusCode, friendlyMessage, providerName);
    }

    private static String extractBriefError(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Không có chi tiết.";
        if (raw.length() > 200) {
            return raw.substring(0, 200) + "...";
        }
        return raw;
    }
}
