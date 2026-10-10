<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản Lý Khách Hàng - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />
<!-- ============ HEADER (DÙNG CHUNG) ============ -->
<jsp:include page="/view/admin/admin-header.jsp">
    <jsp:param name="activeTab" value="customers"/>
</jsp:include>

<!-- ============ MAIN ============ -->
<main class="admin-main">

    <!-- TIÊU ĐỀ + NÚT THÊM -->
    <div class="page-header-flex">
        <div>
            <h1>Quản Lý Danh Sách Khách Hàng</h1>
            <p>Xem, tìm kiếm, lọc và quản lý toàn bộ khách hàng đăng ký tư vấn</p>
        </div>
    </div>

    <!-- FLASH MESSAGE -->
    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <!-- ============ FILTER BAR ============ -->
    <form method="get" action="${pageContext.request.contextPath}/admin/customers" class="filter-bar">
        <div class="filter-row">
            <div class="filter-search">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input type="text" name="keyword" placeholder="Tìm theo Họ tên, SĐT, Gmail hoặc Mã (#KH-1024)..."
                       value="${keyword}">
            </div>

            <select name="status" class="filter-select">
                <option value="">Tất cả trạng thái</option>
                <option value="Mới" ${status == 'Mới' ? 'selected' : ''}>Mới</option>
                <option value="Đã liên hệ" ${status == 'Đã liên hệ' ? 'selected' : ''}>Đã liên hệ</option>
                <option value="Đã ký HĐ" ${status == 'Đã ký HĐ' ? 'selected' : ''}>Đã ký HĐ</option>
                <option value="Hủy" ${status == 'Hủy' ? 'selected' : ''}>Hủy</option>
            </select>

            <div class="filter-date">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="4" width="18" height="18" rx="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                <input type="text" name="dateRange" id="dateRange"
                       placeholder="01/03 - 31/03/2025"
                       value="${dateRange}"
                       onfocus="this.type='text'">
            </div>

            <button type="submit" class="btn-filter">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
                </svg>
                Lọc dữ liệu
            </button>

            <a href="${pageContext.request.contextPath}/admin/customers" class="btn-reset" title="Xóa bộ lọc">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="1 4 1 10 7 10"/>
                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
                </svg>
            </a>
        </div>
    </form>

    <!-- ============ THỐNG KÊ NHANH ============ -->
    <div class="quick-stats">
        <div class="qs-item">
            <span class="qs-label">Tổng hồ sơ:</span>
            <strong class="qs-value">${countAll}</strong>
        </div>
        <div class="qs-item">
            <span class="qs-label">Mới:</span>
            <strong class="qs-value" style="color:#fb923c;">${not empty countNew ? countNew : 0}</strong>
        </div>
        <div class="qs-item">
            <span class="qs-label">Đã liên hệ:</span>
            <strong class="qs-value" style="color:#38bdf8;">${not empty countContacted ? countContacted : 0}</strong>
        </div>
        <div class="qs-item">
            <span class="qs-label">Đã ký HĐ:</span>
            <strong class="qs-value" style="color:#4ade80;">${not empty countSigned ? countSigned : 0}</strong>
        </div>
        <div class="qs-item">
            <span class="qs-label">Đã hủy:</span>
            <strong class="qs-value" style="color:#f87171;">${not empty countCancelled ? countCancelled : 0}</strong>
        </div>
    </div>

    <!-- ============ BẢNG KHÁCH HÀNG ============ -->
    <div class="table-wrapper">
        <div class="table-header">
            <div class="table-title">
                Danh sách chi tiết hồ sơ khách hàng
                <span class="table-badge">Trang ${currentPage} / ${totalPages}</span>
            </div>
        </div>

        <form method="post" action="${pageContext.request.contextPath}/admin/customers" id="bulkForm"><input type="hidden" name="csrf_token" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="action" value="deleteMultiple">

            <table class="customer-table">
                <thead>
                    <tr>
                        <th class="col-check">
                            <input type="checkbox" id="checkAll" onclick="toggleAll(this)">
                        </th>
                        <th>MÃ KH</th>
                        <th>KHÁCH HÀNG</th>
                        <th>GMAIL</th>
                        <th>GÓI QUAN TÂM</th>
                        <th>TRẠNG THÁI</th>
                        <th>THỜI GIAN</th>
                        <th>THAO TÁC</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${customers}" var="c">
                        <tr>
                            <td class="col-check">
                                <input type="checkbox" name="ids" value="${fn:escapeXml(c.id)}" class="rowCheck">
                            </td>
                            <td class="col-id">#KH-${c.id}</td>
                            <td>
                                <div class="customer-info">
                                    <strong>${fn:escapeXml(c.fullName)}</strong>
                                    <span class="phone">${fn:escapeXml(c.phone)}</span>
                                    <c:if test="${not empty c.address}">
                                        <small style="color:#64748b;font-size:12px;display:block;" title="${fn:escapeXml(c.address)}">📍 ${fn:escapeXml(c.address)}</small>
                                    </c:if>
                                </div>
                            </td>
                            <td class="col-email">
                                <svg class="email-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                <polyline points="22,6 12,13 2,6"/>
                                </svg>
                                ${fn:escapeXml(c.email)}
                            </td>
                            <td class="col-package">
                                <c:choose>
                                    <c:when test="${not empty c.packageInterest}">
                                        <span class="pkg-interest">
                                            <svg class="pkg-interest-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                                                <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                                                <line x1="12" y1="22.08" x2="12" y2="12"/>
                                            </svg>
                                            ${fn:escapeXml(c.packageInterest)}
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span style="color: #64748b;">—</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <span class="status-badge status-${c.status}">
                                    ${c.status}
                                </span>
                            </td>
                            <td class="col-time">
                                <fmt:formatDate value="${c.createdAt}" pattern="HH:mm"/>
                                <small>
                                    <c:choose>
                                        <c:when test="${c.status == 'Mới'}">Hôm nay</c:when>
                                        <c:otherwise>
                                            <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy"/>
                                        </c:otherwise>
                                    </c:choose>
                                </small>
                            </td>
                            <td class="col-actions">
                                <a href="${pageContext.request.contextPath}/admin/customer-detail?id=${c.id}"
                                   class="action-btn btn-view" title="Xem chi tiết">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                        <circle cx="12" cy="12" r="3"/>
                                    </svg>
                                </a>
                                <a href="${pageContext.request.contextPath}/admin/customers?action=edit&id=${c.id}"
                                   class="action-btn btn-edit" title="Chỉnh sửa">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                                    </svg>
                                </a>
                                <a href="${pageContext.request.contextPath}/admin/customers?action=delete&id=${c.id}"
                                   class="action-btn btn-delete" title="Xóa"
                                   onclick="return confirm('Xác nhận thao tác này?');">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="3 6 5 6 21 6"/>
                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                                    </svg>
                                </a>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty customers}">
                        <tr>
                            <td colspan="8" class="empty-row">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:48px;height:48px;opacity:0.3;">
                                    <circle cx="11" cy="11" r="8"/>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                                </svg>
                                <p>Không tìm thấy khách hàng nào</p>
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </form>

        <!-- ============ BULK ACTION BAR ============ -->
        <div id="bulkBar" class="bulk-bar" style="display:none;">
            <span>Đã chọn <strong id="selectedCount">0</strong> khách hàng</span>
            <div class="bulk-actions">
                <button type="button" class="btn-bulk-cancel" onclick="clearSelection()">✕ Bỏ chọn</button>
                <button type="button" class="btn-bulk-delete" onclick="deleteSelected()">🗑 Xóa đã chọn</button>
            </div>
        </div>

        <!-- ============ PHÂN TRANG ============ -->
        <div class="table-footer">
            <div class="footer-info">
                Hiển thị
                <strong>${(currentPage - 1) * pageSize + 1} - ${currentPage * pageSize > totalRecords ? totalRecords : currentPage * pageSize}</strong>
                trên tổng số <strong>${totalRecords}</strong> khách hàng
            </div>

            <div class="pagination">
                <c:if test="${currentPage > 1}">
                    <a href="?page=${currentPage - 1}&keyword=${keyword}&status=${status}&dateRange=${dateRange}"
                       class="page-btn">
                        ‹ Trước
                    </a>
                </c:if>

                <c:forEach begin="1" end="${totalPages}" var="i">
                    <c:choose>
                        <c:when test="${i == currentPage}">
                            <span class="page-btn page-active">${i}</span>
                        </c:when>
                        <c:when test="${i <= 3 || i > totalPages - 3 || (i >= currentPage - 1 && i <= currentPage + 1)}">
                            <a href="?page=${i}&keyword=${keyword}&status=${status}&dateRange=${dateRange}"
                               class="page-btn">${i}</a>
                        </c:when>
                        <c:when test="${i == 4 && currentPage > 4}">
                            <span class="page-dots">...</span>
                        </c:when>
                        <c:when test="${i == totalPages - 3 && currentPage < totalPages - 3}">
                            <span class="page-dots">...</span>
                        </c:when>
                    </c:choose>
                </c:forEach>

                <c:if test="${currentPage < totalPages}">
                    <a href="?page=${currentPage + 1}&keyword=${keyword}&status=${status}&dateRange=${dateRange}"
                       class="page-btn">
                        Sau ›
                    </a>
                </c:if>
            </div>
        </div>
    </div>

</main>

<!-- ============ JAVASCRIPT ============ -->
<script>
    // ===== CHỌN TẤT CẢ =====
    function toggleAll(master) {
        const checks = document.querySelectorAll('.rowCheck');
        checks.forEach(c => c.checked = master.checked);
        updateBulkBar();
    }

    // ===== CẬP NHẬT THANH BULK =====
    function updateBulkBar() {
        const checked = document.querySelectorAll('.rowCheck:checked');
        const bulkBar = document.getElementById('bulkBar');
        const count = document.getElementById('selectedCount');

        if (checked.length > 0) {
            bulkBar.style.display = 'flex';
            count.textContent = checked.length;
        } else {
            bulkBar.style.display = 'none';
        }
    }

    // ===== LẮNG NGHE CHECKBOX =====
    document.querySelectorAll('.rowCheck').forEach(cb => {
        cb.addEventListener('change', updateBulkBar);
    });

    // ===== BỎ CHỌN =====
    function clearSelection() {
        document.querySelectorAll('.rowCheck').forEach(c => c.checked = false);
        document.getElementById('checkAll').checked = false;
        updateBulkBar();
    }

    // ===== XÓA NHIỀU =====
    function deleteSelected() {
        const checked = document.querySelectorAll('.rowCheck:checked');
        if (checked.length === 0) return;

        if (confirm('Bạn có chắc muốn xóa ' + checked.length + ' khách hàng đã chọn?')) {
            document.getElementById('bulkForm').submit();
        }
    }
</script>

</body>
</html>