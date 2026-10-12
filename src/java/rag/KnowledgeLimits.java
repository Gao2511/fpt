package rag;

/** Application limits may be lowered by the operator; ceilings match migration 008/Tomcat. */
public final class KnowledgeLimits {
    private KnowledgeLimits() {}
    public static int text() { return value("KB_TEXT_MAX_CHARS",100000,100000); }
    public static int extracted() { return value("KB_EXTRACTED_MAX_CHARS",500000,500000); }
    public static int upload() { return value("KB_UPLOAD_MAX_BYTES",10485760,10485760); }
    public static int value(String name,int fallback,int ceiling) {
        String raw=System.getenv(name);
        if(raw==null || raw.trim().isEmpty())return fallback;
        try { int n=Integer.parseInt(raw);if(n>0 && n<=ceiling)return n; } catch(Exception ignored) {}
        throw new IllegalStateException("Cấu hình giới hạn không hợp lệ: "+name);
    }
}
