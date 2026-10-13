package dao;

import rag.*;
import rag.files.*;
import rag.files.FileKnowledgeSync.State;
import dto.KnowledgeDocumentDTO;
import java.sql.*;
import java.util.*;

/** Session advisory lock spans import + queue; a crash releases the lock, not durable sync evidence. */
public final class FileKnowledgeDAO implements FileKnowledgeSync.Store {
    private final RagDatabase db;private Connection lease;private boolean locked;
    public FileKnowledgeDAO(){this(new RagDatabase());}public FileKnowledgeDAO(RagDatabase db){this.db=db;}
    public boolean lock()throws Exception {
        lease=db.open();try(Statement s=lease.createStatement();ResultSet r=s.executeQuery("SELECT pg_try_advisory_lock(57316009)")){r.next();locked=r.getBoolean(1);return locked;}
    }
    public State find(String id)throws Exception {
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_file_sources WHERE file_id=?")){p.setString(1,id);try(ResultSet r=p.executeQuery()){return r.next()?new State(r.getLong("file_version"),r.getLong("document_id"),r.getString("file_sha"),r.getString("state"),r.getBoolean("emergency_disabled")):null;}}
    }
    public State stage(KnowledgeFile f,int actor)throws Exception {
        if(!locked||actor<=0)throw new IllegalStateException("Sync lease/Admin required");
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            try(PreparedStatement p=c.prepareStatement("SELECT emergency_disabled FROM public.knowledge_file_sources WHERE file_id=? FOR UPDATE")){p.setString(1,f.id);try(ResultSet r=p.executeQuery()){if(r.next()&&r.getBoolean(1))throw new IllegalStateException("Nguồn đã bị vô hiệu hóa khẩn cấp");}}
            // Jobs before document locks, matching activation; never reset durable request slots.
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_publish_jobs SET state='CANCELLED',error_code='STALE_VERSION',updated_at=CURRENT_TIMESTAMP WHERE document_id IN (SELECT document_id FROM public.knowledge_file_revisions WHERE file_id=?) AND state NOT IN ('AVAILABLE_TO_CHATBOT','CANCELLED')")){p.setString(1,f.id);p.executeUpdate();}
            Long oldId=null;String policy="repo."+f.id,authority=f.authority;
            try(PreparedStatement p=c.prepareStatement("SELECT d.* FROM public.knowledge_file_revisions h JOIN public.knowledge_documents d ON d.id=h.document_id WHERE h.file_id IN (?,?) AND d.status='published' ORDER BY h.file_version DESC LIMIT 1 FOR UPDATE OF d")){
                p.setString(1,f.id);p.setString(2,f.replaces);try(ResultSet r=p.executeQuery()){if(r.next()){oldId=r.getLong("id");policy=r.getString("policy_key");if("official".equals(r.getString("source_authority"))&&!"official".equals(authority))throw new IllegalArgumentException("Không hạ source authority");}}
            }
            if(!f.replaces.isEmpty()&&oldId==null)throw new IllegalArgumentException("replaces cần nguồn đã xuất bản: "+f.replaces);
            KnowledgeDocumentDTO d=new KnowledgeDocumentDTO(0,f.version,f.title,f.category,f.body,f.source(),"draft",f.effective,f.expires,"pending",policy,authority,oldId);
            long id;
            String sql="INSERT INTO public.knowledge_documents(title,category,content,source,status,workflow_status,source_type,version,content_hash,effective_at,expires_at,embedding_model,vector_dimensions,indexing_status,policy_key,source_authority,supersedes_document_id,created_by,updated_by,chunk_size,chunk_overlap) VALUES(?,?,?,?,'draft','DRAFT','FILE_MANAGED',?,?,?,?, 'gemini-embedding-001',768,'pending',?,?,?,?,?,?,?) RETURNING id";
            try(PreparedStatement p=c.prepareStatement(sql)){
                int i=1;p.setString(i++,f.title);p.setString(i++,f.category);p.setString(i++,f.body);p.setString(i++,f.source());p.setLong(i++,f.version);p.setString(i++,d.contentHash);p.setTimestamp(i++,RagDatabase.timestamp(f.effective));p.setTimestamp(i++,RagDatabase.timestamp(f.expires));p.setString(i++,policy);p.setString(i++,authority);p.setObject(i++,oldId,Types.BIGINT);p.setInt(i++,actor);p.setInt(i++,actor);p.setInt(i++,f.chunkSize);p.setInt(i,f.overlap);try(ResultSet r=p.executeQuery()){r.next();id=r.getLong(1);}
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_document_revisions(document_id,version_number,title,category,source,source_type,source_authority,policy_key,content,content_hash,workflow_status,effective_at,expires_at,supersedes_document_id,change_note,created_by) SELECT id,version,title,category,source,source_type,source_authority,policy_key,content,content_hash,workflow_status,effective_at,expires_at,supersedes_document_id,'Repository file sync',created_by FROM public.knowledge_documents WHERE id=?")){p.setLong(1,id);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_file_sources(file_id,file_version,file_sha,file_path,document_id,state) VALUES(?,?,?,?,?,'PENDING_QUEUE') ON CONFLICT(file_id) DO UPDATE SET file_version=EXCLUDED.file_version,file_sha=EXCLUDED.file_sha,file_path=EXCLUDED.file_path,document_id=EXCLUDED.document_id,state='PENDING_QUEUE',updated_at=CURRENT_TIMESTAMP")){
                p.setString(1,f.id);p.setLong(2,f.version);p.setString(3,f.sha);p.setString(4,f.path);p.setLong(5,id);p.executeUpdate();
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_file_revisions(file_id,file_version,file_sha,file_path,document_id) VALUES(?,?,?,?,?)")){p.setString(1,f.id);p.setLong(2,f.version);p.setString(3,f.sha);p.setString(4,f.path);p.setLong(5,id);p.executeUpdate();}
            audit(c,actor,"SYNC_REPOSITORY_FILE",id);c.commit();return new State(f.version,id,f.sha,"PENDING_QUEUE",false);
        }catch(Exception e){c.rollback();throw e;}}
    }
    public void touchPath(KnowledgeFile f)throws Exception {
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_file_sources SET file_path=? WHERE file_id=? AND file_sha=? AND file_version=?")){p.setString(1,f.path);p.setString(2,f.id);p.setString(3,f.sha);p.setLong(4,f.version);p.executeUpdate();}
    }
    public void queue(State state,int actor)throws Exception {
        new KnowledgeLifecycleDAO(db).publish(state.documentId,state.version,actor);
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_file_sources SET state='QUEUED',updated_at=CURRENT_TIMESTAMP WHERE document_id=? AND state='PENDING_QUEUE'")){p.setLong(1,state.documentId);p.executeUpdate();}
    }
    public int retire(Set<String> retained,int actor)throws Exception {
        int retired=0;List<String> ids=new ArrayList<>();try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT file_id FROM public.knowledge_file_sources WHERE state NOT IN ('MISSING','INACTIVE','EMERGENCY_DISABLED')")){while(r.next())if(!retained.contains(r.getString(1)))ids.add(r.getString(1));}
        for(String id:ids){disable(id,actor,false);retired++;}return retired;
    }
    public void emergencyDisable(long document,int actor)throws Exception {
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT file_id FROM public.knowledge_file_revisions WHERE document_id=?")){p.setLong(1,document);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không tìm thấy nguồn file");disable(r.getString(1),actor,true);}}
    }
    private void disable(String id,int actor,boolean emergency)throws Exception {
        if(actor<=0)throw new IllegalArgumentException("Yêu cầu Admin");
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            if(!locked)try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT pg_try_advisory_xact_lock(57316009)")){r.next();if(!r.getBoolean(1))throw new IllegalStateException("Đang sync trên instance khác; tải lại để vô hiệu hóa.");}
            // Lock in the worker's order: jobs then documents. Do not clear reserved API slots.
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_publish_jobs SET state='CANCELLED',updated_at=CURRENT_TIMESTAMP WHERE document_id IN (SELECT document_id FROM public.knowledge_file_revisions WHERE file_id=?) AND state<>'AVAILABLE_TO_CHATBOT'")){p.setString(1,id);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET status='archived',workflow_status='ARCHIVED',updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT document_id FROM public.knowledge_file_revisions WHERE file_id=?)")){p.setString(1,id);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_file_sources SET state=?,emergency_disabled=emergency_disabled OR ?,updated_at=CURRENT_TIMESTAMP WHERE file_id=?")){p.setString(1,emergency?"EMERGENCY_DISABLED":(FileKnowledgeCatalog.deployed().files.containsKey(id)?"INACTIVE":"MISSING"));p.setBoolean(2,emergency);p.setString(3,id);p.executeUpdate();}
            audit(c,actor,emergency?"EMERGENCY_DISABLE_FILE":"RETIRE_REPOSITORY_FILE",id);c.commit();
        }catch(Exception e){c.rollback();throw e;}}
    }
    public static boolean permitted(Connection c,long document,String source)throws Exception {
        if(!FileKnowledgeCatalog.allowedSource(source))return false;
        if(source==null||!source.startsWith("repo:"))return true;
        try(PreparedStatement p=c.prepareStatement("SELECT NOT s.emergency_disabled AND s.state NOT IN ('MISSING','INACTIVE','EMERGENCY_DISABLED'),h.file_sha FROM public.knowledge_file_revisions h JOIN public.knowledge_file_sources s ON s.file_id=h.file_id WHERE h.document_id=?")){p.setLong(1,document);try(ResultSet r=p.executeQuery()){return r.next()&&r.getBoolean(1)&&FileKnowledgeCatalog.allowedRevision(source,r.getString(2));}}
    }
    public List<Map<String,Object>> status()throws Exception {
        List<Map<String,Object>> out=new ArrayList<>();String sql="SELECT s.file_id,s.file_path,s.file_version,s.file_sha,s.state,s.emergency_disabled,d.id,d.title,d.source,d.status,d.embedding_provider,d.indexing_status,d.indexed_version,d.version,(d.approved_by>0 AND d.approved_at<=CURRENT_TIMESTAMP AND (d.effective_at IS NULL OR d.effective_at<=CURRENT_TIMESTAMP) AND (d.expires_at IS NULL OR d.expires_at>CURRENT_TIMESTAMP) AND d.embedding_model='gemini-embedding-001' AND d.vector_dimensions=768) AS valid_metadata,(SELECT COUNT(*) FROM public.knowledge_chunks k WHERE k.document_id=d.id AND k.document_version=d.version AND k.document_hash=d.content_hash AND k.embedding_provider=d.embedding_provider AND k.embedding_model=d.embedding_model AND k.vector_dimensions=d.vector_dimensions) AS indexed_chunks FROM public.knowledge_file_sources s JOIN public.knowledge_documents d ON d.id=s.document_id ORDER BY s.file_id LIMIT 200";
        try(Connection c=db.open();Statement p=c.createStatement();ResultSet r=p.executeQuery(sql)){while(r.next()){
            Map<String,Object> m=new LinkedHashMap<>();for(String key:Arrays.asList("file_id","file_path","file_version","file_sha","state","id","title","embedding_provider","indexing_status","indexed_chunks"))m.put(key,r.getObject(key));
            m.put("eligible",r.getBoolean("valid_metadata")&&!r.getBoolean("emergency_disabled")&&!Arrays.asList("MISSING","INACTIVE","EMERGENCY_DISABLED").contains(r.getString("state"))&&FileKnowledgeCatalog.allowedRevision(r.getString("source"),r.getString("file_sha"))&&r.getLong("indexed_chunks")>0&&"published".equals(r.getString("status"))&&"ready".equals(r.getString("indexing_status"))&&r.getLong("indexed_version")==r.getLong("version")&&"GEMINI".equals(r.getString("embedding_provider")));
            int recent=0;for(Map<String,Object> e:RagRequestEvidence.recent())for(Object obj:(List<?>)e.get("sources"))if(((Number)((Map<?,?>)obj).get("id")).longValue()==r.getLong("id"))recent++;
            m.put("recentPassedToGemini",recent);out.add(m);
        }}return out;
    }
    private static void audit(Connection c,int actor,String action,Object id)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO public.admin_audit_logs(correlation_id,actor_id,action,resource_type,resource_id,summary) VALUES(?,?,?,'FILE_MANAGED',?,'Repository knowledge lifecycle')")){p.setString(1,UUID.randomUUID().toString());p.setInt(2,actor);p.setString(3,action);p.setString(4,id.toString());p.executeUpdate();}
    }
    public void close()throws Exception {if(lease!=null){try{if(locked)try(Statement s=lease.createStatement()){s.execute("SELECT pg_advisory_unlock(57316009)");}}finally{lease.close();lease=null;locked=false;}}}
}
