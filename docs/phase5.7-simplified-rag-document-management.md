# Phase 5.7 — Kho tri thức đơn giản, bằng chứng RAG và vòng đời tài liệu

Ngày: 12/10/2026. Branch làm việc: `feature/rag-integration`.
Baseline: `55a30a4`. Không commit/push/deploy trong bước này.

**Đã triển khai code và kiểm tra offline; phát hành NO-GO vì PostgreSQL/pgvector chưa kiểm thử được.**
Không coi HTTP 200, vector tồn tại hoặc chữ “online” là bằng chứng chatbot đã trả lời có grounding.

## 1. Giao diện và đường dẫn

- Sidebar AI có bốn mục: Tải lên & Xuất bản, Quản lý tài liệu, Kiểm tra AI RAG, Cài đặt nâng cao.
- Không thêm framework. Dùng JSP/JSTL, CSS admin hiện có và JS nhỏ cho xác nhận xóa.
- `/admin/knowledge-base?tab=upload` là màn hình mặc định.
- Các tab `dashboard`, `documents`, `imports`, `faqs`, `indexing`, `playground`, `rag-config`, `audit` và `/admin/settings` vẫn truy cập qua khu nâng cao.
- Giữ tìm kiếm, bộ lọc, editor, lịch sử, diff, rollback, FAQ và runtime diagnostics; lịch sử/quy trình kỹ thuật được thu gọn.
- Thao tác thông thường: upload → xem nội dung → bấm Xuất bản. Bấm lại không tạo job trùng phiên bản.
- Gỡ xuất bản, lưu trữ và khôi phục thành bản nháp có nút riêng; xóa vĩnh viễn nằm trong phần nâng cao.

## 2. Upload / Publish

1. Xác thực Admin thực, kiểm tra CSRF và giới hạn multipart hiện có.
2. Kiểm tra loại tệp, chữ ký, macro, XXE, ZIP bomb và PDF action; lưu nguyên bản ngoài webroot và database theo migration 007.
3. Trích xuất UTF-8, giữ revision và phân mục bằng service hiện có.
4. Admin bấm Xuất bản là phê duyệt nội dung tri thức, không phải quyền thay đổi prompt/cấu hình/giá cước.
5. Với tài liệu nhiều chủ đề, tạo entry đơn chủ đề có provenance về revision, section và dòng nguồn. Quy tắc AI, cấu hình và mục không đạt policy được giữ lại để duyệt thủ công. Không bỏ source policy để xuất bản cả tệp.
6. Preflight toàn bộ nội dung và từng entry, sau đó tạo `knowledge_publish_jobs` + `knowledge_publish_chunks`; tài liệu giữ `APPROVED` / `draft`, chưa dùng trong customer retrieval.
7. Worker được operator cho phép mới gửi Gemini. Hoàn tất toàn bộ vector và kiểm tra phiên bản/hash/ngày hiệu lực trước khi kích hoạt trong transaction.

Trạng thái có thật: `PENDING_INDEX`, `WAITING_FOR_QUOTA`, `WAITING_EFFECTIVE`, `INDEXING_FAILED`, `AVAILABLE_TO_CHATBOT`, `CANCELLED`.
Giao diện còn kiểm tra workflow/ngày hết hạn/phiên bản hiện tại để không hiển thị job cũ là available sau khi archive, unpublish, expire hoặc sửa nội dung.
`AVAILABLE_TO_CHATBOT` nghĩa là index hoàn tất và tài liệu đủ điều kiện; gate customer RAG vẫn phải được bật riêng. Không tự bật gate.

Nút Gemini Index và batch route cũ chuyển sang hàng đợi bền vững; bản đã index đúng revision không tạo request thừa. Smoke plan có phê duyệt vẫn đi qua runtime/ledger cũ.

## 3. Giới hạn tài liệu

| Tầng | Giới hạn mặc định / cách xử lý |
|---|---|
| Nhập trực tiếp | 100.000 ký tự; `KB_TEXT_MAX_CHARS`, có thể hạ, ceiling 100.000 |
| Trích xuất | 500.000 ký tự; `KB_EXTRACTED_MAX_CHARS`, có thể hạ, ceiling 500.000 |
| Upload | 10 MB; `KB_UPLOAD_MAX_BYTES`, có thể hạ, ceiling 10 MB |
| Tomcat multipart | File 10 MB, request 11 MB; threshold mặc định dùng tệp tạm |
| PostgreSQL | TEXT; migration 008 nâng CHECK của document và revision từ 60.000 lên 500.000 |
| DOCX | Tổng giải nén 25 MB, giới hạn entries/ratio, DOM bounded, độ sâu traversal tối đa 100 |
| PDF | Stream inflate tối đa 25 MB; accumulator giới hạn theo extracted chars; vẫn là extractor nhẹ hiện có |
| Chunking | Tối đa 5.000 chunk, ranh giới dòng/từ, không cắt surrogate Unicode; overlap bounded |
| Gemini | `gemini-embedding-001`, 768 chiều, `RETRIEVAL_DOCUMENT`; không gửi cả tài liệu vào Chat |

Đã bỏ việc cắt văn bản rồi lưu như extraction thành công. Vượt giới hạn có lỗi rõ ràng, không tạo bản bị cắt.
Accumulator DOCX/PDF được chặn trước khi tăng quá giới hạn. Plain text/file bytes và XML vẫn được đọc trong vùng nhớ có trần; chưa triển khai parser PDF/OCR đầy đủ.
Nội dung upload lớn hơn giới hạn direct text được giữ nguyên khi sửa metadata, không POST toàn bộ body trở lại Tomcat. Để thay nội dung lớn, upload phiên bản thay thế. Revision được pin để tránh ghi đè bản mới hơn.
Render RAM, thời gian import lớn, dung lượng Supabase/backup và quyền JDBC thực tế chưa đo trên cloud; cần kiểm tra trước release. File bytes tối đa 10 MB mỗi bản và staging vectors làm tăng dung lượng lưu trữ.

Request REST giữ `outputDimensionality: 768` và `taskType: RETRIEVAL_DOCUMENT` ở top level, với title và content.parts.text. Đây là các field legacy vẫn có trong [Gemini REST specification](https://ai.google.dev/api/embeddings), hiện được đánh dấu deprecated; không tự đổi payload/model đã được xác minh trước đó.
Chunk size trong publish được hạ theo byte budget bảo thủ (UTF-8 + title + overhead), tối đa 2.048 reserved tokens/request. Không dùng chars/4 để giả định lượng token tiếng Việt. Cần xác minh lại model/quota trước live.

## 4. Hàng đợi, ngân sách và chống gửi trùng

- Worker mặc định không hoạt động. Không có timer gửi API chỉ vì mở trang Admin.
- Bật worker đòi `KB_GEMINI_WORKER_ENABLED`, `RAG_EMBEDDING_CALLS_APPROVED`, `KB_GEMINI_FREE_TIER_VERIFIED`, `KB_GEMINI_MODEL_VERIFIED`, key embedding riêng và đủ RPM/TPM/RPD/request budget dương. Không hoạt động song song với smoke-plan runtime.
- Chỉ dùng `RAG_EMBEDDING_API_KEY`; không fallback vào chat key hoặc key trong settings.
- Adapter một POST/slot, không GET metadata, retry hoặc redirect. Không countTokens hoặc Chat call trong indexing.
- Reserve vào database **trước** network send, dưới advisory transaction lock; budget không chỉ đếm trong RAM.
- `knowledge_embedding_requests` không cascade theo document deletion; xóa tài liệu không reset số API đã dùng.
- Slot `RESERVED` / `UNCERTAIN` chặn worker khác. Request đang gửi hoặc kết quả không xác định không được replay khi process restart.
- Chunk `DONE` đã lưu được tiếp tục dùng sau restart. Có cache theo title/content hash của staging vectors hoàn tất, không reuse vector MOCK.
- Budget/quota hết trước send: chờ và có thể tiếp tục sau khi ngân sách/quota cho phép.
- HTTP 429 / vector sai: ghi lỗi, không retry slot đã gửi. Cần Admin tạo revision mới phù hợp hoặc operator đối soát trước hành động tiếp theo.
- Timeout/disconnect/DB error sau send: trạng thái uncertain, không tự reset. Không cung cấp nút “retry” vô điều kiện.
- Trần request là lifetime của ledger này; tăng trần là quyết định riêng của operator. Không tự chuyển Paid Tier.

Quota này giới hạn worker, không thể xác định mức tiêu thụ từ ứng dụng khác trên cùng Google project. Operator phải phân bổ quota còn lại, chừa phần cho query/chat và các consumer khác. Chưa có bằng chứng đo live ở Phase 5.7.

## 5. Kiểm tra AI RAG

`tab=verify` có hai lớp:

**Readiness:** số document published, index GEMINI của revision hiện tại, chunk có model/provider/hash/date khớp, job lỗi, thời điểm index và gate hiện tại. Không đếm index MOCK là Gemini.

**Runtime:** hook trong ConsultationEngine / retrieval / context renderer ghi UUID, timestamp, gate tại request, có retrieval hay không, số candidate từ pgvector/FTS, số chunk thật sự gửi vào Gemini, document ID/title, provider query embedding hay lexical, grounding và dùng context.
Chỉ đánh dấu grounded sau khi model chọn ID hợp lệ và server revalidate/trích nội dung thành công; ID rỗng hoặc source không còn hiệu lực không được đánh dấu dùng context.

- Không lưu câu hỏi, câu trả lời, session ID, email/điện thoại hoặc toàn bộ hội thoại cho diagnostics.
- Evidence giữ tối đa 100 records / 24 giờ **trong instance hiện tại**, mất sau restart, chưa phải telemetry toàn cụm/durable storage. UI ghi rõ hạn chế này.
- Preview mặc định chỉ lexical search trên stored GEMINI chunks và revalidate source; 0 embedding/Chat API calls. Hiển thị title/snippet/score/issue/rejected reasons. Preview không phải bằng chứng live customer answer.
- Factory customer không fallback sang MOCK vectors. Khi Gemini/query/index unavailable, giữ lexical hoặc consultation fallback có kiểm soát, không bịa dữ liệu.

## 6. Thay thế, hết hạn, archive và delete

- Upload bản thay thế liên kết ID cũ, Policy Key và authority; cũ phải có approval proof. Không lấy recency làm bằng chứng authority.
- Lưu snapshot revision/hash cũ trong job. Bản cũ vẫn truy xuất được khi bản mới pending/failed.
- Activation kiểm tra đủ chunks, vector hữu hạn/768d/L2, hash, version, approval, ngày hiệu lực và revision cũ chưa đổi.
- Publish mới + unpublish cũ trong một transaction; vectors cũ có thể giữ cho rollback nhưng không thỏa retrieval eligibility.
- Chặn hai replacement sibling cùng active. Tài liệu tương lai giữ `WAITING_EFFECTIVE`; tài liệu hết hạn không được kích hoạt/truy xuất.
- Archive/unpublish thu hồi nguồn ngay theo DAO hiện có. Khôi phục tạo revision DRAFT mới, cần publish/index lại.
- Delete phải nhập `DELETE <id>` và xác nhận UI, có Admin + CSRF; từ chối tài liệu còn published, references FAQ/import/lineage/replacement hoặc request uncertain/reserved.
- Xóa file rows/bytes và document trong transaction; FK cascade dọn revisions/chunks/jobs được sở hữu. Giữ audit tối thiểu và request budget evidence.
- Sau commit, chỉ dọn disk file tên UUID thuộc storage root, không follow symlink. Disk cleanup thất bại báo cần dọn thủ công; không undo commit hoặc xóa đường dẫn tùy ý.

Chưa tự động thay thế cả một bộ tài liệu nhiều chủ đề như một policy đơn: cần tạo các replacement đơn chủ đề được duyệt. Mục manual-review không tự trở thành system instructions hay cấu hình AI.

## 7. Migration và quyền

Thêm duy nhất `database/migrations/008-knowledge-publish-lifecycle.sql`, không viết lại 001–007.
Mở rộng CHECK text, tạo ba bảng mới và RLS; revoke PUBLIC/anon/authenticated và sequence; ghi schema version 008.
Không tạo schema/migration tại startup. Thiếu 008 thì UI báo blocker; không fallback sang unbudgeted Gemini indexing.
JDBC role trusted cần quyền bảng/sequence và policy/RLS phù hợp như role backend hiện có. Không grant quyền cho public Supabase REST roles.
Migration 008 chạy một lần sau khi kiểm tra 002–007 và backup. Không chạy lại migration 007 trên production vì file hiện có chứa thao tác reset gate trong settings.
Chưa áp dụng 008 lên bất kỳ Supabase/database production nào.

## 8. Kết quả thực tế và điều chưa kiểm chứng

| Kiểm tra | Kết quả |
|---|---|
| Java compilation / WAR | BUILD SUCCESSFUL; JDK17, source release8 như project hiện có |
| Jasper JSP | 34 JSP, 0 errors |
| Focused offline suite | 27/27 PASS: large Vietnamese extraction, limits, injection/prices, dimensions, worker gates, one POST/no retry, RBAC/CSRF, privacy/retention, engine grounding and consultation fallback |
| Tomcat/Chromium fixture | 9/9 PASS: 4 sections × desktop 1440/mobile 375, CSRF form fields, stored-XSS escape, no horizontal overflow; large metadata form does not submit full body |
| JS syntax / diff whitespace | PASS |
| PostgreSQL/pgvector suite | BLOCKED before container creation; no transaction/retrieval SQL tests completed |
| Real Gemini / production Supabase | 0 requests / 0 connections |
| Historical full matrix | Không chạy theo yêu cầu; hai assertion suites cũ được cập nhật theo limits/navigation mới |

PostgreSQL runner đã chuẩn bị tests cho publish/index activation, idempotency, cache, concurrent reservation, replacement exclusion, failure preservation, quota, archive/delete references, immutable upload revision/provenance, uncertain replay block và RLS.
**Không báo các test SQL này là pass.** Docker Engine pipe chưa có; log Desktop báo không truy cập/rename được `sailor-ingest.sock`. Không reset Docker, xóa container/volume hay đổi sang Supabase để tránh blocker.

Evidence:
- `.build/kb57-cf78c2d419ab4caaba4cde989bcaee51/` — final offline suite after durable legacy indexing route correction, 27/27 PASS.
- `.build/kb57-view-ff4ac9d876824e68b5f400f1404f02a0/` — HTML/screenshots và Tomcat fixture.
- Các evidence/locks trước đây giữ nguyên. Test fixture annotation scan bị tắt và mọi browser URL ngoài loopback bị chặn.

Final build sau bản sửa cuối: `BUILD SUCCESSFUL`. Kiểm tra 35 changed text files không phát hiện dạng credential theo bộ mẫu quét; đây không phải chứng nhận tuyệt đối không có secret. WAR không chứa `.local`, `.git`, `.env` hoặc `.build` workspace entries, chỉ đóng gói một runtime JSTL `jstl-1.2.jar`. `git diff --check` đạt. Lần kiểm tra Docker cuối vẫn không có pipe `dockerDesktopLinuxEngine`; SQL blocker chưa được giải quyết.

## 9. Files

Created:
- `database/migrations/008-knowledge-publish-lifecycle.sql`
- `src/java/dao/KnowledgeLifecycleDAO.java`
- `src/java/rag/KnowledgeLimits.java`, `KnowledgePublishWorker.java`, `RagRequestEvidence.java`
- `web/view/admin/knowledge-simple.jsp`, `knowledge-advanced-links.jsp`
- `web/assets/admin/admin-kb-lifecycle.js`
- `test/rag/KnowledgeLifecycleOfflineTest.java`, `KnowledgeLifecyclePostgresTest.java`
- `test/cms/KnowledgeViewServer.java`, `test-e2e/knowledge-lifecycle.cjs`
- `test/run-knowledge-lifecycle.ps1`, `test/run-knowledge-view.ps1`
- this report.

Modified:
- `.env.example` — bounded limits and worker approval/budget names; empty secrets.
- `src/java/controller/AdminKnowledgeBaseServlet.java` — navigation, publish/import/restore/delete/read-only preview and metadata version pin.
- `src/java/dao/AdminKnowledgeDAO.java` — limits, read-only preview, version pin and durable legacy Gemini route.
- `src/java/dao/KnowledgeDocumentDAO.java` — large extracted document ceiling.
- `src/java/rag/DocumentChunkingService.java`, `rag/importer/DocumentSectionService.java`, `rag/extract/DocumentExtractionService.java` — bounded large corpus handling and full-body policy preflight.
- `src/java/rag/RagFactory.java`, `KnowledgeRetrievalService.java`, `RagContextBuilder.java`, `KnowledgeSourceSelection.java`, `src/java/ai/consultation/ConsultationEngine.java`, `src/java/dto/KnowledgeChunkDTO.java` — provider isolation, evidence hooks and preview getters.
- `web/view/admin/admin-header.jsp`, `knowledge-base.jsp`, `knowledge-import.jsp`, `web/assets/admin/admin-kb.css` — four sections and progressive disclosure.
- `test/run-rag-isolated.ps1` — optional Phase57-only suite, fresh isolated DB; existing switches preserved.
- `test/rag/RagAdminKbTest.java`, `RagBehaviorTest.java` — old navigation/limit assertions adapted.
- `dist/fpt-sale.war` — rebuilt artifact, not deployed.

## 10. Local verification and release plan

```powershell
.\build.bat -f build-portable.xml war
.\test\run-knowledge-lifecycle.ps1
.\test\compile-cms-jsps.ps1 -TomcatHome '<TOMCAT9_HOME>' -AntJar '<ANT_HOME>/lib/ant.jar'
.\test\run-knowledge-view.ps1 -TomcatHome '<TOMCAT9_HOME>' -Chrome '<EXISTING_CHROMIUM_EXE>'
# Only when Docker Engine is Running; creates a fresh test container, preserves old containers/data:
.\test\run-rag-isolated.ps1 -Phase57
```

Docker image is the previously pinned pgvector PostgreSQL17 image, loopback port, test DB `fpt_rag_test`, generated private credentials in ignored `.local`. Runner stops its own new container but does not remove it/volume. No Gemini key is needed for tests.

Release prerequisites:
1. Recover local Docker Engine and pass focused PostgreSQL tests; resolve any migration/transaction failure before committing release.
2. Review source diff and secrets; create a scoped commit only when release is approved. No new commit SHA exists for this work yet.
3. Operator backs up Supabase and applies migration 008 after checking actual existing constraints and trusted JDBC permissions. Do not run 001–007 again blindly.
4. Review cloud RAM/storage/request-size limits and actual RAG settings. Existing **Dockerfile already defaults RAG_ENABLED=true** from earlier commits; this task did not change it. `.env.example` and test runners default false. Do not infer actual Render gates from either file; confirm desired cloud gates privately before deployment.
5. Configure worker approval flags, separate embedding key and allocated quota only after independent authorization; default disabled during rollout.
6. Render tracks `main` (previous user confirmation). Current `origin/main` reference is `55a30a4`; local `main` is older. Do not overwrite main or assume the local branch is deployment HEAD. No push was made.
7. Once migration is confirmed and SQL safety checks pass, approve scoped commit/merge/push separately. Main push triggers production auto-deploy.

Rollback: disable the new worker first; preserve reserved/uncertain ledger entries. Roll Render back to the previous known-good deploy/source revision; leave additive migration 008 and data in place. Do not drop tables or shrink text CHECKs while long documents exist. Export/review long documents and pending jobs if using an old UI that cannot edit their full length. Customer gate remains an operator decision, not part of automatic rollback code.

**Release NO-GO:** focused database suite/migration 008 not executed successfully, production migration/permissions unverified, no live Gemini verification authorized. Code implemented; runtime readiness on production not claimed.
