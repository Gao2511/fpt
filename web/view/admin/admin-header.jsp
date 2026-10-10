<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!-- =========================================================
     ADMIN HEADER COMPONENT (DÙNG CHUNG CHO TẤT CẢ TRANG ADMIN)
     Tham số truyền vào: activeTab ('dashboard' | 'customers' | 'packages' | 'users' | 'email-logs' | 'settings')
     ========================================================= -->
<meta name="admin-csrf" content="<c:out value='${csrfToken}'/>">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-cms.css">
<script src="${pageContext.request.contextPath}/js/admin-security.js" defer></script>
<c:if test="${demoMode}"><div data-demo-admin role="status" style="padding:12px;text-align:center;background:#fff3cd;color:#663c00;font-weight:700;">CHẾ ĐỘ DEMO — KHÔNG THAY ĐỔI DỮ LIỆU THẬT</div></c:if>
<header class="admin-header">
    <!-- LOGO BÊN TRÁI -->
    <div class="header-left">
        <a href="${pageContext.request.contextPath}/admin/dashboard" class="logo" style="text-decoration:none;">
            <span class="logo-fpt">FPT</span>
            <div class="logo-text">
                <strong>Sale Manager</strong>
                <small>Hệ thống Quản trị FPT</small>
            </div>
        </a>
    </div>

    <!-- MENU ĐIỀU HƯỚNG CHÍNH -->
    <nav class="header-nav">
        <!-- 1. Dashboard -->
        <a href="${pageContext.request.contextPath}/admin/dashboard"
           class="${param.activeTab == 'dashboard' ? 'active' : ''}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7" rx="1"/>
                <rect x="14" y="3" width="7" height="7" rx="1"/>
                <rect x="14" y="14" width="7" height="7" rx="1"/>
                <rect x="3" y="14" width="7" height="7" rx="1"/>
            </svg>
            <span>Dashboard</span>
        </a>

        <!-- 2. Khách hàng -->
        <a href="${pageContext.request.contextPath}/admin/customers"
           class="${param.activeTab == 'customers' ? 'active' : ''}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            <span>Khách hàng</span>
        </a>

        <!-- 3. Gói cước & Mesh WiFi -->
        <a href="${pageContext.request.contextPath}/admin/packages"
           class="${param.activeTab == 'packages' ? 'active' : ''}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                <line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
            <span>Gói cước &amp; Mesh</span>
        </a>

        <!-- 4. Nội dung website -->
        <a href="${pageContext.request.contextPath}/admin/content"
           class="${param.activeTab == 'content' ? 'active' : ''}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M3 9h18M9 9v12"/>
            </svg>
            <span>Nội dung Web</span>
        </a>

        <!-- 5. Thông tin kinh doanh -->
        <a href="${pageContext.request.contextPath}/admin/business"
           class="${param.activeTab == 'business' ? 'active' : ''}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="7" width="18" height="14" rx="2"/>
                <path d="M8 7V3h8v4M3 12h18M10 12v3h4v-3"/>
            </svg>
            <span>Kinh doanh</span>
        </a>

        <!-- 6. Tài khoản -->
        <c:if test="${!demoMode}">
            <a href="${pageContext.request.contextPath}/admin/users"
               class="${param.activeTab == 'users' ? 'active' : ''}">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                </svg>
                <span>Tài khoản</span>
            </a>
        </c:if>

        <!-- 7. Thông báo / Email Logs -->
        <c:if test="${!demoMode}">
            <a href="${pageContext.request.contextPath}/admin/email-logs"
               class="${param.activeTab == 'email-logs' ? 'active' : ''}">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                    <polyline points="22,6 12,13 2,6"/>
                </svg>
                <span>Thông báo</span>
            </a>
        </c:if>

        <!-- 8. Cấu hình AI -->
        <c:if test="${!demoMode}">
            <a href="${pageContext.request.contextPath}/admin/settings"
               class="${param.activeTab == 'settings' ? 'active' : ''}">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="3"/>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                </svg>
                <span>Cấu hình AI</span>
            </a>
        </c:if>
    </nav>

    <!-- THÔNG TIN TÀI KHOẢN ADMIN BÊN PHẢI -->
    <div class="header-right">
        <a href="${pageContext.request.contextPath}/home" target="_blank" rel="noopener" class="header-view-site" title="Xem trang khách hàng">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
                <polyline points="15 3 21 3 21 9"/>
                <line x1="10" y1="14" x2="21" y2="3"/>
            </svg>
            <span>Xem Web</span>
        </a>
        <span class="role-badge">${demoMode ? 'Demo' : 'Admin'}</span>
        <div class="user-info">
            <c:choose>
                <c:when test="${not empty sessionScope.user.avatarUrl}">
                    <img src="${pageContext.request.contextPath}/${sessionScope.user.avatarUrl}" class="avatar" alt="Admin"
                         onerror="this.src='${pageContext.request.contextPath}/${sessionScope.user.defaultAvatar}'">
                </c:when>
                <c:otherwise>
                    <img src="${pageContext.request.contextPath}/${sessionScope.user.defaultAvatar}" class="avatar" alt="Admin">
                </c:otherwise>
            </c:choose>
            <div class="user-text">
                <strong>${sessionScope.user.fullName != null ? sessionScope.user.fullName : sessionScope.user.username}</strong>
                <small><span class="online-dot"></span> Online</small>
            </div>
            <a href="${pageContext.request.contextPath}/logout" class="logout-btn" title="Đăng xuất khỏi hệ thống">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
            </a>
        </div>
    </div>
</header>
