package dto;
import java.time.Instant;

public final class KnowledgeChunkDTO {
    /** Database provenance, never populated from model output. Missing proof fails closed. */
    public static final class SourceMetadata {
        public final String status, category, policyKey, authority;
        public final int approvedBy;
        public final Instant approvedAt;
        public final boolean superseded;
        public SourceMetadata(String status,String category,int actor,Instant approvedAt,String policyKey,String authority,boolean superseded) {
            this.status=status;this.category=category;approvedBy=actor;this.approvedAt=approvedAt;
            this.policyKey=policyKey;this.authority=authority;this.superseded=superseded;
        }
    }
    public final SourceMetadata metadata;
    public final long id, documentId, documentVersion;
    public final String title, content, source, documentHash;
    public final double score;
    public final Instant effectiveAt, expiresAt;
    public long getDocumentId(){return documentId;} public String getTitle(){return title;}
    public String getContent(){return content;} public String getSource(){return source;} public double getScore(){return score;}
    public KnowledgeChunkDTO(long id,long documentId,long version,String hash,String title,String content,String source,
            Instant effectiveAt,Instant expiresAt,double score) {
        this(id,documentId,version,hash,title,content,source,effectiveAt,expiresAt,score,null);
    }
    public KnowledgeChunkDTO(long id,long documentId,long version,String hash,String title,String content,String source,
            Instant effectiveAt,Instant expiresAt,double score,SourceMetadata metadata) {
        this.id=id;this.documentId=documentId;this.documentVersion=version;documentHash=hash;
        this.title=title;this.content=content;this.source=source;this.effectiveAt=effectiveAt;this.expiresAt=expiresAt;this.score=score;
        this.metadata=metadata;
    }
}
