# Audit Admin CMS + Safe Demo Admin

Ngày kiểm tra: 11/10/2026. Đây là báo cáo trước triển khai; chưa sửa mã ứng dụng hoặc database cho nhiệm vụ CMS/demo. Các thay đổi phí lắp đặt và artifact đang có trong working tree được giữ nguyên.

## 1. Kiến trúc hiện tại

- Frontend: JSP/JSTL, CSS và JavaScript thuần. Các trang admin dùng header/CSS chung, không phải React/Vue.
- Backend: Java Servlet 4 (`javax.servlet`), URL annotation, Tomcat 9/JRE 17; nguồn Java tương thích release 8. Ant/NetBeans build WAR.
- Persistence: JDBC + PostgreSQL; `SettingsDAO` dùng `ON CONFLICT`. Các bảng được DAO sử dụng: `users`, `packages`, `customers`, `settings`, `email_logs`, lịch sử API key và token reset password. Không tìm thấy migration/schema SQL được Git theo dõi; chưa xác minh mọi constraint của database thực.
- Authentication: local login qua `UserDAO` hoặc Google OAuth; identity/role trong `HttpSession.user`. Google có kiểm tra state. Vai trò hiện được code hỗ trợ: `admin`, `customer`; chưa có demo.
- Authorization: `AdminFilter` cho `/admin/*`, chỉ dispatcher REQUEST mặc định. Servlet admin chủ yếu dựa vào filter.
- Public data: HomeServlet/PackageDetailServlet đọc `PackageDAO`; AIService tạo ProductCatalog từ DAO mỗi lượt chat. Cấu hình AI cache 60 giây và có invalidate sau admin save.
- Deploy: Dockerfile đưa `dist/fpt-sale.war` vào Tomcat ROOT; asset đóng trong WAR. Upload avatar nằm trong exploded webapp, không phải storage bền vững ngoài deployment.
- Biến môi trường đã thấy (chỉ tên): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `AI_ENCRYPTION_KEY`, `RESEND_API_KEY`, `PORT`. Chưa thay biến hay bí mật nào.

## 2. Bảng audit

| Module / trạng thái | Đã có | Thiếu hoặc lỗi | Nguồn dữ liệu | Sửa cần thiết | Ưu tiên |
|---|---|---|---|---|---|
| Authentication/authorization — BROKEN | Login local/Google, session, chặn trực tiếp `/admin/*` | Bypass qua forward MainController; JSP admin truy cập trực tiếp; chưa CSRF/rotate session/kiểm tra khóa account lại | users/session | Bảo vệ REQUEST/FORWARD/ERROR phù hợp, authorization tại backend; bảo vệ JSP; CSRF + POST writes + session rotation | P0 |
| Dashboard — PARTIALLY COMPLETE | Tổng khách/gói/user/email, biểu đồ, khách gần đây | Đọc dữ liệu thật; không thể cấp demo quyền hiện tại; chưa có provider dữ liệu demo | Customer/Package/User/EmailLogDAO | Demo phải dùng fixture tổng hợp trước mọi DAO nhạy cảm | P0/P4 |
| Packages — PARTIALLY COMPLETE | List/search/pagination, add/edit/delete, mã/tên/giá/tốc độ/mô tả/badge; UPDATE giữ ID | Giá/tốc độ chỉ parse số, không kiểm tra dương/range; badge chưa allowlist; GET delete/updateBadge; chưa visibility/order/fee editor; EL output chưa escape đầy đủ | packages; fee getter PackageDTO | Sửa validation/POST/CSRF/escaping; metadata quản lý theo ID ổn định; cùng public read path | P0/P1 |
| Homepage CMS — MISSING | Layout hiện có, carousel 7 ảnh, lợi ích, 4 ô thiết bị, CTA, footer | Tiêu đề/text/ảnh/link/visibility/order hardcode | home.jsp/customer-header.jsp | Editor các section thật đang tồn tại; schema field allowlist, không arbitrary HTML | P1 |
| Contact — BROKEN | Hotline/contact xuất hiện ở nhiều trang | Hardcode nhiều nơi; footer href mailto khác email hiển thị | home/contact/detail/header JSP; ConsultationPolicy.CONTACT; chatbot JS | Snapshot contact chung; URL/phone/email validation và render nhất quán | P2/P3 |
| Standard installation fee — PARTIALLY COMPLETE | Shared fee 300000, hiển thị home/detail, chat phân biệt một lần/cước tháng | Không chỉnh admin được; applicability hiện dựa giá gói | PackageDTO + ProductCatalog | Persist business fee + applicability theo ID/mã ổn định; fallback giữ 300000 khi chưa cấu hình | P1/P3 |
| Mesh — PARTIALLY COMPLETE | F1/F2 equipment, phí lắp và phụ thu hiện có | Lặp trong home/detail/ConsultationPolicy; chưa CRUD | JSP + Java hardcode | Shared validated business snapshot cho UI/chat; không thay các giá trị hiện hành khi triển khai | P2/P3 |
| Promotions — PARTIALLY COMPLETE | Marketing ưu đãi trong hero/CTA/detail | Không thấy PromotionDAO/table/API/calculator hay lịch hiệu lực/association có cấu trúc | JSP marketing copy | Quản lý nội dung ưu đãi hiện có, ngày/visibility/eligible IDs; không tự áp dụng giảm tiền hay invent promotion | P2 |
| Registrations — BROKEN (edit), PARTIALLY COMPLETE (other flows) | List/filter/detail/status/notes/time; public consent/token/idempotency, chỉ success sau insert | Edit customer tạo DTO mới không set packageInterest nhưng DAO ghi NULL vào field; status input chưa allowlist; GET delete; demo chưa chặn public registration | customers/package_interest (text), users relation | Giữ packageInterest khi sửa; validate status theo 4 giá trị hiện có; demo chặn mọi submit/side effect | P0 |
| Users — PARTIALLY COMPLETE | Search/list/lock/unlock/delete/password editor, chặn tự xóa/khóa | GET mutations, thiếu CSRF; null target có thể lỗi; password so sánh/lưu trực tiếp | users | POST/CSRF/validation, password hashing tương thích tài khoản cũ, session invalidation; không demo access | P0 |
| AI admin — PARTIALLY COMPLETE | Saved editable prompt, enable/disable, model/provider, masked key, encrypted key, preview; schema-valid consultation | Không version/rollback prompt; batch save không transaction và nhiều kết quả update bị bỏ qua; SSRF allowLocal quá rộng; demo không được dùng API thật | settings + AIService | Tách business data vs operational settings, bounded prompt validation/revision/rollback, atomic save; real admin only provider/key/prompt | P0/P3/P6 |
| Media — MISSING (CMS), PARTIALLY COMPLETE (avatar) | Bundled static assets; avatar upload max 5MB | Avatar chỉ kiểm tra extension; path cũ không kiểm tra containment; xóa trước DB save; upload vào webapp mất khi redeploy; chưa CMS image uploader | WAR/exploded webapp | Chỉ nhận ảnh decode được, size/dimensions/UUID; storage bền vững, bảo vệ upload; không xóa ảnh đang tham chiếu | P0/P2 |
| SEO — PARTIALLY COMPLETE | title/viewport/favicon, detail title theo gói | Chưa CMS meta description/social title/description/OG image | JSP | Metadata editor theo trang hiện có, context escaping, URL validation | P6 |
| Audit/revision — PARTIALLY COMPLETE | AI config log actor/provider/model, API key history DAO | Chưa general revision cho content/package/contact/fee/prompt, log chưa bền cho CMS | System.out/ApiKeyHistoryDAO | Revision đơn giản actor/type/time, snapshot business allowlist; không key/password/PII | P6 |
| Demo — MISSING | Không có role/router/fixture/guard | Nếu mở quyền filter hiện tại sẽ đọc PII và gọi DAO thật | Không có | Read-only demo, explicit allowed pages, synthetic customers, deny all side effects globally | P4 |

Không đánh dấu module COMPLETE chỉ dựa vào form hoặc nút sửa. Persistence/public propagation sau một write thật chưa được xác minh end-to-end trên database cô lập.

## 3. Bằng chứng quan trọng

1. `src/java/filter/AdminFilter.java:12`: `@WebFilter(urlPatterns = {"/admin/*"})` không khai báo FORWARD. `src/java/controller/MainController.java:16,189,223` nhận `/MainController`/`/main`, chọn route admin và forward. Local fresh unauthenticated HTTP check: `/admin/packages` = 302 về login; `/MainController?action=AdminPackages` = 200; `/view/admin/package-form.jsp` = 200. Chỉ kiểm tra trang gói/public fields; không gọi route khách hàng hoặc action ghi. JSP bypass xác nhận việc truy cập template, không tự kết luận template đó tự đọc database.
2. `AdminPackageServlet.doGet`: `delete`, `updateBadge` gọi DAO write; `AdminUserServlet.doGet`: lock/unlock/delete; `AdminCustomerServlet.doGet`: delete; `AdminEmailLogServlet.doGet`: delete/deleteAll. Không thấy CSRF token trong các flow này. Hộp confirm frontend không bảo vệ HTTP request.
3. `AdminPackageServlet.doPost`: parse giá/tốc độ rồi lưu, không kiểm tra dương; trường min frontend không đủ.
4. `AdminCustomerServlet.doPost(update)` tạo CustomerDTO mới, không đặt packageInterest. `CustomerDAO.java:update` luôn cập nhật package_interest và đặt SQL NULL nếu DTO rỗng. Sửa khách hàng có thể làm mất gói đã đăng ký.
5. `UserDAO.java:checkLoginLocalByIdentifier/registerFull/updatePassword/checkPassword`: raw password dùng trong SQL comparison/persistence. `LoginServlet` giữ session ID sau login, returnUrl chưa allowlist; logged-in session filter chưa kiểm tra account active/current permission lại. Chưa kiểm thử thay đổi tài khoản production.
6. `SettingsDAO.update` từng key trên connection riêng; `AdminSettingsServlet.handleSaveSettings` ghi API key trước validate URL, nhiều update result bỏ qua, chưa rollback một lần lưu thất bại. Masked API key là điểm đã làm đúng; không cho demo đi qua GET settings vì controller đọc config/secrets dù UI che mờ.
7. `AdminSettingsServlet` đặt allowLocal từ provider custom hoặc chuỗi URL chứa localhost/127.0.0.1; `SSRFValidator` return sớm khi allowLocal. Cần cấu hình explicit allowlist server-side và kiểm tra cả test/fetch/preview path.
8. `ProfileServlet.java:113-143`: extension validation, `getRealPath`, write và delete avatar cũ trước xác nhận DB. Chưa có CMS media.
9. `web/view/home.jsp:651`: mailto khác text email. Header/detail/contact và ConsultationPolicy lưu contact tách biệt.
10. `AIService.java:169` dùng PackageDAO mỗi turn; product price/speed hiện đã đi từ cùng nguồn public. Benefit flags suy ra từ description nên cần validator và regression test khi admin chỉnh nội dung.

Có fallback cấu hình bí mật trong source; chưa thay/rotate vì có thể ảnh hưởng tài khoản và dữ liệu mã hóa hiện có. Không ghi giá trị vào báo cáo. Việc loại bỏ fallback phải có kế hoạch chuyển đổi tương thích riêng.

## 4. Đề xuất nhỏ nhất theo kiến trúc hiện có

### P0: vá bảo vệ và các lỗi editor

- Giữ Servlet/JSP/DAO hiện tại. Fix filter dispatcher coverage và kiểm tra permission tại các entry admin; bảo vệ direct JSP ở REQUEST, cho phép forward render từ controller đã được duyệt.
- Tất cả business writes chuyển POST + session CSRF token; GET giữ read-only. Chặn account disabled, session rotation, local-only safe redirects, output escaping theo HTML/attribute/JS context.
- Fix mất package_interest, validate package price/speed/badge/customer status; failure feedback đúng kết quả persist. Không chạy thao tác xóa/sửa thật để test.
- Hash password bằng PBKDF2 của JDK với salt/range cố định; cần quyết định tương thích trước thay authentication: đọc legacy, nâng hash chỉ sau verified login, không đổi password của user khi triển khai. Verify độ dài cột password trong database test trước.

### P1/P2/P3: CMS dùng chung, không dựng lại website

- Tái sử dụng `settings` với namespace `cms.*` và business allowlist; không dùng raw settings map cho public hay demo.
- `SiteContentService` trả typed snapshot gồm section IDs đang tồn tại, contact, fees, Mesh, SEO, promotion marketing metadata, package metadata theo ID (active/order/applicability). Giá/tốc độ và IDs tiếp tục chỉ thuộc `packages`; không sao chép giá vào settings.
- Một trang `/admin/content` chia form theo section; `/admin/business` cho contact/standard fee/Mesh/marketing. Bounded plain text, approved links/images, visible/order; auth endpoints/nav routes không được content editor đổi.
- `SettingsDAO.updateBatch` dùng một transaction, chỉ báo success khi commit. Mọi lưu publish có actor/time/revision. Snapshot đọc mới sau publish; không cache lâu gây stale business data. Public JSP và chatbot dùng cùng typed business reader.
- Reuse package editor cho name/price/speed/descriptions/benefits/badges; thêm metadata active/order/fee policy theo ID, không đổi dữ liệu hiện có mặc định. Đăng ký giữ ID đã xác minh; historical package_interest giữ nguyên, không sửa dữ liệu cũ hàng loạt.
- Khuyến mãi hiện chỉ có marketing: quản lý text/eligibility/date/visibility/associated IDs cho vị trí sẵn có; expired/inactive không publish. Không tạo phép tính miễn giảm mới.
- Gộp Mesh F1/F2 từ giá trị hiện hành vào snapshot, giữ giá trị mặc định nguyên trạng. Chatbot giữ conversation logic, chỉ thay reader factual data/contact và render.

### P4: demo read-only

- Cùng `/login` và HttpSession identity, thêm role `demo_admin` được tạo bởi verifier server-side. Không tin role từ form/query. Không đổi demo thành admin.
- Phương án tối thiểu không sửa users-role constraint: provision demo identifier + salted password hash qua biến môi trường server; mặc định demo tắt. Hash được tạo bởi công cụ CLI JDK; không lưu password/hash thực vào repo/frontend. Không có password công khai/mặc định. Role trong session chỉ sau verifier thành công.
- Permission allowlist: dashboard, package list/edit form, content/business editor preview, synthetic customer list/detail. Mọi dữ liệu demo lấy fixture hoặc public snapshot đã allowlist; không gọi Customer/User/EmailLog/settings-secret DAO từ demo controller path.
- Những trang nhạy cảm `/admin/users`, `/admin/email-logs`, provider/key/model/prompt/history/preview dùng API thật không có quyền demo. Nếu cần demo cấu hình AI thì chỉ màn hình fixture rõ nhãn, không controller production.
- Global demo guard chặn writes/operational calls ở cả URL chính, alias, forward và route public như ContactServlet/profile/password/reset/register/AI calls có side effect. GET action ghi cũng chặn. Không chỉ chặn `/admin/*`.
- Hiện banner `CHẾ ĐỘ DEMO — KHÔNG THAY ĐỔI DỮ LIỆU THẬT`; nút unavailable có lời giải thích. Không giả success. Không interactive editing/reset trong phase này nên không cần thao tác reset database.

### P2/P6: media bền vững và revision

- Không có object storage bền vững được repository cấu hình. Đề xuất ảnh CMS nhỏ lưu trong PostgreSQL hiện có qua bảng `cms_media` (bounded content bytes/type/dimensions/UUID/actor/time), route GET media theo ID; không cần paid provider hay lưu webapp ephemeral. Chỉ real admin upload; validate decoded content/size/dimensions, không SVG/HTML/executable.
- Bảng `cms_revisions` lưu revision business/prompt được allowlist, actor/type/time, hỗ trợ rollback; không lưu API keys/password/customer records. Phải xác định retention/cap cho media/revisions để tránh tăng DB vô hạn.
- Các thay đổi này cần duyệt schema; migration additive, không drop hay cập nhật giá/tốc độ. Không chạy migration production tự động.

## 5. Database và file dự kiến

CMS text/metadata dùng settings hiện có: không bắt buộc thêm cột vào packages/users. Hai bảng mới đề xuất cho persistent media/revision; phải review trước khi áp dụng. Kiểm tra schema users/password/roles/foreign keys ở database test trước authentication transition. Không có schema production đủ để cam kết zero-migration cho mọi constraint.

Files hiện có dự kiến chỉnh theo từng phase:

- `src/java/filter/AdminFilter.java`; `controller/MainController`, `LoginServlet`, `GoogleCallbackServlet`, `DashboardRouterServlet`, `ProfileServlet`, password controllers; `dao/UserDAO`.
- `controller/AdminPackageServlet`, `AdminCustomerServlet`, `AdminUserServlet`, `AdminEmailLogServlet`, `AdminSettingsServlet`, `AdminPlaygroundServlet`; DAO update methods liên quan.
- `dao/SettingsDAO`; `controller/HomeServlet`, `ContactPageServlet`, `PackageDetailServlet`; `dto/PackageDTO`.
- `ai/AIService`, `ai/consultation/ProductCatalog`, `ConsultationPolicy`; provider/model/conversation architecture giữ nguyên.
- JSP admin header/forms/lists/settings; public home/contact/header/detail và chatbot contact fallback JS (chỉ nội dung liên quan CMS).

New files dự kiến: content/business typed service + DTO + validation; permission/CSRF/demo guard; demo verifier/fixture; Content/Business/Media controllers; admin content/business JSP; audit/media DAO; provisioning CLI; additive migration SQL; deterministic servlet/DAO/browser tests. File paths chi tiết chốt theo implementation, không tạo duplicate package price source.

## 6. Kiểm chứng đã thực hiện và phần chưa xác minh

- Đã đọc controllers/DAOs/DTO/filter/JSP/config/test để xác định dataflow và code evidence.
- Đã chạy 3 unauthenticated GET checks trên local Tomcat: kết quả ở mục 3. Không dùng account admin thật, không mở dữ liệu khách, không gửi form/notification/provider API.
- Không chạy `full-project-test.js`, `RealDatabaseSyncTest` hoặc TestController write helpers vì chưa chứng minh môi trường cô lập. Không thực hiện production DB writes.
- Bộ 86 chatbot/registration/provider/frontend tests và responsive installation tests đã chạy thành công ở nhiệm vụ trước; không coi đó là chứng minh CMS/demo đã hoạt động. CMS/demo tests chưa tồn tại hoặc chưa chạy.
- Chưa xác minh admin save→refresh→public end-to-end trên isolated DB; không báo COMPLETE cho những flow chỉ thấy trong code.

Sau duyệt: mock DAO/HTTP/session cho unauthorized/locked/customer/demo/direct alias/FORWARD/JSP routes và CSRF; assert zero production DAO writes/reads PII/notifications/API calls cho demo. Dùng isolated PostgreSQL để kiểm tra CMS transaction/persist/public publish/revision/rollback/package updates; không dùng production URL. Test unsafe URL/upload spoof/size/path traversal, stale cache/promotion expiry, package_interest preservation. Browser desktop/tablet/mobile với synthetic sessions và fixture.

## 7. Điểm cần phê duyệt trước implementation lớn

Theo mục 22 yêu cầu người dùng: “Wait for approval before major architectural or database changes.”

Đề xuất duyệt một phạm vi: P0 security/editor fixes → settings-backed CMS/shared business snapshot → read-only demo dùng verified session và fixture → 2 additive tables media/revisions + password compatibility migration được kiểm tra riêng. Không interactive demo, không paid infrastructure, không production migration execution/deploy/push tự động.

Rollback đề xuất: giữ WAR cũ và business revision trước publish. Schema chỉ additive, giữ tables khi rollback app để không mất dữ liệu; không drop tự động. Password transition cần legacy app compatible với hash trước rollout, nếu không phải rollback bằng bản auth hỗ trợ cả hai formats. Media phải giữ tham chiếu sau rollback. Staging trước production, package prices/speeds baseline được so sánh trước/sau.
