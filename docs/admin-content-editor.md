# Giao diện CMS theo khu vực và tải ảnh trực tiếp

Hai trang `/admin/content` và `/admin/business` sử dụng theme tối/cam, header và kiểu nút của Sale Manager. Nội dung website chia 8 khu vực; kinh doanh chia 4 khu vực. Menu bên trái mở đúng nhóm chỉnh sửa; từng nhóm có mô tả vị trí hiển thị và có thể thu gọn. Form hai cột trên desktop, một cột trên mobile. Lịch sử xuất bản nằm trong nhóm thu gọn riêng.

Ảnh banner/dashboard khách hàng, logo, thiết bị, lợi ích và ảnh chia sẻ dùng ô tải PNG/JPEG với xem trước và nút bỏ ảnh. Không cần nhập hoặc sao chép URL. Giá trị đường dẫn cũ được giữ trong trường hidden để bảo toàn nội dung đang dùng. Tải ảnh dùng endpoint `/admin/media` hiện có, thêm phản hồi JSON khi client yêu cầu `Accept: application/json`; luồng HTML cũ vẫn hoạt động.

Chỉ cập nhật giá trị ảnh sau khi máy chủ xác nhận lưu thành công. Khi upload lỗi, giữ ảnh cũ; khóa xuất bản trong lúc tải. Preview bản nháp dùng object URL trên trình duyệt, không mở quyền truy cập công khai cho ảnh chưa xuất bản. Upload có timeout 30 giây. CSRF và quyền admin, xác minh nội dung PNG/JPEG, tối đa 2 MB/4096px, quota kho ảnh 20 MB tiếp tục dùng logic hiện có. Demo hiển thị nhóm và dữ liệu mẫu nhưng không tải ảnh hoặc xuất bản.

Không thay đổi tên khóa CMS, giá trị mặc định, chính sách phí/ưu đãi, giá gói, logic đăng ký hoặc cơ chế revision/rollback. Không cần migration mới. Các file chính:

- `src/java/cms/EditorLayout.java`: nhóm trường cho giao diện.
- `src/java/controller/AdminContentServlet.java`, `src/java/cms/DemoData.java`: cung cấp nhóm cho admin thật/demo.
- `src/java/controller/MediaServlet.java`: phản hồi upload JSON tùy chọn.
- `web/view/admin/content.jsp`, `web/assets/admin/admin-content.css`, `web/js/admin-content.js`: giao diện, xem trước và upload.
- `web/view/admin/admin-header.jsp`: icon và trạng thái active cho hai trang.
- `test/cms/EditorLayoutTest.java`, `test/run-cms-tests.ps1`, `test-e2e/cms-browser-test.cjs`, `test-e2e/local-admin-test.cjs`: kiểm thử nhóm trường và workflow upload.

Kiểm thử đã thực thi:

- Build `dist/fpt-sale-cms.war`: thành công.
- `test/run-cms-tests.ps1 -Browser`: 34 security + 24 behavior + 9 local SSL checks đạt, kiểm tra toàn bộ trường được nhóm đúng và không trùng/mất trường đạt; 15 nhóm kiểm tra HTTP/browser đạt, bao gồm draft riêng tư, xuất bản ảnh, upload giả bị từ chối, upload lỗi giữ nguyên lựa chọn trước đó, demo không có quyền ghi, bố cục 1440/768/375px không tràn ngang.
- `test/compile-cms-jsps.ps1`: 17 JSP, 0 lỗi (còn cảnh báo scan JAR PostgreSQL có tên chứa khoảng trắng từ cấu hình cũ).
- `test-e2e/local-admin-test.cjs`: đăng nhập thật, 7 trang admin, lưu cả văn bản và ảnh qua PostgreSQL local, kiểm tra ảnh trước/sau xuất bản, rollback khôi phục văn bản/ảnh gốc và ảnh nháp lại không công khai: đạt.
- Đã xem screenshot desktop/mobile và ô upload trên bản local.

Xem bản chạy: http://localhost:8085/fpt-sale/admin/content và http://localhost:8085/fpt-sale/admin/business. Chỉ cập nhật máy chủ local riêng; chưa commit, push hoặc triển khai production.

Khi triển khai sau này: dùng WAR đã kiểm thử, giữ migration/kho ảnh hiện có, kiểm tra upload/lưu bằng admin trên staging trước. Rollback bằng artifact trước đó; dữ liệu CMS và ảnh vẫn tương thích vì schema và khóa không đổi. Ảnh tải lên nhưng chưa xuất bản vẫn chiếm quota kho ảnh theo cơ chế hiện có; dọn ảnh nháp là công việc riêng.
