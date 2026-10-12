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
            <h1>AI Knowledge Base &amp; RAG Management System</h1>
            <p>Quản lý tài liệu, duyệt xuất bản có phiên bản và lập chỉ mục vector 768 chiều. MOCK INDEX phục vụ offline; GEMINI INDEX local cần kế hoạch riêng được duyệt.</p>
        </div>
        <div class="kb-offline-badge">
            <span>● OFFLINE TEST MODE — 0 GEMINI API CALLS</span>
            <span>| Production RAG: LOCKED OFF</span>
        </div>
    </div>

    <!-- FLASH & ERROR MESSAGES -->
    <c:if test="${not empty kbError}">
        <div class="kb-alert kb-alert-error" role="alert"><c:out value="${kbError}"/></div>
    </c:if>
    <c:if test="${not empty sessionScope.kbMessage}">
        <div class="kb-alert kb-alert-success" role="status"><c:out value="${sessionScope.kbMessage}"/></div>
        <c:remove var="kbMessage" scope="session"/>
    </c:if>

    <c:if test="${kbTab == 'documents' && selectedDoc.sourceType == 'UPLOAD' && selectedDoc.workflowStatus == 'DRAFT'}">
        <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" class="kb-panel">
            <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
            <input type="hidden" name="action" value="prepareImport"><input type="hidden" name="id" value="${selectedDoc.id}">
            <input type="hidden" name="version" value="${selectedDoc.version}"><button class="kb-btn" type="submit">Giữ bản gốc &amp; preview từng mục</button>
        </form>
    </c:if>
    <!-- SUB-NAVIGATION TABS -->
    <nav class="kb-subnav" aria-label="Knowledge Base Sections">
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=dashboard" class="${kbTab == 'dashboard' ? 'active' : ''}">1. Tổng quan Dashboard</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents" class="${kbTab == 'documents' ? 'active' : ''}">2. Tài liệu &amp; Duyệt xuất bản (${metrics.totalDocuments})</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=upload" class="${kbTab == 'upload' ? 'active' : ''}">3. Tải lên &amp; Trích xuất tệp</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=imports" class="${kbTab == 'imports' ? 'active' : ''}">Phân mục &amp; Review nhập liệu</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=faqs" class="${kbTab == 'faqs' ? 'active' : ''}">4. Quản lý FAQ (${metrics.totalFaqs})</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=indexing" class="${kbTab == 'indexing' ? 'active' : ''}">5. Chunking &amp; Lập chỉ mục (${metrics.needingReindexDocuments})</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=rag-config" class="${kbTab == 'rag-config' ? 'active' : ''}">6. Cấu hình RAG</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=audit" class="${kbTab == 'audit' ? 'active' : ''}">7. Giám sát &amp; Nhật ký Audit</a>
        <a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=playground" class="${kbTab == 'playground' ? 'active' : ''}">8. RAG Test Playground</a>
    </nav>
    <c:if test="${kbTab == 'imports'}"><jsp:include page="/view/admin/knowledge-import.jsp"/></c:if>

    <!-- ===================================================================
         TAB 1: KNOWLEDGE DASHBOARD
         =================================================================== -->
    <c:if test="${kbTab == 'dashboard'}">
        <div class="kb-metrics-grid">
            <div class="kb-metric-card">
                <div class="metric-label">Tổng tài liệu</div>
                <div class="metric-value"><c:out value="${metrics.totalDocuments}"/></div>
                <div class="metric-sub">Tất cả phiên bản hiện hành</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Bản nháp (DRAFT)</div>
                <div class="metric-value"><c:out value="${metrics.draftDocuments}"/></div>
                <div class="metric-sub">Chưa gửi duyệt</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Chờ duyệt (PENDING)</div>
                <div class="metric-value"><c:out value="${metrics.pendingReviewDocuments}"/></div>
                <div class="metric-sub">Đã duyệt chờ phát hành: <c:out value="${metrics.approvedDocuments}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Đã xuất bản (PUBLISHED)</div>
                <div class="metric-value" style="color:#4ade80;"><c:out value="${metrics.publishedDocuments}"/></div>
                <div class="metric-sub">Đủ điều kiện đưa vào RAG</div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Bị từ chối / Lỗi xử lý</div>
                <div class="metric-value" style="color:#f87171;"><c:out value="${metrics.rejectedDocuments + metrics.failedDocuments}"/></div>
                <div class="metric-sub">Rejected: <c:out value="${metrics.rejectedDocuments}"/> · Failed: <c:out value="${metrics.failedDocuments}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Lưu trữ / Gỡ xuất bản</div>
                <div class="metric-value"><c:out value="${metrics.archivedDocuments + metrics.unpublishedDocuments}"/></div>
                <div class="metric-sub">Archived: <c:out value="${metrics.archivedDocuments}"/> · Unpublished: <c:out value="${metrics.unpublishedDocuments}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Đã lập chỉ mục (READY)</div>
                <div class="metric-value" style="color:#38bdf8;"><c:out value="${metrics.indexedDocuments}"/></div>
                <div class="metric-sub">Tổng số Chunk: <c:out value="${metrics.totalChunks}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Cần lập chỉ mục lại</div>
                <div class="metric-value" style="color:#fbbf24;"><c:out value="${metrics.needingReindexDocuments}"/></div>
                <div class="metric-sub">Lỗi lập chỉ mục: <c:out value="${metrics.indexingFailures}"/></div>
            </div>
            <div class="kb-metric-card">
                <div class="metric-label">Kho câu hỏi FAQ</div>
                <div class="metric-value"><c:out value="${metrics.totalFaqs}"/></div>
                <div class="metric-sub">Đã xuất bản RAG: <c:out value="${metrics.publishedFaqs}"/></div>
            </div>
        </div>

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
                    <li><strong>Production Customer RAG:</strong> <span class="kb-badge kb-badge-DRAFT">OFF (Hard-Locked Server-Side)</span></li>
                    <li><strong>Local Offline Indexing:</strong> <span class="kb-badge kb-badge-PUBLISHED"><c:out value="${ragSettings.localIndexingEnabled ? 'ENABLED' : 'DISABLED'}"/></span></li>
                    <li><strong>Embedding Provider (Offline):</strong> <code>MockEmbeddingProvider (<c:out value="${ragSettings.embeddingModel}"/> / <c:out value="${ragSettings.vectorDimensions}"/>d)</code></li>
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

                    <div class="kb-field" style="margin-bottom:14px;">
                        <label>Nội dung văn bản chuẩn hóa (không chứa thông tin giá cước/tốc độ gói do DB quản lý) *</label>
                        <textarea name="content" rows="8" maxlength="60000" required><c:out value="${not empty selectedDoc ? selectedDoc.content : ''}"/></textarea>
                    </div>

                    <div class="kb-actions-inline">
                        <button type="submit" class="kb-btn"><c:out value="${not empty selectedDoc ? 'Lưu phiên bản mới (v+1)' : 'Tạo bản nháp (DRAFT)'}"/></button>
                        <c:if test="${not empty selectedDoc}">
                            <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents">+ Tạo tài liệu mới</a>
                        </c:if>
                    </div>
                </form>
            </div>

            <!-- LIFECYCLE WORKFLOW, VERSION HISTORY & DIFF -->
            <div class="kb-panel">
                <c:choose>
                    <c:when test="${empty selectedDoc}">
                        <h3>Quy trình Duyệt, Phiên bản &amp; Xuất bản</h3>
                        <p style="color:#94a3b8;font-size:13.5px;">Chọn một tài liệu ở bảng bên trên để thực hiện quy trình duyệt (<code>DRAFT -&gt; PENDING_REVIEW -&gt; APPROVED -&gt; PUBLISHED</code>), xem lịch sử phiên bản, so sánh diff hoặc khôi phục (rollback).</p>
                    </c:when>
                    <c:otherwise>
                        <h3>Quy trình Duyệt &amp; Xuất bản: #<c:out value="${selectedDoc.id}"/> (<span class="kb-badge kb-badge-${selectedDoc.workflowStatus}"><c:out value="${selectedDoc.workflowStatus}"/></span>)</h3>
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
                                    <input type="hidden" name="action" value="publishDocument">
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
                            <h4>So sánh phiên bản v<c:out value="${revisionDiff.fromVersion}"/> vs v<c:out value="${revisionDiff.toVersion}"/> (+<c:out value="${revisionDiff.addedCount}"/> / -<c:out value="${revisionDiff.removedCount}"/> dòng)</h4>
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
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </c:if>

    <!-- ===================================================================
         TAB 3: SECURE DOCUMENT UPLOAD & TEXT EXTRACTION
         =================================================================== -->
    <c:if test="${kbTab == 'upload'}">
        <div class="kb-grid-2">
            <div class="kb-panel">
                <h3>Tải lên &amp; Trích xuất văn bản an toàn (.PDF, .DOCX, .TXT, .MD)</h3>
                <p style="font-size:13px;color:#94a3b8;">Hệ thống kiểm tra chữ ký byte (magic bytes), chặn thực thi/macro/ZIP bomb, lưu ngoài web root và tạo bản nháp <code>DRAFT</code> để Admin kiểm duyệt trước khi xuất bản.</p>
                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base" enctype="multipart/form-data">
                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                    <input type="hidden" name="action" value="uploadDocument">
                    <input type="hidden" name="tab" value="upload">

                    <div class="kb-form-grid">
                        <div class="kb-field">
                            <label>Chọn tệp (.pdf, .docx, .txt, .md — tối đa 10 MB) *</label>
                            <input type="file" name="documentFile" accept=".pdf,.docx,.txt,.md" required>
                        </div>
                        <div class="kb-field">
                            <label>Tiêu đề tài liệu (để trống sẽ lấy theo tên tệp)</label>
                            <input type="text" name="title" maxlength="200">
                        </div>
                        <div class="kb-field">
                            <label>Danh mục</label>
                            <select name="category">
                                <option value="guide">Hướng dẫn (guide)</option>
                                <option value="policy">Chính sách (policy)</option>
                                <option value="faq">Hỏi đáp (faq)</option>
                            </select>
                        </div>
                        <div class="kb-field">
                            <label>Độ tin cậy (Source Authority)</label>
                            <select name="sourceAuthority">
                                <option value="admin_reviewed">admin_reviewed</option>
                                <option value="official">official</option>
                            </select>
                        </div>
                        <div class="kb-field">
                            <label>Policy Key</label>
                            <input type="text" name="policyKey" maxlength="100" placeholder="vd: support.process">
                        </div>
                        <div class="kb-field">
                            <label>Tags</label>
                            <input type="text" name="tags" maxlength="500">
                        </div>
                        <div class="kb-field">
                            <label>Phòng ban</label>
                            <input type="text" name="department" maxlength="100" value="Kỹ thuật &amp; CSKH">
                        </div>
                    </div>
                    <div class="kb-field"><label><input type="checkbox" name="splitImport" value="true" checked> Giữ bản gốc và preview phân mục (khuyến nghị cho kịch bản nhiều chủ đề)</label></div>
                    <button type="submit" class="kb-btn">Tải lên &amp; Trích xuất vào bản nháp (DRAFT)</button>
                </form>
            </div>

            <div class="kb-panel">
                <h3>Lịch sử tệp đã tải lên</h3>
                <div class="kb-table-wrap">
                    <table class="kb-table">
                        <thead>
                            <tr>
                                <th>Tên tệp gốc</th>
                                <th>Định dạng / Kích thước</th>
                                <th>Trạng thái trích xuất</th>
                                <th>Tài liệu liên kết</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty uploadedFiles}">
                                    <tr><td colspan="4">Chưa có tệp nào được tải lên.</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${uploadedFiles}" var="uf">
                                        <tr>
                                            <td>
                                                <strong><c:out value="${uf.originalFilename}"/></strong><br>
                                                <small style="color:#64748b;">SHA-256: <code><c:out value="${fn:substring(uf.sha256Hash, 0, 16)}"/>...</code></small>
                                            </td>
                                            <td><code>.<c:out value="${uf.fileExtension}"/></code> (<c:out value="${uf.fileSizeBytes}"/> bytes)</td>
                                            <td>
                                                <span class="kb-badge kb-badge-${uf.extractionStatus}"><c:out value="${uf.extractionStatus}"/></span>
                                                (<c:out value="${uf.extractedCharCount}"/> ký tự)
                                                <c:if test="${not empty uf.extractionWarning}">
                                                    <br><small style="color:#fca5a5;"><c:out value="${uf.extractionWarning}"/></small>
                                                </c:if>
                                            </td>
                                            <td>
                                                <c:if test="${not empty uf.documentId}">
                                                    <a class="kb-btn kb-btn-secondary" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents&amp;docId=${uf.documentId}">Tài liệu #<c:out value="${uf.documentId}"/></a>
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

    <!-- ===================================================================
         TAB 4: FAQ MANAGEMENT MODULE
         =================================================================== -->
    <c:if test="${kbTab == 'faqs'}">
        <div class="kb-grid-2">
            <div class="kb-panel">
                <h3>Danh sách Câu hỏi thường gặp (FAQ)</h3>
                <div class="kb-table-wrap">
                    <table class="kb-table">
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
                                        <tr>
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
                            <label>Độ ưu tiên (1 = cao nhất)</label>
                            <input type="number" name="priority" min="1" max="1000" value="${not empty selectedFaq ? selectedFaq.priority : 10}">
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

                    <div class="kb-field" style="margin-bottom:14px;">
                        <label>Câu trả lời chuẩn hóa (Answer) *</label>
                        <textarea name="answer" rows="5" maxlength="20000" required><c:out value="${not empty selectedFaq ? selectedFaq.answer : ''}"/></textarea>
                    </div>

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
    </c:if>

    <!-- ===================================================================
         TAB 5: CHUNKING & OFFLINE INDEXING PIPELINE
         =================================================================== -->
    <c:if test="${kbTab == 'indexing'}">
        <jsp:include page="/view/admin/knowledge-gemini-runtime.jsp"/>
        <div class="kb-panel">
            <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;">
                <div>
                    <h3>Điều khiển Lập chỉ mục Offline (MockEmbeddingProvider — 768 dimensions)</h3>
                    <p style="font-size:13px;color:#94a3b8;margin:0;">Tất cả embedding trong chế độ phát triển/kiểm thử được tạo định danh 100% cục bộ, không gọi mạng tới Gemini API.</p>
                </div>
                <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
                    <input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
                    <input type="hidden" name="action" value="runOfflineIndexingBatch">
                    <button type="submit" class="kb-btn">Chạy Batch Lập chỉ mục Offline ngay</button>
                </form>
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
                                                    <input type="hidden" name="action" value="reindexDocument">
                                                    <input type="hidden" name="id" value="${sd.id}">
                                                    <button type="submit" class="kb-btn">Reindex</button>
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
    </c:if>

    <!-- ===================================================================
         TAB 6: ADMIN RAG CONFIGURATION
         =================================================================== -->
    <c:if test="${kbTab == 'rag-config'}">
        <div class="kb-panel">
            <h3>Cấu hình RAG &amp; Kiểm soát Truy hồi</h3>
            <p style="font-size:13px;color:#94a3b8;">Tách biệt rõ ràng giữa Local Offline Indexing, Staging RAG và Production Customer RAG. <strong>Production Customer RAG luôn bị khóa cứng ở trạng thái <code>false</code> tại máy chủ.</strong></p>

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
                        <label>Production Customer RAG (Hard-Locked OFF)</label>
                        <select name="productionRagEnabled">
                            <option value="false" selected>KHÓA AN TOÀN (false)</option>
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
            <h3>Nhật ký Kiểm toán Quản trị (Admin Audit Logs — Không lưu API Key / Vector)</h3>
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
                                            <small style="color:#94a3b8;">Trước: <code><c:out value="${log.previousState}"/></code></small><br>
                                            <small style="color:#6ee7b7;">Sau: <code><c:out value="${log.newState}"/></code></small>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </c:if>

    <!-- ===================================================================
         TAB 8: ADMIN RAG TEST PLAYGROUND (OFFLINE ONLY)
         =================================================================== -->
    <c:if test="${kbTab == 'playground'}">
        <jsp:include page="/view/admin/knowledge-gemini-runtime.jsp"/>
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

                    <h4>Ngữ cảnh JSON chuyển cho bộ trích dẫn (RagContextBuilder)</h4>
                    <div class="kb-diff-box"><c:out value="${playgroundResult.constructedContextJson}"/></div>
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
    </c:if>
</main>
</body>
</html>
