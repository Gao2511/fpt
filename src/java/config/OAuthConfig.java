package config;

import java.io.InputStream;
import java.util.Properties;

public class OAuthConfig {
    private static final Properties props = new Properties();

    static {
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