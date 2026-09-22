<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<!-- Google Fonts -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800;900&family=Inter:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">

<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập FPT ID</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/fpt-id.css">
</head>
<body>
    <div class="fptid-box">

        <!-- NÚT QUAY LẠI TRANG CHỦ -->
        <div class="back-home-wrapper">
            <a href="${pageContext.request.contextPath}/home" class="btn-back-home">
                <svg viewBox="0 0 24 24">
                    <line x1="19" y1="12" x2="5" y2="12"/>
                    <polyline points="12 19 5 12 12 5"/>
                </svg>
                Quay lại trang chủ
            </a>
        </div>

        <!-- LOGO -->
        <div class="fptid-logo">
            <img src="${pageContext.request.contextPath}/assets/images/fpt-logo.jpg"
                 alt="FPT Telecom"
                 class="fptid-logo-img">
            <div class="tagline">Hệ sinh thái số FPT</div>
        </div>

        <!-- TABS -->
        <div class="fptid-tabs">
            <a href="${pageContext.request.contextPath}/login" class="active">Đăng Nhập</a>
            <a href="${pageContext.request.contextPath}/register">Đăng Ký Mới</a>
        </div>

        <!-- TIÊU ĐỀ -->
        <div class="fptid-title">
            <h1>Chào mừng <span>bạn trở lại!</span></h1>
            <p>Đăng nhập tài khoản FPT ID để quản lý toàn bộ dịch vụ</p>
        </div>

        <!-- ALERT -->
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert-error">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                ${error}
            </div>
        <% } %>
        <% if (request.getAttribute("success") != null) { %>
            <div class="alert-success">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
                ${success}
            </div>
        <% } %>

        <!-- FORM -->
        <form action="${pageContext.request.contextPath}/login" method="POST" id="loginForm">
            <!-- Hidden field để giữ returnUrl -->
            <input type="hidden" name="returnUrl" id="returnUrl"
                   value="${param.returnUrl}">

            <!-- Tài khoản -->
            <div class="form-group">
                <label>Số điện thoại hoặc Email FPT ID <span class="req">*</span></label>
                <div class="input-wrap">
                    <span class="icon">
                        <svg viewBox="0 0 24 24">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                    </span>
                    <input type="text" name="username" placeholder="VD: 0987654321 hoặc ten@fpt.vn"
                           value="${username}" required autofocus>
                </div>
            </div>

            <!-- Mật khẩu -->
            <div class="form-group">
                <label>
                    Mật khẩu truy cập <span class="req">*</span>
                    <a href="#" class="forgot">Quên mật khẩu?</a>
                </label>
                <div class="input-wrap">
                    <span class="icon">
                        <svg viewBox="0 0 24 24">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                    </span>
                    <input type="password" id="password" name="password" placeholder="Nhập mật khẩu của bạn" required>
                    <button type="button" class="toggle-eye" onclick="togglePwd('password', this)">
                        <svg viewBox="0 0 24 24">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                    </button>
                </div>
            </div>

            <!-- Nút đăng nhập -->
            <button type="submit" class="btn-fptid">
                <svg viewBox="0 0 24 24">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                ĐĂNG NHẬP FPT ID
            </button>
        </form>

        <!-- Switch -->
        <div class="switch-auth">
            Chưa có tài khoản FPT ID?
            <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
        </div>

    </div>

    <script>
        function togglePwd(id, btn) {
            const inp = document.getElementById(id);
            const svg = btn.querySelector("svg");
            if (inp.type === "password") {
                inp.type = "text";
                svg.innerHTML = '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>';
            } else {
                inp.type = "password";
                svg.innerHTML = '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>';
            }
        }
    </script>
</body>
</html>