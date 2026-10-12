-- Manual local/test migration only; no extension installation or automatic production writes.
BEGIN;
ALTER TABLE public.knowledge_documents ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(8) NOT NULL DEFAULT 'MOCK' CHECK(embedding_provider IN ('MOCK','GEMINI'));
ALTER TABLE public.knowledge_documents ADD COLUMN IF NOT EXISTS last_indexed_at TIMESTAMPTZ;
ALTER TABLE public.knowledge_documents ADD COLUMN IF NOT EXISTS embedding_error VARCHAR(32);
ALTER TABLE public.knowledge_documents ALTER COLUMN indexing_status TYPE VARCHAR(32);
ALTER TABLE public.knowledge_documents DROP CONSTRAINT IF EXISTS knowledge_documents_indexing_status_check;
ALTER TABLE public.knowledge_documents ADD CONSTRAINT knowledge_documents_indexing_status_check CHECK(indexing_status IN ('pending','processing','ready','waiting_for_quota','failed_retryable','failed'));
ALTER TABLE public.knowledge_chunks ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(8) NOT NULL DEFAULT 'MOCK' CHECK(embedding_provider IN ('MOCK','GEMINI'));
ALTER TABLE public.knowledge_index_jobs ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(8) NOT NULL DEFAULT 'MOCK' CHECK(embedding_provider IN ('MOCK','GEMINI'));
ALTER TABLE public.knowledge_index_jobs ALTER COLUMN status TYPE VARCHAR(32);
ALTER TABLE public.knowledge_index_jobs DROP CONSTRAINT IF EXISTS knowledge_index_jobs_status_check;
ALTER TABLE public.knowledge_index_jobs ADD CONSTRAINT knowledge_index_jobs_status_check CHECK(status IN ('queued','running','done','failed','superseded','waiting_for_quota','failed_retryable'));
-- Historical embeddings lack real-provider provenance: never mark them GEMINI merely by model name.
CREATE TABLE IF NOT EXISTS public.rag_runtime_plans(plan_id VARCHAR(80) PRIMARY KEY, plan_hash CHAR(64) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS public.rag_api_requests(
 id UUID PRIMARY KEY, plan_id VARCHAR(80) NOT NULL REFERENCES public.rag_runtime_plans(plan_id),
 operation_hash CHAR(64) NOT NULL, project_alias VARCHAR(64) NOT NULL,
 request_group VARCHAR(20) NOT NULL CHECK(request_group IN ('DOCUMENT','QUERY','CHAT')),
 model VARCHAR(100) NOT NULL, reserved_tokens INTEGER NOT NULL CHECK(reserved_tokens>0),
 state VARCHAR(16) NOT NULL CHECK(state IN ('RESERVED','COMPLETED','FAILED','UNCERTAIN')),
 http_status INTEGER, error_code VARCHAR(32), prompt_tokens INTEGER, output_tokens INTEGER, total_tokens INTEGER,
 latency_ms BIGINT, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, finished_at TIMESTAMPTZ,
 UNIQUE(plan_id,operation_hash)
);
CREATE INDEX IF NOT EXISTS rag_api_requests_project_time ON public.rag_api_requests(project_alias,created_at);
ALTER TABLE public.rag_runtime_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rag_api_requests ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.rag_runtime_plans,public.rag_api_requests FROM PUBLIC;
DO $$ DECLARE r TEXT; BEGIN FOREACH r IN ARRAY ARRAY['anon','authenticated'] LOOP
 IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=r) THEN EXECUTE format('REVOKE ALL ON public.rag_runtime_plans,public.rag_api_requests FROM %I',r); END IF;
END LOOP; END $$;
COMMIT;
