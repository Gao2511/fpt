package dao;

import dto.AdminKnowledgeDTO.ManagedDocument;
import dto.KnowledgeDocumentDTO;
import rag.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Stages unpublished vectors; Admin approval and a complete index activate a revision atomically. */
public final class KnowledgeLifecycleDAO {
    private final RagDatabase db;
    public KnowledgeLifecycleDAO() { this(new RagDatabase()); }
    public KnowledgeLifecycleDAO(RagDatabase db) { this.db=db; }
    private static void actor(int actor) { if(actor<=0)throw new IllegalArgumentException("Yêu cầu Admin hợp lệ."); }
    public boolean available() {
        try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT to_regclass('public.knowledge_publish_jobs') IS NOT NULL")){return r.next()&&r.getBoolean(1);}catch(Exception e){return false;}
    }
    public static List<String> safeChunks(ManagedDocument d) throws Exception {
        // Full-body preflight catches instructions split across boundaries. Do not promote upload rules.
        if(KnowledgeContentPolicy.contentReason(d.title,d.content)!=KnowledgeContentPolicy.Reason.ACCEPTED)throw new IllegalArgumentException("Nội dung chứa chỉ dẫn hoặc thông tin kinh doanh không được phép nhập vào RAG.");
        rag.importer.DocumentSectionService.validateEntry(d.title,d.content);
        int safeSize=Math.min(d.chunkSize,(2048-d.title.getBytes(java.nio.charset.StandardCharsets.UTF_8).length-64)/4);
        if(safeSize<200)throw new RagException(RagException.Code.INVALID_DOCUMENT);
        return new DocumentChunkingService().chunk(d.content,safeSize,Math.min(d.chunkOverlap,safeSize/4));
    }
    public void linkReplacement(long id,long version,long oldId,int admin) throws Exception {
        actor(admin);
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            ManagedDocument old=new AdminKnowledgeDAO(db).findDocument(oldId);
            if(old==null || oldId>=id || old.policyKey.isEmpty() || old.approvedBy<=0)throw new IllegalArgumentException("Bản thay thế cần tài liệu nguồn đã duyệt với Policy Key.");
            KnowledgeDocumentDTO d;
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_documents WHERE id=? AND version=? AND workflow_status='DRAFT' FOR UPDATE")){
                p.setLong(1,id);p.setLong(2,version);try(ResultSet r=p.executeQuery()){if(!r.next())throw new RagException(RagException.Code.STALE_VERSION);d=KnowledgeDocumentDAO.read(r);}
            }
            KnowledgeDocumentDTO linked=new KnowledgeDocumentDTO(d.id,d.version,d.title,d.category,d.content,d.source,d.status,d.effectiveAt,d.expiresAt,d.indexingStatus,old.policyKey,old.sourceAuthority,oldId);
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET supersedes_document_id=?,policy_key=?,source_authority=?,content_hash=? WHERE id=?")){
                p.setLong(1,oldId);p.setString(2,old.policyKey);p.setString(3,old.sourceAuthority);p.setString(4,linked.contentHash);p.setLong(5,id);p.executeUpdate();
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_document_revisions SET supersedes_document_id=?,policy_key=?,source_authority=?,content_hash=? WHERE document_id=? AND version_number=?")){
                p.setLong(1,oldId);p.setString(2,old.policyKey);p.setString(3,old.sourceAuthority);p.setString(4,linked.contentHash);p.setLong(5,id);p.setLong(6,version);p.executeUpdate();
            }
            c.commit();
        }catch(Exception e){c.rollback();throw e;}}
    }
    /** The button authorizes content approval, never a system instruction/configuration change. */
    public long publish(long id,long version,int admin) throws Exception {
        return queue(id,version,admin,false);
    }
    public long queueReindex(long id,long version,int admin) throws Exception {
        return queue(id,version,admin,true);
    }
    private long queue(long id,long version,int admin,boolean reindex) throws Exception {
        actor(admin);
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            ManagedDocument d=lock(c,id);
            if(d.version!=version)throw new RagException(RagException.Code.STALE_VERSION);
            try(PreparedStatement p=c.prepareStatement("SELECT id FROM public.knowledge_publish_jobs WHERE document_id=? AND document_version=?")){
                p.setLong(1,id);p.setLong(2,version);try(ResultSet r=p.executeQuery()){if(r.next()){c.commit();return r.getLong(1);}}
            }
            if(!Arrays.asList("DRAFT","APPROVED","PENDING_REVIEW","UNPUBLISHED","REJECTED").contains(d.workflowStatus) && !(reindex&&"PUBLISHED".equals(d.workflowStatus)))throw new IllegalArgumentException("Hãy tạo bản nháp mới trước khi xuất bản.");
            if(d.expiresAt!=null && !Instant.now().isBefore(d.expiresAt))throw new IllegalArgumentException("Tài liệu đã hết hiệu lực.");
            List<String> parts=safeChunks(d);
            ManagedDocument old=null;
            if(d.supersedesDocumentId!=null){
                old=lock(c,d.supersedesDocumentId);
                if(old.id>=d.id || d.policyKey.isEmpty() || !d.policyKey.equals(old.policyKey) || ("official".equals(old.sourceAuthority)&&!"official".equals(d.sourceAuthority)))throw new IllegalArgumentException("Nguồn thay thế/authority không hợp lệ.");
            }
            long job;
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_publish_jobs(document_id,document_version,document_hash,replacement_id,replacement_version,replacement_hash,actor_id) VALUES(?,?,?,?,?,?,?) RETURNING id")){
                p.setLong(1,id);p.setLong(2,version);p.setString(3,d.contentHash);p.setObject(4,d.supersedesDocumentId,Types.BIGINT);p.setObject(5,old==null?null:old.version,Types.BIGINT);p.setString(6,old==null?null:old.contentHash);p.setInt(7,admin);try(ResultSet r=p.executeQuery()){r.next();job=r.getLong(1);}
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_publish_chunks(job_id,ordinal,content,content_hash) VALUES(?,?,?,?)")){
                for(int i=0;i<parts.size();i++){p.setLong(1,job);p.setInt(2,i);p.setString(3,parts.get(i));p.setString(4,VectorValues.hash(d.title+"\n"+parts.get(i)));p.addBatch();}p.executeBatch();
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET workflow_status='APPROVED',status='draft',approved_by=?,approved_at=CURRENT_TIMESTAMP,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP,indexing_status='pending',indexed_version=NULL,embedding_provider='GEMINI',embedding_model='gemini-embedding-001',vector_dimensions=768,updated_at=CURRENT_TIMESTAMP WHERE id=?")){
                p.setInt(1,admin);p.setInt(2,admin);p.setLong(3,id);p.executeUpdate();
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_document_revisions SET workflow_status='APPROVED' WHERE document_id=? AND version_number=?")){p.setLong(1,id);p.setLong(2,version);p.executeUpdate();}
            audit(c,admin,"AUTHORIZE_PUBLISH",id);c.commit();return job;
        }catch(Exception e){c.rollback();throw e;}}
    }
    /** One durable slot per chunk; interrupted/uncertain sends block all automatic replay. */
    public boolean processOne(EmbeddingService embedding,int rpm,int tpm,int rpd,int totalBudget) throws Exception {
        if(!"GEMINI".equals(embedding.providerKind())||!"gemini-embedding-001".equals(embedding.model())||embedding.dimensions()!=768 || rpm<=0||tpm<=0||rpd<=0||totalBudget<=0)throw new RagException(RagException.Code.CONFIGURATION);
        long jobId=0;int ordinal=-1;String text=null,title=null;
        try(Connection c=db.open()) {c.setAutoCommit(false);try{
            try(Statement s=c.createStatement()){s.execute("SELECT pg_advisory_xact_lock(57316008)");}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT 1 FROM public.knowledge_embedding_requests WHERE state IN ('RESERVED','UNCERTAIN') LIMIT 1")){if(r.next()){c.commit();return false;}}
            try(PreparedStatement p=c.prepareStatement("SELECT j.id AS job_id,d.title,j.document_version,j.document_hash,d.* FROM public.knowledge_publish_jobs j JOIN public.knowledge_documents d ON d.id=j.document_id WHERE j.state IN ('PENDING_INDEX','WAITING_FOR_QUOTA','WAITING_EFFECTIVE') AND (j.state<>'WAITING_EFFECTIVE' OR d.effective_at<=CURRENT_TIMESTAMP) AND NOT EXISTS(SELECT 1 FROM public.knowledge_publish_chunks bad WHERE bad.job_id=j.id AND bad.state IN ('FAILED','UNCERTAIN')) ORDER BY j.id LIMIT 1 FOR UPDATE OF j,d SKIP LOCKED");ResultSet r=p.executeQuery()){
                if(!r.next()){c.commit();return false;}jobId=r.getLong(1);title=r.getString(2);
                if(r.getLong(3)!=r.getLong("version")||!r.getString(4).equals(r.getString("content_hash"))||!"APPROVED".equals(r.getString("workflow_status"))||expired(r)){
                    state(c,jobId,"CANCELLED","STALE_VERSION");c.commit();return true;
                }
            }
            try(PreparedStatement p=c.prepareStatement("SELECT ordinal,content FROM public.knowledge_publish_chunks WHERE job_id=? AND state='PENDING' ORDER BY ordinal LIMIT 1")){
                p.setLong(1,jobId);try(ResultSet r=p.executeQuery()){if(r.next()){ordinal=r.getInt(1);text=r.getString(2);}}
            }
            if(ordinal<0){activate(c,jobId);c.commit();return true;}
            String chunkHash=VectorValues.hash(title+"\n"+text);
            // These staging rows have a fixed model/provider/dimension contract and validated vectors.
            // Reuse only completed responses, never pending/uncertain requests or mock knowledge_chunks.
            try(PreparedStatement p=c.prepareStatement("SELECT embedding::text FROM public.knowledge_publish_chunks WHERE content_hash=? AND state='DONE' AND job_id<>? LIMIT 1")){
                p.setString(1,chunkHash);p.setLong(2,jobId);try(ResultSet r=p.executeQuery()){if(r.next()){
                    float[] cached=VectorValues.parse(r.getString(1));
                    try(PreparedStatement u=c.prepareStatement("UPDATE public.knowledge_publish_chunks SET state='DONE',embedding=CAST(? AS vector),finished_at=CURRENT_TIMESTAMP WHERE job_id=? AND ordinal=? AND state='PENDING'")){
                        u.setString(1,VectorValues.sql(cached));u.setLong(2,jobId);u.setInt(3,ordinal);u.executeUpdate();
                    }
                    activate(c,jobId);c.commit();return true;
                }}
            }
            // Conservative UTF-8 byte reservation, not optimistic chars/4, with title overhead.
            int tokens=(text+title).getBytes(java.nio.charset.StandardCharsets.UTF_8).length+64;
            if(tokens>2048){state(c,jobId,"INDEXING_FAILED","INVALID_DOCUMENT");c.commit();return true;}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FILTER(WHERE reserved_at>CURRENT_TIMESTAMP-INTERVAL '60 seconds'),COALESCE(SUM(reserved_tokens) FILTER(WHERE reserved_at>CURRENT_TIMESTAMP-INTERVAL '60 seconds'),0),COUNT(*) FILTER(WHERE reserved_at>CURRENT_TIMESTAMP-INTERVAL '24 hours'),COUNT(*) FROM public.knowledge_embedding_requests")){
                r.next();if(r.getLong(1)>=rpm||r.getLong(2)+tokens>tpm||r.getLong(3)>=rpd||r.getLong(4)>=totalBudget){state(c,jobId,"WAITING_FOR_QUOTA","BUDGET_EXCEEDED");c.commit();return false;}
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_publish_chunks SET state='RESERVED',reserved_tokens=?,reserved_at=CURRENT_TIMESTAMP WHERE job_id=? AND ordinal=? AND state='PENDING'")){
                p.setInt(1,tokens);p.setLong(2,jobId);p.setInt(3,ordinal);if(p.executeUpdate()!=1)throw new RagException(RagException.Code.STALE_VERSION);
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_embedding_requests(job_id,ordinal,state,reserved_tokens) VALUES(?,?,'RESERVED',?)")){p.setLong(1,jobId);p.setInt(2,ordinal);p.setInt(3,tokens);p.executeUpdate();}
            state(c,jobId,"PENDING_INDEX",null);c.commit();
        }catch(Exception e){c.rollback();throw e;}}
        try {
            float[] vector=VectorValues.normalized(embedding.document(title,text),768);
            try(Connection c=db.open()){c.setAutoCommit(false);try{
                try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_publish_chunks SET state='DONE',embedding=CAST(? AS vector),http_status=200,finished_at=CURRENT_TIMESTAMP WHERE job_id=? AND ordinal=? AND state='RESERVED'")){
                    p.setString(1,VectorValues.sql(vector));p.setLong(2,jobId);p.setInt(3,ordinal);if(p.executeUpdate()!=1)throw new RagException(RagException.Code.STALE_VERSION);
                }
                requestState(c,jobId,ordinal,"DONE");
                activate(c,jobId);c.commit();
            }catch(Exception e){c.rollback();throw e;}}
        }catch(Exception e){
            String code=e instanceof RagException?((RagException)e).code.name():"PROVIDER";
            boolean uncertain="TIMEOUT".equals(code)||"PROVIDER".equals(code)||"DATABASE".equals(code)||!(e instanceof RagException);
            try(Connection c=db.open()){c.setAutoCommit(false);try{
                try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_publish_chunks SET state=?,error_code=?,finished_at=CURRENT_TIMESTAMP WHERE job_id=? AND ordinal=? AND state='RESERVED'")){
                    p.setString(1,uncertain?"UNCERTAIN":"FAILED");p.setString(2,code);p.setLong(3,jobId);p.setInt(4,ordinal);p.executeUpdate();
                }
                requestState(c,jobId,ordinal,uncertain?"UNCERTAIN":"FAILED");
                state(c,jobId,"RATE_LIMIT".equals(code)?"WAITING_FOR_QUOTA":"INDEXING_FAILED",code);c.commit();
            }catch(Exception nested){c.rollback();throw nested;}}
        }
        return true;
    }
    private static boolean expired(ResultSet r)throws SQLException {Timestamp t=r.getTimestamp("expires_at");return t!=null&&!Instant.now().isBefore(t.toInstant());}
    private static void requestState(Connection c,long job,int ordinal,String state)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_embedding_requests SET state=? WHERE job_id=? AND ordinal=? AND state='RESERVED'")){p.setString(1,state);p.setLong(2,job);p.setInt(3,ordinal);p.executeUpdate();}
    }
    private void activate(Connection c,long jobId)throws Exception {
        long id,version;String hash;Long oldId,oldVersion;String oldHash;int admin;
        try(PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_publish_jobs WHERE id=? FOR UPDATE")){p.setLong(1,jobId);try(ResultSet r=p.executeQuery()){
            r.next();if(!Arrays.asList("PENDING_INDEX","WAITING_FOR_QUOTA","WAITING_EFFECTIVE").contains(r.getString("state")))return;
            id=r.getLong("document_id");version=r.getLong("document_version");hash=r.getString("document_hash");oldId=(Long)r.getObject("replacement_id");oldVersion=(Long)r.getObject("replacement_version");oldHash=r.getString("replacement_hash");admin=r.getInt("actor_id");
        }}
        try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FILTER(WHERE state<>'DONE'),COUNT(*) FROM public.knowledge_publish_chunks WHERE job_id=?")){
            p.setLong(1,jobId);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)>0||r.getInt(2)==0)return;}
        }
        ManagedDocument d=lock(c,id);
        if(d.version!=version||!d.contentHash.equals(hash)||!"APPROVED".equals(d.workflowStatus)||d.approvedBy<=0||d.approvedAt==null || (d.expiresAt!=null&&!Instant.now().isBefore(d.expiresAt))){state(c,jobId,"CANCELLED","STALE_VERSION");return;}
        if(d.effectiveAt!=null&&Instant.now().isBefore(d.effectiveAt)){state(c,jobId,"WAITING_EFFECTIVE",null);return;}
        if(oldId!=null){ManagedDocument old=lock(c,oldId);if(old.version!=oldVersion || !old.contentHash.equals(oldHash)){state(c,jobId,"INDEXING_FAILED","STALE_VERSION");return;}
            // Two replacements of the same old policy must not both become active siblings.
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM public.knowledge_documents WHERE supersedes_document_id=? AND id<>? AND status='published'")){
                p.setLong(1,oldId);p.setLong(2,id);try(ResultSet r=p.executeQuery()){if(r.next()){state(c,jobId,"INDEXING_FAILED","STALE_VERSION");return;}}
            }
        }
        try(PreparedStatement p=c.prepareStatement("DELETE FROM public.knowledge_chunks WHERE document_id=?")){p.setLong(1,id);p.executeUpdate();}
        String sql="INSERT INTO public.knowledge_chunks(document_id,document_version,document_hash,ordinal,content,content_hash,embedding_model,vector_dimensions,embedding,search_text,embedding_provider) SELECT ?,?,?,ordinal,content,content_hash,'gemini-embedding-001',768,embedding,?||' '||content,'GEMINI' FROM public.knowledge_publish_chunks WHERE job_id=? AND state='DONE'";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,id);p.setLong(2,version);p.setString(3,hash);p.setString(4,ai.consultation.ProductCatalog.normalize(d.title));p.setLong(5,jobId);p.executeUpdate();}
        // Normalize lexical text exactly like the existing chunk DAO.
        try(PreparedStatement p=c.prepareStatement("SELECT id,content FROM public.knowledge_chunks WHERE document_id=?")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){
            try(PreparedStatement u=c.prepareStatement("UPDATE public.knowledge_chunks SET search_text=? WHERE id=?")){while(r.next()){u.setString(1,ai.consultation.ProductCatalog.normalize(d.title+" "+r.getString(2)));u.setLong(2,r.getLong(1));u.addBatch();}u.executeBatch();}
        }}
        try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET status='published',workflow_status='PUBLISHED',indexing_status='ready',indexed_version=version,last_indexed_at=CURRENT_TIMESTAMP,embedding_provider='GEMINI',embedding_model='gemini-embedding-001',embedding_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?")){p.setLong(1,id);p.executeUpdate();}
        if(oldId!=null){
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET status='draft',workflow_status='UNPUBLISHED',updated_at=CURRENT_TIMESTAMP WHERE id=? AND workflow_status='PUBLISHED'")){p.setLong(1,oldId);p.executeUpdate();}
            audit(c,admin,"REPLACED_BY_INDEXED_VERSION",oldId);
        }
        try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_document_revisions SET workflow_status='PUBLISHED' WHERE document_id=? AND version_number=?")){p.setLong(1,id);p.setLong(2,version);p.executeUpdate();}
        state(c,jobId,"AVAILABLE_TO_CHATBOT",null);audit(c,admin,"ACTIVATE_INDEXED_DOCUMENT",id);
    }
    private ManagedDocument lock(Connection c,long id)throws Exception {
        try(PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_documents WHERE id=? FOR UPDATE")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không tìm thấy tài liệu.");return AdminKnowledgeDAO.readManagedDocument(r);}}
    }
    private static void state(Connection c,long id,String state,String error)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_publish_jobs SET state=?,error_code=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){p.setString(1,state);p.setString(2,error);p.setLong(3,id);p.executeUpdate();}
    }
    private static void audit(Connection c,int admin,String action,long id)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO public.admin_audit_logs(correlation_id,actor_id,action,resource_type,resource_id,summary) VALUES(?, ?,?,'KNOWLEDGE_DOCUMENT',?,'Phase 5.7 lifecycle operation')")){
            p.setString(1,UUID.randomUUID().toString());p.setInt(2,admin);p.setString(3,action);p.setString(4,Long.toString(id));p.executeUpdate();
        }
    }
    public List<Map<String,Object>> jobs()throws Exception {
        List<Map<String,Object>> out=new ArrayList<>();
        try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT j.id,j.document_id,j.document_version,CASE WHEN d.workflow_status IN ('UNPUBLISHED','ARCHIVED') THEN d.workflow_status WHEN d.expires_at<=CURRENT_TIMESTAMP THEN 'EXPIRED' WHEN d.version<>j.document_version THEN 'SUPERSEDED' ELSE j.state END AS state,j.error_code,d.title,COUNT(k.ordinal) AS total,COUNT(k.ordinal) FILTER(WHERE k.state='DONE') AS done FROM public.knowledge_publish_jobs j JOIN public.knowledge_documents d ON d.id=j.document_id LEFT JOIN public.knowledge_publish_chunks k ON k.job_id=j.id GROUP BY j.id,d.id ORDER BY j.id DESC LIMIT 50")){
            while(r.next()){Map<String,Object> m=new LinkedHashMap<>();for(String key:Arrays.asList("id","document_id","document_version","state","error_code","title","total","done"))m.put(key,r.getObject(key));out.add(m);}
        }return out;
    }
    public Map<String,Object> readiness()throws Exception {
        Map<String,Object> out=new LinkedHashMap<>();
        try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FILTER(WHERE status='published') AS published,COUNT(*) FILTER(WHERE status='published' AND embedding_provider='GEMINI' AND embedding_model='gemini-embedding-001' AND vector_dimensions=768 AND indexing_status='ready' AND indexed_version=version) AS gemini,MAX(last_indexed_at) FILTER(WHERE embedding_provider='GEMINI' AND indexing_status='ready') AS last_index FROM public.knowledge_documents")){
            r.next();for(String k:Arrays.asList("published","gemini","last_index"))out.put(k,r.getObject(k));
        }
        try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM public.knowledge_chunks k JOIN public.knowledge_documents d ON d.id=k.document_id WHERE d.status='published' AND d.indexing_status='ready' AND d.indexed_version=d.version AND k.document_version=d.version AND k.document_hash=d.content_hash AND k.embedding_provider='GEMINI' AND d.embedding_provider='GEMINI' AND k.embedding_model=d.embedding_model AND k.vector_dimensions=768 AND (d.effective_at IS NULL OR d.effective_at<=CURRENT_TIMESTAMP) AND (d.expires_at IS NULL OR d.expires_at>CURRENT_TIMESTAMP)")){r.next();out.put("validChunks",r.getLong(1));}
        if(available())try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM public.knowledge_publish_jobs WHERE state='INDEXING_FAILED'")){r.next();out.put("failedPublishJobs",r.getLong(1));}
        return out;
    }
    /** References retained by FAQ/import/version lineage must be detached explicitly before deletion. */
    public List<String> permanentDelete(long id,long version,int admin,String confirmation)throws Exception {
        actor(admin);if(!("DELETE "+id).equals(confirmation))throw new IllegalArgumentException("Nhập đúng DELETE "+id+" để xác nhận.");
        List<String> paths=new ArrayList<>();
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            ManagedDocument d=lock(c,id);if(d.version!=version || "PUBLISHED".equals(d.workflowStatus))throw new IllegalArgumentException("Gỡ xuất bản và xác nhận đúng phiên bản trước khi xóa.");
            String[] refs={"SELECT 1 FROM public.knowledge_documents WHERE supersedes_document_id=?","SELECT 1 FROM public.knowledge_document_revisions WHERE supersedes_document_id=? AND document_id<>?","SELECT 1 FROM public.knowledge_faqs WHERE linked_document_id=?","SELECT 1 FROM public.knowledge_import_sections WHERE source_document_id=? OR document_id=?","SELECT 1 FROM public.knowledge_publish_jobs WHERE replacement_id=?"};
            for(String sql:refs)try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,id);if(sql.contains("<>?")||sql.contains(" OR document_id=?"))p.setLong(2,id);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Tài liệu còn liên kết phiên bản/FAQ/import; hãy lưu trữ để bảo toàn nguồn.");}}
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM public.knowledge_publish_chunks k JOIN public.knowledge_publish_jobs j ON j.id=k.job_id WHERE j.document_id=? AND k.state IN ('RESERVED','UNCERTAIN')")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Request chưa xác định trạng thái; không được xóa evidence.");}}
            try(PreparedStatement p=c.prepareStatement("SELECT storage_rel_path FROM public.knowledge_files WHERE document_id=?")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){while(r.next())paths.add(r.getString(1));}}
            try(PreparedStatement p=c.prepareStatement("DELETE FROM public.knowledge_files WHERE document_id=?")){p.setLong(1,id);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("DELETE FROM public.knowledge_documents WHERE id=?")){p.setLong(1,id);p.executeUpdate();}
            audit(c,admin,"PERMANENT_DELETE",id);c.commit();return paths;
        }catch(Exception e){c.rollback();throw e;}}
    }
}
