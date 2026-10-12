package utils;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Exposes build and deployment metadata embedded at compile/package time
 * in {@code /build-info.properties} along with runtime Render environment indicators.
 */
public final class BuildInfo {
    private static final String DEFAULT_VERSION = "5.3.0-online-rag";
    private static final String DEFAULT_PIPELINE = "portable-ant-jdk17-release8";
    private static final Properties PROPS = loadProperties();

    private BuildInfo() {
    }

    private static Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (Exception ignored) {
        }
        return p;
    }

    public static String getVersion() {
        return nonBlank(PROPS.getProperty("build.version"), DEFAULT_VERSION);
    }

    public static String getFullCommitSha() {
        return getFullCommitSha(System.getenv());
    }

    public static String getFullCommitSha(Map<String, String> env) {
        if (env != null) {
            String renderSha = env.get("RENDER_GIT_COMMIT");
            if (isValidSha(renderSha)) {
                return renderSha.trim();
            }
            String gitSha = env.get("GIT_COMMIT");
            if (isValidSha(gitSha)) {
                return gitSha.trim();
            }
        }
        String propSha = PROPS.getProperty("build.commit");
        if (isValidSha(propSha)) {
            return propSha.trim();
        }
        return "source-build";
    }

    public static String getShortCommitSha() {
        return getShortCommitSha(System.getenv());
    }

    public static String getShortCommitSha(Map<String, String> env) {
        String full = getFullCommitSha(env);
        if (full.length() > 12 && full.matches("[0-9a-fA-F]+")) {
            return full.substring(0, 8);
        }
        return full;
    }

    public static String getBuildTimestamp() {
        return nonBlank(PROPS.getProperty("build.timestamp"), "2026-01-01T00:00:00Z");
    }

    public static String getPipeline() {
        return nonBlank(PROPS.getProperty("build.pipeline"), DEFAULT_PIPELINE);
    }

    public static String getEnvironment() {
        return getEnvironment(System.getenv());
    }

    public static String getEnvironment(Map<String, String> env) {
        String raw = env != null ? env.get("RAG_ENVIRONMENT") : null;
        if (raw == null || raw.trim().isEmpty()) {
            String sysProp = System.getProperty("fpt.rag.environment");
            raw = (sysProp != null && !sysProp.trim().isEmpty()) ? sysProp : "LOCAL";
        }
        return raw.trim().toUpperCase();
    }

    public static String getSummary() {
        return getSummary(System.getenv());
    }

    public static String getSummary(Map<String, String> env) {
        return "v" + getVersion() + " (" + getShortCommitSha(env) + " | " + getBuildTimestamp() + " | " + getEnvironment(env) + ")";
    }

    private static boolean isValidSha(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && !"${build.commit}".equals(trimmed) && !"unknown".equalsIgnoreCase(trimmed);
    }

    private static String nonBlank(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("${")) {
            return fallback;
        }
        return trimmed;
    }
}
