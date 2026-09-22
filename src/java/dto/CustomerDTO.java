package dto;

import java.util.Date;

/**
 * DTO cho bảng customers - đơn đăng ký tư vấn
 * 
 * ⭐ packageInterest: Gói khách quan tâm (khi đăng ký từ trang chi tiết gói cước)
 */
public class CustomerDTO {
    private int id;
    private String fullName;
    private String phone;
    private String address;
    private String email;
    private String note;
    private Integer consultantId;
    private String status;              // "Mới", "Đã liên hệ", "Đã ký HĐ", "Hủy"
    private Date createdAt;
    private Date updatedAt;
    private String consultantName;

    // ⭐ MỚI: Gói quan tâm
    private String packageInterest;

    public CustomerDTO() {}

    // ===== GETTERS & SETTERS =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getConsultantId() { return consultantId; }
    public void setConsultantId(Integer consultantId) { this.consultantId = consultantId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getConsultantName() { return consultantName; }
    public void setConsultantName(String consultantName) { this.consultantName = consultantName; }

    // ⭐ MỚI: Gói quan tâm
    public String getPackageInterest() { return packageInterest; }
    public void setPackageInterest(String packageInterest) { this.packageInterest = packageInterest; }

    // ===== HELPER METHODS =====

    /**
     * Helper: kiểm tra có gói quan tâm không
     */
    public boolean isHasPackageInterest() {
        return packageInterest != null && !packageInterest.trim().isEmpty();
    }

    /**
     * Helper: hiển thị gói quan tâm hoặc "(Chưa chọn gói)"
     */
    public String getPackageInterestDisplay() {
        if (isHasPackageInterest()) {
            return packageInterest;
        }
        return "(Chưa chọn gói)";
    }

    @Override
    public String toString() {
        return "CustomerDTO{" 
             + "id=" + id 
             + ", fullName=" + fullName 
             + ", email=" + email 
             + ", status=" + status 
             + ", packageInterest=" + packageInterest 
             + '}';
    }
}