-- Requires 008. Operator-only additive migration; never applied during startup/sync.
BEGIN;
SET LOCAL search_path=public,extensions,pg_catalog;
ALTER TABLE public.knowledge_documents DROP CONSTRAINT knowledge_documents_source_type_check;
ALTER TABLE public.knowledge_documents ADD CONSTRAINT knowledge_documents_source_type_check CHECK(source_type IN ('MANUAL','UPLOAD','FAQ','FILE_MANAGED'));
ALTER TABLE public.knowledge_document_revisions DROP CONSTRAINT knowledge_document_revisions_source_type_check;
ALTER TABLE public.knowledge_document_revisions ADD CONSTRAINT knowledge_document_revisions_source_type_check CHECK(source_type IN ('MANUAL','UPLOAD','FAQ','FILE_MANAGED'));
CREATE TABLE public.knowledge_file_sources (
 file_id VARCHAR(80) PRIMARY KEY CHECK(file_id ~ '^[a-z0-9][a-z0-9._-]{0,79}$'),
 file_version BIGINT NOT NULL CHECK(file_version>0), file_sha CHAR(64) NOT NULL CHECK(file_sha ~ '^[0-9a-f]{64}$'),
 file_path VARCHAR(300) NOT NULL, document_id BIGINT NOT NULL REFERENCES public.knowledge_documents(id),
 state VARCHAR(32) NOT NULL CHECK(state IN ('PENDING_QUEUE','QUEUED','INACTIVE','MISSING','EMERGENCY_DISABLED')),
 emergency_disabled BOOLEAN NOT NULL DEFAULT false, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE public.knowledge_file_revisions (
 file_id VARCHAR(80) NOT NULL REFERENCES public.knowledge_file_sources(file_id),
 file_version BIGINT NOT NULL CHECK(file_version>0), file_sha CHAR(64) NOT NULL,
 file_path VARCHAR(300) NOT NULL, document_id BIGINT NOT NULL UNIQUE REFERENCES public.knowledge_documents(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(file_id,file_version)
);
ALTER TABLE public.knowledge_file_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_file_revisions ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.knowledge_file_sources,public.knowledge_file_revisions FROM PUBLIC;
DO $$ DECLARE r TEXT; BEGIN
 FOREACH r IN ARRAY ARRAY['anon','authenticated'] LOOP
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=r) THEN EXECUTE format('REVOKE ALL ON public.knowledge_file_sources,public.knowledge_file_revisions FROM %I',r); END IF;
 END LOOP;
END $$;
INSERT INTO public.rag_schema_migrations(version,filename,description) VALUES('009','009-file-managed-knowledge.sql','Repository-managed source identities and immutable file versions');
COMMIT;
