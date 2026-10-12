-- Manual, approved isolated development/staging migration. Never executed at application startup.
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
