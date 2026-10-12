# Phase 2 — kiểm chứng embedding và RAG, 2026-10-11

Báo cáo lịch sử trước bản sửa Phase 2.1. Các giới hạn/query test đã được cập nhật
trong [báo cáo Phase 2.1](rag-phase2.1-quality.md); số liệu dưới đây phản ánh lần
kiểm tra Phase 2 ban đầu, không phải kết quả của source mới nhất.

## Trạng thái thực tế

**Chưa chạy API Gemini thật; chưa hoàn tất đánh giá RAG bằng embedding thật.**
Branch hiện tại là `feature/rag-integration`, HEAD `48b700f`. Các thay đổi Phase 1
vẫn chưa commit. Phase 2 chỉ bổ sung tài liệu, dữ liệu kiểm thử và test offline;
không sửa implementation, prompt, CMS, giá gói, chat model hoặc encryption key.
Không push, merge, deploy hoặc truy cập database production.

Môi trường shell chưa nạp `RAG_EMBEDDING_API_KEY`, các biến `DB_*` hoặc
`AI_ENCRYPTION_KEY`; các cờ bật RAG, xác minh model và phê duyệt API đều chưa bật.
Cấu hình local riêng có thông tin DB/key, nhưng phép kiểm tra JDBC **chỉ đọc**
không kết nối được; cổng DB local đã cấu hình không có listener tại thời điểm kiểm tra.
Vì vậy chưa đọc được trạng thái key Gemini trong bảng settings. Đây không phải
kết luận rằng tài khoản Gemini hoặc key đang dùng trên cloud bị thiếu/hỏng.

Thử chạy lại integration test trong container mới cũng chưa thực hiện được:
Docker không phản hồi, lệnh được ngắt. Không có kết quả pass từ lần thử này.
Tên test dự kiến: `fpt-rag-test-7bd40796a8d5`. Chưa xác nhận được daemon đã tạo
container hay chưa; khi Docker hoạt động lại, quản trị viên kiểm tra đúng tên này
và dừng nếu còn chạy. Không xóa dữ liệu hoặc container tự động.
Cấu hình test được tạo trong `.local/rag-tests/` vẫn được Git ignore.

`RAG_ENABLED=false` vẫn là mặc định. Không nạp hoặc thay đổi biến môi trường
của Tomcat đang chạy. Hai suite offline dùng provider/DAO giả được inject trực tiếp.

## Cấu hình embedding đã kiểm tra

Đối chiếu tài liệu Google trực tiếp trong ngày kiểm tra:

- [Embedding guide](https://ai.google.dev/gemini-api/docs/embeddings): model
  `gemini-embedding-001` vẫn được mô tả; 768 chiều được hỗ trợ. Vector rút gọn
  của model này cần chuẩn hóa thủ công. Đây không chứng minh quyền truy cập của project.
- [API reference](https://ai.google.dev/api/embeddings): request dùng
  `embedContentConfig` với `taskType`, `title`, `outputDimensionality`,
  `autoTruncate`; response có `embedding.values` và có thể có `usageMetadata`.
- [Models API](https://ai.google.dev/api/models): metadata GET dùng để kiểm tra
  model/method; không tạo embedding, không thay cho phép thử POST có phê duyệt.
- [Pricing](https://ai.google.dev/gemini-api/docs/pricing): trang được đọc không có
  mục giá `gemini-embedding-001`. Không lấy giá/free tier của embedding-2 áp cho 001.
- [Rate limits](https://ai.google.dev/gemini-api/docs/rate-limits): quota thuộc
  project và cần xem mức thực tế trong AI Studio.
- [API keys](https://ai.google.dev/gemini-api/docs/api-key): cần kiểm tra key type,
  trạng thái và restrictions hiện tại. Không tự sửa restrictions hoặc tạo/rotate key.

| Nội dung | Bằng chứng implementation | Kết luận |
|---|---|---|
| Model/vector | `src/java/rag/RagConfig.java` | Chỉ chấp nhận 001/768; không trộn model khác |
| Endpoint | `src/java/rag/GeminiEmbeddingService.java` | `v1beta/models/gemini-embedding-001:embedContent` |
| Request | Cùng file, `requestBody` | document/query dùng task riêng; title chỉ document; không tự truncate |
| Response | Cùng file, `embed` | Phải đủ 768 số hữu hạn, không vector zero; L2 normalize |
| Metadata | Cùng file, `verifyAccount` | Phải đúng tên model và có method `embedContent` |
| Retry/timeout | Cùng file, `send`/`HttpTransport` | Deadline 10 giây, tối đa 3 lần; lỗi 400/401/403/404 không retry |
| Secret | Cùng file | Header `x-goog-api-key`, không URL/log/error body; không redirect |
| DB/vector | `database/migrations/002-rag-mvp.sql` | `vector(768)`, 3 bảng, RLS; không tự bật extension |

Request khớp tài liệu và mock contract test; chưa xác minh response/khả năng
hỗ trợ request trên tài khoản thật. Adapter hiện bỏ qua embedding usage metadata,
không có bộ đếm HTTP attempts/token/chi phí để tạo báo cáo vận hành đầy đủ.

## Có thể dùng lại key Gemini hiện tại không?

Code public chat trong `src/java/ai/AIService.java` đọc key mã hóa từ
`ai_provider_gemini_key`, fallback `gemini_api_key`; `RagFactory` ưu tiên
`RAG_EMBEDDING_API_KEY`, sau đó mới dùng key Gemini đã giải mã.
Như vậy **có hỗ trợ tái sử dụng key**, nhưng quyền embedding, quota và billing
của key này vẫn chưa được xác minh. Không gọi `AIService` để dò cấu hình vì luồng
đó có thể thực hiện chat/migration settings; không thay đổi key đang dùng.

Operator tool `tools/run-rag-tool.ps1 -Operation verify-model` cần key riêng trong
environment, không tự lấy/decrypt key từ Admin DB. File `.env.example` cũng không
được ứng dụng tự động nạp. Quản trị viên cần cấu hình riêng process dùng để test.

## Bộ nội dung kiểm thử từ project

`test/rag/fixtures/project-knowledge.json` có 5 snapshot và đường dẫn nguồn.
Test kiểm tra nội dung thật sự xuất hiện trong source, không chỉ kiểm tra prompt.
Không snapshot nào được ghi DB, publish hoặc index tự động.

| Snapshot | Nguồn | Cách sử dụng |
|---|---|---|
| Hướng dẫn chơi game qua LAN, không bảo đảm ping | `ConsultationPolicy.java` | Mẫu RAG được lấy từ logic hiện có; cần quản trị viên xác nhận trước live publish |
| Phí Internet thông thường một lần | `SiteContent.java`, `business.standardFee` | Kiểm thử business route; giá trị live lấy DB/CMS, không lấy snapshot làm giá mới |
| WiFi 6 | `SiteContent.java`, device description | Nội dung marketing, không phải tài liệu hỗ trợ; câu bảo đảm không giật lag cần quản trị viên kiểm chứng |
| Camera | `SiteContent.java`, device description | Cần FAQ kỹ thuật/chính sách được duyệt; không suy ra thời gian lưu trữ/bảo hành |
| Mesh F1 | `ConsultationPolicy.java` | Business route hiện có; không cập nhật phí/thiết bị từ snapshot |

**Chỉ 1/5 snapshot qua content filter hiện tại**, không phải retrieval recall 20%.
Các mục phí/Mesh cố ý giữ ngoài RAG; WiFi 6/Camera cũng bị chặn toàn chunk.
Không dùng tài liệu hợp đồng, SLA lắp đặt, bảo hành/camera privacy hoặc quy trình
đổi mật khẩu tự viết khi chưa có nguồn đã duyệt. Các nội dung thiếu được liệt kê
trong `missingPolicies`. Các conflict/injection probe trong test là dữ liệu tấn công
giả, chỉ dùng in-memory và không phải chính sách FPT.

## Kết quả test đã thực thi

Lệnh thực tế trên Java 17/Windows:

```powershell
./test/run-rag-phase2-offline.ps1 -AntHome '<thu-muc-Ant>'
```

Build Ant thành công; compile test với `--release 8` chạy trên Java 17.
`RagBehaviorTest`: **34/34 pass**. `RagPhase2CorpusTest`: **13/13 diagnostic pass**.
Diagnostic xác nhận giới hạn hiện tại không có nghĩa là sản phẩm đã xử lý tốt tình huống đó.

| Trường hợp yêu cầu | Bằng chứng trong lần chạy này |
|---|---|
| Exact keyword, câu tiếng Việt tự nhiên | Engine gọi DAO mock và render đúng excerpt nguồn, không hiện ID/hash/source path |
| Gen Z/viết tắt | `wifi lag quá bro fix sao z?` không gọi retrieval — thiếu sót đã xác nhận |
| Follow-up | Lịch sử được gửi model, nhưng câu tiếp nối không gọi retrieval — thiếu sót đã xác nhận |
| Câu hỏi không có đáp án | Model mock chọn nguồn rỗng → thông báo chưa có nguồn còn hiệu lực |
| Hết hạn/chưa tới ngày | Bị loại bởi service trong mock test; chưa rerun filter SQL thật |
| Unpublished | Đã kiểm tra điều kiện SQL published/approved/current; chưa chạy được PostgreSQL test Phase 2 |
| Chính sách mâu thuẫn | Hai excerpt đủ điều kiện trái nhau cùng được render khi model chọn cả hai — thiếu sót đã xác nhận |
| Gemini chat lỗi | Mock SERVER_ERROR → safe fallback, không lộ diagnostic |
| Quota embedding | Mock 429, retry hữu hạn; vẫn lexical fallback khi embedding unavailable |
| Retrieval timeout | SQLTimeoutException mock → fallback, không lộ error detail; không phải đo timeout DB thật |
| Injection trong tài liệu | Probe bị loại trước context; chưa red-team toàn diện Unicode/semantic injection |
| Giá/phí/spec | Engine dùng catalog fixture và business policy, bỏ qua dữ liệu sai trong knowledge; không đọc giá live DB lần này |
| Response malformed/truncated | Không render raw payload; nguồn sai/thu hồi bị chặn |

Các assertion SQL/status/indexing/lease/RLS của `RagPostgresTest` được giữ nguyên.
Kết quả PostgreSQL pass được ghi ở Phase 1 là kết quả lịch sử, không đưa vào số
test đã chạy thành công trong Phase 2 này.

## Metrics: không suy diễn số liệu live

| Metric | Kết quả Phase 2 |
|---|---|
| Relevance, Recall@3, MRR | Chưa đo; DAO mock không đánh giá semantic/FTS thật |
| Correct answer rate trên câu hỏi approved với Gemini thật | Chưa đo |
| Unsupported claims của Gemini thật | Chưa đo; đã xác nhận rủi ro contradiction bằng mock |
| Average/p50/p95 response latency end-to-end | Chưa đo; latency mock không dùng làm số liệu |
| Live model metadata / embedding / chat API calls | 0 / 0 / 0 |
| Token usage live | Không có request; chưa có số liệu usage để đánh giá |
| Chi phí API phát sinh do lần kiểm tra này | 0 do không gọi API; không có nghĩa model miễn phí |
| Estimated cost của lần chạy thật | Chưa thể tính vì chưa xác nhận giá 001 và chat model/project tier |

Sau phê duyệt, ghi từng câu hỏi và relevant document IDs do người quản trị gán,
top-3 IDs/version, nguồn được chọn, outcome/fallback, tổng wall-clock latency,
embedding/chat HTTP attempts và token usage được provider trả về. Không ghi key,
private payload hoặc dữ liệu khách hàng. Chấm relevance riêng với correctness;
unknown cần trả lời không đủ nguồn, conflict cần tránh khẳng định một chính sách.
Ước tính chi phí theo token thực tế nhân đơn giá đã xác nhận; không tính API calls
là token, không bỏ qua retry, không diễn giải usage thiếu là 0 token.

## Quản trị viên cần thực hiện trước API thật

1. Mở [AI Studio API Keys](https://aistudio.google.com/api-keys) đúng project của
   chatbot. Xác nhận existing key còn hoạt động và được phép dùng Gemini API
   từ backend test. Không gửi key vào chat/Git hoặc đổi key/restrictions tự động.
   Nếu không có key dùng được, quản trị viên tự cấp key cho đúng project và cấu
   hình server riêng, không thay encryption key hay key production đang hoạt động.
2. Tại [AI Studio](https://aistudio.google.com/), kiểm tra model 001, billing tier,
   quota RPM/TPM/RPD và giá áp dụng chính xác. Nếu không thấy giá/quota cho 001,
   cần xác minh với provider trước POST; không tự bật billing hoặc chuyển model.
3. Nạp riêng process development/staging: `RAG_EMBEDDING_API_KEY`,
   `RAG_ENVIRONMENT`; giữ các cờ approval/activation false. Sau build portable,
   operator chạy `./tools/run-rag-tool.ps1 -Operation verify-model`. Đây là GET
   metadata, chưa chạy ở Phase 2. GET pass không chứng minh free-tier hay quota còn đủ.
4. Chuẩn bị DB **riêng**: khởi động Docker Desktop và dùng container test mới,
   hoặc staging DB được duyệt. Không trỏ vào Supabase production hoặc DB website local.
   Cấu hình `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE`
   trong process test; admin cài pgvector/chạy migration 002 chỉ trên DB test
   đã được duyệt, cấu hình quyền/RLS cho backend role. Không tự tạo cloud service.
5. Xác nhận nội dung LAN và nguồn để publish một document test. Các nội dung
   thiếu chính sách vẫn cần nguồn và phê duyệt, không tự thêm từ kiến thức model.
6. Phê duyệt rõ lần smoke test và trần chi phí trước khi bật
   `RAG_EMBEDDING_MODEL_VERIFIED`, `RAG_EMBEDDING_CALLS_APPROVED`,
   `RAG_DATABASE_APPROVED` trong **process test**. `RAG_ENABLED` của website
   vẫn false. Dùng dependency injection cho harness test, không bật Tomcat production.

Đề xuất smoke test đầu tiên (chưa được thực thi/phê duyệt): một document LAN,
một chunk; ba câu hỏi exact/natural/slang qua engine với Gemini chat model hiện
có. Tối đa bốn logical embedding operations (index + ba query), mỗi operation
có tối đa ba POST attempts; chat tối đa ba attempts/câu trong engine có schema.
Dừng khi lỗi permission/quota, hết budget hoặc dữ liệu không hợp lệ; không cố
làm cạn quota thật. Sau đó mới duyệt corpus rộng hơn và cả phiên follow-up.
Hiện chưa có harness live tổng hợp metrics/budget để tự động chạy đề xuất này.

Sau khi Docker hoạt động, có thể rerun suite PostgreSQL **mock embedding** bằng
`./test/run-rag-isolated.ps1`; script tạo DB/container mới chỉ bind loopback,
không đọc credential website và dừng container sau test. Không dùng script này
làm bằng chứng Gemini embedding thật.

## Vấn đề kỹ thuật và phase tiếp theo

1. Gate `ConsultationEngine.ragQuestion` chỉ xem câu hiện tại và một nhóm cụm từ.
   Cần query rewriting có kiểm soát từ lịch sử cho câu hỏi nối tiếp và tiếng lóng;
   vẫn ưu tiên registration/private account/business rules. Thêm regression thực tế.
2. `KnowledgeContentPolicy` chặn cả camera/WiFi 6 và nội dung số, không chỉ giá sai.
   Trước mở các nhóm FAQ này, cần tách trường business khỏi hướng dẫn kỹ thuật
   đã duyệt; không đơn giản bỏ validator rồi cho model viết tự do.
3. Chưa có authority/priority hoặc quan hệ supersedes giữa các policy document.
   Cần phát hiện conflict trước publication và xử lý nguồn mâu thuẫn khi retrieval.
4. Chưa có measurement layer token/attempts/chi phí và budget tổng cho live test.
   `ChatResponse.latencyMs` là latency provider, không phải toàn pipeline; retrieval
   dùng timeout riêng, chưa dùng chung deadline với toàn lượt chat.
5. Exact vector search/FTS weights và threshold chưa được benchmark tiếng Việt.
   Không coi fake vectors hoặc 47 assertion offline là chất lượng semantic thật.
6. Publication DAO yêu cầu actor nhưng chưa có Knowledge Admin controller/UI;
   publish live cần đường vận hành admin đã xác thực/CSRF được duyệt.

Khuyến nghị: xử lý tối thiểu gate/follow-up và conflict, bổ sung harness đo live
có budget, sau đó mới chạy smoke test được duyệt. Giữ provider/chat model hiện tại
và default-off, chưa cần dịch vụ vector bên ngoài hoặc model chạy trên máy cá nhân.

## File bổ sung trong Phase 2

- `docs/rag-phase2-validation.md`: báo cáo, setup và kế hoạch đánh giá.
- `test/rag/fixtures/project-knowledge.json`: 5 snapshot có nguồn, review state.
- `test/rag/RagPhase2CorpusTest.java`: chẩn đoán hành vi và các giới hạn bằng mock.
- `test/run-rag-phase2-offline.ps1`: build/runner offline, không nạp credential.

Probe local Java chỉ nằm trong `.build/` ignored. Mọi thay đổi implementation
đã có trước Phase 2 được giữ nguyên, chưa commit. Không đổi `.env.example` hay
giá/ưu đãi/Mesh/prompt. Chưa cần rollback production vì không deploy hoặc đổi DB;
giữ cờ RAG false và dừng process test khi chưa có đủ điều kiện.
