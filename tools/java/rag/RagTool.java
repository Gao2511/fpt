package rag;

import dao.*;
import java.sql.*;

/** Operator-only tool, deliberately outside production src/WAR. No migration or automatic activation. */
public final class RagTool {
    public static void main(String[] args) {
        try {run(args);}catch(Exception e){System.err.println("RAG operation failed: "+(e instanceof RagException?((RagException)e).code:"CONFIGURATION_OR_DATABASE"));System.exit(1);}
    }
    private static void run(String[] args)throws Exception {
        String environment=System.getenv("RAG_ENVIRONMENT");
        if(!"development".equals(environment) && !"staging".equals(environment))throw new IllegalStateException();
        if(args.length==0)throw new IllegalArgumentException();RagConfig config=RagConfig.from(System.getenv());
        String key=System.getenv("RAG_EMBEDDING_API_KEY");GeminiEmbeddingService embeddings=new GeminiEmbeddingService(config,key);
        if("verify-model".equals(args[0])) {embeddings.verifyAccount(System.currentTimeMillis()+10000);System.out.println("Model metadata access verified; no embedding generated. Quota/pricing require operator review.");return;}
        RagDatabase db=new RagDatabase();
        if("check".equals(args[0])) {
            try(Connection c=db.open()){c.setReadOnly(true);try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='vector'), to_regclass('public.knowledge_documents') IS NOT NULL, to_regclass('public.knowledge_chunks') IS NOT NULL, to_regclass('public.knowledge_index_jobs') IS NOT NULL, (SELECT count(*)=3 FROM information_schema.columns WHERE table_schema='public' AND table_name='knowledge_documents' AND column_name IN ('policy_key','source_authority','supersedes_document_id'))")){r.next();System.out.println("vector="+r.getBoolean(1)+" documents="+r.getBoolean(2)+" chunks="+r.getBoolean(3)+" jobs="+r.getBoolean(4)+" sourcePolicy="+r.getBoolean(5));}}return;
        }
        if(!"index".equals(args[0]) || !"true".equals(System.getenv("RAG_DATABASE_APPROVED")) || !config.callsApproved || !config.modelVerified)throw new IllegalStateException();
        int limit=args.length>1?Integer.parseInt(args[1]):1;if(limit<1 || limit>10)throw new IllegalArgumentException();
        KnowledgeIndexingService worker=new KnowledgeIndexingService(db,new KnowledgeDocumentDAO(db,config),new KnowledgeChunkDAO(db),new KnowledgeIndexJobDAO(db),embeddings);
        int processed=0;while(processed<limit && worker.runNext())processed++;System.out.println("Processed jobs="+processed+"; consult safe job status/error_code for success.");
    }
}
