package controller;

import dao.AdminKnowledgeDAO;
import dto.AdminKnowledgeDTO.*;
import dto.UserDTO;
import rag.DocumentChunkingService;
import rag.extract.DocumentExtractionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Phase 3: Admin Knowledge Base & RAG Management Console Controller.
 * Enforces Admin RBAC, CSRF validation, multipart upload bounds, and 100% offline operation.
 */
@WebServlet("/admin/knowledge-base")
@MultipartConfig(maxFileSize = 10485760, maxRequestSize = 11534336)
public class AdminKnowledgeBaseServlet extends HttpServlet {

    protected AdminKnowledgeDAO dao() {
        return new AdminKnowledgeDAO();
    }

    protected DocumentExtractionService extractor() {
        return new DocumentExtractionService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");

        HttpSession session = req.getSession(false);
        UserDTO user = session != null ? (UserDTO) session.getAttribute("user") : null;
        if (!security.AdminSecurity.admin(user)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Yêu cầu quyền Quản trị viên (Admin).");
            return;
        }
        req.setAttribute("csrfToken", security.AdminSecurity.token(session));
        req.setAttribute("ragRuntimeMode",System.getenv().getOrDefault("RAG_RUNTIME_MODE","OFFLINE_MOCK"));
        if(rag.runtime.GeminiRuntimeConfig.activeRequested(System.getenv()))try{
            req.setAttribute("ragLivePlan",rag.runtime.GeminiRuntimeConfig.loadActive(System.getenv()));
        }catch(Exception blocked){req.setAttribute("ragRuntimeNotice","Live execution blocked: kiểm tra key test, plan/phê duyệt và xác minh quota/model. Không fallback sang key production.");}

        String tab = cleanParam(req.getParameter("tab"), "dashboard");
        if (!Arrays.asList("dashboard", "documents", "upload", "imports", "faqs", "indexing", "rag-config", "audit", "playground").contains(tab)) {
            tab = "dashboard";
        }
        req.setAttribute("kbTab", tab);

        AdminKnowledgeDAO store = dao();
        req.setAttribute("systemStatus", store.getSystemReadinessStatus());
        try {
            req.setAttribute("ragSettings", store.getRagSettings());
        } catch (Exception ignored) {}
        try {
            if("downloadOriginal".equals(req.getParameter("action"))){
                long documentId=parseLong(req.getParameter("docId"),0);String fileId=req.getParameter("fileId");
                UploadedFileRecord found=null;for(UploadedFileRecord f:store.listUploadedFiles(documentId))if(f.id.equals(fileId))found=f;
                if(found==null){resp.sendError(404);return;}
                byte[] bytes=store.loadUploadedFileBytes(documentId,fileId);
                if(bytes==null){
                    java.nio.file.Path storageRoot=extractor().getStorageRoot();
                    if(!java.nio.file.Files.isDirectory(storageRoot)){resp.sendError(404);return;}
                    java.nio.file.Path root=storageRoot.toRealPath();
                    java.nio.file.Path candidate=root.resolve(found.storageRelPath).normalize();
                    if(!java.nio.file.Files.isRegularFile(candidate)){resp.sendError(404);return;}
                    java.nio.file.Path path=candidate.toRealPath();
                    if(!path.startsWith(root)||java.nio.file.Files.size(path)!=found.fileSizeBytes){resp.sendError(404);return;}
                    bytes=java.nio.file.Files.readAllBytes(path);
                }else if(bytes.length!=found.fileSizeBytes){
                    resp.sendError(409);return;
                }
                StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format("%02x",b&255));
                if(!hash.toString().equals(found.sha256Hash)){resp.sendError(409);return;}
                resp.setContentType("application/octet-stream");resp.setHeader("Content-Disposition","attachment; filename*=UTF-8''"+java.net.URLEncoder.encode(found.originalFilename,"UTF-8").replace("+","%20"));
                resp.setContentLength(bytes.length);resp.getOutputStream().write(bytes);return;
            }
            DashboardMetrics metrics = store.getDashboardMetrics();
            RagAdminSettings ragSettings = store.getRagSettings();
            req.setAttribute("metrics", metrics);
            req.setAttribute("ragSettings", ragSettings);

            // Load documents table data
            String q = req.getParameter("q");
            String category = req.getParameter("category");
            String workflowStatus = req.getParameter("workflowStatus");
            String indexingFilter = req.getParameter("indexingFilter");
            int page = parseInt(req.getParameter("page"), 1);
            int pageSize = 15;

            int totalDocs = store.countDocuments(q, category, workflowStatus, indexingFilter);
            int totalPages = Math.max(1, (totalDocs + pageSize - 1) / pageSize);
            if (page > totalPages) page = totalPages;

            List<ManagedDocument> documents = store.searchDocuments(q, category, workflowStatus, indexingFilter, page, pageSize);
            req.setAttribute("documents", documents);
            req.setAttribute("docTotal", totalDocs);
            req.setAttribute("docPage", page);
            req.setAttribute("docTotalPages", totalPages);
            req.setAttribute("filterQ", q == null ? "" : q);
            req.setAttribute("filterCategory", category == null ? "all" : category);
            req.setAttribute("filterWorkflowStatus", workflowStatus == null ? "all" : workflowStatus);
            req.setAttribute("filterIndexing", indexingFilter == null ? "all" : indexingFilter);

            // Selected document details, revisions, reviews, diff, and chunk preview
            long docId = parseLong(req.getParameter("docId"), 0L);
            if (docId > 0) {
                ManagedDocument selectedDoc = store.findDocument(docId);
                if (selectedDoc != null) {
                    req.setAttribute("selectedDoc", selectedDoc);
                    if("imports".equals(tab))req.setAttribute("importSections",store.listImportSections(docId));
                    List<DocumentRevision> revisions = store.listRevisions(docId);
                    req.setAttribute("docRevisions", revisions);
                    req.setAttribute("docReviews", store.listReviews(docId));
                    req.setAttribute("docFiles", store.listUploadedFiles(Long.valueOf(docId)));
                    try {
                        List<DocumentChunkingService.ChunkPreview> chunkPreviews = store.previewDocumentChunks(docId);
                        req.setAttribute("chunkPreviews", chunkPreviews);
                    } catch (Exception ignored) {
                        req.setAttribute("chunkPreviews", Collections.emptyList());
                    }

                    long compareFrom = parseLong(req.getParameter("compareFrom"), 0L);
                    long compareTo = parseLong(req.getParameter("compareTo"), 0L);
                    if (compareFrom > 0 && compareTo > 0) {
                        req.setAttribute("revisionDiff", store.compareRevisions(docId, compareFrom, compareTo));
                    } else if (revisions.size() >= 2) {
                        req.setAttribute("revisionDiff", store.compareRevisions(
                                docId, revisions.get(1).versionNumber, revisions.get(0).versionNumber));
                    }
                }
            }

            // Uploaded files list
            req.setAttribute("uploadedFiles", store.listUploadedFiles(null));

            // FAQs list & selected FAQ
            String faqStatus = req.getParameter("faqStatus");
            String faqCategory = req.getParameter("faqCategory");
            req.setAttribute("faqs", store.listFaqs(faqStatus, faqCategory));
            req.setAttribute("filterFaqStatus", faqStatus == null ? "all" : faqStatus);
            req.setAttribute("filterFaqCategory", faqCategory == null ? "all" : faqCategory);

            long faqId = parseLong(req.getParameter("faqId"), 0L);
            if (faqId > 0) {
                FaqItem selectedFaq = store.findFaq(faqId);
                if (selectedFaq != null) {
                    req.setAttribute("selectedFaq", selectedFaq);
                    req.setAttribute("faqRevisions", store.listFaqRevisions(faqId));
                }
            }

            // Documents needing reindex for Indexing tab
            req.setAttribute("staleDocuments", store.searchDocuments(null, null, "PUBLISHED", "needs_reindex", 1, 50));

            // Audit logs
            String auditResource = req.getParameter("auditResource");
            String auditAction = req.getParameter("auditAction");
            int auditPage = parseInt(req.getParameter("auditPage"), 1);
            req.setAttribute("auditLogs", store.listAuditLogs(auditResource, auditAction, auditPage, 25));
            req.setAttribute("filterAuditResource", auditResource == null ? "all" : auditResource);
            req.setAttribute("filterAuditAction", auditAction == null ? "all" : auditAction);
            req.setAttribute("auditPage", auditPage);

        } catch (Exception e) {
            req.setAttribute("kbError", "Không thể tải dữ liệu Knowledge Base: " + safeMessage(e));
        }

        req.getRequestDispatcher("/view/admin/knowledge-base.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");

        HttpSession session = req.getSession(false);
        UserDTO user = session != null ? (UserDTO) session.getAttribute("user") : null;
        if (!security.AdminSecurity.admin(user) || !security.AdminSecurity.csrf(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Yêu cầu quyền Admin và mã bảo vệ CSRF hợp lệ.");
            return;
        }
        req.setAttribute("csrfToken", security.AdminSecurity.token(session));

        String action = cleanParam(req.getParameter("action"), "");
        String targetTab = cleanParam(req.getParameter("tab"), "dashboard");
        int actorId = user.getId();
        String actorName = user.getUsername() != null ? user.getUsername() : ("admin#" + actorId);

        AdminKnowledgeDAO store = dao();
        try {
            if ("prepareImport".equals(action)) {
                long id=parseLong(req.getParameter("id"),0);store.prepareImport(id,parseLong(req.getParameter("version"),0),actorId,actorName);
                resp.sendRedirect(req.getContextPath()+"/admin/knowledge-base?tab=imports&docId="+id);return;
            } else if("reviewImportSection".equals(action)||"createImportDraft".equals(action)){
                targetTab="imports";long id=parseLong(req.getParameter("sectionId"),0);
                if("reviewImportSection".equals(action))store.reviewImportSection(id,req.getParameter("title"),req.getParameter("classification"),"true".equals(req.getParameter("ignore")),actorId,actorName);
                else store.createImportDraft(id,actorId,actorName);
                resp.sendRedirect(req.getContextPath()+"/admin/knowledge-base?tab=imports&docId="+parseLong(req.getParameter("docId"),0));return;
            } else if ("createDocument".equals(action)) {
                targetTab = "documents";
                ManagedDocument created = store.createDocument(
                        req.getParameter("title"),
                        cleanParam(req.getParameter("category"), "guide"),
                        req.getParameter("content"),
                        cleanParam(req.getParameter("source"), "manual:admin"),
                        "MANUAL",
                        req.getParameter("tags"),
                        req.getParameter("department"),
                        req.getParameter("policyKey"),
                        cleanParam(req.getParameter("sourceAuthority"), "admin_reviewed"),
                        parseNullableLong(req.getParameter("supersedesDocumentId")),
                        parseDateStart(req.getParameter("effectiveAt")),
                        parseDateEnd(req.getParameter("expiresAt")),
                        parseInt(req.getParameter("chunkSize"), 1000),
                        parseInt(req.getParameter("chunkOverlap"), 100),
                        req.getParameter("changeNote"),
                        actorId,
                        actorName);
                session.setAttribute("kbMessage", "Đã tạo tài liệu #" + created.id + " ở trạng thái DRAFT (v1).");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + created.id);
                return;

            } else if ("updateDocument".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                boolean preservePublished = "true".equalsIgnoreCase(req.getParameter("preservePublishedState"));
                ManagedDocument updated = store.updateDocument(
                        id,
                        req.getParameter("title"),
                        cleanParam(req.getParameter("category"), "guide"),
                        req.getParameter("content"),
                        cleanParam(req.getParameter("source"), "manual:admin"),
                        req.getParameter("tags"),
                        req.getParameter("department"),
                        req.getParameter("policyKey"),
                        cleanParam(req.getParameter("sourceAuthority"), "admin_reviewed"),
                        parseNullableLong(req.getParameter("supersedesDocumentId")),
                        parseDateStart(req.getParameter("effectiveAt")),
                        parseDateEnd(req.getParameter("expiresAt")),
                        parseInt(req.getParameter("chunkSize"), 1000),
                        parseInt(req.getParameter("chunkOverlap"), 100),
                        preservePublished,
                        req.getParameter("changeNote"),
                        actorId,
                        actorName);
                session.setAttribute("kbMessage", "Đã lưu phiên bản mới v" + updated.version + " cho tài liệu #" + updated.id
                        + " (Trạng thái: " + updated.workflowStatus + ").");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + updated.id);
                return;

            } else if ("submitReview".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                ManagedDocument d = store.submitForReview(id, actorId, actorName, req.getParameter("comments"));
                session.setAttribute("kbMessage", "Tài liệu #" + d.id + " đã chuyển sang trạng thái PENDING_REVIEW.");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + d.id);
                return;

            } else if ("approveDocument".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                ManagedDocument d = store.approveDocument(id, actorId, actorName, req.getParameter("comments"));
                session.setAttribute("kbMessage", "Đã phê duyệt (APPROVED) tài liệu #" + d.id + ".");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + d.id);
                return;

            } else if ("rejectDocument".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                ManagedDocument d = store.rejectDocument(id, actorId, actorName, req.getParameter("rejectionReason"));
                session.setAttribute("kbMessage", "Đã từ chối (REJECTED) tài liệu #" + d.id + ".");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + d.id);
                return;

            } else if ("publishDocument".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                ManagedDocument d = store.publishDocument(id, actorId, actorName, req.getParameter("comments"));
                session.setAttribute("kbMessage", "Đã xuất bản (PUBLISHED) tài liệu #" + d.id + " và xếp hàng lập chỉ mục RAG.");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + d.id);
                return;

            } else if ("unpublishDocument".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                ManagedDocument d = store.unpublishDocument(id, actorId, actorName, req.getParameter("comments"));
                session.setAttribute("kbMessage", "Đã gỡ xuất bản (UNPUBLISHED) tài liệu #" + d.id + " và thu hồi khỏi chỉ mục RAG.");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + d.id);
                return;

            } else if ("archiveDocument".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                ManagedDocument d = store.archiveDocument(id, actorId, actorName, req.getParameter("comments"));
                session.setAttribute("kbMessage", "Đã lưu trữ (ARCHIVED) tài liệu #" + d.id + ".");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + d.id);
                return;

            } else if ("rollbackDocument".equals(action)) {
                targetTab = "documents";
                long id = parseLong(req.getParameter("id"), 0L);
                long targetVer = parseLong(req.getParameter("targetVersion"), 0L);
                ManagedDocument d = store.rollbackDocument(id, targetVer, actorId, actorName);
                session.setAttribute("kbMessage", "Đã khôi phục nội dung từ v" + targetVer
                        + " thành phiên bản mới v" + d.version + " (trạng thái DRAFT).");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + d.id);
                return;

            } else if ("uploadDocument".equals(action)) {
                targetTab = "upload";
                Part filePart = req.getPart("documentFile");
                if (filePart == null || filePart.getSize() <= 0) {
                    throw new IllegalArgumentException("Vui lòng chọn tệp tài liệu (.pdf, .docx, .txt, .md) để tải lên.");
                }
                String submittedName = extractSubmittedFileName(filePart);
                DocumentExtractionService.ExtractionResult extraction;
                try (InputStream in = filePart.getInputStream()) {
                    extraction = extractor().storeAndExtract(submittedName, filePart.getContentType(), in);
                }
                ManagedDocument created = store.uploadAndCreateDraft(
                        extraction,
                        req.getParameter("title"),
                        cleanParam(req.getParameter("category"), "guide"),
                        req.getParameter("tags"),
                        req.getParameter("department"),
                        req.getParameter("policyKey"),
                        cleanParam(req.getParameter("sourceAuthority"), "admin_reviewed"),
                        parseDateStart(req.getParameter("effectiveAt")),
                        parseDateEnd(req.getParameter("expiresAt")),
                        parseInt(req.getParameter("chunkSize"), 1000),
                        parseInt(req.getParameter("chunkOverlap"), 100),
                        actorId,
                        actorName);
                session.setAttribute("kbMessage", "Đã tải lên '" + extraction.originalFilename + "' ("
                        + extraction.status + ", " + extraction.charCount + " ký tự) -> Tạo tài liệu #"
                        + created.id + " ở trạng thái " + created.workflowStatus + " (chưa xuất bản/chưa lập chỉ mục).");
                if("true".equals(req.getParameter("splitImport")) && "EXTRACTED".equals(extraction.status)){
                    store.prepareImport(created.id,created.version,actorId,actorName);
                    resp.sendRedirect(req.getContextPath()+"/admin/knowledge-base?tab=imports&docId="+created.id);
                }else resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=documents&docId=" + created.id);
                return;

            } else if ("createFaq".equals(action)) {
                targetTab = "faqs";
                FaqItem faq = store.createFaq(
                        req.getParameter("question"),
                        req.getParameter("answer"),
                        cleanParam(req.getParameter("category"), "faq"),
                        req.getParameter("tags"),
                        parseInt(req.getParameter("priority"), 10),
                        cleanParam(req.getParameter("sourceAuthority"), "admin_reviewed"),
                        req.getParameter("policyKey"),
                        parseDateStart(req.getParameter("effectiveAt")),
                        parseDateEnd(req.getParameter("expiresAt")),
                        actorId,
                        actorName);
                session.setAttribute("kbMessage", "Đã tạo FAQ #" + faq.id + " ở trạng thái DRAFT.");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=faqs&faqId=" + faq.id);
                return;

            } else if ("updateFaq".equals(action)) {
                targetTab = "faqs";
                long id = parseLong(req.getParameter("id"), 0L);
                FaqItem faq = store.updateFaq(
                        id,
                        req.getParameter("question"),
                        req.getParameter("answer"),
                        cleanParam(req.getParameter("category"), "faq"),
                        req.getParameter("tags"),
                        parseInt(req.getParameter("priority"), 10),
                        cleanParam(req.getParameter("sourceAuthority"), "admin_reviewed"),
                        req.getParameter("policyKey"),
                        parseDateStart(req.getParameter("effectiveAt")),
                        parseDateEnd(req.getParameter("expiresAt")),
                        req.getParameter("changeNote"),
                        actorId,
                        actorName);
                session.setAttribute("kbMessage", "Đã cập nhật FAQ #" + faq.id + " lên phiên bản v" + faq.versionNumber + ".");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=faqs&faqId=" + faq.id);
                return;

            } else if (Arrays.asList("submitFaqReview", "approveFaq", "rejectFaq", "publishFaq", "unpublishFaq", "archiveFaq").contains(action)) {
                targetTab = "faqs";
                long id = parseLong(req.getParameter("id"), 0L);
                String faqAction = "SUBMIT_REVIEW";
                if ("approveFaq".equals(action)) faqAction = "APPROVE";
                else if ("rejectFaq".equals(action)) faqAction = "REJECT";
                else if ("publishFaq".equals(action)) faqAction = "PUBLISH";
                else if ("unpublishFaq".equals(action)) faqAction = "UNPUBLISH";
                else if ("archiveFaq".equals(action)) faqAction = "ARCHIVE";

                FaqItem faq = store.transitionFaq(id, faqAction, req.getParameter("rejectionReason"), actorId, actorName);
                session.setAttribute("kbMessage", "Đã chuyển trạng thái FAQ #" + faq.id + " sang " + faq.workflowStatus + ".");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=faqs&faqId=" + faq.id);
                return;

            } else if("requestGeminiIndex".equals(action)){
                targetTab="indexing";long id=parseLong(req.getParameter("id"),0);
                ManagedDocument d=store.requestGeminiIndex(id,actorId,actorName);
                session.setAttribute("kbMessage","GEMINI INDEX #"+id+": "+d.getRuntimeIndexStatus()+". Xem trạng thái và journal; không tự retry.");
                resp.sendRedirect(req.getContextPath()+"/admin/knowledge-base?tab=indexing&docId="+id);return;
            } else if("runGeminiPreview".equals(action)){
                targetTab="playground";req.setAttribute("playgroundResult",store.runGeminiPreview(req.getParameter("query"),actorId,actorName));req.setAttribute("kbTab","playground");doGet(req,resp);return;
            } else if ("reindexDocument".equals(action)) {
                targetTab = "indexing";
                long docId = parseLong(req.getParameter("id"), 0L);
                ManagedDocument indexedDoc = store.reindexDocumentOffline(docId, actorId, actorName);
                session.setAttribute("kbMessage", "MOCK INDEX đã xử lý tài liệu #" + indexedDoc.id
                        + " (v" + indexedDoc.version + ", trạng thái: " + indexedDoc.indexingStatus + ", 0 Gemini API calls).");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=indexing&docId=" + indexedDoc.id);
                return;

            } else if ("runOfflineIndexingBatch".equals(action)) {
                targetTab = "indexing";
                int processed = store.runOfflineIndexingBatch(20, actorId, actorName);
                session.setAttribute("kbMessage", "Đã chạy bộ lập chỉ mục offline (MockEmbeddingProvider): xử lý "
                        + processed + " job (0 Gemini API calls).");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=indexing");
                return;

            } else if ("recoverExpiredLeases".equals(action)) {
                targetTab = "indexing";
                int recovered = store.recoverExpiredIndexLeases(actorId, actorName);
                session.setAttribute("kbMessage", "Đã thu hồi " + recovered + " job quá hạn lease sang trạng thái failed_retryable (0 Gemini API calls).");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=indexing");
                return;

            } else if ("saveRagConfig".equals(action)) {
                targetTab = "rag-config";
                boolean localIdx = "true".equalsIgnoreCase(req.getParameter("localIndexingEnabled"));
                boolean stagingRag = "true".equalsIgnoreCase(req.getParameter("stagingRagEnabled"));
                boolean prodRagRequested = "true".equalsIgnoreCase(req.getParameter("productionRagEnabled"));
                int topK = parseInt(req.getParameter("topK"), 3);
                double sim = parseDouble(req.getParameter("similarityThreshold"), 0.72);
                int maxTokens = parseInt(req.getParameter("maxContextTokens"), 900);
                int chunkSize = parseInt(req.getParameter("chunkSize"), 1000);
                int chunkOverlap = parseInt(req.getParameter("chunkOverlap"), 100);

                store.saveRagSettings(localIdx, stagingRag, prodRagRequested, topK, sim, maxTokens,
                        chunkSize, chunkOverlap, actorId, actorName);
                session.setAttribute("kbMessage", "Đã lưu cấu hình RAG (Production RAG luôn khóa an toàn ở trạng thái OFF).");
                resp.sendRedirect(req.getContextPath() + "/admin/knowledge-base?tab=rag-config");
                return;

            } else if ("runPlaygroundQuery".equals(action)) {
                targetTab = "playground";
                String query = req.getParameter("query");
                PlaygroundResult pgResult = store.runOfflinePlayground(query, actorId, actorName);
                req.setAttribute("playgroundResult", pgResult);
                req.setAttribute("kbTab", "playground");
                doGet(req, resp);
                return;

            } else {
                throw new IllegalArgumentException("Thao tác không được hỗ trợ: " + action);
            }

        } catch (Exception e) {
            req.setAttribute("kbError", safeMessage(e));
            req.setAttribute("kbTab", targetTab);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            doGet(req, resp);
        }
    }

    private static String extractSubmittedFileName(Part part) {
        String submitted = part.getSubmittedFileName();
        if (submitted != null && !submitted.trim().isEmpty()) {
            return submitted.trim();
        }
        String cd = part.getHeader("content-disposition");
        if (cd != null) {
            for (String token : cd.split(";")) {
                String t = token.trim();
                if (t.toLowerCase(Locale.ROOT).startsWith("filename=")) {
                    String name = t.substring(9).trim();
                    if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                        name = name.substring(1, name.length() - 1);
                    }
                    return name;
                }
            }
        }
        return "uploaded.txt";
    }

    private static String cleanParam(String raw, String fallback) {
        return (raw == null || raw.trim().isEmpty()) ? fallback : raw.trim();
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Long parseNullableLong(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            long v = Long.parseLong(raw.trim());
            return v > 0 ? Long.valueOf(v) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Instant parseDateStart(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(raw.trim()).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }

    private static Instant parseDateEnd(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(raw.trim()).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeMessage(Exception e) {
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return e.getMessage();
        }
        return "Không thể thực hiện yêu cầu. Vui lòng kiểm tra dữ liệu đầu vào hoặc kết nối cơ sở dữ liệu.";
    }
}
