package cms;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
/** Decode before storing; no user paths or executable image formats. */
public class MediaStore {
    public static final class Image {
        public final byte[] bytes;public final String type;public final int width,height;
        Image(byte[] b,String t,int w,int h){bytes=b;type=t;width=w;height=h;}
    }
    public static Image validate(InputStream input)throws IOException{
        ByteArrayOutputStream raw=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){if(raw.size()+n>2097152)throw new IOException("Ảnh tối đa 2 MB");raw.write(buffer,0,n);}
        try(ImageInputStream stream=ImageIO.createImageInputStream(new ByteArrayInputStream(raw.toByteArray()))){Iterator<ImageReader> readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IOException("Nội dung không phải ảnh hợp lệ");ImageReader reader=readers.next();try{reader.setInput(stream);String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!format.equals("png")&&!format.equals("jpeg")&&!format.equals("jpg"))throw new IOException("Chỉ nhận PNG/JPEG");int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>4096||h>4096||(long)w*h>16000000)throw new IOException("Kích thước ảnh không hợp lệ");BufferedImage decoded=reader.read(0);ByteArrayOutputStream clean=new ByteArrayOutputStream();if(!ImageIO.write(decoded,format.equals("png")?"png":"jpeg",clean)||clean.size()>2097152)throw new IOException("Ảnh sau xử lý quá lớn");return new Image(clean.toByteArray(),format.equals("png")?"image/png":"image/jpeg",w,h);}finally{reader.dispose();}}
    }
    public String save(Image image,int actor)throws Exception{
        String id=UUID.randomUUID().toString();try(Connection c=utils.DBUtils.getConnection()){c.setAutoCommit(false);try{
            try(PreparedStatement p=c.prepareStatement("SELECT pg_advisory_xact_lock(73119422)")){p.execute();}
            try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(octet_length(content)),0) FROM cms_media");ResultSet r=p.executeQuery()){r.next();if(r.getLong(1)+image.bytes.length>20971520)throw new SQLException("Kho ảnh đã đạt giới hạn 20 MB");}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO cms_media(id,content_type,content,width,height,actor_id) VALUES(CAST(? AS uuid),?,?,?,?,?)")){p.setString(1,id);p.setString(2,image.type);p.setBytes(3,image.bytes);p.setInt(4,image.width);p.setInt(5,image.height);p.setInt(6,actor);p.executeUpdate();}c.commit();return "/media?id="+id;
        }catch(SQLException e){c.rollback();throw e;}}
    }
}
