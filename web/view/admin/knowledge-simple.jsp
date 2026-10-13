<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<jsp:include page="/view/admin/knowledge-files.jsp"/>
<c:if test="${!kbLifecycleAvailable}"><div class="kb-alert kb-alert-error">Cần migration 008 để dùng xuất bản và thay thế an toàn. Các chức năng đọc và đường dẫn cũ vẫn được giữ.</div></c:if>
<c:if test="${kbTab == 'upload'}">
 <section class="kb-panel"><h2>Tải lên &amp; Xuất bản</h2><p>Chọn tệp → kiểm tra nội dung → Xuất bản. Chỉ tài liệu đã lập chỉ mục Gemini thành công mới có thể được chatbot truy xuất khi gate RAG được bật.</p>
 <form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-form">
  <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="uploadDocument"><input type="hidden" name="tab" value="upload">
  <div class="kb-form-grid"><div class="kb-field"><label>Tài liệu (.txt, .md, .docx, .pdf)</label><input type="file" name="documentFile" accept=".txt,.md,.docx,.pdf" required><small>Tối đa <c:out value="${kbUploadLimit}"/> bytes; văn bản trích xuất tối đa <c:out value="${kbExtractedLimit}"/> ký tự. Không cắt ngắn nội dung.</small></div>
   <div class="kb-field"><label>Tiêu đề (để trống để lấy tên tệp)</label><input name="title" maxlength="200"></div></div>
  <details class="kb-advanced-details"><summary>Thay thế tài liệu / Ngày hiệu lực / Metadata</summary><div class="kb-form-grid kb-advanced-body">
   <div class="kb-field"><label>Thay thế tài liệu cũ (ID)</label><input type="number" name="replacementId" min="1" value="${fn:escapeXml(param.replace)}"><small>Bản cũ cần Policy Key và được giữ tới khi bản thay thế lập chỉ mục thành công.</small></div>
   <div class="kb-field"><label>Danh mục</label><select name="category"><option value="guide">Hướng dẫn</option><option value="policy">Chính sách</option><option value="faq">FAQ</option></select></div>
   <div class="kb-field"><label>Authority</label><select name="sourceAuthority"><option value="admin_reviewed">Admin đã kiểm tra</option><option value="official">Nguồn chính thức</option></select></div>
   <div class="kb-field"><label>Policy Key</label><input name="policyKey" maxlength="100" placeholder="wifi.support"></div>
   <div class="kb-field"><label>Có hiệu lực từ</label><input name="effectiveAt" type="date"></div><div class="kb-field"><label>Hết hiệu lực</label><input name="expiresAt" type="date"></div>
  </div></details><button class="kb-btn" type="submit">Tải lên</button>
 </form></section>
</c:if>
<c:if test="${not empty selectedDoc && (kbTab == 'documents' || kbTab == 'imports')}">
 <section class="kb-panel"><h3>Xuất bản tài liệu #<c:out value="${selectedDoc.id}"/></h3>
  <p>Xuất bản là phê duyệt nội dung tri thức. Không thay đổi system prompt, giá cước hay cấu hình. Gemini chỉ chạy trong ngân sách và quota quản trị viên đã cho phép.</p>
  <c:if test="${!selectedDoc.fileManaged && selectedDoc.workflowStatus != 'PUBLISHED' && selectedDoc.workflowStatus != 'ARCHIVED' && selectedDoc.workflowStatus != 'PROCESSING_FAILED'}">
   <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="id" value="${selectedDoc.id}"><input type="hidden" name="version" value="${selectedDoc.version}">
    <input type="hidden" name="action" value="${kbTab == 'imports' && empty selectedDoc.supersedesDocumentId ? 'publishUploadSections' : 'simplePublish'}">
    <button class="kb-btn" type="submit" ${!kbLifecycleAvailable ? 'disabled' : ''}>Xuất bản nội dung đã kiểm tra</button>
   </form>
  </c:if>
  <c:if test="${not empty selectedDoc.supersedesDocumentId}"><p>Thay thế #<c:out value="${selectedDoc.supersedesDocumentId}"/> sau khi index thành công. Nếu nội dung có nhiều chủ đề, cần tạo các bản thay thế đơn chủ đề riêng.</p></c:if>
  <c:if test="${!selectedDoc.fileManaged}"><a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=upload&amp;replace=${selectedDoc.id}">Upload phiên bản thay thế</a></c:if>
  <c:if test="${selectedDoc.fileManaged}"><p>FILE_MANAGED: sửa file và tăng version trong Git. Gỡ xuất bản/Lưu trữ là vô hiệu hóa khẩn cấp toàn bộ nguồn; sync không tự bật lại.</p></c:if>
  <div class="kb-actions-inline">
   <c:if test="${selectedDoc.workflowStatus == 'PUBLISHED'}"><form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base"><input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="unpublishDocument"><input type="hidden" name="id" value="${selectedDoc.id}"><button class="kb-btn kb-btn-secondary" type="submit">Gỡ xuất bản</button></form></c:if>
   <c:if test="${selectedDoc.workflowStatus != 'ARCHIVED'}"><form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base"><input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="archiveDocument"><input type="hidden" name="id" value="${selectedDoc.id}"><button class="kb-btn kb-btn-secondary" type="submit">Lưu trữ</button></form></c:if>
   <c:if test="${!selectedDoc.fileManaged && (selectedDoc.workflowStatus == 'ARCHIVED' || selectedDoc.workflowStatus == 'UNPUBLISHED')}"><form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base"><input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="restoreDocumentDraft"><input type="hidden" name="id" value="${selectedDoc.id}"><input type="hidden" name="version" value="${selectedDoc.version}"><button class="kb-btn kb-btn-secondary" type="submit">Khôi phục thành bản nháp</button></form></c:if>
  </div>
  <c:if test="${!selectedDoc.fileManaged}"><details class="kb-advanced-details"><summary>Xóa vĩnh viễn (nâng cao)</summary>
   <p>Ưu tiên Gỡ xuất bản / Lưu trữ. Xóa vĩnh viễn bị từ chối nếu còn liên kết hoặc request chưa xác định trạng thái.</p>
   <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" data-kb-destructive>
    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="permanentDeleteDocument"><input type="hidden" name="id" value="${selectedDoc.id}"><input type="hidden" name="version" value="${selectedDoc.version}">
    <label>Nhập DELETE ${selectedDoc.id}<input name="confirmation" required autocomplete="off"></label><button class="kb-btn kb-btn-danger" type="submit">Xóa vĩnh viễn</button>
   </form>
  </details></c:if>
 </section>
</c:if>
<c:if test="${kbTab == 'upload' || kbTab == 'documents' || kbTab == 'imports'}">
 <section class="kb-panel"><h3>Tiến độ xuất bản</h3><p><c:out value="${kbWorkerAuthorized ? 'Worker đã được cho phép; quota/budget còn phải hợp lệ.' : 'Worker chưa được cho phép — tài liệu giữ PENDING_INDEX, không dùng quota Gemini.'}"/></p>
 <div class="kb-table-wrap"><table class="kb-table"><thead><tr><th>Tài liệu</th><th>Trạng thái</th><th>Tiến độ</th><th>Lỗi</th></tr></thead><tbody>
  <c:forEach var="job" items="${kbPublishJobs}"><tr><td><a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents&amp;docId=${job.document_id}"><c:out value="${job.title}"/></a> · v<c:out value="${job.document_version}"/></td><td><c:out value="${job.state}"/></td><td><progress max="${job.total}" value="${job.done}"></progress> <c:out value="${job.done}"/>/<c:out value="${job.total}"/></td><td><c:out value="${job.error_code}"/></td></tr></c:forEach>
  <c:if test="${empty kbPublishJobs}"><tr><td colspan="4">Chưa có yêu cầu xuất bản theo luồng mới.</td></tr></c:if>
 </tbody></table></div><a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=${kbTab}">Cập nhật trạng thái</a>
 </section>
</c:if>
<c:if test="${kbTab == 'verify'}">
 <section class="kb-panel"><h2>KIỂM TRA AI ĐÃ SỬ DỤNG RAG CHƯA?</h2>
 <div class="kb-metrics-grid kb-metrics-grid-6"><div>Đã xuất bản: <strong><c:out value="${kbReadiness.published}"/></strong></div><div>Gemini index: <strong><c:out value="${kbReadiness.gemini}"/></strong></div><div>Chunk hợp lệ: <strong><c:out value="${kbReadiness.validChunks}"/></strong></div><div>Job lỗi: <strong><c:out value="${metrics.indexingFailures + kbReadiness.failedPublishJobs}"/></strong></div><div>Index thành công gần nhất: <c:out value="${kbReadiness.last_index}"/></div><div>Model: gemini-embedding-001 · 768<br>Gate: <c:out value="${systemStatus.ragEnabled ? 'BẬT' : 'TẮT'}"/></div></div>
 <h3>Thử truy xuất tài liệu</h3><p>Xem trước lexical từ vector Gemini đã lưu. 0 API calls. Preview này không chứng minh câu trả lời khách hàng đã dùng RAG.</p>
 <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-filter-row"><input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="readOnlyRetrieval"><input type="hidden" name="tab" value="verify"><input name="query" maxlength="1000" required placeholder="Nhập câu hỏi để kiểm tra RAG"><button class="kb-btn" type="submit">Kiểm tra · 0 API</button></form>
 <c:if test="${not empty kbRetrievalPreview}"><p>Kết quả: <c:out value="${kbRetrievalPreview.issue}"/>. Loại bỏ: <c:out value="${kbRetrievalPreview.rejected}"/></p><c:forEach var="chunk" items="${kbRetrievalPreview.chunks}"><details open><summary>#${chunk.documentId} — <c:out value="${chunk.title}"/> · score <c:out value="${chunk.score}"/> · nguồn được phép</summary><pre class="kb-snippet"><c:out value="${chunk.content}"/></pre><small><c:out value="${chunk.source}"/></small></details></c:forEach><c:if test="${empty kbRetrievalPreview.chunks}"><p>Không có nguồn đủ điều kiện trả lời.</p></c:if></c:if>
 </section>
 <section class="kb-panel"><h3>Bằng chứng từ chatbot khách hàng</h3><p>Tối đa 100 lượt / 24 giờ trên instance này. Không lưu câu hỏi, câu trả lời hoặc thông tin khách hàng. Restart sẽ mất danh sách; không có lượt ghi nhận không đồng nghĩa RAG chưa từng chạy.</p>
 <div class="kb-table-wrap"><table class="kb-table"><thead><tr><th>Thời gian / Request ID</th><th>Gate / Retrieval</th><th>Match / Gửi Gemini</th><th>Nguồn / Provider</th><th>Grounding / Dùng context</th></tr></thead><tbody>
 <c:forEach var="e" items="${ragRequestEvidence}"><tr><td><c:out value="${e.timestamp}"/><br><code><c:out value="${e.id}"/></code></td><td><c:out value="${e.enabled}"/> / <c:out value="${e.attempted}"/></td><td><c:out value="${e.matches}"/> / <c:out value="${e.passed}"/></td><td><c:out value="${e.provider}"/><c:forEach var="s" items="${e.sources}"><br>#${s.id} — <c:out value="${s.title}"/></c:forEach></td><td><c:out value="${e.grounded}"/> / <c:out value="${e.usedContext}"/></td></tr></c:forEach>
 <c:if test="${empty ragRequestEvidence}"><tr><td colspan="5">Chưa có bằng chứng runtime trên instance này.</td></tr></c:if>
 </tbody></table></div></section>
</c:if>
<c:if test="${kbTab == 'advanced'}"><section class="kb-panel"><h2>Cài đặt nâng cao</h2><p>Các công cụ cũ vẫn giữ nguyên đường dẫn. Thao tác live cần phê duyệt và ngân sách riêng.</p><jsp:include page="/view/admin/knowledge-advanced-links.jsp"/></section></c:if>
