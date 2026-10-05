<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- =========================================================================
     TRANG ĐỔI MẬT KHẨU KHÁCH HÀNG - CHANGE-PASSWORD.JSP
     =========================================================================
     MỤC LỤC / TABLE OF CONTENTS:
     01. META & STYLESHEETS        - Imports CSS giao diện & theme toggle
     02. SHARED WIDGETS            - Runner Loader, Nav Slider, Theme Toggle
     03. TOP NAVIGATION HEADER     - Header dùng chung (/view/customer/customer-header.jsp)
     04. CHANGE PASSWORD FORM      - Khung nhập Mật khẩu hiện tại & Mật khẩu mới
     05. FORM VALIDATION SCRIPT    - Kiểm tra độ dài và trùng khớp mật khẩu
     ========================================================================= --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi mật khẩu - FPT Telecom</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
<link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/customer/profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/performance-optimize.css">
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />
<jsp:include page="/view/nav-slider.jsp" />
 <jsp:include page="/view/theme-toggle.jsp" />
<!-- ============ TOP NAV ============ -->
<jsp:include page="/view/customer/customer-header.jsp">
    <jsp:param name="activeMenu" value="profile" />
</jsp:include>

<!-- MAIN -->
<main class="profile-container" style="max-width: 560px;">

    <div class="profile-header">
        <h1> Đổi mật khẩu</h1>
        <p>Bảo mật tài khoản của bạn bằng mật khẩu mạnh</p>
    </div>

    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/change-password"
          id="pwdForm" onsubmit="return validateForm()">

        <div class="profile-card">
            <div class="profile-card-header">
                <div class="icon-circle">
                    <svg viewBox="0 0 24 24">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                </div>
                <div>
                    <h3>Thông tin bảo mật</h3>
                    <p>Vui lòng nhập chính xác mật khẩu hiện tại</p>
                </div>
            </div>

            <div class="form-group">
                <label>Mật khẩu hiện tại <span class="req">*</span></label>
                <input type="password" name="currentPassword" id="currentPassword"
                       placeholder="Nhập mật khẩu đang dùng" required autofocus>
            </div>

            <div class="form-group">
                <label>Mật khẩu mới <span class="req">*</span></label>
                <input type="password" name="newPassword" id="newPassword"
                       placeholder="Ít nhất 8 ký tự" required>
            </div>

            <div class="form-group">
                <label>Xác nhận mật khẩu mới <span class="req">*</span></label>
                <input type="password" name="confirmPassword" id="confirmPassword"
                       placeholder="Nhập lại mật khẩu mới" required>
            </div>

            <div class="form-actions">
                <a href="${pageContext.request.contextPath}/profile" class="btn btn-secondary">
                    Hủy
                </a>
                <button type="submit" class="btn btn-primary">
                    <svg viewBox="0 0 24 24">
                        <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                        <polyline points="17 21 17 13 7 13 7 21"/>
                        <polyline points="7 3 7 8 15 8"/>
                    </svg>
                    Đổi mật khẩu
                </button>
            </div>
        </div>
    </form>

</main>

<script>
    function validateForm() {
        const current = document.getElementById('currentPassword').value;
        const newPwd  = document.getElementById('newPassword').value;
        const confirm = document.getElementById('confirmPassword').value;

        if (newPwd.length < 8) {
            alert('Mật khẩu mới phải có ít nhất 8 ký tự!');
            return false;
        }

        if (newPwd !== confirm) {
            alert('Xác nhận mật khẩu không khớp!');
            return false;
        }

        if (newPwd === current) {
            alert('Mật khẩu mới phải khác mật khẩu hiện tại!');
            return false;
        }

        return true;
    }
</script>

</body>
</html>