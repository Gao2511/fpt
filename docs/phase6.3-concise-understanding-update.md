# Phase 6.3 — Concise Understanding Update

## Mục tiêu

Khắc phục các phản hồi dài dòng, hỏi lại dữ liệu đã có và hiểu thiếu ý khách trong chuỗi hội thoại thực tế do quản trị viên cung cấp. Thay đổi được giữ trong kiến trúc `ConsultationEngine` hiện tại; không gọi API thật, không thay đổi database và không bật RAG.

## Bằng chứng và nguyên nhân

Chuỗi hội thoại mẫu cho thấy bốn nguyên nhân chính:

1. Bộ nhớ chỉ nhận phủ định đầy đủ `không`, nên `k lấy camera` có thể bị hiểu thành nhu cầu camera.
2. Dạng nhà `nhà cấp 4` và `1 trệt 2 lầu` chưa được chuẩn hóa thành số tầng, khiến chatbot hỏi lại.
3. Câu trả lời phương án lặp thông tin, tự nối ưu đãi trả trước và luôn đặt câu hỏi tiếp theo dù khách đã hỏi rõ.
4. Một số intent dùng biểu thức quá rộng: `hỗ trợ` có thể khớp nhầm `ở trọ`; câu hỏi camera chưa ưu tiên gói đã chọn.

## Thay đổi

- Nhận `k`, `ko`, `hong`, `khum` là phủ định khi ngữ cảnh rõ cho camera, truyền hình, Mesh và trả trước.
- Nhận `nhà cấp 4` là một tầng; nhận `1 trệt 2 lầu` là ba tầng.
- Ghi nhớ loại nhà, nhà thuê và vị trí camera trong trạng thái có kiểu dữ liệu rõ ràng.
- Bổ sung intent hỗ trợ WiFi, kinh doanh, nhà thuê, chuyển địa chỉ và ngừng dịch vụ.
- Rút gọn luồng khám phá: mỗi lượt chỉ hỏi một thông tin còn thiếu có ích; xác nhận số tầng, số người và quyền lợi ảnh hưởng trực tiếp đến lựa chọn.
- Rút gọn phần đề xuất và đăng ký; không tự động nhắc trả trước khi khách chỉ phản đối phí lắp.
- Không suy đoán tên khách. Chỉ dùng tên khi khách tự giới thiệu rõ.
- Với `259K`, giữ cách biểu diễn minh bạch là Gói 239K + Mesh F2, không tạo gói 259K độc lập.
- Khi hỏi camera, ưu tiên trả lời theo gói đang chọn. Câu khẳng định nhu cầu `có truyền hình và camera` vẫn được xử lý như yêu cầu tư vấn.
- Giữ phí Mesh là tổng phí lắp của phương án, không cộng lại 300.000đ phí Internet thông thường.

## Dữ liệu từ tài liệu cung cấp

Các dữ kiện trùng khớp nguồn nghiệp vụ hiện có được dùng để kiểm thử: sáu gói Internet, F1/F2, phí lắp thông thường và ưu đãi trả trước đã được xác nhận trong cấu hình máy chủ.

Các câu sau không được nhập thành sự thật vì chưa có bằng chứng hệ thống hoặc gây áp lực bán hàng:

- Tự nhận đã chuyển thông tin cho Phúc hoặc sẽ liên hệ lại.
- Cam kết đăng ký chỉ mất 1–2 phút.
- Cam kết chất lượng tuyệt đối như ổn định 24/7.
- Tự động đề nghị trả trước 6/12 tháng ở mọi lượt.
- Địa danh không nhất quán như xem Quy Nhơn là thuộc Gia Lai.

## Kiểm thử thực tế

- `test/run-phase63-offline.ps1`: 13/13 đạt, gồm chuỗi hội thoại trong ảnh, phủ định viết tắt, nhà cấp 4, 1 trệt 2 lầu, đăng ký 259K, phản đối phí và so sánh nhà mạng.
- `test/run-phase62-offline.ps1`: 14/14 đạt.
- `test/run-phase61-offline.ps1`: 10/10 đạt.
- `test/run-phase6-offline.ps1`: 14/14 đạt.
- `test/run-chat-tests.ps1`: 97/97 đạt, gồm backend, session, provider payload, lỗi provider, đăng ký và frontend rendering.

Tất cả dùng provider mock hoặc xử lý máy chủ. Gemini API calls: 0. Database writes: 0.

## Giới hạn

Kết quả production vẫn phụ thuộc System Prompt đang được chọn ở Admin và cấu hình provider. Các quy tắc cốt lõi đã được thêm vào protocol bắt buộc của máy chủ để áp dụng cả khi Admin dùng prompt tùy chỉnh. Bản sửa này chưa được commit, push hoặc deploy.
