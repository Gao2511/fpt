package cms;
import utils.DBUtils;
public final class LocalDatabaseTest {
    public static void main(String[] args) {
        check("require".equals(DBUtils.sslMode("db.example.com", null)));
        check("require".equals(DBUtils.sslMode("127.0.0.1", null)));
        check("disable".equals(DBUtils.sslMode("127.0.0.1", "disable")));
        check("disable".equals(DBUtils.sslMode("localhost", "disable")));
        check("disable".equals(DBUtils.sslMode("::1", "disable")));
        for(String host:new String[]{"db.example.com","127.0.0.1.example.com","192.168.1.2"}) {
            try { DBUtils.sslMode(host,"disable"); throw new AssertionError("Remote SSL disabled"); }
            catch(IllegalArgumentException expected) {}
        }
        try { DBUtils.sslMode("127.0.0.1","invalid"); throw new AssertionError("Invalid SSL mode"); }
        catch(IllegalArgumentException expected) {}
        System.out.println("LocalDatabaseTest: 9 checks passed");
    }
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
}
