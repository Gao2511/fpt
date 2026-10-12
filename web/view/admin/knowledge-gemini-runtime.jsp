<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<section class="kb-panel">
    <h3>Gemini Integration — Local/Test</h3>
    <p>Runtime: <strong><c:out value="${ragRuntimeMode}"/></strong>. Production RAG: OFF.</p>
    <p>MOCK INDEX và GEMINI INDEX là hai không gian vector riêng. Publish chỉ xếp hàng; upload/publish không gọi Gemini.</p>
    <c:if test="${not empty ragRuntimeNotice}"><p role="status"><c:out value="${ragRuntimeNotice}"/></p></c:if>
    <c:if test="${not empty ragLivePlan}">
        <p>Plan <c:out value="${ragLivePlan.planId}"/> · trần <c:out value="${ragLivePlan.totalLimit}"/> requests · RPM <c:out value="${ragLivePlan.rpm}"/> · RPD <c:out value="${ragLivePlan.rpd}"/> · TPM <c:out value="${ragLivePlan.tpm}"/>.</p>
    </c:if>
    <c:if test="${not empty selectedDoc}">
        <p><strong><c:out value="${selectedDoc.indexMode}"/></strong> · <c:out value="${selectedDoc.runtimeIndexStatus}"/> · revision v<c:out value="${selectedDoc.version}"/> / indexed v<c:out value="${selectedDoc.indexedVersion}"/></p>
        <p>Last Indexed At: <c:out value="${selectedDoc.lastIndexedAt}"/> · Embedding Error: <c:out value="${selectedDoc.embeddingError}"/></p>
    </c:if>
    <c:choose>
        <c:when test="${ragRuntimeMode == 'LOCAL_GEMINI' && not empty ragLivePlan}">
            <c:if test="${kbTab == 'indexing'}">
                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="requestGeminiIndex"><input type="hidden" name="tab" value="indexing">
                    <label>ID tài liệu đã duyệt/xuất bản, nằm trong plan</label>
                    <input type="number" name="id" required min="1" value="${selectedDoc.id}">
                    <button class="kb-btn" type="submit">Request GEMINI INDEX / Reindex (dùng quota đã duyệt)</button>
                </form>
            </c:if>
            <c:if test="${kbTab == 'playground'}">
                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="runGeminiPreview"><input type="hidden" name="tab" value="playground">
                    <label>Câu hỏi retrieval đã duyệt trong plan (1 query embedding, không Chat)</label>
                    <input name="query" required maxlength="1000"><button class="kb-btn" type="submit">Gemini Retrieval Preview</button>
                </form>
            </c:if>
        </c:when>
        <c:otherwise><p>Gemini live đang khóa. Cần key test riêng, plan mới được phê duyệt, xác minh model/quota/tier còn hiệu lực và database test có migration 006.</p></c:otherwise>
    </c:choose>
</section>
