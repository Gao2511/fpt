<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập — FPT Telecom</title>

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

    <!-- 3. KHUNG FORM ĐĂNG NHẬP CHÍNH -->
    <main class="login-main">
        <div class="login-wrap">
            <form id="loginForm" class="login-form" method="POST" action="${pageContext.request.contextPath}/login">

                <!-- 3.1. Tiêu đề trang -->
                <h1 class="login-title blur-fade" data-delay="1">Welcome Back</h1>
                <p class="login-sub blur-fade" data-delay="2">Đăng nhập tài khoản để tiếp tục</p>

                <!-- 3.2. Nút đăng nhập Google OAuth -->
                <div class="login-socials blur-fade" data-delay="3">
                    <button type="button" class="glass-btn" onclick="location.href='${pageContext.request.contextPath}/google-login'">
                        <svg viewBox="0 0 64 64" fill="none"><path fill="#4285F4" d="M57.8 30.15c0-2.42-.2-4.19-.62-6.03H29.5v10.95h16.25c-.33 2.72-2.1 6.82-6.03 9.57l-.06.37 8.76 6.78.6.06c5.58-5.15 8.79-12.72 8.79-21.7z"/><path fill="#34A853" d="M29.5 58.99c7.96 0 14.65-2.62 19.53-7.14l-9.31-7.21c-2.49 1.74-5.83 2.95-10.22 2.95-7.8 0-14.42-5.15-16.78-12.26l-.35.03-9.1 7.05-.12.33c4.85 9.63 14.81 16.25 26.35 16.25z"/><path fill="#FBBC05" d="M12.72 35.33c-.62-1.84-.98-3.8-.98-5.83s.36-4 .95-5.83l-.02-.39-9.22-7.16-.3.14C1.15 20.25 0 24.74 0 29.5s1.15 9.24 3.15 13.24l9.57-7.41z"/><path fill="#EB4335" d="M29.5 11.4c5.54 0 9.27 2.39 11.4 4.39l8.32-8.13C44.11 2.92 37.46 0 29.5 0 17.96 0 8 6.62 3.15 16.25l9.54 7.41C15.08 16.55 21.7 11.4 29.5 11.4z"/></svg>
                        Continue with Google
                    </button>
                </div>

                <!-- 3.3. Đường phân cách OR -->
                <div class="login-divider blur-fade" data-delay="4">OR</div>

                <!-- 3.4. Bảng thông báo (Alert Banners) -->
                <c:if test="${not empty error}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>${error}</span>
                    </div>
                </c:if>

                <c:if test="${not empty success}">
                    <div class="glass-alert glass-alert-success blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                        <span>${success}</span>
                    </div>
                </c:if>

                <c:if test="${param.reset == 'success'}">
                    <div class="glass-alert glass-alert-success blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                        <span>Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới của bạn.</span>
                    </div>
                </c:if>

                <c:if test="${param.error == 'email_is_local'}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>Email này đã đăng ký tài khoản mật khẩu thường. Vui lòng đăng nhập bằng email & mật khẩu bên dưới.</span>
                    </div>
                </c:if>

                <c:if test="${param.error == 'account_locked'}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>Tài khoản của bạn đã bị khóa. Vui lòng liên hệ bộ phận hỗ trợ.</span>
                    </div>
                </c:if>

                <c:if test="${param.error == 'google_cancelled' or param.error == 'cancelled'}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>Bạn đã huỷ phiên đăng nhập Google.</span>
                    </div>
                </c:if>

                <c:if test="${param.error == 'invalid_state'}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.</span>
                    </div>
                </c:if>

                <c:if test="${param.error == 'google_no_email'}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>Không lấy được email từ tài khoản Google. Vui lòng thử lại.</span>
                    </div>
                </c:if>

                <c:if test="${param.error == 'oauth_failed'}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>Đăng nhập Google thất bại. Vui lòng thử lại sau.</span>
                    </div>
                </c:if>

                <c:if test="${param.error == 'create_failed'}">
                    <div class="glass-alert glass-alert-error blur-fade" data-delay="4">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <span>Không thể khởi tạo tài khoản từ Google.</span>
                    </div>
                </c:if>

                <!-- 3.5. Ô nhập: Tài khoản / Email / SĐT -->
                <div class="glass-input-wrap blur-fade" data-delay="5">
                    <div class="glass-input-icon">
                        <svg viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    </div>
                    <input type="text" id="inputIdentifier" name="identifier" class="glass-input" placeholder="Tên đăng nhập, email hoặc SĐT"
                           autocomplete="username" required
                           value="${not empty identifier ? identifier : (not empty email ? email : '')}">
                </div>

                <!-- 3.6. Ô nhập: Mật khẩu -->
                <div class="glass-input-wrap blur-fade" data-delay="6" style="position: relative;">
                    <div class="glass-input-icon">
                        <svg viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    </div>
                    <input type="password" id="inputPassword" name="password" class="glass-input" placeholder="Mật khẩu" autocomplete="current-password" required>
                    <!-- Nút ẩn/hiện mật khẩu -->
                    <button type="button" id="btnTogglePassword" class="glass-toggle-pass" aria-label="Toggle Password">
                        <svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    </button>
                    <!-- Nút submit đăng nhập -->
                    <button type="submit" id="btnLoginSubmit" class="glass-submit-btn" aria-label="Đăng nhập">
                        <svg viewBox="0 0 24 24"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
                    </button>
                </div>

                <!-- 3.7. Liên kết Quên mật khẩu -->
                <div class="blur-fade" data-delay="6" style="width: 100%; max-width: 300px; display: flex; justify-content: flex-end;">
                    <a href="${pageContext.request.contextPath}/forgot-password" id="linkForgotPassword" class="login-forgot-link">Quên mật khẩu?</a>
                </div>

                <!-- 3.8. Liên kết chuyển sang Đăng ký -->
                <p class="login-sub blur-fade" data-delay="7" style="margin-top: 10px;">
                    Chưa có tài khoản?
                    <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
                </p>

            </form>
        </div>
    </main>

    <!-- 4. MODAL THÔNG BÁO TIẾN TRÌNH / LỖI (Dùng chung) -->
    <jsp:include page="/view/auth-modal.jsp" />

    <!-- 5. SCRIPT XỬ LÝ (JS Controller) -->
    <script src="${pageContext.request.contextPath}/js/login-glass.js"></script>
</body>
</html>