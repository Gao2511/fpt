# Phase 2.3 — Dry-run chờ phê duyệt

Ngày kiểm tra: 2026-10-11. Branch `feature/rag-integration`, HEAD `48b700f`.
Working tree chứa các thay đổi từ các phase trước; chúng được giữ nguyên.

**Kết quả mới nhất: key test RAG_EMBEDDING_API_KEY đã kiểm tra metadata thành công,
HTTP 200, modelAccessVerified=true, supportsEmbedContent=true, inputTokenLimit=2048,
540,4 ms, đúng 1 GET trong lần chạy này; 0 embedding POST, 0 chat generation.**
Người dùng thực thi probe trong PowerShell test và cung cấp JSON; Codex đã đọc
xác minh file, không gọi API thêm. HTTP 401 / 354,2 ms trước đó vẫn giữ như lịch sử.
Hai JSON hiện có ghi nhận tổng 2 GET ở hai lần riêng; chưa có POST embedding.
Kế hoạch tiếp theo chỉ **1 POST**, xem `docs/rag-phase2.3-single-embedding-plan.md`.
Kế hoạch 10–12 request bên dưới vẫn chưa duyệt và không tự tiếp tục.
Dry-run offline PASS; chưa đo vector thật, semantic relevance, latency hoặc token
usage từ provider. Không thực hiện JDBC write, không tạo/start container trong
phase này. Không commit, push hoặc deploy. Cấu hình repository vẫn
`RAG_ENABLED=false`; process chưa đặt biến này, nên mặc định false. Không truy cập
production để xác nhận cấu hình đang chạy ở đó.

## 1. Adapter và cấu hình đã kiểm tra

| Hạng mục | Bằng chứng / kết luận |
|---|---|
| Model cấu hình | `RagConfig`: chỉ nhận `gemini-embedding-001`, 768 chiều |
| Endpoint | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent` |
| Auth | Header `x-goog-api-key`; không đưa key vào URL/log |
| Payload | `model`, `content.parts[].text`, `embedContentConfig`: taskType, outputDimensionality=768, autoTruncate=false; title chỉ cho document |
| Task | `RETRIEVAL_DOCUMENT` / `RETRIEVAL_QUERY` |
| Response | `embedding.values`; đúng 768 số hữu hạn, vector khác zero, chuẩn hóa L2 |
| Timeout/retry | Deadline 10 giây mỗi embed bao gồm metadata; connect/read tối đa 5 giây; tối đa 3 attempts khi 429/5xx/network; 400/401/403/404 không retry |
| Model access | Metadata bằng key test trả HTTP 200, đúng model, hỗ trợ embedContent; POST/dimensions/quota/billing chưa kiểm chứng |
| Key trong process | Key test ở PowerShell riêng của người dùng, probe ghi keySource=RAG_EMBEDDING_API_KEY; process Codex vẫn chưa nhận biến này; không hiển thị giá trị |
| Key routing | `RagFactory` dùng RAG_EMBEDDING_API_KEY hoặc key Gemini hiện có được truyền từ settings. Không tự đọc GEMINI_API_KEY |
| Approval flags | MODEL_VERIFIED và CALLS_APPROVED hiện chưa đặt → false |
| Usage telemetry | Adapter hiện chỉ lấy vector, chưa lưu usageMetadata/latency/request count |

Đã đối chiếu [REST reference](https://ai.google.dev/api/embeddings):
endpoint, config mới và `embedding.values` phù hợp với tài liệu; provider có
trường `usageMetadata.promptTokenCount`, cần ghi nhận nếu response thực tế trả về.
Chưa kiểm chứng payload bằng HTTP thật.

[Embedding guide](https://ai.google.dev/gemini-api/docs/embeddings) còn liệt kê
`gemini-embedding-001` stable, input tối đa 2.048 token, output 128–3072 chiều,
768 là một kích thước khuyến nghị. Với 001, output dưới 3072 cần chuẩn hóa thủ
công; adapter đã thực hiện. Không đổi sang Embedding 2: hai không gian vector
không tương thích.

Quota cụ thể cần người quản trị xem đúng project trong AI Studio. Giới hạn
RPM/TPM/RPD áp dụng theo project/model/tier, không suy ra từ sự tồn tại của key.
Nguồn: [Rate limits](https://ai.google.dev/gemini-api/docs/rate-limits).

[Trang pricing hiện hành](https://ai.google.dev/gemini-api/docs/pricing) được mở
trong lần audit này có mục Embedding 2, không tìm thấy bảng riêng cho 001.
**Chưa xác nhận giá hoặc free-tier của 001 trong project.** Không dùng giá của
Embedding 2 để tính 001; không khẳng định miễn phí.

## 2. Kế hoạch request và chi phí

Manifest có thể review: `test/rag/fixtures/phase23-embedding-plan.json`,
plan ID `phase23-20261011-v1`, `approved=false`.

| Loại | Request dự kiến |
|---|---:|
| GET metadata đúng model, một adapter instance | 1 |
| POST embedding document, 3 document × 1 chunk | 3 |
| POST embedding query, 6 câu hỏi | 6 |
| Tổng lần gọi bình thường | **10** |
| Trần đề xuất, bao gồm mọi retry/GET/POST | **12** |
| GenerateContent / countTokens / listModels / batch | **0** |

Payload offline có tổng 815 Unicode code points, 1.050 UTF-8 bytes, gồm title.
Ước lượng heuristic **494–1.103 input token**, chưa gồm retry, không phải Google
tokenizer hoặc giới hạn bảo đảm. Cách tính mỗi payload:
`ceil(codePoints/4)+32` đến `codePoints+32`. Sau khi có API response, dùng usage
metadata nếu có; nếu thiếu thì báo unavailable, không biến ước lượng thành số đo.

Nếu người quản trị xác nhận giá là R USD/triệu input token, dự toán cơ bản là
`R × (494..1103) / 1.000.000`, cộng token của những attempt bị retry nếu có tính
phí. Tổng chi phí hiện **chưa xác định** vì R và tier chưa xác nhận.

Giới hạn 12 hiện là **đề xuất trong manifest**, chưa có live runner thực thi
budget. Trước request đầu tiên sau khi được duyệt, phải bổ sung bộ đếm test-only
quanh transport hiện có: tính mọi attempt trước khi gửi, dừng ở 12, ghi status,
latency và usage nếu có; không log auth/raw error. Nếu hết budget, báo phần test
chưa chạy và không tự tăng trần. Không dùng bulk-index CLI hiện tại như một
giới hạn API. Cache và tái sử dụng vector query/document cho mọi DB control.

## 3. Tài liệu chính xác đề nghị duyệt

Cả ba là hướng dẫn kỹ thuật, category `guide`, authority đề nghị
`admin_reviewed`. Chưa published/approved trong database. Bản tóm tắt D1/D2
cần người quản trị duyệt; không coi chúng là chính sách FPT chính thức.

### D1 — Vùng phủ WiFi trong nhà nhiều tầng

Nguồn: `src/java/ai/consultation/ConsultationPolicy.java`, hướng dẫn nhà nhiều
tầng/vật cản/vị trí thiết bị. Policy key `test.wifi.coverage`.

```text
WiFi có thể yếu hoặc không phủ kín các phòng và tầng khi có vật cản hoặc tường dày. Việc tư vấn cho nhà nhiều tầng cần kiểm tra vị trí đặt thiết bị và vật cản thực tế.
```

### D2 — WiFi 6 và WiFi 5: chia sẻ kết nối giữa nhiều thiết bị

Nguồn kỹ thuật: [Cisco, Wi-Fi’s market expands with Wi-Fi 6E](https://blogs.cisco.com/networking/wi-fis-market-expands-with-wi-fi-6e).
Policy key `test.wifi.standards`. Nội dung tiếng Việt đề xuất:

```text
WiFi 6 (IEEE 802.11ax) bổ sung OFDMA so với WiFi 5 (IEEE 802.11ac), giúp phối hợp việc truyền dữ liệu giữa nhiều thiết bị hiệu quả hơn. Đây là khác biệt về chuẩn kết nối; không phải cam kết tốc độ hay vùng phủ của thiết bị FPT cụ thể.
```

### D3 — Hướng dẫn kết nối khi chơi game

Nguồn: nguyên văn câu hướng dẫn hiện có trong
`src/java/ai/consultation/ConsultationPolicy.java`.
Policy key `test.internet.gaming`; dùng làm distractor để phát hiện chọn sai nguồn.

```text
Với nhu cầu chơi game, anh/chị nên ưu tiên kết nối dây LAN; tốc độ gói không bảo đảm ping hay độ trễ thực tế.
```

Không index giá, tốc độ gói, phí lắp đặt, quyền lợi Camera/TV/Mesh hoặc thông tin
khách hàng. Những dữ kiện này thuộc business catalog/settings hiện có. Corpus
không có kiến thức Camera chuyên biệt đã được duyệt, không tự viết thêm.

## 4. Câu hỏi chính xác và tiêu chí đánh giá

| ID | Câu khách hỏi | Nguồn/ứng xử kỳ vọng |
|---|---|---|
| Q1 | Phí lắp đặt thông thường bao nhiêu? | Business: 300.000đ/lần theo dữ liệu hiện có; không dùng KB chứng minh phí |
| Q2 | Nhà nhiều tầng WiFi yếu thì sao? | D1; vị trí/vật cản, có thể hỏi thêm; không tự khẳng định quyền lợi Mesh |
| Q3 | WiFi 6 khác WiFi 5 thế nào? | D2; khác biệt chuẩn chung, không cam kết modem FPT cụ thể |
| Q4 | Shop ơi mạng nhà tui yếu quá. | D1 chỉ liên quan một phần; hỏi rõ yếu sóng WiFi hay lỗi Internet; không suy ra chơi game |
| Q5 | Có camera với truyền hình không? | Business catalog; không lấy quyền lợi từ ba tài liệu này |
| Q6 | Thêm camera thì sao? | Trước đó: “Tôi chọn gói 220K.”; giữ ngữ cảnh gói đã chọn, dùng business rules/catalog, không coi là consent đăng ký |

Query embedding: Q1/Q2/Q3/Q5/Q6 dùng nguyên câu; Q4 dùng chính xác
`Shop ơi mạng nhà tui yếu quá.\ninternet`, trong đó `\n` là newline, do
`RetrievalQuery` hiện tại mở rộng domain. Payload đầy đủ ở
`.build/phase23-dry-run.json`.

Dry-run chỉ chẩn đoán planner với catalog/history rỗng; không chứng minh routing
business hoặc follow-up. Live test dùng fixture catalog/business từ source hiện
có và history cho Q6, không đọc production. Sáu vector là retrieval probes;
business câu Q1/Q5/Q6 còn là đối chứng chọn sai nguồn. Không coi mọi HTTP 200
hoặc một vector gần nhất là câu trả lời đúng.

Sau approval, báo theo từng câu: raw hybrid candidates/scores, nguồn được chọn
sau validator, empty/conflict/ambiguous result, customer-facing reply, lỗi factual,
metadata leakage và latency. Với Q2/Q3 đo source top-1/recall; Q4 chấm partial
relevance + clarification riêng; Q1/Q5/Q6 chấm business routing và sự tách biệt
dữ kiện. Không gộp các nhóm thành tỷ lệ trả lời đúng thiếu nhãn.

Conflicts: tái dùng vector đã có để kiểm tra source/version/authority; trường hợp
cùng policy key, cùng authority nhưng nội dung khác phải fail closed. Đây là
control metadata giả lập, không phải bằng chứng phát hiện mâu thuẫn ngữ nghĩa.
Có thể dùng status/expiry controls với vector cache, không phát sinh API.
Giữ nguyên threshold và ghi nhận failure; không sửa corpus/threshold rồi tính
chung với kết quả đầu. Không có chat-generation trong budget: chất lượng câu
trả lời sinh bởi Gemini chat chưa được đo bởi kế hoạch này.

## 5. Database đích và cách ly

Đề xuất tạo mới sau approval:

- Container `fpt-rag-embedding-test-20261011-p23`; nếu đã tồn tại thì dừng và chọn
  tên mới được ghi lại, không reuse/reset dữ liệu.
- Image pin `pgvector/pgvector:0.8.2-pg17@sha256:feb68f4f15446397d8cac7f4fe48fe4586de83160d1fc48b46283312d1a33966`.
- DB `fpt_rag_test`, bind `127.0.0.1` trên port động; credential mới ngẫu nhiên,
  chỉ file ignored `.local/rag-tests/` và child process test.
- Xác nhận DB mới rỗng; extension vector + migration 002/003 chỉ trong DB mới.
- Dùng DAO/chunker/adapter/retrieval/validator hiện có. Published approval metadata
  test chỉ được tạo sau khi người quản trị duyệt corpus này.
- Stop container khi xong và giữ volume/log để kiểm tra; không xóa dữ liệu cũ.

Read-only Docker audit xác nhận Engine 29.8.0. Container Phase 2.2
`fpt-rag-test-ad8985908086` đang stopped, mount còn giữ; không start hoặc ghi vào
container đó. Kết quả **24/24 DB tests PASS với embedding mock** thuộc Phase 2.2,
không tính thành live semantic PASS trong Phase 2.3.

## 6. Người quản trị cần thực hiện

1. Trong [AI Studio](https://aistudio.google.com/), chọn đúng project gắn với key
   hiện có; xác nhận quyền Generative Language API/model, key restrictions, tier,
   quota còn lại và giá/free-tier riêng của 001. Cung cấp thông tin không nhạy cảm
   (tier/giá/quota) nếu muốn duyệt toàn bộ live test; không gửi key/password.
2. Duyệt ba nội dung ở mục 3, sáu query và trần 12 request gồm retry. Có thể chỉ
   duyệt **1 metadata GET không retry** để kiểm tra access trước; khi đó chưa tạo
   embedding và cần approval riêng cho POST.
3. Sau full approval và xác nhận chi phí: ánh xạ GEMINI_API_KEY sang
   RAG_EMBEDDING_API_KEY **chỉ trong process test**, không tạo/rotate credential,
   không lưu vào tracked files. Model flag chỉ xác nhận sau metadata check thật.
4. Giữ production RAG false; không dùng connection string/settings production.
   Khi gặp 401/403/404, dừng và báo thiếu quyền/model; không thay key/provider tự động.

Tên biến liên quan: `GEMINI_API_KEY`, `RAG_EMBEDDING_API_KEY`,
`RAG_EMBEDDING_MODEL`, `RAG_EMBEDDING_DIMENSIONS`, `RAG_EMBEDDING_MODEL_VERIFIED`,
`RAG_EMBEDDING_CALLS_APPROVED`, `RAG_ENABLED`, `RAG_ENVIRONMENT`,
`RAG_DATABASE_APPROVED`, `RAG_TEST_DATABASE_APPROVED`, `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE`. Không xuất giá trị nhạy cảm.

## 7. File và kiểm thử thực tế của lượt này

Chỉ thêm công cụ offline/manifest/báo cáo, không sửa runtime website:

- `test/rag/fixtures/phase23-embedding-plan.json`: corpus, câu hỏi, request budget,
  DB target và trạng thái chưa duyệt.
- `test/rag/RagEmbeddingDryRun.java`: gọi chunker/filter/request builder hiện có,
  transport offline ném lỗi nếu có ý định HTTP; không dùng key thật/JDBC.
- `test/run-rag-embedding-dry-run.ps1`: javac/Java chạy dry-run.
- `docs/rag-phase2.3-dry-run.md`: báo cáo này.
- `test/check-rag-model-access.ps1`: probe opt-in đúng một metadata GET,
  không retry/redirect, chỉ xuất telemetry an toàn.
- `docs/rag-phase2.3-model-access.md`: kết quả probe sau approval và bước xử lý.

Đã chạy trên JDK 17, compile `--release 8`, exit 0: **3 chunks**, cả 3 qua content
filter với approval proof synthetic, **9 payload** đúng cấu hình, **0 API**.
Proof synthetic chỉ kiểm tra filter, không duyệt/persist nội dung.
Log `.build/phase23-dry-run.log`; JSON `.build/phase23-dry-run.json`, đều ignored.

Chạy lại offline, sau build source hiện có và cấu hình javac/java JDK 17 trên PATH:

```powershell
./test/run-rag-embedding-dry-run.ps1
```

Không rerun full regression vì lượt này không sửa runtime. Chưa có kết quả
embedding provider errors, vector dimensions thực trả, embedding latency, actual token usage,
relevance/answer rate thật; tất cả vẫn **chưa đo**, không giả định PASS.
Metadata probe đã đo riêng: lịch sử HTTP 401 / 354,2 ms; mới nhất HTTP 200 /
540,4 ms. Không phải embedding latency. pricingVerified/quotaVerified vẫn false.

Admin Knowledge Base integration nên tiếp tục sau khi live test đạt source
selection và business separation: draft → review → publish → index-job,
version/expiry/authority, trạng thái lỗi an toàn và audit actor. Tận dụng DAO/CMS
hiện có. Chưa triển khai Admin KB, chưa enable RAG cho khách hàng.
