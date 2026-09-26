package dao;

import dto.CustomerDTO;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<CustomerDTO> getAll() {
        List<CustomerDTO> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name AS consultant_name "
                   + "FROM customers c "
                   + "LEFT JOIN users u ON c.user_id = u.id "
                   + "ORDER BY c.created_at DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CustomerDTO c = mapResultSet(rs);
                c.setConsultantName(rs.getString("consultant_name"));
                list.add(c);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public CustomerDTO getById(int id) {
        String sql = "SELECT c.*, u.full_name AS consultant_name "
                   + "FROM customers c "
                   + "LEFT JOIN users u ON c.user_id = u.id "
                   + "WHERE c.id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CustomerDTO c = mapResultSet(rs);
                    c.setConsultantName(rs.getString("consultant_name"));
                    return c;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public int insert(CustomerDTO c) {
        String sql = "INSERT INTO customers "
                   + "(full_name, phone, address, email, note, user_id, status, package_interest) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getFullName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getAddress());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getNote());
            if (c.getConsultantId() != null) {
                ps.setInt(6, c.getConsultantId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, c.getStatus() != null ? c.getStatus() : "Mới");

            if (c.getPackageInterest() == null || c.getPackageInterest().trim().isEmpty()) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, c.getPackageInterest());
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public boolean update(CustomerDTO c) {
        String sql = "UPDATE customers SET full_name = ?, phone = ?, address = ?, "
                   + "email = ?, note = ?, status = ?, package_interest = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getFullName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getAddress());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getNote());
            ps.setString(6, c.getStatus());

            if (c.getPackageInterest() == null || c.getPackageInterest().trim().isEmpty()) {
                ps.setNull(7, Types.VARCHAR);
            } else {
                ps.setString(7, c.getPackageInterest());
            }

            ps.setInt(8, c.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean updateStatus(int id, String newStatus) {
        String sql = "UPDATE customers SET status = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private CustomerDTO mapResultSet(ResultSet rs) throws SQLException {
        CustomerDTO c = new CustomerDTO();
        c.setId(rs.getInt("id"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setEmail(rs.getString("email"));
        c.setNote(rs.getString("note"));

        int userId = rs.getInt("user_id");
        c.setConsultantId(rs.wasNull() ? null : userId);

        c.setStatus(rs.getString("status"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        try { c.setUpdatedAt(rs.getTimestamp("updated_at")); } catch (SQLException ignored) {}
        try { c.setPackageInterest(rs.getString("package_interest")); } catch (SQLException ignored) {}
        return c;
    }

    public List<CustomerDTO> search(String keyword, String status,
                                     String dateFrom, String dateTo,
                                     int page, int pageSize) {
        List<CustomerDTO> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT c.*, u.full_name AS consultant_name "
          + "FROM customers c "
          + "LEFT JOIN users u ON c.user_id = u.id "
          + "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (c.full_name LIKE ? OR c.phone LIKE ? OR c.email LIKE ? "
                     + "OR c.package_interest LIKE ? OR CAST(c.id AS VARCHAR(50)) LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw); params.add(kw); params.add(kw);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND c.status = ? ");
            params.add(status);
        }
        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            sql.append(" AND CAST(c.created_at AS DATE) >= ? ");
            params.add(java.sql.Date.valueOf(dateFrom));
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            sql.append(" AND CAST(c.created_at AS DATE) <= ? ");
            params.add(java.sql.Date.valueOf(dateTo));
        }

        sql.append(" ORDER BY c.created_at DESC LIMIT ? OFFSET ? ");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CustomerDTO c = mapResultSet(rs);
                    c.setConsultantName(rs.getString("consultant_name"));
                    list.add(c);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public int countSearch(String keyword, String status, String dateFrom, String dateTo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM customers c WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (c.full_name LIKE ? OR c.phone LIKE ? OR c.email LIKE ? "
                     + "OR c.package_interest LIKE ? OR CAST(c.id AS VARCHAR(50)) LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw); params.add(kw); params.add(kw);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND c.status = ? ");
            params.add(status);
        }
        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            sql.append(" AND CAST(c.created_at AS DATE) >= ? ");
            params.add(java.sql.Date.valueOf(dateFrom));
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            sql.append(" AND CAST(c.created_at AS DATE) <= ? ");
            params.add(java.sql.Date.valueOf(dateTo));
        }

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM customers WHERE status = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM customers";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public List<String> getAllEmails() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT email FROM customers "
                   + "WHERE email IS NOT NULL AND email <> '' ORDER BY email";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<CustomerDTO> getByPhone(String phone) {
        List<CustomerDTO> list = new ArrayList<>();
        String normalizedPhone = phone.replaceAll("[\\s.\\-]", "");
        String sql = "SELECT c.*, u.full_name AS consultant_name "
                   + "FROM customers c "
                   + "LEFT JOIN users u ON c.user_id = u.id "
                   + "WHERE REPLACE(REPLACE(REPLACE(c.phone, ' ', ''), '.', ''), '-', '') = ? "
                   + "ORDER BY c.created_at DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedPhone);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CustomerDTO c = mapResultSet(rs);
                    c.setConsultantName(rs.getString("consultant_name"));
                    list.add(c);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /** Đổi từ getByConsultantId → getByUserId */
    public List<CustomerDTO> getByUserId(int userId) {
        List<CustomerDTO> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name AS consultant_name "
                   + "FROM customers c "
                   + "LEFT JOIN users u ON c.user_id = u.id "
                   + "WHERE c.user_id = ? "
                   + "ORDER BY c.created_at DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CustomerDTO c = mapResultSet(rs);
                    c.setConsultantName(rs.getString("consultant_name"));
                    list.add(c);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}