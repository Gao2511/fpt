package ai.dto;

import java.io.Serializable;

/**
 * Thông tin mô hình AI lấy từ API của các hãng
 */
public class ModelInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private String contextWindow;
    private boolean isChatModel = true;

    public ModelInfo() {
    }

    public ModelInfo(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContextWindow() {
        return contextWindow;
    }

    public void setContextWindow(String contextWindow) {
        this.contextWindow = contextWindow;
    }

    public boolean isChatModel() {
        return isChatModel;
    }

    public void setChatModel(boolean chatModel) {
        isChatModel = chatModel;
    }
}
