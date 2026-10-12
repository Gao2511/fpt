package rag;
import dto.KnowledgeChunkDTO;
import ai.consultation.ProductCatalog;
import java.util.*;
import java.time.Instant;

/** Similarity and recency cannot resolve contradictions; authority is trusted-admin metadata. */
public final class KnowledgeSourceSelection {
    public enum Issue {NONE,VERSION_CONFLICT,SOURCE_CONFLICT,AMBIGUOUS_SUBJECT,UNAVAILABLE}
    public static final class Result {
        public final List<KnowledgeChunkDTO> chunks;
        public final Issue issue;
        public final Map<KnowledgeContentPolicy.Reason,Integer> rejected;
        public Result(List<KnowledgeChunkDTO> chunks,Issue issue,Map<KnowledgeContentPolicy.Reason,Integer> rejected){this.chunks=Collections.unmodifiableList(new ArrayList<>(chunks));this.issue=issue;Map<KnowledgeContentPolicy.Reason,Integer> copy=new EnumMap<>(KnowledgeContentPolicy.Reason.class);copy.putAll(rejected);this.rejected=Collections.unmodifiableMap(copy);}
        public boolean uncertain(){return issue==Issue.VERSION_CONFLICT||issue==Issue.SOURCE_CONFLICT||issue==Issue.AMBIGUOUS_SUBJECT;}
    }
    private KnowledgeSourceSelection(){}
    public static Result empty(Issue issue){return new Result(Collections.emptyList(),issue,new EnumMap<>(KnowledgeContentPolicy.Reason.class));}
    private static String subject(KnowledgeChunkDTO c){return c.metadata.policyKey.isEmpty()?"title:"+ProductCatalog.normalize(c.title).trim():"key:"+c.metadata.policyKey;}
    private static int authority(KnowledgeChunkDTO c){return "official".equals(c.metadata.authority)?1:0;}
    // Accent folding is useful for search, but unsafe for evidence equality ("dừng" != "dùng").
    private static String evidence(String text){return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
    public static Result select(List<KnowledgeChunkDTO> candidates) {
        return select(candidates,null);
    }
    public static Result select(List<KnowledgeChunkDTO> candidates,String query) {
        Map<KnowledgeContentPolicy.Reason,Integer> rejected=new EnumMap<>(KnowledgeContentPolicy.Reason.class);
        Map<Long,List<KnowledgeChunkDTO>> documents=new TreeMap<>();Set<Long> ids=new HashSet<>();Instant now=Instant.now();
        for(KnowledgeChunkDTO c:candidates) {
            KnowledgeContentPolicy.Reason reason=KnowledgeContentPolicy.reason(c,now);
            if(reason!=KnowledgeContentPolicy.Reason.ACCEPTED){rejected.merge(reason,1,Integer::sum);continue;}
            List<KnowledgeChunkDTO> rows=documents.computeIfAbsent(c.documentId,k->new ArrayList<>());
            if(!rows.isEmpty()) {KnowledgeChunkDTO first=rows.get(0);if(first.documentVersion!=c.documentVersion || !first.documentHash.equals(c.documentHash) || !subject(first).equals(subject(c)) || authority(first)!=authority(c))return conflict(Issue.VERSION_CONFLICT,rejected);}
            if(ids.add(c.id))rows.add(c);else rejected.merge(KnowledgeContentPolicy.Reason.DUPLICATE,1,Integer::sum);
        }
        Map<String,List<List<KnowledgeChunkDTO>>> subjects=new TreeMap<>();
        for(List<KnowledgeChunkDTO> rows:documents.values())if(!rows.isEmpty()) {rows.sort(Comparator.comparingLong(c->c.id));subjects.computeIfAbsent(subject(rows.get(0)),k->new ArrayList<>()).add(rows);}
        List<KnowledgeChunkDTO> selected=new ArrayList<>();
        for(List<List<KnowledgeChunkDTO>> same:subjects.values()) {
            int best=same.stream().mapToInt(rows->authority(rows.get(0))).max().orElse(0);
            List<KnowledgeChunkDTO> winner=null;String fingerprint=null;
            for(List<KnowledgeChunkDTO> rows:same)if(authority(rows.get(0))==best) {
                Set<String> texts=new TreeSet<>();for(KnowledgeChunkDTO c:rows)texts.add(evidence(c.content));String fp=String.join("\n",texts);
                if(fingerprint!=null && !fingerprint.equals(fp))return conflict(Issue.SOURCE_CONFLICT,rejected);
                if(winner==null){winner=rows;fingerprint=fp;}else rejected.merge(KnowledgeContentPolicy.Reason.DUPLICATE,rows.size(),Integer::sum);
            }
            if(winner!=null)selected.addAll(winner);
        }
        // Resolve every authority/version/content conflict BEFORE considering query intent.
        // Titles are reviewed metadata, unlike generic words in excerpts or similarity alone.
        // The query already contains bounded customer-domain follow-up expansion.
        if(subjects.size()>1) {
            Set<String> intent=RetrievalQuery.topics(query);
            Set<String> matching=new HashSet<>();
            boolean classified=true;
            if(intent.size()==1)for(KnowledgeChunkDTO c:selected) {
                Set<String> domain=RetrievalQuery.topics(c.title);
                if(domain.size()!=1)classified=false;
                String wanted=intent.iterator().next();
                boolean compatible=domain.size()==1 && (domain.contains(wanted) ||
                    ("internet".equals(wanted) && domain.contains("wifi")));
                if(compatible)matching.add(subject(c));
            }
            if(!classified || matching.size()!=1)return conflict(Issue.AMBIGUOUS_SUBJECT,rejected);
            String chosen=matching.iterator().next();
            selected.removeIf(c->!chosen.equals(subject(c)));
        }
        selected.sort(Comparator.comparingDouble((KnowledgeChunkDTO c)->c.score).reversed().thenComparingLong(c->c.id));Set<String> content=new HashSet<>();List<KnowledgeChunkDTO> unique=new ArrayList<>();
        for(KnowledgeChunkDTO c:selected)if(content.add(evidence(c.content))){if(unique.size()<3)unique.add(c);}else rejected.merge(KnowledgeContentPolicy.Reason.DUPLICATE,1,Integer::sum);
        return new Result(unique,Issue.NONE,rejected);
    }
    private static Result conflict(Issue issue,Map<KnowledgeContentPolicy.Reason,Integer> rejected){System.err.println("[RAG source selection] issue="+issue);return new Result(Collections.emptyList(),issue,rejected);}
    public static String confirmation(){return "Dạ thông tin hướng dẫn hiện cần được xác nhận để tránh trả lời mâu thuẫn. Anh/chị vui lòng liên hệ tư vấn viên để kiểm tra trường hợp cụ thể nhé.";}
}
