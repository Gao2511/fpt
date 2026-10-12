# PHASE 2.4.8 — FINAL LIVE AUTHORIZATION PREPARATION REPORT

- **Project:** `D:\miniproject\fpt`
- **Branch:** `feature/rag-integration` (`HEAD: 48b700f`)
- **Date:** `2026-10-11`
- **Plan ID:** `phase24-e2e-20261011-v1` ([`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json))
- **Execution Mode:** **OFFLINE ONLY** (`realGeminiApiRequests = 0`, `supabaseProductionConnections = 0`)
- **Authorization Boundary State:** `approved = false`, `executionAuthorized = false`, `readiness.executable = false`, `verifiedAtEpochMs = null`, `expiresAtEpochMs = null`, `RAG_ENABLED = false`
- **Final Status:** **READY FOR FINAL AUTHORIZATION — NOT EXECUTED**

---

## 1. Confirmed Administrator Decisions

In Phase 2.4.8, the Administrator explicitly confirmed the following three governance items (which have been recorded in [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json)):

1. **API Key / Project Mapping Confirmed (`dedicatedTestKeysConfirmed = true`):**
   - `RAG_CHAT_TEST_API_KEY` belongs to the intended Free Tier Chat project (**Project A — `Default Gemini Project`**, hosting `gemini-3.8-flash`).
   - `RAG_EMBEDDING_API_KEY` belongs to the intended Free Tier Embedding project (**Project B — `Gemini Project`**, hosting `gemini-embedding-001`).
   - Neither key is a production key, and both exist in the Administrator's PowerShell 7 session (`PRESENT`).
2. **Free Tier & Quota Confirmed at Confirmation Time (`cost.* = true`, `quota.verifiedForThisPlan = true`):**
   - Both projects are on **Free Tier** (`$0.00` input / `$0.00` output including thinking tokens; `paidRequestsAllowed = false`, `maximumApprovedPaidCostUsd = 0`).
   - **Gemini Chat (`gemini-3.8-flash`):** At least `6` available requests (`chatRpdRemaining >= 6`, `chatRpmRemaining >= 1`, `chatTpmRemaining >= 17,648`).
   - **Gemini Embedding (`gemini-embedding-001`):** At least `8` available requests (`embeddingRpdRemaining >= 8`, `embeddingRpmRemaining >= 1`, `embeddingTpmRemaining >= 2,048`).
   - *Time-Sensitivity Notice:* Because Google Cloud project quotas are shared across the project and RPD resets daily at Midnight Pacific Time (`14:00 ICT`), the Administrator must re-confirm immediately before live execution that no other activity has consumed those slots.
3. **Corpus `L1` & `L2` Approved Strictly for Isolated PostgreSQL Testing (`approvedForLive = true`, `approvedForIsolatedTestOnly = true`, `productionPublicationApproved = false`):**
   - `L1` and `L2` are approved **ONLY** for indexing inside the disposable local Docker PostgreSQL container (`127.0.0.1 / fpt_rag_test`).
   - They are **NOT** approved for production publication and must never be labeled as official FPT policies or written to Supabase production.
4. **Live Execution NOT Yet Authorized:**
   - The Administrator has **NOT** authorized real Gemini API execution in this step.
   - `approved = false`, `executionAuthorized = false`, `readiness.executable = false`, and `verifiedAtEpochMs = null` / `expiresAtEpochMs = null` remain strictly locked.

---

## 2. Corpus Approval Verification (`L1` & `L2`)

[`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json#L36-L61) was inspected and compared byte-for-byte against the original source files in the repository. **Zero discrepancies were found:**

| Field | Document `L1` | Document `L2` |
|---|---|---|
| **Document ID** | `L1` | `L2` |
| **Title** | `Hướng dẫn kết nối mạng WiFi` (`34` UTF-8 bytes) | `Hướng dẫn chơi game` (`24` UTF-8 bytes) |
| **Exact Content (`UTF-8`)** | `"Anh/chị kiểm tra kết nối dây mạng an toàn trước khi liên hệ hỗ trợ."` (`87` UTF-8 bytes) | `"Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế."` (`146` UTF-8 bytes) |
| **Original Source File** | [`test/rag/RagQualityTest.java`](file:///d:/miniproject/fpt/test/rag/RagQualityTest.java#L23) (`GUIDE` constant) — **Exact Match** | [`src/java/ai/consultation/ConsultationPolicy.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationPolicy.java#L180) — **Exact Match** |
| **Expected Chunks** | `1` chunk (`DocumentChunkingService().chunk(content).size() == 1`) | `1` chunk (`DocumentChunkingService().chunk(content).size() == 1`) |
| **Test DB Metadata** | `category = "guide"`, `policyKey = "phase241.l1"`, `authority = "admin_reviewed"`, `source = "TEST ONLY: Synthetic existing QA guidance, NOT a confirmed FPT policy"` | `category = "guide"`, `policyKey = "phase241.l2"`, `authority = "admin_reviewed"`, `source = "TEST ONLY: Existing customer-facing guidance; no new business policy"` |
| **Isolated Test Approval** | `approvedForLive: true`, `approvedForIsolatedTestOnly: true` | `approvedForLive: true`, `approvedForIsolatedTestOnly: true` |
| **Production Publication** | **`productionPublicationApproved: false` (FORBIDDEN)** | **`productionPublicationApproved: false` (FORBIDDEN)** |

---

## 3. Updated Plan Fields in `test/rag/fixtures/phase24-live-plan.json`

Only the non-executing confirmation fields in [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json) were updated in Phase 2.4.8:

| JSON Path | Previous Value | Updated Phase 2.4.8 Value | Purpose |
|---|---|---|---|
| `status` | `"MOCK READY – LIVE APPROVAL PENDING"` | `"READY FOR FINAL AUTHORIZATION — NOT EXECUTED"` | Reflects Phase 2.4.8 readiness state |
| `documents[0].approvedForLive` (`L1`) | `false` | `true` (+ `approvedForIsolatedTestOnly: true`, `productionPublicationApproved: false`) | Records Admin approval of `L1` for isolated PostgreSQL testing only |
| `documents[1].approvedForLive` (`L2`) | `false` | `true` (+ `approvedForIsolatedTestOnly: true`, `productionPublicationApproved: false`) | Records Admin approval of `L2` for isolated PostgreSQL testing only |
| `cost.currentProjectTierVerifiedForThisPlan` | `false` | `true` | Records Admin Free Tier confirmation (`$0.00` max paid cost) |
| `cost.paidCostMaximumVerified` | `false` | `true` | Records `$0.00` paid cost cap verification |
| `cost.thinkingCostVerified` | `false` | `true` | Records Free Tier thinking token coverage |
| `quota.verifiedForThisPlan` | `false` | `true` (+ `timeSensitiveReverificationRequiredBeforeExecution: true`) | Records Admin quota confirmation (`>= 6` Chat, `>= 8` Embedding) |
| `liveApproval.dedicatedTestKeysConfirmed` | `false` | `true` | Records Admin key-to-project mapping confirmation |
| `liveApproval.metadataEvidenceReviewed` | `false` | `true` | Records model metadata & token limit review |
| `liveApproval.chatRestConfigurationReviewed` | `false` | `true` | Records Gemini 3.8 REST & `thinkingLevel=medium` review |
| `liveApproval.embeddingTier` / `chatTier` | `null` / `null` | `"Free Tier"` / `"Free Tier"` | Enforces Free Tier only |
| `liveApproval.embeddingRpdRemaining` / `chatRpdRemaining` | `null` / `null` | `8` / `6` | Records confirmed minimum available RPD |
| `liveApproval.embeddingRpmRemaining` / `chatRpmRemaining` | `null` / `null` | `1` / `1` | Records confirmed minimum available RPM |
| `liveApproval.embeddingTpmRemaining` / `chatTpmRemaining` | `null` / `null` | `2048` / `17648` | Records confirmed minimum available TPM |
| **`approved`** | `false` | **`false` (UNCHANGED)** | Blocks live execution until final authorization |
| **`executionAuthorized`** | `false` | **`false` (UNCHANGED)** | Blocks live execution until final authorization |
| **`readiness.executable`** | `false` | **`false` (UNCHANGED)** | Blocks live execution until final authorization |
| **`liveApproval.verifiedAtEpochMs` / `expiresAtEpochMs`** | `null` / `null` | **`null` / `null` (UNCHANGED)** | No active execution window generated |
| **`productionRagEnabled`** | `false` | **`false` (UNCHANGED)** | Production RAG remains disabled |

---

## 4. Model Compatibility & Live Plan Safety Verification

| Parameter | Verified Value | Enforcement Mechanism | Status |
|---|---|---|---|
| **Primary Chat Model** | `gemini-3.8-flash` | `LiveSafety.Plan` (`line 38`), `LiveRagRunner.Chat` (`line 57`) | **PASS** |
| **Chat Thinking Level** | `medium` (`thinkingConfig: {"thinkingLevel": "medium"}`) | `LiveSafety.Plan` (`lines 51–52`), `LiveSafety.Ledger.send()` (`line 136`) | **PASS** |
| **Chat Structured Output & Sampling** | `responseMimeType: "application/json"`, `responseJsonSchema`, `maxOutputTokens: 1200`; `temperature`/`topP`/`topK`/`candidateCount`/`thinkingBudget` omitted & rejected | `GeminiProvider.java` (`lines 303–308`), `LiveSafety.java` (`line 136`) | **PASS** |
| **Embedding Model & Dimensions** | `gemini-embedding-001`, `768` dimensions, L2 normalized | `LiveSafety.Plan` (`line 31`), `LiveSafety.Ledger.send()` (`line 121`), `LiveRagRunner.vector()` (`lines 35–41`) | **PASS** |
| **Backup Model Disabled** | `gemini-3.5-flash-lite` disabled (`enabledDuring14RequestLiveRun = false`, `extraBudgetRequests = 0`, `fallback = null`) | `LiveSafety.Plan` (`lines 55–65`), `LiveRagRunner.run()` (`line 125`) | **PASS** |
| **Maximum External Requests** | **`14` POST** (`2` doc embeddings + `6` query embeddings + `6` chat generation) | `LiveSafety.Plan` (`lines 35–36`), `LiveSafety.Ledger.send()` (`line 119`) | **PASS** |
| **Forbidden Operations** | `Retry = 0`, `Redirect = 0`, `Metadata GET = 0`, `countTokens = 0` | `SinglePostWire.java` (`lines 18–45`), `LiveSafety.Plan` (`line 37`) | **PASS** |
| **Same-Model Pacing Interval** | `spacingMs = 65000` (`>= 65 seconds` between consecutive calls to the same model; `<= 1 RPM`) | `LiveSafety.Ledger.awaitModel()` (`lines 97–101`) | **PASS** |
| **Maximum Paid Cost** | **`$0.00 USD`** (`paidRequestsAllowed = false`, Free Tier only) | `LiveSafety.Plan` (`lines 40, 47`) | **PASS** |
| **Production RAG** | **Disabled** (`productionRagEnabled = false`, `RAG_ENABLED = false`) | `LiveSafety.Plan` (`line 30`), `LiveRagRunner.main()` (`line 151`) | **PASS** |

---

## 5. Offline Safety & Regression Test Results (Re-Verified in Phase 2.4.8)

All offline verification suites were re-executed after updating [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json), with **0 real Gemini API requests** and **0 Supabase production connections**:

| Test Suite | Command | Result | Real API Calls | Sanitized Evidence Path |
|---|---|---|---:|---|
| **Live Runner Safety Suite** | `test/test-rag-live-runner.ps1` | **87/87 PASS** (`0` failed) | `0` | `.build/phase241-mocks-4b561c4d-a44c-46cb-8f76-2dee06c920e1/summary.json` |
| **Live Entry Gate Suite** | `test/test-rag-live-entry.ps1` | **4/4 PASS** (PowerShell syntax **5/5 PASS**; unapproved plan blocked with `APPROVAL_MISSING`) | `0` | `.build/phase241-entry-dfb24f83de8e4bd39e7c2ec68a490d8d.json` |
| **Container Isolation Mock Suite** | `test/test-rag-live-isolation.ps1` | **4/4 PASS** | `0` | `.build/phase241-isolation-f1cb50c5a35d428d9306cbcf4846693d.json` |
| **Isolated PostgreSQL Mock E2E Suite** | `test/test-rag-live-database.ps1` | **7/7 DB assertions PASS**, **6/6 (`LQ1`–`LQ6`) PASS**, `14` mock requests (`Recall@3 = 4/4`, `Mean Precision@3 = 0.875`, `Abstentions = 2/2`) | `0` | `.build/phase241-db-mocks/phase24-offline-db-c66e3c8f-6b92-451e-b955-6e95f3760952.evidence/offline-database-validation.json` (Container `fpt-rag-live-test-42fcb3258473` stopped & preserved) |
| **Historical Locks & Evidence Integrity** | SHA-256 + UTC ticks verification against `.build/phase242-preserve-e5fd204e3c94450abccf7a10bc616e7f.json` | **97/97 IDENTICAL** (`0` mismatches) | `0` | Live locks `.build/phase24-live/phase24-e2e-20261011-v1.request.lock` (`False`) and `.build/phase24-orchestration/phase24-e2e-20261011-v1.lock` (`False`) remain unconsumed |

---

## 6. Quota & API Key Pre-Execution Checklist (Time-Sensitive Re-Check)

Because Google AI Studio quotas are project-wide and time-sensitive, the Administrator must perform this quick 30-second verification in their **PowerShell 7 session** and **Google AI Studio** immediately before authorizing execution:

1. **PowerShell 7 Session Key Presence Check (No Secret Displayed):**
   ```powershell
   [pscustomobject]@{
       RAG_ENVIRONMENT     = $env:RAG_ENVIRONMENT
       RAG_ENABLED         = $env:RAG_ENABLED
       EmbeddingKeyPresent = [bool]$env:RAG_EMBEDDING_API_KEY
       ChatTestKeyPresent  = [bool]$env:RAG_CHAT_TEST_API_KEY
   } | Format-Table -AutoSize
   ```
   *(Must show `development`, `false`, `True`, `True`.)*
2. **Immediate Pre-Execution Quota Re-Check in Google AI Studio (`https://aistudio.google.com/rate-limit`):**
   - Confirm no other process has consumed today's quota since the Phase 2.4.8 confirmation:
     - **Project B (`gemini-embedding-001`):** `RPD remaining >= 8`, `RPM >= 1`, `TPM >= 2,048`, `Free Tier`.
     - **Project A (`gemini-3.8-flash`):** `RPD remaining >= 6`, `RPM >= 1`, `TPM >= 17,648`, `Free Tier`.
   - Keep both projects completely idle during the ~8-minute run.

---

## 7. Current GO / NO-GO Status Matrix

| Gate | Expected | Current Status in Phase 2.4.8 |
|---|---|---|
| **Correct Git branch (`feature/rag-integration`)** | `PASS` | **`PASS`** |
| **Primary model compatibility (`gemini-3.8-flash`, `medium`)** | `PASS` | **`PASS`** |
| **Embedding compatibility (`gemini-embedding-001`, `768`)** | `PASS` | **`PASS`** |
| **API key project mapping (`RAG_CHAT_TEST_API_KEY` & `RAG_EMBEDDING_API_KEY`)** | `VERIFIED` | **`VERIFIED` *(Confirmed by Administrator)*** |
| **Free Tier (`$0.00` max paid cost)** | `VERIFIED` | **`VERIFIED` *(Confirmed by Administrator)*** |
| **Current available quota (`>= 6` Chat, `>= 8` Embedding)** | `VERIFIED` | **`VERIFIED AT CONFIRMATION TIME` *(Re-check immediately before execution)*** |
| **Corpus `L1`/`L2` approval (Isolated PostgreSQL only)** | `APPROVED` | **`APPROVED FOR ISOLATED TEST ONLY` (`approvedForLive = true`, `productionPublicationApproved = false`)** |
| **Isolated PostgreSQL (`127.0.0.1 / fpt_rag_test`)** | `PASS` | **`PASS`** |
| **Maximum 14 requests (`2 + 6 + 6`, `0` GET/retry/redirect)** | `PASS` | **`PASS`** |
| **Backup disabled during live test (`gemini-3.5-flash-lite` OFF)** | `PASS` | **`PASS`** |
| **Production isolation (`0` Supabase, `RAG_ENABLED = false`)** | `PASS` | **`PASS`** |
| **Final Administrator Execution Authorization (`approved`, `executionAuthorized`, 30-min window)** | `APPROVED` | **`NOT YET AUTHORIZED` (`approved = false`, `executionAuthorized = false`, `verifiedAtEpochMs = null`)** |
| **OVERALL STATUS** | **READY FOR FINAL AUTHORIZATION** | **`READY FOR FINAL AUTHORIZATION — NOT EXECUTED`** |

---

## 8. Exact Commands to Run Later from Administrator's PowerShell 7 Session

> [!IMPORTANT]
> **DO NOT RUN THESE COMMANDS YET.**
> Currently `approved = false` and `executionAuthorized = false`, so running them now will immediately halt at preflight with `APPROVAL_MISSING`.
> Only run these commands **after** you provide the explicit Final Authorization Statement in Section 10 and Antigravity sets `approved = true`, `executionAuthorized = true`, `readiness.executable = true`, `readiness.blockers = []`, and the 30-minute `verifiedAtEpochMs` / `expiresAtEpochMs` window in `test/rag/fixtures/phase24-live-plan.json`.

Once final execution authorization is granted and `phase24-live-plan.json` has an active 30-minute window, run in the **Administrator's PowerShell 7 window** (where `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` are `PRESENT`):

```powershell
Set-Location -LiteralPath 'D:\miniproject\fpt'
$env:RAG_ENVIRONMENT = 'development'
$env:RAG_ENABLED = 'false'

# Bước 1: Kiểm tra Preflight Offline (0 API calls, 0 DB calls — xác nhận plan JSON và 2 biến môi trường hợp lệ)
java '-Dfile.encoding=UTF-8' -cp ".build/phase241-test-classes;.build/web/WEB-INF/classes;lib/runtime/*;lib/compile-only/*" rag.live.LiveRagRunner --preflight "D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json"

# Bước 2: Thực thi phiên kiểm thử Live RAG duy nhất (tối đa 14 POST, khoảng ~7 phút 40 giây do giãn cách 65s/model)
pwsh -NoProfile -File 'D:\miniproject\fpt\test\run-rag-live.ps1' -ApprovedLivePlan -PlanPath 'D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json'
```

---

## 9. Expected Request Count & Failure Behavior

### 9.1 Expected Request Count (Normal Passing Run)

- **Document Embeddings (`gemini-embedding-001`):** `2` POST requests (`01-document-L1`, `02-document-L2`)
- **Query Embeddings (`gemini-embedding-001`):** `6` POST requests (`03-query-LQ1`, `05-query-LQ2`, `07-query-LQ3`, `09-query-LQ4`, `11-query-LQ5`, `13-query-LQ6`)
- **Chat Generation (`gemini-3.8-flash`, `thinkingLevel: "medium"`):** `6` POST requests (`04-chat-LQ1`, `06-chat-LQ2`, `08-chat-LQ3`, `10-chat-LQ4`, `12-chat-LQ5`, `14-chat-LQ6`)
- **Metadata GET / `countTokens` / Retries / Redirects / Backup Model Calls:** **`0`**
- **Total External Requests:** **At most `14` POST requests** (spaced `>= 65 seconds` between calls to the same model; total duration `~7 minutes 40 seconds`).
- **Sanitized Evidence Output:** Written exclusively to `.build/phase24-live/phase24-e2e-20261011-v1.evidence/` (`summary.json`, `01..14-<op>.reserved.json`, `<op>.result.json`, `LQ1..LQ6.sources.json`, `LQ1..LQ6.quality.json`).

### 9.2 Deterministic Failure Behavior (First-Failure Halt)

If **any** error occurs at any point during the run (HTTP `429` / `503` / `400` / `3xx`, network timeout `> 10s`, embedding latency `> 5s`, turn latency `> 10s`, non-768 vector, `finishReason != "STOP"`, `candidatesTokenCount + thoughtsTokenCount > 1200`, wrong source selection, ungrounded prose, or expired 30-minute window):

1. **Slot Consumed Before Send:** The attempted operation has already consumed its reserved ordinal (`NN-<op>.reserved.json`).
2. **Immediate Total Halt (`0` Retries, `0` Backup Failover):** `LiveSafety.Halt` is thrown immediately; `failed = true` is set on the ledger; `halt-<uuid>.json` and the sanitized `<op>.result.json` (`"state": "halted_no_retry"`, `"errorCode": "<CODE>"`) are flushed to disk.
3. **Remaining Budget Forfeited:** None of the remaining planned requests are sent.
4. **No Automatic Re-run or Lock Reset:** Both `.build/phase24-orchestration/phase24-e2e-20261011-v1.lock` and `.build/phase24-live/phase24-e2e-20261011-v1.request.lock` remain permanently on disk (`claimed_no_resume`), preventing any accidental re-execution under `phase24-e2e-20261011-v1`.
5. **Isolated Container Stopped & Preserved:** The `finally` block in `test/run-rag-live.ps1` restores process environment variables and stops (without deleting) the dedicated `fpt-rag-live-test-<id>` Docker container for offline inspection.

---

## 10. Explicit Final Authorization Statement Required from Administrator

When you are ready to open the 30-minute execution window and run the 14-request Live Validation, please reply with the following explicit authorization statement:

```text
AUTHORIZE LIVE EXECUTION OF PLAN phase24-e2e-20261011-v1:
I re-confirm that Free Tier quotas (Embedding RPD >= 8, Chat RPD >= 6, $0.00 paid cost) are currently available and both test API keys are set in my PowerShell 7 session. You are authorized to set approved=true, executionAuthorized=true, readiness.executable=true, and a 30-minute verifiedAtEpochMs/expiresAtEpochMs window in test/rag/fixtures/phase24-live-plan.json for a single 14-request live validation run.
```
