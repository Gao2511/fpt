package rag;

import dao.*;
import dto.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Explicit worker invocation only. No servlet listener, timer or startup writes/API calls. */
public final class KnowledgeIndexingService {
    private final RagDatabase db;
    private final KnowledgeDocumentDAO documents;
    private final KnowledgeChunkDAO chunks;
    private final KnowledgeIndexJobDAO jobs;
    private final EmbeddingService embeddings;
    public KnowledgeIndexingService(RagDatabase db,KnowledgeDocumentDAO documents,KnowledgeChunkDAO chunks,KnowledgeIndexJobDAO jobs,EmbeddingService embeddings) {
        this.db=db;this.documents=documents;this.chunks=chunks;this.jobs=jobs;this.embeddings=embeddings;
    }
    public boolean runNext() throws Exception {
        KnowledgeIndexJobDTO job=jobs.claim();if(job==null)return false;
        try {
            KnowledgeDocumentDTO d=documents.find(job.documentId);
            if(d==null || d.version!=job.version || !d.contentHash.equals(job.hash) || !d.validAt(Instant.now()) || !job.model.equals(embeddings.model()) || job.dimensions!=embeddings.dimensions() || !job.providerKind.equals(embeddings.providerKind())) {
                try(Connection c=db.open()){KnowledgeIndexJobDAO.finish(c,job,"superseded");}return true;
            }
            List<String> texts=new DocumentChunkingService().chunk(d.content);
            if("GEMINI".equals(embeddings.providerKind())){
                boolean trusted=KnowledgeContentPolicy.trustedOfficialRepository(d.source,d.sourceAuthority);
                for(String text:texts)if(KnowledgeContentPolicy.contentReason(d.title,text,trusted)!=KnowledgeContentPolicy.Reason.ACCEPTED)throw new RagException(RagException.Code.INVALID_DOCUMENT);
                try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET indexing_status='processing',embedding_error=NULL WHERE id=? AND version=? AND embedding_provider='GEMINI'")){p.setLong(1,d.id);p.setLong(2,d.version);if(p.executeUpdate()!=1)throw new RagException(RagException.Code.STALE_VERSION);}
            }
            List<float[]> vectors=new ArrayList<>();Map<String,float[]> dedup=new HashMap<>();
            for(String text:texts) {
                jobs.renew(job);
                String hash=VectorValues.hash(d.title+"\n"+text);float[] values=dedup.get(hash);
                if(values==null)values=chunks.cached(hash,embeddings.model(),embeddings.dimensions());
                if(values==null)values=embeddings.document(d.title,text);
                values=VectorValues.normalized(values,embeddings.dimensions());dedup.put(hash,values);vectors.add(values);
            }
            try(Connection c=db.open()) {
                c.setAutoCommit(false);
                try {
                    if(!jobs.owns(c,job))throw new RagException(RagException.Code.STALE_VERSION);
                    boolean current;
                    try(PreparedStatement p=c.prepareStatement("SELECT id FROM public.knowledge_documents WHERE id=? AND version=? AND content_hash=? AND embedding_provider=? AND status='published' AND approved_by>0 AND approved_at<=CURRENT_TIMESTAMP AND (effective_at IS NULL OR effective_at<=CURRENT_TIMESTAMP) AND (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP) FOR UPDATE")) {
                        p.setLong(1,d.id);p.setLong(2,job.version);p.setString(3,job.hash);p.setString(4,embeddings.providerKind());try(ResultSet r=p.executeQuery()){current=r.next();}
                    }
                    if(!current) {KnowledgeIndexJobDAO.finish(c,job,"superseded");c.commit();return true;}
                    try(PreparedStatement p=c.prepareStatement("DELETE FROM public.knowledge_chunks WHERE document_id=?")){p.setLong(1,d.id);p.executeUpdate();}
                    for(int i=0;i<texts.size();i++)chunks.insert(c,d,i,texts.get(i),vectors.get(i),embeddings);
                    try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET indexing_status='ready',indexed_version=version,last_indexed_at=CURRENT_TIMESTAMP,embedding_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?")) {p.setLong(1,d.id);p.setLong(2,d.version);p.executeUpdate();}
                    KnowledgeIndexJobDAO.finish(c,job,"done");c.commit();
                }catch(Exception e){c.rollback();throw e;}
            }
        }catch(Exception e) {
            RagException.Code code=e instanceof RagException?((RagException)e).code:RagException.Code.DATABASE;
            jobs.fail(job,code);System.err.println("[RAG indexing] error="+code.name());
        }return true;
    }
}
