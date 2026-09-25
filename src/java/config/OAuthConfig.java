package config;

import java.io.InputStream;
import java.util.Properties;

public class OAuthConfig {
    private static final Properties props = new Properties();

    static {
        // ⭐ Ưu tiên đọc từ Environment Variables (Render/Production)
        String envClientId = System.getenv("GOOGLE_CLIENT_ID");
        String envClientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        String envRedirectUri = System.getenv("GOOGLE_REDIRECT_URI");

        if (envClientId != null && !envClientId.isEmpty()) {
            props.setProperty("google.client.id", envClientId);
            props.setProperty("google.client.secret", envClientSecret);
            props.setProperty("google.redirect.uri", envRedirectUri);
            System.out.println("✅ Loaded OAuth config from ENV");
        } else {
            // Fallback: đọc file oauth.properties (local dev)
            try (InputStream is = OAuthConfig.class.getClassLoader()
                    .getResourceAsStream("oauth.properties")) {
                if (is != null) {
                    props.load(is);
                    System.out.println("✅ Loaded oauth.properties");
                } else {
                    System.err.println("⚠️ oauth.properties NOT FOUND!");
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static String getGoogleClientId() {
        return props.getProperty("google.client.id", "").trim();
    }

    public static String getGoogleClientSecret() {
        return props.getProperty("google.client.secret", "").trim();
    }

    public static String getGoogleRedirectUri() {
        return props.getProperty("google.redirect.uri", "").trim();
    }
}