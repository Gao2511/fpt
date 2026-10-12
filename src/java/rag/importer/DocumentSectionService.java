package rag.importer;

import rag.*;
import ai.consultation.ProductCatalog;
import java.util.*;
import java.util.regex.*;

/** Local suggestions only. Text offsets are UTF-16 offsets in the immutable extracted revision. */
public final class DocumentSectionService {
    public enum Kind {AI_BEHAVIOR,KNOWLEDGE_BASE,FAQ,ADMIN_CONFIGURATION,MANUAL_REVIEW}
    public static final class Section {
        public final int ordinal,start,end,startLine,endLine;
        public final String title,content,hash;
        public final Kind kind;
        Section(int ordinal,int start,int end,int startLine,int endLine,String title,String text){
            this.ordinal=ordinal;this.start=start;this.end=end;this.startLine=startLine;this.endLine=endLine;
            this.title=title;content=text.substring(start,end);hash=VectorValues.hash(content);kind=classify(title,content);
        }
        public int getOrdinal(){return ordinal;} public int getStartLine(){return startLine;} public int getEndLine(){return endLine;}
        public String getTitle(){return title;} public String getContent(){return content;} public String getKind(){return kind.name();}
    }
    private static final Pattern HEADING=Pattern.compile("(?im)^(?:#{1,6}\\s+.+|(?:PHẦN|MỤC|CHƯƠNG)\\s+[^\\r\\n]+|[0-9]{1,2}[.)]\\s+[^\\r\\n]+|[IVX]{1,6}[.)]\\s+[^\\r\\n]+)$");
    public List<Section> detect(String text){
        if(text==null||text.trim().isEmpty()||text.length()>KnowledgeLimits.extracted())throw new IllegalArgumentException("Văn bản trích xuất không hợp lệ.");
        List<Integer> offsets=new ArrayList<>();offsets.add(0);Matcher m=HEADING.matcher(text);
        while(m.find())if(m.start()>0)offsets.add(m.start());
        if(offsets.size()>200)throw new IllegalArgumentException("Có quá nhiều mục; cần chia tệp trước khi nhập.");
        List<Section> out=new ArrayList<>();
        for(int i=0;i<offsets.size();i++){
            int a=offsets.get(i),b=i+1<offsets.size()?offsets.get(i+1):text.length();
            String body=text.substring(a,b);String title=body.split("\\r?\\n",2)[0].trim();
            if(title.length()>200)title=title.substring(0,200);
            if(title.isEmpty())title="Mục "+(i+1);
            out.add(new Section(i+1,a,b,line(text,a),line(text,Math.max(a,b-1)),title,text));
        }
        return Collections.unmodifiableList(out);
    }
    private static int line(String text,int end){int n=1;for(int i=0;i<end;i++)if(text.charAt(i)=='\n')n++;return n;}
    private static Kind classify(String title,String content){
        String q=ProductCatalog.normalize(title+"\n"+content);
        if(q.matches("(?s).*\\b(system|prompt|assistant|bo qua|quy tac|xung ho|vai tro|phong cach|quy trinh tu van|bat buoc|chi dan)\\b.*"))return Kind.AI_BEHAVIOR;
        if(q.matches("(?s).*\\b(api|model|cau hinh|hotline|zalo|gio lam viec|nguoi phu trach|khu vuc phuc vu)\\b.*"))return Kind.ADMIN_CONFIGURATION;
        if(KnowledgeContentPolicy.contentReason(title,content)!=KnowledgeContentPolicy.Reason.ACCEPTED || !singleTopic(title))return Kind.MANUAL_REVIEW;
        return q.matches("(?s).*\\b(faq|cau hoi|hoi dap)\\b.*")||title.contains("?")?Kind.FAQ:Kind.KNOWLEDGE_BASE;
    }
    public static boolean singleTopic(String title){return RetrievalQuery.subjectTopics(title).size()==1;}
    public static void validateEntry(String title,String content)throws RagException{
        if(KnowledgeContentPolicy.contentReason(title,content)!=KnowledgeContentPolicy.Reason.ACCEPTED)throw new IllegalArgumentException("Nội dung chưa đạt source policy; giữ hàng chờ duyệt.");
        Kind suggested=classify(title,content);
        if(suggested==Kind.AI_BEHAVIOR||suggested==Kind.ADMIN_CONFIGURATION)throw new IllegalArgumentException("Quy tắc vận hành/cấu hình không được chuyển thành tri thức trả lời tự động.");
        if(!singleTopic(title))throw new IllegalArgumentException("Tiêu đề phải xác định đúng một chủ đề; hãy tách mục hoặc sửa tiêu đề trước khi tạo entry.");
        // Multi-domain body stays for manual splitting; never drop instructions to make a chunk pass.
        Set<String> domains=new HashSet<>(RetrievalQuery.subjectTopics(content));domains.remove("internet");
        if(domains.size()>1)throw new IllegalArgumentException("Nội dung chứa nhiều chủ đề, cần tách riêng và duyệt lại.");
        for(String part:new DocumentChunkingService().chunk(content))if(KnowledgeContentPolicy.contentReason(title,part)!=KnowledgeContentPolicy.Reason.ACCEPTED)
            throw new IllegalArgumentException("Nội dung chưa đạt source policy (giá cước hoặc chỉ dẫn không an toàn); giữ hàng chờ duyệt.");
    }
}
