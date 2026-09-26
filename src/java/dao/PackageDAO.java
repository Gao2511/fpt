package dao;

import dto.PackageDTO;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackageDAO {

    // ===== UPDATE BADGE TYPE =====
    public boolean updateBadgeType(int id, String badgeType) {
        String sql = "UPDATE packages SET badge_type = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (badgeType == null || badgeType.trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, badgeType);
            }
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===== GET ALL =====
    public List<PackageDTO> getAll() {
        List<PackageDTO> list = new ArrayList<>();
        String sql = "SELECT id, package_code, name, price, speed_mbps, "
                   + "description, long_description, is_hot, badge_type, created_at "
                   + "FROM packages ORDER BY price";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ===== GET HOT =====
    public List<PackageDTO> getHotPackages() {
        List<PackageDTO> list = new ArrayList<>();
        String sql = "SELECT id, package_code, name, price, speed_mbps, "
                   + "description, long_description, is_hot, badge_type, created_at "
                   + "FROM packages WHERE is_hot = TRUE";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ===== GET BY ID =====
    public PackageDTO getById(int id) {
        String sql = "SELECT id, package_code, name, price, speed_mbps, "
                   + "description, long_description, is_hot, badge_type, created_at "
                   + "FROM packages WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ===== GET BY CODE =====
    public PackageDTO getByCode(String packageCode) {
        String sql = "SELECT id, package_code, name, price, speed_mbps, "
                   + "description, long_description, is_hot, badge_type, created_at "
                   + "FROM packages WHERE package_code = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, packageCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ===== INSERT =====
    public boolean insert(PackageDTO p) {
        String sql = "INSERT INTO packages (package_code, name, price, speed_mbps, "
                   + "description, long_description, is_hot, badge_type) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getPackageCode());
            ps.setString(2, p.getName());
            ps.setLong(3, p.getPrice());
            ps.setInt(4, p.getSpeedMbps());
            ps.setString(5, p.getDescription());
            ps.setString(6, p.getLongDescription());
            ps.setBoolean(7, p.isHot());
            if (p.getBadgeType() == null || p.getBadgeType().trim().isEmpty()) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, p.getBadgeType());
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ===== UPDATE =====
    public boolean update(PackageDTO p) {
        String sql = "UPDATE packages SET package_code = ?, name = ?, price = ?, "
                   + "speed_mbps = ?, description = ?, long_description = ?, "
                   + "is_hot = ?, badge_type = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getPackageCode());
            ps.setString(2, p.getName());
            ps.setLong(3, p.getPrice());
            ps.setInt(4, p.getSpeedMbps());
            ps.setString(5, p.getDescription());
            ps.setString(6, p.getLongDescription());
            ps.setBoolean(7, p.isHot());
            if (p.getBadgeType() == null || p.getBadgeType().trim().isEmpty()) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, p.getBadgeType());
            }
            ps.setInt(9, p.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===== DELETE =====
    public boolean delete(int id) {
        String sql = "DELETE FROM packages WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ===== MAP RESULTSET =====
    private PackageDTO mapResultSet(ResultSet rs) throws SQLException {
        PackageDTO p = new PackageDTO();
        p.setId(rs.getInt("id"));
        p.setPackageCode(rs.getString("package_code"));
        p.setName(rs.getString("name"));
        p.setPrice(rs.getLong("price"));
        p.setSpeedMbps(rs.getInt("speed_mbps"));
        p.setDescription(rs.getString("description"));
        p.setLongDescription(rs.getString("long_description"));
        p.setHot(rs.getBoolean("is_hot"));
        p.setBadgeType(rs.getString("badge_type"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }

    // ===== SEARCH =====
    public List<PackageDTO> search(String keyword, String sortBy, int page, int pageSize) {
        List<PackageDTO> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT id, package_code, name, price, speed_mbps, "
          + "description, long_description, is_hot, badge_type, created_at "
          + "FROM packages WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR package_code LIKE ? OR description LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw);
        }

        if ("price_desc".equals(sortBy))      sql.append(" ORDER BY price DESC ");
        else if ("price_asc".equals(sortBy))  sql.append(" ORDER BY price ASC ");
        else if ("name".equals(sortBy))       sql.append(" ORDER BY name ASC ");
        else if ("speed".equals(sortBy))      sql.append(" ORDER BY speed_mbps DESC ");
        else                                  sql.append(" ORDER BY id ASC ");

        sql.append(" LIMIT ? OFFSET ? ");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ===== COUNT SEARCH =====
    public int countSearch(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM packages WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR package_code LIKE ? OR description LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw);
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

    // ===== IS CODE EXISTS =====
    public boolean isCodeExists(String packageCode, int excludeId) {
        String sql = "SELECT COUNT(*) FROM packages WHERE package_code = ? AND id <> ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, packageCode);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ===== COUNT HOT =====
    public int countHot() {
        String sql = "SELECT COUNT(*) FROM packages WHERE is_hot = TRUE";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}