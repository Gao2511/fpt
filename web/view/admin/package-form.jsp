<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${mode == 'edit' ? 'Sửa' : 'Thêm'} gói cước - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
<link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-package.css">
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />
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
        <a href="${pageContext.request.contextPath}/admin/customers">Khách hàng</a>
        <a href="${pageContext.request.contextPath}/admin/packages" class="active">Gói cước</a>
        <a href="${pageContext.request.contextPath}/admin/users">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
            </svg>
            Tài khoản
        </a>           
        <a href="${pageContext.request.contextPath}/admin/email-logs">Thông báo</a>
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
            <a href="${pageContext.request.contextPath}/logout" class="logout-btn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
            </a>
        </div>
    </div>
</header>

<main class="admin-main page-package-form">

    <div class="page-header-flex">
        <div>
            <a href="${pageContext.request.contextPath}/admin/packages" class="back-link">
                ← Quay lại danh sách
            </a>
            <h1>${mode == 'edit' ? 'Chỉnh sửa gói cước' : 'Thêm gói cước mới'}</h1>
            <p>${mode == 'edit' ? '#GÓI-' : ''}${mode == 'edit' ? pkg.id : 'Điền thông tin gói cước mới bên dưới'}</p>
        </div>
    </div>

    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/admin/packages"
          class="edit-form" id="pkgForm">

        <input type="hidden" name="action" value="${mode == 'edit' ? 'update' : 'insert'}">
        <c:if test="${mode == 'edit'}">
            <input type="hidden" name="id" value="${pkg.id}">
        </c:if>

        <div class="edit-grid">

            <!-- CỘT TRÁI -->
            <div class="edit-card">
                <div class="edit-card-header">
                    <h3>Thông tin gói cước</h3>
                </div>

                <div class="form-group">
                    <label>Mã gói <span class="req">*</span></label>
                    <input type="text" name="packageCode" id="packageCode"
                           value="${mode == 'edit' ? pkg.packageCode : ''}"
                           placeholder="VD: GÓI 195" required>
                    <small class="field-hint">Mã ngắn gọn, không dấu, viết hoa</small>
                </div>

                <div class="form-group">
                    <label>Tên gói cước <span class="req">*</span></label>
                    <input type="text" name="name" id="name"
                           value="${mode == 'edit' ? pkg.name : ''}"
                           placeholder="VD: Gói 195 - Internet" required>
                </div>

                <div class="form-group">
                    <label>Giá / tháng (VNĐ) <span class="req">*</span></label>
                    <input type="number" name="price" id="price"
                           value="${mode == 'edit' ? pkg.price : ''}"
                           placeholder="VD: 195000" min="0" step="1000" required>
                </div>

                <div class="form-group">
                    <label>Tốc độ (Mbps)</label>
                    <input type="number" name="speedMbps" id="speedMbps"
                           value="${mode == 'edit' ? pkg.speedMbps : ''}"
                           placeholder="VD: 300" min="0">
                </div>

                <div class="form-group">
                    <label>Mô tả ngắn (1 dòng)</label>
                    <textarea name="description" id="description" rows="2"
                              placeholder="VD: Internet 300Mbps - Phù hợp nhu cầu cơ bản">${mode == 'edit' ? pkg.description : ''}</textarea>
                </div>

                <div class="form-group">
                    <label>Mô tả dài (mỗi dòng = 1 gạch ✓ xanh)</label>
                    <textarea name="longDescription" id="longDescription" rows="8"
                              placeholder="WiFi 6 tốc độ cao 300Mbps
Phù hợp nhu cầu lướt web, học tập
Làm việc online ổn định">${mode == 'edit' ? pkg.longDescription : ''}</textarea>
                    <small class="field-hint">💡 Mỗi dòng sẽ hiển thị thành 1 gạch đầu dòng ✓ xanh trên trang chi tiết</small>
                </div>
            </div>

            <!-- CỘT PHẢI -->
            <div class="edit-card">
                <div class="edit-card-header">
                    <h3>Tuỳ chọn hiển thị</h3>
                </div>

                <div class="form-group">
                    <label>Phân loại gói cước</label>
                   <div class="badge-selector">
    <!-- Bình thường — SVG hình tròn -->
    <label class="badge-card ${empty pkg.badgeType ? 'active' : ''}">
        <input type="radio" name="badgeType" value=""
               ${empty pkg.badgeType ? 'checked' : ''}>
        <div class="badge-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="9"/>
            </svg>
        </div>
        <div class="badge-info">
            <strong>Bình thường</strong>
            <small>Không có nhãn đặc biệt</small>
        </div>
    </label>

    <!-- HOT — SVG ngọn lửa -->
    <label class="badge-card badge-card-hot ${pkg.badgeType == 'hot' ? 'active' : ''}">
        <input type="radio" name="badgeType" value="hot"
               ${pkg.badgeType == 'hot' ? 'checked' : ''}>
        <div class="badge-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
            </svg>
        </div>
        <div class="badge-info">
            <strong>HOT</strong>
            <small>Gói bán chạy nhất</small>
        </div>
    </label>

    <!-- NỔI BẬT — SVG ngôi sao -->
    <label class="badge-card badge-card-featured ${pkg.badgeType == 'featured' ? 'active' : ''}">
        <input type="radio" name="badgeType" value="featured"
               ${pkg.badgeType == 'featured' ? 'checked' : ''}>
        <div class="badge-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
        </div>
        <div class="badge-info">
            <strong>NỔI BẬT</strong>
            <small>Gói được đề xuất</small>
        </div>
    </label>
                   </div>
                </div>

                <div class="info-readonly">
                    <div class="info-row">
                        <span class="info-label">Chế độ:</span>
                        <span class="info-value">${mode == 'edit' ? 'Chỉnh sửa' : 'Thêm mới'}</span>
                    </div>
                    <c:if test="${mode == 'edit'}">
                        <div class="info-row">
                            <span class="info-label">ID gói:</span>
                            <span class="info-value">#${pkg.id}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Ngày tạo:</span>
                            <span class="info-value">${pkg.createdAt}</span>
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
                        <strong>Lưu ý:</strong> Thay đổi sẽ áp dụng ngay. Vui lòng kiểm tra kỹ trước khi lưu.
                    </div>
                </div>
            </div>
        </div>

        <div class="form-actions">
            <a href="${pageContext.request.contextPath}/admin/packages" class="btn-secondary">Hủy bỏ</a>

            <c:if test="${mode == 'edit'}">
                <a href="${pageContext.request.contextPath}/admin/packages?action=delete&id=${pkg.id}"
                   class="btn-danger"
                   onclick="return confirm('Bạn có chắc muốn xóa gói ${pkg.name}?');">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    </svg>
                    Xóa gói này
                </a>
            </c:if>

            <button type="submit" class="btn-primary-lg">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                    <polyline points="17 21 17 13 7 13 7 21"/>
                    <polyline points="7 3 7 8 15 8"/>
                </svg>
                ${mode == 'edit' ? 'Lưu thay đổi' : 'Thêm gói cước'}
            </button>
        </div>
    </form>

</main>

<script>
    // =========================================================
    // XỬ LÝ CHỌN BADGE CARD
    // =========================================================
    (function() {
        console.log('✅ Script badge-card đã load');
        
        const cards = document.querySelectorAll('.badge-card');
        console.log('📦 Tìm thấy', cards.length, 'badge-card');
        
        cards.forEach(function(card, index) {
            card.addEventListener('click', function(e) {
                console.log('🖱️ Click vào card #' + index);
                
                // 1. Bỏ active tất cả
                document.querySelectorAll('.badge-card').forEach(function(c) {
                    c.classList.remove('active');
                });
                
                // 2. Thêm active cho card vừa click
                this.classList.add('active');
                
                // 3. Đảm bảo radio bên trong được check
                const radio = this.querySelector('input[type="radio"]');
                if (radio) {
                    radio.checked = true;
                    console.log('✅ Đã chọn:', radio.value || '(Bình thường)');
                }
            });
        });
    })();

    // =========================================================
    // VALIDATE FORM KHI SUBMIT
    // =========================================================
    document.getElementById('pkgForm').addEventListener('submit', function(e) {
        const code = document.getElementById('packageCode').value.trim();
        const name = document.getElementById('name').value.trim();
        const price = document.getElementById('price').value.trim();

        // Log giá trị badgeType để debug
        const selectedBadge = document.querySelector('input[name="badgeType"]:checked');
        console.log('📤 Submit form với badgeType =', selectedBadge ? selectedBadge.value : 'KHÔNG CÓ');

        if (!code || !name || !price) {
            e.preventDefault();
            alert('Vui lòng nhập đầy đủ Mã gói, Tên gói và Giá!');
            return false;
        }

        if (parseInt(price) < 0) {
            e.preventDefault();
            alert('Giá không được âm!');
            return false;
        }
    });
</script>

</body>
</html>