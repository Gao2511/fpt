package ai.dto;

import java.io.Serializable;

/**
 * Tin nhắn trò chuyện theo định dạng nội bộ thống nhất
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String role; // "system", "user", "assistant"
    private String content;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
