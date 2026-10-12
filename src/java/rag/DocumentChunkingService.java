package rag;
import java.util.*;

/** Bounded, deterministic paragraph/sentence-friendly chunks with character overlap. */
public final class DocumentChunkingService {
    public static final int DEFAULT_MAX_CHUNK_SIZE = 1000;
    public static final int DEFAULT_OVERLAP = 100;
    public static final int MIN_CHUNK_SIZE = 200;
    public static final int MAX_CHUNK_SIZE = 1200;
    public static final int MAX_OVERLAP = 400;

    public static final class ChunkPreview {
        public final int ordinal;
        public final String content;
        public final int charCount;
        public final int estimatedTokens;
        public final String contentHash;
        public ChunkPreview(int ordinal, String content, int charCount, int estimatedTokens, String contentHash) {
            this.ordinal = ordinal;
            this.content = content;
            this.charCount = charCount;
            this.estimatedTokens = estimatedTokens;
            this.contentHash = contentHash;
        }
        public int getOrdinal() { return ordinal; }
        public String getContent() { return content; }
        public int getCharCount() { return charCount; }
        public int getEstimatedTokens() { return estimatedTokens; }
        public String getContentHash() { return contentHash; }
    }

    public List<String> chunk(String content) throws RagException {
        return chunk(content, DEFAULT_MAX_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunk(String content, int maxChunkSize, int overlap) throws RagException {
        if(content==null || content.trim().isEmpty() || content.length()>KnowledgeLimits.extracted()) throw new RagException(RagException.Code.INVALID_DOCUMENT);
        if(maxChunkSize<MIN_CHUNK_SIZE || maxChunkSize>MAX_CHUNK_SIZE || overlap<0 || overlap>MAX_OVERLAP || overlap>=maxChunkSize/2)
            throw new RagException(RagException.Code.INVALID_DOCUMENT);
        String text=content.replace("\r\n","\n").replace('\r','\n').trim();
        List<String> chunks=new ArrayList<>(); int start=0;
        int minBoundaryOffset=Math.max(100,maxChunkSize/2);
        while(start<text.length()) {
            int end=Math.min(start+maxChunkSize,text.length());
            if(end<text.length()) {
                int boundary=text.lastIndexOf('\n',end);
                if(boundary<start+minBoundaryOffset)boundary=text.lastIndexOf(' ',end);
                if(boundary>=start+minBoundaryOffset)end=boundary;
                if(Character.isHighSurrogate(text.charAt(end-1)))end--;
            }
            String part=text.substring(start,end).trim(); if(!part.isEmpty()) chunks.add(part);
            if(end==text.length())break;
            int next=Math.max(start+1,end-overlap);
            while(next<end && !Character.isWhitespace(text.charAt(next)))next++;
            if(next<text.length() && Character.isLowSurrogate(text.charAt(next)))next++;
            start=next;
        }
        if(chunks.size()>5000)throw new RagException(RagException.Code.INVALID_DOCUMENT);
        return Collections.unmodifiableList(chunks);
    }

    public List<ChunkPreview> preview(String title, String content, int maxChunkSize, int overlap) throws RagException {
        String safeTitle = title == null ? "" : title.trim();
        List<String> parts = chunk(content, maxChunkSize, overlap);
        List<ChunkPreview> out = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            String text = parts.get(i);
            int chars = text.length();
            int estTokens = Math.max(1, (chars + 3) / 4);
            String hash = VectorValues.hash(safeTitle + "\n" + text);
            out.add(new ChunkPreview(i, text, chars, estTokens, hash));
        }
        return Collections.unmodifiableList(out);
    }
}
