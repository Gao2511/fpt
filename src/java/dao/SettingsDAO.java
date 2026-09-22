package dao;

import dto.SettingsDTO;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsDAO {

    /** Lấy 1 giá trị theo key */
    public String getValue(String key) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getNString("setting_value");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    /** Lấy tất cả settings dạng Map */
    public Map<String, String> getAllAsMap() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM settings";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getNString("setting_key"), rs.getNString("setting_value"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    /** Lấy tất cả settings dạng List */
    public List<SettingsDTO> getAll() {
        List<SettingsDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM settings ORDER BY id";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SettingsDTO s = new SettingsDTO();
                s.setId(rs.getInt("id"));
                s.setSettingKey(rs.getNString("setting_key"));
                s.setSettingValue(rs.getNString("setting_value"));
                s.setDescription(rs.getNString("description"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(s);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /** Cập nhật giá trị setting */
    public boolean update(String key, String value) {
        String sql = "UPDATE settings SET setting_value = ?, updated_at = GETDATE() WHERE setting_key = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, value);
            ps.setNString(2, key);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /** Insert hoặc Update (upsert) */
    public boolean upsert(String key, String value, String description) {
        String sql = "MERGE settings AS target "
                   + "USING (SELECT ? AS setting_key, ? AS setting_value, ? AS description) AS source "
                   + "ON target.setting_key = source.setting_key "
                   + "WHEN MATCHED THEN UPDATE SET setting_value = source.setting_value, "
                   + "    description = source.description, updated_at = GETDATE() "
                   + "WHEN NOT MATCHED THEN INSERT (setting_key, setting_value, description) "
                   + "    VALUES (source.setting_key, source.setting_value, source.description);";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, key);
            ps.setNString(2, value);
            ps.setNString(3, description);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}