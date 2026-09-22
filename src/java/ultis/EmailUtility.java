package utils;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

public class EmailUtility {

    // ====================================================
    // MAIL HỆ THỐNG - Gmail dùng để GỬI thông báo
    // ====================================================
    private static final String FROM_EMAIL   = "gaoji281125@gmail.com";
    
    // ⚠️ THAY BẰNG APP PASSWORD 16 KÝ TỰ CỦA GAOJI281125@GMAIL.COM
    // Cách lấy: https://myaccount.google.com/apppasswords
    private static final String APP_PASSWORD = "bjyd xpjf dqbv tebe";

    public static boolean sendEmail(String toEmail, String subject, String bodyText) {
        System.out.println("==================================================");
        System.out.println("📧 [EmailUtility] Bắt đầu gửi email:");
        System.out.println("   FROM_EMAIL = " + FROM_EMAIL);
        System.out.println("   TO_EMAIL   = " + toEmail);
        System.out.println("   Subject    = " + subject);

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        
        // BỔ SUNG: Ép buộc mã hóa UTF-8 ngay từ cấu hình
        props.put("mail.mime.charset", "UTF-8");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "FPT Telecom", "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            // ===== SỬA LỖI CHÍNH Ở ĐÂY =====
            // Thêm "UTF-8" vào cả subject và body để hiển thị đúng tiếng Việt
          message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
          message.setContent(bodyText, "text/plain; charset=UTF-8");
            // ================================

            Transport.send(message);
            System.out.println("✅ [EmailUtility] Gửi email THÀNH CÔNG tới: " + toEmail);
            System.out.println("==================================================");
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ [EmailUtility] LỖI MessagingException:");
            System.err.println("   Class:   " + e.getClass().getName());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==================================================");
            return false;

        } catch (Exception e) {
            System.err.println("❌ [EmailUtility] LỖI Exception:");
            System.err.println("   Class:   " + e.getClass().getName());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==================================================");
            return false;
        }
    }
}