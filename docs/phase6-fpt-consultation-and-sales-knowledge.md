# Phase 6.0 — FPT Sales Knowledge and Intelligent Consultation Backend

Ngày kiểm tra: 2026-10-13  
Branch: `feature/rag-integration`  
Phạm vi thực thi: local/offline. Không gọi Gemini thật, không kết nối hoặc ghi Supabase production, không commit/push/deploy.

## 1. Kết quả audit

- Dữ liệu gói khách hàng nhìn thấy được lấy từ `PackageDAO` qua `SiteContent.publicCatalog()` và `ProductCatalog`; Gemini không phải nguồn giá.
- Cấu hình phí Internet và Mesh lấy từ nhóm `business.*` của CMS. Giá trị mặc định hiện là 300.000đ, F1 500.000đ + 10.000đ/tháng, F2 700.000đ + 20.000đ/tháng.
- `ConsultationEngine` gọi `ConsultationPolicy` trước provider cho giá, đăng ký và phương án kết hợp. Đây là đường chạy thật đã được sửa.
- `CustomerRequirements` là bộ nhớ có kiểu theo từng `ChatSessionData`; lịch sử được lưu theo cặp user/assistant và cắt gọn độc lập giữa các session.
- File Markdown trong `knowledge/` được đóng gói vào WAR. `FileKnowledgeRuntime` chỉ đồng bộ khi `KB_FILE_AUTO_SYNC=true` và có `KB_FILE_SYNC_ACTOR_ID` hợp lệ. Đồng bộ file chỉ tạo/đưa bản sửa vào hàng đợi; indexing Gemini là bước riêng có khóa và quota gate.
- Truy vấn giá/phí/ưu đãi được đánh dấu là business và đi qua catalog/backend, không để RAG ghi đè dữ liệu có cấu trúc.

Lỗi chính đã xác nhận: `RecommendedSolution` cộng phí Internet tiêu chuẩn với phí Mesh, dẫn đến F2 có thể thành 1.000.000đ. Logic đăng ký cũng coi câu “Tôi muốn lắp mạng” là consent đăng ký, còn bộ nhớ thiếu khu vực, Smart TV, bóng đá và tốc độ mong muốn.

## 2. Tri thức Markdown được cập nhật

- `knowledge/products/internet-packages.md`: thêm nguồn official cho sáu gói, tốc độ, modem và quyền lợi đúng theo dữ liệu chủ sở hữu cung cấp.
- `knowledge/products/wifi-multifloor-coverage.md`: tăng version 1 → 2, bổ sung F1/F2 và nguyên tắc phí Mesh thay thế phí thường.
- `knowledge/policies/contract-payment-safety.md`: tăng version 1 → 2, bổ sung trả trước 6/12 tháng, mức giảm 200.000đ và 1 tháng thêm cho kỳ 12 tháng; ghi rõ hiệu lực và khả năng áp dụng cho Mesh chưa được xác nhận.
- `knowledge/faq/provider-comparison-internet.md`: tăng version 1 → 2, thêm cách so sánh nhà mạng trung lập.
- `knowledge/faq/new-internet-registration.md`: tăng version 1 → 2, thêm trình tự khám phá nhu cầu và ranh giới xác minh hạ tầng/đăng ký.

Nguồn official trong repository được phép chứa dữ liệu thương mại qua một ngoại lệ hẹp: `source` phải bắt đầu bằng `repo:` và `source_authority` phải là `official`. Upload Admin và nội dung không có provenance vẫn bị chặn khi chứa giá hoặc quyền lợi. Kiểm tra prompt injection vẫn áp dụng cho mọi nguồn, kể cả official.

## 3. Dữ liệu thương mại được ghi nhận

| Gói | Cước/tháng | Tốc độ | Camera | TV/FPT Play/Ngoại Hạng Anh |
|---|---:|---:|---|---|
| 195K | 195.000đ | 300Mbps | Không | Không |
| 205K | 205.000đ | 300Mbps | 1 | Không |
| 220K | 220.000đ | 300Mbps | Không | Có |
| 230K | 230.000đ | 300Mbps | 1 | Có |
| 239K | 239.000đ | 1000Mbps | Không | Có |
| 249K | 249.000đ | 1000Mbps | 1 | Có |

Nội dung thể thao được mô tả là phụ thuộc quyền phát sóng và điều kiện còn hiệu lực. Không thêm phí, ưu đãi hoặc cam kết vùng phủ khác.

Seed database local trước đây ghi 220K/230K là 500Mbps; `database/local/schema.sql` đã được sửa thành 300Mbps cho database local mới. Demo data đã là 300Mbps. Supabase production không được truy vấn hay chỉnh sửa, nên quản trị viên vẫn phải đối chiếu hai bản ghi này trước release.

## 4. Sửa phép tính phí Mesh

`CommercialPriceValidator` thực hiện phép tính có giới hạn trên máy chủ:

- Internet thường: phí lắp 300.000đ/lần.
- F1: tổng phí lắp 500.000đ/lần, thay cho 300.000đ; phụ thu 10.000đ/tháng.
- F2: tổng phí lắp 700.000đ/lần, thay cho 300.000đ; phụ thu 20.000đ/tháng.
- Tổng tháng hợp lệ: 195K + F1 = 205K; 195K + F2 = 215K; 220K + F2 = 240K; 239K + F2 = 259K.
- Không còn đường tính 300K + 500K hoặc 300K + 700K.
- Nếu phụ thu Mesh thiếu, tổng tháng trả về trạng thái chưa xác định thay vì coi phụ thu bằng 0.
- Giảm 200.000đ chỉ được trừ khi eligibility đã xác nhận. Hiện eligibility cho Mesh chưa xác nhận nên tổng phí lắp không bị giảm.

`RecommendedSolution` hiện tách `standardInternetInstallationFee`, `installationFee`, `confirmedPromotion`, `finalInstallationFee`, `sourceIds` và `missingConditions`.

## 5. Logic tư vấn và bộ nhớ

- “Tôi muốn lắp mạng” khởi tạo tư vấn và chỉ hỏi một câu về loại nhà/số tầng; không mở form và không đổ bảng sáu gói.
- Bộ nhớ ghi nhận số tầng, số người/thiết bị, khu vực xã/phường ở mức giới hạn, Smart TV, bóng đá, camera có/không, Mesh, tốc độ mong muốn và gói đã chọn.
- Với chuỗi 3 tầng + 5 người + bóng đá + không camera + Tuy Phước, backend chọn 220K + F2 = 240.000đ/tháng và đưa đúng một lựa chọn tốc độ cao 239K + F2 = 259.000đ/tháng.
- Câu tiếp theo “1000Mbps thì giá sao?” giữ nguyên F2, TV/bóng đá và không camera, rồi chọn 239K.
- Gói có camera bị loại khi khách đã nói rõ không lấy camera.
- Khu vực được nhớ để duy trì hội thoại nhưng không được dùng làm bằng chứng hạ tầng đã kiểm tra.
- “Chốt gói 259K” được phân giải thành gói 239K + F2. Backend mở form đăng ký hiện có, không tạo gói 259K và không báo đăng ký thành công.

## 6. Xử lý phản đối

- Phản đối 700K: xác nhận mối quan tâm, nói rõ đây là tổng phí F2 và không cộng 300K, giải thích thiết bị, đưa F1/Internet thường như lựa chọn cân nhắc và hỏi một câu.
- Hỏi F1 cho nhà ba tầng: nêu cấu hình và chi phí F1, không bảo đảm phủ sóng, hỏi khách ưu tiên chi phí hay vùng phủ.
- Đối thủ rẻ hơn: xem đó là thông tin do khách cung cấp, so sánh cấu hình tương đương và không hạ thấp nhà mạng khác.
- “Giá cao quá”: hỏi rõ khách đang lo cước tháng hay phí lắp.
- “Để anh suy nghĩ”: không gây áp lực và giữ lại phương án đang cân nhắc.
- Không muốn trả trước: tôn trọng, không tiếp tục đẩy ưu đãi.
- Hỏi ưu đãi: nêu đúng dữ liệu bảng, nhưng không trừ phí Mesh hoặc cam kết hiệu lực khi chưa xác minh.

## 7. Ví dụ trước và sau

**Trước:** F2 có thể hiển thị phí Internet 300.000đ + phí Mesh 700.000đ = 1.000.000đ.  
**Sau:** “Tổng phí lắp áp dụng: 700.000đ/lần (đã thay cho phí Internet thông thường, không cộng thêm 300.000đ).”

**Trước:** “Tôi muốn lắp mạng” có thể bị coi là yêu cầu đăng ký.  
**Sau:** “Dạ em là Tư vấn viên AI của Phúc… Nhà mình là nhà cấp 4 hay có mấy tầng vậy anh/chị?”

**Sau chuỗi đầy đủ:** gói chính 220K + F2 = 240.000đ/tháng, phí lắp 700.000đ; lựa chọn 1000Mbps là 239K + F2 = 259.000đ/tháng. Phản hồi nói rõ hạ tầng chưa được xác minh và quyền nội dung thể thao còn tùy điều kiện hiệu lực.

## 8. File backend và kiểm thử thay đổi

- `src/java/ai/consultation/CommercialPriceValidator.java`
- `src/java/ai/consultation/RecommendedSolution.java`
- `src/java/ai/consultation/CustomerRequirements.java`
- `src/java/ai/consultation/ConsultationPolicy.java`
- `src/java/ai/consultation/ConsultationEngine.java`
- `src/java/rag/KnowledgeContentPolicy.java`
- `src/java/rag/importer/DocumentSectionService.java`
- `src/java/rag/files/KnowledgeFile.java`
- `src/java/rag/KnowledgeIndexingService.java`
- `src/java/dao/KnowledgeLifecycleDAO.java`
- `src/java/dao/AdminKnowledgeDAO.java`
- `database/local/schema.sql`
- Các fixture/assertion liên quan trong `test/ai/`
- `test/ai/Phase6ConsultationTest.java`
- `test/run-phase6-offline.ps1`
- `test/run-chat-tests.ps1` (bổ sung classpath resource đã build để runner đọc System Prompt)

Không sửa Admin UI, OAuth, Resend, production credentials, schema Supabase hoặc luồng ghi khách hàng.

## 9. Kết quả kiểm tra thực tế

| Lệnh | Kết quả |
|---|---|
| `build.bat dist` | PASS; 128 Java source; 18 knowledge files parse; WAR tạo thành công |
| `test/run-phase6-offline.ps1` | PASS 14/14 Phase 6 + 10/10 combined-solution regression |
| `test/run-chat-tests.ps1` | PASS 33 consultation + 21 adaptive + 17 installation + 9 registration + 11 provider + 6 frontend |
| `test/run-rag-phase316-offline.ps1` | PASS 53 Vietnamese/content-policy + 23 Admin KB checks |
| `test/run-file-knowledge.ps1` | PASS 35 file-KB checks |

Tất cả các lần trên dùng mock/offline: 0 Gemini request thật, 0 Supabase connection/write, 0 email thật.

## 10. Trạng thái đồng bộ Knowledge

- Markdown hiện parse thành công và được đóng gói vào WAR.
- Không chạy sync database hoặc embedding trong Phase 6, nên chưa thể tuyên bố nội dung mới đã có vector trên Supabase.
- Sau khi deployment được duyệt, `wifi-multifloor-coverage` version 2 dùng cùng ID để thay version 1. Bản cũ chỉ bị loại khỏi retrieval sau khi sync, indexing và activation của bản mới thành công.
- `internet-packages` là nguồn mới. Dù được index, giá trả lời khách vẫn do `ProductCatalog`/database quyết định; RAG không được phép ghi đè.

## 11. Yêu cầu trước deployment

1. Trên Admin/Supabase production, đối chiếu sáu package rows với bảng ở mục 3, đặc biệt 220K/230K hiện có phải là 300Mbps. Không dùng migration local và không cập nhật âm thầm.
2. Xác minh ngày hiệu lực/hết hiệu lực của bảng giá, quyền nội dung Ngoại Hạng Anh và eligibility ưu đãi trả trước đối với Mesh. Cho tới lúc đó chatbot tiếp tục dùng câu chữ có điều kiện.
3. Xác nhận Render đang deploy `main` và review đúng diff Phase 6 trước khi merge/push.
4. Nếu muốn file Git tự vào hàng đợi, cấu hình `KB_FILE_AUTO_SYNC=true` và `KB_FILE_SYNC_ACTOR_ID=<ID Admin hiện có>`. Kiểm tra migration 008/009 và trạng thái sync sau restart.
5. Indexing thật cần các gate sẵn có của worker và key embedding ở server. Không mở quota/call approval hoặc `RAG_ENABLED` chỉ để né lỗi sync. Theo dõi job đến `AVAILABLE_TO_CHATBOT` và xác minh version/hash trước khi coi nguồn cũ đã được thay thế.
6. Giữ rollback đơn giản: đặt `RAG_ENABLED=false` để tắt retrieval công khai; với tài liệu lỗi, dùng emergency disable/unpublish theo lifecycle hiện có và giữ revision/evidence. Nếu backend release có lỗi, rollback Render về commit trước Phase 6; không xóa migration, vector hoặc history.

## 12. Trạng thái phát hành

**Code và kiểm thử offline: GO.**  
**Production deployment/indexing: chưa thực hiện.** Còn cần đối chiếu package rows production, xác minh điều kiện ưu đãi/quyền nội dung và thực hiện sync/indexing có kiểm soát sau phê duyệt release.

FPT KNOWLEDGE UPDATED — BACKEND CONSULTATION IMPROVED — COMMERCIAL RULES ENFORCED.
