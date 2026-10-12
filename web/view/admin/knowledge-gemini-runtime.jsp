<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<details class="kb-panel kb-advanced-details" id="kbGeminiRuntimeAdvanced">
    <summary class="kb-advanced-summary">
        <span><strong>Điều khiển nâng cao: Gemini Runtime, Quota Plan &amp; Khôi phục Lease</strong> (Runtime: <code><c:out value="${ragRuntimeMode}"/></code> · Production RAG: <code>OFF</code>)</span>
        <span class="kb-badge kb-badge-APPROVED">Mở rộng / Thu gọn</span>
    </summary>
    <div class="kb-advanced-body">
        <p style="margin-top:0;font-size:13px;color:#94a3b8;">MOCK INDEX và GEMINI INDEX là hai không gian vector riêng. Publish chỉ xếp hàng; upload/publish không gọi Gemini.</p>
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
        </c:if>
        <c:choose>
            <c:when test="${(ragRuntimeMode == 'LOCAL_GEMINI' || ragRuntimeMode == 'STAGING' || ragRuntimeMode == 'PRODUCTION') && not empty ragLivePlan}">
                <c:if test="${kbTab == 'indexing'}">
                    <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-actions-inline">
                        <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="requestGeminiIndex"><input type="hidden" name="tab" value="indexing">
                        <label style="font-size:12.5px;">ID tài liệu đã duyệt/xuất bản, nằm trong plan:</label>
                        <input type="number" name="id" required min="1" value="${selectedDoc.id}" style="width:110px;padding:7px 10px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;">
                        <button class="kb-btn" type="submit">Request GEMINI INDEX / Reindex (dùng quota đã duyệt)</button>
                    </form>
                </c:if>
                <c:if test="${kbTab == 'playground'}">
                    <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-actions-inline">
                        <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="runGeminiPreview"><input type="hidden" name="tab" value="playground">
                        <label style="font-size:12.5px;">Câu hỏi retrieval đã duyệt trong plan (1 query embedding, không Chat):</label>
                        <input name="query" required maxlength="1000" style="flex:1;min-width:220px;padding:7px 10px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;">
                        <button class="kb-btn" type="submit">Gemini Retrieval Preview</button>
                    </form>
                </c:if>
            </c:when>
            <c:otherwise><p style="margin-bottom:0;font-size:12.5px;color:#94a3b8;">Gemini live đang khóa. Cần key test riêng, plan mới được phê duyệt, xác minh model/quota/tier còn hiệu lực và database test có migration 006–007.</p></c:otherwise>
        </c:choose>
    </div>
</details>
