package dao;

import dto.*;
import rag.*;
import java.sql.*;
import java.util.UUID;

/** Transactional outbox with bounded attempts and leased SKIP LOCKED claims. */
public class KnowledgeIndexJobDAO {
    private final RagDatabase db;
    private final String kind;
    private final Long onlyDocument;
    public KnowledgeIndexJobDAO(RagDatabase db) {this(db,"MOCK",null);}
    public KnowledgeIndexJobDAO(RagDatabase db,String kind,Long document) {
        if(!java.util.Arrays.asList("MOCK","GEMINI").contains(kind))throw new IllegalArgumentException();
        this.db=db;this.kind=kind;onlyDocument=document;
    }
    static void enqueue(Connection c,KnowledgeDocumentDTO d,RagConfig cfg) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_index_jobs (document_id,document_version,document_hash,embedding_model,vector_dimensions,available_at,embedding_provider) VALUES (?,?,?,?,?,GREATEST(COALESCE(?,CURRENT_TIMESTAMP),CURRENT_TIMESTAMP),(SELECT embedding_provider FROM public.knowledge_documents WHERE id=?)) ON CONFLICT (document_id,document_version,embedding_model,vector_dimensions) DO NOTHING")) {
            p.setLong(1,d.id);p.setLong(2,d.version);p.setString(3,d.contentHash);p.setString(4,cfg.model);p.setInt(5,cfg.dimensions);p.setTimestamp(6,RagDatabase.timestamp(d.effectiveAt));p.setLong(7,d.id);p.executeUpdate();
        }
    }
    public static int recoverExpiredLeases(Connection c) throws SQLException {
        int recovered = 0;
        try (PreparedStatement p = c.prepareStatement(
                "WITH expired_gemini AS ("
                + "UPDATE public.knowledge_index_jobs SET status='failed_retryable',error_code='LEASE_EXPIRED',lease_until=NULL,updated_at=CURRENT_TIMESTAMP "
                + "WHERE status='running' AND lease_until<CURRENT_TIMESTAMP AND embedding_provider='GEMINI' "
                + "RETURNING document_id,document_version,document_hash) "
                + "UPDATE public.knowledge_documents d SET indexing_status='failed_retryable',embedding_error='LEASE_EXPIRED',updated_at=CURRENT_TIMESTAMP "
                + "FROM expired_gemini e WHERE d.id=e.document_id AND d.version=e.document_version AND d.content_hash=e.document_hash AND d.indexing_status<>'ready'")) {
            recovered += p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement(
                "WITH exhausted AS ("
                + "UPDATE public.knowledge_index_jobs SET status='failed',error_code='TIMEOUT',updated_at=CURRENT_TIMESTAMP "
                + "WHERE status='running' AND lease_until<CURRENT_TIMESTAMP AND attempts>=3 "
                + "RETURNING document_id,document_version,document_hash) "
                + "UPDATE public.knowledge_documents d SET indexing_status='failed',updated_at=CURRENT_TIMESTAMP "
                + "FROM exhausted e WHERE d.id=e.document_id AND d.version=e.document_version AND d.content_hash=e.document_hash AND d.indexing_status<>'ready'")) {
            recovered += p.executeUpdate();
        }
        return recovered;
    }
    public int recoverExpiredLeases() throws Exception {
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                int count = recoverExpiredLeases(c);
                c.commit();
                return count;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
    public KnowledgeIndexJobDTO claim() throws Exception {
        UUID token=UUID.randomUUID();
        try(Connection c=db.open()) {
            c.setAutoCommit(false);
            try {
                recoverExpiredLeases(c);
                String sql="WITH next_job AS (SELECT id FROM public.knowledge_index_jobs WHERE embedding_provider='"+kind+"' "+(onlyDocument==null?"":"AND document_id="+onlyDocument.longValue()+" ")+"AND attempts<3 AND ((status='queued' AND available_at<=CURRENT_TIMESTAMP) OR (status='running' AND lease_until<CURRENT_TIMESTAMP AND embedding_provider='MOCK')) ORDER BY available_at,id FOR UPDATE SKIP LOCKED LIMIT 1) "
                    +"UPDATE public.knowledge_index_jobs j SET status='running',attempts=attempts+1,lease_token=?,lease_until=CURRENT_TIMESTAMP+INTERVAL '5 minutes',updated_at=CURRENT_TIMESTAMP FROM next_job n WHERE j.id=n.id RETURNING j.*";
                KnowledgeIndexJobDTO job=null;
                try(PreparedStatement p=c.prepareStatement(sql)) {
                    p.setObject(1,token);try(ResultSet r=p.executeQuery()){if(r.next())job=new KnowledgeIndexJobDTO(r.getLong("id"),r.getLong("document_id"),r.getLong("document_version"),r.getString("document_hash"),r.getString("embedding_model"),r.getInt("vector_dimensions"),r.getInt("attempts"),token,r.getString("embedding_provider"));}
                } c.commit();return job;
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    public boolean owns(Connection c,KnowledgeIndexJobDTO job) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT id FROM public.knowledge_index_jobs WHERE id=? AND lease_token=? AND status='running' AND lease_until>CURRENT_TIMESTAMP FOR UPDATE")) {
            p.setLong(1,job.id);p.setObject(2,job.leaseToken);try(ResultSet r=p.executeQuery()){return r.next();}
        }
    }
    public void renew(KnowledgeIndexJobDTO job) throws Exception {
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_index_jobs SET lease_until=CURRENT_TIMESTAMP+INTERVAL '5 minutes' WHERE id=? AND lease_token=? AND status='running' AND lease_until>CURRENT_TIMESTAMP")) {
            p.setLong(1,job.id);p.setObject(2,job.leaseToken);if(p.executeUpdate()!=1)throw new RagException(RagException.Code.STALE_VERSION);
        }
    }
    public static void finish(Connection c,KnowledgeIndexJobDTO job,String status) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_index_jobs SET status=?,lease_until=NULL,error_code=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND lease_token=? AND status='running'")) {
            p.setString(1,status);p.setLong(2,job.id);p.setObject(3,job.leaseToken);p.executeUpdate();
        }
    }
    public void fail(KnowledgeIndexJobDTO job,RagException.Code code) throws Exception {
        if("GEMINI".equals(job.providerKind)){
            String status=code==RagException.Code.RATE_LIMIT||code==RagException.Code.BUDGET_EXCEEDED?"waiting_for_quota":
                code==RagException.Code.TIMEOUT||code==RagException.Code.PROVIDER||code==RagException.Code.DATABASE?"failed_retryable":"failed";
            try(Connection c=db.open()){c.setAutoCommit(false);try{
                int changed;
                try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_index_jobs SET status=?,error_code=?,lease_until=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND lease_token=? AND status='running' AND lease_until>CURRENT_TIMESTAMP")){
                    p.setString(1,status);p.setString(2,code.name());p.setLong(3,job.id);p.setObject(4,job.leaseToken);changed=p.executeUpdate();
                }
                if(changed>0)try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET indexing_status=?,embedding_error=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=? AND content_hash=? AND embedding_provider='GEMINI'")){
                    p.setString(1,status);p.setString(2,code.name());p.setLong(3,job.documentId);p.setLong(4,job.version);p.setString(5,job.hash);p.executeUpdate();
                }c.commit();
            }catch(Exception e){c.rollback();throw e;}}return;
        }
        boolean retry=job.attempts<3 && (code==RagException.Code.TIMEOUT || code==RagException.Code.RATE_LIMIT || code==RagException.Code.PROVIDER || code==RagException.Code.DATABASE);
        try(Connection c=db.open()) {
            c.setAutoCommit(false);
            try {
                // Match both lease and current revision; an old worker cannot overwrite newer status.
                int changed;
                try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_index_jobs SET status=?,error_code=?,lease_until=NULL,available_at=CURRENT_TIMESTAMP+INTERVAL '60 seconds',updated_at=CURRENT_TIMESTAMP WHERE id=? AND lease_token=? AND status='running' AND lease_until>CURRENT_TIMESTAMP")) {
                    p.setString(1,retry?"queued":"failed");p.setString(2,code.name());p.setLong(3,job.id);p.setObject(4,job.leaseToken);changed=p.executeUpdate();
                }
                if(changed>0 && !retry)try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET indexing_status='failed',updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=? AND content_hash=? AND indexing_status<>'ready'")) {
                    p.setLong(1,job.documentId);p.setLong(2,job.version);p.setString(3,job.hash);p.executeUpdate();
                }
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
}
