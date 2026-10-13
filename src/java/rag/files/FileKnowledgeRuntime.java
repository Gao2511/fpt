package rag.files;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import dao.FileKnowledgeDAO;

/** No automatic sync/network calls on startup. Admin authorizes a bounded background import. */
@WebListener
public final class FileKnowledgeRuntime implements ServletContextListener {
    private static volatile ExecutorService executor;
    private static final AtomicBoolean running=new AtomicBoolean();
    private static volatile String status="NOT_INITIALIZED";
    public void contextInitialized(ServletContextEvent event){
        try{FileKnowledgeCatalog.install(FileKnowledgeCatalog.load(event.getServletContext()));status="READY_TO_SYNC";}
        catch(Exception e){FileKnowledgeCatalog.install(null);status="INVALID_DEPLOYED_FILES";event.getServletContext().log("[File KB] deployed catalog rejected; check file metadata/content offline");return;}
        executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"repository-knowledge-sync");t.setDaemon(true);return t;});
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
