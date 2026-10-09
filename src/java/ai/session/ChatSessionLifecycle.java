package ai.session;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.*;
import java.util.concurrent.*;

@WebListener
public class ChatSessionLifecycle implements ServletContextListener, HttpSessionListener, HttpSessionIdListener {
    private ScheduledExecutorService cleaner;
    public void contextInitialized(ServletContextEvent event) {
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "fpt-chat-retention"); thread.setDaemon(true); return thread;
        });
        cleaner.scheduleAtFixedRate(ChatSessionManager::cleanupExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }
    public void contextDestroyed(ServletContextEvent event) {
        if (cleaner != null) cleaner.shutdownNow();
        ai.provider.ProviderDeadline.shutdown();
        ChatSessionManager.clearAll();
    }
    public void sessionCreated(HttpSessionEvent event) {}
    public void sessionDestroyed(HttpSessionEvent event) { remove(event.getSession().getId()); }
    public void sessionIdChanged(HttpSessionEvent event, String oldId) { remove(oldId); }
    private void remove(String id) { ChatSessionManager.removeSession(id); ChatSessionManager.removeSession("playground_" + id); }
}
