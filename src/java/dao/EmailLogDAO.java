package dao;

import dto.EmailLogDTO;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmailLogDAO {

    /** Ghi log gửi thành công */
    public boolean insertSuccess(int customerId, String recipientEmail, String subject) {
        return insert(customerId, recipientEmail, subject, "Success", null);
    }

    /** Ghi log gửi thất bại */
    public boolean insertFailed(int customerId, String recipientEmail, String subject, String errorMessage) {
        return insert(customerId, recipientEmail, subject, "Failed", errorMessage);
    }

    /** Hàm insert dùng chung */
    private boolean insert(int customerId, String recipientEmail, String subject, String status, String errorMessage) {
        String sql = "INSERT INTO email_logs (customer_id, recipient_email, subject, status, error_message) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (customerId > 0) {
                ps.setInt(1, customerId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setNString(2, recipientEmail);
            ps.setNString(3, subject);
            ps.setNString(4, status);
            ps.setNString(5, errorMessage);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Lấy tất cả log (kèm tên khách hàng) */
    public List<EmailLogDTO> getAll() {
        List<EmailLogDTO> list = new ArrayList<>();
        String sql = "SELECT e.*, c.full_name AS customer_name "
                   + "FROM email_logs e "
                   + "LEFT JOIN customers c ON e.customer_id = c.id "
                   + "ORDER BY e.sent_at DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                EmailLogDTO log = mapResultSet(rs);
                log.setCustomerName(rs.getNString("customer_name"));
                list.add(log);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Lấy log theo customer_id */
    public List<EmailLogDTO> getByCustomerId(int customerId) {
        List<EmailLogDTO> list = new ArrayList<>();
        String sql = "SELECT e.*, c.full_name AS customer_name "
                   + "FROM email_logs e "
                   + "LEFT JOIN customers c ON e.customer_id = c.id "
                   + "WHERE e.customer_id = ? "
                   + "ORDER BY e.sent_at DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmailLogDTO log = mapResultSet(rs);
                    log.setCustomerName(rs.getNString("customer_name"));
                    list.add(log);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Map ResultSet -> DTO */
    private EmailLogDTO mapResultSet(ResultSet rs) throws SQLException {
        EmailLogDTO log = new EmailLogDTO();
        log.setId(rs.getInt("id"));

        int customerId = rs.getInt("customer_id");
        log.setCustomerId(rs.wasNull() ? null : customerId);

        log.setRecipientEmail(rs.getNString("recipient_email"));
        log.setSubject(rs.getNString("subject"));
        log.setStatus(rs.getNString("status"));
        log.setErrorMessage(rs.getNString("error_message"));
        log.setSentAt(rs.getTimestamp("sent_at"));
        return log;
    }

    // ============================================================
    // CÁC METHOD BỔ SUNG CHO TRANG ADMIN
    // ============================================================

    /** Tìm kiếm + lọc + phân trang log email */
    public List<EmailLogDTO> search(String keyword, String status,
                                     String dateFrom, String dateTo,
                                     int page, int pageSize) {
        List<EmailLogDTO> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT e.*, c.full_name AS customer_name "
          + "FROM email_logs e "
          + "LEFT JOIN customers c ON e.customer_id = c.id "
          + "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (c.full_name LIKE ? OR e.recipient_email LIKE ? OR e.subject LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND e.status = ? ");
            params.add(status);
        }

        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            sql.append(" AND CAST(e.sent_at AS DATE) >= ? ");
            params.add(java.sql.Date.valueOf(dateFrom));
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            sql.append(" AND CAST(e.sent_at AS DATE) <= ? ");
            params.add(java.sql.Date.valueOf(dateTo));
        }

        sql.append(" ORDER BY e.sent_at DESC ");
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY ");
        params.add((page - 1) * pageSize);
        params.add(pageSize);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmailLogDTO log = mapResultSet(rs);
                    log.setCustomerName(rs.getNString("customer_name"));
                    list.add(log);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Đếm tổng số log sau khi lọc */
    public int countSearch(String keyword, String status, String dateFrom, String dateTo) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM email_logs e "
          + "LEFT JOIN customers c ON e.customer_id = c.id "
          + "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (c.full_name LIKE ? OR e.recipient_email LIKE ? OR e.subject LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND e.status = ? ");
            params.add(status);
        }
        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            sql.append(" AND CAST(e.sent_at AS DATE) >= ? ");
            params.add(java.sql.Date.valueOf(dateFrom));
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            sql.append(" AND CAST(e.sent_at AS DATE) <= ? ");
            params.add(java.sql.Date.valueOf(dateTo));
        }

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Đếm số log theo trạng thái */
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM email_logs WHERE status = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Đếm tổng số log */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM email_logs";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Xóa 1 log */
    public boolean delete(int id) {
        String sql = "DELETE FROM email_logs WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /** Xóa toàn bộ log */
    public int deleteAll() {
        String sql = "DELETE FROM email_logs";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); return 0; }
    }

    /** Lấy log theo ID */
    public EmailLogDTO getById(int id) {
        String sql = "SELECT e.*, c.full_name AS customer_name "
                   + "FROM email_logs e "
                   + "LEFT JOIN customers c ON e.customer_id = c.id "
                   + "WHERE e.id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EmailLogDTO log = mapResultSet(rs);
                    log.setCustomerName(rs.getNString("customer_name"));
                    return log;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}