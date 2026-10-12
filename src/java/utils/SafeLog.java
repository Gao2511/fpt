package utils;
/** Operational failures without exception messages, SQL payloads, account data or secrets. */
public final class SafeLog {
    private SafeLog() {}
    public static void error(String component,Exception e) {
        String state=e instanceof java.sql.SQLException?((java.sql.SQLException)e).getSQLState():null;
        System.err.println("["+component+"] failure="+e.getClass().getSimpleName()+
            (state!=null && state.matches("[A-Z0-9]{5}")?" sqlState="+state:""));
    }
}
