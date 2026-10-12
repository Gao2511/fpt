# Phase 1 — RAG MVP, 2026-10-11

Historical Phase 1 report. Current source also requires manual migration 003 and
the source/query safeguards documented in [Phase 2.1](rag-phase2.1-quality.md).
No migration is run automatically; website RAG remains default-off.

## State and boundaries

Branch `feature/rag-integration`, created from approved `admin-ui-mesh` commit
`48b700f4f331bf56a327a197288c82d68761f7be`. Existing `.local/` was preserved.
No commit, push, merge, website deployment, production database writes or billable AI calls.
`RAG_ENABLED` defaults to false; no startup migration, background indexing or activation.
The existing chat provider/model settings are unchanged. CMS pages and registration flow remain intact.

## Implemented architecture

`KnowledgeDocumentDAO` handles draft/publish/update/archive with optimistic document versions.
Publication and index-job enqueue commit atomically. Published content requires a trusted actor.
There is no public knowledge-write endpoint or Admin Knowledge Base UI in this phase.

`DocumentChunkingService` produces deterministic chunks up to 1,000 characters with approximately
100-character overlap, at most 100 chunks per document. Document/title/source/category hashes
invalidate old versions; title + chunk hashes deduplicate document embeddings within a job
and reuse identical stored embeddings in the same model/dimension space.

`KnowledgeIndexJobDAO` uses SKIP LOCKED, lease tokens, renewal and at most three job attempts.
`KnowledgeIndexingService` embeds outside the publish transaction, rechecks the document/version
under lock, then atomically replaces chunks and marks the revision ready. Old workers cannot
publish over a newer edit or a renewed lease. Failures store enum error codes only.
No worker starts automatically; the operator tool must be invoked explicitly.

`KnowledgeChunkDAO` combines accent-normalized PostgreSQL full-text OR queries and exact cosine
similarity. It uses a GIN text index and ordinary relational indexes, no ANN index. Model and
dimension must match. Only published, approved, currently effective, unexpired documents whose
current revision is fully indexed can be retrieved. Updating a document immediately makes its
old chunks ineligible until its new revision is ready. No stale document/result cache fallback.
Ranking weights and the semantic threshold are initial MVP settings, not benchmarked production
relevance guarantees. Validate a real Vietnamese FAQ corpus before activation.

`KnowledgeRetrievalService` permits lexical retrieval if query embedding is unavailable or not
approved. SQL failures return no sources and preserve the original consultation fallback.

`ConsultationEngine` preserves server-owned package/installation/registration decisions. Generic
contract/FAQ questions can reach retrieval before the legacy contract/hotline shortcut. Explicit
human-contact and private billing requests keep their controlled handoff. With RAG disabled,
the existing chatbot behavior remains available.

`RagContextBuilder` puts JSON-escaped excerpts in the user/data role, never in the system prompt.
The existing admin-editable prompt is not rewritten. An additional protocol/schema is used only
when safe knowledge sources exist: the provider selects `knowledgeChunkIds`; unknown, duplicate,
malformed or unsolicited action/package fields are rejected. The server revalidates selected
sources and renders approved excerpts itself. Generated policy prose is not displayed.
Source IDs, hashes and structured objects never reach the chat UI or session response text.

This is a conservative **extractive** MVP, not unrestricted policy synthesis. Financial/package
claims and suspicious instructional content cause whole chunks to be excluded. Approval,
structured reference selection, current-version checks and server rendering supplement the
content screening; safety does not depend on stripping tags or on keyword blocking alone.
Financial/promotional policy documents are not suitable for this MVP: package prices, speeds,
installation charges and promotions remain authoritative in the existing business data.

Customer history remains the existing process-local session store. RAG does not make history
persistent across restarts or shared across backend instances.

## Migration and access

`database/migrations/002-rag-mvp.sql` creates `knowledge_documents`, `knowledge_chunks` and
`knowledge_index_jobs`, with dates, approvals, versions, hashes, embedding identity/dimensions,
timestamps and indexing/job status. It is a one-time transactional migration, fails safely if
pgvector is missing or tables already exist, and never installs an extension automatically.
Record migration 002 in the operator's migration ledger; do not run it twice blindly.

RLS is enabled, PUBLIC privileges are revoked, and existing `anon`/`authenticated` roles are
explicitly denied table/sequence privileges. A non-owner backend role also needs deliberate
grants and named RLS policies. On staging, an administrator should grant only SELECT/INSERT/
UPDATE/DELETE on these three tables and USAGE/SELECT on their sequences to a dedicated backend
role, with policies restricted to that role. Do not grant this access to browser/REST roles,
and do not give the website superuser or schema/extension-management permissions.
The future admin controller must verify `AdminSecurity`/CSRF and the current admin actor before
calling DAO publication methods; an integer actor ID alone is not an authentication mechanism.

No CMS or RAG migration was applied to Supabase. The earlier audit found migration 001 absent
there; production CMS readiness and applying 001 need their own reviewed deployment plan.

## Embeddings: setup remains gated

Adapter: `GeminiEmbeddingService`, model `gemini-embedding-001`, 768 dimensions. It uses Google's
`embedContent` endpoint, `RETRIEVAL_DOCUMENT` with title, and `RETRIEVAL_QUERY` for questions.
The request uses `embedContentConfig` with explicit dimensions and `autoTruncate=false`.
Vectors are checked for size/nonfinite/zero values and normalized. HTTP transport has a total
deadline, connect/read timeouts, bounded response size and at most three attempts for transient
failures. Keys are sent in `x-goog-api-key`, not URLs; redirects are disabled.

Official references checked on 2026-10-11:

- [Embedding API request/response](https://ai.google.dev/api/embeddings)
- [Embedding dimensions/task types/normalization](https://ai.google.dev/gemini-api/docs/embeddings)
- [Account model metadata](https://ai.google.dev/api/models)
- [Current pricing](https://ai.google.dev/gemini-api/docs/pricing)
- [Project-specific quota](https://ai.google.dev/gemini-api/docs/rate-limits)

Documentation still describes embedding-001 and reduced-dimensional normalization. The current
pricing page emphasizes embedding-2 and does not establish an embedding-001 account price for
this project. **Exact project pricing/quota and actual account access have not been verified.**
Do not infer that requests are free, do not enable billing, and do not change the chat model.

Required operator steps before live embedding:

1. Confirm embedding-001 is supported for this account using a non-generating model metadata GET.
2. Check AI Studio project quota, billing tier, applicable pricing, availability and data terms.
3. Obtain explicit cost approval, then set the two verification/approval flags.
4. Make an approved 768-dimensional staging probe and verify the real response shape/vector length.
5. Index only reviewed public knowledge, never customer records, API settings, secrets or logs.

The adapter checks account metadata before its first embedding request. A metadata error blocks
POST; the flags do not replace that check. Document and query embeddings can both incur costs.
Changing the embedding model/dimensions is not supported by this migration/config: plan a new
space/migration and full reindex rather than mixing vectors.

## Environment variables — names only

Application connection/key prerequisites:
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE`, `AI_ENCRYPTION_KEY`.

Other existing services if used:
`RESEND_API_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`.

RAG:
`RAG_ENABLED`, `RAG_EMBEDDING_MODEL`, `RAG_EMBEDDING_DIMENSIONS`,
`RAG_EMBEDDING_MODEL_VERIFIED`, `RAG_EMBEDDING_CALLS_APPROVED`, `RAG_EMBEDDING_API_KEY`,
`RAG_ENVIRONMENT`, `RAG_DATABASE_APPROVED`.
Tests additionally require `RAG_TEST_DATABASE_APPROVED`; build tools use `ANT_HOME`/`CATALINA_HOME`.
See `.env.example`. The application does not automatically load .env files.
An explicit embedding key may be supplied; public chat can reuse its existing encrypted Gemini
key when approved. The operator tool requires the explicit embedding-key environment variable.

## Security changes and operator prerequisites

Removed embedded DB credential defaults from the active `utils/DBUtils.java`. The misspelled
`ultis/DBUtils.java` exists only in history, not in this approved source. The new build requires
complete DB environment configuration, retains required remote SSL and permits SSL disable only
on loopback. Do not deploy until connection configuration has been verified on staging.

The fixed encryption fallback was externalized without rotating/re-encrypting any stored data.
Encryption format/derivation stay AES-256-GCM/SHA-256. The operator must configure the **same
effective key** used by existing ciphertexts. No default/generated replacement key exists.
If cloud already has an explicit key, keep that value; do not overwrite it with the historical
fallback preserved locally. The backup does not establish the cloud's effective runtime key.
Encryption failure no longer returns plaintext for storage; decryption failure logs no secrets.

For non-destructive preparation, prior fallback values were preserved privately in ignored
`.local/phase1-legacy-config.json`; existing `.local/runtime.json` was not changed. This file is
operator configuration, not a test fixture, and is never read by RAG tests or included in Git,
Docker context or WAR. Protect its local access and remove it only after a reviewed transition.
Do not paste its contents into tickets, terminal output or documentation.

Email logging no longer prints OTP, recipients, subjects, bodies or raw provider responses.
Missing mail configuration reports failure rather than falsely reporting a sent email. DAO
diagnostic logs report exception class/SQLState without messages, SQL contents or stack traces.
Development `TestController` and private OAuth properties are excluded from new artifacts.
Test key fixtures are explicitly synthetic. Ignore rules cover local/env/dependency/build data.

Deferred operator actions: rotate/revoke exposed DB/OAuth/API credentials after preparing
replacement config; inspect prior publication scope; approve any Git-history cleanup separately.
Existing tracked historical/prebuilt WAR files still contain old code and are not sanitized by
this patch. The original Dockerfile remains unchanged; **do not assume its prebuilt WAR contains
these fixes**. Use the reviewed source-built artifact/pipeline only after deployment approval.

Before any key rotation: back up settings/ciphertexts, verify the current key without logging
plaintext, test versioned re-encryption on a staging copy, approve a production migration, preserve
old ciphertext/key rollback securely, and restore that pair together if needed. None was executed.

## Reproducible build and local verification

New portable Ant build does not import NetBeans private build configuration. Libraries are pinned
by version and SHA256 from Maven Central; servlet API is compile-only. Unused Cloudinary reference
was removed (no source imports). Canonical JavaMail 1.6.2 differs from the local archive metadata
but all 340 class entries are identical. Existing website runtime libraries were not overwritten.

Prerequisites: JDK 17, Ant 1.10+, Tomcat 9 for JSP checks, and Docker Desktop for isolated DB tests.

```powershell
./tools/bootstrap-libs.ps1
./tools/build-portable.ps1 -AntHome $env:ANT_HOME -Test
./test/compile-cms-jsps.ps1 -TomcatHome $env:CATALINA_HOME
node test-e2e/chatbot-test.cjs
./test/run-rag-isolated.ps1
```

The last command creates a new loopback-only database/container with generated test credentials,
installs vector **only there**, applies migration 002, runs JDBC tests with mock embeddings, and
stops the container in finally. It never reads the website's local/production credentials. It
preserves stopped test containers/private config for inspection rather than deleting data.
The integration Java suite refuses non-loopback/non-test databases and refuses existing data.

Source-only Linux check, without deployment:

```powershell
docker build -f Dockerfile.rag -t fpt-rag-mvp:phase1 .
```

WAR output: `.build/dist/fpt-sale-rag.war`; existing tracked dist/WAR and running Tomcat were not
replaced. Dockerfile.rag has a separate ignore file excluding old dist artifacts and private data.
Two independent clean Windows build directories produced the same WAR SHA256 with the same
JDK/dependencies. This is not a promise of byte identity across different JDKs/base image updates.
Fresh build directories are recommended when deleting/renaming Java classes (Ant is incremental).

For approved staging, configure the intended database explicitly, have its administrator install
vector and apply the reviewed migrations/grants, then:

```powershell
./tools/run-rag-tool.ps1 -Operation check
./tools/run-rag-tool.ps1 -Operation verify-model
# Only after database/cost approval and environment flags:
./tools/run-rag-tool.ps1 -Operation index -Limit 1
```

The tool refuses environments other than development/staging. `index` additionally requires
database approval, model verification and embedding-call approval. It does not activate RAG or
submit customer registrations. It is deliberately excluded from the production WAR.

## Verification results

Executed suites: CMS security 34; CMS behavior 24; local SSL 9; consultation 26; adaptive
conversation 21; installation fee 17; registration 9; provider HTTP adapter 7; multi-provider
security 6; RAG behavior 34. CMS editor field grouping also passed. Frontend behavior: 6 passed.
Jasper compiled 17 CMS/public JSPs with zero errors.

Real PostgreSQL 17.10 / pgvector 0.8.2 and PostgreSQL 17.11 / pgvector 0.8.7 local integration:
the same 17 tests passed on each, including CRUD,
optimistic edits, atomic enqueue/index, published-only/date filtering, FTS/vector relevance,
hash reuse, archive/current-source validation, rollback after partial insertion, concurrent
claims, stale revisions, expired leases, exhausted attempts, and RLS. Embeddings were mocks.
Migration 002 applied successfully only to fresh test containers; no Supabase writes.
Supabase audit reported vector available 0.8.2, not installed; its role configuration and
staging deployment still need verification before production activation.

Build: JDK17/Java8 target and source-only Docker Linux build passed. New WAR checks found zero
matches for prior embedded DB password/encryption fallback and zero forbidden private/development
entries. There were no billable calls or live chat/embedding quality tests.

## Remaining work and safe activation/rollback

Add Admin Knowledge Base list/editor/preview, authenticated publish/archive, source/date validation,
audit/revision history, CSRF and indexing-status display in the next phase. Queue indexing only
after trusted publication; do not automatically copy all CMS/settings/customer tables into RAG.
Validate representative Vietnamese FAQs and injection/financial edge cases with the actual model
and approved corpus on staging. Benchmark retrieval thresholds and corpus size before considering
ANN indexes. Current safety screening is deliberately conservative and may exclude useful text.

Production activation is a separate approval: backup, configure secrets/grants, verify migrations
and artifact provenance, deploy with all RAG flags false, verify CMS/chat regression, then approve
embedding cost/indexing and finally enable RAG. Default-off code does not remove the need to
prepare DB/encryption/mail/OAuth environment configuration before deploying security changes.

Immediate RAG rollback: disable `RAG_ENABLED` and embedding-call approval, stop explicit workers,
keep knowledge tables/data intact, and return to the original consultation path. Full application
rollback: restore the previously reviewed WAR and its matching configuration/key; account for
the security risks of older artifacts. Do not drop tables/extensions or change package data as
part of rollback. No production rollback/deployment action was performed in Phase 1.
