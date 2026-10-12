# Phase 2.3 — Metadata PASS và kế hoạch một embedding POST

**Kết quả mới nhất, thay thế trạng thái chờ thực thi bên dưới:** POST đã dùng
hết approval một request, HTTP 200 / 835 ms nhưng trả 3072 chiều; kiểm thử 768
chiều chưa đạt. Không xóa lock hoặc chạy lại. Đã sửa wire payload offline
theo Developer API SDK: taskType/outputDimensionality ở cấp trực tiếp, không
autoTruncate. Các JSON/config lồng bên dưới là lịch sử. Xem báo cáo
`rag-phase2.3-dimension-investigation.md`; không có approval live mới.

Ngày 2026-10-11, branch `feature/rag-integration`. Trong lượt chuẩn bị này:
**0 Gemini API request mới**, không đọc/ghi database, không đổi key, không bật
RAG, không commit/push/deploy. Chỉ đọc JSON đã sanitize và tài liệu công khai
của Google, cập nhật báo cáo và kiểm tra payload offline.

**Cập nhật mới nhất:** quản trị viên đã xác nhận Free Tier, RPM 0/100,
TPM 0/30K, RPD 0/1K của đúng project và duyệt đúng một POST. Runner đã
triển khai, compile offline thành công và 18/18 mock tests PASS. Process Codex
chưa có RAG_EMBEDDING_API_KEY (key nằm ở PowerShell riêng), vì vậy vẫn **0 POST
thật**. Xem `docs/rag-phase2.3-single-embedding-execution.md` để chạy một lần
trong process có key. Các đoạn chờ duyệt bên dưới là lịch sử chuẩn bị trước
xác nhận cuối cùng; manifest hiện approved=true. Kế hoạch 10–12 request vẫn
chưa được duyệt. Không có kết quả vector/latency POST thật để báo cáo.

## 1. Kết quả thật đã xác minh từ JSON

File do người dùng cung cấp:
`.build/rag-auth-probes/20261011T073232469-324add1c9a8146dcbefac5dd6f2f0331.json`.
Thời điểm 2026-10-11 07:32:32 UTC, **14:32:32 giờ Việt Nam**.

| Trường | Kết quả thật |
|---|---|
| HTTP status / latency | **200 / 540,4 ms** |
| requestedModel | gemini-embedding-001 |
| keySource | RAG_EMBEDDING_API_KEY |
| expectedModelNameReturned / modelAccessVerified | true / true |
| supportsEmbedContent | true |
| inputTokenLimit | **2048** |
| outputTokenLimit | 1 — **không phải số chiều embedding** |
| metadataGetAttempts | **1** |
| embeddingPostRequests / chatGenerationRequests | **0 / 0** |
| errorKind / providerStatus / providerReasons | null / null / [] |
| pricingVerified / quotaVerified | **false / false** |

JSON được Git ignore và không chứa key. Người dùng thực thi probe ở terminal
test riêng; Codex chỉ đọc file. GET PASS chứng minh key đó truy cập metadata và
model có method embedContent; chưa chứng minh POST được thực thi, billing/quota
còn đủ hoặc vector trả về 768 chiều. Chưa có embedding model thực chạy thành công.
Lỗi 401 với nguồn key trước đây không đủ bằng chứng xác định nguyên nhân cụ thể;
không sửa lại kết quả cũ thành PASS. Hai artifact ghi nhận tổng hai GET riêng
(401 rồi 200), zero embedding POST.

## 2. Cấu hình embedding và input

Đã kiểm tra `RagConfig`, `GeminiEmbeddingService`, `VectorValues`, `.env.example`:

- Model hiện có giữ nguyên `gemini-embedding-001`, dimension bắt buộc **768**.
- Payload từ `GeminiEmbeddingService.requestBody`: content.parts[].text và
  embedContentConfig với taskType, outputDimensionality=768, autoTruncate=false.
- `embedding.values` phải là 768 số hữu hạn, khác zero. VectorValues thực hiện
  chuẩn hóa L2; không ép/cắt/pad vector sai chiều thành hợp lệ.
- Giới hạn 2.048 input token đã nhận từ metadata thật. Adapter còn có guard
  `text.length() <= 2000`, đây là **UTF-16 characters, không phải token**; guard
  đó không bảo đảm mọi input 2.000 ký tự nằm trong giới hạn token.
- Câu hỏi đầu tiên rất ngắn; không gửi countTokens để đo trước vì budget chỉ
  một POST. autoTruncate=false giúp tránh âm thầm cắt input.

[Embedding guide](https://ai.google.dev/gemini-api/docs/embeddings) còn liệt kê
001 stable, input 2.048 token, output 128–3072 chiều và 768 là kích thước khuyến
nghị; dưới 3072 chiều cần chuẩn hóa thủ công.
[REST reference](https://ai.google.dev/api/embeddings) xác nhận endpoint/config,
response embedding.values và usageMetadata.promptTokenCount. Lưu token count nếu
response thực sự trả; nếu không có thì báo unavailable.

## 3. Payload chính xác cần phê duyệt

Manifest `test/rag/fixtures/phase23-single-embedding-plan.json`,
plan ID `phase23-single-embedding-20261011-v1`, **approved=false**.

```text
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent
```

```json
{
  "model": "models/gemini-embedding-001",
  "content": {"parts": [{"text": "Nhà nhiều tầng WiFi yếu thì sao?"}]},
  "embedContentConfig": {
    "taskType": "RETRIEVAL_QUERY",
    "outputDimensionality": 768,
    "autoTruncate": false
  }
}
```

Chỉ một câu hỏi công khai, không PII, không policy chưa duyệt, không title/chunk
tài liệu. Auth chỉ dùng Process RAG_EMBEDDING_API_KEY đã được quản trị xác nhận
là key test dùng cho GET thành công; không fallback GEMINI_API_KEY hoặc settings.
Metadata artifact không có fingerprint key, nên không tự chứng minh key env mới
và key GET là cùng một key; người quản trị cần xác nhận điều này.

| Budget sau approval mới | Số lượng |
|---|---:|
| Tổng HTTP request tối đa | **1** |
| Embedding POST | **1** |
| Metadata GET mới / retry / countTokens / chat generation | **0 / 0 / 0 / 0** |
| Database write / indexing / retrieval | **0 / 0 / 0** |

**Dừng sau response hoặc lỗi đầu tiên**, kể cả 200/400/401/403/429/5xx/timeout.
Không chuyển sang corpus 3 documents/6 queries, không tiếp tục kế hoạch 10–12
request. Không thay giá gói hoặc tạo dữ liệu trong Supabase/test PostgreSQL.

### Vì sao không chạy ngay RagTool/adapter.document/query

Code hiện tại gọi metadata GET ở embed lần đầu và có tối đa ba attempts.
`accountVerified` chỉ là cache trong Java instance; JSON HTTP 200 từ terminal
khác không tự prime cache này. Nếu chạy trực tiếp worker/query/document, có thể
phát sinh nhiều hơn một request.

Sau approval mới, dùng **runner test-only một POST**, payload lấy từ request
builder hiện có, không đi qua automatic metadata/retry. Kiểm tra response bằng
các quy tắc và VectorValues hiện có; không sửa runtime để bỏ verification hoặc
retry của chatbot. Runner phải đếm attempt trước SendAsync, chặn GET/redirect/
retry, timeout 10 giây và read limit 64 KiB, sanitize lỗi như probe metadata.
Runner live chưa triển khai/chạy trong lượt chuẩn bị này; manifest/payload đã
cụ thể để người dùng review. Trước thực thi sau approval phải kiểm thử guard
runner bằng mocks; không chạy networking test để thử guard.

## 4. Chi phí, quota và xác nhận trong AI Studio

Payload có **32 Unicode code points, 40 UTF-8 bytes**. Ước lượng input **40–64
token** theo heuristic offline `ceil(codePoints/4)+32` đến `codePoints+32`,
không phải số đo tokenizer, không phải cost bound tuyệt đối. Không có retry.

Trong audit trước, [pricing chính thức](https://ai.google.dev/gemini-api/docs/pricing)
được mở nhưng chưa tìm thấy bảng riêng cho 001. Sau đó, quản trị viên xác nhận
bảng giá công khai của **gemini-embedding-001**: Free Tier miễn phí trong quota
được cấp; Paid Tier Standard **0,15 USD / 1 triệu input token**. Ghi nhận xác nhận
này làm mức giá tham chiếu; không áp giá Embedding 2 hoặc Vertex AI.
**Tier và quota còn lại của đúng project vẫn chưa xác nhận.** Xác nhận giá công
khai không xác nhận project đang thuộc Free Tier hoặc còn quota miễn phí.
Không gọi API hoặc mở thêm tài liệu mạng trong lượt cập nhật dự toán này.

| Trường hợp được quản trị xác nhận | Chi phí dự kiến cho một request |
|---|---|
| Model 001 thực sự được miễn phí trong tier/quota áp dụng | **0 USD** theo chính sách đó |
| Paid Standard theo mức tham chiếu quản trị viên cung cấp: 0,15 USD / 1 triệu input token | **0,000006–0,0000096 USD** theo ước lượng 40–64 token |
| Chưa xác nhận tier/quota của project | Chưa thể chốt trường hợp áp dụng; không mặc định miễn phí |

Cách tính Paid: `0,15 × (40..64) / 1.000.000 USD`. Đây là dự toán điều kiện từ
mức tham chiếu công khai và token heuristic, chưa phải phí thực thu hoặc số đo
provider. Chi phí thực tế phải theo token usage và billing áp dụng; timeout có
thể không xác định việc tính phí, không tự retry hoặc khẳng định 0 chi phí khi
không nhận response.

[Rate limits](https://ai.google.dev/gemini-api/docs/rate-limits) áp dụng theo
project/model/tier, gồm RPM, input TPM và RPD; xem active limits trong AI Studio.
GET thành công không xác nhận embedding quota. Không dùng con số quota cũ hoặc
quota batch làm quota cho một synchronous embedContent.

Người quản trị cần kiểm tra đúng project trong AI Studio:

1. Project/tier/billing status gắn với key test; model 001 còn được phép gọi
   synchronous embedContent bằng key này. Không bật billing hoặc upgrade tự động.
2. Giá công khai riêng của 001 đã được quản trị viên xác nhận như trên; kiểm tra
   tier/billing thực tế của project để xác định trường hợp nào áp dụng.
3. Active RPM/TPM/RPD và usage còn lại: đủ ít nhất một synchronous request và
   input ngắn này; kiểm tra credit/spend limit nếu project có áp dụng.
4. Xác nhận chỉ gửi câu ở mục 3, dùng cùng key test; không gửi secret hoặc thông
   tin khách hàng. Có thể cung cấp tier/đơn giá/quota không nhạy cảm qua chat.
5. Sau các xác nhận trên, phê duyệt **một POST duy nhất**. Approval GET trước đã
   hoàn tất, không bao gồm embedding; cả hai manifest hiện vẫn approved=false.

**Trạng thái mới nhất:** người dùng yêu cầu chưa POST, chờ xác nhận quota và
approval cuối cùng. Manifest giữ approved=false, pricingVerified=false (chưa
xác nhận billing áp dụng cho project), quotaVerified=false, projectTierVerified=false.
Input, 768 chiều, budget một POST và mọi giới hạn an toàn giữ nguyên.

## 5. Kết quả cần đo ở POST sau approval

HTTP status/provider codes/message đã sanitize; số HTTP attempts; elapsed time;
dimension thực trả; mọi phần tử numeric/finite; vector norm trước/sau L2; token
usage nếu có. Không in/lưu key/header/raw error. Không dump vector hoặc toàn
response ra log; chỉ lưu telemetry đã kiểm tra trong ignored .build/.

HTTP 200 chỉ PASS khi vector đúng 768 và validation đầy đủ. Một query embedding
không chứng minh semantic retrieval, source selection, conflict handling hoặc
correct-answer rate. Những mục đó cần một kế hoạch nhiều vector + isolated
PostgreSQL được duyệt riêng. Admin KB integration vẫn chờ các bước này.

## 6. File và kiểm tra offline của lượt này

- Cập nhật `docs/rag-phase2.3-dry-run.md`, `docs/rag-phase2.3-model-access.md`,
  `docs/rag-phase2.3-auth-recovery.md` với metadata HTTP 200; giữ snapshot 401.
- Thêm báo cáo này, manifest single embedding và
  `test/rag/RagSingleEmbeddingPlan.java` (offline only).
- Offline validator đọc evidence HTTP 200, xác nhận budget 1 và tạo payload bằng
  requestBody hiện có. Transport ném lỗi nếu có ý định HTTP; không đọc key/DAO.
- Payload preview `.build/phase23-single-embedding-preview.json` được ignore;
  log `.build/phase23-single-embedding-plan.log` ghi kết quả compile/validation.

Java 17 compile --release 8 và offline validator đã chạy thành công, exit 0.
Không chạy lại full regression vì không sửa production Java/frontend.
Không sửa .env.example hoặc đặt flags modelVerified/callsApproved/RAG_ENABLED.
Repository mặc định RAG_ENABLED=false; process Codex cũng false/default false.
Không truy cập production để xác nhận trạng thái deploy ở đó.
