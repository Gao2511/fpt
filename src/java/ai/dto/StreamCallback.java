package ai.dto;

/**
 * Callback để nhận dữ liệu streaming (Server-Sent Events)
 */
public interface StreamCallback {
    /** Nhận một phần văn bản mới sinh */
    void onToken(String token);

    /** Nhận kết quả tổng kết khi hoàn tất */
    void onComplete(ChatResponse response);

    /** Nhận thông báo lỗi */
    void onError(Throwable error);
}
