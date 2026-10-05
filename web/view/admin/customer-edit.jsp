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
<jsp:include page="/view/loading-runner.jsp" />
<!-- ============ HEADER (DÙNG CHUNG) ============ -->
<jsp:include page="/view/admin/admin-header.jsp">
    <jsp:param name="activeTab" value="customers"/>
</jsp:include>

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