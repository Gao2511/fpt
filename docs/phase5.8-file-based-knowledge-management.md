# Phase 5.8 — File-based knowledge management

Ngày: 13/10/2026, Asia/Saigon. Baseline `c71e2f7`; làm trên `feature/rag-integration`. Render theo dõi `main` theo xác nhận của chủ dự án. Chưa phê duyệt push main/production trong Phase 5.8.

## Đã triển khai

- `knowledge/chatbot-knowledge.md`, `policies/`, `products/`, `faq/`; sample WiFi inactive, không tạo chính sách/giá mới.
- Frontmatter scalar có ID, title/category, version, active, authority, ngày hiệu lực/hết hạn và chunk settings; optional replaces.
- Loader UTF-8 nghiêm ngặt, bounded Reader, SHA-256 toàn file, không truncate, không YAML dependency. Giới hạn theo KnowledgeLimits cũ, thêm cap catalog 200 file/2 triệu ký tự và sâu 8 tầng.
- Build validate offline trước WAR; Docker source build COPY knowledge. Đóng gói **WEB-INF/knowledge**, không public webroot. Dọn riêng bản catalog build cũ trước copy để file bị xóa không lọt WAR mới. Không xóa evidence/locks/data.
- Servlet listener chỉ đọc catalog deployment vào RAM; không JDBC, không API, không sync tự động khi startup.
- Một nút Admin sync, cùng RBAC/CSRF hiện có; background executor có giới hạn, không chặn Tomcat startup. Không cần upload file qua Admin.
- JDBC store có session advisory lock `57316009`, phiên bản/hash bền vững và lịch sử document. File không đổi không tạo document/job/request mới. Pending queue được tiếp tục bằng cùng document/job khi thao tác trước bị gián đoạn.
- File đổi phải tăng version; rollback deployment không được ghi đè bản mới. Path-only move không tạo embedding mới; đường dẫn hiện tại cập nhật, provenance revision cũ giữ nguyên.
- Mọi file qua KnowledgeContentPolicy và single-topic DocumentSectionService; không sửa ConsultationPolicy/system prompt/giá cước. Secret-shaped text bị từ chối bổ sung. Admin không thể giả nguồn repo: qua create/edit form.
- Queue dùng KnowledgeLifecycleDAO + KnowledgePublishWorker hiện có; không viết lại Gemini adapter, không dùng mock vectors cho customer retrieval.

## Dữ liệu tới Supabase/pgvector như thế nào

Git file → source build / WEB-INF private catalog → authorized deployment sync → document FILE_MANAGED + immutable file revision → PENDING_QUEUE → durable publish job → approved budgeted Gemini worker → 768d normalized vectors → atomic activation in knowledge_chunks/documents.

Sync dùng database JDBC đã cấu hình của server nhưng **không tự migrate** và không tự cấp credentials. Thao tác sync không trực tiếp gửi embedding/Chat requests; worker riêng có thể gửi sau đó chỉ khi cấu hình authorization, key và quota đã hợp lệ.

Giữ `RAG_EMBEDDING_API_KEY` riêng cho worker. Các cờ/ngân sách Phase 5.7 vẫn bắt buộc: `KB_GEMINI_WORKER_ENABLED`, `KB_GEMINI_FREE_TIER_VERIFIED`, `KB_GEMINI_MODEL_VERIFIED`, `RAG_EMBEDDING_CALLS_APPROVED`, `KB_EMBEDDING_RPM`, `KB_EMBEDDING_TPM`, `KB_EMBEDDING_RPD`, `KB_EMBEDDING_REQUEST_BUDGET`. Không tự cấu hình giá trị này.

Ledger reserve trước send, advisory lock chống concurrent over-budget, cache completed chunks, không reset uncertain requests, no hidden retries/redirects. Status sync không phải phê duyệt API hoặc bằng chứng RAG live. Existing customer gate trong database/env vẫn quyết định cuối cùng.

**Không thay production feature flags.** Dockerfile đã có defaults RAG=true từ trước; Phase 5.8 chỉ thêm COPY knowledge, không đổi ENV. Test runners dùng false và không dùng credentials production.

## Lifecycle và bảo vệ retrieval

- Update cùng file ID tạo source version/document mới, tham chiếu bản đang published; v1 còn active khi v2 chờ index.
- Phase 5.7 activation transaction kiểm tra index đủ, provider/model/dimensions/hash, approval, dates, authority và pin bản cũ; activate mới/unpublish cũ cùng transaction.
- New ID + replaces yêu cầu nguồn đã published, giữ policy key, không hạ official authority. Tài liệu mới không được lấy recency hoặc similarity làm approval proof.
- Deployment catalog là một điều kiện bổ sung ở search, current-source revalidation và worker/activation. Deleted/inactive file bị loại ngay, trước DB sync. Cũ được giữ tạm nếu có replacement active minh thị.
- Instance deployment cũ không phục vụ phiên bản file mới chưa nằm trong catalog đó. Revision bằng version hiện tại phải có SHA khớp file deployed; bản cũ hơn chỉ giữ khi replacement pending và các source checks khác vẫn hợp lệ.
- Instance chưa load catalog không tự hủy/replay job FILE_MANAGED lúc startup; worker chờ catalog. Catalog lỗi fail closed đối với nguồn repo, nguồn Admin giữ cơ chế cũ.
- Missing/inactive sources archive, cancel incomplete jobs, giữ document/revision/request evidence. Không destructive DB delete.
- Emergency disable có cờ bền vững; sync không bật lại nguồn đó. Không tự reset cờ bằng file mới.
- FILE_MANAGED read-only trong Admin; backend reject forged write/review/rollback/publish POSTs. Unpublish/archive được điều phối thành emergency disable cả family. Admin upload/manual/FAQ workflows vẫn giữ; source Admin không được supersede một FILE_MANAGED bằng upload.
- Permanent delete FILE_MANAGED bị chặn để bảo toàn lineage; việc purge riêng cần thiết kế/phê duyệt operator. Existing Admin permanent-delete workflow không bị bỏ.

Search filter file eligibility được áp dụng sau top-12 ranking rồi revalidate trước trả lời. Nếu nhiều file bị thu hồi chiếm kết quả đầu, preview có thể ít kết quả hơn; an toàn ưu tiên không trả nguồn đã thu hồi. Rolling deployment nhiều revision có thể làm các instance thấy tập nguồn khác nhau; không khẳng định fleet-wide consistency.

## Giao diện và monitoring

Quản lý tài liệu / Kiểm tra RAG / Advanced có bảng FILE_MANAGED: file ID/path/version/SHA, sync state, index status/chunks, provider, eligibility và số source chunks gửi Gemini trong evidence gần đây. Các nguồn khác hiển thị ADMIN_UPLOADED / ADMIN_MANAGED.

Eligibility hiển thị cho revision mới nhất, không phải khẳng định customer gate bật hoặc AI đã dùng nguồn. Khi bản mới pending, bản cũ có thể còn đủ điều kiện; xem lịch sử/document và job progress. Runtime evidence hiện có bounded 100 lượt/24h trong instance, không chứa full question/answer/session PII và mất sau restart.

Readiness không thực hiện Gemini calls. One-click import không đổi gate. Live grounded-answer verification vẫn cần ngân sách và phê duyệt riêng; Phase này không thử live.

## Migration và quyền

Thêm **009-file-managed-knowledge.sql**, cần 008 và schema trước đó. Mở rộng source_type enum của document/revision; thêm knowledge_file_sources + knowledge_file_revisions với FKs, RLS và revoke PUBLIC/anon/authenticated. Không recreate các bảng vector/business. Operator kiểm tra tên CHECK thực tế, backup, owner/JDBC permissions trước khi áp dụng; không tự run trên Supabase.

Không deploy code này trước khi xác nhận migration 008/009 tương thích. Các route cũ không cần bảng file khi không có repo sources; file status báo migration missing và file sync sẽ fail, không tự fallback DB khác.

## Kết quả kiểm tra

| Kiểm tra | Thực tế |
|---|---|
| Portable Java/WAR build | BUILD SUCCESSFUL, catalog mẫu validated offline |
| JSP | 35 JSP, 0 compile errors |
| Focused file suite | 31/31 PASS, gồm deployment version/hash và privacy guards |
| Browser/Tomcat fixture | 11/11 PASS: desktop/mobile, CSRF fields, escaped content, file read-only controls, key settings navigation |
| PostgreSQL focused suite | BLOCKED: Docker Engine pipe dockerDesktopLinuxEngine không tồn tại; Phase58 runner fail trước tạo credentials/container/migrations |
| Gemini/Supabase production | 0 calls / 0 connections |
| Historical full matrix | Không chạy |

Offline suite dùng actual file loader/parser/sync planner/chunker, in-memory Store và MockEmbeddingProvider. Fixture version replacement kiểm chứng boundary behavior, **không phải bằng chứng transaction pgvector thật**. PostgreSQL suite đã viết nhưng chưa thực thi; không báo SQL tests pass.

Evidence final: `.build/file-kb-a313a88cbc964a62935aeaef3611af0e/`, `.build/kb57-view-b87c862b0d2b444c8046469be0428339/`. Evidence cũ không sửa/xóa. Test PostgreSQL 10 checks mới được compile, chưa chạy. `git diff --check` đạt.

Quét 32 changed files: 0 credential-shaped findings theo bộ mẫu (không phải bảo đảm tuyệt đối không có secret). WAR có đúng file mẫu dưới WEB-INF/knowledge, 0 public knowledge entries và 0 private workspace entries; sample vẫn active:false. Latest remote main là c71e2f7, giữ nguyên trong Phase 5.8; scoped changes chỉ được commit/push feature/rag-integration. SHA của commit công việc được báo sau thao tác Git, không tự tham chiếu trong chính nội dung commit.

Status file dùng một query bounded thay vì truy vấn JDBC từng dòng và chỉ tải ở tab documents/verify/advanced. Catalog không log raw file/parser exception; build/runtime lỗi báo danh sách nguyên nhân kiểm tra, không in nội dung/credential. Filter email/số điện thoại phổ biến là guard bổ sung, không phải cam kết phát hiện toàn bộ PII; chủ repository vẫn phải review nội dung.

## Files

Created: knowledge tree/sample/.gitattributes; docs/knowledge-file-editing-guide.md; this report; database/migrations/009-file-managed-knowledge.sql; src/java/rag/files/{KnowledgeFile,FileKnowledgeCatalog,FileKnowledgeSync,FileKnowledgeRuntime,FileKnowledgeValidator}.java; src/java/dao/FileKnowledgeDAO.java; web/view/admin/knowledge-files.jsp; test/rag/{FileKnowledgeOfflineTest,FileKnowledgePostgresTest}.java; test/run-file-knowledge.ps1.

Modified: build-portable.xml, build.xml, Dockerfile, dist/fpt-sale.war; AdminKnowledgeBaseServlet, AdminKnowledgeDAO, KnowledgeChunkDAO, KnowledgeLifecycleDAO, AdminKnowledgeDTO; knowledge-base.jsp, knowledge-simple.jsp; test/run-rag-isolated.ps1, test/cms/KnowledgeViewServer.java, test-e2e/knowledge-lifecycle.cjs.

Không sửa giá gói, đăng ký khách hàng, encryption keys, Gemini model/adapter, production settings hoặc historical evidence.

## Editing, verification và deploy

Xem `docs/knowledge-file-editing-guide.md` để có hướng dẫn tiếng Việt đầy đủ. Không cần sửa Java cho mỗi file mới.

```powershell
.\build.bat -f build-portable.xml war
.\test\run-file-knowledge.ps1
.\test\compile-cms-jsps.ps1 -TomcatHome '<TOMCAT9_HOME>' -AntJar '<ANT_HOME>/lib/ant.jar'
.\test\run-knowledge-view.ps1 -Chrome '<EXISTING_CHROMIUM_EXE>'
# Chỉ khi Docker Engine Running, fresh isolated test database:
.\test\run-rag-isolated.ps1 -Phase58
```

Trước production: khôi phục Docker và pass suite PostgreSQL tập trung; review diff/migration, backup; apply 008/009 bằng operator; xác minh RLS/JDBC privileges và tài nguyên Render/Supabase. Chỉ cấu hình worker/key/quota bằng phê duyệt riêng. Sau đó phê duyệt merge/push main riêng; Render source build đóng gói file. One-click sync sau deploy, kiểm tra trạng thái index và runtime evidence, không bật gate tự động.

Rollback: disable worker trước, giữ request ledger/reservations, rollback Render revision được biết tốt; giữ additive migrations và dữ liệu. Do not shrink text constraints/drop tables hoặc reset locks. App cũ chưa có file catalog filtering nên có thể coi file sources là knowledge bình thường: operator phải emergency-disable/thu hồi nguồn file trước khi quay về app cũ, hoặc tạm tắt gate theo phê duyệt riêng. Không tự rollback database/content vào phiên bản thấp.

**Code implemented, production NO-GO:** migration/transaction pgvector chưa được xác minh do Docker blocker; không có live Gemini authorization/test. Chỉ chuẩn bị scoped commit và push non-production branch được cho phép; không push main/deploy trong Phase 5.8.
