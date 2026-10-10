package cms;
import java.util.*;
/** Presentation groups only; preserves the existing CMS keys and validation rules. */
public final class EditorLayout {
    public static final class Group {
        private final String title; private final List<SiteContent.Field> fields=new ArrayList<>();
        Group(String title){this.title=title;}
        public String getTitle(){return title;} public List<SiteContent.Field> getFields(){return fields;}
    }
    public static final class Section {
        private final String id,title,description; private final Map<String,Group> groups=new LinkedHashMap<>();
        Section(String id,String title,String description){this.id=id;this.title=title;this.description=description;}
        public String getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;}
        public Collection<Group> getGroups(){return groups.values();}
    }
    private EditorLayout(){}
    public static List<Section> sections(boolean business){
        Map<String,Section> sections=new LinkedHashMap<>();
        if(business){
            add(sections,"contact","Thông tin liên hệ","Hotline, địa chỉ và các kênh liên hệ trên website.");
            add(sections,"fees","Phí lắp đặt Internet","Khoản thu một lần, tách biệt với giá gói cước hàng tháng.");
            add(sections,"mesh","Dịch vụ Mesh WiFi","Nội dung khu vực Mesh WiFi và chi phí của từng cấu hình.");
            add(sections,"promotion","Thông tin ưu đãi","Nội dung, thời hạn và các gói được áp dụng ưu đãi.");
        }else{
            add(sections,"hero","Đầu trang khách hàng","Tiêu đề và nút hành động ở đầu trang chủ.");
            add(sections,"images","Ảnh banner & nền","Các ảnh trượt trên dashboard khách hàng và ảnh nền đầu trang.");
            add(sections,"navigation","Logo & menu","Logo và các mục điều hướng trên thanh menu khách hàng.");
            add(sections,"packages","Khu vực bảng giá","Tiêu đề phía trên danh sách gói. Giá và tốc độ sửa tại mục Gói cước.");
            add(sections,"benefits","Lợi ích dịch vụ","Các ô lợi ích phía dưới bảng giá trên trang khách hàng.");
            add(sections,"devices","Thiết bị đi kèm","Ảnh và nội dung các ô WiFi 6, FPT Play, Camera và truyền hình.");
            add(sections,"footer","Liên hệ & chân trang","Tiêu đề biểu mẫu tư vấn và phần giới thiệu cuối trang.");
            add(sections,"seo","Tìm kiếm & chia sẻ","Tiêu đề, mô tả và ảnh khi chia sẻ đường dẫn trang chủ.");
        }
        for(SiteContent.Field field:SiteContent.fields(business)){
            String k=field.getKey(),id,group="";
            if(business){id=k.startsWith("business.promotion")?"promotion":k.startsWith("business.mesh")?"mesh":k.equals("business.standardFee")?"fees":"contact";
                if(k.startsWith("business.meshF1"))group="Cấu hình Mesh F1";else if(k.startsWith("business.meshF2"))group="Cấu hình Mesh F2";
            }else if(k.startsWith("hero.image")||k.equals("hero.background")){id="images";group=k.equals("hero.background")?"Ảnh nền (tùy chọn)":"Banner trượt";}
            else if(k.startsWith("hero."))id="hero";
            else if(k.startsWith("nav.")||k.startsWith("header.")){id="navigation";if(k.startsWith("nav.")){String item=k.split("\\.")[1];group=item.equals("home")?"Menu Trang chủ":item.equals("packages")?"Menu Bảng giá":item.equals("contact")?"Menu Liên hệ":"Menu Tư vấn";}}
            else if(k.startsWith("packages."))id="packages";
            else if(k.startsWith("benefit")){id="benefits";if(!k.startsWith("benefits."))group="Ô lợi ích "+k.substring(7,8);}
            else if(k.startsWith("device")){id="devices";if(!k.startsWith("devices."))group="Ô thiết bị "+k.substring(6,7);}
            else if(k.startsWith("seo."))id="seo";else id="footer";
            Section section=sections.get(id);Group g=section.groups.get(group);if(g==null){g=new Group(group);section.groups.put(group,g);}g.fields.add(field);
        }
        return new ArrayList<>(sections.values());
    }
    private static void add(Map<String,Section> map,String id,String title,String description){map.put(id,new Section(id,title,description));}
}
