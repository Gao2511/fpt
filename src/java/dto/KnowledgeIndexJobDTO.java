package dto;
import java.util.UUID;
public final class KnowledgeIndexJobDTO {
    public final long id,documentId,version;
    public final String hash,model;
    public final int dimensions,attempts;
    public final UUID leaseToken;
    public final String providerKind;
    public KnowledgeIndexJobDTO(long id,long documentId,long version,String hash,String model,int dimensions,int attempts,UUID leaseToken) {
        this(id,documentId,version,hash,model,dimensions,attempts,leaseToken,"MOCK");
    }
    public KnowledgeIndexJobDTO(long id,long documentId,long version,String hash,String model,int dimensions,int attempts,UUID leaseToken,String kind) {
        this.id=id;this.documentId=documentId;this.version=version;this.hash=hash;this.model=model;
        this.dimensions=dimensions;this.attempts=attempts;this.leaseToken=leaseToken;
        providerKind=kind;
    }
}
