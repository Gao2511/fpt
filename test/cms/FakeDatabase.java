package cms;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
/** JDBC contract fixture. Never opens a network socket or falls through to a real driver. */
public final class FakeDatabase implements Driver {
    public final Map<String,String> settings=new LinkedHashMap<>();
    public final List<Map<String,Object>> revisions=new ArrayList<>();
    public final List<Map<String,Object>> packages=new ArrayList<>();
    public final Map<String,Map<String,Object>> media=new LinkedHashMap<>();
    public final Map<String,Object> customer=new LinkedHashMap<>();
    public int writes,privateReads,connections;public String failKey;
    public FakeDatabase(){for(dto.PackageDTO p:DemoData.packages()){Map<String,Object> row=new LinkedHashMap<>();row.put("id",p.getId());row.put("package_code",p.getPackageCode());row.put("name",p.getName());row.put("price",p.getPrice());row.put("speed_mbps",p.getSpeedMbps());row.put("description",p.getDescription());row.put("long_description","");row.put("is_hot",false);row.put("badge_type",null);packages.add(row);}}
    public void install()throws Exception{Class.forName("org.postgresql.Driver");for(Driver d:Collections.list(DriverManager.getDrivers()))DriverManager.deregisterDriver(d);DriverManager.registerDriver(this);}
    static Object value(Class<?> t){if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;return null;}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> t,InvocationHandler h){return (T)Proxy.newProxyInstance(t.getClassLoader(),new Class[]{t},h);}
    public Connection connect(String url,Properties info){connections++;Map<String,String> old=new LinkedHashMap<>(settings);List<Map<String,Object>> oldRevisions=new ArrayList<>(revisions);List<Map<String,Object>> oldPackages=new ArrayList<>();for(Map<String,Object> p:packages)oldPackages.add(new LinkedHashMap<>(p));
        return proxy(Connection.class,(o,m,a)->{switch(m.getName()){
            case "prepareStatement":return statement((String)a[0]);case "commit":return null;
            case "rollback":settings.clear();settings.putAll(old);revisions.clear();revisions.addAll(oldRevisions);packages.clear();packages.addAll(oldPackages);return null;
            case "isClosed":return false;case "getAutoCommit":return true;default:return value(m.getReturnType());}});
    }
    private PreparedStatement statement(String sql){Map<Integer,Object> params=new HashMap<>();return proxy(PreparedStatement.class,(o,m,a)->{
        if(m.getName().startsWith("set")&&a!=null&&a.length>=2&&a[0] instanceof Integer){params.put((Integer)a[0],m.getName().equals("setNull")?null:a[1]);return null;}
        switch(m.getName()){case "executeQuery":return result(query(sql,params));case "executeUpdate":return write(sql,params);case "execute":if(sql.contains("pg_advisory"))return true;write(sql,params);return true;case "getGeneratedKeys":return result(Collections.singletonList(row("id",packages.size())));default:return value(m.getReturnType());}
    });}
    private static Map<String,Object> row(String key,Object value){Map<String,Object> m=new LinkedHashMap<>();m.put(key,value);return m;}
    private List<Map<String,Object>> query(String sql,Map<Integer,Object> p)throws SQLException{
        if(sql.contains("FROM cms_media")){
            if(sql.contains("SUM(octet_length")){long total=0;for(Map<String,Object> image:media.values())total+=((byte[])image.get("content")).length;return Collections.singletonList(row("bytes",total));}
            Map<String,Object> image=media.get(p.get(1));if(image==null||!settings.containsValue(p.get(2)))return Collections.emptyList();Map<String,Object> out=row("content_type",image.get("content_type"));out.put("content",image.get("content"));return Collections.singletonList(out);
        }
        if(sql.contains("FROM customers")){privateReads++;return customer.isEmpty()?Collections.emptyList():Collections.singletonList(customer);}
        if(sql.contains("FROM settings")){List<Map<String,Object>> rows=new ArrayList<>();if(sql.contains("LIKE 'cms.%'")){for(Map.Entry<String,String> e:settings.entrySet())if(e.getKey().startsWith("cms.")){Map<String,Object> r=row("setting_key",e.getKey());r.put("setting_value",e.getValue());rows.add(r);}}else if(sql.contains("setting_key = ?")||sql.contains("setting_key=?")){String key=(String)p.get(1);if(settings.containsKey(key))rows.add(row("setting_value",settings.get(key)));}else for(Map.Entry<String,String> e:settings.entrySet()){Map<String,Object> r=row("setting_key",e.getKey());r.put("setting_value",e.getValue());rows.add(r);}return rows;}
        if(sql.contains("FROM cms_revisions")){List<Map<String,Object>> rows=new ArrayList<>();for(int i=revisions.size()-1;i>=0;i--){Map<String,Object> r=revisions.get(i);if(!r.get("change_type").equals(p.get(1)))continue;Map<String,Object> out=new LinkedHashMap<>();out.put("id",r.get("id"));if(sql.contains("before_data")){out.put("before_data",r.get("before_data"));out.put("after_data",r.get("after_data"));}else {out.put("actor_id",r.get("actor_id"));out.put("created_at","2026-10-11T00:00:00Z");}rows.add(out);if(sql.contains("LIMIT 1"))break;}return rows;}
        if(sql.contains("FROM packages")){List<Map<String,Object>> rows=new ArrayList<>();for(Map<String,Object> r:packages){if(sql.contains("WHERE id")&&!r.get("id").equals(p.get(1)))continue;if(sql.contains("package_code = ?")&&(!r.get("package_code").equals(p.get(1))||r.get("id").equals(p.get(2))))continue;if(sql.contains("is_hot = TRUE")&&!Boolean.TRUE.equals(r.get("is_hot")))continue;rows.add(sql.contains("to_jsonb")?row("json",new com.google.gson.Gson().toJson(r)):r);}return sql.contains("COUNT(*)")?Collections.singletonList(row("count",rows.size())):rows;}
        if(sql.contains("FROM users")){privateReads++;Map<String,Object> u=new LinkedHashMap<>();u.put("id",101);u.put("username","test-admin");u.put("role","admin");u.put("is_active",true);u.put("password",null);u.put("full_name","Test administrator");u.put("auth_provider","local");return Collections.singletonList(u);}
        if(sql.contains("pg_advisory"))return Collections.emptyList();throw new SQLException("Unsupported fixture query: "+sql);
    }
    private int write(String sql,Map<Integer,Object> p)throws SQLException{
        writes++;if(failKey!=null&&failKey.equals(p.get(1)))throw new SQLException("Injected persist failure");
        if(sql.startsWith("INSERT INTO cms_media")){Map<String,Object> image=row("content_type",p.get(2));image.put("content",p.get(3));media.put((String)p.get(1),image);return 1;}
        if(sql.startsWith("UPDATE customers SET full_name")){String[] keys={"full_name","phone","address","email","note","status","package_interest"};for(int i=0;i<keys.length;i++)customer.put(keys[i],p.get(i+1));return 1;}
        if(sql.startsWith("INSERT INTO settings")){settings.put((String)p.get(1),(String)p.get(2));return 1;}
        if(sql.startsWith("DELETE FROM settings")){settings.remove(p.get(1));return 1;}
        if(sql.startsWith("UPDATE settings")){settings.put((String)p.get(2),(String)p.get(1));return 1;}
        if(sql.startsWith("INSERT INTO cms_revisions")){Map<String,Object> r=row("id",revisions.size()+1);r.put("actor_id",p.get(1));r.put("change_type",p.get(2));r.put("before_data",p.get(3));r.put("after_data",p.get(4));revisions.add(r);return 1;}
        if(sql.startsWith("INSERT INTO packages")||sql.startsWith("UPDATE packages SET package_code")){boolean insert=sql.startsWith("INSERT");Map<String,Object> r=null;for(Map<String,Object> row:packages)if(row.get("id").equals(p.get(9)))r=row;if(insert){r=row("id",packages.size()+1);packages.add(r);}if(r==null)return 0;String[] keys={"package_code","name","price","speed_mbps","description","long_description","is_hot","badge_type"};for(int i=0;i<keys.length;i++)r.put(keys[i],p.get(i+1));return 1;}
        throw new SQLException("Unsupported fixture write");
    }
    private ResultSet result(List<Map<String,Object>> rows){int[] at={-1};boolean[] nil={false};return proxy(ResultSet.class,(o,m,a)->{if(m.getName().equals("next"))return ++at[0]<rows.size();if(m.getName().equals("wasNull"))return nil[0];if(m.getName().startsWith("get")&&a!=null&&a.length==1){Map<String,Object> r=rows.get(at[0]);Object v=a[0] instanceof Integer?new ArrayList<>(r.values()).get((Integer)a[0]-1):r.get(a[0]);nil[0]=v==null;switch(m.getName()){case "getString":return v==null?null:String.valueOf(v);case "getInt":return v==null?0:((Number)v).intValue();case "getLong":return v==null?0L:((Number)v).longValue();case "getBoolean":return v!=null&&(Boolean)v;case "getTimestamp":return null;default:return v;}}return value(m.getReturnType());});}
    public boolean acceptsURL(String u){return true;}public DriverPropertyInfo[]getPropertyInfo(String u,Properties p){return new DriverPropertyInfo[0];}public int getMajorVersion(){return 1;}public int getMinorVersion(){return 0;}public boolean jdbcCompliant(){return false;}public Logger getParentLogger(){return Logger.getGlobal();}
}
