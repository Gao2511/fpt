# Phase 2.3 — Single approved embedding request

**Trạng thái mới nhất (2026-10-11):** request đã thực thi một lần: HTTP 200,
835 ms, vector 3072 thay vì 768, INVALID_EMBEDDING_RESPONSE. Approval cũ đã
được sử dụng; không chạy lại lệnh bên dưới. Result và request lock được giữ
nguyên. Bản sửa payload chỉ được kiểm thử offline; chưa có approval/request
live mới. Xem `rag-phase2.3-dimension-investigation.md`.

Phần mô tả chuẩn bị và payload lồng bên dưới là lịch sử trước lần chạy thật;
payload hiện tại dùng taskType/outputDimensionality trực tiếp và không gửi
autoTruncate cho Gemini Developer API.

Ngày 2026-10-11, branch `feature/rag-integration`. Chỉ một POST cho
`gemini-embedding-001`, input `Nhà nhiều tầng WiFi yếu thì sao?`, output 768
chiều, RETRIEVAL_QUERY, autoTruncate=false. Dùng request builder hiện có;
không chạy adapter live vì adapter tự gọi metadata và có retries.

## Approval và chi phí

Quản trị viên đã xác nhận đúng project: Free Tier, RPM 0/100, TPM 0/30K,
RPD 0/1K và duyệt **đúng một POST**. Đây là thông tin quản trị viên cung cấp,
không phải quota/billing đo bằng API. Dự toán **0 USD trong Free Tier/quota**;
40–64 input tokens là ước lượng offline, chưa phải số đo. Không gọi countTokens.
Metadata HTTP 200 trước đó không phải kết quả embedding.

## Kết quả thực tế của lượt chuẩn bị

- Java 17 compile `--release 8` và payload preview offline: PASS.
- `test/rag-single-embedding-tests.ps1`: **18 PASS / 0 FAIL**, đã thực thi.
- Real HTTP requests trong lượt này: **0**; chưa có status, chiều vector,
  latency, token usage hoặc lỗi provider của POST thật.
- RAG_EMBEDDING_API_KEY không có trong process Codex; key ở terminal khác
  không được process đang chạy tự kế thừa. Không đọc key từ settings/global key.
- RAG_ENABLED của process Codex không được đặt (runtime mặc định false).
  Runner yêu cầu literal false trước HTTP. Không thay cấu hình production.
- Real request lock và real result chưa tồn tại khi kiểm tra.
- Không database access, GET bổ sung, Chat API, commit, push hoặc deploy.

Mock tests kiểm tra vector 768/normalization bằng Java VectorValues hiện có,
sai chiều, zero/nonfinite/string values, malformed/empty/missing responses,
token usage có/không có, 401 sanitization, 429/503/redirect một attempt,
exception không lộ private text, và guard chống claim lần hai.
Các metrics/mock vectors không được coi là kết quả Gemini thật.

## Thực thi trong đúng PowerShell đã cấu hình key test

Offline preparation đã chạy và classes có trong workspace. Không nhập key vào
chat, file hay command argument. Trong cửa sổ có biến RAG_EMBEDDING_API_KEY:

```powershell
$env:RAG_ENVIRONMENT = 'development'
$env:RAG_ENABLED = 'false'
pwsh -NoProfile -File 'D:\miniproject\fpt\test\run-rag-single-embedding.ps1' -ApprovedSingleEmbeddingPost
```

Runner chỉ đọc key process đó. Payload cố định, một SendAsync, không redirects,
không retry/GET/DB/chat, timeout 10 giây, response bounded 64 KiB. Input không
chứa dữ liệu khách hàng. Vector chỉ tồn tại trong memory/stdin Java validator,
không lưu vào database/file/log. Chỉ lưu metrics và mã lỗi allowlist/message
sanitized; không lưu key/header/raw response. Latency gồm HTTP/body read, không
gồm Java validation. Token usage chỉ ghi nếu response thực sự cung cấp.

Output sanitized:
`.build/rag-embedding-probes/phase23-single-embedding-20261011-v1.result.json`.
HTTP 200 chỉ hợp lệ khi vector numeric/finite/nonzero và đúng 768 chiều.

Guard atomic CreateNew lưu persistent request lock trước SendAsync:
`.build/rag-embedding-probes/phase23-single-embedding-20261011-v1.request.lock`.
**Không chạy lại hoặc xóa lock**, kể cả timeout/lỗi. Lock có thể được claim
trước khi request hoàn tất; nếu process crash, không biết chắc provider đã
nhận request chưa. Giữ lock và dừng để không vượt budget. Không tự chuyển sang
10–12 requests. Khi user cung cấp result, chỉ đọc result và cập nhật báo cáo.

## File của lượt này

- `test/rag/fixtures/phase23-single-embedding-plan.json`: ghi approval/tier/quota
  từ xác nhận người quản trị, giữ budget một POST.
- `test/rag/RagSingleEmbeddingPlan.java`: offline builder chấp nhận manifest
  approved; không HTTP/key/DAO.
- `test/rag/RagEmbeddingVectorCheck.java`: validate/normalize từ stdin và chỉ
  trả metrics allowlist.
- `test/prepare-rag-single-embedding.ps1`: compile/build preview offline.
- `test/rag-single-embedding-helpers.ps1`: payload guards, sanitized metrics,
  Java validation, injected single-attempt transport.
- `test/run-rag-single-embedding.ps1`: runner opt-in có persistent guard.
- `test/rag-single-embedding-tests.ps1`: mocks/local Java tests, không live HTTP.
- Báo cáo này và notice mới nhất trong single-embedding-plan.

Chưa đo retrieval relevance, source selection hoặc chất lượng câu trả lời;
một vector query không chứng minh toàn bộ RAG. Chưa sửa runtime chatbot/CMS.
