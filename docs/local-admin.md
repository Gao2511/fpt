# Admin đầy đủ trên máy local

Đã tạo PostgreSQL riêng tại `.local/postgres/data`, chỉ nghe `127.0.0.1:15432`, database `fpt_local`, role ứng dụng `fpt_app`. Tomcat riêng nằm ở `.local/tomcat`, chỉ nghe `127.0.0.1:8085`. Tomcat cũ tại 8084 được giữ nguyên.

Đăng nhập: http://localhost:8085/fpt-sale/login, username `local-admin`. Mật khẩu ngẫu nhiên nằm trong `.local/admin-credentials.txt`, được bảo vệ bằng quyền tài khoản Windows hiện tại. Mật khẩu trong bảng users dùng PBKDF2 SHA256 210.000 vòng, không dùng tài khoản demo hay bypass đăng nhập.

Database mới chứa sáu gói mẫu theo dữ liệu đã cung cấp, không sao chép khách hàng, người dùng hoặc cấu hình từ Supabase. Dữ liệu sửa qua CMS local được lưu thật trong database này. AI chưa cấu hình API key và email không có RESEND_API_KEY; cần cấu hình riêng nếu muốn kiểm thử các dịch vụ ngoài.

Chạy tại thư mục dự án:

```powershell
.\start-local.ps1
# Sau khi sửa code: dừng trước, rồi build và khởi động lại
.\stop-local.ps1
.\start-local.ps1 -Build
node test-e2e/local-admin-test.cjs
```

Không dùng `run-web.bat` để chạy database này: script đó sử dụng Tomcat cũ. `start-local.ps1` chỉ cấp biến môi trường cho tiến trình local rồi khôi phục môi trường PowerShell. Nó kiểm tra host, port và tên database trước khi khởi động. DB_SSLMODE=disable chỉ được DBUtils chấp nhận với host loopback; mặc định production vẫn require.

Các biến runtime: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_SSLMODE, AI_ENCRYPTION_KEY, DEMO_ADMIN_ENABLED, RESEND_API_KEY. Giá trị nằm trong `.local/runtime.json` (không đưa vào Git). Toàn bộ `.local/` đã được gitignore.

`database/local/schema.sql` chỉ dùng khởi tạo database local trống, không chạy vào production, không chạy lại trên database đang có dữ liệu. CMS migration đã được áp dụng chỉ vào fpt_local. Các script start/stop không tạo lại database hay đổi mật khẩu.

Kiểm chứng thực tế: build WAR thành công; đăng nhập admin thật, 7 trang quản trị trả HTTP 200; sửa tiêu đề CMS, reload thấy dữ liệu đã lưu, phiên khách mới thấy nội dung sửa; rollback qua CMS thành công và trang khách trở về nội dung cũ. Dừng/khởi động lại bằng script và chạy lại kiểm thử thành công. Bộ test CMS chạy thực tế đạt 67 checks (34 security, 24 behavior, 9 local SSL). Kiểm thử trình duyệt chặn mọi request ngoài origin localhost:8085. Bộ kiểm thử này thực sự tạo và rollback revision local, không dùng mock database.

Dừng bằng stop-local.ps1 giữ nguyên dữ liệu; sao lưu local trước khi xóa thư mục .local. Không cần thay đổi hay rollback Supabase vì không có thao tác nào với database production trong quá trình tạo tài khoản này.
