# Sửa tri thức chatbot bằng file

System Prompt được quản lý riêng tại `config/chatbot-system-prompt.md`. Khi `AI_SYSTEM_PROMPT_SOURCE=file`, backend đọc nguyên văn file này từ deployment và gửi trong vai trò system cho Gemini. Không đặt System Prompt vào `knowledge/`.

1. Mở `knowledge/chatbot-knowledge.md` bằng VS Code. Dùng **UTF-8**; chỉ dùng `.md` hoặc `.txt`.
2. Đổi nội dung bên dưới dòng `---` thứ hai. Không đặt key, mật khẩu, dữ liệu khách hàng hay chỉ dẫn hệ thống vào file.
3. Khi đổi nội dung/metadata, tăng `version` (1 → 2 → 3). Giữ `id` nếu là cùng tài liệu. Không có hai file cùng `id`.
4. Đặt `active: true` khi nội dung đã được duyệt qua Git. File ví dụ mặc định `false`, chưa được xuất bản.
5. Chạy `./build.bat -f build-portable.xml war`. Build kiểm tra file offline trước khi đóng gói; không gọi Gemini.
6. Commit trên branch làm việc; merge/push main cần phê duyệt vì Render tự deploy. Source branch này không tự thay website đang chạy.
7. Trên môi trường chạy, cấu hình `KB_FILE_AUTO_SYNC=true` và `KB_FILE_SYNC_ACTOR_ID=<ID Admin hiện có>` một lần. ID này chỉ dùng để ghi audit cho deployment Git; không cần duyệt từng tài liệu trên giao diện.
8. Sau deploy, ứng dụng tự đồng bộ các file `active: true`. Có thể xem bảng FILE_MANAGED để theo dõi index hoặc vô hiệu hóa khẩn cấp. Tài liệu chỉ được dùng khi index Gemini hoàn tất và các gate RAG hiện có cho phép.

Để thêm tài liệu, copy file mẫu sang `knowledge/faq/`, `knowledge/policies/` hoặc `knowledge/products/` và chọn một `id` mới. Một file = một chủ đề xác định ở tiêu đề. Tài liệu nhiều chủ đề phải tách thành nhiều file; không bỏ source policy.

Frontmatter chỉ nhận scalar `key: value`, có thể bọc value bằng dấu `"`. Không nhận YAML list/object, tag, alias, key trùng hoặc field cấu hình tùy ý:

| Field | Cách nhập |
|---|---|
| id | Chữ thường, số, `.`, `_`, `-`, tối đa 80 ký tự |
| title | Tiếng Việt, tối đa 200 ký tự; xác định một chủ đề như WiFi/Camera/Mesh |
| category | `guide`, `faq` hoặc `policy` |
| version | Số nguyên dương, tăng khi sửa |
| effective_date | `YYYY-MM-DD` hoặc `none`; mốc đầu ngày UTC |
| expiration_date | `YYYY-MM-DD` hoặc `none`; không còn hiệu lực từ đầu ngày này UTC |
| active | `true` hoặc `false` |
| source_authority | `admin_reviewed` hoặc `official`; không tự xưng official nếu chưa có nguồn được duyệt |
| chunk_size | 200–1200; mặc định 400; worker có thể hạ để phù hợp input model |
| overlap | 0–400 và nhỏ hơn nửa chunk_size; mặc định 40 |
| replaces | Tùy chọn: ID nguồn FILE_MANAGED đã xuất bản trước đó, khi dùng một ID mới |

Thay thế cùng tài liệu: giữ `id`, tăng `version`, chỉnh nội dung. Backend tạo document row mới có liên kết tới bản đang published. Bản cũ giữ hiệu lực khi bản mới chờ index.

Thay thế khác ID/đường dẫn: tạo file mới với `replaces: <id-cũ>`. Nguồn cũ phải từng được đồng bộ/xuất bản và chưa bị sửa authority trái phép. Có thể bỏ file cũ khỏi Git; tham chiếu `replaces` giữ nó đủ điều kiện tạm thời cho tới khi bản mới được kích hoạt. Nếu chủ động đặt file cũ `active: false`, đó là thu hồi ngay, không phải giữ bản dự phòng. Không dùng `replaces` tạo chu kỳ.

Gỡ kiến thức: đổi `active: false` và tăng version, hoặc xóa file rồi deploy. Nguồn bị loại khỏi retrieval trên deployment mới ngay; nút sync lưu trữ các bản trong database, giữ lịch sử. Không xóa dữ liệu PostgreSQL vĩnh viễn.

Sau thao tác **Gỡ xuất bản/Lưu trữ khẩn cấp** trong Admin, nguồn bị khóa ở database và auto-sync không tự bật lại, kể cả version mới. Cần operator kiểm tra lý do và source policy trước khi mở lại; hiện chưa có nút tự reset khóa này.

Giá, tốc độ, quyền lợi gói, khuyến mãi và phí lắp đặt tiếp tục lấy từ business database. `products/` dùng cho hướng dẫn kỹ thuật phù hợp, không phải bản sao bảng giá. File có số tiền hay chỉ dẫn thay system prompt sẽ bị từ chối. Không cần và không được đặt API key trong frontmatter.

Giới hạn: tối đa 500.000 ký tự/file theo `KB_EXTRACTED_MAX_CHARS` hiện có; 10 MB byte theo `KB_UPLOAD_MAX_BYTES`; 200 file và 2 triệu ký tự cho toàn catalog. Vượt giới hạn báo lỗi, không cắt bỏ nội dung. Symlink/UTF-8 lỗi bị từ chối. Giữ ít nhất file mẫu inactive trong thư mục để WAR có catalog hợp lệ.
