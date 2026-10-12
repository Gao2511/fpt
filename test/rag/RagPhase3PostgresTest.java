package rag;

import dao.AdminKnowledgeDAO;
import dto.AdminKnowledgeDTO.*;
import rag.extract.DocumentExtractionService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * Phase 3.12: End-to-End Isolated PostgreSQL 17 + pgvector 0.8.2 Integration Test Suite
 * for the Admin Knowledge Base & RAG Management System.
 * Zero external Gemini API calls; uses MockEmbeddingProvider (768d).
 */
public final class RagPhase3PostgresTest {
    private static int passed;

    interface Case {
        void run() throws Exception;
    }

    private static void test(String name, Case c) throws Exception {
        c.run();
        passed++;
        System.out.println("PASS Phase3-PostgreSQL " + name);
    }

    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    public static void main(String[] args) throws Exception {
        if (!"true".equals(System.getenv("RAG_TEST_DATABASE_APPROVED"))
                || !"development".equals(System.getenv("RAG_ENVIRONMENT"))
                || !"127.0.0.1".equals(System.getenv("DB_HOST"))
                || !"fpt_rag_test".equals(System.getenv("DB_NAME"))) {
            throw new IllegalStateException("Refusing database writes: require an approved fresh loopback fpt_rag_test database");
        }

        RagDatabase db = new RagDatabase();
        try (Connection c = db.open(); Statement s = c.createStatement()) {
            // Ensure settings table exists for RagAdminSettings persistence in isolated test DB
            s.execute("CREATE TABLE IF NOT EXISTS public.settings ("
                    + "id SERIAL PRIMARY KEY, "
                    + "setting_key VARCHAR(120) UNIQUE NOT NULL, "
                    + "setting_value TEXT, "
                    + "description TEXT, "
                    + "updated_at TIMESTAMPTZ DEFAULT NOW())");
        }

        AdminKnowledgeDAO store = new AdminKnowledgeDAO(db);
        Path tempUploadDir = Files.createTempDirectory("fpt-kb-pg-uploads-");

        try {
            DocumentExtractionService extractor = new DocumentExtractionService(tempUploadDir);

            // 1. INITIAL DASHBOARD METRICS
            test("initial dashboard metrics reflect empty knowledge base", () -> {
                DashboardMetrics m = store.getDashboardMetrics();
                check(m.totalDocuments == 0 && m.publishedDocuments == 0 && m.totalFaqs == 0 && m.totalChunks == 0,
                        "Expected empty initial metrics");
            });

            // 2. SECURE UPLOAD -> CREATES DRAFT DOCUMENT + KNOWLEDGE_FILES RECORD (NOT PUBLISHED, NOT INDEXED)
            final long[] uploadedDocId = {0L};
            test("upload TXT file creates DRAFT document and knowledge_files row without publishing or indexing", () -> {
                String txt = "Quy định bảo hành thiết bị modem WiFi 6 tại chi nhánh FPT Telecom: "
                        + "Khách hàng mang thiết bị và giấy tờ tùy thân đến quầy giao dịch để kiểm tra kỹ thuật.";
                DocumentExtractionService.ExtractionResult ext = extractor.storeAndExtract(
                        "bao-hanh-modem-wifi6.txt", "text/plain",
                        new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8)));

                ManagedDocument doc = store.uploadAndCreateDraft(
                        ext, "Quy định bảo hành thiết bị modem WiFi 6", "policy",
                        "modem,wifi6,baohanh", "Kỹ thuật FPT", "warranty.modem", "official",
                        null, null, 800, 80, 101, "admin_fpt");
                uploadedDocId[0] = doc.id;

                check(doc.id > 0 && doc.version == 1, "Expected v1 document");
                check("DRAFT".equals(doc.workflowStatus) && "draft".equals(doc.status), "Upload must start as DRAFT");
                check("UPLOAD".equals(doc.sourceType), "Expected UPLOAD sourceType");
                check(store.listUploadedFiles(Long.valueOf(doc.id)).size() == 1, "Expected 1 knowledge_files row");

                // Verify DRAFT document is NOT retrievable in playground
                PlaygroundResult pg = store.runOfflinePlayground("quy định bảo hành thiết bị modem wifi 6", 101, "admin_fpt");
                check(pg.selectedChunks.isEmpty() && pg.abstained, "DRAFT document must not be retrieved");
            });

            // 3. CANNOT PUBLISH DIRECTLY FROM DRAFT OR PENDING_REVIEW
            test("cannot publish directly from DRAFT or PENDING_REVIEW without approval", () -> {
                try {
                    store.publishDocument(uploadedDocId[0], 101, "admin_fpt", "Attempt premature publish");
                    throw new AssertionError("Should not allow publish from DRAFT");
                } catch (IllegalStateException expected) {}

                ManagedDocument pending = store.submitForReview(uploadedDocId[0], 101, "admin_fpt", "Gửi duyệt lần 1");
                check("PENDING_REVIEW".equals(pending.workflowStatus) && "draft".equals(pending.status), "Expected PENDING_REVIEW");

                try {
                    store.publishDocument(uploadedDocId[0], 101, "admin_fpt", "Attempt publish from PENDING_REVIEW");
                    throw new AssertionError("Should not allow publish from PENDING_REVIEW");
                } catch (IllegalStateException expected) {}
            });

            // 4. REJECTION REQUIRES REASON & RECORDS REVIEW HISTORY
            test("reject document requires reason and transitions to REJECTED", () -> {
                try {
                    store.rejectDocument(uploadedDocId[0], 102, "reviewer_admin", "   ");
                    throw new AssertionError("Should require non-empty rejection reason");
                } catch (IllegalArgumentException expected) {}

                ManagedDocument rejected = store.rejectDocument(uploadedDocId[0], 102, "reviewer_admin",
                        "Cần bổ sung hướng dẫn kiểm tra nguồn điện trước khi mang ra quầy.");
                check("REJECTED".equals(rejected.workflowStatus) && "draft".equals(rejected.status), "Expected REJECTED");
                check(rejected.rejectionReason.contains("nguồn điện"), "Rejection reason not saved");
                check(rejected.reviewedBy == 102 && rejected.reviewedAt != null, "Reviewer metadata not saved");
            });

            // 5. EDIT CREATES NEW REVISION (v2), DIFF COMPARISON WORKS
            test("editing rejected document creates v2 revision in DRAFT and computes accurate RevisionDiff", () -> {
                String v2Content = "Quy định bảo hành thiết bị modem WiFi 6 tại chi nhánh FPT Telecom:\n"
                        + "Bước 1: Khách hàng kiểm tra dây nguồn điện và khởi động lại thiết bị tại nhà.\n"
                        + "Bước 2: Nếu vẫn lỗi, khách hàng mang thiết bị và giấy tờ tùy thân đến quầy giao dịch FPT.";
                ManagedDocument v2 = store.updateDocument(
                        uploadedDocId[0], "Quy định bảo hành thiết bị modem WiFi 6", "policy",
                        v2Content, "policy:fpt-telecom/warranty-v2", "modem,wifi6,baohanh,nguondien",
                        "Kỹ thuật FPT", "warranty.modem", "official", null, null, null,
                        800, 80, false, "Bổ sung Bước 1 kiểm tra nguồn điện", 101, "admin_fpt");

                check(v2.version == 2 && "DRAFT".equals(v2.workflowStatus), "Expected v2 in DRAFT");
                List<DocumentRevision> revisions = store.listRevisions(uploadedDocId[0]);
                check(revisions.size() == 2, "Expected 2 immutable revisions");

                RevisionDiff diff = store.compareRevisions(uploadedDocId[0], 1L, 2L);
                check(diff.contentChanged && diff.addedCount >= 2 && !diff.metadataChanges.isEmpty(),
                        "Expected content and metadata diff between v1 and v2");
            });

            // 6. FULL APPROVAL -> PUBLISH -> OFFLINE INDEXING -> PLAYGROUND RETRIEVAL
            test("DRAFT -> PENDING_REVIEW -> APPROVED -> PUBLISHED -> Offline Indexing -> Retrievable in Playground", () -> {
                store.submitForReview(uploadedDocId[0], 101, "admin_fpt", "Gửi duyệt v2");
                ManagedDocument approved = store.approveDocument(uploadedDocId[0], 102, "reviewer_admin", "Nội dung v2 đạt chuẩn");
                check("APPROVED".equals(approved.workflowStatus) && "draft".equals(approved.status), "Expected APPROVED");

                // APPROVED but not yet PUBLISHED must still not be retrievable
                PlaygroundResult pgBefore = store.runOfflinePlayground("quy định bảo hành thiết bị modem wifi 6", 101, "admin_fpt");
                check(pgBefore.selectedChunks.isEmpty(), "APPROVED (un-published) document must not be retrieved");

                ManagedDocument published = store.publishDocument(uploadedDocId[0], 102, "reviewer_admin", "Xuất bản chính thức v2");
                check("PUBLISHED".equals(published.workflowStatus) && "published".equals(published.status), "Expected PUBLISHED");
                check(published.isNeedsReindex(), "Newly published document must indicate needsReindex before worker runs");

                ManagedDocument indexed = store.reindexDocumentOffline(uploadedDocId[0], 102, "reviewer_admin");
                check("ready".equals(indexed.indexingStatus) && Long.valueOf(2L).equals(indexed.indexedVersion) && !indexed.isNeedsReindex(),
                        "Expected ready index at v2");

                // Repeated reindex is idempotent
                ManagedDocument reindexedAgain = store.reindexDocumentOffline(uploadedDocId[0], 102, "reviewer_admin");
                check("ready".equals(reindexedAgain.indexingStatus) && Long.valueOf(2L).equals(reindexedAgain.indexedVersion),
                        "Idempotent reindex failed");

                // Verify Playground retrieves the published & indexed v2 document with 0 Gemini API calls
                PlaygroundResult pgAfter = store.runOfflinePlayground("quy định bảo hành thiết bị modem wifi 6", 101, "admin_fpt");
                check("OFFLINE TEST MODE — NO GEMINI API REQUESTS".equals(pgAfter.banner), "Missing offline banner");
                check(!pgAfter.abstained && pgAfter.selectedChunks.size() == 1,
                        "Expected 1 selected chunk in playground, issue=" + pgAfter.selectionIssue + ", rejected=" + pgAfter.rejectedReasons);
                check(pgAfter.deterministicMockAnswer.contains("Bước 1: Khách hàng kiểm tra dây nguồn điện"),
                        "Playground answer did not include verified v2 content: " + pgAfter.deterministicMockAnswer);
            });

            // 7. EDITING PUBLISHED DOCUMENT MARKS NEEDS_REINDEX UNTIL REINDEXED
            test("editing a published document with preservePublishedState=true marks it NEEDS_REINDEX until reindexed", () -> {
                String v3Content = "Quy định bảo hành thiết bị modem WiFi 6 tại chi nhánh FPT Telecom:\n"
                        + "Bước 1: Khách hàng kiểm tra dây nguồn điện và khởi động lại thiết bị tại nhà.\n"
                        + "Bước 2: Khách hàng liên hệ tổng đài kỹ thuật hoặc mang thiết bị đến quầy giao dịch FPT Pleiku.";
                ManagedDocument v3 = store.updateDocument(
                        uploadedDocId[0], "Quy định bảo hành thiết bị modem WiFi 6", "policy",
                        v3Content, "policy:fpt-telecom/warranty-v3", "modem,wifi6,baohanh,pleiku",
                        "Kỹ thuật FPT", "warranty.modem", "official", null, null, null,
                        800, 80, true, "Cập nhật địa điểm quầy giao dịch Pleiku (v3)", 101, "admin_fpt");

                check(v3.version == 3 && "PUBLISHED".equals(v3.workflowStatus) && v3.isNeedsReindex(),
                        "Edited published document must be v3 and flagged NEEDS_REINDEX");

                // Reindex v3 offline
                ManagedDocument v3Indexed = store.reindexDocumentOffline(uploadedDocId[0], 101, "admin_fpt");
                check("ready".equals(v3Indexed.indexingStatus) && Long.valueOf(3L).equals(v3Indexed.indexedVersion) && !v3Indexed.isNeedsReindex(),
                        "Expected v3 indexed");
            });

            // 8. NON-DESTRUCTIVE ROLLBACK CREATES NEW REVISION (v4) FROM v2
            test("rollback from v3 to v2 creates new revision v4 in DRAFT without deleting revision history", () -> {
                ManagedDocument v4 = store.rollbackDocument(uploadedDocId[0], 2L, 101, "admin_fpt");
                check(v4.version == 4 && "DRAFT".equals(v4.workflowStatus) && "draft".equals(v4.status),
                        "Rollback must create v4 in DRAFT");
                check(!v4.content.contains("Pleiku") && v4.content.contains("Bước 1"),
                        "Rollback content did not match v2");
                List<DocumentRevision> revisions = store.listRevisions(uploadedDocId[0]);
                check(revisions.size() == 4 && Long.valueOf(2L).equals(revisions.get(0).rollbackFromVersion),
                        "All 4 revisions must be preserved with rollbackFromVersion=2");

                // Re-approve and re-publish v4, then unpublish and verify immediate removal from retrieval
                store.submitForReview(uploadedDocId[0], 101, "admin_fpt", "Gửi duyệt bản rollback v4");
                store.approveDocument(uploadedDocId[0], 102, "reviewer_admin", "Duyệt v4");
                store.publishDocument(uploadedDocId[0], 102, "reviewer_admin", "Xuất bản v4");
                store.reindexDocumentOffline(uploadedDocId[0], 102, "reviewer_admin");

                ManagedDocument unpublished = store.unpublishDocument(uploadedDocId[0], 102, "reviewer_admin", "Tạm gỡ xuất bản");
                check("UNPUBLISHED".equals(unpublished.workflowStatus) && "draft".equals(unpublished.status), "Expected UNPUBLISHED");
                PlaygroundResult pgUnpub = store.runOfflinePlayground("quy định bảo hành thiết bị modem wifi 6", 101, "admin_fpt");
                check(pgUnpub.selectedChunks.isEmpty() && pgUnpub.abstained, "Unpublished document must not be retrievable");
            });

            // 9. FAQ LIFECYCLE & RAG SOURCE INTEGRATION
            test("FAQ lifecycle syncs published FAQ to RAG with source_type=FAQ and excludes unpublished/archived FAQs", () -> {
                FaqItem faq = store.createFaq(
                        "Thủ tục tạm ngưng hợp đồng Internet FPT khi đi công tác xa?",
                        "Khách hàng mang giấy tờ tùy thân đến quầy giao dịch FPT Telecom để làm thủ tục tạm ngưng hợp đồng có thời hạn.",
                        "faq", "tamngung,hopdong", 5, "official", "contract.suspend", null, null, 101, "admin_fpt");
                check(faq.id > 0 && "DRAFT".equals(faq.workflowStatus) && faq.linkedDocumentId == null, "New FAQ must be DRAFT without RAG doc");

                store.transitionFaq(faq.id, "SUBMIT_REVIEW", null, 101, "admin_gialai");
                store.transitionFaq(faq.id, "APPROVE", null, 102, "reviewer_admin");
                FaqItem pubFaq = store.transitionFaq(faq.id, "PUBLISH", null, 102, "reviewer_admin");
                check("PUBLISHED".equals(pubFaq.workflowStatus) && pubFaq.linkedDocumentId != null,
                        "Published FAQ must link to knowledge_documents");

                ManagedDocument linkedDoc = store.findDocument(pubFaq.linkedDocumentId.longValue());
                check("FAQ".equals(linkedDoc.sourceType) && ("faq:" + faq.id).equals(linkedDoc.source) && "PUBLISHED".equals(linkedDoc.workflowStatus),
                        "Linked RAG document must have source_type=FAQ and source=faq:<id>");

                // Run offline indexing batch and verify FAQ is retrievable in Playground
                int jobsRun = store.runOfflineIndexingBatch(10, 102, "reviewer_admin");
                check(jobsRun >= 1, "Expected FAQ indexing job to run");

                PlaygroundResult pgFaq = store.runOfflinePlayground("thủ tục tạm ngưng hợp đồng internet khi đi công tác", 101, "admin_gialai");
                check(!pgFaq.abstained && !pgFaq.selectedChunks.isEmpty(), "Published FAQ must be retrieved in Playground");
                check(pgFaq.deterministicMockAnswer.contains("tạm ngưng hợp đồng có thời hạn"), "FAQ answer not rendered");

                // Editing published FAQ increments version and marks linked RAG document as NEEDS_REINDEX
                FaqItem editedFaq = store.updateFaq(
                        faq.id,
                        "Thủ tục tạm ngưng hợp đồng Internet FPT khi đi công tác xa?",
                        "Khách hàng mang giấy tờ tùy thân đến quầy giao dịch FPT hoặc liên hệ tổng đài hỗ trợ để đăng ký tạm ngưng.",
                        "faq", "tamngung,hopdong", 5, "official", "contract.suspend", null, null,
                        "Bổ sung kênh liên hệ tổng đài", 101, "admin_gialai");
                check(editedFaq.versionNumber == 2, "Expected FAQ v2");
                ManagedDocument linkedAfterEdit = store.findDocument(editedFaq.linkedDocumentId.longValue());
                check(linkedAfterEdit.isNeedsReindex(), "Edited published FAQ must mark RAG document as NEEDS_REINDEX");

                // Unpublishing FAQ immediately removes it from RAG retrieval
                store.transitionFaq(faq.id, "UNPUBLISH", null, 102, "reviewer_admin");
                PlaygroundResult pgFaqUnpub = store.runOfflinePlayground("thủ tục tạm ngưng hợp đồng internet khi đi công tác", 101, "admin_gialai");
                check(pgFaqUnpub.selectedChunks.isEmpty() && pgFaqUnpub.abstained, "Unpublished FAQ must not be retrieved");
            });

            // 10. RAG CONFIGURATION HARD-LOCK ON PRODUCTION_RAG_ENABLED + AUDIT LOG VERIFICATION
            test("saveRagSettings blocks enabling production_rag_enabled and records audit log", () -> {
                try {
                    store.saveRagSettings(true, true, true, 3, 0.72, 900, 1000, 100, 101, "admin_gialai");
                    throw new AssertionError("Server must reject productionRagEnabled=true");
                } catch (IllegalArgumentException expected) {
                    check(expected.getMessage().contains("production_rag_enabled"), "Expected clear block message");
                }

                RagAdminSettings saved = store.saveRagSettings(true, true, false, 4, 0.75, 1000, 900, 90, 101, "admin_gialai");
                check(!saved.productionRagEnabled && saved.stagingRagEnabled && saved.topK == 4 && saved.chunkSize == 900,
                        "Valid staging RAG config must save while productionRagEnabled stays false");

                List<AuditLogEntry> logs = store.listAuditLogs(null, null, 1, 50);
                check(logs.size() >= 8, "Expected comprehensive audit log trail");
                boolean foundBlockLog = false;
                for (AuditLogEntry log : logs) {
                    check(!log.previousState.contains("api_key") && !log.newState.contains("embedding"),
                            "Audit log must never contain secrets or embeddings");
                    if ("BLOCKED_PRODUCTION_RAG_ENABLE".equals(log.action)) {
                        foundBlockLog = true;
                    }
                }
                check(foundBlockLog, "Expected BLOCKED_PRODUCTION_RAG_ENABLE audit record");
            });

            System.out.println("RagPhase3PostgresTest: " + passed + " tests passed (isolated PostgreSQL 17 + pgvector 0.8.2; 0 Gemini API calls)");
        } finally {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(tempUploadDir)) {
                for (Path p : ds) Files.deleteIfExists(p);
            } catch (Exception ignored) {}
            Files.deleteIfExists(tempUploadDir);
        }
    }
}
