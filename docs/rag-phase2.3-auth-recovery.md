# Phase 2.3 — Chuẩn bị authentication recovery

2026-10-11, branch `feature/rag-integration`. **Cập nhật sau người dùng thực thi:**
probe key test trả HTTP 200 / 540,4 ms, đúng 1 GET, hỗ trợ embedContent,
inputTokenLimit=2048, 0 embedding POST. Codex đã đọc JSON an toàn, không gọi API
thêm. HTTP 401 cũ là lịch sử; pricing/quota vẫn chưa xác nhận. Xem kế hoạch mới
`docs/rag-phase2.3-single-embedding-plan.md`, chỉ 1 POST chờ duyệt.
Không đọc/ghi database settings, không đổi key chatbot, không bật RAG,
không tạo embedding, không commit/push/deploy.

## Key selection đã xác minh

| Thành phần | Nguồn key |
|---|---|
| `GeminiEmbeddingService` | Nhận String apiKey qua constructor; không tự đọc env/SDK |
| Java operator `tools/java/rag/RagTool.java` | `RAG_EMBEDDING_API_KEY` |
| Website `RagFactory` | RAG_EMBEDDING_API_KEY, nếu thiếu thì key Gemini được AIService truyền từ settings; **giữ nguyên** |
| Metadata probe mới | **Chỉ RAG_EMBEDDING_API_KEY của Process**; thiếu/blank/whitespace lỗi thì dừng trước HTTP |
| GOOGLE_API_KEY / GEMINI_API_KEY | Probe mới không đọc, không fallback; không dùng key cũ của process để thay key test |

Key trong variable test phải do người quản trị cung cấp/xác nhận là key thử
nghiệm. Không thể suy ra quyền hoặc nguồn gốc của key chỉ từ biến môi trường.
Việc audit trước xác nhận GEMINI_API_KEY đọc đúng không làm biến này trở thành
key test cho probe mới. Nếu website chạy với env riêng, không đặt biến test vào
launcher Tomcat/production.

## Probe đã chuẩn bị và thực chạy trong PowerShell test

`test/check-rag-model-access.ps1` vẫn bắt buộc switch approval; yêu cầu process
development/staging và RAG không bật. Endpoint duy nhất:

```text
GET https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001
```

Header x-goog-api-key chỉ nằm trong RAM/request. Một lần SendAsync, timeout
10 giây, giới hạn response 64 KiB, không retry/redirect/fallback. Không GET
endpoint khác, POST, generateContent, JDBC hoặc automatic index. Kết thúc ngay
sau response/error. Không tự chuyển sang kế hoạch 10–12 request hoặc chỉnh
MODEL_VERIFIED/CALLS_APPROVED dù GET thành công.

JSON mới được ghi vào `.build/rag-auth-probes/<timestamp>-<random-id>.json`,
ignored; giữ nguyên `.build/phase23-model-access.json` chứa HTTP 401 trước đó.

## Error diagnostics an toàn

`test/rag-auth-diagnostics.ps1` cung cấp helpers test-only. Error body chỉ được
parse trong RAM, không ghi nguyên văn. JSON/log chỉ lưu:

- HTTP status thực tế, latency và số attempt.
- Numeric error code trong miền HTTP hợp lệ.
- Provider status thuộc allowlist, ví dụ UNAUTHENTICATED/PERMISSION_DENIED.
- ErrorInfo reason thuộc allowlist, ví dụ API_KEY_INVALID, restriction-IP/service,
  IAM_PERMISSION_DENIED, SERVICE_DISABLED hoặc QUOTA_EXCEEDED.
- Thông báo tiếng Việt cố định sinh từ reason/status đã nhận diện, không sao chép
  provider error.message. Nhiều reasons thì giữ các mã riêng, không đoán nguyên
  nhân chính. Unknown reason/status không được xuất ra.

Không log raw message, ErrorInfo metadata/domain, project ID, email, URL, request
headers, key hoặc exception text. Nếu JSON lỗi/empty/oversized hoặc provider
không trả ErrorInfo, giữ HTTP status và thông báo tổng quát; không suy ra key
sai chỉ từ HTTP 401. Dữ liệu chi tiết của lỗi cũ không thể khôi phục.

Java adapter/runtime error transport vẫn giữ nguyên trong lượt này; cải thiện
diagnostics nằm trong probe kiểm tra xác thực riêng. Không thêm persistence hoặc
logging mới cho chatbot production.

## File thay đổi

- `test/check-rag-model-access.ps1`: key test bắt buộc, approval/environment gate,
  đọc response lỗi để sanitize, output riêng từng lần.
- `test/rag-auth-diagnostics.ps1`: sanitizer allowlist và hàm probe một invocation,
  hỗ trợ injected mock transport.
- `test/check-rag-auth-offline.ps1`: chẩn đoán header/ready của biến test, không
  dùng GEMINI_API_KEY khi construct header; vẫn chỉ xuất boolean, không gửi HTTP.
- `test/rag-auth-recovery-tests.ps1`: kiểm thử hành vi với key/response giả.
- `docs/rag-phase2.3-auth-recovery.md`: hướng dẫn và kết quả này.

Các báo cáo cũ là snapshot lịch sử. Quy tắc probe dùng GEMINI_API_KEY trong
snapshot cũ đã được thay bằng key test bắt buộc ở lượt này.

## Kiểm thử thực tế

Đã chạy `./test/rag-auth-recovery-tests.ps1`: **25 PASS / 0 FAIL**, exit 0,
mock-only. Lần đầu harness mock có lỗi scope, đã sửa và chạy lại thành công;
không gọi API trong bất kỳ lần chạy test nào. Các nhóm kiểm tra:

- Guard khi chưa approval; không fallback key global; key whitespace/CRLF bị chặn.
- Correct metadata/model/method, wrong model và malformed success.
- 401 giữ HTTP/provider code; 403 phân biệt restriction/IAM nếu provider trả reason.
- Unknown strings/raw message/metadata chứa fake secret không lọt ra output.
- Empty/malformed/oversized error bodies vẫn giữ HTTP status an toàn.
- Đọc body UTF-8 có byte limit 64 KiB và cancellation deadline; HTTP headers
  được nhận trước body để không mất status khi body quá lớn hoặc đọc body lỗi.
- 429/503/302/transport exception chỉ một mock invocation, không retry.
- JSON mock đã persist không chứa sentinel giả lập secret/project/email/URL.

Log ignored `.build/phase23-auth-recovery-tests.log`; mock telemetry ignored
`.build/phase23-auth-mock-tests.json`, có `mockOnly=true`, `realApiRequests=0`.
Không coi mock HTTP 200 là model-access thật hoặc relevance PASS.

PowerShell parse checks và git diff check được chạy trong lượt này. Không chạy
Java/full website regression vì không sửa Java/frontend hoặc cấu hình runtime.

## Người quản trị cấu hình test riêng

Trong một PowerShell test riêng, nhập key ẩn; không gửi key vào chat, không dùng
setx hoặc lưu vào tracked file. Đoạn này **chỉ đặt env, không gọi API**:

```powershell
$testSecret = Read-Host 'Gemini API key thử nghiệm' -AsSecureString
$testCredential = [pscredential]::new('test-only', $testSecret)
$env:RAG_EMBEDDING_API_KEY = $testCredential.GetNetworkCredential().Password
$testSecret.Dispose()
Remove-Variable testSecret, testCredential
$env:RAG_ENVIRONMENT = 'development'
$env:RAG_ENABLED = 'false'
$env:RAG_EMBEDDING_MODEL = 'gemini-embedding-001'
$env:RAG_EMBEDDING_DIMENSIONS = '768'
$env:RAG_EMBEDDING_MODEL_VERIFIED = 'false'
$env:RAG_EMBEDDING_CALLS_APPROVED = 'false'
./test/check-rag-auth-offline.ps1
```

Không đổi GEMINI_API_KEY/GOOGLE_API_KEY hoặc key trong settings. Biến chỉ có hiệu
lực trong phiên đó và child processes; Codex đang chạy không tự nhận env của
terminal khác. Nếu muốn Codex thực thi, phải cấu hình env test cho process/launcher
test mà Codex có thể chạy; không đưa key qua chat. Khi đóng phiên, env tạm hết hiệu lực.

Sau khi key test sẵn sàng, người dùng phải xác nhận approval mới cho **đúng một
metadata GET**. Chỉ khi đó mới chạy probe opt-in trong process test đã cấu hình.
Không có authorization ở lượt này cho request mới. Mọi kết quả thật sau đó phải
báo HTTP status/provider codes đã sanitize rồi dừng, kể cả 200/401/403/429.
