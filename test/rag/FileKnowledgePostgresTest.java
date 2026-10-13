package rag;
import dao.*;
import rag.files.*;
import java.util.*;
import java.sql.*;

/** Optional focused verification ONLY in an approved loopback fpt_rag_test database. */
public final class FileKnowledgePostgresTest {
    static int pass;static RagDatabase db=new RagDatabase();
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);pass++;System.out.println("PASS FILE-KB-PG "+name);}
    static long count(String sql)throws Exception{try(Connection c=db.open();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    static final class FakeGemini implements EmbeddingService {
        public String model(){return "gemini-embedding-001";}public String providerKind(){return "GEMINI";}public int dimensions(){return 768;}
        public float[] document(String title,String text)throws RagException{return new MockEmbeddingProvider().document(title,text);}
        public float[] query(String query){throw new AssertionError("No query API");}
    }
    static void drain()throws Exception{for(int i=0;i<20;i++)if(!new KnowledgeLifecycleDAO(db).processOne(new FakeGemini(),100,100000,1000,1000))return;throw new AssertionError("worker did not drain");}
    static FileKnowledgeCatalog catalog(int version,boolean active,String body)throws Exception{String raw=FileKnowledgeOfflineTest.text("wifi-file",version,active,body);return new FileKnowledgeCatalog(Arrays.asList(KnowledgeFile.parse(raw,"faq/wifi.md",VectorValues.hash(raw))));}
    static void sync(FileKnowledgeCatalog c)throws Exception{FileKnowledgeCatalog.install(c);FileKnowledgeSync.sync(c,new FileKnowledgeDAO(db),101);}
    public static void main(String[] args)throws Exception{
        if(!"127.0.0.1".equals(System.getenv("DB_HOST"))||!"fpt_rag_test".equals(System.getenv("DB_NAME"))||!"true".equals(System.getenv("RAG_TEST_DATABASE_APPROVED"))||!"development".equals(System.getenv("RAG_ENVIRONMENT"))||!"false".equals(System.getenv("RAG_ENABLED")))throw new IllegalStateException("Isolated database only");
        FileKnowledgeCatalog first=catalog(1,true,"Kiểm tra WiFi, vị trí modem và vật cản.");sync(first);
        check(count("SELECT COUNT(*) FROM knowledge_file_sources")==1&&count("SELECT COUNT(*) FROM knowledge_file_revisions")==1,"file and immutable revision persisted once");
        check(new AdminKnowledgeDAO(db).readOnlyRetrieval("WiFi modem").chunks.isEmpty(),"queued source not prematurely retrieved");
        drain();long old=new FileKnowledgeDAO(db).find("wifi-file").documentId;
        check(new AdminKnowledgeDAO(db).readOnlyRetrieval("WiFi modem").chunks.stream().anyMatch(c->c.documentId==old),"indexed fixture becomes eligible");
        long requests=count("SELECT COUNT(*) FROM knowledge_embedding_requests");sync(first);drain();
        check(count("SELECT COUNT(*) FROM knowledge_file_revisions")==1&&count("SELECT COUNT(*) FROM knowledge_embedding_requests")==requests,"restart/sync unchanged file has no duplicate document or API slot");
        FileKnowledgeCatalog second=catalog(2,true,"Kiểm tra WiFi, đặt modem tại vị trí thông thoáng.");sync(second);
        check(new AdminKnowledgeDAO(db).readOnlyRetrieval("WiFi modem").chunks.stream().anyMatch(c->c.documentId==old),"active old version preserved while successor pending");drain();
        long latest=new FileKnowledgeDAO(db).find("wifi-file").documentId;
        check(!new AdminKnowledgeDAO(db).findDocument(old).status.equals("published")&&new AdminKnowledgeDAO(db).readOnlyRetrieval("WiFi modem").chunks.stream().allMatch(c->c.documentId!=old),"atomic replacement excludes old policy");
        try(FileKnowledgeDAO one=new FileKnowledgeDAO(db);FileKnowledgeDAO two=new FileKnowledgeDAO(db)){check(one.lock()&&!two.lock(),"session DB advisory lock denies concurrent sync");}
        new FileKnowledgeDAO(db).emergencyDisable(latest,101);sync(catalog(3,true,"Kiểm tra WiFi và modem mới."));
        check(new FileKnowledgeDAO(db).find("wifi-file").disabled&&new AdminKnowledgeDAO(db).readOnlyRetrieval("WiFi modem").chunks.isEmpty(),"emergency disable survives deploy/sync");
        FileKnowledgeCatalog.install(new FileKnowledgeCatalog(Collections.emptyList()));check(!FileKnowledgeCatalog.allowedSource("repo:wifi-file/v1"),"deleted catalog excludes history immediately");
        check(count("SELECT COUNT(*) FROM pg_class WHERE relname IN ('knowledge_file_sources','knowledge_file_revisions') AND relrowsecurity")==2,"RLS enabled on file tables");
        System.out.println("FILE-KB PostgreSQL: "+pass+" PASS; actual Gemini requests=0; fixture embeddings mocked");
    }
}
