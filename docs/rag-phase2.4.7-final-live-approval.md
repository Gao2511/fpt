# PHASE 2.4.7 — GEMINI PROJECT VERIFICATION & FINAL LIVE APPROVAL REPORT

- **Project:** `D:\miniproject\fpt`
- **Branch:** `feature/rag-integration` (`HEAD: 48b700f`)
- **Date:** `2026-10-11`
- **Plan ID:** `phase24-e2e-20261011-v1` ([`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json))
- **Execution Mode:** **OFFLINE ONLY** (`realGeminiApiRequests = 0`, `supabaseProductionConnections = 0`)
- **Offline Architecture, Pacing & Safety Status:** **PASS (`OFFLINE READY` — 87/87 Safety, 7/7 DB, 6/6 Mock E2E, 97/97 Preserved Files)**
- **Overall Live Execution Status:** **NO-GO (`LIVE NO-GO` — Pending Administrator Confirmation of Project-Key Mapping, Today's Remaining RPD Quota, Corpus `L1`/`L2` Approval, and Final Execution Authorization)**

---

## 1. Environment Audit (`TASK E`)

Environment variables are process-scoped in Windows (`pwsh`). Setting `$env:RAG_EMBEDDING_API_KEY` and `$env:RAG_CHAT_TEST_API_KEY` inside the Administrator's interactive PowerShell 7 window does **not** automatically export them into the separate Antigravity agent process.

Per Task E, the table below reports only `PRESENT / MISSING` and verification status (zero secrets read, logged, or displayed):

| Variable Name | Purpose & Model | Status in Antigravity Process | Status in Admin's PowerShell 7 Session | Correct Project Mapping | Free Tier Status |
|---|---|---|---|---|---|
| `RAG_EMBEDDING_API_KEY` | Embedding (`gemini-embedding-001`, `768` dims) | `MISSING` | `PRESENT` *(reported by Admin)* | `UNVERIFIED` *(Needs Admin check in AI Studio UI that key belongs to **Project B — Gemini Project**)* | `VERIFIED` *(Project B screenshot shows Free Tier; pending key-to-project mapping)* |
| `RAG_CHAT_TEST_API_KEY` | Primary Chat (`gemini-3.8-flash`, `thinkingLevel: "medium"`) | `MISSING` | `PRESENT` *(reported by Admin)* | `UNVERIFIED` *(Needs Admin check in AI Studio UI that key belongs to **Project A — Default Gemini Project**)* | `VERIFIED` *(Project A screenshot shows Free Tier; pending key-to-project mapping)* |

- **Production Key Fallback:** **FORBIDDEN & BLOCKED** (`LiveRagRunner.main()` at [`LiveRagRunner.java:152`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L152) reads only `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY`; `LiveRunnerSafetyTest.java:104` verifies that `GEMINI_API_KEY` and `GOOGLE_API_KEY` are ignored).
- **Execution Implication:** Because the keys reside in the Administrator's PowerShell 7 session (`PRESENT`) and not in the Antigravity process (`MISSING`), the final `--preflight` and `run-rag-live.ps1 -ApprovedLivePlan` commands must be executed directly inside **that same Administrator PowerShell 7 window** once the plan JSON is approved.

---

## 2. API Key / Project Mapping Verification (`TASK A`)

### 2.1 Official Google AI Studio Documentation Audit (`0` API Calls)

Per Google's official documentation ([Using Gemini API keys](https://ai.google.dev/gemini-api/docs/api-key), updated `2026-10-06 UTC`, and [Rate limits](https://ai.google.dev/gemini-api/docs/rate-limits), updated `2026-10-09 UTC`):

1. **Project Association:** Every Gemini API key is bound to a single Google Cloud project that governs its usage tier (`Free` vs `Paid`), billing, and per-model rate limits (`RPM`, `TPM`, `RPD`).
2. **Why Offline Lexical Inspection Cannot Identify Project Ownership:** A Gemini API key (`AIzaSy...`) is an opaque token that does **not** encode the Google Cloud project name or project number in plaintext. Verifying which project owns a given key without sending an API request requires matching the **last 4 characters** of the key against the **API Keys** list in the Google AI Studio web UI (`https://aistudio.google.com/api-keys`).
3. **Key Type & Dormant/Unrestricted Key Policy (May 2026 Update):**
   - Starting May 28, 2026, new keys created in AI Studio are **Authorization (auth) keys** restricted to the Gemini API by default.
   - Starting May 7, 2026, Google blocks **unrestricted standard keys** (`Unrestricted` or `Blocked` badge in AI Studio). Both test keys must show as active (`Auth key` or `Restricted to Gemini API`) and **not** `Blocked` or `Unrestricted`.
4. **Separate Projects for Embedding vs. Chat Is Valid & Safe:**
   - Using **Project B ("Gemini Project")** for `RAG_EMBEDDING_API_KEY` (`gemini-embedding-001`) and **Project A ("Default Gemini Project")** for `RAG_CHAT_TEST_API_KEY` (`gemini-3.8-flash`) assigns one fixed key to Embedding and one fixed key to Chat.
   - It does **not** rotate keys on the same model or circumvent per-project rate limits.

### 2.2 Key-to-Project Mapping Assessment

| Key Variable | Intended Google AI Studio Project | Intended Model | Current Assessment | Exact Manual Check Required in `https://aistudio.google.com/api-keys` |
|---|---|---|---|---|
| `RAG_CHAT_TEST_API_KEY` | **Project A — Default Gemini Project** | `gemini-3.8-flash` (`5 RPM / 250K TPM / 20 RPD`) | **NEEDS ADMIN CONFIRMATION** | Confirm the last 4 characters of `$env:RAG_CHAT_TEST_API_KEY` match an active key under **Default Gemini Project** (`Free tier`, not `Blocked`/`Unrestricted`, not used by production). |
| `RAG_EMBEDDING_API_KEY` | **Project B — Gemini Project** | `gemini-embedding-001` (`100 RPM / 30K TPM / 1,000 RPD`) | **NEEDS ADMIN CONFIRMATION** | Confirm the last 4 characters of `$env:RAG_EMBEDDING_API_KEY` match an active key under **Gemini Project** (`Free tier`, not `Blocked`/`Unrestricted`, not used by production). *(Note: if Project A also has `gemini-embedding-001` on Free Tier with `>= 8` RPD remaining, Project A may also host the embedding key, provided the Admin confirms which project owns `RAG_EMBEDDING_API_KEY`.)* |

### 2.3 Safe PowerShell Helper to Check Only the Last 4 Characters (No Key Leak)

The Administrator can run this command in their PowerShell 7 session to see **only the last 4 characters** of each key and match them visually on `https://aistudio.google.com/api-keys`:

```powershell
[pscustomobject]@{
    EmbeddingKey_Status = if ($env:RAG_EMBEDDING_API_KEY) { 'PRESENT' } else { 'MISSING' }
    EmbeddingKey_Last4  = if ($env:RAG_EMBEDDING_API_KEY -and $env:RAG_EMBEDDING_API_KEY.Length -ge 8) { '...' + $env:RAG_EMBEDDING_API_KEY.Substring($env:RAG_EMBEDDING_API_KEY.Length - 4) } else { 'N/A' }
    ChatKey_Status      = if ($env:RAG_CHAT_TEST_API_KEY) { 'PRESENT' } else { 'MISSING' }
    ChatKey_Last4       = if ($env:RAG_CHAT_TEST_API_KEY -and $env:RAG_CHAT_TEST_API_KEY.Length -ge 8) { '...' + $env:RAG_CHAT_TEST_API_KEY.Substring($env:RAG_CHAT_TEST_API_KEY.Length - 4) } else { 'N/A' }
} | Format-Table -AutoSize
```

---

## 3. Model, Free Tier & Quota Verification (`TASK B`)

### 3.1 Analysis of Screenshot Evidence vs. Plan Requirements

| Dimension | Primary Chat (`gemini-3.8-flash`) — **Project A ("Default Gemini Project")** | Embedding (`gemini-embedding-001`) — **Project B ("Gemini Project")** | Backup Chat (`gemini-3.5-flash-lite` / `gemini-3.1-flash-lite`) |
|---|---|---|---|
| **Exact Model ID in Code** | `gemini-3.8-flash` (`thinkingLevel: "medium"`) | `gemini-embedding-001` (`dimensions: 768`, L2 normalized) | `gemini-3.5-flash-lite` (`thinkingLevel: "low"`); secondary compatible: `gemini-3.1-flash-lite` |
| **Role in 14-Request Live Run** | **Active (`6` POSTs max)** | **Active (`8` POSTs max = `2` doc + `6` query)** | **DISABLED (`0` POSTs; `enabledDuring14RequestLiveRun = false`)** |
| **Usage Tier in Screenshot** | **Free Tier (`$0.00`)** — *Verified in screenshot* | **Free Tier (`$0.00`)** — *Verified in screenshot* | **Free Tier (`$0.00`)** (`gemini-3.1-flash-lite` shown in Project B screenshot: `15 RPM / 250K TPM / 500 RPD`) |
| **RPM Limit vs. Required** | Screenshot Limit: **`5` RPM** vs. Required: **`>= 1` RPM** (**Limit PASS** — runner sends `<= 1` req / `65s`) | Screenshot Limit: **`100` RPM** vs. Required: **`>= 1` RPM** (**Limit PASS** — runner sends `<= 1` req / `65s`) | N/A during 14-request run |
| **TPM Limit vs. Required** | Screenshot Limit: **`250,000` TPM** vs. Required: **`>= 17,648` TPM** (**Limit PASS** — `14x` safety margin) | Screenshot Limit: **`30,000` TPM** vs. Required: **`>= 2,048` TPM** (**Limit PASS** — `14x` safety margin) | N/A during 14-request run |
| **RPD Limit vs. Required** | Screenshot Limit: **`20` RPD** vs. Required remaining: **`>= 6` RPD** (**Limit PASS**, requires `<= 14` used today) | Screenshot Limit: **`1,000` RPD** vs. Required remaining: **`>= 8` RPD** (**Limit PASS**, requires `<= 992` used today) | N/A during 14-request run |
| **28-Day Peak Usage in Screenshot** | `2 / 5` RPM, `1.9K / 250K` TPM, `6 / 20` RPD | `1 / 100` RPM, `8 / 30K` TPM, `2 / 1,000` RPD | N/A |
| **Current Remaining Quota Status** | **`UNVERIFIED` (Requires Admin confirmation of today's remaining RPD `>= 6`)** | **`UNVERIFIED` (Requires Admin confirmation of today's remaining RPD `>= 8`)** | N/A |

### 3.2 Critical Engineering Notes on Quota, 28-Day Peak, Reset Time, and Prior 429 Errors

1. **Why 28-Day Peak Usage Does Not Prove Current Remaining Quota:**
   - By default, Google AI Studio opens `https://aistudio.google.com/rate-limit?timeRange=last-28-days`, which displays the **highest peak usage observed over the last 28 days** (`6 / 20` RPD on Project A for `gemini-3.8-flash`, and `2 / 1,000` RPD on Project B for `gemini-embedding-001`).
   - While a 28-day daily peak of `6 / 20` RPD strongly indicates that daily usage has never exceeded `6` requests in any single day (`leaving >= 14 RPD`), strict safety policy requires the Administrator to confirm **current remaining quota for today** (`chatRpdRemaining >= 6` and `embeddingRpdRemaining >= 8`) rather than inferring it automatically.
2. **Daily Quota Reset Time (`Midnight Pacific Time` = `14:00 ICT`):**
   - Per official Google documentation (`https://ai.google.dev/gemini-api/docs/rate-limits#how-rate-limits-work`): *"Requests per day (RPD) quotas reset at midnight Pacific time."*
   - In Vietnam (`ICT`, `UTC+7`), Midnight US Pacific Daylight Time (`00:00 PDT`, `UTC-7`) is **14:00 (2:00 PM) Vietnam time** each day. Any requests sent after **14:00 ICT today (2026-10-11)** count toward today's RPD bucket (`20` RPD for `gemini-3.8-flash` on Project A; `1,000` RPD for `gemini-embedding-001` on Project B).
3. **Impact of Rate-Limit (`429`) Errors Seen on Other Models in Project A:**
   - Per Google's Rate Limits specification: *"Limits vary depending on the specific model being used."*
   - Rate-limit errors triggered previously on other models in Project A (e.g., legacy or preview models with `0` or lower RPM/RPD) do **not** consume the `gemini-3.8-flash` quota bucket (`5 RPM / 250,000 TPM / 20 RPD`).
   - However, to prevent any concurrent contention in Project A during the live test, no other script, browser tab, or model test should run in Project A during the ~8-minute execution window.

---

## 4. Corpus Approval Review (`TASK C`)

Both candidate documents in [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json#L36-L55) have been re-verified offline:

| Field | Document `L1` | Document `L2` |
|---|---|---|
| **Document ID** | `L1` | `L2` |
| **Title** | `Hướng dẫn kết nối mạng WiFi` | `Hướng dẫn chơi game` |
| **Exact UTF-8 Content** | `"Anh/chị kiểm tra kết nối dây mạng an toàn trước khi liên hệ hỗ trợ."` | `"Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế."` |
| **Source File in Repo** | [`test/rag/RagQualityTest.java`](file:///d:/miniproject/fpt/test/rag/RagQualityTest.java#L23) | [`src/java/ai/consultation/ConsultationPolicy.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationPolicy.java#L180) |
| **Nature & Authority** | Synthetic test troubleshooting text; saved as `authority = "admin_reviewed"`, `source = "TEST ONLY: Synthetic existing QA guidance, NOT a confirmed FPT policy"`. Never labeled `official`. | Synthetic/test consultation guidance; saved as `authority = "admin_reviewed"`, `source = "TEST ONLY: Existing customer-facing guidance; no new business policy"`. Never labeled `official`. |
| **Expected Chunks & Size** | `1` chunk (`87` UTF-8 bytes text + `34` UTF-8 bytes title + `64` margin = `185 <= 2,048` limit) | `1` chunk (`146` UTF-8 bytes text + `24` UTF-8 bytes title + `64` margin = `234 <= 2,048` limit) |
| **Target Database** | Disposable local Docker PostgreSQL (`127.0.0.1 / fpt_rag_test`) only; **never written to Supabase production**. | Disposable local Docker PostgreSQL (`127.0.0.1 / fpt_rag_test`) only; **never written to Supabase production**. |
| **Conflict with Business Data** | **None** (`0` package names, `0` prices, `0` speeds, `0` fees) | **None** (states bandwidth does not guarantee ping, matching `ConsultationPolicy`) |
| **Current Flag in Plan** | **`approvedForLive: false` (Preserved — awaiting Admin authorization)** | **`approvedForLive: false` (Preserved — awaiting Admin authorization)** |

---

## 5. Request Budget & Execution Safety Audit (`TASK D`)

### 5.1 Strict 14-POST Request Budget Validation

| Operation Category | Operations | HTTP Method & Endpoint | Max Count |
|---|---|---|---:|
| **Document Embeddings** | `document-L1`, `document-L2` | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent` | **`2`** |
| **Query Embeddings** | `query-LQ1` … `query-LQ6` | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent` | **`6`** |
| **Chat Generation** | `chat-LQ1` … `chat-LQ6` | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent` | **`6`** |
| **Metadata GET / `countTokens` / Retries / Redirects / Key Rotation / Backup Failover** | Hard-blocked by `LiveSafety` & `SinglePostWire` | Forbidden (`0`) | **`0`** |
| **Total External Requests** | `01` … `14` | One-shot TLS 1.2/1.3 POST (`SinglePostWire`) | **`14`** |

### 5.2 Chronological Pacing & Schedule Verification Against Project A (`5 RPM`) & Project B (`100 RPM`)

`LiveRagRunner.run()` ([`LiveRagRunner.java:114–142`](file:///d:/miniproject/fpt/test/rag/live/LiveRagRunner.java#L114-L142)) and `LiveSafety.Ledger.awaitModel()` ([`LiveSafety.java:97–101`](file:///d:/miniproject/fpt/test/rag/live/LiveSafety.java#L97-L101)) enforce `spacingMs = 65000` (`65 seconds`) between consecutive requests to the **same model** (`last.getOrDefault(model, 0L) + 65000 - System.currentTimeMillis()`).

Crucially, **all six Chat requests are NEVER sent within a single minute**. They are interleaved with query embeddings and paced `>= 65 seconds` apart:

| Ordinal | Operation ID | Target Model | Approx. Elapsed Time | Same-Model Wait Before Send | Rolling 60s RPM Used vs. Project Limit | Rolling 60s Max TPM Used vs. Project Limit |
|---:|---|---|---:|---:|---|---|
| `01` | `document-L1` | `gemini-embedding-001` | `t = 0s` | `0s` (first embedding call) | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `02` | `document-L2` | `gemini-embedding-001` | `t = 65s` | **`65s`** after `01` | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `03` | `query-LQ1` | `gemini-embedding-001` | `t = 130s` | **`65s`** after `02` | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `04` | `chat-LQ1` | `gemini-3.8-flash` | `t = 131s` | `0s` (first chat call) | **`1 / 5` RPM (Project A — 20% of limit)** | **`<= 17,648 / 250,000` TPM (<7% of limit)** |
| `05` | `query-LQ2` | `gemini-embedding-001` | `t = 195s` | **`65s`** after `03` | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `06` | `chat-LQ2` | `gemini-3.8-flash` | `t = 196s` | **`65s`** after `04` | **`1 / 5` RPM (Project A)** | **`<= 17,648 / 250,000` TPM** |
| `07` | `query-LQ3` | `gemini-embedding-001` | `t = 260s` | **`65s`** after `05` | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `08` | `chat-LQ3` | `gemini-3.8-flash` | `t = 261s` | **`65s`** after `06` | **`1 / 5` RPM (Project A)** | **`<= 17,648 / 250,000` TPM** |
| `09` | `query-LQ4` | `gemini-embedding-001` | `t = 325s` | **`65s`** after `07` | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `10` | `chat-LQ4` | `gemini-3.8-flash` | `t = 326s` | **`65s`** after `08` | **`1 / 5` RPM (Project A)** | **`<= 17,648 / 250,000` TPM** |
| `11` | `query-LQ5` | `gemini-embedding-001` | `t = 390s` | **`65s`** after `09` | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `12` | `chat-LQ5` | `gemini-3.8-flash` | `t = 391s` | **`65s`** after `10` | **`1 / 5` RPM (Project A)** | **`<= 17,648 / 250,000` TPM** |
| `13` | `query-LQ6` | `gemini-embedding-001` | `t = 455s` | **`65s`** after `11` | `1 / 100` RPM (Project B) | `<= 2,048 / 30,000` TPM |
| `14` | `chat-LQ6` | `gemini-3.8-flash` | `t = 456s` | **`65s`** after `12` | **`1 / 5` RPM (Project A)** | **`<= 17,648 / 250,000` TPM** |

- **Total Run Duration:** `~460 seconds` (`~7 minutes 40 seconds`), safely inside the `30-minute` (`1,800 seconds`) approval window.
- **Engine Deadline Protection:** `LiveRagRunner.java:124` calls `ledger.awaitModel("chat")` **before** entering `ConsultationEngine.chat(...)`, so the 65-second quota pacing wait never consumes the Engine's 40-second turn deadline or the 10-second `TURN_LATENCY_TARGET`.

### 5.3 Execution Safety Audit Summary

| Safety Invariant | Verification Result |
|---|---|
| **Pre-Send Journal (`01..14-<op>.reserved.json`)** | **PASS** — Written with `CREATE_NEW` + `FileChannel.force(true)` before socket connect (`LiveSafety.java:148`). |
| **Exclusive Persistent Locks** | **PASS** — `.build/phase24-orchestration/phase24-e2e-20261011-v1.lock` (`False`) and `.build/phase24-live/phase24-e2e-20261011-v1.request.lock` (`False`) are unconsumed and never deleted once created. |
| **First-Failure Immediate Halt** | **PASS** — Any HTTP non-200, timeout, malformed JSON, `finishReason != "STOP"`, wrong source selection, or latency violation throws `LiveSafety.Halt extends Error`, sets `failed = true`, and aborts all remaining slots (`87/87` safety tests PASS). |
| **Historical Evidence Preservation** | **PASS** — `97 / 97` files in `.build/phase242-preserve-e5fd204e3c94450abccf7a10bc616e7f.json` verified identical by SHA-256 and UTC ticks. |
| **Isolated PostgreSQL & Zero Supabase** | **PASS** — `127.0.0.1 / fpt_rag_test` only (`4/4` isolation tests + `7/7` DB assertions PASS; `0` Supabase connections). |
| **Production RAG Disabled** | **PASS** — `productionRagEnabled = false` and `RAG_ENABLED = false` required at startup. |

---

## 6. Final GO / NO-GO Matrix (`TASK F`)

Per Task F, any gate that is not yet `PASS` / `VERIFIED` / `APPROVED` keeps the overall status at **NO-GO**:

| Gate | Expected | Current Actual Status | Evidence / Reason |
|---|---|---|---|
| **Correct Git branch** | `PASS` | **`PASS`** | Branch `feature/rag-integration` (`HEAD: 48b700f`). |
| **Primary model compatibility** | `PASS` | **`PASS`** | `gemini-3.8-flash` (`thinkingLevel: "medium"`, `maxOutputTokens: 1200`, `responseJsonSchema`, no `temperature`/`topP`); `87/87` safety tests PASS. |
| **Embedding compatibility** | `PASS` | **`PASS`** | `gemini-embedding-001` (`outputDimensionality: 768`, L2 normalized, `RETRIEVAL_DOCUMENT` / `RETRIEVAL_QUERY`); `29/29` embedding tests PASS. |
| **API key project mapping** | `VERIFIED` | **`UNVERIFIED` (`NEEDS ADMIN CONFIRMATION`)** | Keys are `PRESENT` in Admin's PowerShell 7 session (`MISSING` in Antigravity process); Admin must confirm in `https://aistudio.google.com/api-keys` that `RAG_CHAT_TEST_API_KEY` belongs to **Project A ("Default Gemini Project")** and `RAG_EMBEDDING_API_KEY` belongs to **Project B ("Gemini Project")**. |
| **Free Tier** | `VERIFIED` | **`VERIFIED IN SCREENSHOTS` (`PENDING KEY MAPPING`)** | Both Project A and Project B display `Free tier` (`$0.00`) in Admin screenshots; becomes fully `VERIFIED` once key-to-project mapping is confirmed. |
| **Current available quota** | `VERIFIED` | **`UNVERIFIED` (`NEEDS ADMIN CONFIRMATION`)** | Project limits exceed requirements (`Project A: 5 RPM / 250K TPM / 20 RPD`; `Project B: 100 RPM / 30K TPM / 1,000 RPD`), and 28-day peaks were low (`6/20` RPD and `2/1,000` RPD), but Admin must confirm **today's remaining RPD** since `14:00 ICT` (`chatRpdRemaining >= 6`, `embeddingRpdRemaining >= 8`). |
| **Corpus L1/L2 approval** | `APPROVED` | **`PENDING` (`approvedForLive = false`)** | `L1` and `L2` are audited and ready (`1` chunk each, synthetic `admin_reviewed`), awaiting explicit Admin approval to set `approvedForLive = true`. |
| **Isolated PostgreSQL** | `PASS` | **`PASS`** | Loopback `127.0.0.1 / fpt_rag_test` Docker container with verified ID, label, port, and 32-hex nonce (`7/7` DB assertions PASS). |
| **Maximum 14 requests** | `PASS` | **`PASS`** | `2` doc + `6` query + `6` chat = `14` POST max; `0` GET / `countTokens` / retry / redirect; `65s` same-model spacing (`<= 1 RPM`). |
| **Backup disabled during live test** | `PASS` | **`PASS`** | `gemini-3.5-flash-lite` backup is locked off (`enabledDuring14RequestLiveRun = false`, `extraBudgetRequests = 0`, `fallback = null`). |
| **Production isolation** | `PASS` | **`PASS`** | `0` Supabase connections, `0` production key reads, `RAG_ENABLED = false`, `productionRagEnabled = false`. |
| **Administrator authorization** | `APPROVED` | **`PENDING` (`approved = false`, `executionAuthorized = false`)** | Preserved as `false` until Administrator gives explicit final execution sign-off. |
| **OVERALL DECISION** | **`GO` only if all 12 pass** | **`NO-GO` (`HOLD BEFORE LIVE API CALLS`)** | **8/12 technical gates are `PASS`; 4/12 governance & project-mapping gates await Administrator confirmation below.** |

---

## 7. Gói Phê Duyệt Cuối Cùng Dành Cho Administrator (`TASK G — TIẾNG VIỆT`)

Dưới đây là bản tóm tắt rõ ràng gồm **9 mục** dành cho Administrator để đối chiếu nhanh và cấp quyền thực thi:

### 7.1 Tóm tắt 9 câu hỏi cốt lõi cho Administrator

1. **Project Google AI Studio nào dùng cho Chat (`gemini-3.8-flash`)?**
   - Sử dụng **Project A — `Default Gemini Project`** (project có `Gemini 3.8 Flash` trên gói **Free Tier** với giới hạn `5 RPM`, `250,000 TPM`, `20 RPD`).
2. **Project Google AI Studio nào dùng cho Embedding (`gemini-embedding-001`)?**
   - Sử dụng **Project B — `Gemini Project`** (project có `Gemini Embedding 1` / `gemini-embedding-001` trên gói **Free Tier** với giới hạn `100 RPM`, `30,000 TPM`, `1,000 RPD`).
3. **Hai API key test (`RAG_CHAT_TEST_API_KEY` và `RAG_EMBEDDING_API_KEY`) đã được ánh xạ đúng project chưa?**
   - **CẦN ADMIN XÁC NHẬN (`NEEDS ADMIN CONFIRMATION`):** Hai biến đã `PRESENT` trong cửa sổ PowerShell 7 của Admin (và `MISSING` trong process Antigravity độc lập). Vì chuỗi key không chứa tên project, Admin vui lòng kiểm tra 4 ký tự cuối của mỗi key trên trang `https://aistudio.google.com/api-keys` (dùng đoạn lệnh PowerShell ở Mục 2.3) để xác nhận:
     - `RAG_CHAT_TEST_API_KEY` thuộc **Default Gemini Project (Project A)** và ở trạng thái hoạt động (không bị gắn nhãn `Blocked` hay `Unrestricted`).
     - `RAG_EMBEDDING_API_KEY` thuộc **Gemini Project (Project B)** và ở trạng thái hoạt động.
4. **Gói Free Tier (`$0.00`) đã được xác nhận chưa?**
   - **ĐÃ XÁC NHẬN TRÊN ẢNH CHỤP (`VERIFIED IN SCREENSHOTS`):** Cả **Project A** và **Project B** đều hiển thị **Free Tier** (`$0.00` chi phí). Khi Admin xác nhận 2 key thuộc đúng 2 project này và không bật billing trả phí (`paidRequestsAllowed = false`), điều kiện Free Tier đạt 100%.
5. **Quota hiện tại có đủ cho phiên chạy 14 request không?**
   - **Giới hạn cấu hình (`RPM` / `TPM` / `RPD` Limit): ĐỦ DƯ DẢ (`PASS`).**
     - Runner giãn cách `65 giây` giữa các request cùng model nên chỉ dùng tối đa **`1 RPM`** (so với giới hạn `5 RPM` của Chat và `100 RPM` của Embedding) và tối đa **`<= 17,648 TPM`** (so với `250,000 TPM` của Chat).
   - **Quota ngày còn lại hôm nay (`RPD Remaining`): CẦN ADMIN XÁC NHẬN (`UNVERIFIED`).**
     - Ảnh chụp hiển thị **mức dùng đỉnh trong 28 ngày (`28-day peak`)** là `6 / 20 RPD` cho `gemini-3.8-flash` (Project A) và `2 / 1,000 RPD` cho `gemini-embedding-001` (Project B). Dù ngay tại mức đỉnh 28 ngày thì số RPD còn lại (`20 - 6 = 14 >= 6` và `1,000 - 2 = 998 >= 8`) vẫn đủ, Admin vui lòng xác nhận **tính từ 14:00 giờ Việt Nam hôm nay** (giờ reset RPD hàng ngày — `00:00 US Pacific Time`), Project A còn ít nhất **`6` RPD** cho `gemini-3.8-flash` và Project B còn ít nhất **`8` RPD** cho `gemini-embedding-001`.
6. **Hai tài liệu `L1` và `L2` có thể phê duyệt được không?**
   - **CÓ — SẴN SÀNG ĐỂ ADMIN PHÊ DUYỆT:**
     - `L1` (*"Anh/chị kiểm tra kết nối dây mạng an toàn trước khi liên hệ hỗ trợ."*) và `L2` (*"Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế."*) đều là dữ liệu kiểm thử tổng hợp (`1` chunk/tài liệu), gắn nhãn `admin_reviewed` (không gắn nhãn chính sách chính thức `official` của FPT), không xung đột giá/gói cước, và chỉ nạp vào PostgreSQL Docker cô lập (`127.0.0.1 / fpt_rag_test`).
7. **Runner có giữ đúng giới hạn tối đa 14 POST request không?**
   - **ĐÃ XÁC MINH (`PASS`):** Tối đa đúng **14 POST** (`2` document embeddings + `6` query embeddings + `6` chat generation); `GET = 0`, `countTokens = 0`, `retry = 0`, `redirect = 0`, `backupModel = OFF`. Dừng ngay lập tức tại lỗi đầu tiên.
8. **Hệ thống đã sẵn sàng thực thi Live ngay lập tức chưa?**
   - **Kỹ thuật & An toàn Offline: ĐÃ SẴN SÀNG (`OFFLINE READY`).**
   - **Trạng thái thực thi Live hiện tại: `NO-GO`** cho đến khi Admin phản hồi xác nhận Mục 7.3 bên dưới.
9. **Những điều kiện nào còn cần Admin xác nhận thủ công?**
   - Chỉ còn **4 xác nhận thủ công** từ Admin:
     1. Xác nhận ánh xạ Key ↔ Project (`RAG_CHAT_TEST_API_KEY` → Project A; `RAG_EMBEDDING_API_KEY` → Project B).
     2. Xác nhận RPD còn lại hôm nay (`chatRpdRemaining >= 6` trên Project A; `embeddingRpdRemaining >= 8` trên Project B; Free Tier `$0.00`).
     3. Phê duyệt `L1` và `L2` (`approvedForLive = true`) cho DB test cô lập.
     4. Cấp quyền thực thi (`approved = true`, `executionAuthorized = true`) để cập nhật file plan JSON và chạy lệnh trong cửa sổ PowerShell 7 của Admin.

### 7.2 Phân biệt rõ: Sự thật đã xác minh vs. Bằng chứng ảnh chụp vs. Điều kiện chờ xác nhận

| Phân loại | Nội dung chi tiết |
|---|---|
| **1. Sự thật kỹ thuật đã xác minh Offline (`Verified Facts`)** | - Nhánh `feature/rag-integration` (`HEAD: 48b700f`), `0` request Gemini thật trong Phase 2.4.1–2.4.7, `0` kết nối Supabase production.<br>- `87/87` safety tests, `4/4` entry tests, `4/4` isolation tests, `7/7` DB assertions, `6/6` kịch bản `LQ1`–`LQ6` mock E2E (`14` mock requests) đều `PASS`.<br>- `97/97` file khóa và evidence cũ giữ nguyên SHA-256 + timestamp.<br>- Giãn cách `65 giây` cùng model đảm bảo tốc độ gửi thực tế `<= 1 RPM` và tổng thời gian chạy `~7 phút 40 giây`. |
| **2. Bằng chứng từ ảnh chụp AI Studio (`Screenshot-Based Evidence`)** | - **Project A (`Default Gemini Project`)** đang ở gói **Free Tier**, có model `Gemini 3.8 Flash` với giới hạn `5 RPM`, `250,000 TPM`, `20 RPD` (mức dùng đỉnh 28 ngày: `2/5 RPM`, `1.9K/250K TPM`, `6/20 RPD`).<br>- **Project B (`Gemini Project`)** đang ở gói **Free Tier**, có model `Gemini Embedding 1` (`gemini-embedding-001`) với giới hạn `100 RPM`, `30,000 TPM`, `1,000 RPD` (mức dùng đỉnh 28 ngày: `1/100 RPM`, `8/30K TPM`, `2/1,000 RPD`) và `Gemini 3.1 Flash-Lite` (`15 RPM`, `250,000 TPM`, `500 RPD`). |
| **3. Giả định KHÔNG tự suy diễn / Cần Admin xác nhận (`Pending Manual Confirmations`)** | - Không tự suy diễn key nào trong PowerShell thuộc Project A hay Project B nếu chưa đối chiếu 4 ký tự cuối trên `https://aistudio.google.com/api-keys`.<br>- Không tự coi số liệu đỉnh 28 ngày (`6/20 RPD` và `2/1,000 RPD`) là số RPD còn lại tức thời của ngày hôm nay nếu chưa có xác nhận của Admin.<br>- Không tự bật `approvedForLive`, `approved` hoặc `executionAuthorized` khi chưa có lệnh duyệt rõ ràng. |

---

## 8. Exact Next Steps for the Administrator

### Bước 1 — Kiểm tra nhanh 4 ký tự cuối của 2 API Key trong cửa sổ PowerShell 7 hiện có

Chạy đoạn lệnh sau trong cửa sổ PowerShell 7 nơi Admin đã nạp 2 key (lệnh chỉ hiển thị `PRESENT` và 4 ký tự cuối):

```powershell
[pscustomobject]@{
    EmbeddingKey_Status = if ($env:RAG_EMBEDDING_API_KEY) { 'PRESENT' } else { 'MISSING' }
    EmbeddingKey_Last4  = if ($env:RAG_EMBEDDING_API_KEY -and $env:RAG_EMBEDDING_API_KEY.Length -ge 8) { '...' + $env:RAG_EMBEDDING_API_KEY.Substring($env:RAG_EMBEDDING_API_KEY.Length - 4) } else { 'N/A' }
    ChatKey_Status      = if ($env:RAG_CHAT_TEST_API_KEY) { 'PRESENT' } else { 'MISSING' }
    ChatKey_Last4       = if ($env:RAG_CHAT_TEST_API_KEY -and $env:RAG_CHAT_TEST_API_KEY.Length -ge 8) { '...' + $env:RAG_CHAT_TEST_API_KEY.Substring($env:RAG_CHAT_TEST_API_KEY.Length - 4) } else { 'N/A' }
} | Format-Table -AutoSize
```

Đối chiếu 4 ký tự cuối trên `https://aistudio.google.com/api-keys`:
- `RAG_CHAT_TEST_API_KEY` thuộc **Default Gemini Project (Project A)**.
- `RAG_EMBEDDING_API_KEY` thuộc **Gemini Project (Project B)**.

### Bước 2 — Phản hồi xác nhận phê duyệt (Copy & Điền ngắn gọn)

Admin phản hồi xác nhận 4 nội dung sau:

```markdown
XÁC NHẬN PHÊ DUYỆT PHASE 2.4.7 LIVE VALIDATION:
1. Key Mapping: Đã xác nhận `RAG_CHAT_TEST_API_KEY` thuộc Default Gemini Project (Project A - Free Tier) và `RAG_EMBEDDING_API_KEY` thuộc Gemini Project (Project B - Free Tier), không phải key production.
2. Quota hôm nay (Free Tier, max paid cost = $0.00):
   - `gemini-embedding-001` (Project B): RPD còn lại = ____ (>= 8), RPM còn lại = ____ (>= 1), TPM còn lại = ____ (>= 2048).
   - `gemini-3.8-flash` (Project A, thinkingLevel=medium): RPD còn lại = ____ (>= 6), RPM còn lại = ____ (>= 1), TPM còn lại = ____ (>= 17648).
3. Corpus: Phê duyệt `L1` và `L2` (`approvedForLive = true`) chỉ cho PostgreSQL Docker test cô lập (`127.0.0.1 / fpt_rag_test`).
4. Execution Authorization: Phê duyệt bật `approved = true`, `executionAuthorized = true` cho kế hoạch 14 request (`phase24-e2e-20261011-v1`, Backup Model tắt).
```

### Bước 3 — Thực thi từ cửa sổ PowerShell 7 của Admin (Chỉ sau khi Bước 2 được duyệt và file Plan JSON được cập nhật)

Sau khi Admin gửi xác nhận ở Bước 2 và Antigravity cập nhật các trường phê duyệt kèm cửa sổ 30 phút (`verifiedAtEpochMs` / `expiresAtEpochMs`) vào [`test/rag/fixtures/phase24-live-plan.json`](file:///d:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json), Admin chạy trực tiếp trong cửa sổ PowerShell 7 đã có sẵn 2 key:

```powershell
Set-Location -LiteralPath 'D:\miniproject\fpt'
$env:RAG_ENVIRONMENT = 'development'
$env:RAG_ENABLED = 'false'

# 1. Kiểm tra preflight offline (0 network calls, 0 DB calls):
java '-Dfile.encoding=UTF-8' -cp ".build/phase241-test-classes;.build/web/WEB-INF/classes;lib/runtime/*;lib/compile-only/*" rag.live.LiveRagRunner --preflight "D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json"

# 2. Thực thi phiên Live RAG Validation duy nhất (tối đa 14 POST, tự động tạo & dừng container PostgreSQL cô lập):
pwsh -NoProfile -File 'D:\miniproject\fpt\test\run-rag-live.ps1' -ApprovedLivePlan -PlanPath 'D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json'
```
