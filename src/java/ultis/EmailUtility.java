package ultis;

import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * EmailUtility - Gửi mail qua Resend API
 * Không dùng SMTP (vì Render chặn port 25/465/587)
 */
public class EmailUtility {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String FROM_EMAIL = "FPT Sale <onboarding@resend.dev>";

    public static boolean sendEmail(String toEmail, String subject, String body) {
        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("❌ [EmailUtility] RESEND_API_KEY chưa được cấu hình!");
            return false;
        }

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("from", FROM_EMAIL);
            requestBody.addProperty("to", toEmail);
            requestBody.addProperty("subject", subject);
            requestBody.addProperty("text", body);

            URL url = new URL(RESEND_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

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

            if (statusCode >= 200 && statusCode < 300) {
                System.out.println("✅ [EmailUtility] Gửi mail thành công đến: " + toEmail);
                System.out.println("   Response: " + responseStr.toString());
                return true;
            } else {
                System.err.println("❌ [EmailUtility] Gửi mail thất bại (HTTP " + statusCode + ")");
                System.err.println("   Response: " + responseStr.toString());
                return false;
            }

        } catch (IOException e) {
            System.err.println("❌ [EmailUtility] Lỗi kết nối: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
        public static boolean sendResetPasswordEmail(String toEmail, String userName, String resetLink) {
        String subject = "Đặt lại mật khẩu FPT ID";
        String body = "Xin chào " + (userName != null ? userName : "bạn") + ",\n\n"
                    + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản FPT ID của bạn.\n\n"
                    + "Click vào link sau để đặt lại mật khẩu (hết hạn sau 15 phút):\n"
                    + resetLink + "\n\n"
                    + "Nếu bạn KHÔNG yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n"
                    + "Trân trọng,\n"
                    + "FPT Telecom\n";
                 
        return sendEmail(toEmail, subject, body);
    }
}