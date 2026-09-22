package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtils {

    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "1433";
    private static final String DB_NAME = "fpt_sale_db";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "12345";

    public static Connection getConnection()
            throws ClassNotFoundException, SQLException {

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        // ⚠️ SQLEXPRESS → thêm \\SQLEXPRESS vào URL
        String url = "jdbc:sqlserver://" + DB_HOST + "\\SQLEXPRESS:" + DB_PORT
                   + ";databaseName=" + DB_NAME
                   + ";encrypt=true;trustServerCertificate=true";

        System.out.println("🔌 Connecting: " + url);

        return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
    }
}