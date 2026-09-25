<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu - FPT Telecom</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/fpt-id.css">
</head>
<body>
    <div class="fptid-box">

        <!-- LOGO -->
        <div class="fptid-logo">
            <img src="${pageContext.request.contextPath}/assets/images/fpt-logo.jpg"
                 alt="FPT Telecom"
                 class="fptid-logo-img">
            <div class="tagline">Hệ sinh thái số FPT</div>
        </div>

        <!-- TIÊU ĐỀ -->
        <div class="fptid-title">
            <h1>Đặt lại <span>mật khẩu mới</span></h1>
            <p>Nhập mật khẩu mới cho tài khoản FPT ID của bạn.</p>
        </div>

        <!-- ALERT -->
        <c:if test="${not empty error}">
            <div class="alert-error">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                ${error}
            </div>
        </c:if>

        <!-- FORM -->
        <form action="${pageContext.request.contextPath}/reset-password" method="POST" id="resetForm">
            <input type="hidden" name="token" value="${token}">

            <div class="form-group">
                <label>Mật khẩu mới <span class="req">*</span></label>
                <div class="input-wrap">
                    <span class="icon">
                        <svg viewBox="0 0 24 24">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                    </span>
                    <input type="password" name="newPassword" id="newPassword" required
                           placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)">
                    <button type="button" class="toggle-eye" onclick="togglePwd('newPassword', this)">
                        <svg viewBox="0 0 24 24">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                    </button>
                </div>
            </div>

            <div class="form-group">
                <label>Xác nhận mật khẩu <span class="req">*</span></label>
                <div class="input-wrap">
                    <span class="icon">
                        <svg viewBox="0 0 24 24">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                    </span>
                    <input type="password" name="confirmPassword" id="confirmPassword" required
                           placeholder="Nhập lại mật khẩu mới">
                    <button type="button" class="toggle-eye" onclick="togglePwd('confirmPassword', this)">
                        <svg viewBox="0 0 24 24">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                    </button>
                </div>
            </div>

            <button type="submit" class="btn-fptid">
                <svg viewBox="0 0 24 24">
                    <polyline points="20 6 9 17 4 12"/>
                </svg>
                ĐẶT LẠI MẬT KHẨU
            </button>
        </form>

        <!-- CHUYỂN VỀ ĐĂNG NHẬP -->
        <div class="switch-auth">
            <a href="${pageContext.request.contextPath}/login">← Quay lại đăng nhập</a>
        </div>

    </div>

    <script>
        function togglePwd(id, btn) {
            const inp = document.getElementById(id);
            if (inp.type === 'password') {
                inp.type = 'text';
                btn.style.color = '#f37021';
            } else {
                inp.type = 'password';
                btn.style.color = '#9ca3af';
            }
        }

        document.getElementById('resetForm').addEventListener('submit', function(e) {
            const pwd = document.getElementById('newPassword').value;
            const cfm = document.getElementById('confirmPassword').value;
            if (pwd.length < 6) {
                e.preventDefault();
                alert('Mật khẩu phải có ít nhất 6 ký tự!');
                return false;
            }
            if (pwd !== cfm) {
                e.preventDefault();
                alert('Mật khẩu xác nhận không khớp!');
                return false;
            }
        });
    </script>
</body>
</html>