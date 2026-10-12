package rag;
import ai.consultation.ProductCatalog;
import dto.KnowledgeChunkDTO;
import java.time.Instant;
import java.util.Arrays;

/** Diagnose whole chunks; never strip instructions and assume remaining content is safe. */
public final class KnowledgeContentPolicy {
    public enum Reason {ACCEPTED,MISSING_SOURCE,MISSING_APPROVAL,INVALID_METADATA,EXPIRED,NOT_EFFECTIVE,SUPERSEDED,LOW_CONFIDENCE,INVALID_BUSINESS_CLAIM,UNSAFE_CONTENT,DUPLICATE}
    private KnowledgeContentPolicy() {}
    public static boolean allowed(KnowledgeChunkDTO chunk){return reason(chunk,Instant.now())==Reason.ACCEPTED;}
    public static Reason reason(KnowledgeChunkDTO c,Instant now) {
        if(c==null)return Reason.INVALID_METADATA;
        if(c.source==null || c.source.trim().isEmpty())return Reason.MISSING_SOURCE;
        KnowledgeChunkDTO.SourceMetadata m=c.metadata;
        if(m==null || !"published".equals(m.status) || m.approvedBy<=0 || m.approvedAt==null)return Reason.MISSING_APPROVAL;
        if(m.approvedAt.isAfter(now) || !Arrays.asList("faq","policy","guide").contains(m.category) ||
            !Arrays.asList("admin_reviewed","official").contains(m.authority) || m.policyKey==null ||
            !(m.policyKey.isEmpty() || m.policyKey.matches("[a-z0-9][a-z0-9._-]{0,99}")) ||
            c.id<=0 || c.documentId<=0 || c.documentVersion<=0 || c.documentHash==null || !c.documentHash.matches("[0-9a-f]{64}") ||
            c.content==null || c.content.trim().isEmpty() || c.content.length()>1200 || c.title==null || c.title.trim().isEmpty() || c.title.length()>200 || c.source.length()>500 ||
            (c.expiresAt!=null && c.effectiveAt!=null && !c.expiresAt.isAfter(c.effectiveAt)))return Reason.INVALID_METADATA;
        if(m.superseded)return Reason.SUPERSEDED;
        if(c.expiresAt!=null && !now.isBefore(c.expiresAt))return Reason.EXPIRED;
        if(c.effectiveAt!=null && now.isBefore(c.effectiveAt))return Reason.NOT_EFFECTIVE;
        if(!Double.isFinite(c.score)||c.score<.28)return Reason.LOW_CONFIDENCE;
        return contentReason(c.title,c.content);
    }
    /** Pre-publication linter; does not grant approval or replace metadata/authority checks. */
    public static Reason contentReason(String title,String content) {
        String text=title+"\n"+content;
        if(text.matches("(?s).*[<>{}`\\p{Cc}&&[^\\n\\t]].*"))return Reason.UNSAFE_CONTENT;
        String q=ProductCatalog.normalize(text);
        if(q.matches("(?s).*\\b(ignore|disregard|system|developer|assistant|instruction|prompt|api.?key|password|secret|token|bo qua chi dan|bo qua huong dan|bo qua quy tac|tiet lo|chi dan he thong|state tracking|tag constraint|suy luan noi bo|dang ky thanh cong|da gui yeu cau|da luu dang ky)\\b.*"))return Reason.UNSAFE_CONTENT;
        // Device names alone are not business claims. Prices, entitlements and specs stay DB-owned.
        String accented=java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFC).toLowerCase(java.util.Locale.ROOT);
        // Currency/quantities and rate-statement grammar, never accent-folded bare gia/cuoc/ngan.
        if(accented.matches("(?s).*\\b[0-9][0-9., ]*\\s*(?:nghìn|ngàn|triệu|đồng|usd|vnd|vnđ|đ|k\\b).*" ) ||
            q.matches("(?s).*\\b[0-9][0-9.,]*\\s*(?:nghin|trieu|dong|ngan(?!\\s+(?:cach|keo|tu)\\b))\\b.*") ||
            q.matches("(?s).*\\b(?:mien phi|uu dai|khuyen mai|giam gia|mbps|gbps|ngoai hang|180 kenh|goi\\s+[0-9][0-9a-z]*)\\b.*") ||
            q.matches("(?s).*\\b(?:bang gia|goi cuoc|muc cuoc|tien cuoc|gia\\s+(?:goi|cuoc|internet|camera|truyen hinh)|cuoc\\s+(?:internet|hang thang|thang)|phi\\s+(?:internet|lap dat|lap mang|hoa mang|dich vu|thue bao))\\b.*") ||
            q.matches("(?s).*(?<!danh )(?<!tham )\\b(?:gia|cuoc|phi)\\s*(?:(?:la|chi|tu|con|bang|:)\\s*)?[0-9]+.*") ||
            accented.matches("(?s).*\\b(?:một|hai|ba|bốn|năm|sáu|bảy|tám|chín|mười|trăm)\\s+(?:trăm\\s+)?(?:nghìn|ngàn|triệu|đồng)\\b.*") ||
            q.matches("(?s).*(?:[0-9][0-9.,]*\\s*(?:mbps|gbps|kenh|camera|access point)|bao gom.*(?:modem|camera|wifi|truyen hinh)|khong giat lag|giam sat an ninh 24/7).*" ) ||
            text.matches("(?s).*(?:[0-9][0-9., ]*\\s*(?:đ|Đ|VNĐ|VND|vnd|k\\b)|(?:đ|VNĐ|VND)\\s*[0-9]|[0-9]\\s*%).*"))return Reason.INVALID_BUSINESS_CLAIM;
        return Reason.ACCEPTED;
    }
}
