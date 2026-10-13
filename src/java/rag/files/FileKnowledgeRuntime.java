package rag.files;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import dao.FileKnowledgeDAO;

/** Loads repository knowledge and optionally syncs it without an Admin UI approval step. */
@WebListener
public final class FileKnowledgeRuntime implements ServletContextListener {
    private static volatile ExecutorService executor;
    private static final AtomicBoolean running=new AtomicBoolean();
    private static volatile String status="NOT_INITIALIZED";
    public void contextInitialized(ServletContextEvent event){
        try{FileKnowledgeCatalog.install(FileKnowledgeCatalog.load(event.getServletContext()));status="READY_TO_SYNC";}
        catch(Exception e){FileKnowledgeCatalog.install(null);status="INVALID_DEPLOYED_FILES";event.getServletContext().log("[File KB] deployed catalog rejected; check file metadata/content offline");return;}
        executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"repository-knowledge-sync");t.setDaemon(true);return t;});
        try{
            int actor=autoSyncActor(System.getenv());
            if(actor>0){status="AUTO_SYNC_QUEUED";sync(actor);}
        }catch(Exception e){status="AUTO_SYNC_BLOCKED";event.getServletContext().log("[File KB] automatic sync disabled: configure KB_FILE_AUTO_SYNC and a valid KB_FILE_SYNC_ACTOR_ID");}
    }
    /** The actor is audit attribution for a reviewed Git deployment, not an interactive approval. */
    public static int autoSyncActor(Map<String,String> env){
        if(env==null||!"true".equalsIgnoreCase(env.get("KB_FILE_AUTO_SYNC")))return 0;
        String raw=env.get("KB_FILE_SYNC_ACTOR_ID");
        try{int actor=Integer.parseInt(raw==null?"":raw.trim());if(actor<=0)throw new IllegalArgumentException();return actor;}
        catch(Exception e){throw new IllegalArgumentException("KB_FILE_SYNC_ACTOR_ID phải là ID Admin dương");}
    }
    public static String status(){return status;}
    public static void sync(int actor){
        if(actor<=0||executor==null)throw new IllegalStateException("Catalog chưa sẵn sàng");
        FileKnowledgeCatalog catalog=FileKnowledgeCatalog.deployed();if(!running.compareAndSet(false,true))throw new IllegalStateException("Đang đồng bộ");status="SYNCING";
        try{executor.submit(()->{try{status="SYNCED "+FileKnowledgeSync.sync(catalog,new FileKnowledgeDAO(),actor)+" — chưa xác nhận RAG live";}catch(Exception e){status="SYNC_FAILED — kiểm tra migration 008/009, version và source policy";}finally{running.set(false);}});}
        catch(RuntimeException e){running.set(false);throw e;}
    }
    public void contextDestroyed(ServletContextEvent e){ExecutorService service=executor;executor=null;if(service!=null)service.shutdownNow();FileKnowledgeCatalog.install(null);}
}
