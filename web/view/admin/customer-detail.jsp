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
<jsp:include page="/view/loading-runner.jsp" />
<!-- ============ HEADER (DÙNG CHUNG) ============ -->
<jsp:include page="/view/admin/admin-header.jsp">
    <jsp:param name="activeTab" value="customers"/>
</jsp:include>

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
                <label>Địa chỉ:</label>
                <span>${not empty customer.address ? customer.address : '—'}</span>
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