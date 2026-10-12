-- Phase 5.3: Read-Only Preflight & Post-Migration Verification Script for Supabase PostgreSQL
-- Strictly SELECT-only: zero DDL, zero DML, zero customer PII, and zero secret exposure.
-- Safe to execute in Supabase SQL Editor or via psql BOTH before and after migrations 001..007.

-- 1. PostgreSQL Server Version, Role & pgvector Extension Readiness
SELECT
    version() AS postgres_version,
    current_database() AS database_name,
    current_user AS connected_role,
    EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') AS pgvector_available,
    EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') AS pgvector_installed,
    (SELECT extversion FROM pg_extension WHERE extname = 'vector') AS pgvector_version,
    (SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace WHERE e.extname = 'vector') AS pgvector_schema;

-- 2. Complete Table Inventory & Row-Level Security (RLS) Audit (Base + 001..007)
SELECT
    expected.phase,
    expected.table_name,
    (c.oid IS NOT NULL) AS table_present,
    COALESCE(c.relrowsecurity, FALSE) AS rls_enabled,
    (SELECT COUNT(*) FROM pg_policies p WHERE p.schemaname = 'public' AND p.tablename = expected.table_name) AS rls_policy_count
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
ORDER BY expected.phase, expected.table_name;

-- 3. Exact Migration 001..007 Schema Difference Classification
SELECT
    m.migration_id,
    m.filename,
    CASE
        WHEN m.all_present THEN 'ALREADY_APPLIED'
        WHEN NOT m.any_present THEN 'MISSING'
        ELSE 'PARTIALLY_APPLIED_REQUIRES_REVIEW'
    END AS schema_state,
    m.notes
FROM (
    VALUES
        (
            '001',
            '001-cms-media-revisions.sql',
            to_regclass('public.cms_media') IS NOT NULL AND to_regclass('public.cms_revisions') IS NOT NULL,
            to_regclass('public.cms_media') IS NOT NULL OR to_regclass('public.cms_revisions') IS NOT NULL,
            'CMS media assets (BYTEA <= 2MB) and JSONB revisions'
        ),
        (
            '002',
            '002-rag-mvp.sql',
            to_regclass('public.knowledge_documents') IS NOT NULL
                AND to_regclass('public.knowledge_chunks') IS NOT NULL
                AND to_regclass('public.knowledge_index_jobs') IS NOT NULL,
            to_regclass('public.knowledge_documents') IS NOT NULL
                OR to_regclass('public.knowledge_chunks') IS NOT NULL
                OR to_regclass('public.knowledge_index_jobs') IS NOT NULL,
            'RAG MVP tables + vector(768) column (non-idempotent CREATE TABLE)'
        ),
        (
            '003',
            '003-rag-source-policy.sql',
            EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'policy_key')
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'source_authority')
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'supersedes_document_id'),
            EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name IN ('policy_key', 'source_authority', 'supersedes_document_id')),
            'Source authority & policy_key columns on knowledge_documents (non-idempotent ADD COLUMN)'
        ),
        (
            '004',
            '004-admin-knowledge-base.sql',
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
            'Admin Knowledge Base tables, workflow columns, and RLS (idempotent IF NOT EXISTS)'
        ),
        (
            '005',
            '005-knowledge-section-import.sql',
            to_regclass('public.knowledge_import_sections') IS NOT NULL
                AND EXISTS (SELECT 1 FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public' AND c.relname = 'knowledge_documents' AND t.tgname = 'guard_knowledge_import_original'),
            to_regclass('public.knowledge_import_sections') IS NOT NULL,
            'Section import table and guard_knowledge_import_original trigger (non-idempotent CREATE TABLE/TRIGGER)'
        ),
        (
            '006',
            '006-rag-gemini-runtime.sql',
            to_regclass('public.rag_runtime_plans') IS NOT NULL
                AND to_regclass('public.rag_api_requests') IS NOT NULL
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'embedding_provider')
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_chunks' AND column_name = 'embedding_provider')
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_index_jobs' AND column_name = 'embedding_provider'),
            to_regclass('public.rag_runtime_plans') IS NOT NULL
                OR to_regclass('public.rag_api_requests') IS NOT NULL
                OR EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'embedding_provider'),
            'Gemini runtime ledger & embedding_provider isolation (idempotent IF NOT EXISTS)'
        ),
        (
            '007',
            '007-online-production-readiness.sql',
            to_regclass('public.rag_schema_migrations') IS NOT NULL
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_files' AND column_name = 'file_content')
                AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_files' AND column_name = 'storage_backend'),
            to_regclass('public.rag_schema_migrations') IS NOT NULL
                OR EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_files' AND column_name = 'file_content'),
            'Schema migration registry and durable PostgreSQL BYTEA file storage (idempotent IF NOT EXISTS)'
        )
) AS m(migration_id, filename, all_present, any_present, notes)
ORDER BY m.migration_id;

-- 4. Verify Vector Column Type (768d) on knowledge_chunks and Durable Storage Columns on knowledge_files
SELECT
    c.relname AS table_name,
    a.attname AS column_name,
    pg_catalog.format_type(a.atttypid, a.atttypmod) AS formatted_type,
    a.attnotnull AS not_null
FROM pg_catalog.pg_attribute a
JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND (
      (c.relname = 'knowledge_chunks' AND a.attname IN ('embedding', 'embedding_model', 'embedding_provider', 'vector_dimensions', 'search_vector'))
      OR (c.relname = 'knowledge_files' AND a.attname IN ('file_content', 'storage_backend', 'storage_rel_path', 'sha256_hash', 'file_size_bytes'))
      OR (c.relname = 'knowledge_index_jobs' AND a.attname IN ('status', 'embedding_provider', 'lease_token', 'lease_until', 'error_code'))
  )
  AND a.attnum > 0
  AND NOT a.attisdropped
ORDER BY c.relname, a.attname;

-- 5. Verify Indexes & Constraints on CMS / RAG / Knowledge Base Tables
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN (
      'cms_revisions',
      'knowledge_documents',
      'knowledge_chunks',
      'knowledge_index_jobs',
      'knowledge_document_revisions',
      'knowledge_document_reviews',
      'knowledge_files',
      'knowledge_faqs',
      'knowledge_faq_revisions',
      'admin_audit_logs',
      'rag_api_requests'
  )
ORDER BY tablename, indexname;

-- 6. Verify JDBC Role Privileges & Public REST Role Isolation (anon / authenticated)
SELECT
    current_user AS jdbc_role,
    has_schema_privilege(current_user, 'public', 'USAGE') AS public_schema_usage,
    has_schema_privilege(current_user, 'public', 'CREATE') AS public_schema_create,
    CASE
        WHEN EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') AND to_regclass('public.knowledge_documents') IS NOT NULL
        THEN has_table_privilege('anon', 'public.knowledge_documents', 'SELECT')
        ELSE FALSE
    END AS anon_can_select_kb,
    CASE
        WHEN EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') AND to_regclass('public.knowledge_documents') IS NOT NULL
        THEN has_table_privilege('authenticated', 'public.knowledge_documents', 'SELECT')
        ELSE FALSE
    END AS authenticated_can_select_kb;

-- 7. Safe Runtime Settings & Operational State Summary (Zero Secrets, Zero Customer PII)
SELECT
    CASE WHEN to_regclass('public.settings') IS NOT NULL THEN
        query_to_xml(
            $$SELECT setting_key,
                     CASE
                         WHEN setting_key LIKE '%key%' OR setting_key LIKE '%secret%' OR setting_key LIKE '%password%'
                         THEN CASE WHEN setting_value IS NULL OR btrim(setting_value) = '' THEN 'ABSENT' ELSE 'PRESENT_REDACTED' END
                         ELSE COALESCE(setting_value, '')
                     END AS safe_value
              FROM public.settings
              WHERE setting_key IN (
                  'ai_chatbot_enabled',
                  'ai_active_provider',
                  'ai_active_model',
                  'ai_thinking_level',
                  'ai_fallback_enabled',
                  'ai_fallback_provider',
                  'ai_fallback_model',
                  'ai_fallback_thinking_level',
                  'ai_provider_gemini_key',
                  'gemini_api_key',
                  'rag.local_indexing_enabled',
                  'rag.staging_rag_enabled',
                  'rag.production_rag_enabled',
                  'rag.top_k',
                  'rag.similarity_threshold'
              )
              ORDER BY setting_key$$,
            true, false, ''
        )::text
    ELSE 'SETTINGS_TABLE_ABSENT'
    END AS ai_rag_settings_xml,
    CASE WHEN to_regclass('public.rag_schema_migrations') IS NOT NULL THEN
        query_to_xml('SELECT version, filename, applied_at FROM public.rag_schema_migrations ORDER BY version', true, false, '')::text
    ELSE 'MIGRATION_007_NOT_APPLIED_YET'
    END AS applied_migrations_xml,
    CASE WHEN to_regclass('public.knowledge_index_jobs') IS NOT NULL THEN
        query_to_xml(
            $$SELECT status,
                     COUNT(*) AS job_count,
                     COUNT(*) FILTER (WHERE status = 'running' AND lease_until < CURRENT_TIMESTAMP) AS expired_leases
              FROM public.knowledge_index_jobs
              GROUP BY status
              ORDER BY status$$,
            true, false, ''
        )::text
    ELSE 'INDEX_JOBS_TABLE_NOT_APPLIED_YET'
    END AS indexing_jobs_xml,
    CASE WHEN to_regclass('public.rag_api_requests') IS NOT NULL THEN
        query_to_xml(
            $$SELECT request_group, state, COUNT(*) AS request_count
              FROM public.rag_api_requests
              GROUP BY request_group, state
              ORDER BY request_group, state$$,
            true, false, ''
        )::text
    ELSE 'LEDGER_TABLE_NOT_APPLIED_YET'
    END AS api_ledger_xml;
