<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu — FPT Telecom</title>

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

    <!-- 3. KHUNG FORM QUÊN MẬT KHẨU -->
    <main class="login-main">
        <div class="login-wrap">

            <c:set var="currentStep" value="${empty step ? 'email' : step}" />

            <c:choose>
                <%-- BƯỚC 1: NHẬP EMAIL ĐỂ NHẬN OTP --%>
                <c:when test="${currentStep eq 'email'}">
                    <form id="forgotForm" class="login-form" method="POST" action="${pageContext.request.contextPath}/forgot-password">
                        <input type="hidden" name="action" value="send_otp">

                        <!-- 3.1. Tiêu đề trang -->
                        <h1 class="login-title blur-fade" data-delay="1">Reset Access</h1>
                        <p class="login-sub blur-fade" data-delay="2">Nhập địa chỉ email tài khoản để nhận mã xác thực OTP</p>

                        <!-- 3.2. Bảng thông báo lỗi / thành công -->
                        <c:if test="${not empty error}">
                            <div class="glass-alert glass-alert-error blur-fade" data-delay="3">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                                <span>${error}</span>
                            </div>
                        </c:if>

                        <c:if test="${not empty message}">
                            <div class="glass-alert glass-alert-success blur-fade" data-delay="3">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                                <span>${message}</span>
                            </div>
                        </c:if>

                        <!-- 3.3. Ô nhập: Email -->
                        <div class="glass-input-wrap blur-fade" data-delay="4" style="max-width: 100%;">
                            <div class="glass-input-icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                    <polyline points="22,6 12,13 2,6"/>
                                </svg>
                            </div>
                            <input type="email" id="inputEmail" name="email" class="glass-input"
                                   placeholder="Địa chỉ email đã đăng ký" required autofocus
                                   value="${not empty email ? email : (not empty param.email ? param.email : '')}">
                        </div>

                        <!-- 3.4. Ghi chú bảo mật với Icon SVG -->
                        <p class="login-sub blur-fade" data-delay="5" style="font-size: 12.5px; color: rgba(255,255,255,0.65); text-align: left; width: 100%; display: flex; align-items: center; gap: 8px;">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#f37021" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0;">
                                <circle cx="12" cy="12" r="10"/>
                                <line x1="12" y1="16" x2="12" y2="12"/>
                                <line x1="12" y1="8" x2="12.01" y2="8"/>
                            </svg>
                            <span>Mã xác thực OTP gồm 6 chữ số sẽ được gửi qua email (hiệu lực 5 phút).</span>
                        </p>

                        <!-- 3.5. Nút gửi OTP -->
                        <button type="submit" id="btnSubmitForgot" class="glass-btn-primary blur-fade" data-delay="6">
                            GỬI MÃ OTP
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="22" y1="2" x2="11" y2="13"/>
                                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                            </svg>
                        </button>

                        <!-- 3.6. Quay lại Đăng nhập -->
                        <p class="login-sub blur-fade" data-delay="7" style="margin-top: 15px;">
                            Đã nhớ mật khẩu?
                            <a href="${pageContext.request.contextPath}/login">Đăng nhập ngay</a>
                        </p>
                    </form>
                </c:when>

                <%-- BƯỚC 2: NHẬP MÃ OTP XÁC THỰC --%>
                <c:otherwise>
                    <form id="otpForm" class="login-form" method="POST" action="${pageContext.request.contextPath}/forgot-password">
                        <input type="hidden" name="action" value="verify_otp">
                        <input type="hidden" name="email" value="${email}">

                        <!-- 3.1. Tiêu đề trang -->
                        <h1 class="login-title blur-fade" data-delay="1">Xác thực OTP</h1>
                        <p class="login-sub blur-fade" data-delay="2" style="word-break: break-all;">
                            Mã OTP đã gửi đến <strong>${email}</strong>
                        </p>

                        <!-- 3.2. Bảng thông báo lỗi / thành công -->
                        <c:if test="${not empty error}">
                            <div class="glass-alert glass-alert-error blur-fade" data-delay="3">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                                <span>${error}</span>
                            </div>
                        </c:if>

                        <c:if test="${not empty message}">
                            <div class="glass-alert glass-alert-success blur-fade" data-delay="3">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                                <span>${message}</span>
                            </div>
                        </c:if>

                        <!-- 3.3. Ô nhập: Mã OTP 6 số (Icon bảo mật số) -->
                        <div class="glass-input-wrap blur-fade" data-delay="4" style="max-width: 100%;">
                            <div class="glass-input-icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                    <circle cx="12" cy="16" r="1" fill="currentColor"/>
                                </svg>
                            </div>
                            <input type="text" id="inputOtp" name="otp" class="glass-input"
                                   placeholder="••••••" required maxlength="6" pattern="[0-9]{6}"
                                   inputmode="numeric" autocomplete="one-time-code" autofocus
                                   style="font-size: 22px; font-weight: 700; letter-spacing: 8px; text-align: center;">
                        </div>

                        <!-- 3.4. Ghi chú thời hạn với SVG đồng hồ -->
                        <p class="login-sub blur-fade" data-delay="5" style="font-size: 12.5px; color: rgba(255,255,255,0.65); text-align: left; width: 100%; display: flex; align-items: center; gap: 8px;">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0;">
                                <circle cx="12" cy="12" r="10"/>
                                <polyline points="12 6 12 12 16 14"/>
                            </svg>
                            <span>Mã OTP có hiệu lực trong 5 phút. Vui lòng kiểm tra cả thư mục Spam/Rác.</span>
                        </p>

                        <!-- 3.5. Nút xác nhận OTP với SVG Shield-check -->
                        <button type="submit" id="btnVerifyOtp" class="glass-btn-primary blur-fade" data-delay="6">
                            XÁC NHẬN OTP
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                                <polyline points="9 12 11 14 15 10"/>
                            </svg>
                        </button>

                        <!-- 3.6. Các tùy chọn: Gửi lại mã & Đổi email với SVG tinh xảo -->
                        <div class="blur-fade" data-delay="7" style="display: flex; justify-content: space-between; align-items: center; width: 100%; margin-top: 18px; font-size: 13px;">
                            <button type="button" id="btnResendOtp" style="background: none; border: none; color: #ff8a3d; cursor: pointer; text-decoration: none; font-size: 13px; font-weight: 600; padding: 0; display: inline-flex; align-items: center; gap: 6px; transition: color 0.2s ease;">
                                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                    <polyline points="23 4 23 10 17 10"/>
                                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                                </svg>
                                <span>Gửi lại mã OTP</span>
                            </button>
                            <a href="${pageContext.request.contextPath}/forgot-password" style="color: rgba(255,255,255,0.7); text-decoration: none; font-size: 13px; display: inline-flex; align-items: center; gap: 6px; transition: color 0.2s ease;">
                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                                </svg>
                                <span>Đổi email khác</span>
                            </a>
                        </div>

                        <!-- 3.7. Quay lại Đăng nhập -->
                        <p class="login-sub blur-fade" data-delay="7" style="margin-top: 16px;">
                            <a href="${pageContext.request.contextPath}/login" style="display: inline-flex; align-items: center; gap: 6px;">
                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <line x1="19" y1="12" x2="5" y2="12"/>
                                    <polyline points="12 19 5 12 12 5"/>
                                </svg>
                                <span>Quay lại đăng nhập</span>
                            </a>
                        </p>
                    </form>

                    <!-- Form ẩn để submit Resend OTP -->
                    <form id="resendForm" method="POST" action="${pageContext.request.contextPath}/forgot-password" style="display: none;">
                        <input type="hidden" name="action" value="resend_otp">
                        <input type="hidden" name="email" value="${email}">
                    </form>
                </c:otherwise>
            </c:choose>

        </div>
    </main>

    <!-- 4. MODAL THÔNG BÁO TIẾN TRÌNH / LỖI (Dùng chung) -->
    <jsp:include page="/view/auth-modal.jsp" />

    <!-- 5. SCRIPT XỬ LÝ -->
    <script src="${pageContext.request.contextPath}/js/login-glass.js"></script>
    <script>
        // Xử lý nút gửi lại OTP
        document.getElementById('btnResendOtp')?.addEventListener('click', function () {
            if (window.showGlassModal) {
                window.showGlassModal('loading', 'Đang gửi lại mã OTP...');
            }
            document.getElementById('resendForm')?.submit();
        });

        // Chỉ cho phép nhập số vào ô OTP
        document.getElementById('inputOtp')?.addEventListener('input', function (e) {
            this.value = this.value.replace(/[^0-9]/g, '');
        });
    </script>
</body>
</html>