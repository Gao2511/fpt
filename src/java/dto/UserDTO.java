package dto;

import java.util.Date;

/**
 * DTO cho bảng users - tài khoản đăng nhập
 * Role chỉ có 2 giá trị: "admin" hoặc "customer"
 */
public class UserDTO {
    private int id;
    private String username;
    private String password;
    private String role;
    private boolean isActive;
    private Date createdAt;

    // Thuộc tính phụ (JOIN với consultants)
    private String fullName;
    private Integer consultantId;

    // Avatar + Email + Phone
    private String avatarUrl;
    private String email;
    private String phone;

    public UserDTO() {}

    public UserDTO(int id, String username, String password, String role, boolean isActive, Date createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // ===== GETTERS & SETTERS =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Integer getConsultantId() { return consultantId; }
    public void setConsultantId(Integer consultantId) { this.consultantId = consultantId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // ===== HELPER METHODS =====
    public String getInitial() {
        String source = (fullName != null && !fullName.trim().isEmpty())
                        ? fullName.trim()
                        : username;
        if (source == null || source.isEmpty()) return "?";
        return source.substring(0, 1).toUpperCase();
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName;
        return username;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isCustomer() {
        return "customer".equals(role);
    }

    public String getStatusDisplay() {
        return isActive ? "Hoạt động" : "Đã khóa";
    }

    public String getStatusClass() {
        return isActive ? "active" : "locked";
    }

    @Override
    public String toString() {
        return "UserDTO{" + "id=" + id + ", username=" + username 
             + ", role=" + role + ", isActive=" + isActive + '}';
    }
}