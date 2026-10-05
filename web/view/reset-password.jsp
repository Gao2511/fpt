<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu — FPT Telecom</title>

    <!-- Favicon -->
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">

    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@300;400;500;600&family=Be+Vietnam+Pro:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">

    <!-- Stylesheet chính (Glassmorphism Dark) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/login-glass.css">
</head>
<body class="login-body">

    <!-- 1. HIỆU ỨNG NỀN GRADIENT ĐỘNG (Dùng chung) -->
    <jsp:include page="/view/auth-bg.jsp" />

    <!-- 2. THANH ĐIỀU HƯỚNG TRÊN CÙNG: NÚT QUAY LẠI & LOGO FPT (Dùng chung) -->
    <jsp:include page="/view/auth-header.jsp" />

    <!-- 3. KHUNG FORM ĐẶT LẠI MẬT KHẨU -->
    <main class="login-main">
        <div class="login-wrap">
            <form id="resetForm" class="login-form" method="POST" action="${pageContext.request.contextPath}/reset-password">
                <input type="hidden" name="token" value="${token}">

                <!-- 3.1. Tiêu đề trang -->
                <h1 class="login-title blur-fade" data-delay="1">New Password</h1>
                <p class="login-sub blur-fade" data-delay="2">Nhập mật khẩu mới cho tài khoản FPT ID</p>

                <!-- 3.2. Bảng thông báo lỗi -->
                <c:if test="${not empty error}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="3">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>${error}</span>
                    </div>
                </c:if>

                <!-- 3.3. Ô nhập: Mật khẩu mới -->
                <div class="glass-input-wrap blur-fade" data-delay="4" style="max-width: 100%; position: relative;">
                    <div class="glass-input-icon">
                        <svg viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    </div>
                    <input type="password" id="inputNewPassword" name="newPassword" class="glass-input"
                           placeholder="Mật khẩu mới (tối thiểu 8 ký tự)" required>
                    <button type="button" class="glass-toggle-pass" data-target="inputNewPassword" aria-label="Toggle Password">
                        <svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    </button>
                </div>

                <!-- 3.4. Ô nhập: Xác nhận mật khẩu mới -->
                <div class="glass-input-wrap blur-fade" data-delay="5" style="max-width: 100%; position: relative;">
                    <div class="glass-input-icon">
                        <svg viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    </div>
                    <input type="password" id="inputConfirmPassword" name="confirmPassword" class="glass-input"
                           placeholder="Xác nhận lại mật khẩu mới" required>
                    <button type="button" class="glass-toggle-pass" data-target="inputConfirmPassword" aria-label="Toggle Confirm Password">
                        <svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    </button>
                </div>

                <!-- 3.5. Nút submit đổi mật khẩu -->
                <button type="submit" class="glass-btn-primary blur-fade" data-delay="6">
                    ĐẶT LẠI MẬT KHẨU
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"/>
                    </svg>
                </button>

                <!-- 3.6. Quay lại Đăng nhập -->
                <p class="login-sub blur-fade" data-delay="7" style="margin-top: 15px;">
                    <a href="${pageContext.request.contextPath}/login" style="display: inline-flex; align-items: center; gap: 6px;">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="19" y1="12" x2="5" y2="12"/>
                            <polyline points="12 19 5 12 12 5"/>
                        </svg>
                        <span>Quay lại đăng nhập</span>
                    </a>
                </p>

            </form>
        </div>
    </main>

    <!-- 4. MODAL THÔNG BÁO TIẾN TRÌNH / LỖI (Dùng chung) -->
    <jsp:include page="/view/auth-modal.jsp" />

    <!-- 5. SCRIPT XỬ LÝ -->
    <script src="${pageContext.request.contextPath}/js/login-glass.js"></script>
    <script>
        document.getElementById('resetForm')?.addEventListener('submit', function (e) {
            const pwd = document.getElementById('inputNewPassword')?.value || '';
            const cfm = document.getElementById('inputConfirmPassword')?.value || '';
            if (pwd.length < 8) {
                e.preventDefault();
                window.showGlassModal('error', 'Mật khẩu phải có ít nhất 8 ký tự!');
                return;
            }
            if (pwd !== cfm) {
                e.preventDefault();
                window.showGlassModal('error', 'Mật khẩu xác nhận không khớp!');
                return;
            }
            window.showGlassModal('loading', 'Đang cập nhật mật khẩu...');
        });
    </script>
</body>
</html>