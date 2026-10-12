# Phase 2.3 — Kết quả single metadata GET

**Cập nhật mới nhất:** probe bằng key test riêng sau đó trả HTTP 200,
modelAccessVerified=true, supportsEmbedContent=true, inputTokenLimit=2048,
540,4 ms, 1 GET, 0 embedding POST. File đã được Codex đọc xác minh:
`.build/rag-auth-probes/20261011T073232469-324add1c9a8146dcbefac5dd6f2f0331.json`.
Xem `docs/rag-phase2.3-single-embedding-plan.md` cho bước tiếp theo chưa duyệt.
Phần HTTP 401 dưới đây là snapshot lịch sử, không phải kết quả hiện tại của key test.

Người dùng duyệt đúng 1 metadata GET, không retry, không embedding hoặc DB write.
Thực hiện 2026-10-11 06:41:17 UTC (13:41:17 giờ Việt Nam), branch
`feature/rag-integration`. Đã dùng key hiện có trong `GEMINI_API_KEY` của process;
không hiển thị, lưu mới, rotate hoặc sửa credential.

| Chỉ số thực đo | Kết quả |
|---|---|
| Endpoint | GET /v1beta/models/gemini-embedding-001 trên generativelanguage.googleapis.com |
| Request metadata thật | **1** |
| Retry / redirect | **0 / 0** |
| HTTP status | **401** |
| Phân loại | AUTHENTICATION |
| Latency client, bao gồm nhận response | **354,2 ms** |
| Model metadata/access | Chưa xác minh, không có metadata hợp lệ |
| Embedding POST / chat generation | **0 / 0** |
| Vector thực trả / token usage | Không có / không có |
| Giá/quota/free-tier | Chưa xác nhận |
| Database writes | **0** |

**Dừng sau request duy nhất.** Không gọi key khác, không thử endpoint khác,
không bật flags MODEL_VERIFIED/CALLS_APPROVED, không chạy corpus embedding.
HTTP 401 chứng minh request không được xác thực; không đủ để kết luận key bị
revoke/leak, hết quota, thiếu model hoặc adapter embedding sai. Không lưu raw
provider error hoặc headers; vì vậy báo cáo không suy đoán nguyên nhân chi tiết.
Model cấu hình vẫn 001, nhưng **chưa có model embedding nào thực thi thành công**.

Đây là probe REST metadata trực tiếp bằng test-only HttpClient, không phải lần
chạy toàn pipeline Java adapter/DAO. Quota, 768 chiều thực trả và chất lượng
semantic vẫn chưa kiểm chứng; dry-run trước đó không thay thế các kết quả này.

## Hành động của người quản trị

1. Mở [Google AI Studio](https://aistudio.google.com/), chọn đúng project và kiểm
   tra trạng thái key dự định dùng với Gemini, quyền API và restrictions. Không
   gửi key vào chat. [Hướng dẫn Google](https://ai.google.dev/gemini-api/docs/troubleshooting)
   khuyến nghị kiểm tra setup khi gặp lỗi key và xem trạng thái key trong Studio.
   [Tài liệu API key hiện hành](https://ai.google.dev/gemini-api/docs/api-key?hl=en)
   nêu auth key gắn với service account và unrestricted standard key bị từ chối.
   Trong Cloud Console → APIs & Services → Credentials, kiểm tra API restrictions
   cho Generative Language API và application restrictions có phù hợp request
   backend không; với auth key, kiểm tra service-account binding. Đây là danh
   sách cần kiểm tra, chưa xác định mục nào gây HTTP 401. Chưa thay restriction
   hoặc quyền của key dùng chung với production.
2. Kiểm tra riêng trên máy rằng `GEMINI_API_KEY` trong process test chứa key đúng
   của project đó, không phải giá trị mẫu/credential dịch vụ khác. Key trong
   settings website chưa được dùng ở probe này; không suy ra chatbot production
   cũng lỗi 401 từ kết quả này. Không yêu cầu thay production credentials.
3. Nếu cần cập nhật cấu hình test, người quản trị tự đặt biến process bằng cơ chế
   nhập bí mật nội bộ, không đưa giá trị vào command history, tracked files hoặc
   output. Không tạo/rotate key tự động; không xóa fallback hiện có.
4. Xác nhận giá/quota riêng của 001 trước embedding test. Sau khi sửa cấu hình,
   cần duyệt **một request metadata mới**; approval 1 GET vừa rồi đã dùng hết.
   Chưa có approval cho POST hoặc kế hoạch trần 12 request.

## File và kiểm tra

- `test/check-rag-model-access.ps1`: opt-in switch, header key từ process,
  timeout 10 giây, response tối đa 64 KiB, đúng một SendAsync, không retry hoặc
  redirect. Xuất allowlist status/latency/boolean/limits, không raw lỗi.
- `.build/phase23-model-access.json`: kết quả an toàn ở trên, được Git ignore.
- `docs/rag-phase2.3-dry-run.md`: cập nhật counter/status sau approval.
- `docs/rag-phase2.3-model-access.md`: báo cáo này.

PowerShell syntax check **PASS**; guard khi thiếu approval switch **PASS**, không
HTTP. Probe opt-in thực chạy trả exit **1** vì HTTP 401; không báo test thành công.
Không sửa runtime website, database, model chat, giá gói hoặc encryption key.
Repository vẫn mặc định `RAG_ENABLED=false`; không kiểm tra/chỉnh production,
không commit, push hoặc deploy.
