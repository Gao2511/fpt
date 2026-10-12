# PHASE 2.4.4 — GEMINI 3.8 FLASH MIGRATION & RAG LIVE READINESS REPORT

- **Project**: `D:\miniproject\fpt`
- **Branch**: `feature/rag-integration`
- **Phase**: `2.4.4` (Gemini 3.8 Flash Migration & RAG Live Validation Readiness)
- **Date**: `2026-10-11`
- **Execution Mode**: **OFFLINE ONLY** (`0` real Gemini API calls, `0` Supabase production DB connections)
- **Target Test Configuration**:
  - `chatModel`: `"gemini-3.8-flash"`
  - `thinkingConfig`: `{"thinkingLevel": "medium"}`
  - `embeddingModel`: `"gemini-embedding-001"` (`768` dimensions, L2 normalized)
- **Production Impact**: **NONE** (`RAG_ENABLED=false`, production `settings` table and runtime defaults untouched)
- **Current Live Execution Status**: **NO-GO (Awaiting Administrator Sign-off on Quota, Corpus `L1`/`L2`, and Live Execution Flags)**

---

## 1. Executive Summary

Phase 2.4.4 migrates the RAG Live Validation harness (`test/rag/live/*`) and the Gemini 3 request builder (`src/java/ai/provider/GeminiProvider.java`) to target **`gemini-3.8-flash`** with **`thinkingConfig: {"thinkingLevel": "medium"}`**, resolving the legacy `thinkingBudget: 0` incompatibility identified in Phase 2.4.3 while preserving all zero-cost, zero-retry, and database-isolation invariants.

Key outcomes of Phase 2.4.4:

1. **Gemini 3.8 Flash API Compatibility**:
   - Verified against official Google Gemini 3 / 3.8 documentation (`ai.google.dev/gemini-api/docs/gemini-3`, `models/gemini-3.8-flash`, `thinking`, `whats-new-gemini-3.6`, `pricing`, and `api/generate-content#ThinkingConfig`).
   - Updated `GeminiProvider.buildRequestBody()` so that `gemini-3*` models omit deprecated sampling parameters (`temperature`, `topP`) that are unsupported starting in Gemini 3.6/3.8, while preserving `temperature` and `topP` for `gemini-2.5-*` and earlier models so production behavior is unchanged.
   - Updated `LiveSafety` to require exact `"chatModel": "gemini-3.8-flash"` and exact `"thinkingConfig": {"thinkingLevel": "medium"}`, and to reject `thinkingBudget`, `low`/`high`/`minimal` thinking levels, deprecated sampling parameters (`temperature`, `topP`, `topK`, `candidateCount`), and prefilled `"model"` turns before any network send.
   - Updated `LiveRagRunner` to accept and record `thoughtsTokenCount` (`0 <= thoughtsTokenCount <= 1200`) and verify the combined output + thinking token bound (`candidatesTokenCount + thoughtsTokenCount <= maxOutputTokens (1200)`).
2. **100% Offline Verification Passed (`0` Real API Calls)**:
   - Live Runner Safety Suite: **81/81 PASS** (`+18` new Gemini 3.8 compatibility & wire-guard tests).
   - Live Entry Gate Suite: **4/4 PASS** (PowerShell syntax **5/5 PASS**).
   - Container Isolation Mock Suite: **4/4 PASS**.
   - Live Runner Isolated PostgreSQL Mock E2E Suite (`LQ1`–`LQ6`): **7/7 safety assertions PASS**, **6/6 scenarios PASS** (`4/4` positive recall, `2/2` unknown abstentions, mean `Precision@3 = 0.875`), **14/14 mock requests**, `thoughtsTokenCount` recorded in sanitized evidence.
   - Offline RAG Regression Suite: **75/75 PASS** (`35` behavior + `13` corpus + `27` quality) + **22/22** subject selection PASS.
   - Single Embedding Mock Suite: **29/29 PASS**.
   - Offline Chatbot Regression Suite: **86/86 PASS**.
   - Isolated PostgreSQL 17 + `pgvector 0.8.2` Integration Suite: **24/24 PASS**.
   - Isolated PostgreSQL Phase 2.4 E2E Suite: **41/41 PASS**.
   - Existing Evidence & Lock Preservation: **97/97 files identical** (SHA-256 hash and UTC timestamp unchanged).

---

## 2. Gemini 3.8 Flash Compatibility Analysis

The table below documents the 12-point audit against official Google Gemini API documentation:

| # | Audit Area | Official Gemini 3.8 Specification | Repository Implementation & Enforcement | Status |
|---|---|---|---|---|
| 1 | **Exact Model ID** | `gemini-3.8-flash` (GA stable model; `1,048,576` input token limit, `65,536` output token limit). | `phase24-live-plan.json` sets `"chatModel": "gemini-3.8-flash"`. `LiveSafety.Plan` enforces `"gemini-3.8-flash".equals(model)` (`CHAT_MODEL_MISSING`). | **PASS** |
| 2 | **REST Endpoint & API Version** | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent` with `x-goog-api-key` header. | `LiveSafety.Plan.endpoint("chat")` constructs exact `v1beta/models/gemini-3.8-flash:generateContent`. `SinglePostWire` sends HTTP/1.1 TLS POST with `x-goog-api-key`. | **PASS** |
| 3 | **`thinkingConfig` Structure** | Nested inside `generationConfig.thinkingConfig`: `{"thinkingLevel": "medium"}`. Sending both `thinkingBudget` and `thinkingLevel` returns HTTP 400. | `LiveSafety.Plan` and `LiveSafety.Ledger.send()` require `thinkingConfig` to have `size() == 1`, `"thinkingLevel": "medium"`, and `!thinking.has("thinkingBudget")`. | **PASS** |
| 4 | **`thinkingLevel: "medium"`** | `gemini-3.8-flash` supports `"low"`, `"medium"` (default), `"high"`. `"minimal"` is unsupported on `gemini-3.8-flash`; `thinkingBudget: 0` is legacy 2.5. | Enforced as exact `"medium"` in `LiveSafety.Plan` (`THINKING_CONFIGURATION`) and verified on the wire in `LiveSafety.Ledger.send()` (`CHAT_CONFIGURATION`). | **PASS** |
| 5 | **Structured Outputs & JSON Schema** | Supports `responseMimeType: "application/json"` with `responseJsonSchema` in `generationConfig`. | `GeminiProvider.buildRequestBody()` sets `responseMimeType` and `responseJsonSchema` for `model.startsWith("gemini-3")`. `LiveSafety.Ledger.send()` requires both fields. | **PASS** |
| 6 | **Deprecated / Removed Sampling Parameters & Prefilled Turns** | Starting in Gemini 3.6/3.8, `temperature`, `topP`, `topK`, `candidateCount`, and prefilled `"model"` turns (last message role != `"user"`) are deprecated/unsupported and rejected with HTTP 400. | `GeminiProvider.buildRequestBody()` omits `temperature` and `topP` when `model.startsWith("gemini-3")`. `LiveSafety.Ledger.send()` rejects `temperature`, `topP`, `topK`, `candidateCount`, `thinkingBudget`, and any `contents` array not ending with `"role": "user"`. | **PASS** |
| 7 | **Token Usage & `thoughtsTokenCount`** | `usageMetadata` includes `promptTokenCount`, `candidatesTokenCount`, `totalTokenCount`, and `thoughtsTokenCount` (`>= 0`). | `LiveRagRunner` records all four fields in `summary.usage` (`chat-LQ*.result.json`) and validates `0 <= thoughtsTokenCount <= 1200` (`CHAT_THINKING_USAGE_BOUND`). | **PASS** |
| 8 | **`maxOutputTokens` Interaction** | `maxOutputTokens` caps the **combined total** of `candidatesTokenCount + thoughtsTokenCount`. | `maxOutputTokens` is fixed at `1200`. Our JSON envelope is ~50–150 tokens, leaving >1,000 tokens of headroom for `medium` thinking. `LiveRagRunner` enforces `candidatesTokenCount + thoughtsTokenCount <= 1200` (`CHAT_TOTAL_OUTPUT_USAGE_BOUND`). | **PASS** |
| 9 | **`finishReason`** | Must be `"STOP"` for a complete structured JSON response; `"MAX_TOKENS"`, `"SAFETY"`, `"RECITATION"` indicate truncation or block. | `LiveRagRunner.Chat.chat()` requires `"STOP".equals(finishReason)` (`CHAT_TRUNCATED_OR_BLOCKED`) and halts immediately without retry on any other value. | **PASS** |
| 10 | **Validation of JSON Response** | Response `candidates[0].content.parts` may contain thought parts (`"thought": true`) before the JSON text part. | `GeminiProvider.customerText(parts)` skips any part with `"thought": true`. `ConsultationReply.parse()` strictly validates schema keys, catalog IDs, and allowed `knowledgeChunkIds`. | **PASS** |
| 11 | **Thought Signatures (`thoughtSignature`)** | Required in multi-turn `functionCall` / tool-use turns; for text-only turns in `generateContent`, standard `user`/`model` text turns do not require `thoughtSignature` replay. | Chatbot uses zero `functionCall` tools. `ConsultationEngine` renders deterministic Vietnamese replies and stores plain text turns in session history. | **PASS** |
| 12 | **Compatibility with `ChatSessionManager`** | Multi-turn follow-up (`LQ3` -> `LQ4` in session `gaming-follow-up`) must preserve customer domain context without leaking assistant instructions. | `RetrievalQuery.plan()` expands `LQ4` using customer-only domain history (`game`), freezes query hashes before request #1, and `ChatSessionData` maintains chronological `user`/`model` turns ending with `"role": "user"`. | **PASS** |

---

## 3. Thinking Level MEDIUM Analysis

1. **Why `thinkingLevel: "medium"` on `gemini-3.8-flash`**:
   - Per official Gemini 3.8 documentation, `gemini-3.8-flash` uses dynamic thinking controlled by `thinkingLevel` (`"low"`, `"medium"`, `"high"`), where `"medium"` is the balanced default for general reasoning and structured instruction following.
   - `gemini-3.8-flash` does **not** support `"minimal"` and deprecates the legacy 2.5 `thinkingBudget: 0` parameter.
2. **Token & Latency Behavior**:
   - Under `thinkingLevel: "medium"`, `gemini-3.8-flash` generates internal reasoning tokens reported in `usageMetadata.thoughtsTokenCount` (`>= 0`).
   - Any part in `candidates[0].content.parts` marked `"thought": true` is stripped by `GeminiProvider.customerText()` and never reaches `ConsultationReply.parse()` or the customer response.
   - Because `maxOutputTokens: 1200` bounds `candidatesTokenCount + thoughtsTokenCount`, worst-case output + thinking consumption across 6 Chat POSTs is strictly bounded by `6 * 1,200 = 7,200` tokens (`<= 1,200` per request).
   - Per-request turn latency remains guarded by `LiveSafety` (`<= 10,000 ms` per Chat POST and `<= 10,000 ms` per turn excluding deliberate 65-second quota spacing).

---

## 4. Code Changes Made

Only minimal, targeted files were updated to support `gemini-3.8-flash` (`thinkingLevel: "medium"`), leaving the production chatbot configuration and all existing locks/evidence untouched:

1. [`src/java/ai/provider/GeminiProvider.java`](file:///d:/miniproject/fpt/src/java/ai/provider/GeminiProvider.java#L305-L320)
   - In `buildRequestBody()`, omitted deprecated `temperature` and `topP` properties from `generationConfig` when `model != null && model.startsWith("gemini-3")`.
   - Preserved `temperature` and `topP` for `gemini-2.5-*` and earlier models so existing production models behave identically.
2. [`test/rag/live/LiveSafety.java`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L38-L55)
   - Enforced `"gemini-3.8-flash".equals(model)` (`CHAT_MODEL_MISSING`).
   - Updated `Plan` thinking check (`THINKING_CONFIGURATION`) to require `thinkingConfig` with `size() == 1`, `"thinkingLevel": "medium"`, `!thinking.has("thinkingBudget")`, and `!yes(approval, "thinkingDisabledSupported")`.
   - Updated `Ledger.send()` chat wire guard (`CHAT_CONFIGURATION`, `CHAT_TURN_ROLE`) to require `responseJsonSchema`, reject `temperature`, `topP`, `topK`, `candidateCount`, and `thinkingBudget`, and verify `contents` ends with a `"user"` role turn.
3. [`test/rag/live/LiveRagRunner.java`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L71-L72)
   - Replaced the legacy `thoughtsTokenCount == 0` requirement with bounded Gemini 3.8 thinking usage validation:
     - `0 <= thoughtsTokenCount <= 1200` (`CHAT_THINKING_USAGE_BOUND`)
     - `candidatesTokenCount + thoughtsTokenCount <= 1200` (`CHAT_TOTAL_OUTPUT_USAGE_BOUND`)
     - `0 <= totalTokenCount <= chatInputByteCap + 1200 + 64` (`CHAT_TOTAL_USAGE_BOUND`)
4. [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json#L11-L13)
   - Configured `"chatModel": "gemini-3.8-flash"`, `"liveApproval.chatModel": "gemini-3.8-flash"`, `"liveApproval.thinkingConfig": {"thinkingLevel": "medium"}`, `"liveApproval.thinkingDisabledSupported": false`, `"chatInputTokenLimit": 1048576`, and `"chatOutputTokenLimit": 65536`.
   - Kept `"approved": false`, `"executionAuthorized": false`, `"productionRagEnabled": false`, `"paidRequestsAllowed": false`, `"maximumApprovedPaidCostUsd": 0`, and `L1`/`L2` `"approvedForLive": false`.
5. [`test/rag/live/LiveRunnerSafetyTest.java`](file:///d:/miniproject/fpt/test/rag/live/LiveRunnerSafetyTest.java#L18-L140) & [`test/rag/live/LiveRunnerDatabaseTest.java`](file:///d:/miniproject/fpt/test/rag/live/LiveRunnerDatabaseTest.java#L31-L36)
   - Updated `approvedFixture()` to use `gemini-3.8-flash` and `{"thinkingLevel": "medium"}`.
   - Added 18 offline unit tests covering `gemini-3.8-flash` request generation, rejection of `gemini-2.5-flash` / `gemini-3.1-flash-lite-preview` / `gemini-3.8-pro`, rejection of `thinkingBudget: 0` / `low` / `high` / `minimal` / `disabledFlag`, wire-guard rejection of `temperature` / `topP` / `topK` / `candidateCount` / `thinkingBudget` / `missingSchema` / `prefilledModelRole`, and bounded `thoughtsTokenCount` recording.
   - Updated `LiveRunnerDatabaseTest` mock chat responses to include a `"thought": true` part and `usageMetadata.thoughtsTokenCount = 48`, verifying end-to-end persistence in `chat-LQ1.result.json`.

---

## 5. Offline Test Results

All required offline test suites were executed with **0 real Gemini API requests** and **0 production DB connections**:

| Test Suite | Command | Result | Real API Calls | Sanitized Evidence / Output |
|---|---|---|---:|---|
| **Live Runner Safety Suite** | `test/test-rag-live-runner.ps1` | **81/81 PASS** (`0` fail) | `0` | `.build/phase241-mocks-7a20bc57-7d15-49f0-bf82-654e216dba74/summary.json` |
| **Live Entry Gate Suite** | `test/test-rag-live-entry.ps1` | **4/4 PASS** (syntax **5/5 PASS**) | `0` | `.build/phase241-entry-61bc28129cdb4163a8a5bf3ab4201f92.json` |
| **Container Isolation Mock Suite** | `test/test-rag-live-isolation.ps1` | **4/4 PASS** | `0` | `.build/phase241-isolation-013819d470ce4d219cd5de2fd1b0a069.json` |
| **Live Runner DB Mock E2E Suite** | `test/test-rag-live-database.ps1` | **7/7 assertions PASS**, **LQ1–LQ6 (6/6) PASS**, **14 mock requests** | `0` | `.build/phase241-db-mocks/phase24-offline-db-41f57977-4de5-4c9d-97bf-c79740fd56bd.evidence/offline-database-validation.json` |
| **Offline RAG Regression Suite** | `test/run-rag-phase2-offline.ps1` | **75/75 PASS** (`35` behavior + `13` corpus + `27` quality) + **22/22** subject selection PASS | `0` | Console verified (`0` failures) |
| **Offline Chatbot Regression Suite** | `test/run-chat-tests.ps1` | **86/86 PASS** (`26` consultation + `21` adaptive + `17` fee + `9` registration + `7` provider + `6` frontend) | `0` | Console verified (`0` failures) |
| **Single Embedding Mock Suite** | `test/rag-single-embedding-tests.ps1` | **29/29 PASS** | `0` | `.build/rag-embedding-probes/phase23-single-embedding-20261011-v1.mocks.0957da1aa3b94bf286c50b71aa1f6b8a.json` |
| **PostgreSQL + pgvector Integration** | `test/run-rag-isolated.ps1` | **24/24 PASS** (PostgreSQL 17.10 + `pgvector 0.8.2`) | `0` | Container `fpt-rag-test-9c7573a91c06` (stopped & preserved) |
| **Phase 2.4 PostgreSQL E2E Suite** | `test/run-rag-isolated.ps1 -Phase24` | **41/41 PASS** | `0` | `.build/phase24-e2e-632fb1f2d5b8413e98575df869fc75f4.json` (Container `fpt-rag-test-78a02118b7f3`) |
| **Preserved Artifacts & Locks Check** | SHA-256 + UTC ticks check against `.build/phase242-preserve-e5fd204e3c94450abccf7a10bc616e7f.json` | **97/97 IDENTICAL** (`0` mismatches) | `0` | All Phase 2.3 and Phase 2.4.1/2.4.2 locks and results untouched |

---

## 6. LQ1–LQ6 Mock E2E Results (PostgreSQL 17 + `pgvector 0.8.2` + `gemini-3.8-flash` Mock)

Executed against newly created isolated container `fpt-rag-live-test-116336d16381` (`127.0.0.1`, `fpt_rag_test`) with simulated `gemini-3.8-flash` (`thinkingLevel: "medium"`, `"thought": true` part exclusion, and `thoughtsTokenCount: 48`):

| Case ID | Customer Question | Session | Expected Source | Top-3 Retrieved | Validated Context | Selected Source | Grounding / Abstention Behavior | Result |
|---|---|---|---|---|---|---|---|---|
| **LQ1** | `"Mạng nhà mình chậm quá."` | `LQ1` | `["L1"]` | `["L1"]` | `["L1"]` | `["L1"]` | Grounded literal excerpt from `L1` only | **PASS** |
| **LQ2** | `"Shop ơi mạng nhà tui yếu quá."` | `LQ2` | `["L1"]` | `["L1"]` | `["L1"]` | `["L1"]` | Grounded literal excerpt from `L1` only | **PASS** |
| **LQ3** | `"Hướng dẫn chơi game Internet sao cho ổn?"` | `gaming-follow-up` | `["L2"]` | `["L2"]` | `["L2"]` | `["L2"]` | Grounded literal excerpt from `L2` only; no ping guarantee | **PASS** |
| **LQ4** | `"Vậy cách đó có đảm bảo ping thấp không?"` | `gaming-follow-up` | `["L2"]` | `["L2", "L1"]` | `["L2"]` | `["L2"]` | Query-aware selection retains `L2` via customer gaming context | **PASS** |
| **LQ5** | `"WiFi 6 có tốt hơn WiFi 5 không?"` | `LQ5` | `[]` | `["L1"]` | `["L1"]` | `[]` | Safe abstention; does not invent WiFi 6 vs WiFi 5 comparison | **PASS** |
| **LQ6** | `"Có cung cấp internet ở nước ngoài không?"` | `LQ6` | `[]` | `[]` | `[]` | `[]` | Safe abstention; discloses unconfirmed coverage | **PASS** |

- **Positive Recall@3**: `4/4` (`100%`)
- **Mean Precision@3 (raw retrieval)**: `0.875` (`>= 0.80` threshold)
- **Unknown Abstentions**: `2/2` (`100%`)
- **Unsupported Claims / Metadata Leaks / Business Overrides**: `0`
- **Recorded Mock Usage per Chat Call**: `promptTokenCount = 420`, `candidatesTokenCount = 64`, `thoughtsTokenCount = 48`, `totalTokenCount = 532` (all within `maxOutputTokens = 1200`).

---

## 7. Request Budget & Safety Verification

The live request budget remains strictly locked at a maximum of **14 POST requests**:

| Request Category | Endpoint | Method | Max Requests | Retry / Redirect | Pre-Send Reservation |
|---|---|---|---:|---:|---|
| **Document Embeddings (`L1`, `L2`)** | `.../v1beta/models/gemini-embedding-001:embedContent` | `POST` | `2` | `0` | Mandatory (`01`–`02` `.reserved.json`) |
| **Query Embeddings (`LQ1`–`LQ6`)** | `.../v1beta/models/gemini-embedding-001:embedContent` | `POST` | `6` | `0` | Mandatory (`03,05,07,09,11,13` `.reserved.json`) |
| **Gemini 3.8 Flash Chat (`LQ1`–`LQ6`)** | `.../v1beta/models/gemini-3.8-flash:generateContent` | `POST` | `6` | `0` | Mandatory (`04,06,08,10,12,14` `.reserved.json`) |
| **Metadata GET / `countTokens`** | Any | `GET` / `POST` | `0` | `0` | Forbidden (`UNPLANNED_ENDPOINT_OR_METHOD`) |
| **Total** | | | **14** | **0** | Atomic `CREATE_NEW` lock + journal |

Safety invariants verified by `LiveRunnerSafetyTest` (`81/81 PASS`):
- **Pre-send accounting**: Request slot and `NN-<operation>.reserved.json` are flushed to disk (`FileChannel.force(true)`) *before* `SinglePostWire.post()` opens a socket.
- **First-error halt**: Any HTTP non-200 (`429`, `503`, `302`, `400`), socket timeout, malformed JSON, `finishReason != "STOP"`, unapproved chunk ID, or latency violation immediately halts the entire run (`failed = true`) and forbids remaining slots.
- **Cross-process lock**: `<planId>.request.lock` is opened with `StandardOpenOption.CREATE_NEW` and is **never** deleted, preventing any duplicate or resumed execution under the same `planId`.
- **Sanitized evidence**: API keys (`RAG_EMBEDDING_API_KEY`, `RAG_CHAT_TEST_API_KEY`), raw headers, full 768-float vectors, and raw error bodies are never written to logs or `.result.json` files.

---

## 8. Free Tier & Quota Checklist (Google AI Studio Verification)

Per official Google Gemini API Pricing (`https://ai.google.dev/gemini-api/docs/pricing`), **`gemini-3.8-flash` Standard Tier** and **`gemini-embedding-001`** both offer a **Free Tier ($0.00 input / $0.00 output, including thinking tokens)** in eligible Google AI Studio projects:

| Model | Free Tier Input | Free Tier Output (incl. Thinking Tokens) | Paid Tier Rate (Forbidden in this Plan) | Plan Requirement |
|---|---|---|---|---|
| `gemini-embedding-001` | **Free of charge** | N/A (`768`-dim vector) | `$0.15 / 1M tokens` | `embeddingTier = "Free Tier"`, `RPD >= 8`, `RPM >= 1`, `TPM >= 2,048` |
| `gemini-3.8-flash` | **Free of charge** | **Free of charge** | `$0.75 / 1M` input, `$3.75 / 1M` output (through Dec 31, 2026) | `chatTier = "Free Tier"`, `RPD >= 6`, `RPM >= 1`, `TPM >= 17,648` |

### Step-by-Step Administrator Verification in Google AI Studio

Before authorizing the live run, the Administrator must verify in [Google AI Studio](https://aistudio.google.com/):

1. **API Keys & Project Identity**:
   - Open **Google AI Studio -> API keys**.
   - Confirm that both `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` belong to a dedicated test project (never copied from production `.local/runtime.json` or Supabase `settings`).
   - Confirm the project's **Plan / Billing Tier** displays **Free** (`Free Tier`), with billing either unlinked or strictly capped at `$0.00` (`paidRequestsAllowed = false`, `maximumApprovedPaidCostUsd = 0`).
2. **Model Availability & Rate Limits**:
   - Open **Dashboard -> Rate limits / Usage** for that project.
   - Verify `gemini-3.8-flash` is available on **Free Tier** with:
     - Remaining **RPD** `>= 6` requests
     - **RPM** limit `>= 1` (runner spaces same-model requests by `>= 65,000 ms`)
     - **TPM** limit `>= 17,648` tokens/minute (`16,384` input byte cap + `1,200` `maxOutputTokens` covering output + `medium` thinking tokens + `64` safety margin)
   - Verify `gemini-embedding-001` is available on **Free Tier** with:
     - Remaining **RPD** `>= 8` requests (`2` document + `6` query embeddings)
     - **RPM** limit `>= 1`
     - **TPM** limit `>= 2,048` tokens/minute

---

## 9. Corpus Approval Checklist (`L1` & `L2`)

Both test documents remain locked (`"approvedForLive": false`) until the Administrator explicitly approves them for the isolated test database:

| ID | Title | Exact Content (`UTF-8`) | Source File | Scope / Authority | Expected Chunks | Conflict with Business Data | Current Status |
|---|---|---|---|---|---:|---|---|
| **L1** | `Hướng dẫn kết nối mạng WiFi` | `"Anh/chị kiểm tra kết nối dây mạng an toàn trước khi liên hệ hỗ trợ."` | `test/rag/RagQualityTest.java` | Synthetic QA troubleshooting guidance (`admin_reviewed` in isolated test DB; **not** official FPT policy) | `1` (`69` chars) | **None** (contains no package prices, speeds, or installation fees) | `approvedForLive: false` |
| **L2** | `Hướng dẫn chơi game` | `"Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế."` | `src/java/ai/consultation/ConsultationPolicy.java` | Existing customer-facing gaming guidance (`admin_reviewed` in isolated test DB) | `1` (`110` chars) | **None** (aligned with `ConsultationPolicy`; states speed does not guarantee ping) | `approvedForLive: false` |

---

## 10. Remaining Blockers Before Live Execution

All technical, code, schema, and thinking-configuration blockers from Phase 2.4.3 have been **resolved offline**. Only the mandatory **Administrator Governance Gates** remain:

1. **Blocker 1 — Quota & Free Tier Confirmation (`cost.*`, `quota.verifiedForThisPlan`, `liveApproval.*`)**:
   - Administrator must confirm in Google AI Studio that the test project for `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` is on **Free Tier** and has sufficient remaining RPD/RPM/TPM for `gemini-3.8-flash` and `gemini-embedding-001`.
2. **Blocker 2 — Corpus Approval (`documents[0].approvedForLive`, `documents[1].approvedForLive`)**:
   - Administrator must confirm `approvedForLive = true` for `L1` and `L2`.
3. **Blocker 3 — Time-Bounded Live Execution Authorization (`approved`, `executionAuthorized`, `verifiedAtEpochMs`, `expiresAtEpochMs`)**:
   - Administrator must grant explicit authorization (`<= 30 minutes` window) to execute the single 14-request live validation run.

---

## 11. Final GO / NO-GO Decision

- **Offline Technical Readiness (`gemini-3.8-flash` + `thinkingLevel: "medium"`)**: **READY (PASS)**
- **Immediate Unapproved Live API Execution**: **NO-GO** (by design: `approved=false`, `executionAuthorized=false`, `L1`/`L2` `approvedForLive=false`, and live project quota fields are `null` until Administrator sign-off).
- **Conditional GO Criteria**: Once the Administrator completes the Sign-off Block in Section 12 and provides `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` in the process environment, `test/run-rag-live.ps1 -ApprovedLivePlan` is ready to execute the single bounded 14-request validation run.

---

## 12. Administrator Sign-off Block

To authorize the single 14-request Live Validation run, please confirm the following items:

- [ ] **Model & Thinking Config Approved**: `chatModel = "gemini-3.8-flash"` with `thinkingConfig = {"thinkingLevel": "medium"}` and `embeddingModel = "gemini-embedding-001"` (`768` dimensions) for the isolated test runner only (production model unchanged).
- [ ] **Free Tier & Quota Verified in Google AI Studio**:
  - `embeddingTier = "Free Tier"`, `embeddingRpdRemaining >= 8`, `embeddingRpmRemaining >= 1`, `embeddingTpmRemaining >= 2048`
  - `chatTier = "Free Tier"`, `chatRpdRemaining >= 6`, `chatRpmRemaining >= 1`, `chatTpmRemaining >= 17648`
  - `paidRequestsAllowed = false`, `maximumApprovedPaidCostUsd = 0`
- [ ] **Test API Keys Ready**: `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` are set in the shell session and are dedicated test keys (not production keys).
- [ ] **Test Corpus Approved**: `L1` (`approvedForLive = true`) and `L2` (`approvedForLive = true`) approved for indexing in the disposable local Docker PostgreSQL container (`127.0.0.1 / fpt_rag_test`).
- [ ] **Execute Single Run**: Authorize setting `approved = true`, `executionAuthorized = true`, and a 30-minute `verifiedAtEpochMs` / `expiresAtEpochMs` window in `test/rag/fixtures/phase24-live-plan.json` and running `test/run-rag-live.ps1 -ApprovedLivePlan`.

---

## 13. Phase 3 Preparation — Admin Knowledge Base Checklist (Design Only — Not Implemented Yet)

To ensure a smooth transition after Phase 2.4 Live Validation completes, the following architectural & security checklist is prepared for **Phase 3 (Admin Knowledge Base Management)**:

1. **Knowledge Document CRUD & Lifecycle States**:
   - Support structured lifecycle states in `knowledge_documents`: `draft`, `published`, `archived`, `expired` (`effective_until < now()`), and `superseded` (`superseded_by_id IS NOT NULL`).
   - Only `published`, currently effective, non-superseded documents with `indexing_status = 'ready'` and matching `document_version` / `document_hash` may be served by `KnowledgeChunkDAO`.
2. **Immutable Versioning & Content Hashing**:
   - Every content or policy-metadata edit increments `version`, computes a new SHA-256 `content_hash` (`VectorValues.hash(...)`), sets `indexing_status = 'pending'`, and enqueues a new `knowledge_index_jobs` entry atomically inside the same database transaction.
   - Old chunks from prior versions are atomically replaced or excluded by `c.document_version = d.version` and revalidated immediately before rendering (`KnowledgeChunkDAO.current()`).
3. **Required Source & Authority Metadata**:
   - Enforce mandatory metadata on publish: `category` (`guide`, `faq`, `policy`, `technical`), `policy_key`, `authority_level` (`official`, `admin_reviewed`), `source`, optional `effective_from` / `effective_until`, and `superseded_by_id` (validated against self-cycles and authority downgrades).
4. **Publication Approval Workflow**:
   - Separate `draft` editing from `publish` / `withdraw` actions.
   - Require explicit administrator review confirmation before transitioning a document to `published` (`approved_by`, `approved_at`).
5. **Background Indexing Job & Re-indexing**:
   - Reuse `KnowledgeIndexingService` + `KnowledgeIndexJobDAO` with bounded worker leases, content-hash deduplication (skipping redundant embedding calls when only non-indexed metadata changes), and fail-closed status reporting (`pending` -> `processing` -> `ready` / `failed`).
6. **Role-Based Access Control (RBAC) & Audit Logging**:
   - Restrict all Knowledge Base Admin endpoints (`/admin/knowledge/*`) to authenticated Administrator sessions with CSRF protection.
   - Record an immutable audit log entry for every create, edit, publish, withdraw, supersede, and re-index action (actor ID, timestamp, document ID, old/new version, status).
7. **Prompt Injection & Unsafe Content Defense**:
   - Enforce `KnowledgeContentPolicy` both at Admin publish/preview time and at retrieval time to reject documents containing system-prompt overrides, role-hijacking markers, or internal metadata tags (`UNSAFE_CONTENT`).
8. **Strict Separation from Authoritative Business Data**:
   - Prevent Knowledge Base documents from overriding authoritative SQL package prices, speeds, installation fees, or promotions (`INVALID_BUSINESS_CLAIM` filter in `KnowledgeContentPolicy` + `ProductCatalog` precedence in `ConsultationEngine`).
