package rag.runtime;
import rag.*;
import dao.*;
import ai.consultation.*;
import ai.dto.*;
import ai.session.ChatSessionData;
import java.sql.*;

/** Explicit local services; constructing them sends no request and writes no database. */
public final class GeminiRuntime {
    public final GeminiRuntimeConfig config;public final EmbeddingService embeddings;public final KnowledgeRetrievalService retrieval;
    private final RagDatabase db;private final ApiRequestLedger ledger;private final ai.LLMProvider chatWire;
    public GeminiRuntime(GeminiRuntimeConfig config,RagDatabase db)throws Exception{this(config,db,new PostgresRequestLedger(db,config),null);}
    public GeminiRuntime(GeminiRuntimeConfig config,RagDatabase db,ApiRequestLedger ledger,EmbeddingService injected)throws Exception{
        this(config,db,ledger,injected,null);
    }
    /** Injected transports are for deterministic offline verification, never selected by environment. */
    public GeminiRuntime(GeminiRuntimeConfig config,RagDatabase db,ApiRequestLedger ledger,EmbeddingService injected,ai.LLMProvider chatWire)throws Exception{
        this.chatWire=chatWire;
        this.config=config;this.db=db;this.ledger=ledger;embeddings=injected==null?new RuntimeEmbeddingService(config,ledger):injected;
        if(!"GEMINI".equals(embeddings.providerKind())||!"gemini-embedding-001".equals(embeddings.model())||embeddings.dimensions()!=768)throw new RagException(RagException.Code.CONFIGURATION);
        if(config.isOnline())verifyOnlineDatabase(db);else verifyDatabase(db);
        retrieval=new KnowledgeRetrievalService(config.rag(),new KnowledgeChunkDAO(db,"GEMINI"),embeddings,true);
    }
    public static GeminiRuntime local()throws Exception{return new GeminiRuntime(GeminiRuntimeConfig.load(System.getenv()),new RagDatabase());}
    public static GeminiRuntime online()throws Exception{return new GeminiRuntime(GeminiRuntimeConfig.loadOnline(System.getenv()),new RagDatabase());}
    public static GeminiRuntime active()throws Exception{return new GeminiRuntime(GeminiRuntimeConfig.loadActive(System.getenv()),new RagDatabase());}
    public static void verifyDatabase(RagDatabase db)throws Exception{
        try(Connection c=db.open()){
            java.net.URI jdbc=new java.net.URI(c.getMetaData().getURL().substring(5));
            if(!"127.0.0.1".equals(jdbc.getHost())||!"/fpt_rag_test".equals(jdbc.getPath()))throw new RagException(RagException.Code.CONFIGURATION);
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database(),to_regclass('public.rag_api_requests') IS NOT NULL")){
                if(!r.next()||!"fpt_rag_test".equals(r.getString(1))||!r.getBoolean(2))throw new RagException(RagException.Code.CONFIGURATION);
            }
        }
    }
    public static void verifyOnlineDatabase(RagDatabase db)throws Exception{
        try(Connection c=db.open()){
            String url=c.getMetaData().getURL();
            if(url!=null&&url.startsWith("jdbc:")){
                java.net.URI jdbc=new java.net.URI(url.substring(5));
                if(utils.DBUtils.isLoopbackHost(jdbc.getHost()))throw new RagException(RagException.Code.CONFIGURATION);
            }
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery(
                "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='vector'),"
                +"to_regclass('public.rag_api_requests') IS NOT NULL,"
                +"to_regclass('public.rag_runtime_plans') IS NOT NULL,"
                +"to_regclass('public.knowledge_documents') IS NOT NULL,"
                +"to_regclass('public.knowledge_chunks') IS NOT NULL,"
                +"to_regclass('public.knowledge_index_jobs') IS NOT NULL")){
                if(!r.next()||!r.getBoolean(1)||!r.getBoolean(2)||!r.getBoolean(3)||!r.getBoolean(4)||!r.getBoolean(5)||!r.getBoolean(6))
                    throw new RagException(RagException.Code.CONFIGURATION);
            }
        }
    }
    public boolean index(long documentId)throws Exception{
        config.current();dto.KnowledgeDocumentDTO doc=new KnowledgeDocumentDAO(db,config.rag()).find(documentId);if(doc==null)throw new RagException(RagException.Code.INVALID_DOCUMENT);config.document(doc.id,doc.version,doc.contentHash);
        return new KnowledgeIndexingService(db,new KnowledgeDocumentDAO(db,config.rag()),new KnowledgeChunkDAO(db,"GEMINI"),new KnowledgeIndexJobDAO(db,"GEMINI",documentId),embeddings).runNext();
    }
    public ChatResponse chat(String question,ChatSessionData session,ProductCatalog catalog,String prompt)throws ai.exception.AIException{
        RuntimeGeminiProvider provider=chatWire==null?new RuntimeGeminiProvider(config,ledger):new RuntimeGeminiProvider(config,ledger,chatWire);
        ConsultationEngine.Target primary=new ConsultationEngine.Target(provider,config.chatKey(),"gemini-3.8-flash",null,"medium",config.maxChatTokens);
        ConsultationEngine.Target backup=config.backupEnabled?new ConsultationEngine.Target(provider,config.chatKey(),"gemini-3.5-flash-lite",null,"low",config.maxChatTokens):null;
        ChatOptions options=new ChatOptions(.3,config.maxChatTokens);options.setThinkingLevel("medium");options.setMaxProviderAttempts(config.backupEnabled?2:1);
        return new ConsultationEngine(retrieval).chat(question,session,catalog,primary,backup,options,prompt);
    }
}
