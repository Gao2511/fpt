# Phase 5.3 — Live Gemini Validation, Supabase Migration & Render Release Gates

- **Project**: `D:\miniproject\fpt`
- **Branch**: `feature/rag-integration`
- **Target Production**: `https://fpt.gialai.vn/` (Render Web Service + Supabase PostgreSQL + Google Gemini API)
- **Build Version**: `5.3.0-online-rag`
- **Real Gemini API Requests Executed**: `0`
- **Supabase Production Write Queries Executed**: `0`
- **Production RAG Status (`RAG_ENABLED`)**: `false` (Locked Disabled)
- **Smoke Plan Approval Status**: `approved=false`, `executionAuthorized=false`

---

## 1. Phase 5.2 Audit Findings & Phase 5.3 Engineering Fixes

All Phase 5.2 artifacts were audited at the file, SQL, Java bytecode, and JSP levels. Below is the exact audit log and the fixes applied in Phase 5.3:

| Area | Phase 5.2 State / Finding | Phase 5.3 Fix / Verification | Status |
|---|---|---|---|
| [`Dockerfile`](file:///d:/miniproject/fpt/Dockerfile) | Multi-stage JDK 17 + Ant source build into Tomcat 9 JRE 17 runtime. Potential CRLF line-ending risk if cloned on Windows hosts before `docker build`. | Added `sed -i 's/\r$//' tools/bootstrap-libs.sh tools/dependencies.lock` prior to running `sh tools/bootstrap-libs.sh`; bumped `BUILD_VERSION=5.3.0-online-rag`. | **FIXED & VERIFIED** |
| [`tools/dependencies.lock`](file:///d:/miniproject/fpt/tools/dependencies.lock) & Bootstrap Scripts | Pinned Maven Central coordinates and SHA-256 checksums for all 6 runtime/compile JARs. | Verified via `./tools/bootstrap-libs.ps1 -VerifyOnly`: all 6 JARs (`postgresql-42.7.10`, `javax.mail-1.6.2`, `activation-1.1.1`, `taglibs-standard-impl-1.2.5`, `taglibs-standard-spec-1.2.5`, `servlet-api-2.5`) match exact SHA-256 hashes. | **VERIFIED (`PASS`)** |
| [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) | Previously queried `public.rag_schema_migrations` directly (failing with `ERROR: relation "public.rag_schema_migrations" does not exist` on a pre-007 database) and referenced mismatched names (`knowledge_audit_logs`, `ai_provider_settings`, `rag_retrieval_logs`, `embedding_dimensions`, `storage_path`). | Rewritten using `to_regclass(...)` and `query_to_xml(...)` so the entire script is 100% read-only (`SELECT` only) and succeeds with `0` errors both **before** and **after** migrations `002`–`007` are applied. Verified against local PostgreSQL 18 (`--set ON_ERROR_STOP=1`, exit code `0`). | **FIXED & VERIFIED** |
| [`database/migrations/007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql) | Created `public.rag_schema_migrations` and added `lease_owner`/`lease_until`/`max_attempts`/`last_heartbeat_at` to `public.knowledge_index_jobs`. | Hardened with a `DO $$` prerequisite verification block checking `pgvector` and `002`/`004`/`006` tables (`!contains("CREATE EXTENSION")`), conditionally seeding `001`–`007` into `rag_schema_migrations`, and locking `rag.production_rag_enabled = 'false'` in `public.settings`. | **FIXED & VERIFIED** |
| [`src/java/rag/runtime/GeminiRuntimeConfig.java`](file:///d:/miniproject/fpt/src/java/rag/runtime/GeminiRuntimeConfig.java) | Validated `approved=true` on the live execution plan JSON, but did not enforce `executionAuthorized=true` when the field was present in the plan JSON. | Added `requireExecutionAuthorizedIfPresent(root)` to `loadLiveExecutionPlan(...)` so `STAGING`/`PRODUCTION`/`LOCAL_LIVE_TEST` fail closed whenever `"executionAuthorized": false` is present in the plan JSON. | **FIXED & VERIFIED** |
| [`config/phase52-online-smoke-plan.json`](file:///d:/miniproject/fpt/config/phase52-online-smoke-plan.json) | Had placeholder `allowedPayloads: {}` and lacked `"environment": "STAGING"` and `"executionAuthorized": false`. | Populated exact SHA-256 payload hashes for the 1 synthetic smoke document and 2 smoke queries, set `"environment": "STAGING"`, `"approved": false`, `"executionAuthorized": false`, and capped `maxTotalGeminiRequests=4`. | **FIXED & VERIFIED** |
| [`src/java/controller/AdminKnowledgeBaseServlet.java`](file:///d:/miniproject/fpt/src/java/controller/AdminKnowledgeBaseServlet.java) & Admin JSPs | `loadGeminiRuntimeTabAttributes` only loaded `ragLivePlan` when `RAG_EXECUTION_MODE == LOCAL_LIVE_TEST` and lacked a UI button for `recoverExpiredLeases`. | Updated `AdminKnowledgeBaseServlet.java` to load `ragLivePlan` across `LOCAL_LIVE_TEST`, `STAGING`, and `PRODUCTION`; updated [`knowledge-gemini-runtime.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-gemini-runtime.jsp) and [`knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp) to display the active plan and expose the `recoverExpiredLeases` action on the Indexing tab. | **FIXED & VERIFIED** |
| [`src/java/utils/BuildInfo.java`](file:///d:/miniproject/fpt/src/java/utils/BuildInfo.java) & [`build-portable.xml`](file:///d:/miniproject/fpt/build-portable.xml) | Version `5.2.0-online-rag`. | Bumped `DEFAULT_BUILD_VERSION` and Ant property `build.version` to `5.3.0-online-rag`. | **FIXED & VERIFIED** |

---

## 2. Supabase Preflight Status & Read-Only Inspection Package

### 2.1 Live Supabase Execution Status
- **Live Supabase Preflight Status**: **`NOT VERIFIED (Awaiting Read-Only Supabase Execution)`**
- **Reason**: No `DB_URL`, `DB_USER`, or `DB_PASSWORD` pointing to Supabase production/staging are present in the local environment, and no production database queries were executed.
- **Offline Baseline Verification**: Executed [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) with `psql -X --set ON_ERROR_STOP=1` against local PostgreSQL 18 (`fpt_local`, containing base tables + `001` only). The script exited `0` with zero errors, accurately reporting `001` as `ALREADY_APPLIED` and `002`–`007` as `MISSING`, and confirming zero PII or secrets in its output.

### 2.2 Read-Only Preflight Command for Administrator
Run the following read-only command against the Supabase database (or paste [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) into the Supabase SQL Editor):

```powershell
psql "postgresql://<READONLY_USER>:<PASSWORD>@<SUPABASE_HOST>:5432/postgres?sslmode=require" `
  -X --set ON_ERROR_STOP=1 `
  -f database/migrations/verify-supabase-readiness.sql
```

What [`verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) inspects (100% `SELECT`, zero DDL/DML, zero PII/secret values):
1. PostgreSQL server version, current database/schema, `transaction_read_only`, and UTC timestamp.
2. Installed extensions (`vector` / `pgvector` version and schema, `pgcrypto`) and available `vector` version in `pg_available_extensions`.
3. Base website/CMS tables (`users`, `packages`, `registrations`, `news`, `banners`, `faqs`, `settings`, `contact_messages`, `email_logs`, `chat_logs`) and `001` tables (`cms_media_assets`, `cms_content_revisions`, `admin_audit_logs`).
4. Detailed `ALREADY_APPLIED` vs `MISSING` status for migrations `001` through `007` based on structural table/column/trigger existence.
5. `public.rag_schema_migrations` ledger rows (safely guarded via `query_to_xml` when table `007` is not yet applied).
6. Column definitions and vector dimensions (`atttypmod = 768`) on `public.knowledge_chunks`.
7. Existing indexes (including `idx_knowledge_chunks_embedding_hnsw` and `idx_knowledge_index_jobs_lease`).
8. Safe row counts and aggregate statuses across `knowledge_documents`, `knowledge_chunks`, and `knowledge_index_jobs`.
9. Key-only check on `public.settings` for `rag.%` keys (`value_redacted = '[REDACTED_IN_PREFLIGHT]'`).

---

## 3. Schema Difference Matrix (`001` – `007`)

| Migration Version | Script File | Target Objects Created / Altered | Idempotent Re-Run? | Expected Pre-RAG Production State |
|---|---|---|---|---|
| `001-cms-media-revisions` | [`database/migrations/001-cms-media-revisions.sql`](file:///d:/miniproject/fpt/database/migrations/001-cms-media-revisions.sql) | `cms_media_assets`, `cms_content_revisions`, `admin_audit_logs`, `packages.display_order`, `banners.media_asset_id` | **Yes** (`IF NOT EXISTS`) | `ALREADY_APPLIED` (verify via Section 4 of preflight) |
| `002-rag-mvp` | [`database/migrations/002-rag-mvp.sql`](file:///d:/miniproject/fpt/database/migrations/002-rag-mvp.sql) | `EXTENSION vector`, `knowledge_documents`, `knowledge_chunks` (`vector(768)` + `tsvector`), `idx_knowledge_chunks_embedding_hnsw` | **No** (bare `CREATE TABLE`) — run **only** if preflight reports `MISSING` | `MISSING` |
| `003-rag-source-policy` | [`database/migrations/003-rag-source-policy.sql`](file:///d:/miniproject/fpt/database/migrations/003-rag-source-policy.sql) | Adds `source_type`, `authority_level`, `effective_from`, `effective_to`, `approval_status`, `reviewed_by`, `reviewed_at`, `supersedes_document_id` to `knowledge_documents` | **No** (bare `ADD COLUMN`) — run **only** if preflight reports `MISSING` | `MISSING` |
| `004-admin-knowledge-base` | [`database/migrations/004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql) | Adds KB upload/hash/chunking columns to `knowledge_documents` & `knowledge_chunks`; creates `knowledge_document_revisions`, `knowledge_faqs`, `knowledge_index_jobs`, `rag_Playground_logs` | **Yes** (`IF NOT EXISTS` + guarded constraint `DO $$`) | `MISSING` |
| `005-knowledge-section-import` | [`database/migrations/005-knowledge-section-import.sql`](file:///d:/miniproject/fpt/database/migrations/005-knowledge-section-import.sql) | Creates `knowledge_import_sections` and trigger `guard_knowledge_import_original` on `knowledge_documents` | **No** (bare `CREATE TRIGGER`) — run **only** if preflight reports `MISSING` | `MISSING` |
| `006-rag-gemini-runtime` | [`database/migrations/006-rag-gemini-runtime.sql`](file:///d:/miniproject/fpt/database/migrations/006-rag-gemini-runtime.sql) | Adds `vector_dimensions` to `knowledge_chunks` and `knowledge_index_jobs`; seeds `rag.*` runtime keys in `settings` with `rag.production_rag_enabled = 'false'` | **Yes** (`IF NOT EXISTS` + `ON CONFLICT`) | `MISSING` |
| `007-online-production-readiness` | [`database/migrations/007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql) | Verifies prerequisites (`!CREATE EXTENSION`), creates `rag_schema_migrations`, adds lease columns (`lease_owner`, `lease_until`, `max_attempts`, `last_heartbeat_at`) to `knowledge_index_jobs`, locks `rag.production_rag_enabled = 'false'` | **Yes** (`IF NOT EXISTS` + `ON CONFLICT`) | `MISSING` |

---

## 4. Safe Migration Execution Plan

### 4.1 Prerequisites before Running Any Migration
1. Take a Supabase database snapshot / logical backup (`pg_dump --format=custom --no-owner --no-acl`).
2. Run [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) and inspect Section 2 (`pgvector` availability) and Section 4 (`status` per migration `001`–`007`).
3. Ensure `pgvector` is enabled on Supabase (Database -> Extensions -> `vector` in `public` or `extensions` schema) before running `002-rag-mvp.sql`.

### 4.2 Ordered Migration Execution Sequence
Execute **only** the migrations marked `MISSING` in Section 4 of the preflight output, in strict numerical order (`001` -> `002` -> `003` -> `004` -> `005` -> `006` -> `007`), using the direct Supabase connection (port `5432`, **not** Transaction Pooler port `6543`):

```powershell
$SupabaseDirectUrl = "postgresql://postgres.<PROJECT_REF>:<PASSWORD>@<HOST>:5432/postgres?sslmode=require"

# 1. Pre-migration read-only check
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/verify-supabase-readiness.sql

# 2. Apply ONLY the migrations reported as MISSING in strict order:
# (Skip 001 if Section 4 reports 001-cms-media-revisions = ALREADY_APPLIED)
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/002-rag-mvp.sql
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/003-rag-source-policy.sql
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/004-admin-knowledge-base.sql
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/005-knowledge-section-import.sql
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/006-rag-gemini-runtime.sql
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/007-online-production-readiness.sql

# 3. Post-migration read-only verification
psql $SupabaseDirectUrl -X --set ON_ERROR_STOP=1 -f database/migrations/verify-supabase-readiness.sql
```

### 4.3 Lock Impact & Storage Impact Analysis
- **Zero Lock Impact on Public Website Tables**: Migrations `002`–`007` do **not** alter `users`, `packages`, `registrations`, `news`, `banners`, `faqs`, `contact_messages`, `email_logs`, or `chat_logs`. In `006` and `007`, `public.settings` receives lightweight single-row upserts (`INSERT ... ON CONFLICT (key) DO UPDATE`), acquiring only row-level locks on `rag.*` keys for `< 5 ms`.
- **Storage Impact**: Empty RAG tables and the empty HNSW index (`m = 16, ef_construction = 64` on `vector(768)`) consume `< 2 MB`. Each 1,000 indexed chunks (768 float32 dimensions + HNSW graph + `tsvector` + text) consume ~`8–12 MB` in Supabase storage.

---

## 5. Gemini API Readiness Status

| Dimension | Configured Value / Enforcement | Offline & Contract Verification | Live Project Status |
|---|---|---|---|
| **Primary Chat Model** | `gemini-3.8-flash` (`thinkingConfig.thinkingLevel = "medium"`) | Verified in [`GeminiConsultationProvider`](file:///d:/miniproject/fpt/src/java/ai/GeminiConsultationProvider.java) & [`RagPhase4OfflineTest`](file:///d:/miniproject/fpt/test/rag/RagPhase4OfflineTest.java) | **`NOT VERIFIED`** (0 live calls executed) |
| **Backup Chat Model** | `gemini-3.5-flash-lite` (`thinkingConfig.thinkingLevel = "low"`) | Disabled during smoke test (`backupModelEnabled=false`); bounded failover verified offline | **`NOT VERIFIED`** (0 live calls executed) |
| **Embedding Model** | `gemini-embedding-001` (`outputDimensionality = 768`, L2 normalized) | Verified in [`GeminiEmbeddingProvider`](file:///d:/miniproject/fpt/src/java/rag/GeminiEmbeddingProvider.java) (`embedContent`, `RETRIEVAL_DOCUMENT` / `RETRIEVAL_QUERY`, explicit `outputDimensionality: 768`, rejects zero-norm/wrong-dimension vectors) | **`NOT VERIFIED`** (0 live calls executed) |
| **REST Transport** | `https://generativelanguage.googleapis.com/v1beta/models/{model}:{action}` | `POST` only, `x-goog-api-key` header only (key never placed in URL or logs), `setInstanceFollowRedirects(false)`, `0` automatic retries on HTTP `400`/`401`/`403`/`429`/`500`/`302` | **VERIFIED (`46/46 PASS`)** |
| **Key Isolation** | `RAG_EMBEDDING_API_KEY` + `RAG_CHAT_TEST_API_KEY` (or `GEMINI_API_KEY` fallback only in `STAGING`/`PRODUCTION`) | Never exposed to JSPs, browser responses, error strings, or `admin_audit_logs` | **VERIFIED** |

---

## 6. 4-Request Real Gemini Smoke Test Package

Prepared in [`config/phase52-online-smoke-plan.json`](file:///d:/miniproject/fpt/config/phase52-online-smoke-plan.json):
- **Safety Locks (Default Repo State)**:
  - `"environment": "STAGING"`
  - `"approved": false`
  - `"executionAuthorized": false`
  - `"productionRagEnabled": false`
  - `"maxTotalGeminiRequests": 4` (`maxEmbeddingRequests: 3`, `maxChatRequests: 1`, `maxRetries: 0`)
- **4-Request Budget & SHA-256 Allowlist**:
  1. **Request 1 (`embedContent` / `RETRIEVAL_DOCUMENT`)**: Single-chunk synthetic test document (`SMOKE-PHASE53-DOC-1`, 178 chars):
     - Text: `"Tài liệu kiểm thử kỹ thuật FPT Telecom Gia Lai Phase 5.3: Khi đèn LOS trên modem nhấp nháy đỏ, khách hàng không tự ý tháo cáp quang mà kiểm tra đầu nối và liên hệ kỹ thuật hỗ trợ."`
     - SHA-256: `464d8523bcc5f69bf4a398738422f576e74a901024b6fd7a762677b81f243192`
  2. **Request 2 (`embedContent` / `RETRIEVAL_QUERY`)**: Approved knowledge query (`SMOKE-PHASE53-Q1`):
     - Text: `"Đèn LOS trên modem FPT nhấp nháy đỏ thì xử lý thế nào?"`
     - SHA-256: `36742152fcf25e819537c9a4f6403a5d5d3fce5dc1561ca1183b5d5433781ece`
  3. **Request 3 (`generateContent` / `gemini-3.8-flash` / `thinkingLevel=medium`)**: Grounded answer generation for `SMOKE-PHASE53-Q1` citing `SMOKE-PHASE53-DOC-1`.
  4. **Request 4 (`embedContent` / `RETRIEVAL_QUERY`)**: Unrelated/unverified query (`SMOKE-PHASE53-Q2`):
     - Text: `"Dự báo thời tiết mưa bão ngày mai tại Pleiku Gia Lai như thế nào?"`
     - SHA-256: `3de10ff1392f5d8cec54ce7c4d0848bf49b78d7decf05c120a3d76de558c9d95`
     - Expected Result: Retrieval similarity below threshold (`0` eligible chunks selected) -> fails closed before `generateContent` (`0` chat requests used for `Q2`).

---

## 7. Render Configuration Audit & Readiness Matrix

| Variable / Resource | Target Value (`STAGING` Phase) | Target Value (`PRODUCTION` Phase) | Current Status |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://<SUPABASE_HOST>:5432/postgres?sslmode=require` | Same (or `?sslmode=require&prepareThreshold=0` if port `6543`) | **`BLOCKED`** (Must be verified in Render Dashboard) |
| `DB_USER` / `DB_PASSWORD` | Supabase DB user & password | Supabase DB user & password | **`BLOCKED`** (Secret in Render Dashboard) |
| `RAG_EXECUTION_MODE` | `STAGING` | `PRODUCTION` | **`READY`** (Supported in code) |
| `RAG_ENABLED` | `false` | `false` initially -> `true` only at Step 16 | **`READY`** (Locked `false` by default) |
| `RAG_DATABASE_APPROVED` | `true` (after migrations `001`–`007` verified) | `true` | **`BLOCKED`** (Awaiting Supabase migration) |
| `RAG_LIVE_PLAN_PATH` | `/usr/local/tomcat/webapps/ROOT/WEB-INF/config/phase52-online-smoke-plan.json` (or persistent disk copy) | Same | **`READY`** (Bundled in WAR at `/WEB-INF/config/phase52-online-smoke-plan.json`) |
| `RAG_UPLOAD_STORAGE_DIR` | `/var/data/fpt-sale/knowledge-uploads` | `/var/data/fpt-sale/knowledge-uploads` | **`READY`** (Validated outside webroot) |
| `RAG_LEDGER_PATH` | `/var/data/fpt-sale/rag-ledger/gemini-smoke-ledger.json` | `/var/data/fpt-sale/rag-ledger/gemini-prod-ledger.json` | **`READY`** (Enforced by `GeminiRuntimeConfig`) |
| `GEMINI_API_KEY` / `RAG_EMBEDDING_API_KEY` | Valid Google AI Studio API key(s) | Valid Google AI Studio API key(s) | **`BLOCKED`** (Must be set in Render Secret Env Vars) |

---

## 8. Durable Indexing & Storage Assessment on Render

1. **Database Truth vs Ephemeral Filesystem**:
   - Canonical document metadata, extracted text (`raw_text`), revision snapshots (`knowledge_document_revisions`), chunks (`knowledge_chunks`), embeddings (`vector(768)`), and job leases (`knowledge_index_jobs`) are persisted in **Supabase PostgreSQL** and survive Render container restarts and redeployments even without a Render Persistent Disk.
   - Raw binary uploads (`RAG_UPLOAD_STORAGE_DIR`) and local JSON execution ledgers (`RAG_LEDGER_PATH`) reside on the container filesystem. If Render uses an ephemeral disk, raw uploaded files are lost on container redeploy unless a Render Persistent Disk is mounted at `/var/data/fpt-sale`.
2. **Stale Indexing Job Recovery (`lease_until`)**:
   - Migration `007` adds `lease_owner`, `lease_until`, `max_attempts`, and `last_heartbeat_at` to `public.knowledge_index_jobs`.
   - If a Render container restarts mid-indexing, the administrator can click **"Thu hồi Lease hết hạn"** (`action=recoverExpiredLeases`) on `/admin/knowledge-base?tab=indexing` to transition any expired `RUNNING` jobs to `FAILED` with a clear audit trail (`Recovered stale lease after container restart or timeout`) without calling Gemini automatically.

---

## 9. Ordered 19-Step Release Gate Sequence

Do **not** skip or reorder any step:

1. **Offline Build & Test Gate (`COMPLETED — PASS`)**: Build `dist/fpt-sale.war` (`5.3.0-online-rag`) and pass all offline unit, JSP, JSTL, Phase 3.16, Phase 4, and CMS E2E suites.
2. **Supabase Backup Gate**: Create a Supabase backup/snapshot before any database DDL.
3. **Supabase Read-Only Preflight Gate**: Execute [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) and review all 9 sections.
4. **Supabase Migration Execution Gate**: Apply only `MISSING` migrations (`002` through `007`) in strict order on port `5432`.
5. **Post-Migration Verification Gate**: Re-run [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) and confirm `001`–`007` report `ALREADY_APPLIED` and `rag.production_rag_enabled = 'false'`.
6. **Render Environment Configuration Gate**: Configure Render env vars with `RAG_EXECUTION_MODE=STAGING`, `RAG_ENABLED=false`, `RAG_DATABASE_APPROVED=true`, and valid `DB_*` + `GEMINI_API_KEY`.
7. **Deploy Build with `RAG_ENABLED=false` Gate**: Deploy `5.3.0-online-rag` to Render while public RAG remains disabled.
8. **Verify Public Website & Existing Chatbot Gate**: Confirm `/`, `/packages`, `/news`, `/contact`, and `/ai-chat` (business catalog consultation) operate normally.
9. **Verify Admin Login & Left Sidebar Gate**: Log in to `/admin/dashboard` and confirm the dark sticky left sidebar and active states work across desktop (`1440px`) and mobile (`375px`).
10. **Verify Admin Knowledge Base Status Panel Gate**: Open `/admin/knowledge-base` and `/admin/knowledge-base?tab=gemini-runtime` and confirm all 6 components and 5 readiness dimensions render without errors.
11. **Execute 1-Document Upload / Approval / Chunk Preview Gate**: Upload synthetic document `SMOKE-PHASE53-DOC-1`, preview chunks, and approve it (`APPROVED`, `is_active=true`, `index_status=PENDING`).
12. **Execute 4-Request Real Gemini Smoke Test Gate**: After explicit administrator sign-off (`approved=true`, `executionAuthorized=true` in the active smoke plan file), run the 4-request smoke test (`1` document embedding + `1` approved query embedding + `1` grounded chat response + `1` unrelated query embedding).
13. **Verify Embedding Storage (`vector(768)`) Gate**: Confirm `public.knowledge_chunks` stores a 768-dimension L2-normalized vector (`vector_dimensions = 768`).
14. **Verify Admin Retrieval Playground Gate**: Verify `SMOKE-PHASE53-Q1` returns `SMOKE-PHASE53-DOC-1` with citation and `SMOKE-PHASE53-Q2` returns no unverified commercial claims.
15. **Verify Audit Logs & Zero Secret Leakage Gate**: Inspect `/admin/knowledge-base?tab=audit` and Render logs to confirm `0` API keys, passwords, or raw vectors appear.
16. **Enable `RAG_ENABLED=true` Gate (Only After Explicit Sign-Off)**: Switch `RAG_EXECUTION_MODE=PRODUCTION`, `RAG_ENABLED=true`, and set `rag.production_rag_enabled='true'` in `/admin/knowledge-base?tab=gemini-runtime`.
17. **Verify Public Chatbot Grounded Response Gate**: Test 1 approved customer question on `/ai-chat` and verify grounded response and normal latency.
18. **Clean Up Synthetic Smoke Document Gate**: Set `SMOKE-PHASE53-DOC-1` to `ARCHIVED` / `is_active=false` so synthetic test data is excluded from customer retrieval.
19. **Post-Release Monitoring Gate**: Monitor Render logs, Supabase query metrics, and Gemini AI Studio quota dashboard for 24 hours.

---

## 10. Failure & Rollback Runbook

| Failure Scenario | Immediate Rollback / Mitigation Action | Impact on Public Website & Existing Chatbot |
|---|---|---|
| **1. Immediate RAG Disable (`< 60s` without redeploy)** | In `/admin/knowledge-base?tab=gemini-runtime`, set `production_rag_enabled = false` (or set `RAG_ENABLED=false` in Render Environment). | Public website and existing catalog/package chatbot continue working 100% normally; RAG retrieval is bypassed immediately. |
| **2. Gemini Quota Exhaustion (`HTTP 429`) or API Outage (`HTTP 5xx`)** | `GeminiEmbeddingProvider` and `GeminiConsultationProvider` perform `0` retries. Disable `production_rag_enabled = false` if sustained. | Customer chatbot falls back cleanly to deterministic catalog consultation or lexical retrieval without exposing internal diagnostics. |
| **3. Supabase Connection Pool Exhaustion or Timeout** | Switch `DB_URL` to Supabase Pooler (`:6543/...?sslmode=require&prepareThreshold=0`) and keep `RAG_ENABLED=false` until connection count stabilizes. | Retrieval timeout is capped and fails closed; existing pages recover immediately once pooler URL is applied. |
| **4. Bad Knowledge Document Published** | In `/admin/knowledge-base?tab=documents`, set the document status to `ARCHIVED` or `REJECTED` (or use Revision Rollback). | `RagConfig.recheckEligibilityBeforeAnswer()` immediately excludes withdrawn/archived documents before any customer answer is rendered. |
| **5. Stale Indexing Job (`RUNNING` past `lease_until`)** | In `/admin/knowledge-base?tab=indexing`, click **"Thu hồi Lease hết hạn"** (`action=recoverExpiredLeases`), then manually click **"Re-index"** when ready. | Marks expired jobs as `FAILED` safely with `0` automatic Gemini API calls. |
| **6. Full Render Application Rollback** | In Render Dashboard -> **Events / Deploys**, click **Rollback** to the previous known-good container image. Additive tables `002`–`007` are ignored by older code. | Restores prior application image in `< 2 minutes` without requiring destructive database `DROP TABLE` statements. |

---

## 11. Offline Regression Test Results (Phase 5.3 Execution Evidence)

| Test Suite | Command / Runner | Result | Details |
|---|---|---|---|
| **Dependency Lock SHA-256 Verification** | `./tools/bootstrap-libs.ps1 -VerifyOnly` | **PASS (`6/6`)** | All 6 pinned JAR checksums verified against `tools/dependencies.lock` |
| **Portable Ant Build & Dual WAR Packaging** | `ant -f build-portable.xml war test` | **PASS (`14/14 suites`)** | Built `dist/fpt-sale.war` & `.build/dist/fpt-sale-rag.war` (`5.3.0-online-rag`, Java `--release 8`) |
| **Admin KB & Phase 5.1/5.2/5.3 Unit Suite** | `rag.RagAdminKbTest` | **PASS (`22/22`)** | Includes Phase 5.3 smoke plan hashes, `executionAuthorized` gate, `BuildInfo 5.3.0-online-rag`, and migration `007` checks (`0` API calls) |
| **RAG Behavior & Security Suite** | `rag.RagBehaviorTest` | **PASS (`35/35`)** | Source policy, injection rejection, commercial guardrails (`0` API calls) |
| **RAG Phase 2 Corpus Suite** | `rag.RagPhase2CorpusTest` | **PASS (`13/13`)** | Offline corpus snapshots and conflict detection (`0` API calls) |
| **RAG Quality Suite** | `rag.RagQualityTest` | **PASS (`27/27`)** | Vietnamese routing, session isolation, authority resolution (`0` API calls) |
| **Phase 3.16 Offline Suite** | `./test/run-rag-phase316-offline.ps1` | **PASS (`53/53` + `22/22`)** | Vietnamese diacritics, section splitting, commercial statement filter (`0` API calls) |
| **Phase 4 Offline Live-Gate Suite** | `./test/run-rag-phase4-offline.ps1` | **PASS (`46/46`)** | Strict approval gates, zero-retry HTTP error handling, 768-dim L2 bounds (`0` API calls) |
| **Tomcat 9 JSP Compilation (`JspC`)** | `./test/compile-cms-jsps.ps1` | **PASS (`32/32 JSPs`)** | `0` compilation errors across all 32 Admin CMS & public JSPs |
| **JSTL Runtime Classloader Verification** | `./test/test-jstl-runtime.ps1` | **PASS (`6/6`)** | Single JSTL implementation, Vietnamese `fn:escapeXml` verified |
| **CMS Security, Behavior & Headless Browser E2E** | `./test/run-cms-tests.ps1 -Browser` | **PASS (`34` + `24` + `9` + `15 E2E`)** | RBAC, CSRF, Demo isolation, responsive `1440px`/`768px`/`375px` screenshots (`C:\Users\Caoky\AppData\Local\Temp\fpt-cms-qa`) |
| **Supabase Read-Only Preflight SQL Test** | `psql -X --set ON_ERROR_STOP=1 -f database/migrations/verify-supabase-readiness.sql` | **PASS (`Exit 0`)** | Verified against local PostgreSQL 18 pre-migration baseline (`001` applied, `002`–`007` missing) |
| **Docker Daemon Container Build** | `docker build` | **SKIPPED** | Local Docker Desktop Linux engine is not running (`//./pipe/dockerDesktopLinuxEngine` unavailable) |

---

## 12. Exact Administrator Action List & Final Go/No-Go Recommendation

### 12.1 Administrator Actions Required to Proceed to Live Execution
1. **Run Read-Only Supabase Preflight (Step 3)**:
   - Execute [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) against your Supabase database and share or review the output of Sections 2 and 4.
2. **Apply Missing Supabase Migrations (Step 4)**:
   - Take a Supabase backup, then run only the migrations reported as `MISSING` (`002` through `007`) in order on direct port `5432`, followed by re-running `verify-supabase-readiness.sql`.
3. **Configure Render Staging Environment Variables (Step 6)**:
   - Set `RAG_EXECUTION_MODE=STAGING`, `RAG_ENABLED=false`, `RAG_DATABASE_APPROVED=true`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `GEMINI_API_KEY` in Render.
4. **Authorize the 4-Request Smoke Test (Step 12)**:
   - When ready to spend up to 4 Free-Tier Gemini requests (`3` embeddings + `1` chat), authorize updating `approved=true` and `executionAuthorized=true` in the active smoke plan.

### 12.2 Final Go/No-Go Recommendation
- **Offline Engineering, Migration Package & Release Gates**: **`GO (READY)`**
- **Live Gemini Execution & Public Production RAG Activation**: **`NO-GO (BLOCKED BY REQUIRED ADMINISTRATOR PREFLIGHT & APPROVAL GATES)`**
