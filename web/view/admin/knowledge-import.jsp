<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<section class="kb-panel">
    <h2>Phân mục tài liệu &amp; review từng phần</h2>
    <p>Bản gốc được lưu để đối chiếu và không xuất bản nguyên khối. Phân loại là gợi ý; Admin review trước khi tạo nháp và duyệt xuất bản riêng.</p>
    <p>Quy tắc AI và cấu hình chỉ nằm trong hàng chờ đề xuất. Chức năng áp dụng thay đổi system instructions chưa được triển khai.</p>
    <c:if test="${empty selectedDoc}"><p>Chọn tài liệu upload ở tab Tài liệu, bấm “Giữ bản gốc &amp; preview từng mục”, hoặc tải lên tệp mới với tùy chọn phân mục.</p></c:if>
    <c:if test="${not empty selectedDoc}">
        <h3><c:out value="${selectedDoc.title}"/> — bản nguồn #<c:out value="${selectedDoc.id}"/></h3>
        <c:forEach items="${docFiles}" var="file">
            <p><a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?action=downloadOriginal&amp;docId=${selectedDoc.id}&amp;fileId=${file.id}">Tải bản gốc: <c:out value="${file.originalFilename}"/></a> SHA256: <c:out value="${file.sha256Hash}"/></p>
        </c:forEach>
        <details><summary>Xem toàn văn trích xuất hiện hành (v<c:out value="${selectedDoc.version}"/>)</summary><pre class="kb-import-text"><c:out value="${selectedDoc.content}"/></pre></details>
    </c:if>
</section>
<c:forEach items="${importSections}" var="section">
    <section class="kb-panel">
        <h3>Mục <c:out value="${section.ordinal}"/> — <c:out value="${section.title}"/></h3>
        <p>Revision nguồn v<c:out value="${section.sourceVersion}"/> · Dòng <c:out value="${section.startLine}"/>–<c:out value="${section.endLine}"/> · <c:out value="${section.reviewState}"/></p>
        <p><small>Nguồn: <c:out value="${section.sourceUri}"/></small></p>
        <details><summary>Nội dung nguyên mục</summary><pre class="kb-import-text"><c:out value="${section.content}"/></pre></details>
        <details><summary>Preview chunks (chưa indexing)</summary>
            <c:forEach items="${section.chunks}" var="chunk"><p>#<c:out value="${chunk.ordinal}"/> · <c:out value="${chunk.charCount}"/> ký tự · ~<c:out value="${chunk.estimatedTokens}"/> tokens</p><pre class="kb-import-text"><c:out value="${chunk.content}"/></pre></c:forEach>
        </details>
        <c:if test="${section.reviewState != 'CREATED'}">
            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="reviewImportSection">
                <input type="hidden" name="sectionId" value="${section.id}"><input type="hidden" name="docId" value="${selectedDoc.id}">
                <div class="kb-grid-2">
                    <div class="kb-field"><label>Tiêu đề đơn chủ đề</label><input name="title" required maxlength="200" value="${fn:escapeXml(section.title)}"></div>
                    <div class="kb-field"><label>Loại nội dung</label><select name="classification">
                        <option value="AI_BEHAVIOR" ${section.classification == 'AI_BEHAVIOR' ? 'selected' : ''}>AI Behavior &amp; Consultation Rules — đề xuất duyệt</option>
                        <option value="KNOWLEDGE_BASE" ${section.classification == 'KNOWLEDGE_BASE' ? 'selected' : ''}>Knowledge Base</option>
                        <option value="FAQ" ${section.classification == 'FAQ' ? 'selected' : ''}>FAQ</option>
                        <option value="ADMIN_CONFIGURATION" ${section.classification == 'ADMIN_CONFIGURATION' ? 'selected' : ''}>Admin Configuration — chưa áp dụng</option>
                        <option value="MANUAL_REVIEW" ${section.classification == 'MANUAL_REVIEW' ? 'selected' : ''}>Needs Manual Review</option>
                    </select></div>
                </div>
                <label><input type="checkbox" name="ignore" value="true"> Bỏ qua mục (giữ nội dung gốc)</label>
                <button type="submit" class="kb-btn">Lưu review / hàng chờ duyệt</button>
            </form>
        </c:if>
        <c:if test="${section.reviewState == 'REVIEWED' && (section.classification == 'KNOWLEDGE_BASE' || section.classification == 'FAQ')}">
            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="createImportDraft">
                <input type="hidden" name="sectionId" value="${section.id}"><input type="hidden" name="docId" value="${selectedDoc.id}">
                <button type="submit" class="kb-btn kb-btn-success">Tạo entry DRAFT riêng (chưa phê duyệt/chưa xuất bản)</button>
            </form>
        </c:if>
        <c:if test="${not empty section.documentId}"><a class="kb-btn" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents&amp;docId=${section.documentId}">Review &amp; phê duyệt tài liệu #<c:out value="${section.documentId}"/></a></c:if>
        <c:if test="${not empty section.faqId}"><a class="kb-btn" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=faqs&amp;faqId=${section.faqId}">Review &amp; phê duyệt FAQ #<c:out value="${section.faqId}"/></a></c:if>
    </section>
</c:forEach>
