-- Review and apply manually in an isolated/staging database first. No business rows changed.
BEGIN;
CREATE TABLE IF NOT EXISTS cms_media (
 id uuid PRIMARY KEY, content_type varchar(32) NOT NULL CHECK(content_type IN ('image/png','image/jpeg')),
 content bytea NOT NULL CHECK(octet_length(content)<=2097152), width integer NOT NULL CHECK(width BETWEEN 1 AND 4096),
 height integer NOT NULL CHECK(height BETWEEN 1 AND 4096), actor_id integer NOT NULL, created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS cms_revisions (
 id bigserial PRIMARY KEY, actor_id integer NOT NULL, change_type varchar(64) NOT NULL,
 before_data jsonb NOT NULL, after_data jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS cms_revisions_kind_id ON cms_revisions(change_type,id DESC);
COMMIT;
-- Do not automatically DROP tables on rollback. Keep media and revision history.
