<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sửa khách hàng - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
<link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
</head>
<body>

<!-- ============ HEADER ============ -->
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
        <a href="${pageContext.request.contextPath}/admin/customers" class="active">
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

<!-- ============ MAIN ============ -->
<main class="admin-main">

    <div class="page-header-flex">
        <div>
            <a href="${pageContext.request.contextPath}/admin/customers" class="back-link">
                ← Quay lại danh sách
            </a>
            <h1>Chỉnh sửa khách hàng</h1>
            <p>#KH-${customer.id} • Cập nhật thông tin chi tiết</p>
        </div>
    </div>

    <!-- FLASH MESSAGE -->
    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <!-- FORM EDIT -->
    <form method="post" action="${pageContext.request.contextPath}/admin/customers"
          class="edit-form" id="editForm">

        <input type="hidden" name="action" value="update">
        <input type="hidden" name="id" value="${customer.id}">

        <div class="edit-grid">

            <!-- CỘT TRÁI: THÔNG TIN CHÍNH -->
            <div class="edit-card">
                <div class="edit-card-header">
                    <h3>Thông tin khách hàng</h3>
                </div>

                <div class="form-group">
                    <label>Họ và tên <span class="req">*</span></label>
                    <input type="text" name="fullName" id="fullName"
                           value="${customer.fullName}" required
                           placeholder="VD: Nguyễn Văn An">
                </div>

                <div class="form-group">
                    <label>Số điện thoại <span class="req">*</span></label>
                    <input type="tel" name="phone" id="phone"
                           value="${customer.phone}" required
                           placeholder="0912 345 678">
                </div>

                <div class="form-group">
                    <label>Gmail khách hàng</label>
                    <input type="email" name="email" id="email"
                           value="${customer.email}"
                           placeholder="khachhang@gmail.com">
                    <small class="field-hint">📧 Dùng để gửi thông báo và hóa đơn điện tử</small>
                </div>

                <div class="form-group">
                    <label>Địa chỉ lắp đặt</label>
                    <input type="text" name="address" id="address"
                           value="${customer.address}"
                           placeholder="Ví dụ: 123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. HCM">
                </div>

                <div class="form-group">
                    <label>Ghi chú nội bộ</label>
                    <textarea name="note" id="note" rows="4"
                              placeholder="VD: Khách gọi buổi tối, quan tâm camera...">${customer.note}</textarea>
                </div>
            </div>

            <!-- CỘT PHẢI: TRẠNG THÁI + THÔNG TIN PHỤ -->
            <div class="edit-card">
                <div class="edit-card-header">
                    <h3>Trạng thái & Thông tin hệ thống</h3>
                </div>

                <div class="form-group">
                    <label>Trạng thái xử lý <span class="req">*</span></label>
                    <div class="status-options">
                        <label class="status-option ${customer.status == 'Mới' ? 'active' : ''}">
                            <input type="radio" name="status" value="Mới"
                                   ${customer.status == 'Mới' ? 'checked' : ''}>
                            <span class="status-dot dot-new"></span>
                            <span>Mới</span>
                        </label>
                        <label class="status-option ${customer.status == 'Đã liên hệ' ? 'active' : ''}">
                            <input type="radio" name="status" value="Đã liên hệ"
                                   ${customer.status == 'Đã liên hệ' ? 'checked' : ''}>
                            <span class="status-dot dot-contacted"></span>
                            <span>Đã liên hệ</span>
                        </label>
                        <label class="status-option ${customer.status == 'Đã ký HĐ' ? 'active' : ''}">
                            <input type="radio" name="status" value="Đã ký HĐ"
                                   ${customer.status == 'Đã ký HĐ' ? 'checked' : ''}>
                            <span class="status-dot dot-signed"></span>
                            <span>Đã ký HĐ</span>
                        </label>
                        <label class="status-option ${customer.status == 'Hủy' ? 'active' : ''}">
                            <input type="radio" name="status" value="Hủy"
                                   ${customer.status == 'Hủy' ? 'checked' : ''}>
                            <span class="status-dot dot-cancelled"></span>
                            <span>Hủy</span>
                        </label>
                    </div>
                </div>

                <div class="info-readonly">
                    <div class="info-row">
                        <span class="info-label">Mã khách hàng:</span>
                        <span class="info-value">#KH-${customer.id}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Ngày đăng ký:</span>
                        <span class="info-value">
                            <fmt:formatDate value="${customer.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                        </span>
                    </div>
                    <c:if test="${not empty customer.consultantName}">
                        <div class="info-row">
                            <span class="info-label">Tư vấn viên:</span>
                            <span class="info-value">${customer.consultantName}</span>
                        </div>
                    </c:if>
                </div>

                <div class="warning-box">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="8" x2="12" y2="12"/>
                        <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    <div>
                        <strong>Lưu ý:</strong> Thay đổi sẽ được lưu ngay vào database.
                        Vui lòng kiểm tra kỹ trước khi lưu.
                    </div>
                </div>
            </div>

        </div>

        <!-- ACTION BUTTONS -->
        <div class="form-actions">
            <a href="${pageContext.request.contextPath}/admin/customer-detail?id=${customer.id}"
               class="btn-secondary">
                Hủy bỏ
            </a>
            <a href="${pageContext.request.contextPath}/admin/customers?action=delete&id=${customer.id}"
               class="btn-danger"
               onclick="return confirm('Bạn có chắc muốn xóa khách hàng ${customer.fullName}?');">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
                Xóa khách hàng
            </a>
            <button type="submit" class="btn-primary-lg">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                    <polyline points="17 21 17 13 7 13 7 21"/>
                    <polyline points="7 3 7 8 15 8"/>
                </svg>
                Lưu thay đổi
            </button>
        </div>

    </form>

</main>

<script>
    // Highlight status option đang chọn
    document.querySelectorAll('input[name="status"]').forEach(radio => {
        radio.addEventListener('change', function() {
            document.querySelectorAll('.status-option').forEach(opt => {
                opt.classList.remove('active');
            });
            this.closest('.status-option').classList.add('active');
        });
    });

    // Confirm trước khi submit
    document.getElementById('editForm').addEventListener('submit', function(e) {
        const fullName = document.getElementById('fullName').value.trim();
        const phone = document.getElementById('phone').value.trim();

        if (!fullName || !phone) {
            e.preventDefault();
            alert('Vui lòng nhập đầy đủ Họ tên và Số điện thoại!');
            return false;
        }

        // Validate SĐT: 10-11 số
        const phoneRegex = /^[0-9]{10,11}$/;
        if (!phoneRegex.test(phone.replace(/\s+/g, ''))) {
            e.preventDefault();
            alert('Số điện thoại không hợp lệ! Vui lòng nhập 10-11 chữ số.');
            return false;
        }
    });
</script>

</body>
</html>