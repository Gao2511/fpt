<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Chi tiết khách hàng - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
<link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
   <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
</head>
<body>

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
        <a href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
        <a href="${pageContext.request.contextPath}/admin/customers" class="active">Khách hàng</a>
        <a href="${pageContext.request.contextPath}/admin/packages">Gói cước</a>
        <a href="${pageContext.request.contextPath}/admin/email-logs">Thông báo</a>
        <a href="${pageContext.request.contextPath}/admin/users">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
        <circle cx="12" cy="7" r="4"/>
    </svg>
    Tài khoản
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
        <a href="${pageContext.request.contextPath}/logout" class="logout-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
        </a>
    </div>
</header>

<main class="admin-main">

    <div class="page-header-flex">
        <div>
            <a href="${pageContext.request.contextPath}/admin/customers" class="back-link">
                ← Quay lại danh sách
            </a>
            <h1>Chi tiết khách hàng</h1>
            <p>#KH-${customer.id} • ${customer.fullName}</p>
        </div>
    </div>

    <div class="detail-grid">

        <!-- CỘT TRÁI: THÔNG TIN KHÁCH HÀNG -->
        <div class="detail-card">
            <div class="detail-card-header">
                <h3>Thông tin khách hàng</h3>
                <span class="status-badge status-${customer.status}">${customer.status}</span>
            </div>

            <div class="detail-row">
                <label>Họ và tên:</label>
                <span><strong>${customer.fullName}</strong></span>
            </div>
            <div class="detail-row">
                <label>Số điện thoại:</label>
                <span><a href="tel:${customer.phone}" class="phone-link">${customer.phone}</a></span>
            </div>
            <div class="detail-row">
                <label>Gmail:</label>
                <span>
                    <a href="mailto:${customer.email}" class="phone-link">${customer.email}</a>
                </span>
            </div>
            <div class="detail-row">
                <label>Ngày đăng ký:</label>
                <span><fmt:formatDate value="${customer.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span>
            </div>
            <div class="detail-row">
                <label>Ghi chú:</label>
                <span>${customer.note != null ? customer.note : '—'}</span>
            </div>

            <!-- ĐỔI TRẠNG THÁI -->
            <div class="status-update">
                <label>Cập nhật trạng thái:</label>
                <form method="post" action="${pageContext.request.contextPath}/admin/customers" class="status-form">
                    <input type="hidden" name="action" value="updateStatus">
                    <input type="hidden" name="id" value="${customer.id}">
                    <select name="status" class="status-select">
                        <option value="Mới" ${customer.status == 'Mới' ? 'selected' : ''}>Mới</option>
                        <option value="Đã liên hệ" ${customer.status == 'Đã liên hệ' ? 'selected' : ''}>Đã liên hệ</option>
                        <option value="Đã ký HĐ" ${customer.status == 'Đã ký HĐ' ? 'selected' : ''}>Đã ký HĐ</option>
                        <option value="Hủy" ${customer.status == 'Hủy' ? 'selected' : ''}>Hủy</option>
                    </select>
                    <button type="submit" class="btn-primary">Cập nhật</button>
                </form>
            </div>
        </div>

        <!-- CỘT PHẢI: LỊCH SỬ EMAIL -->
        <div class="detail-card">
            <div class="detail-card-header">
                <h3>Lịch sử liên hệ</h3>
                <span class="badge-count">${emailLogs.size()} lần gửi</span>
            </div>

            <c:if test="${empty emailLogs}">
                <div class="empty-state">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:48px;height:48px;opacity:0.3;">
                        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                        <polyline points="22,6 12,13 2,6"/>
                        </svg>
                        <p>Chưa có lần liên hệ nào</p>
                </div>
            </c:if>

            <c:if test="${not empty emailLogs}">
                <div class="timeline">
                    <c:forEach items="${emailLogs}" var="log">
                        <div class="timeline-item ${log.status == 'Success' ? 'success' : 'failed'}">
                            <div class="timeline-dot ${log.status == 'Success' ? 'success' : 'failed'}"></div>
                            <div class="timeline-content">
                                <div class="timeline-title">
                                    <strong>${log.subject}</strong>
                                    <span class="timeline-status ${log.status == 'Success' ? 'text-success' : 'text-failed'}">
                                        ${log.status == 'Success' ? '✓ Thành công' : '✗ Thất bại'}
                                    </span>
                                </div>
                                <div class="timeline-meta">
                                    Gửi tới: ${log.recipientEmail}
                                    • <fmt:formatDate value="${log.sentAt}" pattern="dd/MM/yyyy HH:mm"/>
                                </div>
                                <c:if test="${not empty log.errorMessage}">
                                    <div class="timeline-error">Lỗi: ${log.errorMessage}</div>
                                </c:if>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>

    </div>

</main>

</body>
</html>