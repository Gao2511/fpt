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
                   <a href="${pageContext.request.contextPath}/forgot-password" class="forgot">Quên mật khẩu?</a>
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

        <!-- ⭐ HOẶC ĐĂNG NHẬP VỚI GOOGLE -->
        <div style="display:flex; align-items:center; gap:12px; margin:20px 0;">
            <div style="flex:1; height:1px; background:linear-gradient(90deg, transparent, #e5e7eb, transparent);"></div>
            <span style="color:#9ca3af; font-size:12.5px; font-weight:600;">hoặc</span>
            <div style="flex:1; height:1px; background:linear-gradient(90deg, transparent, #e5e7eb, transparent);"></div>
        </div>

        <a href="${pageContext.request.contextPath}/google-login"
           style="display:flex; align-items:center; justify-content:center; gap:10px;
                  width:100%; padding:13px 20px; background:#fff;
                  border:1.5px solid #e5e7eb; border-radius:12px;
                  font-size:14px; font-weight:700; color:#1f2937;
                  text-decoration:none; cursor:pointer; margin-bottom:10px;
                  box-shadow:0 2px 8px rgba(0,0,0,0.04);
                  transition:all 0.25s cubic-bezier(0.4, 0, 0.2, 1);"
           onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 8px 20px rgba(0,0,0,0.1)'; this.style.borderColor='#4285F4';"
           onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 8px rgba(0,0,0,0.04)'; this.style.borderColor='#e5e7eb';">
            <svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Đăng nhập với Google
        </a>

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