-- Phase 5.2 / 5.3: Online Production Readiness (Render + Supabase PostgreSQL + Gemini RAG)
-- Manual migration for an approved staging/production database AFTER backup verification.
-- Never installs extensions automatically; verifies pgvector and 002..006 prerequisites first.
BEGIN;
SET LOCAL search_path = public, extensions, pg_catalog;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE EXCEPTION 'pgvector extension is not installed; enable vector extension in Supabase before running 007';
    END IF;
    IF to_regclass('public.knowledge_documents') IS NULL
       OR to_regclass('public.knowledge_chunks') IS NULL
       OR to_regclass('public.knowledge_index_jobs') IS NULL THEN
        RAISE EXCEPTION 'Prerequisite migration 002-rag-mvp.sql is missing';
    END IF;
    IF to_regclass('public.knowledge_files') IS NULL
       OR to_regclass('public.knowledge_faqs') IS NULL
       OR to_regclass('public.admin_audit_logs') IS NULL THEN
        RAISE EXCEPTION 'Prerequisite migration 004-admin-knowledge-base.sql is missing';
    END IF;
    IF to_regclass('public.rag_runtime_plans') IS NULL
       OR to_regclass('public.rag_api_requests') IS NULL THEN
        RAISE EXCEPTION 'Prerequisite migration 006-rag-gemini-runtime.sql is missing';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.rag_schema_migrations (
    version VARCHAR(32) PRIMARY KEY,
    filename VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public.rag_schema_migrations ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.rag_schema_migrations FROM PUBLIC;
DO $$
DECLARE
    role_name TEXT;
BEGIN
    FOREACH role_name IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
            EXECUTE format('REVOKE ALL ON public.rag_schema_migrations FROM %I', role_name);
        END IF;
    END LOOP;
END $$;

-- Durable uploaded file storage in PostgreSQL so Render ephemeral container redeployments preserve files
ALTER TABLE public.knowledge_files
    ADD COLUMN IF NOT EXISTS file_content BYTEA,
    ADD COLUMN IF NOT EXISTS storage_backend VARCHAR(16) NOT NULL DEFAULT 'DISK';

ALTER TABLE public.knowledge_files
    DROP CONSTRAINT IF EXISTS chk_knowledge_files_storage_backend;
ALTER TABLE public.knowledge_files
    ADD CONSTRAINT chk_knowledge_files_storage_backend
    CHECK (storage_backend IN ('DISK', 'DATABASE', 'HYBRID'));

ALTER TABLE public.knowledge_files
    DROP CONSTRAINT IF EXISTS chk_knowledge_files_content_size;
ALTER TABLE public.knowledge_files
    ADD CONSTRAINT chk_knowledge_files_content_size
    CHECK (file_content IS NULL OR octet_length(file_content) <= 10485760);

-- Index for lease expiration recovery on Render container restarts
CREATE INDEX IF NOT EXISTS idx_knowledge_index_jobs_running_lease
    ON public.knowledge_index_jobs(status, lease_until)
    WHERE status = 'running';

-- Keep customer-facing production RAG hard-locked OFF in settings if settings table is present
DO $$
BEGIN
    IF to_regclass('public.settings') IS NOT NULL THEN
        INSERT INTO public.settings (setting_key, setting_value, description)
        VALUES ('rag.production_rag_enabled', 'false', 'Hard safety gate for customer-facing RAG')
        ON CONFLICT (setting_key) DO UPDATE SET setting_value = 'false';
    END IF;
END $$;

-- Record verified migrations idempotently based on actual catalog presence
INSERT INTO public.rag_schema_migrations (version, filename, description)
SELECT v.version, v.filename, v.description
FROM (
    VALUES
        ('001', '001-cms-media-revisions.sql', 'CMS media assets and content revisions',
            to_regclass('public.cms_media') IS NOT NULL AND to_regclass('public.cms_revisions') IS NOT NULL),
        ('002', '002-rag-mvp.sql', 'RAG MVP pgvector documents, 768d chunks, FTS, and index jobs',
            to_regclass('public.knowledge_documents') IS NOT NULL AND to_regclass('public.knowledge_chunks') IS NOT NULL AND to_regclass('public.knowledge_index_jobs') IS NOT NULL),
        ('003', '003-rag-source-policy.sql', 'RAG source policy and active retrieval gate',
            EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'knowledge_documents' AND column_name = 'policy_key')),
        ('004', '004-admin-knowledge-base.sql', 'Admin Knowledge Base workflow, files, FAQs, reviews, audit log, RLS',
            to_regclass('public.knowledge_files') IS NOT NULL AND to_regclass('public.knowledge_faqs') IS NOT NULL AND to_regclass('public.admin_audit_logs') IS NOT NULL),
        ('005', '005-knowledge-section-import.sql', 'Section-based Markdown knowledge import and archival trigger',
            to_regclass('public.knowledge_import_sections') IS NOT NULL),
        ('006', '006-rag-gemini-runtime.sql', 'Gemini runtime provenance, rag_runtime_plans, rag_api_requests ledger',
            to_regclass('public.rag_runtime_plans') IS NOT NULL AND to_regclass('public.rag_api_requests') IS NOT NULL),
        ('007', '007-online-production-readiness.sql', 'Schema version tracking and durable PostgreSQL BYTEA file storage',
            TRUE)
) AS v(version, filename, description, is_present)
WHERE v.is_present
ON CONFLICT (version) DO UPDATE
SET filename = EXCLUDED.filename,
    description = EXCLUDED.description;

COMMIT;
