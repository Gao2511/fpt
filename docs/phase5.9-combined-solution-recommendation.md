# Phase 5.9 — Combined Package + Mesh Recommendation

Ngày kiểm tra: 2026-10-13

Branch: `feature/rag-integration`

Chế độ: local/offline; không gọi Gemini, không kết nối Supabase production, không deploy.

## 1. Kết quả audit và nguyên nhân gốc

Luồng cũ chọn gói Internet và tư vấn Mesh ở hai nhánh độc lập trong
`ConsultationPolicy`:

- `choose(...)` chỉ chọn gói Internet rẻ nhất đáp ứng ngân sách/quyền lợi.
- Các nhánh `2 tầng`, `3 tầng`, `F1`, `F2` trả về nội dung Mesh riêng và yêu cầu
  khách cung cấp thêm thông tin để chọn gói Internet.
- Không có DTO biểu diễn một giải pháp gồm gói Internet, phủ sóng và dịch vụ tùy
  chọn; do đó cũng không có nơi tính tổng tiền phía server.
- Giá Mesh từng bị lặp trực tiếp trong Java, sau đó được thay chuỗi bằng cấu hình
  CMS. Cách này dễ tạo sai lệch và không thể chứng minh số tiền nào đã tham gia
  phép cộng.
- Khi system prompt được lấy từ file, một số câu hỏi nghiệp vụ có thể đi qua
  provider trước khi backend thực hiện quyết định kết hợp. Vì vậy prompt tốt hơn
  vẫn không bảo đảm tổng giá đúng.
- `CustomerRequirements` đã nhớ số tầng/người/thiết bị và nhu cầu chung nhưng
  chưa nhớ số camera, xem phim, sử dụng kinh doanh hoặc lựa chọn từ chối Mesh.
- RAG hiện được tách khỏi quyết định giá. Đây là đúng về source authority: package
  database/CMS quyết định giá và quyền lợi; tài liệu RAG không được ghi đè giá.
- Frontend chỉ hiển thị trường `reply` bằng `textContent` với `white-space:
  pre-wrap`. Vì vậy định dạng nhiều dòng của giải pháp kết hợp hiển thị được và
  không cần thay framework hoặc dùng HTML không an toàn.

## 2. File thay đổi

- `src/java/ai/consultation/RecommendedSolution.java` — mô hình giải pháp hoàn
  chỉnh và phép cộng tiền an toàn.
- `src/java/ai/consultation/CustomerRequirements.java` — nhớ thêm số camera,
  streaming, nhu cầu kinh doanh và lựa chọn Mesh có chủ ý.
- `src/java/ai/consultation/ConsultationPolicy.java` — chọn một giải pháp chính,
  lấy giá từ nguồn có thẩm quyền, tính tổng và render phản hồi.
- `src/java/ai/consultation/ConsultationEngine.java` — áp dụng quyết định kết hợp
  phía server cho mọi chế độ system prompt trước khi gọi provider.
- `test/ai/CombinedSolutionRecommendationTest.java` — 6 tình huống demo và các
  kiểm thử giá/không bịa giá.
- `test/cms/CmsBehaviorTest.java` — cập nhật kỳ vọng: hàm thay số liên hệ không
  còn được phép sửa các con số thương mại bằng thay chuỗi.
- `docs/phase5.9-combined-solution-recommendation.md` — báo cáo này.

Không thay đổi schema database, frontend, khóa API, RAG flag hoặc migration.

## 3. Logic mới

`RecommendedSolution` chứa:

- gói Internet đã chọn;
- lựa chọn phủ sóng Mesh F1/F2;
- dịch vụ tùy chọn đã ghi nhận;
- cước Internet hàng tháng;
- phụ thu Mesh hàng tháng;
- phí lắp Internet và phí lắp Mesh một lần;
- tổng hàng tháng và tổng phí một lần;
- lý do, mức độ hoàn thiện của dữ liệu và source ID nội bộ.

Nguồn dữ liệu:

- Giá/tốc độ/quyền lợi gói: `ProductCatalog`, được tạo từ package database.
- Giá và thiết bị Mesh: `catalog.business`, tức cấu hình CMS đã được validate.
- Phí lắp Internet: phí của package; nếu package kế thừa thì dùng
  `business.standardFee`.
- Lời khách hàng và tài liệu hội thoại không được dùng làm nguồn giá.

Phép cộng dùng số nguyên `long` phía server và kiểm tra tràn số. Nếu thiếu bất kỳ
thành phần giá Mesh nào, hệ thống không tạo tổng giả mà báo phần giá đó chưa được
xác nhận.

Quy tắc lựa chọn ưu tiên một phương án:

- Nhà 2 tầng: kết hợp gói phù hợp với Mesh F1 nếu khách không từ chối Mesh.
- Nhà từ 3 tầng: kết hợp gói phù hợp với Mesh F2 nếu khách không từ chối Mesh.
- Nhu cầu từ 5 người, xem phim nhiều, gaming, nhiều thiết bị hoặc từ 2 camera:
  ưu tiên từ nhóm tốc độ 500 Mbps trong danh mục hiện hành.
- Nhu cầu từ 8 người, từ 4 camera, từ 12 thiết bị hoặc mục đích kinh doanh: ưu
  tiên từ nhóm tốc độ 1000 Mbps trong danh mục hiện hành.
- Các ngưỡng trên chỉ dùng để xếp hạng phương án; phản hồi không cam kết tốc độ,
  ping, vùng phủ hoặc số thiết bị thực tế.
- Nếu khách nêu rõ một gói, backend giữ gói đó làm thành phần Internet của phương
  án. Nếu khách yêu cầu so sánh, luồng so sánh hiện có được giữ nguyên.
- Nếu khách nói chỉ cần Internet/không cần Mesh, lựa chọn này được nhớ và Mesh
  không bị ép thêm.
- Chỉ một `recommendedPackageId` được trả về. Không có danh sách nhiều gói hoặc
  phương án thay thế khi không cần thiết.

## 4. Cách hiển thị và tổng giá

Ví dụ với dữ liệu kiểm thử có chủ ý khác giá mặc định:

```text
Dạ với nhà 3 tầng, em ưu tiên phương án Gói 195K + Mesh WiFi F2.

- Internet: 195.000đ/tháng — 300Mbps
- Phủ sóng: Mesh WiFi F2 (...) — +29.000đ/tháng
- Tổng hàng tháng: 224.000đ/tháng
- Phí lắp Internet thông thường: 300.000đ/lần
- Phí lắp Mesh: 830.000đ/lần
- Tổng phí một lần: 1.130.000đ
```

Các giá 29.000đ và 830.000đ trong kiểm thử được nạp qua cấu hình CMS giả lập,
không nằm trong logic production. Điều này chứng minh phép cộng dùng dữ liệu có
thẩm quyền thay vì số ví dụ 20.000đ/700.000đ.

Cước hàng tháng và phí một lần luôn nằm ở các dòng riêng. Hệ thống cũng ghi rõ
phí một lần là mức thông thường và chỉ áp dụng miễn giảm khi cấu hình hiện hành
xác nhận.

## 5. Kết quả 6 tình huống demo

| Tình huống | Kết quả offline |
|---|---|
| Nhà 3 tầng | PASS — một phương án sơ bộ gói Internet + F2, có đúng một câu hỏi thêm về số người/thiết bị |
| Nhà 3 tầng, 8 người | PASS — một gói 1000 Mbps + F2; không liệt kê các gói khác |
| Nhà 2 tầng, 5 người, xem phim nhiều | PASS — một gói 500 Mbps + F1 |
| Nhà 3 tầng, 8 người, 5 camera | PASS — kết hợp Internet + F2 + lưu ý camera; không bịa giá camera |
| Valorant, nhà 3 tầng | PASS — Internet + F2, khuyến nghị LAN và không hứa “không lag” |
| Chỉ muốn Internet, không cần Mesh | PASS — chỉ gói Internet, không có Mesh |

Kiểm thử theo chuỗi `3 tầng → 8 người → 5 camera` xác nhận bộ nhớ làm phương án
thay đổi theo dữ liệu mới. Một kiểm thử bổ sung xác nhận thông tin số tầng đã nhớ
không làm câu “Cảm ơn em” bị trả lại bảng giá.

## 6. Kiểm thử đã chạy

### Build/compile

```text
.\build.bat -f build-portable.xml compile
BUILD SUCCESSFUL
```

### Focused Phase 5.9 tests

```text
CombinedSolutionRecommendationTest: 10 passed
```

Bao phủ: 6 demo case, tổng cước tháng, tổng phí một lần, giá cấu hình không mặc
định, thiếu giá Mesh, không bịa tổng, không lặp gói, không package spam, không
chiếm nhầm lượt hội thoại xã giao và câu hỏi bổ sung khi chưa biết số tầng.

### WAR build

```text
.\build.bat -f build-portable.xml war
BUILD SUCCESSFUL
Validated repository knowledge files: 17; API calls=0
```

File WAR sinh ra chỉ dùng để xác minh build và đã được hoàn nguyên khỏi working
tree. Không chạy full historical suite theo phạm vi Phase 5.9.

## 7. Trạng thái RAG và an toàn

- Không có Gemini request thật.
- Không có kết nối hoặc ghi Supabase production.
- Không thay đổi `RAG_ENABLED`.
- Tổng giá không đi qua Gemini và không lấy từ RAG text.
- Phản hồi deterministic vẫn đi qua `AIChatServlet` dưới dạng đúng một chuỗi
  `reply`; frontend dùng `textContent`, nên không lộ DTO, source ID hoặc HTML.
- RAG vẫn có thể cung cấp hướng dẫn trong các câu hỏi tài liệu độc lập. Luồng
  combined hiện dùng hướng dẫn bảo thủ phía server để tài liệu không thể ghi đè
  giá/cấu hình thương mại.

## 8. Hạn chế còn lại

- Ngưỡng xếp hạng theo người/thiết bị/camera đang là rule Java có kiểm thử, chưa
  có giao diện Admin để cấu hình hoặc version hóa.
- CMS lưu giá và thiết bị Mesh nhưng quan hệ F1 cho 2 tầng/F2 cho từ 3 tầng vẫn là
  rule backend hiện hành.
- Chưa có giá add-on cho camera riêng trong nguồn structured, nên hệ thống chỉ nêu
  nhu cầu kiểm tra và không cộng giá camera.
- Chưa chạy UAT với dữ liệu package thật trên cloud/Tomcat production trong bước
  offline này.
- Không tạo card “Recommended Solution” riêng; giao diện hiện tại hiển thị rõ nội
  dung nhiều dòng. Một card riêng chỉ nên làm nếu có yêu cầu thiết kế UI sau này.

## 9. Kết luận

**COMBINED SOLUTION RECOMMENDATION IMPLEMENTED (OFFLINE VERIFIED).**

Backend hiện tạo một phương án `Internet package + Mesh add-on`, tính tổng bằng
dữ liệu package/CMS và giữ phí hàng tháng tách khỏi phí một lần. Chưa commit,
push, merge hoặc deploy.
