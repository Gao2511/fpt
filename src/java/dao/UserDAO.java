package dao;

import dto.UserDTO;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // =========================================================
    // ĐĂNG NHẬP
    // =========================================================
    public UserDTO checkLoginByPhoneOrEmail(String input, String password) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u "
                   + "WHERE (u.username = ? OR u.phone = ? OR u.email = ?) "
                   + "  AND u.password = ? "
                   + "  AND u.is_active = 1";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String cleaned = input.trim();
            ps.setNString(1, cleaned);
            ps.setNString(2, cleaned);
            ps.setNString(3, cleaned);
            ps.setNString(4, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public UserDTO checkLogin(String username, String password) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u "
                   + "WHERE u.username = ? AND u.password = ? AND u.is_active = 1";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, username);
            ps.setNString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // =========================================================
    // LẤY USER
    // =========================================================
    public UserDTO getByUsername(String username) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u WHERE u.username = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public UserDTO getByEmail(String email) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u WHERE u.email = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public UserDTO getByPhone(String phone) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u WHERE u.phone = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public UserDTO getById(int id) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u WHERE u.id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public List<UserDTO> getAll() {
        List<UserDTO> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u ORDER BY u.id";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // =========================================================
    // INSERT / UPDATE / DELETE
    // =========================================================
    public boolean insert(UserDTO u) {
        String sql = "INSERT INTO users (username, password, role, is_active) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, u.getUsername());
            ps.setNString(2, u.getPassword());
            String role = u.getRole() != null ? u.getRole() : "customer";
            if (!"admin".equals(role) && !"customer".equals(role)) role = "customer";
            ps.setNString(3, role);
            ps.setBoolean(4, u.isActive());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // =========================================================
    // UPDATE PROFILE
    // =========================================================
    public boolean updateProfile(int userId, String newUsername, String fullName,
                                  String phone, String email, String avatarUrl) {
        String sql = "UPDATE users SET username = ?, avatar_url = ?, email = ?, "
                   + "phone = ?, full_name = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, newUsername);
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                ps.setNull(2, Types.NVARCHAR);
            } else {
                ps.setNString(2, avatarUrl);
            }
            ps.setNString(3, email);
            ps.setNString(4, phone);
            ps.setNString(5, fullName);
            ps.setInt(6, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean checkPassword(int userId, String currentPassword) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ? AND password = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setNString(2, currentPassword);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // =========================================================
    // MAP RESULTSET (có OAuth)
    // =========================================================
    private UserDTO mapResultSet(ResultSet rs) throws SQLException {
        UserDTO u = new UserDTO();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getNString("username"));
        u.setPassword(rs.getNString("password"));
        u.setRole(rs.getNString("role"));
        u.setActive(rs.getBoolean("is_active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));

        try { u.setAvatarUrl(rs.getNString("avatar_url")); } catch (SQLException ignored) {}
        try { u.setEmail(rs.getNString("email")); } catch (SQLException ignored) {}
        try { u.setPhone(rs.getNString("phone")); } catch (SQLException ignored) {}
        try { u.setFullName(rs.getNString("full_name")); } catch (SQLException ignored) {}

        // ⭐ OAUTH FIELDS
        try { u.setGoogleId(rs.getNString("google_id")); } catch (SQLException ignored) {}
        try { u.setFacebookId(rs.getNString("facebook_id")); } catch (SQLException ignored) {}
        try { u.setAuthProvider(rs.getNString("auth_provider")); } catch (SQLException ignored) {}
        try { u.setEmailVerified(rs.getBoolean("email_verified")); } catch (SQLException ignored) {}

        int cid = rs.getInt("consultant_id");
        u.setConsultantId(rs.wasNull() ? null : cid);
        return u;
    }

    // =========================================================
    // ĐĂNG KÝ
    // =========================================================
    public int register(String username, String password) {
        if (getByUsername(username) != null) return -1;
        String sql = "INSERT INTO users (username, password, role, is_active) "
                   + "VALUES (?, ?, 'customer', 1)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, username);
            ps.setNString(2, password);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public int registerFull(String username, String password, String fullName,
                            String phone, String email) {
        if (getByUsername(username) != null) return -1;

        String sql = "INSERT INTO users (username, password, role, is_active, "
                   + "full_name, phone, email) VALUES (?, ?, 'customer', 1, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, username);
            ps.setNString(2, password);
            ps.setNString(3, fullName);

            if (phone == null || phone.trim().isEmpty()) {
                ps.setNull(4, Types.NVARCHAR);
            } else {
                ps.setNString(4, phone);
            }
            if (email == null || email.trim().isEmpty()) {
                ps.setNull(5, Types.NVARCHAR);
            } else {
                ps.setNString(5, email);
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

    // =========================================================
    // QUẢN LÝ TÀI KHOẢN (ADMIN)
    // =========================================================
    public List<UserDTO> search(String keyword, String status, int page, int pageSize) {
        List<UserDTO> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
          + "       u.avatar_url, u.email, u.phone, u.full_name, "
          + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
          + "       u.id AS consultant_id "
          + "FROM users u WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if ("active".equals(status)) sql.append(" AND u.is_active = 1 ");
        else if ("locked".equals(status)) sql.append(" AND u.is_active = 0 ");

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (u.username LIKE ? OR u.email LIKE ? OR u.phone LIKE ? "
                     + "OR u.full_name LIKE ? OR CAST(u.id AS NVARCHAR(50)) LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw); params.add(kw); params.add(kw);
        }

        sql.append(" ORDER BY u.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY ");
        params.add((page - 1) * pageSize);
        params.add(pageSize);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public int countSearch(String keyword, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users u WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if ("active".equals(status)) sql.append(" AND u.is_active = 1 ");
        else if ("locked".equals(status)) sql.append(" AND u.is_active = 0 ");

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (u.username LIKE ? OR u.email LIKE ? OR u.phone LIKE ? "
                     + "OR u.full_name LIKE ? OR CAST(u.id AS NVARCHAR(50)) LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw); params.add(kw); params.add(kw);
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

    public boolean lockAccount(int userId) {
        String sql = "UPDATE users SET is_active = 0 WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean unlockAccount(int userId) {
        String sql = "UPDATE users SET is_active = 1 WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public int countActive() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = 1";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public int countLocked() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = 0";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================
    // ⭐ QUÊN MẬT KHẨU (MỚI)
    // =========================================================

    public UserDTO findByEmailOrPhone(String identifier) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u "
                   + "WHERE (u.email = ? OR u.phone = ? OR u.username = ?) "
                   + "  AND u.is_active = 1";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, identifier);
            ps.setNString(2, identifier);
            ps.setNString(3, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean saveResetToken(int userId, String token, Timestamp expiresAt) {
        // Xóa token cũ
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM password_reset_token WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }

        // Thêm token mới
        String sql = "INSERT INTO password_reset_token "
                   + "(user_id, token, expires_at, used, created_at) "
                   + "VALUES (?, ?, ?, 0, GETDATE())";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setNString(2, token);
            ps.setTimestamp(3, expiresAt);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public UserDTO findByValidToken(String token) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u "
                   + "JOIN password_reset_token t ON u.id = t.user_id "
                   + "WHERE t.token = ? AND t.used = 0 AND t.expires_at > GETDATE()";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean markTokenAsUsed(String token) {
        String sql = "UPDATE password_reset_token SET used = 1 WHERE token = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, token);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // =========================================================
    // ⭐ OAUTH (MỚI)
    // =========================================================

    public UserDTO findByGoogleId(String googleId) {
        String sql = "SELECT u.id, u.username, u.password, u.role, u.is_active, u.created_at, "
                   + "       u.avatar_url, u.email, u.phone, u.full_name, "
                   + "       u.google_id, u.facebook_id, u.auth_provider, u.email_verified, "
                   + "       u.id AS consultant_id "
                   + "FROM users u WHERE u.google_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, googleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean linkGoogleAccount(int userId, String googleId, String avatarUrl) {
        String sql = "UPDATE users SET google_id = ?, auth_provider = 'google', "
                   + "email_verified = 1, avatar_url = COALESCE(?, avatar_url) "
                   + "WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, googleId);
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                ps.setNull(2, Types.NVARCHAR);
            } else {
                ps.setNString(2, avatarUrl);
            }
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public int insertGoogleUser(UserDTO user) {
        String sql = "INSERT INTO users "
                   + "(username, password, email, full_name, avatar_url, "
                   + " google_id, auth_provider, email_verified, role, is_active, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, 'google', 1, 'customer', 1, GETDATE())";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, user.getUsername());
            ps.setNull(2, Types.NVARCHAR);  // password NULL cho Google user
            ps.setNString(3, user.getEmail());
            ps.setNString(4, user.getFullName());
            if (user.getAvatarUrl() == null || user.getAvatarUrl().trim().isEmpty()) {
                ps.setNull(5, Types.NVARCHAR);
            } else {
                ps.setNString(5, user.getAvatarUrl());
            }
            ps.setNString(6, user.getGoogleId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }
}