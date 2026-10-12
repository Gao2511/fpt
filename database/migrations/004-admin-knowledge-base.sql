-- Phase 3.10: Additive migration for Admin Knowledge Base & RAG Management System.
-- Preserves full backward compatibility with 002-rag-mvp.sql and 003-rag-source-policy.sql.
BEGIN;

ALTER TABLE public.knowledge_documents
    ADD COLUMN IF NOT EXISTS workflow_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
        CHECK (workflow_status IN ('DRAFT','PENDING_REVIEW','APPROVED','PUBLISHED','REJECTED','ARCHIVED','UNPUBLISHED','PROCESSING_FAILED')),
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL'
        CHECK (source_type IN ('MANUAL','UPLOAD','FAQ')),
    ADD COLUMN IF NOT EXISTS tags VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS department VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS created_by INTEGER,
    ADD COLUMN IF NOT EXISTS updated_by INTEGER,
    ADD COLUMN IF NOT EXISTS reviewed_by INTEGER,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS chunk_size INTEGER NOT NULL DEFAULT 1000
        CHECK (chunk_size BETWEEN 200 AND 1200),
    ADD COLUMN IF NOT EXISTS chunk_overlap INTEGER NOT NULL DEFAULT 100
        CHECK (chunk_overlap BETWEEN 0 AND 400);

UPDATE public.knowledge_documents
   SET workflow_status = CASE
       WHEN status = 'published' THEN 'PUBLISHED'
       WHEN status = 'archived' THEN 'ARCHIVED'
       ELSE workflow_status
   END
 WHERE (status = 'published' AND workflow_status = 'DRAFT')
    OR (status = 'archived' AND workflow_status = 'DRAFT');

CREATE INDEX IF NOT EXISTS knowledge_documents_workflow_idx
    ON public.knowledge_documents (workflow_status, category, indexing_status, updated_at DESC);

CREATE TABLE IF NOT EXISTS public.knowledge_document_revisions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES public.knowledge_documents(id) ON DELETE CASCADE,
    version_number BIGINT NOT NULL CHECK (version_number >= 1),
    title VARCHAR(200) NOT NULL,
    category VARCHAR(32) NOT NULL CHECK (category IN ('faq','policy','guide')),
    tags VARCHAR(500) NOT NULL DEFAULT '',
    department VARCHAR(100) NOT NULL DEFAULT '',
    source VARCHAR(500) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' CHECK (source_type IN ('MANUAL','UPLOAD','FAQ')),
    source_authority VARCHAR(32) NOT NULL DEFAULT 'admin_reviewed' CHECK (source_authority IN ('admin_reviewed','official')),
    policy_key VARCHAR(100) NOT NULL DEFAULT '',
    content TEXT NOT NULL CHECK (length(content) BETWEEN 1 AND 60000),
    content_hash CHAR(64) NOT NULL,
    workflow_status VARCHAR(32) NOT NULL CHECK (workflow_status IN ('DRAFT','PENDING_REVIEW','APPROVED','PUBLISHED','REJECTED','ARCHIVED','UNPUBLISHED','PROCESSING_FAILED')),
    effective_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    supersedes_document_id BIGINT REFERENCES public.knowledge_documents(id),
    change_note VARCHAR(500) NOT NULL DEFAULT '',
    rollback_from_version BIGINT,
    created_by INTEGER NOT NULL CHECK (created_by > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (document_id, version_number)
);

CREATE INDEX IF NOT EXISTS knowledge_doc_revisions_doc_idx
    ON public.knowledge_document_revisions (document_id, version_number DESC);

CREATE TABLE IF NOT EXISTS public.knowledge_document_reviews (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES public.knowledge_documents(id) ON DELETE CASCADE,
    version_number BIGINT NOT NULL CHECK (version_number >= 1),
    action VARCHAR(32) NOT NULL CHECK (action IN ('SUBMIT_REVIEW','APPROVE','REJECT','PUBLISH','UNPUBLISH','ARCHIVE','ROLLBACK','REINDEX_REQUEST')),
    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    reviewer_id INTEGER NOT NULL CHECK (reviewer_id > 0),
    comments VARCHAR(1000) NOT NULL DEFAULT '',
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS knowledge_doc_reviews_doc_idx
    ON public.knowledge_document_reviews (document_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.knowledge_files (
    id UUID PRIMARY KEY,
    document_id BIGINT REFERENCES public.knowledge_documents(id) ON DELETE SET NULL,
    version_number BIGINT NOT NULL DEFAULT 1,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(128) NOT NULL,
    storage_rel_path VARCHAR(500) NOT NULL,
    detected_mime_type VARCHAR(128) NOT NULL,
    file_extension VARCHAR(16) NOT NULL CHECK (file_extension IN ('pdf','docx','txt','md')),
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes BETWEEN 1 AND 10485760),
    sha256_hash CHAR(64) NOT NULL,
    extraction_status VARCHAR(32) NOT NULL CHECK (extraction_status IN ('EXTRACTED','PROCESSING_FAILED')),
    extracted_char_count INTEGER NOT NULL DEFAULT 0 CHECK (extracted_char_count >= 0),
    extraction_warning VARCHAR(1000) NOT NULL DEFAULT '',
    uploaded_by INTEGER NOT NULL CHECK (uploaded_by > 0),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS knowledge_files_doc_idx
    ON public.knowledge_files (document_id, uploaded_at DESC);

CREATE TABLE IF NOT EXISTS public.knowledge_faqs (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL CHECK (length(btrim(question)) > 0),
    answer TEXT NOT NULL CHECK (length(btrim(answer)) BETWEEN 1 AND 20000),
    category VARCHAR(32) NOT NULL DEFAULT 'faq' CHECK (category IN ('faq','policy','guide')),
    tags VARCHAR(500) NOT NULL DEFAULT '',
    priority INTEGER NOT NULL DEFAULT 10 CHECK (priority BETWEEN 1 AND 1000),
    workflow_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
        CHECK (workflow_status IN ('DRAFT','PENDING_REVIEW','APPROVED','PUBLISHED','REJECTED','ARCHIVED','UNPUBLISHED')),
    version_number BIGINT NOT NULL DEFAULT 1 CHECK (version_number >= 1),
    effective_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    source_authority VARCHAR(32) NOT NULL DEFAULT 'admin_reviewed' CHECK (source_authority IN ('admin_reviewed','official')),
    policy_key VARCHAR(100) NOT NULL DEFAULT '',
    linked_document_id BIGINT REFERENCES public.knowledge_documents(id) ON DELETE SET NULL,
    created_by INTEGER NOT NULL CHECK (created_by > 0),
    updated_by INTEGER NOT NULL CHECK (updated_by > 0),
    reviewed_by INTEGER,
    reviewed_at TIMESTAMPTZ,
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (expires_at IS NULL OR effective_at IS NULL OR expires_at > effective_at)
);

CREATE INDEX IF NOT EXISTS knowledge_faqs_status_priority_idx
    ON public.knowledge_faqs (workflow_status, priority ASC, id DESC);

CREATE TABLE IF NOT EXISTS public.knowledge_faq_revisions (
    id BIGSERIAL PRIMARY KEY,
    faq_id BIGINT NOT NULL REFERENCES public.knowledge_faqs(id) ON DELETE CASCADE,
    version_number BIGINT NOT NULL CHECK (version_number >= 1),
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    tags VARCHAR(500) NOT NULL DEFAULT '',
    priority INTEGER NOT NULL,
    workflow_status VARCHAR(32) NOT NULL,
    change_note VARCHAR(500) NOT NULL DEFAULT '',
    created_by INTEGER NOT NULL CHECK (created_by > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (faq_id, version_number)
);

CREATE INDEX IF NOT EXISTS knowledge_faq_revisions_faq_idx
    ON public.knowledge_faq_revisions (faq_id, version_number DESC);

CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(64) NOT NULL,
    actor_id INTEGER NOT NULL CHECK (actor_id > 0),
    actor_username VARCHAR(100) NOT NULL DEFAULT '',
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    previous_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    new_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    summary VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS admin_audit_logs_created_idx
    ON public.admin_audit_logs (created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS admin_audit_logs_resource_idx
    ON public.admin_audit_logs (resource_type, resource_id, created_at DESC);

-- Enforce Row Level Security and revoke untrusted REST/public access on all Phase 3 tables
ALTER TABLE public.knowledge_document_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_document_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_faqs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_faq_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_audit_logs ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON public.knowledge_document_revisions,
              public.knowledge_document_reviews,
              public.knowledge_files,
              public.knowledge_faqs,
              public.knowledge_faq_revisions,
              public.admin_audit_logs FROM PUBLIC;

DO $$ DECLARE r TEXT; BEGIN
  FOREACH r IN ARRAY ARRAY['anon','authenticated'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
      EXECUTE format('REVOKE ALL ON public.knowledge_document_revisions, public.knowledge_document_reviews, public.knowledge_files, public.knowledge_faqs, public.knowledge_faq_revisions, public.admin_audit_logs FROM %I', r);
    END IF;
  END LOOP;
END $$;

COMMIT;
