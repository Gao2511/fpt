<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Knowledge Base &amp; RAG Management - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-kb.css">
</head>
<body>
<jsp:include page="/view/admin/admin-header.jsp">
    <jsp:param name="activeTab" value="knowledge-base"/>
</jsp:include>

<main class="admin-main kb-page">
    <!-- HEADER BANNER -->
    <div class="kb-header-banner">
        <div>
            <span style="font-size:11px;font-weight:700;color:#f37021;letter-spacing:0.08em;">QUẢN TRỊ TRI THỨC AI &amp; RAG</span>
            <h1>Kho tri thức AI</h1>
            <p>Quản lý tài liệu, duyệt xuất bản có phiên bản và lập chỉ mục vector 768 chiều. Trạng thái được tổng hợp từ cấu hình máy chủ, PostgreSQL và nhật ký thực thi (0 cuộc gọi Gemini API khi tải trang).</p>
        </div>
        <div class="kb-status-pills">
            <span class="kb-offline-badge" data-status-runtime="${fn:escapeXml(systemStatus.runtimeMode)}">
                <span>● Runtime: <c:out value="${not empty systemStatus ? systemStatus.runtimeMode : 'OFFLINE_MOCK'}"/></span>
                <span>| Public RAG: <c:out value="${not empty systemStatus && systemStatus.ragEnabled ? 'ENABLED' : 'LOCKED OFF'}"/></span>
            </span>
        </div>
    </div>

    <nav class="kb-subnav kb-primary-nav" aria-label="Knowledge Base Sections">
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=upload" class="${kbTab == 'upload' || kbTab == 'imports' ? 'active' : ''}">1. Tải lên &amp; Xuất bản</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents" class="${kbTab == 'documents' ? 'active' : ''}">2. Quản lý tài liệu</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=verify" class="${kbTab == 'verify' ? 'active' : ''}">3. Kiểm tra AI RAG</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=advanced" class="${kbTab == 'advanced' ? 'active' : ''}">4. Cài đặt nâng cao</a>
    </nav>
    <details class="kb-advanced-details"><summary>Công cụ nâng cao &amp; đường dẫn cũ</summary><jsp:include page="/view/admin/knowledge-advanced-links.jsp"/></details>
    <jsp:include page="/view/admin/knowledge-simple.jsp"/>
    <!-- FLASH & ERROR MESSAGES -->
    <c:if test="${not empty kbError}">
        <div class="kb-alert kb-alert-error" role="alert"><c:out value="${kbError}"/></div>
    </c:if>
    <c:if test="${not empty sessionScope.kbMessage}">
        <div class="kb-alert kb-alert-success" role="status"><c:out value="${sessionScope.kbMessage}"/></div>
        <c:remove var="kbMessage" scope="session"/>
    </c:if>

    <!-- ===================================================================
         TAB 1: TỔNG QUAN AI (6 CORE SUMMARY METRICS)
         =================================================================== -->
    <c:if test="${kbTab == 'dashboard'}">
        <div class="kb-metrics-grid kb-metrics-grid-6" aria-label="Tổng quan 6 chỉ số vận hành AI và Knowledge Base">
            <div class="kb-metric-card">
                <div class="metric-label">1. Đã duyệt (Approved)</div>
                <div class="metric-value" style="color:#7dd3fc;"><c:out value="${metrics.approvedDocuments}"/></div>
                <div class="metric-sub">Chờ duyệt: <c:out value="${metrics.pendingReviewDocuments}"/> · Nháp: <c:out value="${metrics.draftDocuments}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">2. Đã xuất bản (Published)</div>
                <div class="metric-value" style="color:#4ade80;"><c:out value="${metrics.publishedDocuments}"/></div>
                <div class="metric-sub">Tổng tài liệu: <c:out value="${metrics.totalDocuments}"/> · FAQ: <c:out value="${metrics.publishedFaqs}"/>/<c:out value="${metrics.totalFaqs}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">3. Đã lập chỉ mục (Indexed)</div>
                <div class="metric-value" style="color:#38bdf8;"><c:out value="${metrics.indexedDocuments}"/></div>
                <div class="metric-sub">Tổng số Chunk: <c:out value="${metrics.totalChunks}"/> · Lưu trữ: <c:out value="${metrics.archivedDocuments + metrics.unpublishedDocuments}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">4. Chờ / Lỗi xử lý (Pending / Failed)</div>
                <div class="metric-value" style="color:${(metrics.needingReindexDocuments + metrics.indexingFailures) > 0 ? '#fbbf24' : '#cbd5e1'};"><c:out value="${metrics.needingReindexDocuments + metrics.indexingFailures}"/></div>
                <div class="metric-sub">Cần Reindex: <c:out value="${metrics.needingReindexDocuments}"/> · Lỗi Index: <c:out value="${metrics.indexingFailures}"/> · Bị từ chối: <c:out value="${metrics.rejectedDocuments + metrics.failedDocuments}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">5. Trạng thái RAG (RAG Status)</div>
                <div class="metric-value" style="font-size:20px;color:#fcd34d;"><c:out value="${not empty systemStatus ? systemStatus.rag.state : 'LOCKED OFF'}"/></div>
                <div class="metric-sub">Public RAG: <c:out value="${not empty systemStatus && systemStatus.ragEnabled ? 'ON' : 'LOCKED OFF'}"/> · Gate: <c:out value="${not empty systemStatus && systemStatus.productionReleaseAuthorized ? 'AUTHORIZED' : 'LOCKED OFF'}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">6. Gemini Runtime Status</div>
                <div class="metric-value" style="font-size:20px;color:#6ee7b7;"><c:out value="${not empty systemStatus ? systemStatus.runtimeMode : 'OFFLINE_MOCK'}"/></div>
                <div class="metric-sub">Chat: <c:out value="${not empty systemStatus ? systemStatus.geminiChat.state : 'Offline'}"/> · Embed: <c:out value="${not empty systemStatus ? systemStatus.geminiEmbedding.state : 'Offline'}"/></div>
            </div>
        </div>
    </c:if>

    <!-- 5 INDEPENDENT OPERATIONAL DIMENSIONS & 7-COMPONENT STATUS PANEL (COLLAPSIBLE PROGRESSIVE DISCLOSURE) -->
    <c:if test="${not empty systemStatus}">
        <details class="kb-panel kb-readiness-panel kb-advanced-details" ${kbTab == 'dashboard' ? 'open' : ''} aria-label="System Online and Offline Readiness Status">
            <summary class="kb-readiness-header kb-advanced-summary">
                <div>
                    <h3>Chẩn đoán chi tiết Hệ thống AI, Gemini &amp; RAG (Online / Offline Readiness — 7 thành phần)</h3>
                    <p>Phân tách rõ 5 chiều vận hành độc lập. Không bao giờ báo <code>Connected</code> chỉ vì có biến môi trường API Key khi chưa có bằng chứng xác minh.</p>
                </div>
                <div class="kb-dimension-pills">
                    <span class="kb-dim-pill ${systemStatus.aiChatbotEnabled ? 'kb-dim-on' : 'kb-dim-off'}">
                        1. AI Chatbot: <strong><c:out value="${systemStatus.aiChatbotEnabled ? 'ENABLED' : 'DISABLED'}"/></strong>
                    </span>
                    <span class="kb-dim-pill ${systemStatus.ragEnabled ? 'kb-dim-on' : 'kb-dim-warn'}">
                        2. Customer RAG: <strong><c:out value="${systemStatus.ragEnabled ? 'ENABLED' : 'OFF (Release Gate)'}"/></strong>
                    </span>
                    <span class="kb-dim-pill ${systemStatus.geminiIndexingEnabled ? 'kb-dim-on' : 'kb-dim-warn'}">
                        3. Gemini Indexing: <strong><c:out value="${systemStatus.geminiIndexingEnabled ? systemStatus.runtimeMode : 'MOCK OFFLINE'}"/></strong>
                    </span>
                    <span class="kb-dim-pill ${systemStatus.kbDatabaseConnected ? 'kb-dim-on' : 'kb-dim-err'}">
                        4. KB Database: <strong><c:out value="${systemStatus.kbDatabaseConnected ? 'CONNECTED' : 'MISSING / ERROR'}"/></strong>
                    </span>
                    <span class="kb-dim-pill ${systemStatus.productionReleaseAuthorized ? 'kb-dim-on' : 'kb-dim-warn'}">
                        5. Production Gate: <strong><c:out value="${systemStatus.productionReleaseAuthorized ? 'AUTHORIZED' : 'LOCKED OFF'}"/></strong>
                    </span>
                    <span class="kb-dim-pill" id="kbBuildMeta" title="Build Timestamp: ${fn:escapeXml(systemStatus.buildTimestamp)}">
                        Build: <strong>v<c:out value="${systemStatus.buildVersion}"/></strong> (<code><c:out value="${systemStatus.buildCommit}"/></code> &middot; <c:out value="${systemStatus.environment}"/>)
                    </span>
                </div>
            </summary>

            <div class="kb-status-grid kb-advanced-body" id="adminStatusPanel">
                <!-- 1. Gemini Chat -->
                <div class="kb-status-card" data-component="gemini-chat" data-status-code="${systemStatus.geminiChat.code}">
                    <div class="kb-status-card-top">
                        <span class="kb-status-title"><c:out value="${systemStatus.geminiChat.name}"/></span>
                        <span class="kb-badge kb-badge-${systemStatus.geminiChat.badgeVariant}" title="${systemStatus.geminiChat.code}"><c:out value="${systemStatus.geminiChat.state}"/> (<c:out value="${systemStatus.geminiChat.code}"/>)</span>
                    </div>
                    <div class="kb-status-summary"><c:out value="${systemStatus.geminiChat.summary}"/></div>
                    <ul class="kb-status-kv">
                        <li><span>Model:</span> <code><c:out value="${ragSettings.primaryChatModel}"/></code></li>
                        <li><span>Thinking Level:</span> <code><c:out value="${ragSettings.primaryThinkingLevel}"/></code></li>
                        <li><span>Key Configured:</span> <strong><c:out value="${systemStatus.geminiChat.code == 'MISSING_KEY' ? 'No' : 'Yes'}"/></strong></li>
                    </ul>
                    <div class="kb-status-detail"><c:out value="${systemStatus.geminiChat.detail}"/></div>
                </div>

                <!-- 2. Gemini Embedding -->
                <div class="kb-status-card" data-component="gemini-embedding" data-status-code="${systemStatus.geminiEmbedding.code}">
                    <div class="kb-status-card-top">
                        <span class="kb-status-title"><c:out value="${systemStatus.geminiEmbedding.name}"/></span>
                        <span class="kb-badge kb-badge-${systemStatus.geminiEmbedding.badgeVariant}" title="${systemStatus.geminiEmbedding.code}"><c:out value="${systemStatus.geminiEmbedding.state}"/> (<c:out value="${systemStatus.geminiEmbedding.code}"/>)</span>
                    </div>
                    <div class="kb-status-summary"><c:out value="${systemStatus.geminiEmbedding.summary}"/></div>
                    <ul class="kb-status-kv">
                        <li><span>Model:</span> <code><c:out value="${ragSettings.embeddingModel}"/></code></li>
                        <li><span>Dimensions:</span> <code><c:out value="${ragSettings.vectorDimensions}"/></code></li>
                        <li><span>Key Configured:</span> <strong><c:out value="${systemStatus.geminiEmbedding.code == 'MISSING_KEY' ? 'No' : 'Yes'}"/></strong></li>
                    </ul>
                    <div class="kb-status-detail"><c:out value="${systemStatus.geminiEmbedding.detail}"/></div>
                </div>

                <!-- 3. Database (Supabase / PostgreSQL + pgvector) -->
                <div class="kb-status-card" data-component="database" data-status-code="${systemStatus.database.code}">
                    <div class="kb-status-card-top">
                        <span class="kb-status-title"><c:out value="${systemStatus.database.name}"/></span>
                        <span class="kb-badge kb-badge-${systemStatus.database.badgeVariant}" title="${systemStatus.database.code}"><c:out value="${systemStatus.database.code}"/></span>
                    </div>
                    <div class="kb-status-summary"><c:out value="${systemStatus.database.summary}"/></div>
                    <ul class="kb-status-kv">
                        <li><span>Environment:</span> <code><c:out value="${systemStatus.environment}"/></code></li>
                        <li><span>File Storage:</span> <code><c:out value="${systemStatus.storageBackend}"/></code></li>
                        <li><span>Migrations:</span> <strong><c:out value="${systemStatus.kbDatabaseConnected ? 'Verified (001-007)' : 'Pending / Error'}"/></strong></li>
                    </ul>
                    <div class="kb-status-detail"><c:out value="${systemStatus.database.detail}"/></div>
                </div>

                <!-- 4. Knowledge Base -->
                <div class="kb-status-card" data-component="knowledge-base" data-status-code="${systemStatus.knowledgeBase.code}">
                    <div class="kb-status-card-top">
                        <span class="kb-status-title"><c:out value="${systemStatus.knowledgeBase.name}"/></span>
                        <span class="kb-badge kb-badge-${systemStatus.knowledgeBase.badgeVariant}" title="${systemStatus.knowledgeBase.code}"><c:out value="${systemStatus.knowledgeBase.state}"/> (<c:out value="${systemStatus.knowledgeBase.code}"/>)</span>
                    </div>
                    <div class="kb-status-summary"><c:out value="${systemStatus.knowledgeBase.summary}"/></div>
                    <ul class="kb-status-kv">
                        <li><span>DB Connection:</span> <strong><c:out value="${systemStatus.knowledgeBase.state == 'Error' ? 'Disconnected' : 'Connected'}"/></strong></li>
                        <li><span>Status Code:</span> <code><c:out value="${systemStatus.knowledgeBase.code}"/></code></li>
                        <li><span>Published / Chunks:</span> <strong><c:out value="${metrics.publishedDocuments}"/> docs / <c:out value="${metrics.totalChunks}"/> chunks</strong></li>
                    </ul>
                    <div class="kb-status-detail"><c:out value="${systemStatus.knowledgeBase.detail}"/></div>
                </div>

                <!-- 5. RAG -->
                <div class="kb-status-card" data-component="rag" data-status-code="${systemStatus.rag.code}">
                    <div class="kb-status-card-top">
                        <span class="kb-status-title"><c:out value="${systemStatus.rag.name}"/></span>
                        <span class="kb-badge kb-badge-${systemStatus.rag.badgeVariant}" title="${systemStatus.rag.code}"><c:out value="${systemStatus.rag.state}"/> (<c:out value="${systemStatus.rag.code}"/>)</span>
                    </div>
                    <div class="kb-status-summary"><c:out value="${systemStatus.rag.summary}"/></div>
                    <ul class="kb-status-kv">
                        <li><span>Mode:</span> <code><c:out value="${systemStatus.runtimeMode}"/></code></li>
                        <li><span>Public Website RAG:</span> <strong><c:out value="${systemStatus.ragEnabled ? 'ON' : 'OFF'}"/></strong></li>
                        <li><span>Admin Playground:</span> <strong><c:out value="${systemStatus.geminiIndexingEnabled ? 'LIVE + MOCK' : 'MOCK'}"/></strong></li>
                    </ul>
                    <div class="kb-status-detail"><c:out value="${systemStatus.rag.detail}"/></div>
                </div>

                <!-- 6. Indexing -->
                <div class="kb-status-card" data-component="indexing" data-status-code="${systemStatus.indexing.code}">
                    <div class="kb-status-card-top">
                        <span class="kb-status-title"><c:out value="${systemStatus.indexing.name}"/></span>
                        <span class="kb-badge kb-badge-${systemStatus.indexing.badgeVariant}" title="${systemStatus.indexing.code}"><c:out value="${systemStatus.indexing.state}"/> (<c:out value="${systemStatus.indexing.code}"/>)</span>
                    </div>
                    <div class="kb-status-summary"><c:out value="${systemStatus.indexing.summary}"/></div>
                    <ul class="kb-status-kv">
                        <li><span>Indexed Ready:</span> <strong><c:out value="${metrics.indexedDocuments}"/> docs</strong></li>
                        <li><span>Pending Reindex:</span> <strong><c:out value="${metrics.needingReindexDocuments}"/> docs</strong></li>
                        <li><span>Failed Docs:</span> <strong><c:out value="${metrics.indexingFailures}"/> docs</strong></li>
                    </ul>
                    <div class="kb-status-detail"><c:out value="${systemStatus.indexing.detail}"/></div>
                </div>

                <!-- 7. Backup Model -->
                <div class="kb-status-card" data-component="backup-model" data-status-code="${systemStatus.backupModel.code}">
                    <div class="kb-status-card-top">
                        <span class="kb-status-title"><c:out value="${systemStatus.backupModel.name}"/></span>
                        <span class="kb-badge kb-badge-${systemStatus.backupModel.badgeVariant}" title="${systemStatus.backupModel.code}"><c:out value="${systemStatus.backupModel.state}"/> (<c:out value="${systemStatus.backupModel.code}"/>)</span>
                    </div>
                    <div class="kb-status-summary"><c:out value="${systemStatus.backupModel.summary}"/></div>
                    <ul class="kb-status-kv">
                        <li><span>Model:</span> <code><c:out value="${ragSettings.backupChatModel}"/></code></li>
                        <li><span>Enabled:</span> <code><c:out value="${ragSettings.backupEnabled}"/></code></li>
                        <li><span>Thinking Level:</span> <code>low</code></li>
                    </ul>
                    <div class="kb-status-detail"><c:out value="${systemStatus.backupModel.detail}"/></div>
                </div>
            </div>
        </details>
    </c:if>

    <c:if test="${kbTab == 'documents' && selectedDoc.sourceType == 'UPLOAD' && selectedDoc.workflowStatus == 'DRAFT'}">
        <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-panel" style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;">
            <div>
                <strong>Tài liệu tải lên (#<c:out value="${selectedDoc.id}"/>) đang ở trạng thái Nháp (DRAFT)</strong>
                <div style="font-size:12.5px;color:#94a3b8;">Bạn có thể tách tự động thành các mục đơn chủ đề để review trước khi xuất bản.</div>
            </div>
            <div>
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                <input type="hidden" name="action" value="prepareImport"><input type="hidden" name="id" value="${selectedDoc.id}">
                <input type="hidden" name="version" value="${selectedDoc.version}"><button class="kb-btn" type="submit">Giữ bản gốc &amp; preview từng mục</button>
            </div>
        </form>
    </c:if>
    <c:if test="${kbTab == 'imports'}"><jsp:include page="/view/admin/knowledge-import.jsp"/></c:if>

    <c:if test="${kbTab == 'dashboard'}">
        <div class="kb-grid-2">
            <div class="kb-panel">
                <h3>Hoạt động quản trị gần đây</h3>
                <div class="kb-table-wrap">
                    <table class="kb-table">
                        <thead>
                            <tr>
                                <th>Thời gian</th>
                                <th>Admin</th>
                                <th>Thao tác</th>
                                <th>Đối tượng</th>
                                <th>Tóm tắt</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty metrics.recentActivities}">
                                    <tr><td colspan="5">Chưa có hoạt động nào được ghi nhận.</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${metrics.recentActivities}" var="act">
                                        <tr>
                                            <td><c:out value="${act.createdAt}"/></td>
                                            <td><c:out value="${act.actorUsername}"/> (#<c:out value="${act.actorId}"/>)</td>
                                            <td><span class="kb-badge kb-badge-APPROVED"><c:out value="${act.action}"/></span></td>
                                            <td><c:out value="${act.resourceType}"/>:<c:out value="${act.resourceId}"/></td>
                                            <td><c:out value="${act.summary}"/></td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="kb-panel">
                <h3>Trạng thái an toàn hệ thống AI &amp; RAG</h3>
                <ul style="line-height:1.9;padding-left:18px;font-size:13.5px;">
                    <li><strong>Production Customer RAG:</strong> <span class="kb-badge kb-badge-${systemStatus.rag.badgeVariant}"><c:out value="${systemStatus.rag.state}"/></span></li>
                    <li><strong>Local Offline Indexing:</strong> <span class="kb-badge kb-badge-PUBLISHED"><c:out value="${ragSettings.localIndexingEnabled ? 'ENABLED' : 'DISABLED'}"/></span></li>
                    <li><strong>Embedding Provider:</strong> <code><c:out value="${systemStatus.geminiIndexingEnabled ? 'RuntimeEmbeddingService' : 'MockEmbeddingProvider'}"/> (<c:out value="${ragSettings.embeddingModel}"/> / <c:out value="${ragSettings.vectorDimensions}"/>d)</code></li>
                    <li><strong>Primary Chat Model:</strong> <code><c:out value="${ragSettings.primaryChatModel}"/></code> (Thinking: <code><c:out value="${ragSettings.primaryThinkingLevel}"/></code>)</li>
                    <li><strong>Backup Chat Model:</strong> <code><c:out value="${ragSettings.backupChatModel}"/></code> (Fallback: <code><c:out value="${ragSettings.backupEnabled ? 'ON' : 'OFF'}"/></code>)</li>
                    <li><strong>Retrieval Config:</strong> <code>topK=<c:out value="${ragSettings.topK}"/></code>, <code>threshold=<c:out value="${ragSettings.similarityThreshold}"/></code>, <code>maxTokens=<c:out value="${ragSettings.maxContextTokens}"/></code></li>
                </ul>
                <div class="kb-actions-inline" style="margin-top:16px;">
                    <a class="kb-btn" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents">Quản lý Tài liệu</a>
                    <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=upload">Tải tệp lên</a>
                    <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=playground">Mở RAG Playground</a>
                </div>
            </div>
        </div>
    </c:if>

    <!-- ===================================================================
         TAB 2: DOCUMENT MANAGEMENT, REVIEW, VERSIONING & PUBLICATION
         =================================================================== -->
    <c:if test="${kbTab == 'documents'}">
        <div class="kb-panel">
            <h3>Tìm kiếm &amp; Lọc tài liệu tri thức</h3>
            <form method="get" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-form-grid">
                <input type="hidden" name="tab" value="documents">
                <div class="kb-field">
                    <label for="kb-q">Từ khóa (Tiêu đề, Tags, Nguồn, Phòng ban)</label>
                    <input id="kb-q" type="text" name="q" value="${fn:escapeXml(filterQ)}" placeholder="Nhập từ khóa tìm kiếm...">
                </div>
                <div class="kb-field">
                    <label for="kb-cat">Danh mục</label>
                    <select id="kb-cat" name="category">
                        <option value="all" ${filterCategory == 'all' ? 'selected' : ''}>Tất cả danh mục</option>
                        <option value="policy" ${filterCategory == 'policy' ? 'selected' : ''}>Chính sách (policy)</option>
                        <option value="guide" ${filterCategory == 'guide' ? 'selected' : ''}>Hướng dẫn (guide)</option>
                        <option value="faq" ${filterCategory == 'faq' ? 'selected' : ''}>Hỏi đáp (faq)</option>
                    </select>
                </div>
                <div class="kb-field">
                    <label for="kb-ws">Trạng thái duyệt</label>
                    <select id="kb-ws" name="workflowStatus">
                        <option value="all" ${filterWorkflowStatus == 'all' ? 'selected' : ''}>Tất cả trạng thái</option>
                        <option value="DRAFT" ${filterWorkflowStatus == 'DRAFT' ? 'selected' : ''}>DRAFT</option>
                        <option value="PENDING_REVIEW" ${filterWorkflowStatus == 'PENDING_REVIEW' ? 'selected' : ''}>PENDING_REVIEW</option>
                        <option value="APPROVED" ${filterWorkflowStatus == 'APPROVED' ? 'selected' : ''}>APPROVED</option>
                        <option value="PUBLISHED" ${filterWorkflowStatus == 'PUBLISHED' ? 'selected' : ''}>PUBLISHED</option>
                        <option value="REJECTED" ${filterWorkflowStatus == 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                        <option value="UNPUBLISHED" ${filterWorkflowStatus == 'UNPUBLISHED' ? 'selected' : ''}>UNPUBLISHED</option>
                        <option value="ARCHIVED" ${filterWorkflowStatus == 'ARCHIVED' ? 'selected' : ''}>ARCHIVED</option>
                        <option value="PROCESSING_FAILED" ${filterWorkflowStatus == 'PROCESSING_FAILED' ? 'selected' : ''}>PROCESSING_FAILED</option>
                    </select>
                </div>
                <div class="kb-field">
                    <label for="kb-idx">Trạng thái chỉ mục</label>
                    <select id="kb-idx" name="indexingFilter">
                        <option value="all" ${filterIndexing == 'all' ? 'selected' : ''}>Tất cả</option>
                        <option value="ready" ${filterIndexing == 'ready' ? 'selected' : ''}>Đã lập chỉ mục (ready)</option>
                        <option value="needs_reindex" ${filterIndexing == 'needs_reindex' ? 'selected' : ''}>Cần Reindex</option>
                        <option value="pending" ${filterIndexing == 'pending' ? 'selected' : ''}>Chờ xử lý (pending)</option>
                        <option value="failed" ${filterIndexing == 'failed' ? 'selected' : ''}>Lỗi (failed)</option>
                    </select>
                </div>
                <div class="kb-field" style="display:flex;align-items:flex-end;gap:8px;">
                    <button type="submit" class="kb-btn">Lọc danh sách</button>
                    <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents">Đặt lại</a>
                </div>
            </form>

            <div class="kb-table-wrap">
                <table class="kb-table">
                    <thead>
                        <tr>
                            <th>ID / Ver</th>
                            <th>Tiêu đề &amp; Nguồn</th>
                            <th>Danh mục / Độ tin cậy</th>
                            <th>Workflow Status</th>
                            <th>RAG Index</th>
                            <th>Cập nhật</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty documents}">
                                <tr><td colspan="7">Không có tài liệu nào khớp bộ lọc.</td></tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach items="${documents}" var="doc">
                                    <tr>
                                        <td><strong>#<c:out value="${doc.id}"/></strong> · v<c:out value="${doc.version}"/></td>
                                        <td>
                                            <strong><c:out value="${doc.title}"/></strong><br>
                                            <small style="color:#94a3b8;">Nguồn: <c:out value="${doc.source}"/> (<c:out value="${doc.sourceType}"/>)</small>
                                            <c:if test="${not empty doc.tags}">
                                                <br><small style="color:#38bdf8;">Tags: <c:out value="${doc.tags}"/></small>
                                            </c:if>
                                        </td>
                                        <td>
                                            <code><c:out value="${doc.category}"/></code> / <code><c:out value="${doc.sourceAuthority}"/></code>
                                            <c:if test="${not empty doc.policyKey}">
                                                <br><small>Key: <code><c:out value="${doc.policyKey}"/></code></small>
                                            </c:if>
                                        </td>
                                        <td>
                                            <span class="kb-badge kb-badge-${doc.workflowStatus}"><c:out value="${doc.workflowStatus}"/></span>
                                            <c:if test="${not empty doc.rejectionReason}">
                                                <br><small style="color:#fca5a5;">Lý do: <c:out value="${doc.rejectionReason}"/></small>
                                            </c:if>
                                        </td>
                                        <td>
                                            <span class="kb-badge kb-badge-${doc.indexingStatus}"><c:out value="${doc.indexMode}"/> · <c:out value="${doc.runtimeIndexStatus}"/></span>
                                            <c:if test="${doc.needsReindex}">
                                                <br><span class="kb-badge kb-badge-PENDING_REVIEW">NEEDS_REINDEX</span>
                                            </c:if>
                                        </td>
                                        <td><small><c:out value="${doc.updatedAt}"/></small></td>
                                        <td>
                                            <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents&amp;docId=${doc.id}">Chi tiết / Duyệt / Lịch sử</a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="kb-grid-2">
            <!-- CREATE OR EDIT DOCUMENT FORM -->
            <div class="kb-panel">
                <h3><c:out value="${not empty selectedDoc ? 'Chỉnh sửa & Quản lý Tài liệu #' : 'Tạo mới Tài liệu Tri thức (DRAFT)'}"/><c:if test="${not empty selectedDoc}"><c:out value="${selectedDoc.id}"/> (v<c:out value="${selectedDoc.version}"/>)</c:if></h3>
                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                    <input type="hidden" name="tab" value="documents">
                    <input type="hidden" name="action" value="${not empty selectedDoc ? 'updateDocument' : 'createDocument'}">
                    <c:if test="${not empty selectedDoc}">
                        <input type="hidden" name="id" value="${selectedDoc.id}">
                    </c:if>

                    <div class="kb-form-grid">
                        <div class="kb-field">
                            <label>Tiêu đề tài liệu *</label>
                            <input type="text" name="title" maxlength="200" required value="${fn:escapeXml(not empty selectedDoc ? selectedDoc.title : '')}">
                        </div>
                        <div class="kb-field">
                            <label>Danh mục *</label>
                            <select name="category">
                                <option value="guide" ${not empty selectedDoc && selectedDoc.category == 'guide' ? 'selected' : ''}>Hướng dẫn (guide)</option>
                                <option value="policy" ${not empty selectedDoc && selectedDoc.category == 'policy' ? 'selected' : ''}>Chính sách (policy)</option>
                                <option value="faq" ${not empty selectedDoc && selectedDoc.category == 'faq' ? 'selected' : ''}>Hỏi đáp (faq)</option>
                            </select>
                        </div>
                        <div class="kb-field">
                            <label>Nguồn (Source URI / Mã văn bản) *</label>
                            <input type="text" name="source" maxlength="500" required value="${fn:escapeXml(not empty selectedDoc ? selectedDoc.source : 'policy:fpt-gialai/curated')}">
                        </div>
                        <div class="kb-field">
                            <label>Độ tin cậy (Source Authority) *</label>
                            <select name="sourceAuthority">
                                <option value="admin_reviewed" ${not empty selectedDoc && selectedDoc.sourceAuthority == 'admin_reviewed' ? 'selected' : ''}>admin_reviewed</option>
                                <option value="official" ${not empty selectedDoc && selectedDoc.sourceAuthority == 'official' ? 'selected' : ''}>official</option>
                            </select>
                        </div>
                    </div>

                    <div class="kb-field" style="margin-bottom:14px;">
                        <label>Nội dung văn bản chuẩn hóa (không chứa thông tin giá cước/tốc độ gói do DB quản lý) *</label>
                        <c:set var="largeUploadedContent" value="${selectedDoc.sourceType == 'UPLOAD' && fn:length(selectedDoc.content) > kbTextLimit}"/>
                        <textarea name="content" rows="8" maxlength="${selectedDoc.sourceType == 'UPLOAD' ? kbExtractedLimit : kbTextLimit}" ${largeUploadedContent ? 'disabled' : 'required'}><c:out value="${not empty selectedDoc ? selectedDoc.content : ''}"/></textarea>
                        <c:if test="${not empty selectedDoc}"><input type="hidden" name="version" value="${selectedDoc.version}"></c:if>
                        <c:if test="${largeUploadedContent}"><input type="hidden" name="preserveUploadedContent" value="true"><small>Văn bản lớn được giữ nguyên khi sửa metadata. Để sửa nội dung, upload phiên bản thay thế; không gửi toàn bộ văn bản qua form.</small></c:if>
                    </div>

                    <details class="kb-advanced-details" style="margin-bottom:14px;">
                        <summary class="kb-advanced-summary">
                            <span>Cấu hình nâng cao (Policy Key, Chunking, Supersedes, Tags &amp; Ghi chú phiên bản)</span>
                            <span class="kb-badge kb-badge-DRAFT">Tùy chọn</span>
                        </summary>
                        <div class="kb-form-grid kb-advanced-body">
                            <div class="kb-field">
                                <label>Policy Key (chữ thường/số/dấu gạch)</label>
                                <input type="text" name="policyKey" maxlength="100" value="${fn:escapeXml(not empty selectedDoc ? selectedDoc.policyKey : '')}" placeholder="vd: warranty.modem">
                            </div>
                            <div class="kb-field">
                                <label>Thay thế tài liệu ID (Supersedes ID)</label>
                                <input type="number" name="supersedesDocumentId" min="1" value="${not empty selectedDoc && not empty selectedDoc.supersedesDocumentId ? selectedDoc.supersedesDocumentId : ''}">
                            </div>
                            <div class="kb-field">
                                <label>Tags (cách nhau bằng dấu phẩy)</label>
                                <input type="text" name="tags" maxlength="500" value="${fn:escapeXml(not empty selectedDoc ? selectedDoc.tags : '')}">
                            </div>
                            <div class="kb-field">
                                <label>Phòng ban / Chủ sở hữu</label>
                                <input type="text" name="department" maxlength="100" value="${fn:escapeXml(not empty selectedDoc ? selectedDoc.department : 'CSKH Gia Lai')}">
                            </div>
                            <div class="kb-field">
                                <label>Chunk Size (200..1200)</label>
                                <input type="number" name="chunkSize" min="200" max="1200" value="${not empty selectedDoc ? selectedDoc.chunkSize : ragSettings.chunkSize}">
                            </div>
                            <div class="kb-field">
                                <label>Chunk Overlap (0..400)</label>
                                <input type="number" name="chunkOverlap" min="0" max="400" value="${not empty selectedDoc ? selectedDoc.chunkOverlap : ragSettings.chunkOverlap}">
                            </div>
                            <div class="kb-field">
                                <label>Ghi chú phiên bản (Change Note)</label>
                                <input type="text" name="changeNote" maxlength="500" placeholder="Mô tả nội dung thay đổi...">
                            </div>
                            <c:if test="${not empty selectedDoc && selectedDoc.workflowStatus == 'PUBLISHED'}">
                                <div class="kb-field">
                                    <label>Chế độ cập nhật tài liệu đang PUBLISHED</label>
                                    <select name="preservePublishedState">
                                        <option value="true">Giữ PUBLISHED và đánh dấu NEEDS_REINDEX</option>
                                        <option value="false">Đưa về DRAFT để duyệt lại từ đầu</option>
                                    </select>
                                </div>
                            </c:if>
                        </div>
                    </details>

                    <div class="kb-actions-inline">
                        <button type="submit" class="kb-btn"><c:out value="${not empty selectedDoc ? 'Lưu phiên bản mới (v+1)' : 'Tạo bản nháp (DRAFT)'}"/></button>
                        <c:if test="${not empty selectedDoc}">
                            <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents">+ Tạo tài liệu mới</a>
                        </c:if>
                    </div>
                </form>
            </div>

            <!-- LIFECYCLE WORKFLOW, VERSION HISTORY & DIFF -->
            <details class="kb-panel"><summary>Lịch sử phiên bản &amp; quy trình nâng cao</summary>
                <c:choose>
                    <c:when test="${empty selectedDoc}">
                        <h3>Quy trình Duyệt, Phiên bản &amp; Xuất bản</h3>
                        <div class="kb-workflow-stepper" style="margin-bottom:14px;">
                            <div class="kb-step is-active"><span class="kb-step-num">1</span><span>Nháp (DRAFT)</span></div>
                            <div class="kb-step"><span class="kb-step-num">2</span><span>Chờ duyệt (PENDING)</span></div>
                            <div class="kb-step"><span class="kb-step-num">3</span><span>Đã duyệt (APPROVED)</span></div>
                            <div class="kb-step"><span class="kb-step-num">4</span><span>Xuất bản (PUBLISHED)</span></div>
                            <div class="kb-step"><span class="kb-step-num">5</span><span>Lập chỉ mục (INDEXED)</span></div>
                        </div>
                        <p style="color:#94a3b8;font-size:13.5px;">Chọn một tài liệu ở bảng bên trên để thực hiện quy trình duyệt (<code>DRAFT -&gt; PENDING_REVIEW -&gt; APPROVED -&gt; PUBLISHED</code>), xem lịch sử phiên bản, so sánh diff hoặc khôi phục (rollback).</p>
                    </c:when>
                    <c:otherwise>
                        <h3>Quy trình Duyệt &amp; Xuất bản: #<c:out value="${selectedDoc.id}"/> (<span class="kb-badge kb-badge-${selectedDoc.workflowStatus}"><c:out value="${selectedDoc.workflowStatus}"/></span>)</h3>
                        <div class="kb-workflow-stepper" style="margin-bottom:12px;">
                            <div class="kb-step ${selectedDoc.workflowStatus == 'DRAFT' ? 'is-active' : 'is-done'}"><span class="kb-step-num">1</span><span>DRAFT</span></div>
                            <div class="kb-step ${selectedDoc.workflowStatus == 'PENDING_REVIEW' ? 'is-active' : (selectedDoc.workflowStatus == 'APPROVED' || selectedDoc.workflowStatus == 'PUBLISHED' ? 'is-done' : '')}"><span class="kb-step-num">2</span><span>PENDING_REVIEW</span></div>
                            <div class="kb-step ${selectedDoc.workflowStatus == 'APPROVED' ? 'is-active' : (selectedDoc.workflowStatus == 'PUBLISHED' ? 'is-done' : '')}"><span class="kb-step-num">3</span><span>APPROVED</span></div>
                            <div class="kb-step ${selectedDoc.workflowStatus == 'PUBLISHED' ? 'is-active' : ''}"><span class="kb-step-num">4</span><span>PUBLISHED</span></div>
                            <div class="kb-step ${selectedDoc.indexingStatus == 'ready' ? 'is-done' : ''}"><span class="kb-step-num">5</span><span>INDEXED</span></div>
                        </div>
                        <p style="font-size:12.5px;color:#94a3b8;">Content Hash: <code><c:out value="${selectedDoc.contentHash}"/></code></p>

                        <div class="kb-actions-inline" style="margin-bottom:16px;">
                            <c:if test="${selectedDoc.workflowStatus == 'DRAFT' || selectedDoc.workflowStatus == 'REJECTED' || selectedDoc.workflowStatus == 'UNPUBLISHED'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                    <input type="hidden" name="action" value="submitReview">
                                    <input type="hidden" name="id" value="${selectedDoc.id}">
                                    <button type="submit" class="kb-btn">Gửi duyệt (Submit for Review)</button>
                                </form>
                            </c:if>

                            <c:if test="${selectedDoc.workflowStatus == 'PENDING_REVIEW'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                    <input type="hidden" name="action" value="approveDocument">
                                    <input type="hidden" name="id" value="${selectedDoc.id}">
                                    <button type="submit" class="kb-btn kb-btn-success">Phê duyệt (Approve)</button>
                                </form>
                            </c:if>

                            <c:if test="${selectedDoc.workflowStatus == 'APPROVED'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                    <input type="hidden" name="action" value="publishDocument"><input type="hidden" name="version" value="${selectedDoc.version}">
                                    <input type="hidden" name="id" value="${selectedDoc.id}">
                                    <button type="submit" class="kb-btn kb-btn-success">Xuất bản (Publish)</button>
                                </form>
                            </c:if>

                            <c:if test="${selectedDoc.workflowStatus == 'PUBLISHED'}">
                                <c:if test="${selectedDoc.indexMode == 'MOCK INDEX'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                    <input type="hidden" name="action" value="reindexDocument">
                                    <input type="hidden" name="id" value="${selectedDoc.id}">
                                    <button type="submit" class="kb-btn">Lập chỉ mục Offline ngay (Reindex)</button>
                                </form>
                                </c:if>
                                <c:if test="${selectedDoc.indexMode == 'GEMINI INDEX'}"><a class="kb-btn" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=indexing&amp;docId=${selectedDoc.id}">GEMINI INDEX: trạng thái / reindex có phê duyệt</a></c:if>
                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                    <input type="hidden" name="action" value="unpublishDocument">
                                    <input type="hidden" name="id" value="${selectedDoc.id}">
                                    <button type="submit" class="kb-btn kb-btn-danger">Gỡ xuất bản (Unpublish)</button>
                                </form>
                            </c:if>

                            <c:if test="${selectedDoc.workflowStatus != 'ARCHIVED'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                    <input type="hidden" name="action" value="archiveDocument">
                                    <input type="hidden" name="id" value="${selectedDoc.id}">
                                    <button type="submit" class="kb-btn kb-btn-secondary">Lưu trữ (Archive)</button>
                                </form>
                            </c:if>
                        </div>

                        <c:if test="${selectedDoc.workflowStatus == 'PENDING_REVIEW' || selectedDoc.workflowStatus == 'APPROVED'}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" style="margin-bottom:16px;">
                                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                <input type="hidden" name="action" value="rejectDocument">
                                <input type="hidden" name="id" value="${selectedDoc.id}">
                                <div class="kb-field" style="margin-bottom:8px;">
                                    <label>Từ chối duyệt (bắt buộc ghi rõ lý do)</label>
                                    <input type="text" name="rejectionReason" required maxlength="500" placeholder="Nhập lý do từ chối...">
                                </div>
                                <button type="submit" class="kb-btn kb-btn-danger">Từ chối (Reject)</button>
                            </form>
                        </c:if>

                        <h4>Lịch sử phiên bản (Immutable Revisions)</h4>
                        <div class="kb-table-wrap" style="margin-bottom:14px;">
                            <table class="kb-table">
                                <thead>
                                    <tr>
                                        <th>Ver</th>
                                        <th>Trạng thái</th>
                                        <th>Ghi chú</th>
                                        <th>Thời gian</th>
                                        <th>Khôi phục</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${docRevisions}" var="rev">
                                        <tr>
                                            <td><strong>v<c:out value="${rev.versionNumber}"/></strong></td>
                                            <td><span class="kb-badge kb-badge-${rev.workflowStatus}"><c:out value="${rev.workflowStatus}"/></span></td>
                                            <td><c:out value="${rev.changeNote}"/></td>
                                            <td><small><c:out value="${rev.createdAt}"/></small></td>
                                            <td>
                                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                                    <input type="hidden" name="action" value="rollbackDocument">
                                                    <input type="hidden" name="id" value="${selectedDoc.id}">
                                                    <input type="hidden" name="targetVersion" value="${rev.versionNumber}">
                                                    <button type="submit" class="kb-btn kb-btn-secondary">Rollback -&gt; v+1</button>
                                                </form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>

                        <c:if test="${not empty revisionDiff}">
                            <details class="kb-advanced-details" open>
                                <summary class="kb-advanced-summary">
                                    <span>So sánh phiên bản v<c:out value="${revisionDiff.fromVersion}"/> vs v<c:out value="${revisionDiff.toVersion}"/> (+<c:out value="${revisionDiff.addedCount}"/> / -<c:out value="${revisionDiff.removedCount}"/> dòng)</span>
                                    <span class="kb-badge kb-badge-APPROVED">Diff</span>
                                </summary>
                                <div class="kb-advanced-body">
                                    <c:if test="${not empty revisionDiff.metadataChanges}">
                                        <ul style="font-size:12px;color:#fcd34d;">
                                            <c:forEach items="${revisionDiff.metadataChanges}" var="mc">
                                                <li><c:out value="${mc}"/></li>
                                            </c:forEach>
                                        </ul>
                                    </c:if>
                                    <div class="kb-diff-box">
                                        <c:forEach items="${revisionDiff.diffLines}" var="dl">
                                            <div class="${fn:startsWith(dl, '+ ') ? 'kb-diff-add' : (fn:startsWith(dl, '- ') ? 'kb-diff-del' : '')}"><c:out value="${dl}"/></div>
                                        </c:forEach>
                                    </div>
                                </div>
                            </details>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </details>
        </div>
    </c:if>

    <!-- ===================================================================
         TAB 3: SECURE DOCUMENT UPLOAD & TEXT EXTRACTION
         =================================================================== -->
    <c:if test="${kbTab == 'faqs'}">
        <div class="kb-grid-2">
            <div class="kb-panel">
                <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;margin-bottom:12px;">
                    <h3 style="margin:0;">Danh sách Câu hỏi thường gặp (FAQ)</h3>
                    <div class="kb-actions-inline">
                        <input type="search" id="faqQuickSearch" placeholder="Tìm nhanh câu hỏi / trả lời..." style="padding:7px 11px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;font-size:12.5px;min-width:200px;">
                        <select id="faqStatusFilter" aria-label="Lọc trạng thái FAQ" style="padding:7px 10px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;font-size:12.5px;">
                            <option value="ALL">Tất cả trạng thái</option>
                            <option value="PUBLISHED">PUBLISHED</option>
                            <option value="APPROVED">APPROVED</option>
                            <option value="PENDING_REVIEW">PENDING_REVIEW</option>
                            <option value="DRAFT">DRAFT</option>
                        </select>
                    </div>
                </div>
                <div class="kb-table-wrap">
                    <table class="kb-table" id="kbFaqTable">
                        <thead>
                            <tr>
                                <th>ID / Ver</th>
                                <th>Câu hỏi &amp; Trả lời</th>
                                <th>Ưu tiên</th>
                                <th>Trạng thái</th>
                                <th>RAG Source</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty faqs}">
                                    <tr><td colspan="6">Chưa có mục FAQ nào.</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${faqs}" var="f">
                                        <tr data-faq-row data-faq-status="${fn:escapeXml(f.workflowStatus)}">
                                            <td><strong>#<c:out value="${f.id}"/></strong> · v<c:out value="${f.versionNumber}"/></td>
                                            <td>
                                                <strong><c:out value="${f.question}"/></strong><br>
                                                <small style="color:#94a3b8;"><c:out value="${f.answer}"/></small>
                                            </td>
                                            <td><c:out value="${f.priority}"/></td>
                                            <td><span class="kb-badge kb-badge-${f.workflowStatus}"><c:out value="${f.workflowStatus}"/></span></td>
                                            <td>
                                                <code><c:out value="${f.sourceId}"/></code>
                                                <c:if test="${not empty f.linkedDocumentId}">
                                                    <br><small>Doc #<c:out value="${f.linkedDocumentId}"/></small>
                                                </c:if>
                                            </td>
                                            <td>
                                                <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=faqs&amp;faqId=${f.id}">Sửa / Duyệt</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="kb-panel">
                <h3><c:out value="${not empty selectedFaq ? 'Chỉnh sửa & Duyệt FAQ #' : 'Tạo mới FAQ'}"/><c:if test="${not empty selectedFaq}"><c:out value="${selectedFaq.id}"/></c:if></h3>
                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                    <input type="hidden" name="tab" value="faqs">
                    <input type="hidden" name="action" value="${not empty selectedFaq ? 'updateFaq' : 'createFaq'}">
                    <c:if test="${not empty selectedFaq}">
                        <input type="hidden" name="id" value="${selectedFaq.id}">
                    </c:if>

                    <div class="kb-form-grid">
                        <div class="kb-field">
                            <label>Câu hỏi (Question) *</label>
                            <input type="text" name="question" maxlength="500" required value="${fn:escapeXml(not empty selectedFaq ? selectedFaq.question : '')}">
                        </div>
                        <div class="kb-field">
                            <label>Danh mục</label>
                            <select name="category">
                                <option value="faq" ${not empty selectedFaq && selectedFaq.category == 'faq' ? 'selected' : ''}>faq</option>
                                <option value="policy" ${not empty selectedFaq && selectedFaq.category == 'policy' ? 'selected' : ''}>policy</option>
                                <option value="guide" ${not empty selectedFaq && selectedFaq.category == 'guide' ? 'selected' : ''}>guide</option>
                            </select>
                        </div>
                        <div class="kb-field">
                            <label>Độ ưu tiên (1 = cao nhất)</label>
                            <input type="number" name="priority" min="1" max="1000" value="${not empty selectedFaq ? selectedFaq.priority : 10}">
                        </div>
                    </div>

                    <div class="kb-field" style="margin-bottom:14px;">
                        <label>Câu trả lời chuẩn hóa (Answer) *</label>
                        <textarea name="answer" rows="5" maxlength="20000" required><c:out value="${not empty selectedFaq ? selectedFaq.answer : ''}"/></textarea>
                    </div>

                    <details class="kb-advanced-details" style="margin-bottom:14px;">
                        <summary class="kb-advanced-summary">
                            <span>Metadata nâng cao (Source Authority, Tags, Policy Key)</span>
                            <span class="kb-badge kb-badge-DRAFT">Tùy chọn</span>
                        </summary>
                        <div class="kb-form-grid kb-advanced-body">
                            <div class="kb-field">
                                <label>Source Authority</label>
                                <select name="sourceAuthority">
                                    <option value="admin_reviewed" ${not empty selectedFaq && selectedFaq.sourceAuthority == 'admin_reviewed' ? 'selected' : ''}>admin_reviewed</option>
                                    <option value="official" ${not empty selectedFaq && selectedFaq.sourceAuthority == 'official' ? 'selected' : ''}>official</option>
                                </select>
                            </div>
                            <div class="kb-field">
                                <label>Tags</label>
                                <input type="text" name="tags" maxlength="500" value="${fn:escapeXml(not empty selectedFaq ? selectedFaq.tags : '')}">
                            </div>
                            <div class="kb-field">
                                <label>Policy Key</label>
                                <input type="text" name="policyKey" maxlength="100" value="${fn:escapeXml(not empty selectedFaq ? selectedFaq.policyKey : '')}">
                            </div>
                        </div>
                    </details>

                    <div class="kb-actions-inline">
                        <button type="submit" class="kb-btn"><c:out value="${not empty selectedFaq ? 'Lưu cập nhật FAQ' : 'Tạo FAQ (DRAFT)'}"/></button>
                        <c:if test="${not empty selectedFaq}">
                            <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=faqs">+ Tạo FAQ mới</a>
                        </c:if>
                    </div>
                </form>

                <c:if test="${not empty selectedFaq}">
                    <hr style="border-color:rgba(255,255,255,0.08);margin:16px 0;">
                    <div class="kb-actions-inline">
                        <c:if test="${selectedFaq.workflowStatus == 'DRAFT' || selectedFaq.workflowStatus == 'REJECTED' || selectedFaq.workflowStatus == 'UNPUBLISHED'}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                <input type="hidden" name="action" value="submitFaqReview">
                                <input type="hidden" name="id" value="${selectedFaq.id}">
                                <button type="submit" class="kb-btn">Gửi duyệt FAQ</button>
                            </form>
                        </c:if>
                        <c:if test="${selectedFaq.workflowStatus == 'PENDING_REVIEW'}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                <input type="hidden" name="action" value="approveFaq">
                                <input type="hidden" name="id" value="${selectedFaq.id}">
                                <button type="submit" class="kb-btn kb-btn-success">Duyệt FAQ (Approve)</button>
                            </form>
                        </c:if>
                        <c:if test="${selectedFaq.workflowStatus == 'APPROVED'}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                <input type="hidden" name="action" value="publishFaq">
                                <input type="hidden" name="id" value="${selectedFaq.id}">
                                <button type="submit" class="kb-btn kb-btn-success">Xuất bản FAQ vào RAG</button>
                            </form>
                        </c:if>
                        <c:if test="${selectedFaq.workflowStatus == 'PUBLISHED'}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                <input type="hidden" name="action" value="unpublishFaq">
                                <input type="hidden" name="id" value="${selectedFaq.id}">
                                <button type="submit" class="kb-btn kb-btn-danger">Gỡ xuất bản FAQ</button>
                            </form>
                        </c:if>
                        <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                            <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                            <input type="hidden" name="action" value="archiveFaq">
                            <input type="hidden" name="id" value="${selectedFaq.id}">
                            <button type="submit" class="kb-btn kb-btn-secondary">Lưu trữ FAQ</button>
                        </form>
                    </div>
                </c:if>
            </div>
        </div>
        <script>
            (function () {
                var searchInput = document.getElementById('faqQuickSearch');
                var statusSelect = document.getElementById('faqStatusFilter');
                var table = document.getElementById('kbFaqTable');
                if (!searchInput || !statusSelect || !table) return;
                function filterFaqs() {
                    var q = (searchInput.value || '').toLowerCase().trim();
                    var st = statusSelect.value || 'ALL';
                    var rows = table.querySelectorAll('tr[data-faq-row]');
                    for (var i = 0; i < rows.length; i++) {
                        var row = rows[i];
                        var rowStatus = row.getAttribute('data-faq-status') || '';
                        var text = (row.textContent || '').toLowerCase();
                        var matchQ = !q || text.indexOf(q) !== -1;
                        var matchSt = st === 'ALL' || rowStatus === st;
                        row.style.display = (matchQ && matchSt) ? '' : 'none';
                    }
                }
                searchInput.addEventListener('input', filterFaqs);
                statusSelect.addEventListener('change', filterFaqs);
            })();
        </script>
    </c:if>

    <!-- ===================================================================
         TAB 5: CHUNKING & OFFLINE INDEXING PIPELINE
         =================================================================== -->
    <c:if test="${kbTab == 'indexing'}">
        <div class="kb-metrics-grid">
            <div class="kb-metric-card">
                <div class="metric-label">Đã đồng bộ (Ready)</div>
                <div class="metric-value" style="color:#4ade80;"><c:out value="${metrics.indexedDocuments}"/></div>
                <div class="metric-sub">Tổng số Chunk: <c:out value="${metrics.totalChunks}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Cần lập chỉ mục (Pending / Stale)</div>
                <div class="metric-value" style="color:#fbbf24;"><c:out value="${metrics.needingReindexDocuments}"/></div>
                <div class="metric-sub">Tài liệu PUBLISHED chờ đồng bộ</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Lỗi lập chỉ mục (Failed)</div>
                <div class="metric-value" style="color:#f87171;"><c:out value="${metrics.indexingFailures}"/></div>
                <div class="metric-sub">Cần kiểm tra &amp; chạy lại</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Embedding Provider hiện hành</div>
                <div class="metric-value" style="font-size:18px;color:#38bdf8;"><c:out value="${systemStatus.geminiIndexingEnabled ? 'GEMINI (768d)' : 'MOCK OFFLINE (768d)'}"/></div>
                <div class="metric-sub">Model: <code><c:out value="${ragSettings.embeddingModel}"/></code></div>
            </div>
        </div>

        <div class="kb-panel">
            <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;">
                <div>
                    <h3>Điều khiển Lập chỉ mục Vector (Gemini 768d &amp; Offline Mock 768d)</h3>
                    <p style="font-size:13px;color:#94a3b8;margin:0;">Hỗ trợ lập chỉ mục trực tiếp bằng <code>gemini-embedding-001</code> (768 chiều) cho môi trường Production hoặc <code>MockEmbeddingProvider</code> offline cho kiểm thử nội bộ.</p>
                </div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                        <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                        <input type="hidden" name="action" value="runGeminiIndexingBatch">
                        <button type="submit" class="kb-btn">Chạy Batch Gemini Index (768d)</button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                        <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                        <input type="hidden" name="action" value="runOfflineIndexingBatch">
                        <button type="submit" class="kb-btn kb-btn-secondary">Chạy Batch Lập chỉ mục Offline</button>
                    </form>
                </div>
            </div>
        </div>

        <div class="kb-grid-2">
            <div class="kb-panel">
                <h3>Tài liệu PUBLISHED cần Lập chỉ mục / Reindex (<c:out value="${fn:length(staleDocuments)}"/>)</h3>
                <div class="kb-table-wrap">
                    <table class="kb-table">
                        <thead>
                            <tr>
                                <th>ID / Ver</th>
                                <th>Tiêu đề</th>
                                <th>Trạng thái Index</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty staleDocuments}">
                                    <tr><td colspan="4">Tất cả tài liệu PUBLISHED đều đã được lập chỉ mục đồng bộ (ready).</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${staleDocuments}" var="sd">
                                        <tr>
                                            <td><strong>#<c:out value="${sd.id}"/></strong> · v<c:out value="${sd.version}"/></td>
                                            <td><c:out value="${sd.title}"/></td>
                                            <td><span class="kb-badge kb-badge-PENDING_REVIEW"><c:out value="${sd.indexMode}"/> · <c:out value="${sd.runtimeIndexStatus}"/> (indexed: <c:out value="${sd.indexedVersion}"/>)</span></td>
                                            <td>
                                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" style="display:inline;">
                                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                                    <input type="hidden" name="action" value="requestGeminiIndex">
                                                    <input type="hidden" name="id" value="${sd.id}">
                                                    <button type="submit" class="kb-btn">Gemini Index</button>
                                                </form>
                                                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" style="display:inline;">
                                                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                                                    <input type="hidden" name="action" value="reindexDocument">
                                                    <input type="hidden" name="id" value="${sd.id}">
                                                    <button type="submit" class="kb-btn kb-btn-secondary">Mock Index</button>
                                                </form>
                                                <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=indexing&amp;docId=${sd.id}">Xem Chunks</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="kb-panel">
                <h3>Xem trước Phân đoạn (Chunk Preview)</h3>
                <c:choose>
                    <c:when test="${empty selectedDoc}">
                        <p style="color:#94a3b8;font-size:13.5px;">Truy cập với tham số <code>?tab=indexing&amp;docId=&lt;id&gt;</code> hoặc chọn một tài liệu để xem trước danh sách các chunk, độ dài ký tự, ước tính token và mã băm SHA-256.</p>
                    </c:when>
                    <c:otherwise>
                        <p><strong>Tài liệu #<c:out value="${selectedDoc.id}"/>:</strong> <c:out value="${selectedDoc.title}"/> (Chunk Size: <c:out value="${selectedDoc.chunkSize}"/>, Overlap: <c:out value="${selectedDoc.chunkOverlap}"/>)</p>
                        <div class="kb-table-wrap">
                            <table class="kb-table">
                                <thead>
                                    <tr>
                                        <th>#</th>
                                        <th>Ký tự / ~Token</th>
                                        <th>Nội dung Chunk</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${chunkPreviews}" var="cp">
                                        <tr>
                                            <td><strong>#<c:out value="${cp.ordinal}"/></strong></td>
                                            <td><c:out value="${cp.charCount}"/> chars<br><small>~<c:out value="${cp.estimatedTokens}"/> tokens</small></td>
                                            <td><c:out value="${cp.content}"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <jsp:include page="/view/admin/knowledge-gemini-runtime.jsp"/>
    </c:if>

    <!-- ===================================================================
         TAB 6: ADMIN RAG CONFIGURATION
         =================================================================== -->
    <c:if test="${kbTab == 'rag-config'}">
        <div class="kb-panel">
            <h3>Cấu hình RAG &amp; Kiểm soát Truy hồi</h3>
            <p style="font-size:13px;color:#94a3b8;">Quản lý bật/tắt <strong>Production Customer RAG</strong> (Chatbot ngoài trang chủ), <strong>Staging / Test RAG</strong> và <strong>Local Offline Indexing</strong>, cùng các thông số truy hồi Vector + FTS.</p>

            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                <input type="hidden" name="action" value="saveRagConfig">
                <input type="hidden" name="tab" value="rag-config">

                <div class="kb-form-grid">
                    <div class="kb-field">
                        <label>Local Indexing Enabled (Offline Mock)</label>
                        <select name="localIndexingEnabled">
                            <option value="true" ${ragSettings.localIndexingEnabled ? 'selected' : ''}>Bật (true)</option>
                            <option value="false" ${!ragSettings.localIndexingEnabled ? 'selected' : ''}>Tắt (false)</option>
                        </select>
                    </div>
                    <div class="kb-field">
                        <label>Staging / Test RAG Enabled</label>
                        <select name="stagingRagEnabled">
                            <option value="true" ${ragSettings.stagingRagEnabled ? 'selected' : ''}>Bật (true)</option>
                            <option value="false" ${!ragSettings.stagingRagEnabled ? 'selected' : ''}>Tắt (false)</option>
                        </select>
                    </div>
                    <div class="kb-field">
                        <label>Production Customer RAG (Chatbot Trang chủ)</label>
                        <select name="productionRagEnabled">
                            <option value="true" ${ragSettings.productionRagEnabled ? 'selected' : ''}>Bật (true) — Đang áp dụng cho Chatbot</option>
                            <option value="false" ${!ragSettings.productionRagEnabled ? 'selected' : ''}>Tắt (false) — Tạm ngưng ngoài trang chủ</option>
                        </select>
                    </div>
                    <div class="kb-field">
                        <label>Retrieval Top-K (1..12)</label>
                        <input type="number" name="topK" min="1" max="12" value="${ragSettings.topK}">
                    </div>
                    <div class="kb-field">
                        <label>Similarity Threshold (0.28..0.95)</label>
                        <input type="number" name="similarityThreshold" step="0.01" min="0.28" max="0.95" value="${ragSettings.similarityThreshold}">
                    </div>
                    <div class="kb-field">
                        <label>Max Context Tokens (200..1200)</label>
                        <input type="number" name="maxContextTokens" min="200" max="1200" value="${ragSettings.maxContextTokens}">
                    </div>
                    <div class="kb-field">
                        <label>Default Chunk Size (200..1200)</label>
                        <input type="number" name="chunkSize" min="200" max="1200" value="${ragSettings.chunkSize}">
                    </div>
                    <div class="kb-field">
                        <label>Default Chunk Overlap (0..400)</label>
                        <input type="number" name="chunkOverlap" min="0" max="400" value="${ragSettings.chunkOverlap}">
                    </div>
                </div>

                <button type="submit" class="kb-btn">Lưu Cấu hình RAG</button>
            </form>
        </div>
    </c:if>

    <!-- ===================================================================
         TAB 7: AI MONITORING & STRUCTURED AUDIT LOGGING
         =================================================================== -->
    <c:if test="${kbTab == 'audit'}">
        <div class="kb-metrics-grid">
            <div class="kb-metric-card">
                <div class="metric-label">Tỷ lệ Abstention / No-Source</div>
                <div class="metric-value" style="font-size:18px;color:#94a3b8;">Chưa có dữ liệu</div>
                <div class="metric-sub">Production RAG đang tắt</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Số lần Fallback sang Backup Model</div>
                <div class="metric-value" style="font-size:18px;color:#94a3b8;">Chưa có dữ liệu</div>
                <div class="metric-sub">Chưa kích hoạt lưu lượng thật</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Lỗi Provider / Timeout</div>
                <div class="metric-value" style="font-size:18px;color:#94a3b8;">Chưa có dữ liệu</div>
                <div class="metric-sub">0 Gemini API requests</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Lỗi Lập chỉ mục (Indexing Failures)</div>
                <div class="metric-value"><c:out value="${metrics.indexingFailures}"/></div>
                <div class="metric-sub">Từ bảng knowledge_documents</div>
            </div>
        </div>

        <div class="kb-panel">
            <div style="display:flex;justify-content:space-between;align-items:flex-end;flex-wrap:wrap;gap:12px;margin-bottom:14px;">
                <div>
                    <h3 style="margin:0 0 4px;">Nhật ký Kiểm toán Quản trị (Admin Audit Logs — Không lưu API Key / Vector)</h3>
                    <small style="color:#94a3b8;">Tổng số bản ghi khớp: <strong><c:out value="${not empty auditTotalCount ? auditTotalCount : fn:length(auditLogs)}"/></strong> (Trang <c:out value="${not empty auditPage ? auditPage : 1}"/> / <c:out value="${not empty auditTotalPages ? auditTotalPages : 1}"/>)</small>
                </div>
                <form method="get" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-actions-inline">
                    <input type="hidden" name="tab" value="audit">
                    <select name="auditResource" aria-label="Lọc theo loại tài nguyên" style="padding:7px 10px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;font-size:12.5px;">
                        <option value="all" ${auditResource == 'all' ? 'selected' : ''}>Tất cả tài nguyên</option>
                        <option value="DOCUMENT" ${auditResource == 'DOCUMENT' ? 'selected' : ''}>DOCUMENT</option>
                        <option value="FAQ" ${auditResource == 'FAQ' ? 'selected' : ''}>FAQ</option>
                        <option value="RAG_CONFIG" ${auditResource == 'RAG_CONFIG' ? 'selected' : ''}>RAG_CONFIG</option>
                        <option value="INDEX_BATCH" ${auditResource == 'INDEX_BATCH' ? 'selected' : ''}>INDEX_BATCH</option>
                        <option value="PLAYGROUND" ${auditResource == 'PLAYGROUND' ? 'selected' : ''}>PLAYGROUND</option>
                    </select>
                    <input type="text" name="auditAction" value="${fn:escapeXml(auditAction == 'all' ? '' : auditAction)}" placeholder="Hành động (vd: PUBLISH_DOCUMENT)..." style="padding:7px 10px;border-radius:8px;border:1px solid rgba(255,255,255,0.14);background:rgba(30,41,59,0.9);color:#f8fafc;font-size:12.5px;min-width:210px;">
                    <button type="submit" class="kb-btn">Lọc nhật ký</button>
                    <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=audit">Đặt lại</a>
                </form>
            </div>
            <div class="kb-table-wrap">
                <table class="kb-table">
                    <thead>
                        <tr>
                            <th>ID / Thời gian</th>
                            <th>Correlation ID</th>
                            <th>Admin</th>
                            <th>Hành động</th>
                            <th>Tài nguyên</th>
                            <th>Trạng thái Trước -&gt; Sau</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty auditLogs}">
                                <tr><td colspan="6">Chưa có bản ghi kiểm toán nào.</td></tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach items="${auditLogs}" var="log">
                                    <tr>
                                        <td><strong>#<c:out value="${log.id}"/></strong><br><small><c:out value="${log.createdAt}"/></small></td>
                                        <td><code><c:out value="${fn:substring(log.correlationId, 0, 12)}"/>...</code></td>
                                        <td><c:out value="${log.actorUsername}"/> (#<c:out value="${log.actorId}"/>)</td>
                                        <td><span class="kb-badge kb-badge-APPROVED"><c:out value="${log.action}"/></span></td>
                                        <td><code><c:out value="${log.resourceType}"/>:<c:out value="${log.resourceId}"/></code><br><small><c:out value="${log.summary}"/></small></td>
                                        <td>
                                            <details class="kb-audit-state-details">
                                                <summary style="cursor:pointer;font-size:12px;color:#38bdf8;">Xem chi tiết Trước / Sau</summary>
                                                <div style="margin-top:4px;">
                                                    <small style="color:#94a3b8;">Trước: <code><c:out value="${log.previousState}"/></code></small><br>
                                                    <small style="color:#6ee7b7;">Sau: <code><c:out value="${log.newState}"/></code></small>
                                                </div>
                                            </details>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
            <c:if test="${not empty auditTotalPages && auditTotalPages > 1}">
                <div class="kb-actions-inline" style="margin-top:12px;justify-content:flex-end;">
                    <c:if test="${auditPage > 1}">
                        <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=audit&amp;auditResource=${fn:escapeXml(auditResource)}&amp;auditAction=${fn:escapeXml(auditAction)}&amp;auditPage=${auditPage - 1}">&larr; Trang trước</a>
                    </c:if>
                    <c:if test="${auditPage < auditTotalPages}">
                        <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=audit&amp;auditResource=${fn:escapeXml(auditResource)}&amp;auditAction=${fn:escapeXml(auditAction)}&amp;auditPage=${auditPage + 1}">Trang tiếp &rarr;</a>
                    </c:if>
                </div>
            </c:if>
        </div>
    </c:if>

    <!-- ===================================================================
         TAB 8: ADMIN RAG TEST PLAYGROUND (OFFLINE ONLY)
         =================================================================== -->
    <c:if test="${kbTab == 'playground'}">
        <div class="kb-playground-banner" role="status">
            <c:choose><c:when test="${not empty playgroundResult}"><c:out value="${playgroundResult.banner}"/></c:when><c:otherwise>MOCK PLAYGROUND — nút kiểm thử Offline bên dưới không gọi Gemini</c:otherwise></c:choose>
        </div>

        <div class="kb-panel">
            <h3>Kiểm thử Truy hồi RAG &amp; Chính sách Nguồn (100% Offline)</h3>
            <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                <input type="hidden" name="action" value="runPlaygroundQuery">
                <input type="hidden" name="tab" value="playground">

                <div class="kb-field" style="margin-bottom:12px;">
                    <label for="pg-query">Nhập câu hỏi của khách hàng để kiểm tra các Chunk được truy xuất, điểm tương đồng, độ tin cậy và phản hồi mẫu:</label>
                    <input id="pg-query" type="text" name="query" maxlength="1000" required
                           value="${fn:escapeXml(not empty playgroundResult ? playgroundResult.query : '')}"
                           placeholder="Ví dụ: Chính sách bảo hành thiết bị modem WiFi FPT như thế nào?">
                </div>
                <button type="submit" class="kb-btn">Chạy Kiểm thử Truy hồi Offline</button>
            </form>
        </div>

        <c:if test="${not empty playgroundResult}">
            <div class="kb-grid-2">
                <div class="kb-panel">
                    <h3>Kết quả truy hồi &amp; Kiểm tra Xung đột</h3>
                    <p>
                        <strong>Selection Issue:</strong> <span class="kb-badge kb-badge-${playgroundResult.abstained ? 'PENDING_REVIEW' : 'PUBLISHED'}"><c:out value="${playgroundResult.selectionIssue}"/></span>
                        · <strong>Abstained:</strong> <code><c:out value="${playgroundResult.abstained}"/></code>
                    </p>
                    <div class="kb-diff-box" style="margin-bottom:14px;"><c:out value="${playgroundResult.deterministicMockAnswer}"/></div>

                    <details class="kb-advanced-details">
                        <summary class="kb-advanced-summary">
                            <span>Ngữ cảnh JSON chuyển cho bộ trích dẫn (RagContextBuilder)</span>
                            <span class="kb-badge kb-badge-APPROVED">JSON Diagnostics</span>
                        </summary>
                        <div class="kb-advanced-body">
                            <div class="kb-diff-box"><c:out value="${playgroundResult.constructedContextJson}"/></div>
                        </div>
                    </details>
                </div>

                <div class="kb-panel">
                    <h3>Danh sách Chunks Ứng viên (<c:out value="${fn:length(playgroundResult.candidateChunks)}"/>)</h3>
                    <div class="kb-table-wrap">
                        <table class="kb-table">
                            <thead>
                                <tr>
                                    <th>Chunk / Doc</th>
                                    <th>Tiêu đề &amp; Nội dung</th>
                                    <th>Điểm / Authority</th>
                                    <th>Quyết định Policy</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${empty playgroundResult.candidateChunks}">
                                        <tr><td colspan="4">Không tìm thấy chunk nào khớp câu hỏi.</td></tr>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach items="${playgroundResult.candidateChunks}" var="ch">
                                            <tr>
                                                <td>
                                                    Chunk #<c:out value="${ch.chunkId}"/><br>
                                                    <small>Doc #<c:out value="${ch.documentId}"/> (v<c:out value="${ch.documentVersion}"/>)</small>
                                                </td>
                                                <td>
                                                    <strong><c:out value="${ch.title}"/></strong><br>
                                                    <small><c:out value="${ch.content}"/></small>
                                                </td>
                                                <td>
                                                    Score: <code><c:out value="${ch.score}"/></code><br>
                                                    Auth: <code><c:out value="${ch.authority}"/></code>
                                                </td>
                                                <td>
                                                    <span class="kb-badge kb-badge-${ch.policyDecision}"><c:out value="${ch.policyDecision}"/></span>
                                                    <c:if test="${ch.selectedForContext}">
                                                        <br><span class="kb-badge kb-badge-PUBLISHED">SELECTED</span>
                                                    </c:if>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </c:if>

        <jsp:include page="/view/admin/knowledge-gemini-runtime.jsp"/>
    </c:if>
</main>
<script src="${pageContext.request.contextPath}/assets/admin/admin-kb-lifecycle.js"></script>
</body>
</html>
