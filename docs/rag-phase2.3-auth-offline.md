# Phase 2.3 — Audit xác thực HTTP 401, hoàn toàn offline

Ngày 2026-10-11, branch `feature/rag-integration`. **0 API request mới**, không
đọc/ghi database settings, không sửa credential, không truy cập Supabase hoặc
bật RAG. Chỉ thêm helper offline và báo cáo này; runtime/probe live giữ nguyên.

## Kết luận có bằng chứng

Không phát hiện lỗi đọc biến hoặc header trong probe hiện có. Request trước nhận
HTTP 401; nguồn cụ thể của lỗi xác thực phía dịch vụ **chưa xác định** vì body
lỗi không được lưu. Không thể sửa/khẳng định key bị revoke, restrictions hoặc IAM
chỉ bằng kiểm tra local.

| Kiểm tra offline | Kết quả / bằng chứng |
|---|---|
| GEMINI_API_KEY | Có trong Process và User, không trong Machine; Process khớp User ở thời điểm audit |
| Sai định dạng cơ bản | Không có whitespace thừa, CR/LF hoặc quotes bọc; không khớp các placeholder phổ biến; shape được nhận diện, không chứng minh key hợp lệ |
| Header | Construct HttpRequestMessage trong RAM thành công; đúng một x-goog-api-key và giá trị khớp Process; không gửi request |
| GOOGLE_API_KEY | Không có trong Process/User/Machine; probe không đọc nó, không dùng Google SDK nên không áp dụng SDK key precedence |
| RAG_EMBEDDING_API_KEY | Chưa đặt; không phải nguồn key của GET trước |
| Endpoint/method | GET https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001; header x-goog-api-key, không query key/Bearer/redirect |
| Model/dimensions | RagConfig và env/default khớp gemini-embedding-001 / 768; dimension không được gửi trong metadata GET, chưa thể gây lỗi payload embedding |
| Flags | RAG_ENABLED, MODEL_VERIFIED, CALLS_APPROVED đều false/default false |
| Lỗi cũ | .build/phase23-model-access.json: HTTP 401, 1 GET, 354,2 ms; AUTHENTICATION là nhãn do script tự ánh xạ từ HTTP, không phải reason chi tiết từ Google |

Probe live `test/check-rag-model-access.ps1` đọc **Process GEMINI_API_KEY**, chặn
missing/blank trước khi gửi, thêm header nguyên giá trị, không trim/fallback.
Ở lần GET trước key vượt guard và request nhận response 401. Đối chiếu env/User
trong audit mới không chứng minh giá trị key ở lần GET trước giống hệt hiện tại:
probe cũ không lưu dấu định danh key, và audit này không tạo fingerprint secret.

Endpoint/header/model phù hợp tài liệu Google đã kiểm tra trong Phase 2.3:
[Models GET](https://ai.google.dev/api/models),
[API key](https://ai.google.dev/gemini-api/docs/api-key?hl=en),
[Embedding](https://ai.google.dev/gemini-api/docs/embeddings).
Audit này không truy cập mạng, chỉ dùng tài liệu đã kiểm tra trước đó.

## Phân biệt các nguyên nhân

| Khả năng | Mức độ xác nhận |
|---|---|
| Thiếu/blank key | Loại trừ ở Process hiện tại; lần GET trước cũng vượt missing-key guard |
| Key bị cắt/mất khi gắn header | Không phát hiện; offline construct giữ nguyên giá trị |
| Nhầm key/project, key cũ/deleted/disabled | Vẫn có thể; shape đúng không chứng minh còn hiệu lực |
| Restrictions không phù hợp | Chưa xác minh, cần xem API/application restrictions trong Google Console |
| Auth-key service-account binding/IAM | Chưa xác minh; phải kiểm tra với quản trị project |
| GOOGLE_API_KEY ghi đè | Không có bằng chứng; biến không tồn tại và probe không đọc nó |
| Model không được cấp quyền | Chưa chứng minh; 401 không xác nhận model supportedGenerationMethods hoặc quyền embedContent |
| Hết quota | Không có bằng chứng quota; không nhận HTTP 429 hoặc reason chi tiết |
| Lỗi model/768 payload | Chưa có embedding POST; metadata GET không có body/dimension |

Không phân loại restrictions/IAM chỉ theo một HTTP code; chúng có thể cần status,
reason và thông tin cấu hình account để phân biệt chính xác.

## Log chi tiết và hạn chế

Đã kiểm tra `.build/phase23-model-access.json`, `.build/phase23-dry-run.log`,
`docs/rag-phase2.3-model-access.md`, `docs/rag-phase2.3-dry-run.md`.
Không có provider `error.status`, `google.rpc.ErrorInfo` hoặc reason chi tiết.
Probe live cố ý bỏ qua body khi status không 2xx. Java HttpTransport cũng trả
Result(status, empty-body) cho lỗi HTTP. Không có body cũ để parse lại; không thể
khôi phục nguyên nhân cụ thể mà không có dữ liệu mới.

Khuyến nghị cho lần probe **được duyệt sau này**: chỉ lưu status/code/reason đã
kiểm tra theo allowlist, tuyệt đối không raw error.message, request headers, key
hoặc toàn bộ ErrorInfo metadata. Chưa sửa transport/probe live hoặc thực hiện
request để thu thông tin này trong lượt audit.

## Thứ tự chọn key khác nhau cần lưu ý

- Metadata probe: chỉ GEMINI_API_KEY từ Process.
- `tools/java/rag/RagTool.java`: chỉ RAG_EMBEDDING_API_KEY, không tự fallback sang
  GEMINI_API_KEY hoặc GOOGLE_API_KEY. Nếu chạy tool ngay với env hiện tại sẽ
  thiếu key, **đây không phải nguyên nhân HTTP 401 của probe trước**.
- `src/java/rag/RagFactory.java`: RAG_EMBEDDING_API_KEY ưu tiên; nếu thiếu thì key
  Gemini được truyền từ AIService/settings. Khi RAG false thì không khởi tạo.
- `src/java/ai/AIService.java`: lấy key từ settings và giải mã. Chỉ đọc source,
  không gọi DAO/giải mã key hoặc sửa production settings để đối chiếu.

Kết quả key env không chứng minh key trong production database cũng lỗi.

## Bước khắc phục của quản trị viên

1. Trong AI Studio chọn đúng project → API Keys: kiểm tra key dự định dùng còn
   hoạt động/blocked không. Trong Cloud Console → APIs & Services → Credentials:
   xem API restrictions cho Generative Language API, application restrictions có
   hợp với server REST không, và service-account binding/quyền với auth key.
   Đây là kiểm tra account; không tự thay restrictions/key production.
2. Nếu key sai/không còn dùng được, chọn một key test hợp lệ hoặc tự tạo key test
   riêng theo quyền project. Không cần thay key trong website settings. Không
   gửi key vào chat, không ghi vào tracked env file hoặc command history.
3. Đặt key **chỉ trong phiên PowerShell test riêng**, nhập ẩn. Ví dụ dưới đây chỉ
   cấu hình process, không gọi API hoặc tạo credential:

```powershell
# Mở phiên pwsh -NoProfile riêng, không phải process Tomcat/website.
$testSecret = Read-Host 'Gemini key cho test' -AsSecureString
$testCredential = [pscredential]::new('test-only', $testSecret)
$env:GEMINI_API_KEY = $testCredential.GetNetworkCredential().Password
$env:RAG_EMBEDDING_API_KEY = $env:GEMINI_API_KEY
Remove-Item Env:GOOGLE_API_KEY -ErrorAction SilentlyContinue
$env:RAG_ENABLED = 'false'
$env:RAG_ENVIRONMENT = 'development'
$env:RAG_EMBEDDING_MODEL = 'gemini-embedding-001'
$env:RAG_EMBEDDING_DIMENSIONS = '768'
$env:RAG_EMBEDDING_MODEL_VERIFIED = 'false'
$env:RAG_EMBEDDING_CALLS_APPROVED = 'false'
$testSecret.Dispose()
Remove-Variable testSecret, testCredential
# Kiểm tra offline trong chính phiên này:
./test/check-rag-auth-offline.ps1
```

Lệnh trên không dùng setx/User/Machine, không lưu bí mật ra file. Process khác,
bao gồm Codex đang chạy, không tự nhận thay đổi env của terminal này. Test được
duyệt sau đó phải chạy trong đúng phiên/process kế thừa env test, hoặc cấu hình
launcher test riêng. Đóng phiên test khi xong để bỏ các biến tạm.

4. Xác nhận giá/quota model trước embedding test. Sau khi cấu hình xong, cần
   approval mới cho một metadata GET; approval 1 request trước đã sử dụng hết.
   Nếu chưa có thông tin key/account mới, giữ dừng; không retry để đoán nguyên nhân.

## File và xác minh

Thêm `test/check-rag-auth-offline.ps1` và báo cáo này.
Artifact `.build/phase23-auth-offline.json` ignored, chỉ chứa boolean/config
diagnostics, không value/hash/prefix/length key. Helper chỉ construct message
trong RAM; không tạo HttpClient, không SendAsync, không JDBC.

Files inspected: metadata probe, offline/helper output, RagConfig,
GeminiEmbeddingService, RagFactory, AIService, RagTool, .env.example và bốn
log/report ở trên. Không sửa runtime hoặc key selection trong website.

Đã thực thi offline helper exit 0. Syntax check và diff check được thực hiện
trong lượt này; không coi đây là API authentication PASS. Model access, vector
dimensions thực tế và chất lượng retrieval vẫn chờ kiểm thử sau approval.
