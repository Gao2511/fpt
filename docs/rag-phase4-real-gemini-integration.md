# Phase 4 — Real Gemini integration implemented; live execution pending approval

Date: 2026-10-12. Project: `D:\miniproject\fpt`. Branch: `feature/rag-integration`.

**Code implementation: GO. Live execution / staging / production: NO-GO until the separate prerequisites and authorization below are completed.**

Authenticated Gemini API calls in this phase: **0**. Supabase production connections: **0**. Production settings/credentials/database writes, commit, push and deployment: **0**. Public Google documentation was consulted; this does not verify access in the administrator's project.

## 1. Existing integration audit

| Component | Evidence before Phase 4 | Correction |
| --- | --- | --- |
| Existing chat | `src/java/ai/AIService.java`, `src/java/ai/provider/GeminiProvider.java`: normal consultation already calls Gemini `generateContent` with Admin provider/model/backup configuration. | Reuse provider and ConsultationEngine; add a separate, explicitly authorized local path using only the test chat key. Ordinary chat remains configured as before, with production RAG disabled. |
| Admin indexing/playground | `src/java/dao/AdminKnowledgeDAO.java`: constructs `MockEmbeddingProvider`; offline index and playground never used real embeddings. | Retain those tools with clear MOCK labels. Add explicit GEMINI index and real retrieval preview actions. Upload/publish still make no API request. |
| Embedding adapter | `src/java/rag/GeminiEmbeddingService.java`: correct top-level 768 payload, but normal constructor verifies metadata with GET and permits three attempts. | Runtime overload receives externally verified access and permits **one POST**, no implicit GET/retry. Existing offline adapter behavior remains compatible. |
| Mock/real identity | `MockEmbeddingProvider.MODEL_NAME` also equals `gemini-embedding-001`; old cache/chunk/job identity was only model+dimensions. | Migration 006 separates provider provenance in documents/chunks/jobs. Historical unproven vectors remain MOCK; they are never promoted to GEMINI by model name. |
| Factory/key fallback | Previous `RagFactory` could use the chatbot key as an embedding fallback. | Remove fallback. New runtime requires both dedicated test keys; production RAG stays disabled. |
| Index failures | Existing mock worker uses bounded leased retries. | Real 429/budget failure -> WAITING_FOR_QUOTA; provider/network failure -> FAILED with retryable internal state. No automatic real retry or recovery of an expired real lease. |
| Admin model overview | `getRagSettings()` referenced legacy names/defaults. | Display actual `ai_active_model`, `ai_thinking_level`, `ai_fallback_model` configuration, with compatibility fallback for legacy settings. |

Reviewed/reused without replacing: `KnowledgeSourceSelection`, `RagContextBuilder`, `ConsultationPolicy`, `ConsultationReply`, `RetrievalQuery`, existing session manager, source approval/revision workflow, AdminFilter/security, JDBC conventions, migrations 002–005 and historical Phase 2 plans/locks.

## 2. Runtime architecture and safety boundary

New package: `src/java/rag/runtime/`.

`GeminiRuntimeConfig` accepts LOCAL_GEMINI only, development/test only, explicit authorization, `RAG_ENABLED=false`, `DB_HOST=127.0.0.1`, `DB_NAME=fpt_rag_test`, explicit test credentials and a new Phase 4 plan. OFFLINE_MOCK is the default. STAGING and PRODUCTION are named modes but **cannot activate this integration**.

The plan must specify exact approved models, thinking settings, 768 dimensions, Free Tier confirmation, quotas, request/token limits, document ID/version/hash, exact outgoing embedding payload hashes and exact chat questions. Verification is valid for at most one hour; it is checked again before every reservation. Missing, expired, unapproved or inconsistent configuration fails before any wire request. The proposal remains `approved=false`.

`GeminiRuntime` verifies the actual JDBC metadata host/path, `current_database()` and existence of migration 006 before constructing operational retrieval/indexing. An environment label alone is insufficient. Database verification and configuration load send no Gemini request and perform no migration.

### Indexing

Admin upload -> extraction -> draft -> review -> approval -> publish -> **explicit Request GEMINI INDEX** -> verify current approval/effective dates/version/hash -> preflight every chunk's content and payload allowlist -> leased job -> reserve API slot -> existing GeminiEmbeddingService -> finite 768-dimensional normalized vector -> transactional version/lease revalidation -> save GEMINI chunks -> INDEXED.

Only approved, published, effective sources can be indexed. Disallowed business claims/instructions are rejected before export. A change/unpublish during an API call cannot commit stale chunks. Mock cache values cannot satisfy a Gemini index job. Reindex is explicit and consumes remaining authorized slots; the same document payload operation cannot be sent twice in a plan. A failed/incomplete plan is not silently reset.

### Retrieval and chat

`AIService` in authorized LOCAL_GEMINI -> existing session/history/catalog -> `RetrievalQuery` -> RuntimeEmbeddingService (`RETRIEVAL_QUERY`) -> GEMINI-only hybrid search -> existing KnowledgeSourceSelection -> RagContextBuilder -> RuntimeGeminiProvider wrapping existing GeminiProvider -> ConsultationReply/ConsultationPolicy -> customer message.

Source authority, publication, effective dates, exact indexed version/hash and provider kind are checked. Selected sources are revalidated immediately before rendering. Prices, installation fees, Mesh information, consent/registration and other structured facts remain owned by the existing catalog/business policy; documents cannot replace them. Retrieved text is data, not system instructions. RAG answers continue to use validated source IDs and server-rendered approved excerpts, not arbitrary generated business claims.

For knowledge questions, an embedding/retrieval failure or empty trustworthy context prevents ungrounded Chat generation. Existing safe fallback/clarification is returned. Ordinary non-RAG conversation still passes the existing validator. Local mode caps initial provider attempts at one; malformed/truncated responses do not cause a repair API request. Backup, if separately enabled and verified, has at most two provider attempts, the same test chat key, independent low thinking, and the same durable budget gate. Unknown transport outcomes block additional calls, including backup.

## 3. Gemini REST verification

Public specifications were checked on 2026-10-12:

- [Gemini 3.8 generateContent REST guide](https://ai.google.dev/gemini-api/docs/generate-content/latest-model): exact `gemini-3.8-flash:generateContent`, `x-goog-api-key`, `generationConfig.thinkingConfig.thinkingLevel=medium`. Existing adapter omits deprecated sampling fields for Gemini 3 and sends a JSON response schema.
- [Thinking documentation](https://ai.google.dev/gemini-api/docs/thinking): backup uses `gemini-3.5-flash-lite`, low thinking. Runtime never substitutes another model.
- [Embedding REST specification](https://ai.google.dev/api/embeddings): POST `gemini-embedding-001:embedContent`. Current documentation still lists top-level taskType/title/outputDimensionality but marks them deprecated in favor of EmbedContentConfig. Phase 4 retains the already corrected legacy REST payload and validates every returned vector; no automatic payload/model migration was introduced.

Example wire shape (no key):

```json
{
  "model": "models/gemini-embedding-001",
  "content": {"parts": [{"text": "EXACT ADMIN-APPROVED CHUNK OR QUERY"}]},
  "taskType": "RETRIEVAL_DOCUMENT",
  "outputDimensionality": 768,
  "title": "EXACT APPROVED DOCUMENT TITLE"
}
```

Query task is RETRIEVAL_QUERY, without title. Document/query use the same model/dimensions and L2 normalization. Wrong dimensions, zero/nonfinite vectors and malformed usage are rejected. Requests have timeouts and response-size limits. Redirects are disabled. Fixed-length request streaming prevents the JDK from replaying a buffered POST on EOF; actual loopback socket fault-injection tests verify this for both transports.

**Not verified in this phase:** account-specific access to any model, current project tier/quota and semantic quality of real embeddings. No metadata GET or live model listing was authorized or executed. Historic metadata success cannot replace fresh confirmation for this plan.

## 4. Durable request / quota / cost controls

Migration 006 adds `rag_runtime_plans` and `rag_api_requests`, enables RLS and revokes public/anon/authenticated access. JDBC backend identity must be permitted to use these tables in the isolated environment. No browser gets database/key access.

Before wire, `PostgresRequestLedger.reserve()` commits a RESERVED row. Plan row locks, project advisory transaction locks, unique plan/operation hashes and immutable plan fingerprint coordinate independent JVMs. Counters enforce total/group budgets, rolling 60-second/24-hour request limits and conservative token reservations. Failed requests count. Another in-flight or UNCERTAIN request for the verified project blocks new requests; unknown process/network state is never assumed safe. Finish records status/error enum, elapsed time and usage when available, not headers, raw provider errors, keys, question/content or vectors.

Completion state means the HTTP operation ended, **not that retrieval/answer quality passed**. Output validation remains separate. Unreported token usage is NULL, not a measured zero. Embedding input is conservatively bounded by UTF-8 bytes (text plus title) to 2,048; no countTokens call is used. Chat reserves prompt/schema bytes conservatively plus up to 1,200 output/thinking tokens. Longer input can therefore be rejected even when a real tokenizer might admit it.

Limits are operator-configurable in the plan, capped in code. They do not measure other applications' traffic or query live account quotas. Correct project aliases and fresh account attestation are required; do not rename a project alias to bypass budgets. No key rotation, billing operation, paid fallback, tool/search request or automatic model discovery exists in this path.

[Google pricing](https://ai.google.dev/gemini-api/docs/pricing) currently lists Free Tier text input/output for the selected chat models; paid 3.8 introductory prices are $0.75/M input and $3.75/M output including thinking through 2026-12-31. The current English table no longer explicitly lists embedding-001; the [official localized pricing page](https://ai.google.dev/gemini-api/docs/pricing?hl=es) lists its Free Tier / $0.15/M paid reference. **Embedding-001 access and pricing must be reconfirmed for the actual project; do not assume it inherits embedding-2 pricing or switch models.**

Expected smoke cost is **$0 only if both actual projects are confirmed Free Tier and within quota at execution**. Maximum authorized paid spend: $0. The runtime rejects a paid/unverified plan. This is an application policy, not a guarantee about Google billing; changed/missing tier verification means NO-GO.

## 5. Admin CMS changes

Existing publish/unpublish/disable-source workflow remains. New explicit real indexing action, reindex link and real retrieval preview are guarded by the existing Admin verification, CSRF checks and demo restrictions. GET/publish/upload never invoke Gemini. Real controls appear only with valid LOCAL_GEMINI configuration.

Display: **MOCK INDEX** versus **GEMINI INDEX**, PENDING, PROCESSING, INDEXED, WAITING_FOR_QUOTA, FAILED and STALE, indexed revision, last indexed timestamp and sanitized embedding error. Retryable internal failures display FAILED and retain `failed_retryable` in job/document state; manual review/retry authorization is required. A real document does not expose a misleading mock reindex button. Preview explicitly says it consumes one query embedding and sends no Chat request. Existing mock preview remains available and clearly labeled.

## 6. Phase 4 files created/modified

These are Phase 4 changes; the workspace contains earlier uncommitted Phase 1–3 work that has been preserved.

| File(s), relative to project root | Change |
| --- | --- |
| `.env.example` | Safe mode/test-key/plan environment names; defaults remain off. |
| `database/migrations/006-rag-gemini-runtime.sql` | Provider provenance, diagnostics/states and protected request journal. |
| `src/java/rag/runtime/GeminiRuntimeConfig.java`, `ApiRequestLedger.java`, `PostgresRequestLedger.java` | Short-lived explicit authorization and durable limits. |
| `src/java/rag/runtime/RuntimeEmbeddingService.java`, `RuntimeGeminiProvider.java`, `GeminiRuntime.java` | Real adapter wiring, isolated database checks and strict retrieval/chat. |
| `src/java/rag/EmbeddingService.java`, `GeminiEmbeddingService.java`, `RagException.java`, `RagFactory.java`, `KnowledgeIndexingService.java`, `KnowledgeRetrievalService.java` | Provider identity, single-attempt runtime, safe failure/state/grounding handling; retain offline mocks. |
| `src/java/dao/KnowledgeChunkDAO.java`, `KnowledgeIndexJobDAO.java` | Separate vector/cache/job identities and explicit real retry states. |
| `src/java/dto/KnowledgeIndexJobDTO.java`, `AdminKnowledgeDTO.java` | Provider provenance and Admin status/diagnostic properties. |
| `src/java/dao/AdminKnowledgeDAO.java`, `src/java/controller/AdminKnowledgeBaseServlet.java` | Approved real indexing, explicit preview, model overview and Admin actions. |
| `src/java/ai/AIService.java`, `src/java/ai/consultation/ConsultationEngine.java`, `src/java/ai/dto/ChatOptions.java`, `src/java/ai/provider/GeminiProvider.java` | Local test path, attempt bounds, grounding, header authentication/no redirect/no implicit POST replay. |
| `web/view/admin/knowledge-gemini-runtime.jsp`, `knowledge-base.jsp` | Existing theme reused; clear modes/states/actions. |
| `test/rag/RagPhase4RuntimeTest.java`, `RagPhase4PostgresTest.java` | Focused injected-provider and isolated JDBC tests. |
| `test/ai/provider/ProviderResponseTest.java` | Actual loopback REST/header/model/redirect/EOF tests. |
| `test/run-rag-phase4-offline.ps1`, `run-rag-phase4-postgres.ps1` | New focused runners. |
| `test/run-rag-isolated.ps1`, `run-rag-phase316-uat.ps1` | Apply migration 006 in new isolated test databases; add Phase4 selection. Full Phase3.16 UAT was not rerun. |
| `test/rag/fixtures/phase4-gemini-smoke-plan.json` | New **non-executable, approved=false** proposal; old Phase2 plan/locks not reused. |
| `docs/rag-phase4-real-gemini-integration.md` | Audit, implementation, measured checks, blockers and proposal. |

No system prompt rewrite, package/business prices change, registration change, unrelated website redesign or dependency introduction was made in Phase 4.

## 7. Executed offline verification

JDK17/Ant build succeeded. Local PostgreSQL **17.10**, pgvector **0.8.2**, pinned image; migrations **002–006 succeeded**. Dedicated fresh `127.0.0.1:<ephemeral-port>/fpt_rag_test` databases were used. Test containers were stopped and retained; no old container/volume/data was deleted.

| Executed suite | Result |
| --- | --- |
| Phase4 runtime authorization/adapter/response/failure tests | **46/46 PASS** |
| Phase4 PostgreSQL durable journal/concurrency/indexing/retrieval/E2E tests | **24/24 PASS** |
| Existing RAG behavior/corpus/quality/subject regression | **35 + 13 + 27 + 22 = 97/97 PASS** |
| Existing Admin KB extraction/RBAC/CSRF/demo security regression | **17/17 PASS** |
| Existing consultation/policy/history/backup regression | **32/32 PASS** |
| Actual loopback provider parsing/model/header/no-redirect/no-POST-replay tests | **11/11 PASS** |
| Jasper compilation of 18 CMS/public JSP entry pages, including new KB runtime include | **0 errors** |

Total independent behavior cases: **227/227 PASS**, excluding build/JSP compilation. No full Phase3.17 real-document UAT or real `văn bản.txt` validation is claimed. Tests inject synthetic keys and mocked API results. Successful mock E2E proves wiring/guards, not measured Gemini semantic relevance.

Two initial PostgreSQL test attempts failed because the fake Chat provider incremented its own counter twice; the corrected test and subsequent full focused runs pass. Earlier standalone Admin test invocation lacked fake DB environment setup; rerun with isolated fake configuration passed. The loopback mock initially required the sandbox's local-network permission; it was then executed successfully. These failed attempt logs remain preserved.

Evidence directory: `D:\miniproject\fpt\.build\phase4-evidence\2f2c9471a4e04f278ef9cca4dab07fb5`.
Authoritative logs: `phase4-offline-authoritative.log`, `postgres-confirmed.log`, `provider-no-replay.log`, `rag-regression.log`, `rag.RagAdminKbTest.isolated.log`, `ai.ConsultationBehaviorTest.isolated.log`, `build-final.log`, `jsp-final.log`. Logs are in a newly generated directory, separate from historical evidence.

Final focused PostgreSQL container: `fpt-rag-test-222cac01dd65` (stopped, retained). Earlier test containers from this phase are also retained. Private synthetic database configuration resides only in ignored `.local/rag-tests`.

## 8. Operator setup and remaining blockers

The Codex process currently has neither `RAG_EMBEDDING_API_KEY` nor `RAG_CHAT_TEST_API_KEY`. Only presence booleans were inspected. No other process/user environment, production settings key or credential file was searched/copied.

Required secret environment names (values are never requested in chat): `RAG_EMBEDDING_API_KEY`, `RAG_CHAT_TEST_API_KEY`, `DB_USER`, `DB_PASSWORD`. Existing local application encryption/auth environment must be configured through its existing mechanism; do not change production encryption keys.

Other runtime names: `RAG_RUNTIME_MODE`, `RAG_ENVIRONMENT`, `RAG_ENABLED`, `RAG_DATABASE_APPROVED`, `RAG_LOCAL_GEMINI_AUTHORIZED`, `RAG_LIVE_PLAN_PATH`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SSLMODE`.

Before live:

1. Use a dedicated local test database with migrations 002–006. Do not point to Supabase or copy private customer records/production keys into it. Seed only approved nonprivate business/knowledge data needed for the test. The tests' containers are not an automatically configured local Admin deployment.
2. Configure the two test keys in the **same process environment used to start local Tomcat**. Environment set in another PowerShell window is not inherited by an already running process. Do not paste keys in chat, commit them or use production fallback.
3. Confirm exact embedding and primary Chat model access, REST configuration, current Free Tier status and available RPM/TPM/RPD in the correct AI Studio projects. Confirmation must be fresh (<=1 hour). Public documentation alone does not satisfy this. If unavailable, remain blocked; no model change/billing enablement/metadata request is implied.
4. Confirm local Admin provider/model/thinking configuration: Gemini, 3.8 Flash, medium, backup disabled. Existing production configuration is untouched.
5. Select **one actual approved, published, effective, single-chunk document** from the isolated Admin KB and one related support question. Do not reuse the offline synthetic fixture as proof of real document testing. Record its exact ID/version/hash, title/chunk and resulting outgoing body hash. Record raw Admin query and the existing RetrievalQuery expansion used by customer chat; approve both exact query payload hashes.
6. Create an ignored private execution copy of the new Phase4 proposal with those exact allowlists, consistent project aliases and fresh verification/quota fields. Keep `approved=false` and LOCAL authorization off until the administrator separately approves the completed plan. Do not edit historical plans/locks. Once requests begin, plan fingerprint is immutable; do not reset its journal or create a new plan to conceal uncertain execution.

Current non-executable proposal lacks the actual selected document/question/hashes, fresh project verification and keys. Therefore it cannot be used to send requests simply by clicking an Admin button. No live runner or new external service is automatically launched.

## 9. Limited live smoke-test proposal (NOT APPROVED / NOT EXECUTED)

New plan ID: **phase4-gemini-smoke-20261012-v1**. File: `test/rag/fixtures/phase4-gemini-smoke-plan.json`, `approved=false`, `productionRagEnabled=false`, backup disabled.

| Order | Operation | Maximum Gemini requests |
| --- | --- | --- |
| 1 | Explicit Admin GEMINI INDEX for the selected single chunk, RETRIEVAL_DOCUMENT, 768 | 1 POST embedding |
| 2 | Admin real retrieval preview for the exact approved support question, RETRIEVAL_QUERY | 1 POST embedding |
| 3 | Existing customer question path in isolated local mode: query embedding and grounded 3.8 Flash/medium JSON Chat | 1 POST embedding + 1 POST Chat |
| Total | Document=1, query=2, Chat=1 | **at most 4 requests** |

No GET, countTokens, retry, redirect, embedding batch, model discovery, tools, backup or extra question. A cached real vector may reduce calls; choose a document without existing GEMINI cache for the proposed fresh-embedding check. A multi-chunk document is outside this plan. Stop operator execution on the first failure, non-INDEXED state, empty/ambiguous preview or invalid/truncated/ungrounded Chat result; no re-click or next operation. Provider failures remain counted, and unknown outcomes block requests at the journal gate. This is a controlled manual Admin/customer smoke procedure, not a background live worker.

Acceptance requires **all**: finite 768-dimensional normalized vectors, document/query model identity, confirmed indexed revision, expected approved source selection, unchanged catalog/business facts, grounded Vietnamese response with no internal metadata, correct counts <=4, no production destination/key and no failed/uncertain journal state. HTTP 200 alone is not PASS. Latency/usage/status must be taken from actual journal rows; no speculative semantic/latency result is reported here.

Quota: embedding needs >=3 available request slots, Chat >=1; if same project, >=4 combined and conservative shared token reservations. Planned safety RPM/RPD=4, TPM=30,000, daily reserved tokens=60,000, max Chat output+thinking=1,200. Set lower limits if the actual account allows less; insufficient limits cause NO-GO. Each embedding input reserves <=2,048 tokens conservatively; Chat reserves full prompt/schema byte bound plus output. Operator must check the generated prompt/context fits before approval; a reservation that would exceed these limits sends no request. Historical quota readings are not reused.

Evidence: dedicated `rag_runtime_plans` row/fingerprint and committed `rag_api_requests` operation journal in **fpt_rag_test**; export a sanitized status/count/usage/latency-only report into a new Phase4 directory using exclusive new-file creation. Keep all old evidence intact. No automatic overwrite/reset or second approval is inferred. API response bodies/full vectors/keys must not be exported.

## 10. Local verification and rollback

Offline commands (existing local installations; no Gemini):

```powershell
./test/run-rag-phase4-offline.ps1 -AntHome 'D:/Subject FPT/PRJ301/NetBeans-13/netbeans/extide/ant'
./test/run-rag-isolated.ps1 -Phase4
./test/run-rag-phase2-offline.ps1 -AntHome 'D:/Subject FPT/PRJ301/NetBeans-13/netbeans/extide/ant'
```

The isolated runner creates/stops only its own fresh loopback PostgreSQL container and preserves it. Migration 006 is manual and was applied only to those tests. Build output is not deployed.

Rollback of local live configuration: stop the local test runtime, set `RAG_RUNTIME_MODE=OFFLINE_MOCK`, `RAG_LOCAL_GEMINI_AUTHORIZED=false`, keep `RAG_ENABLED=false`, and restart only the isolated local instance. Disable/unpublish a problematic test source using the existing Admin action. Preserve migration columns, versions, journals, locks and evidence; no destructive down migration, production DB change or key rotation is needed. Do not clear UNKNOWN/RESERVED evidence to retry. Review actual outcome and obtain a new explicit authorization if further testing is necessary.

No production deployment is proposed. Staging/production requires a later phase with explicit environment authorization, model/account verification, real semantic evaluation, privacy review, operational queue management and rollout approval.

**Final state: REAL GEMINI INTEGRATION IMPLEMENTED — LIVE EXECUTION PENDING APPROVAL.**
