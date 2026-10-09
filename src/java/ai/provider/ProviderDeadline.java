package ai.provider;

import ai.dto.ChatOptions;
import java.net.HttpURLConnection;
import java.util.concurrent.*;

/** Cancels a stalled connection at the shared consultation deadline. */
public final class ProviderDeadline {
    private static ScheduledThreadPoolExecutor timer;
    private ProviderDeadline() {}
    private static synchronized ScheduledThreadPoolExecutor timer() {
        if (timer == null || timer.isShutdown()) {
            timer = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "fpt-provider-deadline"); t.setDaemon(true); return t;
            });
            timer.setRemoveOnCancelPolicy(true);
        }
        return timer;
    }
    static ScheduledFuture<?> watch(HttpURLConnection connection, ChatOptions options) {
        if (options == null || options.getResponseSchema() == null) return null;
        return timer().schedule(connection::disconnect, options.remainingMillis(40000), TimeUnit.MILLISECONDS);
    }
    public static synchronized void shutdown() { if (timer != null) timer.shutdownNow(); }
}
