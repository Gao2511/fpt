# Phase 2.4 — Rà soát kế hoạch live trước phê duyệt

> Phase 2.4.2: blocker LQ4 đã được giải quyết bằng query-aware source selection; cùng corpus/expectations đạt 6/6 mock. Ngân sách vẫn 2+6+6=14, approved=false, executionAuthorized=false, productionRagEnabled=false. Exact Chat model, thinking/quota/tier và corpus/live approval vẫn chặn live. [Báo cáo](D:/miniproject/fpt/docs/rag-phase2.4.2-readiness-report.md).

> Cập nhật Phase 2.4.1: runnerImplemented=true trong plan; approved=false, executionAuthorized=false. Cơ chế lock/budget/isolation đã được test offline, **chưa LIVE READY** vì exact chat/quota/approval còn thiếu và measured offline LQ4 ambiguity. [Báo cáo mới](D:/miniproject/fpt/docs/rag-phase2.4.1-live-runner-readiness.md). Nội dung review dưới đây phản ánh trạng thái trước implementation, được giữ để truy vết.

2026-10-11, branch feature/rag-integration. Plan phase24-e2e-20261011-v1.
**approved=false, executionAuthorized=false, runnerImplemented=false.**
Lần rà soát này: Gemini thật 0 request, database access 0; không Docker/DB production, không đổi runtime code, credentials hoặc flags production; không commit/push/deploy.

Kết luận: ngân sách và cấu hình dự kiến nhất quán, nhưng **chưa thể thực thi**. Thiếu exact chat model, xác nhận Free Tier/quota hiện tại, duyệt nội dung live và transport/runner cưỡng chế budget. Phê duyệt live không tự giải quyết các thiếu hụt kỹ thuật này.

Kế hoạch máy đọc được: [phase24-live-plan.json](D:/miniproject/fpt/test/rag/fixtures/phase24-live-plan.json).

## 1. Ngân sách request

| Operation | Số tối đa |
| --- | ---: |
| Embedding tài liệu L1/L2, mỗi tài liệu một chunk | 2 POST |
| Embedding câu hỏi LQ1–LQ6 | 6 POST |
| Gemini generateContent | 6 POST |
| Tổng outbound provider requests | **14** |
| GET metadata / model listing / countTokens | **0** |
| Retry / redirect / repair POST / fallback POST bổ sung | **0** |

Chat không cần thiết hoặc gặp lỗi sớm có thể làm tổng ít hơn 14. Không dùng phần budget còn lại để thêm câu hỏi hoặc chạy lại. Request timeout/failed vẫn tiêu thụ slot. Lỗi đầu tiên dừng toàn bộ plan, không coi kết quả một phần là PASS.

Hai tài liệu ngắn nằm trong kế hoạch: L1 là hướng dẫn kiểm tra dây mạng từ fixture QA đã tồn tại; L2 là nguyên văn hướng dẫn LAN/chơi game của ConsultationPolicy. approvedForLive của cả hai vẫn false. L1 không được trình bày như policy FPT đã chứng thực.

## 2. Cùng vector space và task type

Document và query đều dùng **gemini-embedding-001**, root **outputDimensionality: 768** (JSON integer), cùng endpoint REST cố định models/gemini-embedding-001:embedContent.

| Loại | taskType | title |
| --- | --- | --- |
| Document | RETRIEVAL_DOCUMENT | Title L1/L2 đã duyệt |
| Query | RETRIEVAL_QUERY | Không có title |

Sau mỗi response: đúng 768 số hữu hạn, vector khác zero, chuẩn hóa L2 bằng VectorValues cho cả document/query. Sai chiều là FAIL; không truncate/pad. Lưu embedding_model/vector_dimensions/revision/hash với chunks; DAO chỉ tìm đúng model/dimensions/version đang có hiệu lực. DB vector(768) là lớp bảo vệ thêm.

DB live test mới phải trống: không dùng lại vector mock, vector 3072 hoặc vector khác model. Không đổi model tự động khi API lỗi.

Adapter thật đã được kiểm thử offline lại **35/35** RagBehaviorTest với injected transport: serialized document/query payload đúng task/root dimension/title, normalized output; 3072 bị loại, cost gates và bounded failures đúng hành vi. Đây không phải real provider verification mới.

InputTokenLimit=2048 là metadata evidence lịch sử đã lưu, không GET lại. Query/document trong plan ngắn nhưng giới hạn 2000 ký tự của runtime không đồng nghĩa 2048 tokens. Final payloads/query rewrites và token-setting assumptions phải được review trước gửi. Không gọi countTokens vì budget không cho phép.

## 3. Câu hỏi và kết quả mong đợi

| Case | Câu hỏi | Source được chọn / hành vi |
| --- | --- | --- |
| LQ1 | Mạng nhà mình chậm quá. | L1; hướng dẫn kiểm tra dây mạng an toàn, không hứa tốc độ/ping |
| LQ2 | Shop ơi mạng nhà tui yếu quá. | L1; cùng hướng dẫn, xử lý tiếng Việt đời thường |
| LQ3 | Hướng dẫn chơi game Internet sao cho ổn? | L2; ưu tiên LAN, giữ nguyên giới hạn không bảo đảm độ trễ |
| LQ4 | Vậy cách đó có đảm bảo ping thấp không? | L2; cùng session LQ3, hiểu chủ đề game, không bảo đảm ping |
| LQ5 | WiFi 6 có tốt hơn WiFi 5 không? | Không chọn source; corpus không có so sánh được duyệt, nói thiếu dữ liệu |
| LQ6 | Có cung cấp internet ở nước ngoài không? | Không chọn source; không tự xác nhận vùng phục vụ |

Chỉ LQ3/LQ4 chia sẻ session; các case còn lại độc lập. Query rewrite dùng RetrievalQuery hiện có, chỉ domain từ lượt user trước, không copy PII/assistant instructions. Ghi hash của actual rewritten input trước send.

Raw candidates, validated candidates, selected IDs và customer reply phải báo cáo riêng. HTTP 200 hoặc “có đoạn được retrieve” không đủ để PASS.

Rủi ro đã biết: khi cả L1/L2 cùng active, nguồn khác chủ đề có thể xuất hiện qua semantic/lexical overlap và bị AMBIGUOUS_SUBJECT. Chưa đo bằng Gemini thật. Nếu xảy ra, ghi FAIL/stop; không giảm ngưỡng hoặc thay corpus giữa lượt để tạo PASS.

## 4. Tier, quota và chi phí tối đa

**Chỉ cho phép Free Tier đã xác nhận cho cả hai exact models. Trần paid cost được phép: 0 USD; paidRequestsAllowed=false.**
Chi phí dự kiến tối đa 0 USD chỉ có hiệu lực nếu admin xác nhận model đủ điều kiện Free Tier và quota ở đúng projects của test keys. Thiếu xác nhận hoặc Paid Tier thì không gửi request; không upgrade billing/paid fallback.
Đây là giới hạn kế hoạch và điều kiện thực thi, không phải phép đo billing hoặc bảo đảm đối với thay đổi billing ngoài harness.

Snapshot Phase 2.3 do admin xác nhận: Embedding Free Tier, RPM100/TPM30000/RPD1000. Snapshot này không chứng minh quota còn lại hiện tại hoặc Free Tier của model chat chưa cung cấp.

Cần dung lượng còn lại **ít nhất 8 embedding và 6 chat requests**. Chạy tuần tự, cách ít nhất 65 giây giữa hai sends cùng model để không cần RPM cao hơn 1; không concurrent requests. Quota theo project, các consumer khác có thể dùng hết trong lúc chạy.

Token estimates chưa đo:

- Embedding input tổng 300–700; mỗi input phải nằm dưới model limit 2048.
- Chat input tổng 12000–24000, dự trù khoảng tối đa 4096/call gồm system/catalog/context và bounded follow-up history.
- maxOutputTokens=1200/call, tổng tối đa 7200 output allowance. Phải xác nhận cách exact model giới hạn/tính phí thinking; chưa giả định thinking miễn phí hoặc đã được tắt.
- Chat TPM phải đủ input và output/thinking theo quy tắc của model đó. Ước lượng 4096 không phải kết quả tokenizer hay hard token bound.

Giá lịch sử Embedding 1 $0.15/million do admin xác nhận trước đây chỉ cho minh họa $0.000045–$0.000105 với 300–700 tokens; **không phải mức paid được duyệt**. Chưa xác minh giá 001 hiện tại hoặc giá chat. Không lấy giá Embedding 2 để thay 001.

Admin cần cung cấp tên model chat đang dùng (không secret), xác nhận Free Tier/remaining RPM/TPM/RPD của cả hai models và thinking behavior từ đúng AI Studio projects. Không gửi API keys/billing details nhạy cảm. Không đọc production settings để lấy các dữ liệu này.

## 5. PASS/FAIL cố định trước live

| Hạng mục | Điều kiện |
| --- | --- |
| Retrieval recall | 4/4 positive queries có expected document trong top3 document IDs hợp lệ, deduplicated |
| Retrieval precision | Mean Precision@3 trên LQ1–LQ4 >=0.8. Mẫu số là min(3, số document thực trả về); empty result =0, không chia luôn cho 3 khi corpus chỉ có 2 docs |
| Source selection | LQ1/LQ2 chỉ L1, LQ3/LQ4 chỉ L2, LQ5/LQ6 không source |
| Grounding | Chỉ approved selected literal excerpts; không invented coverage/ping/specs/discount/price facts |
| Authority/currentness | Trusted test publication, current hash/version, valid dates, admin_reviewed authority; revalidate ngay trước render |
| Unknown questions | 2/2 abstain rõ thiếu bằng chứng; không dùng đoạn không liên quan làm câu trả lời |
| Follow-up | LQ4 giữ chủ đề game từ LQ3; không nhầm customer/session khác |
| Safety | Unsupported claims, secret/metadata/PII leaks, business overrides, unsolicited registration, production access, unexpected requests đều =0 |
| Language/format | Customer response hoàn chỉnh tiếng Việt; strict schema và finish reason hợp lệ; không display object/reasoning |
| Vector | Mọi returned/stored/query vector đúng 768, finite, nonzero, normalized; exact model metadata |
| Latency targets | Embedding <=5s; complete turn <=10s, loại quota-spacing sleep khỏi latency. Báo target failure riêng khỏi semantic failure |

Live smoke này chỉ có hai nguồn admin_reviewed, không chứng minh real API handling cho mọi conflict/supersession/adversarial case. Authority conflict/draft/expired/replacement/injection gates được giữ từ 41/41 + 24/24 offline/DB tests. Không dán nhãn official cho synthetic QA docs.

Mọi nội dung giá/tốc độ/phí/combo/promotion tiếp tục từ test business catalog có nguồn JDBC rõ ràng; KB không chứa/bổ sung business claims. Không đổi giá hoặc chính sách hệ thống.

## 6. DB cô lập và bảo vệ production

Chưa chọn instance live test và không tạo DB trong lượt review này. Dự kiến container PostgreSQL17/pgvector0.8.2 **mới**:

- 127.0.0.1/random mapped port, database fpt_rag_test, label com.fpt.rag.test=true, credentials test mới.
- Verify container identity/mapped port, database/user và tables trống; migrate 002/003 chỉ trong DB đó.
- Inject explicit connection vào DAO/harness. Không dùng default RagFactory/DBUtils fallback hoặc .local/runtime.json.
- Test-only RAG_EMBEDDING_API_KEY và proposed RAG_CHAT_TEST_API_KEY; không đọc key CMS/chatbot production.
- RAG_ENVIRONMENT=development; RAG_ENABLED=false tại website/default. Test config được inject riêng, không enable customer-facing RAG.
- Giữ tất cả container/volume/data cũ. Teardown chỉ stop container chính run đó tạo, không delete/drop production.

Chỉ tên DB/credential variables được báo cáo. Runner kiểm tra isolation phải fail closed trước connection/API nếu identity chưa xác nhận.

## 7. Counter, evidence và chống chạy lại

**Thiết kế dưới đây chưa được triển khai cho live 14-request runner.**

Trước request đầu: cần separate approval của plan/corpus/model/tier/quota, frozen embedding payload/query hashes và chat template rules; preflight DB/keys/budget/evidence paths.
Final chat context phụ thuộc retrieval thật nên ghi hash dynamic body ngay trước từng send, giới hạn nội dung trong approved docs/template; không giả rằng toàn bộ chat bytes đã biết trước.

Atomic CreateNew lock dự kiến:
.build/phase24-live/phase24-e2e-20261011-v1.request.lock.
Existing lock **hoặc** orphan result bất kỳ của plan đều chặn; lock không reset hoặc resume sau crash/failure.
Lượt review này không tạo lock live (đã kiểm tra: absent).

Mỗi operation có ordinal duy nhất trong allowlist 2 documents/6 questions/6 chats. Reserve slot và ledger atomically **trước** send; global<=14, per-kind<=2/6/6; timeout tính đã tiêu thụ. Reused ordinal bị từ chối, chặn cả internal repair/retry/fallback.
Transport chỉ POST exact approved Gemini endpoints; no outbound GET/listModels/countTokens, redirect disabled; 3xx/HTTP failures stop. Cached local metadata evidence có thể phục vụ verification nội bộ, không send GET thật.

Audit source hiện tại:

- GeminiEmbeddingService có first-call metadata GET và tối đa 3 send attempts; HTTP transport của nó đã tắt redirects.
- GeminiProvider tắt provider retry khi có responseSchema, nhưng ConsultationEngine vẫn có repair/fallback và provider openConnection không explicitly tắt redirects.
- Vì vậy dùng adapter/Engine mặc định trực tiếp **chưa bảo đảm trần 14**. Cần inject/review budget guard trên mọi outbound path rồi mock-test trước bất kỳ live authorization execution nào.

Evidence paths unique + CREATE_NEW, reserve storage trước network; chỉ finalize files thuộc run hiện tại. Fail khi không bảo đảm lưu được lock/counter/evidence.
Lưu plan/body hashes, task/model/dimensions, status/sanitized code, norm/dim không toàn vector, retrieved/validated/selected IDs, public reply, latency, token counts nếu trả về (null nếu không có), global/per-operation counters. Không lưu header/key/raw errors/PII.
Nếu quota/auth/timeout/vector/schema/source/grounding/DB/evidence lỗi thì stop toàn plan; không dùng request khác để sửa hoặc kiểm tra thêm.

## 8. Kiểm chứng runner không ghi đè artifact cũ

Đã thực thi [review-rag-phase24-plan.ps1](D:/miniproject/fpt/test/review-rag-phase24-plan.ps1):

- Snapshot SHA256 và LastWriteTimeUtc trước chạy của 8 artifact có sẵn trong rag-embedding-probes/rag-auth-probes.
- Chạy runner mock v2 hai lần cùng PlanId, injected fake transport, **29/29 mỗi lần**, realApiRequests=0.
- Hai output filenames khác nhau, dạng .mocks.GUID.json; không trùng bất kỳ tên có sẵn.
- Kiểm tra lại 8 artifact gốc + 2 report mới: **10/10 unchanged** đối với snapshot; report lần thứ nhất được giữ nguyên qua lần thứ hai.
- Loopback/database/Docker/provider thật không được gọi. Budget/type/isolation/unapproved plan checks PASS.
- RagBehaviorTest chạy lại **35/35** với mock transport để xác minh adapter contracts.

Sanitized review evidence: [phase24-plan-review JSON](D:/miniproject/fpt/.build/phase24-plan-review-f171aa93b29044f6abd81efc011e20e9.json).

Giới hạn kết luận: kiểm chứng này bảo vệ snapshot ở đầu lượt review hiện tại. Nó **không phục hồi** nội dung cũ của legacy v2 .mocks.json đã bị ghi đè trong lượt trước. Sự cố cũ vẫn được công khai ở mục 7 báo cáo Phase 2.4; không thay/che evidence đó. Kết quả API thật và execution locks không bị ghi đè.

File thay đổi trong lượt này: phase24-live-plan.json, rag-phase2.4-e2e-validation.md, report review này và offline review script mới. Không sửa app/runtime/provider/credentials/migrations.

Giữ approved=false. Dừng sau offline review; chờ xác nhận riêng về corpus, exact chat model/tier/quota và phê duyệt bước tiếp theo. Live runner chưa sẵn sàng nên chưa được thực thi, kể cả khi budget trên giấy đã đúng.
