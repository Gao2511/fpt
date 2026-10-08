package controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dao.SettingsDAO;
import dto.UserDTO;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/admin/settings")
public class AdminSettingsServlet extends HttpServlet {

    private final SettingsDAO settingsDAO = new SettingsDAO();

    // =========================================================
    // DO GET: Hiển thị trang cài đặt AI
    // =========================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Load tất cả cấu hình từ DB
        Map<String, String> settingsMap = settingsDAO.getAllAsMap();
        request.setAttribute("settingsMap", settingsMap);

        // Flash message
        HttpSession session = request.getSession();
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("messageType", session.getAttribute("messageType"));
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        request.getRequestDispatcher("/view/admin/settings.jsp")
               .forward(request, response);
    }

    // =========================================================
    // DO POST: Xử lý cập nhật cấu hình hoặc kiểm tra kết nối API
    // =========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ===== 1. KIỂM TRA KẾT NỐI GEMINI API (AJAX TEST) =====
        if ("testConnection".equals(action)) {
            response.setContentType("application/json;charset=UTF-8");

            String apiKey = request.getParameter("gemini_api_key");
            String model = request.getParameter("gemini_model");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                apiKey = settingsDAO.getValue("gemini_api_key");
            }
            if (model == null || model.trim().isEmpty()) {
                model = settingsDAO.getValue("gemini_model");
            }
            if (model == null || model.trim().isEmpty()) {
                model = "gemini-1.5-flash";
            }

            apiKey = (apiKey != null) ? apiKey.trim() : "";
            model = model.trim();

            JsonObject result = testGeminiApi(apiKey, model);
            response.getWriter().write(result.toString());
            return;
        }

        // ===== 2. ĐỒNG BỘ DANH SÁCH MODEL TỪ GOOGLE GEMINI (FETCH MODELS) =====
        if ("fetchModels".equals(action)) {
            response.setContentType("application/json;charset=UTF-8");

            String apiKey = request.getParameter("gemini_api_key");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                apiKey = settingsDAO.getValue("gemini_api_key");
            }
            apiKey = (apiKey != null) ? apiKey.trim() : "";

            JsonObject result = fetchGeminiModels(apiKey);
            response.getWriter().write(result.toString());
            return;
        }

        // ===== 3. LƯU CẤU HÌNH AI TOÀN DIỆN =====
        if ("updateAI".equals(action)) {
            String apiKey = request.getParameter("gemini_api_key");
            String model = request.getParameter("gemini_model");
            String systemPrompt = request.getParameter("gemini_system_prompt");
            String temperature = request.getParameter("gemini_temperature");
            String maxTokens = request.getParameter("gemini_max_tokens");
            String chatbotEnabled = request.getParameter("ai_chatbot_enabled");

            HttpSession session = request.getSession();

            // Cập nhật API Key nếu có nhập
            if (apiKey != null) {
                settingsDAO.update("gemini_api_key", apiKey.trim());
            }

            // Cập nhật Model
            if (model != null && !model.trim().isEmpty()) {
                settingsDAO.update("gemini_model", model.trim());
            }

            // Cập nhật System Prompt (chỉ dẫn persona & kiến thức)
            if (systemPrompt != null) {
                settingsDAO.update("gemini_system_prompt", systemPrompt.trim());
            }

            // Cập nhật Temperature
            if (temperature != null && !temperature.trim().isEmpty()) {
                settingsDAO.update("gemini_temperature", temperature.trim());
            } else {
                settingsDAO.update("gemini_temperature", "0.7");
            }

            // Cập nhật Max Output Tokens
            if (maxTokens != null && !maxTokens.trim().isEmpty()) {
                settingsDAO.update("gemini_max_tokens", maxTokens.trim());
            } else {
                settingsDAO.update("gemini_max_tokens", "600");
            }

            // Cập nhật Trạng thái bật/tắt Chatbot
            if ("true".equalsIgnoreCase(chatbotEnabled) || "on".equalsIgnoreCase(chatbotEnabled)) {
                settingsDAO.update("ai_chatbot_enabled", "true");
            } else {
                settingsDAO.update("ai_chatbot_enabled", "false");
            }

            session.setAttribute("message", "✓ Đã lưu toàn bộ cấu hình AI thành công! Thay đổi có hiệu lực ngay lập tức.");
            session.setAttribute("messageType", "success");
        }

        response.sendRedirect(request.getContextPath() + "/admin/settings");
    }

    // =========================================================
    // HELPER: TEST KẾT NỐI GEMINI API TRỰC TIẾP
    // =========================================================
    private JsonObject testGeminiApi(String apiKey, String model) {
        JsonObject result = new JsonObject();

        if (apiKey == null || apiKey.isEmpty()) {
            result.addProperty("success", false);
            result.addProperty("message", "Vui lòng nhập API Key trước khi kiểm tra kết nối!");
            return result;
        }

        long startTime = System.currentTimeMillis();
        try {
            String fullEndpointUrl = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
            );

            URL url = new URL(fullEndpointUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            // Gửi ping ngắn
            JsonObject userPart = new JsonObject();
            userPart.addProperty("text", "Xin chào FPT Telecom");
            JsonArray parts = new JsonArray();
            parts.add(userPart);
            JsonObject content = new JsonObject();
            content.addProperty("role", "user");
            content.add("parts", parts);
            JsonArray contents = new JsonArray();
            contents.add(content);

            JsonObject body = new JsonObject();
            body.add("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();
            long latency = System.currentTimeMillis() - startTime;

            InputStream stream = (statusCode >= 200 && statusCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
            }

            if (statusCode >= 200 && statusCode < 300) {
                result.addProperty("success", true);
                result.addProperty("message", "Kết nối thành công tới model " + model + " (Độ trễ: " + latency + "ms)!");
                result.addProperty("latencyMs", latency);
                result.addProperty("model", model);
            } else {
                result.addProperty("success", false);
                String errorDetail = "Mã lỗi HTTP " + statusCode;
                try {
                    JsonObject errorJson = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    if (errorJson.has("error")) {
                        JsonObject err = errorJson.getAsJsonObject("error");
                        if (err.has("message")) {
                            errorDetail = err.get("message").getAsString();
                        }
                    }
                } catch (Exception ignored) {}

                if (statusCode == 400 && errorDetail.toLowerCase().contains("api key")) {
                    result.addProperty("message", "API Key không hợp lệ! Vui lòng kiểm tra lại key của bạn.");
                } else if (statusCode == 404) {
                    result.addProperty("message", "Không tìm thấy model '" + model + "'. Hãy chọn một model khác.");
                } else if (statusCode == 429) {
                    result.addProperty("message", "API Key đã vượt quá hạn mức truy vấn (Quota Exceeded).");
                } else {
                    result.addProperty("message", "Lỗi từ Google Gemini: " + errorDetail);
                }
            }
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("message", "Lỗi kết nối mạng: " + e.getMessage());
        }

        return result;
    }

    // =========================================================
    // HELPER: LẤY DANH SÁCH CÁC MODEL KHẢ DỤNG TỪ GOOGLE GEMINI
    // =========================================================
    private JsonObject fetchGeminiModels(String apiKey) {
        JsonObject result = new JsonObject();
        if (apiKey == null || apiKey.isEmpty()) {
            result.addProperty("success", false);
            result.addProperty("message", "Vui lòng nhập API Key trước khi đồng bộ danh sách Model!");
            return result;
        }

        try {
            String urlStr = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder sb = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }
            }

            if (code >= 200 && code < 300) {
                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                JsonArray modelsArray = json.getAsJsonArray("models");
                JsonArray availableModels = new JsonArray();

                if (modelsArray != null) {
                    for (int i = 0; i < modelsArray.size(); i++) {
                        JsonObject m = modelsArray.get(i).getAsJsonObject();
                        // Chỉ lấy model hỗ trợ sinh văn bản (generateContent)
                        boolean canGenerate = false;
                        if (m.has("supportedGenerationMethods")) {
                            JsonArray methods = m.getAsJsonArray("supportedGenerationMethods");
                            for (int j = 0; j < methods.size(); j++) {
                                if ("generateContent".equals(methods.get(j).getAsString())) {
                                    canGenerate = true;
                                    break;
                                }
                            }
                        }

                        if (canGenerate) {
                            String name = m.get("name").getAsString(); // e.g. "models/gemini-1.5-flash"
                            String id = name.startsWith("models/") ? name.substring(7) : name;
                            String displayName = m.has("displayName") ? m.get("displayName").getAsString() : id;

                            JsonObject item = new JsonObject();
                            item.addProperty("id", id);
                            item.addProperty("displayName", displayName);
                            if (m.has("description")) {
                                item.addProperty("description", m.get("description").getAsString());
                            }
                            availableModels.add(item);
                        }
                    }
                }

                result.addProperty("success", true);
                result.addProperty("message", "Đã lấy thành công " + availableModels.size() + " models từ tài khoản Google Gemini!");
                result.add("models", availableModels);
            } else {
                result.addProperty("success", false);
                result.addProperty("message", "Không thể lấy danh sách model từ Google (HTTP " + code + "). Vui lòng kiểm tra lại API Key.");
            }
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("message", "Lỗi kết nối khi lấy model: " + e.getMessage());
        }

        return result;
    }
}