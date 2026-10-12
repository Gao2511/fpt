-- Apply manually to approved local/staging only. Never applied automatically at application startup.
BEGIN;
CREATE TABLE public.knowledge_import_sections (
 id BIGSERIAL PRIMARY KEY,
 source_document_id BIGINT NOT NULL,
 source_version BIGINT NOT NULL,
 ordinal INTEGER NOT NULL CHECK (ordinal>0),
 start_offset INTEGER NOT NULL CHECK(start_offset>=0),
 end_offset INTEGER NOT NULL CHECK(end_offset>start_offset),
 start_line INTEGER NOT NULL CHECK(start_line>0),
 end_line INTEGER NOT NULL CHECK(end_line>=start_line),
 original_title VARCHAR(200) NOT NULL,
 title VARCHAR(200) NOT NULL CHECK(length(btrim(title))>0),
 content TEXT NOT NULL,
 content_hash CHAR(64) NOT NULL,
 source_uri VARCHAR(500) NOT NULL,
 classification VARCHAR(32) NOT NULL CHECK(classification IN ('AI_BEHAVIOR','KNOWLEDGE_BASE','FAQ','ADMIN_CONFIGURATION','MANUAL_REVIEW')),
 review_state VARCHAR(16) NOT NULL DEFAULT 'SUGGESTED' CHECK(review_state IN ('SUGGESTED','REVIEWED','IGNORED','CREATED')),
 reviewed_by INTEGER, reviewed_at TIMESTAMPTZ,
 document_id BIGINT REFERENCES public.knowledge_documents(id),
 faq_id BIGINT REFERENCES public.knowledge_faqs(id),
 created_by INTEGER NOT NULL CHECK(created_by>0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(source_document_id,source_version) REFERENCES public.knowledge_document_revisions(document_id,version_number),
 UNIQUE(source_document_id,source_version,ordinal),
 CHECK(NOT(document_id IS NOT NULL AND faq_id IS NOT NULL))
);
ALTER TABLE public.knowledge_import_sections ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.knowledge_import_sections FROM PUBLIC;
DO $$ DECLARE role_name TEXT; BEGIN
 FOREACH role_name IN ARRAY ARRAY['anon','authenticated'] LOOP
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=role_name) THEN
   EXECUTE format('REVOKE ALL ON public.knowledge_import_sections FROM %I',role_name);
  END IF;
 END LOOP;
END $$;
-- Archive originals can be viewed/edited/versioned, but never indexed as a monolithic source.
CREATE FUNCTION public.guard_knowledge_import_original() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.status='published' AND EXISTS(SELECT 1 FROM public.knowledge_import_sections WHERE source_document_id=NEW.id) THEN
  RAISE EXCEPTION 'Imported original is archival only; publish reviewed individual entries';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_knowledge_import_original BEFORE UPDATE ON public.knowledge_documents
 FOR EACH ROW EXECUTE FUNCTION public.guard_knowledge_import_original();
COMMIT;
