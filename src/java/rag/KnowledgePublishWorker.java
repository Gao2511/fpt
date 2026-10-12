package rag;

import dao.KnowledgeLifecycleDAO;
import java.util.Map;
import java.util.concurrent.*;
import javax.servlet.*;
import javax.servlet.annotation.WebListener;

/** Opt-in queue worker. One POST/slot, no metadata GET, redirects or provider retries. */
@WebListener
public final class KnowledgePublishWorker implements ServletContextListener {
    private ScheduledExecutorService executor;
    public static boolean authorized(Map<String,String> env) {
        return "true".equalsIgnoreCase(env.get("KB_GEMINI_WORKER_ENABLED"))
            && "true".equalsIgnoreCase(env.get("RAG_EMBEDDING_CALLS_APPROVED"))
            && "true".equalsIgnoreCase(env.get("KB_GEMINI_FREE_TIER_VERIFIED"))
            && "true".equalsIgnoreCase(env.get("KB_GEMINI_MODEL_VERIFIED"))
            && positive(env,"KB_EMBEDDING_RPM") && positive(env,"KB_EMBEDDING_TPM")
            && positive(env,"KB_EMBEDDING_RPD") && positive(env,"KB_EMBEDDING_REQUEST_BUDGET")
            && env.get("RAG_EMBEDDING_API_KEY")!=null && !env.get("RAG_EMBEDDING_API_KEY").trim().isEmpty()
            && !rag.runtime.GeminiRuntimeConfig.smokePlanConfigured(env);
    }
    public void contextInitialized(ServletContextEvent event) {
        final Map<String,String> env=System.getenv();if(!authorized(env))return;
        final int rpm,tpm,rpd,budget;
        try {
            rpm=required(env,"KB_EMBEDDING_RPM");tpm=required(env,"KB_EMBEDDING_TPM");rpd=required(env,"KB_EMBEDDING_RPD");budget=required(env,"KB_EMBEDDING_REQUEST_BUDGET");
        }catch(Exception invalid){event.getServletContext().log("[KB worker] disabled: explicit quota/budget configuration required");return;}
        final EmbeddingService embeddings=new GeminiEmbeddingService(new RagConfig(true,true,true,"gemini-embedding-001",768),env.get("RAG_EMBEDDING_API_KEY"),GeminiEmbeddingService.httpTransport(),true,1);
        executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"kb-publish-worker");t.setDaemon(true);return t;});
        executor.scheduleWithFixedDelay(()->{
            try{new KnowledgeLifecycleDAO().processOne(embeddings,rpm,tpm,rpd,budget);}
            catch(Exception error){event.getServletContext().log("[KB worker] blocked: "+(error instanceof RagException?((RagException)error).code.name():"DATABASE"));}
        },5,5,TimeUnit.SECONDS);
    }
    private static int required(Map<String,String> env,String key){int value=Integer.parseInt(env.get(key));if(value<=0)throw new IllegalArgumentException();return value;}
    private static boolean positive(Map<String,String> env,String key){try{return required(env,key)>0;}catch(Exception e){return false;}}
    public void contextDestroyed(ServletContextEvent event){if(executor!=null)executor.shutdownNow();}
}
