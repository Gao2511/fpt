package rag;

import dao.KnowledgeChunkDAO;
import dto.KnowledgeChunkDTO;
import java.util.*;
import java.time.Instant;

/** No document/result cache: status, dates and revisions are checked on every retrieval. */
public class KnowledgeRetrievalService {
    private final RagConfig config;
    private final KnowledgeChunkDAO chunks;
    private final EmbeddingService embeddings;
    private final boolean strict;
    public KnowledgeRetrievalService(RagConfig config,KnowledgeChunkDAO chunks,EmbeddingService embeddings) {this(config,chunks,embeddings,false);}
    public KnowledgeRetrievalService(RagConfig config,KnowledgeChunkDAO chunks,EmbeddingService embeddings,boolean strict) {this.config=config;this.chunks=chunks;this.embeddings=embeddings;this.strict=strict;}
    public boolean requiresGrounding(){return strict;}
    public static KnowledgeRetrievalService disabled(){return new KnowledgeRetrievalService(RagConfig.disabled(),null,null);}
    public boolean enabled(){return config.enabled;}
    public List<KnowledgeChunkDTO> retrieve(String question) {
        return lookup(question).chunks;
    }
    public KnowledgeSourceSelection.Result lookup(String question) {
        if(!config.enabled || chunks==null || question==null || question.trim().isEmpty() || question.length()>2000)return KnowledgeSourceSelection.empty(KnowledgeSourceSelection.Issue.NONE);
        String provider="STORED_LEXICAL";
        RagRequestEvidence.retrieval(provider,0);
        float[] vector=null;
        if(config.callsApproved && config.modelVerified && embeddings!=null)try{RagRequestEvidence.retrieval(embeddings.providerKind()+"_ATTEMPTED",0);vector=embeddings.query(question);provider=embeddings.providerKind();}
        catch(RagException e){System.err.println("[RAG query embedding] error="+e.code);if(strict)return KnowledgeSourceSelection.empty(KnowledgeSourceSelection.Issue.UNAVAILABLE);}
        try {
            java.util.List<KnowledgeChunkDTO> matches=chunks.search(question,vector,config.model,config.dimensions,12);
            RagRequestEvidence.retrieval(provider,matches.size());
            KnowledgeSourceSelection.Result result=KnowledgeSourceSelection.select(matches,question);
            for(Map.Entry<KnowledgeContentPolicy.Reason,Integer> entry:result.rejected.entrySet())System.err.println("[RAG filter] reason="+entry.getKey()+" count="+entry.getValue());
            return result;
        }catch(Exception e){System.err.println("[RAG retrieval] unavailable");return KnowledgeSourceSelection.empty(KnowledgeSourceSelection.Issue.UNAVAILABLE);}
    }
    public boolean current(KnowledgeChunkDTO chunk) {
        if(!config.enabled || chunks==null || !KnowledgeContentPolicy.allowed(chunk))return false;
        try{return chunks.current(chunk);}catch(Exception e){return false;}
    }
}
