# Phase 2.4.1 — Safe Live RAG Runner Readiness

2026-10-11. `D:\miniproject\fpt`, branch `feature/rag-integration`, HEAD `48b700f`; working tree từ các phase trước được giữ nguyên.

**NOT LIVE READY.** Cơ chế an toàn đã triển khai và kiểm thử offline. Còn blocker chất lượng LQ4, exact Chat model/thinking/Free Tier/quota và phê duyệt corpus/live. `approved=false`, `executionAuthorized=false`, `RAG_ENABLED=false`. **Gemini Embedding/Chat thật: 0 requests trong phase này.** Không dùng Supabase, key production, commit/push/deploy.

## 1. Files created / modified

Tất cả đường dẫn dưới đây tương đối với `D:\miniproject\fpt`; các file uncommitted khác từ phase trước không được thay bằng source cũ.

| File | Thay đổi |
| --- | --- |
| `test/rag/live/LiveSafety.java` | Plan/cost/quota/DB gates, immutable ledger, exclusive persistent lock, operation allowlist, counters trước send, first-error halt |
| `test/rag/live/SinglePostWire.java` | Một TLS socket và một HTTP/1.1 POST; bounded DNS/connect/read/overall deadline; không HTTP retry/redirect/proxy fallback |
| `test/rag/live/LiveRagRunner.java` | CLI preflight + harness dùng worker, DAO hybrid search, source policy, ConsultationEngine và public projection hiện có |
| `test/rag/live/LiveRunnerSafetyTest.java` | 63 mock/fault tests, bao gồm hai OS processes và process interruption |
| `test/rag/live/LiveRunnerDatabaseTest.java` | Real isolated JDBC pipeline với fake providers; kiểm chứng first-quality-failure stop, identity nonce và từ chối DB đã có dữ liệu |
| `test/compile-rag-live-runner.ps1` | Compile offline Java 17; chỉ verify libs local, không download |
| `test/rag-live-database.ps1` | Tạo riêng container/credentials/nonce mới; loopback mapping, label/ID checks và migrations 002/003 |
| `test/run-rag-live.ps1` | Entry switch + preflight, frozen read-locked plan snapshot, orchestration lock, scoped test environment, stop container của chính run |
| `test/test-rag-live-runner.ps1` | Compile/run mock safety suite |
| `test/test-rag-live-database.ps1` | Run isolated PG + mock-only provider harness |
| `test/test-rag-live-entry.ps1` | Negative PowerShell approval/environment gates và syntax checks |
| `test/test-rag-live-isolation.ps1` | Fake Docker command tests cho sai ID/label/mapped port và valid isolation; zero real Docker calls |
| `src/java/ai/provider/GeminiProvider.java` | Chỉ mở hai pure helpers thành `protected`: buildRequestBody/customerText. Harness tái sử dụng serialization và loại thought parts. Không đổi logic/provider/model/prompt runtime |
| `test/rag/fixtures/phase24-live-plan.json` | Mark runner implemented, thêm template approval bắt buộc, ghi blocker LQ4; các approval vẫn false, model null |
| `docs/rag-phase2.4-e2e-validation.md` | Thêm kết quả preparation hiện tại; giữ kết quả/history cũ |
| `docs/rag-phase2.4-live-plan-review.md` | Thêm cập nhật implementation/readiness; review cũ vẫn là evidence lịch sử |
| `docs/rag-phase2.4.1-live-runner-readiness.md` | Báo cáo này |

Không đổi migration, giá/gói, ưu đãi, Mesh, Admin UI, customer registration, production config hoặc runtime RAG factory.

## 2. Architecture

`run-rag-live.ps1` → frozen plan/preflight → exclusive orchestration lock → new verified loopback PostgreSQL → Java exclusive request lock/journal → indexing/retrieval/ConsultationEngine → strict test adapters → guarded one-shot TLS transport.

- Code runner nằm trong `test/`, không đóng vào WAR. Java 17 + PowerShell 7, không thêm dependency.
- Embedding payload dùng `GeminiEmbeddingService.requestBody` hiện có, root integer `outputDimensionality:768`; không gọi default adapter verifyAccount/send nên không GET metadata/retry ngầm.
- Document L1/L2 dùng `RETRIEVAL_DOCUMENT` + title; sáu query dùng `RETRIEVAL_QUERY`, không title. Cùng `gemini-embedding-001`, finite/nonzero/768, `VectorValues.normalized` cho cả hai phía. SQL lưu và xác minh model/dimensions/vector_dims.
- Query rewrites được dựng bằng `RetrievalQuery` hiện có từ approved questions và customer-only history, freeze SHA256 trước request đầu. Actual retrieval input phải khớp cache của đúng case. Query POST body khác frozen hash bị chặn.
- Sáu query embeddings là sáu operation cố định; Engine dùng cached vector của case đó, không tạo query embedding bổ sung. Chat có thể ít hơn sáu do server-owned routing/clarification.
- Chat adapter dùng pure helpers của GeminiProvider, schema của Engine, exact model trong approved plan, không fallback model/provider. Validate strict envelope + selected source allowlist **trước** trả raw response về Engine.
- Unknown cases phải có selected=[] và một trong hai câu deferral tiếng Việt cố định đã kiểm thử; không dùng keyword “chưa” để cho phép đoạn bổ sung chứa khẳng định chưa chứng thực. Tại smoke runner, safe synonyms ngoài allowlist cũng bị reject; không đổi prose validation production.
- Test-only `LiveSafety.Halt extends Error` thoát các `catch(Exception)`/repair/fallback runtime; boundary runner ghi sanitized halt. Mock tests đã kiểm chứng malformed/truncated/foreign-source/business-action đều chỉ một Chat POST.
- Source/currentness/render vẫn dùng policy/DAO hiện có. Chỉ literal excerpt đã duyệt xuất hiện trong technical public reply; public JSON vẫn qua `AIChatServlet.customerJson`.
- Business package snapshot là fixture JDBC riêng của test DB, cùng dữ liệu Phase 2.4; không dùng vector/KB làm nguồn giá/quyền lợi và không đọc settings production.

## 3. Request budgets / transport

| Operation | Trần |
| --- | ---: |
| document-L1, document-L2 | 2 |
| query-LQ1 … query-LQ6 | 6 |
| chat-LQ1 … chat-LQ6 | 6 |
| Tổng | 14 |
| GET / countTokens / retry / redirect / listing / connection probe / stream | 0 |

Transport nhận đúng POST, exact host/path/model và operation allowlist. Một failure vẫn tiêu thụ slot đã reserve, kể cả kết nối/timeout xảy ra trước khi HTTP bytes gửi được; đây là conservative attempt accounting. Không dùng số budget còn lại để repair/đổi input/chạy lại.

Counter có nhóm document/query/chat và tổng; mỗi reservation chứa cumulative counters, plan hash, operation, model, task/dim, body hash. CREATE_NEW + `FileChannel.force(true)` hoàn tất trước gọi Wire. Có cả result storage độc quyền trước send. Counter không chỉ ở bộ nhớ: journal trên đĩa và khóa cả plan ngăn process khác gửi thêm/resume.

Fresh TLS connection, xác minh certificate/HTTPS hostname và SNI; một POST write, không thư viện HTTP có retry/redirect tự động. DNS bound 3s, connect/read 5s và overall deadline 10s đóng socket; response <=256KiB và headers bounded. Non-200 chỉ giữ status; không đọc/lưu raw diagnostic body, Location không được follow. HTTP decoder length/chunked/truncation đã mock-test; chưa kiểm chứng socket với Gemini thật.

Spacing ≥65s cùng model, tuần tự, pause trước send. Chat pacing thực hiện trước vào Engine để không làm hết 40s Engine deadline. Transport vẫn tự kiểm tra pacing. Expired approval sau pause từ chối send. Quota waits tách khỏi request latency.

Latency target của plan cũng là gate riêng: embedding <=5s, Chat request <=10s, complete turn (query embedding + Engine/SQL, loại quota wait) <=10s. Nếu vượt, ghi latency và halt, không gọi request tiếp theo dù content/vector hợp lệ. Mock delayed send đã kiểm chứng gate này. Không coi mock latency như latency Gemini thực.

## 4. Locks / evidence

- Orchestration: `.build/phase24-orchestration/<planId>.lock`, FileMode.CreateNew, FileShare.None, giữ nguyên sau kết thúc/failure.
- Request lock: `.build/phase24-live/<planId>.request.lock`, atomic CREATE_NEW. Concurrent OS process chỉ một process claim thành công.
- Evidence: `.build/phase24-live/<planId>.evidence/`. Orphan artifact có prefix plan ID cũng từ chối claim.
- Reservation: `NN-<operation>.reserved.json`, CREATE_NEW, forced trước network; result handle CREATE_NEW trước reservation. Body/vector không ghi vào evidence.
- Unknown state như process crash sau reservation: lock và journal giữ nguyên, result rỗng nếu chưa có response. **Không resume/reset tự động.** Một test đã dùng `Runtime.halt(17)` trong child OS process và kiểm chứng không claim lại được.
- Operation ID đã reserve không thể gửi lại. Duplicate/artifact collision dừng plan; không ghi đè file cũ.
- Final summary chỉ tạo khi đủ quality gates; thất bại lưu halt và sources/operation summaries, không tạo summary PASS giả.
- Plan được snapshot bằng CREATE_NEW và giữ handle không cho ghi trong PowerShell suốt preflight/execution; không đọc lại một bản plan có thể bị thay giữa hai bước.

10/10 Phase 2.3 embedding/auth artifacts đã tồn tại ở đầu phase này giữ nguyên SHA256 **và** modification time. Live request lock thật của Phase 2.4 chưa được tạo. Đây không phục hồi báo cáo mock legacy bị ghi đè ở phase trước; sự cố cũ vẫn được ghi trong docs Phase 2.4.

Preservation report: [JSON](D:/miniproject/fpt/.build/phase241-preservation-06a36f14ecb3415d8833f4b8faab7136.json).

## 5. Database isolation

Chỉ helper tạo container **mới**, image pinned PostgreSQL17/pgvector0.8.2 có sẵn local. Không pull/create cloud service. Host mapping chính xác `127.0.0.1:<random port>`, DB `fpt_rag_test`, role `fpt_rag_user`, credentials RNG riêng.

Trước migration: so Docker ID vừa tạo + label test/plan/nonce + running state + mapped loopback port. Sai bất kỳ điều kiện nào thì không exec/copy migration. Chỉ new container nhận `CREATE EXTENSION vector`, migrations 002/003 và identity marker.

Java dựng URL loopback từ validated numeric port; không nhận JDBC URL tùy ý, không DB_HOST/DBUtils/default factory. Trước business/KB writes: `current_database`, `current_user`, identity nonce và empty documents/chunks phải đúng. Nonce mismatch và populated DB được kiểm thử bằng PostgreSQL thực.

Container tạo trong lượt này: `fpt-rag-live-test-b43a6a053091`, `501fe9deccd8`, `c280af1dab15`, `f54b775c0280` (đều prefix `fpt-rag-live-test-`). Đã stop chính container của từng run, giữ nguyên container/volume/data/private config. Không tác động containers cũ. Initial fixture policy key dùng uppercase bị DAO từ chối, đã sửa riêng harness sang lowercase; failed-run evidence/database được giữ.

## 6. Credentials / environment

Provider keys chỉ đọc từ process: `RAG_EMBEDDING_API_KEY`, `RAG_CHAT_TEST_API_KEY`. Không GEMINI_API_KEY/GOOGLE_API_KEY/CMS key fallback; không tìm key từ process khác hoặc `.local/runtime.json`. Negative child-process test chỉ có fake production-style variables vẫn bị từ chối vì thiếu test key.

Các biến admin tự cấu hình: hai test key names trên, `RAG_ENVIRONMENT=development`, `RAG_ENABLED=false`. Không gửi key qua chat hoặc command arguments.

Helper tạo scoped `RAG_TEST_DB_HOST`, `RAG_TEST_DB_NAME`, `RAG_TEST_DB_PORT`, `RAG_TEST_DB_PASSWORD`, `RAG_TEST_DB_NONCE`, `RAG_TEST_CONTAINER_VERIFIED`; không yêu cầu admin sao chép password production. Private DB env file chỉ trong ignored `.local/rag-tests/<new container>/postgres.env`, CREATE_NEW. Không serialize Docker Config.Env/credentials/context object. Khôi phục process environment trong finally.

Evidence chỉ status/error code cố định, hashes, dimensions/L2, numeric usage nếu provider trả, safe source IDs và validated public text. Không header, API key, raw error text, toàn vector hoặc customer PII. Missing usage được ghi null, không suy đoán số token đã dùng.

## 7. Offline execution results

| Suite thực thi | Kết quả |
| --- | --- |
| Java 17 application/runner compile | PASS |
| LiveRunnerSafetyTest | **63/63 PASS** |
| PowerShell entry gates | **4/4 PASS**, syntax **5/5** |
| Fake Docker isolation guards | **4/4 PASS**, real Docker/JDBC/provider calls=0 trong suite này |
| Real isolated PostgreSQL harness safety | **6/6 assertions PASS**, migrations 002/003 thành công |
| Exact live-corpus quality trên real SQL + mock embeddings/chat | LQ1–LQ3 PASS; **LQ4 FAIL**; LQ5/LQ6 **NOT RUN** vì first-failure stop |
| RAG regression | **75/75 PASS** (35+13+27) |
| Chat regression | **86/86 PASS** (26+21+17+9+7+6), bao gồm HTTP loopback fake và frontend projection |
| Previous evidence hashes/times | **10/10 unchanged** |

Safety cases bao gồm normal 14 operations; HTTP429/503; timeout/connection failure; invalid/zero/nonfinite/3072 vectors; duplicate execution/operation; concurrent OS processes và threads; interruption/unknown state; global/group budgets; missing key/approval/model/quota/Free Tier/thinking; wrong DB/Supabase URL/localhost alias; journal/result collision; unplanned GET/countTokens/endpoint/document/query; invalid Chat schema/truncation/source/action và credential sanitization. Sau fault không có additional mock send; không retry ngầm.

Reports:

- [63 mock safety cases](D:/miniproject/fpt/.build/phase241-mocks-1eaac6dc-25f6-4182-b88c-0eabb02d81bb/summary.json)
- [PowerShell gates](D:/miniproject/fpt/.build/phase241-entry-a9fc17e950734fcebf48f8a8ae277737.json)
- [Isolation mocks](D:/miniproject/fpt/.build/phase241-isolation-7d013716b3534fd4a622b5e039cfa9df.json)
- [Real DB safety + quality blocker](D:/miniproject/fpt/.build/phase241-db-mocks/phase24-offline-db-1df0a924-b83f-456f-91da-8b5590ef68df.evidence/offline-database-validation.json)

Real DB run dừng sau **9 mock sends: 2 document +4 query +3 chat**. LQ4 không gọi Chat; LQ5/LQ6 không được bắt đầu. Next-send probe bị từ chối và giữ count=9. Đây là PASS cho fail-closed, **không phải PASS chất lượng RAG**. Không cộng reruns thành test cases mới. 41/41 E2E và 24/24 PostgreSQL của phase trước không được quảng cáo là đã chạy lại trong phase này.

Chat regression lần đầu bị sandbox chặn loopback socket; chạy lại với quyền loopback đã PASS toàn bộ 86. Không có Gemini thật trong cả hai lần.

## 8. Quality blocker / remaining risks

Exact approved-candidate corpus giữ cả L1/L2 active. LQ4 rewritten input thêm `game` và `ket noi day LAN`. SQL FTS OR match nhóm từ kết nối/dây trong L1, trong khi semantic mock đúng L2. DAO trả `[L2,L1]`; source policy trả `AMBIGUOUS_SUBJECT`, validated/selected=[]; expected `[L2]` nên harness halt `SOURCE_SELECTION_LQ4`.

Chứng cứ [LQ4 source observation](D:/miniproject/fpt/.build/phase241-db-mocks/phase24-offline-db-1df0a924-b83f-456f-91da-8b5590ef68df.evidence/LQ4.sources.json). Không thay thresholds, corpus, source authority hoặc hide failure để tạo PASS. 41-case E2E cũ đã dùng các case có archive/switch subjects; không chứng minh two-active-document plan này PASS.

Các hạn chế còn lại:

- Cần bản sửa retrieval/follow-up **riêng, có review** và test lại exact corpus trước khi khẳng định quality ready. Không tự triển khai semantic refactor trong nhiệm vụ runner.
- TLS transport mới chỉ có parser/fault tests offline; handshake và REST behavior với provider phải kiểm chứng trong live approval riêng. HTTP200 không đồng nghĩa RAG accuracy.
- Chưa đo Gemini semantic relevance, token usage, real latency, billing hoặc live source authority conflict handling. Mock latencies không đại diện provider.
- Snapshot quota/tier do admin xác nhận hết hạn sau tối đa 30 phút; runner recheck thời gian trước mỗi send. Không thể quan sát các consumer khác/project billing changes mà không thêm API; admin cần dành riêng test project/quota và giữ Free Tier, không bật billing.
- Filesystem/local evidence phải đáng tin cậy. Process interruption đã test, không mô phỏng mất điện/NTFS corruption hoặc kẻ có quyền sửa/xóa journal/code. Bất kỳ nghi ngờ previous request state đều không được reset plan.
- Chỉ hỗ trợ REST thinking disabled `thinkingBudget:0` đã được admin xác nhận cho exact model. Model cần thinkingLevel hoặc minimum thinking khác là blocker, cần patch riêng; không tự đổi model.

## 9. Exact Chat model, quota/cost requirements

**Exact Chat model chưa được cung cấp (`chatModel:null`).** Không đọc settings/key production để suy ra và không tự thay model. `gemini-2.5-flash` xuất hiện trong mock fixture để thử guards, **không phải lựa chọn/live eligibility đã xác minh**.

Admin cần xác nhận đúng test projects, exact model metadata/REST configuration, input/output limits, Free Tier và current remaining RPM/TPM/RPD cho cả hai test keys. Các fields nằm trong `liveApproval` template của plan; không chứa secrets.

- Embedding: exact 001, 768; remaining RPD >=8, RPM >=1, TPM >=2048; input text+title UTF8 bytes +64 conservative allowance <=2048. Không countTokens call.
- Chat: remaining RPD >=6, RPM >=1, maxOutputTokens=1200; current input/output model limits phải >= frozen bound/output cap.
- Chat request JSON UTF8 byte bound +64 là conservative token allowance, không phải số token đo. Default cap=16384 bytes → yêu cầu Chat TPM còn lại >=**17648** (16384+64+1200); có thể review cap nhỏ hơn nếu toàn bộ final dynamic payloads vẫn fit. Historical 4096-token estimate trong plan không được dùng thay gate này.
- Thinking: exact REST support cho `thinkingBudget:0`, no additional thinking; flag/schema proof reviewed. Usage có thinking tokens khác zero hoặc token counts vượt bound → FAIL/stop.
- Tier phải đúng `Free Tier` cho cả hai, paidRequestsAllowed=false, paid budget và estimated allowed cost=0 USD. Không tự chuyển paid/billing/provider/model. Giá public cũ không được dùng như current billing verification; offline task này không browse/query provider pricing.
- VerifiedAt/ExpiresAt timestamp hợp lệ, expiry >now và window <=30min; chờ spacing ≥65s cùng model. Thiếu một gate → reject **trước provider request**.

## 10. Local verification / readiness / next authorization

Các lệnh offline:

```powershell
pwsh -NoProfile -File 'D:\miniproject\fpt\test\test-rag-live-runner.ps1' -AntHome 'D:\Subject FPT\PRJ301\NetBeans-13\netbeans\extide\ant'
pwsh -NoProfile -File 'D:\miniproject\fpt\test\test-rag-live-entry.ps1'
pwsh -NoProfile -File 'D:\miniproject\fpt\test\test-rag-live-isolation.ps1'
pwsh -NoProfile -File 'D:\miniproject\fpt\test\test-rag-live-database.ps1'
```

Lệnh database cần Docker Engine có sẵn image pinned; tạo/stop riêng container mới, không xóa resources. Chất lượng LQ4 được báo blocker; test safety kiểm tra expected stop, không đánh tráo thành quality PASS.

Lệnh live dưới đây **CHƯA ĐƯỢC PHÉP CHẠY**. Chỉ dùng sau blocker resolution/review, thông tin model/cost/quota/content đầy đủ và phê duyệt live mới:

```powershell
$env:RAG_ENVIRONMENT='development'
$env:RAG_ENABLED='false'
# Two test API keys must already exist in this same PowerShell process; never paste them into chat.
pwsh -NoProfile -File 'D:\miniproject\fpt\test\run-rag-live.ps1' -ApprovedLivePlan -PlanPath 'D:\miniproject\fpt\test\rag\fixtures\phase24-live-plan.json'
```

Current plan sẽ bị `APPROVAL_MISSING` dù truyền switch. Runner còn bắt buộc `readiness.executable=true` và blockers=[] sau review riêng; chỉ đổi approval không đủ vượt gate. Không sửa/reset locks, không tự approve JSON, không gọi thêm request sau failure. Không auto deploy/enable RAG/Phase3.

Readiness: **safety mechanisms verified offline; execution and quality NOT LIVE READY.** Dừng tại preparation. Những việc tiếp theo là review/sửa blocker LQ4 theo phạm vi riêng, admin xác nhận exact Chat model/thinking/tier/quota và corpus, rồi xin phê duyệt test live riêng. Không có API request nào được cấp quyền bởi báo cáo này.
