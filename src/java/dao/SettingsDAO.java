package dao;

import dto.SettingsDTO;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsDAO {
    /** Narrow public data query; operational secrets never enter CMS/demo snapshots. */
    public Map<String,String> getCms() {
        Map<String,String> values=new java.util.LinkedHashMap<>();
        try(Connection c=DBUtils.getConnection();PreparedStatement p=c.prepareStatement("SELECT setting_key, setting_value FROM settings WHERE setting_key LIKE 'cms.%'");ResultSet r=p.executeQuery()){
            while(r.next())values.put(r.getString(1),r.getString(2));
        }catch(Exception e){System.err.println("[CMS] Public content unavailable; using defaults");}return values;
    }
    /** Atomic configuration publish and revision. Requires reviewed additive migration. */
    public boolean saveVersioned(Map<String,String> updates,int actor,String kind) {
        try(Connection c=DBUtils.getConnection()){
            c.setAutoCommit(false);
            try{
                // Serialize settings publishers to make each old snapshot and rollback accurate.
                try(PreparedStatement lock=c.prepareStatement("SELECT pg_advisory_xact_lock(73119421)")){lock.execute();}
                com.google.gson.JsonObject before=new com.google.gson.JsonObject(),after=new com.google.gson.JsonObject();
                String sql="INSERT INTO settings(setting_key,setting_value,updated_at) VALUES(?,?,NOW()) ON CONFLICT(setting_key) DO UPDATE SET setting_value=EXCLUDED.setting_value,updated_at=NOW()";
                for(Map.Entry<String,String> entry:updates.entrySet()){
                    String key=entry.getKey();boolean tracked=key.startsWith("cms.")||key.equals("ai_system_prompt")||key.equals("gemini_system_prompt");
                    if(tracked){try(PreparedStatement read=c.prepareStatement("SELECT setting_value FROM settings WHERE setting_key=?")){read.setString(1,key);try(ResultSet r=read.executeQuery()){before.addProperty(key,r.next()?r.getString(1):null);}}after.addProperty(key,entry.getValue());}
                    if(entry.getValue()==null){try(PreparedStatement delete=c.prepareStatement("DELETE FROM settings WHERE setting_key=?")){delete.setString(1,key);delete.executeUpdate();}}
                    else try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,key);p.setString(2,entry.getValue());if(p.executeUpdate()!=1)throw new SQLException("Publish failed");}
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO cms_revisions(actor_id,change_type,before_data,after_data) VALUES(?,?,CAST(? AS jsonb),CAST(? AS jsonb))")){p.setInt(1,actor);p.setString(2,kind);p.setString(3,before.toString());p.setString(4,after.toString());p.executeUpdate();}
                c.commit();return true;
            }catch(Exception e){c.rollback();System.err.println("[CMS] Publish rolled back");return false;}
        }catch(Exception e){System.err.println("[CMS] Storage unavailable");return false;}
    }
    public java.util.List<Map<String,String>> revisions(String kind){java.util.List<Map<String,String>> list=new java.util.ArrayList<>();try(Connection c=DBUtils.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,actor_id,created_at FROM cms_revisions WHERE change_type=? ORDER BY id DESC LIMIT 20")){p.setString(1,kind);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,String> m=new java.util.HashMap<>();m.put("id",r.getString(1));m.put("actor",r.getString(2));m.put("time",r.getString(3));list.add(m);}}}catch(Exception e){System.err.println("[CMS] Revision list unavailable");}return list;}
    public boolean rollbackLatest(int revisionId,int actor,String kind){
        // Optimistic precondition: never overwrite a later publish with an obsolete snapshot.
        try(Connection c=DBUtils.getConnection()){
            c.setAutoCommit(false);
            try{
                try(PreparedStatement lock=c.prepareStatement("SELECT pg_advisory_xact_lock(73119421)")){lock.execute();}
                com.google.gson.JsonObject before,after;
                try(PreparedStatement p=c.prepareStatement("SELECT id,before_data,after_data FROM cms_revisions WHERE change_type=? ORDER BY id DESC LIMIT 1")){p.setString(1,kind);try(ResultSet r=p.executeQuery()){if(!r.next()||r.getInt(1)!=revisionId){c.rollback();return false;}before=com.google.gson.JsonParser.parseString(r.getString(2)).getAsJsonObject();after=com.google.gson.JsonParser.parseString(r.getString(3)).getAsJsonObject();}}
                for(Map.Entry<String,com.google.gson.JsonElement> entry:after.entrySet()){String key=entry.getKey();if(!(key.startsWith("cms.")||key.equals("ai_system_prompt")||key.equals("gemini_system_prompt")))throw new SQLException("Invalid revision");try(PreparedStatement p=c.prepareStatement("SELECT setting_value FROM settings WHERE setting_key=?")){p.setString(1,key);try(ResultSet r=p.executeQuery()){String current=r.next()?r.getString(1):null;String expected=entry.getValue().isJsonNull()?null:entry.getValue().getAsString();if(!java.util.Objects.equals(current,expected))throw new SQLException("Revision conflict");}}}
                for(Map.Entry<String,com.google.gson.JsonElement> e:before.entrySet()){if(e.getValue().isJsonNull()){try(PreparedStatement p=c.prepareStatement("DELETE FROM settings WHERE setting_key=?")){p.setString(1,e.getKey());p.executeUpdate();}}else try(PreparedStatement p=c.prepareStatement("INSERT INTO settings(setting_key,setting_value,updated_at) VALUES(?,?,NOW()) ON CONFLICT(setting_key) DO UPDATE SET setting_value=EXCLUDED.setting_value,updated_at=NOW()")){p.setString(1,e.getKey());p.setString(2,e.getValue().getAsString());if(p.executeUpdate()!=1)throw new SQLException("Rollback restore failed");}}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO cms_revisions(actor_id,change_type,before_data,after_data) VALUES(?,?,CAST(? AS jsonb),CAST(? AS jsonb))")){p.setInt(1,actor);p.setString(2,kind);p.setString(3,after.toString());p.setString(4,before.toString());p.executeUpdate();}c.commit();return true;
            }catch(Exception e){c.rollback();return false;}
        }catch(Exception e){return false;}
    }

    /** Lấy 1 giá trị theo key */
    public String getValue(String key) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("setting_value");
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
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
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
                s.setSettingKey(rs.getString("setting_key"));
                s.setSettingValue(rs.getString("setting_value"));
                s.setDescription(rs.getString("description"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(s);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /** Cập nhật hoặc thêm mới giá trị setting (Safe Upsert) */
    public boolean update(String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value, updated_at) "
                   + "VALUES (?, ?, NOW()) "
                   + "ON CONFLICT (setting_key) "
                   + "DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = NOW()";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /** Insert hoặc Update (upsert) — PostgreSQL ON CONFLICT */
    public boolean upsert(String key, String value, String description) {
        String sql = "INSERT INTO settings (setting_key, setting_value, description, updated_at) "
                   + "VALUES (?, ?, ?, NOW()) "
                   + "ON CONFLICT (setting_key) "
                   + "DO UPDATE SET setting_value = EXCLUDED.setting_value, "
                   + "              description = EXCLUDED.description, "
                   + "              updated_at = NOW()";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, description);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}
