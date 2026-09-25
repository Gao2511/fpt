package util;

import java.security.MessageDigest;


public class PasswordUtil {

    /**
     * Hash mật khẩu bằng SHA-256
     * Trả về chuỗi hex 64 ký tự
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hash mật khẩu", e);
        }
    }

    /**
     * So sánh mật khẩu nhập vào với mật khẩu đã hash
     */
    public static boolean checkPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return hashPassword(rawPassword).equals(hashedPassword);
    }
}