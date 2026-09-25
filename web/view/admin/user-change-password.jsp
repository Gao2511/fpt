<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi Mật Khẩu Tài Khoản - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-user.css">
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
        <a href="${pageContext.request.contextPath}/admin/packages">Gói cước</a>
        <a href="${pageContext.request.contextPath}/admin/users" class="active">Tài khoản</a>
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

<main class="admin-main">

    <div class="page-header-flex">
        <div>
            <a href="${pageContext.request.contextPath}/admin/users" class="back-link">
                ← Quay lại danh sách
            </a>
            <h1>Đổi Mật Khẩu Tài Khoản</h1>
            <p>#${targetUser.id} • ${targetUser.username} • ${targetUser.displayName}</p>
        </div>
    </div>

    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/admin/users"
          class="edit-form" id="pwdForm">

        <input type="hidden" name="action" value="update-password">
        <input type="hidden" name="id" value="${targetUser.id}">

        <div class="edit-grid">

            <!-- CỘT TRÁI: THÔNG TIN USER -->
            <div class="edit-card">
                <div class="edit-card-header">
                    <h3>Thông tin tài khoản</h3>
                </div>

                <div class="user-profile-box">
                    <div class="user-avatar-big">
                        <c:choose>
                            <c:when test="${not empty targetUser.avatarUrl}">
                                <img src="${pageContext.request.contextPath}/${targetUser.avatarUrl}" alt="Avatar">
                            </c:when>
                            <c:otherwise>
                                <span>${targetUser.initial}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="user-profile-info">
                        <h3>${targetUser.displayName}</h3>
                        <p>
                            <strong>@${targetUser.username}</strong>
                        </p>
                        <span class="role-tag ${targetUser.admin ? 'role-admin' : 'role-customer'}">
                            ${targetUser.admin ? 'Quản trị viên' : 'Khách hàng'}
                        </span>
                    </div>
                </div>

                <div class="info-readonly">
                    <div class="info-row">
                        <span class="info-label">Mã tài khoản:</span>
                        <span class="info-value">#${targetUser.id}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Tên đăng nhập:</span>
                        <span class="info-value">${targetUser.username}</span>
                    </div>
                    <c:if test="${not empty targetUser.phone}">
                        <div class="info-row">
                            <span class="info-label">Số điện thoại:</span>
                            <span class="info-value">${targetUser.phone}</span>
                        </div>
                    </c:if>
                    <c:if test="${not empty targetUser.email}">
                        <div class="info-row">
                            <span class="info-label">Email:</span>
                            <span class="info-value">${targetUser.email}</span>
                        </div>
                    </c:if>
                    <div class="info-row">
                        <span class="info-label">Trạng thái:</span>
                        <span class="info-value">
                            <span class="status-badge status-${targetUser.statusClass}">
                                ${targetUser.statusDisplay}
                            </span>
                        </span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Ngày tạo:</span>
                        <span class="info-value">
                            <fmt:formatDate value="${targetUser.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                        </span>
                    </div>
                </div>
            </div>

            <!-- CỘT PHẢI: FORM ĐỔI MẬT KHẨU -->
            <div class="edit-card">
                <div class="edit-card-header">
                    <h3>Đặt mật khẩu mới</h3>
                </div>

                <div class="warning-box">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="8" x2="12" y2="12"/>
                        <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    <div>
                        <strong>Lưu ý:</strong> Mật khẩu sẽ được đổi ngay lập tức.
                        Người dùng cần đăng nhập lại bằng mật khẩu mới.
                    </div>
                </div>

                <div class="form-group">
                    <label>Mật khẩu mới <span class="req">*</span></label>
                    <div class="input-wrap">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                            </svg>
                        </span>
                        <input type="password" name="newPassword" id="newPassword"
                               placeholder="Ít nhất 6 ký tự" required minlength="6"
                               oninput="checkPwdStrength(this.value)">
                    </div>
                </div>

                <div class="form-group">
                    <label>Xác nhận mật khẩu mới <span class="req">*</span></label>
                    <div class="input-wrap">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                            </svg>
                        </span>
                        <input type="password" name="confirmPassword" id="confirmPassword"
                               placeholder="Nhập lại mật khẩu mới" required minlength="6">
                    </div>
                </div>

                <!-- Thanh độ mạnh -->
                <div class="pwd-strength">
                    <div class="top">
                        <div class="label">Độ bảo mật:</div>
                        <div class="status" id="pwdStatus">Chưa nhập</div>
                    </div>
                    <div class="pwd-bars">
                        <div class="bar" id="bar1"></div>
                        <div class="bar" id="bar2"></div>
                        <div class="bar" id="bar3"></div>
                        <div class="bar" id="bar4"></div>
                    </div>
                </div>
            </div>
        </div>

        <div class="form-actions">
            <a href="${pageContext.request.contextPath}/admin/users" class="btn-secondary">
                Hủy bỏ
            </a>
            <button type="submit" class="btn-primary-lg">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                    <polyline points="17 21 17 13 7 13 7 21"/>
                    <polyline points="7 3 7 8 15 8"/>
                </svg>
                Đổi mật khẩu
            </button>
        </div>
    </form>

</main>

<script>
    document.getElementById('pwdForm').addEventListener('submit', function(e) {
        var pwd = document.getElementById('newPassword').value;
        var confirm = document.getElementById('confirmPassword').value;

        if (pwd.length < 6) {
            e.preventDefault();
            alert('Mật khẩu phải có ít nhất 6 ký tự!');
            document.getElementById('newPassword').focus();
            return false;
        }

        if (pwd !== confirm) {
            e.preventDefault();
            alert('Mật khẩu xác nhận không khớp!');
            document.getElementById('confirmPassword').focus();
            return false;
        }
    });

    function checkPwdStrength(pwd) {
        var status = document.getElementById("pwdStatus");
        var bars = [
            document.getElementById("bar1"),
            document.getElementById("bar2"),
            document.getElementById("bar3"),
            document.getElementById("bar4")
        ];

        bars.forEach(function(b) { b.className = "bar"; });

        var score = 0;
        if (pwd.length >= 6) score++;
        if (/[A-Z]/.test(pwd)) score++;
        if (/[0-9]/.test(pwd)) score++;
        if (/[^A-Za-z0-9]/.test(pwd)) score++;

        if (pwd.length === 0) {
            status.textContent = "Chưa nhập";
            status.className = "status";
        } else if (score <= 2) {
            status.textContent = "Yếu";
            status.className = "status weak";
            bars[0].classList.add("active-weak");
        } else if (score === 3) {
            status.textContent = "Trung bình";
            status.className = "status medium";
            bars[0].classList.add("active-medium");
            bars[1].classList.add("active-medium");
            bars[2].classList.add("active-medium");
        } else {
            status.textContent = "Mạnh";
            status.className = "status strong";
            bars.forEach(function(b) { b.classList.add("active-strong"); });
        }
    }
</script>

</body>
</html>