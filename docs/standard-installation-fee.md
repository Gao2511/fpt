# Phí lắp đặt Internet thông thường

## Nguồn dữ liệu và phạm vi

`src/java/dto/PackageDTO.java`: `STANDARD_INSTALLATION_FEE = 300000L` và getter `getStandardInstallationFee()`. Phí một lần, trước ưu đãi; không cộng vào giá tháng. Sáu gói thường được nhận diện theo mức giá của danh mục hiện tại (195/205/220/230/239/249 nghìn) và phải có tốc độ Internet. Nếu sau này thay mức giá/định danh gói cần cập nhật phạm vi này. Không thêm cột database hay thay bản ghi.

DAO hiện không ánh xạ trường phí tương đương; không có bộ tính phí sau ưu đãi trong luồng này. Các nội dung ưu đãi/miễn giảm hiện có được giữ nguyên. Phí chuẩn không phải xác nhận số tiền cuối cùng khách phải trả.

## Các file thay đổi

- `src/java/dto/PackageDTO.java`: nguồn phí dùng chung cho JSP và chatbot.
- `src/java/ai/consultation/ProductCatalog.java`: snapshot và JSON dữ liệu thêm phí chuẩn, tần suất một lần trước ưu đãi.
- `src/java/ai/consultation/ConsultationPolicy.java`: trả lời phí bằng dữ liệu máy chủ, phân biệt cước tháng; không tự xác nhận miễn giảm; giữ Mesh riêng. Điều chỉnh câu thiếu dữ liệu để nói về phí thực tế sau ưu đãi.
- `src/java/ai/consultation/ConsultationEngine.java`: xử lý phí đã xác nhận cả khi admin dùng prompt tùy chỉnh hoặc provider lỗi.
- `web/WEB-INF/tags/installation-fee.tag`: component phí dùng chung, tự xuống dòng, chỉ hiện khi gói thuộc phạm vi.
- `web/view/home.jsp`: dòng phí trên từng thẻ và chú thích chung dưới bảng giá (bảng giá hiện là phần này của trang chủ).
- `web/view/customer/package-detail.jsp`: phí và chú thích trước nút tư vấn/đăng ký. Form chưa có bảng tóm tắt chi phí nên không thêm tổng tiền hoặc bước thanh toán.
- `test/ai/ConsultationBehaviorTest.java`, `test/ai/AdaptiveConversationTest.java`: đổi kỳ vọng phí chuẩn đã biết; ưu đãi và thông tin chưa xác nhận vẫn được kiểm thử.
- `test/ai/InstallationFeeTest.java`, `test/run-chat-tests.ps1`: kiểm thử phí sáu gói, JSON provider, prompt tùy chỉnh, một lần/cước tháng, giữ lựa chọn, ưu đãi chưa xác nhận, Mesh và dịch vụ khác.
- `test-e2e/installation-fee-test.cjs`: kiểm tra giao diện thật trên local Tomcat; không gửi form.

System Prompt không bị viết lại hay ghi đè cấu hình admin. Quy tắc hiện tại “Không tự suy ra phí … Khi thiếu dữ liệu, nói rõ” vẫn đúng: phí mới là dữ liệu đã xác nhận và được máy chủ hiển thị. Provider/model, giá, tốc độ, quyền lợi, Mesh và logic đăng ký không thay đổi. Không cần biến môi trường mới.

## Kiểm thử thực tế

- Offline: 26 consultation + 21 adaptive + 17 installation + 9 registration + 7 provider + 6 frontend = 86 test thành công (mock provider/database/email).
- Chrome headless trên `http://localhost:8084/fpt-sale`: 1440/768/375px; trang chủ và chi tiết cả 6 gói mỗi kích thước. Đủ dòng 300.000đ/lần, không cắt chữ/che nút, không tràn ngang; ô đồng ý đăng ký vẫn tồn tại.
- Jasper biên dịch home/detail và tag: 0 lỗi; có cảnh báo tên JAR PostgreSQL chứa khoảng trắng từ trước.
- Ant build thành công: `dist/fpt-sale-installation.war` (artifact local riêng, chưa deploy).

## Chạy lại / phát hành an toàn

```powershell
.\test\run-chat-tests.ps1
node test-e2e/installation-fee-test.cjs
.\build.bat '-Ddist.war=D:/miniproject/fpt/dist/fpt-sale-installation.war' dist
```

Browser test cần local Tomcat đang chạy bản build mới và Chrome đã cài. Có thể đổi URL bằng `FPT_TEST_BASE_URL` (chỉ dành cho test). Offline provider fixture cần quyền loopback.

Chưa commit/push/deploy. Sau khi được duyệt phát hành: lưu WAR production cũ, build đúng artifact `dist/fpt-sale.war` theo pipeline hiện tại (giải phóng khóa file local nếu cần), kiểm tra staging và các câu hỏi phí/đăng ký/Mesh rồi mới deploy. Rollback bằng WAR trước đó; không cần rollback database hay prompt admin vì không thay các dữ liệu đó. Không dùng WAR cũ trong Docker để phát hành thay đổi này.
