package rag.files;

import java.time.*;
import java.util.*;
import rag.*;

/** Deliberately small scalar frontmatter grammar; no YAML tags, aliases or arbitrary configuration. */
public final class KnowledgeFile {
    public final String id,title,category,authority,path,sha,body,replaces;
    public final long version; public final boolean active;
    public final Instant effective,expires; public final int chunkSize,overlap;
    private KnowledgeFile(Map<String,String> m,String path,String sha,String body)throws RagException {
        id=required(m,"id");title=required(m,"title");category=required(m,"category");authority=required(m,"source_authority");
        this.path=path;this.sha=sha;this.body=body;replaces=m.getOrDefault("replaces","");
        if(path==null||path.length()>300)throw new IllegalArgumentException("Đường dẫn file quá dài");
        version=Long.parseLong(required(m,"version"));String flag=required(m,"active");
        if(!Arrays.asList("true","false").contains(flag))throw new IllegalArgumentException("active phải là true/false");active=Boolean.parseBoolean(flag);
        effective=date(required(m,"effective_date"));expires=date(required(m,"expiration_date"));
        chunkSize=Integer.parseInt(m.getOrDefault("chunk_size","400"));overlap=Integer.parseInt(m.getOrDefault("overlap","40"));
        if(!id.matches("[a-z0-9][a-z0-9._-]{0,79}")||(!replaces.isEmpty()&&!replaces.matches("[a-z0-9][a-z0-9._-]{0,79}"))||replaces.equals(id))throw new IllegalArgumentException("ID/replaces không hợp lệ");
        if(version<1||title.length()>200||!Arrays.asList("guide","faq","policy").contains(category)||!Arrays.asList("admin_reviewed","official").contains(authority))throw new IllegalArgumentException("Metadata không hợp lệ");
        if(expires!=null&&effective!=null&&!expires.isAfter(effective))throw new IllegalArgumentException("Ngày hết hạn phải sau ngày hiệu lực");
        if(body.trim().isEmpty()||body.length()>KnowledgeLimits.extracted()||chunkSize<200||chunkSize>1200||overlap<0||overlap>400||overlap>=chunkSize/2)throw new IllegalArgumentException("Nội dung/chunk vượt giới hạn");
        // Only official repository files may carry commercial facts. Unsafe instructions remain blocked.
        rag.importer.DocumentSectionService.validateEntry(title,body,"official".equals(authority));
    }
    public static KnowledgeFile parse(String raw,String path,String sha)throws RagException {
        String text=raw.startsWith("\uFEFF")?raw.substring(1):raw;text=text.replace("\r\n","\n");
        if(!text.startsWith("---\n"))throw new IllegalArgumentException("Thiếu frontmatter");
        int end=text.indexOf("\n---\n",4);if(end<0||end>4096)throw new IllegalArgumentException("Frontmatter không hợp lệ");
        if(text.matches("(?s).*(?:AIza[0-9A-Za-z_-]{35}|-----BEGIN [A-Z ]*PRIVATE KEY-----|postgres(?:ql)?://[^\\s]+:[^\\s]+@|gh[pousr]_[0-9A-Za-z]{30,}).*"))throw new IllegalArgumentException("File chứa dạng credential, không được đưa vào tri thức");
        if(java.util.regex.Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b|(?<![0-9])(?:\\+84|0)(?:[ .-]?[0-9]){9,10}(?![0-9])").matcher(text).find())throw new IllegalArgumentException("Không nhập email/số điện thoại vào tri thức; giữ thông tin liên hệ trong cấu hình được duyệt");
        Set<String> keys=new HashSet<>(Arrays.asList("id","title","category","version","effective_date","expiration_date","active","source_authority","chunk_size","overlap","replaces"));
        Map<String,String> m=new LinkedHashMap<>();for(String line:text.substring(4,end).split("\n")){
            int colon=line.indexOf(':');if(colon<=0)throw new IllegalArgumentException("Frontmatter chỉ nhận key: value");
            String key=line.substring(0,colon).trim(),value=line.substring(colon+1).trim();
            if(!keys.contains(key)||m.containsKey(key))throw new IllegalArgumentException("Field trùng/không hỗ trợ");
            if(value.startsWith("\"")&&value.endsWith("\"")&&value.length()>=2)value=value.substring(1,value.length()-1);
            if(value.contains("\n")||value.indexOf('\0')>=0)throw new IllegalArgumentException("Giá trị không hợp lệ");m.put(key,value);
        }
        return new KnowledgeFile(m,path,sha,text.substring(end+5));
    }
    private static String required(Map<String,String> m,String k){String v=m.get(k);if(v==null||v.isEmpty())throw new IllegalArgumentException("Thiếu field "+k);return v;}
    private static Instant date(String v){return "none".equals(v)?null:LocalDate.parse(v).atStartOfDay().toInstant(ZoneOffset.UTC);}
    public String source(){return "repo:"+id+"/v"+version;}
    public boolean effectiveNow(){Instant now=Instant.now();return active&&(expires==null||now.isBefore(expires));}
}
