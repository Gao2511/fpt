<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản Lý Tài Khoản - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-user.css">
</head>
<body>

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
        <a href="${pageContext.request.contextPath}/admin/users" class="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
            </svg>
            Tài khoản
        </a>
        <a href="${pageContext.request.contextPath}/admin/email-logs">
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

<!-- MAIN -->
<main class="admin-main">

    <div class="page-header-flex">
        <div>
            <h1>Quản Lý Tài Khoản</h1>
            <p>Xem, tìm kiếm, khóa, xóa và đổi mật khẩu tài khoản người dùng</p>
        </div>
    </div>

    <!-- FLASH -->
    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <!-- THỐNG KÊ -->
    <div class="quick-stats">
        <div class="qs-item">
            <span class="qs-label">Tổng tài khoản:</span>
            <strong class="qs-value">${countAll}</strong>
        </div>
        <div class="qs-divider"></div>
        <div class="qs-item">
            <span class="qs-dot dot-signed"></span>
            <span class="qs-label">Hoạt động:</span>
            <strong class="qs-value" style="color: #16a34a;">${countActive}</strong>
        </div>
        <div class="qs-divider"></div>
        <div class="qs-item">
            <span class="qs-dot dot-cancelled"></span>
            <span class="qs-label">Đã khóa:</span>
            <strong class="qs-value" style="color: #dc2626;">${countLocked}</strong>
        </div>
    </div>

    <!-- FILTER -->
    <form method="get" action="${pageContext.request.contextPath}/admin/users" class="filter-bar">
        <div class="filter-row">
            <div class="filter-search">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input type="text" name="keyword"
                       placeholder="Tìm theo ID, username, họ tên, email, SĐT..."
                       value="${keyword}">
            </div>

            <select name="status" class="filter-select">
                <option value="">Tất cả trạng thái</option>
                <option value="active" ${status == 'active' ? 'selected' : ''}>✓ Hoạt động</option>
                <option value="locked" ${status == 'locked' ? 'selected' : ''}>🔒 Đã khóa</option>
            </select>

            <button type="submit" class="btn-filter">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
                </svg>
                Lọc
            </button>

            <a href="${pageContext.request.contextPath}/admin/users" class="btn-reset" title="Xóa bộ lọc">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="1 4 1 10 7 10"/>
                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
                </svg>
            </a>
        </div>
    </form>

    <!-- TABLE -->
    <div class="table-wrapper">
<div class="table-header">
    <div class="table-title">
        Danh sách tài khoản
        <span class="table-badge">Trang ${currentPage} / ${totalPages}</span>
    </div>
</div>

        <table class="customer-table">
            <thead>
                <tr>
                    <th style="width: 60px;">ID</th>
                    <th style="width: 200px;">TÀI KHOẢN</th>
                    <th>THÔNG TIN</th>
                    <th style="width: 180px;">LIÊN HỆ</th>
                    <th style="width: 130px;">TRẠNG THÁI</th>
                    <th style="width: 130px;">NGÀY TẠO</th>
                    <th style="width: 220px;">THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${users}" var="u">
                    <tr>
                        <td class="col-id">#${u.id}</td>
                        <td>
                            <div class="user-cell">
                                <div class="user-avatar-small">
                                    <c:choose>
                                        <c:when test="${not empty u.avatarUrl}">
                                            <img src="${pageContext.request.contextPath}/${u.avatarUrl}" alt="Avatar">
                                        </c:when>
                                        <c:otherwise>
                                            <span>${u.initial}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="user-cell-info">
                                    <strong>${u.username}</strong>
                                    <small>
                                        <c:choose>
                                            <c:when test="${u.admin}">
                                                <span class="role-tag role-admin">
                                                    <svg class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 21 12 17.77 5.82 21 7 14.14l-5-4.87 6.91-1.01L12 2z"/>
                                                    </svg>
                                                    Admin
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="role-tag role-customer">
                                                    <svg class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                                    <circle cx="12" cy="7" r="4"/>
                                                    </svg>
                                                    Khách hàng
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </small>
                                </div>
                            </div>
                        </td>
                        <td>
                            <div class="customer-info">
                                <strong>${u.displayName}</strong>
                                <c:if test="${not empty u.fullName and u.fullName != u.username}">
                                    <span class="phone">${u.fullName}</span>
                                </c:if>
                            </div>
                        </td>
                        <td class="col-email">
                            <div class="contact-mini">
                                <c:if test="${not empty u.phone}">
                                    <div class="contact-line">
                                        <svg class="contact-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                                        </svg>
                                        <span>${u.phone}</span>
                                    </div>
                                </c:if>
                                <c:if test="${not empty u.email}">
                                    <div class="contact-line">
                                        <svg class="contact-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                        <polyline points="22,6 12,13 2,6"/>
                                        </svg>
                                        <span>${u.email}</span>
                                    </div>
                                </c:if>
                                <c:if test="${empty u.phone && empty u.email}">
                                    <span style="color: #94a3b8;">—</span>
                                </c:if>
                            </div>
                        </td>
                        <td>
                            <span class="status-badge status-${u.statusClass}">
                                <c:choose>
                                    <c:when test="${u.active}">
                                        <svg class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                        <polyline points="20 6 9 17 4 12"/>
                                        </svg>
                                        Hoạt động
                                    </c:when>
                                    <c:otherwise>
                                        <svg class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                        </svg>
                                        Đã khóa
                                    </c:otherwise>
                                </c:choose>
                            </span>
                        </td>
                        <td class="col-time">
                            <fmt:formatDate value="${u.createdAt}" pattern="dd/MM/yyyy"/>
                            <small>
                                <fmt:formatDate value="${u.createdAt}" pattern="HH:mm"/>
                            </small>
                        </td>
                        <td class="col-actions">
                            <%-- Đổi mật khẩu --%>
                            <a href="${pageContext.request.contextPath}/admin/users?action=change-password&id=${u.id}"
                               class="action-btn btn-password" title="Đổi mật khẩu">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                </svg>
                            </a>

                            <%-- Khóa / Mở --%>
                            <c:choose>
                                <c:when test="${u.active}">
                                    <a href="${pageContext.request.contextPath}/admin/users?action=lock&id=${u.id}"
                                       class="action-btn btn-lock" title="Khóa tài khoản"
                                       onclick="return confirm('Bạn có chắc muốn khóa tài khoản ${u.username}?');">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                        </svg>
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <a href="${pageContext.request.contextPath}/admin/users?action=unlock&id=${u.id}"
                                       class="action-btn btn-unlock" title="Mở lại tài khoản"
                                       onclick="return confirm('Bạn có chắc muốn mở lại tài khoản ${u.username}?');">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                            <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
                                        </svg>
                                    </a>
                                </c:otherwise>
                            </c:choose>

                            <%-- Xóa --%>
                            <a href="${pageContext.request.contextPath}/admin/users?action=delete&id=${u.id}"
                               class="action-btn btn-delete" title="Xóa tài khoản"
                               onclick="return confirm('Bạn có chắc muốn xóa tài khoản ${u.username}? Hành động này KHÔNG thể hoàn tác!');">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="3 6 5 6 21 6"/>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                                </svg>
                            </a>
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${empty users}">
                    <tr>
                        <td colspan="7" class="empty-row">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:48px;height:48px;opacity:0.3;">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                <circle cx="12" cy="7" r="4"/>
                            </svg>
                            <p>Không tìm thấy tài khoản nào</p>
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <!-- PHÂN TRANG -->
        <div class="table-footer">
            <div class="footer-info">
                Hiển thị
                <strong>${(currentPage - 1) * pageSize + 1} - ${currentPage * pageSize > totalRecords ? totalRecords : currentPage * pageSize}</strong>
                trên tổng số <strong>${totalRecords}</strong> tài khoản
            </div>

            <div class="pagination">
                <c:if test="${currentPage > 1}">
                    <a href="?page=${currentPage - 1}&keyword=${keyword}&status=${status}" class="page-btn">‹ Trước</a>
                </c:if>

                <c:forEach begin="1" end="${totalPages}" var="i">
                    <c:choose>
                        <c:when test="${i == currentPage}">
                            <span class="page-btn page-active">${i}</span>
                        </c:when>
                        <c:when test="${i <= 3 || i > totalPages - 3 || (i >= currentPage - 1 && i <= currentPage + 1)}">
                            <a href="?page=${i}&keyword=${keyword}&status=${status}" class="page-btn">${i}</a>
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
                    <a href="?page=${currentPage + 1}&keyword=${keyword}&status=${status}" class="page-btn">Sau ›</a>
                </c:if>
            </div>
        </div>
    </div>

</main>

</body>
</html>