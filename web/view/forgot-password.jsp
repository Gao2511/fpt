<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu - FPT Telecom</title>
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

        <!-- TIÊU ĐỀ -->
        <div class="fptid-title">
            <h1>Quên <span>mật khẩu?</span></h1>
            <p>Nhập email hoặc số điện thoại đã đăng ký, chúng tôi sẽ gửi link đặt lại mật khẩu cho bạn.</p>
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
        <c:if test="${not empty message}">
            <div class="alert-success">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
                ${message}
            </div>
        </c:if>

        <!-- FORM -->
        <form action="${pageContext.request.contextPath}/forgot-password" method="POST">
            <div class="form-group">
                <label>Email hoặc Số điện thoại <span class="req">*</span></label>
                <div class="input-wrap">
                    <span class="icon">
                        <svg viewBox="0 0 24 24">
                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                            <polyline points="22,6 12,13 2,6"/>
                        </svg>
                    </span>
                    <input type="text" name="identifier" required
                           placeholder="VD: 0987654321 hoặc ten@fpt.vn"
                           value="${param.identifier}">
                </div>
            </div>

            <button type="submit" class="btn-fptid">
                <svg viewBox="0 0 24 24">
                    <line x1="22" y1="2" x2="11" y2="13"/>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
                GỬI LINK ĐẶT LẠI
            </button>
        </form>

        <!-- INFO BOX -->
        <div class="info-box">
            <span class="info-icon">i</span>
            <div>
                Link đặt lại mật khẩu có hiệu lực trong <strong>15 phút</strong>.
                Vui lòng kiểm tra cả hộp thư <strong>Spam</strong> nếu không thấy email.
            </div>
        </div>

        <!-- CHUYỂN VỀ ĐĂNG NHẬP -->
        <div class="switch-auth">
            Đã nhớ mật khẩu?
            <a href="${pageContext.request.contextPath}/login">Đăng nhập ngay</a>
        </div>

    </div>
</body>
</html>