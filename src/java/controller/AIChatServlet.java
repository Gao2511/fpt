package controller;

import ai.AIService;
import ai.dto.ChatResponse;
import ai.exception.AIException;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Endpoint trò chuyện AI công khai dành cho khách hàng trên website fpt.gialai.vn
 * - Tuyệt đối không nhận tên provider/model/key từ phía client
 * - Sử dụng kiến trúc Provider trung tâm từ AIService
 * - Tự động Fallback sang model dự phòng khi model chính quá tải
 * - Lọc Output Guardrail chống rò rỉ thông tin nhạy cảm
 */
@WebServlet("/ai-chat")
public class AIChatServlet extends HttpServlet {

    // =========================================================
    // DANH SÁCH KEYWORD BỊ CHẶN (block trước khi gọi API)
    // =========================================================
    private static final List<String> BLOCKED_KEYWORDS = Arrays.asList(
        // Chính trị - tôn giáo
        "chính trị", "tôn giáo", "đảng", "chủ tịch", "chính phủ", "quốc hội",
        "công an", "quân đội", "biểu tình", "phản động",
        // Đối thủ
        "viettel", "vnpt", "mobifone", "vinaphone", "vietnamobile",
        "gmobile", "reddi", "wintel", "sfone",
        // Lập trình - kỹ thuật
        "code", "lập trình", "python", "java", "javascript", "html", "css",
        "sql", "database", "hack", "crack", "exploit",
        // Học tập - văn bản
        "dịch văn bản", "viết bài", "viết luận", "viết văn", "làm thơ",
        "toán", "vật lý", "hóa học", "sinh học", "lịch sử",
        // Đời sống - sức khỏe
        "sức khỏe", "bệnh", "thuốc", "bác sĩ", "y tế",
        "tình yêu", "tình cảm", "hẹn hò", "sex", "18+",
        // Giải trí - thể thao
        "thời tiết", "bóng đá", "bóng rổ", "ca sĩ", "diễn viên",
        "phim", "nhạc", "game", "cá cược", "cờ bạc",
        // Prompt injection
        "bỏ qua", "ignore", "system prompt", "system instruction",
        "api key", "mật khẩu", "password", "token",
        "đóng vai", "roleplay", "act as", "pretend"
    );

    // =========================================================
    // TIN NHẮN TỪ CHỐI
    // =========================================================
    private static final String REFUSAL_MESSAGE =
        "Xin lỗi anh/chị, em chỉ có thể tư vấn về dịch vụ FPT Telecom. " +
        "Anh/chị vui lòng gọi 0932 079 469 để được hỗ trợ thêm ạ.";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String userMessage = request.getParameter("message");

        // ===== 1. VALIDATE RỖNG =====
        if (userMessage == null || userMessage.trim().isEmpty()) {
            writeJson(response, "Vui lòng nhập câu hỏi.");
            return;
        }

        userMessage = userMessage.trim();

        // ===== 2. GIỚI HẠN ĐỘ DÀI CÂU HỎI =====
        if (userMessage.length() > 500) {
            writeJson(response, REFUSAL_MESSAGE);
            return;
        }

        // ===== 3. RATE LIMITING (chống spam làm cạn quota API) =====
        HttpSession session = request.getSession(true);
        Long lastChatTime = (Long) session.getAttribute("ai_last_chat_time");
        Integer chatCount = (Integer) session.getAttribute("ai_chat_count");
        long now = System.currentTimeMillis();

        if (lastChatTime == null || now - lastChatTime > 60000) {
            session.setAttribute("ai_last_chat_time", now);
            session.setAttribute("ai_chat_count", 1);
        } else {
            if (chatCount != null && chatCount >= 10) {
                writeJson(response, "Anh/chị đang gửi câu hỏi quá nhanh. Vui lòng đợi 1 phút rồi thử lại giúp em nhé.");
                return;
            }
            session.setAttribute("ai_chat_count", (chatCount != null ? chatCount : 0) + 1);
        }

        // ===== 4. BLOCK KEYWORD (chặn trước khi gọi API) =====
        if (containsBlockedKeyword(userMessage)) {
            System.out.println("🚫 [AI Chat] Chặn từ khóa nhạy cảm: " + userMessage);
            writeJson(response, REFUSAL_MESSAGE);
            return;
        }

        // ===== 5. GỌI AISERVICE (TỰ ĐỘNG MULTI-PROVIDER & FALLBACK) =====
        try {
            ChatResponse chatResponse = AIService.askAI(userMessage);
            writeJson(response, chatResponse.getContent());
        } catch (AIException aie) {
            System.err.println("⚠️ [AI Chat] " + aie.getProviderName() + " lỗi: " + aie.getMessage());
            writeJson(response, "Xin lỗi anh/chị, em đang tiếp nhận lượng câu hỏi lớn. Anh/chị vui lòng gọi hotline 0932 079 469 để em hỗ trợ tư vấn lắp đặt ngay ạ.");
        } catch (Exception e) {
            System.err.println("❌ [AI Chat System Error] " + e.getMessage());
            writeJson(response, "Xin lỗi anh/chị, hệ thống đang bận. Anh/chị vui lòng liên hệ hotline 0932 079 469 ạ.");
        }
    }

    private void writeJson(HttpServletResponse response, String reply) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("reply", reply != null ? reply : "");
        response.getWriter().write(result.toString());
    }

    private boolean containsBlockedKeyword(String message) {
        String lower = message.toLowerCase();
        for (String keyword : BLOCKED_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}