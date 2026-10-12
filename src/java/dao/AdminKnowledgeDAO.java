package dao;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dto.AdminKnowledgeDTO.*;
import dto.KnowledgeChunkDTO;
import dto.KnowledgeDocumentDTO;
import dto.KnowledgeIndexJobDTO;
import rag.*;
import rag.extract.DocumentExtractionService;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Phase 3: Comprehensive DAO & Service coordinator for the Admin Knowledge Base & RAG Management System.
 * Operates 100% offline using PostgreSQL + MockEmbeddingProvider (zero external API calls).
 */
public class AdminKnowledgeDAO {
    private static final List<String> VALID_CATEGORIES = Arrays.asList("faq", "policy", "guide");
    private static final List<String> VALID_AUTHORITIES = Arrays.asList("admin_reviewed", "official");
    private static final List<String> VALID_WORKFLOW_STATES = Arrays.asList(
            "DRAFT", "PENDING_REVIEW", "APPROVED", "PUBLISHED", "REJECTED", "ARCHIVED", "UNPUBLISHED", "PROCESSING_FAILED");

    private final RagDatabase db;
    private final RagConfig ragConfig;
    private final MockEmbeddingProvider mockEmbeddings;

    public AdminKnowledgeDAO() {
        this(new RagDatabase());
    }

    public AdminKnowledgeDAO(RagDatabase db) {
        this.db = db;
        this.ragConfig = new RagConfig(true, true, true, MockEmbeddingProvider.MODEL_NAME, MockEmbeddingProvider.DIMENSIONS);
        this.mockEmbeddings = new MockEmbeddingProvider();
    }

    public MockEmbeddingProvider getMockEmbeddings() {
        return mockEmbeddings;
    }

    public List<dto.KnowledgeImportSection> listImportSections(long sourceId) throws Exception {
        List<dto.KnowledgeImportSection> out=new ArrayList<>();
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_import_sections WHERE source_document_id=? ORDER BY source_version DESC,ordinal")){
            p.setLong(1,sourceId);try(ResultSet r=p.executeQuery()){while(r.next())out.add(readImportSection(r));}
        }return out;
    }
    private static dto.KnowledgeImportSection readImportSection(ResultSet r)throws SQLException{
        dto.KnowledgeImportSection s=new dto.KnowledgeImportSection();s.id=r.getLong("id");s.sourceDocumentId=r.getLong("source_document_id");s.sourceVersion=r.getLong("source_version");
        s.ordinal=r.getInt("ordinal");s.startLine=r.getInt("start_line");s.endLine=r.getInt("end_line");s.title=r.getString("title");s.content=r.getString("content");
        s.classification=r.getString("classification");s.reviewState=r.getString("review_state");s.sourceUri=r.getString("source_uri");s.documentId=(Long)r.getObject("document_id");s.faqId=(Long)r.getObject("faq_id");return s;
    }
    private static boolean importSchema(Connection c)throws SQLException{
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT to_regclass('public.knowledge_import_sections') IS NOT NULL")){r.next();return r.getBoolean(1);}
    }
    public void prepareImport(long sourceId,long version,int actor,String username)throws Exception{
        if(actor<=0)throw new IllegalArgumentException("Yêu cầu Admin hợp lệ.");
        try(Connection c=db.open()) {c.setAutoCommit(false);try{
            String text,uri;
            try(PreparedStatement p=c.prepareStatement("SELECT d.content,d.source,d.workflow_status,d.source_type,d.version FROM public.knowledge_documents d WHERE d.id=? FOR UPDATE")){
                p.setLong(1,sourceId);try(ResultSet r=p.executeQuery()){
                    if(!r.next()||r.getLong("version")!=version||!"UPLOAD".equals(r.getString("source_type"))||!"DRAFT".equals(r.getString("workflow_status")))throw new IllegalArgumentException("Chỉ phân mục bản upload DRAFT đúng phiên bản.");
                    text=r.getString("content");uri=r.getString("source");
                }
            }
            try(PreparedStatement p=c.prepareStatement("SELECT id,original_filename,extraction_warning FROM public.knowledge_files WHERE document_id=? AND version_number=? AND extraction_status='EXTRACTED' ORDER BY uploaded_at DESC LIMIT 1")){
                p.setLong(1,sourceId);p.setLong(2,version);try(ResultSet r=p.executeQuery()){
                    if(!r.next())throw new IllegalStateException("Không có tệp gốc tương ứng revision đã trích xuất; hãy upload bản mới.");
                    if(r.getString("extraction_warning")!=null&&!r.getString("extraction_warning").trim().isEmpty())throw new IllegalStateException("Trích xuất có cảnh báo hoặc bị cắt ngắn; giữ bản gốc và kiểm tra thủ công trước khi phân mục.");
                    uri="upload:"+r.getString("id")+"/"+r.getString("original_filename");
                }
            }
            // Freeze offsets/content against revision, not a mutable edited document pointer.
            try(PreparedStatement p=c.prepareStatement("SELECT content FROM public.knowledge_document_revisions WHERE document_id=? AND version_number=?")){
                p.setLong(1,sourceId);p.setLong(2,version);try(ResultSet r=p.executeQuery()){if(!r.next()||!text.equals(r.getString(1)))throw new IllegalStateException("Revision nguồn không khớp.");}
            }
            for(rag.importer.DocumentSectionService.Section s:new rag.importer.DocumentSectionService().detect(text)){
                try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_import_sections(source_document_id,source_version,ordinal,start_offset,end_offset,start_line,end_line,original_title,title,content,content_hash,source_uri,classification,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(source_document_id,source_version,ordinal) DO NOTHING")){
                    p.setLong(1,sourceId);p.setLong(2,version);p.setInt(3,s.ordinal);p.setInt(4,s.start);p.setInt(5,s.end);p.setInt(6,s.startLine);p.setInt(7,s.endLine);p.setString(8,s.title);p.setString(9,s.title);p.setString(10,s.content);p.setString(11,s.hash);
                    p.setString(12,uri+"#v"+version+"/section-"+s.ordinal+"/lines-"+s.startLine+"-"+s.endLine);p.setString(13,s.kind.name());p.setInt(14,actor);p.executeUpdate();
                }
            }
            recordAuditTx(c,actor,username,"PREVIEW_IMPORT","KNOWLEDGE_DOCUMENT",Long.toString(sourceId),Collections.emptyMap(),Collections.singletonMap("version",version),"Phân mục; chưa tạo entry hoặc xuất bản");c.commit();
        }catch(Exception e){c.rollback();throw e;}}
    }
    public void reviewImportSection(long id,String title,String kind,boolean ignore,int actor,String username)throws Exception{
        if(actor<=0||title==null||title.trim().isEmpty()||title.length()>200)throw new IllegalArgumentException("Tiêu đề/Admin không hợp lệ.");
        rag.importer.DocumentSectionService.Kind.valueOf(kind);
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_import_sections SET title=?,classification=?,review_state=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP WHERE id=? AND review_state<>'CREATED'")){
                p.setString(1,title.trim());p.setString(2,kind);p.setString(3,ignore?"IGNORED":"REVIEWED");p.setInt(4,actor);p.setLong(5,id);if(p.executeUpdate()!=1)throw new IllegalStateException("Mục đã tạo entry hoặc không tồn tại.");
            }
            recordAuditTx(c,actor,username,"REVIEW_IMPORT_SECTION","KNOWLEDGE_IMPORT_SECTION",Long.toString(id),Collections.emptyMap(),Collections.singletonMap("classification",kind),ignore?"Bỏ qua mục":"Duyệt phân loại; không thay đổi chỉ dẫn AI");c.commit();
        }catch(Exception e){c.rollback();throw e;}}
    }
    /** Atomic, idempotent conversion; no approval/publication/settings or network operation. */
    public dto.KnowledgeImportSection createImportDraft(long id,int actor,String username)throws Exception{
        if(actor<=0)throw new IllegalArgumentException("Yêu cầu Admin hợp lệ.");
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            dto.KnowledgeImportSection s;
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM public.knowledge_import_sections WHERE id=? FOR UPDATE")){
                p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không tìm thấy mục.");s=readImportSection(r);}
            }
            if("CREATED".equals(s.reviewState)){c.commit();return s;}
            if(!"REVIEWED".equals(s.reviewState)||!Arrays.asList("KNOWLEDGE_BASE","FAQ").contains(s.classification))throw new IllegalStateException("Chỉ mục Knowledge/FAQ đã review được tạo nháp. Quy tắc và cấu hình giữ hàng chờ duyệt thủ công.");
            rag.importer.DocumentSectionService.validateEntry(s.title,s.content);
            String key="import.section."+s.id;
            if("FAQ".equals(s.classification)){
                try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_faqs(question,answer,category,workflow_status,source_authority,policy_key,created_by,updated_by) VALUES(?,?,'faq','DRAFT','admin_reviewed',?,?,?) RETURNING *")){
                    p.setString(1,s.title);p.setString(2,s.content);p.setString(3,key);p.setInt(4,actor);p.setInt(5,actor);try(ResultSet r=p.executeQuery()){r.next();FaqItem faq=readFaq(r);s.faqId=faq.id;insertFaqRevision(c,faq,"Nhập từ "+s.sourceUri,actor);}
                }
            }else{
                KnowledgeDocumentDTO d=new KnowledgeDocumentDTO(0,1,s.title,"guide",s.content,s.sourceUri,"draft",null,null,"pending",key,"admin_reviewed",null);
                try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_documents(title,category,content,source,status,workflow_status,source_type,content_hash,embedding_model,vector_dimensions,policy_key,source_authority,created_by,updated_by) VALUES(?,'guide',?,?,'draft','DRAFT','MANUAL',?,?,768,?,'admin_reviewed',?,?) RETURNING *")){
                    p.setString(1,s.title);p.setString(2,s.content);p.setString(3,s.sourceUri);p.setString(4,d.contentHash);p.setString(5,ragConfig.model);p.setString(6,key);p.setInt(7,actor);p.setInt(8,actor);try(ResultSet r=p.executeQuery()){r.next();ManagedDocument target=readManagedDocument(r);s.documentId=target.id;insertRevisionSnapshot(c,target,"Nhập từ "+s.sourceUri,null,actor);}
                }
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_import_sections SET document_id=?,faq_id=?,review_state='CREATED' WHERE id=?")){
                p.setObject(1,s.documentId,Types.BIGINT);p.setObject(2,s.faqId,Types.BIGINT);p.setLong(3,s.id);p.executeUpdate();
            }
            recordAuditTx(c,actor,username,"CREATE_IMPORT_DRAFT","KNOWLEDGE_IMPORT_SECTION",Long.toString(id),Collections.emptyMap(),Collections.singletonMap("source",s.sourceUri),"Tạo nháp riêng, bắt buộc duyệt xuất bản");c.commit();s.reviewState="CREATED";return s;
        }catch(Exception e){c.rollback();throw e;}}
    }

    // =========================================================================
    // 1. DASHBOARD METRICS (PHASE 3.2)
    // =========================================================================

    public DashboardMetrics getDashboardMetrics() throws Exception {
        int totalDocs = 0, draft = 0, pending = 0, approved = 0, published = 0, rejected = 0, archived = 0, unpublished = 0, failed = 0;
        int indexed = 0, needingReindex = 0, indexingFailures = 0;
        int totalFaqs = 0, publishedFaqs = 0, totalChunks = 0;

        try (Connection c = db.open()) {
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT workflow_status, indexing_status, version, indexed_version, COUNT(*) AS cnt "
                    + "FROM public.knowledge_documents GROUP BY workflow_status, indexing_status, version, indexed_version");
                 ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String ws = r.getString("workflow_status");
                    String is = r.getString("indexing_status");
                    long ver = r.getLong("version");
                    Long idxVer = (Long) r.getObject("indexed_version");
                    int cnt = r.getInt("cnt");
                    totalDocs += cnt;
                    if ("DRAFT".equals(ws)) draft += cnt;
                    else if ("PENDING_REVIEW".equals(ws)) pending += cnt;
                    else if ("APPROVED".equals(ws)) approved += cnt;
                    else if ("PUBLISHED".equals(ws)) {
                        published += cnt;
                        if ("ready".equals(is) && idxVer != null && idxVer.longValue() == ver) {
                            indexed += cnt;
                        } else {
                            needingReindex += cnt;
                        }
                    } else if ("REJECTED".equals(ws)) rejected += cnt;
                    else if ("ARCHIVED".equals(ws)) archived += cnt;
                    else if ("UNPUBLISHED".equals(ws)) unpublished += cnt;
                    else if ("PROCESSING_FAILED".equals(ws)) failed += cnt;

                    if ("failed".equals(is)) {
                        indexingFailures += cnt;
                    }
                }
            }

            try (PreparedStatement p = c.prepareStatement(
                    "SELECT workflow_status, COUNT(*) AS cnt FROM public.knowledge_faqs GROUP BY workflow_status");
                 ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String ws = r.getString("workflow_status");
                    int cnt = r.getInt("cnt");
                    totalFaqs += cnt;
                    if ("PUBLISHED".equals(ws)) publishedFaqs += cnt;
                }
            }

            try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM public.knowledge_chunks");
                 ResultSet r = p.executeQuery()) {
                if (r.next()) totalChunks = r.getInt(1);
            }
        }

        List<AuditLogEntry> recent = listAuditLogs(null, null, 1, 10);
        return new DashboardMetrics(totalDocs, draft, pending, approved, published, rejected,
                archived, unpublished, failed, indexed, needingReindex, indexingFailures,
                totalFaqs, publishedFaqs, totalChunks, recent);
    }

    // =========================================================================
    // 2. DOCUMENT MANAGEMENT, VERSIONING & LIFECYCLE (PHASE 3.2, 3.3, 3.4)
    // =========================================================================

    public ManagedDocument findDocument(long id) throws Exception {
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement("SELECT * FROM public.knowledge_documents WHERE id = ?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? readManagedDocument(r) : null;
            }
        }
    }

    public List<ManagedDocument> searchDocuments(String keyword, String category, String workflowStatus,
            String indexingFilter, int page, int pageSize) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT * FROM public.knowledge_documents WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(tags) LIKE ? OR LOWER(source) LIKE ? OR LOWER(department) LIKE ?)");
            String pat = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            params.add(pat); params.add(pat); params.add(pat); params.add(pat);
        }
        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            params.add(category.trim().toLowerCase(Locale.ROOT));
        }
        if (workflowStatus != null && !workflowStatus.trim().isEmpty() && !"all".equalsIgnoreCase(workflowStatus)) {
            sql.append(" AND workflow_status = ?");
            params.add(workflowStatus.trim().toUpperCase(Locale.ROOT));
        }
        if ("needs_reindex".equalsIgnoreCase(indexingFilter)) {
            sql.append(" AND workflow_status = 'PUBLISHED' AND (indexing_status <> 'ready' OR indexed_version IS NULL OR indexed_version <> version)");
        } else if (indexingFilter != null && !indexingFilter.trim().isEmpty() && !"all".equalsIgnoreCase(indexingFilter)) {
            sql.append(" AND indexing_status = ?");
            params.add(indexingFilter.trim().toLowerCase(Locale.ROOT));
        }

        int safeSize = Math.max(1, Math.min(pageSize, 100));
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * safeSize;
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?");
        params.add(safeSize);
        params.add(offset);

        List<ManagedDocument> result = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    result.add(readManagedDocument(r));
                }
            }
        }
        return result;
    }

    public int countDocuments(String keyword, String category, String workflowStatus, String indexingFilter) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM public.knowledge_documents WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(tags) LIKE ? OR LOWER(source) LIKE ? OR LOWER(department) LIKE ?)");
            String pat = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            params.add(pat); params.add(pat); params.add(pat); params.add(pat);
        }
        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            params.add(category.trim().toLowerCase(Locale.ROOT));
        }
        if (workflowStatus != null && !workflowStatus.trim().isEmpty() && !"all".equalsIgnoreCase(workflowStatus)) {
            sql.append(" AND workflow_status = ?");
            params.add(workflowStatus.trim().toUpperCase(Locale.ROOT));
        }
        if ("needs_reindex".equalsIgnoreCase(indexingFilter)) {
            sql.append(" AND workflow_status = 'PUBLISHED' AND (indexing_status <> 'ready' OR indexed_version IS NULL OR indexed_version <> version)");
        } else if (indexingFilter != null && !indexingFilter.trim().isEmpty() && !"all".equalsIgnoreCase(indexingFilter)) {
            sql.append(" AND indexing_status = ?");
            params.add(indexingFilter.trim().toLowerCase(Locale.ROOT));
        }

        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    /**
     * Creates a new Knowledge Base document in DRAFT status (never auto-published or auto-indexed).
     */
    public ManagedDocument createDocument(String title, String category, String content, String source,
            String sourceType, String tags, String department, String policyKey, String sourceAuthority,
            Long supersedesDocumentId, Instant effectiveAt, Instant expiresAt,
            int chunkSize, int chunkOverlap, String changeNote,
            int actorId, String actorUsername) throws Exception {
        return createDocumentWithWorkflow(title, category, content, source, sourceType, tags, department,
                policyKey, sourceAuthority, supersedesDocumentId, effectiveAt, expiresAt,
                chunkSize, chunkOverlap, "DRAFT", changeNote, actorId, actorUsername);
    }

    private ManagedDocument createDocumentWithWorkflow(String title, String category, String content, String source,
            String sourceType, String tags, String department, String policyKey, String sourceAuthority,
            Long supersedesDocumentId, Instant effectiveAt, Instant expiresAt,
            int chunkSize, int chunkOverlap, String initialWorkflowStatus, String changeNote,
            int actorId, String actorUsername) throws Exception {
        validateDocumentInputs(title, category, content, source, sourceType, policyKey, sourceAuthority,
                effectiveAt, expiresAt, chunkSize, chunkOverlap, actorId);
        if (!"DRAFT".equals(initialWorkflowStatus) && !"PROCESSING_FAILED".equals(initialWorkflowStatus)) {
            throw new IllegalArgumentException("Tài liệu mới phải khởi tạo ở trạng thái DRAFT hoặc PROCESSING_FAILED.");
        }

        String cleanTitle = title.trim();
        String cleanSource = source.trim();
        String cleanPolicyKey = policyKey == null ? "" : policyKey.trim();
        String cleanTags = sanitizeMeta(tags, 500);
        String cleanDept = sanitizeMeta(department, 100);
        String cleanType = (sourceType == null || sourceType.trim().isEmpty()) ? "MANUAL" : sourceType.trim().toUpperCase(Locale.ROOT);
        String lowStatus = mapLowLevelStatus(initialWorkflowStatus);

        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO(
                0L, 1L, cleanTitle, category, content, cleanSource, lowStatus,
                effectiveAt, expiresAt, "pending", cleanPolicyKey, sourceAuthority, supersedesDocumentId);

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                String sql = "INSERT INTO public.knowledge_documents ("
                        + "title, category, content, source, status, workflow_status, source_type, tags, department, "
                        + "version, content_hash, effective_at, expires_at, embedding_model, vector_dimensions, "
                        + "indexing_status, policy_key, source_authority, supersedes_document_id, "
                        + "created_by, updated_by, chunk_size, chunk_overlap"
                        + ") VALUES (?,?,?,?,?,?,?,?,?,1,?,?,?,?,?,'pending',?,?,?,?,?,?,?) RETURNING *";
                ManagedDocument created;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setString(1, cleanTitle);
                    p.setString(2, category);
                    p.setString(3, content);
                    p.setString(4, cleanSource);
                    p.setString(5, lowStatus);
                    p.setString(6, initialWorkflowStatus);
                    p.setString(7, cleanType);
                    p.setString(8, cleanTags);
                    p.setString(9, cleanDept);
                    p.setString(10, dto.contentHash);
                    p.setTimestamp(11, RagDatabase.timestamp(effectiveAt));
                    p.setTimestamp(12, RagDatabase.timestamp(expiresAt));
                    p.setString(13, ragConfig.model);
                    p.setInt(14, ragConfig.dimensions);
                    p.setString(15, cleanPolicyKey);
                    p.setString(16, sourceAuthority);
                    if (supersedesDocumentId == null) p.setNull(17, Types.BIGINT);
                    else p.setLong(17, supersedesDocumentId.longValue());
                    p.setInt(18, actorId);
                    p.setInt(19, actorId);
                    p.setInt(20, chunkSize);
                    p.setInt(21, chunkOverlap);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Insert knowledge_documents failed");
                        created = readManagedDocument(r);
                    }
                }

                insertRevisionSnapshot(c, created, changeNote == null || changeNote.trim().isEmpty()
                        ? "Khởi tạo tài liệu (v1)" : changeNote.trim(), null, actorId);
                recordAuditTx(c, actorId, actorUsername, "CREATE_DOCUMENT", "KNOWLEDGE_DOCUMENT",
                        Long.toString(created.id), Collections.<String, Object>emptyMap(), toAuditState(created),
                        "Tạo mới tài liệu #" + created.id + " (" + created.workflowStatus + ")");
                c.commit();
                return created;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Handles secure file upload + extraction (Phase 3.3).
     * Creates a DRAFT document (or PROCESSING_FAILED if extraction yielded no text) and records knowledge_files.
     * Never auto-publishes and never auto-indexes.
     */
    public ManagedDocument uploadAndCreateDraft(DocumentExtractionService.ExtractionResult extraction,
            String titleOverride, String category, String tags, String department,
            String policyKey, String sourceAuthority, Instant effectiveAt, Instant expiresAt,
            int chunkSize, int chunkOverlap, int actorId, String actorUsername) throws Exception {
        if (extraction == null) {
            throw new IllegalArgumentException("Kết quả trích xuất tệp rỗng.");
        }
        String docTitle = (titleOverride != null && !titleOverride.trim().isEmpty())
                ? titleOverride.trim()
                : stripExtensionForTitle(extraction.originalFilename);
        if (docTitle.length() > 200) {
            docTitle = docTitle.substring(0, 200).trim();
        }
        String initialWorkflow = "EXTRACTED".equals(extraction.status) ? "DRAFT" : "PROCESSING_FAILED";
        String bodyContent = ("EXTRACTED".equals(extraction.status) && !extraction.extractedText.isEmpty())
                ? extraction.extractedText
                : "[PROCESSING_FAILED] Không thể trích xuất văn bản từ tệp " + extraction.originalFilename
                  + ". Lý do: " + extraction.warningMessage;
        String sourceUri = "upload:" + extraction.fileId.toString() + "/" + extraction.originalFilename;

        ManagedDocument doc = createDocumentWithWorkflow(
                docTitle,
                category == null || category.trim().isEmpty() ? "guide" : category.trim().toLowerCase(Locale.ROOT),
                bodyContent,
                sourceUri,
                "UPLOAD",
                tags,
                department,
                policyKey,
                sourceAuthority == null || sourceAuthority.trim().isEmpty() ? "admin_reviewed" : sourceAuthority.trim(),
                null,
                effectiveAt,
                expiresAt,
                chunkSize,
                chunkOverlap,
                initialWorkflow,
                "Tải lên tệp " + extraction.originalFilename + " (" + extraction.status + ")",
                actorId,
                actorUsername);

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                boolean hasByteaCol = columnExists(c, "knowledge_files", "file_content");
                String storageBackend = (hasByteaCol && extraction.rawBytes != null) ? "HYBRID" : "DISK";
                if (hasByteaCol) {
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO public.knowledge_files ("
                            + "id, document_id, version_number, original_filename, stored_filename, storage_rel_path, "
                            + "detected_mime_type, file_extension, file_size_bytes, sha256_hash, extraction_status, "
                            + "extracted_char_count, extraction_warning, uploaded_by, file_content, storage_backend"
                            + ") VALUES (CAST(? AS uuid),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                        p.setString(1, extraction.fileId.toString());
                        p.setLong(2, doc.id);
                        p.setLong(3, doc.version);
                        p.setString(4, extraction.originalFilename);
                        p.setString(5, extraction.storedFilename);
                        p.setString(6, extraction.storageRelPath);
                        p.setString(7, extraction.detectedMimeType);
                        p.setString(8, extraction.extension);
                        p.setLong(9, extraction.sizeBytes);
                        p.setString(10, extraction.sha256Hash);
                        p.setString(11, extraction.status);
                        p.setInt(12, extraction.charCount);
                        p.setString(13, extraction.warningMessage);
                        p.setInt(14, actorId);
                        if (extraction.rawBytes != null) {
                            p.setBytes(15, extraction.rawBytes);
                        } else {
                            p.setNull(15, Types.BINARY);
                        }
                        p.setString(16, storageBackend);
                        p.executeUpdate();
                    }
                } else {
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO public.knowledge_files ("
                            + "id, document_id, version_number, original_filename, stored_filename, storage_rel_path, "
                            + "detected_mime_type, file_extension, file_size_bytes, sha256_hash, extraction_status, "
                            + "extracted_char_count, extraction_warning, uploaded_by"
                            + ") VALUES (CAST(? AS uuid),?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                        p.setString(1, extraction.fileId.toString());
                        p.setLong(2, doc.id);
                        p.setLong(3, doc.version);
                        p.setString(4, extraction.originalFilename);
                        p.setString(5, extraction.storedFilename);
                        p.setString(6, extraction.storageRelPath);
                        p.setString(7, extraction.detectedMimeType);
                        p.setString(8, extraction.extension);
                        p.setLong(9, extraction.sizeBytes);
                        p.setString(10, extraction.sha256Hash);
                        p.setString(11, extraction.status);
                        p.setInt(12, extraction.charCount);
                        p.setString(13, extraction.warningMessage);
                        p.setInt(14, actorId);
                        p.executeUpdate();
                    }
                }

                Map<String, Object> fileAudit = new LinkedHashMap<>();
                fileAudit.put("fileId", extraction.fileId.toString());
                fileAudit.put("documentId", doc.id);
                fileAudit.put("originalFilename", extraction.originalFilename);
                fileAudit.put("detectedMimeType", extraction.detectedMimeType);
                fileAudit.put("sizeBytes", extraction.sizeBytes);
                fileAudit.put("extractionStatus", extraction.status);
                fileAudit.put("extractedCharCount", extraction.charCount);
                fileAudit.put("storageBackend", storageBackend);
                recordAuditTx(c, actorId, actorUsername, "UPLOAD_FILE", "KNOWLEDGE_FILE",
                        extraction.fileId.toString(), Collections.<String, Object>emptyMap(), fileAudit,
                        "Tải lên tệp " + extraction.originalFilename + " -> Tài liệu #" + doc.id);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
        return doc;
    }

    /**
     * Updates a document's content or metadata by creating a new immutable revision (version + 1).
     * If preservePublishedState is true on a PUBLISHED document, it remains PUBLISHED with indexing_status='pending'
     * (so isNeedsReindex() becomes true until reindexed). Otherwise editing transitions the revision to DRAFT.
     */
    public ManagedDocument updateDocument(long id, String title, String category, String content, String source,
            String tags, String department, String policyKey, String sourceAuthority,
            Long supersedesDocumentId, Instant effectiveAt, Instant expiresAt,
            int chunkSize, int chunkOverlap, boolean preservePublishedState, String changeNote,
            int actorId, String actorUsername) throws Exception {
        ManagedDocument existing = findDocument(id);
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy tài liệu #" + id);
        }
        validateDocumentInputs(title, category, content, source, existing.sourceType, policyKey, sourceAuthority,
                effectiveAt, expiresAt, chunkSize, chunkOverlap, actorId);

        String cleanTitle = title.trim();
        String cleanSource = source.trim();
        String cleanPolicyKey = policyKey == null ? "" : policyKey.trim();
        String cleanTags = sanitizeMeta(tags, 500);
        String cleanDept = sanitizeMeta(department, 100);

        String nextWorkflow = (preservePublishedState && "PUBLISHED".equals(existing.workflowStatus))
                ? "PUBLISHED"
                : "DRAFT";
        String nextLowStatus = mapLowLevelStatus(nextWorkflow);

        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO(
                id, existing.version + 1, cleanTitle, category, content, cleanSource, nextLowStatus,
                effectiveAt, expiresAt, "pending", cleanPolicyKey, sourceAuthority, supersedesDocumentId);

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                if (importSchema(c)) {
                    try (PreparedStatement guard = c.prepareStatement("SELECT 1 FROM public.knowledge_import_sections i JOIN public.knowledge_faqs f ON f.id=i.faq_id WHERE f.linked_document_id=?")) {
                        guard.setLong(1, id);
                        try (ResultSet imported = guard.executeQuery()) {
                            if (imported.next()) throw new IllegalArgumentException("Hãy chỉnh sửa FAQ nhập từ tài liệu tại tab FAQ để duyệt lại độc lập.");
                        }
                    }
                    try (PreparedStatement guard = c.prepareStatement("SELECT source_uri FROM public.knowledge_import_sections WHERE document_id=?")) {
                        guard.setLong(1, id);
                        try (ResultSet imported = guard.executeQuery()) {
                            if (imported.next()) {
                                if (preservePublishedState && "PUBLISHED".equals(existing.workflowStatus))
                                    throw new IllegalArgumentException("Entry nhập từ tài liệu phải trở về nháp và duyệt lại khi chỉnh sửa.");
                                if (!cleanSource.equals(imported.getString(1)))
                                    throw new IllegalArgumentException("Không được thay đổi provenance của entry nhập từ tài liệu.");
                            }
                        }
                    }
                }
                String sql = "UPDATE public.knowledge_documents SET "
                        + "title=?, category=?, content=?, source=?, status=?, workflow_status=?, "
                        + "tags=?, department=?, version=version+1, content_hash=?, "
                        + "effective_at=?, expires_at=?, indexing_status='pending', indexed_version=NULL, "
                        + "policy_key=?, source_authority=?, supersedes_document_id=?, "
                        + "updated_by=?, chunk_size=?, chunk_overlap=?, updated_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND version=? RETURNING *";
                ManagedDocument updated;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setString(1, cleanTitle);
                    p.setString(2, category);
                    p.setString(3, content);
                    p.setString(4, cleanSource);
                    p.setString(5, nextLowStatus);
                    p.setString(6, nextWorkflow);
                    p.setString(7, cleanTags);
                    p.setString(8, cleanDept);
                    p.setString(9, dto.contentHash);
                    p.setTimestamp(10, RagDatabase.timestamp(effectiveAt));
                    p.setTimestamp(11, RagDatabase.timestamp(expiresAt));
                    p.setString(12, cleanPolicyKey);
                    p.setString(13, sourceAuthority);
                    if (supersedesDocumentId == null) p.setNull(14, Types.BIGINT);
                    else p.setLong(14, supersedesDocumentId.longValue());
                    p.setInt(15, actorId);
                    p.setInt(16, chunkSize);
                    p.setInt(17, chunkOverlap);
                    p.setLong(18, id);
                    p.setLong(19, existing.version);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new RagException(RagException.Code.STALE_VERSION);
                        updated = readManagedDocument(r);
                    }
                }

                if (!"PUBLISHED".equals(nextWorkflow)) {
                    try (PreparedStatement del = c.prepareStatement("DELETE FROM public.knowledge_chunks WHERE document_id = ?")) {
                        del.setLong(1, id);
                        del.executeUpdate();
                    }
                } else {
                    KnowledgeDocumentDTO savedDto = toDocumentDTO(updated);
                    KnowledgeIndexJobDAO.enqueue(c, savedDto, ragConfig);
                }

                insertRevisionSnapshot(c, updated,
                        changeNote == null || changeNote.trim().isEmpty()
                                ? "Cập nhật nội dung/metadata (v" + updated.version + ")"
                                : changeNote.trim(),
                        null, actorId);
                recordAuditTx(c, actorId, actorUsername, "UPDATE_DOCUMENT", "KNOWLEDGE_DOCUMENT",
                        Long.toString(id), toAuditState(existing), toAuditState(updated),
                        "Cập nhật tài liệu #" + id + " lên phiên bản v" + updated.version);
                c.commit();
                return updated;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public ManagedDocument submitForReview(long id, int actorId, String actorUsername, String comments) throws Exception {
        ManagedDocument doc = findDocument(id);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + id);
        if (!Arrays.asList("DRAFT", "REJECTED", "UNPUBLISHED").contains(doc.workflowStatus)) {
            throw new IllegalStateException("Chỉ tài liệu ở trạng thái DRAFT, REJECTED hoặc UNPUBLISHED mới có thể gửi duyệt (hiện tại: " + doc.workflowStatus + ").");
        }
        return transitionWorkflow(doc, "PENDING_REVIEW", "SUBMIT_REVIEW", comments, null, actorId, actorUsername);
    }

    public ManagedDocument approveDocument(long id, int actorId, String actorUsername, String comments) throws Exception {
        ManagedDocument doc = findDocument(id);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + id);
        if (!"PENDING_REVIEW".equals(doc.workflowStatus)) {
            throw new IllegalStateException("Chỉ tài liệu đang chờ duyệt (PENDING_REVIEW) mới có thể được phê duyệt (hiện tại: " + doc.workflowStatus + ").");
        }
        return transitionWorkflow(doc, "APPROVED", "APPROVE", comments, null, actorId, actorUsername);
    }

    public ManagedDocument rejectDocument(long id, int actorId, String actorUsername, String rejectionReason) throws Exception {
        ManagedDocument doc = findDocument(id);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + id);
        if (!Arrays.asList("PENDING_REVIEW", "APPROVED").contains(doc.workflowStatus)) {
            throw new IllegalStateException("Chỉ tài liệu ở trạng thái PENDING_REVIEW hoặc APPROVED mới có thể bị từ chối (hiện tại: " + doc.workflowStatus + ").");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Bắt buộc nhập lý do từ chối khi Reject tài liệu.");
        }
        return transitionWorkflow(doc, "REJECTED", "REJECT", rejectionReason.trim(), rejectionReason.trim(), actorId, actorUsername);
    }

    public ManagedDocument publishDocument(long id, int actorId, String actorUsername, String comments) throws Exception {
        ManagedDocument doc = findDocument(id);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + id);
        if (!"APPROVED".equals(doc.workflowStatus)) {
            throw new IllegalStateException("Không thể xuất bản tài liệu chưa được phê duyệt (yêu cầu trạng thái APPROVED, hiện tại: " + doc.workflowStatus + ").");
        }
        try(Connection c=db.open()){if(importSchema(c)){
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM public.knowledge_import_sections i JOIN public.knowledge_faqs f ON f.id=i.faq_id WHERE f.linked_document_id=?")){
                p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Hãy phê duyệt/xuất bản FAQ nhập từ tài liệu tại tab FAQ.");}
            }
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM public.knowledge_import_sections WHERE document_id=?")){
                p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(r.next())rag.importer.DocumentSectionService.validateEntry(doc.title,doc.content);}
            }
        }}
        return transitionWorkflow(doc, "PUBLISHED", "PUBLISH", comments, null, actorId, actorUsername);
    }

    public ManagedDocument unpublishDocument(long id, int actorId, String actorUsername, String comments) throws Exception {
        ManagedDocument doc = findDocument(id);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + id);
        if (!"PUBLISHED".equals(doc.workflowStatus)) {
            throw new IllegalStateException("Chỉ tài liệu đang PUBLISHED mới có thể gỡ xuất bản (hiện tại: " + doc.workflowStatus + ").");
        }
        return transitionWorkflow(doc, "UNPUBLISHED", "UNPUBLISH", comments, null, actorId, actorUsername);
    }

    public ManagedDocument archiveDocument(long id, int actorId, String actorUsername, String comments) throws Exception {
        ManagedDocument doc = findDocument(id);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + id);
        if ("ARCHIVED".equals(doc.workflowStatus)) {
            throw new IllegalStateException("Tài liệu đã ở trạng thái ARCHIVED.");
        }
        return transitionWorkflow(doc, "ARCHIVED", "ARCHIVE", comments, null, actorId, actorUsername);
    }

    private ManagedDocument transitionWorkflow(ManagedDocument doc, String targetWorkflow, String reviewAction,
            String comments, String rejectionReason, int actorId, String actorUsername) throws Exception {
        String lowStatus = mapLowLevelStatus(targetWorkflow);
        boolean isPublish = "PUBLISHED".equals(targetWorkflow);
        boolean isReviewDecision = "APPROVE".equals(reviewAction) || "REJECT".equals(reviewAction) || "PUBLISH".equals(reviewAction);

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                StringBuilder sql = new StringBuilder(
                        "UPDATE public.knowledge_documents SET status=?, workflow_status=?, updated_by=?, updated_at=CURRENT_TIMESTAMP");
                if (isPublish) {
                    sql.append(", approved_by=?, approved_at=CURRENT_TIMESTAMP, indexing_status='pending', indexed_version=NULL, rejection_reason=NULL");
                } else if ("UNPUBLISH".equals(reviewAction) || "ARCHIVE".equals(reviewAction) || "REJECT".equals(reviewAction)) {
                    sql.append(", indexing_status='pending', indexed_version=NULL");
                }
                if (isReviewDecision) {
                    sql.append(", reviewed_by=?, reviewed_at=CURRENT_TIMESTAMP");
                }
                if ("REJECT".equals(reviewAction)) {
                    sql.append(", rejection_reason=?");
                } else if ("APPROVE".equals(reviewAction) || "SUBMIT_REVIEW".equals(reviewAction)) {
                    sql.append(", rejection_reason=NULL");
                }
                sql.append(" WHERE id=? AND version=? RETURNING *");

                ManagedDocument updated;
                try (PreparedStatement p = c.prepareStatement(sql.toString())) {
                    int idx = 1;
                    p.setString(idx++, lowStatus);
                    p.setString(idx++, targetWorkflow);
                    p.setInt(idx++, actorId);
                    if (isPublish) {
                        p.setInt(idx++, actorId);
                    }
                    if (isReviewDecision) {
                        p.setInt(idx++, actorId);
                    }
                    if ("REJECT".equals(reviewAction)) {
                        p.setString(idx++, rejectionReason);
                    }
                    p.setLong(idx++, doc.id);
                    p.setLong(idx, doc.version);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new RagException(RagException.Code.STALE_VERSION);
                        updated = readManagedDocument(r);
                    }
                }

                // Update latest revision snapshot's workflow_status to stay synchronized
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE public.knowledge_document_revisions SET workflow_status=? WHERE document_id=? AND version_number=?")) {
                    p.setString(1, targetWorkflow);
                    p.setLong(2, updated.id);
                    p.setLong(3, updated.version);
                    p.executeUpdate();
                }

                if (isPublish) {
                    KnowledgeDocumentDTO savedDto = toDocumentDTO(updated);
                    KnowledgeIndexJobDAO.enqueue(c, savedDto, ragConfig);
                } else if ("UNPUBLISH".equals(reviewAction) || "ARCHIVE".equals(reviewAction) || "REJECT".equals(reviewAction)) {
                    try (PreparedStatement del = c.prepareStatement("DELETE FROM public.knowledge_chunks WHERE document_id=?")) {
                        del.setLong(1, updated.id);
                        del.executeUpdate();
                    }
                }

                insertReviewRecord(c, updated.id, updated.version, reviewAction, doc.workflowStatus,
                        targetWorkflow, actorId, comments, rejectionReason);
                recordAuditTx(c, actorId, actorUsername, reviewAction + "_DOCUMENT", "KNOWLEDGE_DOCUMENT",
                        Long.toString(updated.id), toAuditState(doc), toAuditState(updated),
                        reviewAction + " tài liệu #" + updated.id + " (" + doc.workflowStatus + " -> " + targetWorkflow + ")");
                c.commit();
                return updated;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Non-destructive rollback: creates a NEW revision (version + 1) in DRAFT status from a historical revision.
     */
    public ManagedDocument rollbackDocument(long documentId, long targetVersionNumber, int actorId, String actorUsername) throws Exception {
        ManagedDocument current = findDocument(documentId);
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy tài liệu #" + documentId);
        }
        DocumentRevision targetRev = findRevision(documentId, targetVersionNumber);
        if (targetRev == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên bản v" + targetVersionNumber + " của tài liệu #" + documentId);
        }

        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO(
                documentId, current.version + 1, targetRev.title, targetRev.category, targetRev.content,
                targetRev.source, "draft", targetRev.effectiveAt, targetRev.expiresAt, "pending",
                targetRev.policyKey, targetRev.sourceAuthority, targetRev.supersedesDocumentId);

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                String sql = "UPDATE public.knowledge_documents SET "
                        + "title=?, category=?, content=?, source=?, status='draft', workflow_status='DRAFT', "
                        + "tags=?, department=?, version=version+1, content_hash=?, "
                        + "effective_at=?, expires_at=?, indexing_status='pending', indexed_version=NULL, "
                        + "policy_key=?, source_authority=?, supersedes_document_id=?, "
                        + "updated_by=?, rejection_reason=NULL, updated_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND version=? RETURNING *";
                ManagedDocument rolledBack;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setString(1, targetRev.title);
                    p.setString(2, targetRev.category);
                    p.setString(3, targetRev.content);
                    p.setString(4, targetRev.source);
                    p.setString(5, targetRev.tags);
                    p.setString(6, targetRev.department);
                    p.setString(7, dto.contentHash);
                    p.setTimestamp(8, RagDatabase.timestamp(targetRev.effectiveAt));
                    p.setTimestamp(9, RagDatabase.timestamp(targetRev.expiresAt));
                    p.setString(10, targetRev.policyKey);
                    p.setString(11, targetRev.sourceAuthority);
                    if (targetRev.supersedesDocumentId == null) p.setNull(12, Types.BIGINT);
                    else p.setLong(12, targetRev.supersedesDocumentId.longValue());
                    p.setInt(13, actorId);
                    p.setLong(14, documentId);
                    p.setLong(15, current.version);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new RagException(RagException.Code.STALE_VERSION);
                        rolledBack = readManagedDocument(r);
                    }
                }

                // Remove any active chunks because rolled back new revision starts in DRAFT requiring review/publish
                try (PreparedStatement del = c.prepareStatement("DELETE FROM public.knowledge_chunks WHERE document_id=?")) {
                    del.setLong(1, documentId);
                    del.executeUpdate();
                }

                String note = "Khôi phục (rollback) từ phiên bản v" + targetVersionNumber + " -> tạo phiên bản mới v" + rolledBack.version;
                insertRevisionSnapshot(c, rolledBack, note, Long.valueOf(targetVersionNumber), actorId);
                insertReviewRecord(c, documentId, rolledBack.version, "ROLLBACK", current.workflowStatus,
                        "DRAFT", actorId, note, null);
                recordAuditTx(c, actorId, actorUsername, "ROLLBACK_DOCUMENT", "KNOWLEDGE_DOCUMENT",
                        Long.toString(documentId), toAuditState(current), toAuditState(rolledBack), note);
                c.commit();
                return rolledBack;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<DocumentRevision> listRevisions(long documentId) throws Exception {
        List<DocumentRevision> list = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(
                     "SELECT * FROM public.knowledge_document_revisions WHERE document_id=? ORDER BY version_number DESC")) {
            p.setLong(1, documentId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) list.add(readRevision(r));
            }
        }
        return list;
    }

    public DocumentRevision findRevision(long documentId, long versionNumber) throws Exception {
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(
                     "SELECT * FROM public.knowledge_document_revisions WHERE document_id=? AND version_number=?")) {
            p.setLong(1, documentId);
            p.setLong(2, versionNumber);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? readRevision(r) : null;
            }
        }
    }

    public RevisionDiff compareRevisions(long documentId, long fromVersion, long toVersion) throws Exception {
        DocumentRevision fromRev = findRevision(documentId, fromVersion);
        DocumentRevision toRev = findRevision(documentId, toVersion);
        if (fromRev == null || toRev == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên bản cần so sánh.");
        }

        List<String> metaChanges = new ArrayList<>();
        if (!Objects.equals(fromRev.title, toRev.title)) {
            metaChanges.add("Tiêu đề: '" + fromRev.title + "' -> '" + toRev.title + "'");
        }
        if (!Objects.equals(fromRev.category, toRev.category)) {
            metaChanges.add("Danh mục: " + fromRev.category + " -> " + toRev.category);
        }
        if (!Objects.equals(fromRev.tags, toRev.tags)) {
            metaChanges.add("Tags: '" + fromRev.tags + "' -> '" + toRev.tags + "'");
        }
        if (!Objects.equals(fromRev.department, toRev.department)) {
            metaChanges.add("Phòng ban: '" + fromRev.department + "' -> '" + toRev.department + "'");
        }
        if (!Objects.equals(fromRev.sourceAuthority, toRev.sourceAuthority)) {
            metaChanges.add("Độ tin cậy: " + fromRev.sourceAuthority + " -> " + toRev.sourceAuthority);
        }
        if (!Objects.equals(fromRev.policyKey, toRev.policyKey)) {
            metaChanges.add("Policy Key: '" + fromRev.policyKey + "' -> '" + toRev.policyKey + "'");
        }

        String[] oldLines = fromRev.content.replace("\r\n", "\n").split("\n");
        String[] newLines = toRev.content.replace("\r\n", "\n").split("\n");
        List<String> diffLines = new ArrayList<>();
        int added = 0, removed = 0;

        Set<String> oldSet = new LinkedHashSet<>(Arrays.asList(oldLines));
        Set<String> newSet = new LinkedHashSet<>(Arrays.asList(newLines));

        for (String line : oldLines) {
            if (!newSet.contains(line)) {
                diffLines.add("- " + line);
                removed++;
            } else {
                diffLines.add("  " + line);
            }
        }
        for (String line : newLines) {
            if (!oldSet.contains(line)) {
                diffLines.add("+ " + line);
                added++;
            }
        }

        boolean contentChanged = !Objects.equals(fromRev.contentHash, toRev.contentHash);
        return new RevisionDiff(documentId, fromVersion, toVersion, contentChanged, metaChanges, diffLines, added, removed);
    }

    public List<DocumentReview> listReviews(long documentId) throws Exception {
        List<DocumentReview> list = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(
                     "SELECT * FROM public.knowledge_document_reviews WHERE document_id=? ORDER BY created_at DESC, id DESC")) {
            p.setLong(1, documentId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new DocumentReview(
                            r.getLong("id"),
                            r.getLong("document_id"),
                            r.getLong("version_number"),
                            r.getString("action"),
                            r.getString("previous_status"),
                            r.getString("new_status"),
                            r.getInt("reviewer_id"),
                            r.getString("comments"),
                            r.getString("rejection_reason"),
                            RagDatabase.instant(r, "created_at")));
                }
            }
        }
        return list;
    }

    public List<UploadedFileRecord> listUploadedFiles(Long documentId) throws Exception {
        List<UploadedFileRecord> list = new ArrayList<>();
        String cols = "id, document_id, version_number, original_filename, stored_filename, storage_rel_path, "
                + "detected_mime_type, file_extension, file_size_bytes, sha256_hash, extraction_status, "
                + "extracted_char_count, extraction_warning, uploaded_by, uploaded_at";
        String sql = documentId == null
                ? "SELECT " + cols + " FROM public.knowledge_files ORDER BY uploaded_at DESC LIMIT 50"
                : "SELECT " + cols + " FROM public.knowledge_files WHERE document_id=? ORDER BY uploaded_at DESC";
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(sql)) {
            if (documentId != null) p.setLong(1, documentId.longValue());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new UploadedFileRecord(
                            r.getString("id"),
                            (Long) r.getObject("document_id"),
                            r.getLong("version_number"),
                            r.getString("original_filename"),
                            r.getString("stored_filename"),
                            r.getString("storage_rel_path"),
                            r.getString("detected_mime_type"),
                            r.getString("file_extension"),
                            r.getLong("file_size_bytes"),
                            r.getString("sha256_hash"),
                            r.getString("extraction_status"),
                            r.getInt("extracted_char_count"),
                            r.getString("extraction_warning"),
                            r.getInt("uploaded_by"),
                            RagDatabase.instant(r, "uploaded_at")));
                }
            }
        }
        return list;
    }

    public byte[] loadUploadedFileBytes(long documentId, String fileId) throws Exception {
        if (documentId <= 0 || fileId == null || fileId.trim().isEmpty()) {
            return null;
        }
        try (Connection c = db.open()) {
            if (!columnExists(c, "knowledge_files", "file_content")) {
                return null;
            }
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT file_content FROM public.knowledge_files WHERE document_id=? AND id=CAST(? AS uuid)")) {
                p.setLong(1, documentId);
                p.setString(2, fileId.trim());
                try (ResultSet r = p.executeQuery()) {
                    return r.next() ? r.getBytes(1) : null;
                }
            }
        }
    }

    // =========================================================================
    // 3. FAQ MANAGEMENT & RAG SOURCE INTEGRATION (PHASE 3.5)
    // =========================================================================

    public FaqItem findFaq(long id) throws Exception {
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement("SELECT * FROM public.knowledge_faqs WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? readFaq(r) : null;
            }
        }
    }

    public List<FaqItem> listFaqs(String workflowStatus, String category) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT * FROM public.knowledge_faqs WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (workflowStatus != null && !workflowStatus.trim().isEmpty() && !"all".equalsIgnoreCase(workflowStatus)) {
            sql.append(" AND workflow_status = ?");
            params.add(workflowStatus.trim().toUpperCase(Locale.ROOT));
        }
        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            params.add(category.trim().toLowerCase(Locale.ROOT));
        }
        sql.append(" ORDER BY priority ASC, id DESC LIMIT 100");

        List<FaqItem> list = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) list.add(readFaq(r));
            }
        }
        return list;
    }

    public FaqItem createFaq(String question, String answer, String category, String tags, int priority,
            String sourceAuthority, String policyKey, Instant effectiveAt, Instant expiresAt,
            int actorId, String actorUsername) throws Exception {
        validateFaqInputs(question, answer, category, priority, sourceAuthority, policyKey, effectiveAt, expiresAt, actorId);
        String cleanQ = question.trim();
        String cleanA = answer.trim();
        String cleanCat = category == null || category.trim().isEmpty() ? "faq" : category.trim().toLowerCase(Locale.ROOT);
        String cleanTags = sanitizeMeta(tags, 500);
        String cleanAuth = sourceAuthority == null || sourceAuthority.trim().isEmpty() ? "admin_reviewed" : sourceAuthority.trim();
        String cleanKey = policyKey == null ? "" : policyKey.trim();

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                FaqItem created;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO public.knowledge_faqs ("
                        + "question, answer, category, tags, priority, workflow_status, version_number, "
                        + "effective_at, expires_at, source_authority, policy_key, created_by, updated_by"
                        + ") VALUES (?,?,?,?,?,'DRAFT',1,?,?,?,?,?,?) RETURNING *")) {
                    p.setString(1, cleanQ);
                    p.setString(2, cleanA);
                    p.setString(3, cleanCat);
                    p.setString(4, cleanTags);
                    p.setInt(5, priority);
                    p.setTimestamp(6, RagDatabase.timestamp(effectiveAt));
                    p.setTimestamp(7, RagDatabase.timestamp(expiresAt));
                    p.setString(8, cleanAuth);
                    p.setString(9, cleanKey);
                    p.setInt(10, actorId);
                    p.setInt(11, actorId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Insert FAQ failed");
                        created = readFaq(r);
                    }
                }
                insertFaqRevision(c, created, "Tạo mới FAQ (v1)", actorId);
                recordAuditTx(c, actorId, actorUsername, "CREATE_FAQ", "KNOWLEDGE_FAQ",
                        Long.toString(created.id), Collections.<String, Object>emptyMap(), toFaqAuditState(created),
                        "Tạo mới FAQ #" + created.id);
                c.commit();
                return created;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public FaqItem updateFaq(long id, String question, String answer, String category, String tags, int priority,
            String sourceAuthority, String policyKey, Instant effectiveAt, Instant expiresAt,
            String changeNote, int actorId, String actorUsername) throws Exception {
        FaqItem existing = findFaq(id);
        if (existing == null) throw new IllegalArgumentException("Không tìm thấy FAQ #" + id);
        validateFaqInputs(question, answer, category, priority, sourceAuthority, policyKey, effectiveAt, expiresAt, actorId);

        String cleanQ = question.trim();
        String cleanA = answer.trim();
        String cleanCat = category == null || category.trim().isEmpty() ? "faq" : category.trim().toLowerCase(Locale.ROOT);
        String cleanTags = sanitizeMeta(tags, 500);
        String cleanAuth = sourceAuthority == null || sourceAuthority.trim().isEmpty() ? "admin_reviewed" : sourceAuthority.trim();
        String cleanKey = policyKey == null ? "" : policyKey.trim();

        // Editing a published FAQ keeps it PUBLISHED or marks its linked RAG document as needing reindex (version+1, indexing_status='pending')
        String nextStatus = "PUBLISHED".equals(existing.workflowStatus) ? "PUBLISHED" : "DRAFT";

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                if (importSchema(c)) {
                    try (PreparedStatement guard = c.prepareStatement("SELECT 1 FROM public.knowledge_import_sections WHERE faq_id=?")) {
                        guard.setLong(1, id);
                        try (ResultSet imported = guard.executeQuery()) {
                            if (imported.next()) nextStatus = "DRAFT";
                        }
                    }
                }
                FaqItem updated;
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE public.knowledge_faqs SET question=?, answer=?, category=?, tags=?, priority=?, "
                        + "workflow_status=?, version_number=version_number+1, effective_at=?, expires_at=?, "
                        + "source_authority=?, policy_key=?, updated_by=?, updated_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND version_number=? RETURNING *")) {
                    p.setString(1, cleanQ);
                    p.setString(2, cleanA);
                    p.setString(3, cleanCat);
                    p.setString(4, cleanTags);
                    p.setInt(5, priority);
                    p.setString(6, nextStatus);
                    p.setTimestamp(7, RagDatabase.timestamp(effectiveAt));
                    p.setTimestamp(8, RagDatabase.timestamp(expiresAt));
                    p.setString(9, cleanAuth);
                    p.setString(10, cleanKey);
                    p.setInt(11, actorId);
                    p.setLong(12, id);
                    p.setLong(13, existing.versionNumber);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new RagException(RagException.Code.STALE_VERSION);
                        updated = readFaq(r);
                    }
                }

                syncFaqToKnowledgeDocumentTx(c, updated, actorId);
                insertFaqRevision(c, updated,
                        changeNote == null || changeNote.trim().isEmpty()
                                ? "Cập nhật FAQ (v" + updated.versionNumber + ")"
                                : changeNote.trim(),
                        actorId);
                recordAuditTx(c, actorId, actorUsername, "UPDATE_FAQ", "KNOWLEDGE_FAQ",
                        Long.toString(id), toFaqAuditState(existing), toFaqAuditState(updated),
                        "Cập nhật FAQ #" + id + " lên v" + updated.versionNumber);
                c.commit();
                return findFaq(id);
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public FaqItem transitionFaq(long id, String action, String rejectionReason, int actorId, String actorUsername) throws Exception {
        FaqItem faq = findFaq(id);
        if (faq == null) throw new IllegalArgumentException("Không tìm thấy FAQ #" + id);

        String targetStatus;
        if ("SUBMIT_REVIEW".equals(action)) {
            if (!Arrays.asList("DRAFT", "REJECTED", "UNPUBLISHED").contains(faq.workflowStatus)) {
                throw new IllegalStateException("FAQ không ở trạng thái cho phép gửi duyệt.");
            }
            targetStatus = "PENDING_REVIEW";
        } else if ("APPROVE".equals(action)) {
            if (!"PENDING_REVIEW".equals(faq.workflowStatus)) {
                throw new IllegalStateException("Chỉ FAQ ở trạng thái PENDING_REVIEW mới có thể được duyệt.");
            }
            targetStatus = "APPROVED";
        } else if ("REJECT".equals(action)) {
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối FAQ.");
            }
            targetStatus = "REJECTED";
        } else if ("PUBLISH".equals(action)) {
            if (!"APPROVED".equals(faq.workflowStatus)) {
                throw new IllegalStateException("Chỉ FAQ đã được APPROVED mới có thể xuất bản.");
            }
            targetStatus = "PUBLISHED";
        } else if ("UNPUBLISH".equals(action)) {
            if (!"PUBLISHED".equals(faq.workflowStatus)) {
                throw new IllegalStateException("Chỉ FAQ đang PUBLISHED mới có thể gỡ xuất bản.");
            }
            targetStatus = "UNPUBLISHED";
        } else if ("ARCHIVE".equals(action)) {
            targetStatus = "ARCHIVED";
        } else {
            throw new IllegalArgumentException("Thao tác FAQ không hợp lệ: " + action);
        }

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                FaqItem updated;
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE public.knowledge_faqs SET workflow_status=?, updated_by=?, reviewed_by=?, "
                        + "reviewed_at=CURRENT_TIMESTAMP, rejection_reason=?, updated_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? RETURNING *")) {
                    p.setString(1, targetStatus);
                    p.setInt(2, actorId);
                    p.setInt(3, actorId);
                    p.setString(4, "REJECT".equals(action) ? rejectionReason.trim() : null);
                    p.setLong(5, id);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("FAQ transition failed");
                        updated = readFaq(r);
                    }
                }

                syncFaqToKnowledgeDocumentTx(c, updated, actorId);
                recordAuditTx(c, actorId, actorUsername, action + "_FAQ", "KNOWLEDGE_FAQ",
                        Long.toString(id), toFaqAuditState(faq), toFaqAuditState(updated),
                        action + " FAQ #" + id + " (" + faq.workflowStatus + " -> " + targetStatus + ")");
                c.commit();
                return findFaq(id);
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<FaqRevision> listFaqRevisions(long faqId) throws Exception {
        List<FaqRevision> list = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(
                     "SELECT * FROM public.knowledge_faq_revisions WHERE faq_id=? ORDER BY version_number DESC")) {
            p.setLong(1, faqId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new FaqRevision(
                            r.getLong("id"),
                            r.getLong("faq_id"),
                            r.getLong("version_number"),
                            r.getString("question"),
                            r.getString("answer"),
                            r.getString("category"),
                            r.getString("tags"),
                            r.getInt("priority"),
                            r.getString("workflow_status"),
                            r.getString("change_note"),
                            r.getInt("created_by"),
                            RagDatabase.instant(r, "created_at")));
                }
            }
        }
        return list;
    }

    private void syncFaqToKnowledgeDocumentTx(Connection c, FaqItem faq, int actorId) throws Exception {
        String docTitle = faq.question.length() > 200 ? faq.question.substring(0, 200).trim() : faq.question.trim();
        String docContent = "Câu hỏi thường gặp: " + faq.question.trim() + "\nHướng dẫn trả lời: " + faq.answer.trim();
        String sourceId = "faq:" + faq.id;
        String lowStatus = mapLowLevelStatus(faq.workflowStatus);
        boolean isPublished = "PUBLISHED".equals(faq.workflowStatus);
        if(importSchema(c))try(PreparedStatement p=c.prepareStatement("SELECT source_uri FROM public.knowledge_import_sections WHERE faq_id=?")){
            p.setLong(1,faq.id);try(ResultSet r=p.executeQuery()){if(r.next()){
                sourceId+="/"+r.getString(1);
                if(isPublished)rag.importer.DocumentSectionService.validateEntry(docTitle,docContent);
            }}
        }

        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO(
                faq.linkedDocumentId == null ? 0L : faq.linkedDocumentId.longValue(),
                faq.versionNumber, docTitle, faq.category, docContent, sourceId, lowStatus,
                faq.effectiveAt, faq.expiresAt, "pending", faq.policyKey, faq.sourceAuthority, null);

        if (faq.linkedDocumentId == null) {
            if (!isPublished) {
                return; // Do not create a RAG document until FAQ is published
            }
            long newDocId;
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO public.knowledge_documents ("
                    + "title, category, content, source, status, workflow_status, source_type, tags, department, "
                    + "version, content_hash, effective_at, expires_at, approved_by, approved_at, "
                    + "embedding_model, vector_dimensions, indexing_status, policy_key, source_authority, "
                    + "created_by, updated_by"
                    + ") VALUES (?,?,?,?,?,'PUBLISHED','FAQ',?,'FAQ',?,?,?,?,?,?,?,?,'pending',?,?,?,?) RETURNING id")) {
                p.setString(1, docTitle);
                p.setString(2, faq.category);
                p.setString(3, docContent);
                p.setString(4, sourceId);
                p.setString(5, "published");
                p.setString(6, faq.tags);
                p.setLong(7, faq.versionNumber);
                p.setString(8, dto.contentHash);
                p.setTimestamp(9, RagDatabase.timestamp(faq.effectiveAt));
                p.setTimestamp(10, RagDatabase.timestamp(faq.expiresAt));
                p.setInt(11, actorId);
                p.setTimestamp(12, Timestamp.from(Instant.now()));
                p.setString(13, ragConfig.model);
                p.setInt(14, ragConfig.dimensions);
                p.setString(15, faq.policyKey);
                p.setString(16, faq.sourceAuthority);
                p.setInt(17, actorId);
                p.setInt(18, actorId);
                try (ResultSet r = p.executeQuery()) {
                    r.next();
                    newDocId = r.getLong(1);
                }
            }
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE public.knowledge_faqs SET linked_document_id=? WHERE id=?")) {
                p.setLong(1, newDocId);
                p.setLong(2, faq.id);
                p.executeUpdate();
            }
            ManagedDocument linkedDoc;
            try (PreparedStatement p = c.prepareStatement("SELECT * FROM public.knowledge_documents WHERE id=?")) {
                p.setLong(1, newDocId);
                try (ResultSet r = p.executeQuery()) {
                    r.next();
                    linkedDoc = readManagedDocument(r);
                }
            }
            insertRevisionSnapshot(c, linkedDoc, "Đồng bộ từ FAQ #" + faq.id + " (v" + faq.versionNumber + ")", null, actorId);
            KnowledgeIndexJobDAO.enqueue(c, toDocumentDTO(linkedDoc), ragConfig);
        } else {
            long docId = faq.linkedDocumentId.longValue();
            ManagedDocument updatedDoc;
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE public.knowledge_documents SET title=?, category=?, content=?, source=?, "
                    + "status=?, workflow_status=?, tags=?, version=?, content_hash=?, "
                    + "effective_at=?, expires_at=?, approved_by=?, approved_at=?, "
                    + "indexing_status='pending', indexed_version=NULL, policy_key=?, source_authority=?, "
                    + "updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE id=? RETURNING *")) {
                p.setString(1, docTitle);
                p.setString(2, faq.category);
                p.setString(3, docContent);
                p.setString(4, sourceId);
                p.setString(5, lowStatus);
                p.setString(6, faq.workflowStatus);
                p.setString(7, faq.tags);
                p.setLong(8, faq.versionNumber);
                p.setString(9, dto.contentHash);
                p.setTimestamp(10, RagDatabase.timestamp(faq.effectiveAt));
                p.setTimestamp(11, RagDatabase.timestamp(faq.expiresAt));
                if (isPublished) {
                    p.setInt(12, actorId);
                    p.setTimestamp(13, Timestamp.from(Instant.now()));
                } else {
                    p.setNull(12, Types.INTEGER);
                    p.setNull(13, Types.TIMESTAMP);
                }
                p.setString(14, faq.policyKey);
                p.setString(15, faq.sourceAuthority);
                p.setInt(16, actorId);
                p.setLong(17, docId);
                try (ResultSet r = p.executeQuery()) {
                    r.next();
                    updatedDoc = readManagedDocument(r);
                }
            }
            insertRevisionSnapshot(c, updatedDoc, "Đồng bộ từ FAQ #" + faq.id + " (v" + faq.versionNumber + ")", null, actorId);
            if (isPublished) {
                KnowledgeIndexJobDAO.enqueue(c, toDocumentDTO(updatedDoc), ragConfig);
            } else {
                try (PreparedStatement del = c.prepareStatement("DELETE FROM public.knowledge_chunks WHERE document_id=?")) {
                    del.setLong(1, docId);
                    del.executeUpdate();
                }
            }
        }
    }

    // =========================================================================
    // 4. CHUNKING PREVIEW & OFFLINE INDEXING PIPELINE (PHASE 3.6)
    // =========================================================================

    public List<DocumentChunkingService.ChunkPreview> previewDocumentChunks(long documentId) throws Exception {
        ManagedDocument doc = findDocument(documentId);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + documentId);
        return new DocumentChunkingService().preview(doc.title, doc.content, doc.chunkSize, doc.chunkOverlap);
    }

    /**
     * Enqueues a published document for reindexing and runs the offline indexing pipeline using MockEmbeddingProvider.
     */
    public ManagedDocument reindexDocumentOffline(long documentId, int actorId, String actorName) throws Exception {
        ManagedDocument doc = findDocument(documentId);
        if (doc == null) throw new IllegalArgumentException("Không tìm thấy tài liệu #" + documentId);
        if (!"PUBLISHED".equals(doc.workflowStatus)) {
            throw new IllegalStateException("Chỉ tài liệu ở trạng thái PUBLISHED mới được phép lập chỉ mục RAG (hiện tại: " + doc.workflowStatus + ").");
        }
        RagAdminSettings cfg = getRagSettings();
        if (!cfg.localIndexingEnabled) {
            throw new IllegalStateException("Local Indexing hiện đang tắt trong cấu hình RAG.");
        }

        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                boolean hasProviderCol = columnExists(c, "knowledge_documents", "embedding_provider");
                String updateDocSql = hasProviderCol
                        ? "UPDATE public.knowledge_documents SET embedding_provider='MOCK', indexing_status='pending', indexed_version=NULL, embedding_error=NULL, updated_at=CURRENT_TIMESTAMP WHERE id=?"
                        : "UPDATE public.knowledge_documents SET indexing_status='pending', indexed_version=NULL, updated_at=CURRENT_TIMESTAMP WHERE id=?";
                try (PreparedStatement p = c.prepareStatement(updateDocSql)) {
                    p.setLong(1, documentId);
                    p.executeUpdate();
                }
                // Reset or insert job in knowledge_index_jobs so reindexing is idempotent across repeated runs
                String upsertJobSql = hasProviderCol
                        ? "INSERT INTO public.knowledge_index_jobs (document_id, document_version, document_hash, embedding_model, vector_dimensions, embedding_provider, status, attempts, available_at) "
                          + "VALUES (?,?,?,?,?,'MOCK','queued',0,CURRENT_TIMESTAMP) "
                          + "ON CONFLICT (document_id, document_version, embedding_model, vector_dimensions) "
                          + "DO UPDATE SET embedding_provider='MOCK', status='queued', attempts=0, lease_token=NULL, lease_until=NULL, error_code=NULL, available_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP"
                        : "INSERT INTO public.knowledge_index_jobs (document_id, document_version, document_hash, embedding_model, vector_dimensions, status, attempts, available_at) "
                          + "VALUES (?,?,?,?,?,'queued',0,CURRENT_TIMESTAMP) "
                          + "ON CONFLICT (document_id, document_version, embedding_model, vector_dimensions) "
                          + "DO UPDATE SET status='queued', attempts=0, lease_token=NULL, lease_until=NULL, error_code=NULL, available_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP";
                try (PreparedStatement p = c.prepareStatement(upsertJobSql)) {
                    p.setLong(1, doc.id);
                    p.setLong(2, doc.version);
                    p.setString(3, doc.contentHash);
                    p.setString(4, ragConfig.model);
                    p.setInt(5, ragConfig.dimensions);
                    p.executeUpdate();
                }
                insertReviewRecord(c, doc.id, doc.version, "REINDEX_REQUEST", doc.workflowStatus,
                        doc.workflowStatus, actorId, "Yêu cầu lập chỉ mục offline (MockEmbeddingProvider)", null);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }

        runOfflineIndexingBatch(10, actorId, actorName);
        return findDocument(documentId);
    }

    /**
     * Processes queued indexing jobs using KnowledgeIndexingService + MockEmbeddingProvider (zero network calls).
     */
    public int runOfflineIndexingBatch(int maxJobs, int actorId, String actorName) throws Exception {
        KnowledgeDocumentDAO docDao = new KnowledgeDocumentDAO(db, ragConfig);
        KnowledgeChunkDAO chunkDao = new KnowledgeChunkDAO(db);
        KnowledgeIndexJobDAO jobDao = new KnowledgeIndexJobDAO(db);
        KnowledgeIndexingService indexer = new KnowledgeIndexingService(db, docDao, chunkDao, jobDao, mockEmbeddings);

        int processed = 0;
        int bound = Math.max(1, Math.min(maxJobs, 50));
        while (processed < bound && indexer.runNext()) {
            processed++;
        }

        if (processed > 0 && actorId > 0) {
            try (Connection c = db.open()) {
                c.setAutoCommit(false);
                try {
                    Map<String, Object> state = new LinkedHashMap<>();
                    state.put("processedJobs", processed);
                    state.put("embeddingProvider", "MockEmbeddingProvider (offline)");
                    state.put("dimensions", MockEmbeddingProvider.DIMENSIONS);
                    state.put("geminiApiRequests", 0);
                    recordAuditTx(c, actorId, actorName, "RUN_OFFLINE_INDEXING", "KNOWLEDGE_INDEX_JOB",
                            "batch-" + processed, Collections.<String, Object>emptyMap(), state,
                            "Chạy lập chỉ mục offline hoàn tất " + processed + " job (0 Gemini API calls)");
                    c.commit();
                } catch (Exception e) {
                    c.rollback();
                }
            }
        }
        return processed;
    }

    /** Explicit synchronous one-job execution; upload/publish never invoke this method. */
    public ManagedDocument requestGeminiIndex(long id, int actor, String username) throws Exception {
        if (rag.runtime.GeminiRuntimeConfig.smokePlanConfigured(System.getenv())) {
            return requestGeminiIndex(id, actor, username, rag.runtime.GeminiRuntime.active());
        }
        if (actor <= 0) throw new IllegalArgumentException("Yêu cầu tài khoản Admin hợp lệ.");
        ManagedDocument d = findDocument(id);
        if (d == null || !"PUBLISHED".equals(d.workflowStatus) || d.approvedBy <= 0 || d.approvedAt == null
                || (d.effectiveAt != null && Instant.now().isBefore(d.effectiveAt))
                || (d.expiresAt != null && !Instant.now().isBefore(d.expiresAt))) {
            throw new IllegalArgumentException("Chỉ tài liệu PUBLISHED đã duyệt và đang trong thời hạn hiệu lực mới được phép lập chỉ mục Gemini.");
        }
        for (String chunk : new DocumentChunkingService().chunk(d.content, d.chunkSize, d.chunkOverlap)) {
            if (KnowledgeContentPolicy.contentReason(d.title, chunk) != KnowledgeContentPolicy.Reason.ACCEPTED) {
                throw new IllegalArgumentException("Nội dung tài liệu vi phạm chính sách nguồn RAG (chứa giá cước/khuyến mãi chưa chuẩn hóa hoặc chỉ dẫn không an toàn).");
            }
        }
        String embKey = RagFactory.resolveGeminiEmbeddingKey(null, System.getenv());
        if (embKey.isEmpty()) {
            throw new IllegalArgumentException("Chưa cấu hình khóa Gemini API (vui lòng nhập khóa Gemini trong AI Model Settings hoặc cấu hình GEMINI_API_KEY / RAG_EMBEDDING_API_KEY).");
        }
        RagConfig activeRagConfig = new RagConfig(true, true, true, "gemini-embedding-001", 768);
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE public.knowledge_documents SET embedding_provider='GEMINI', embedding_model='gemini-embedding-001', vector_dimensions=768, indexing_status='pending', indexed_version=NULL, embedding_error=NULL, updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=? AND content_hash=? AND status='published' AND approved_by>0")) {
                    p.setLong(1, id);
                    p.setLong(2, d.version);
                    p.setString(3, d.contentHash);
                    if (p.executeUpdate() != 1) throw new RagException(RagException.Code.STALE_VERSION);
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO public.knowledge_index_jobs(document_id, document_version, document_hash, embedding_model, vector_dimensions, embedding_provider, status) VALUES(?,?,?,'gemini-embedding-001',768,'GEMINI','queued') ON CONFLICT(document_id, document_version, embedding_model, vector_dimensions) DO UPDATE SET embedding_provider='GEMINI', status='queued', attempts=0, lease_token=NULL, lease_until=NULL, error_code=NULL, available_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP")) {
                    p.setLong(1, id);
                    p.setLong(2, d.version);
                    p.setString(3, d.contentHash);
                    p.executeUpdate();
                }
                recordAuditTx(c, actor, username, "REQUEST_GEMINI_INDEX", "KNOWLEDGE_DOCUMENT", Long.toString(id),
                        Collections.<String, Object>emptyMap(),
                        Collections.<String, Object>singletonMap("embeddingModel", "gemini-embedding-001 (768d)"),
                        "Lập chỉ mục vector thật bằng Gemini Embedding (gemini-embedding-001, 768 chiều)");
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
        EmbeddingService geminiEmbeddings = new GeminiEmbeddingService(
                activeRagConfig, embKey, GeminiEmbeddingService.httpTransport(), true, 2);
        new KnowledgeIndexingService(
                db,
                new KnowledgeDocumentDAO(db, activeRagConfig),
                new KnowledgeChunkDAO(db, "GEMINI"),
                new KnowledgeIndexJobDAO(db, "GEMINI", id),
                geminiEmbeddings).runNext();
        return findDocument(id);
    }

    public int runGeminiIndexingBatch(int maxJobs, int actorId, String actorName) throws Exception {
        if (actorId <= 0) throw new IllegalArgumentException("Yêu cầu tài khoản Admin hợp lệ.");
        List<ManagedDocument> stale = searchDocuments(null, null, "PUBLISHED", "needs_reindex", 1, Math.max(1, Math.min(maxJobs, 25)));
        int processed = 0;
        for (ManagedDocument d : stale) {
            if (processed >= maxJobs) break;
            ManagedDocument after = requestGeminiIndex(d.id, actorId, actorName);
            if (after != null && "ready".equalsIgnoreCase(after.indexingStatus)) {
                processed++;
            }
        }
        return processed;
    }

    public ManagedDocument requestGeminiIndex(long id,int actor,String username,rag.runtime.GeminiRuntime runtime)throws Exception{
        if(actor<=0)throw new IllegalArgumentException("Admin required");runtime.config.current();
        ManagedDocument d=findDocument(id);if(d==null||!"PUBLISHED".equals(d.workflowStatus)||d.approvedBy<=0||d.approvedAt==null||(d.effectiveAt!=null&&Instant.now().isBefore(d.effectiveAt))||(d.expiresAt!=null&&!Instant.now().isBefore(d.expiresAt)))throw new IllegalArgumentException("Only current approved published sources may be indexed");
        runtime.config.document(id,d.version,d.contentHash);
        GeminiEmbeddingService requestBuilder=new GeminiEmbeddingService(runtime.config.rag(),"unused-no-transport");
        for(String chunk:new DocumentChunkingService().chunk(d.content)){
            if(KnowledgeContentPolicy.contentReason(d.title,chunk)!=KnowledgeContentPolicy.Reason.ACCEPTED)throw new IllegalArgumentException("Source content policy rejected before Gemini export");
            runtime.config.payload("DOCUMENT",requestBuilder.requestBody(d.title,chunk,"RETRIEVAL_DOCUMENT"));
        }
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            try(PreparedStatement p=c.prepareStatement("SELECT status,lease_until FROM public.knowledge_index_jobs WHERE document_id=? AND document_version=? FOR UPDATE")){
                p.setLong(1,id);p.setLong(2,d.version);try(ResultSet r=p.executeQuery()){while(r.next())if("running".equals(r.getString(1)))throw new IllegalArgumentException("Existing running/unknown indexing job must be reviewed; no automatic reset");}
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE public.knowledge_documents SET embedding_provider='GEMINI',embedding_model='gemini-embedding-001',vector_dimensions=768,indexing_status='pending',indexed_version=NULL,embedding_error=NULL WHERE id=? AND version=? AND content_hash=? AND status='published' AND approved_by>0 AND approved_at<=CURRENT_TIMESTAMP")){
                p.setLong(1,id);p.setLong(2,d.version);p.setString(3,d.contentHash);if(p.executeUpdate()!=1)throw new RagException(RagException.Code.STALE_VERSION);
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.knowledge_index_jobs(document_id,document_version,document_hash,embedding_model,vector_dimensions,embedding_provider,status) VALUES(?,?,?,'gemini-embedding-001',768,'GEMINI','queued') ON CONFLICT(document_id,document_version,embedding_model,vector_dimensions) DO UPDATE SET embedding_provider='GEMINI',status='queued',attempts=0,lease_token=NULL,lease_until=NULL,error_code=NULL,available_at=CURRENT_TIMESTAMP")){
                p.setLong(1,id);p.setLong(2,d.version);p.setString(3,d.contentHash);p.executeUpdate();
            }
            recordAuditTx(c,actor,username,"REQUEST_GEMINI_INDEX","KNOWLEDGE_DOCUMENT",Long.toString(id),Collections.emptyMap(),Collections.singletonMap("planId",runtime.config.planId),"Explicit Gemini index; request budget reserved before API send");c.commit();
        }catch(Exception e){c.rollback();throw e;}}
        runtime.index(id);return findDocument(id);
    }

    /** Explicitly recovers expired GEMINI/MOCK index job leases after a Render container restart without making API calls. */
    public int recoverExpiredIndexLeases(int actor, String username) throws Exception {
        if (actor <= 0) throw new IllegalArgumentException("Admin required");
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                int recovered = KnowledgeIndexJobDAO.recoverExpiredLeases(c);
                recordAuditTx(c, actor, username, "RECOVER_EXPIRED_LEASES", "KNOWLEDGE_INDEX_JOB",
                        "expired-leases", Collections.<String, Object>emptyMap(),
                        Collections.<String, Object>singletonMap("recoveredCount", recovered),
                        "Thu hồi " + recovered + " job quá hạn lease sau khi khởi động lại container (0 API calls)");
                c.commit();
                return recovered;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public PlaygroundResult runGeminiPreview(String query, int actor, String username) throws Exception {
        if (rag.runtime.GeminiRuntimeConfig.smokePlanConfigured(System.getenv())) {
            return runGeminiPreview(query, actor, username, rag.runtime.GeminiRuntime.active());
        }
        if (actor <= 0 || query == null || query.trim().isEmpty() || query.length() > 1000) {
            throw new IllegalArgumentException("Vui lòng nhập câu hỏi kiểm thử hợp lệ (1..1000 ký tự).");
        }
        String embKey = RagFactory.resolveGeminiEmbeddingKey(null, System.getenv());
        if (embKey.isEmpty()) {
            throw new IllegalArgumentException("Chưa cấu hình khóa Gemini API để chạy Gemini Retrieval Preview.");
        }
        RagConfig activeRagConfig = new RagConfig(true, true, true, "gemini-embedding-001", 768);
        EmbeddingService geminiEmb = new GeminiEmbeddingService(
                activeRagConfig, embKey, GeminiEmbeddingService.httpTransport(), true, 2);
        KnowledgeRetrievalService retrieval = new KnowledgeRetrievalService(
                activeRagConfig, new KnowledgeChunkDAO(db, "GEMINI"), geminiEmb, true);
        KnowledgeSourceSelection.Result selection = retrieval.lookup(query.trim());
        List<PlaygroundChunkView> selected = new ArrayList<>();
        for (KnowledgeChunkDTO c : selection.chunks) {
            selected.add(new PlaygroundChunkView(c.id, c.documentId, c.documentVersion, c.title, c.content, c.source,
                    c.score, c.metadata.status, c.metadata.category, c.metadata.authority, c.metadata.policyKey, "ACCEPTED", true));
        }
        RagContextBuilder context = new RagContextBuilder(selection.chunks);
        Map<String, Integer> rejected = new LinkedHashMap<>();
        for (Map.Entry<KnowledgeContentPolicy.Reason, Integer> e : selection.rejected.entrySet()) {
            rejected.put(e.getKey().name(), e.getValue());
        }
        PlaygroundResult result = new PlaygroundResult(query.trim(), selected, selected, selection.issue.name(),
                rejected, context.json().toString(),
                "Gemini Retrieval Preview (768d vector): " + selected.size() + " chunk được chọn.",
                selection.uncertain() || selected.isEmpty());
        result.banner = "GEMINI RETRIEVAL PREVIEW (768d) — sử dụng gemini-embedding-001 trực tiếp";
        try (Connection c = db.open()) {
            recordAuditTx(c, actor, username, "GEMINI_RETRIEVAL_PREVIEW", "RAG_CONFIG", "direct-gemini-preview",
                    Collections.<String, Object>emptyMap(), Collections.<String, Object>singletonMap("selectedSources", selected.size()),
                    "Kiểm thử truy hồi Gemini Embedding 768d trực tiếp");
        }
        return result;
    }

    public PlaygroundResult runGeminiPreview(String query,int actor,String username,rag.runtime.GeminiRuntime runtime)throws Exception{
        if(actor<=0||query==null||query.trim().isEmpty()||query.length()>1000)throw new IllegalArgumentException("Admin and valid query required");
        KnowledgeSourceSelection.Result selection=runtime.retrieval.lookup(query.trim());
        List<PlaygroundChunkView> selected=new ArrayList<>();for(KnowledgeChunkDTO c:selection.chunks)selected.add(new PlaygroundChunkView(c.id,c.documentId,c.documentVersion,c.title,c.content,c.source,c.score,c.metadata.status,c.metadata.category,c.metadata.authority,c.metadata.policyKey,"ACCEPTED",true));
        RagContextBuilder context=new RagContextBuilder(selection.chunks);Map<String,Integer> rejected=new LinkedHashMap<>();for(Map.Entry<KnowledgeContentPolicy.Reason,Integer> e:selection.rejected.entrySet())rejected.put(e.getKey().name(),e.getValue());
        String modeLabel=runtime.config.isOnline()?"ONLINE GEMINI RETRIEVAL":"LOCAL GEMINI RETRIEVAL";
        PlaygroundResult result=new PlaygroundResult(query,selected,selected,selection.issue.name(),rejected,context.json().toString(),"Retrieval preview only; no Chat API request",selection.uncertain()||selected.isEmpty());result.banner=modeLabel+" — consumes one approved query embedding slot; no Chat API";
        try(Connection c=db.open()){recordAuditTx(c,actor,username,"GEMINI_RETRIEVAL_PREVIEW","RAG_CONFIG",runtime.config.planId,Collections.emptyMap(),Collections.singletonMap("selectedSources",selected.size()),"Sanitized preview; request/token counts in rag_api_requests");}return result;
    }

    // =========================================================================
    // 5. ADMIN RAG CONFIGURATION (PHASE 3.7)
    // =========================================================================

    public RagAdminSettings getRagSettings() {
        return getRagSettings(System.getenv());
    }

    public RagAdminSettings getRagSettings(Map<String, String> env) {
        SettingsDAO settingsDao = new SettingsDAO();
        Map<String, String> all = settingsDao.getAllAsMap();

        boolean localIdx = !"false".equalsIgnoreCase(all.getOrDefault("rag.local_indexing_enabled", "true"));
        boolean stagingRag = "true".equalsIgnoreCase(all.getOrDefault("rag.staging_rag_enabled", "false"));
        String defaultProdRag = (env != null && "true".equalsIgnoreCase(env.get("RAG_ENABLED"))) ? "true" : "false";
        boolean prodRag = "true".equalsIgnoreCase(all.getOrDefault("rag.production_rag_enabled", defaultProdRag));
        int topK = parseIntSafe(all.get("rag.top_k"), 3, 1, 12);
        double sim = parseDoubleSafe(all.get("rag.similarity_threshold"), 0.72, 0.28, 0.95);
        int maxTokens = parseIntSafe(all.get("rag.max_context_tokens"), 900, 200, 1200);
        int chunkSize = parseIntSafe(all.get("rag.chunk_size"), 1000, 200, 1200);
        int chunkOverlap = parseIntSafe(all.get("rag.chunk_overlap"), 100, 0, 400);

        String primaryModel = all.getOrDefault("ai_active_model",all.getOrDefault("ai_model_gemini", "gemini-3.8-flash"));
        String primaryThinking = all.getOrDefault("ai_thinking_level",all.getOrDefault("ai_thinking_level_gemini", "medium"));
        String backupModel = all.getOrDefault("ai_fallback_model",all.getOrDefault("ai_backup_model_gemini", "gemini-3.5-flash-lite"));
        boolean backupEnabled = "true".equalsIgnoreCase(all.getOrDefault("ai_fallback_enabled", "false"));

        return new RagAdminSettings(
                localIdx, stagingRag, prodRag, topK, sim, maxTokens, chunkSize, chunkOverlap,
                primaryModel, primaryThinking, backupModel, backupEnabled,
                MockEmbeddingProvider.MODEL_NAME, MockEmbeddingProvider.DIMENSIONS);
    }

    public SystemReadinessStatus getSystemReadinessStatus() {
        return getSystemReadinessStatus(System.getenv());
    }

    public SystemReadinessStatus getSystemReadinessStatus(Map<String, String> env) {
        Map<String, String> safeEnv = env == null ? Collections.<String, String>emptyMap() : env;
        String runtimeMode = safeEnv.getOrDefault("RAG_RUNTIME_MODE", "OFFLINE_MOCK").trim();
        if (runtimeMode.isEmpty()) runtimeMode = "OFFLINE_MOCK";
        String ragEnvironment = utils.BuildInfo.getEnvironment(safeEnv);

        boolean kbDbConnected = false;
        boolean chatVerifiedInJournal = false;
        boolean embeddingVerifiedInJournal = false;
        boolean chatErrorInJournal = false;
        boolean embeddingErrorInJournal = false;
        int waitingQuotaCount = 0;
        int processingCount = 0;
        int failedIndexCount = 0;
        int readyIndexCount = 0;
        int approvedPublishedCount = 0;
        String storageBackend = "DISK";
        ComponentStatus dbStatus;
        ComponentStatus kbStatus;

        try (Connection c = db.open()) {
            List<String> missing = new ArrayList<>();
            boolean hasPgVector = false;
            try (Statement s = c.createStatement()) {
                try (ResultSet r = s.executeQuery("SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='vector')")) {
                    hasPgVector = r.next() && r.getBoolean(1);
                    if (!hasPgVector) missing.add("pgvector");
                }
                boolean has002 = regclassExists(s, "public.knowledge_documents")
                        && regclassExists(s, "public.knowledge_chunks")
                        && regclassExists(s, "public.knowledge_index_jobs");
                if (!has002) missing.add("002-rag-mvp.sql");

                boolean has003 = has002 && columnExists(c, "knowledge_documents", "policy_key");
                if (!has003) missing.add("003-rag-source-policy.sql");

                boolean has004 = regclassExists(s, "public.knowledge_document_revisions")
                        && regclassExists(s, "public.knowledge_document_reviews")
                        && regclassExists(s, "public.knowledge_files")
                        && regclassExists(s, "public.knowledge_faqs")
                        && regclassExists(s, "public.knowledge_faq_revisions")
                        && regclassExists(s, "public.admin_audit_logs");
                if (!has004) missing.add("004-admin-knowledge-base.sql");

                boolean has005 = regclassExists(s, "public.knowledge_import_sections");
                boolean has006 = regclassExists(s, "public.rag_runtime_plans")
                        && regclassExists(s, "public.rag_api_requests")
                        && (has002 && columnExists(c, "knowledge_documents", "embedding_provider"));
                if (("LOCAL_GEMINI".equals(runtimeMode) || "STAGING".equals(runtimeMode) || "PRODUCTION".equals(runtimeMode)) && !has006) {
                    missing.add("006-rag-gemini-runtime.sql");
                }
                boolean has007 = has004 && columnExists(c, "knowledge_files", "file_content")
                        && regclassExists(s, "public.rag_schema_migrations");
                if (has007) {
                    storageBackend = "HYBRID_DB";
                }

                if (has002) {
                    try (ResultSet r = s.executeQuery(
                            "SELECT indexing_status, COUNT(*) FROM public.knowledge_documents GROUP BY indexing_status")) {
                        while (r.next()) {
                            String st = r.getString(1);
                            int cnt = r.getInt(2);
                            if ("waiting_for_quota".equals(st)) waitingQuotaCount += cnt;
                            else if ("processing".equals(st)) processingCount += cnt;
                            else if ("failed".equals(st) || "failed_retryable".equals(st)) failedIndexCount += cnt;
                            else if ("ready".equals(st)) readyIndexCount += cnt;
                        }
                    }
                    try (ResultSet r = s.executeQuery(
                            "SELECT COUNT(*) FROM public.knowledge_documents WHERE status='published' AND approved_by>0")) {
                        if (r.next()) approvedPublishedCount = r.getInt(1);
                    }
                    try (ResultSet r = s.executeQuery(
                            "SELECT status, COUNT(*) FROM public.knowledge_index_jobs WHERE status IN ('running','waiting_for_quota') GROUP BY status")) {
                        while (r.next()) {
                            String st = r.getString(1);
                            int cnt = r.getInt(2);
                            if ("waiting_for_quota".equals(st)) waitingQuotaCount += cnt;
                            else if ("running".equals(st)) processingCount += cnt;
                        }
                    }
                }

                if (has006) {
                    try (ResultSet r = s.executeQuery(
                            "SELECT request_group, state, COUNT(*) FROM public.rag_api_requests GROUP BY request_group, state")) {
                        while (r.next()) {
                            String grp = r.getString(1);
                            String st = r.getString(2);
                            int cnt = r.getInt(3);
                            if ("CHAT".equals(grp)) {
                                if ("COMPLETED".equals(st) && cnt > 0) chatVerifiedInJournal = true;
                                if (("FAILED".equals(st) || "UNCERTAIN".equals(st)) && cnt > 0) chatErrorInJournal = true;
                            } else if ("DOCUMENT".equals(grp) || "QUERY".equals(grp)) {
                                if ("COMPLETED".equals(st) && cnt > 0) embeddingVerifiedInJournal = true;
                                if (("FAILED".equals(st) || "UNCERTAIN".equals(st)) && cnt > 0) embeddingErrorInJournal = true;
                            }
                        }
                    }
                }

                if (missing.isEmpty()) {
                    kbDbConnected = true;
                    List<String> optionalMissing = new ArrayList<>();
                    if (!has005) optionalMissing.add("005");
                    if (!has006) optionalMissing.add("006");
                    if (!has007) optionalMissing.add("007");
                    String detailMsg = optionalMissing.isEmpty()
                            ? "Đã xác minh extension vector và đầy đủ bảng migrations 001–007 (Lưu trữ tệp bền vững: " + storageBackend + ")."
                            : "Đã xác minh pgvector và bảng KB cốt lõi 002–004 (migration mở rộng chưa áp dụng: " + String.join(", ", optionalMissing) + ").";
                    dbStatus = new ComponentStatus("Database", "CONNECTED", "CONNECTED", "ready",
                            "PostgreSQL + pgvector sẵn sàng", detailMsg);
                    String kbCode = approvedPublishedCount > 0 ? "READY" : "NO_APPROVED_DOCS";
                    String kbSummary = approvedPublishedCount > 0
                            ? approvedPublishedCount + " tài liệu đã duyệt & xuất bản"
                            : "Chưa có tài liệu xuất bản (0 approved docs)";
                    kbStatus = new ComponentStatus("Knowledge Base", kbCode, "Database Ready", "ready",
                            kbSummary, detailMsg);
                } else {
                    kbDbConnected = false;
                    String dbCode = !hasPgVector ? "PGVECTOR_MISSING" : "MIGRATION_PENDING";
                    dbStatus = new ComponentStatus("Database", dbCode, dbCode, "pending",
                            "Thiếu: " + String.join(", ", missing),
                            "Cần chạy thủ công các migration còn thiếu trên PostgreSQL/Supabase trước khi vận hành.");
                    kbStatus = new ComponentStatus("Knowledge Base", "DEMO_FALLBACK", "Migration Missing", "pending",
                            "Thiếu: " + String.join(", ", missing),
                            "Cần chạy thủ công các migration còn thiếu trên PostgreSQL/Supabase trước khi vận hành.");
                }
            }
        } catch (Exception dbErr) {
            kbDbConnected = false;
            dbStatus = new ComponentStatus("Database", "DISCONNECTED", "DISCONNECTED", "failed",
                    "Không thể kết nối PostgreSQL",
                    "Kiểm tra cấu hình DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD và SSL mode.");
            kbStatus = new ComponentStatus("Knowledge Base", "DEMO_FALLBACK", "Error", "failed",
                    "Không thể kết nối PostgreSQL",
                    "Kiểm tra cấu hình DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD và SSL mode.");
        }

        ai.AIService.ConfigCache aiCfg;
        RagAdminSettings ragCfg;
        String dbGeminiKey = "";
        try {
            aiCfg = ai.AIService.getConfig();
            ragCfg = getRagSettings(safeEnv);
            dbGeminiKey = ai.AIService.getDecryptedApiKey("gemini");
        } catch (Exception cfgErr) {
            aiCfg = new ai.AIService.ConfigCache();
            ragCfg = new RagAdminSettings(true, false, false, 3, 0.72, 900, 1000, 100,
                    "gemini-3.8-flash", "medium", "gemini-3.5-flash-lite", false,
                    MockEmbeddingProvider.MODEL_NAME, MockEmbeddingProvider.DIMENSIONS);
        }

        String envChatKey = safeEnv.get("RAG_CHAT_TEST_API_KEY");
        String envProdChatKey = safeEnv.get("GEMINI_API_KEY");
        boolean hasChatKey = (dbGeminiKey != null && !dbGeminiKey.trim().isEmpty())
                || (envChatKey != null && !envChatKey.trim().isEmpty())
                || (envProdChatKey != null && !envProdChatKey.trim().isEmpty());
        String chatModel = (aiCfg.activeModel != null && !aiCfg.activeModel.trim().isEmpty())
                ? aiCfg.activeModel.trim() : ragCfg.primaryChatModel;

        ComponentStatus geminiChat;
        if (chatErrorInJournal) {
            geminiChat = new ComponentStatus("Gemini Chat", "QUOTA_EXHAUSTED", "Error", "failed",
                    "Ghi nhận lỗi trong nhật ký thực thi",
                    "Bảng rag_api_requests có trạng thái FAILED/UNCERTAIN cho nhóm CHAT. Kiểm tra journal trước khi thử lại.");
        } else if (!hasChatKey || chatModel == null || chatModel.trim().isEmpty()) {
            geminiChat = new ComponentStatus("Gemini Chat", "MISSING_KEY", "Configuration Missing", "failed",
                    "Thiếu API Key hoặc Model",
                    "Chưa cấu hình khóa Gemini Chat trong Admin Settings hoặc biến GEMINI_API_KEY / RAG_CHAT_TEST_API_KEY.");
        } else if (chatVerifiedInJournal) {
            geminiChat = new ComponentStatus("Gemini Chat", "READY", "Connected", "ready",
                    chatModel + " (Thinking: " + ragCfg.primaryThinkingLevel + ")",
                    "Đã có bằng chứng kết nối thành công trong rag_api_requests (0 live calls khi hiển thị trạng thái).");
        } else {
            geminiChat = new ComponentStatus("Gemini Chat", "MODEL_UNVERIFIED", "Not Verified", "pending",
                    chatModel + " (Thinking: " + ragCfg.primaryThinkingLevel + ")",
                    "Đã cấu hình Model/Key nhưng chưa xác minh kết nối trực tiếp (không tự gọi Gemini API để kiểm tra).");
        }

        String envEmbKey = safeEnv.get("RAG_EMBEDDING_API_KEY");
        if ((envEmbKey == null || envEmbKey.trim().isEmpty()) && safeEnv.get("GEMINI_API_KEY") != null) {
            envEmbKey = safeEnv.get("GEMINI_API_KEY");
        }
        if ((envEmbKey == null || envEmbKey.trim().isEmpty()) && dbGeminiKey != null && !dbGeminiKey.trim().isEmpty()) {
            envEmbKey = dbGeminiKey;
        }
        String embModel = safeEnv.getOrDefault("RAG_EMBEDDING_MODEL", MockEmbeddingProvider.MODEL_NAME).trim();
        String embDimStr = safeEnv.getOrDefault("RAG_EMBEDDING_DIMENSIONS", "768").trim();
        boolean validEmbConfig = "gemini-embedding-001".equals(embModel) && "768".equals(embDimStr);

        ComponentStatus geminiEmbedding;
        if (!validEmbConfig || embeddingErrorInJournal) {
            geminiEmbedding = new ComponentStatus("Gemini Embedding", "QUOTA_EXHAUSTED", "Error", "failed",
                    "Cấu hình hoặc journal lỗi",
                    "Yêu cầu model gemini-embedding-001 (768 chiều) và không có lỗi trong rag_api_requests.");
        } else if (envEmbKey == null || envEmbKey.trim().isEmpty()) {
            geminiEmbedding = new ComponentStatus("Gemini Embedding", "MISSING_KEY", "Configuration Missing", "failed",
                    "Thiếu khóa Gemini API",
                    "Chưa cấu hình khóa Gemini trong Admin Settings hoặc RAG_EMBEDDING_API_KEY; hệ thống đang dùng MockEmbeddingProvider offline.");
        } else if (embeddingVerifiedInJournal && "true".equalsIgnoreCase(safeEnv.get("RAG_EMBEDDING_MODEL_VERIFIED"))) {
            geminiEmbedding = new ComponentStatus("Gemini Embedding", "READY", "Connected", "ready",
                    embModel + " (768d)",
                    "Đã xác minh sinh vector 768 chiều hợp lệ trong rag_api_requests.");
        } else {
            geminiEmbedding = new ComponentStatus("Gemini Embedding", "MODEL_UNVERIFIED", "Not Verified", "pending",
                    embModel + " (768d)",
                    "Đã cấu hình khóa Gemini API và sẵn sàng lập chỉ mục vector 768 chiều.");
        }

        boolean ragEnvEnabled = "true".equalsIgnoreCase(safeEnv.get("RAG_ENABLED"));
        boolean prodReleaseAuthorized = ragEnvEnabled || ragCfg.productionRagEnabled;
        ComponentStatus ragStatus;
        if (prodReleaseAuthorized) {
            ragStatus = new ComponentStatus("RAG", "ACTIVE", "Enabled", "ready",
                    "Đang kích hoạt",
                    "Truy hồi tri thức RAG đang bật cho Chatbot tư vấn khách hàng.");
        } else if (!ragEnvEnabled && !ragCfg.localIndexingEnabled && !ragCfg.stagingRagEnabled) {
            ragStatus = new ComponentStatus("RAG", "READY_DISABLED", "Disabled", "DRAFT",
                    "Đang tắt hoàn toàn",
                    "RAG và Local Indexing đều đang tắt trong cấu hình.");
        } else {
            ragStatus = new ComponentStatus("RAG", "BLOCKED", "Blocked by Release Gate", "pending",
                    "Đang tắt ngoài trang chủ",
                    "Production Customer RAG hiện đang tắt; có thể bật trực tiếp tại tab RAG Settings.");
        }

        boolean geminiIndexingEnabled = (envEmbKey != null && !envEmbKey.trim().isEmpty()) && kbDbConnected;
        if (!geminiIndexingEnabled && rag.runtime.GeminiRuntimeConfig.activeRequested(safeEnv)) {
            try {
                rag.runtime.GeminiRuntimeConfig.loadActive(safeEnv);
                geminiIndexingEnabled = true;
            } catch (Exception ignored) {
                geminiIndexingEnabled = false;
            }
        }

        ComponentStatus indexingStatus;
        if (!kbDbConnected) {
            indexingStatus = new ComponentStatus("Indexing", "FAILED_RETRYABLE", "Failed", "failed",
                    "Chưa sẵn sàng do CSDL/Migration",
                    "Cần kết nối PostgreSQL và hoàn tất migrations 002–007 trước khi lập chỉ mục.");
        } else if (waitingQuotaCount > 0) {
            indexingStatus = new ComponentStatus("Indexing", "WAITING_FOR_QUOTA", "Waiting for Quota", "pending",
                    waitingQuotaCount + " tác vụ chờ hạn ngạch",
                    "Có tài liệu ở trạng thái waiting_for_quota; không tự động retry để bảo vệ Free Tier.");
        } else if (processingCount > 0) {
            indexingStatus = new ComponentStatus("Indexing", "QUEUED", "Processing", "APPROVED",
                    processingCount + " tác vụ đang xử lý",
                    "Đang thực thi phân đoạn và tạo chỉ mục vector.");
        } else if (failedIndexCount > 0) {
            indexingStatus = new ComponentStatus("Indexing", "FAILED_RETRYABLE", "Failed", "failed",
                    failedIndexCount + " tài liệu lỗi lập chỉ mục",
                    "Kiểm tra chi tiết tại tab Chunking & Lập chỉ mục trước khi yêu cầu chạy lại.");
        } else {
            indexingStatus = new ComponentStatus("Indexing", "READY", "Ready", "ready",
                    "Sẵn sàng (" + (geminiIndexingEnabled ? "GEMINI + MOCK" : "OFFLINE_MOCK") + ")",
                    "Hàng đợi lập chỉ mục hoạt động tốt (" + readyIndexCount + " tài liệu đã index READY).");
        }

        ComponentStatus backupStatus;
        if (!aiCfg.fallbackEnabled) {
            backupStatus = new ComponentStatus("Backup Model", "DISABLED", "Disabled", "DRAFT",
                    "Đang tắt (" + ragCfg.backupChatModel + ")",
                    "Chế độ tự động chuyển đổi sang Backup Model đang tắt theo cấu hình an toàn.");
        } else {
            backupStatus = new ComponentStatus("Backup Model", "READY", "Unverified", "pending",
                    ragCfg.backupChatModel + " (Thinking: " + aiCfg.fallbackThinkingLevel + ")",
                    "Đã bật Fallback trong cấu hình nhưng chưa xác minh kết nối trực tiếp của model dự phòng.");
        }

        return new SystemReadinessStatus(
                geminiChat,
                geminiEmbedding,
                dbStatus,
                kbStatus,
                ragStatus,
                indexingStatus,
                backupStatus,
                aiCfg.chatbotEnabled,
                prodReleaseAuthorized,
                geminiIndexingEnabled,
                kbDbConnected,
                prodReleaseAuthorized,
                runtimeMode,
                ragEnvironment,
                utils.BuildInfo.getVersion(),
                utils.BuildInfo.getShortCommitSha(safeEnv),
                utils.BuildInfo.getBuildTimestamp(),
                storageBackend);
    }

    private static boolean regclassExists(Statement s, String qualifiedTable) throws SQLException {
        try (ResultSet r = s.executeQuery("SELECT to_regclass('" + qualifiedTable + "') IS NOT NULL")) {
            return r.next() && r.getBoolean(1);
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=? AND column_name=?")) {
            p.setString(1, table);
            p.setString(2, column);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    /**
     * Saves Admin RAG Configuration including Production Customer RAG toggle.
     */
    public RagAdminSettings saveRagSettings(boolean localIndexingEnabled, boolean stagingRagEnabled,
            boolean requestedProductionRagEnabled, int topK, double similarityThreshold,
            int maxContextTokens, int chunkSize, int chunkOverlap,
            int actorId, String actorUsername) throws Exception {
        if (topK < 1 || topK > 12) {
            throw new IllegalArgumentException("Retrieval topK phải nằm trong khoảng 1..12.");
        }
        if (!Double.isFinite(similarityThreshold) || similarityThreshold < 0.28 || similarityThreshold > 0.95) {
            throw new IllegalArgumentException("Ngưỡng tương đồng (Similarity Threshold) phải nằm trong khoảng 0.28..0.95.");
        }
        if (maxContextTokens < 200 || maxContextTokens > 1200) {
            throw new IllegalArgumentException("Max Context Tokens phải nằm trong khoảng 200..1200.");
        }
        if (chunkSize < 200 || chunkSize > 1200 || chunkOverlap < 0 || chunkOverlap > 400 || chunkOverlap >= chunkSize / 2) {
            throw new IllegalArgumentException("Cấu hình Chunk Size (200..1200) hoặc Overlap (0..400, < chunkSize/2) không hợp lệ.");
        }

        RagAdminSettings before = getRagSettings();
        SettingsDAO settingsDao = new SettingsDAO();
        settingsDao.update("rag.local_indexing_enabled", Boolean.toString(localIndexingEnabled));
        settingsDao.update("rag.staging_rag_enabled", Boolean.toString(stagingRagEnabled));
        settingsDao.update("rag.production_rag_enabled", Boolean.toString(requestedProductionRagEnabled));
        settingsDao.update("rag.top_k", Integer.toString(topK));
        settingsDao.update("rag.similarity_threshold", String.format(Locale.ROOT, "%.2f", similarityThreshold));
        settingsDao.update("rag.max_context_tokens", Integer.toString(maxContextTokens));
        settingsDao.update("rag.chunk_size", Integer.toString(chunkSize));
        settingsDao.update("rag.chunk_overlap", Integer.toString(chunkOverlap));
        ai.AIService.invalidateCache();

        RagAdminSettings after = getRagSettings();
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                recordAuditTx(c, actorId, actorUsername, "UPDATE_RAG_CONFIG", "RAG_CONFIG", "rag_settings",
                        toRagSettingsMap(before), toRagSettingsMap(after),
                        "Cập nhật cấu hình RAG (production_rag_enabled=" + requestedProductionRagEnabled + ")");
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
        return after;
    }

    // =========================================================================
    // 6. AUDIT LOGGING (PHASE 3.8)
    // =========================================================================

    public List<AuditLogEntry> listAuditLogs(String resourceType, String actionFilter, int page, int pageSize) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT * FROM public.admin_audit_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (resourceType != null && !resourceType.trim().isEmpty() && !"all".equalsIgnoreCase(resourceType)) {
            sql.append(" AND resource_type = ?");
            params.add(resourceType.trim().toUpperCase(Locale.ROOT));
        }
        if (actionFilter != null && !actionFilter.trim().isEmpty() && !"all".equalsIgnoreCase(actionFilter)) {
            sql.append(" AND action = ?");
            params.add(actionFilter.trim().toUpperCase(Locale.ROOT));
        }
        int safeSize = Math.max(1, Math.min(pageSize, 100));
        int safePage = Math.max(1, page);
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        params.add(safeSize);
        params.add((safePage - 1) * safeSize);

        List<AuditLogEntry> list = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new AuditLogEntry(
                            r.getLong("id"),
                            r.getString("correlation_id"),
                            r.getInt("actor_id"),
                            r.getString("actor_username"),
                            r.getString("action"),
                            r.getString("resource_type"),
                            r.getString("resource_id"),
                            r.getString("previous_state"),
                            r.getString("new_state"),
                            r.getString("summary"),
                            RagDatabase.instant(r, "created_at")));
                }
            }
        }
        return list;
    }

    public static JsonObject sanitizeForAudit(Map<String, Object> raw) {
        JsonObject clean = new JsonObject();
        if (raw == null) return clean;
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            String key = e.getKey();
            if (key == null) continue;
            // Never log API keys, secrets, tokens, passwords, or raw embedding vectors
            if (key.matches("(?i).*(api_?key|secret|token|password|credential|embedding|vector).*")) {
                continue;
            }
            Object val = e.getValue();
            if (val == null) {
                clean.add(key, null);
            } else if (val instanceof Number) {
                clean.addProperty(key, (Number) val);
            } else if (val instanceof Boolean) {
                clean.addProperty(key, (Boolean) val);
            } else {
                String str = String.valueOf(val);
                if (str.length() > 300) str = str.substring(0, 300) + "...";
                clean.addProperty(key, str);
            }
        }
        return clean;
    }

    private void recordAuditTx(Connection c, int actorId, String actorUsername, String action,
            String resourceType, String resourceId, Map<String, Object> prev, Map<String, Object> next,
            String summary) throws SQLException {
        String corrId = UUID.randomUUID().toString();
        JsonObject prevJson = sanitizeForAudit(prev);
        JsonObject nextJson = sanitizeForAudit(next);
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO public.admin_audit_logs ("
                + "correlation_id, actor_id, actor_username, action, resource_type, resource_id, "
                + "previous_state, new_state, summary"
                + ") VALUES (?,?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),?)")) {
            p.setString(1, corrId);
            p.setInt(2, actorId);
            p.setString(3, actorUsername == null ? ("admin#" + actorId) : actorUsername);
            p.setString(4, action);
            p.setString(5, resourceType);
            p.setString(6, resourceId);
            p.setString(7, prevJson.toString());
            p.setString(8, nextJson.toString());
            p.setString(9, summary == null ? "" : summary);
            p.executeUpdate();
        }
    }

    // =========================================================================
    // 7. OFFLINE RAG TEST PLAYGROUND (PHASE 3.9)
    // =========================================================================

    public PlaygroundResult runOfflinePlayground(String query, int actorId, String actorUsername) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập câu hỏi kiểm thử RAG.");
        }
        String cleanQuery = query.trim();
        if (cleanQuery.length() > 1000) {
            throw new IllegalArgumentException("Câu hỏi kiểm thử tối đa 1000 ký tự.");
        }

        RagAdminSettings cfg = getRagSettings();
        KnowledgeChunkDAO chunkDao = new KnowledgeChunkDAO(db);
        KnowledgeRetrievalService retrieval = new KnowledgeRetrievalService(ragConfig, chunkDao, mockEmbeddings);

        float[] queryVector = mockEmbeddings.query(cleanQuery);
        List<KnowledgeChunkDTO> rawCandidates = chunkDao.search(
                cleanQuery, queryVector, ragConfig.model, ragConfig.dimensions, Math.max(1, Math.min(cfg.topK * 3, 12)));

        KnowledgeSourceSelection.Result selection = KnowledgeSourceSelection.select(rawCandidates, cleanQuery);
        RagContextBuilder contextBuilder = new RagContextBuilder(selection.chunks);

        Set<Long> selectedIds = new HashSet<>();
        List<Long> orderedSelectedIds = new ArrayList<>();
        for (KnowledgeChunkDTO c : contextBuilder.chunks) {
            if (orderedSelectedIds.size() < cfg.topK) {
                selectedIds.add(c.id);
                orderedSelectedIds.add(c.id);
            }
        }

        Instant now = Instant.now();
        List<PlaygroundChunkView> candidateViews = new ArrayList<>();
        List<PlaygroundChunkView> selectedViews = new ArrayList<>();

        for (KnowledgeChunkDTO c : rawCandidates) {
            KnowledgeContentPolicy.Reason decision = KnowledgeContentPolicy.reason(c, now);
            boolean inContext = selectedIds.contains(c.id);
            PlaygroundChunkView view = new PlaygroundChunkView(
                    c.id,
                    c.documentId,
                    c.documentVersion,
                    c.title,
                    c.content,
                    c.source,
                    c.score,
                    c.metadata != null ? c.metadata.status : "unknown",
                    c.metadata != null ? c.metadata.category : "unknown",
                    c.metadata != null ? c.metadata.authority : "unknown",
                    c.metadata != null ? c.metadata.policyKey : "",
                    decision.name(),
                    inContext);
            candidateViews.add(view);
            if (inContext) {
                selectedViews.add(view);
            }
        }

        Map<String, Integer> rejectedMap = new LinkedHashMap<>();
        for (Map.Entry<KnowledgeContentPolicy.Reason, Integer> entry : selection.rejected.entrySet()) {
            rejectedMap.put(entry.getKey().name(), entry.getValue());
        }

        String mockAnswer;
        boolean abstained;
        if (selection.uncertain()) {
            abstained = true;
            mockAnswer = "[OFFLINE MOCK - ABSTAIN (" + selection.issue.name() + ")] "
                    + KnowledgeSourceSelection.confirmation();
        } else if (orderedSelectedIds.isEmpty()) {
            abstained = true;
            mockAnswer = "[OFFLINE MOCK - NO ELIGIBLE SOURCE] "
                    + contextBuilder.render(Collections.<Long>emptyList(), retrieval);
        } else {
            abstained = false;
            mockAnswer = "[OFFLINE MOCK - VERIFIED CITATION] "
                    + contextBuilder.render(orderedSelectedIds, retrieval);
        }

        if (actorId > 0) {
            try (Connection c = db.open()) {
                c.setAutoCommit(false);
                try {
                    Map<String, Object> audit = new LinkedHashMap<>();
                    audit.put("query", cleanQuery);
                    audit.put("candidateCount", candidateViews.size());
                    audit.put("selectedCount", selectedViews.size());
                    audit.put("issue", selection.issue.name());
                    audit.put("abstained", abstained);
                    audit.put("geminiApiCalls", 0);
                    recordAuditTx(c, actorId, actorUsername, "PLAYGROUND_QUERY", "RAG_PLAYGROUND",
                            "offline-query", Collections.<String, Object>emptyMap(), audit,
                            "Kiểm thử RAG Playground offline (" + selectedViews.size() + " chunk chọn, 0 Gemini API calls)");
                    c.commit();
                } catch (Exception ignored) {
                    c.rollback();
                }
            }
        }

        return new PlaygroundResult(
                cleanQuery,
                candidateViews,
                selectedViews,
                selection.issue.name(),
                rejectedMap,
                contextBuilder.json().toString(),
                mockAnswer,
                abstained);
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    private static String mapLowLevelStatus(String workflowStatus) {
        if ("PUBLISHED".equals(workflowStatus)) return "published";
        if ("ARCHIVED".equals(workflowStatus)) return "archived";
        return "draft";
    }

    private static void validateDocumentInputs(String title, String category, String content, String source,
            String sourceType, String policyKey, String authority, Instant effectiveAt, Instant expiresAt,
            int chunkSize, int chunkOverlap, int actorId) {
        if (actorId <= 0) throw new IllegalArgumentException("Yêu cầu tài khoản Admin hợp lệ.");
        if (title == null || title.trim().isEmpty() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Tiêu đề tài liệu bắt buộc (1..200 ký tự).");
        }
        if (category == null || !VALID_CATEGORIES.contains(category.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Danh mục tài liệu không hợp lệ (chỉ nhận faq, policy, guide).");
        }
        if (content == null || content.trim().isEmpty() || content.length() > 300000) {
            throw new IllegalArgumentException("Nội dung tài liệu bắt buộc (1..300.000 ký tự).");
        }
        if (source == null || source.trim().isEmpty() || source.trim().length() > 500) {
            throw new IllegalArgumentException("Nguồn tài liệu (source) bắt buộc (1..500 ký tự).");
        }
        if (policyKey != null && !policyKey.trim().isEmpty() && !policyKey.trim().matches("[a-z0-9][a-z0-9._-]{0,99}")) {
            throw new IllegalArgumentException("Policy Key chỉ được chứa chữ thường, số, '.', '_', '-' (tối đa 100 ký tự).");
        }
        if (authority == null || !VALID_AUTHORITIES.contains(authority.trim())) {
            throw new IllegalArgumentException("Source Authority chỉ nhận 'admin_reviewed' hoặc 'official'.");
        }
        if (effectiveAt != null && expiresAt != null && !expiresAt.isAfter(effectiveAt)) {
            throw new IllegalArgumentException("Ngày hết hạn (expires_at) phải sau ngày hiệu lực (effective_at).");
        }
        if (chunkSize < 200 || chunkSize > 1200 || chunkOverlap < 0 || chunkOverlap > 400 || chunkOverlap >= chunkSize / 2) {
            throw new IllegalArgumentException("Chunk size (200..1200) hoặc overlap (0..400) không hợp lệ.");
        }
    }

    private static void validateFaqInputs(String question, String answer, String category, int priority,
            String authority, String policyKey, Instant effectiveAt, Instant expiresAt, int actorId) {
        if (actorId <= 0) throw new IllegalArgumentException("Yêu cầu tài khoản Admin hợp lệ.");
        if (question == null || question.trim().isEmpty() || question.trim().length() > 500) {
            throw new IllegalArgumentException("Câu hỏi FAQ bắt buộc (1..500 ký tự).");
        }
        if (answer == null || answer.trim().isEmpty() || answer.trim().length() > 20000) {
            throw new IllegalArgumentException("Câu trả lời FAQ bắt buộc (1..20.000 ký tự).");
        }
        if (category != null && !category.trim().isEmpty() && !VALID_CATEGORIES.contains(category.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Danh mục FAQ không hợp lệ.");
        }
        if (priority < 1 || priority > 1000) {
            throw new IllegalArgumentException("Độ ưu tiên (priority) phải từ 1 đến 1000.");
        }
        if (authority != null && !authority.trim().isEmpty() && !VALID_AUTHORITIES.contains(authority.trim())) {
            throw new IllegalArgumentException("Source Authority không hợp lệ.");
        }
        if (policyKey != null && !policyKey.trim().isEmpty() && !policyKey.trim().matches("[a-z0-9][a-z0-9._-]{0,99}")) {
            throw new IllegalArgumentException("Policy Key không hợp lệ.");
        }
        if (effectiveAt != null && expiresAt != null && !expiresAt.isAfter(effectiveAt)) {
            throw new IllegalArgumentException("Ngày hết hạn phải sau ngày hiệu lực.");
        }
    }

    private static String sanitizeMeta(String s, int maxLen) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    private static String stripExtensionForTitle(String filename) {
        if (filename == null) return "Tài liệu tải lên";
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base.replace('_', ' ').replace('-', ' ').trim();
    }

    private static void insertRevisionSnapshot(Connection c, ManagedDocument d, String note,
            Long rollbackFromVersion, int actorId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO public.knowledge_document_revisions ("
                + "document_id, version_number, title, category, tags, department, source, source_type, "
                + "source_authority, policy_key, content, content_hash, workflow_status, "
                + "effective_at, expires_at, supersedes_document_id, change_note, rollback_from_version, created_by"
                + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT (document_id, version_number) DO UPDATE SET "
                + "workflow_status = EXCLUDED.workflow_status, change_note = EXCLUDED.change_note")) {
            p.setLong(1, d.id);
            p.setLong(2, d.version);
            p.setString(3, d.title);
            p.setString(4, d.category);
            p.setString(5, d.tags);
            p.setString(6, d.department);
            p.setString(7, d.source);
            p.setString(8, d.sourceType);
            p.setString(9, d.sourceAuthority);
            p.setString(10, d.policyKey);
            p.setString(11, d.content);
            p.setString(12, d.contentHash);
            p.setString(13, d.workflowStatus);
            p.setTimestamp(14, RagDatabase.timestamp(d.effectiveAt));
            p.setTimestamp(15, RagDatabase.timestamp(d.expiresAt));
            if (d.supersedesDocumentId == null) p.setNull(16, Types.BIGINT);
            else p.setLong(16, d.supersedesDocumentId.longValue());
            p.setString(17, note == null ? "" : note);
            if (rollbackFromVersion == null) p.setNull(18, Types.BIGINT);
            else p.setLong(18, rollbackFromVersion.longValue());
            p.setInt(19, actorId);
            p.executeUpdate();
        }
    }

    private static void insertReviewRecord(Connection c, long docId, long versionNumber, String action,
            String prevStatus, String newStatus, int reviewerId, String comments, String rejectionReason) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO public.knowledge_document_reviews ("
                + "document_id, version_number, action, previous_status, new_status, reviewer_id, comments, rejection_reason"
                + ") VALUES (?,?,?,?,?,?,?,?)")) {
            p.setLong(1, docId);
            p.setLong(2, versionNumber);
            p.setString(3, action);
            p.setString(4, prevStatus);
            p.setString(5, newStatus);
            p.setInt(6, reviewerId);
            p.setString(7, comments == null ? "" : comments);
            p.setString(8, rejectionReason);
            p.executeUpdate();
        }
    }

    private static void insertFaqRevision(Connection c, FaqItem faq, String note, int actorId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO public.knowledge_faq_revisions ("
                + "faq_id, version_number, question, answer, category, tags, priority, workflow_status, change_note, created_by"
                + ") VALUES (?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT (faq_id, version_number) DO UPDATE SET workflow_status = EXCLUDED.workflow_status")) {
            p.setLong(1, faq.id);
            p.setLong(2, faq.versionNumber);
            p.setString(3, faq.question);
            p.setString(4, faq.answer);
            p.setString(5, faq.category);
            p.setString(6, faq.tags);
            p.setInt(7, faq.priority);
            p.setString(8, faq.workflowStatus);
            p.setString(9, note == null ? "" : note);
            p.setInt(10, actorId);
            p.executeUpdate();
        }
    }

    private static ManagedDocument readManagedDocument(ResultSet r) throws SQLException {
        ManagedDocument d=new ManagedDocument(
                r.getLong("id"),
                r.getLong("version"),
                r.getString("title"),
                r.getString("category"),
                r.getString("content"),
                r.getString("source"),
                r.getString("status"),
                r.getString("workflow_status"),
                r.getString("source_type"),
                r.getString("tags"),
                r.getString("department"),
                r.getString("content_hash"),
                r.getString("indexing_status"),
                (Long) r.getObject("indexed_version"),
                r.getString("policy_key"),
                r.getString("source_authority"),
                (Long) r.getObject("supersedes_document_id"),
                RagDatabase.instant(r, "effective_at"),
                RagDatabase.instant(r, "expires_at"),
                r.getInt("approved_by"),
                RagDatabase.instant(r, "approved_at"),
                r.getInt("created_by"),
                r.getInt("updated_by"),
                r.getInt("reviewed_by"),
                RagDatabase.instant(r, "reviewed_at"),
                r.getString("rejection_reason"),
                r.getInt("chunk_size"),
                r.getInt("chunk_overlap"),
                RagDatabase.instant(r, "created_at"),
                RagDatabase.instant(r, "updated_at"));
        try{d.embeddingProvider=r.getString("embedding_provider");d.embeddingError=r.getString("embedding_error");d.lastIndexedAt=RagDatabase.instant(r,"last_indexed_at");}catch(SQLException missingMigration){/* legacy listing only; indexing needs migration 006 */}
        return d;
    }

    private static DocumentRevision readRevision(ResultSet r) throws SQLException {
        return new DocumentRevision(
                r.getLong("id"),
                r.getLong("document_id"),
                r.getLong("version_number"),
                r.getString("title"),
                r.getString("category"),
                r.getString("tags"),
                r.getString("department"),
                r.getString("source"),
                r.getString("source_type"),
                r.getString("source_authority"),
                r.getString("policy_key"),
                r.getString("content"),
                r.getString("content_hash"),
                r.getString("workflow_status"),
                RagDatabase.instant(r, "effective_at"),
                RagDatabase.instant(r, "expires_at"),
                (Long) r.getObject("supersedes_document_id"),
                r.getString("change_note"),
                (Long) r.getObject("rollback_from_version"),
                r.getInt("created_by"),
                RagDatabase.instant(r, "created_at"));
    }

    private static FaqItem readFaq(ResultSet r) throws SQLException {
        return new FaqItem(
                r.getLong("id"),
                r.getString("question"),
                r.getString("answer"),
                r.getString("category"),
                r.getString("tags"),
                r.getInt("priority"),
                r.getString("workflow_status"),
                r.getLong("version_number"),
                RagDatabase.instant(r, "effective_at"),
                RagDatabase.instant(r, "expires_at"),
                r.getString("source_authority"),
                r.getString("policy_key"),
                (Long) r.getObject("linked_document_id"),
                r.getInt("created_by"),
                r.getInt("updated_by"),
                r.getInt("reviewed_by"),
                RagDatabase.instant(r, "reviewed_at"),
                r.getString("rejection_reason"),
                RagDatabase.instant(r, "created_at"),
                RagDatabase.instant(r, "updated_at"));
    }

    private static Map<String, Object> toAuditState(ManagedDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.id);
        m.put("version", d.version);
        m.put("title", d.title);
        m.put("category", d.category);
        m.put("workflowStatus", d.workflowStatus);
        m.put("status", d.status);
        m.put("indexingStatus", d.indexingStatus);
        m.put("indexedVersion", d.indexedVersion);
        m.put("sourceAuthority", d.sourceAuthority);
        m.put("policyKey", d.policyKey);
        m.put("contentHash", d.contentHash);
        return m;
    }

    private static Map<String, Object> toFaqAuditState(FaqItem f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.id);
        m.put("versionNumber", f.versionNumber);
        m.put("question", f.question);
        m.put("category", f.category);
        m.put("priority", f.priority);
        m.put("workflowStatus", f.workflowStatus);
        m.put("linkedDocumentId", f.linkedDocumentId);
        return m;
    }

    private static Map<String, Object> toRagSettingsMap(RagAdminSettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("localIndexingEnabled", s.localIndexingEnabled);
        m.put("stagingRagEnabled", s.stagingRagEnabled);
        m.put("productionRagEnabled", s.productionRagEnabled);
        m.put("topK", s.topK);
        m.put("similarityThreshold", s.similarityThreshold);
        m.put("maxContextTokens", s.maxContextTokens);
        m.put("chunkSize", s.chunkSize);
        m.put("chunkOverlap", s.chunkOverlap);
        return m;
    }

    private static KnowledgeDocumentDTO toDocumentDTO(ManagedDocument d) {
        return new KnowledgeDocumentDTO(
                d.id, d.version, d.title, d.category, d.content, d.source, d.status,
                d.effectiveAt, d.expiresAt, d.indexingStatus, d.policyKey, d.sourceAuthority,
                d.supersedesDocumentId, d.approvedBy, d.approvedAt);
    }

    private static int parseIntSafe(String raw, int def, int min, int max) {
        if (raw == null || raw.trim().isEmpty()) return def;
        try {
            int v = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) {
            return def;
        }
    }

    private static double parseDoubleSafe(String raw, double def, double min, double max) {
        if (raw == null || raw.trim().isEmpty()) return def;
        try {
            double v = Double.parseDouble(raw.trim());
            if (!Double.isFinite(v)) return def;
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) {
            return def;
        }
    }
}
