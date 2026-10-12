# Phase 3.16 — Vietnamese RAG quality & multi-topic import

Ngày kiểm thử: 2026-10-12. Branch được kiểm tra: `feature/rag-integration`.
Phạm vi: local/offline, Java 17, Servlet/JSP, PostgreSQL 17.10, pgvector 0.8.2, Tomcat 9.0.118. **0 Gemini requests thật; 0 kết nối Supabase production. `RAG_ENABLED=false`. Không commit, push hoặc deploy.**

## 1. Root cause và bản sửa

### LIM-315-01

`KnowledgeContentPolicy.reason()` trước đây bỏ dấu qua `ProductCatalog.normalize()` rồi chặn các từ đứng riêng như `gia`, `cuoc`, `ngan`. Vì vậy Gia Lai/gia đình/tham gia/đánh giá, căn cước, ngắn hạn/ngăn cách bị đánh đồng với phát biểu giá cước.

Bản sửa kiểm tra ngữ cảnh: số tiền kèm đơn vị tiền tệ, số và cấu trúc giá/cước/phí, số tiền viết bằng chữ có dấu, câu mô tả gói/ưu đãi/quyền lợi/thông số. Không còn chặn riêng các từ `gia/cuoc/ngan`. Có ngoại lệ ngữ cảnh cho “đánh giá 5 sao”, “tham gia …”, “2 ngăn cách …”. Những dấu hiệu thương mại rõ như bảng giá, gói cước, phí lắp đặt vẫn bị giữ ngoài knowledge prose; dữ liệu này tiếp tục thuộc nguồn business có cấu trúc.

Thêm `contentReason(title, content)` để kiểm tra trước xuất bản. Hàm này **không cấp approval**: `reason()` vẫn kiểm tra nguồn, approval, authority, category, revision/hash, hiệu lực, supersession và confidence trước khi kiểm tra nội dung. Source authority và chính sách giải quyết xung đột không được nới lỏng.

Trong vòng kiểm thử đầu, “Đánh giá 5 sao” vẫn bị chặn và fixture phí `Phí Internet thông thường / lần: 300000` lại lọt. Đã sửa nhận diện ngữ cảnh tương ứng rồi chạy lại cả bộ mới và bộ hồi quy; không đổi expected result của các bộ RAG hiện có.

### LIM-315-02

`KnowledgeSourceSelection` xử lý nhiều subject dựa trên tiêu đề đã review, yêu cầu một domain rõ ràng và một subject phù hợp. Upload nguyên một playbook nhiều chủ đề dùng chung tiêu đề/subject không đáp ứng điều kiện này. Chunking theo độ dài/đoạn không tạo metadata chủ đề riêng, nên chia chunk đơn thuần vẫn không giải quyết được `AMBIGUOUS_SUBJECT`.

Giải pháp là giữ upload gốc làm archive, detect section và tạo **entry DRAFT đơn chủ đề có provenance riêng**, sau review của Admin. `KnowledgeSourceSelection` và thuật toán retrieval/chunking hiện có giữ nguyên; chỉ mở helper read-only `RetrievalQuery.subjectTopics()` để importer dùng cùng taxonomy.

Audit cũng phát hiện `DocumentExtractionService.normalizeExtractedText()` từng thay `< > { }` và backtick để nội dung vượt content policy. Đã bỏ bước thay ký tự này. Nội dung nguy hiểm được giữ nguyên để review và bị policy từ chối, không được biến thành prose an toàn giả tạo. Chuẩn hóa xuống dòng/khoảng trắng hiện có vẫn giữ; **file upload gốc giữ nguyên byte**.

## 2. Luồng import đã triển khai

`Upload original → extract → detect sections → preview classification → Admin review → create independent drafts → submit review → approve → publish locally → mock index → offline playground`.

- Tab mới **Phân mục & Review nhập liệu**, tích hợp theme Admin hiện có. Upload mặc định chọn preview từng mục; có nút preview cho upload DRAFT đã lưu.
- Download bản gốc bằng quyền Admin, metadata UUID từ DB, đường dẫn trong storage ngoài web root; kiểm tra size/SHA256 và tải dưới dạng attachment. Hiển thị toàn văn trích xuất, nội dung từng mục, revision, dòng nguồn và preview chunk.
- Nhận diện heading số `1.`/`1)`, số La Mã, Markdown và PHẦN/MỤC/CHƯƠNG; giữ preamble. Nội dung section là substring nguyên vẹn của **revision trích xuất**. Lưu ordinal, UTF-16 offsets, dòng, hash, revision và URI `upload:<file UUID>/<filename>#vN/section-M/lines-a-b`.
- Năm phân loại: AI Behavior & Consultation Rules, Knowledge Base, FAQ, Admin Configuration, Needs Manual Review. Phân loại là gợi ý deterministic/offline; Admin sửa title/loại, lưu review hoặc bỏ qua. Không gọi AI để phân loại.
- Chỉ Knowledge/FAQ đã review mới được tạo draft. Nội dung/tiêu đề phải đạt kiểm tra đơn chủ đề và content policy; đổi dropdown không thể chuyển rules/config/injection thành knowledge tự động.
- AI Behavior và Admin Configuration chỉ là **hàng chờ đề xuất/review**. Không có chức năng áp dụng vào system prompt, `ConsultationPolicy` hoặc AI settings.
- Create draft dùng một transaction và `SELECT … FOR UPDATE`; target, revision, provenance và audit cùng commit. Gửi lại thao tác trả về cùng target, không tạo bản trùng.
- Migration 005 liên kết section tới revision immutable và target Knowledge/FAQ, bật RLS, thu hồi quyền PUBLIC/anon/authenticated. Trigger chặn xuất bản archive gốc sau khi đã phân mục; không index nguyên playbook.
- Imported Knowledge phải về DRAFT và duyệt lại khi sửa; từ chối giữ PUBLISHED hoặc thay URI provenance. Imported FAQ sửa cũng về DRAFT, linked document rút khỏi retrieval. Chặn sửa/xuất bản linked FAQ bằng tab Documents để bỏ qua review FAQ.
- Chỉ import từ revision có file extraction tương ứng. Cảnh báo extraction, kể cả cắt quá 60.000 ký tự, khiến preview import bị từ chối để tránh mất nội dung âm thầm; file gốc và upload DRAFT vẫn được giữ để đối chiếu thủ công.

## 3. Files thay đổi trong Phase 3.16

Các file đã tồn tại (một số còn untracked từ phase trước):

| File | Thay đổi |
|---|---|
| `src/java/rag/KnowledgeContentPolicy.java` | Nhận diện commercial claim theo ngữ cảnh; pre-publication linter |
| `src/java/rag/RetrievalQuery.java` | Helper taxonomy read-only cho importer |
| `src/java/rag/extract/DocumentExtractionService.java` | Giữ nguyên markup thay vì thay ký tự để vượt policy |
| `src/java/dao/AdminKnowledgeDAO.java` | Preview/review/create draft, transaction/provenance, guard publish/edit, FAQ sync |
| `src/java/controller/AdminKnowledgeBaseServlet.java` | Import actions, upload-to-preview, download original có kiểm tra |
| `web/view/admin/knowledge-base.jsp` | Navigation/import option/preview action |
| `web/assets/admin/admin-kb.css` | Trình bày văn bản/section theo theme hiện có |
| `test/run-rag-isolated.ps1` | Áp dụng thêm migration 005 vào DB test mới |

Files tạo mới:

- `database/migrations/005-knowledge-section-import.sql`
- `src/java/rag/importer/DocumentSectionService.java`
- `src/java/dto/KnowledgeImportSection.java`
- `web/view/admin/knowledge-import.jsp`
- `test/rag/RagPhase316OfflineTest.java`
- `test/rag/RagPhase316UatTest.java`
- `test/run-rag-phase316-offline.ps1`
- `test/run-rag-phase316-uat.ps1`
- `docs/rag-phase3.16-vietnamese-rag-quality.md`

Không sửa system prompt, ConsultationPolicy, giá gói, Mesh/business settings hoặc logic đăng ký trong phase này. Những thay đổi chưa commit từ phase trước được giữ nguyên.

## 4. Kết quả đã thực thi

| Bộ kiểm thử | PASS | FAIL cuối cùng |
|---|---:|---:|
| Vietnamese policy + section import offline | 53 | 0 |
| Admin KB extraction/security mock regression | 17 | 0 |
| Phase 3.16 JDBC/Tomcat UAT | 23 | 0 |
| Phase 3.15 UAT hồi quy | 13 | 0 |
| RAG regression (35 + 13 + 27 + 22) | 97 | 0 |
| Admin PostgreSQL lifecycle/revision | 10 | 0 |
| PostgreSQL/pgvector integration | 24 | 0 |
| Mock-provider RAG E2E | 41 | 0 |
| Chat backend/frontend (32 + 21 + 17 + 9 + 7 + 6) | 92 | 0 |
| Live-runner safety **mock only** | 87 | 0 |
| **Tổng các case cuối cùng, không cộng lượt chạy lại** | **457** | **0** |

Trong 53 case mới: 17 câu/cụm tiếng Việt thông thường được chấp nhận; 14 commercial claims giá/ưu đãi/phí/thông số bị từ chối; injection vẫn bị từ chối. Đây là corpus regression cụ thể, không phải chứng minh mọi cách diễn đạt tiếng Việt đều được nhận diện hoàn hảo.

23 UAT kiểm tra upload multipart, nguyên bản UTF-8/bytes, giữ 9 heading của fixture, offsets/provenance, idempotency, stale version, review riêng, draft exclusion, archive exclusion, mock indexing, FAQ sync, sửa phải duyệt lại, injection, cắt nội dung, RBAC (anonymous/customer/Admin), CSRF và stored XSS. HTTP import JSP render 200; non-admin/thiếu CSRF bị từ chối; title `<script>` hiển thị escaped. Build WAR thành công với Java 17/Tomcat 9; target bytecode Java 8 hiện có vẫn được giữ.

Lượt chạy trung gian có lỗi assertion giữa text đã chuẩn hóa và raw bytes, assumption “không có selected source nào” sau rút FAQ trong khi một nguồn khác còn published, và thiếu environment synthetic của JDBC fixture Admin. Đã tách đúng assertion nguyên bản/extracted revision, kiểm tra document ID của FAQ chưa duyệt, và cung cấp cấu hình dummy cho driver mock (deregister toàn bộ real JDBC drivers). Các lỗi compile/test trung gian đã sửa; không coi chúng là pass. Container/log riêng của các lượt UAT được giữ, không reset evidence/lock cũ.

### Retrieval offline

Fixture minh họa `phase316-synthetic.txt` có 9 mục: rules, WiFi, gaming, camera FAQ, cấu hình, commercial claim, mixed-topic, injection, manual note. Ba mục an toàn được review/xuất bản riêng và mock-index. **3/3 query WiFi/game/camera chọn đúng document ID theo chủ đề, không abstain, không `AMBIGUOUS_SUBJECT`** khi ba nguồn này cùng khả dụng. Rules/config/commercial/injection/mixed/manual không được tạo Knowledge tự động. Archive và drafts không được đưa vào retrieval. Các bộ hồi quy tiếp tục từ chối nguồn thiếu approval, authority sai, expired/superseded và source conflict.

Không đo chất lượng semantic bằng Gemini embeddings thật hoặc latency API. Mock-vector retrieval không chứng minh chất lượng với dữ liệu thực tế; trong môi trường mock, một nguồn khác có thể được chọn khi nguồn đúng chủ đề bị rút. Test isolation kiểm tra đúng document ID chưa duyệt không xuất hiện, không đánh đồng điều đó với độ chính xác semantic.

### Evidence mới

- Logs regression: `D:\miniproject\fpt\.build\phase316-validation-ff1b9186a1f94a52a1ce5bab7e760394\`.
- Offline policy/Admin cuối: `D:\miniproject\fpt\.build\phase316-final-offline-799dad0146da41ea841ab463a1b02c25.log`.
- UAT cuối (có assertion source ID): `C:\Users\Caoky\AppData\Local\Temp\fpt-tomcat-uat316-b9729df8\logs\phase316-test.log`; container giữ lại/stopped: `fpt-rag-uat316-33ae19d82a61`, DB `fpt_rag_test`, loopback port `58215` trong lần chạy này. Evidence các lượt trước vẫn giữ nguyên.
- E2E JSON: `D:\miniproject\fpt\.build\phase24-e2e-76ebb6d031d74466b3698c805a93be81.json`.
- Runner safety JSON: `D:\miniproject\fpt\.build\phase241-mocks-5ce69e9b-850e-43dc-8204-6f8da8dd929f\summary.json`.
- Admin PostgreSQL được chạy lại sau guard FAQ, 10/10; container giữ/stopped `fpt-rag-test-b3a471c50d59`.

Runner mới sử dụng DB/container và CATALINA_BASE riêng, pin host `127.0.0.1`, DB `fpt_rag_test`, credentials riêng, cờ live false. Chỉ dừng tài nguyên do lượt chạy đó tạo; không xóa container/volume/log/upload/config. Log UAT dùng CREATE_NEW trong thư mục có UUID. Config test nằm trong `.local/rag-tests` đã được Git ignore; báo cáo không chứa credentials. Plan live vẫn `approved=false`.

## 5. Tài liệu thật: NOT RECEIVED

Không tìm thấy bản `văn bản.txt` thật của Administrator trong project. File trùng tên được bộ Phase 3.15 tự tạo là **fixture synthetic**, không phải tài liệu của anh. Phase 3.16 dùng tên fixture khác và không tự tạo tài liệu thay thế rồi coi là kiểm thử thật.

Do đó chưa xác nhận được cấu trúc 9 mục, tình huống tư vấn, FAQ, mandatory rules, thông tin cấu hình hoặc retrieval của **tài liệu thật**.

Administrator cần cung cấp file UTF-8 thật vào thư mục local đã chọn trong project, ví dụ `D:\miniproject\fpt\.local\rag-import\văn bản.txt`, hoặc upload từ Admin Knowledge Base ở môi trường local có migration 005. Không upload vào Supabase production để làm test này.

## 6. Cách xác minh local và việc Admin cần làm

Các lệnh offline, chạy từ `D:\miniproject\fpt`:

```powershell
pwsh -NoProfile -File .\test\run-rag-phase316-offline.ps1
pwsh -NoProfile -File .\test\run-rag-phase316-uat.ps1
pwsh -NoProfile -File .\test\run-rag-isolated.ps1 -Phase3
pwsh -NoProfile -File .\test\run-rag-isolated.ps1
pwsh -NoProfile -File .\test\run-rag-isolated.ps1 -Phase24
pwsh -NoProfile -File .\test\run-chat-tests.ps1
```

UAT cần Docker Desktop, image pgvector đã có local, Java 17 và Tomcat/Ant ở đường dẫn runner hiện có; server test dùng `127.0.0.1:18088` và tự dừng sau suite. Không dùng link này như website local chạy thường trực. Mỗi lần chạy tạo DB test mới và giữ container sau khi dừng. Không chạy runner Phase 3.15 cũ riêng để tránh cơ chế cleanup cũ.

Environment names: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE`, `RAG_ENVIRONMENT`, `RAG_TEST_DATABASE_APPROVED`, `RAG_ENABLED`, `RAG_EMBEDDING_CALLS_APPROVED`, `RAG_CALLS_APPROVED`, `RAG_MODEL_VERIFIED`. **Không cần Gemini key cho các bài kiểm thử này.**

Cho website local thường trực: Admin xác minh đúng database riêng, backup trước rồi áp dụng migration 005 sau 002–004, bằng quyền DDL thích hợp. Phase này chỉ chạy migration trên DB test do runner tạo, không migration DB website hiện tại hay staging.

Sau khi nhận file thật: upload có tùy chọn phân mục → so sánh bản gốc và đủ 9 mục → review title/loại từng mục → giữ rules/config trong proposal queue → tạo Knowledge/FAQ DRAFT đơn chủ đề → submit/approve/publish riêng → mock index → playground với câu hỏi theo tình huống. Đối chiếu source URI và version, kiểm tra không có `AMBIGUOUS_SUBJECT`, không mất mandatory rules, không đưa cấu hình chưa xác minh vào factual knowledge. Không bật RAG live trong bước này.

## 7. Giới hạn và GO/NO-GO

**GO cho kiểm thử local/offline của bản sửa. NO-GO cho nghiệm thu staging hoàn chỉnh ở thời điểm này:** còn thiếu tài liệu thật và kiểm thử/review của Admin trên tài liệu đó; migration 005 và quyền DB staging chưa được áp dụng/xác minh. Sau khi các điều kiện này đạt, có thể đề nghị staging Admin/offline riêng, vẫn `RAG_ENABLED=false`. Không có phê duyệt triển khai trong phase này.

Đã triển khai: archive/download, section preview, năm phân loại, sửa title/loại/skip, lưu draft, FAQ, proposal queue, chunk/source preview, review/publish độc lập, mock retrieval và các guard/test nêu trên.

Chưa triển khai: AI Behavior Management áp dụng system instructions, config automation, classifier AI, editor cây heading/subsection hoặc chọn đoạn tùy ý để chia nhỏ bên trong một section. Nếu một section vẫn chứa nhiều chủ đề thì bị giữ để review; cần chuẩn bị tài liệu có heading con rõ hoặc thiết kế bước chia section thủ công trong phase sau, không bỏ subject policy. Heading detector dựa trên cấu trúc văn bản, không hiểu mọi bố cục PDF/DOCX; extraction có warning cần đối chiếu thủ công.

Chưa chứng minh chất lượng semantic/tỷ lệ trả lời trên tài liệu thật hoặc Gemini thật. Bộ lọc ngữ cảnh vẫn mang tính rule-based/conservative; corpus rộng hơn cần được Admin duyệt và bổ sung. Lịch sử revisions và file gốc được giữ; không tự áp dụng nội dung upload vào các quy tắc an toàn lõi.

Nếu sau này được phê duyệt đưa lên staging: backup DB/files, migration 005 có kiểm soát, deploy artifact đã review, giữ cờ live false, chạy Admin UAT với file thật. Rollback code về artifact trước nếu lỗi; giữ bảng/provenance/files mới để điều tra, không DROP dữ liệu hoặc reset locks. Production/live RAG vẫn NO-GO cho tới phase/phê duyệt riêng.
