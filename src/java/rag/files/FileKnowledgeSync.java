package rag.files;

import java.util.*;

/** Coordinates import/deduplication. Indexing remains in the durable Phase 5.7 worker. */
public final class FileKnowledgeSync {
    public static final class State {
        public final long version,documentId;public final String sha,status;public final boolean disabled;
        public State(long v,long d,String h,String s,boolean blocked){version=v;documentId=d;sha=h;status=s;disabled=blocked;}
    }
    public interface Store extends AutoCloseable {
        boolean lock()throws Exception;State find(String id)throws Exception;
        default void touchPath(KnowledgeFile file)throws Exception {}
        State stage(KnowledgeFile file,int actor)throws Exception;
        void queue(State state,int actor)throws Exception;
        int retire(Set<String> retained,int actor)throws Exception;
        void close()throws Exception;
    }
    public static Map<String,Integer> sync(FileKnowledgeCatalog catalog,Store store,int actor)throws Exception {
        if(actor<=0)throw new IllegalArgumentException("Yêu cầu Admin");
        Map<String,Integer> result=new LinkedHashMap<>();result.put("queued",0);result.put("unchanged",0);result.put("emergencyDisabled",0);
        try(Store owned=store){if(!owned.lock())throw new IllegalStateException("Một instance khác đang đồng bộ");
            // Validate all monotonic versions before any writes; rollback of deployment cannot revert policy.
            for(KnowledgeFile f:catalog.files.values()){State old=owned.find(f.id);if(old!=null&&(!old.sha.equals(f.sha)&&f.version<=old.version||f.version<old.version||f.effectiveNow()&&Arrays.asList("MISSING","INACTIVE").contains(old.status)&&f.version<=old.version))throw new IllegalArgumentException("Tăng version khi thay đổi/khôi phục file: "+f.id);}
            List<KnowledgeFile> ordered=new ArrayList<>();Set<String> done=new HashSet<>();
            for(KnowledgeFile f:catalog.files.values())order(f,catalog,done,ordered);
            for(KnowledgeFile f:ordered)if(f.effectiveNow()){
                State old=owned.find(f.id);
                if(old!=null&&old.disabled){result.put("emergencyDisabled",result.get("emergencyDisabled")+1);continue;}
                if(old!=null&&old.sha.equals(f.sha)){owned.touchPath(f);if("PENDING_QUEUE".equals(old.status))owned.queue(old,actor);result.put("unchanged",result.get("unchanged")+1);continue;}
                State created=owned.stage(f,actor);owned.queue(created,actor);result.put("queued",result.get("queued")+1);
            }
            result.put("retired",owned.retire(catalog.eligibleIds(),actor));return result;
        }
    }
    private static void order(KnowledgeFile f,FileKnowledgeCatalog catalog,Set<String> done,List<KnowledgeFile> out){if(!done.add(f.id))return;KnowledgeFile prior=catalog.files.get(f.replaces);if(prior!=null)order(prior,catalog,done,out);out.add(f);}
}
