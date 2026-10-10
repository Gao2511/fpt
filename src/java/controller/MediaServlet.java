package controller;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import dto.UserDTO;
@WebServlet(urlPatterns={"/media","/admin/media"})
@MultipartConfig(maxFileSize=2097152,maxRequestSize=2200000)
public class MediaServlet extends HttpServlet {
    protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException,ServletException{
        boolean json="application/json".equals(r.getHeader("Accept"));
        UserDTO u=(UserDTO)r.getSession().getAttribute("user");if(!r.getServletPath().equals("/admin/media")||!security.AdminSecurity.admin(u)||!security.AdminSecurity.csrf(r)){p.sendError(403);return;}
        try{Part part=r.getPart("image");if(part==null)throw new IOException("Chọn ảnh");String url;try(InputStream input=part.getInputStream()){url=new cms.MediaStore().save(cms.MediaStore.validate(input),u.getId());}
            p.setHeader("X-Content-Type-Options","nosniff");
            if(json){p.setContentType("application/json;charset=UTF-8");com.google.gson.JsonObject result=new com.google.gson.JsonObject();result.addProperty("url",url);p.getWriter().write(result.toString());}
            else {p.setContentType("text/html;charset=UTF-8");p.getWriter().write("<!doctype html><meta charset='utf-8'><p>Đã tải ảnh. Sao chép đường dẫn vào trường ảnh và lưu xuất bản:</p><code>"+url+"</code><p><a href='"+r.getContextPath()+"/admin/content'>Quay lại nội dung</a></p>");}
        }catch(Exception e){if(json){p.setStatus(400);p.setContentType("application/json;charset=UTF-8");p.getWriter().write("{\"error\":\"Không thể tải ảnh. Chọn PNG/JPEG tối đa 2 MB, kích thước không quá 4096px hoặc kiểm tra kho ảnh.\"}");}else p.sendError(400,"Không thể tải ảnh. Kiểm tra PNG/JPEG, giới hạn 2 MB, kho ảnh và migration.");}
    }
    protected void doGet(HttpServletRequest r,HttpServletResponse p)throws IOException{
        String id=r.getParameter("id");if(id==null||!id.matches("[a-f0-9-]{36}")){p.sendError(404);return;}
        // Only published/referenced media is public; an uploaded draft is not automatically published.
        String sql="SELECT content_type,content FROM cms_media WHERE id=CAST(? AS uuid) AND (EXISTS(SELECT 1 FROM settings WHERE setting_key LIKE 'cms.%' AND setting_value=?) OR EXISTS(SELECT 1 FROM users WHERE avatar_url=?))";
        try(Connection c=utils.DBUtils.getConnection();PreparedStatement q=c.prepareStatement(sql)){q.setString(1,id);q.setString(2,"/media?id="+id);q.setString(3,"media?id="+id);try(ResultSet result=q.executeQuery()){if(!result.next()){p.sendError(404);return;}p.setHeader("X-Content-Type-Options","nosniff");p.setHeader("Content-Security-Policy","default-src 'none'");p.setHeader("Cache-Control","public,max-age=3600");p.setContentType(result.getString(1));byte[] b=result.getBytes(2);p.setContentLength(b.length);p.getOutputStream().write(b);}}
        catch(Exception e){p.sendError(503);}
    }
}
