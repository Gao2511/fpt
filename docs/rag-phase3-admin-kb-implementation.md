# PHASE 3 — ADMIN KNOWLEDGE BASE & RAG MANAGEMENT SYSTEM: IMPLEMENTATION REPORT

- **Project:** `D:\miniproject\fpt` (FPT Telecom Gia Lai)
- **Branch:** `feature/rag-integration`
- **Execution Mode:** `LOCAL DEVELOPMENT & OFFLINE TESTING ONLY`
- **Live Gemini API Requests:** `0`
- **Production Database Connections:** `0`
- **Customer-Facing `RAG_ENABLED` Default:** `false` (`production_rag_enabled = false` hard-locked server-side)

---

## 1. Architecture & Module Overview

Phase 3 extends the existing FPT Telecom Gia Lai Admin CMS (`Java Servlets / JSP / JDBC / PostgreSQL 17 + pgvector 0.8.2`) with a complete **AI Knowledge Base & RAG Management System** without rebuilding the website, altering customer-facing consultation logic, or making any external API requests.

### 1.1 Implemented Modules (`PHASE 3.2` – `PHASE 3.11`)

1. **Admin Navigation & Console (`web/view/admin/admin-header.jsp`, `web/view/admin/knowledge-base.jsp`, `web/assets/admin/admin-kb.css`, `src/java/controller/AdminKnowledgeBaseServlet.java`)**:
   - Added **AI Knowledge Base** tab (`activeTab=knowledge-base`) to `admin-header.jsp`.
   - Mapped `AdminKnowledgeBaseServlet` at `/admin/knowledge-base` with 8 structured sub-tabs:
     1. **Overview Dashboard (`tab=dashboard`)**: Real database counts for total documents, `DRAFT`, `PENDING_REVIEW`, `APPROVED`, `PUBLISHED`, `REJECTED`, `ARCHIVED`, `UNPUBLISHED`, `PROCESSING_FAILED`, indexed (`ready`), needing reindex (`NEEDS_REINDEX`), indexing failures, FAQs, chunks, and recent audit activity.
     2. **Documents & Lifecycle Management (`tab=documents`)**: Searchable, filterable, paginated table (`q`, `category`, `workflowStatus`, `indexingFilter`); create/edit document form; workflow transition actions (`Submit for Review`, `Approve`, `Reject` with mandatory reason, `Publish`, `Unpublish`, `Archive`, `Reindex`); immutable revision history table (`knowledge_document_revisions`); line-by-line version comparison (`RevisionDiff`); and non-destructive rollback (`Rollback -> v+1`).
     3. **Secure Document Upload (`tab=upload`)**: Multipart upload for `.pdf`, `.docx`, `.txt`, and `.md` (max `10 MB`) with magic-byte verification, storage outside the web root, text extraction into `DRAFT` revisions, and upload history table (`knowledge_files`).
     4. **FAQ Management (`tab=faqs`)**: Structured Q&A CRUD (`question`, `answer`, `category`, `tags`, `priority`, `sourceAuthority`, `policyKey`, `effectiveAt`, `expiresAt`), editorial review/publish workflow, immutable FAQ version history (`knowledge_faq_revisions`), and automatic synchronization of published FAQs to `public.knowledge_documents` (`source_type='FAQ'`, `source='faq:<id>'`).
     5. **Chunking & Offline Indexing (`tab=indexing`)**: Configurable chunk size (`200..1200`) and overlap (`0..400`), `ChunkPreview` inspector (ordinal, character count, estimated tokens, SHA-256 hash), stale-index (`NEEDS_REINDEX`) detector, and offline indexing trigger powered by `MockEmbeddingProvider` (`768` dimensions, `0` Gemini API calls).
     6. **Admin RAG Configuration (`tab=rag-config`)**: Separate toggles for `local_indexing_enabled`, `staging_rag_enabled`, and `production_rag_enabled` (hard-locked `false` server-side), plus retrieval `topK`, `similarityThreshold`, `maxContextTokens`, default `chunkSize` / `chunkOverlap`, and read-only Primary/Backup model health indicators.
     7. **AI Monitoring & Audit Log (`tab=audit`)**: Telemetry cards displaying `"Chưa có dữ liệu"` when live metrics are not collected, plus a paginated viewer for `public.admin_audit_logs` showing correlation ID, actor, action, resource type/ID, and sanitized before/after JSONB state.
     8. **Offline RAG Test Playground (`tab=playground`)**: Displays `"OFFLINE TEST MODE — NO GEMINI API REQUESTS"` banner, executes hybrid retrieval and source selection using `MockEmbeddingProvider` + `KnowledgeChunkDAO` + `KnowledgeContentPolicy` + `KnowledgeSourceSelection` + `RagContextBuilder`, and inspects candidate chunks, scores, authority, abstention/conflict decisions, context JSON, and deterministic mock answer.

---

## 2. Database Schema & Additive Migration (`PHASE 3.10`)

File: [`004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql)

- **Additive Columns on `public.knowledge_documents`**:
  - `workflow_status` (`DRAFT`, `PENDING_REVIEW`, `APPROVED`, `PUBLISHED`, `REJECTED`, `ARCHIVED`, `UNPUBLISHED`, `PROCESSING_FAILED`)
  - `source_type` (`MANUAL`, `UPLOAD`, `FAQ`)
  - `tags`, `department`, `created_by`, `updated_by`, `reviewed_by`, `reviewed_at`, `rejection_reason`, `chunk_size`, `chunk_overlap`
  - **Retrieval Safety Invariant**: Only `workflow_status = 'PUBLISHED'` maps to low-level `status = 'published'`. All non-published workflow states map to `status = 'draft'` or `'archived'`, guaranteeing that `KnowledgeChunkDAO` and `KnowledgeContentPolicy` never expose unapproved, rejected, unpublished, failed, or archived documents to customer retrieval.
- **New Tables**:
  - `public.knowledge_document_revisions` (`UNIQUE (document_id, version_number)`)
  - `public.knowledge_document_reviews`
  - `public.knowledge_files`
  - `public.knowledge_faqs`
  - `public.knowledge_faq_revisions` (`UNIQUE (faq_id, version_number)`)
  - `public.admin_audit_logs`

---

## 3. Files Created & Modified

### Created Files
- [`database/migrations/004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql) — Additive schema migration for Phase 3.
- [`src/java/rag/MockEmbeddingProvider.java`](file:///d:/miniproject/fpt/src/java/rag/MockEmbeddingProvider.java) — Deterministic offline 768-dimensional embedding provider (`EmbeddingService`).
- [`src/java/rag/extract/DocumentExtractionService.java`](file:///d:/miniproject/fpt/src/java/rag/extract/DocumentExtractionService.java) — Secure file upload validator, out-of-web-root storage manager, and UTF-8 text extractor for `.pdf`, `.docx`, `.txt`, and `.md`.
- [`src/java/dto/AdminKnowledgeDTO.java`](file:///d:/miniproject/fpt/src/java/dto/AdminKnowledgeDTO.java) — DTOs for Dashboard, ManagedDocument, DocumentRevision, RevisionDiff, DocumentReview, UploadedFileRecord, FaqItem, FaqRevision, AuditLogEntry, RagAdminSettings, and PlaygroundResult.
- [`src/java/dao/AdminKnowledgeDAO.java`](file:///d:/miniproject/fpt/src/java/dao/AdminKnowledgeDAO.java) — Transactionally safe DAO & service layer for all Phase 3 operations.
- [`src/java/controller/AdminKnowledgeBaseServlet.java`](file:///d:/miniproject/fpt/src/java/controller/AdminKnowledgeBaseServlet.java) — Admin servlet mapped at `/admin/knowledge-base`.
- [`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp) — Admin UI view for all 8 Knowledge Base & RAG management tabs.
- [`web/assets/admin/admin-kb.css`](file:///d:/miniproject/fpt/web/assets/admin/admin-kb.css) — Stylesheet for the Knowledge Base console.
- [`test/rag/RagAdminKbTest.java`](file:///d:/miniproject/fpt/test/rag/RagAdminKbTest.java) — Offline unit, upload/extraction, chunking, mock embedding, and servlet security test suite.
- [`test/rag/RagPhase3PostgresTest.java`](file:///d:/miniproject/fpt/test/rag/RagPhase3PostgresTest.java) — End-to-end isolated PostgreSQL 17 + pgvector 0.8.2 integration test suite for Phase 3.
- [`test/run-rag-phase3-postgres.ps1`](file:///d:/miniproject/fpt/test/run-rag-phase3-postgres.ps1) — Runner script for `RagPhase3PostgresTest`.

### Modified Files
- [`src/java/rag/DocumentChunkingService.java`](file:///d:/miniproject/fpt/src/java/rag/DocumentChunkingService.java) — Added configurable `chunk(content, maxChunkSize, overlap)` and `preview(...)` while preserving default `chunk(content)`.
- [`src/java/security/AdminSecurity.java`](file:///d:/miniproject/fpt/src/java/security/AdminSecurity.java) — Added `/admin/knowledge-base` to the local redirect whitelist.
- [`web/view/admin/admin-header.jsp`](file:///d:/miniproject/fpt/web/view/admin/admin-header.jsp) — Added **AI Knowledge Base** navigation tab (`activeTab == 'knowledge-base'`).
- [`build-portable.xml`](file:///d:/miniproject/fpt/build-portable.xml) — Added `rag.RagAdminKbTest` to offline test suite.
- [`test/compile-cms-jsps.ps1`](file:///d:/miniproject/fpt/test/compile-cms-jsps.ps1) — Added `view/admin/knowledge-base.jsp` to Jasper JSP compilation check (18 JSPs total).
- [`test/run-rag-isolated.ps1`](file:///d:/miniproject/fpt/test/run-rag-isolated.ps1) — Added `004-admin-knowledge-base.sql` migration and `-Phase3` switch.
