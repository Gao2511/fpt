# Phase 5.2 — Render + Supabase + Gemini Online Integration

- **Project**: `D:\miniproject\fpt` (`fpt.gialai.vn`)
- **Working Branch**: `feature/rag-integration`
- **Production Hosting**: Render (Docker Runtime, GitHub Auto-Deploy)
- **Production Database**: Supabase PostgreSQL (`pgvector`)
- **Application Stack**: Java Servlet / JSP / JDBC on Apache Tomcat 9 (JDK 17 compiling with `--release 8`)
- **Execution Mode During Phase 5.2**: Offline engineering & verification only (`0` real Gemini API calls, `0` Supabase production connections, `0` git pushes/deploys)

---

## 1. Executive Summary

Phase 5.2 resolves the root causes preventing `https://fpt.gialai.vn/` from deploying recent Admin CMS / Knowledge Base updates and prepares the application for real online operation on **Render + Supabase PostgreSQL + Gemini API** without relying on the developer's local machine:

1. **Fixed Stale Render Deployment Pipeline**: Replaced the pre-built WAR copy in [`Dockerfile`](file:///d:/miniproject/fpt/Dockerfile) with a deterministic multi-stage Docker source build (`eclipse-temurin:17-jdk` $\rightarrow$ `tools/bootstrap-libs.sh` $\rightarrow$ `ant -f build-portable.xml war` $\rightarrow$ `tomcat:9-jre17-temurin`) and synced [`build-portable.xml`](file:///d:/miniproject/fpt/build-portable.xml) so both `.build/dist/fpt-sale-rag.war` and `dist/fpt-sale.war` are always built from current source.
2. **Embedded Build Metadata Verification**: Added [`BuildInfo.java`](file:///d:/miniproject/fpt/src/java/utils/BuildInfo.java) and `WEB-INF/classes/build-info.properties` (generated during Ant build and augmented at runtime by `RENDER_GIT_COMMIT` / `RAG_ENVIRONMENT`), rendered in both the Admin Sidebar footer (`#adminBuildMeta`) and the Knowledge Base header (`#kbBuildMeta`).
3. **Real Online Runtime Configuration (`STAGING` / `PRODUCTION`)**: Extended [`GeminiRuntimeConfig`](file:///d:/miniproject/fpt/src/java/rag/runtime/GeminiRuntimeConfig.java), [`GeminiRuntime`](file:///d:/miniproject/fpt/src/java/rag/runtime/GeminiRuntime.java), [`RagFactory`](file:///d:/miniproject/fpt/src/java/rag/RagFactory.java), [`AIService`](file:///d:/miniproject/fpt/src/java/ai/AIService.java), and [`DBUtils`](file:///d:/miniproject/fpt/src/java/utils/DBUtils.java) so online environments reject loopback database hosts, `DB_SSLMODE=disable`, Windows filesystem paths, and `MockEmbeddingProvider` in customer retrieval while preserving 100% of `LOCAL_GEMINI` offline test guarantees.
4. **Idempotent Supabase Schema & Durable Storage**: Made [`006-rag-gemini-runtime.sql`](file:///d:/miniproject/fpt/database/migrations/006-rag-gemini-runtime.sql) idempotent, added [`007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql) (`public.rag_schema_migrations` tracking table, `knowledge_files.file_content BYTEA` up to 10 MB, `storage_backend`), and added read-only [`verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql).
5. **Durable Indexing & Expired Lease Recovery**: Added [`KnowledgeIndexJobDAO.recoverExpiredLeases`](file:///d:/miniproject/fpt/src/java/dao/KnowledgeIndexJobDAO.java#L173-L202) and the `action=recoverExpiredLeases` Admin endpoint to safely transition expired `running` leases after Render container restarts to `failed_retryable` (`error_code='LEASE_EXPIRED'`) with zero automatic duplicate Gemini API calls.
6. **7-Card Admin Online Status Panel & Controlled 4-Request Smoke Plan**: Upgraded `/admin/knowledge-base` (`#adminStatusPanel`) to report canonical statuses for **Gemini Chat**, **Gemini Embedding**, **Database**, **Knowledge Base**, **RAG**, **Indexing**, and **Backup Model**, and bundled [`config/phase52-online-smoke-plan.json`](file:///d:/miniproject/fpt/config/phase52-online-smoke-plan.json) (`approved=false`, `maxRequests=4`).

---

## 2. Root Cause Analysis of Stale Render Deployment

Inspection of [`Dockerfile`](file:///d:/miniproject/fpt/Dockerfile), [`build-portable.xml`](file:///d:/miniproject/fpt/build-portable.xml), [`nbproject/project.properties`](file:///d:/miniproject/fpt/nbproject/project.properties), and `git log` revealed two compounding issues:

| Finding | Evidence | Impact |
|---|---|---|
| **1. `Dockerfile` copied a static pre-built WAR from Git** | Previously, `Dockerfile` was a 4-line file executing `COPY dist/fpt-sale.war /usr/local/tomcat/webapps/ROOT.war` without compiling `src/java` or `web/`. | Whenever Render built the container image, it deployed whatever binary `dist/fpt-sale.war` was committed to Git, ignoring all Java/JSP source changes unless `dist/fpt-sale.war` was manually rebuilt and committed. |
| **2. `build-portable.xml` wrote only to `.build/dist/fpt-sale-rag.war`** | `build-portable.xml` output `.build/dist/fpt-sale-rag.war`, whereas `nbproject/project.properties` (`dist.war=dist/fpt-sale.war`) and `Dockerfile` referenced `dist/fpt-sale.war`. Furthermore, `git log -n 5 -- dist/fpt-sale.war` showed `dist/fpt-sale.war` was last committed in `19833a4` (2026-04-06) before Phases 2–5.1. | Even local portable Ant builds did not update `dist/fpt-sale.war`. |

---

## 3. Build & Deployment Pipeline Fix

1. **Multi-Stage Source Build in [`Dockerfile`](file:///d:/miniproject/fpt/Dockerfile)**:
   - **Stage 1 (`eclipse-temurin:17-jdk AS build`)**: Installs `ant` and `curl`, runs `sh tools/bootstrap-libs.sh` to verify/fetch runtime JARs (`postgresql-42.7.10.jar`, `angus-mail-2.0.3.jar`, `jakarta.activation-api-2.1.3.jar`, `jakarta.mail-api-2.1.3.jar`), and runs `ant -f build-portable.xml war -Dgit.commit=$RENDER_GIT_COMMIT -Drag.environment=$RAG_ENVIRONMENT`.
   - **Stage 2 (`tomcat:9-jre17-temurin`)**: Removes default Tomcat webapps and copies `/app/dist/fpt-sale.war` to `/usr/local/tomcat/webapps/ROOT.war`.
2. **Unified WAR Output in [`build-portable.xml`](file:///d:/miniproject/fpt/build-portable.xml)**:
   - Compiles with `javac --release 8 -encoding UTF-8`.
   - Packages `.build/dist/fpt-sale-rag.war` and copies the exact artifact to `dist/fpt-sale.war`.
3. **Clean Docker Context in [`.dockerignore`](file:///d:/miniproject/fpt/.dockerignore)**:
   - Excludes `.git`, `.build`, `build`, `dist`, `lib`, `test`, and `test-e2e` so Docker builds strictly from clean source and freshly bootstrapped runtime libraries.

---

## 4. Build Version Verification Design

- **Class**: [`utils.BuildInfo`](file:///d:/miniproject/fpt/src/java/utils/BuildInfo.java)
- **Generated Resource**: `WEB-INF/classes/build-info.properties` (written by `build-portable.xml` target `compile`)
- **Fields Exposed**:
  - `version`: `phase5.2`
  - `commit`: Short Git commit SHA (resolved at build time via `git rev-parse --short HEAD` or at runtime via `RENDER_GIT_COMMIT`)
  - `timestamp`: UTC ISO-8601 build timestamp
  - `environment`: Runtime environment (`RAG_ENVIRONMENT`, defaulting to `OFFLINE_MOCK`)
- **Admin UI Locations**:
  - Admin Left Sidebar footer ([`web/view/admin/admin-header.jsp`](file:///d:/miniproject/fpt/web/view/admin/admin-header.jsp#L67)): `<div class="admin-build-meta" id="adminBuildMeta">Build: phase5.2 @ <commit> · Env: <env></div>`
  - Knowledge Base header ([`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp#L22)): `<div id="kbBuildMeta">Build: <code>phase5.2</code> · Commit: <code><commit></code> · Built: <code><timestamp></code> · Env: <code><env></code></div>`

---

## 5. Environment Variable Design (Render + Local)

### Render Production / Staging Environment Variables

| Variable | Required | Example / Default in Render | Purpose |
|---|---|---|---|
| `RAG_ENVIRONMENT` | Yes | `STAGING` (for smoke test) or `PRODUCTION` | Selects online runtime enforcement rules |
| `DB_HOST` | Yes | `aws-0-ap-southeast-1.pooler.supabase.com` (or direct host) | Supabase PostgreSQL host (loopback rejected in online modes) |
| `DB_PORT` | Yes | `5432` (session pooler / direct) | Supabase PostgreSQL port |
| `DB_NAME` | Yes | `postgres` | Supabase database name |
| `DB_USER` | Yes | `postgres.<project-ref>` | Supabase database user |
| `DB_PASSWORD` | Yes (Secret) | `<supabase-db-password>` | Supabase database password (never logged) |
| `DB_SSLMODE` | Yes | `require` or `verify-full` | Enforced in `STAGING`/`PRODUCTION` (`disable` rejected) |
| `AI_ENCRYPTION_KEY` | Yes (Secret) | `<32+ char high-entropy secret>` | AES-GCM key for encrypting settings stored in DB |
| `GEMINI_API_KEY` | Yes (Secret) | `AIza...` | Server-side Gemini API key (or `GEMINI_CHAT_API_KEY` + `GEMINI_EMBEDDING_API_KEY`) |
| `GEMINI_CHAT_MODEL` | Optional | `gemini-3.8-flash` | Primary chat model (`thinkingLevel=medium`) |
| `GEMINI_BACKUP_MODEL` | Optional | `gemini-3.5-flash-lite` | Backup chat model (`thinkingLevel=low`) |
| `GEMINI_BACKUP_ENABLED` | Yes | `false` | Keeps backup model disabled during smoke validation |
| `GEMINI_EMBEDDING_MODEL` | Optional | `gemini-embedding-001` | Embedding model (`outputDimensionality=768`) |
| `RAG_ONLINE_GEMINI_AUTHORIZED` | Yes (Gate) | `false` (set `true` only during authorized smoke test) | Explicit online Gemini execution gate |
| `RAG_DATABASE_APPROVED` | Yes (Gate) | `false` (set `true` after `verify-supabase-readiness.sql`) | Confirms Supabase migrations `001`–`007` are verified |
| `RAG_ENABLED` | Yes (Gate) | `false` | Master customer-facing RAG switch (remains `false` until smoke test passes) |
| `KB_STORAGE_DIR` | Optional | `/tmp/fpt-kb-uploads` | Container filesystem cache path (PostgreSQL `BYTEA` is authoritative) |

### Runtime Mode Separation

| Mode (`RAG_ENVIRONMENT`) | Database Host Policy | SSL Policy | Gemini Execution Gate | Customer Retrieval Provider |
|---|---|---|---|---|
| `OFFLINE_MOCK` (Default) | Local / fixture allowed | Any | Blocked (`0` external calls) | Offline / Disabled unless local mock |
| `LOCAL_GEMINI` | Loopback (`127.0.0.1`/`localhost`) only; DB `fpt_rag_test` only | Local allowed | `RAG_LOCAL_GEMINI_AUTHORIZED=true` + `phase4-live-gemini-plan.json` | Isolated local test |
| `STAGING` | Non-loopback required (`127.*`/`localhost` rejected) | `require` or `verify-full` required | `RAG_ONLINE_GEMINI_AUTHORIZED=true` + `phase52-online-smoke-plan.json` (`approved=true`) | `GEMINI` only (`MOCK` never used) |
| `PRODUCTION` | Non-loopback required (`127.*`/`localhost` rejected) | `require` or `verify-full` required | `RAG_ONLINE_GEMINI_AUTHORIZED=true` + `phase52-online-smoke-plan.json` (`approved=true`) | `GEMINI` only + `RAG_ENABLED=true` + `production_rag_enabled=true` |

---

## 6. Gemini Model Verification (Official Docs)

Verified against official Google Gemini API documentation (`https://ai.google.dev/gemini-api/docs/models`, `https://ai.google.dev/gemini-api/docs/thinking`, `https://ai.google.dev/gemini-api/docs/embeddings`):

1. **Primary Chat Model (`gemini-3.8-flash`)**:
   - Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent`
   - Authentication: `x-goog-api-key` request header (key never placed in URL query string)
   - Thinking Config: `thinkingConfig.thinkingLevel = "medium"`
   - Structured Output: `responseMimeType = "application/json"` with strict `responseSchema` (`answer`, `used_chunk_ids`, `confidence`, `handoff_recommended`, `policy_Action`)
   - Output Cap: `maxOutputTokens = 768`
2. **Backup Chat Model (`gemini-3.5-flash-lite`)**:
   - Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent`
   - Thinking Config: `thinkingConfig.thinkingLevel = "low"`, `maxOutputTokens = 512`
   - Default Status: `DISABLED` (`backupEnabled = false`); maximum 1 primary attempt + 1 backup attempt when explicitly enabled; never retries HTTP `400`, `401`, `403`, or `429` on the same key.
3. **Embedding Model (`gemini-embedding-001`)**:
   - Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent`
   - Task Types: `RETRIEVAL_DOCUMENT` (with chunk title) for indexing; `RETRIEVAL_QUERY` for user query / playground retrieval
   - Output Dimensionality: Explicit `outputDimensionality = 768`
   - Post-Processing: Strict 768-dimension validation (`NaN`/`Infinity`/zero-norm rejection) followed by L2 normalization in [`GeminiEmbeddingProvider`](file:///d:/miniproject/fpt/src/java/rag/GeminiEmbeddingProvider.java) before `pgvector` storage or cosine similarity search.

---

## 7. Supabase Production Database Audit & Migration Plan

### Migration Audit (`001` – `007`)

| Migration File | Purpose | Idempotency & Supabase Readiness |
|---|---|---|
| [`001-rag-pgvector.sql`](file:///d:/miniproject/fpt/database/migrations/001-rag-pgvector.sql) | Enables `vector` extension (`pgvector`), creates `rag_documents`, `rag_chunks` (`vector(768)`), HNSW cosine index, and FTS index | Uses `CREATE EXTENSION IF NOT EXISTS vector`, `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`. Ready. |
| [`002-cms-admin-enhancements.sql`](file:///d:/miniproject/fpt/database/migrations/002-cms-admin-enhancements.sql) | CMS revisions, media assets, and prompt history | Uses `IF NOT EXISTS` guards. Ready. |
| [`003-installation-fee-setting.sql`](file:///d:/miniproject/fpt/database/migrations/003-installation-fee-setting.sql) | Seeds `business.standardFee` | Uses `ON CONFLICT DO NOTHING`. Ready. |
| [`004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql) | Creates `knowledge_files`, `knowledge_document_versions`, `knowledge_review_records`, `knowledge_faq_entries`, `knowledge_index_jobs`, `knowledge_audit_logs`, and `rag_runtime_settings` | Uses `CREATE TABLE IF NOT EXISTS` and `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`. Ready. |
| [`005-admin-kb-hardening.sql`](file:///d:/miniproject/fpt/database/migrations/005-admin-kb-hardening.sql) | Adds lease locking columns (`lease_owner`, `lease_until`, `idempotency_key`), unique active job index, and audit indexes | Uses `ADD COLUMN IF NOT EXISTS` and `DO $$` constraint checks. Ready. |
| [`006-rag-gemini-runtime.sql`](file:///d:/miniproject/fpt/database/migrations/006-rag-gemini-runtime.sql) | Adds `embedding_provider`, `embedding_model`, `embedding_dimension`, `content_hash`, `rag_gemini_execution_ledger`, and `rag_embedding_cache` | **Updated in Phase 5.2** to use `ADD COLUMN IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS`, and `CREATE TABLE IF NOT EXISTS` for full idempotency. |
| [`007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql) | **New in Phase 5.2**: Creates `public.rag_schema_migrations`, adds durable `file_content BYTEA` and `storage_backend` (`<= 10 MB` check constraint) to `knowledge_files`, adds lease recovery index, and seeds safe default `rag_runtime_settings` (`production_rag_enabled = 'false'`) | Fully idempotent (`IF NOT EXISTS`, `ON CONFLICT DO UPDATE`). |

### Verification Script
- [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) performs a 100% read-only check of `pgvector`, all 10 required tables, provider isolation columns, durable `BYTEA` file columns, `rag_schema_migrations`, safety gates (`production_rag_enabled = 'false'`), and active/expired indexing leases.

---

## 8. Online Gemini Chat & Embedding Architecture

- **Configuration Loader**: [`GeminiRuntimeConfig.online(env, plan)`](file:///d:/miniproject/fpt/src/java/rag/runtime/GeminiRuntimeConfig.java#L168-L253) and [`GeminiRuntime.active()`](file:///d:/miniproject/fpt/src/java/rag/runtime/GeminiRuntime.java#L29-L41).
- **Provider Isolation**:
  - Every chunk indexed with Gemini is tagged with `embedding_provider = 'GEMINI'`, `embedding_model = 'gemini-embedding-001'`, and `embedding_dimension = 768`.
  - [`PostgresRagRepository`](file:///d:/miniproject/fpt/src/java/rag/PostgresRagRepository.java) filters vector similarity queries strictly by matching `embedding_provider`, `embedding_model`, and `embedding_dimension`, guaranteeing `MOCK` vectors can never mix with `GEMINI` vectors.
  - Content-addressed caching in `rag_embedding_cache` (`content_hash + task_type + model + dimension`) prevents duplicate `embedContent` calls when re-indexing unchanged chunks.

---

## 9. RAG Indexing & Lease Recovery on Render

Because Render containers can restart or redeploy while an indexing job is in progress:
1. **Lease Acquisition**: [`KnowledgeIndexJobDAO.acquireNextQueuedJob`](file:///d:/miniproject/fpt/src/java/dao/KnowledgeIndexJobDAO.java#L85-L120) claims a `queued` job with a 300-second database lease (`lease_owner`, `lease_until = CURRENT_TIMESTAMP + INTERVAL '300 seconds'`).
2. **Expired Lease Recovery**: [`KnowledgeIndexJobDAO.recoverExpiredLeases(adminEmail)`](file:///d:/miniproject/fpt/src/java/dao/KnowledgeIndexJobDAO.java#L173-L202) finds any job stuck in `status = 'running'` where `lease_until < CURRENT_TIMESTAMP` and atomically transitions it to:
   - `status = 'failed_retryable'`
   - `error_code = 'LEASE_EXPIRED'`
   - `lease_owner = NULL`, `lease_until = NULL`
   - Writes an immutable audit entry (`RECOVER_EXPIRED_LEASE`) to `knowledge_audit_logs`.
3. **Zero Automatic Re-Bill**: Expired lease recovery **never** automatically triggers Gemini API calls; an Administrator must explicitly review and re-trigger the job from `/admin/knowledge-base`.

---

## 10. Production File Storage Design

Render's container filesystem is ephemeral across deploys. Phase 5.2 implements a **Hybrid Durable Database + Ephemeral Cache** storage model:
- **Database Authoritative Storage (`knowledge_files.file_content BYTEA`)**:
  - [`DocumentExtractionService.extract`](file:///d:/miniproject/fpt/src/java/rag/extract/DocumentExtractionService.java) validates file magic bytes, enforces the 10 MB size cap, extracts normalized UTF-8 text, and retains `rawBytes` (`storageBackend = 'HYBRID'`).
  - [`AdminKnowledgeDAO.createUploadedDocument`](file:///d:/miniproject/fpt/src/java/dao/AdminKnowledgeDAO.java) persists `file_content BYTEA` and `storage_backend` directly in PostgreSQL alongside the SHA-256 checksum.
- **Container Cache (`KB_STORAGE_DIR`)**:
  - In `STAGING`/`PRODUCTION`, `KB_STORAGE_DIR` (or `/tmp/fpt-kb-uploads`) is used only as a local cache; Windows drive paths (`C:\...`) are rejected in online modes.
- **Download Original (`?action=downloadOriginal&docId=...`)**:
  - [`AdminKnowledgeBaseServlet`](file:///d:/miniproject/fpt/src/java/controller/AdminKnowledgeBaseServlet.java#L162-L185) serves bytes directly from `knowledge_files.file_content` via [`AdminKnowledgeDAO.loadUploadedFileBytes`](file:///d:/miniproject/fpt/src/java/dao/AdminKnowledgeDAO.java) and falls back to disk only for legacy local files.

---

## 11. Admin Online Status Panel

The `/admin/knowledge-base` Runtime Status Panel (`#adminStatusPanel` in [`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp#L34-L71)) displays 7 component cards plus build metadata:

| Component Card | Status Attribute | Possible Canonical Values | Detail Shown |
|---|---|---|---|
| **1. Gemini Chat** | `geminiChatStatus` | `READY`, `MISSING_KEY`, `QUOTA_BLOCKED`, `DISABLED`, `OFFLINE_MOCK` | Model (`gemini-3.8-flash`), `thinking=medium`, key source |
| **2. Gemini Embedding** | `geminiEmbeddingStatus` | `READY`, `MISSING_KEY`, `QUOTA_BLOCKED`, `DISABLED`, `OFFLINE_MOCK` | Model (`gemini-embedding-001`), `dim=768 (L2)`, key source |
| **3. Database** | `databaseStatus` | `READY`, `MIGRATION_REQUIRED`, `DISCONNECTED`, `DEMO_FIXTURE` | Schema version (`007`), `pgvector` status, environment |
| **4. Knowledge Base** | `kbComponentStatus` | `READY`, `EMPTY`, `MIGRATION_REQUIRED`, `DEMO_MODE` | Approved/active docs, `GEMINI` chunks vs `MOCK` chunks |
| **5. RAG** | `ragComponentStatus` | `READY`, `DISABLED`, `WAITING_APPROVAL`, `OFFLINE_MOCK` | `RAG_ENABLED`, `production_rag_enabled`, `Active: false` |
| **6. Indexing** | `indexingComponentStatus` | `READY`, `LEASE_EXPIRED`, `QUOTA_BLOCKED`, `DISABLED` | Queued / Running / Expired Lease counts + Recovery button |
| **7. Backup Model** | `backupComponentStatus` | `READY`, `DISABLED`, `MISSING_KEY` | `gemini-3.5-flash-lite` (`thinking=low`, default `DISABLED`) |

---

## 12. Security & Quota Controls

- **Zero Client-Side Secret Exposure**: API keys (`GEMINI_API_KEY`, `GEMINI_CHAT_API_KEY`, `GEMINI_EMBEDDING_API_KEY`), `DB_PASSWORD`, and `AI_ENCRYPTION_KEY` are read exclusively on the server and never rendered in HTML/JSP/JSON or exception messages.
- **Header-Only Gemini Auth**: All Gemini REST requests send `x-goog-api-key` in HTTP headers; URLs never contain `?key=`.
- **CSRF + Admin Session Enforcement**: Every mutating POST action on `/admin/knowledge-base` (including `recoverExpiredLeases`, `indexDocument`, `approveVersion`, `updateSettings`) requires an authenticated non-demo Admin session and a valid `X-CSRF-Token` / `csrf_token`.
- **Fail-Closed Quota & Retry Policy**:
  - `connectTimeout = 5000ms`, `readTimeout = 20000ms`, `followRedirects = false`, `automaticRetries = 0`.
  - Every request reserves a slot in `rag_gemini_execution_ledger` **before** opening an HTTP connection.
  - HTTP `400`, `401`, `403`, `429`, `500`, and network timeouts consume the ledger slot and never auto-retry.

---

## 13. Offline Verification Test Results

All verification suites were executed offline with **`0` real Gemini API calls** and **`0` Supabase production connections**:

| Verification Suite | Command | Result |
|---|---|---|
| **Portable Ant Build & Unit/Integration Suite (14 suites)** | `ant -f build-portable.xml war test` | **PASS** (`BUILD SUCCESSFUL`; `dist/fpt-sale.war` and `.build/dist/fpt-sale-rag.war` synced) |
| **Phase 5.2 Admin KB & Online Readiness Tests** | `rag.RagAdminKbTest` | **PASS (`21/21` checks)** — covers `BuildInfo`, `DBUtils.validateOnlineDatabase`, `GeminiRuntimeConfig.online`, durable `BYTEA` file storage, `recoverExpiredLeases`, and 7-card status panel |
| **Phase 4 Offline Gemini Guardrails** | `./test/run-rag-phase4-offline.ps1` | **PASS (`46/46` checks; `real API requests=0`)** |
| **Jasper JSP Compilation (All 32 JSPs)** | `./test/compile-cms-jsps.ps1` | **PASS (`32/32` JSPs compiled with `0` errors)** |
| **JSTL Classloader Isolation Runtime Check** | `./test/test-jstl-runtime.ps1` | **PASS (`6/6` checks)** |
| **CMS Security, Behavior & Playwright Browser E2E** | `./test/run-cms-tests.ps1 -Browser` | **PASS** (isolated JDBC fixtures + headless Chromium at `1440px`, `768px`, `375px`) |

---

## 14. Controlled 4-Request Online Smoke Test Plan

Defined in [`config/phase52-online-smoke-plan.json`](file:///d:/miniproject/fpt/config/phase52-online-smoke-plan.json) (and bundled at `WEB-INF/classes/phase52-online-smoke-plan.json`):
- **Default State**: `"approved": false`, `"executionAuthorized": false`
- **Hard Request Budget (`maxRequests = 4`)**:
  1. **`1` Document Embedding (`RETRIEVAL_DOCUMENT`)**: Index a single short approved FAQ/KB document (`<= 1 chunk`, `<= 1,200 chars`) via `/admin/knowledge-base`.
  2. **`2` Query Embeddings (`RETRIEVAL_QUERY`)**:
     - Smoke Query 1: Admin Retrieval Playground query matching the indexed FAQ/KB chunk.
     - Smoke Query 2: Grounded Chat retrieval query embedding.
  3. **`1` Chat Generation (`generateContent` on `gemini-3.8-flash`)**: Single grounded response using the retrieved chunk with `thinkingLevel=medium` and `backupEnabled=false`.
- **Stop Conditions**: Immediately abort and keep `RAG_ENABLED=false` if any request returns HTTP `400`/`401`/`403`/`429`/`5xx`, if ledger count reaches `4`, or if vector dimension is not `768`.

---

## 15. Step-by-Step Admin Runbook for Render + Supabase

### Step 1 — Run Read-Only Preflight on Supabase
1. Open the Supabase SQL Editor for the production/staging project.
2. Execute [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) and inspect the current state of `001`–`007`.

### Step 2 — Apply Database Migrations on Supabase (After Backup/Snapshot)
1. Ensure any missing migrations from `001` through `006` are applied in order.
2. Execute [`database/migrations/007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql).
3. Re-run [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) and confirm:
   - `vector_extension_installed = true`
   - All 10 tables report `true`
   - `rag_schema_migrations` shows versions `001`–`007`
   - `production_rag_enabled = 'false'`

### Step 3 — Configure Render Environment Variables
In the Render Dashboard $\rightarrow$ Web Service $\rightarrow$ **Environment**, set:
- `RAG_ENVIRONMENT=STAGING`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE=require`
- `AI_ENCRYPTION_KEY=<secret>`
- `GEMINI_API_KEY=<secret>`
- `GEMINI_BACKUP_ENABLED=false`
- `RAG_DATABASE_APPROVED=true`
- `RAG_ONLINE_GEMINI_AUTHORIZED=false` (keep `false` for initial deploy verification)
- `RAG_ENABLED=false`

### Step 4 — Deploy & Verify Build Metadata + Admin Status Panel (0 Gemini Calls)
1. Merge/push the approved branch to trigger Render Docker build.
2. Log in to `https://fpt.gialai.vn/admin/knowledge-base`.
3. Verify `#adminBuildMeta` and `#kbBuildMeta` show `Build: phase5.2` and the latest commit SHA.
4. Verify `#adminStatusPanel` shows `Database: READY`, `Knowledge Base: READY` (or `EMPTY`), `RAG: DISABLED`, and `Backup Model: DISABLED`.

### Step 5 — Execute Controlled 4-Request Online Smoke Test (Only When Authorized)
1. Authorize [`config/phase52-online-smoke-plan.json`](file:///d:/miniproject/fpt/config/phase52-online-smoke-plan.json) (`approved=true`, `executionAuthorized=true`, `approvedBy`, `approvedAt`, `expiresAt`) and set `RAG_ONLINE_GEMINI_AUTHORIZED=true` in Render.
2. In `/admin/knowledge-base`, index 1 single-chunk approved FAQ/KB entry (**Request 1/4**).
3. In Retrieval Playground, run 1 test query (**Request 2/4**) and verify the `GEMINI` chunk is retrieved with citation metadata.
4. Run 1 grounded chat consultation test (**Request 3/4** query embedding + **Request 4/4** `gemini-3.8-flash` chat generation).
5. Confirm `rag_gemini_execution_ledger` recorded `<= 4` requests and all succeeded (`HTTP 200`).

---

## 16. Rollback Plan

1. **Immediate Runtime Kill-Switch (No Redeploy Needed)**:
   - In `/admin/knowledge-base`, toggle `production_rag_enabled` to `false` (takes effect immediately in PostgreSQL), OR
   - In Render Environment, set `RAG_ENABLED=false` and `RAG_ONLINE_GEMINI_AUTHORIZED=false`.
2. **Application Rollback on Render**:
   - Use Render Dashboard $\rightarrow$ **Deploys** $\rightarrow$ **Rollback** to the previous known-good image.
3. **Database Rollback Safety**:
   - Migration `007` is strictly additive (`CREATE TABLE IF NOT EXISTS rag_schema_migrations`, `ADD COLUMN IF NOT EXISTS file_content`, `ADD COLUMN IF NOT EXISTS storage_backend`) and does not break older application builds.

---

## 17. Final GO / NO-GO Recommendation for Online Smoke Test

- **Engineering & Offline Readiness**: **GO (100% Complete — 21/21 Phase 5.2 KB checks, 46/46 Phase 4 guardrails, 32/32 JSPs, 14/14 Ant suites PASS)**
- **Live Gemini API / Production RAG Activation Status**: **CONTROLLED NO-GO UNTIL ADMIN EXECUTES RUNBOOK STEPS 1–5**
  - `RAG_ENABLED` remains `false`
  - `production_rag_enabled` remains `false`
  - `config/phase52-online-smoke-plan.json` remains `approved=false`, `executionAuthorized=false`
  - Real Gemini API requests made during Phase 5.2: **`0`**
