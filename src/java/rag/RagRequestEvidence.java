package rag;

import dto.KnowledgeChunkDTO;
import java.time.Instant;
import java.util.*;

/** Bounded per-instance evidence: no question, answer, session ID or customer personal data. */
public final class RagRequestEvidence implements AutoCloseable {
    private static final ThreadLocal<RagRequestEvidence> ACTIVE=new ThreadLocal<>();
    private static final Deque<Map<String,Object>> RECENT=new ArrayDeque<>();
    private final Map<String,Object> row=new LinkedHashMap<>();
    private boolean lastGemini;
    private RagRequestEvidence(boolean enabled){row.put("id",UUID.randomUUID().toString());row.put("timestamp",Instant.now().toString());row.put("enabled",enabled);row.put("attempted",false);row.put("matches",0);row.put("passed",0);row.put("provider","NONE");row.put("sources",Collections.emptyList());row.put("grounded",false);row.put("usedContext",false);}
    public static RagRequestEvidence begin(boolean enabled){RagRequestEvidence e=new RagRequestEvidence(enabled);ACTIVE.set(e);return e;}
    public static void retrieval(String provider,int count){RagRequestEvidence e=ACTIVE.get();if(e!=null){e.row.put("attempted",true);e.row.put("provider",provider);e.row.put("matches",count);}}
    public static void sent(List<KnowledgeChunkDTO> chunks,boolean gemini){RagRequestEvidence e=ACTIVE.get();if(e==null)return;e.lastGemini=gemini;if(!gemini)return;
        List<Map<String,Object>> sources=new ArrayList<>();for(KnowledgeChunkDTO chunk:chunks){Map<String,Object> source=new LinkedHashMap<>();source.put("id",chunk.documentId);source.put("title",chunk.title);sources.add(source);}e.row.put("passed",chunks.size());e.row.put("sources",sources);
    }
    public static void grounded(boolean used){RagRequestEvidence e=ACTIVE.get();if(e!=null){e.row.put("grounded",true);e.row.put("usedContext",used && e.lastGemini && ((Integer)e.row.get("passed"))>0);}}
    public void close(){ACTIVE.remove();synchronized(RECENT){purge();RECENT.addFirst(Collections.unmodifiableMap(new LinkedHashMap<>(row)));while(RECENT.size()>100)RECENT.removeLast();}}
    private static void purge(){while(!RECENT.isEmpty()&&Instant.parse((String)RECENT.peekLast().get("timestamp")).isBefore(Instant.now().minusSeconds(86400)))RECENT.removeLast();}
    public static List<Map<String,Object>> recent(){synchronized(RECENT){purge();return new ArrayList<>(RECENT);}}
}
