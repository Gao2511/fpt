package dao;

import dto.ApiKeyHistoryDTO;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApiKeyHistoryDAO {

    /** Ghi log khi admin đổi API key */
    public boolean log(int changedBy, String changedByName, String apiKey,
                       String model, String action, String note) {
        String sql = "INSERT INTO api_key_history "
                   + "(api_key, model, changed_by, changed_by_name, action, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setNString(1, apiKey);
            ps.setNString(2, model);

            if (changedBy > 0) {
                ps.setInt(3, changedBy);
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setNString(4, changedByName);
            ps.setNString(5, action != null ? action : "UPDATE");
            ps.setNString(6, note);

            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /** Lấy tất cả lịch sử (mới nhất trước) */
    public List<ApiKeyHistoryDTO> getAll(int limit) {
        List<ApiKeyHistoryDTO> list = new ArrayList<>();
        String sql = "SELECT TOP (?) * FROM api_key_history "
                   + "ORDER BY created_at DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /** Xóa 1 dòng lịch sử */
    public boolean delete(int id) {
        String sql = "DELETE FROM api_key_history WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /** Xóa toàn bộ lịch sử */
    public int deleteAll() {
        String sql = "DELETE FROM api_key_history";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); return 0; }
    }

    /** Đếm tổng số */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM api_key_history";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private ApiKeyHistoryDTO mapResultSet(ResultSet rs) throws SQLException {
        ApiKeyHistoryDTO h = new ApiKeyHistoryDTO();
        h.setId(rs.getInt("id"));
        h.setApiKey(rs.getNString("api_key"));
        h.setModel(rs.getNString("model"));

        int cid = rs.getInt("changed_by");
        h.setChangedBy(rs.wasNull() ? null : cid);

        h.setChangedByName(rs.getNString("changed_by_name"));
        h.setAction(rs.getNString("action"));
        h.setNote(rs.getNString("note"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        return h;
    }
}