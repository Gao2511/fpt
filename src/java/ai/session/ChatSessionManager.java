package ai.session;

import ai.LLMProvider;
import ai.dto.ChatMessage;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trình quản lý bộ nhớ phiên hội thoại và sổ tay trạng thái khách hàng (Session Memory Manager).
 * Giúp chatbot "không quên ngữ cảnh", duy trì thông tin qua nhiều lượt trò chuyện.
 */
public class ChatSessionManager {

    private static final Map<String, ChatSessionData> SESSIONS = new ConcurrentHashMap<>();

    // Regex trích xuất khối <state>{...}</state> ở cuối câu trả lời của AI
    private static final Pattern STATE_PATTERN = Pattern.compile(
            "<state>\\s*(\\{.*?\\})\\s*</state>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Regex dự phòng nếu thẻ đóng bị thiếu hoặc sai lệch
    private static final Pattern LOOSE_STATE_PATTERN = Pattern.compile(
            "<state>.*?(</state>|$)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    /**
     * Lấy hoặc tạo mới dữ liệu phiên chat theo sessionId
     */
    public static ChatSessionData getOrCreate(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "guest_default";
        }
        cleanupExpiredSessions();
        return SESSIONS.computeIfAbsent(sessionId, ChatSessionData::new);
    }

    /**
     * Xóa trắng lịch sử và reset sổ tay của phiên (Dùng cho Playground hoặc nút xóa)
     */
    public static void clearSession(String sessionId) {
        if (sessionId == null) return;
        ChatSessionData data = SESSIONS.get(sessionId);
        if (data != null) {
            data.clearHistory();
        }
    }

    /**
     * Trích xuất khối <state>{...}</state>, cập nhật sổ tay, kiểm tra tạo lead,
     * và XÓA sạch khối này khỏi nội dung gửi đến khách hàng.
     * @return Nội dung phản hồi đã làm sạch (Clean Text)
     */
    public static String extractAndUpdateState(ChatSessionData sessionData, String rawAiResponse) {
        if (rawAiResponse == null) return "";

        Matcher matcher = STATE_PATTERN.matcher(rawAiResponse);
        if (matcher.find()) {
            String jsonStr = matcher.group(1);
            try {
                JsonObject newState = JsonParser.parseString(jsonStr).getAsJsonObject();
                sessionData.mergeState(newState);

                // Kiểm tra xem đã đủ thông tin để tạo Lead tự động chưa
                LeadService.checkAndCreateLead(sessionData);
            } catch (Exception e) {
                System.err.println("⚠️ [ChatSessionManager] Không thể parse JSON từ thẻ <state>: " + jsonStr + " (Lỗi: " + e.getMessage() + ")");
                // Giữ nguyên state cũ, không gây lỗi hệ thống
            }

            // Xóa sạch khối <state>...</state> khỏi câu trả lời
            String cleanText = matcher.replaceAll("").trim();
            return cleanText;
        }

        // Nếu model vô tình viết sai cú pháp thẻ <state>, xóa sạch để tránh rò rỉ mã
        Matcher looseMatcher = LOOSE_STATE_PATTERN.matcher(rawAiResponse);
        if (looseMatcher.find()) {
            return looseMatcher.replaceAll("").trim();
        }

        return rawAiResponse.trim();
    }

    /**
     * Xây dựng nội dung tin nhắn user có đính kèm thẻ <sotay_hientai>{...}</sotay_hientai>
     */
    public static String wrapUserMessageWithState(ChatSessionData sessionData, String userQuestion) {
        if (sessionData == null) return userQuestion;

        JsonObject state = sessionData.getState();
        String stateStr = state.toString();

        return "<sotay_hientai>" + stateStr + "</sotay_hientai>\n" + userQuestion;
    }

    /**
     * Kiểm tra và tóm tắt lịch sử hội thoại khi vượt quá 20 lượt (10 cặp user-bot).
     * Sử dụng một lần gọi LLM riêng để tóm tắt các lượt cũ và đặt bản tóm tắt lên đầu lịch sử.
     */
    public static void summarizeAndTrimIfNeeded(ChatSessionData sessionData, LLMProvider provider,
                                                String apiKey, String model, String baseUrl) {
        if (sessionData == null) return;

        List<ChatMessage> history = sessionData.getHistory();
        // 20 lượt tương đương 20 tin nhắn (10 cặp hội thoại)
        if (history.size() <= 20) {
            return;
        }

        System.out.println("🔄 [ChatSessionManager] Hội thoại session " + sessionData.getSessionId()
                + " đã vượt quá 20 lượt (" + history.size() + " tin nhắn). Tiến hành tóm tắt ngữ cảnh cũ...");

        try {
            // Tách 10 tin nhắn đầu tiên để tóm tắt
            int trimCount = 10;
            List<ChatMessage> toSummarize = new ArrayList<>(history.subList(0, trimCount));
            List<ChatMessage> remaining = new ArrayList<>(history.subList(trimCount, history.size()));

            StringBuilder oldConvoText = new StringBuilder();
            for (ChatMessage m : toSummarize) {
                oldConvoText.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
            }

            // Gọi LLM riêng để tóm tắt
            List<ChatMessage> sumReq = new ArrayList<>();
            sumReq.add(ChatMessage.system("Bạn là chuyên gia tóm tắt hội thoại bán hàng."));
            sumReq.add(ChatMessage.user(
                    "Hãy tóm tắt ngắn gọn trong 2-3 câu các điểm mấu chốt của cuộc trò chuyện sau " +
                    "(nhu cầu khách hàng, thông tin đã cung cấp, gói cước đã tư vấn):\n" + oldConvoText
            ));

            ChatOptions opt = new ChatOptions(0.3, 150);
            ChatResponse sumResp = provider.chat(sumReq, apiKey, model, baseUrl, opt);
            String summaryContent = sumResp.getContent();

            // Tạo tin nhắn tóm tắt đặt lên đầu danh sách tin nhắn còn lại
            ChatMessage summaryMsg = ChatMessage.user("[TÓM TẮT HỘI THOẠI TRƯỚC]: " + summaryContent.trim());
            remaining.add(0, summaryMsg);

            // Cập nhật lại danh sách lịch sử mới
            sessionData.setHistory(remaining);
            System.out.println("✓ [ChatSessionManager] Tóm tắt hoàn tất: " + summaryContent);
        } catch (Exception e) {
            System.err.println("⚠️ [ChatSessionManager] Lỗi khi tóm tắt lịch sử hội thoại: " + e.getMessage());
            // Nếu lỗi tóm tắt thì cắt gọt trực tiếp giữ 20 tin nhắn gần nhất
            if (history.size() > 20) {
                sessionData.setHistory(new ArrayList<>(history.subList(history.size() - 20, history.size())));
            }
        }
    }

    /**
     * Dọn dẹp các session không hoạt động quá 2 giờ để giải phóng RAM
     */
    private static void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        long twoHoursMs = 2 * 60 * 60 * 1000L;

        SESSIONS.entrySet().removeIf(entry -> (now - entry.getValue().getLastActivityTime()) > twoHoursMs);
    }
}
