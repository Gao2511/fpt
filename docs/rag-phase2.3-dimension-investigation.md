# Phase 2.3 — 3072 vs 768: offline investigation

Ngày 2026-10-11, branch `feature/rag-integration`. Không gọi Gemini, không
đọc key, không truy cập JDBC/PostgreSQL/Supabase, không commit/push/deploy.
Chỉ đọc tài liệu công khai Google để đối chiếu hợp đồng API; không truy cập
endpoint Gemini hoặc dùng credentials. Các provider/DAO trong test đều mock.

## Kết quả thật đã đọc

Đường dẫn user gõ `D:\miniproject\fpt.build\...` không tồn tại. Artifact thực
tế nằm ở `D:\miniproject\fpt\.build\rag-embedding-probes\phase23-single-embedding-20261011-v1.result.json`.
Timestamp: 2026-10-11 11:35:23 UTC / **18:35:23 giờ Việt Nam**.

| Metric | Kết quả từ artifact cũ |
|---|---|
| Model | gemini-embedding-001 |
| HTTP / latency | 200 / 835 ms |
| POST / retry / metadata GET / chat / DB write | 1 / 0 / 0 / 0 / 0 |
| Expected / returned dimensions | 768 / 3072 |
| vectorValid / errorKind | false / INVALID_EMBEDDING_RESPONSE |
| Token usage | Không có số đo được lưu |
| Estimated cost | 0 USD theo Free Tier quản trị viên xác nhận; không đo billing |

Không coi HTTP 200 là PASS. Validator đã từ chối vector sai chiều trước
normalization. Token usage null không chứng minh provider không trả usage:
runner cũ chỉ đọc usage sau khi vector validation thành công, nên trường này
không được xử lý khi chiều vector sai. Không truy xuất lại API để bổ sung.

## Bằng chứng và ranh giới kết luận

1. `GeminiEmbeddingService.requestBody` tạo model/content và **embedContentConfig**
   chứa taskType, outputDimensionality=768, autoTruncate=false (title cho document).
2. `RagSingleEmbeddingPlan` gọi chính builder này. Preview trước sửa được giữ
   tại `.build/phase23-single-embedding-preview-before-dimension-fix.json`.
3. `Get-RagSingleEmbeddingPayload` đọc preview và kiểm tra đúng cấu hình lồng
   đó. `Invoke-RagSingleEmbeddingProbe` dùng ConvertTo-Json -Compress -Depth 8
   rồi StringContent UTF-8. Không có flattening, override từ environment,
   hoặc middleware chuyển 768 thành 3072. `RagConfig` bắt buộc dimension 768.
4. Vì vậy **768 có trong JSON**, nhưng không có ở cấp trực tiếp của request.
   Property naming camelCase/numeric và encoding không phải lỗi được tìm thấy.
5. [Google REST reference](https://ai.google.dev/api/embeddings) hiện mô tả
   cả cấu hình lồng và các trường trực tiếp taskType/title/outputDimensionality
   (đánh dấu deprecated). Không thể kết luận cấu hình lồng luôn là JSON sai.
6. [SDK Python chính thức của Google](https://github.com/googleapis/python-genai/blob/main/google/genai/models.py),
   hàm `_EmbedContentConfig_to_mldev`, chuyển task/title/dimension thành trường
   **trực tiếp của từng request**. Hàm từ chối auto_truncate ở Developer API.
   Nhánh `_EmbedContentConfig_to_vertex` dùng cấu hình lồng cho EMBED_CONTENT.
   [Embedding guide REST example](https://ai.google.dev/gemini-api/docs/embeddings#output-dimensionality)
   cũng đặt dimension ở cấp trực tiếp (ví dụ hiện dùng Embedding 2; dự án giữ
   model 001); cùng guide nêu 3072 là kích thước mặc định.

**Sai khác được xác nhận:** adapter REST Developer API đã dùng layout khác
mapping chính thức cho backend này; mock tests cũ xác nhận lại layout lồng,
không kiểm tra trường dimension trực tiếp trên JSON gửi đi. Tham số dimension
đã không đạt hiệu lực mong muốn trong lần gọi thật được lưu.

**Nguyên nhân khả dĩ có bằng chứng:** compatibility của layout cấu hình lồng
với model/backend đã dùng dẫn tới output mặc định 3072. Đây là suy luận từ
payload + SDK mapping + kết quả, chưa phải bằng chứng provider bỏ qua field:
không có capture raw request của lần cũ, server trace, hoặc phép thử A/B.
Tài liệu API tổng quát chấp nhận nested config; vì vậy không khẳng định chắc
chắn implementation server tại sao không áp dụng 768. Bản vá sửa khác biệt
wire format rõ ràng; chưa khẳng định nó đã giải quyết provider bằng test thật.

## Bản sửa nhỏ nhất

Giữ model, kích thước vector/database, key source, timeout/retry runtime,
feature flags, chunking và retrieval. Chỉ thay builder bằng payload tương thích
Developer API theo SDK:

```json
{
  "model": "models/gemini-embedding-001",
  "content": {"parts": [{"text": "Nhà nhiều tầng WiFi yếu thì sao?"}]},
  "taskType": "RETRIEVAL_QUERY",
  "outputDimensionality": 768
}
```

Document dùng RETRIEVAL_DOCUMENT và title trực tiếp; query không có title.
Không gửi hai bản dimension có thể xung đột; không gửi autoTruncate mà SDK
Developer API từ chối. Giữ guard input hiện có 2.000 UTF-16 characters, nhưng
không coi guard này là token budget 2.048 hoặc bảo đảm chống truncate cho mọi
input dài. Input test cố định rất ngắn; không gọi countTokens trong audit.

Runner guard kiểm tra số nguyên 768, đúng task/input/model, đúng số field và
chặn preview lồng cũ trước transport. Response vẫn phải đúng 768 numeric,
finite/nonzero và được L2 normalize bằng VectorValues. **Không cắt/pad** 3072
thành 768 để biến response sai thành PASS.

## Kiểm thử đã thực thi

| Lệnh / suite | Kết quả |
|---|---|
| test/prepare-rag-single-embedding.ps1 | PASS compile Java 17 --release 8, payload offline |
| test/rag-single-embedding-tests.ps1 | **24 PASS / 0 FAIL** |
| RagBehaviorTest qua run-rag-phase2-offline.ps1 | **35 PASS / 0 FAIL** |
| RagPhase2CorpusTest | **13 PASS / 0 FAIL** |
| RagQualityTest | **27 PASS / 0 FAIL** |
| test/run-rag-embedding-dry-run.ps1 | PASS offline corpus/request builder |

Tổng **99 automated tests PASS**, 0 provider/database calls. Corpus dry-run
chỉ xây payload cho kế hoạch 10–12 request chưa được duyệt; không thực thi.

New/updated tests parse JSON từ HttpRequestMessage.Content thật trong mock để
kiểm tra numeric root outputDimensionality=768, task/query và absence nested
config/autoTruncate; fixture giả lập trả chiều theo root field để kiểm tra
serialization. Đây là mock contract, không mô phỏng chính xác server Gemini.
Preview cũ/string dimension/3072 configuration/tên field sai hoa thường bị
chặn trước transport.
Java adapter và runner đều được test HTTP 200 + vector 3072 và từ chối sau
đúng một mock POST, không retry vì vector sai; valid 768 vẫn normalize đúng.

Logs/metrics ignored: `.build/phase23-dimension-offline-tests.log`,
`.build/phase23-single-embedding-mocks.json`, payload preview hiện tại và
`.build/phase23-dry-run.json`. Mock outputs không thay thế artifact API thật.

## Files chỉnh sửa

- `src/java/rag/GeminiEmbeddingService.java`: sửa wire request fields.
- `test/rag-single-embedding-helpers.ps1`: kiểm tra payload trực tiếp/numeric.
- `test/rag-single-embedding-tests.ps1`: outgoing serialization và regression 3072.
- `test/rag/RagBehaviorTest.java`: document/query wire contract và 3072 rejection.
- `test/rag/RagSingleEmbeddingPlan.java`: offline preview/contract mới.
- `test/rag/RagEmbeddingDryRun.java`: offline corpus dùng contract mới.
- `docs/rag-phase2.3-single-embedding-plan.md` và
  `docs/rag-phase2.3-single-embedding-execution.md`: notice kết quả thật/superseded plan.
- Báo cáo này.

Không sửa live runner request limit/key source/lock hoặc vector validator.
Không sửa .env.example, model, pricing hay production credentials.

## Dừng và bước tiếp theo

Đã xác minh result/lock thật **không đổi SHA256, kích thước, LastWriteTimeUtc**
sau test; không xóa/reset lock. Approval một POST đã được sử dụng, không tái
sử dụng hoặc phê duyệt tự động. RAG_ENABLED=false/default false được giữ;
test compilation/mocks chạy process RAG_ENABLED=false. Không đọc production.

Phase 2.3 hiện **chưa đạt 768 chiều bằng API thật**. Chỉ sau approval mới mới
lập kế hoạch test riêng; không gỡ khóa hoặc gửi thêm request trong lượt này.
Retrieval/semantic quality chưa được đo với embedding thật. Audit kết thúc.
