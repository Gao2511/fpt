package controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.SettingsDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ai-chat")
public class AIChatServlet extends HttpServlet {

    private static final SettingsDAO settingsDAO = new SettingsDAO();

    // =========================================================
    // FALLBACK VALUES (khi DB chưa cấu hình)
    // =========================================================
    private static final String DEFAULT_API_KEY = "";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";

    // System prompt fallback khi DB hoàn toàn chưa thiết lập
    private static final String DEFAULT_SYSTEM_PROMPT =
        "Bạn là chuyên viên tư vấn AI thông minh của FPT Telecom. " +
        "Hãy tư vấn nhiệt tình, lịch sự, ngắn gọn và chính xác về các dịch vụ cáp quang, truyền hình FPT Play, camera FPT. " +
        "Hotline hỗ trợ: 0932 079 469.";

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

    // =========================================================
    // DO POST
    // =========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String userMessage = request.getParameter("message");

        // ===== VALIDATE RỖNG =====
        if (userMessage == null || userMessage.trim().isEmpty()) {
            writeJson(response, "Vui lòng nhập câu hỏi.");
            return;
        }

        userMessage = userMessage.trim();

        // ===== GIỚI HẠN ĐỘ DÀI CÂU HỎI =====
        if (userMessage.length() > 500) {
            writeJson(response, REFUSAL_MESSAGE);
            return;
        }

        // ===== RATE LIMITING (chống spam làm cạn quota API Key) =====
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

        // ===== BLOCK KEYWORD (chặn trước khi gọi API) =====
        if (containsBlockedKeyword(userMessage)) {
            System.out.println("🚫 [AI Chat] Blocked keyword: " + userMessage);
            writeJson(response, REFUSAL_MESSAGE);
            return;
        }

        // ===== ĐỌC CẤU HÌNH TỪ DB =====
        String enabledStr = settingsDAO.getValue("ai_chatbot_enabled");
        if ("false".equalsIgnoreCase(enabledStr)) {
            writeJson(response, "Hệ thống tư vấn AI hiện đang tạm bảo trì để nâng cấp. Anh/chị vui lòng gọi hotline 0932 079 469 để được tư vấn trực tiếp ạ.");
            return;
        }

        String apiKey = settingsDAO.getValue("gemini_api_key");
        String model = settingsDAO.getValue("gemini_model");
        String systemPrompt = settingsDAO.getValue("gemini_system_prompt");
        String tempStr = settingsDAO.getValue("gemini_temperature");
        String tokensStr = settingsDAO.getValue("gemini_max_tokens");

        // Kiểm tra an toàn: nếu Admin chưa nhập API Key
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("⚠️ [AI Chat] Chưa cấu hình Gemini API Key trong Admin Settings.");
            writeJson(response, "Hệ thống tư vấn AI đang được cập nhật cấu hình. Anh/chị vui lòng gọi hotline 0932 079 469 để được hỗ trợ ngay ạ.");
            return;
        }

        if (model == null || model.trim().isEmpty()) model = DEFAULT_MODEL;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) systemPrompt = DEFAULT_SYSTEM_PROMPT;

        double temperature = 0.7;
        if (tempStr != null) {
            try { temperature = Double.parseDouble(tempStr.trim()); } catch (Exception ignored) {}
        }

        int maxOutputTokens = 600;
        if (tokensStr != null) {
            try { maxOutputTokens = Integer.parseInt(tokensStr.trim()); } catch (Exception ignored) {}
        }

        // ===== GỌI API =====
        try {
            String aiReply = callGeminiAPI(userMessage, apiKey, model, systemPrompt, temperature, maxOutputTokens);
            writeJson(response, aiReply);
        } catch (Exception e) {
            System.err.println("========== LỖI AI CHAT (GEMINI) ==========");
            e.printStackTrace();
            writeJson(response, "Xin lỗi, em đang gặp sự cố. Anh/chị vui lòng gọi 0932 079 469 ạ.");
        }
    }

    // =========================================================
    // HELPER: GHI JSON RESPONSE
    // =========================================================
    private void writeJson(HttpServletResponse response, String reply) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("reply", reply);
        response.getWriter().write(result.toString());
    }

    // =========================================================
    // HELPER: KIỂM TRA CÓ CHỨA KEYWORD BỊ CHẶN KHÔNG
    // =========================================================
    private boolean containsBlockedKeyword(String message) {
        String lower = message.toLowerCase();
        for (String keyword : BLOCKED_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // =========================================================
    // GỌI GEMINI API
    // =========================================================
    private String callGeminiAPI(String userMessage, String apiKey, String model, String systemPrompt,
                                 double temperature, int maxOutputTokens)
            throws IOException {

        String fullEndpointUrl = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
            model, apiKey
        );

        URL url = new URL(fullEndpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        // ===== BUILD REQUEST BODY =====
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        JsonArray sysPartsArr = new JsonArray();
        sysPartsArr.add(sysPart);
        JsonObject sysInstruction = new JsonObject();
        sysInstruction.add("parts", sysPartsArr);

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userMessage);
        JsonArray userPartsArr = new JsonArray();
        userPartsArr.add(userPart);
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", userPartsArr);
        JsonArray contentsArr = new JsonArray();
        contentsArr.add(userContent);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", temperature);
        genConfig.addProperty("maxOutputTokens", maxOutputTokens);

        JsonObject requestBody = new JsonObject();
        requestBody.add("system_instruction", sysInstruction);
        requestBody.add("contents", contentsArr);
        requestBody.add("generationConfig", genConfig);

        // ===== GỬI REQUEST =====
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int statusCode = conn.getResponseCode();

        InputStreamReader streamReader = (statusCode >= 200 && statusCode < 300)
            ? new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            : new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8);

        StringBuilder responseStr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(streamReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
        }

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Gemini API error (" + statusCode + "): " + responseStr);
        }

        // ===== PARSE RESPONSE =====
        JsonObject jsonResponse = JsonParser.parseString(responseStr.toString()).getAsJsonObject();

        if (jsonResponse.has("candidates")) {
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                if (firstCandidate.has("content")) {
                    JsonObject content = firstCandidate.getAsJsonObject("content");
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && parts.size() > 0) {
                        return parts.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            }
        }

        throw new IOException("Cấu trúc JSON phản hồi từ Gemini không khớp: " + responseStr);
    }
}