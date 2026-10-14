# Phase 6.2 — Context-aware sales conversation

Ngày thực hiện: 2026-10-14

Branch kiểm tra: `main`

Phạm vi: local/offline; không gọi Gemini, không kết nối Supabase production, không commit/push/deploy.

## 1. Nguồn đã đối chiếu

- `HƯỚNG DẪN VẬN HÀNH CHATBOT – PHÚC.pdf`: đọc đủ 10 trang. Các nguyên tắc được dùng gồm trả lời trực tiếp, mỗi lượt chỉ hỏi một câu cần thiết, không lặp dữ liệu khách đã cung cấp và không tuyên bố thao tác chưa xảy ra.
- `kịch bản chatbot testfinal.docx`: trích xuất đủ 156 đoạn/23.048 ký tự để đối chiếu kịch bản và dữ liệu thương mại. Môi trường không có LibreOffice nên không render trực quan DOCX; nội dung văn bản vẫn đọc được.
- Bài viết `haipham.vn/kich-ban-tu-van-khach-hang-thoi-mien-duoc-soan-thao-boi-ai/`: chỉ dùng các ý chung về hiểu nhu cầu, giới hạn lựa chọn và xử lý phản đối theo ngữ cảnh. Không dùng nội dung “thôi miên”, gây sợ hãi, khan hiếm giả, ép chọn hoặc suy đoán tâm lý.

PDF/DOCX và bài viết không được dùng để ghi đè bảng giá runtime. Giá, tốc độ, Mesh và khuyến mãi tiếp tục lấy từ `ProductCatalog` và cấu hình business đã duyệt.

## 2. Audit đường tạo câu trả lời thật

Các file đã kiểm tra gồm:

- `src/java/ai/AIService.java`
- `src/java/ai/consultation/ConsultationEngine.java`
- `src/java/ai/consultation/ConsultationPolicy.java`
- `src/java/ai/consultation/ConsultationPrompt.java`
- `src/java/ai/consultation/ConsultationReply.java`
- `src/java/ai/consultation/ProductCatalog.java`
- `src/java/ai/consultation/CustomerRequirements.java`
- `src/java/ai/consultation/ConversationSignals.java`
- `src/java/ai/provider/GeminiProvider.java`
- `src/java/rag/KnowledgeSourceSelection.java`
- `src/java/rag/RagContextBuilder.java`
- `src/java/rag/KnowledgeContentPolicy.java`
- `src/java/ai/session/ChatSessionData.java`
- `src/java/controller/AIChatServlet.java`
- `web/js/ai-chat.js`

Luồng public chat là non-streaming. `GeminiProvider` tách `system_instruction`, dùng response schema khi model hỗ trợ và loại thought parts. Frontend chỉ render trường nội dung đã xác thực bằng `textContent`, nên không phải nguồn gây câu dang dở hoặc lộ JSON.

Nguyên nhân đã xác nhận:

1. `ConsultationPolicy.render()` nối mọi `packageIds` và `recommendedPackageId` từ model vào câu trả lời. Một câu về AI, hỗ trợ hoặc quyền riêng tư có thể bị nối quảng cáo gói không liên quan.
2. `renderSolution()` luôn thêm nhiều dòng quyền lợi, khuyến mãi chung, cảnh báo và phương án phụ. Câu trả lời đúng số liệu nhưng dài hơn câu hỏi yêu cầu.
3. `ConsultationEngine` chạy ghép giải pháp trước một số câu hỏi giá/tốc độ hẹp. Vì vậy “1000Mbps thì bao nhiêu?” có thể nhận lại toàn bộ bản đề xuất thay vì câu trả lời đầu dòng.
4. Memory đã giữ nhu cầu cốt lõi nhưng chưa ghi nhận việc từ chối trả trước, loại phản đối, vấn đề vùng phủ, tên khách được nói rõ và giai đoạn hội thoại.
5. Validator cố ý cấm model tự phát biểu số liệu thương mại. Cơ chế này đúng về nguồn dữ liệu, nhưng khi thiếu nhánh server an toàn, một phản hồi bình thường bị loại sẽ rơi vào fallback chung. Bản sửa bổ sung nhánh có dữ liệu xác định thay vì nới lỏng bộ lọc giá, bí mật hoặc nội dung nội bộ.

## 3. Thay đổi backend

### Intent và độ sâu câu trả lời

Thêm `ConversationIntent` để phân loại bảo thủ các nhóm chào hỏi, giá trực tiếp, so sánh, đề xuất, Mesh, kỹ thuật, TV/bóng đá, camera, phí lắp, ưu đãi, hai loại phản đối giá, so sánh đối thủ, từ chối trả trước, đăng ký, hỗ trợ, khiếu nại, riêng tư, nhận dạng AI và ngoài phạm vi.

Intent này được dùng để giới hạn catalog server append:

- So sánh: tối đa 6 gói.
- Đề xuất: tối đa 2 gói.
- Hỏi giá trực tiếp: tối đa 1 gói.
- Các intent khác: không nối package nào và không ghi package model đề xuất vào memory.

### Trả lời trực tiếp

`ConsultationEngine` gọi nhánh factual hẹp trước nhánh ghép giải pháp. Các câu về F2, 1000Mbps, camera, vùng phủ tầng 3, hạ tầng, khiếu nại và quyền riêng tư có câu trả lời ngắn theo cấu trúc: trả lời chính → lý do liên quan → bước tiếp theo nếu cần.

### Memory

`CustomerRequirements` bổ sung các trường có giới hạn:

- `customerName`: chỉ nhận khi khách tự nói theo mẫu tên rõ ràng; không thu số điện thoại/email.
- `coverageIssue`.
- `conversationStage`.
- `prepaymentRejected`.
- `installationPriceObjected`.
- `monthlyPriceObjected`.
- `competitorCompared`.

Memory vẫn thuộc từng `ChatSessionData`, tồn tại khi history bị trim và bị xóa khi reset session. Không thêm dữ liệu liên hệ hay dùng text tự do làm chỉ dẫn.

### Phản đối và câu hỏi tiếp theo

- Phản đối 700K giải thích đúng tổng phí F2, 2 Access Point, không cộng thêm 300K; chỉ nêu giảm trả trước một lần khi chưa bị từ chối; đưa F1/Internet thường như lựa chọn có đánh đổi vùng phủ.
- Phản đối 259K/tháng chuyển sang 220K + F2 = 240K/tháng khi phù hợp, giữ truyền hình và không camera, đồng thời nói rõ 300Mbps thay cho 1000Mbps.
- Mức giá đối thủ được đánh dấu là thông tin khách cung cấp; hệ thống không bịa thông số của đối thủ.
- Sau khi khách từ chối trả trước, bản đề xuất sau không nhắc lại ưu đãi trả trước.
- “Để suy nghĩ” kết thúc nhẹ nhàng và giữ phương án hiện tại, không đặt câu hỏi ép tiếp.
- Câu hỏi discovery chỉ xuất hiện khi intent và dữ liệu còn thiếu thực sự cần nó. Số người/thiết bị được hỏi trước; khi đã có, hệ thống mới hỏi một câu về truyền hình/camera nếu cần.

## 4. Prompt và Knowledge Markdown

Đã sửa:

- `config/chatbot-system-prompt.md`: thêm answer-first, độ dài 1–2 câu cho câu hỏi đơn giản, giới hạn lựa chọn và không quảng cáo ngoài chủ đề.
- `knowledge/faq/provider-comparison-internet.md`: version 2 → 3; phân biệt phí một lần/cước tháng và ghi rõ giá đối thủ do khách cung cấp chưa phải dữ liệu hệ thống xác minh.
- `knowledge/faq/new-internet-registration.md`: version 3 → 4; chỉ hỏi dữ liệu còn thiếu và không hỏi lại thông tin đã có.

Hướng dẫn hành vi cốt lõi vẫn nằm trong code/System Prompt tin cậy. Không đưa chỉ dẫn vận hành thành tài liệu RAG có thể ghi đè hệ thống. Không sao chép nguyên bài viết bên ngoài.

## 5. Ví dụ trước và sau

**Trước:** “1000Mbps thì giá sao?” có thể mở toàn bộ bản ghép giải pháp và quảng bá quyền lợi không được hỏi.

**Sau:** “Dạ Gói 239K có tốc độ 1000Mbps, giá 239.000đ/tháng. Với Mesh F2 theo nhu cầu nhà nhiều tầng đã ghi nhận, tổng là 259.000đ/tháng.”

**Trước:** model trả `packageIds` trong câu về AI có thể làm máy chủ nối danh sách gói.

**Sau:** “Dạ em là Tư vấn viên AI của Phúc…”; recommendation và package IDs bị bỏ qua cho intent AI identity.

**Trước:** phản đối 700K có thể đi kèm TV, khuyến mãi dài và câu hỏi bán hàng.

**Sau:** trả lời đúng tổng phí F2, thiết bị gồm 2 Access Point, cách tính không cộng 300K và lựa chọn F1/Internet thường.

## 6. File thay đổi

- `src/java/ai/consultation/ConversationIntent.java` (mới)
- `src/java/ai/consultation/ConversationSignals.java`
- `src/java/ai/consultation/CustomerRequirements.java`
- `src/java/ai/consultation/ConsultationEngine.java`
- `src/java/ai/consultation/ConsultationPolicy.java`
- `src/java/ai/consultation/ConsultationPrompt.java`
- `config/chatbot-system-prompt.md`
- `knowledge/faq/provider-comparison-internet.md`
- `knowledge/faq/new-internet-registration.md`
- `test/ai/Phase62ContextAwareConversationTest.java` (mới)
- `test/run-phase62-offline.ps1` (mới)
- `docs/phase6.2-context-aware-sales-conversation.md` (mới)
- `dist/fpt-sale.war` (build artifact được tạo lại từ source đã sửa)

Không sửa frontend, bảng giá, database schema, credential, model ID, token limit hoặc RAG release gate.

## 7. Kết quả kiểm thử thực tế

| Kiểm tra | Kết quả |
|---|---:|
| 12 tình huống A–L | 12/12 PASS |
| Renderer package-spam + structured memory/Markdown | 2/2 PASS |
| Phase 6.0 regression | 14/14 PASS |
| Combined-solution regression | 10/10 PASS |
| Phase 6.1 regression | 10/10 PASS |
| Build Java/WAR | PASS |
| System Prompt parse | PASS, 5.173 ký tự |
| Knowledge Markdown parse | PASS, 18 files |

Tổng các assertion suite được chạy: **48/48 PASS**. Gemini API calls: **0**. Supabase/PostgreSQL connections or writes: **0**. Registration writes: **0**. WAR tạo tại `dist/fpt-sale.war`.

## 8. RAG synchronization

Hai file Markdown thay version cần đi qua workflow sync/index hiện có sau khi release được duyệt. Phase này chỉ parse và đóng gói file vào WAR; chưa đồng bộ database và chưa tạo embedding. Giá, Mesh và promotion vẫn do catalog/business config quyết định ngay cả khi Markdown được index.

## 9. Giới hạn còn lại

- Kiểm thử dùng provider `null` và dữ liệu catalog deterministic; chưa đánh giá cách diễn đạt thực tế của Gemini sau release.
- Khả dụng hạ tầng theo địa chỉ chưa có API kiểm tra tự động; chatbot tiếp tục nói rõ chưa xác minh.
- Promotion chưa có ngày hết hiệu lực được xác nhận; hệ thống không bịa ngày.
- Tên khách chỉ nằm trong session tư vấn hiện tại và không phải xác minh danh tính.
- Phản hồi recommendation vẫn dài hơn câu factual vì cần giữ tổng tháng, phí lắp, quyền lợi chính và điều kiện vùng phủ.

Trạng thái local: **DIRECT ANSWERS FIRST — CONTEXTUAL CONSULTATION — MINIMAL REPETITION — CORRECT PRICES — NATURAL CUSTOMER CONVERSATIONS.**
