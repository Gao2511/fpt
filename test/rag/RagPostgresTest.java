package rag;

import dao.*;
import dto.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Integration suite for a fresh, isolated, loopback PostgreSQL + real pgvector database ONLY. */
public final class RagPostgresTest {
    private static int passed;
    private static final RagConfig CONFIG=new RagConfig(true,true,true,"gemini-embedding-001",768);
    private static final RagDatabase DB=new RagDatabase();
    private static final KnowledgeDocumentDAO DOCS=new KnowledgeDocumentDAO(DB,CONFIG);
    private static final KnowledgeChunkDAO CHUNKS=new KnowledgeChunkDAO(DB);
    private static final KnowledgeIndexJobDAO JOBS=new KnowledgeIndexJobDAO(DB);
    private static final MockEmbedding EMBEDDINGS=new MockEmbedding();
    private static final KnowledgeIndexingService WORKER=new KnowledgeIndexingService(DB,DOCS,CHUNKS,JOBS,EMBEDDINGS);
    interface Case {void run()throws Exception;}
    private static void test(String name,Case c)throws Exception{c.run();passed++;System.out.println("PASS PostgreSQL RAG "+name);}
    private static void check(boolean ok){if(!ok)throw new AssertionError("Database behavior mismatch");}
    private static class MockEmbedding implements EmbeddingService {
        int calls;Runnable before;
        public String model(){return CONFIG.model;}public int dimensions(){return CONFIG.dimensions;}
        public float[] document(String title,String content) {calls++;if(before!=null){Runnable r=before;before=null;r.run();}return vector(title+" "+content);}
        public float[] query(String text){return vector(text);}
        float[] vector(String text){float[] v=new float[768];String q=ai.consultation.ProductCatalog.normalize(text);v[q.contains("chuyen") || q.contains("di doi")?0:1]=1;return v;}
    }
    private static KnowledgeDocumentDTO draft(String title,String content,String status) {
        return new KnowledgeDocumentDTO(0,0,title,"faq",content,"Nguồn kiểm thử đã duyệt",status,null,null,"pending");
    }
    private static KnowledgeDocumentDTO edit(KnowledgeDocumentDTO d,String content,String status,String source) {
        return new KnowledgeDocumentDTO(d.id,d.version,d.title,d.category,content,source,status,d.effectiveAt,d.expiresAt,"pending",d.policyKey,d.sourceAuthority,d.supersedesDocumentId);
    }
    private static long count(String table)throws Exception {
        if(!Arrays.asList("knowledge_documents","knowledge_chunks","knowledge_index_jobs").contains(table))throw new IllegalArgumentException();
        try(Connection c=DB.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM public."+table)){r.next();return r.getLong(1);}
    }
    private static void setDate(long id,String field,Instant date)throws Exception {
        if(!Arrays.asList("effective_at","expires_at").contains(field))throw new IllegalArgumentException();
        try(Connection c=DB.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET "+field+"=? WHERE id=?")){p.setTimestamp(1,RagDatabase.timestamp(date));p.setLong(2,id);p.executeUpdate();}
    }
    private static List<KnowledgeChunkDTO> lexical(String q)throws Exception{return CHUNKS.search(q,null,CONFIG.model,768,12);}
    private static void drain()throws Exception{for(int i=0;i<30;i++)if(!WORKER.runNext())return;throw new AssertionError("Unbounded jobs");}
    private static KnowledgeDocumentDTO policy(String title,String content,String key,String authority,Long supersedes) {
        return new KnowledgeDocumentDTO(0,0,title,"guide",content,"FAKE_REVIEWED_INTEGRATION_SOURCE","published",null,null,"pending",key,authority,supersedes);
    }
    private static List<KnowledgeChunkDTO> subject(String keyword,String key)throws Exception {
        List<KnowledgeChunkDTO> result=new ArrayList<>();for(KnowledgeChunkDTO c:lexical(keyword))if(key.equals(c.metadata.policyKey))result.add(c);return result;
    }
    public static void main(String[] args)throws Exception {
        if(!"true".equals(System.getenv("RAG_TEST_DATABASE_APPROVED")) || !"development".equals(System.getenv("RAG_ENVIRONMENT")) ||
            !"127.0.0.1".equals(System.getenv("DB_HOST")) || !"fpt_rag_test".equals(System.getenv("DB_NAME")))
            throw new IllegalStateException("Refusing database writes: require an approved fresh loopback fpt_rag_test database");
        if(count("knowledge_documents")!=0 || count("knowledge_chunks")!=0 || count("knowledge_index_jobs")!=0)throw new IllegalStateException("Fresh empty test database required; existing data was preserved");
        final KnowledgeDocumentDTO[] doc={null};
        test("CRUD draft and immutable revision",()->{
            doc[0]=DOCS.save(draft("Hướng dẫn chuyển địa chỉ","Anh/chị liên hệ hỗ trợ để kiểm tra địa chỉ mới.","draft"),1);
            check(doc[0].id>0 && doc[0].version==1 && DOCS.find(doc[0].id).content.equals(doc[0].content) && DOCS.list(10).size()==1 && count("knowledge_index_jobs")==0);
        });
        test("invalid actor rejected without writes",()->{long n=count("knowledge_documents");try{DOCS.save(draft("Hướng dẫn","Nội dung.","published"),0);throw new AssertionError();}catch(RagException e){check(e.code==RagException.Code.INVALID_DOCUMENT);}check(count("knowledge_documents")==n);});
        test("publish enqueues job atomically and stale edit is rejected",()->{
            KnowledgeDocumentDTO old=doc[0];doc[0]=DOCS.save(edit(old,old.content,"published",old.source),1);check(doc[0].version==2 && count("knowledge_index_jobs")==1);
            try{DOCS.save(edit(old,"Nội dung lỗi thời.","published",old.source),1);throw new AssertionError();}catch(RagException e){check(e.code==RagException.Code.STALE_VERSION);}
            check(DOCS.find(old.id).version==2 && count("knowledge_index_jobs")==1);
        });
        test("index publishes complete current revision and vector",()->{drain();check("ready".equals(DOCS.find(doc[0].id).indexingStatus) && count("knowledge_chunks")==1 && lexical("thủ tục chuyển địa chỉ FPT?").size()==1);});
        test("unpublished document is not retrievable",()->{DOCS.save(draft("Hướng dẫn chuyển địa chỉ riêng","Đây là tài liệu chưa duyệt về chuyển địa chỉ.","draft"),1);for(KnowledgeChunkDTO c:lexical("chuyển địa chỉ"))check(c.documentId==doc[0].id);});
        test("withdrawn publication is excluded even when its indexed chunks remain",()->{
            try(Connection c=DB.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET status=? WHERE id=?")) {
                p.setString(1,"draft");p.setLong(2,doc[0].id);p.executeUpdate();check(lexical("chuyển địa chỉ").isEmpty());
                p.setString(1,"published");p.executeUpdate();
            }
        });
        test("content-hash dedup reuses embedding on source-only revision",()->{int calls=EMBEDDINGS.calls;doc[0]=DOCS.save(edit(doc[0],doc[0].content,"published","Nguồn kiểm thử phiên bản mới"),1);check(lexical("chuyển địa chỉ").isEmpty());drain();check(EMBEDDINGS.calls==calls && count("knowledge_chunks")==1 && CHUNKS.current(lexical("chuyển địa chỉ").get(0)));});
        test("expired documents excluded by database clock",()->{setDate(doc[0].id,"expires_at",Instant.now().minusSeconds(1));check(lexical("chuyển địa chỉ").isEmpty());setDate(doc[0].id,"expires_at",null);});
        test("not-yet-effective documents excluded",()->{setDate(doc[0].id,"effective_at",Instant.now().plusSeconds(60));check(lexical("chuyển địa chỉ").isEmpty());setDate(doc[0].id,"effective_at",null);});
        test("semantic synonyms and lexical relevance use real pgvector/FTS",()->{
            DOCS.save(draft("Hướng dẫn xử lý mất mạng","Anh/chị kiểm tra nguồn điện và kết nối dây trước khi liên hệ hỗ trợ.","published"),1);drain();
            List<KnowledgeChunkDTO> semantic=CHUNKS.search("di dời",EMBEDDINGS.query("di dời"),CONFIG.model,768,12);
            check(!semantic.isEmpty() && semantic.get(0).documentId==doc[0].id && lexical("mất mạng").get(0).title.contains("mất mạng"));
        });
        test("archived source cannot be served or revalidated",()->{KnowledgeChunkDTO old=lexical("chuyển địa chỉ").get(0);doc[0]=DOCS.archive(doc[0].id,doc[0].version,1);check(lexical("chuyển địa chỉ").isEmpty() && !CHUNKS.current(old));});
        test("partial index failure rolls back all new chunks",()->{
            long before=count("knowledge_chunks");KnowledgeDocumentDTO d=DOCS.save(draft("Hướng dẫn dài",String.join(" ",Collections.nCopies(100,"Anh/chị kiểm tra dây mạng và nguồn điện an toàn.")),"published"),1);
            KnowledgeChunkDAO fail=new KnowledgeChunkDAO(DB){int inserts;@Override public void insert(Connection c,KnowledgeDocumentDTO d,int n,String t,float[] v,EmbeddingService e)throws Exception{super.insert(c,d,n,t,v,e);if(++inserts==2)throw new SQLException("mock write failure");}};
            new KnowledgeIndexingService(DB,DOCS,fail,JOBS,EMBEDDINGS).runNext();check(count("knowledge_chunks")==before && !"ready".equals(DOCS.find(d.id).indexingStatus));
        });
        test("two workers claim distinct jobs",()->{
            DOCS.save(draft("Hướng dẫn nguồn điện","Anh/chị kiểm tra nguồn điện an toàn.","published"),1);DOCS.save(draft("Hướng dẫn dây mạng","Anh/chị kiểm tra dây mạng an toàn.","published"),1);
            ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<KnowledgeIndexJobDTO> a=pool.submit(JOBS::claim),b=pool.submit(JOBS::claim);KnowledgeIndexJobDTO x=a.get(),y=b.get();check(x!=null && y!=null && x.id!=y.id);
                try(Connection c=DB.open()){KnowledgeIndexJobDAO.finish(c,x,"superseded");KnowledgeIndexJobDAO.finish(c,y,"superseded");}
            }finally{pool.shutdownNow();}
        });
        test("update during embedding never publishes stale chunks",()->{
            KnowledgeDocumentDTO d=DOCS.save(draft("Hướng dẫn chuyển dịch vụ","Anh/chị liên hệ hỗ trợ trước khi chuyển dịch vụ.","published"),1);
            EMBEDDINGS.before=()->{try{DOCS.save(edit(d,"Anh/chị kiểm tra địa chỉ mới với tư vấn viên.","published",d.source),1);}catch(Exception e){throw new RuntimeException(e);}};
            WORKER.runNext();check(!"ready".equals(DOCS.find(d.id).indexingStatus));drain();check("ready".equals(DOCS.find(d.id).indexingStatus));
            for(KnowledgeChunkDTO c:lexical("chuyển dịch vụ"))if(c.documentId==d.id)check(c.documentVersion==d.version+1 && !c.content.contains("trước khi chuyển dịch vụ"));
        });
        test("expired worker lease cannot overwrite a renewed claim",()->{
            DOCS.save(draft("Hướng dẫn kiểm tra an toàn","Anh/chị kiểm tra kết nối an toàn.","published"),1);
            KnowledgeIndexJobDTO old=JOBS.claim();check(old!=null);
            try(Connection c=DB.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_index_jobs SET lease_until=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?")){p.setLong(1,old.id);p.executeUpdate();}
            try{JOBS.renew(old);throw new AssertionError();}catch(RagException e){check(e.code==RagException.Code.STALE_VERSION);}
            KnowledgeIndexJobDTO fresh=JOBS.claim();check(fresh!=null && fresh.id==old.id && !fresh.leaseToken.equals(old.leaseToken) && fresh.attempts==old.attempts+1);
            JOBS.fail(old,RagException.Code.INVALID_VECTOR);
            try(Connection c=DB.open()){check(JOBS.owns(c,fresh));KnowledgeIndexJobDAO.finish(c,fresh,"superseded");}
        });
        test("exhausted crashed jobs report failed without infinite retry",()->{
            KnowledgeDocumentDTO d=DOCS.save(draft("Hướng dẫn quá hạn xử lý","Anh/chị kiểm tra kết nối trước khi gọi hỗ trợ.","published"),1);
            try(Connection c=DB.open();PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_index_jobs SET status='running',attempts=3,lease_until=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE document_id=?")){p.setLong(1,d.id);p.executeUpdate();}
            check(JOBS.claim()==null && "failed".equals(DOCS.find(d.id).indexingStatus));
        });
        test("RLS denies untrusted REST-like role even with accidental SELECT grants",()->{
            try(Connection c=DB.open();Statement s=c.createStatement()) {
                s.execute("CREATE ROLE fpt_rag_untrusted NOLOGIN");s.execute("GRANT USAGE ON SCHEMA public TO fpt_rag_untrusted");
                s.execute("GRANT SELECT ON public.knowledge_documents,public.knowledge_chunks,public.knowledge_index_jobs TO fpt_rag_untrusted");
                s.execute("SET ROLE fpt_rag_untrusted");
                try(ResultSet r=s.executeQuery("SELECT count(*) FROM public.knowledge_documents")){r.next();check(r.getLong(1)==0);}
                try(ResultSet r=s.executeQuery("SELECT count(*) FROM public.knowledge_chunks")){r.next();check(r.getLong(1)==0);}
                try(ResultSet r=s.executeQuery("SELECT count(*) FROM public.knowledge_index_jobs")){r.next();check(r.getLong(1)==0);}
            }
        });
        final KnowledgeDocumentDTO[] lineage=new KnowledgeDocumentDTO[3];
        test("reviewed policy metadata persists and returns publication proof",()->{
            lineage[0]=DOCS.save(policy("Hướng dẫn lineageprobe","Anh/chị kiểm tra nguồn điện an toàn.","test.lineage","official",null),1);drain();
            KnowledgeChunkDTO c=subject("lineageprobe","test.lineage").get(0);
            check(c.metadata.approvedBy==1 && c.metadata.approvedAt!=null && "official".equals(c.metadata.authority) && CHUNKS.current(c));
        });
        test("explicit replacement excludes obsolete policy before and after indexing",()->{
            KnowledgeChunkDTO old=subject("lineageprobe","test.lineage").get(0);
            lineage[1]=DOCS.save(policy("Hướng dẫn lineageprobe","Anh/chị kiểm tra kết nối dây an toàn.","test.lineage","official",lineage[0].id),1);
            check(subject("lineageprobe","test.lineage").isEmpty() && !CHUNKS.current(old));drain();
            check(subject("lineageprobe","test.lineage").size()==1 && subject("lineageprobe","test.lineage").get(0).documentId==lineage[1].id);
        });
        test("future replacement waits and expired replacement does not revive obsolete policy",()->{
            setDate(lineage[1].id,"effective_at",Instant.now().plusSeconds(60));check(subject("lineageprobe","test.lineage").get(0).documentId==lineage[0].id);setDate(lineage[1].id,"effective_at",null);
            setDate(lineage[1].id,"expires_at",Instant.now().minusMillis(1));check(subject("lineageprobe","test.lineage").isEmpty());setDate(lineage[1].id,"expires_at",null);
        });
        test("transitive supersession remains effective after intermediate archive",()->{
            lineage[2]=DOCS.save(policy("Hướng dẫn lineageprobe","Anh/chị liên hệ hỗ trợ để kiểm tra kết nối.","test.lineage","official",lineage[1].id),1);drain();
            DOCS.archive(lineage[1].id,lineage[1].version,1);
            List<KnowledgeChunkDTO> rows=subject("lineageprobe","test.lineage");check(rows.size()==1 && rows.get(0).documentId==lineage[2].id && CHUNKS.current(rows.get(0)));
        });
        test("invalid subject authority downgrade and self-cycle cannot supersede",()->{
            long before=count("knowledge_documents");
            for(KnowledgeDocumentDTO bad:Arrays.asList(policy("Hướng dẫn","Thông tin giả.","test.other","official",lineage[2].id),policy("Hướng dẫn","Thông tin giả.","test.lineage","admin_reviewed",lineage[2].id),
                new KnowledgeDocumentDTO(lineage[2].id,lineage[2].version,lineage[2].title,"guide",lineage[2].content,lineage[2].source,"published",null,null,"pending","test.lineage","official",lineage[2].id))) {
                try{DOCS.save(bad,1);throw new AssertionError();}catch(RagException e){check(e.code==RagException.Code.INVALID_DOCUMENT);}
            }check(count("knowledge_documents")==before);
        });
        test("equal authoritative conflicts are rejected including peers outside candidate window",()->{
            KnowledgeDocumentDTO a=DOCS.save(policy("Hướng dẫn conflictprobe","Anh/chị dùng kết nối dây.","test.conflict","official",null),1);
            KnowledgeDocumentDTO b=DOCS.save(policy("Hướng dẫn conflictprobe","Anh/chị không dùng kết nối dây.","test.conflict","official",null),1);drain();
            List<KnowledgeChunkDTO> rows=subject("conflictprobe","test.conflict");check(rows.size()==2 && KnowledgeSourceSelection.select(rows).issue==KnowledgeSourceSelection.Issue.SOURCE_CONFLICT && !CHUNKS.current(rows.get(0)));
            DOCS.archive(b.id,b.version,1);rows=subject("conflictprobe","test.conflict");check(rows.size()==1 && rows.get(0).documentId==a.id && CHUNKS.current(rows.get(0)));
        });
        test("explicit official authority overrides reviewed source without trusting recency",()->{
            KnowledgeDocumentDTO official=DOCS.save(policy("Hướng dẫn authorityprobe","Anh/chị kiểm tra kết nối dây.","test.authority","official",null),1);drain();
            DOCS.save(policy("Hướng dẫn authorityprobe","Anh/chị liên hệ hỗ trợ.","test.authority","admin_reviewed",null),1);drain();
            List<KnowledgeChunkDTO> rows=subject("authorityprobe","test.authority");KnowledgeSourceSelection.Result selected=KnowledgeSourceSelection.select(rows);
            check(rows.size()==2 && selected.chunks.size()==1 && selected.chunks.get(0).documentId==official.id && CHUNKS.current(selected.chunks.get(0)));
            for(KnowledgeChunkDTO c:rows)if(c.documentId!=official.id)check(!CHUNKS.current(c));
        });
        System.out.println("RagPostgresTest: "+passed+" integration tests passed with real PostgreSQL/pgvector and mocked embeddings");
    }
}
