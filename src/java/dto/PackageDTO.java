package dto;

import java.util.Date;

public class PackageDTO {
    private int id;
    private String packageCode;
    private String name;
    private long price;
    private int speedMbps;
    private String description;
    private String longDescription;
    private boolean hot;
    private String badgeType;
    private Date createdAt;

    // ===== GETTERS & SETTERS =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public int getSpeedMbps() { return speedMbps; }
    public void setSpeedMbps(int speedMbps) { this.speedMbps = speedMbps; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isHot() { return hot; }
    public void setHot(boolean hot) { this.hot = hot; }

    public String getBadgeType() { return badgeType; }
    public void setBadgeType(String badgeType) { this.badgeType = badgeType; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getLongDescription() { return longDescription; }
    public void setLongDescription(String longDescription) { 
        this.longDescription = longDescription; 
    }
    
    /**
     * Helper: tách long_description thành List<String>
     * Mỗi dòng = 1 phần tử → JSP forEach hiển thị gạch ✓ xanh
     */
    public java.util.List<String> getLongDescriptionLines() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (longDescription != null && !longDescription.trim().isEmpty()) {
            for (String line : longDescription.split("\\r?\\n")) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }
        return lines;
    }

    // ===== HELPER METHODS =====
    
    /**
     * Helper: kiểm tra gói có badge HOT không
     */
    public boolean isBadgeHot() {
        return "hot".equals(badgeType);
    }

    /**
     * Helper: kiểm tra gói có badge NỔI BẬT không
     */
    public boolean isBadgeFeatured() {
        return "featured".equals(badgeType);
    }

    @Override
    public String toString() {
        return "PackageDTO{" 
             + "id=" + id 
             + ", packageCode=" + packageCode 
             + ", name=" + name 
             + ", price=" + price 
             + ", badgeType=" + badgeType 
             + '}';
    }
}