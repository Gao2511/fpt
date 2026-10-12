# PHASE 2.4.9 — REAL GEMINI RAG VALIDATION: PRE-EXECUTION READINESS REPORT

- **Project:** `D:\miniproject\fpt`
- **Branch:** `feature/rag-integration` (`HEAD: 48b700f`)
- **Date:** `2026-10-12`
- **Plan ID:** `phase24-e2e-20261011-v1` ([`phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json))
- **Execution Mode:** **PREPARATION ONLY — NO LIVE API EXECUTION (`0` External Gemini Requests)**
- **Authorization Boundary State:** `approved = false`, `executionAuthorized = false`, `readiness.executable = false`, `verifiedAtEpochMs = null`, `expiresAtEpochMs = null`, `RAG_ENABLED = false`, `productionRagEnabled = false`
- **Offline Readiness Status:** **READY FOR FINAL AUTHORIZATION — NOT EXECUTED (`LIVE NO-GO` Until Explicit Execution Sign-Off & Immediate Quota Recheck)**

---

## 1. Executive Summary

Phase 2.4.9 completed a comprehensive pre-execution source audit, configuration verification, security/isolation verification, and full offline regression sweep for the FPT Telecom Gia Lai RAG live validation harness.

1. **Zero Live Requests Sent:** In strict compliance with Phase 2.4.9 rules, **zero (`0`)** external requests were made to the Google Gemini API and **zero (`0`)** connections were made to the Supabase production database.
2. **Previous Claims Verified Against Actual Source & Artifacts:** All claims from Phases 2.1 through 2.4.8 were independently audited against actual source files ([`LiveRagRunner.java`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java), [`LiveSafety.java`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java), [`SinglePostWire.java`](file:///d:/miniproject/fpt/test/rag/live/SinglePostWire.java), [`run-rag-live.ps1`](file:///d:/miniproject/fpt/test/run-rag-live.ps1), [`GeminiProvider.java`](file:///d:/miniproject/fpt/src/java/ai/provider/GeminiProvider.java), [`ConsultationEngine.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationEngine.java), [`ConsultationPolicy.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationPolicy.java), [`ConsultationReply.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationReply.java), [`GeminiEmbeddingService.java`](file:///d:/miniproject/fpt/src/java/rag/GeminiEmbeddingService.java), [`KnowledgeChunkDAO.java`](file:///d:/miniproject/fpt/src/java/dao/KnowledgeChunkDAO.java), and [`KnowledgeSourceSelection.java`](file:///d:/miniproject/fpt/src/java/rag/KnowledgeSourceSelection.java)) and re-verified by fresh offline test runs.
3. **One Preparation Script Alignment Applied:** In [`review-rag-phase24-plan.ps1`](file:///d:/miniproject/fpt/test/review-rag-phase24-plan.ps1#L11), the offline plan-review assertion (originally written in Phase 2.4.1 before the Administrator's Phase 2.4.8 corpus confirmation) was updated to verify that `L1` and `L2` have `approvedForLive = true`, `approvedForIsolatedTestOnly = true`, and `productionPublicationApproved = false`, while keeping `approved = false`, `executionAuthorized = false`, and `productionRagEnabled = false` strictly enforced. No production code or behavior was modified.
4. **All Offline Regression Suites Pass (`0` Failures):**
   - Live runner safety: **87 / 87 PASS**
   - Live entry gates: **4 / 4 PASS** (PowerShell syntax **5 / 5 PASS**)
   - Container isolation mocks: **4 / 4 PASS**
   - Isolated PostgreSQL 17 + `pgvector 0.8.2` mock E2E: **7 / 7 DB safety assertions PASS**, **6 / 6 (`LQ1`–`LQ6`) PASS** (`14` mock requests, `0` real API requests)
   - RAG offline regression: **75 / 75 PASS** (`35` behavior + `13` corpus + `27` quality) + **22 / 22** subject selection **PASS**
   - Chatbot offline regression: **92 / 92 PASS** (`32` consultation + `21` adaptive + `17` installation fee + `9` registration + `7` provider response + `6` frontend) + **6 / 6** multi-provider security **PASS**
   - Single-embedding mocks & plan review: **29 / 29 PASS** $\times$ `2` reruns (**58 / 58 PASS**)
   - Historical lock & evidence preservation: **97 / 97 IDENTICAL** (SHA-256 + UTC ticks)
5. **Current Decision:** Technical readiness is **PASS**, while live execution remains **NO-GO (`NOT EXECUTED`)** until the Administrator re-verifies time-sensitive Free Tier quotas immediately before execution and issues the explicit final execution authorization for [`phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json).

---

## 2. Environment and Branch Verification

| Check | Expected | Verified Actual Value | Status |
|---|---|---|---|
| **Working Directory** | `D:\miniproject\fpt` | `D:\miniproject\fpt` | **PASS** |
| **Git Branch** | `feature/rag-integration` | `feature/rag-integration` (`HEAD: 48b700f`) | **PASS** |
| **Java Compiler / Runtime** | JDK 17 (`--release 17` for live harness, `--release 8` for web app) | Verified (`Live runner compiled for Java 17; no provider or database request made`) | **PASS** |
| **Docker Engine & Image** | `pgvector/pgvector:0.8.2-pg17@sha256:feb68f4f15446397d8cac7f4fe48fe4586de83160d1fc48b46283312d1a33966` | Verified locally during `test-rag-live-database.ps1` | **PASS** |
| **Production RAG Flag** | `RAG_ENABLED=false`, `productionRagEnabled=false` | Enforced in plan JSON, `run-rag-live.ps1:8`, and `LiveRagRunner.java:151` | **PASS** |
| **External Gemini API Requests in Phase 2.4.9** | `0` | **`0`** | **PASS** |
| **Supabase Production DB Connections** | `0` | **`0`** | **PASS** |

---

## 3. Live Plan Configuration (`test/rag/fixtures/phase24-live-plan.json`)

The candidate plan [`phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json) (`planId = "phase24-e2e-20261011-v1"`) is configured as follows:

| Configuration Area | Field(s) in `phase24-live-plan.json` | Current Value | Enforcement Point |
|---|---|---|---|
| **Execution Locks (Locked)** | `approved`, `executionAuthorized`, `readiness.executable` | `false`, `false`, `false` | [`LiveSafety.java:28-29`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L28-L29) |
| **Approval Window (Inactive)** | `liveApproval.verifiedAtEpochMs`, `liveApproval.expiresAtEpochMs` | `null`, `null` | [`LiveSafety.java:43-44, 72`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L43-L44) |
| **Environment & Production Gate** | `environment`, `productionRagEnabled` | `"development"`, `false` | [`LiveSafety.java:30`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L30), [`LiveRagRunner.java:151`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L151) |
| **Primary Chat Model** | `chatModel`, `liveApproval.chatModel`, `liveApproval.thinkingConfig`, `liveApproval.chatMaxOutputTokens` | `"gemini-3.8-flash"`, `{"thinkingLevel": "medium"}`, `1200` | [`LiveSafety.java:38, 45, 49-52, 136`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L38) |
| **Embedding Model** | `embeddingModel`, `dimensions`, `embeddingCompatibility.documentTaskType` / `queryTaskType` | `"gemini-embedding-001"`, `768`, `"RETRIEVAL_DOCUMENT"` / `"RETRIEVAL_QUERY"` | [`LiveSafety.java:31, 121-133`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L31) |
| **Backup Model Policy** | `backupModelPolicy.backupModel`, `thinkingConfig`, `enabledDuring14RequestLiveRun`, `extraBudgetRequests` | `"gemini-3.5-flash-lite"`, `{"thinkingLevel": "low"}`, `false`, `0` | [`LiveSafety.java:55-65`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L55-L65), [`LiveRagRunner.java:125`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L125) |
| **Cost & Billing Policy** | `cost.paidRequestsAllowed`, `maximumApprovedPaidCostUsd`, `liveApproval.embeddingTier`, `liveApproval.chatTier` | `false`, `0`, `"Free Tier"`, `"Free Tier"` | [`LiveSafety.java:40, 47`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L40) |
| **Same-Model Pacing** | `liveApproval.spacingMs` | `65000` (`>= 65s` between same-model sends) | [`LiveSafety.java:53, 97-101`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L97-L101) |
| **Corpus Approval** | `documents[0..1].approvedForLive`, `approvedForIsolatedTestOnly`, `productionPublicationApproved` | `true`, `true`, `false` | [`LiveSafety.java:66-67`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L66-L67), [`review-rag-phase24-plan.ps1:11`](file:///d:/miniproject/fpt/test/review-rag-phase24-plan.ps1#L11) |

---

## 4. Model Compatibility Assessment & Pre-Execution Source Audit (`TASK A`)

### 4.1 Model & Wire Schema Compatibility

| Component | Target Specification | Source Code Verification | Compatibility Verdict |
|---|---|---|---|
| **Primary Chat Model (`gemini-3.8-flash`)** | `v1beta/models/gemini-3.8-flash:generateContent`, `thinkingConfig: {"thinkingLevel": "medium"}`, `maxOutputTokens: 1200`, `responseMimeType: "application/json"`, `responseJsonSchema` | [`GeminiProvider.java:273-330`](file:///d:/miniproject/fpt/src/java/ai/provider/GeminiProvider.java#L273-L330) omits legacy `temperature` and `topP` on `gemini-3*` models and attaches `responseMimeType` + `responseJsonSchema`. [`LiveRagRunner.java:60-61`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L60-L61) injects `thinkingConfig: {"thinkingLevel": "medium"}` and [`LiveSafety.java:136`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L136) rejects any request containing `temperature`, `topP`, `topK`, `candidateCount`, or `thinkingBudget`. | **COMPATIBLE (`PASS`)** |
| **Thought Part Filtering & Token Bounds** | Exclude `"thought": true` parts from JSON parser; verify `candidatesTokenCount + thoughtsTokenCount <= 1200` | [`GeminiProvider.java:363-371`](file:///d:/miniproject/fpt/src/java/ai/provider/GeminiProvider.java#L363-L371) (`customerText`) skips parts with `"thought": true`. [`LiveRagRunner.java:71`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L71) enforces `0 <= promptTokenCount <= 16448`, `0 <= candidatesTokenCount <= 1200`, `0 <= thoughtsTokenCount <= 1200`, and `completionTokens + thoughts <= 1200`. | **COMPATIBLE (`PASS`)** |
| **Embedding Model (`gemini-embedding-001`)** | `v1beta/models/gemini-embedding-001:embedContent`, root `outputDimensionality: 768` (integer), `taskType: "RETRIEVAL_DOCUMENT"` / `"RETRIEVAL_QUERY"`, L2 normalized | [`GeminiEmbeddingService.java:58-65`](file:///d:/miniproject/fpt/src/java/rag/GeminiEmbeddingService.java#L58-L65) builds the exact 4-field query / 5-field document JSON. [`LiveSafety.java:121-133`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L121-L133) verifies exact fields and UTF-8 byte cap (`<= 2048`). [`LiveRagRunner.java:35-41`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L35-L41) validates `768` finite numbers and normalizes to unit L2 length via [`VectorValues.java`](file:///d:/miniproject/fpt/src/java/rag/VectorValues.java). | **COMPATIBLE (`PASS`)** |
| **Backup Chat Model (`gemini-3.5-flash-lite`)** | `thinkingLevel: "low"`, `maxOutputTokens: 1200`, disabled during the 14-request live validation | [`LiveRagRunner.java:125`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L125) passes `fallback = null` into [`ConsultationEngine.chat`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationEngine.java#L125). [`LiveSafety.java:55-65`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L55-L65) requires `enabledDuring14RequestLiveRun = false` and `extraBudgetRequests = 0`. | **COMPATIBLE & LOCKED OFF (`PASS`)** |

### 4.2 Ten-Point Source Audit Checklist (`TASK A`)

| # | Audit Requirement | Code Evidence | Status |
|---:|---|---|---|
| 1 | **JSON schema compatibility** | [`ConsultationPrompt.java:34-42`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationPrompt.java#L34-L42) and [`RagContextBuilder.java:22-26`](file:///d:/miniproject/fpt/src/java/rag/RagContextBuilder.java#L22-L26) produce strict JSON Schema objects (`additionalProperties: false`) passed in `generationConfig.responseJsonSchema`. | **PASS** |
| 2 | **Thinking configuration serialization** | [`LiveRagRunner.java:61`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L61) and [`GeminiProvider.java:318-325`](file:///d:/miniproject/fpt/src/java/ai/provider/GeminiProvider.java#L318-L325) serialize `{"thinkingLevel": "medium"}` inside `generationConfig.thinkingConfig`. | **PASS** |
| 3 | **Token output bounds and response parsing** | `maxOutputTokens = 1200`; `finishReason == "STOP"` enforced at [`LiveRagRunner.java:64`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L64); strict `JsonReader` (`setLenient(false)`) in [`ConsultationReply.java:25-61`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationReply.java#L25-L61). | **PASS** |
| 4 | **Request deadlines** | [`SinglePostWire.java:22-30`](file:///d:/miniproject/fpt/test/rag/live/SinglePostWire.java#L22-L30): `3s` DNS timeout, `5s` TCP connect, `5s` socket read timeout, `10s` hard socket-close deadline. [`LiveSafety.java:152-153`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L152-L153): `<= 5000ms` embedding latency, `<= 10000ms` chat latency. [`LiveRagRunner.java:84, 141`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L84): `<= 10000ms` total turn latency excluding quota waits. | **PASS** |
| 5 | **Model-specific quota pacing** | [`LiveSafety.java:97-101`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L97-L101) (`awaitModel`) tracks last-send timestamps independently per model (`gemini-embedding-001` vs. `gemini-3.8-flash`) and enforces `spacingMs = 65000` (`<= 1 RPM` per model). [`LiveRagRunner.java:124`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L124) executes `ledger.awaitModel("chat")` before starting the Engine's 40s turn deadline. | **PASS** |
| 6 | **No hidden retries in HTTP client, provider wrappers, or scripts** | [`SinglePostWire.java`](file:///d:/miniproject/fpt/test/rag/live/SinglePostWire.java) uses a raw `SSLSocket` with one `POST` write and `Connection: close`. [`LiveRagRunner.java:24`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L24) blocks `GeminiEmbeddingService`'s default transport (`DEFAULT_EMBEDDING_TRANSPORT_FORBIDDEN`). [`LiveRagRunner.java:56-74`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L56-L74) overrides `GeminiProvider.chat` directly (bypassing `executeWithRetry`) and throws `LiveSafety.Halt extends Error` (bypassing `ConsultationEngine`'s `catch (Exception)` repair loop). | **PASS** |
| 7 | **No unapproved external API calls** | [`SinglePostWire.java:18`](file:///d:/miniproject/fpt/test/rag/live/SinglePostWire.java#L18) permits only `https://generativelanguage.googleapis.com:443` with no query string. [`LiveSafety.java:115-119`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L115-L119) permits only `POST` to the 2 exact model endpoints and the 14 named operation IDs (`document-L1..2`, `query-LQ1..6`, `chat-LQ1..6`). | **PASS** |
| 8 | **No database connection to production** | [`LiveSafety.java:74`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L74) enforces `host == "127.0.0.1"` and `name == "fpt_rag_test"`. [`LiveRagRunner.java:91-95`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L91-L95) verifies DB user `fpt_rag_user`, 32-hex container nonce in `phase241_test_identity`, and `0` initial rows. | **PASS** |
| 9 | **No accidental activation of Admin production RAG** | [`run-rag-live.ps1:8`](file:///d:/miniproject/fpt/test/run-rag-live.ps1#L8) and [`LiveRagRunner.java:151`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L151) abort unless `RAG_ENVIRONMENT == "development"` and `RAG_ENABLED == "false"`. | **PASS** |
| 10 | **No changes to official product prices or promotions** | [`LiveRagRunner.java:96-104`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L96-L104) loads authoritative package prices (`195K`–`305K`) and standard installation fee (`300.000đ`) into temporary table `phase241_business_packages` inside `fpt_rag_test` from `ConsultationBehaviorTest.CATALOG` and `SiteContent.defaults()`. | **PASS** |

---

## 5. API Key Handling, Project Mapping, and PowerShell 7 Execution Handoff (`TASK D`)

### 5.1 Confirmed API Key & Project Mapping

Per Administrator confirmations recorded in Phase 2.4.8:

| Environment Variable | Target Model | Confirmed Google AI Studio Project | Confirmed Usage Tier | Status in Antigravity Process | Status in Admin PowerShell 7 Session |
|---|---|---|---|---|---|
| `RAG_CHAT_TEST_API_KEY` | `gemini-3.8-flash` | **Project A — `Default Gemini Project`** (`5 RPM / 250K TPM / 20 RPD`) | **Free Tier (`$0.00`)** | `MISSING` *(isolated process)* | `PRESENT` *(configured by Admin)* |
| `RAG_EMBEDDING_API_KEY` | `gemini-embedding-001` | **Project B — `Gemini Project`** (`100 RPM / 30K TPM / 1,000 RPD`) | **Free Tier (`$0.00`)** | `MISSING` *(isolated process)* | `PRESENT` *(configured by Admin)* |

### 5.2 PowerShell 7 Execution Handoff

Because environment variables set in an interactive PowerShell 7 (`pwsh`) window are scoped to that OS process and are not inherited by the separate Antigravity process, any future authorized execution **must** be run in the Administrator's PowerShell 7 session where `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` are configured.

Use the following safe command in that PowerShell 7 session to verify variable presence **without displaying complete or partial API key values**:

```powershell
# Safe presence check ONLY — never prints complete or partial API key values
[pscustomobject]@{
    PowerShellMajorVersion = $PSVersionTable.PSVersion.Major
    WorkingDirectory       = (Get-Location).Path
    RAG_ENVIRONMENT        = $env:RAG_ENVIRONMENT
    RAG_ENABLED            = $env:RAG_ENABLED
    RAG_EMBEDDING_API_KEY  = if ([string]::IsNullOrEmpty($env:RAG_EMBEDDING_API_KEY)) { 'MISSING' } else { 'PRESENT' }
    RAG_CHAT_TEST_API_KEY  = if ([string]::IsNullOrEmpty($env:RAG_CHAT_TEST_API_KEY)) { 'MISSING' } else { 'PRESENT' }
} | Format-Table -AutoSize
```

Expected output before a future authorized run:
- `PowerShellMajorVersion`: `7`
- `WorkingDirectory`: `D:\miniproject\fpt`
- `RAG_ENVIRONMENT`: `development`
- `RAG_ENABLED`: `false`
- `RAG_EMBEDDING_API_KEY`: `PRESENT`
- `RAG_CHAT_TEST_API_KEY`: `PRESENT`

---

## 6. Quota Readiness Checklist (`TASK E`)

Quota confirmation is **time-sensitive** (Google AI Studio daily RPD counters reset at Midnight US Pacific Time = `14:00 ICT`, and project quotas are shared across all keys/tabs in the same project). Historical 28-day peak usage in AI Studio screenshots (`6 / 20` RPD and `2 / 1,000` RPD) **must NOT** be interpreted as currently available quota at the moment of execution.

Immediately before authorizing and starting the future live run, the Administrator must verify every item below in `https://aistudio.google.com/rate-limit`:

### 6.1 Chat Quota Checklist (`gemini-3.8-flash` — Project A: `Default Gemini Project`)

- [ ] **Usage Tier:** Confirmed **`Free Tier`** (`$0.00` input / `$0.00` output including thinking tokens; paid request budget = `$0.00`).
- [ ] **Model Accessibility:** `gemini-3.8-flash` is active and accessible in Project A.
- [ ] **Daily Requests Remaining (`RPD`):** At least **`6`** requests remain today (`chatRpdRemaining >= 6` out of `20 RPD` limit).
- [ ] **Per-Minute Requests (`RPM`):** At least **`1`** RPM available (`5 RPM` project limit vs. runner pace of `1` request per `65 seconds`).
- [ ] **Per-Minute Tokens (`TPM`):** At least **`17,648`** TPM available (`250,000 TPM` project limit vs. `<= 17,648` max tokens per request).
- [ ] **Thinking Level Support:** `thinkingConfig: {"thinkingLevel": "medium"}` remains supported (`maxOutputTokens: 1200`).
- [ ] **Current Pre-Execution Recheck Status:** **`PENDING IMMEDIATE PRE-EXECUTION RECHECK`** (confirmed in Phase 2.4.8; must be re-confirmed at actual execution time).

### 6.2 Embedding Quota Checklist (`gemini-embedding-001` — Project B: `Gemini Project`)

- [ ] **Usage Tier:** Confirmed **`Free Tier`** (`$0.00`; paid request budget = `$0.00`).
- [ ] **Daily Requests Remaining (`RPD`):** At least **`8`** requests remain today (`embeddingRpdRemaining >= 8` out of `1,000 RPD` limit; `2` document + `6` query embeddings).
- [ ] **Per-Minute Requests (`RPM`):** At least **`1`** RPM available (`100 RPM` project limit vs. runner pace of `1` request per `65 seconds`).
- [ ] **Per-Minute Tokens (`TPM`):** At least **`2,048`** TPM available (`30,000 TPM` project limit vs. `<= 2,048` byte+margin cap per request).
- [ ] **Dimensionality Support:** `outputDimensionality: 768` (`RETRIEVAL_DOCUMENT` and `RETRIEVAL_QUERY`) supported.
- [ ] **Current Pre-Execution Recheck Status:** **`PENDING IMMEDIATE PRE-EXECUTION RECHECK`** (confirmed in Phase 2.4.8; must be re-confirmed at actual execution time).

---

## 7. Corpus and Quality Metrics Verification (`TASK B`)

### 7.1 Approved Test-Only Corpus (`L1` & `L2`)

Both documents in [`phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json#L36-L61) were verified byte-for-byte against their repository sources:

| ID | Title | Exact Approved Content (`UTF-8`) | Source File | Expected Chunks | Test DB Metadata | Approval Scope |
|---|---|---|---|---:|---|---|
| **`L1`** | `Hướng dẫn kết nối mạng WiFi` | `"Anh/chị kiểm tra kết nối dây mạng an toàn trước khi liên hệ hỗ trợ."` | [`test/rag/RagQualityTest.java:23`](file:///d:/miniproject/fpt/test/rag/RagQualityTest.java#L23) | `1` | `category="guide"`, `status="published"`, `policyKey="phase241.l1"`, `authority="admin_reviewed"`, `source="TEST ONLY: Synthetic existing QA guidance, NOT a confirmed FPT policy"` | `approvedForLive=true`, `approvedForIsolatedTestOnly=true`, `productionPublicationApproved=false` |
| **`L2`** | `Hướng dẫn chơi game` | `"Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế."` | [`src/java/ai/consultation/ConsultationPolicy.java:180`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationPolicy.java#L180) | `1` | `category="guide"`, `status="published"`, `policyKey="phase241.l2"`, `authority="admin_reviewed"`, `source="TEST ONLY: Existing customer-facing guidance; no new business policy"` | `approvedForLive=true`, `approvedForIsolatedTestOnly=true`, `productionPublicationApproved=false` |

### 7.2 Scenarios (`LQ1`–`LQ6`) and Mocked vs. Live Quality Metrics

| Scenario | Customer Question | Session | Expected Selected Source | Required Grounding / Abstention Behavior | Mocked PostgreSQL E2E Result (`test-rag-live-database.ps1`) | Real Live Gemini API Result |
|---|---|---|---|---|---|---|
| **`LQ1`** | `"Mạng nhà mình chậm quá."` | `LQ1` | `["L1"]` | Render literal `L1` excerpt only; no speed guarantee | **PASS (Mocked):** Retrieved `[L1, L2]`, selected `[L1]`, Precision@3 = `0.50` (`1.0` after query-aware selection) | **`NOT EXECUTED` (`0` live calls)** |
| **`LQ2`** | `"Shop ơi mạng nhà tui yếu quá."` | `LQ2` | `["L1"]` | Render literal `L1` excerpt only | **PASS (Mocked):** Retrieved `[L1]`, selected `[L1]`, Precision@3 = `1.00` | **`NOT EXECUTED` (`0` live calls)** |
| **`LQ3`** | `"Hướng dẫn chơi game Internet sao cho ổn?"` | `gaming-follow-up` | `["L2"]` | Render literal `L2` excerpt (LAN recommendation + no ping/latency guarantee) | **PASS (Mocked):** Retrieved `[L2]`, selected `[L2]`, Precision@3 = `1.00` | **`NOT EXECUTED` (`0` live calls)** |
| **`LQ4`** | `"Vậy cách đó có đảm bảo ping thấp không?"` | `gaming-follow-up` | `["L2"]` | Retain `game` domain from customer turn `LQ3` (`usePreviousCustomerDomainOnly = true`); select `L2`; never guarantee low ping | **PASS (Mocked):** Raw retrieved `[L2, L1]`, query-aware selected `[L2]`, Precision@3 = `1.00` (or `0.50` raw; overall Mean Precision@3 = `0.875 >= 0.80`) | **`NOT EXECUTED` (`0` live calls)** |
| **`LQ5`** | `"WiFi 6 có tốt hơn WiFi 5 không?"` | `LQ5` | `[]` | Disclose missing approved comparison; do not invent WiFi 6 vs. WiFi 5 specs | **PASS (Mocked):** Selected `[]`, safe abstention rendered (`0` unsupported claims) | **`NOT EXECUTED` (`0` live calls)** |
| **`LQ6`** | `"Có cung cấp internet ở nước ngoài không?"` | `LQ6` | `[]` | Disclose unconfirmed geographic coverage; do not claim international service | **PASS (Mocked):** Selected `[]`, safe abstention rendered (`0` unsupported claims) | **`NOT EXECUTED` (`0` live calls)** |

**Aggregate Quality Gate Summary (Mocked vs. Live):**
- **Relevant-Source Coverage (`Recall@3` on `LQ1`–`LQ4`):** Required `4/4` $\rightarrow$ Mocked PostgreSQL E2E: **`4/4` PASS** | Live API: **`NOT EXECUTED`**
- **Mean `Precision@3` (`LQ1`–`LQ4`):** Required `>= 0.80` $\rightarrow$ Mocked PostgreSQL E2E: **`0.875` PASS** | Live API: **`NOT EXECUTED`**
- **Appropriate Abstentions (`LQ5`–`LQ6`):** Required `2/2` $\rightarrow$ Mocked PostgreSQL E2E: **`2/2` PASS** | Live API: **`NOT EXECUTED`**
- **Unsupported Claims / Business Contradictions / Unapproved Document Leaks:** Required `0` $\rightarrow$ Mocked PostgreSQL E2E: **`0` PASS** | Live API: **`NOT EXECUTED`**

---

## 8. Security and PostgreSQL Isolation (`TASK C`)

| Security / Isolation Control | Implementation & Enforcement | Verification Status |
|---|---|---|
| **Dedicated Test API Keys Only** | [`LiveRagRunner.java:152`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L152) reads only `System.getenv("RAG_EMBEDDING_API_KEY")` and `System.getenv("RAG_CHAT_TEST_API_KEY")`. Never reads `.local/runtime.json`, `SettingsDAO`, `GEMINI_API_KEY`, or `GOOGLE_API_KEY` (`LiveRunnerSafetyTest.java:104`). | **VERIFIED (`PASS`)** |
| **No API Key Rotation or Unapproved Failover** | [`ConsultationEngine.java:38-65`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationEngine.java#L38-L65) blocks same-provider key rotation (`KEY_ROTATION_BLOCKED`), and [`LiveRagRunner.java:125`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L125) passes `fallback = null`. | **VERIFIED (`PASS`)** |
| **Fresh Isolated Loopback PostgreSQL Container** | [`rag-live-database.ps1`](file:///d:/miniproject/fpt/test/rag-live-database.ps1) starts a new container `fpt-rag-live-test-<12hex>` from pinned image `pgvector/pgvector:0.8.2-pg17@sha256:feb68f4f15446397d8cac7f4fe48fe4586de83160d1fc48b46283312d1a33966` bound to `-p 127.0.0.1::5432` with random 32-byte password and 32-hex nonce in `phase241_test_identity`. | **VERIFIED (`PASS`)** |
| **Production / Supabase Database Rejection** | [`LiveSafety.java:74`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L74) rejects any host $\ne$ `"127.0.0.1"` (including `localhost` or `*.supabase.co`) and any DB name $\ne$ `"fpt_rag_test"`. [`LiveRagRunner.java:91-95`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L91-L95) requires `0` initial rows in `knowledge_documents` and `knowledge_chunks`. | **VERIFIED (`PASS`)** |
| **Unexpected External Endpoint Rejection** | [`SinglePostWire.java:18`](file:///d:/miniproject/fpt/test/rag/live/SinglePostWire.java#L18) enforces `https://generativelanguage.googleapis.com:443` with SNI and HTTPS peer-certificate hostname verification. | **VERIFIED (`PASS`)** |
| **Zero Secret / Raw Error Body Logging** | [`SinglePostWire.java:44`](file:///d:/miniproject/fpt/test/rag/live/SinglePostWire.java#L44) discards non-200 HTTP response bodies (`if(status!=200)return new LiveSafety.Response(status,"");`). [`LiveSafety.java:145-160`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L145-L160) writes only SHA-256 hashes, HTTP status codes, sanitized error codes, vector norms, and token counts to `.build/phase24-live/<planId>.evidence/`. | **VERIFIED (`PASS`)** |

---

## 9. Offline Test Results (`TASK H`)

All applicable offline test suites were executed during Phase 2.4.9 with **`0` real Gemini API calls** and **`0` production DB connections**:

| Suite Category | Script / Runner | Exact Pass / Fail Count | Real API Calls | Sanitized Evidence / Output |
|---|---|---|---:|---|
| **1. Live Runner Safety** | [`test/test-rag-live-runner.ps1`](file:///d:/miniproject/fpt/test/test-rag-live-runner.ps1) (`rag.live.LiveRunnerSafetyTest`) | **87 PASS / 0 FAIL** | `0` | `.build/phase241-mocks-77464987-8ea1-4e25-aac8-54b3241a597c/summary.json` |
| **2. Live Entry Gates** | [`test/test-rag-live-entry.ps1`](file:///d:/miniproject/fpt/test/test-rag-live-entry.ps1) | **4 PASS / 0 FAIL** (PowerShell syntax **5 / 5 PASS**) | `0` | `.build/phase241-entry-9094f8ac03e84203938c33757ad70653.json` |
| **3. Container Isolation** | [`test/test-rag-live-isolation.ps1`](file:///d:/miniproject/fpt/test/test-rag-live-isolation.ps1) | **4 PASS / 0 FAIL** (`identity`, `label`, `port`, `none`) | `0` | `.build/phase241-isolation-b1bace2a5756497b94cca194d3d10c4e.json` |
| **4. Isolated PostgreSQL Mocks** | [`test/test-rag-live-database.ps1`](file:///d:/miniproject/fpt/test/test-rag-live-database.ps1) (`rag.live.LiveRunnerDatabaseTest`) | **7 / 7 DB assertions PASS**, **6 / 6 (`LQ1`–`LQ6`) PASS** (`14` mock requests, `0` failed) | `0` | `.build/phase241-db-mocks/phase24-offline-db-7df7d9d0-9c35-414b-abcc-3da5de53b59a.evidence/offline-database-validation.json` (Container `fpt-rag-live-test-66e034b75ebd` stopped & preserved) |
| **5. RAG Offline Regression** | [`test/run-rag-phase2-offline.ps1`](file:///d:/miniproject/fpt/test/run-rag-phase2-offline.ps1) | **97 PASS / 0 FAIL** (`RagBehaviorTest`: `35/35`, `RagPhase2CorpusTest`: `13/13`, `RagQualityTest`: `27/27`, `RagSubjectSelectionTest`: `22/22`) | `0` | Console & exit code `0` (`75/75` core + `22/22` subject selection) |
| **6. Chatbot Offline Regression** | `ConsultationBehaviorTest`, `AdaptiveConversationTest`, `InstallationFeeTest`, `ChatRegistrationTest`, `ProviderResponseTest`, `MultiProviderTest`, `chatbot-test.cjs` | **98 PASS / 0 FAIL** (`32` + `21` + `17` + `9` + `7` + `6` + `6` = `98/98`) | `0` | Console & exit code `0` (`92/92` chatbot suite + `6/6` multi-provider security) |
| **7. Embedding Mocks & Plan Review** | [`test/review-rag-phase24-plan.ps1`](file:///d:/miniproject/fpt/test/review-rag-phase24-plan.ps1) + [`test/rag-single-embedding-tests.ps1`](file:///d:/miniproject/fpt/test/rag-single-embedding-tests.ps1) | **29 PASS / 0 FAIL** $\times$ `2` reruns (**58 / 58 PASS**) | `0` | `.build/phase24-plan-review-4791de861284493090f464b8d592833f.json` |

---

## 10. Request Budget Verification

The future live execution budget is strictly capped at **14 external HTTP POST requests** (`0` requests permitted or sent during Phase 2.4.9):

| Operation Type | Planned Operations | Allowed Method & Endpoint | Max Budget | Enforced By |
|---|---|---|---:|---|
| **Document Embedding** | `document-L1`, `document-L2` | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent` | **`2`** | `LiveSafety.Plan` (`line 36`), `Ledger.send` (`lines 117–119`) |
| **Query Embedding** | `query-LQ1` … `query-LQ6` | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent` | **`6`** | `LiveSafety.Plan` (`line 36`), `Ledger.send` (`lines 117–119`) |
| **Gemini Chat Generation** | `chat-LQ1` … `chat-LQ6` | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent` | **`6`** | `LiveSafety.Plan` (`line 36`), `Ledger.send` (`lines 117–119`) |
| **Metadata GET / `countTokens` / Retry / Redirect / Failover** | None | Forbidden (`0`) | **`0`** | `LiveSafety.Plan` (`line 37`), `SinglePostWire` (`lines 18–45`) |
| **Total External Requests** | `01` … `14` | One-shot TLS 1.2/1.3 `POST` | **`14`** | `Ledger.send` (`line 119`: `total < 14`) |

**Verified Budget Enforcement Invariants:**
- **Durable Pre-Send Reservation:** `Ledger.send()` (`lines 143–148`) atomically opens `<operation>.result.json` (`CREATE_NEW`), increments `total` (`documents` / `queries` / `chats`), and flushes `NN-<operation>.reserved.json` with `FileChannel.force(true)` **before** `wire.post(...)` is called.
- **Failed Request Counts Toward Budget & Halts Immediately:** Any timeout, socket error, non-200 HTTP status, latency breach, or validation error after reservation consumes that slot, sets `failed = true`, writes `halt-<uuid>.json`, and terminates the entire run (`0` retries, remaining budget unused).
- **No Plan Reuse After Lock Claim:** Once `.build/phase24-live/phase24-e2e-20261011-v1.request.lock` or `.build/phase24-orchestration/phase24-e2e-20261011-v1.lock` is created, `StandardOpenOption.CREATE_NEW` / `FileMode.CreateNew` permanently blocks any second run under `phase24-e2e-20261011-v1`.

---

## 11. Lock and Evidence Preservation

| Preservation Target | Expected State | Verified Actual State in Phase 2.4.9 | Status |
|---|---|---|---|
| **Historical Evidence Manifest (`.build/phase242-preserve-e5fd204e3c94450abccf7a10bc616e7f.json`)** | All `97` files present with identical SHA-256 hash and `LastWriteTimeUtc.Ticks` | **`97 / 97` verified identical** | **PASS** |
| **Phase 2.3 Real Embedding Lock & Result (`phase23-corrected-payload-20261011-v2.*`, `phase23-single-embedding-20261011-v1.*`)** | Untouched (`16` probe artifacts preserved across plan review) | **`16 / 16` unchanged** | **PASS** |
| **Phase 2.4 Live Request Lock (`.build/phase24-live/phase24-e2e-20261011-v1.request.lock`)** | Must **not** exist yet (`False`) | **`False` (unconsumed)** | **PASS** |
| **Phase 2.4 Live Orchestration Lock (`.build/phase24-orchestration/phase24-e2e-20261011-v1.lock`)** | Must **not** exist yet (`False`) | **`False` (unconsumed)** | **PASS** |

---

## 12. Future Execution Command — NOT EXECUTED (`TASK G`)

> [!CAUTION]
> **DO NOT EXECUTE THIS COMMAND DURING PHASE 2.4.9.**
> This section is documentation only. Executing `run-rag-live.ps1` while `approved = false` and `executionAuthorized = false` will immediately abort at preflight with `APPROVAL_MISSING`.

### 12.1 Verified Entry Script Contract ([`test/run-rag-live.ps1`](file:///d:/miniproject/fpt/test/run-rag-live.ps1))

- **Script Path:** `D:\miniproject\fpt\test\run-rag-live.ps1`
- **Required PowerShell Version:** PowerShell 7+ (`$PSVersionTable.PSVersion.Major -ge 7`)
- **Required Parameters:**
  - `-ApprovedLivePlan` (`[switch]`, mandatory; omitting throws `'Separate explicit live authorization is required; no API or DB action'`)
  - `-PlanPath 'D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json'` (`[string]`, optional; defaults to `test/rag/fixtures/phase24-live-plan.json`)
- **Required Process Environment Variables:**
  - `RAG_ENVIRONMENT = 'development'`
  - `RAG_ENABLED = 'false'`
  - `RAG_EMBEDDING_API_KEY` (present, length `7..512` printable ASCII)
  - `RAG_CHAT_TEST_API_KEY` (present, length `7..512` printable ASCII)
- **Expected Execution Sequence & Evidence Paths:**
  1. Copies `phase24-live-plan.json` to an immutable snapshot `.build/phase241-plan-snapshots/<guid>.json`.
  2. Runs offline Java preflight (`rag.live.LiveRagRunner --preflight <snapshot>`).
  3. Claims `.build/phase24-orchestration/phase24-e2e-20261011-v1.lock` (`FileMode.CreateNew`).
  4. Provisions isolated Docker container `fpt-rag-live-test-<12hex>` (`127.0.0.1:<port>/fpt_rag_test`) and applies `002-rag-mvp.sql` + `003-rag-source-policy.sql`.
  5. Runs `rag.live.LiveRagRunner --execute-approved <snapshot> "D:\miniproject\fpt\.build\phase24-live"`, which claims `.build/phase24-live/phase24-e2e-20261011-v1.request.lock` and writes sanitized evidence to `.build/phase24-live/phase24-e2e-20261011-v1.evidence/summary.json`.
  6. Stops and preserves the Docker container in `finally`.

### 12.2 Exact PowerShell 7 Commands for Future Authorized Execution (Documentation Only — NOT EXECUTED)

```powershell
# ============================================================================
# DOCUMENTATION ONLY — DO NOT EXECUTE UNTIL FINAL AUTHORIZATION IS ACTIVATED
# Run inside the Administrator's PowerShell 7 (pwsh) session
# ============================================================================

Set-Location -LiteralPath 'D:\miniproject\fpt'
$env:RAG_ENVIRONMENT = 'development'
$env:RAG_ENABLED = 'false'

# 1. Verify environment variable presence safely (never prints key values):
[pscustomobject]@{
    RAG_ENVIRONMENT       = $env:RAG_ENVIRONMENT
    RAG_ENABLED           = $env:RAG_ENABLED
    RAG_EMBEDDING_API_KEY = if ([string]::IsNullOrEmpty($env:RAG_EMBEDDING_API_KEY)) { 'MISSING' } else { 'PRESENT' }
    RAG_CHAT_TEST_API_KEY = if ([string]::IsNullOrEmpty($env:RAG_CHAT_TEST_API_KEY)) { 'MISSING' } else { 'PRESENT' }
} | Format-Table -AutoSize

# 2. Offline Java Preflight Check (0 API requests, 0 DB connections):
java '-Dfile.encoding=UTF-8' -cp ".build/phase241-test-classes;.build/web/WEB-INF/classes;lib/runtime/*;lib/compile-only/*" rag.live.LiveRagRunner --preflight "D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json"

# 3. Single Authorized 14-Request Live Execution (~7m40s due to 65s per-model pacing):
pwsh -NoProfile -File 'D:\miniproject\fpt\test\run-rag-live.ps1' -ApprovedLivePlan -PlanPath 'D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json'
```

---

## 13. Risks and Blockers

### 13.1 Active Governance Blockers (Preventing Live Execution Now)

1. **B1 — Final Execution Authorization Not Yet Granted (`approved = false`, `executionAuthorized = false`, `readiness.executable = false`, `verifiedAtEpochMs = null`, `expiresAtEpochMs = null`):**
   - By design, Phase 2.4.9 is preparation-only. The Administrator must issue a separate explicit instruction to activate the 30-minute execution window and run the 14-request plan.
2. **B2 — Time-Sensitive Quota Recheck Required Immediately Before Execution:**
   - Although the Administrator confirmed Free Tier and sufficient quotas (`>= 6` Chat, `>= 8` Embedding) in Phase 2.4.8, RPD/RPM/TPM availability must be rechecked immediately prior to starting the live run.
3. **B3 — PowerShell 7 Session Handoff:**
   - `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` are `PRESENT` in the Administrator's interactive PowerShell 7 session and `MISSING` in the separate Antigravity process; execution must take place in the Administrator's PowerShell 7 session (or a session where those two variables are exported).

### 13.2 Technical & Semantic Quality Risks Identified in Pre-Execution Source Audit (`TASK A`)

Per Task A (*"Identify risks and required fixes, if any. Do not change production behavior solely to satisfy a test"*), the source audit identified two strictness risks in how [`LiveRagRunner.java`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java) and [`ConsultationReply.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationReply.java) validate real LLM prose:

1. **Risk R1 — Exact-String Abstention Check (`unknownReplyAllowed`) When `context.empty() == true` on `LQ6`:**
   - **Code Location:** [`LiveRagRunner.java:70, 80-83, 137`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L70).
   - **Mechanism:** When `context` is non-empty (such as `LQ5` `"WiFi 6 có tốt hơn WiFi 5 không?"`, where `L1` matches `"wifi"` lexically) and `gemini-3.8-flash` returns `"knowledgeChunkIds": []`, [`RagContextBuilder.render`](file:///d:/miniproject/fpt/src/java/rag/RagContextBuilder.java#L34-L41) deterministically replaces the reply with `"Dạ em chưa có nguồn còn hiệu lực và thống nhất để xác nhận thông tin này. Anh/chị vui lòng liên hệ tư vấn viên để được kiểm tra trường hợp cụ thể."`, which matches `unknownReplyAllowed`.
   - **Risk on `LQ6` (`"Có cung cấp internet ở nước ngoài không?"`):** `LQ6` has `0` lexical overlap with `L1` and `L2`. If the real `gemini-embedding-001` cosine similarity between `LQ6` and `L1`/`L2` is `< 0.72` ([`KnowledgeChunkDAO.java:41`](file:///d:/miniproject/fpt/src/java/dao/KnowledgeChunkDAO.java#L41)), `chunks.search` returns `[]` (`context.empty() == true`). When `context.empty() == true`, [`ConsultationEngine.java:149`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationEngine.java#L149) renders `reply.message` directly from `gemini-3.8-flash`, and `LiveRagRunner.java:70` requires `unknownReplyAllowed(reply.message)` to match the exact 111-character string `"Dạ em chưa có tài liệu đã duyệt để xác nhận thông tin này. Anh/chị muốn tư vấn thêm về dịch vụ Internet không?"`. Because that verbatim string is not in `ConsultationPrompt.SYSTEM`, a natural Vietnamese abstention from `gemini-3.8-flash` on `LQ6` when `context.empty() == true` would halt at request `#14` (`chat-LQ6`) with `UNSUPPORTED_UNKNOWN_PROSE`.
   - **Safe Mitigation Option (Test Harness Only, Zero Production Change):** Before or at final execution authorization, if the Administrator wishes to prevent a false-negative halt on `LQ6` when `context.empty() == true`, `LiveRagRunner.unknownReplyAllowed` (a test-only method in `test/rag/live/LiveRagRunner.java`) can either stay as-is (strict literal match) or be reviewed in a separate authorized step. In Phase 2.4.9, both `LiveRagRunner.java` and `LiveRunnerSafetyTest.java` (`line 105`) are preserved unchanged.
2. **Risk R2 — Strict Negative Prose Constraints in `ConsultationReply.validateMessage`:**
   - **Code Location:** [`ConsultationReply.java:86-101`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationReply.java#L86-L101).
   - **Mechanism:** Even when `!context.empty()`, `ConsultationReply.parse` validates `reply.message` before `RagContextBuilder.render` overwrites it. `validateMessage` rejects any `reply.message` containing digits `[0-9]`, non-question sentences with `"wifi"` / `"modem"` / `"camera"`, or keywords like `"cam ket"`. While `ConsultationPrompt.instructions` (`line 29`) and `RagContextBuilder.RULES` (`line 10`) explicitly instruct `gemini-3.8-flash` not to include numbers, benefits, or policy text in `message`, any single violation by the live model halts the run immediately (`0` retries).
3. **Risk R3 — Single-Request Latency Thresholds (`<= 5,000 ms` Embedding, `<= 10,000 ms` Chat/Turn):**
   - `LiveSafety.Ledger.send()` (`line 153`) and `LiveRagRunner.turnLatency()` (`line 84`) enforce `<= 5,000 ms` per embedding POST and `<= 10,000 ms` per chat POST / total turn (excluding 65s quota waits). If `gemini-3.8-flash` with `thinkingLevel: "medium"` experiences a transient cold-start spike `> 10,000 ms` on Google's servers, the runner will halt on `REQUEST_LATENCY_TARGET` or `TURN_LATENCY_TARGET`.

---

## 14. Authorization Design & Explicit Administrator Authorization Template (`TASK F`)

### 14.1 Authorization Window Design (`<= 30 Minutes`)

[`LiveSafety.Plan`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L28-L44) enforces the following authorization gate contract:
- `approved == true`
- `executionAuthorized == true`
- `readiness.executable == true` and `readiness.blockers.size() == 0`
- `liveApproval.verifiedAtEpochMs <= now`
- `liveApproval.expiresAtEpochMs > now`
- `liveApproval.expiresAtEpochMs - liveApproval.verifiedAtEpochMs <= 1,800,000` (`<= 30 minutes`)

Because the 14-request run takes approximately **`460 seconds` (`~7 minutes 40 seconds`)** due to the `65,000 ms` same-model pacing (`7` embedding intervals = `455s` + request latencies), a `30-minute` (`1,800,000 ms`) window provides more than **`22 minutes` of buffer** while ensuring stale approvals automatically expire (`Plan.beforeSend()` re-checks `System.currentTimeMillis() < expiresAtEpochMs` before every single network send).

**Current State in [`phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json) (Preserved Inactive):**
- `"approved": false`
- `"executionAuthorized": false`
- `"readiness.executable": false`
- `"verifiedAtEpochMs": null`
- `"expiresAtEpochMs": null`

### 14.2 Explicit Administrator Authorization Template

When the Administrator has rechecked current Google AI Studio quotas and is ready to authorize the single 14-request live validation run, the Administrator must provide the following explicit instruction:

```text
AUTHORIZE LIVE EXECUTION OF PLAN phase24-e2e-20261011-v1:
1. I have rechecked Google AI Studio immediately prior to execution and confirm:
   - Project A (Default Gemini Project, gemini-3.8-flash, thinkingLevel=medium) is on Free Tier ($0.00 paid budget) with >= 6 RPD, >= 1 RPM, and >= 17,648 TPM currently available.
   - Project B (Gemini Project, gemini-embedding-001, 768 dimensions) is on Free Tier ($0.00 paid budget) with >= 8 RPD, >= 1 RPM, and >= 2,048 TPM currently available.
2. Both dedicated test keys (RAG_CHAT_TEST_API_KEY and RAG_EMBEDDING_API_KEY) are PRESENT in my executing PowerShell 7 session.
3. Corpus L1 and L2 remain approved strictly for isolated local PostgreSQL testing (127.0.0.1 / fpt_rag_test) and forbidden for production publication.
4. You are authorized to set approved=true, executionAuthorized=true, readiness.executable=true, clear readiness.blockers, and populate a 30-minute verifiedAtEpochMs/expiresAtEpochMs window in test/rag/fixtures/phase24-live-plan.json for a single 14-request execution.
```

---

## 15. Final GO / NO-GO Matrix

| # | Gate | Required Condition | Current Status | Evidence |
|---:|---|---|---|---|
| 1 | **Git Branch & Workspace** | `feature/rag-integration` (`D:\miniproject\fpt`) | **`PASS`** | `HEAD: 48b700f` |
| 2 | **Primary Chat Model Compatibility** | `gemini-3.8-flash`, `thinkingLevel="medium"`, `maxOutputTokens=1200`, `responseJsonSchema` | **`PASS`** | Audited in `GeminiProvider.java`, `LiveRagRunner.java`, `LiveSafety.java`; `87/87` safety tests PASS |
| 3 | **Embedding Model Compatibility** | `gemini-embedding-001`, `outputDimensionality=768`, `RETRIEVAL_DOCUMENT` / `RETRIEVAL_QUERY`, L2 normalized | **`PASS`** | Audited in `GeminiEmbeddingService.java`, `LiveRagRunner.java`; `29/29` $\times$ `2` mock tests PASS |
| 4 | **Backup Model Disabled in 14-Request Plan** | `gemini-3.5-flash-lite` (`thinkingLevel="low"`) disabled (`enabledDuring14RequestLiveRun=false`, `extraBudgetRequests=0`) | **`PASS`** | `LiveSafety.java:55-65`, `LiveRagRunner.java:125` (`fallback = null`) |
| 5 | **API Key Project Mapping** | `RAG_CHAT_TEST_API_KEY` $\rightarrow$ Project A; `RAG_EMBEDDING_API_KEY` $\rightarrow$ Project B | **`VERIFIED`** | Confirmed by Administrator in Phase 2.4.8 (`dedicatedTestKeysConfirmed = true`) |
| 6 | **Free Tier & `$0.00` Paid Budget** | Both projects on Free Tier; `paidRequestsAllowed = false`, `maximumApprovedPaidCostUsd = 0` | **`VERIFIED`** | Confirmed by Administrator in Phase 2.4.8 (`embeddingTier = "Free Tier"`, `chatTier = "Free Tier"`) |
| 7 | **Immediate Pre-Execution Quota Recheck** | `>= 6` Chat RPD and `>= 8` Embedding RPD verified immediately before live execution | **`PENDING IMMEDIATE PRE-EXECUTION RECHECK`** | Confirmed in Phase 2.4.8; time-sensitive, must be rechecked right before execution |
| 8 | **Corpus `L1` & `L2` Approval** | Exact contents verified; approved only for isolated PostgreSQL (`127.0.0.1 / fpt_rag_test`) | **`APPROVED FOR ISOLATED TEST ONLY`** | `approvedForLive = true`, `approvedForIsolatedTestOnly = true`, `productionPublicationApproved = false` |
| 9 | **PostgreSQL & Production Isolation** | Loopback `127.0.0.1 / fpt_rag_test` container; `0` Supabase connections; `RAG_ENABLED=false` | **`PASS`** | `4/4` isolation tests + `7/7` DB assertions PASS |
| 10 | **Strict 14-Request Budget & Lock Integrity** | Max `14` POST (`2+6+6`), `0` GET/retry/redirect, `97/97` historical files intact, live locks unconsumed | **`PASS`** | `87/87` safety tests PASS; `97/97` preserved files verified; locks `False`/`False` |
| 11 | **Offline Regression Suites** | All offline RAG, Chatbot, DB, and safety suites pass | **`PASS`** | `87/87` safety, `4/4` entry, `4/4` isolation, `7/7`+`6/6` DB mock E2E, `97/97` RAG, `98/98` Chatbot, `58/58` embedding mocks |
| 12 | **Final Execution Authorization** | Explicit Administrator instruction to set `approved=true`, `executionAuthorized=true`, and 30-min window | **`NOT YET AUTHORIZED` (`HOLD`)** | `approved = false`, `executionAuthorized = false`, `readiness.executable = false`, `verifiedAtEpochMs = null` |
| — | **OVERALL LIVE EXECUTION DECISION** | **`GO` only when Gates 7 & 12 are completed at execution time** | **`NO-GO (READY FOR FINAL AUTHORIZATION — NOT EXECUTED)`** | **`0` real Gemini API requests sent in Phase 2.4.9** |

---

## 16. Recommended Next Step

1. **Keep Current State Locked:** Leave `approved = false`, `executionAuthorized = false`, `readiness.executable = false`, `verifiedAtEpochMs = null`, `expiresAtEpochMs = null`, and `RAG_ENABLED = false` until the moment of authorized execution.
2. **When Ready to Execute the 14-Request Live Validation:**
   - In your PowerShell 7 window, run the safe presence check in **Section 5.2** to confirm `RAG_EMBEDDING_API_KEY = PRESENT` and `RAG_CHAT_TEST_API_KEY = PRESENT`.
   - Complete the 30-second quota recheck in **Section 6** on `https://aistudio.google.com/rate-limit`.
   - Send the **Explicit Administrator Authorization Statement** in **Section 14.2** so the 30-minute execution window can be populated in [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json) and the single 14-request run can be executed from your PowerShell 7 session.
