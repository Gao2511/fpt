-- Additive Phase 5.7 migration. Operator applies after backup; never run at app startup.
BEGIN;
SET LOCAL search_path=public,extensions,pg_catalog;
ALTER TABLE public.knowledge_documents DROP CONSTRAINT IF EXISTS knowledge_documents_content_check;
ALTER TABLE public.knowledge_documents ADD CONSTRAINT knowledge_documents_content_check CHECK(length(content) BETWEEN 1 AND 500000);
ALTER TABLE public.knowledge_document_revisions DROP CONSTRAINT IF EXISTS knowledge_document_revisions_content_check;
ALTER TABLE public.knowledge_document_revisions ADD CONSTRAINT knowledge_document_revisions_content_check CHECK(length(content) BETWEEN 1 AND 500000);
CREATE TABLE public.knowledge_publish_jobs (
 id BIGSERIAL PRIMARY KEY,
 document_id BIGINT NOT NULL REFERENCES public.knowledge_documents(id) ON DELETE CASCADE,
 document_version BIGINT NOT NULL, document_hash CHAR(64) NOT NULL,
 replacement_id BIGINT REFERENCES public.knowledge_documents(id),
 replacement_version BIGINT, replacement_hash CHAR(64),
 actor_id INTEGER NOT NULL CHECK(actor_id>0),
 state VARCHAR(32) NOT NULL DEFAULT 'PENDING_INDEX' CHECK(state IN ('PENDING_INDEX','WAITING_FOR_QUOTA','WAITING_EFFECTIVE','INDEXING_FAILED','AVAILABLE_TO_CHATBOT','CANCELLED')),
 error_code VARCHAR(32), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(document_id,document_version)
);
CREATE TABLE public.knowledge_publish_chunks (
 job_id BIGINT NOT NULL REFERENCES public.knowledge_publish_jobs(id) ON DELETE CASCADE,
 ordinal INTEGER NOT NULL CHECK(ordinal>=0), content TEXT NOT NULL CHECK(length(content) BETWEEN 1 AND 1200),
 content_hash CHAR(64) NOT NULL,
 state VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','RESERVED','DONE','FAILED','UNCERTAIN')),
 embedding vector(768), reserved_tokens INTEGER, http_status INTEGER,
 reserved_at TIMESTAMPTZ, finished_at TIMESTAMPTZ, error_code VARCHAR(32),
 PRIMARY KEY(job_id,ordinal), CHECK(state<>'DONE' OR embedding IS NOT NULL)
);
CREATE INDEX knowledge_publish_pending ON public.knowledge_publish_jobs(state,id);
-- Intentionally no cascading FK: request reservations survive permanent document deletion.
CREATE TABLE public.knowledge_embedding_requests (
 job_id BIGINT NOT NULL, ordinal INTEGER NOT NULL,
 state VARCHAR(16) NOT NULL CHECK(state IN ('RESERVED','DONE','FAILED','UNCERTAIN')),
 reserved_tokens INTEGER NOT NULL CHECK(reserved_tokens>0),
 reserved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(job_id,ordinal)
);
ALTER TABLE public.knowledge_publish_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_publish_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_embedding_requests ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.knowledge_publish_jobs,public.knowledge_publish_chunks,public.knowledge_embedding_requests FROM PUBLIC;
REVOKE ALL ON SEQUENCE public.knowledge_publish_jobs_id_seq FROM PUBLIC;
DO $$ DECLARE r TEXT; BEGIN
 FOREACH r IN ARRAY ARRAY['anon','authenticated'] LOOP
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=r) THEN
   EXECUTE format('REVOKE ALL ON public.knowledge_publish_jobs,public.knowledge_publish_chunks,public.knowledge_embedding_requests FROM %I',r);
   EXECUTE format('REVOKE ALL ON SEQUENCE public.knowledge_publish_jobs_id_seq FROM %I',r);
  END IF;
 END LOOP;
END $$;
INSERT INTO public.rag_schema_migrations(version,filename,description) VALUES('008','008-knowledge-publish-lifecycle.sql','Bounded large documents, staged chunks and atomic publish/replace');
COMMIT;
