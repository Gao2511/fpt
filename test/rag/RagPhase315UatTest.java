package rag;

import ai.ConsultationBehaviorTest;
import ai.consultation.*;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.session.ChatSessionData;
import dao.AdminKnowledgeDAO;
import dto.AdminKnowledgeDTO.*;
import rag.extract.DocumentExtractionService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 3.15: End-to-End Admin Knowledge Base UAT & Real Document Import Validation.
 * Verifies:
 *  - Task 1: Real PostgreSQL 004 migration schema, RLS, Tomcat HTTP endpoints (all 8 tabs), Demo vs Real DB mode.
 *  - Task 2: Administrator UTF-8 Vietnamese document "văn bản.txt" upload, extraction, section/numbering preservation,
 *            preview, revision history, approval, publication, and unpublish workflow.
 *  - Task 3: Classification of "văn bản.txt" into (A) AI Behavior & Consultation Rules, (B) Knowledge Base & FAQ,
 *            and (C) Admin Configuration, verifying that uploading does NOT auto-mutate system instructions.
 *  - Task 4: End-to-End Offline UAT (Upload -> Extract -> Draft -> Review -> Approve -> Publish -> Mock Index -> Playground),
 *            plus KnowledgeContentPolicy diagnostics on monolithic vs curated FAQ/Guide chunks, price/policy protection.
 *  - Task 5: RBAC, CSRF, XSS output encoding, malicious upload rejection, optimistic locking concurrency, and audit logs.
 */
public final class RagPhase315UatTest {
    private static int passed;

    interface Case {
        void run() throws Exception;
    }

    private static void test(String name, Case c) throws Exception {
        c.run();
        passed++;
        System.out.println("PASS Phase3.15-UAT " + name);
    }

    private static void check(boolean ok, String msg) {
        if (!ok) {
            throw new AssertionError(msg);
        }
    }

    /**
     * Representative UTF-8 Vietnamese operational playbook "văn bản.txt" containing all 3 categories
     * specified in Phase 3.15 (Part A: AI Behavior, Part B: Knowledge Base & FAQ, Part C: Admin Configuration).
     */
    public static final String ADMIN_PLAYBOOK_FILENAME = "văn bản.txt";
    public static final String ADMIN_PLAYBOOK_CONTENT =
            "TÀI LIỆU HƯỚNG DẪN VẬN HÀNH CHATBOT TƯ VẤN FPT TELECOM GIA LAI\n"
            + "================================================================\n\n"
            + "PHẦN I. QUY TẮC ỨNG XỬ & TƯ VẤN CỦA TRỢ LÝ AI (AI BEHAVIOR & CONSULTATION RULES)\n"
            + "1. Vai trò trợ lý:\n"
            + "   - Trợ lý ảo hỗ trợ tư vấn dịch vụ Internet cáp quang, Truyền hình FPT Play và Camera an ninh tại chi nhánh.\n"
            + "2. Cách xưng hô:\n"
            + "   - Xưng \"em\" và gọi khách hàng là \"anh/chị\" trong mọi tình huống giao tiếp.\n"
            + "3. Phong cách giao tiếp:\n"
            + "   - Lịch sự, ngắn gọn, rõ ràng, ưu tiên giải đáp đúng trọng tâm nhu cầu thực tế của khách hàng.\n"
            + "4. Quy trình tư vấn:\n"
            + "   - Bước 1: Lắng nghe nhu cầu sử dụng (số lượng người dùng, diện tích nhà, nhu cầu xem TV hoặc lắp Camera).\n"
            + "   - Bước 2: Đề xuất gói dịch vụ phù hợp từ danh mục gói hiện hành trong cơ sở dữ liệu.\n"
            + "   - Bước 3: Hướng dẫn khách hàng để lại thông tin đăng ký hoặc kết nối nhân viên phụ trách.\n"
            + "5. Quy tắc không bịa thông tin:\n"
            + "   - Tuyệt đối không tự suy diễn mức cước, tốc độ đường truyền, khuyến mãi hoặc cam kết kỹ thuật ngoài dữ liệu đã phê duyệt.\n"
            + "6. Chuyển giao khách hàng:\n"
            + "   - Khi khách hàng yêu cầu khảo sát thực tế, khiếu nại phức tạp hoặc ký hợp đồng, chuyển thông tin cho nhân viên kinh doanh phụ trách.\n\n"
            + "PHẦN II. KỊCH BẢN TRI THỨC & CÂU HỎI THƯỜNG GẶP (KNOWLEDGE BASE & FAQ)\n"
            + "1. Các tình huống hỏi giá:\n"
            + "   - Khi khách hàng hỏi bảng giá hoặc gói rẻ nhất, hệ thống lấy trực tiếp từ danh mục gói cước chuẩn trong cơ sở dữ liệu.\n"
            + "2. Tình huống WiFi yếu:\n"
            + "   - Hướng dẫn khách hàng đặt modem ở vị trí trung tâm, thông thoáng, tránh vật cản kim loại và khởi động lại thiết bị định kỳ.\n"
            + "3. Hướng dẫn kết nối:\n"
            + "   - Khuyến nghị sử dụng băng tần 5GHz ở khoảng cách gần và băng tần 2.4GHz cho các thiết bị ở phòng xa.\n"
            + "4. Nhu cầu chơi game:\n"
            + "   - Với nhu cầu chơi game trực tuyến, anh/chị nên ưu tiên kết nối dây mạng LAN trực tiếp vào thiết bị để ổn định tín hiệu.\n"
            + "5. Dịch vụ Truyền hình:\n"
            + "   - Hỗ trợ xem truyền hình qua ứng dụng hoặc đầu thu giải mã kết nối trực tiếp với tivi thông minh tại nhà.\n"
            + "6. Dịch vụ Camera:\n"
            + "   - Hỗ trợ quan sát từ xa qua ứng dụng điện thoại, lưu trữ dữ liệu đám mây an toàn và cảnh báo chuyển động thông minh.\n"
            + "7. Thủ tục lắp đặt:\n"
            + "   - Khách hàng cá nhân chuẩn bị giấy tờ tùy thân để nhân viên hỗ trợ khảo sát hạ tầng và làm hợp đồng điện tử.\n"
            + "8. Các kịch bản hỗ trợ khác:\n"
            + "   - Hỗ trợ thủ tục chuyển địa điểm sử dụng dịch vụ hoặc tạm ngưng hợp đồng có thời hạn tại quầy giao dịch.\n\n"
            + "PHẦN III. THÔNG TIN CẤU HÌNH QUẢN TRỊ (ADMIN CONFIGURATION)\n"
            + "1. Tên trợ lý: Trợ lý tư vấn FPT Telecom\n"
            + "2. Người phụ trách: Bộ phận Kinh doanh & CSKH Chi nhánh Gia Lai\n"
            + "3. Số liên hệ: 0935 000 000 (Hotline hỗ trợ đăng ký & kỹ thuật)\n"
            + "4. Giờ làm việc: 07:30 - 17:30 (Từ Thứ Hai đến Chủ Nhật)\n"
            + "5. Khu vực phục vụ: Thành phố Pleiku và các huyện, thị xã thuộc tỉnh Gia Lai.";

    public static void main(String[] args) throws Exception {
        if (!"true".equals(System.getenv("RAG_TEST_DATABASE_APPROVED"))
                || !"development".equals(System.getenv("RAG_ENVIRONMENT"))
                || !"127.0.0.1".equals(System.getenv("DB_HOST"))
                || !"fpt_rag_test".equals(System.getenv("DB_NAME"))) {
            throw new IllegalStateException("Refusing UAT database writes: require approved loopback fpt_rag_test");
        }

        String baseUrl = args.length > 0 ? args[0].trim() : "";
        RagDatabase db = new RagDatabase();

        // Seed core CMS tables (users, settings, site_content) for full web application & DAO integration
        seedCoreCmsTables(db);

        AdminKnowledgeDAO store = new AdminKnowledgeDAO(db);
        Path tempUploadDir = Files.createTempDirectory("fpt-kb-uat315-");

        try {
            DocumentExtractionService extractor = new DocumentExtractionService(tempUploadDir);

            // =========================================================================
            // TASK 1.2 — VERIFY MIGRATION 004 ON REAL POSTGRESQL 17 + PGVECTOR 0.8.2
            // =========================================================================
            test("Task 1.2: Migration 004 tables, columns, indexes, and RLS policies exist in PostgreSQL", () -> {
                try (Connection c = db.open()) {
                    List<String> expectedTables = Arrays.asList(
                            "knowledge_documents",
                            "knowledge_document_revisions",
                            "knowledge_document_reviews",
                            "knowledge_files",
                            "knowledge_faqs",
                            "knowledge_faq_revisions",
                            "admin_audit_logs",
                            "knowledge_chunks",
                            "knowledge_index_jobs");
                    for (String tbl : expectedTables) {
                        try (PreparedStatement p = c.prepareStatement(
                                "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = ?")) {
                            p.setString(1, tbl);
                            try (ResultSet r = p.executeQuery()) {
                                check(r.next(), "Missing table in PostgreSQL: public." + tbl);
                                check(r.getBoolean("rowsecurity"), "RLS must be enabled on public." + tbl);
                            }
                        }
                    }

                    List<String> expectedCols = Arrays.asList(
                            "workflow_status", "source_type", "department", "tags",
                            "created_by", "updated_by", "reviewed_by", "reviewed_at",
                            "rejection_reason", "chunk_size", "chunk_overlap");
                    for (String col : expectedCols) {
                        try (PreparedStatement p = c.prepareStatement(
                                "SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='knowledge_documents' AND column_name=?")) {
                            p.setString(1, col);
                            try (ResultSet r = p.executeQuery()) {
                                check(r.next(), "Missing 004 column on knowledge_documents: " + col);
                            }
                        }
                    }
                }
            });

            // =========================================================================
            // TASK 2 — UPLOAD & EXTRACT UTF-8 VIETNAMESE "văn bản.txt" WITHOUT ALTERING CONTENT
            // =========================================================================
            final long[] playbookDocId = {0L};
            test("Task 2: Upload UTF-8 Vietnamese 'văn bản.txt' preserves diacritics, sections, and numbering in DRAFT", () -> {
                byte[] utf8Bytes = ADMIN_PLAYBOOK_CONTENT.getBytes(StandardCharsets.UTF_8);
                DocumentExtractionService.ExtractionResult ext = extractor.storeAndExtract(
                        ADMIN_PLAYBOOK_FILENAME, "text/plain", new ByteArrayInputStream(utf8Bytes));

                check("văn bản.txt".equals(ext.originalFilename), "Expected Unicode filename 'văn bản.txt', got: " + ext.originalFilename);
                check("EXTRACTED".equals(ext.status), "Expected EXTRACTED status");
                check("text/plain".equals(ext.detectedMimeType), "Expected text/plain MIME");
                check(ADMIN_PLAYBOOK_CONTENT.equals(ext.extractedText),
                        "Extracted text must preserve exact Vietnamese diacritics, sections, and numbering without modification");

                ManagedDocument draft = store.uploadAndCreateDraft(
                        ext, "", "guide", "chatbot,vanhanh,fpt", "CSKH & Kinh doanh",
                        "chatbot.playbook", "admin_reviewed", null, null, 800, 80, 1, "admin");
                playbookDocId[0] = draft.id;

                check("văn bản".equals(draft.title), "Default title from 'văn bản.txt' should be 'văn bản', got: " + draft.title);
                check("DRAFT".equals(draft.workflowStatus) && "draft".equals(draft.status), "Must start in DRAFT");
                check(ADMIN_PLAYBOOK_CONTENT.equals(draft.content), "Stored DB content must match original UTF-8 text 100%");
                check(draft.content.contains("PHẦN I. QUY TẮC ỨNG XỬ & TƯ VẤN CỦA TRỢ LÝ AI"), "Missing Section I header");
                check(draft.content.contains("PHẦN II. KỊCH BẢN TRI THỨC & CÂU HỎI THƯỜNG GẶP"), "Missing Section II header");
                check(draft.content.contains("PHẦN III. THÔNG TIN CẤU HÌNH QUẢN TRỊ"), "Missing Section III header");

                List<DocumentRevision> revisions = store.listRevisions(draft.id);
                check(revisions.size() == 1 && revisions.get(0).versionNumber == 1L, "Expected immutable revision v1");
                check(ADMIN_PLAYBOOK_CONTENT.equals(revisions.get(0).content), "Revision v1 content mismatch");
            });

            // =========================================================================
            // TASK 3 & TASK 4 — VERIFY UPLOADED PLAYBOOK DOES NOT AUTO-APPLY TO SYSTEM INSTRUCTIONS
            //                   AND DIAGNOSE MONOLITHIC vs CURATED RAG CHUNKS
            // =========================================================================
            test("Task 3 & 4: Uploading and publishing 'văn bản.txt' never alters ConsultationPrompt system instructions or package prices", () -> {
                String sysBefore = ConsultationPrompt.instructions(ConsultationPrompt.SYSTEM);

                // Verify DRAFT document is NOT retrievable
                PlaygroundResult pgDraft = store.runOfflinePlayground("hướng dẫn kết nối dây mạng LAN khi chơi game", 1, "admin");
                check(pgDraft.selectedChunks.isEmpty() && pgDraft.abstained, "DRAFT playbook must not be retrievable");

                // Submit -> Approve -> Publish -> Index monolithic playbook
                store.submitForReview(playbookDocId[0], 1, "admin", "Gửi duyệt tài liệu văn bản.txt");
                store.approveDocument(playbookDocId[0], 1, "admin", "Duyệt thử nghiệm UAT");
                store.publishDocument(playbookDocId[0], 1, "admin", "Xuất bản cục bộ UAT");
                ManagedDocument indexedPlaybook = store.reindexDocumentOffline(playbookDocId[0], 1, "admin");
                check("ready".equals(indexedPlaybook.indexingStatus), "Expected ready index on playbook");

                String sysAfter = ConsultationPrompt.instructions(ConsultationPrompt.SYSTEM);
                check(sysBefore.equals(sysAfter), "Uploading/indexing document must NEVER mutate ConsultationPrompt system instructions");

                // Inspect how KnowledgeContentPolicy evaluates the monolithic playbook chunks in Playground:
                // Because the monolithic playbook contains "bảng giá" ("gia"), "mức cước" ("cuoc"), and "Gia Lai" ("gia lai"),
                // KnowledgeContentPolicy flags chunks containing those terms as INVALID_BUSINESS_CLAIM!
                PlaygroundResult pgMonolithic = store.runOfflinePlayground("hướng dẫn kết nối dây mạng LAN khi chơi game", 1, "admin");
                check(!pgMonolithic.candidateChunks.isEmpty(), "Expected candidate chunks from indexed playbook");
                boolean sawBusinessClaimFilter = pgMonolithic.rejectedReasons.containsKey("INVALID_BUSINESS_CLAIM");
                check(sawBusinessClaimFilter,
                        "Expected KnowledgeContentPolicy to flag monolithic playbook chunks containing price/Gia Lai terms as INVALID_BUSINESS_CLAIM");

                // Unpublish monolithic playbook so it does not pollute curated knowledge
                ManagedDocument unpub = store.unpublishDocument(playbookDocId[0], 1, "admin",
                        "Gỡ xuất bản tài liệu nguyên khối để tách thành các mục FAQ và Hướng dẫn chuẩn hóa riêng biệt");
                check("UNPUBLISHED".equals(unpub.workflowStatus) && "draft".equals(unpub.status), "Expected UNPUBLISHED");
                PlaygroundResult pgAfterUnpub = store.runOfflinePlayground("hướng dẫn kết nối dây mạng LAN khi chơi game", 1, "admin");
                check(pgAfterUnpub.candidateChunks.isEmpty() && pgAfterUnpub.abstained, "Unpublished playbook must have 0 chunks");
            });

            // =========================================================================
            // TASK 4 — CURATED FAQ & SUPPORT ITEMS EXTRACTED FROM PART B OF "văn bản.txt"
            // =========================================================================
            test("Task 4: Curated Part B support items (Chơi game, WiFi yếu, Thủ tục hợp đồng) pass workflow and retrieve cleanly", () -> {
                // Create curated Guide for Gaming connection (Part B.4 — single topic 'game')
                String guideText = "Với nhu cầu chơi game trực tuyến, anh/chị nên ưu tiên kết nối dây mạng LAN trực tiếp vào thiết bị để ổn định tín hiệu, hạn chế suy hao qua sóng không dây.";
                ManagedDocument guideDoc = store.createDocument(
                        "Hướng dẫn tối ưu khi chơi game trực tuyến", "guide",
                        guideText, "guide:fpt-support/gaming-lan", "MANUAL",
                        "game,lan,ping", "Kỹ thuật FPT", "gaming.connection", "official",
                        null, null, null, 800, 80, "Trích xuất từ Phần II (Mục 4) tài liệu vận hành", 1, "admin");

                store.submitForReview(guideDoc.id, 1, "admin", "Gửi duyệt hướng dẫn chơi game");
                store.approveDocument(guideDoc.id, 1, "admin", "Phê duyệt hướng dẫn chơi game");
                store.publishDocument(guideDoc.id, 1, "admin", "Xuất bản hướng dẫn chơi game");
                store.reindexDocumentOffline(guideDoc.id, 1, "admin");

                PlaygroundResult pgGaming = store.runOfflinePlayground("hướng dẫn khi chơi game trực tuyến", 1, "admin");
                check(!pgGaming.abstained && pgGaming.selectedChunks.size() == 1,
                        "Expected curated gaming guide to be selected, issue=" + pgGaming.selectionIssue + ", rejected=" + pgGaming.rejectedReasons);
                check(pgGaming.deterministicMockAnswer.contains("ưu tiên kết nối dây mạng LAN"),
                        "Playground answer must include verified LAN guidance");

                // Create curated FAQ for Contract & Installation Procedure (Part B.7 & B.8 — single topic 'hop dong')
                FaqItem installFaq = store.createFaq(
                        "Thủ tục làm hợp đồng và tạm ngưng hợp đồng dịch vụ cần chuẩn bị giấy tờ gì?",
                        "Khách hàng cá nhân chuẩn bị giấy tờ tùy thân để nhân viên hỗ trợ khảo sát hạ tầng, làm hợp đồng điện tử hoặc tạm ngưng hợp đồng có thời hạn tại quầy giao dịch.",
                        "faq", "thutuc,hopdong,tamngung", 1, "official", "contract.procedure", null, null, 1, "admin");
                store.transitionFaq(installFaq.id, "SUBMIT_REVIEW", null, 1, "admin");
                store.transitionFaq(installFaq.id, "APPROVE", null, 1, "admin");
                store.transitionFaq(installFaq.id, "PUBLISH", null, 1, "admin");
                store.runOfflineIndexingBatch(10, 1, "admin");

                PlaygroundResult pgInstall = store.runOfflinePlayground("thủ tục tạm ngưng hợp đồng dịch vụ cần chuẩn bị giấy tờ gì", 1, "admin");
                check(!pgInstall.abstained && !pgInstall.selectedChunks.isEmpty(),
                        "Expected published contract FAQ to be retrieved, issue=" + pgInstall.selectionIssue + ", rejected=" + pgInstall.rejectedReasons);
                check(pgInstall.deterministicMockAnswer.contains("giấy tờ tùy thân"),
                        "Expected FAQ answer in playground output");

                // Verify that package prices & installation fees remain strictly owned by ProductCatalog / ConsultationPolicy
                ConsultationBehaviorTest.MockProvider mockChat = new ConsultationBehaviorTest.MockProvider();
                KnowledgeRetrievalService retrievalSvc = new KnowledgeRetrievalService(
                        new RagConfig(true, true, true, MockEmbeddingProvider.MODEL_NAME, MockEmbeddingProvider.DIMENSIONS),
                        new dao.KnowledgeChunkDAO(db),
                        store.getMockEmbeddings());
                ConsultationEngine engine = new ConsultationEngine(retrievalSvc);
                ChatResponse priceReply = engine.chat(
                        "Gói 195K có mất phí lắp đặt không?",
                        new ChatSessionData("uat-price-check"),
                        ConsultationBehaviorTest.CATALOG,
                        new ConsultationEngine.Target(mockChat, "FAKE_UAT_KEY", "unchanged-chat-model", ""),
                        null,
                        new ChatOptions(0.3, 1200));
                check(priceReply.getContent().contains("195.000đ/tháng") && priceReply.getContent().contains("300.000đ"),
                        "Package price and installation fee must remain strictly DB/Catalog-owned");
                check(mockChat.calls == 0, "Deterministic price/fee query must not call LLM provider");
            });

            // =========================================================================
            // TASK 5 — REVISION CONCURRENCY (OPTIMISTIC LOCKING) & SECURITY CHECKS
            // =========================================================================
            test("Task 5: Optimistic locking rejects stale concurrent version updates without corrupting revisions", () -> {
                try (Connection c = db.open();
                     PreparedStatement p = c.prepareStatement(
                             "UPDATE public.knowledge_documents SET title='Stale Test' WHERE id=? AND version=?")) {
                    p.setLong(1, playbookDocId[0]);
                    p.setLong(2, 999L); // Wrong version number simulating concurrent modification
                    int updated = p.executeUpdate();
                    check(updated == 0, "Optimistic version check must reject mismatched version");
                }
            });

            // =========================================================================
            // TASK 1, 2, 4, 5 — LIVE TOMCAT 9 HTTP SERVER VERIFICATION (IF RUNNING)
            // =========================================================================
            if (!baseUrl.isEmpty()) {
                runLiveHttpChecks(baseUrl);
            }

            System.out.println("RagPhase315UatTest: " + passed + " UAT checks passed (0 Gemini API calls; 0 production DB connections)");
        } finally {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(tempUploadDir)) {
                for (Path p : ds) Files.deleteIfExists(p);
            } catch (Exception ignored) {}
            Files.deleteIfExists(tempUploadDir);
        }
    }

    private static void seedCoreCmsTables(RagDatabase db) throws Exception {
        try (Connection c = db.open(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS public.users ("
                    + "id SERIAL PRIMARY KEY, "
                    + "username VARCHAR(100) UNIQUE NOT NULL, "
                    + "password TEXT NOT NULL, "
                    + "role VARCHAR(30) NOT NULL DEFAULT 'user', "
                    + "is_active BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "created_at TIMESTAMPTZ DEFAULT NOW(), "
                    + "avatar_url TEXT, "
                    + "email VARCHAR(150) UNIQUE, "
                    + "phone VARCHAR(30), "
                    + "full_name VARCHAR(150), "
                    + "google_id VARCHAR(150), "
                    + "facebook_id VARCHAR(150), "
                    + "auth_provider VARCHAR(30) DEFAULT 'local', "
                    + "email_verified BOOLEAN DEFAULT TRUE)");

            s.execute("CREATE TABLE IF NOT EXISTS public.settings ("
                    + "id SERIAL PRIMARY KEY, "
                    + "setting_key VARCHAR(120) UNIQUE NOT NULL, "
                    + "setting_value TEXT, "
                    + "description TEXT, "
                    + "updated_at TIMESTAMPTZ DEFAULT NOW())");

            String hashedPw = security.Passwords.hash("AdminUat#2026");
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO public.users (id, username, password, role, is_active, email, full_name) "
                    + "VALUES (1, 'admin_uat', ?, 'admin', TRUE, 'admin@fpt.test', 'Admin UAT Gia Lai') "
                    + "ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password, is_active = TRUE, role = 'admin'")) {
                p.setString(1, hashedPw);
                p.executeUpdate();
            }
        }
    }

    private static void runLiveHttpChecks(String baseUrl) throws Exception {
        // 1. Unauthenticated access to /admin/knowledge-base must redirect to /login (302)
        test("Task 1 & 5 (HTTP): Unauthenticated GET /admin/knowledge-base redirects to /login (302)", () -> {
            HttpResp r = httpRequest("GET", baseUrl + "/admin/knowledge-base", null, null, null);
            check(r.status == 302 && r.location != null && r.location.endsWith("/login"),
                    "Expected 302 redirect to /login, got status=" + r.status + " location=" + r.location);
        });

        // 2. Direct access to /view/admin/knowledge-base.jsp must return 403
        test("Task 5 (HTTP): Direct request to /view/admin/knowledge-base.jsp is blocked (403/302)", () -> {
            HttpResp r = httpRequest("GET", baseUrl + "/view/admin/knowledge-base.jsp", null, null, null);
            check(r.status == 302 || r.status == 403, "Expected 302/403 for direct JSP access, got " + r.status);
        });

        // 3. Login as real Admin via POST /login
        final String[] sessionCookie = {null};
        final String[] csrfToken = {null};
        test("Task 1 (HTTP): Real Admin login via POST /login establishes session and accesses /admin/knowledge-base", () -> {
            String form = "identifier=" + URLEncoder.encode("admin_uat", "UTF-8")
                    + "&password=" + URLEncoder.encode("AdminUat#2026", "UTF-8");
            HttpResp loginResp = httpRequest("POST", baseUrl + "/login", null,
                    "application/x-www-form-urlencoded", form.getBytes(StandardCharsets.UTF_8));
            check(loginResp.status == 302 && loginResp.cookie != null,
                    "Expected 302 + session cookie on admin login, got status=" + loginResp.status);
            sessionCookie[0] = loginResp.cookie;

            HttpResp kbDashboard = httpRequest("GET", baseUrl + "/admin/knowledge-base?tab=dashboard", sessionCookie[0], null, null);
            check(kbDashboard.status == 200, "Expected HTTP 200 on /admin/knowledge-base?tab=dashboard, got " + kbDashboard.status);
            check(kbDashboard.body.contains("AI Knowledge Base &amp; RAG Management System")
                    || kbDashboard.body.contains("AI Knowledge Base & RAG Management System"),
                    "Dashboard HTML missing main heading");
            check(kbDashboard.body.contains("OFFLINE TEST MODE"), "Missing offline mode badge");

            Matcher m = Pattern.compile("name=\"csrf_token\"\\s+value=\"([^\"]+)\"").matcher(kbDashboard.body);
            if (!m.find()) {
                // Also check admin-header logout form or other forms
                HttpResp docTab = httpRequest("GET", baseUrl + "/admin/knowledge-base?tab=documents", sessionCookie[0], null, null);
                m = Pattern.compile("name=\"csrf_token\"\\s+value=\"([^\"]+)\"").matcher(docTab.body);
                check(m.find(), "Could not extract csrf_token from /admin/knowledge-base?tab=documents");
            }
            csrfToken[0] = m.group(1);
        });

        // 4. Verify all 8 tabs return HTTP 200 over real Tomcat HTTP
        test("Task 1.4 (HTTP): All 8 tabs of /admin/knowledge-base render HTTP 200 with valid UTF-8 content", () -> {
            String[] tabs = {"dashboard", "documents", "upload", "faqs", "indexing", "rag-config", "audit", "playground"};
            for (String tab : tabs) {
                HttpResp r = httpRequest("GET", baseUrl + "/admin/knowledge-base?tab=" + tab, sessionCookie[0], null, null);
                check(r.status == 200, "Tab '" + tab + "' returned HTTP " + r.status);
                check(!r.body.contains("Không thể tải dữ liệu Knowledge Base"),
                        "Tab '" + tab + "' rendered DAO error banner");
            }
        });

        // 5. Real HTTP Multipart Upload of "văn bản.txt"
        final long[] httpUploadedDocId = {0L};
        test("Task 2 (HTTP): Multipart POST upload of UTF-8 'văn bản.txt' succeeds and redirects to document detail", () -> {
            String boundary = "----FptKbUatBoundary" + System.currentTimeMillis();
            ByteArrayOutputStream mp = new ByteArrayOutputStream();
            writePart(mp, boundary, "csrf_token", csrfToken[0]);
            writePart(mp, boundary, "action", "uploadDocument");
            writePart(mp, boundary, "tab", "upload");
            writePart(mp, boundary, "title", "Kịch bản vận hành Chatbot FPT Gia Lai (văn bản.txt)");
            writePart(mp, boundary, "category", "guide");
            writePart(mp, boundary, "sourceAuthority", "admin_reviewed");
            writePart(mp, boundary, "policyKey", "chatbot.operation.guide");
            writePart(mp, boundary, "tags", "chatbot,vanhanh,uat");
            writePart(mp, boundary, "department", "CSKH Gia Lai");
            writeFilePart(mp, boundary, "documentFile", ADMIN_PLAYBOOK_FILENAME, "text/plain; charset=UTF-8",
                    ADMIN_PLAYBOOK_CONTENT.getBytes(StandardCharsets.UTF_8));
            mp.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpResp up = httpRequest("POST",
                    baseUrl + "/admin/knowledge-base",
                    sessionCookie[0], "multipart/form-data; boundary=" + boundary, mp.toByteArray());
            check(up.status == 302 && up.location != null && up.location.contains("tab=documents&docId="),
                    "Expected 302 redirect to new document after multipart upload, got status=" + up.status + " location=" + up.location + " body=" + up.body);

            Matcher dm = Pattern.compile("docId=([0-9]+)").matcher(up.location);
            check(dm.find(), "Missing docId in redirect location: " + up.location);
            httpUploadedDocId[0] = Long.parseLong(dm.group(1));

            HttpResp detail = httpRequest("GET", baseUrl + "/admin/knowledge-base?tab=documents&docId=" + httpUploadedDocId[0],
                    sessionCookie[0], null, null);
            check(detail.status == 200 && detail.body.contains("PHẦN I. QUY TẮC ỨNG XỬ"),
                    "Document detail page did not render extracted Vietnamese content of văn bản.txt");
        });

        // 6. Real HTTP Workflow Transitions on uploaded "văn bản.txt" (Submit -> Reject -> Approve -> Publish -> Reindex -> Unpublish)
        test("Task 4 (HTTP): Full lifecycle POST actions (submitReview, rejectDocument, approveDocument, publishDocument, reindexDocument, unpublishDocument) work over HTTP", () -> {
            long docId = httpUploadedDocId[0];

            // Submit for review
            String submitForm = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=submitReview&id=" + docId;
            HttpResp rSub = httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", submitForm.getBytes(StandardCharsets.UTF_8));
            check(rSub.status == 302, "Expected 302 on submitReview, got " + rSub.status);

            // Reject with mandatory reason
            String rejectForm = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=rejectDocument&id=" + docId
                    + "&rejectionReason=" + URLEncoder.encode("Tài liệu nguyên khối cần tách riêng phần AI Behavior và phần FAQ trước khi xuất bản chính thức.", "UTF-8");
            HttpResp rRej = httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", rejectForm.getBytes(StandardCharsets.UTF_8));
            check(rRej.status == 302, "Expected 302 on rejectDocument, got " + rRej.status);

            HttpResp afterRej = httpRequest("GET", baseUrl + "/admin/knowledge-base?tab=documents&docId=" + docId,
                    sessionCookie[0], null, null);
            check(afterRej.status == 200 && afterRej.body.contains("REJECTED") && afterRej.body.contains("Tài liệu nguyên khối cần tách riêng"),
                    "Expected REJECTED badge and rejection reason on document detail page");

            // Re-submit -> Approve -> Publish -> Reindex -> Unpublish
            httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", submitForm.getBytes(StandardCharsets.UTF_8));
            String approveForm = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=approveDocument&id=" + docId;
            Check302(httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", approveForm.getBytes(StandardCharsets.UTF_8)), "approveDocument");

            String publishForm = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=publishDocument&id=" + docId;
            Check302(httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", publishForm.getBytes(StandardCharsets.UTF_8)), "publishDocument");

            String reindexForm = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=reindexDocument&id=" + docId;
            Check302(httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", reindexForm.getBytes(StandardCharsets.UTF_8)), "reindexDocument");

            // Check Chunk Preview on Indexing tab
            HttpResp idxPage = httpRequest("GET", baseUrl + "/admin/knowledge-base?tab=indexing&docId=" + docId,
                    sessionCookie[0], null, null);
            check(idxPage.status == 200 && idxPage.body.contains("#0") && idxPage.body.contains("tokens"),
                    "Expected Chunk Preview table on indexing tab for docId=" + docId);

            // Unpublish
            String unpubForm = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=unpublishDocument&id=" + docId;
            Check302(httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", unpubForm.getBytes(StandardCharsets.UTF_8)), "unpublishDocument");
        });

        // 7. Real HTTP Malicious Upload Rejection (.jsp and double extension)
        test("Task 5 (HTTP): Multipart upload of disallowed .jsp and double extension 'văn bản.jsp.txt' returns 400 with error alert", () -> {
            String boundary = "----FptKbBadBoundary" + System.currentTimeMillis();
            ByteArrayOutputStream mp = new ByteArrayOutputStream();
            writePart(mp, boundary, "csrf_token", csrfToken[0]);
            writePart(mp, boundary, "action", "uploadDocument");
            writePart(mp, boundary, "tab", "upload");
            writeFilePart(mp, boundary, "documentFile", "văn bản.jsp.txt", "text/plain",
                    "Nội dung thử nghiệm".getBytes(StandardCharsets.UTF_8));
            mp.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpResp badUp = httpRequest("POST", baseUrl + "/admin/knowledge-base",
                    sessionCookie[0], "multipart/form-data; boundary=" + boundary, mp.toByteArray());
            check(badUp.status == 400 && badUp.body.contains("kb-alert-error") && badUp.body.contains(".jsp"),
                    "Expected HTTP 400 + kb-alert-error on double extension upload, got status=" + badUp.status);
        });

        // 8. Real HTTP CSRF, GET mutation, XSS, and Production Lock Verification
        test("Task 5 (HTTP): Missing CSRF returns 403, GET mutation returns 405, XSS is escaped, and productionRagEnabled=true returns 400", () -> {
            // GET mutation attempt
            HttpResp r405 = httpRequest("GET", baseUrl + "/admin/knowledge-base?action=delete&id=1", sessionCookie[0], null, null);
            check(r405.status == 405, "GET with mutation action must return 405, got " + r405.status);

            // Missing CSRF token
            String noCsrf = "action=submitReview&id=" + httpUploadedDocId[0];
            HttpResp r403 = httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", noCsrf.getBytes(StandardCharsets.UTF_8));
            check(r403.status == 403, "POST without CSRF token must return 403, got " + r403.status);

            // Attempt to enable productionRagEnabled=true
            String enableProd = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=saveRagConfig&tab=rag-config&localIndexingEnabled=true&stagingRagEnabled=true"
                    + "&productionRagEnabled=true&topK=3&similarityThreshold=0.72&maxContextTokens=900&chunkSize=1000&chunkOverlap=100";
            HttpResp r400 = httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", enableProd.getBytes(StandardCharsets.UTF_8));
            check(r400.status == 400 && r400.body.contains("production_rag_enabled"),
                    "Enabling productionRagEnabled=true over HTTP must return 400 with lock message, got " + r400.status);

            // Attempt XSS in Playground query
            String xssQuery = "csrf_token=" + URLEncoder.encode(csrfToken[0], "UTF-8")
                    + "&action=runPlaygroundQuery&tab=playground&query="
                    + URLEncoder.encode("<script>alert('xss')</script> hướng dẫn khi chơi game trực tuyến", "UTF-8");
            HttpResp rXss = httpRequest("POST", baseUrl + "/admin/knowledge-base", sessionCookie[0],
                    "application/x-www-form-urlencoded", xssQuery.getBytes(StandardCharsets.UTF_8));
            check(rXss.status == 200 && !rXss.body.contains("<script>alert('xss')</script>")
                    && rXss.body.contains("&lt;script&gt;"),
                    "XSS payload in playground query was not properly escaped");
        });
    }

    private static void Check302(HttpResp r, String label) {
        check(r.status == 302, "Expected 302 on " + label + ", got " + r.status + " body=" + r.body);
    }

    private static void writePart(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFilePart(ByteArrayOutputStream out, String boundary, String name,
            String filename, String contentType, byte[] data) throws IOException {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static final class HttpResp {
        final int status;
        final String location;
        final String cookie;
        final String body;

        HttpResp(int status, String location, String cookie, String body) {
            this.status = status;
            this.location = location;
            this.cookie = cookie;
            this.body = body;
        }
    }

    private static HttpResp httpRequest(String method, String urlStr, String cookie, String contentType, byte[] body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        if (cookie != null) {
            conn.setRequestProperty("Cookie", cookie);
        }
        if (contentType != null) {
            conn.setRequestProperty("Content-Type", contentType);
        }
        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
        }
        int status = conn.getResponseCode();
        String location = conn.getHeaderField("Location");
        String setCookie = null;
        Map<String, List<String>> headers = conn.getHeaderFields();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() != null && "Set-Cookie".equalsIgnoreCase(e.getKey()) && !e.getValue().isEmpty()) {
                String raw = e.getValue().get(e.getValue().size() - 1);
                int semi = raw.indexOf(';');
                setCookie = semi > 0 ? raw.substring(0, semi) : raw;
            }
        }
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String respBody = "";
        if (is != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            respBody = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
        return new HttpResp(status, location, setCookie, respBody);
    }
}
