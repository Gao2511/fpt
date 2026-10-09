package ai.session;

import ai.dto.ChatMessage;
import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lưu trữ trạng thái ngữ cảnh hội thoại và sổ tay thông tin khách hàng theo từng Session.
 */
public class ChatSessionData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final List<ChatMessage> history = new ArrayList<>();
    private JsonObject state = new JsonObject();
    private boolean leadCreated = false;
    private long lastActivityTime = System.currentTimeMillis();
    private final ai.consultation.CustomerRequirements requirements = new ai.consultation.CustomerRequirements();
    public final java.util.concurrent.locks.ReentrantLock turnLock = new java.util.concurrent.locks.ReentrantLock();
    public ai.consultation.CustomerRequirements getRequirements() { return requirements; }

    /** Commit one validated turn and trim whole pairs; typed requirements survive. */
    public synchronized void commitTurn(String user, String assistant) {
        history.add(ChatMessage.user(user));
        history.add(ChatMessage.assistant(assistant));
        int chars = 0;
        for (ChatMessage m : history) chars += m.getContent().length();
        while (history.size() > 20 || (chars > 24000 && history.size() > 2)) {
            chars -= history.remove(0).getContent().length();
            chars -= history.remove(0).getContent().length();
        }
        touch();
    }

    public ChatSessionData(String sessionId) {
        this.sessionId = sessionId;
        // Khởi tạo state rỗng ban đầu
        this.state.addProperty("ten", "");
        this.state.addProperty("sdt", "");
        this.state.addProperty("dia_chi", "");
        this.state.addProperty("goi_de_xuat", "");
    }

    public String getSessionId() {
        return sessionId;
    }

    public synchronized List<ChatMessage> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void addMessage(ChatMessage message) {
        if (message != null) {
            history.add(message);
            lastActivityTime = System.currentTimeMillis();
        }
    }

    public synchronized void clearHistory() {
        history.clear();
        requirements.clear();
        state = new JsonObject();
        state.addProperty("ten", "");
        state.addProperty("sdt", "");
        state.addProperty("dia_chi", "");
        state.addProperty("goi_de_xuat", "");
        leadCreated = false;
        lastActivityTime = System.currentTimeMillis();
    }

    public synchronized JsonObject getState() {
        return state.deepCopy();
    }

    /**
     * Hợp nhất state mới từ model vào state hiện có.
     * Quy tắc an toàn: Không ghi đè trường đã có giá trị bằng null hoặc chuỗi rỗng.
     */
    public synchronized void mergeState(JsonObject newState) {
        if (newState == null) return;

        for (String key : newState.keySet()) {
            if (newState.get(key) != null && !newState.get(key).isJsonNull()) {
                String newVal = newState.get(key).getAsString().trim();
                if (!newVal.isEmpty()) {
                    state.addProperty(key, newVal);
                }
            }
        }
        lastActivityTime = System.currentTimeMillis();
    }

    public synchronized boolean isLeadCreated() {
        return leadCreated;
    }

    public synchronized void setLeadCreated(boolean leadCreated) {
        this.leadCreated = leadCreated;
    }

    public synchronized long getLastActivityTime() {
        return lastActivityTime;
    }

    public synchronized void touch() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public synchronized void setHistory(List<ChatMessage> newHistory) {
        history.clear();
        if (newHistory != null) {
            history.addAll(newHistory);
        }
        lastActivityTime = System.currentTimeMillis();
    }
}
