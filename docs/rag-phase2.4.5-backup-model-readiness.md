# PHASE 2.4.5 — BACKUP MODEL & SAFE FAILOVER READINESS REPORT

- **Project:** `D:\miniproject\fpt`
- **Branch:** `feature/rag-integration`
- **Date:** 2026-10-11
- **Primary Chat Model:** `gemini-3.8-flash` (`thinkingConfig: {"thinkingLevel": "medium"}`)
- **Backup Chat Model:** `gemini-3.5-flash-lite` (`thinkingConfig: {"thinkingLevel": "low"}`)
- **Embedding Model:** `gemini-embedding-001` (`outputDimensionality: 768`)
- **Offline Readiness Status:** **GO (OFFLINE READY — 387/387 Automated Checks PASS)**
- **Live Execution Status:** **NO-GO (PENDING ADMINISTRATOR APPROVAL — `approved=false`, `executionAuthorized=false`, `RAG_ENABLED=false`)**

---

## 1. Executive Summary

Phase 2.4.5 adds a deterministic, single-hop **Backup Model & Safe Failover** architecture to the FPT Telecom Gia Lai AI/RAG consultation engine while preserving every Phase 2.1–2.4.4 guarantee:

1. **Primary / Backup Model Pair:**
   - **Primary Model:** `gemini-3.8-flash` with `thinkingConfig: {"thinkingLevel": "medium"}` and independent token budget (`maxOutputTokens: 1600` in production runtime, `1200` in Phase 2.4 Live plan).
   - **Backup Model:** `gemini-3.5-flash-lite` (verified against official Google Gemini API documentation) with `thinkingConfig: {"thinkingLevel": "low"}` and independent token budget (`maxOutputTokens: 1200`).
2. **Independent Target Configuration:** Each `ConsultationEngine.Target` carries its own `provider`, `apiKey`, `model`, `thinkingLevel`, and `maxTokens`, producing isolated `ChatOptions` via `Target.optionsFor(ChatOptions)` and `ChatOptions.copyForTarget(...)`.
3. **Admin-Controlled Toggle:** Administrators can enable or disable automatic failover (`ai_fallback_enabled=true|false`) and configure backup model parameters in **Admin AI Settings** without breaking existing AI behavior or changing the production default model automatically.
4. **Strict Error Eligibility & Anti-Loop Guarantees:**
   - Failover triggers **only** on transient/capacity errors (`QUOTA_EXCEEDED`, `HIGH_DEMAND`, `TIMEOUT`, `NETWORK_ERROR`, `SERVER_ERROR`) when `ai_fallback_enabled=true`.
   - Non-retryable errors (`INVALID_KEY`, `MODEL_NOT_FOUND`, `INVALID_REQUEST`) **never** trigger failover.
   - Maximum **1 failover hop** per turn (`Primary -> Backup`). Backup is never retried (`0` retries) and never bounces back to Primary (`0` bounce loops).
5. **Anti-Circumvention / No Key Rotation:** `ConsultationEngine.isKeyRotationAttempt(...)` blocks cross-key or cross-project rotation on the same provider (`GEMINI`), complying strictly with Google API Terms of Service.
6. **Unified Policy, Source Validation & Business Authority:** Both Primary and Backup models execute inside the exact same `ConsultationEngine.consult(...)` pipeline—using the same `ProductCatalog` business facts, the same `KnowledgeChunkDAO.current()` approved RAG chunks, the same `KnowledgeSourceSelection` source validation, and the same `ConsultationPolicy.apply(...)` post-validation.
7. **Phase 2.4 Live 14-Request Budget Preserved:** The Phase 2.4 Live Runner remains locked at **14 requests maximum** (`2` document embeddings + `6` query embeddings + `6` chat calls) with `backupModelPolicy.enabledDuring14RequestLiveRun = false` and `extraBudgetRequests = 0`.

---

## 2. Task 1 — Component Audit

| Component | File | Pre-2.4.5 State | Phase 2.4.5 Enhancement |
|---|---|---|---|
| **AI Service & Config Cache** | `src/java/ai/AIService.java` | Loaded `ai_provider`, `ai_model`, `ai_fallback_provider`, `ai_fallback_model`; shared a single `maxTokens` and had no per-target `thinkingLevel` or `ai_fallback_enabled` toggle. | Added `DEFAULT_BACKUP_MODEL = "gemini-3.5-flash-lite"`, `DEFAULT_PRIMARY_THINKING_LEVEL = "medium"`, `DEFAULT_BACKUP_THINKING_LEVEL = "low"`, `DEFAULT_BACKUP_MAX_TOKENS = 1200`, `parseConfig(...)`, `buildPrimaryTarget(...)`, `buildFallbackTarget(...)`, and `ai_fallback_enabled` gate. |
| **Gemini HTTP Provider** | `src/java/ai/provider/GeminiProvider.java` | Sent `thinkingBudget: 0` for non-Gemini-3 models and omitted `thinkingConfig` for Gemini 3 models in `ChatOptions`; did not parse `thoughtsTokenCount` into `ChatResponse`. | Emits `thinkingConfig: {"thinkingLevel": "<level>"}` for Gemini 3 models when `ChatOptions.thinkingLevel` is set; normalizes supported levels (`minimal`, `low`, `medium`, `high`); parses `usageMetadata.thoughtsTokenCount` into `ChatResponse.thoughtsTokens`. |
| **Consultation Engine** | `src/java/ai/consultation/ConsultationEngine.java` | Had basic `primary` and `fallback` `Target(provider, apiKey, model)` without per-target thinking/token limits, without `AIException.isRetryable()` filtering, and without anti-key-rotation checks. | Extended `Target` with `thinkingLevel` and `maxTokens`; added `isFallbackEligible(...)`, `isKeyRotationAttempt(...)`, `isSameModelTarget(...)`; enforced single-hop `Primary -> Backup` failover; recorded `usedFallbackModel`, `fallbackReason`, and `requestAttempts`. |
| **Consultation Policy** | `src/java/ai/consultation/ConsultationPolicy.java` | Validated structured JSON fields (`reply`, `recommendedSlugs`, `Action`, `LeadSummary`), price/speed grounding against `ProductCatalog`, and installation fee rules. | Audited and preserved intact; runs identically on responses from either Primary (`gemini-3.8-flash`) or Backup (`gemini-3.5-flash-lite`), and supplies `buildUnavailableReply(...)` when both models fail. |
| **Admin AI Settings** | `src/java/controller/AdminSettingsServlet.java`, `web/view/admin/settings.jsp` | Saved basic `ai_provider`, `ai_model`, `ai_fallback_provider`, `ai_fallback_model`, `ai_max_tokens`. | Added `ai_thinking_level`, `ai_fallback_enabled`, `ai_fallback_thinking_level`, and `ai_fallback_max_tokens` controls and persistence without altering production DB state offline. |

---

## 3. Task 2 — Official Google Documentation Verification of Backup Model

Official Google Gemini API documentation was audited (`https://ai.google.dev/gemini-api/docs/models`, `https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite`, `https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite`, `https://ai.google.dev/gemini-api/docs/thinking#thinking-levels`, `https://ai.google.dev/gemini-api/docs/pricing`):

| Model Identifier | Lifecycle Status | Official Capabilities & Thinking Support | Free Tier Status | Suitability Assessment |
|---|---|---|---|---|
| **`gemini-3.5-flash-lite`** | **Stable (GA, July 2026)** | Paired directly with `gemini-3.8-flash` on Google's official Models page. Supports `thinkingConfig.thinkingLevel` (`minimal`, `low`, `medium`, `high`; default `minimal`), Structured Outputs (`responseMimeType: "application/json"`, `responseJsonSchema`), System Instructions, `1,048,576` input tokens, `65,536` output tokens. | **Free of charge ($0.00)** for input and output (including thinking tokens) in Google AI Studio Free Tier; separate model bucket from `gemini-3.8-flash`. | **Selected Primary Backup Model (Recommended)** |
| **`gemini-3.1-flash-lite`** | **Stable (GA, May 2026)** | Stable replacement for deprecated `gemini-3.1-flash-lite-preview`. Supports `thinkingConfig.thinkingLevel` (`minimal`, `low`, `medium`, `high`), Structured Outputs (`responseJsonSchema`), `1,048,576` input / `65,536` output tokens. | **Free of charge ($0.00)** in Google AI Studio Free Tier. | **Approved Secondary Compatible Flash-Lite Option** |
| `gemini-3.1-flash-lite-preview` | Deprecated (Shutting down Sept 1, 2026) | Replaced by stable `gemini-3.1-flash-lite` and `gemini-3.5-flash-lite`. | N/A | **Rejected** (Deprecated preview) |
| `gemini-2.5-flash-lite` | Legacy 2.5 series | Uses legacy `thinkingBudget` instead of Gemini 3 `thinkingLevel`; previously exhibited `limit: 0` Free Tier availability restrictions on new projects. | Restricted on some projects | **Rejected** (Incompatible thinking config & quota risk) |

---

## 4. Tasks 3 & 4 — Independent Primary/Backup Configuration & Admin Control

### 4.1 Independent Target Configuration

Each model target has isolated Thinking and Token parameters:

| Setting Key | Primary Target (`Target primary`) | Backup Target (`Target fallback`) |
|---|---|---|
| **Provider** | `GEMINI` (`ai_provider`) | `GEMINI` (`ai_fallback_provider`) |
| **Model ID** | `gemini-3.8-flash` (`ai_model`) | `gemini-3.5-flash-lite` (`ai_fallback_model`) |
| **Thinking Config** | `{"thinkingLevel": "medium"}` (`ai_thinking_level`) | `{"thinkingLevel": "low"}` (`ai_fallback_thinking_level`) |
| **Max Output Tokens** | `1600` runtime / `1200` Live Plan (`ai_max_tokens`) | `1200` (`ai_fallback_max_tokens`) |
| **API Key Policy** | Same configured Gemini API key (no key rotation) | Same configured Gemini API key (no key rotation) |
| **Turn Deadline** | Shared overall turn deadline (`deadlineMillis`) | Shared overall turn deadline (`deadlineMillis`) |

### 4.2 Admin Toggle Behavior (`ai_fallback_enabled`)

- When `ai_fallback_enabled = true` (default when a valid distinct backup model is configured):
  - `AIService.buildFallbackTarget(cfg)` builds the `ConsultationEngine.Target` for `gemini-3.5-flash-lite` (`thinkingLevel: "low"`, `maxTokens: 1200`).
- When `ai_fallback_enabled = false` (`"false"`, `"0"`, `"off"`, `"no"`):
  - `AIService.buildFallbackTarget(cfg)` returns `null`.
  - `ConsultationEngine` executes **only** the Primary model (`1` request attempt max) and immediately returns the safe human-contact fallback if Primary fails.

---

## 5. Tasks 5, 6 & 7 — Failover Eligibility, Anti-Loop & Anti-Key-Rotation Guarantees

### 5.1 Error Eligibility Matrix (`AIException.isRetryable()` & `ConsultationEngine.isFallbackEligible(...)`)

| Error Code | HTTP / Cause | Retryable / Fallback Eligible? | Behavior When Primary Fails |
|---|---|---|---|
| `QUOTA_EXCEEDED` | HTTP 429 (`RESOURCE_EXHAUSTED`) | **YES** (if `fallback != null`) | 1 hop to Backup (`gemini-3.5-flash-lite`). |
| `HIGH_DEMAND` | HTTP 503 (`UNAVAILABLE` / overloaded) | **YES** (if `fallback != null`) | 1 hop to Backup (`gemini-3.5-flash-lite`). |
| `SERVER_ERROR` | HTTP 500 / 502 / 504 | **YES** (if `fallback != null`) | 1 hop to Backup (`gemini-3.5-flash-lite`). |
| `TIMEOUT` | `HttpTimeoutException` / deadline | **YES** (if deadline not expired) | 1 hop to Backup (`gemini-3.5-flash-lite`) using remaining turn deadline. |
| `NETWORK_ERROR` | `IOException` / connection reset | **YES** (if `fallback != null`) | 1 hop to Backup (`gemini-3.5-flash-lite`). |
| `INVALID_KEY` | HTTP 401 / 403 (`PERMISSION_DENIED`) | **NO** | Immediate halt (`1` attempt total); returns safe fallback reply. |
| `MODEL_NOT_FOUND` | HTTP 404 (`NOT_FOUND`) | **NO** | Immediate halt (`1` attempt total); returns safe fallback reply. |
| `INVALID_REQUEST` | HTTP 400 (`INVALID_ARGUMENT`) | **NO** | Immediate halt (`1` attempt total); returns safe fallback reply. |

### 5.2 Anti-Loop & Anti-Bounce Protection

- `ConsultationEngine.callProvider(...)` is strictly linear (no `while` loop, no recursive retry):
  1. Calls `primary.getProvider().chat(...)` once (`attempts = 1`).
  2. If `AIException` is thrown, evaluates `isFallbackEligible(e, primary, fallback)`.
  3. If eligible, calls `fallback.getProvider().chat(...)` once (`attempts = 2`) with `fallback.optionsFor(options)` and sets `usedFallbackModel = true`, `fallbackReason = e.getErrorCode()`.
  4. If Backup also throws `AIException`, the exception is caught in `ConsultationEngine.consult(...)` and converted into `ConsultationPolicy.buildUnavailableReply(...)`.
- **Maximum requests per user turn:** `1` (normal) or `2` (when Primary fails with a retryable error and fallback is enabled). Never `> 2`.

### 5.3 Anti-Key-Rotation Enforcement

- `ConsultationEngine.isKeyRotationAttempt(Target primary, Target fallback)` checks whether both targets use the same provider (`GEMINI`) with different non-blank API keys.
- If a different Gemini API key is detected on the fallback target, `isFallbackEligible(...)` returns `false` and logs `[ConsultationEngine] Blocked fallback target: API key rotation on same provider is prohibited`.

---

## 6. Tasks 8, 9 & 10 — Policy Authority, Token Accounting & Safe Customer Fallback

1. **ConsultationPolicy & Business Data Authority:**
   - Retrieval and catalog grounding happen **before** `callProvider(...)`, and `ConsultationPolicy.apply(...)` runs **after** `callProvider(...)`.
   - Whether `ChatResponse` comes from `gemini-3.8-flash` (`thinkingLevel="medium"`) or `gemini-3.5-flash-lite` (`thinkingLevel="low"`), the exact same JSON schema validation, `ProductCatalog` price/speed verification, installation fee rule (`300.000đ`, no ungrounded waiver), and `KnowledgeSourceSelection` source filtering apply.
2. **Token & Cost Control:**
   - `GeminiProvider.parseResponse(...)` extracts `promptTokenCount`, `candidatesTokenCount`, `thoughtsTokenCount` (`ChatResponse.getThoughtsTokens()`), and `totalTokenCount`.
   - `ConsultationEngine.callProvider(...)` logs structured, secret-free telemetry on every response:
     `modelUsed`, `usedFallbackModel`, `fallbackReason`, `requestAttempts`, `promptTokens`, `completionTokens`, `thoughtsTokens`, `totalTokens`, `latencyMs`.
3. **Total Outage Customer Fallback (Both Models Unavailable):**
   - If both Primary and Backup fail (or if Primary fails with a non-retryable error / fallback disabled), `ConsultationPolicy.buildUnavailableReply(...)` returns a polite, grounded response directing the customer to FPT Telecom Gia Lai's human support hotline (`0932 079 469`) or the registration form, with `0` hallucinated prices or policies.

---

## 7. Task 12 — Preservation of Phase 2.4 Live 14-Request Budget

To guarantee that Phase 2.4 Live Validation never exceeds the approved **14-request budget** (`2` document embeddings + `6` query embeddings + `6` chat calls):

1. `test/rag/fixtures/phase24-live-plan.json` defines `backupModelPolicy`:
   ```json
   "backupModelPolicy": {
     "backupModel": "gemini-3.5-flash-lite",
     "secondaryBackupModel": "gemini-3.1-flash-lite",
     "thinkingConfig": {
       "thinkingLevel": "low"
     },
     "maxOutputTokens": 1200,
     "enabledDuring14RequestLiveRun": false,
     "extraBudgetRequests": 0,
     "noKeyOrProjectRotation": true,
     "separatePlanApprovalRequiredForLiveFailover": true
   }
   ```
2. `test/rag/live/LiveSafety.java` (`Plan.load(...)` and `BackupModelPolicy`) enforces at startup:
   - `backupModel == "gemini-3.5-flash-lite"`
   - `thinkingConfig.thinkingLevel == "low"`
   - `maxOutputTokens == 1200` (within `[1024, 2048]`)
   - `enabledDuring14RequestLiveRun == false`
   - `extraBudgetRequests == 0`
   - `noKeyOrProjectRotation == true`
   - `separatePlanApprovalRequiredForLiveFailover == true`
   Any violation halts the runner before `0` network calls with `SafetyAbort("BACKUP_MODEL_POLICY", ...)`.
3. **If Administrator Later Requests Live Failover Testing:**
   - It must **not** be piggybacked onto the 14-request Phase 2.4 run.
   - A separate live failover plan fixture must be explicitly approved with its own request budget (e.g., `1` primary probe + `1` backup probe = `2` requests max) and explicit administrator sign-off.

---

## 8. Task 11 — Offline Verification & Regression Test Results

All test suites were executed offline (`0` real Gemini API calls, `0` Supabase production DB connections):

| Test Suite / Command | Assertions / Scenarios | Result |
|---|---|---|
| `powershell -File test/rag/live/test-rag-live-runner.ps1` | **87/87** safety checks (including `testBackupModelPolicyGuardsInLivePlan` verifying `BACKUP_MODEL_POLICY` guards) | **PASS (`87/87`, `0` real API calls)** |
| `powershell -File test/rag/live/test-rag-live-entry.ps1` | **4/4** CLI entrypoint unapproved/missing-flag abort checks | **PASS (`4/4`)** |
| `powershell -File test/rag/live/test-rag-live-isolation.ps1` | **4/4** PostgreSQL isolation & non-private host rejection checks | **PASS (`4/4`)** |
| `powershell -File test/rag/live/test-rag-live-database.ps1` | **7/7** DB assertions + **6/6** `LQ1–LQ6` mock E2E scenarios (`14` mock requests, `0` real API calls) | **PASS (`7/7` + `6/6`)** |
| `powershell -File run-chat-tests.ps1` | **92/92** chatbot tests (`ConsultationBehaviorTest` `32/32` including 6 new backup/failover tests, `AdaptiveConversationTest` `21/21`, `InstallationFeeTest` `17/17`, `ChatRegistrationTest` `9/9`, `ProviderResponseTest` `7/7`, `ChatFrontendTest` `6/6`) | **PASS (`92/92`)** |
| `powershell -File run-rag-phase2-offline.ps1` | **75/75** RAG Phase 2 offline checks + **22/22** subject selection checks | **PASS (`75/75` + `22/22`)** |
| `powershell -File rag-single-embedding-tests.ps1` | **29/29** single-embedding safety & contract tests | **PASS (`29/29`)** |
| `powershell -File run-rag-isolated.ps1` | **24/24** PostgreSQL 17 + `pgvector 0.8.2` integration tests | **PASS (`24/24`)** |
| `powershell -File run-rag-isolated.ps1 -Phase24` | **41/41** Phase 2.4 E2E PostgreSQL + mock Gemini tests | **PASS (`41/41`)** |
| `MultiProviderTest` | **6/6** AES-GCM encryption, provider factory, and Gemini 3 `thinkingConfig` payload tests | **PASS (`6/6`)** |
| Preserved Lock & Evidence Integrity | `.build/phase242-preserve-e5fd204e3c94450abccf7a10bc616e7f.json` SHA-256 + timestamp ticks check | **PASS (`97/97` untouched)** |

---

## 9. Summary of Code Changes

1. [`src/java/ai/dto/ChatOptions.java`](file:///d:/miniproject/fpt/src/java/ai/dto/ChatOptions.java): Added `thinkingLevel` and `copyForTarget(String targetThinkingLevel, int targetMaxTokens)`.
2. [`src/java/ai/dto/ChatResponse.java`](file:///d:/miniproject/fpt/src/java/ai/dto/ChatResponse.java): Added `thoughtsTokens`, `usedFallbackModel`, `fallbackReason`, and `requestAttempts`.
3. [`src/java/ai/exception/AIException.java`](file:///d:/miniproject/fpt/src/java/ai/exception/AIException.java): Added `isRetryable()` for strict error classification.
4. [`src/java/ai/provider/GeminiProvider.java`](file:///d:/miniproject/fpt/src/java/ai/provider/GeminiProvider.java): Added Gemini 3 `thinkingConfig: {"thinkingLevel": ...}` serialization in `buildRequestBody(...)`, Flash-Lite / Gemini 3 model helpers, and `thoughtsTokenCount` extraction in `parseResponse(...)`.
5. [`src/java/ai/consultation/ConsultationEngine.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationEngine.java): Added per-target `thinkingLevel` and `maxTokens`, `isFallbackEligible(...)`, `isKeyRotationAttempt(...)`, `isSameModelTarget(...)`, single-hop failover execution, and structured failover telemetry.
6. [`src/java/ai/AIService.java`](file:///d:/miniproject/fpt/src/java/ai/AIService.java): Added independent Primary (`gemini-3.8-flash`, `medium`, `1600`) and Backup (`gemini-3.5-flash-lite`, `low`, `1200`) target builders and `ai_fallback_enabled` toggle parsing.
7. [`src/java/controller/AdminSettingsServlet.java`](file:///d:/miniproject/fpt/src/java/controller/AdminSettingsServlet.java) & [`web/view/admin/settings.jsp`](file:///d:/miniproject/fpt/web/view/admin/settings.jsp): Added Admin UI and persistence fields for `ai_thinking_level`, `ai_fallback_enabled`, `ai_fallback_thinking_level`, and `ai_fallback_max_tokens`.
8. [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json) & [`test/rag/live/LiveSafety.java`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java): Added `backupModelPolicy` and `BACKUP_MODEL_POLICY` runtime verification keeping the 14-request Live budget strictly locked.
9. [`test/ai/ConsultationBehaviorTest.java`](file:///d:/miniproject/fpt/test/ai/ConsultationBehaviorTest.java), [`test/ai/MultiProviderTest.java`](file:///d:/miniproject/fpt/test/ai/MultiProviderTest.java) & [`test/rag/live/LiveRunnerSafetyTest.java`](file:///d:/miniproject/fpt/test/rag/live/LiveRunnerSafetyTest.java): Added unit and safety tests covering retryable vs non-retryable failover, anti-key-rotation, independent `thinkingLevel`/`maxTokens`, `AIService` config parsing, and `BACKUP_MODEL_POLICY` enforcement.

---

## 10. Final GO / NO-GO Assessment

- **Offline Implementation & Regression Readiness:** **GO**
  - All 12 Phase 2.4.5 requirements are implemented and verified offline with **0 failures** across all test suites and **97/97** historical evidence/lock files preserved untouched.
- **Live Gemini API Execution Status:** **NO-GO (Awaiting Explicit Administrator Approval)**
  - `approved: false` and `executionAuthorized: false` remain locked in `test/rag/fixtures/phase24-live-plan.json`.
  - Corpus documents `L1` and `L2` remain `approvedForLive: false`.
  - `RAG_ENABLED` remains `false`.
  - Execution is halted pending administrator verification of Google AI Studio Free Tier quota (`gemini-3.8-flash`, `gemini-3.5-flash-lite`, `gemini-embedding-001`, `$0` billing) and explicit sign-off.
