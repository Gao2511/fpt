package rag;
import java.sql.*;

/** Injectable JDBC boundary; no swallowed database errors or automatic migrations. */
public final class RagDatabase {
    public interface Connections { Connection open() throws Exception; }
    private final Connections connections;
    public RagDatabase() { this(utils.DBUtils::getConnection); }
    public RagDatabase(Connections connections) { this.connections=connections; }
    public Connection open() throws Exception {
        Connection c=connections.open();
        try(Statement s=c.createStatement()) {
            s.execute("SET search_path=public,extensions,pg_catalog");
            s.execute("SET statement_timeout='3000ms'");
        } catch(Exception e) { c.close();throw e; }
        return c;
    }
    public static Timestamp timestamp(java.time.Instant value) { return value==null?null:Timestamp.from(value); }
    public static java.time.Instant instant(ResultSet r,String name) throws SQLException {
        Timestamp value=r.getTimestamp(name);return value==null?null:value.toInstant();
    }
}
