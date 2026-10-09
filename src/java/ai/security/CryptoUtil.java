package ai.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Tiện ích mã hóa & giải mã bí mật bằng thuật toán AES-256-GCM
 * và che mờ (masking) API Key trên giao diện người dùng.
 */
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes (chuẩn khuyến nghị cho GCM)
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Khóa mã hóa 256-bit được dẫn xuất từ biến môi trường
    private static final SecretKey SECRET_KEY = initSecretKey();

    private static SecretKey initSecretKey() {
        try {
            String envKey = System.getenv("AI_ENCRYPTION_KEY");
            if (envKey == null || envKey.trim().isEmpty()) {
                envKey = System.getProperty("AI_ENCRYPTION_KEY");
            }

            // Fallback an toàn có salt ứng dụng nếu môi trường chưa cấu hình biến
            String rawSecret = (envKey != null && !envKey.trim().isEmpty())
                    ? envKey.trim()
                    : "FPT_TELECOM_SECURE_VAULT_KEY_v2026_@_GIA_LAI_TELECOM";

            // Băm SHA-256 để luôn tạo đúng 32 bytes (256 bits)
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo khóa mã hóa AES: " + e.getMessage(), e);
        }
    }

    /**
     * Mã hóa chuỗi văn bản thuần thành chuỗi Base64 chứa [IV + Ciphertext + Tag]
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, spec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Ghép IV và Ciphertext: [12 bytes IV] + [Ciphertext + Tag]
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return "enc:gcm:" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            System.err.println("⚠️ [CryptoUtil] Lỗi mã hóa: " + e.getMessage());
            return plainText; // Fallback an toàn
        }
    }

    /**
     * Giải mã chuỗi Base64 đã mã hóa về văn bản thuần
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }
        // Nếu không có tiền tố enc:gcm: thì là chuỗi cũ chưa mã hóa (backward compatibility)
        if (!encryptedText.startsWith("enc:gcm:")) {
            return encryptedText;
        }

        try {
            String base64Payload = encryptedText.substring("enc:gcm:".length());
            byte[] combined = Base64.getDecoder().decode(base64Payload);

            if (combined.length <= IV_LENGTH) {
                return "";
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("⚠️ [CryptoUtil] Lỗi giải mã: " + e.getMessage());
            return "";
        }
    }

    /**
     * Che mờ API Key khi hiển thị ra màn hình Admin (VD: sk-••••••••••••a1b2)
     */
    public static String maskApiKey(String rawKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            return "";
        }
        String trimmed = rawKey.trim();
        int len = trimmed.length();
        if (len <= 8) {
            return "••••••••";
        }
        if (len <= 14) {
            return trimmed.substring(0, 2) + "••••••••" + trimmed.substring(len - 2);
        }
        // Tiêu chuẩn: 4 ký tự đầu + chấm tròn + 4 ký tự cuối
        return trimmed.substring(0, 4) + "••••••••••••" + trimmed.substring(len - 4);
    }

    /**
     * Kiểm tra xem chuỗi có phải là chuỗi đã bị che mờ không
     */
    public static boolean isMasked(String key) {
        if (key == null) return false;
        return key.contains("••••") || key.contains("****");
    }

    /**
     * Xóa sạch thông tin nhạy cảm trong câu log hoặc URL
     */
    public static String sanitizeForLog(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)(key|api_key|token|password|secret)=([^&\\s]+)", "$1=***MASKED***")
                    .replaceAll("(?i)(Bearer\\s+)([A-Za-z0-9_\\-\\.]+)", "$1***MASKED***")
                    .replaceAll("(AIzaSy)[A-Za-z0-9_\\-]{25,}", "$1***MASKED***")
                    .replaceAll("(sk-[A-Za-z0-9_\\-]{15,})", "sk-***MASKED***");
    }
}
