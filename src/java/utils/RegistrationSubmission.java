package utils;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.*;

/** Session-bound CSRF and idempotency tokens, consumed only after persistence. */
public final class RegistrationSubmission {
    private static final String KEY = "registration_submissions";
    private RegistrationSubmission() {}
    public static final class Submission implements Serializable {
        private static final long serialVersionUID = 1L;
        final long issued = System.currentTimeMillis();
        public int customerId;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Submission> tokens(HttpSession session) {
        Map<String, Submission> map = (Map<String, Submission>) session.getAttribute(KEY);
        if (map == null) { map = new LinkedHashMap<>(); session.setAttribute(KEY, map); }
        map.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().issued > 30 * 60 * 1000L);
        return map;
    }
    public static String issue(HttpSession session) {
        synchronized (session) {
            Map<String, Submission> map = tokens(session);
            while (map.size() >= 20) map.remove(map.keySet().iterator().next());
            String token = UUID.randomUUID().toString(); map.put(token, new Submission()); return token;
        }
    }
    /** Caller holds the session lock through insert and success assignment. */
    public static Submission find(HttpSession session, String token) {
        if (token == null || token.length() > 50) return null;
        return tokens(session).get(token);
    }
    public static String normalizedPhone(String phone) {
        if (phone == null || !phone.matches("[+0-9 ().-]{8,25}")) return "";
        String normalized = phone.replaceAll("[ ().-]", "");
        if (normalized.startsWith("+84")) normalized = "0" + normalized.substring(3);
        return normalized.matches("0(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}") ? normalized : "";
    }
    public static boolean validFields(String name, String phone, String address, String email, String note, String consent) {
        return validText(name, 120) && !normalizedPhone(phone).isEmpty() && validText(address, 500) &&
            (email == null || email.isEmpty() || email.length() <= 254 && email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) &&
            (note == null || note.length() <= 2000) && "yes".equals(consent);
    }
    private static boolean validText(String value, int maximum) {
        return value != null && !value.trim().isEmpty() && value.length() <= maximum && !value.matches("(?s).*[\\p{Cntrl}].*");
    }
}
