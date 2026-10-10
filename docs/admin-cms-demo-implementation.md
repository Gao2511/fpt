# Admin CMS và Demo Admin — bàn giao triển khai

Ngày: 11/10/2026. Phạm vi đã được duyệt: sửa editor và phân quyền hiện có, CMS trên `settings`, snapshot kinh doanh dùng chung, demo chỉ đọc, media bền vững và revision. Giữ Java Servlet/JSP/JDBC/PostgreSQL/Tomcat 9, không thêm thư viện hoặc nhà cung cấp trả phí.

Báo cáo trước sửa: [admin-cms-demo-audit.md](D:/miniproject/fpt/docs/admin-cms-demo-audit.md). Các kết quả dưới đây được kiểm tra bằng dữ liệu giả. Chưa áp dụng SQL vào database thật, chưa commit/push/deploy.

## 1. Các thay đổi chính

| Module | Kết quả |
|---|---|
| Nội dung website | `/admin/content`: tiêu đề/phụ đề/mô tả/CTA Hero, bảy banner, nền/nhãn tùy chọn, logo, menu label/link/order/visibility, lợi ích/icon hoặc ảnh, thiết bị/ảnh/mô tả/order/visibility, footer, tiêu đề liên hệ và SEO trang chủ. Giá trị chưa cấu hình giữ nội dung hiện có. |
| Thông tin kinh doanh | `/admin/business`: hotline, email, địa chỉ, Zalo/social, phí Internet chuẩn, Mesh F1/F2/phí/phụ thu/thiết bị/visibility, nội dung ưu đãi/điều kiện/ngày/ID gói. |
| Gói cước | Giữ bảng `packages` và ID hiện có. Thêm active/order/fee trong `settings` theo ID, save cùng transaction với gói và audit. Cho phép kế thừa phí chuẩn hoặc đặt phí riêng. Validate giá/tốc độ/phí/độ dài/badge phía server. |
| Khách đăng ký | Giữ workflow hiện có. Sửa lỗi editor làm mất `package_interest`; chỉ nhận bốn status hiện hành. POST và CSRF cho thao tác quản trị. Không tạo đăng ký thật khi thử demo. |
| AI | Public website và chatbot cùng đọc `PackageDAO` + snapshot `cms.*`. Phí/mesh/contact/promotion dùng dữ liệu đã xuất bản; giữ logic hội thoại/consent/registration. Cấu hình AI được lưu nguyên tử, prompt có revision và rollback; editor vẫn sửa và xóa được. |
| Phân quyền | Kiểm tra session admin đang active và xác minh lại UserDAO. Bảo vệ REQUEST/FORWARD/ERROR, direct JSP, alias MainController và đường dẫn mã hóa. Thao tác ghi cần POST + CSRF. UTF-8 được đặt trước khi parse form. |
| Media | Decode/re-encode PNG/JPEG, tối đa 2 MB/ảnh, cạnh tối đa 4096, tổng pixel tối đa 16 triệu; UUID, kho 20 MB. Lưu BYTEA vào PostgreSQL hiện có. Avatar mới cũng dùng kho này; không xóa ảnh cũ khi update thất bại. |
| Theo dõi thay đổi | Actor/type/time và snapshot allowlist. Content/business/prompt khôi phục bản gần nhất với kiểm tra xung đột; khôi phục cũng tạo revision mới. Package save có audit, không có nút tự động rollback gói. API key/password/PII không được thêm vào revision. |

Mặc định giữ sáu giá 195/205/220/230/239/249 nghìn, tốc độ 300/300/500/500/1000/1000 Mbps, phí Internet 300.000đ/lần, Mesh F1 500.000đ + 10.000đ/tháng và F2 700.000đ + 20.000đ/tháng. Không có thao tác cập nhật những dữ liệu này vào database trong quá trình làm việc. Thay đổi giá trong tests chỉ diễn ra trong fixture.

Ưu đãi chỉ là nội dung tư vấn được admin xuất bản, không tự giảm tiền hoặc xác nhận khách đủ điều kiện. Inactive/expired không hiển thị. Camera/TV/equipment vẫn được quản lý qua description/longDescription hiện có, không tạo bảng gói thứ hai. Những route đăng nhập, tài khoản và đơn hàng không được CMS thay đổi.

## 2. API và quyền truy cập

| Route | GET | POST |
|---|---|---|
| `/admin/content`, `/admin/business` | Admin: form hiện tại + lịch sử; demo: fixture | Admin + CSRF: validate/save/publish hoặc rollback gần nhất |
| `/admin/packages` | Danh sách/edit/add; demo: dữ liệu giả | Admin + CSRF: CRUD hiện có, metadata/fee; demo 403 |
| `/admin/customers`, `/admin/customer-detail` | Admin: dữ liệu được phép; demo: khách giả | Admin + CSRF: workflow quản trị hiện có; demo 403 |
| `/admin/settings`, `/admin/ai-playground` | Chỉ admin thật; demo 403 | Chỉ admin thật + CSRF; không gọi provider trong demo |
| `/admin/users`, `/admin/email-logs` | Chỉ admin thật; demo 403 | Chỉ admin thật + CSRF |
| `/admin/media` | Không cung cấp trình liệt kê ảnh | Admin + CSRF: upload PNG/JPEG đã decode |
| `/media?id=UUID` | Chỉ ảnh đã được tham chiếu từ CMS/avatar; draft 404; demo 403 | Không có thao tác upload công khai |

GET delete/lock/unlock/updateBadge/deleteAll/deleteMultiple không còn thực hiện write; cần POST. JavaScript chuyển các link thao tác hiện có sang form POST có CSRF, giữ confirmation. Không đủ quyền: 403; chưa login vào admin: 302 về `/login`; direct JSP admin của admin thật: 403. Oversized form trong admin trả 413.

Public content được đọc mới theo request; trang chủ dùng `no-store`. Product catalog được đọc mới theo lượt chat. Save/rollback business/prompt gọi `AIService.invalidateCache()`; không xóa lịch sử hội thoại khách.

## 3. Demo chỉ đọc và cách cấp tài khoản

Demo dùng `/login` hiện có, backend xác thực identifier và PBKDF2 hash do người vận hành cấp qua môi trường; sau thành công mới tạo `HttpSession.user` role `demo_admin`. Không chấp nhận role/tenant do browser gửi. Session ID được đổi sau login; hết hạn sau 30 phút không hoạt động. Demo mặc định tắt.

Các biến mới, chỉ tên:

- `DEMO_ADMIN_ENABLED`
- `DEMO_ADMIN_IDENTIFIER`
- `DEMO_ADMIN_PASSWORD_HASH`
- `AI_LOCAL_BASE_URL` — chỉ cần cho provider nội bộ do quản trị máy chủ cho phép chính xác.

Các biến môi trường hiện có liên quan, chỉ tên: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `AI_ENCRYPTION_KEY`, `RESEND_API_KEY`, `PORT`.

Provision:

1. Người vận hành tự chọn identifier và mật khẩu dài, riêng cho demo. Không lưu vào Git, JSP, ảnh chụp hoặc task report.
2. Sau build, mở terminal tương tác và chạy `java -cp build/web/WEB-INF/classes security.Passwords`. CLI đọc mật khẩu ẩn, yêu cầu ít nhất 12 ký tự, chỉ xuất salted hash để người vận hành đưa vào secret configuration của server. Không dùng console output này làm tài liệu công khai.
3. Cấu hình ba biến demo ở runtime, bật demo có chủ đích, restart ứng dụng trong staging rồi kiểm tra `/login`. Không cần INSERT tài khoản demo vào `users` hay đổi constraint role của database.
4. Login bằng identifier/mật khẩu đã chọn. Trang hiện `CHẾ ĐỘ DEMO — KHÔNG THAY ĐỔI DỮ LIỆU THẬT`, badge Demo, nút lưu bị khóa.
5. Muốn thu hồi: tắt demo và restart/invalidate session hiện có. Chỉ đổi biến enable chưa tự thu hồi các session đã được cấp trước đó.

Quyền chính xác: GET dashboard, package list/add/edit form, customer list/edit/detail giả, content/business preview, static JS/CSS/ảnh đóng trong ứng dụng và logout/login. Demo không xem users, email logs, key/model/prompt/provider config, conversation history, avatar thật hoặc public media. Các yêu cầu khác mặc định bị từ chối, kể cả POST public ContactServlet/profile/password/reset/register/ai-chat, alias `/main`, `/MainController` và URL mã hóa.

`DemoData` tạo fixture mới mỗi request, không phụ thuộc DAO. Tên/địa chỉ/email khách là dữ liệu giả, không có số điện thoại khách thật. Không có simulated save hoặc reset database. Tests chứng minh toàn bộ các trang và request demo thử nghiệm không tăng bộ đếm write hoặc private DAO read.

## 4. Schema, media và retention

Migration: [001-cms-media-revisions.sql](D:/miniproject/fpt/database/migrations/001-cms-media-revisions.sql), chưa chạy. Chỉ tạo `cms_media`, `cms_revisions` và index trong transaction; không DROP, không đổi cột/gói/khách/ưu đãi.

Trước staging phải kiểm tra schema hiện có: `settings.setting_key` unique; `setting_value` đủ dài cho prompt/CMS; `updated_at`; bảng package và foreign keys; password column đủ chứa hash 83 ký tự (khuyến nghị ít nhất 128). `CREATE IF NOT EXISTS` không chứng minh bảng trùng tên sẵn có có schema tương thích. Cấp quyền tối thiểu SELECT/INSERT/UPDATE/DELETE cần thiết cho ứng dụng, không cấp DDL cho request web.

Upload trả `/media?id=...`; admin sao chép vào trường ảnh rồi publish. URL draft chưa public; media GET chỉ trả bytes khi có reference, kèm nosniff/CSP. Không phụ thuộc exploded WAR cho ảnh mới. Ảnh static và avatar cũ không được tự chuyển hoặc xóa. Sao lưu thư mục upload cũ riêng trước redeploy nếu đang được sử dụng.

Giới hạn tổng media 20 MB bao gồm avatar mới. Khi đầy, upload bị từ chối và không xóa ảnh đang dùng. Chỉ cho phép bảo trì orphan sau khi kiểm tra toàn bộ reference hiện tại và revision/backup cần rollback; không có job xóa tự động.

UI hiển thị 20 revision gần nhất; table không tự hết hạn. Khuyến nghị vận hành lưu revision 90 ngày sau khi backup và giữ baseline rollback gần nhất; thực hiện cleanup ở staging trước, chỉ chạy production sau duyệt riêng. Không cung cấp lệnh xóa hàng loạt tự động.

## 5. Kiểm thử đã chạy

| Lệnh/suite | Kết quả thực tế |
|---|---|
| Ant build WAR CMS | BUILD SUCCESSFUL; artifact `dist/fpt-sale-cms.war` |
| `test/run-cms-tests.ps1 -Browser` | CmsSecurityTest: 34; CmsBehaviorTest: 24; tất cả passed. Browser chạy thành công 13 nhóm kiểm chứng, gồm ba viewport. |
| `test/run-chat-tests.ps1` | Consultation 26, Adaptive 21, Installation fee 17, Registration 9, Provider 7, frontend 6: 86 passed |
| `node test-e2e/admin-prompt-browser-test.cjs` | 3 passed: nhập multiline, nạp mẫu, xóa/sửa prompt |
| `test/compile-cms-jsps.ps1` | 17 JSP compile, 0 errors |
| Kiểm tra ZIP/WAR | Có class/asset CMS mới, JSP home khớp source cuối, không chứa fixture server/test DAO; không có entry hỏng |

CMS tests chạy code DAO thật với JDBC fixture không có network socket; lỗi publish được inject để kiểm tra atomic rollback. Browser dùng Tomcat loopback, login demo thật qua LoginServlet, identity admin chỉ từ fixture test, Chrome headless và chặn external browser requests. Không chạy các script RealDatabaseSync/full-project/TestController có khả năng đọc/ghi DB hoặc gửi email thật.

Các hành vi kiểm chứng: save UTF-8 → fresh read → public render; rollback/stale conflict và undo rollback; allowlisted URLs; promotion expiry; package ID/price/speed/active propagation sang catalog; fee inheritance và phí riêng tách khỏi cước tháng; Mesh replacement không sửa nhầm giá gói; giữ package_interest và status; decoded media lưu qua JDBC; spoof/size rejection; revision không chứa API key; CSRF; unauth/locked/customer/demo/direct/FORWARD/ERROR/encoded URL isolation.

Browser kiểm tra dashboard/module demo ở 1440/768/375px; phí/hotline/public monthly prices và homepage overflow ở cả ba kích thước; upload draft/public; backend từ chối giá âm; form prompt vẫn sửa được. Đã xem ảnh chụp thực tế tại `%TEMP%/fpt-cms-qa` (admin mobile và demo desktop).

Jasper có cảnh báo scan JAR PostgreSQL sẵn có vì tên chứa khoảng trắng; kết quả compile vẫn 0 errors. Một số lượt build bị Windows khóa WAR tạm thời; retry thành công và artifact cuối đã được kiểm tra. Các lỗi UTF-8 và Date formatting được kiểm thử hồi quy trước lượt cuối.

## 6. Danh sách file

File mới:

- `src/java/cms/{SiteContent,MediaStore,DemoData}.java`
- `src/java/security/{AdminSecurity,Passwords,DemoAccount}.java`
- `src/java/controller/{AdminContentServlet,MediaServlet}.java`
- `src/java/filter/SiteContentFilter.java`
- `web/view/admin/content.jsp`, `web/js/admin-security.js`, `web/assets/admin/admin-cms.css`
- `database/migrations/001-cms-media-revisions.sql`
- `test/cms/{FakeDatabase,CmsSecurityTest,CmsBehaviorTest,CmsBrowserServer}.java`
- `test/run-cms-tests.ps1`, `test/compile-cms-jsps.ps1`, `test-e2e/cms-browser-test.cjs`
- Hai báo cáo `docs/admin-cms-demo-audit.md`, `docs/admin-cms-demo-implementation.md`; artifact build CMS.

File hiện có sửa theo nhóm:

- Authorization/auth/media: `filter/AdminFilter.java`; `controller/LoginServlet.java`, `GoogleCallbackServlet.java`, `DashboardRouterServlet.java`, `ProfileServlet.java`; `dao/UserDAO.java`.
- Editors: `controller/AdminPackageServlet.java`, `AdminCustomerServlet.java`, `AdminUserServlet.java`, `AdminEmailLogServlet.java`, `AdminSettingsServlet.java`, `AdminPlaygroundServlet.java`; `dao/SettingsDAO.java`, `PackageDAO.java`; `dto/PackageDTO.java`.
- Public/chat readers: `controller/HomeServlet.java`, `PackageDetailServlet.java`, `ContactServlet.java`, `AIChatServlet.java`; `ai/AIService.java`, `ai/consultation/{ProductCatalog,ConsultationEngine,ConsultationPolicy}.java`, `ai/security/SSRFValidator.java`, `ai/provider/OpenAICompatibleProvider.java`; `web/js/ai-chat.js`.
- JSP: `web/view/admin/{admin-header,dashboard,package-form,package-list,customer-list,customer-detail,customer-edit,user-list,user-change-password,email-log,settings}.jsp`; `web/view/home.jsp`, `contact.jsp`, `web/view/customer/{customer-header,package-detail,profile}.jsp`.
- Regression fixture/config: `test/ai/provider/ProviderResponseTest.java`, `test/run-chat-tests.ps1`.

Một số file đã có thay đổi từ nhiệm vụ phí lắp đặt/ảnh trước khi bắt đầu CMS. Không revert các thay đổi đó, không ghi đè `dist/fpt-sale.war` đang sửa sẵn. `dist/fpt-sale-images.war`, `dist/fpt-sale-installation.war`, các test/tag/report phí và ảnh không phải deliverable CMS mới. Helper script dùng trong lúc chỉnh mã đã được bỏ, không cần chạy lại.

## 7. Hạn chế và bước xác minh còn cần trước production

- Chưa có integration test trên PostgreSQL cô lập thật; fixture chứng minh luồng Java/SQL contract và web nhưng chưa xác nhận constraint/type/grant/lock semantics của DB triển khai. Cần chạy migration và smoke tests trên staging tách riêng, không dùng production credentials.
- Password mới được PBKDF2, mật khẩu legacy được nâng cấp sau login xác minh nếu DB chấp nhận hash. WAR cũ chỉ so sánh plaintext không thể dùng để rollback auth sau khi có hash. Chuẩn bị rollback artifact hỗ trợ cả hai format trước rollout; không hạ hash về plaintext.
- Repository vẫn có fallback DB credentials và fallback encryption secret từ trước nhiệm vụ. Không in lại, không tự xoay chúng. Người vận hành cần dùng runtime secrets và kế hoạch rotation/rewrap được duyệt; đổi AI_ENCRYPTION_KEY đột ngột có thể làm key đã lưu không giải mã được.
- Chỉ prompt có revision/rollback trong cấu hình AI; key/model/provider được lưu cùng transaction nhưng bí mật không nằm trong revision. Rollback prompt không rollback API key.
- Package audit chưa có auto rollback; restore giá/tốc độ/metadata bằng admin editor và baseline backup đã kiểm chứng. Không thay ID. Audit không bao gồm mọi thao tác delete/badge/khách hiện có; các thao tác này vẫn được bảo vệ POST/CSRF.
- Business snapshot mới là nguồn dữ liệu mỗi lượt chat. Lịch sử hội thoại cũ có thể chứa lời tư vấn theo dữ liệu tại thời điểm trước; không xóa các cuộc trò chuyện đang có khi publish.
- SEO mới tập trung trang chủ; title detail tiếp tục theo gói và route SEO hiện có. Không thêm sitemap/canonical/domain cấu hình mới.
- Provider live, email delivery, cloud restart và production rollout chưa được thử. CMS không tự áp dụng ưu đãi và không tự deploy.

## 8. Xác minh local và triển khai an toàn

Local offline từ repo:

```powershell
.\build.bat '-Ddist.war=D:/miniproject/fpt/dist/fpt-sale-cms.war' dist
.\test\run-cms-tests.ps1 -Browser
.\test\run-chat-tests.ps1
.\test\compile-cms-jsps.ps1
node test-e2e/admin-prompt-browser-test.cjs
```

Scripts cho phép chỉ định TomcatHome/AntJar; browser tests dùng playwright-core đã có trong `test-e2e/node_modules` và Chrome local. Fixture server chỉ listen 127.0.0.1, các test helper không được đóng vào WAR. Test CMS cần build trước để dùng JSP mới.

Staging/deploy (không tự chạy):

1. Review diff, migration, schema/grants/password width và baseline gói/phí/Mesh. Chuẩn bị DB test cô lập, secrets tách production; email/provider live tắt trong staging.
2. Backup DB/settings và media/avatar cũ; giữ rollback artifact tương thích password hash và bản cấu hình encryption đang hoạt động.
3. Chỉ áp dụng migration vào staging trước; build artifact mới, deploy staging thủ công, xác nhận auth local/Google/password/profile, CRUD/save-refresh-public/chat, revision/media và demo/direct URLs. So sánh baseline không tự đổi giá/tốc độ/ưu đãi.
4. Production cần phê duyệt riêng cho migration và deploy. Dockerfile hiện lấy `dist/fpt-sale.war`: build/review artifact mới theo pipeline rồi thay artifact đầu vào có chủ đích; file WAR cũ trong working tree chưa được tự thay bằng bản CMS.
5. Ban đầu để demo tắt; bật sau khi provision secrets và smoke test. Theo dõi lỗi transaction/upload/provider và quyền truy cập, không log secret/body khách.

Rollback: tắt demo và thu hồi session; rollback content/business/prompt qua revision gần nhất nếu chưa bị ghi đè; rollback ứng dụng bằng WAR đã hỗ trợ PBKDF2 và format cũ. Giữ nguyên hai bảng additive và bytes/media references; không DROP hoặc chạy cleanup. Restore baseline package qua editor hoặc DB backup có duyệt, giữ nguyên registration relations. Không restore toàn bộ DB production để sửa một trường CMS và không khôi phục secret từ revision.
