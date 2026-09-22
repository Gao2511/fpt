package dto;

import java.util.Date;

public class ApiKeyHistoryDTO {
    private int id;
    private String apiKey;
    private String model;
    private Integer changedBy;
    private String changedByName;
    private String action;      // UPDATE / DELETE
    private String note;
    private Date createdAt;

    public ApiKeyHistoryDTO() {}

    // ===== GETTERS & SETTERS =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getChangedBy() { return changedBy; }
    public void setChangedBy(Integer changedBy) { this.changedBy = changedBy; }

    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    // ===== HELPER: Hiển thị key rút gọn (bảo mật) =====
    public String getApiKeyMasked() {
        if (apiKey == null || apiKey.length() < 15) return apiKey;
        return apiKey.substring(0, 10) + "..." + apiKey.substring(apiKey.length() - 5);
    }

    // ===== HELPER: Icon theo action =====
    public String getActionIcon() {
        return "UPDATE".equals(action) ? "✏️" : "🗑️";
    }

    // ===== HELPER: Label tiếng Việt =====
    public String getActionLabel() {
        if ("UPDATE".equals(action)) return "Cập nhật";
        if ("DELETE".equals(action)) return "Xóa";
        return action;
    }
}