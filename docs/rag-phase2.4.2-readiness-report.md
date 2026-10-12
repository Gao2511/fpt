# Phase 2.4.2 — Resolve LQ4 & readiness

## 1. Executive Summary

2026-10-11; `D:/miniproject/fpt`, branch `feature/rag-integration`, HEAD `48b700f`.

**MOCK READY – LIVE APPROVAL PENDING.** Cùng corpus L1/L2 và kỳ vọng LQ1–LQ6: **6/6 PASS** với PostgreSQL thật cô lập, embeddings/Chat giả lập. Tổng **2 document + 6 query + 6 Chat = 14 mock requests**. Gemini thật trong phase này: **0**; live chất lượng/latency/token/cost: **NOT TESTED**.

`approved=false`, `executionAuthorized=false`, `productionRagEnabled=false`, `.env.example: RAG_ENABLED=false`. Không đọc key production, dùng Supabase, sửa credentials, commit/push/deploy. Không thay gói/giá/ưu đãi/Mesh/CMS/đăng ký. Không thay migration, SQL ranking, thresholds, corpus hoặc expectedSelectedDocuments.

## 2. LQ4 Root Cause

Pipeline và bằng chứng:

1. `src/java/rag/RetrievalQuery.java`: LQ3 nêu game; LQ4 là follow-up. Planner chỉ lấy domain từ tối đa hai lượt CUSTOMER, bỏ assistant prose; rewrite thực tế:

   ```text
   Vậy cách đó có đảm bảo ping thấp không?
   game ket noi day LAN
   ```

   Subject `game` có căn cứ từ LQ3. Đây không phải câu hỏi mất ngữ cảnh. Expansion LAN phục vụ retrieval, nhưng cũng chứa các từ chung về kết nối.
2. `test/rag/live/LiveRunnerDatabaseTest.java`: mock vectors document L1 theo trục 0, L2 theo trục 1; query game/ping theo trục 1, đều 768 chiều được chuẩn hóa. Query không bị mất subject trước embedding. Đây là kiểm chứng cơ học, **không** phải phép đo semantic Gemini.
3. `src/java/dao/KnowledgeChunkDAO.java`: FTS dùng các term OR. Điều kiện candidate SQL là `lexical>0 OR semantic>=0.72`. LQ4 có `ket/noi/day/LAN` nên L1 đủ lexical, dù cosine với query mock bằng 0. L2 vừa phù hợp semantic vừa lexical.
4. Score SQL:

   ```sql
   (CASE WHEN lexical>0 THEN 0.4+LEAST(lexical,1)*0.2 ELSE 0 END)
     + GREATEST(semantic,0)*0.4
   ORDER BY score DESC,document_id,id
   ```

   Kết quả PostgreSQL đo lại: **L2=1.0, L1=0.5**, thứ tự `[L2,L1]`. SQL xếp đúng nguồn game trước nguồn kết nối; không phải ranking bị đảo. Metadata của cả hai là published/current/admin_reviewed, hai policy_key khác nhau; không có metadata lỗi hay nguồn hết hạn. DAO vẫn kiểm tra hash/version/model/dimensions/approval/dates/supersession.
5. Lỗi chính: `KnowledgeRetrievalService.lookup` trước đây gọi `KnowledgeSourceSelection.select(candidates)` **không truyền query**. Selector queryless coi mọi `subjects.size()>1` là `AMBIGUOUS_SUBJECT`. Engine hỏi làm rõ, không gọi Chat; harness đối chiếu selected=[] với expected=[L2] rồi dừng.
6. Harness cũng tái tạo validator context từ toàn bộ raw candidates qua selector queryless, nên phải đồng bộ với bước chọn context của runtime. Không được đưa expected document IDs vào context để ép model.

Bằng chứng trước sửa vẫn giữ nguyên:
`D:/miniproject/fpt/.build/phase241-db-mocks/phase24-offline-db-1df0a924-b83f-456f-91da-8b5590ef68df.evidence/LQ4.sources.json`:
retrieved=[L2,L1], selected=[], validated=[]. Trước: LQ1–3 PASS, LQ4 FAIL, LQ5–6 NOT TESTED trong sequence; dừng **9** mock requests.

Bằng chứng sau sửa:
`D:/miniproject/fpt/.build/phase241-db-mocks/phase24-offline-db-ff395e17-39dd-43a8-b2fa-85f4c1617b01.evidence/LQ4.sources.json` chứa rewrite, title, policyKey, authority, SQL score; raw vẫn `[L2,L1]`, validated/selected chỉ `[L2]`.

Đánh giá fixture: kỳ vọng LQ4 chọn L2 phù hợp nội dung gaming và customer follow-up. Không chỉnh corpus để loại lexical overlap. Nhiều candidates là hành vi bình thường của hybrid search; selector thiếu query intent mới là nguyên nhân chặn.

## 3. Changes Implemented

- Thêm overload `KnowledgeSourceSelection.select(candidates,query)`; giữ API queryless và hành vi conservative cũ cho các caller không có query.
- Tái sử dụng classifier `RetrievalQuery.topics`, không thêm LQ4/L1/L2/phase241 ID vào logic runtime. Query single-domain và title đã review xác định được duy nhất một policy subject mới được chọn. Internet connectivity chung có thể khớp title WiFi; explicit game giữ game. Không dùng score gần nhau/cao hơn để giải quyết subject.
- Title không xác định hoặc chứa nhiều domain, nhiều policy cùng khớp, hay query không rõ/multi-domain vẫn trả ambiguity để Engine hỏi làm rõ. Không trộn tài liệu.
- Kiểm tra approval/content/version/authority và conflicts **trước** query filtering. Không che source conflict của một subject bằng cách bỏ nó vì câu hỏi thuộc subject khác. Revalidation trước customer render giữ nguyên.
- Service truyền rewritten query vào selector. Harness giữ raw candidates cho recall/precision, nhưng validator context dùng cùng query-aware selection như runtime. Không đổi expected answers/metrics.
- Ghi thêm ranked candidate score/title/authority vào evidence nội bộ test. Không thay public JSON hay hiển thị metadata cho khách.
- DB mock harness nay yêu cầu cả sáu case hoàn tất, kiểm chứng journal có trước mỗi send và lock không thể chạy lại. Các fault tests first-error/no-retry vẫn giữ nguyên.

## 4. Files Modified

Đường dẫn đầy đủ; đây là các thay đổi **Phase 2.4.2**, không phải toàn bộ working tree từ các phase trước:

| File | Mục đích |
| --- | --- |
| `D:/miniproject/fpt/src/java/rag/KnowledgeSourceSelection.java` | Query-aware cross-subject selection sau conflict validation |
| `D:/miniproject/fpt/src/java/rag/RetrievalQuery.java` | Chia sẻ classifier trong package; null query an toàn |
| `D:/miniproject/fpt/src/java/rag/KnowledgeRetrievalService.java` | Truyền query cho selector |
| `D:/miniproject/fpt/test/rag/RagSubjectSelectionTest.java` | Mới: 22 behavior regressions, injected DAO/provider |
| `D:/miniproject/fpt/test/run-rag-phase2-offline.ps1` | Compile/run suite chủ đề mới |
| `D:/miniproject/fpt/test/rag/live/LiveRagRunner.java` | Đồng bộ validated context, giữ raw metrics, thêm ranking evidence |
| `D:/miniproject/fpt/test/rag/live/LiveRunnerDatabaseTest.java` | Full sequence, journal-before-send, duplicate lock, source/budget/DB guards |
| `D:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json` | Ghi mock result, bỏ blocker LQ4; giữ mọi approval false/model null |
| `D:/miniproject/fpt/docs/rag-phase2.4-e2e-validation.md` | Thêm note hiện tại, giữ lịch sử |
| `D:/miniproject/fpt/docs/rag-phase2.4-live-plan-review.md` | Thêm note hiện tại, giữ lịch sử |
| `D:/miniproject/fpt/docs/rag-phase2.4.2-readiness-report.md` | Báo cáo mới này |

## 5. Test Results

Các bộ dưới đây **đã chạy**, exit code 0. Không có regression failure.

| Bộ | Kết quả | Phạm vi |
| --- | --- | --- |
| Safety | **PASS 63/63** | Faults, timeout/connection/429/503, redirect refusal, interruption, concurrency, budgets, schema/foreign sources, no retry/fallback |
| Entry | **PASS 4/4**, syntax 5/5 | Không switch/không approval/production/enabled flag chặn trước API/DB |
| Isolation | **PASS 4/4** | Container ID/label/loopback port mocks |
| Actual PostgreSQL mock-run safety | **PASS 7/7 categories** | Wrong nonce; pre-send counter+journal; complete budget; no post-completion send; LQ4 raw/selected evidence; duplicate lock; reject populated DB reuse |
| Existing RAG regression | **PASS 75/75** | Behavior 35 + corpus 13 + quality 27 |
| New subject regressions | **PASS 22/22** | L1/L2, customer follow-up, mixed/ambiguous, missing/invalid metadata, near/reversed score, empty, unknown/multi-domain titles, authority/conflicts, actual service/Engine clarification |
| Chatbot regression | **PASS 86/86** | Consultation 26 + adaptive 21 + fee 17 + registration 9 + provider loopback 7 + frontend 6 |
| Phase24 E2E regression | **PASS 41/41** | PostgreSQL thật, mock providers; expiry/injection/conflicts/unknown/business/registration/failures |
| PostgreSQL regression | **PASS 24/24** | Migrations/indexing/currentness/RLS/authority/concurrency; mock embeddings |
| Exact live-plan mock cases | **PASS 6/6** | Same corpus/queries/expected sources; 14 mock POST slots |
| Real Gemini embedding/Chat | **NOT TESTED** | 0 real requests in this phase |

Evidence mới (đường dẫn dưới `D:/miniproject/fpt/`):

- `.build/phase241-mocks-9e2f106b-7f1e-4287-b414-1fa532409b19/summary.json`
- `.build/phase241-entry-f066eb0cae254957942a1d146b331b3a.json`
- `.build/phase241-isolation-6df6390240284bb59a6612b9aeb5b943.json`
- `.build/phase241-db-mocks/phase24-offline-db-ff395e17-39dd-43a8-b2fa-85f4c1617b01.evidence/offline-database-validation.json`
- `.build/phase24-e2e-909d73031e454e54983aac067ff7dc34.json`
- `.build/phase242-rag-regression-final.log`

97 existing files trong auth/embedding probes và phase241 DB mock evidence đã được snapshot hash + LastWriteTime trước sửa. **97/97 unchanged**:
`.build/phase242-preservation-8b033d71386d4c379a61744326e69ebd.json`.
Không reset lock hoặc ghi đè result cũ; GUID evidence/CREATE_NEW tiếp tục áp dụng. Incident legacy artifact overwrite được mô tả trong Phase2.4 trước vẫn là lịch sử; không tuyên bố khôi phục artifact đã mất của incident đó.

## 6. LQ1–LQ6 Mock Results

| Case / câu hỏi nguyên bản | Raw ranked sources | Validated context | Final selected | Kết quả |
| --- | --- | --- | --- | --- |
| LQ1: Mạng nhà mình chậm quá. | L1 | L1 | L1 | **PASS**: literal connectivity guidance |
| LQ2: Shop ơi mạng nhà tui yếu quá. | L1 | L1 | L1 | **PASS**: same source for slang |
| LQ3: Hướng dẫn chơi game Internet sao cho ổn? | L2 | L2 | L2 | **PASS**: gaming guidance, no ping guarantee |
| LQ4: Vậy cách đó có đảm bảo ping thấp không? | L2, L1 | L2 | L2 | **PASS**: customer game context retained |
| LQ5: WiFi 6 có tốt hơn WiFi 5 không? | L1 | L1 | none | **PASS**: mock model abstains; connectivity excerpt cannot substantiate standards comparison |
| LQ6: Có cung cấp internet ở nước ngoài không? | none | none | none | **PASS**: no invented coverage |

Measured **mock** positive recall 4/4; raw mean precision@3 **0.875** (LQ4=0.5), correct final selections 6/6; unknown abstention 2/2; unsupported claims/public metadata leaks/business overrides 0. LQ4 passes by correct context selection, not by artificially improving raw precision.

Turn latency excluding quota waits: 78.40 / 35.96 / 36.05 / 40.42 / 22.59 / 23.02 ms, mean about 39.40 ms. These are local SQL/mock latencies, **not** Gemini live latency. Real token usage unavailable; synthetic cost field 0 is not a live billing measurement.

All 14 immutable reservations exist before mock wire invocation. Per-group counters 2/6/6; GET/countTokens/retry/redirect 0. Concurrent/duplicate/interruption first-error gates still PASS in Safety. Sequence no longer stops at LQ4; no skip/reorder/extra request allowance was introduced.

## 7. Gemini Configuration Verification

| Item | Evidence/status |
| --- | --- |
| Embedding ID | **PASS** configured `gemini-embedding-001`; [Google model specification](https://ai.google.dev/gemini-api/docs/models/gemini-embedding-001) documents 2,048 input tokens and recommended 768 dimensions |
| REST payload | **PASS offline** root `model`, `content.parts[].text`, `taskType`, integer `outputDimensionality:768`; document title only for RETRIEVAL_DOCUMENT. [REST reference](https://ai.google.dev/api/embeddings) still lists root fields, now deprecated in favor of embedContentConfig. No unapproved switch back to the previously failed nested payload |
| Tasks/vector handling | **PASS offline** RETRIEVAL_DOCUMENT / RETRIEVAL_QUERY, finite nonzero 768, normalize both with VectorValues; SQL exact model/dimension/current revision; no reused 3072 or old mock DB vectors |
| L2 normalization | [Google embedding guide](https://ai.google.dev/gemini-api/docs/embeddings) requires manual normalization for non-3072 output of this model; implementation matches |
| Prior live evidence | Phase2.3 corrected single POST result is historical: 768 valid, 858.7ms; untouched. No new account/model-access probe |
| Exact Chat ID | **BLOCKED** real plan `chatModel=null`; no production settings DB read. `gemini-2.5-flash` exists ONLY in approvedFixture mock clone and is not proof of the running chatbot model or permission to replace it |
| Chat REST/schema | `GeminiProvider.buildRequestBody` builds systemInstruction + chronological contents + generationConfig.responseJsonSchema/responseMimeType/maxOutputTokens. Harness reuses these helpers; actual exact-model support **UNVERIFIED** |
| Thinking | Actual approval thinkingConfig=null, disabledSupported=false: **BLOCKED**. Runner currently accepts only explicit `{thinkingBudget:0}` and verifies zero thoughtsTokenCount when returned. [Google thinking reference](https://ai.google.dev/gemini-api/docs/generate-content/thinking) documents model-specific support; 2.5 Flash can disable, 2.5 Pro cannot. Do not assume another model supports off or auto-select it |
| Output/parser | cap 1200; exactly one candidate, STOP, customerText excludes thought parts, strict ConsultationReply/source/action checks; invalid/truncated ends runner, no repair HTTP. Public JSON publishes only customer fields |
| Input tokens | No countTokens call. Conservative UTF-8 byte +64 bounds: embedding <=2048/request, Chat byte cap 16384 with declared token limit >=16448. This is an offline guard, not measured tokenizer usage. Numeric usageMetadata recorded only if returned; otherwise null |
| Timeouts/errors | SinglePostWire DNS 3s/connect+read 5s/overall socket close 10s; embedding target <=5s, turn target <=10s excluding deliberate spacing. Non-200 sanitized status, no raw body/header/key/vector; first error stops; no redirect/retry |

Runtime `GeminiEmbeddingService` still has initial metadata GET and bounded retries. Runtime Gemini/Engine may repair/fallback. **Do not use those default network paths for the 14-request plan.** Runner injects Embeddings/Chat wrappers and guarded one-shot transport; only pure requestBody/parser helpers are reused. No runtime provider transport executed by mocks.

## 8. Free Tier / Quota Verification

Public Google pages inspected; no authenticated API/quota requests:

- [Pricing](https://ai.google.dev/gemini-api/docs/pricing): public Standard 2.5 Flash Free Tier is listed; that proves only public eligibility of the mock fixture model, not the actual project or current Chat model. Current fetched page does not contain a dedicated gemini-embedding-001 tariff; earlier $0.15/1M is historical and **current exact embedding tariff/eligibility remains UNVERIFIED**. Do not reuse another embedding model's price.
- [Rate limits](https://ai.google.dev/gemini-api/docs/rate-limits): RPM, input TPM, RPD are separate; quotas are project-level, not API-key-level. A new key does not reset quota. Current values must be checked in the correct AI Studio projects. Billing/spend cap is separate from throughput; it cannot replace quota confirmation.

Phase2.3 administrator snapshot (embedding Free Tier 100 RPM/30K TPM/1K RPD) remains historical. Current **remaining** quota for BOTH models is **UNVERIFIED**. Exact Chat model, permission, pricing, model-specific thinking and token limits are **BLOCKED** pending administrator evidence.

Runner prerequisites: embedding RPD remaining >=8, Chat >=6; both RPM remaining >=1, serial spacing >=65s/model; embedding TPM >=2048; Chat TPM >= inputByteCap+64+1200 (**17648** with byte cap16384). TPM is a conservative admission requirement, not measured consumption. Worst admitted token envelopes: 8*2048 embedding input, 6*16448 Chat input, 6*1200 output; no countTokens or thinking requests.

Cost policy remains Free Tier only, maximum approved paid cost **$0**, paid fallback forbidden. Expected future cost $0 is **conditional** on confirmed eligibility/capacity of both exact models in executing test projects. If cannot verify, do not execute; no assertion of a paid-cost estimate for unknown Chat model. Offline phase provider cost **$0** because real requests=0.

## 9. Remaining Risks

- Title-based domain classification is deliberately bounded to existing known domains. Generic/unclassified/multi-domain titles in mixed retrieval ask clarification; more general semantic subject classification needs separate corpus evaluation. A clear domain may still have several distinct policies, which remain ambiguous.
- Mock vectors are synthetic; full real embedding/Chat source selection, natural-language correctness, usage and network latency remain NOT TESTED in Phase2.4.2. Successful HTTP alone will not pass future quality checks.
- LQ5 has superficially related L1 context but no standards evidence. Mock abstention does not prove real Chat abstention; future run must enforce the same acceptance criterion, not accept unrelated excerpts.
- Two live documents cannot prove all authority/supersession cases; keep the 41/24 regressions. L1 is synthetic QA text, not an official FPT policy; both approvedForLive remain false.
- Required zero-thinking configuration may be incompatible with the actual Chat model. Resolve by model-specific review and a separately reviewed plan/runner adjustment if needed; no automatic model replacement.
- Shared-project traffic can consume quota after a manual check. Time-bound approval (<=30 minutes) and strict first-error abort remain necessary; no quota probe or paid fallback.
- `.local/rag-tests` credentials for new containers stay ignored; inspect evidence without exposing them. Old containers/volumes are retained.

## 10. LIVE Readiness Checklist

| Gate | Status |
| --- | --- |
| Proven root cause / general fix | **PASS** |
| LQ1–LQ6 full mock, regressions, safety | **PASS** |
| 2+6+6, persistent pre-send journal, exclusive CREATE_NEW locks | **PASS** |
| Fresh DB identity/nonce/model/dimensions, no production fallback | **PASS** |
| Credentials separation and sanitized evidence | **PASS mock**; live test key presence not inspected |
| Old evidence/locks preserved | **PASS 97/97 baseline files** |
| Public embedding model/dimensions/tasks | **PASS** |
| Exact actual Chat model & REST/thinking compatibility | **BLOCKED** |
| Current exact-model Free Tier/prices/project quotas | **UNVERIFIED** |
| Corpus approval | **BLOCKED**, both false |
| Live approval | **BLOCKED**, approved/executionAuthorized false |
| Real Gemini E2E quality | **NOT TESTED**, zero requests |

Latest mock DB: `127.0.0.1`, database `fpt_rag_test`, dedicated random credentials/nonce, PostgreSQL17.10 + pgvector0.8.2, migrations002/003, newly created container **fpt-rag-live-test-c456ba660770**. Stopped and retained after test; other existing containers/volumes untouched. Additional regression containers **fpt-rag-test-5c3932cff39a** and **fpt-rag-test-23824eec0ae2** likewise stopped/retained. Supabase never used.

## 11. Recommendation

Ready to request model/project/corpus evidence for a later controlled live plan, **not ready to execute live now**. Administrator supplies exact existing Chat ID (no secret), checks model-specific JSON schema/thinking-off support and token limits, confirms current Free Tier/prices and remaining RPM/TPM/RPD of BOTH models in AI Studio, and approves exact L1/L2 test texts. Keys stay exclusively in `RAG_EMBEDDING_API_KEY` and `RAG_CHAT_TEST_API_KEY` of executing test process; never paste into chat or copy from production.

After evidence review, a separate explicit live approval and immutable approved plan are required. Preserve existing locks; if a plan ID has been consumed, use a new separately reviewed ID, never reset it. No auto Phase3 or production enablement. This task ends at **MOCK READY – LIVE APPROVAL PENDING**.

## 12. Commands Executed

From `D:/miniproject/fpt`, PowerShell7 + Java17; Ant path below is local. Scripts use injected mocks or newly isolated databases. No live runner execution switch was used manually; Entry suite exercises only refusing paths.

```powershell
git branch --show-current
git rev-parse --short HEAD
$env:RAG_ENABLED = 'false'
./test/run-rag-phase2-offline.ps1 -AntHome 'D:/Subject FPT/PRJ301/NetBeans-13/netbeans/extide/ant'
./test/test-rag-live-runner.ps1 -AntHome 'D:/Subject FPT/PRJ301/NetBeans-13/netbeans/extide/ant'
./test/test-rag-live-entry.ps1
./test/test-rag-live-isolation.ps1
./test/test-rag-live-database.ps1
./test/run-rag-isolated.ps1 -Phase24
./test/run-rag-isolated.ps1
./test/run-chat-tests.ps1 -AntHome 'D:/Subject FPT/PRJ301/NetBeans-13/netbeans/extide/ant'
```

Also read-only SHA256/LastWriteTime evidence comparison and official public Google documentation review. Suites repeated only after subsequent scoped code changes; all final runs PASS. All prior uncommitted work retained; no commit/push/deploy or production database write.
