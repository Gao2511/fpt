package dto;

import java.time.Instant;
import rag.VectorValues;

/** Immutable document revision; publication requires a trusted operator/admin actor. */
public final class KnowledgeDocumentDTO {
    public final long id, version;
    public final String title, category, content, source, status, contentHash, indexingStatus;
    public final Instant effectiveAt, expiresAt;
    public final String policyKey, sourceAuthority;
    public final Long supersedesDocumentId;
    public final int approvedBy;
    public final Instant approvedAt;
    public KnowledgeDocumentDTO(long id,long version,String title,String category,String content,String source,
        String status,Instant effectiveAt,Instant expiresAt,String indexingStatus) {
        this(id,version,title,category,content,source,status,effectiveAt,expiresAt,indexingStatus,"","admin_reviewed",null);
    }
    public KnowledgeDocumentDTO(long id,long version,String title,String category,String content,String source,
        String status,Instant effectiveAt,Instant expiresAt,String indexingStatus,String policyKey,String authority,Long supersedes) {
        this(id,version,title,category,content,source,status,effectiveAt,expiresAt,indexingStatus,policyKey,authority,supersedes,0,null);
    }
    public KnowledgeDocumentDTO(long id,long version,String title,String category,String content,String source,
        String status,Instant effectiveAt,Instant expiresAt,String indexingStatus,String policyKey,String authority,Long supersedes,int actor,Instant approval) {
        this.id=id;this.version=version;this.title=title;this.category=category;this.content=content;this.source=source;
        this.status=status;this.effectiveAt=effectiveAt;this.expiresAt=expiresAt;this.indexingStatus=indexingStatus;
        this.policyKey=policyKey;sourceAuthority=authority;supersedesDocumentId=supersedes;
        approvedBy=actor;approvedAt=approval;
        String identity=title+"\n"+category+"\n"+source+"\n"+content;
        if(!"".equals(policyKey)||!"admin_reviewed".equals(authority)||supersedes!=null)identity+="\nsource-policy:"+policyKey+"\n"+authority+"\n"+supersedes;
        contentHash=VectorValues.hash(identity);
    }
    public boolean validAt(Instant now) {
        return "published".equals(status) && approvedBy>0 && approvedAt!=null && !approvedAt.isAfter(now) && (effectiveAt==null || !now.isBefore(effectiveAt)) && (expiresAt==null || now.isBefore(expiresAt));
    }
}
