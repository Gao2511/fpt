package dao;

import dto.*;
import rag.*;
import ai.consultation.ProductCatalog;
import java.sql.*;
import java.util.*;

/** Exact-vector + accent-normalized FTS retrieval in the same transaction/snapshot. */
public class KnowledgeChunkDAO {
    private final RagDatabase db;
    private final String kind;
    public KnowledgeChunkDAO(RagDatabase db){this(db,"MOCK");}
    public KnowledgeChunkDAO(RagDatabase db,String kind){if(!Arrays.asList("MOCK","GEMINI").contains(kind))throw new IllegalArgumentException();this.db=db;this.kind=kind;}
    // Explicit supersession follows older IDs only, so the reviewed lineage cannot cycle.
    private static final String LINEAGE="WITH RECURSIVE superseded(id,policy_key,authority) AS ("
        +"SELECT s.supersedes_document_id,s.policy_key,s.source_authority FROM public.knowledge_documents s WHERE s.supersedes_document_id IS NOT NULL AND s.status IN ('published','archived') AND s.approved_by>0 AND s.approved_at<=CURRENT_TIMESTAMP AND (s.effective_at IS NULL OR s.effective_at<=CURRENT_TIMESTAMP) AND (s.expires_at IS NULL OR s.approved_at<s.expires_at) "
        +"UNION SELECT p.supersedes_document_id,x.policy_key,x.authority FROM superseded x JOIN public.knowledge_documents p ON p.id=x.id WHERE p.supersedes_document_id IS NOT NULL AND p.policy_key=x.policy_key AND (x.authority='official' OR p.source_authority='admin_reviewed')) ";
    private static final String ELIGIBLE="d.status='published' AND d.approved_by>0 AND d.approved_at<=CURRENT_TIMESTAMP AND d.indexing_status='ready' AND d.indexed_version=d.version AND k.document_version=d.version AND k.document_hash=d.content_hash AND (d.effective_at IS NULL OR d.effective_at<=CURRENT_TIMESTAMP) AND (d.expires_at IS NULL OR d.expires_at>CURRENT_TIMESTAMP) AND k.embedding_model=d.embedding_model AND k.vector_dimensions=d.vector_dimensions AND NOT EXISTS(SELECT 1 FROM superseded x WHERE x.id=d.id AND x.policy_key=d.policy_key AND (x.authority='official' OR d.source_authority='admin_reviewed'))";
    public float[] cached(String hash,String model,int dimensions) throws Exception {
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT embedding::text FROM public.knowledge_chunks WHERE content_hash=? AND embedding_model=? AND vector_dimensions=? AND embedding_provider=? LIMIT 1")) {
            p.setString(1,hash);p.setString(2,model);p.setInt(3,dimensions);p.setString(4,kind);try(ResultSet r=p.executeQuery()){return r.next()?VectorValues.parse(r.getString(1)):null;}
        }
    }
    public void insert(Connection c,KnowledgeDocumentDTO d,int ordinal,String text,float[] vector,EmbeddingService service) throws Exception {
        if(!kind.equals(service.providerKind()))throw new RagException(RagException.Code.CONFIGURATION);
        String sql="INSERT INTO public.knowledge_chunks (document_id,document_version,document_hash,ordinal,content,content_hash,embedding_model,vector_dimensions,embedding,search_text,embedding_provider) VALUES (?,?,?,?,?,?,?,?,CAST(? AS vector),?,?)";
        try(PreparedStatement p=c.prepareStatement(sql)) {
            p.setLong(1,d.id);p.setLong(2,d.version);p.setString(3,d.contentHash);p.setInt(4,ordinal);p.setString(5,text);
            p.setString(6,VectorValues.hash(d.title+"\n"+text));p.setString(7,service.model());p.setInt(8,service.dimensions());p.setString(9,VectorValues.sql(vector));
            p.setString(10,ProductCatalog.normalize(d.title+" "+text));p.setString(11,kind);p.executeUpdate();
        }
    }
    public List<KnowledgeChunkDTO> search(String query,float[] vector,String model,int dimensions,int limit) throws Exception {
        String normalized=RetrievalQuery.normalize(query).replaceAll("(?<=\\d)\\s+(?=k\\b)","").replaceAll("[^a-z0-9 ]"," ").trim();
        if(normalized.isEmpty())return Collections.emptyList();
        Set<String> terms=new LinkedHashSet<>();
        Set<String> stop=new HashSet<>(Arrays.asList("toi","minh","anh","chi","em","co","khong","the","nao","gi","la","cho","cua","voi","va","fpt","internet","telecom","muon","biet","xin","hoi","ve","duoc","can","hay","tai","thi","neu","lam","shop","oi","qua","nha","tui","bro","sao","vay","nay","huong","dan"));
        for(String word:normalized.split("\\s+"))if(word.length()>1 && !stop.contains(word) && terms.size()<24)terms.add(word);
        if(terms.isEmpty())return Collections.emptyList();
        normalized=String.join(" | ",terms);
        String sim=vector==null?"0::double precision":"1-(k.embedding <=> CAST(? AS vector))";
        String sql=LINEAGE+", ranked AS (SELECT k.*,d.title,d.source,d.category,d.status,d.approved_by,d.approved_at,d.policy_key,d.source_authority,d.effective_at,d.expires_at,ts_rank_cd(k.search_vector,to_tsquery('simple',?)) AS lexical,"+sim+" AS semantic FROM public.knowledge_chunks k JOIN public.knowledge_documents d ON d.id=k.document_id WHERE "+ELIGIBLE+" AND k.embedding_provider=d.embedding_provider AND k.embedding_model=? AND k.vector_dimensions=?) "
            +"SELECT *, (CASE WHEN lexical>0 THEN 0.4+LEAST(lexical,1)*0.2 ELSE 0 END)+GREATEST(semantic,0)*0.4 AS score FROM ranked WHERE embedding_provider=? AND (lexical>0 OR semantic>=0.72) ORDER BY score DESC,document_id,id LIMIT ?";
        List<KnowledgeChunkDTO> result=new ArrayList<>();
        try(Connection c=db.open()) {
            c.setReadOnly(true);
            try(PreparedStatement p=c.prepareStatement(sql)) {
                int i=1;p.setString(i++,normalized);if(vector!=null)p.setString(i++,VectorValues.sql(vector));
                p.setString(i++,model);p.setInt(i++,dimensions);p.setString(i++,kind);p.setInt(i,Math.max(1,Math.min(limit,12)));
                try(ResultSet r=p.executeQuery()){while(r.next())result.add(new KnowledgeChunkDTO(r.getLong("id"),r.getLong("document_id"),r.getLong("document_version"),r.getString("document_hash"),r.getString("title"),r.getString("content"),r.getString("source"),RagDatabase.instant(r,"effective_at"),RagDatabase.instant(r,"expires_at"),r.getDouble("score"),new KnowledgeChunkDTO.SourceMetadata(r.getString("status"),r.getString("category"),r.getInt("approved_by"),RagDatabase.instant(r,"approved_at"),r.getString("policy_key"),r.getString("source_authority"),false)));}
            }
        }return result;
    }
    public boolean current(KnowledgeChunkDTO chunk) throws Exception {
        if(chunk.metadata==null)return false;
        // Recheck same-subject peers outside the initial top-12 result window as well.
        String peers="EXISTS(SELECT 1 FROM public.knowledge_documents peer WHERE peer.id<>d.id AND ((d.policy_key<>'' AND peer.policy_key=d.policy_key) OR (d.policy_key='' AND peer.policy_key='' AND lower(peer.title)=lower(d.title))) AND peer.status='published' AND peer.approved_by>0 AND peer.approved_at<=CURRENT_TIMESTAMP AND (peer.effective_at IS NULL OR peer.effective_at<=CURRENT_TIMESTAMP) AND (peer.expires_at IS NULL OR peer.expires_at>CURRENT_TIMESTAMP) AND NOT EXISTS(SELECT 1 FROM superseded x WHERE x.id=peer.id AND x.policy_key=peer.policy_key AND (x.authority='official' OR peer.source_authority='admin_reviewed')) AND ((peer.source_authority='official' AND d.source_authority='admin_reviewed') OR (peer.source_authority=d.source_authority AND peer.content IS DISTINCT FROM d.content)))";
        String sql=LINEAGE+"SELECT "+peers+" AS conflict FROM public.knowledge_chunks k JOIN public.knowledge_documents d ON d.id=k.document_id WHERE "+ELIGIBLE+" AND k.embedding_provider='"+kind+"' AND d.embedding_provider=k.embedding_provider AND k.id=? AND d.id=? AND d.version=? AND d.content_hash=? AND k.content=? AND d.source=? AND d.title=? AND d.policy_key=? AND d.source_authority=? AND d.approved_by=? AND d.approved_at=?";
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)) {
            p.setLong(1,chunk.id);p.setLong(2,chunk.documentId);p.setLong(3,chunk.documentVersion);p.setString(4,chunk.documentHash);p.setString(5,chunk.content);
            p.setString(6,chunk.source);p.setString(7,chunk.title);p.setString(8,chunk.metadata.policyKey);p.setString(9,chunk.metadata.authority);p.setInt(10,chunk.metadata.approvedBy);p.setTimestamp(11,RagDatabase.timestamp(chunk.metadata.approvedAt));
            try(ResultSet r=p.executeQuery()){if(!r.next())return false;if(r.getBoolean("conflict")){System.err.println("[RAG source revalidation] issue=SOURCE_CONFLICT document="+chunk.documentId);return false;}return true;}
        }
    }
}
