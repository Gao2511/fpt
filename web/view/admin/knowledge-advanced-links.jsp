<%@page contentType="text/html" pageEncoding="UTF-8"%>
<nav class="kb-advanced-links" aria-label="Công cụ nâng cao">
 <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=dashboard">Dashboard AI</a>
 <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=imports">Review phân mục</a>
 <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=faqs">FAQ Manager</a>
 <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=indexing">Gemini Runtime · Embedding · Indexing Jobs · Budgets</a>
 <a href="${pageContext.request.contextPath}/admin/settings">Primary / Backup Model</a>
 <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=playground">RAG Playground</a>
 <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=rag-config">RAG Settings</a>
 <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=audit">Monitoring · Audit Logs · Diagnostics</a>
</nav>
