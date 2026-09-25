<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông Báo & Lịch Sử Email - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-email.css">
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />
<!-- HEADER -->
<header class="admin-header">
    <div class="header-left">
        <div class="logo">
            <span class="logo-fpt">FPT</span>
            <div class="logo-text">
                <strong>Sale Manager</strong>
                <small>Bán quyền của bạn</small>
            </div>
        </div>
    </div>

    <nav class="header-nav">
        <a href="${pageContext.request.contextPath}/admin/dashboard">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"/>
                <rect x="14" y="3" width="7" height="7"/>
                <rect x="14" y="14" width="7" height="7"/>
                <rect x="3" y="14" width="7" height="7"/>
            </svg>
            Dashboard
        </a>
        <a href="${pageContext.request.contextPath}/admin/customers">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            Khách hàng
        </a>
        <a href="${pageContext.request.contextPath}/admin/packages">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                <line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
            Gói cước
        </a>
        <a href="${pageContext.request.contextPath}/admin/users">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
            </svg>
            Tài khoản
        </a>
        <a href="${pageContext.request.contextPath}/admin/email-logs" class="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
            </svg>
            Thông báo
        </a>
            <a href="${pageContext.request.contextPath}/admin/settings">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="3"/>
        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
    </svg>
    Cấu hình AI
</a>
    </nav>

    <div class="header-right">
        <span class="role-badge">Admin</span>
        <div class="user-info">
            <div class="avatar">AD</div>
            <div class="user-text">
                <strong>${sessionScope.user.fullName != null ? sessionScope.user.fullName : sessionScope.user.username}</strong>
                <small><span class="online-dot"></span> Online</small>
            </div>
            <a href="${pageContext.request.contextPath}/logout" class="logout-btn" title="Đăng xuất">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
            </a>
        </div>
    </div>
</header>

<main class="admin-main">

    <div class="page-header-flex">
        <div>
            <h1>Thông Báo & Lịch Sử Email</h1>
            <p>Xem toàn bộ lịch sử gửi email thông báo cho khách hàng</p>
        </div>
    </div>

    <!-- FLASH -->
    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <!-- ============ 4 STAT CARDS — CLICKABLE NHƯ DASHBOARD ============ -->
    <div class="email-stats">

        <!-- Card 1: Tổng email → clickable, hover XANH DƯƠNG -->
        <a href="${pageContext.request.contextPath}/admin/email-logs" class="email-stat-card stat-link-blue">
            <div class="esc-icon esc-blue">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                    <polyline points="22,6 12,13 2,6"/>
                </svg>
            </div>
            <div class="esc-info">
                <div class="esc-label">TỔNG EMAIL</div>
                <div class="esc-value">${totalAll}</div>
            </div>
        </a>

        <!-- Card 2: Thành công → clickable, hover XANH LÁ -->
        <a href="${pageContext.request.contextPath}/admin/email-logs?status=Success" class="email-stat-card stat-link-green">
            <div class="esc-icon esc-green">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"/>
                </svg>
            </div>
            <div class="esc-info">
                <div class="esc-label">THÀNH CÔNG</div>
                <div class="esc-value" style="color:#4ade80">${totalSuccess}</div>
            </div>
        </a>

        <!-- Card 3: Thất bại → clickable, hover ĐỎ -->
        <a href="${pageContext.request.contextPath}/admin/email-logs?status=Failed" class="email-stat-card stat-link-red">
            <div class="esc-icon esc-red">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
            </div>
            <div class="esc-info">
                <div class="esc-label">THẤT BẠI</div>
                <div class="esc-value" style="color:#f87171">${totalFailed}</div>
            </div>
        </a>

        <!-- Card 4: Tỷ lệ thành công → không clickable, hover CAM -->
        <div class="email-stat-card stat-link-orange">
            <div class="esc-icon esc-orange">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="20" x2="12" y2="10"/>
                    <line x1="18" y1="20" x2="18" y2="4"/>
                    <line x1="6" y1="20" x2="6" y2="16"/>
                </svg>
            </div>
            <div class="esc-info">
                <div class="esc-label">TỶ LỆ THÀNH CÔNG</div>
                <div class="esc-value" style="color:#fb923c">${successRate}%</div>
            </div>
        </div>

    </div>

    <!-- ============ FILTER ============ -->
    <form method="get" action="${pageContext.request.contextPath}/admin/email-logs" class="filter-bar">
        <div class="filter-row">
            <div class="filter-search">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input type="text" name="keyword" placeholder="Tìm theo tên khách, email, tiêu đề..."
                       value="${keyword}">
            </div>

            <select name="status" class="filter-select">
                <option value="">Tất cả trạng thái</option>
                <option value="Success" ${status == 'Success' ? 'selected' : ''}>✓ Thành công</option>
                <option value="Failed" ${status == 'Failed' ? 'selected' : ''}>✗ Thất bại</option>
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
                       value="${dateRange}">
            </div>

            <button type="submit" class="btn-filter">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
                </svg>
                Lọc
            </button>

            <a href="${pageContext.request.contextPath}/admin/email-logs" class="btn-reset" title="Xóa bộ lọc">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="1 4 1 10 7 10"/>
                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
                </svg>
            </a>
        </div>
    </form>

    <!-- ============ BẢNG LOG ============ -->
    <div class="table-wrapper">
        <div class="table-header">
            <div class="table-title">
                Lịch sử gửi email
                <span class="table-badge">Trang ${currentPage} / ${totalPages}</span>
            </div>
            <div class="table-actions">
                <c:if test="${totalAll > 0}">
                    <a href="${pageContext.request.contextPath}/admin/email-logs?action=deleteAll"
                       class="btn-clear-all"
                       onclick="return confirm('Bạn có chắc muốn xóa TOÀN BỘ ${totalAll} log email? Hành động này không thể hoàn tác!');">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                        </svg>
                        Xóa toàn bộ
                    </a>
                </c:if>
            </div>
        </div>

        <table class="customer-table email-table">
            <thead>
                <tr>
                    <th style="width:60px;">ID</th>
                    <th style="width:150px;">KHÁCH HÀNG</th>
                    <th>EMAIL NHẬN</th>
                    <th>TIÊU ĐỀ</th>
                    <th style="width:130px;">TRẠNG THÁI</th>
                    <th style="width:150px;">THỜI GIAN</th>
                    <th style="width:80px;">THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${logs}" var="log">
                    <tr>
                        <td class="col-id">#${log.id}</td>
                        <td>
                            <div class="customer-info">
                                <strong>${log.customerName != null ? log.customerName : 'N/A'}</strong>
                                <span class="phone">
                                    <c:if test="${log.customerId != null}">
                                        #KH-${log.customerId}
                                    </c:if>
                                </span>
                            </div>
                        </td>
                        <td class="col-email">
                            <div class="email-recipient-line">
                                <svg class="email-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                    <polyline points="22,6 12,13 2,6"/>
                                </svg>
                                <span class="email-recipient">${log.recipientEmail}</span>
                            </div>
                        </td>
                        <td>
                            <div class="email-subject-line">
                                <svg class="email-subject-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                                </svg>
                                <span class="email-subject">${log.subject}</span>
                            </div>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${log.status == 'Success'}">
                                    <span class="status-pill status-success">
                                        <svg class="pill-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                                            <polyline points="20 6 9 17 4 12"/>
                                        </svg>
                                        Thành công
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="status-pill status-failed">
                                        <svg class="pill-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                                            <line x1="18" y1="6" x2="6" y2="18"/>
                                            <line x1="6" y1="6" x2="18" y2="18"/>
                                        </svg>
                                        Thất bại
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="col-time">
                            <fmt:formatDate value="${log.sentAt}" pattern="HH:mm"/>
                            <small>
                                <fmt:formatDate value="${log.sentAt}" pattern="dd/MM/yyyy"/>
                            </small>
                        </td>
                        <td class="col-actions">
                            <a href="${pageContext.request.contextPath}/admin/email-logs?action=delete&id=${log.id}"
                               class="action-btn btn-delete" title="Xóa log"
                               onclick="return confirm('Bạn có chắc muốn xóa log #${log.id}?');">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="3 6 5 6 21 6"/>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                                </svg>
                            </a>
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${empty logs}">
                    <tr>
                        <td colspan="7" class="empty-row">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:48px;height:48px;opacity:0.3;">
                                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                <polyline points="22,6 12,13 2,6"/>
                            </svg>
                            <p>Chưa có email nào được gửi</p>
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <!-- PAGINATION -->
        <div class="table-footer">
            <div class="footer-info">
                Hiển thị
                <strong>${(currentPage - 1) * pageSize + 1} - ${currentPage * pageSize > totalRecords ? totalRecords : currentPage * pageSize}</strong>
                trên tổng số <strong>${totalRecords}</strong> log
            </div>

            <div class="pagination">
                <c:if test="${currentPage > 1}">
                    <a href="?page=${currentPage - 1}&keyword=${keyword}&status=${status}&dateRange=${dateRange}"
                       class="page-btn">‹ Trước</a>
                </c:if>

                <c:forEach begin="1" end="${totalPages}" var="i">
                    <c:choose>
                        <c:when test="${i == currentPage}">
                            <span class="page-btn page-active">${i}</span>
                        </c:when>
                        <c:otherwise>
                            <a href="?page=${i}&keyword=${keyword}&status=${status}&dateRange=${dateRange}"
                               class="page-btn">${i}</a>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>

                <c:if test="${currentPage < totalPages}">
                    <a href="?page=${currentPage + 1}&keyword=${keyword}&status=${status}&dateRange=${dateRange}"
                       class="page-btn">Sau ›</a>
                </c:if>
            </div>
        </div>
    </div>

</main>

</body>
</html>