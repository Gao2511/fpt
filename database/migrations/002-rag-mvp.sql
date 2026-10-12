-- Manual migration for an APPROVED isolated development/staging database only.
-- Operator must install pgvector separately. This file never installs extensions.
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
-- Exact cosine search for MVP. No HNSW/IVFFlat without corpus benchmarks.

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

-- Do not expose knowledge or jobs through the public Supabase REST roles.
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
