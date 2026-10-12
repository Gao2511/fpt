# Phase 5.4 — Supabase Production RAG Database Migration Readiness

- **Project**: `D:\miniproject\fpt`
- **Branch**: `feature/rag-integration`
- **Production Website**: `https://fpt.gialai.vn/`
- **Database**: Supabase PostgreSQL
- **Hosting**: Render
- **Production Database Modifications Executed by Agent**: `0`
- **Real Gemini API Calls Executed**: `0`
- **Production RAG Status (`RAG_ENABLED`)**: `false` (Locked Disabled)

---

## 1. Verified Production Findings & Repository Audit

### 1.1 Analysis of Administrator's Live Supabase Findings

| Finding | Administrator Query Result | Technical Interpretation & Impact |
|---|---|---|
| **Finding 1** | Querying `knowledge_%` and `rag_%` tables in schema `public` returned **`0` rows**. | None of the RAG or Admin Knowledge Base tables (`knowledge_documents`, `knowledge_chunks`, `knowledge_index_jobs`, `knowledge_document_revisions`, `knowledge_document_reviews`, `knowledge_files`, `knowledge_faqs`, `knowledge_faq_revisions`, `knowledge_import_sections`, `rag_runtime_plans`, `rag_api_requests`, `rag_schema_migrations`) exist in `public` yet. Migrations **`002` through `007` are `MISSING`** on production. |
| **Finding 2** | `pg_available_extensions`: `name = 'vector'`, `installed_version = NULL`, `default_version = '0.8.2'`. | Supabase production supports **pgvector `0.8.2`** natively, which matches the exact version (`0.8.2`) validated in the repository's offline PostgreSQL test suite. Because `installed_version IS NULL`, **`pgvector` must be installed first** before running `002-rag-mvp.sql` (which enforces `IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN RAISE EXCEPTION`). |
| **Unchecked Non-`knowledge_%` Objects** | `cms_media`, `cms_revisions` (from `001-cms-media-revisions.sql`) and `admin_audit_logs` (from `004-admin-knowledge-base.sql`) do not start with `knowledge_` or `rag_`. | Both `001-cms-media-revisions.sql` and `004-admin-knowledge-base.sql` use `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`, so they will never fail with duplicate-table errors. However, **Part A (Read-Only Preflight)** explicitly checks whether `cms_media`, `cms_revisions`, and `admin_audit_logs` already exist before any migration is run. |

---

## 2. Repository Migration & SQL Dependency Audit (`001` – `007`)

### 2.1 Complete Table, Schema, Vector Type & Dependency Matrix

| Migration | File | Tables / Columns Created or Altered (All in `public`) | Depends On | Idempotent (`IF NOT EXISTS`)? | Touches Existing Business Data? |
|---|---|---|---|---|---|
| **`001`** | [`database/migrations/001-cms-media-revisions.sql`](file:///d:/miniproject/fpt/database/migrations/001-cms-media-revisions.sql) | Creates `public.cms_media` (`UUID`, `BYTEA <= 2MB`) and `public.cms_revisions` (`BIGSERIAL`, `JSONB`) + index `cms_revisions_kind_id`. | Base schema | **Yes** (`CREATE TABLE IF NOT EXISTS`) | **No** (`0` rows modified) |
| **`002`** | [`database/migrations/002-rag-mvp.sql`](file:///d:/miniproject/fpt/database/migrations/002-rag-mvp.sql) | Requires `extname = 'vector'`. Creates `public.knowledge_documents`, `public.knowledge_chunks` (`embedding vector(768) NOT NULL`, `search_vector TSVECTOR GENERATED ALWAYS`), `public.knowledge_index_jobs`, GIN index `knowledge_chunks_fts`, B-tree indexes, RLS, and `REVOKE` from `PUBLIC`/`anon`/`authenticated`. | `pgvector` extension | **No** (bare `CREATE TABLE`) — safe because Finding 1 confirmed `0` `knowledge_%` tables exist | **No** (`0` rows modified) |
| **`003`** | [`database/migrations/003-rag-source-policy.sql`](file:///d:/miniproject/fpt/database/migrations/003-rag-source-policy.sql) | Alters `public.knowledge_documents`: adds `policy_key`, `source_authority`, `supersedes_document_id`, check constraints, and indexes `knowledge_documents_policy`, `knowledge_documents_supersedes`. | `002` | **No** (bare `ADD COLUMN`) — run once after `002` | **No** (`0` rows modified) |
| **`004`** | [`database/migrations/004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql) | Alters `public.knowledge_documents` (`ADD COLUMN IF NOT EXISTS` `workflow_status`, `source_type`, `tags`, `department`, `created_by`, `updated_by`, `reviewed_by`, `reviewed_at`, `rejection_reason`, `chunk_size`, `chunk_overlap`). Creates `public.knowledge_document_revisions`, `public.knowledge_document_reviews`, `public.knowledge_files`, `public.knowledge_faqs`, `public.knowledge_faq_revisions`, `public.admin_audit_logs` + RLS + `REVOKE`. | `002`, `003` | **Yes** (`IF NOT EXISTS` throughout) | **No** (`UPDATE` only touches `public.knowledge_documents`, which is empty) |
| **`005`** | [`database/migrations/005-knowledge-section-import.sql`](file:///d:/miniproject/fpt/database/migrations/005-knowledge-section-import.sql) | Creates `public.knowledge_import_sections` (FKs to `knowledge_documents`, `knowledge_faqs`, and `knowledge_document_revisions`), enables RLS + `REVOKE`, and creates trigger `guard_knowledge_import_original` on `public.knowledge_documents`. | `002`, `003`, `004` | **No** (bare `CREATE TABLE` / `CREATE TRIGGER`) — run once after `004` | **No** (`0` rows modified) |
| **`006`** | [`database/migrations/006-rag-gemini-runtime.sql`](file:///d:/miniproject/fpt/database/migrations/006-rag-gemini-runtime.sql) | Alters `knowledge_documents`, `knowledge_chunks`, `knowledge_index_jobs` (`ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(8) DEFAULT 'MOCK'`, `last_indexed_at`, `embedding_error`, widens `indexing_status` and `status` constraints). Creates `public.rag_runtime_plans` and `public.rag_api_requests` + RLS + `REVOKE`. | `002`, `004` | **Yes** (`IF NOT EXISTS` / `DROP CONSTRAINT IF EXISTS`) | **No** (`0` rows modified) |
| **`007`** | [`database/migrations/007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql) | Verifies `pgvector` + `002`/`004`/`006`. Creates `public.rag_schema_migrations` + RLS + `REVOKE`. Alters `public.knowledge_files` (`ADD COLUMN IF NOT EXISTS file_content BYTEA`, `storage_backend VARCHAR(16) DEFAULT 'DISK'`). Creates `idx_knowledge_index_jobs_running_lease`. Upserts `rag.production_rag_enabled = 'false'` in `public.settings`. Populates `rag_schema_migrations` (`001`–`007`). | `pgvector`, `001`–`006` | **Yes** (`IF NOT EXISTS` + `ON CONFLICT`) | **Only** upserts `setting_key = 'rag.production_rag_enabled'` (`'false'`) in `public.settings` |

---

## 3. Compatibility Verification: Supabase PostgreSQL, `pgvector 0.8.2`, RLS & JDBC

### 3.1 `pgvector 0.8.2` & Schema Resolution (`extensions` vs `public`)
1. **Extension Schema Best Practice on Supabase**:
   - Supabase recommends installing extensions into the `extensions` schema (`CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions;`).
2. **Alignment with Migrations (`002` & `007`)**:
   - Both [`002-rag-mvp.sql`](file:///d:/miniproject/fpt/database/migrations/002-rag-mvp.sql#L4-L9) and [`007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql#L5-L11) explicitly execute:
     ```sql
     SET LOCAL search_path = public, extensions, pg_catalog;
     ```
     and verify `SELECT 1 FROM pg_extension WHERE extname = 'vector'` (which is schema-agnostic). Thus `embedding vector(768) NOT NULL` in `002-rag-mvp.sql` resolves `extensions.vector(768)` automatically.
3. **Alignment with Java JDBC Queries**:
   - [`RagDatabase.java`](file:///d:/miniproject/fpt/src/java/rag/RagDatabase.java#L10-L16) sets `search_path` on every opened connection before any query runs:
     ```java
     s.execute("SET search_path=public,extensions,pg_catalog");
     s.execute("SET statement_timeout='3000ms'");
     ```
   - [`KnowledgeChunkDAO.java`](file:///d:/miniproject/fpt/src/java/dao/KnowledgeChunkDAO.java#L21-L44) uses:
     - `SELECT embedding::text FROM public.knowledge_chunks ...`
     - `INSERT INTO public.knowledge_chunks (...) VALUES (..., CAST(? AS vector), ...)`
     - `1-(k.embedding <=> CAST(? AS vector))`
   - Because `extensions` is in `search_path` on every `RagDatabase` connection (and in Supabase's default role `search_path`), `CAST(? AS vector)` and the `<=>` cosine distance operator resolve to `extensions.vector` and `extensions.<=>` with zero errors on `pgvector 0.8.2`.
   - [`AdminKnowledgeDAO.java`](file:///d:/miniproject/fpt/src/java/dao/AdminKnowledgeDAO.java#L1579) checks `SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='vector')`, which returns `TRUE` whether `vector` is in `extensions` or `public`.

### 3.2 Protection of Existing Production Tables (`customers`, `packages`, `users`, `settings`)
- **`public.customers`**: `0` DDL or DML statements across `001`–`007`.
- **`public.packages`**: `0` DDL or DML statements across `001`–`007`.
- **`public.users`**: `0` DDL or DML statements across `001`–`007`.
- **`public.email_logs`, `public.api_key_history`, `public.password_reset_token`**: `0` DDL or DML statements across `001`–`007`.
- **`public.settings`**: Existing keys (`cms.*`, `ai_*`, `gemini_*`, etc.) are **never modified or deleted**. Only `007-online-production-readiness.sql` inserts/updates a single safety gate row:
  `setting_key = 'rag.production_rag_enabled'`, `setting_value = 'false'`.

### 3.3 RLS, `GRANT`/`REVOKE`, and JDBC Role Permissions
- **Supabase Public REST API (`anon` and `authenticated` roles)**:
  - All 13 tables created in `002`–`007` have `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` with **zero** permissive policies for `anon` or `authenticated`, plus explicit `REVOKE ALL ... FROM PUBLIC, anon, authenticated`.
  - Consequently, none of the Knowledge Base documents, chunks, uploaded files, audit logs, or Gemini API request ledgers are accessible via the public Supabase PostgREST URL/anon key.
- **Java Backend JDBC Role (`postgres` / `DB_USER`)**:
  - When executing migrations in the Supabase SQL Editor (which runs as `postgres`) and connecting from Render Tomcat via `DB_USER=postgres` (or `postgres.<project_ref>`), the role is the table owner and has `rolbypassrls = true` (`BYPASSRLS`), so JDBC queries have full `SELECT, INSERT, UPDATE, DELETE` access on `public.knowledge_%` and `public.rag_%` without needing permissive RLS policies.

---

# PART A — READ-ONLY PREFLIGHT (SAFE TO RUN IMMEDIATELY)

> [!NOTE]
> The SQL below is **100% read-only (`SELECT` only)**. It performs **zero** DDL/DML changes, exposes **zero** customer PII or secret values, and can be pasted and run immediately in the **Supabase SQL Editor** to confirm the exact pre-migration baseline.

### A.1 Copy-Paste Read-Only Preflight SQL for Supabase SQL Editor

Run the following script in **Supabase Dashboard → SQL Editor → New query**:

```sql
-- =============================================================================
-- PHASE 5.4 — PART A: 100% READ-ONLY PRODUCTION PREFLIGHT
-- Safe to run immediately in Supabase SQL Editor. Zero writes, zero secrets.
-- =============================================================================

-- Query A1: PostgreSQL Version, Connected Role Capabilities, search_path & pgvector Status
SELECT
    version() AS postgres_version,
    current_database() AS database_name,
    current_user AS connected_role,
    (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) AS is_superuser,
    (SELECT rolbypassrls FROM pg_roles WHERE rolname = current_user) AS bypasses_rls,
    current_setting('search_path') AS current_search_path,
    (SELECT default_version FROM pg_available_extensions WHERE name = 'vector') AS vector_available_version,
    (SELECT installed_version FROM pg_available_extensions WHERE name = 'vector') AS vector_installed_version,
    (SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace WHERE e.extname = 'vector') AS vector_schema,
    EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'extensions') AS extensions_schema_exists;

-- Query A2: Pre-Migration Inventory of Existing Base Tables + 001..007 Target Tables
SELECT
    expected.phase,
    expected.table_name,
    (c.oid IS NOT NULL) AS exists_in_public,
    COALESCE(c.relrowsecurity, FALSE) AS rls_enabled,
    CASE
        WHEN c.oid IS NOT NULL THEN COALESCE(s.n_live_tup, 0)
        ELSE 0
    END AS approx_row_count
FROM (
    VALUES
        ('BASE', 'users'),
        ('BASE', 'packages'),
        ('BASE', 'settings'),
        ('BASE', 'customers'),
        ('BASE', 'email_logs'),
        ('BASE', 'api_key_history'),
        ('BASE', 'password_reset_token'),
        ('001',  'cms_media'),
        ('001',  'cms_revisions'),
        ('002',  'knowledge_documents'),
        ('002',  'knowledge_chunks'),
        ('002',  'knowledge_index_jobs'),
        ('004',  'knowledge_document_revisions'),
        ('004',  'knowledge_document_reviews'),
        ('004',  'knowledge_files'),
        ('004',  'knowledge_faqs'),
        ('004',  'knowledge_faq_revisions'),
        ('004',  'admin_audit_logs'),
        ('005',  'knowledge_import_sections'),
        ('006',  'rag_runtime_plans'),
        ('006',  'rag_api_requests'),
        ('007',  'rag_schema_migrations')
) AS expected(phase, table_name)
LEFT JOIN pg_namespace n ON n.nspname = 'public'
LEFT JOIN pg_class c ON c.relnamespace = n.oid AND c.relname = expected.table_name AND c.relkind = 'r'
LEFT JOIN pg_stat_user_tables s ON s.relid = c.oid
ORDER BY expected.phase, expected.table_name;

-- Query A3: Exact Migration 001..007 Readiness Classification
SELECT
    m.migration_id,
    m.filename,
    CASE
        WHEN m.all_present THEN 'ALREADY_APPLIED'
        WHEN NOT m.any_present THEN 'MISSING'
        ELSE 'PARTIALLY_APPLIED_REQUIRES_REVIEW'
    END AS migration_status,
    m.action_required
FROM (
    VALUES
        (
            '001', '001-cms-media-revisions.sql',
            to_regclass('public.cms_media') IS NOT NULL AND to_regclass('public.cms_revisions') IS NOT NULL,
            to_regclass('public.cms_media') IS NOT NULL OR to_regclass('public.cms_revisions') IS NOT NULL,
            'Idempotent (IF NOT EXISTS). Run only if MISSING.'
        ),
        (
            '002', '002-rag-mvp.sql',
            to_regclass('public.knowledge_documents') IS NOT NULL
                AND to_regclass('public.knowledge_chunks') IS NOT NULL
                AND to_regclass('public.knowledge_index_jobs') IS NOT NULL,
            to_regclass('public.knowledge_documents') IS NOT NULL
                OR to_regclass('public.knowledge_chunks') IS NOT NULL
                OR to_regclass('public.knowledge_index_jobs') IS NOT NULL,
            'Non-idempotent CREATE TABLE. Run AFTER installing pgvector.'
        ),
        (
            '003', '003-rag-source-policy.sql',
            EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'policy_key')
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'source_authority')
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'supersedes_document_id'),
            EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name IN ('policy_key', 'source_authority', 'supersedes_document_id')),
            'Non-idempotent ADD COLUMN. Run immediately after 002.'
        ),
        (
            '004', '004-admin-knowledge-base.sql',
            to_regclass('public.knowledge_document_revisions') IS NOT NULL
                AND to_regclass('public.knowledge_document_reviews') IS NOT NULL
                AND to_regclass('public.knowledge_files') IS NOT NULL
                AND to_regclass('public.knowledge_faqs') IS NOT NULL
                AND to_regclass('public.knowledge_faq_revisions') IS NOT NULL
                AND to_regclass('public.admin_audit_logs') IS NOT NULL
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'workflow_status'),
            to_regclass('public.knowledge_files') IS NOT NULL
                OR to_regclass('public.knowledge_faqs') IS NOT NULL
                OR to_regclass('public.admin_audit_logs') IS NOT NULL,
            'Idempotent (IF NOT EXISTS). Run after 003.'
        ),
        (
            '005', '005-knowledge-section-import.sql',
            to_regclass('public.knowledge_import_sections') IS NOT NULL
                AND EXISTS (SELECT 1 FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public' AND c.relname = 'knowledge_documents' AND t.tgname = 'guard_knowledge_import_original'),
            to_regclass('public.knowledge_import_sections') IS NOT NULL,
            'Non-idempotent CREATE TABLE/TRIGGER. Run once after 004.'
        ),
        (
            '006', '006-rag-gemini-runtime.sql',
            to_regclass('public.rag_runtime_plans') IS NOT NULL
                AND to_regclass('public.rag_api_requests') IS NOT NULL
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'embedding_provider'),
            to_regclass('public.rag_runtime_plans') IS NOT NULL
                OR to_regclass('public.rag_api_requests') IS NOT NULL,
            'Idempotent (IF NOT EXISTS). Run after 005.'
        ),
        (
            '007', '007-online-production-readiness.sql',
            to_regclass('public.rag_schema_migrations') IS NOT NULL
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_files' AND column_name = 'file_content'),
            to_regclass('public.rag_schema_migrations') IS NOT NULL,
            'Idempotent (IF NOT EXISTS). Run last after 006.'
        )
) AS m(migration_id, filename, all_present, any_present, action_required)
ORDER BY m.migration_id;

-- Query A4: Verify public.settings Structure for 007 Upsert Compatibility
SELECT
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'settings'
ORDER BY ordinal_position;
```

### A.2 Actual Verified Results from Live Supabase Production (`VERIFIED — PASS`)

The read-only preflight queries were executed in the **Supabase SQL Editor** on production and verified with **zero conflicts**:

| `check_order` | `check_group` | `item` | Verified Production `status` | Verified Production `details` |
|---|---|---|---|---|
| `1` | `1_ENV_PGVECTOR` | `PostgreSQL & Role` | **`postgres (bypassrls=true)`** | `search_path="\$user", public, extensions` |
| `2` | `1_ENV_PGVECTOR` | `pgvector extension` | **`NOT_INSTALLED (available=0.8.2)`** | `extensions schema exists=true` |
| `10` | `2_MIGRATION_STATUS` | `001-cms-media-revisions.sql` | **`MISSING`** | `cms_media=false, cms_revisions=false` |
| `11` | `2_MIGRATION_STATUS` | `002-rag-mvp.sql` | **`MISSING`** | `knowledge_documents, knowledge_chunks, knowledge_index_jobs` |
| `12` | `2_MIGRATION_STATUS` | `003-rag-source-policy.sql` | **`MISSING`** | `policy_key, source_authority, supersedes_document_id` |
| `13` | `2_MIGRATION_STATUS` | `004-admin-knowledge-base.sql` | **`MISSING`** | `admin_audit_logs=false, knowledge_files=false` |
| `14` | `2_MIGRATION_STATUS` | `005-knowledge-section-import.sql` | **`MISSING`** | `knowledge_import_sections + guard_knowledge_import_original` |
| `15` | `2_MIGRATION_STATUS` | `006-rag-gemini-runtime.sql` | **`MISSING`** | `rag_runtime_plans, rag_api_requests` |
| `16` | `2_MIGRATION_STATUS` | `007-online-production-readiness.sql` | **`MISSING`** | `rag_schema_migrations + knowledge_files.file_content` |
| `20` | `3_SETTINGS_CHECK` | `public.settings(setting_key) UNIQUE` | **`READY (UNIQUE index present)`** | `Required for ON CONFLICT (setting_key) in 007` |

Additionally, `Query A4` on `public.settings` confirmed the exact 5 columns:
- `id` (`integer`, `NO`)
- `setting_key` (`character varying`, `NO`, `UNIQUE` index verified)
- `setting_value` (`text`, `YES`)
- `description` (`text`, `YES`)
- `updated_at` (`timestamp without time zone`, `YES`)

**Preflight Conclusion**:
1. Schema `extensions` already exists (`extensions schema exists=true`) and is already in the default `search_path` (`"$user", public, extensions`).
2. All 7 migrations (`001` through `007`) are cleanly `MISSING` (`0` partial tables, `0` naming collisions).
3. `public.settings` has the exact schema and `UNIQUE (setting_key)` constraint required by `007`.

---

# PART B — APPROVAL-REQUIRED MIGRATION (DO NOT RUN UNTIL BACKUP & APPROVAL)

> [!CAUTION]
> **APPROVAL GATE**: Do **not** execute any SQL in Part B until:
> 1. You have run **Part A** and confirmed `0` unexpected `PARTIALLY_APPLIED_REQUIRES_REVIEW` rows.
> 2. You have completed **Step B.0 (Database Backup)** below.
> 3. You explicitly approve executing the migration steps in the Supabase SQL Editor.

---

## Step B.0 — Mandatory Backup Before Any Schema Change

Before running any DDL in Part B, create a verifiable backup using one or both of the following methods:

1. **Supabase Dashboard Backup / Snapshot**:
   - Open **Supabase Dashboard → Database → Backups** and verify the latest daily backup or trigger an on-demand backup/snapshot if on Pro plan.
2. **Logical Schema + Data Dump via `pg_dump` (Recommended on all plans)**:
   ```powershell
   pg_dump "postgresql://postgres.<PROJECT_REF>:<PASSWORD>@<SUPABASE_HOST>:5432/postgres?sslmode=require" `
     --format=custom --no-owner --no-acl `
     --file="backup-pre-phase54-$(Get-Date -Format 'yyyyMMdd-HHmmss').dump"
   ```

---

## Step B.1 — Install `pgvector 0.8.2` on Supabase (`APPROVAL REQUIRED`)

Run this query first in **Supabase SQL Editor**:

```sql
-- =============================================================================
-- STEP B.1: INSTALL PGVECTOR 0.8.2 IN SCHEMA extensions
-- =============================================================================
BEGIN;

CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions;

GRANT USAGE ON SCHEMA extensions TO postgres, anon, authenticated, service_role;

COMMIT;

-- Immediate verification of Step B.1:
SELECT
    e.extname,
    e.extversion,
    n.nspname AS installed_schema,
    '[1,2,3]'::extensions.vector(3) <=> '[1,2,3]'::extensions.vector(3) AS cosine_distance_self_check
FROM pg_extension e
JOIN pg_namespace n ON n.oid = e.extnamespace
WHERE e.extname = 'vector';
```

**Expected output of Step B.1 verification**:
- `extname = 'vector'`
- `extversion = '0.8.2'`
- `installed_schema = 'extensions'`
- `cosine_distance_self_check = 0`

---

## Step B.2 — Apply `001-cms-media-revisions.sql` (If `001` Was `MISSING` in Part A)

Source file: [`database/migrations/001-cms-media-revisions.sql`](file:///d:/miniproject/fpt/database/migrations/001-cms-media-revisions.sql). Because it uses `IF NOT EXISTS`, it is safe even if `cms_media` / `cms_revisions` already exist:

```sql
-- =============================================================================
-- STEP B.2: MIGRATION 001 (CMS MEDIA & REVISIONS — IDEMPOTENT IF NOT EXISTS)
-- =============================================================================
BEGIN;
CREATE TABLE IF NOT EXISTS public.cms_media (
 id uuid PRIMARY KEY, content_type varchar(32) NOT NULL CHECK(content_type IN ('image/png','image/jpeg')),
 content bytea NOT NULL CHECK(octet_length(content)<=2097152), width integer NOT NULL CHECK(width BETWEEN 1 AND 4096),
 height integer NOT NULL CHECK(height BETWEEN 1 AND 4096), actor_id integer NOT NULL, created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS public.cms_revisions (
 id bigserial PRIMARY KEY, actor_id integer NOT NULL, change_type varchar(64) NOT NULL,
 before_data jsonb NOT NULL, after_data jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS cms_revisions_kind_id ON public.cms_revisions(change_type,id DESC);
COMMIT;
```

---

## Step B.3 — Apply Missing RAG & Admin KB Migrations (`002` → `003` → `004` → `005` → `006` → `007`)

Execute each migration below **in strict numerical order** in the **Supabase SQL Editor** (one query tab per migration, waiting for `Success. No rows returned` before running the next):

### 1. Run `002-rag-mvp.sql` ([`database/migrations/002-rag-mvp.sql`](file:///d:/miniproject/fpt/database/migrations/002-rag-mvp.sql))
```sql
BEGIN;
SET LOCAL search_path = public, extensions, pg_catalog;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
    RAISE EXCEPTION 'pgvector is not installed; request administrator approval first';
  END IF;
END $$;

CREATE TABLE public.knowledge_documents (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
  category VARCHAR(32) NOT NULL CHECK (category IN ('faq','policy','guide')),
  content TEXT NOT NULL CHECK (length(content) BETWEEN 1 AND 60000),
  source VARCHAR(500) NOT NULL CHECK (length(trim(source)) > 0),
  status VARCHAR(16) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','published','archived')),
  version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
  content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
  effective_at TIMESTAMPTZ, expires_at TIMESTAMPTZ,
  approved_by INTEGER, approved_at TIMESTAMPTZ,
  embedding_model VARCHAR(100) NOT NULL,
  vector_dimensions INTEGER NOT NULL CHECK (vector_dimensions = 768),
  indexing_status VARCHAR(16) NOT NULL DEFAULT 'pending' CHECK (indexing_status IN ('pending','ready','failed')),
  indexed_version BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (expires_at IS NULL OR effective_at IS NULL OR expires_at > effective_at),
  CHECK (status <> 'published' OR (approved_by IS NOT NULL AND approved_at IS NOT NULL))
);

CREATE TABLE public.knowledge_chunks (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES public.knowledge_documents(id) ON DELETE CASCADE,
  document_version BIGINT NOT NULL,
  document_hash CHAR(64) NOT NULL,
  ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
  content TEXT NOT NULL CHECK (length(content) BETWEEN 1 AND 1200),
  content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
  embedding_model VARCHAR(100) NOT NULL,
  vector_dimensions INTEGER NOT NULL CHECK (vector_dimensions = 768),
  embedding vector(768) NOT NULL,
  search_text TEXT NOT NULL,
  search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple'::regconfig, search_text)) STORED,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (document_id, document_version, ordinal)
);
CREATE INDEX knowledge_chunks_fts ON public.knowledge_chunks USING GIN (search_vector);
CREATE INDEX knowledge_chunks_document ON public.knowledge_chunks (document_id, document_version);

CREATE TABLE public.knowledge_index_jobs (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES public.knowledge_documents(id) ON DELETE CASCADE,
  document_version BIGINT NOT NULL,
  document_hash CHAR(64) NOT NULL,
  embedding_model VARCHAR(100) NOT NULL,
  vector_dimensions INTEGER NOT NULL CHECK (vector_dimensions = 768),
  status VARCHAR(16) NOT NULL DEFAULT 'queued' CHECK (status IN ('queued','running','done','failed','superseded')),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 3),
  lease_token UUID, lease_until TIMESTAMPTZ,
  available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  error_code VARCHAR(32),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (document_id, document_version, embedding_model, vector_dimensions)
);
CREATE INDEX knowledge_index_jobs_pending ON public.knowledge_index_jobs (status, available_at);

ALTER TABLE public.knowledge_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_index_jobs ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.knowledge_documents, public.knowledge_chunks, public.knowledge_index_jobs FROM PUBLIC;
REVOKE ALL ON SEQUENCE public.knowledge_documents_id_seq, public.knowledge_chunks_id_seq, public.knowledge_index_jobs_id_seq FROM PUBLIC;
DO $$ DECLARE r TEXT; BEGIN
  FOREACH r IN ARRAY ARRAY['anon','authenticated'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname=r) THEN
      EXECUTE format('REVOKE ALL ON public.knowledge_documents, public.knowledge_chunks, public.knowledge_index_jobs FROM %I',r);
      EXECUTE format('REVOKE ALL ON SEQUENCE public.knowledge_documents_id_seq, public.knowledge_chunks_id_seq, public.knowledge_index_jobs_id_seq FROM %I',r);
    END IF;
  END LOOP;
END $$;
COMMENT ON TABLE public.knowledge_documents IS 'RAG schema version 002; curated public knowledge only, never customer PII or secrets';
COMMIT;
```

### 2. Run `003-rag-source-policy.sql` ([`database/migrations/003-rag-source-policy.sql`](file:///d:/miniproject/fpt/database/migrations/003-rag-source-policy.sql))
```sql
BEGIN;
ALTER TABLE public.knowledge_documents
  ADD COLUMN policy_key VARCHAR(100) NOT NULL DEFAULT '',
  ADD COLUMN source_authority VARCHAR(32) NOT NULL DEFAULT 'admin_reviewed',
  ADD COLUMN supersedes_document_id BIGINT REFERENCES public.knowledge_documents(id),
  ADD CONSTRAINT knowledge_policy_key_format CHECK (policy_key='' OR policy_key ~ '^[a-z0-9][a-z0-9._-]{0,99}$'),
  ADD CONSTRAINT knowledge_source_authority CHECK (source_authority IN ('admin_reviewed','official')),
  ADD CONSTRAINT knowledge_supersession_order CHECK (supersedes_document_id IS NULL OR (supersedes_document_id<id AND policy_key<>''));
CREATE INDEX knowledge_documents_policy ON public.knowledge_documents (policy_key,source_authority);
CREATE INDEX knowledge_documents_supersedes ON public.knowledge_documents (supersedes_document_id);
COMMENT ON COLUMN public.knowledge_documents.source_authority IS 'Explicit trusted-admin classification, not inferred from URL, timestamp or model output';
COMMENT ON COLUMN public.knowledge_documents.policy_key IS 'Exact reviewed subject identifier; distinct keys do not prove semantic consistency';
COMMIT;
```

### 3. Run `004-admin-knowledge-base.sql` ([`database/migrations/004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql))
Paste the full contents of [`database/migrations/004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql) into Supabase SQL Editor and click **Run**.

### 4. Run `005-knowledge-section-import.sql` ([`database/migrations/005-knowledge-section-import.sql`](file:///d:/miniproject/fpt/database/migrations/005-knowledge-section-import.sql))
Paste the full contents of [`database/migrations/005-knowledge-section-import.sql`](file:///d:/miniproject/fpt/database/migrations/005-knowledge-section-import.sql) into Supabase SQL Editor and click **Run**.

### 5. Run `006-rag-gemini-runtime.sql` ([`database/migrations/006-rag-gemini-runtime.sql`](file:///d:/miniproject/fpt/database/migrations/006-rag-gemini-runtime.sql))
Paste the full contents of [`database/migrations/006-rag-gemini-runtime.sql`](file:///d:/miniproject/fpt/database/migrations/006-rag-gemini-runtime.sql) into Supabase SQL Editor and click **Run**.

### 6. Run `007-online-production-readiness.sql` ([`database/migrations/007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql))
Paste the full contents of [`database/migrations/007-online-production-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/007-online-production-readiness.sql) into Supabase SQL Editor and click **Run**.

---

## 4. Post-Migration Verification SQL (Read-Only)

Immediately after running Step B.3 (`007`), run [`database/migrations/verify-supabase-readiness.sql`](file:///d:/miniproject/fpt/database/migrations/verify-supabase-readiness.sql) (or the compact verification query below) in the Supabase SQL Editor:

```sql
-- =============================================================================
-- POST-MIGRATION READ-ONLY VERIFICATION
-- =============================================================================

-- 1. Verify all 7 migrations are recorded in public.rag_schema_migrations
SELECT version, filename, description, applied_at
FROM public.rag_schema_migrations
ORDER BY version;

-- 2. Verify all 13 RAG/KB tables + 2 CMS tables exist and have RLS enabled as designed
SELECT
    c.relname AS table_name,
    c.relrowsecurity AS rls_enabled,
    has_table_privilege('anon', 'public.' || quote_ident(c.relname), 'SELECT') AS anon_can_select,
    has_table_privilege('authenticated', 'public.' || quote_ident(c.relname), 'SELECT') AS authenticated_can_select
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname IN (
      'cms_media', 'cms_revisions',
      'knowledge_documents', 'knowledge_chunks', 'knowledge_index_jobs',
      'knowledge_document_revisions', 'knowledge_document_reviews', 'knowledge_files',
      'knowledge_faqs', 'knowledge_faq_revisions', 'admin_audit_logs',
      'knowledge_import_sections', 'rag_runtime_plans', 'rag_api_requests',
      'rag_schema_migrations'
  )
ORDER BY c.relname;

-- 3. Verify vector(768) column on knowledge_chunks & BYTEA column on knowledge_files
SELECT
    c.relname AS table_name,
    a.attname AS column_name,
    pg_catalog.format_type(a.atttypid, a.atttypmod) AS formatted_type
FROM pg_attribute a
JOIN pg_class c ON c.oid = a.attrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND ((c.relname = 'knowledge_chunks' AND a.attname = 'embedding')
    OR (c.relname = 'knowledge_files' AND a.attname IN ('file_content', 'storage_backend')))
  AND a.attnum > 0 AND NOT a.attisdropped;

-- 4. Verify production RAG remains hard-locked OFF in public.settings
SELECT setting_key, setting_value
FROM public.settings
WHERE setting_key = 'rag.production_rag_enabled';
```

### 4.1 Actual Verified Post-Migration Results on Supabase Production (`10/10 PASS`)

Following administrator approval and execution in the Supabase SQL Editor, the unified post-migration verification query returned:

| `check_order` | `check_group` | `item` | Verified Post-Migration `status` | Verified Post-Migration `details` |
|---|---|---|---|---|
| `1` | `1_ENV_PGVECTOR` | `pgvector extension` | **`INSTALLED (0.8.2 in extensions)`** | `cosine_self_check=0` |
| `11` | `2_MIGRATION_LEDGER` | `001 - 001-cms-media-revisions.sql` | **`ALREADY_APPLIED`** | `CMS media assets and content revisions` |
| `12` | `2_MIGRATION_LEDGER` | `002 - 002-rag-mvp.sql` | **`ALREADY_APPLIED`** | `RAG MVP pgvector documents, 768d chunks, FTS, and index jobs` |
| `13` | `2_MIGRATION_LEDGER` | `003 - 003-rag-source-policy.sql` | **`ALREADY_APPLIED`** | `RAG source policy and active retrieval gate` |
| `14` | `2_MIGRATION_LEDGER` | `004 - 004-admin-knowledge-base.sql` | **`ALREADY_APPLIED`** | `Admin Knowledge Base workflow, files, FAQs, reviews, audit log, RLS` |
| `15` | `2_MIGRATION_LEDGER` | `005 - 005-knowledge-section-import.sql` | **`ALREADY_APPLIED`** | `Section-based Markdown knowledge import and archival trigger` |
| `16` | `2_MIGRATION_LEDGER` | `006 - 006-rag-gemini-runtime.sql` | **`ALREADY_APPLIED`** | `Gemini runtime provenance, rag_runtime_plans, rag_api_requests ledger` |
| `17` | `2_MIGRATION_LEDGER` | `007 - 007-online-production-readiness.sql` | **`ALREADY_APPLIED`** | `Schema version tracking and durable PostgreSQL BYTEA file storage` |
| `30` | `3_SECURITY_RLS` | `RLS & Public Role Isolation (13 RAG/KB tables)` | **`VERIFIED (13/13 RLS ON, anon/auth REVOKED)`** | `knowledge_chunks.embedding=vector(768)` |
| `40` | `4_SAFETY_GATE` | `public.settings -> rag.production_rag_enabled` | **`false`** | `Customer-facing RAG remains locked OFF` |

---

## 5. Step-by-Step Supabase SQL Editor Execution Guide

1. **Open Supabase SQL Editor**:
   - Log in to Supabase Dashboard → select the production project → click **SQL Editor** in the left navigation bar.
2. **Execute Part A (Read-Only Preflight) (`COMPLETED — PASS`)**:
   - Confirmed `vector_available_version = 0.8.2`, `001`–`007` = `MISSING`, and `public.settings(setting_key) UNIQUE` = `READY`.
3. **Create & Verify Database Backup (Step B.0) (`COMPLETED`)**:
   - Confirmed pre-migration backup before running DDL.
4. **Execute Step B.1 (`pgvector 0.8.2`) & Step B.2/B.3 (`001` → `007`) (`COMPLETED — PASS`)**:
   - Executed atomically with RLS enabled on all created tables.
5. **Execute Section 4 (Post-Migration Verification) (`COMPLETED — 10/10 PASS`)**:
   - Verified `pgvector 0.8.2` in `extensions`, all 7 migrations recorded in `public.rag_schema_migrations`, `13/13` RAG/KB tables with RLS enabled and `anon`/`authenticated` revoked, `knowledge_chunks.embedding = vector(768)`, and `rag.production_rag_enabled = 'false'`.

---

## 6. Backup, Recovery & Rollback Plan

### 6.1 Non-Destructive Operational Rollback (Default Recommendation)
- Because `002`–`007` only create **new** `knowledge_%`, `rag_%`, and `admin_audit_logs` tables and do **not** alter `customers`, `packages`, `users`, or existing `settings`, the existing production website and catalog chatbot are 100% unaffected by the presence of these empty tables.
- Even after `002`–`007` are applied, customer-facing RAG remains **disabled** (`rag.production_rag_enabled = 'false'` and `RAG_ENABLED=false`).

### 6.2 Emergency Schema Rollback SQL (Only If You Must Remove Empty `002`–`007` Tables)
If you ever need to revert the newly created `002`–`007` RAG schema completely back to the pre-Phase-5.4 state (while keeping `customers`, `packages`, `users`, `settings`, and `001` `cms_media`/`cms_revisions` untouched), run:

```sql
-- EMERGENCY ROLLBACK OF EMPTY 002..007 RAG TABLES ONLY
-- Does NOT touch users, packages, customers, settings (except removing rag.production_rag_enabled), or cms_media/cms_revisions.
BEGIN;

DROP TRIGGER IF EXISTS guard_knowledge_import_original ON public.knowledge_documents;
DROP FUNCTION IF EXISTS public.guard_knowledge_import_original();

DROP TABLE IF EXISTS public.rag_schema_migrations;
DROP TABLE IF EXISTS public.rag_api_requests;
DROP TABLE IF EXISTS public.rag_runtime_plans;
DROP TABLE IF EXISTS public.knowledge_import_sections;
DROP TABLE IF EXISTS public.knowledge_faq_revisions;
DROP TABLE IF EXISTS public.knowledge_faqs;
DROP TABLE IF EXISTS public.knowledge_files;
DROP TABLE IF EXISTS public.knowledge_document_reviews;
DROP TABLE IF EXISTS public.knowledge_document_revisions;
DROP TABLE IF EXISTS public.knowledge_index_jobs;
DROP TABLE IF EXISTS public.knowledge_chunks;
DROP TABLE IF EXISTS public.knowledge_documents;

DELETE FROM public.settings WHERE setting_key = 'rag.production_rag_enabled';

COMMIT;
```

---

## 7. Administrator Approval & Execution Checklist

- [x] **1. Part A Read-Only Preflight Executed**: Verified `001`–`007` `MISSING` and `public.settings(setting_key) UNIQUE` `READY`.
- [x] **2. Pre-Migration Backup Confirmed (Step B.0)**: Confirmed before running DDL.
- [x] **3. Step B.1 (`pgvector 0.8.2` Installation)**: Verified `INSTALLED (0.8.2 in extensions)` and `cosine_self_check=0`.
- [x] **4. Step B.2 & Step B.3 (Ordered Migrations `001`–`007`)**: Verified `001` through `007` `ALREADY_APPLIED`, `13/13 RLS ON, anon/auth REVOKED`, `knowledge_chunks.embedding=vector(768)`, and `rag.production_rag_enabled = 'false'`.

---

SUPABASE RAG MIGRATION COMPLETED & VERIFIED — PRODUCTION RAG REMAINS SAFELY LOCKED OFF (`false`).
