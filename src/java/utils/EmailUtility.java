package utils;

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
        return sendEmail(toEmail, subject, body, null);
    }

    public static boolean sendEmail(String toEmail, String subject, String textBody, String htmlBody) {
        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            try {
                apiKey = new dao.SettingsDAO().getValue("resend_api_key");
            } catch (Exception ignored) {}
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("⚠️ [EmailUtility] RESEND_API_KEY chưa được cấu hình. (Đã ghi log nội dung mail bên dưới)");
            System.out.println("   📧 To: " + toEmail + " | Subject: " + subject);
            System.out.println("   📄 Body: " + textBody);
            return true; // Trả về true để không chặn luồng dev/test
        }

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("from", FROM_EMAIL);
            requestBody.addProperty("to", toEmail);
            requestBody.addProperty("subject", subject);
            if (textBody != null) {
                requestBody.addProperty("text", textBody);
            }
            if (htmlBody != null) {
                requestBody.addProperty("html", htmlBody);
            }

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

    public static boolean sendOtpEmail(String toEmail, String userName, String otpCode) {
        String subject = "[FPT Telecom] Mã xác thực OTP đặt lại mật khẩu: " + otpCode;
        String textBody = "Xin chào " + (userName != null ? userName : "quý khách") + ",\n\n"
                        + "Mã OTP xác thực đặt lại mật khẩu của bạn là: " + otpCode + "\n\n"
                        + "Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n"
                        + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                        + "Trân trọng,\nFPT Telecom";

        String htmlBody = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;\">"
                        + "  <div style=\"text-align: center; margin-bottom: 20px;\">"
                        + "    <h2 style=\"color: #f37021; margin: 0;\">FPT TELECOM</h2>"
                        + "    <p style=\"color: #64748b; font-size: 14px; margin: 5px 0 0 0;\">Xác thực đặt lại mật khẩu FPT ID</p>"
                        + "  </div>"
                        + "  <p style=\"font-size: 15px; color: #1e293b;\">Xin chào <strong>" + (userName != null ? userName : "quý khách") + "</strong>,</p>"
                        + "  <p style=\"font-size: 14px; color: #475569; line-height: 1.6;\">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản FPT ID của bạn. Dưới đây là mã xác thực OTP của bạn:</p>"
                        + "  <div style=\"text-align: center; margin: 25px 0;\">"
                        + "    <span style=\"display: inline-block; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #f37021; background: #fff5eb; padding: 14px 28px; border-radius: 8px; border: 2px dashed #f37021;\">" + otpCode + "</span>"
                        + "  </div>"
                        + "  <p style=\"font-size: 13px; color: #dc2626; text-align: center;\">⏱️ Mã OTP này có hiệu lực trong vòng <strong>5 phút</strong>.</p>"
                        + "  <p style=\"font-size: 13px; color: #64748b; line-height: 1.5;\">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ ngay hotline <strong>1900 6600</strong> để bảo vệ tài khoản.</p>"
                        + "  <hr style=\"border: none; border-top: 1px solid #f1f5f9; margin: 25px 0;\">"
                        + "  <p style=\"font-size: 12px; color: #94a3b8; text-align: center; margin: 0;\">Đây là email tự động từ hệ thống FPT Telecom. Vui lòng không trả lời email này.</p>"
                        + "</div>";

        System.out.println("====================================================");
        System.out.println("🔐 [FPT TELECOM OTP] Email: " + toEmail + " | Mã OTP: " + otpCode);
        System.out.println("====================================================");

        return sendEmail(toEmail, subject, textBody, htmlBody);
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