package dto;

import java.util.Date;

/**
 * DTO cho bảng email_logs
 * Đại diện cho một lần gửi email thông báo
 */
public class EmailLogDTO {
    private int id;
    private Integer customerId;
    private String recipientEmail;
    private String subject;
    private String status;          // "Success" hoặc "Failed"
    private String errorMessage;
    private Date sentAt;

    // Thuộc tính phụ để hiển thị tên khách hàng khi JOIN
    private String customerName;

    // --- Constructors ---
    public EmailLogDTO() {
    }

    public EmailLogDTO(int id, Integer customerId, String recipientEmail, String subject, String status, String errorMessage, Date sentAt) {
        this.id = id;
        this.customerId = customerId;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.status = status;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
    }

    // --- Getters & Setters ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    // ===== HELPER METHODS =====

    /**
     * Helper: kiểm tra gửi thành công không
     */
    public boolean isSuccess() {
        return "Success".equals(status);
    }

    /**
     * Helper: kiểm tra gửi thất bại không
     */
    public boolean isFailed() {
        return "Failed".equals(status);
    }

    @Override
    public String toString() {
        return "EmailLogDTO{" 
             + "id=" + id 
             + ", recipientEmail=" + recipientEmail 
             + ", status=" + status 
             + '}';
    }
}