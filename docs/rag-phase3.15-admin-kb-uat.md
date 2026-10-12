# PHASE 3.15 — ADMIN KNOWLEDGE BASE UAT & REAL DOCUMENT IMPORT VALIDATION REPORT

**Project:** `D:\miniproject\fpt`  
**Branch:** `feature/rag-integration`  
**Target Website:** FPT Telecom Gia Lai  
**Execution Mode:** **LOCAL / OFFLINE ONLY (`0` Real Gemini API Requests, `0` Supabase Production Connections)**  
**UAT Date:** 2026-10-12  
**Test Environment:** Isolated PostgreSQL `17.10` + `pgvector 0.8.2` + Apache Tomcat `9.0.118` (`127.0.0.1:18088`)

---

## 1. Những chức năng đã thực sự chạy được (Verified Working Functionality)

Qua quá trình khởi chạy thực tế ứng dụng trên **Apache Tomcat 9.0.118** kết nối với container **PostgreSQL 17.10 + pgvector 0.8.2** cô lập (qua kịch bản tự động [`test/run-rag-phase315-uat.ps1`](file:///d:/miniproject/fpt/test/run-rag-phase315-uat.ps1) và [`test/rag/RagPhase315UatTest.java`](file:///d:/miniproject/fpt/test/rag/RagPhase315UatTest.java)), các chức năng sau đã được xác minh hoạt động thật từ giao diện HTTP đến cơ sở dữ liệu:

| Phân khu / Module | URL / Action | Trạng thái thực tế | Bằng chứng xác minh |
|---|---|---|---|
| **Migration `004-admin-knowledge-base.sql`** | PostgreSQL `public.*` | **CHẠY THẬT (REAL DB)** | Đã tạo đầy đủ 6 bảng mới (`knowledge_document_revisions`, `knowledge_document_reviews`, `knowledge_files`, `knowledge_faqs`, `knowledge_faq_revisions`, `admin_audit_logs`), mở rộng 11 cột trên `knowledge_documents`, bật `ENABLE ROW LEVEL SECURITY` và `REVOKE ALL` khỏi `PUBLIC`, `anon`, `authenticated`. |
| **Tab 1 — Tổng quan Dashboard** | `GET /admin/knowledge-base?tab=dashboard` | **CHẠY THẬT (REAL DB)** | Hiển thị `HTTP 200`, đếm chính xác số lượng tài liệu theo từng trạng thái (`DRAFT`, `PENDING_REVIEW`, `APPROVED`, `PUBLISHED`, `REJECTED`, `ARCHIVED`, `UNPUBLISHED`, `PROCESSING_FAILED`), trạng thái chỉ mục (`ready`, `NEEDS_REINDEX`, `failed`), tổng số FAQ, tổng số Chunk và bảng 10 hoạt động kiểm toán gần nhất. |
| **Tab 2 — Tài liệu & Duyệt xuất bản** | `GET/POST /admin/knowledge-base?tab=documents` | **CHẠY THẬT (REAL DB)** | Tìm kiếm/lọc theo từ khóa, danh mục, trạng thái duyệt, trạng thái chỉ mục; tạo/cập nhật tài liệu (`v+1`); chuyển trạng thái `DRAFT -> PENDING_REVIEW -> APPROVED -> PUBLISHED`, `REJECTED` (bắt buộc nhập lý do), `UNPUBLISHED`, `ARCHIVED`; lưu lịch sử revision bất biến; so sánh khác biệt theo dòng (`RevisionDiff`); khôi phục không phá hủy (`Rollback -> v+1` ở trạng thái `DRAFT`). |
| **Tab 3 — Tải lên & Trích xuất tệp** | `POST /admin/knowledge-base` (`multipart/form-data`, `action=uploadDocument`) | **CHẠY THẬT (REAL IO + DB)** | Tải tệp `.txt`, `.md`, `.docx`, `.pdf` (`<= 10 MB`), kiểm tra chữ ký magic bytes, chặn path traversal / double extension / macro VBA / PDF `/JavaScript`, lưu tệp ngoài web root (`${java.io.tmpdir}/fpt-kb-uploads`), trích xuất văn bản UTF-8 vào tài liệu `DRAFT` và lưu metadata vào `public.knowledge_files`. |
| **Tab 4 — Quản lý FAQ** | `GET/POST /admin/knowledge-base?tab=faqs` | **CHẠY THẬT (REAL DB)** | Tạo/sửa FAQ, lưu lịch sử phiên bản (`knowledge_faq_revisions`), thực thi quy trình duyệt (`DRAFT -> PENDING_REVIEW -> APPROVED -> PUBLISHED`), tự động đồng bộ FAQ đã xuất bản sang `public.knowledge_documents` (`source_type='FAQ'`, `source='faq:<id>'`), và tự động thu hồi khỏi RAG ngay khi `UNPUBLISH` hoặc `ARCHIVE`. |
| **Tab 5 — Chunking & Lập chỉ mục** | `GET/POST /admin/knowledge-base?tab=indexing` | **CHẠY THẬT (OFFLINE VECTOR DB)** | Chia đoạn văn bản theo `chunkSize` (`200..1200`) và `chunkOverlap` (`0..400`), hiển thị bảng `ChunkPreview` (`#ordinal`, số ký tự, ước lượng token), và ghi vector `768` chiều + `tsvector` vào bảng `public.knowledge_chunks` thông qua `PgVectorKnowledgeStore`. |
| **Tab 6 — Cấu hình RAG** | `GET/POST /admin/knowledge-base?tab=rag-config` | **CHẠY THẬT (REAL DB)** | Lưu cấu hình `localIndexingEnabled`, `stagingRagEnabled`, `topK`, `similarityThreshold`, `maxContextTokens`, `chunkSize`, `chunkOverlap` vào bảng `public.settings`; khóa cứng `production_rag_enabled = false` ở phía máy chủ. |
| **Tab 7 — Giám sát & Nhật ký Audit** | `GET /admin/knowledge-base?tab=audit` | **CHẠY THẬT (REAL DB)** | Truy vấn và lọc danh sách nhật ký kiểm toán từ `public.admin_audit_logs` kèm `correlation_id`, trạng thái trước/sau dạng JSON đã tự động che (`[REDACTED]`) các trường khóa bí mật và vector. |
| **Tab 8 — RAG Test Playground** | `POST /admin/knowledge-base` (`action=runPlaygroundQuery`) | **CHẠY THẬT (OFFLINE RETRIEVAL)** | Thực thi truy vấn lai (`pgvector` cosine distance + PostgreSQL FTS `ts_rank_cd`) trên `public.knowledge_chunks`, chạy bộ lọc `KnowledgeContentPolicy` và `KnowledgeSourceSelection`, hiển thị danh sách chunk ứng viên, điểm số, quyền hạn nguồn (`authority`), quyết định `ACCEPTED`/lý do từ chối và JSON ngữ cảnh `RagContextBuilder`. |

---

## 2. Phân biệt rõ chức năng chạy Database thật và chức năng Demo / Mock

| Thành phần | Chế độ hoạt động | Mô tả chính xác hành vi hiện tại |
|---|---|---|
| **Tài khoản `demo_admin` (`FPT_DEMO_ADMIN=true`)** | **Chặn truy cập (403)** | Trong `AdminFilter.java` và `DemoData.java`, tài khoản `demo_admin` chỉ được xem dữ liệu giả lập cố định tại `/admin/dashboard`, `/admin/packages`, `/admin/customers`, `/admin/content`, `/admin/business`. Khi truy cập `/admin/knowledge-base`, hệ thống trả về `HTTP 403 Chức năng này không khả dụng trong chế độ Demo.` và không chạm vào DAO/database. |
| **Tài khoản `admin` thật (`role='admin'`)** | **Database PostgreSQL thật** | Mọi thao tác trên 8 phân khu của `/admin/knowledge-base` đọc/ghi trực tiếp vào PostgreSQL thông qua `AdminKnowledgeDAO` và `PgVectorKnowledgeStore`. |
| **Bộ tạo Vector (`MockEmbeddingProvider`)** | **Offline Mock (Tất định)** | Tạo vector chuẩn hóa đơn vị `768` chiều (`gemini-embedding-001`) bằng thuật toán băm đặc trưng từ vựng/bigram/chủ đề cục bộ (`0` cuộc gọi mạng tới Gemini API). Do đó độ tương đồng ngữ nghĩa (`semantic`) trong môi trường offline phản ánh độ khớp từ khóa/chủ đề cục bộ chứ chưa phải mô hình ngôn ngữ thần kinh của Google Gemini. |
| **Câu trả lời mẫu trong Playground (`deterministicMockAnswer`)** | **Offline Mock (Template)** | Tổng hợp câu trả lời kiểm thử từ các chunk đạt chuẩn (`selectedChunks`) bằng mẫu chuỗi tất định tại máy chủ, hoàn toàn không gọi `gemini-3.8-flash` hay `gemini-3.5-flash-lite`. |
| **3 thẻ Telemetry thời gian thực tại Tab 7 (`Abstention Rate`, `Backup Model Fallbacks`, `Provider Errors / Timeouts`)** | **Placeholder (`"Chưa có dữ liệu"`)** | Hiển thị trung thực `"Chưa có dữ liệu"` vì `RAG_ENABLED=false` và `production_rag_enabled=false`, chưa có lưu lượng truy vấn RAG thật từ khách hàng. |

---

## 3. Kết quả kiểm thử tài liệu `văn bản.txt` của Administrator (Task 2)

### 3.1 Trạng thái tệp trong kho mã nguồn & Hướng dẫn tải lên
- **Kiểm tra thực tế:** Tệp `văn bản.txt` của Administrator hiện **chưa tồn tại sẵn** trong thư mục dự án `D:\miniproject\fpt` (đúng theo giả định của Task 2).
- **Hướng dẫn Administrator tải tệp `văn bản.txt` lên giao diện Local:**
  1. Khởi chạy PostgreSQL local đã áp dụng các migration `002`, `003`, `004` và khởi động ứng dụng trên Tomcat 9.
  2. Đăng nhập vào `/login` bằng tài khoản Quản trị viên (`role = 'admin'`).
  3. Truy cập **AI Knowledge Base** trên thanh menu quản trị (`/admin/knowledge-base`) và chọn tab **3. Tải lên & Trích xuất tệp** (`?tab=upload`).
  4. Tại ô **Chọn tệp**, chọn file `văn bản.txt` (mã hóa UTF-8), chọn danh mục (`guide` hoặc `policy`), nhập `Policy Key` (ví dụ `chatbot.operation.guide`) và bấm **Tải lên & Trích xuất vào bản nháp (DRAFT)**.

### 3.2 Kết quả kiểm thử tự động với tệp UTF-8 `văn bản.txt`
Trong kịch bản UAT [`RagPhase315UatTest.java`](file:///d:/miniproject/fpt/test/rag/RagPhase315UatTest.java), hệ thống đã kiểm thử tải lên tệp có tên đúng là `văn bản.txt` (chứa tiếng Việt có dấu và khoảng trắng trong tên tệp) qua cả tầng dịch vụ và giao thức HTTP `multipart/form-data` thật trên Tomcat 9:
- **Xử lý tên tệp Unicode (`văn bản.txt`):** `DocumentExtractionService.sanitizeFilename("văn bản.txt")` chấp nhận hợp lệ tên tệp tiếng Việt có dấu, đồng thời sinh tên tệp lưu trữ vật lý dạng `<UUID>.txt` bên ngoài web root để tránh xung đột hệ thống tệp.
- **Bảo toàn 100% nội dung tiếng Việt có dấu & cấu trúc đánh số:** Nội dung trích xuất khớp tuyệt đối (`100%` byte-for-byte sau chuẩn hóa dòng `LF`) với văn bản gốc: giữ nguyên các tiêu đề `PHẦN I`, `PHẦN II`, `PHẦN III`, các mục đánh số `1.`..`8.`, dấu ngoặc kép và dấu câu tiếng Việt.
- **Lưu Revision & Preview:** Tự động tạo tài liệu ở trạng thái `DRAFT` (`version = 1`), tạo bản ghi bất biến `v1` trong `public.knowledge_document_revisions`, tạo bản ghi tệp trong `public.knowledge_files`, và hiển thị đầy đủ trên giao diện `/admin/knowledge-base?tab=documents&docId=<id>`.
- **Chu trình Duyệt & Gỡ xuất bản qua HTTP:** Đã thực thi thành công chuỗi thao tác `POST` qua HTTP: `submitReview` (`PENDING_REVIEW`) -> `rejectDocument` (`REJECTED` kèm lý do bắt buộc) -> `submitReview` -> `approveDocument` (`APPROVED`) -> `publishDocument` (`PUBLISHED`) -> `reindexDocument` (`ready`) -> `unpublishDocument` (`UNPUBLISHED`).

---

## 4. Phân loại nội dung tài liệu vận hành (`văn bản.txt`) (Task 3)

Tài liệu kịch bản vận hành chatbot gồm 3 nhóm nội dung có bản chất kiến trúc hoàn toàn khác nhau. Hệ thống **tuyệt đối không tự động đưa tài liệu upload vào `systemInstruction`** (đã xác minh `ConsultationPrompt.instructions(ConsultationPrompt.SYSTEM)` không thay đổi trước và sau khi upload/publish).

### Nhóm A — AI Behavior & Consultation Rules (Thuộc tầng `ConsultationPrompt` & `ConsultationPolicy`, KHÔNG đưa vào RAG Chunks)

| Mục nội dung trong tài liệu | Thành phần hệ thống phụ trách hiện tại | Đánh giá & Khuyến nghị xử lý |
|---|---|---|
| **1. Vai trò trợ lý** | `ConsultationPrompt.SYSTEM` (`src/java/ai/consultation/ConsultationPrompt.java`) | Đã được định nghĩa cứng trong prompt hệ thống. Không đưa vào RAG chunk vì không phải tri thức tra cứu theo câu hỏi. |
| **2. Cách xưng hô (`em` - `anh/chị`)** | `ConsultationPrompt.SYSTEM` + `ConsultationEngine.commit(...)` | Đã được kiểm soát bởi prompt hệ thống và bộ hậu xử lý xưng hô trong `ConsultationEngine`. |
| **3. Phong cách giao tiếp** | `ConsultationPrompt.SYSTEM` | Thuộc chỉ dẫn hành vi của mô hình ngôn ngữ (System Prompt). |
| **4. Quy trình tư vấn** | `ConsultationEngine` + `CustomerRequirements` + `ConsultationPolicy` | Đã được thực thi bằng máy trạng thái (state machine) thu thập nhu cầu (`people`, `floors`, `budget`, `television`, `camera`). |
| **5. Quy tắc không bịa thông tin** | `ConsultationPrompt.SYSTEM` + `RagContextBuilder.RULES` + `ConsultationReply.parse(...)` | Đã được cưỡng chế bằng JSON Schema (`knowledgeChunkIds`, `packageIds`) và hậu kiểm tại máy chủ. |
| **6. Chuyển giao khách hàng (Handoff)** | `ConversationSignals.handoff(...)` + `ConsultationEngine.explicitHandoff(...)` + `ConsultationPolicy` | Đã được xử lý tất định tại máy chủ trước khi gọi RAG hoặc LLM. |

> **Cảnh báo kỹ thuật quan trọng:** Nếu Admin giữ nguyên các câu quy tắc thuộc **Nhóm A** bên trong một tài liệu RAG xuất bản, các từ khóa như `system`, `assistant`, `prompt`, `chi dan he thong`, `suy luan noi bo` sẽ bị [`KnowledgeContentPolicy.java`](file:///d:/miniproject/fpt/src/java/rag/KnowledgeContentPolicy.java#L30) chặn với lý do `UNSAFE_CONTENT` (cơ chế chống Prompt Injection từ tài liệu).

### Nhóm B — Knowledge Base & FAQ (Các mục nên chuẩn hóa đưa vào Kho tri thức RAG / FAQ)

| Mục nội dung trong tài liệu | Phân loại đích trong Admin KB | Lưu ý bắt buộc khi biên tập vào RAG |
|---|---|---|
| **1. Các tình huống hỏi giá** | **KHÔNG ĐƯA VÀO RAG** (Thuộc `ProductCatalog` & `/admin/packages` + `/admin/business`) | Trong kiến trúc hiện tại, mọi câu hỏi về giá cước (`195K`..`249K`) và phí lắp đặt (`300.000đ/lần`) được `ConsultationPolicy` trả lời trực tiếp từ Database. Nếu đưa các từ khóa `giá`, `cước`, `phí`, `VNĐ`, `khuyến mãi`, `Mbps` vào RAG chunk, `KnowledgeContentPolicy` sẽ từ chối chunk đó với mã `INVALID_BUSINESS_CLAIM`. |
| **2. Tình huống WiFi yếu** | **FAQ** hoặc **Guide** (`category='guide'`, `policyKey='wifi.troubleshooting'`, chủ đề tiêu đề: `wifi`) | Nên tách thành 1 bài hướng dẫn/FAQ độc lập chuyên về xử lý sóng WiFi yếu (vị trí đặt thiết bị, khởi động lại, kiểm tra vật cản). |
| **3. Hướng dẫn kết nối (2.4GHz / 5GHz)** | **FAQ** hoặc **Guide** (`category='guide'`, `policyKey='wifi.band_selection'`, chủ đề tiêu đề: `wifi`) | Nên tách thành 1 mục FAQ/Guide hướng dẫn chọn băng tần 5GHz (ở gần) và 2.4GHz (ở xa/xuyên tường). |
| **4. Nhu cầu chơi game** | **Guide** hoặc **FAQ** (`category='guide'`, `policyKey='gaming.connection'`, chủ đề tiêu đề: `game`) | Nên tách thành 1 mục độc lập có từ khóa `"chơi game"` trong tiêu đề, khuyến nghị ưu tiên kết nối dây mạng LAN. |
| **5. Dịch vụ Truyền hình** | **FAQ** (`category='faq'`, `policyKey='tv.usage_guide'`, chủ đề tiêu đề: `truyen hinh`) | Chỉ mô tả hướng dẫn sử dụng/kết nối đầu thu và ứng dụng trên Smart TV; **không** ghi giá tiền hay câu `"180 kênh"` / `"Ngoại Hạng Anh"` (vì `KnowledgeContentPolicy` chặn các tuyên bố gói quyền lợi thương mại). |
| **6. Dịch vụ Camera** | **FAQ** (`category='faq'`, `policyKey='camera.usage_guide'`, chủ đề tiêu đề: `camera`) | Mô tả hướng dẫn sử dụng ứng dụng quan sát và lưu trữ đám mây; **không** dùng cụm từ quảng cáo `"giám sát an ninh 24/7"` hay `"bao gồm camera"` (bị chặn bởi `KnowledgeContentPolicy`). |
| **7. Thủ tục lắp đặt** | **FAQ** (`category='faq'`, `policyKey='contract.installation'`, chủ đề tiêu đề: `hop dong`) | Hướng dẫn chuẩn bị giấy tờ tùy thân và quy trình khảo sát/ký hợp đồng điện tử. |
| **8. Các kịch bản hỗ trợ khác (Chuyển địa chỉ / Tạm ngưng)** | **FAQ** (`category='faq'`, `policyKey='contract.modification'`, chủ đề tiêu đề: `hop dong`) | Tách thành các mục FAQ về thủ tục chuyển địa điểm và tạm ngưng hợp đồng có thời hạn. |

### Nhóm C — Admin Configuration (Thuộc Cấu hình Hệ thống / CMS Kinh doanh, KHÔNG đưa vào RAG Chunks)

| Mục nội dung trong tài liệu | Nơi cấu hình đúng trong Admin CMS hiện tại | Ghi chú |
|---|---|---|
| **1. Tên trợ lý** | Chưa có trường riêng (hiện nằm trong `ConsultationPrompt` / giao diện widget chat) | Cần bổ sung vào cấu hình AI nếu muốn đổi động. |
| **2. Người phụ trách** | `/admin/business` (`SiteContent`) hoặc cấu hình thông báo nội bộ | Thuộc thông tin vận hành chi nhánh. |
| **3. Số liên hệ (Hotline / Zalo)** | `/admin/business` (`business.phone`, `business.zalo`) | `ConsultationPolicy.facts(...)` tự động đồng bộ `business.phone` và `business.zalo` vào câu trả lời của chatbot. |
| **4. Giờ làm việc** | `/admin/content` / `/admin/business` | Thuộc thông tin hiển thị chung của website chi nhánh. |
| **5. Khu vực phục vụ (Gia Lai)** | `/admin/content` / `/admin/business` | Thuộc cấu hình địa bàn phục vụ của chi nhánh. |

---

## 5. Những phần còn thiếu trong Admin CMS hiện tại (Honest Gap Analysis)

1. **Chưa có trình quản lý AI Behavior / Consultation Rules có cấu trúc (Structured AI Behavior Management):**
   - Hiện tại `/admin/settings` chỉ có một ô văn bản `system_prompt` thô. Hơn nữa, trong [`ConsultationEngine.java`](file:///d:/miniproject/fpt/src/java/ai/consultation/ConsultationEngine.java#L97), một số nhánh tối ưu chỉ kích hoạt khi `ConsultationPrompt.SYSTEM.equals(systemPrompt)`. Hệ thống chưa có giao diện quản trị tách biệt để Admin cấu hình an toàn từng thuộc tính hành vi (Tên trợ lý, Cách xưng hô, Giờ làm việc hỗ trợ, Quy tắc chuyển giao) mà không làm ảnh hưởng tới cấu trúc prompt JSON Schema.
2. **Chưa có bộ kiểm tra trước xuất bản (Pre-Publish Content Policy Linter) trên giao diện Admin KB:**
   - Hiện tại, Admin có thể tải lên và bấm `Publish` + `Reindex` một tài liệu có chứa các từ bị `KnowledgeContentPolicy` chặn (ví dụ chứa giá tiền hoặc từ `"Gia Lai"` / `"căn cước"`). Tài liệu vẫn báo `PUBLISHED` và `indexing_status = ready`, nhưng đến khi truy vấn trong Playground hoặc Chatbot thì chunk đó mới bị loại với mã `INVALID_BUSINESS_CLAIM` hoặc `UNSAFE_CONTENT`.
3. **Chưa có công cụ tách tài liệu tự động (Playbook / Multi-FAQ Splitter):**
   - Khi Admin tải lên một tệp kịch bản tổng hợp như `văn bản.txt` chứa nhiều chủ đề (WiFi, Game, Truyền hình, Camera, Hợp đồng), hệ thống hiện lưu thành **1 tài liệu duy nhất** (`1 document_id`), thay vì hỗ trợ tách tự động thành các bản nháp FAQ/Guide đơn chủ đề.

---

## 6. Kết quả kiểm thử UI, Backend & Security (Tasks 4 & 5)

### 6.1 Kết quả chạy thực tế `RagPhase315UatTest` (PostgreSQL 17.10 + Tomcat 9.0.118 HTTP)

| # | Kịch bản kiểm thử UAT | Giao thức | Kết quả |
|---:|---|---|---|
| 1 | Xác minh toàn bộ bảng, cột, chỉ mục và chính sách `ROW LEVEL SECURITY` của migration `004` trên PostgreSQL 17.10 | JDBC (`information_schema`, `pg_tables`) | **PASS** |
| 2 | Upload tệp tiếng Việt UTF-8 `văn bản.txt`, bảo toàn dấu tiếng Việt, các phần `PHẦN I..III` và mục đánh số trong `DRAFT` (`v1`) | Service + JDBC | **PASS** |
| 3 | Xác minh upload và xuất bản `văn bản.txt` không làm thay đổi `ConsultationPrompt` system instructions; kiểm tra hành vi lọc của `KnowledgeContentPolicy` trên tài liệu nguyên khối và kiểm tra thu hồi ngay khi `UNPUBLISH` | Service + JDBC | **PASS** |
| 4 | Chuẩn hóa các mục thuộc Phần II (`văn bản.txt`) thành Guide/FAQ đơn chủ đề, duyệt -> xuất bản -> lập chỉ mục offline -> truy xuất thành công trong Playground; đảm bảo giá gói (`195.000đ/tháng`) và phí lắp đặt (`300.000đ`) vẫn thuộc quyền kiểm soát tuyệt đối của `ConsultationPolicy` | Service + Engine + JDBC | **PASS** |
| 5 | Kiểm tra chống ghi đè đồng thời (Optimistic Locking `WHERE id=? AND version=?`) khi cập nhật revision | JDBC | **PASS** |
| 6 | Truy cập `/admin/knowledge-base` khi chưa đăng nhập bị chuyển hướng `HTTP 302` về `/login` | Live HTTP (`Tomcat 9`) | **PASS** |
| 7 | Truy cập trực tiếp tệp `/view/admin/knowledge-base.jsp` bị chặn (`HTTP 403/302`) | Live HTTP (`Tomcat 9`) | **PASS** |
| 8 | Đăng nhập Admin thật qua `POST /login`, thiết lập phiên `JSESSIONID` và lấy mã bảo vệ `csrf_token` | Live HTTP (`Tomcat 9`) | **PASS** |
| 9 | Truy cập toàn bộ 8 tab (`dashboard`, `documents`, `upload`, `faqs`, `indexing`, `rag-config`, `audit`, `playground`) trả về `HTTP 200` với tiếng Việt UTF-8 chuẩn xác | Live HTTP (`Tomcat 9`) | **PASS** |
| 10 | Upload tệp `văn bản.txt` qua biểu mẫu `multipart/form-data` `POST /admin/knowledge-base` (chỉ truyền `csrf_token` trong phần thân multipart giống hệt trình duyệt), chuyển hướng `HTTP 302` tới trang chi tiết tài liệu và hiển thị đầy đủ nội dung trích xuất | Live HTTP (`Tomcat 9`) | **PASS** |
| 11 | Thực thi trọn vẹn chu trình `submitReview` -> `rejectDocument` (kèm lý do tiếng Việt) -> `submitReview` -> `approveDocument` -> `publishDocument` -> `reindexDocument` -> xem `ChunkPreview` -> `unpublishDocument` qua `POST /admin/knowledge-base` | Live HTTP (`Tomcat 9`) | **PASS** |
| 12 | Chặn tải lên tệp có phần mở rộng kép nguy hiểm (`văn bản.jsp.txt`) qua `multipart/form-data`, trả về `HTTP 400` và hiển thị thông báo lỗi `.kb-alert-error` | Live HTTP (`Tomcat 9`) | **PASS** |
| 13 | Bảo mật HTTP: thiếu `csrf_token` trả về `HTTP 403`; dùng `GET ?action=delete` trả về `HTTP 405`; mã độc XSS `<script>alert('xss')</script>` trong Playground bị mã hóa thành `&lt;script&gt;`; cố tình gửi `productionRagEnabled=true` bị chặn với `HTTP 400` và ghi nhận `BLOCKED_PRODUCTION_RAG_ENABLE` vào `admin_audit_logs` | Live HTTP (`Tomcat 9`) | **PASS** |

### 6.2 Kiểm tra Giao diện Desktop / Mobile & Thông báo lỗi
- **Responsive Desktop & Mobile (`web/assets/admin/admin-kb.css`):**
  - Trên màn hình Desktop (`> 1100px`): Lưới 2 cột (`.kb-grid-2`) hiển thị song song danh sách/biểu mẫu và bảng lịch sử/diff/preview; thanh 8 tab (`.kb-subnav`) hiển thị ngang rõ ràng.
  - Trên màn hình Tablet/Mobile (`@media (max-width: 1100px)` và `@media (max-width: 768px)`): `.kb-grid-2` và `.kb-form-grid` tự động chuyển về 1 cột (`grid-template-columns: 1fr`), các bảng dữ liệu lớn nằm trong `.kb-table-wrap` (`overflow-x: auto`) giúp cuộn ngang mượt mà không vỡ khung trang.
- **Thông báo lỗi & Flash Message:**
  - Mọi lỗi nghiệp vụ/bảo mật (`IllegalArgumentException`, `IllegalStateException`) được bắt an toàn tại `AdminKnowledgeBaseServlet`, trả về `HTTP 400` kèm hộp cảnh báo màu đỏ `.kb-alert-error` bằng tiếng Việt rõ ràng, không lộ stack trace hay thông tin kết nối DB.

---

## 7. Các lỗi phát hiện trong UAT & Các giới hạn còn tồn tại (Section 8)

### 7.1 Lỗi đã phát hiện và khắc phục ngay trong Phase 3.15 (`FIXED`)
- **`BUG-315-01` — Thiếu `ENABLE ROW LEVEL SECURITY` và `REVOKE ALL` trên 6 bảng mới trong `004-admin-knowledge-base.sql`:**
  - *Phát hiện:* Khi chạy kiểm thử `RagPhase315UatTest` đối chiếu `pg_tables.rowsecurity`, 6 bảng mới của Phase 3 chưa bật RLS như 3 bảng của `002-rag-mvp.sql`.
  - *Khắc phục:* Đã bổ sung `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` và `REVOKE ALL ... FROM PUBLIC, anon, authenticated` vào [`database/migrations/004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql#L165-L186) và xác minh `PASS` trên PostgreSQL 17.10.

### 7.2 Các giới hạn hiện tại cần lưu ý (`OPEN / BY DESIGN`)
1. **`LIM-315-01` — Bộ lọc không dấu trong `KnowledgeContentPolicy.java` chặn nhầm một số từ tiếng Việt thông dụng (`"Gia Lai"`, `"gia đình"`, `"căn cước"`, `"ngắn hạn"`):**
   - *Nguyên nhân:* [`KnowledgeContentPolicy.java`](file:///d:/miniproject/fpt/src/java/rag/KnowledgeContentPolicy.java#L29-L33) gọi `ProductCatalog.normalize(text)` (bỏ toàn bộ dấu tiếng Việt) rồi kiểm tra biểu thức chính quy `\b(gia|cuoc|phi|nghin|ngan|trieu|...)\b` nhằm chặn các tuyên bố về **giá**, **cước**, **phí**, **ngàn**, **triệu**. Tuy nhiên khi bỏ dấu:
     - `"Gia Lai"`, `"gia đình"`, `"tham gia"`, `"đánh giá"`, `"gia hạn"` đều chứa từ `"gia"` -> bị từ chối với lý do `INVALID_BUSINESS_CLAIM`.
     - `"căn cước công dân"` chứa từ `"cuoc"` -> bị từ chối với lý do `INVALID_BUSINESS_CLAIM`.
     - `"ngắn hạn"` chứa từ `"ngan"` -> bị từ chối với lý do `INVALID_BUSINESS_CLAIM`.
   - *Tác động:* Nếu Admin nhập các từ `"Gia Lai"` hoặc `"căn cước công dân"` vào nội dung FAQ/Guide, chunk đó vẫn được lưu và lập chỉ mục nhưng sẽ bị bộ lọc `KnowledgeContentPolicy` loại bỏ ở bước truy xuất (`INVALID_BUSINESS_CLAIM`).
2. **`LIM-315-02` — Ràng buộc đơn chủ đề trên tiêu đề tài liệu trong `KnowledgeSourceSelection.java`:**
   - *Nguyên nhân:* Khi có từ 2 tài liệu trở lên cùng khớp từ khóa tìm kiếm (`subjects.size() > 1`), [`KnowledgeSourceSelection.java`](file:///d:/miniproject/fpt/src/java/rag/KnowledgeSourceSelection.java#L56-L64) gọi `RetrievalQuery.topics(c.title)` trên từng tiêu đề ứng viên và yêu cầu `domain.size() == 1`.
   - *Tác động:* Nếu một tài liệu có tiêu đề chứa từ 2 chủ đề trở lên (ví dụ: `"Hướng dẫn xử lý WiFi yếu và tối ưu khi chơi game"` chứa cả `wifi` và `game`), hoặc không chứa từ khóa chủ đề nào trong danh sách (`camera`, `mesh`, `wifi`, `game`, `truyen hinh`, `internet`, `hop dong`), bộ chọn nguồn sẽ trả về `AMBIGUOUS_SUBJECT` khi có nhiều tài liệu ứng viên cùng xuất hiện.

---

## 8. Đề xuất cho Phase 3.16 (Recommendations)

1. **Tinh chỉnh bộ lọc tiếng Việt có dấu trong `KnowledgeContentPolicy` (`Phase 3.16.1`):**
   - Kiểm tra các từ khóa tiền tệ/giá cước dựa trên văn bản NFC tiếng Việt có dấu (`\b(giá|cước\s+phí|giá\s+cước|mức\s+cước|gói\s+cước|phí|nghìn|ngàn|triệu|vnđ|vnd|miễn\s+phí|ưu\s+đãi|khuyến\s+mãi|giảm\s+giá)\b`) hoặc loại trừ tường minh các cụm từ hợp lệ không phải giá tiền (`gia lai`, `gia dinh`, `tham gia`, `danh gia`, `gia han`, `can cuoc`, `ngan han`) trước khi kiểm tra không dấu.
2. **Bổ sung Pre-Publish Policy & Topic Linter ngay trên giao diện Admin KB (`Phase 3.16.2`):**
   - Khi Admin tạo/sửa hoặc bấm `Approve`/`Publish` tài liệu và FAQ, chạy kiểm tra trước mỗi chunk qua `KnowledgeContentPolicy.reason(...)` và `RetrievalQuery.topics(title)`:
     - Hiển thị cảnh báo trực quan nếu chunk chứa từ khóa bị chặn (`INVALID_BUSINESS_CLAIM`, `UNSAFE_CONTENT`) hoặc tiêu đề chứa `0` / `>1` chủ đề dễ gây `AMBIGUOUS_SUBJECT`.
3. **Bổ sung trình quản lý AI Behavior & Playbook Splitter (`Phase 3.16.3`):**
   - Cung cấp giao diện cấu hình có cấu trúc cho **Nhóm A (AI Behavior)** và công cụ tách tự động tệp kịch bản nhiều mục (`văn bản.txt`) thành từng bản nháp FAQ/Guide đơn chủ đề.

---

## 9. Kết luận & Quyết định GO / NO-GO cho Staging (Task 6.10)

- **Đối với môi trường Local / Staging kiểm thử nội bộ (Offline Mode):** **CONDITIONAL GO**
  - Toàn bộ 8 phân khu của `/admin/knowledge-base`, migration `004`, cơ chế phân quyền Admin, chống CSRF/XSS, kiểm tra an toàn tệp tải lên, quy trình duyệt xuất bản có phiên bản và RAG Playground Offline đều hoạt động ổn định và vượt qua **100%** các bài kiểm thử tự động (`13/13` trong `RagPhase315UatTest`, `10/10` trong `RagPhase3PostgresTest`, `17/17` trong `RagAdminKbTest`).
  - *Điều kiện đi kèm khi nhập liệu trên Staging:* Không xuất bản nguyên khối tệp `văn bản.txt` vào RAG; cần tách riêng các mục thuộc **Phần II (Knowledge Base & FAQ)** thành từng bài Guide/FAQ đơn chủ đề và lưu ý giới hạn từ khóa `LIM-315-01` / `LIM-315-02` cho tới khi triển khai Phase 3.16.
- **Đối với môi trường Production:** **NO-GO (CHƯA TRIỂN KHAI PRODUCTION)**
  - Hệ thống chưa thực hiện bước kiểm thử Gemini API thật (`Phase 2.4.9`), đang dùng `MockEmbeddingProvider` offline, và cờ `RAG_ENABLED=false` cùng `production_rag_enabled=false` đang được khóa cứng an toàn.
