package dto;
import rag.DocumentChunkingService;
import java.util.*;
public final class KnowledgeImportSection {
    public long id,sourceDocumentId,sourceVersion;public int ordinal,startLine,endLine;
    public String title,content,classification,reviewState,sourceUri;public Long documentId,faqId;
    public long getId(){return id;} public long getSourceDocumentId(){return sourceDocumentId;} public long getSourceVersion(){return sourceVersion;}
    public int getOrdinal(){return ordinal;} public int getStartLine(){return startLine;} public int getEndLine(){return endLine;}
    public String getTitle(){return title;} public String getContent(){return content;} public String getClassification(){return classification;}
    public String getReviewState(){return reviewState;} public String getSourceUri(){return sourceUri;}
    public Long getDocumentId(){return documentId;} public Long getFaqId(){return faqId;}
    public List<DocumentChunkingService.ChunkPreview> getChunks(){try{return new DocumentChunkingService().preview(title,content,1000,100);}catch(Exception e){return Collections.emptyList();}}
}
