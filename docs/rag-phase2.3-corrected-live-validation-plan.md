# Phase 2.3 — Corrected payload: prepare one live validation

2026-10-11, `D:\miniproject\fpt`, branch `feature/rag-integration`.
**Prepared only, not approved/executed. Zero Gemini requests this turn.**
No embeddings generated, JDBC/PostgreSQL/Supabase activity, production key
changes, feature activation, commit/push/deploy. Only public Google REST
documentation was read; no Gemini endpoints or credentials were used.

## REST specification review

[Official models.embedContent REST reference](https://ai.google.dev/api/embeddings#method:-models.embedcontent)
documents POST `https://generativelanguage.googleapis.com/v1beta/{model=models/*}:embedContent`,
content.parts[].text input, taskType enum and integer outputDimensionality as
direct request fields. RETRIEVAL_QUERY is the documented query/retrieval task.
The current reference marks direct taskType/title/outputDimensionality fields
deprecated and also lists embedContentConfig; direct fields remain described.
This plan tests the documented direct-field compatibility path. It does not
claim nested config is universally invalid or that the server fix is proven.

Java `GeminiEmbeddingService.requestBody` produces exactly this JSON. The
offline Java validator serializes/parses it; PowerShell guards and transport
mocks verify its field placement, camelCase spelling, numeric type and value:

```json
{
  "model": "models/gemini-embedding-001",
  "content": {"parts": [{"text": "Nhà nhiều tầng WiFi yếu thì sao?"}]},
  "taskType": "RETRIEVAL_QUERY",
  "outputDimensionality": 768
}
```

Target: `https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent`.
Model matches previous HTTP 200 metadata evidence with embedContent support.
Metadata reports 2,048 input tokens; this public input is 32 code points / 40
UTF-8 bytes. Estimated 40–64 tokens is a heuristic, not measured usage.
No countTokens request. No title for query, nested options or autoTruncate.
Adapter request format is unchanged from the offline correction. Its runtime
metadata/retry flow is bypassed by this test runner, not altered for production.

## Isolated plan and approval

New plan ID: **phase23-corrected-payload-20261011-v2**.
Manifest: `test/rag/fixtures/phase23-corrected-embedding-plan.json`.
`approved=false`, `pendingStatus=prepared_waiting_explicit_live_approval`.
Old approval was consumed by v1 and does not authorize v2. Preparing a payload
does not grant live approval. The CLI switch records the administrator's
explicit execution approval when used after approval in this conversation;
it does not require silently flipping manifest approval during preparation.

Previous administrator confirmed Free Tier and quota snapshot before the first
POST: RPM 0/100, TPM 0/30K, RPD 0/1K. The new manifest cites this historical
confirmation, **not fresh quota verification**. Before approving execution,
confirm quota remains available in the same AI Studio project. Estimated cost
is 0 USD within the confirmed Free Tier/quota; no billing measurement or paid
upgrade. No new credentials or external infrastructure.

Distinct paths under .build (all ignored, none contains keys or vector data):

| Artifact | New path |
|---|---|
| Payload preview | .build/phase23-corrected-embedding-preview.json |
| Persistent execution lock | .build/rag-embedding-probes/phase23-corrected-payload-20261011-v2.request.lock |
| Sanitized result | .build/rag-embedding-probes/phase23-corrected-payload-20261011-v2.result.json |
| Mock-only metrics | .build/rag-embedding-probes/phase23-corrected-payload-20261011-v2.mocks.json |

Runner supports only fixed v1/v2 IDs (ValidateSet), not arbitrary paths, input,
model or dimensions. Defaults to v1, so administrator must select v2 explicitly.
Before HTTP: explicit approval switch, development/staging process gate and
literal RAG_ENABLED=false, test-key-only validation, exact plan/preview identity
and payload guard, local vector validator preflight, then atomic CreateNew lock.
Any existing result also refuses execution before claiming a new lock.
Paths are derived from the selected ID; old evidence is never used as v2 output.

Exactly one SendAsync, no redirects/cookies/application retries, ten-second
deadline and response body bound 64 KiB. Key read only from Process
RAG_EMBEDDING_API_KEY, never global/chatbot keys or database settings.
Raw headers, response/error body and vectors are never printed or saved.
Local Java stdin validation emits only metrics. Error messages use allowlisted
codes/static sanitized text. Token usage, when returned, is retained even if
vector dimensions are invalid. Cost remains an estimate, not billed usage.

Lock persists after success, provider errors, timeout or crash. Do not delete,
reset or rerun. If a process dies after claiming, assume the attempt may have
been sent and stop. No GET/chat/countTokens/DB/indexing/retrieval or 10–12 plan.

## Administrator command — ONLY after new explicit approval

Run in the existing PowerShell containing the test key; never paste the key
into chat, scripts or command arguments. Offline compile/preview already ran.

```powershell
$env:RAG_ENVIRONMENT = 'development'
$env:RAG_ENABLED = 'false'
pwsh -NoProfile -File 'D:\miniproject\fpt\test\run-rag-single-embedding.ps1' -PlanId 'phase23-corrected-payload-20261011-v2' -ApprovedSingleEmbeddingPost
```

If offline classes later become unavailable, prepare first without networking:

```powershell
pwsh -NoProfile -File 'D:\miniproject\fpt\test\prepare-rag-single-embedding.ps1' -PlanId 'phase23-corrected-payload-20261011-v2'
```

No execution command was run with approval in this preparation. Tests invoke
the wrapper without its approval switch, so they stop before any live client.

## Expected output and acceptance criteria (NOT actual API results)

Output is sanitized JSON plus `Sanitized result: <new result path>`.
Accept only:

- planId is v2, model gemini-embedding-001, keySource RAG_EMBEDDING_API_KEY.
- HTTP 200 and returnedDimensions=768, vectorValid=true, errorKind=null.
- All vector values numeric/finite, nonzero norm; normalized norm close to 1.
- embeddingPostRequests=1; retryRequests, metadataGetRequests,
  chatGenerationRequests, databaseWrites all 0.
- latencyMs measured; inputTokens reported only if returned, otherwise null
  with tokenUsageAvailable=false. Token usage availability is not a PASS gate.
- estimatedCostUsd=0 conditional on administrator-confirmed Free Tier/quota.
- No key/full vector/raw error/internal payload in output; old evidence intact.

HTTP 200 with 3072/other dimensions is FAIL without truncating or padding.
401/403/429/5xx/redirect/timeout/malformed/empty vector is FAIL, no retry.
This validates one query embedding only, not semantic retrieval or answer quality.
Stop after the single result, whether PASS or FAIL, and await next instruction.

## Executed offline verification

| Check | Actual result |
|---|---|
| prepare-rag-single-embedding.ps1 -PlanId v2 | PASS Java 17 --release 8 compile and exact builder preview |
| rag-single-embedding-tests.ps1 -PlanId v2 | **29 PASS / 0 FAIL** |
| Same suite -PlanId v1, compatibility only | **29 PASS / 0 FAIL** |
| RagBehaviorTest | **35 PASS / 0 FAIL** |
| RagPhase2CorpusTest | **13 PASS / 0 FAIL** |
| RagQualityTest | **27 PASS / 0 FAIL** |
| PowerShell syntax, 4 scripts; git diff --check | PASS |

**133 test executions PASS / 0 FAIL**, including the same 29-case suite run
for both plans; 104 cases when counting that parameterized suite once.
All providers/DAO mocked; no real API requests or database calls. Tests cover
wire JSON, rejecting stale/nonnumeric/wrong-case/wrong-dimension requests,
valid 768 normalization, invalid 3072, error sanitization, token usage on invalid
vectors, single transport call on failure, atomic duplicate claim, orphan
result protection, path separation, unsupported plan IDs and approval gate.
Claim tests use separate temporary mock roots, never actual execution locks.

Ignored logs: `.build/phase23-corrected-validation-offline-tests.log`, per-plan
`.mocks.json`, `.build/phase23-corrected-validation-evidence-check.json`.

## Previous evidence preserved

Compared SHA256, size and LastWriteTimeUtc before/after preparation for:

- v1 real result JSON (HTTP 200, 3072 dimensions, 835 ms).
- v1 execution lock.
- prior successful metadata JSON.
- pre-correction payload preview copy.
- prior v1 payload preview.

All **unchanged**. New actual v2 lock/result do not exist at preparation end.
Prior manifests/results/reports are not rewritten. No production verification
or mutations; .env.example remains RAG_ENABLED=false.

## Files changed this preparation

- `test/rag/fixtures/phase23-corrected-embedding-plan.json` (new pending plan).
- `test/run-rag-single-embedding.ps1` (select fixed PlanId, derive unique paths).
- `test/rag-single-embedding-helpers.ps1` (path mapping, real claim guard,
  plan/preview binding, per-plan metrics, token usage before vector validation).
- `test/prepare-rag-single-embedding.ps1` and `test/rag/RagSingleEmbeddingPlan.java`
  (generate selected preview without overwriting v1 evidence).
- `test/rag-single-embedding-tests.ps1` (parameterized mock/guard tests).
- This report.

No runtime chatbot/DAO/CMS changes this turn. **STOP: waiting for explicit
approval of v2 before any live POST.**
