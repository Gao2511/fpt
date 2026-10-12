<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<details class="kb-panel kb-advanced-details" id="kbGeminiRuntimeAdvanced">
    <summary class="kb-advanced-summary">
        <span><strong>Điều khiển nâng cao: Gemini Runtime, Lập chỉ mục theo ID &amp; Khôi phục Lease</strong> (Runtime: <code><c:out value="${ragRuntimeMode}"/></code> · Production RAG: <code><c:out value="${ragSettings.productionRagEnabled ? 'ON' : 'OFF'}"/></code>)</span>
        <span class="kb-badge kb-badge-APPROVED">Mở rộng / Thu gọn</span>
    </summary>
    <div class="kb-advanced-body">
        <p style="margin-top:0;font-size:13px;color:#94a3b8;">Hỗ trợ lập chỉ mục vector 768 chiều bằng <code>gemini-embedding-001</code> và kiểm thử truy hồi trực tiếp. Hệ thống tự động lấy khóa Gemini từ <code>RAG_EMBEDDING_API_KEY</code>, <code>GEMINI_API_KEY</code> hoặc cấu hình Admin Settings.</p>
        <c:if test="${not empty ragRuntimeNotice}"><p role="status" class="kb-alert kb-alert-success"><c:out value="${ragRuntimeNotice}"/></p></c:if>
        <c:if test="${not empty ragLivePlan}">
            <p style="font-size:12.5px;color:#cbd5e1;">Plan <code><c:out value="${ragLivePlan.planId}"/></code> · trần <c:out value="${ragLivePlan.totalLimit}"/> requests · RPM <c:out value="${ragLivePlan.rpm}"/> · RPD <c:out value="${ragLivePlan.rpd}"/> · TPM <c:out value="${ragLivePlan.tpm}"/>.</p>
        </c:if>
        <c:if test="${not empty selectedDoc}">
            <p style="font-size:12.5px;color:#cbd5e1;"><strong><c:out value="${selectedDoc.indexMode}"/></strong> · <c:out value="${selectedDoc.runtimeIndexStatus}"/> · revision v<c:out value="${selectedDoc.version}"/> / indexed v<c:out value="${selectedDoc.indexedVersion}"/> · Last Indexed At: <c:out value="${selectedDoc.lastIndexedAt}"/> · Embedding Error: <c:out value="${selectedDoc.embeddingError}"/></p>
        </c:if>
        <c:if test="${kbTab == 'indexing'}">
            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" style="margin-bottom:10px;">
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                <input type="hidden" name="action" value="recoverExpiredLeases">
                <input type="hidden" name="tab" value="indexing">
                <button class="kb-btn kb-btn-secondary" type="submit">Thu hồi Indexing Lease quá hạn sau khi restart container (0 Gemini API calls)</button>
            </form>
            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-actions-inline">
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="requestGeminiIndex"><input type="hidden" name="tab" value="indexing">
                <label style="font-size:12.5px;">ID tài liệu PUBLISHED cần chạy GEMINI INDEX (768d):</label>
                <input type="number" name="id" required min="1" value="${selectedDoc.id}" style="width:110px;padding:7px 10px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;">
                <button class="kb-btn" type="submit">Chạy GEMINI INDEX / Reindex theo ID</button>
            </form>
        </c:if>
        <c:if test="${kbTab == 'playground'}">
            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-actions-inline">
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="runGeminiPreview"><input type="hidden" name="tab" value="playground">
                <label style="font-size:12.5px;">Kiểm thử truy hồi bằng Gemini Embedding thật (1 query embedding 768d, không gọi Chat):</label>
                <input name="query" required maxlength="1000" placeholder="Nhập câu hỏi kiểm thử Gemini Retrieval..." style="flex:1;min-width:220px;padding:7px 10px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;">
                <button class="kb-btn" type="submit">Gemini Retrieval Preview (768d)</button>
            </form>
        </c:if>
    </div>
</details>
