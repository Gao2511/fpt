# Phase 2.4.3 — Final Live Validation Approval Preparation & Chat Model / Thinking Blocker Resolution

- **Date:** 2026-10-11
- **Project:** `D:\miniproject\fpt`
- **Branch:** `feature/rag-integration`
- **Plan ID:** `phase24-e2e-20261011-v1` (`test/rag/fixtures/phase24-live-plan.json`)
- **Mode:** OFFLINE ONLY (`realGeminiApiRequests = 0`, `supabaseConnections = 0`, `RAG_ENABLED = false`, `approved = false`, `executionAuthorized = false`, `L1.approvedForLive = false`, `L2.approvedForLive = false`)
- **Current Gate Status:** **NO-GO FOR IMMEDIATE LIVE EXECUTION (MODEL & THINKING TECHNICAL SPECIFICATIONS RESOLVED — AWAITING ADMINISTRATOR QUOTA VERIFICATION & APPROVAL SIGN-OFF)**

---

## 1. Source Code & Repository Documentation Audit for Chat Model (`TASK 1` & `TASK 2`)

### 1.1. Complete Code & Documentation Inspection (Zero Supabase Connection)
In strict compliance with Task 2, **no connection was made to Supabase production** (`0` database queries, `0` reads of `.local/runtime.json`). All source files, SQL schemas, tests, and documentation in the repository were inspected offline:

| Location | Mechanism / Key | Value in Source Code / Docs | Technical Findings |
| --- | --- | --- | --- |
| `src/java/ai/AIService.java` (`getConfig()`, lines 72–89) | `ai_active_provider`, `ai_active_model` (fallback: `gemini_model`) | Default in Java: `activeProvider = "gemini"`, `activeModel = ""` | Reads the active model ID at runtime from PostgreSQL table `settings` (cached 60 seconds). Does not hardcode a default Gemini Chat model in production code. |
| `src/java/ai/provider/GeminiProvider.java` (`getPresetModels()`, lines 45–50) | Preset fallback model list | `Collections.emptyList()` (`[]`) | Explicit comment: `// Discover live models with listModels; retired presets are not safe defaults.` |
| `src/java/ai/provider/GeminiProvider.java` (`buildRequestBody()`, lines 306–310) | Structured output schema gate | `model.startsWith("gemini-2.5-") \|\| model.startsWith("gemini-3")` | Attaches `responseMimeType: "application/json"` and `responseJsonSchema` **only** for `gemini-2.5-*` and `gemini-3*` models. Any older model prefix (`gemini-1.5-*`, `gemini-2.0-*`) omits the schema. |
| `src/java/ai/provider/GeminiProvider.java` (`customerText()`, lines 322–331) | Thought part filter | Skips `part.get("thought").getAsBoolean() == true` | Production parser ignores internal reasoning parts if thinking is active on the production model. |
| `src/java/controller/AdminSettingsServlet.java` (lines 329–337) & `web/view/admin/settings.jsp` (lines 214–216) | Admin UI model selector | Populated dynamically via `GET /v1beta/models` | Persists `ai_active_model` and syncs `gemini_model` into the database `settings` table. |
| `database/local/schema.sql` (line 28) | Local SQL seed | Seeds only `('ai_chatbot_enabled', 'false')` | Does not seed `ai_active_model` or `gemini_model`. |
| `AI_PROVIDER_GUIDE.md` (line 9) | Legacy markdown guide | Lists `gemini-1.5-flash, gemini-2.0-flash, gemini-1.5-flash-8b, gemini-1.5-pro` | **Outdated documentation:** All four listed models are retired/shut down by Google and are rejected by `GeminiProvider.java:307`'s `responseJsonSchema` prefix check. |
| `test/ai/provider/ProviderResponseTest.java` (line 39) | Provider unit test | `"gemini-2.5-flash"` | Verifies `GeminiProvider.chat()` request serialization and response parsing with `"gemini-2.5-flash"`. |
| `test/rag/live/LiveRunnerSafetyTest.java` (lines 19, 22–24) | Live runner offline safety fixture (`approvedFixture()`) | `"gemini-2.5-flash"`, `thinkingConfig: {"thinkingBudget": 0}`, `thinkingDisabledSupported: true` | All 63/63 safety tests and 6/6 isolated PostgreSQL mock tests (`LiveRunnerDatabaseTest`) were validated against `"gemini-2.5-flash"`. |
| `test/rag/fixtures/phase24-live-plan.json` (lines 11, 294) | Live plan model fields (`chatModel`, `liveApproval.chatModel`) | `null` | Intentionally left `null` (`approved: false`) until Administrator approves the test model configuration proposed in Section 4. |

### 1.2. Isolation Between Production Model and Live Test Runner (`TASK 6`)
- **Production Model Is Never Modified or Read by `LiveRagRunner`:**
  - `LiveRagRunner` (`test/rag/live/LiveRagRunner.java:43, 162`) constructs `new ConsultationEngine.Target(chat, "", plan.model, "")` using **only** `chatModel` from the frozen JSON plan (`test/rag/fixtures/phase24-live-plan.json`) and the dedicated environment variable `RAG_CHAT_TEST_API_KEY`.
  - It never calls `AIService.getConfig()` or `SettingsDAO`, and never touches Supabase production.
  - Therefore, configuring a dedicated test-only `chatModel` in `phase24-live-plan.json` has **zero effect on the production chatbot setting**.

---

## 2. Candidate Gemini Chat Models & Official Google Documentation Cross-Reference (`TASK 3` & `TASK 4`)

### 2.1. Official Google Gemini API Documentation Sources Consulted
All specifications below were cross-referenced against official Google Gemini API documentation (static public documentation pages; `0` Gemini API calls executed):
1. **Gemini Models Overview & Lifecycle:** `https://ai.google.dev/gemini-api/docs/models` (last updated 2026-10-09)
2. **Gemini 2.5 Flash Model Card:** `https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash`
3. **Gemini 2.5 Flash-Lite Model Card:** `https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash-lite`
4. **Gemini 2.5 Pro Model Card:** `https://ai.google.dev/gemini-api/docs/models/gemini-2.5-pro`
5. **Gemini 3.5 Flash-Lite & 3.8 Flash Model Cards:** `https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite`, `https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash`
6. **Gemini Thinking Guide (`thinkingBudget` vs `thinkingLevel`):** `https://ai.google.dev/gemini-api/docs/thinking`
7. **Gemini Structured Outputs Guide (`responseJsonSchema`):** `https://ai.google.dev/gemini-api/docs/structured-output`
8. **Gemini API Pricing & Free Tier Eligibility:** `https://ai.google.dev/gemini-api/docs/pricing`
9. **Gemini API Rate Limits (RPM / TPM / RPD):** `https://ai.google.dev/gemini-api/docs/rate-limits`

### 2.2. Compatibility Matrix of Candidate Gemini Models with `LiveRagRunner` & `LiveSafety`

The Phase 2.4 live runner enforces five technical gates on the Chat model:
1. **Endpoint Gate (`LiveSafety.java:38, 60`):** `POST https://generativelanguage.googleapis.com/v1beta/models/{chatModel}:generateContent` where `{chatModel}` matches `gemini-[a-zA-Z0-9.-]+` and does not contain `"embedding"`.
2. **Structured Output Schema Gate (`GeminiProvider.java:307`, `LiveRagRunner.java:60`):** Must support `responseMimeType: "application/json"` with `responseJsonSchema` (`ConsultationReply.responseSchema()`).
3. **Thinking Disabled Gate (`LiveSafety.java:51–52`, `LiveRagRunner.java:61, 71`):** Must accept `generationConfig.thinkingConfig: {"thinkingBudget": 0}` and return `thoughtsTokenCount == 0` (or omitted).
4. **Token Limit Gate (`LiveSafety.java:54`):** Must have `inputTokenLimit >= 16,448` (`chatInputByteCap 16384 + 64`) and `outputTokenLimit >= 1,200`.
5. **Zero-Cost Free Tier Gate (`LiveSafety.java:40, 47`):** Must be eligible for `"Free Tier"` (`0 USD` input, `0 USD` output/thinking, `paidRequestsAllowed = false`).

| Model ID | Google Lifecycle Status (`ai.google.dev/gemini-api/docs/models`) | Token Limits (`Input` / `Output`) | `responseJsonSchema` Support | Official Thinking Control & Default | Supports `thinkingBudget: 0` (`thoughtsTokenCount = 0`)? | Official Free Tier Status (`pricing`) | Compatible with Current `LiveRagRunner`? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| **`gemini-2.5-flash`** | **Stable** *(Note: existing 2.5 projects; see §2.3)* | `1,048,576` / `65,536` | **Yes** (`gemini-2.5-*`) | `thinkingBudget` (`0` to `24,576`); Default: **Dynamic (On)** | **YES** (`thinkingBudget: 0` turns thinking **Off**) | **Free of charge** (Input & Output) | **YES — PRIMARY COMPATIBLE** |
| **`gemini-2.5-flash-lite`** | **Stable** *(Note: existing 2.5 projects; see §2.3)* | `1,048,576` / `65,536` | **Yes** (`gemini-2.5-*`) | `thinkingBudget` (`0`, or `512` to `24,576`); Default: **Off (`0`)** | **YES** (Thinking is **Off by default** and accepts `thinkingBudget: 0`) | **Free of charge** (Input & Output) | **YES — SECONDARY COMPATIBLE** |
| `gemini-2.5-pro` | **Stable** | `1,048,576` / `65,536` | **Yes** (`gemini-2.5-*`) | `thinkingBudget` (`128` to `32,768`); Default: **Dynamic (On)** | **NO** (Cannot disable thinking; minimum budget is `128`) | Free tier limited / Paid tier | **NO** (Rejected by API & `UNEXPECTED_THINKING_TOKENS`) |
| `gemini-3.5-flash-lite` / `gemini-3.1-flash-lite` | **Stable** (Latest default for new projects) | `1,048,576` / `65,536` | **Yes** (`gemini-3*`) | `thinkingLevel` (`minimal`, `low`, `medium`, `high`); Default: `minimal` (On) | **NO** (Uses `thinkingLevel`; cannot set `thoughtsTokenCount = 0`) | **Free of charge** (Input & Output) | **NO without code change** (Blocked by `thinkingBudget == 0` gate) |
| `gemini-3.8-flash` / `gemini-3.6-flash` | **Stable** (Latest default for new projects) | `1,048,576` / `65,536` | **Yes** (`gemini-3*`) | `thinkingLevel` (`minimal`, `low`, `medium`, `high`); Default: `medium` (On) | **NO** (Uses `thinkingLevel`; cannot set `thoughtsTokenCount = 0`) | **Free of charge** (Input & Output) | **NO without code change** (Blocked by `thinkingBudget == 0` gate) |
| `gemini-2.0-flash` / `gemini-2.0-flash-lite` | **Shut down** | `1,048,576` / `8,192` | Blocked by `GeminiProvider.java:307` | N/A | N/A | Shut down | **NO** (Shut down by Google) |
| `gemini-1.5-flash` / `gemini-1.5-pro` | **Shut down / Retired** | N/A | Blocked by `GeminiProvider.java:307` | N/A | N/A | Shut down | **NO** (Shut down by Google) |

### 2.3. Key Official Documentation Evidence on `thinkingBudget`, `responseJsonSchema`, and `Free Tier`

1. **`thinkingBudget: 0` on Gemini 2.5 (`https://ai.google.dev/gemini-api/docs/thinking`):**
   - Official documentation states:
     - **Gemini 2.5 Flash (`gemini-2.5-flash`):** *"Default: Dynamic thinking (model decides when and how much to think) ... `thinkingBudget` range: `0–24576`. **Disable thinking: Set `thinkingBudget` to `0`.**"*
     - **Gemini 2.5 Flash-Lite (`gemini-2.5-flash-lite`):** *"Default: **Model does not think** ... `thinkingBudget` range: `512–24576`. **Disable thinking: Set `thinkingBudget` to `0` (or leave as default).**"*
     - **Gemini 2.5 Pro (`gemini-2.5-pro`):** *"Default: Dynamic thinking ... `thinkingBudget` range: `128–32768`. **Cannot disable thinking.**"*
2. **`responseJsonSchema` & `maxOutputTokens` Interaction (`https://ai.google.dev/gemini-api/docs/structured-output` & `/thinking`):**
   - Official documentation confirms that `responseMimeType: "application/json"` with `responseJsonSchema` is supported on `gemini-2.5-flash`, `gemini-2.5-flash-lite`, `gemini-2.5-pro`, and `gemini-3.*`.
   - Crucially, Google's Thinking documentation warns: *"Because `max_output_tokens` applies to the combined total of thinking tokens and output tokens, setting a low limit can truncate responses when thinking is enabled."*
   - Because `LiveRagRunner.Chat` sets `maxOutputTokens: 1200` (`LiveSafety.java:49, 125`) and immediately halts on `finishReason != "STOP"` (`LiveRagRunner.java:70`), keeping `thinkingBudget: 0` ensures all `1,200` output tokens are reserved exclusively for the customer JSON payload.
3. **Free Tier Eligibility & Pricing (`https://ai.google.dev/gemini-api/docs/pricing`):**
   - Both **`gemini-2.5-flash`** and **`gemini-2.5-flash-lite`** have an active **Free Tier** where Input price is **Free of charge** and Output price (including thinking tokens) is **Free of charge**.
   - **`gemini-embedding-001`** also has an active **Free Tier** where Input price is **Free of charge** (already confirmed in Phase 2.3 on the embedding test key: `Free Tier`, `RPM = 100`, `TPM = 30,000`, `RPD = 1,000`).
4. **Google's 2026-10-09 Access Policy Note for Gemini 2.5 (`https://ai.google.dev/gemini-api/docs/models`):**
   - Official documentation notes: *"To ensure reliable performance for everyone, we are limiting access to the 2.5 models to users who have actively used them in the past. These models are not deprecated and will continue to be served until further notice through the API. For any new projects, use our latest models: 3.5 Flash-Lite (`gemini-3.5-flash-lite`) or 3.8 Flash (`gemini-3.8-flash`)."*
   - This makes the Administrator's Google AI Studio check (Section 5) essential: if the test project has used Gemini 2.5 previously, `gemini-2.5-flash` and `gemini-2.5-flash-lite` will appear with active Free Tier quotas in AI Studio.

---

## 3. Evaluation: Does `thinkingBudget=0` Unnecessarily Exclude Suitable Models? (`TASK 5`)

We evaluated whether the requirement `thinkingConfig: {"thinkingBudget": 0}` (`LiveSafety.java:51–52`) and `thoughtsTokenCount == 0` (`LiveRagRunner.java:71`) unnecessarily excludes suitable models:

### 3.1. Within the Gemini 2.5 Model Family: **NO — It Does Not Exclude Any Suitable Model**
- Both **`gemini-2.5-flash`** and **`gemini-2.5-flash-lite`** natively support `thinkingConfig: {"thinkingBudget": 0}` on `POST /v1beta/models/{model}:generateContent` and produce `thoughtsTokenCount = 0`.
- The only Gemini 2.5 text model excluded by `thinkingBudget = 0` is **`gemini-2.5-pro`**, and excluding `gemini-2.5-pro` is **necessary and beneficial** for three independent reasons:
  1. **Truncation Risk under `maxOutputTokens = 1200`:** `gemini-2.5-pro` cannot disable thinking (`thinkingBudget >= 128`) and routinely consumes `500–2,000+` thinking tokens before emitting JSON, which would exhaust `maxOutputTokens: 1200` and trigger an irreversible `FINISH_REASON` halt (`MAX_TOKENS`) on the first Chat call.
  2. **Latency Target Risk (`<= 10,000 ms`):** `gemini-2.5-pro` reasoning latency frequently exceeds `10,000 ms`, which would trigger an immediate `REQUEST_LATENCY_TARGET` or `TURN_LATENCY_TARGET` halt.
  3. **Quota Conservation:** Thinking tokens count against the project's Free Tier TPM limit.

### 3.2. Across the Gemini 3 Model Family (`gemini-3.5-flash-lite`, `gemini-3.8-flash`): **YES, Conditional on Project Vintage**
- Gemini 3 models (`gemini-3.5-flash-lite`, `gemini-3.8-flash`) use `thinkingLevel` (`"minimal"`, `"low"`, `"medium"`, `"high"`) instead of `thinkingBudget: 0` and cannot completely turn off thinking (`thoughtsTokenCount > 0` even at `"minimal"`), although their thinking tokens are **Free of charge** on the Free Tier.
- Because `LiveSafety.java:51–52` requires `thinkingConfig == {"thinkingBudget": 0}` and `LiveRagRunner.java:71` requires `thoughtsTokenCount == 0`, the current runner **excludes Gemini 3 models**.
- **Engineering Decision:**
  - **Do NOT relax `thinkingBudget=0` now.** Keeping `thinkingBudget=0` preserves 100% of the Phase 2.4.1/2.4.2 verified safety tests (`63/63` PASS) and guarantees zero thinking-token truncation with `gemini-2.5-flash` or `gemini-2.5-flash-lite`.
  - **Contingency only if needed:** If and only if the Administrator checks Google AI Studio (Section 5) and finds that the test project is a newly created project without Gemini 2.5 access (so only `gemini-3.5-flash-lite` / `gemini-3.8-flash` have non-zero quota), a minimal approved code adjustment can be made in a separate step to allow `thinkingConfig: {"thinkingLevel": "minimal"}` with ` gemini-3.5-flash-lite`.

---

## 4. Proposed Dedicated Test-Environment Chat Model Configuration (`TASK 6` & `TASK 7`)

Without modifying the production model or database settings, we propose the following **dedicated test-environment configuration** for `test/rag/fixtures/phase24-live-plan.json`:

### 4.1. Primary Proposal (Option 1 — Recommended): `gemini-2.5-flash`
Use **`gemini-2.5-flash`** if the test project in Google AI Studio shows active Free Tier quota (`RPD >= 6`, `RPM >= 1`, `TPM >= 17,648`) for `gemini-2.5-flash`.

- **Why `gemini-2.5-flash` is primary:**
  1. Already used as the reference model in `LiveRunnerSafetyTest.approvedFixture()` (`test/rag/live/LiveRunnerSafetyTest.java:19, 22`) and `ProviderResponseTest.java:39`.
  2. Fully supports `responseMimeType: "application/json"` + `responseJsonSchema` (`GeminiProvider.java:307`).
  3. Fully supports `thinkingConfig: {"thinkingBudget": 0}` (`thinkingDisabledSupported: true`, `thoughtsTokenCount = 0`).
  4. Official token limits (`inputTokenLimit = 1,048,576`, `outputTokenLimit = 65,536`) comfortably exceed `LiveSafety.Plan`'s requirements (`>= 16,448` input, `>= 1,200` output).
  5. Eligible for Google AI Studio **Free Tier** (`0 USD` cost).

### 4.2. Secondary Proposal (Option 2 — Drop-in Alternative): `gemini-2.5-flash-lite`
Use **`gemini-2.5-flash-lite`** if `gemini-2.5-flash` has insufficient remaining daily quota (`RPD < 6`) in the test project or if maximum latency safety margin (`<= 10,000 ms`) is desired.

- **Why `gemini-2.5-flash-lite` is an ideal drop-in alternative:**
  1. Requires **zero code changes** — only the string `"gemini-2.5-flash-lite"` in `chatModel` and `liveApproval.chatModel`.
  2. Thinking is **Off by default** (`0` tokens) and explicitly accepts `thinkingConfig: {"thinkingBudget": 0}`.
  3. Same `1,048,576` input / `65,536` output token limits, same `responseJsonSchema` support, Free of charge on Free Tier, and fastest response latency in the Gemini 2.5 family.

### 4.3. Exact Proposed JSON Configuration for `test/rag/fixtures/phase24-live-plan.json` (To Apply Only Upon Administrator Approval)
Until Administrator approval is granted, `test/rag/fixtures/phase24-live-plan.json` remains untouched with `approved: false`, `executionAuthorized: false`, and `L1`/`L2` `approvedForLive: false`. Once approved, the exact model and thinking fields to populate are:

```json
{
  "chatModel": "gemini-2.5-flash",
  "cost": {
    "currentProjectTierVerifiedForThisPlan": true,
    "paidRequestsAllowed": false,
    "maximumApprovedPaidCostUsd": 0,
    "maximumEstimatedCostUsd": 0,
    "paidCostMaximumVerified": true,
    "thinkingCostVerified": true
  },
  "quota": {
    "verifiedForThisPlan": true
  },
  "liveApproval": {
    "verifiedAtEpochMs": "<EPOCH_MS_AT_APPROVAL>",
    "expiresAtEpochMs": "<EPOCH_MS_PLUS_MAX_1800000>",
    "embeddingModel": "gemini-embedding-001",
    "chatModel": "gemini-2.5-flash",
    "dedicatedTestKeysConfirmed": true,
    "metadataEvidenceReviewed": true,
    "chatRestConfigurationReviewed": true,
    "embeddingTier": "Free Tier",
    "chatTier": "Free Tier",
    "embeddingRpdRemaining": "<ADMIN_CONFIRMED_>=_8>",
    "chatRpdRemaining": "<ADMIN_CONFIRMED_>=_6>",
    "embeddingRpmRemaining": "<ADMIN_CONFIRMED_>=_1>",
    "chatRpmRemaining": "<ADMIN_CONFIRMED_>=_1>",
    "embeddingTpmRemaining": "<ADMIN_CONFIRMED_>=_2048>",
    "chatTpmRemaining": "<ADMIN_CONFIRMED_>=_17648>",
    "chatInputByteCap": 16384,
    "chatInputTokenLimit": 1048576,
    "chatOutputTokenLimit": 65536,
    "chatMaxOutputTokens": 1200,
    "thinkingConfig": {
      "thinkingBudget": 0
    },
    "thinkingDisabledSupported": true,
    "spacingMs": 65000
  }
}
```
*(If Option 2 is chosen, replace `"gemini-2.5-flash"` with `"gemini-2.5-flash-lite"`.)*

---

## 5. Step-by-Step Administrator Guide to Verify Quota in Google AI Studio (`TASK 2` & `TASK 8`)

This verification is performed entirely in the Google AI Studio web UI (**`0` Gemini API requests consumed**).

### Step 1 — Verify the Google Cloud Project & Tier Backing the Test Keys
1. Open **Google AI Studio API Keys**: `https://aistudio.google.com/apikey`
2. Locate the Google Cloud project(s) associated with your local test environment variables:
   - `RAG_EMBEDDING_API_KEY`
   - `RAG_CHAT_TEST_API_KEY`
   *(Do **not** paste the keys into chat.)*
3. Confirm that:
   - Neither key is used by the running production deployment (`dedicatedTestKeysConfirmed = true`).
   - In the **Plan / Billing Tier** column for the project(s), the tier displays **`Free`** / **`Free tier`** (no active pay-as-you-go billing attached, so paid fallback is impossible and max paid cost is strictly **`0 USD`**).

### Step 2 — Open Rate Limits & Select the Test Project
1. Open **Google AI Studio Rate Limits / Usage**: `https://aistudio.google.com/rate-limit` (or click **Usage & Billing → Rate limits** in the left sidebar).
2. In the top project dropdown, select the exact Google Cloud project that owns `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY`.
3. Confirm the **Tier** badge at the top of the Rate Limits page reads **`Free tier`**.

### Step 3 — Verify `gemini-embedding-001` Remaining Quota
Locate **`gemini-embedding-001`** (under *Embedding models* or *All models*) and check current usage vs limit against the 8-request requirement (`2` documents + `6` queries, spaced `>= 65s` apart):

| Metric | Live Plan Requirement (`LiveSafety.java:48–49`) | Typical Free Tier Limit (Ref: Phase 2.3) | How to Calculate Remaining Value |
| --- | --- | --- | --- |
| **RPD (Requests / day)** | **`embeddingRpdRemaining >= 8`** | `1,000` RPD | `Daily Limit - Current Daily Usage >= 8` |
| **RPM (Requests / minute)** | **`embeddingRpmRemaining >= 1`** | `100` RPM | `Minute Limit - Current Minute Usage >= 1` |
| **TPM (Tokens / minute)** | **`embeddingTpmRemaining >= 2,048`** | `30,000` TPM | `Minute Token Limit - Current Minute Usage >= 2,048` |

### Step 4 — Verify Chat Model Availability & Remaining Quota (`gemini-2.5-flash` or `gemini-2.5-flash-lite`)
In the same Rate Limits table (`https://aistudio.google.com/rate-limit`), locate **`gemini-2.5-flash`** (Option 1) or **`gemini-2.5-flash-lite`** (Option 2):

| Metric | Live Plan Requirement (`LiveSafety.java:48, 53`) | Typical Free Tier Limit (`ai.google.dev`) | How to Calculate Remaining Value |
| --- | --- | --- | --- |
| **Model Listed & Active** | Model row present with non-zero Free Tier limit | Listed under *Text-out models* | If `gemini-2.5-flash` is listed with non-zero RPD/RPM, choose **Option 1** (`gemini-2.5-flash`). If `gemini-2.5-flash` is at `0` RPD, check **Option 2** (`gemini-2.5-flash-lite`). *(If neither `2.5` model is available on a brand-new project, report that only `gemini-3.5-flash-lite` is listed.)* |
| **RPD (Requests / day)** | **`chatRpdRemaining >= 6`** | `20–500` RPD (varies by project cohort) | `Daily Limit - Current Daily Usage >= 6` |
| **RPM (Requests / minute)** | **`chatRpmRemaining >= 1`** | `5–15` RPM | `Minute Limit - Current Minute Usage >= 1` |
| **TPM (Tokens / minute)** | **`chatTpmRemaining >= 17,648`** | `250,000` TPM | `Minute Token Limit - Current Minute Usage >= 17,648` (`chatInputByteCap 16384 + maxOutputTokens 1200 + 64`) |

### Step 5 — Ensure a Quiet Project Window During the Run
- Because Google enforces RPM/TPM/RPD **per Google Cloud Project** (not per API key), ensure no other browser tab, script, or developer uses the test project during the ~10-minute serial run (`14` requests with `65,000 ms` same-model spacing; approval window valid for `<= 30` minutes).

---

## 6. Corpus Details & Unapproved Preservation Status (`TASK 3` & `TASK 9`)

In strict compliance with Task 9, **both `L1` and `L2` remain in unapproved status (`approvedForLive: false`)** in `test/rag/fixtures/phase24-live-plan.json` until the Administrator explicitly confirms them.

### 6.1. Document Summary Table

| Field | Document `L1` | Document `L2` |
| --- | --- | --- |
| **Document ID** | `L1` | `L2` |
| **Title** | `Hướng dẫn kết nối mạng WiFi` | `Hướng dẫn chơi game` |
| **Topic / Domain** | General WiFi / wired connection check (`internet` domain; `policyKey: "phase241.l1"`, `category: "guide"`) | Gaming LAN recommendation & latency disclaimer (`game` domain; `policyKey: "phase241.l2"`, `category: "guide"`) |
| **Source File** | `test/rag/RagQualityTest.java` (line 23: `GUIDE`) | `src/java/ai/consultation/ConsultationPolicy.java` (line 180) |
| **Provenance / Scope** | Synthetic existing QA guidance — **NOT an official FPT Telecom policy** (`authority: "admin_reviewed"`, never labeled `official`) | Existing customer-facing consultation guidance (`authority: "admin_reviewed"`, no new business policy) |
| **Current Approval Status** | **`approvedForLive: false` (UNAPPROVED — Task 9 preserved)** | **`approvedForLive: false` (UNAPPROVED — Task 9 preserved)** |
| **Expected Chunks** | `1` (`87` UTF-8 bytes content + `34` UTF-8 bytes title) | `1` (`146` UTF-8 bytes content + `24` UTF-8 bytes title) |
| **Exact Content** | `Anh/chị kiểm tra kết nối dây mạng an toàn trước khi liên hệ hỗ trợ.` | `Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế.` |

### 6.2. Expected Retrieval Questions (`LQ1`–`LQ6`) & Business Data Non-Conflict
- **Positive Queries (`4/4`):**
  - `LQ1` (*"Mạng nhà mình chậm quá."*) → selects `["L1"]`
  - `LQ2` (*"Shop ơi mạng nhà tui yếu quá."*) → selects `["L1"]`
  - `LQ3` (*"Hướng dẫn chơi game Internet sao cho ổn?"*, session `gaming-follow-up`) → selects `["L2"]`
  - `LQ4` (*"Vậy cách đó có đảm bảo ping thấp không?"*, session `gaming-follow-up`) → retains `game` domain from `LQ3`, selects `["L2"]` only
- **Abstention Queries (`2/2`):**
  - `LQ5` (*"WiFi 6 có tốt hơn WiFi 5 không?"*) → selects `[]`, abstains in Vietnamese
  - `LQ6` (*"Có cung cấp internet ở nước ngoài không?"*) → selects `[]`, abstains in Vietnamese
- **Zero Conflict with Authoritative Business Data:** Neither `L1` nor `L2` mentions any package code (`195K`–`249K`), monthly price (`195.000đ`–`249.000đ`), bandwidth (`300`/`500`/`1000 Mbps`), installation fee (`300.000đ`), or Mesh WiFi F1/F2 pricing/hardware.

---

## 7. Final Execution Safety Verification (`TASK 4`)

All nine execution safety invariants remain verified offline:

| # | Safety Invariant | Code Location | Status |
| --- | --- | --- | --- |
| 1 | **Maximum 14 requests** (`2` doc + `6` query + `6` chat; `0` GET / `countTokens`) | `LiveSafety.Plan` (`lines 35–37`), `LiveSafety.Ledger.send()` (`lines 104–108`) | **PASS (`63/63` safety, `14/14` DB mock)** |
| 2 | **No retry or redirect** | `SinglePostWire.java` (`lines 18–45`), `LiveSafety.Halt extends Error` (`line 16`) | **PASS** |
| 3 | **Counter recorded before sending** | `LiveSafety.Ledger.send()` (`lines 131–137`) | **PASS (`CREATE_NEW` + `force(true)` before `wire.post`)** |
| 4 | **Execution locks preserved** | `.build/phase242-preserve-e5fd204e3c94450abccf7a10bc616e7f.json` | **PASS (`97/97` files untouched; `0` modifications)** |
| 5 | **Failure stops the run** | `LiveSafety.Ledger.send()` (`lines 144–149`), `LiveRagRunner.main()` (`line 163`) | **PASS** |
| 6 | **Isolated PostgreSQL only** | `test/rag-live-database.ps1`, `LiveRagRunner.verifyDatabase()` (`lines 91–95`) | **PASS (`127.0.0.1`, `fpt_rag_test`, Docker nonce verified)** |
| 7 | **No production credential fallback** | `LiveRagRunner.main()` (`line 152`), `LiveSafety.Plan` (`line 32`) | **PASS (`RAG_EMBEDDING_API_KEY` & `RAG_CHAT_TEST_API_KEY` only)** |
| 8 | **No artifact overwrites** | `LiveSafety.create()` (`line 155`), `LiveSafety.Ledger` (`lines 77–80`) | **PASS (`CREATE_NEW` everywhere)** |
| 9 | **No API key exposure** | `SinglePostWire.java` (`lines 32, 44`), `LiveRagRunner.java` (`line 139`) | **PASS (`x-goog-api-key` header; non-200 bodies unread)** |

---

## 8. Updated GO / NO-GO Assessment & Remaining Conditions (`TASK 10`)

### 8.1. Blocker Resolution Summary Table

| Gate / Blocker Dimension | Previous Status | Updated Status | Evidence & Resolution Summary |
| --- | --- | --- | --- |
| **Offline Runner, Isolation & Safety Gates** | **PASS** | **PASS** | `63/63` safety tests, `4/4` entry tests, `7/7` isolated DB assertions, `6/6` mock live scenarios (`14` mock requests, `0` real API calls). |
| **Existing Evidence & Lock Preservation** | **PASS** | **PASS** | `97/97` preserved files verified identical by SHA-256 and timestamp; `RAG_ENABLED=false`. |
| **Technical Identification of Compatible Chat Model & Token Limits** | **BLOCKED** | **RESOLVED (SPECIFIED FOR TEST ENV)** | Cross-referenced `GeminiProvider.java:307`, `LiveSafety.java:51–54`, and official Google docs: **`gemini-2.5-flash`** (Primary) and **`gemini-2.5-flash-lite`** (Secondary) are Stable, Free Tier eligible, support `responseJsonSchema`, and have `inputTokenLimit = 1,048,576` (`>= 16,448`) and `outputTokenLimit = 65,536` (`>= 1,200`). Production model remains untouched. |
| **Technical Evaluation of `thinkingBudget: 0` Requirement** | **BLOCKED** | **RESOLVED (VERIFIED COMPATIBLE)** | Official Google Thinking documentation confirms `thinkingConfig: {"thinkingBudget": 0}` is supported on both `gemini-2.5-flash` and `gemini-2.5-flash-lite` (`thoughtsTokenCount = 0`), does **not** exclude suitable 2.5 models, and protects `maxOutputTokens = 1200` from JSON truncation. |
| **Administrator Confirmation of Selected Test Model & Google AI Studio Quota** | **BLOCKED** | **AWAITING ADMIN CONFIRMATION** | Requires Administrator to check `https://aistudio.google.com/rate-limit` (Section 5) and confirm `Free Tier` + remaining `RPD`/`RPM`/`TPM` for `gemini-embedding-001` and `gemini-2.5-flash` (or `gemini-2.5-flash-lite`). |
| **Corpus `L1` & `L2` Approval (`approvedForLive`)** | **BLOCKED** | **AWAITING ADMIN CONFIRMATION** | Preserved as `approvedForLive: false` per Task 9 until Administrator approves both documents. |
| **Final Live Run Authorization (`approved`, `executionAuthorized`)** | **BLOCKED** | **AWAITING ADMIN CONFIRMATION** | Preserved as `approved: false` and `executionAuthorized: false` until Administrator gives explicit go-ahead. |
| **Overall Live Execution Decision** | **NO-GO** | **NO-GO (TECHNICAL BLOCKERS RESOLVED — WAITING FOR ADMIN QUOTA & APPROVAL SIGN-OFF)** | **All code, model, schema, and thinking blockers are resolved offline. Execution awaits only the Administrator's 4 confirmation inputs below.** |

### 8.2. Concise Administrator Sign-Off Checklist (To Transition from `NO-GO` to `GO`)
To authorize the single 14-request live validation run, please reply with confirmation of these **4 items**:
1. **Selected Test Chat Model:** Confirm **`gemini-2.5-flash`** (Option 1 — Recommended) or **`gemini-2.5-flash-lite`** (Option 2) with `thinkingConfig: {"thinkingBudget": 0}` (`thinkingDisabledSupported = true`, `chatInputTokenLimit = 1048576`, `chatOutputTokenLimit = 65536`).
2. **Test Keys & Free Tier (`0 USD` Max Paid Cost):** Confirm `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` are set in the test environment, belong to a non-production project on **Free Tier**, and have `paidRequestsAllowed = false` (`0 USD` max paid cost).
3. **Remaining Quota in Google AI Studio (`https://aistudio.google.com/rate-limit`):** Provide or confirm the current remaining quotas:
   - `gemini-embedding-001`: `embeddingRpdRemaining >= 8`, `embeddingRpmRemaining >= 1`, `embeddingTpmRemaining >= 2048`
   - Selected Chat Model: `chatRpdRemaining >= 6`, `chatRpmRemaining >= 1`, `chatTpmRemaining >= 17648`
4. **Corpus (`L1`, `L2`) & Execution Authorization:** Confirm approval of `L1` and `L2` (`approvedForLive = true`) for the isolated test database `fpt_rag_test` and authorize setting `approved = true` and `executionAuthorized = true` for the single 14-request run.
