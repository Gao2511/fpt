package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtils {

    // Đọc từ Environment Variables
    // - Local (NetBeans): ENV không có → dùng fallback localhost\SQLEXPRESS
    // - Render: ENV có giá trị → dùng Azure
    private static final String DB_HOST = getEnv("DB_HOST", "localhost");
    private static final String DB_PORT = getEnv("DB_PORT", "1433");
    private static final String DB_NAME = getEnv("DB_NAME", "fpt_sale_db");
    private static final String DB_USER = getEnv("DB_USER", "sa");
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "12345");

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static Connection getConnection()
            throws ClassNotFoundException, SQLException {

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        String url;
        if (DB_HOST.contains(".database.windows.net")) {
            // Azure SQL
            url = "jdbc:sqlserver://" + DB_HOST + ":" + DB_PORT
                + ";databaseName=" + DB_NAME
                + ";encrypt=true;trustServerCertificate=false;"
                + "hostNameInCertificate=*.database.windows.net;loginTimeout=30";
        } else if (DB_HOST.contains("\\")) {
            // Local SQLEXPRESS
            url = "jdbc:sqlserver://" + DB_HOST + ":" + DB_PORT
                + ";databaseName=" + DB_NAME
                + ";encrypt=true;trustServerCertificate=true";
        } else {
            // Other SQL Server
            url = "jdbc:sqlserver://" + DB_HOST + ":" + DB_PORT
                + ";databaseName=" + DB_NAME
                + ";encrypt=true;trustServerCertificate=true";
        }

        System.out.println("🔌 Connecting: " + url);

        return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
    }
}