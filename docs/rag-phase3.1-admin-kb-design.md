# PHASE 3.1 — ADMIN KNOWLEDGE BASE & RAG MANAGEMENT SYSTEM: SOURCE AUDIT & TECHNICAL DESIGN

- **Project:** `D:\miniproject\fpt` (FPT Telecom Gia Lai)
- **Branch:** `feature/rag-integration`
- **Execution Mode:** `LOCAL DEVELOPMENT & OFFLINE TESTING ONLY`
- **Live Gemini API Requests:** `0`
- **Production Database Connections:** `0`
- **Customer-Facing `RAG_ENABLED` Default:** `false` (`production_rag_enabled = false` hard-locked server-side)

---

## 1. Existing Codebase Audit

### 1.1 Existing Admin CMS Architecture
- **Authentication & Authorization (`src/java/filter/AdminFilter.java`, `src/java/security/AdminSecurity.java`)**:
  - `AdminFilter` intercepts `/*` across `REQUEST`, `FORWARD`, and `ERROR` dispatcher types.
  - All `/admin/*` and `/view/admin/*` routes require an authenticated session with `AdminSecurity.admin(user)` (`role = "admin"` and `isActive = true`), re-verified against `UserDAO.getById(user.getId())`.
  - Direct requests (`DispatcherType.REQUEST`) to `/view/admin/*.jsp` return HTTP `403`.
  - Every non-`GET`/`HEAD` request to `/admin/*` requires a valid constant-time verified CSRF token (`AdminSecurity.csrf(request)` via `X-CSRF-Token` header or `csrf_token` parameter).
  - `GET` requests carrying mutating `action` parameters are rejected with HTTP `405`.
  - `demo_admin` accounts (`AdminSecurity.demo(user)`) are strictly isolated to read-only synthetic views in `cms.DemoData` and receive HTTP `403` on all non-whitelisted admin routes and all `POST` requests.
- **Admin Navigation & UI Styling (`web/view/admin/admin-header.jsp`, `web/assets/admin/admin-cms.css`)**:
  - Shared top header with `param.activeTab` highlighting (`dashboard`, `customers`, `packages`, `content`, `business`, `users`, `email-logs`, `settings`).
  - Exposes `<meta name="admin-csrf" content="<c:out value='${csrfToken}'/>">` and loads `/js/admin-security.js`.
- **Settings & Revisions (`src/java/dao/SettingsDAO.java`, `src/java/controller/AdminSettingsServlet.java`)**:
  - `settings` table stores key-value pairs; `cms_revisions` stores JSONB `before_data` / `after_data` snapshots serialized with `pg_advisory_xact_lock`.
  - `AdminSettingsServlet` manages multi-provider AI configuration (Primary & Backup models, thinking levels, fallback toggles, masked API keys).

### 1.2 Existing RAG Engine & Database Schema
- **Database Migrations (`database/migrations/002-rag-mvp.sql`, `database/migrations/003-rag-source-policy.sql`)**:
  - `public.knowledge_documents`:
    - Columns: `id`, `title`, `category` (`faq`, `policy`, `guide`), `content` (`1..60000` chars), `source`, `status` (`draft`, `published`, `archived`), `version`, `content_hash` (`CHAR(64)`), `effective_at`, `expires_at`, `approved_by`, `approved_at`, `embedding_model`, `vector_dimensions` (`768`), `indexing_status` (`pending`, `ready`, `failed`), `indexed_version`, `policy_key`, `source_authority` (`admin_reviewed`, `official`), `supersedes_document_id`, `created_at`, `updated_at`.
  - `public.knowledge_chunks`:
    - Columns: `id`, `document_id`, `document_version`, `document_hash`, `ordinal`, `content` (`1..1200` chars), `content_hash`, `embedding_model`, `vector_dimensions` (`768`), `embedding vector(768)`, `search_text`, `search_vector TSVECTOR`, `created_at`.
  - `public.knowledge_index_jobs`:
    - Columns: `id`, `document_id`, `document_version`, `document_hash`, `embedding_model`, `vector_dimensions`, `status` (`queued`, `running`, `done`, `failed`, `superseded`), `attempts` (`0..3`), `lease_token`, `lease_until`, `available_at`, `error_code`, `created_at`, `updated_at`.
- **RAG Services (`src/java/rag/`, `src/java/dao/`)**:
  - `DocumentChunkingService`: Splits document content into bounded chunks (default max 1000 chars, 100 chars overlap, max 100 chunks, surrogate-pair safe).
  - `KnowledgeIndexingService`: Explicit worker (`runNext()`) claiming jobs from `KnowledgeIndexJobDAO`, chunking content, generating 768-dim normalized vectors via `EmbeddingService`, replacing `knowledge_chunks` transactionally, and marking `indexing_status = 'ready'` and `indexed_version = version`.
  - `KnowledgeChunkDAO` & `KnowledgeRetrievalService`: Hybrid pgvector cosine similarity + accent-normalized PostgreSQL FTS (`ts_rank_cd`), enforcing `ELIGIBLE` predicates (`status = 'published'`, `approved_by > 0`, `indexing_status = 'ready'`, `indexed_version = version`, effective/expiry window, supersession lineage).
  - `KnowledgeContentPolicy` & `KnowledgeSourceSelection`: Filters out unapproved, expired, superseded, low-confidence, prompt-injection, or business-price/speed-claiming chunks; resolves authority (`official` > `admin_reviewed`) and abstains on conflicts (`VERSION_CONFLICT`, `SOURCE_CONFLICT`, `AMBIGUOUS_SUBJECT`).

---

## 2. Gap Analysis (What Exists vs. What Must Be Built for Phase 3)

| Capability Area | Existing State (Phase 2.4.9) | Phase 3 Gap & Required Addition |
|---|---|---|
| **Admin Navigation & UI** | 8 tabs in `admin-header.jsp`; no Knowledge Base UI | Add `AI Knowledge Base` tab (`activeTab=knowledge-base`) and `/admin/knowledge-base` console with Dashboard, Documents, Upload, Review/Versions, FAQs, Indexing, RAG Config, Audit Log, and Offline Playground |
| **Document Lifecycle** | 3 low-level statuses (`draft`, `published`, `archived`) in `knowledge_documents` | Full 8-state editorial lifecycle (`DRAFT`, `PENDING_REVIEW`, `APPROVED`, `PUBLISHED`, `REJECTED`, `ARCHIVED`, `UNPUBLISHED`, `PROCESSING_FAILED`) mapped cleanly onto `knowledge_documents` so only `PUBLISHED` maps to `status='published'` |
| **Revision & Review History** | In-place `version = version + 1` update on `knowledge_documents`; no revision snapshots or review log | Dedicated `knowledge_document_revisions` (immutable snapshots + diff/rollback) and `knowledge_document_reviews` (reviewer, timestamp, decision, rejection reason) |
| **File Upload & Extraction** | Image-only upload in `MediaServlet` (`cms_media`) | Secure document upload (`PDF`, `DOCX`, `TXT`, `MD`) stored outside web root with UUID names, magic-byte validation, ZIP-bomb/macro/PDF-JS protection, UTF-8 text extraction into `DRAFT` revisions (`knowledge_files`) |
| **FAQ Management** | `category='faq'` supported in `knowledge_documents`, no structured Q&A CRUD | Dedicated `knowledge_faqs` and `knowledge_faq_revisions` tables with priority, tags, editorial workflow, and automatic synchronization to `knowledge_documents` (`source_type='FAQ'`, `source='faq:<id>'`) |
| **Offline Indexing & Mock Embeddings** | `KnowledgeIndexingService` requires an `EmbeddingService` implementation; only `GeminiEmbeddingService` exists in `src/java/rag/` | Add deterministic offline `MockEmbeddingProvider` (`768` dimensions, normalized, zero network I/O), configurable chunk size/overlap preview, stale-index detection, and explicit Admin reindex trigger |
| **RAG Admin Settings** | Environment-only `RagConfig` | Admin-manageable RAG settings (`local_indexing_enabled`, `staging_rag_enabled`, `production_rag_enabled` hard-locked `false`, `topK`, `similarity_threshold`, `max_context_tokens`, `chunk_size`, `chunk_overlap`) |
| **Audit Logging & Monitoring** | `cms_revisions` for CMS settings only | Dedicated `admin_audit_logs` table recording actor, action, resource type/ID, sanitized before/after JSONB state, correlation ID, and timestamp |
| **Offline RAG Test Playground** | `/admin/ai-playground` calls live LLM provider | Dedicated Offline RAG Test Playground in `/admin/knowledge-base` using `MockEmbeddingProvider` + `KnowledgeRetrievalService` + `KnowledgeSourceSelection` + `RagContextBuilder` with `0` network calls |

---

## 3. Technical Design

### 3.1 Additive Database Schema (`database/migrations/004-admin-knowledge-base.sql`)
All schema changes are strictly additive (`ALTER TABLE ... ADD COLUMN IF NOT EXISTS` with safe defaults, and `CREATE TABLE IF NOT EXISTS`) so existing Phase 2.1–2.4 queries and tests continue to work without modification:

1. **`public.knowledge_documents` (Additive Columns)**:
   - `workflow_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' CHECK (workflow_status IN ('DRAFT','PENDING_REVIEW','APPROVED','PUBLISHED','REJECTED','ARCHIVED','UNPUBLISHED','PROCESSING_FAILED'))`
   - `source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' CHECK (source_type IN ('MANUAL','UPLOAD','FAQ'))`
   - `tags VARCHAR(500) NOT NULL DEFAULT ''`
   - `department VARCHAR(100) NOT NULL DEFAULT ''`
   - `created_by INTEGER`
   - `updated_by INTEGER`
   - `reviewed_by INTEGER`
   - `reviewed_at TIMESTAMPTZ`
   - `rejection_reason VARCHAR(1000)`
   - `chunk_size INTEGER NOT NULL DEFAULT 1000 CHECK (chunk_size BETWEEN 200 AND 1200)`
   - `chunk_overlap INTEGER NOT NULL DEFAULT 100 CHECK (chunk_overlap BETWEEN 0 AND 400)`
   - **Invariance Mapping**:
     - `workflow_status = 'PUBLISHED'` $\iff$ `status = 'published'`
     - `workflow_status = 'ARCHIVED'` $\iff$ `status = 'archived'`
     - All other `workflow_status` values (`DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `UNPUBLISHED`, `PROCESSING_FAILED`) map to `status = 'draft'`, guaranteeing `KnowledgeChunkDAO` and `KnowledgeContentPolicy` never expose non-published documents to customer retrieval.

2. **`public.knowledge_document_revisions`**:
   - Stores immutable snapshots for every version (`document_id`, `version_number`, `title`, `category`, `tags`, `department`, `source`, `source_type`, `source_authority`, `policy_key`, `content`, `content_hash`, `workflow_status`, `effective_at`, `expires_at`, `supersedes_document_id`, `change_note`, `rollback_from_version`, `created_by`, `created_at`).
   - Supports side-by-side / line-by-line version comparison and non-destructive rollback (creating `version + 1` in `DRAFT` or `PENDING_REVIEW` state from a prior revision).

3. **`public.knowledge_document_reviews`**:
   - Stores immutable editorial transition records (`document_id`, `version_number`, `action`, `previous_status`, `new_status`, `reviewer_id`, `comments`, `rejection_reason`, `created_at`).

4. **`public.knowledge_files`**:
   - Tracks uploaded source files (`id UUID PRIMARY KEY`, `document_id`, `version_number`, `original_filename`, `stored_filename`, `storage_rel_path`, `detected_mime_type`, `file_extension`, `file_size_bytes`, `sha256_hash`, `extraction_status`, `extracted_char_count`, `extraction_warning`, `uploaded_by`, `uploaded_at`).

5. **`public.knowledge_faqs` & `public.knowledge_faq_revisions`**:
   - Manages structured Q&A entries (`question`, `answer`, `category`, `tags`, `priority`, `workflow_status`, `version_number`, `effective_at`, `expires_at`, `source_authority`, `policy_key`, `linked_document_id`, `created_by`, `updated_by`, `reviewed_by`, `reviewed_at`, `rejection_reason`).
   - Publishing an FAQ synchronizes it with its linked `public.knowledge_documents` entry (`source_type = 'FAQ'`, `source = 'faq:' || id`, `category = 'faq'`) and marks it ready for indexing. Unpublishing, rejecting, or archiving immediately demotes the linked document out of `'published'` status.

6. **`public.admin_audit_logs`**:
   - Structured audit trail (`id`, `correlation_id`, `actor_id`, `actor_username`, `action`, `resource_type`, `resource_id`, `previous_state JSONB`, `new_state JSONB`, `summary`, `created_at`).
   - Enforces automatic secret/embedding redaction before persistence.

### 3.2 Secure File Upload & Text Extraction (`src/java/rag/extract/`)
- **`KnowledgeFileStorageService`**:
  - Stores files outside the web root in `${java.io.tmpdir}/fpt-kb-uploads` (or configured `KB_UPLOAD_STORAGE_DIR` outside `web/`), organized by UUID (`<uuid>.<ext>`).
  - Enforces max upload size (default `10 MB` = `10,485,760` bytes), filename sanitization, path traversal rejection (`..`, `/`, `\`, null bytes), and extension allowlisting (`.pdf`, `.docx`, `.txt`, `.md`).
- **`DocumentTextExtractor`**:
  - Validates file content signatures (magic bytes) before parsing:
    - `.pdf`: Must start with `%PDF-1.` or `%PDF-2.` and contain `%%EOF`; rejects embedded `/JavaScript`, `/JS`, `/Launch`, `/EmbeddedFiles`. Decompresses `/FlateDecode` streams with strict byte caps and extracts text operators (`Tj`, `TJ`, `'`, `"`).
    - `.docx`: Must start with ZIP signature `PK\x03\x04` (`0x50 0x4B 0x03 0x04`); inspects entries with bounded `ZipInputStream` (max 250 entries, max 25 MB uncompressed, max 50:1 ratio, no `..` entry paths); rejects macro-enabled archives containing `vbaProject.bin` or `vbaData.xml`; parses `word/document.xml` with XXE-hardened `DocumentBuilderFactory`.
    - `.txt` / `.md`: Rejects binary files containing null bytes (`0x00`) or control characters and decodes with strict `UTF-8` validation.
  - Normalizes extracted text into clean UTF-8 (`1..60000` characters) and creates a `DRAFT` document revision (never auto-published, never auto-indexed). If extraction fails or yields empty text, records `PROCESSING_FAILED` safely.

### 3.3 Deterministic Offline Indexing (`src/java/rag/MockEmbeddingProvider.java`)
- Implements `EmbeddingService` (`model = "gemini-embedding-001"`, `dimensions = 768`).
- Generates deterministic, unit-normalized 768-dimensional vectors by combining:
  - Token-level hashed feature projections (using accent-normalized tokens from `RetrievalQuery.normalize`) across dimensions `0..511` so semantically/lexically related Vietnamese queries and documents have high cosine similarity (`> 0.75`) in pgvector searches.
  - SHA-256 character n-gram projections across dimensions `512..767` for deterministic uniqueness.
- Performs **zero network I/O** and requires **no API keys**.
- `DocumentChunkingService` is extended with configurable `chunk(String content, int maxChunkSize, int overlap)` (keeping `chunk(String content)` with `1000` / `100` defaults for backward compatibility).

### 3.4 Admin Knowledge Base Controller, Views & Security
- **Controller**: `src/java/controller/AdminKnowledgeBaseServlet.java` mapped at `/admin/knowledge-base` with `@MultipartConfig(maxFileSize = 10485760, maxRequestSize = 11534336)`.
- **Redirect Whitelist**: Update `AdminSecurity.redirect(...)` to allow `/admin/knowledge-base`.
- **Navigation**: Update `web/view/admin/admin-header.jsp` to add the **AI Knowledge Base** tab (`activeTab == 'knowledge-base'`).
- **View**: `web/view/admin/knowledge-base.jsp` + `web/assets/admin/admin-kb.css` providing:
  1. **Overview Dashboard** (real DB metrics + recent activity)
  2. **Documents & Lifecycle Management** (filterable/paginated table, create/edit metadata & content, submit for review, approve, reject with reason, publish, unpublish, archive, version history & diff/rollback)
  3. **Secure Document Upload** (`PDF`, `DOCX`, `TXT`, `MD` upload into `DRAFT` with extraction preview)
  4. **FAQ Management** (CRUD, category/tags/priority, review/publish workflow, RAG sync)
  5. **Chunking & Offline Indexing** (chunk preview, stale-index detection, offline indexing trigger with `MockEmbeddingProvider`, job status table)
  6. **RAG Configuration** (`local_indexing_enabled`, `staging_rag_enabled`, `production_rag_enabled` hard-locked `false`, retrieval `topK`, similarity threshold, max context tokens, chunk size/overlap, Primary/Backup model health)
  7. **Audit Log & Monitoring** (paginated `admin_audit_logs` viewer + metric cards showing `"Chưa có dữ liệu"` when telemetry is absent)
  8. **Offline RAG Test Playground** (prominent `"OFFLINE TEST MODE — NO GEMINI API REQUESTS"` banner, query input, retrieved chunks, scores, authority, abstention/conflict diagnosis, and deterministic mock response)
