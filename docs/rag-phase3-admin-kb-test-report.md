# PHASE 3.12 & 3.13 — ADMIN KNOWLEDGE BASE & RAG TEST & REGRESSION REPORT

**Project:** `D:\miniproject\fpt`  
**Branch:** `feature/rag-integration`  
**Target Website:** FPT Telecom Gia Lai  
**Test Date:** 2026-10-12  
**Execution Mode:** **LOCAL DEVELOPMENT & OFFLINE TESTING ONLY (`0` Real Gemini API Calls, `0` Production DB Connections)**

---

## 1. Executive Summary

| Verification Category | Suite / Script | Total Checks | Passed | Failed | Real Gemini API Calls | Status |
|---|---|---:|---:|---:|---:|---|
| **Phase 3 Offline Unit & Security Suite** | `rag.RagAdminKbTest` (`ant -f build-portable.xml test`) | 17 | 17 | 0 | 0 | **PASS** |
| **Phase 3 Isolated PostgreSQL 17 + pgvector 0.8.2 Suite** | `rag.RagPhase3PostgresTest` (`test/run-rag-isolated.ps1 -Phase3`) | 10 | 10 | 0 | 0 | **PASS** |
| **Jasper JSP Compilation (All Admin + Public JSPs)** | `test/compile-cms-jsps.ps1` (Tomcat 9.0.118 `JspC`) | 18 | 18 | 0 | 0 | **PASS** |
| **Portable WAR Packaging (`Java 17 --release 8`)** | `ant -f build-portable.xml war` | 1 | 1 | 0 | 0 | **PASS** |
| **CMS Security & Behavior Regression** | `CmsSecurityTest`, `CmsBehaviorTest`, `LocalDatabaseTest`, `EditorLayoutTest` | 48 | 48 | 0 | 0 | **PASS** |
| **Chatbot & Consultation Regression** | `ConsultationBehaviorTest`, `AdaptiveConversationTest`, `InstallationFeeTest`, `ChatRegistrationTest`, `ProviderResponseTest`, `MultiProviderTest` | 105 | 105 | 0 | 0 | **PASS** |
| **RAG Offline Regression** | `RagBehaviorTest`, `RagPhase2CorpusTest`, `RagQualityTest` | 75 | 75 | 0 | 0 | **PASS** |
| **Phase 2.2 Isolated PostgreSQL 17 + pgvector Regression** | `RagPostgresTest` (`test/run-rag-isolated.ps1`) | 24 | 24 | 0 | 0 | **PASS** |
| **Phase 2.4 End-to-End Isolated PostgreSQL 17 + pgvector Regression** | `RagPhase24E2ETest` (`test/run-rag-isolated.ps1 -Phase24`) | 41 | 41 | 0 | 0 | **PASS** |
| **Phase 2.4.9 Live Runner Safety & Preserve Manifest** | `test/test-rag-live-runner.ps1` + `test/review-rag-phase24-plan.ps1` | 91 | 91 | 0 | 0 | **PASS** |

---

## 2. Detailed Phase 3 Test Coverage

### 2.1 `rag.RagAdminKbTest` (17/17 Offline Unit & Security Tests)

1. `extract UTF-8 TXT file and compute SHA-256` — **PASS**
2. `extract valid DOCX file and strip XML tags cleanly` — **PASS**
3. `extract valid PDF text stream` — **PASS**
4. `extract Markdown file` — **PASS**
5. `reject path traversal and double extensions` (`../../etc/passwd.txt`, `policy.jsp.pdf`, `doc.exe.txt`) — **PASS**
6. `reject executable, JSP, HTML, and macro-enabled Office extensions` (`.jsp`, `.docm`, `.html`, `.exe`) — **PASS**
7. `reject magic byte mismatch (fake PDF and fake DOCX)` — **PASS**
8. `reject binary file with NUL bytes disguised as TXT` — **PASS**
9. `reject DOCX containing VBA macro payload (vbaProject.bin)` — **PASS**
10. `reject PDF containing embedded /JavaScript` — **PASS**
11. `MockEmbeddingProvider generates deterministic unit-normalized 768-d vectors with 0 API calls` — **PASS**
12. `DocumentChunkingService configurable chunkSize/overlap and ChunkPreview accuracy` — **PASS**
13. `DocumentChunkingService rejects out-of-range chunkSize or overlap` — **PASS**
14. `ManagedDocument.isNeedsReindex detects pending, failed, and stale indexed versions` — **PASS**
15. `AdminSecurity.redirect allows /admin/knowledge-base and blocks external redirects` — **PASS**
16. `AdminKnowledgeDAO.computeLineDiff detects added, removed, and unchanged lines` — **PASS**
17. `AdminKnowledgeDAO.redactMap redacts secrets, tokens, passwords, and embeddings` — **PASS**

### 2.2 `rag.RagPhase3PostgresTest` (10/10 Isolated PostgreSQL 17.10 + pgvector 0.8.2 Integration Tests)

1. `initial dashboard metrics reflect empty knowledge base` — **PASS**
2. `upload TXT file creates DRAFT document and knowledge_files row without publishing or indexing` — **PASS**
3. `cannot publish directly from DRAFT or PENDING_REVIEW without approval` — **PASS**
4. `reject document requires reason and transitions to REJECTED` — **PASS**
5. `editing rejected document creates v2 revision in DRAFT and computes accurate RevisionDiff` — **PASS**
6. `DRAFT -> PENDING_REVIEW -> APPROVED -> PUBLISHED -> Offline Indexing -> Retrievable in Playground` — **PASS**
7. `editing a published document with preservePublishedState=true marks it NEEDS_REINDEX until reindexed` — **PASS**
8. `rollback from v3 to v2 creates new revision v4 in DRAFT without deleting revision history` — **PASS**
9. `FAQ lifecycle syncs published FAQ to RAG with source_type=FAQ and excludes unpublished/archived FAQs` — **PASS**
10. `saveRagSettings blocks enabling production_rag_enabled and records audit log` — **PASS**

### 2.3 Tomcat 9 Jasper JSP Compilation (`18/18` JSPs)

- Verified via `test/compile-cms-jsps.ps1` using Apache Tomcat `9.0.118` (`org.apache.jasper.JspC`) + `javac --release 8`:
  - `web/view/admin/knowledge-base.jsp` and `web/view/admin/admin-header.jsp` compiled into Java servlets and `.class` bytecode with **`0` errors**.

---

## 3. Manual Admin UI Verification Checklist

1. **Navigation & Layout:**
   - Sign in at `/admin/login` with an Admin account.
   - Click **AI Knowledge Base** in the top Admin navigation bar (`/admin/knowledge-base`).
   - Verify the safety banner displays: `OFFLINE TEST MODE — NO GEMINI API REQUESTS · RAG_ENABLED=false · production_rag_enabled=false (Server-Locked)`.
2. **Overview Dashboard (`?section=dashboard`):**
   - Verify KPI cards display real database counts for Total Documents, Published & Indexed, Workflow Queue (`DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`), Index Health (`NEEDS_REINDEX`, `failed`), FAQs, and Chunks.
3. **Document Lifecycle, Diff & Rollback (`?section=documents`):**
   - Create a new document -> verify it starts in `DRAFT`.
   - Click **Gửi duyệt** (`PENDING_REVIEW`) -> **Phê duyệt** (`APPROVED`) -> **Xuất bản** (`PUBLISHED`) -> **Reindex Offline** (`ready`).
   - Edit the document -> verify version increments (`v2`), Revision History lists both `v1` and `v2`, and the **So sánh phiên bản (Diff)** viewer highlights line-by-line additions (`+`) and removals (`-`).
   - Click **Rollback bản này** on `v1` -> verify non-destructive creation of `v3` (`DRAFT`, `rollback_from_version=1`).
4. **Secure File Upload (`?section=upload`):**
   - Upload a `.txt`, `.md`, `.docx`, or `.pdf` file (`<= 10 MB`).
   - Verify text is extracted into a new `DRAFT` document without auto-publishing or auto-indexing.
5. **FAQ Management (`?section=faqs`):**
   - Create an FAQ, transition `DRAFT -> PENDING_REVIEW -> APPROVED -> PUBLISHED`, run offline indexing, and verify its linked RAG document (`source_type='FAQ'`, `source='faq:<id>'`) is created.
6. **Chunking & Indexing (`?section=indexing`):**
   - Inspect chunk previews (`#index`, character length, approximate tokens, `SHA-256` hash) and run batch offline indexing.
7. **RAG Configuration (`?section=settings`):**
   - Update `topK`, `similarityThreshold`, `maxContextTokens`, `chunkSize`, and `chunkOverlap`.
   - Verify `production_rag_enabled` is locked to `false`.
8. **AI Monitoring & Audit Logs (`?section=monitoring`):**
   - Inspect structured audit entries in `admin_audit_logs` and verify live telemetry cards without production metrics show `"Chưa có dữ liệu"`.
9. **Offline RAG Test Playground (`?section=playground`):**
   - Enter a test query and verify candidate chunks, scores, authority, policy acceptance/rejection reasons, context JSON, and deterministic mock response are displayed with `0` Gemini API calls.
