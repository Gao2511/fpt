package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtils {

    // ⭐ Supabase PostgreSQL config
    // - Local (NetBeans): đọc từ ENV nếu có, không thì dùng giá trị mặc định
    // - Production (Render): set ENV trong dashboard Render
    private static final String DB_HOST = getEnv("DB_HOST",
            "aws-0-ap-southeast-1.pooler.supabase.com");
    private static final String DB_PORT = getEnv("DB_PORT", "5432");
    private static final String DB_NAME = getEnv("DB_NAME", "postgres");
    private static final String DB_USER = getEnv("DB_USER",
            "postgres.dllkuavomoccgnzpemwx");  // ⚠️ 2 chữ "ll"
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD",
            "Caoky@2k528");  // ⚠️ Thay password của bạn

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static Connection getConnection()
            throws ClassNotFoundException, SQLException {

        // ⭐ Load driver PostgreSQL (không phải SQL Server nữa)
        Class.forName("org.postgresql.Driver");

        // ⭐ JDBC URL cho Supabase (Session pooler)
        // sslmode=require: Supabase yêu cầu SSL
        String url = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                   + "?sslmode=require"
                   + "&connectTimeout=10"
                   + "&socketTimeout=30";

        System.out.println("🔌 Connecting to Supabase: " + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);
        return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
    }
}