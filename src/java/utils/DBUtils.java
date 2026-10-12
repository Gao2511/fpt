package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Environment-only JDBC configuration. Never log credentials or connection URLs. */
public class DBUtils {
    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String host = required("DB_HOST"), port = required("DB_PORT"), name = required("DB_NAME");
        if ((!host.matches("[A-Za-z0-9.-]+") && !"::1".equals(host)) || !port.matches("[0-9]{1,5}") ||
            Integer.parseInt(port)<1 || Integer.parseInt(port)>65535 || !name.matches("[A-Za-z0-9_-]+"))
            throw new SQLException("Invalid database configuration");
        Properties p = new Properties();
        p.setProperty("user", required("DB_USER"));
        p.setProperty("password", required("DB_PASSWORD"));
        String requestedSsl = System.getenv("DB_SSLMODE");
        try {
            validateOnlineDatabase(System.getenv("RAG_ENVIRONMENT"), host, requestedSsl);
        } catch (IllegalArgumentException ex) {
            throw new SQLException(ex.getMessage(), ex);
        }
        p.setProperty("sslmode", sslMode(host, requestedSsl));
        p.setProperty("connectTimeout", "5");
        p.setProperty("socketTimeout", "30");
        String jdbcHost="::1".equals(host)?"[::1]":host;
        return DriverManager.getConnection("jdbc:postgresql://" + jdbcHost + ":" + port + "/" + name, p);
    }
    private static String required(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new SQLException("Missing environment variable: " + name);
        return value;
    }
    public static boolean isLoopbackHost(String host) {
        if (host == null) return false;
        String h = host.trim().toLowerCase();
        return "127.0.0.1".equals(h) || "localhost".equals(h) || "::1".equals(h) || "[::1]".equals(h)
                || h.matches("127(?:\\.[0-9]{1,3}){3}");
    }
    public static void validateOnlineDatabase(String ragEnv, String host, String requestedSslMode) {
        if (ragEnv == null) return;
        String env = ragEnv.trim().toUpperCase();
        if ("STAGING".equals(env) || "PRODUCTION".equals(env)) {
            if (isLoopbackHost(host)) {
                throw new IllegalArgumentException(env + " environment must not use a loopback database host: " + host);
            }
            if ("disable".equalsIgnoreCase(requestedSslMode)) {
                throw new IllegalArgumentException(env + " environment requires TLS (DB_SSLMODE=require or verify-full)");
            }
        }
    }
    public static String sslMode(String host, String requested) {
        if (requested == null || requested.isEmpty() || "require".equals(requested) || "verify-full".equals(requested))
            return requested == null || requested.isEmpty() ? "require" : requested;
        if ("disable".equals(requested) && isLoopbackHost(host)) return "disable";
        throw new IllegalArgumentException("DB_SSLMODE=disable is permitted only for a loopback database");
    }
}
