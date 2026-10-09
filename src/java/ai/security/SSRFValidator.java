package ai.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;

/**
 * Xác thực Base URL chống tấn công SSRF (Server-Side Request Forgery)
 */
public class SSRFValidator {

    /**
     * Kiểm tra tính hợp lệ của Base URL
     * @param urlStr URL cần kiểm tra
     * @param allowLocal Cờ cho phép chạy local/LAN (dành cho Ollama hoặc LM Studio)
     * @throws IllegalArgumentException Nếu URL không an toàn
     */
    public static void validateBaseUrl(String urlStr, boolean allowLocal) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL không được để trống.");
        }

        String trimmed = urlStr.trim();
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Base URL sai định dạng cú pháp: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Base URL chỉ chấp nhận giao thức http:// hoặc https://.");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được Host trong Base URL.");
        }

        // Nếu admin chủ đích cho phép local (Ollama / LM Studio)
        if (allowLocal) {
            return;
        }

        // Chặn các hostname localhost thông dụng
        String hostLower = host.toLowerCase();
        if ("localhost".equals(hostLower) || hostLower.endsWith(".local") || hostLower.endsWith(".internal")) {
            throw new IllegalArgumentException("Không cho phép Base URL trỏ vào localhost hoặc mạng nội bộ (SSRF Protection). Bật tùy chọn 'Cho phép Local (Ollama)' nếu bạn muốn dùng server nội bộ.");
        }

        // Phân giải DNS và kiểm tra IP
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                // Kiểm tra loopback (127.0.0.1, ::1)
                if (addr.isLoopbackAddress()) {
                    throw new IllegalArgumentException("Base URL trỏ vào địa chỉ Loopback (127.0.0.1/::1) bị chặn vì lý do an toàn.");
                }

                // Kiểm tra Site Local (10.x, 172.16-31.x, 192.168.x)
                if (addr.isSiteLocalAddress()) {
                    throw new IllegalArgumentException("Base URL trỏ vào dải IP nội bộ mạng LAN (" + addr.getHostAddress() + ") bị chặn.");
                }

                // Kiểm tra Link Local & Any Local
                if (addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                    throw new IllegalArgumentException("Base URL trỏ vào dải địa chỉ Link Local hoặc 0.0.0.0 bị chặn.");
                }

                // Kiểm tra AWS / GCP / Azure Cloud Metadata IP (169.254.169.254)
                String ip = addr.getHostAddress();
                if ("169.254.169.254".equals(ip) || ip.startsWith("169.254.")) {
                    throw new IllegalArgumentException("Base URL trỏ vào dải Cloud Metadata (169.254.x.x) bị nghiêm cấm.");
                }
            }
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể phân giải tên miền host '" + host + "': " + e.getMessage());
        }
    }
}
