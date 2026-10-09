package ai.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Process-local bounded storage; servlet lifecycle removes customer data on expiry. */
public final class ChatSessionManager {
    private static final Map<String, ChatSessionData> SESSIONS = new ConcurrentHashMap<>();
    private static final long RETENTION_MS = 2 * 60 * 60 * 1000L;
    private static final int MAX_SESSIONS = 2000;
    private ChatSessionManager() {}

    public static synchronized ChatSessionData getOrCreate(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("A private session ID is required");
        cleanupExpiredSessions();
        ChatSessionData found = SESSIONS.get(id);
        if (found != null) { found.touch(); return found; }
        if (SESSIONS.size() >= MAX_SESSIONS) throw new IllegalStateException("Chat capacity reached");
        ChatSessionData created = new ChatSessionData(id); SESSIONS.put(id, created); return created;
    }
    public static void clearSession(String id) {
        ChatSessionData data = SESSIONS.get(id);
        if (data == null) return;
        data.turnLock.lock();
        try { data.clearHistory(); } finally { data.turnLock.unlock(); }
    }
    public static void removeSession(String id) { if (id != null) SESSIONS.remove(id); }
    public static void clearAll() { SESSIONS.clear(); }
    public static void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        SESSIONS.entrySet().removeIf(e -> !e.getValue().turnLock.isLocked() && now - e.getValue().getLastActivityTime() > RETENTION_MS);
    }
}
