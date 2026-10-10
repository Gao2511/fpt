package cms;
import dto.PackageDTO;
import java.util.*;
import java.net.URI;
/** Public allowlist: never returns raw operational settings or secrets. */
public final class SiteContent {
    public static final class Field {
        private final String key,label,type;
        Field(String key,String label,String type){this.key=key;this.label=label;this.type=type;}
        public String getKey(){return key;}public String getLabel(){return label;}public String getType(){return type;}
    }
    private static final Map<String,String> DEFAULTS=new LinkedHashMap<>();
    private static final List<Field> FIELDS=new ArrayList<>();
    private static void add(String k,String label,String type,String value){FIELDS.add(new Field(k,label,type));DEFAULTS.put(k,value);}
    static {
        add("hero.title","Hero: dòng tiêu đề","text","TỐC ĐỘ CAO");add("hero.subtitle","Hero: dòng phụ","text","HỖ TRỢ 24/7");add("hero.description","Hero: mô tả bổ sung","text","");add("hero.visible","Hiện Hero","boolean","true");
        add("hero.cta","Nút bảng giá","text","Xem bảng giá ngay →");add("hero.ctaUrl","Đích nút bảng giá","url","#packages");add("hero.contactCta","Nút tư vấn","text","Tư vấn miễn phí");add("hero.contactUrl","Đích nút tư vấn","url","#contact");
        for(int i=1;i<=7;i++)add("hero.image"+i,"Banner "+i,"image","/assets/images/Hero"+i+".png");
        add("hero.background","Ảnh nền Hero (tùy chọn)","image","");
        add("hero.label","Nhãn Hero (tùy chọn)","text","");
        add("header.logo","Logo","image","/assets/images/fpt-logo.jpg");
        for(String[] nav:new String[][]{{"home","Trang chủ"},{"packages","Bảng giá"},{"contact","Liên hệ"},{"consultation","Tư vấn"}}){add("nav."+nav[0]+".label","Menu "+nav[1],"text",nav[1]);add("nav."+nav[0]+".visible","Hiện menu "+nav[1],"boolean","true");add("nav."+nav[0]+".order","Thứ tự menu "+nav[1],"number",String.valueOf(FIELDS.size()));}
        add("packages.title","Tiêu đề bảng giá","text","GÓI CƯỚC WIFI FPT TỐC ĐỘ CAO BÁN CHẠY NHẤT");
        for(String nav:new String[]{"home","packages","contact","consultation"})add("nav."+nav+".url","Đích menu "+nav,"url",nav.equals("packages")?"/home#packages":nav.equals("consultation")?"/home#contact":"/"+nav);
        add("benefits.title","Tiêu đề lợi ích","text","Cam Kết Chất Lượng Dịch Vụ Hàng Đầu FPT");add("benefits.visible","Hiện lợi ích","boolean","true");
        String[] titles={"CÔNG NGHỆ WIFI 6","TỐC ĐỘ CAO 1GBPS","LẮP ĐẶT TRONG 24H","HỖ TRỢ 24/7 TẬN TÂM"};
        String[] benefitDescriptions={"Kết nối không giới hạn, cho phép nhiều thiết bị cùng lúc mà không bị giật lag.","Đường truyền siêu tốc, đáp ứng mọi nhu cầu giải trí, làm việc và học tập online.","Kỹ thuật viên có mặt nhanh chóng, lắp đặt và kích hoạt dịch vụ trong ngày.","Tổng đài hỗ trợ hoạt động 24/7, giải đáp mọi thắc mắc của bạn mọi lúc mọi nơi."};
        for(int i=1;i<=4;i++){add("benefit"+i+".title","Lợi ích "+i+": tiêu đề","text",titles[i-1]);add("benefit"+i+".description","Lợi ích "+i+": mô tả","text",benefitDescriptions[i-1]);add("benefit"+i+".visible","Hiện lợi ích "+i,"boolean","true");add("benefit"+i+".order","Thứ tự lợi ích "+i,"number",String.valueOf(i));}
        add("devices.title","Tiêu đề thiết bị","text","Thiết Bị Đi Kèm Chính Hãng FPT");add("devices.visible","Hiện thiết bị","boolean","true");
        for(int i=1;i<=4;i++)add("benefit"+i+".image","Lợi ích "+i+": ảnh thay icon (tùy chọn)","image","");
        String[] images={"Wifi_6.png","FPT-Play-Box.png","Camera-FPT-Play.png","Ngoai_Hang_Anh.png"},names={"Modem WiFi 6","FPT Play Box 4K Voice","Camera AI Giám Sát 24/7","Tài Khoản FPT Play Vip"};
        String[] deviceDescriptions={"Modem WiFi 6 tốc độ cao, hỗ trợ nhiều thiết bị cùng lúc không giật lag.","Đầu thu FPT Play 4K, điều khiển bằng giọng nói, xem hàng trăm kênh truyền hình.","Camera AI thông minh, giám sát an ninh 24/7, xem mọi lúc mọi nơi qua điện thoại.","Xem trọn vẹn Ngoại hạng Anh và nhiều giải đấu hấp dẫn khác với chất lượng 4K."};
        for(int i=1;i<=4;i++){add("device"+i+".title","Thiết bị "+i+": tiêu đề","text",names[i-1]);add("device"+i+".description","Thiết bị "+i+": mô tả","text",deviceDescriptions[i-1]);add("device"+i+".image","Thiết bị "+i+": ảnh","image","/assets/images/"+images[i-1]);add("device"+i+".visible","Hiện thiết bị "+i,"boolean","true");add("device"+i+".order","Thứ tự thiết bị "+i,"number",String.valueOf(i));}
        add("contact.title","Tiêu đề form tư vấn","text","Liên hệ tư vấn khảo sát và nhận ưu đãi có hạn");add("footer.description","Mô tả footer","text","Công ty Cổ phần Viễn thông FPT — Nhà cung cấp dịch vụ Internet tốc độ cao.");
        add("seo.title","SEO: tiêu đề trang chủ","text","FPT Telecom - Internet, Truyền hình, Camera");add("seo.description","SEO: mô tả","text","");add("seo.socialTitle","Chia sẻ: tiêu đề","text","");add("seo.socialDescription","Chia sẻ: mô tả","text","");add("seo.image","Chia sẻ: ảnh","image","/assets/images/Hero1.png");
        add("business.phone","Hotline","phone","0932079469");add("business.email","Email công khai","email","PhucBN2@fpt.com");add("business.address","Địa chỉ","text","94 Phạm Hùng, Quy Nhơn, Gia Lai.");add("business.zalo","Zalo","url","https://zalo.me/0932079469");add("business.social","Liên kết mạng xã hội","url","");add("business.standardFee","Phí Internet thông thường / lần","number","300000");
        add("business.meshTitle","Tiêu đề Mesh","text","MỞ RỘNG PHỦ SÓNG TOÀN DIỆN VỚI MESH WIFI FPT");add("business.meshVisible","Hiện Mesh","boolean","true");
        for(int i=1;i<=2;i++){add("business.meshF"+i+"Fee","Mesh F"+i+": phí lắp","number",i==1?"500000":"700000");add("business.meshF"+i+"Monthly","Mesh F"+i+": phụ thu tháng","number",i==1?"10000":"20000");add("business.meshF"+i+"Equipment","Mesh F"+i+": thiết bị","text","1 Modem WiFi 6 + "+i+" Access Point");}
        add("business.promotionTitle","Nội dung ưu đãi hiện có: tiêu đề","text","");add("business.promotionDescription","Nội dung ưu đãi: mô tả / điều kiện","text","");add("business.promotionStart","Hiệu lực từ (YYYY-MM-DD)","date","");add("business.promotionEnd","Hiệu lực đến (YYYY-MM-DD)","date","");add("business.promotionActive","Hiện nội dung ưu đãi","boolean","false");add("business.promotionPackageIds","ID gói áp dụng, cách nhau bởi dấu phẩy","ids","");
    }
    private SiteContent(){}
    public static Map<String,String> defaults(){return new LinkedHashMap<>(DEFAULTS);}
    public static List<Field> fields(boolean business){List<Field> result=new ArrayList<>();for(Field f:FIELDS)if(f.key.startsWith("business.")==business)result.add(f);return result;}
    public static Map<String,String> from(Map<String,String> raw){Map<String,String> result=defaults();for(Field f:FIELDS){String v=raw.get("cms."+f.key);if(v!=null)try{validate(f,v);result.put(f.key,v);}catch(IllegalArgumentException ignored){}}return result;}
    public static Map<String,String> load(){return from(new dao.SettingsDAO().getCms());}
    public static String validate(Field f,String v){if(v==null)v="";v=v.trim();if(v.length()>2000||v.indexOf('\0')>=0||v.contains("<")||v.contains(">"))throw new IllegalArgumentException("Nội dung không hợp lệ: "+f.label);
        if(f.key.equals("hero.title")||f.key.equals("hero.subtitle")){if(v.isEmpty()||v.length()>30)throw new IllegalArgumentException("Tiêu đề Hero tối đa 30 ký tự");}
        if(f.type.equals("boolean")&&!v.matches("true|false"))throw new IllegalArgumentException("Giá trị hiển thị không hợp lệ");
        if(f.type.equals("number")){try{long n=Long.parseLong(v);if(n<0||n>100000000)throw new Exception();}catch(Exception e){throw new IllegalArgumentException("Số không hợp lệ: "+f.label);}}
        if(f.type.equals("phone")&&!v.matches("0[0-9]{9,10}"))throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        if(f.type.equals("email")&&!v.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"))throw new IllegalArgumentException("Email không hợp lệ");
        if(f.type.equals("date")&&!v.isEmpty())try{java.time.LocalDate.parse(v);}catch(Exception e){throw new IllegalArgumentException("Ngày không hợp lệ");}
        if(f.type.equals("ids")&&!v.matches("(?:[1-9][0-9]*(?:,[1-9][0-9]*)*)?"))throw new IllegalArgumentException("Danh sách ID không hợp lệ");
        if(f.type.equals("url")||f.type.equals("image"))safeUrl(v,f.type.equals("image"));return v;
    }
    public static void safeUrl(String value,boolean image){if(value.isEmpty())return;try{URI u=new URI(value);if(u.getUserInfo()!=null||value.contains("\\")||value.contains("\"")||value.contains("'")||value.contains("..")||value.contains("%")||value.contains("\n")||value.contains("\r"))throw new Exception();if("https".equals(u.getScheme())&&u.getHost()!=null){if(u.getHost().equalsIgnoreCase("localhost")||u.getHost().contains(":")||u.getHost().matches("[0-9.]+"))throw new Exception();return;}if(u.getScheme()!=null||u.getAuthority()!=null)throw new Exception();if(image ? value.matches("/(?:assets/images/[A-Za-z0-9_./-]+\\.(?:png|jpg|jpeg|webp)|media\\?id=[a-f0-9-]{36})") : value.matches("(?:#(?:packages|contact|mesh-wifi|devices|contact-info)|/(?:home|contact|package-detail)(?:[?#][A-Za-z0-9_=&?#.-]*)?)"))return;throw new Exception();}catch(Exception e){throw new IllegalArgumentException("Liên kết không hợp lệ");}}
    public static boolean promotionVisible(Map<String,String> v){if(!"true".equals(v.get("business.promotionActive"))||v.get("business.promotionTitle").isEmpty())return false;java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));String start=v.get("business.promotionStart"),end=v.get("business.promotionEnd");return (start.isEmpty()||!today.isBefore(java.time.LocalDate.parse(start)))&&(end.isEmpty()||!today.isAfter(java.time.LocalDate.parse(end)));}
    public static List<PackageDTO> products(List<PackageDTO> rows,Map<String,String> raw,Map<String,String> values,boolean onlyActive){List<PackageDTO> result=new ArrayList<>();for(PackageDTO p:rows){String k="cms.package."+p.getId()+".";p.setActive(!"false".equals(raw.get(k+"active")));try{p.setDisplayOrder(Integer.parseInt(raw.getOrDefault(k+"order","0")));long fee=Long.parseLong(raw.getOrDefault(k+"fee",values.get("business.standardFee")));if(fee>=0&&fee<=100000000&&(raw.containsKey(k+"fee")||"true".equals(raw.get(k+"standard"))||p.getStandardInstallationFee()>0))p.setStandardInstallationFee(fee);}catch(Exception ignored){}p.setInstallationFeeInherited(!raw.containsKey(k+"fee"));if(!onlyActive||p.isActive())result.add(p);}result.sort(Comparator.comparingInt(PackageDTO::getDisplayOrder).thenComparingLong(PackageDTO::getPrice));return result;}
    public static List<PackageDTO> publicPackages(){Map<String,String> raw=new dao.SettingsDAO().getCms();return products(new dao.PackageDAO().getAll(),raw,from(raw),true);}
    public static ai.consultation.ProductCatalog publicCatalog(){Map<String,String> raw=new dao.SettingsDAO().getCms();Map<String,String> values=from(raw);return new ai.consultation.ProductCatalog(products(new dao.PackageDAO().getAll(),raw,values,true),values);}
    public static String amount(Map<String,String> values,String key){return String.format(java.util.Locale.US,"%,d",Long.parseLong(values.get(key))).replace(',','.');}
}
