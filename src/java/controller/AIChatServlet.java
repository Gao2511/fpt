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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ai-chat")
public class AIChatServlet extends HttpServlet {

    private static final SettingsDAO settingsDAO = new SettingsDAO();

    // Fallback nếu DB chưa có
    private static final String DEFAULT_API_KEY = "";
    private static final String DEFAULT_MODEL = "gemini-3.6-flash";
    private static final String DEFAULT_SYSTEM_PROMPT =
        "Bạn là nhân viên tư vấn của FPT Telecom. Nhiệm vụ của bạn là tư vấn khách hàng " +
        "về các gói cước Internet, truyền hình FPT Play, Camera AI. " +
        "Các gói cước hiện có:\n" +
        "- Gói 195: Internet 300Mbps, 195.000đ/tháng\n" +
        "- Gói 220: Internet 1Gbps + Truyền hình, 220.000đ/tháng\n" +
        "- Gói 239: Internet + Ngoại hạng Anh, 239.000đ/tháng\n" +
        "- Gói 249: Internet + Ngoại hạng Anh + Camera AI, 249.000đ/tháng\n" +
        "Hotline: 0932 079 469. Trả lời ngắn gọn, thân thiện, xưng 'em' gọi khách là 'anh/chị'.";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String userMessage = request.getParameter("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            response.getWriter().write("{\"reply\":\"Vui lòng nhập câu hỏi.\"}");
            return;
        }

        // ===== ĐỌC CẤU HÌNH TỪ DB =====
        String apiKey = settingsDAO.getValue("gemini_api_key");
        String model = settingsDAO.getValue("gemini_model");
        String systemPrompt = settingsDAO.getValue("gemini_system_prompt");

        // Fallback nếu DB chưa có
        if (apiKey == null || apiKey.trim().isEmpty()) apiKey = DEFAULT_API_KEY;
        if (model == null || model.trim().isEmpty()) model = DEFAULT_MODEL;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) systemPrompt = DEFAULT_SYSTEM_PROMPT;

        try {
            String aiReply = callGeminiAPI(userMessage, apiKey, model, systemPrompt);
            JsonObject result = new JsonObject();
            result.addProperty("reply", aiReply);
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            System.err.println("========== LỖI AI CHAT (GEMINI) ==========");
            e.printStackTrace();

            JsonObject error = new JsonObject();
            error.addProperty("reply", "Xin lỗi, em đang gặp sự cố. Anh/chị vui lòng gọi 0932 079 469 ạ.");
            response.getWriter().write(error.toString());
        }
    }

    private String callGeminiAPI(String userMessage, String apiKey, String model, String systemPrompt)
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
        genConfig.addProperty("temperature", 0.7);
        genConfig.addProperty("maxOutputTokens", 500);

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