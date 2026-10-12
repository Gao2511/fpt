package dao;

import dto.KnowledgeDocumentDTO;
import rag.*;
import java.sql.*;
import java.util.*;

/** Curated-document CRUD. Caller supplies a verified admin actor; no public HTTP write endpoint. */
public class KnowledgeDocumentDAO {
    private final RagDatabase db;
    private final RagConfig config;
    public KnowledgeDocumentDAO(RagDatabase db,RagConfig config) { this.db=db;this.config=config; }
    public KnowledgeDocumentDTO find(long id) throws Exception {
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_documents WHERE id=?")) {
            p.setLong(1,id);try(ResultSet r=p.executeQuery()) { return r.next()?read(r):null; }
        }
    }
    public List<KnowledgeDocumentDTO> list(int limit) throws Exception {
        List<KnowledgeDocumentDTO> result=new ArrayList<>();
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_documents ORDER BY id DESC LIMIT ?")) {
            p.setInt(1,Math.max(1,Math.min(limit,100)));try(ResultSet r=p.executeQuery()) { while(r.next())result.add(read(r)); }
        } return result;
    }
    public KnowledgeDocumentDTO save(KnowledgeDocumentDTO d,int actorId) throws Exception {
        validate(d,actorId);
        try(Connection c=db.open()) {
            c.setAutoCommit(false);
            try {
                validateSupersession(c,d);
                boolean insert=d.id==0;
                String fields="title=?,category=?,content=?,source=?,status=?,content_hash=?,effective_at=?,expires_at=?,approved_by=?,approved_at=?,embedding_model=?,vector_dimensions=?,policy_key=?,source_authority=?,supersedes_document_id=?";
                String sql=insert ? "INSERT INTO public.knowledge_documents (title,category,content,source,status,content_hash,effective_at,expires_at,approved_by,approved_at,embedding_model,vector_dimensions,policy_key,source_authority,supersedes_document_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING *"
                    : "UPDATE public.knowledge_documents SET "+fields+",version=version+1,indexing_status='pending',indexed_version=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=? RETURNING *";
                KnowledgeDocumentDTO saved;
                try(PreparedStatement p=c.prepareStatement(sql)) {
                    p.setString(1,d.title.trim());p.setString(2,d.category);p.setString(3,d.content);p.setString(4,d.source.trim());
                    p.setString(5,d.status);p.setString(6,d.contentHash);p.setTimestamp(7,RagDatabase.timestamp(d.effectiveAt));p.setTimestamp(8,RagDatabase.timestamp(d.expiresAt));
                    if("published".equals(d.status)) {p.setInt(9,actorId);p.setTimestamp(10,Timestamp.from(java.time.Instant.now()));}
                    else {
                        p.setNull(9,Types.INTEGER);p.setNull(10,Types.TIMESTAMP);
                        // Archive retains the prior publication proof for reviewed replacement lineage.
                        if("archived".equals(d.status) && d.id>0)try(PreparedStatement old=c.prepareStatement("SELECT approved_by,approved_at FROM public.knowledge_documents WHERE id=? AND version=?")) {
                            old.setLong(1,d.id);old.setLong(2,d.version);try(ResultSet prior=old.executeQuery()){if(prior.next() && prior.getObject(1)!=null && prior.getTimestamp(2)!=null){p.setInt(9,prior.getInt(1));p.setTimestamp(10,prior.getTimestamp(2));}}
                        }
                    }
                    p.setString(11,config.model);p.setInt(12,config.dimensions);
                    p.setString(13,d.policyKey);p.setString(14,d.sourceAuthority);
                    if(d.supersedesDocumentId==null)p.setNull(15,Types.BIGINT);else p.setLong(15,d.supersedesDocumentId);
                    if(!insert){p.setLong(16,d.id);p.setLong(17,d.version);}
                    try(ResultSet r=p.executeQuery()) {
                        if(!r.next())throw new RagException(RagException.Code.STALE_VERSION); saved=read(r);
                    }
                }
                if("published".equals(saved.status)) KnowledgeIndexJobDAO.enqueue(c,saved,config);
                c.commit(); return saved;
            } catch(Exception e) {c.rollback();throw e;}
        }
    }
    /** Archive rather than destructive delete. Old chunks immediately become ineligible. */
    public KnowledgeDocumentDTO archive(long id,long version,int actor) throws Exception {
        KnowledgeDocumentDTO d=find(id);
        if(d==null || d.version!=version) throw new RagException(RagException.Code.STALE_VERSION);
        return save(new KnowledgeDocumentDTO(d.id,d.version,d.title,d.category,d.content,d.source,"archived",d.effectiveAt,d.expiresAt,"pending",d.policyKey,d.sourceAuthority,d.supersedesDocumentId),actor);
    }
    private static void validateSupersession(Connection c,KnowledgeDocumentDTO d)throws Exception {
        if(d.supersedesDocumentId==null)return;
        if(d.policyKey.isEmpty() || d.supersedesDocumentId<=0 || (d.id>0 && d.supersedesDocumentId>=d.id))throw new RagException(RagException.Code.INVALID_DOCUMENT);
        try(PreparedStatement p=c.prepareStatement("SELECT policy_key,source_authority,approved_by,approved_at FROM public.knowledge_documents WHERE id=? FOR SHARE")) {
            p.setLong(1,d.supersedesDocumentId);try(ResultSet r=p.executeQuery()) {
                if(!r.next() || !d.policyKey.equals(r.getString(1)) || ("official".equals(r.getString(2)) && !"official".equals(d.sourceAuthority)) || r.getObject(3)==null || r.getTimestamp(4)==null)
                    throw new RagException(RagException.Code.INVALID_DOCUMENT);
            }
        }
    }
    private static void validate(KnowledgeDocumentDTO d,int actor) throws RagException {
        if(d==null || actor<=0 || d.id<0 || d.version<0 || (d.id>0 && d.version==0) ||
            d.title==null || d.title.trim().isEmpty() || d.title.length()>200 || !d.title.equals(d.title.trim()) ||
            d.content==null || d.content.trim().isEmpty() || d.content.length()>300000 ||
            d.source==null || d.source.trim().isEmpty() || d.source.length()>500 || !d.source.equals(d.source.trim()) ||
            !Arrays.asList("faq","policy","guide").contains(d.category) || !Arrays.asList("draft","published","archived").contains(d.status) ||
            d.policyKey==null || !(d.policyKey.isEmpty() || d.policyKey.matches("[a-z0-9][a-z0-9._-]{0,99}")) ||
            !Arrays.asList("admin_reviewed","official").contains(d.sourceAuthority) ||
            (d.expiresAt!=null && d.effectiveAt!=null && !d.expiresAt.isAfter(d.effectiveAt))) throw new RagException(RagException.Code.INVALID_DOCUMENT);
    }
    public static KnowledgeDocumentDTO read(ResultSet r) throws SQLException {
        Long supersedes=(Long)r.getObject("supersedes_document_id");
        return new KnowledgeDocumentDTO(r.getLong("id"),r.getLong("version"),r.getString("title"),r.getString("category"),r.getString("content"),r.getString("source"),r.getString("status"),RagDatabase.instant(r,"effective_at"),RagDatabase.instant(r,"expires_at"),r.getString("indexing_status"),r.getString("policy_key"),r.getString("source_authority"),supersedes,r.getInt("approved_by"),RagDatabase.instant(r,"approved_at"));
    }
}
