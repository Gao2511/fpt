<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!-- =========================================================
     CUSTOMER TOP NAVIGATION HEADER (DÙNG CHUNG CHO KHÁCH HÀNG)
     Tham số truyền vào: activeMenu ('home' | 'packages' | 'orders' | 'contact' | 'profile')
     ========================================================= -->
<header class="top-nav">
    <div class="container">
        <!-- LOGO FPT TELECOM -->
        <a href="${pageContext.request.contextPath}/home" class="logo" title="FPT Telecom">
            <img src="${pageContext.request.contextPath}/assets/images/fpt-logo.jpg"
                 alt="FPT Telecom"
                 style="height: 50px; width: auto; object-fit: contain; display: block;">
        </a>

        <!-- MENU ĐIỀU HƯỚNG CHÍNH -->
        <nav class="menu">
            <c:choose>
                <c:when test="${param.isHomePage == 'true'}">
                    <a href="${pageContext.request.contextPath}/home" onclick="goHome(event)" class="active">Trang chủ</a>
                    <a href="#packages" onclick="scrollToSection(event, 'packages')">Bảng giá</a>
                    <a href="#contact" onclick="scrollToSection(event, 'contact')">Tư vấn</a>
                    <a href="${pageContext.request.contextPath}/contact">Liên hệ</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/home"
                       class="${param.activeMenu == 'home' ? 'active' : ''}">Trang chủ</a>
                    <a href="${pageContext.request.contextPath}/home#packages"
                       class="${param.activeMenu == 'packages' ? 'active' : ''}">Bảng giá</a>
                    <a href="${pageContext.request.contextPath}/my-orders"
                       class="${param.activeMenu == 'orders' ? 'active' : ''}">Xem đơn</a>
                    <a href="${pageContext.request.contextPath}/contact"
                       class="${param.activeMenu == 'contact' ? 'active' : ''}">Liên hệ</a>
                </c:otherwise>
            </c:choose>
        </nav>

        <!-- KHU VỰC BÊN PHẢI (HOTLINE & TÀI KHOẢN) -->
        <div class="right">
            <!-- Khối Hotline liên hệ nhanh -->
            <div class="hotline-box">
                <div class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                    </svg>
                </div>
                <div class="text">
                    <small>Hotline</small>
                    <strong>1900 6600</strong>
                </div>
            </div>

            <!-- Phân quyền hiển thị Menu tài khoản -->
            <c:choose>
                <%-- 1. Chưa đăng nhập --%>
                <c:when test="${empty sessionScope.user}">
                    <a href="${pageContext.request.contextPath}/login" class="btn-login-small">Đăng nhập</a>
                    <a href="${pageContext.request.contextPath}/register" class="btn-register-small">Đăng ký ngay</a>
                </c:when>

                <%-- 2. Quản trị viên (Admin) --%>
                <c:when test="${sessionScope.user.role == 'admin'}">
                    <a href="${pageContext.request.contextPath}/admin/dashboard" class="btn-login-small">Trang quản trị</a>
                    <a href="${pageContext.request.contextPath}/logout" class="btn-register-small">Đăng xuất</a>
                </c:when>

                <%-- 3. Khách hàng đã đăng nhập --%>
                <c:otherwise>
                    <div class="user-avatar-wrap">
                        <div class="user-avatar" onclick="toggleUserMenu(event)">
                            <c:choose>
                                <c:when test="${not empty sessionScope.user.avatarUrl}">
                                    <img src="${pageContext.request.contextPath}/${sessionScope.user.avatarUrl}"
                                         alt="Avatar" class="avatar-img-small">
                                </c:when>
                                <c:otherwise>
                                    <span class="avatar-initial">${sessionScope.user.initial}</span>
                                </c:otherwise>
                            </c:choose>
                            <span class="avatar-name">${sessionScope.user.displayName}</span>
                            <svg class="avatar-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <polyline points="6 9 12 15 18 9"/>
                            </svg>
                        </div>

                        <!-- Dropdown Menu khách hàng -->
                        <div class="user-dropdown" id="userDropdown">
                            <div class="dropdown-header">
                                <c:choose>
                                    <c:when test="${not empty sessionScope.user.avatarUrl}">
                                        <img src="${pageContext.request.contextPath}/${sessionScope.user.avatarUrl}"
                                             alt="Avatar" class="dropdown-avatar-img">
                                    </c:when>
                                    <c:otherwise>
                                        <div class="dropdown-avatar">${sessionScope.user.initial}</div>
                                    </c:otherwise>
                                </c:choose>
                                <div class="dropdown-info">
                                    <strong>${sessionScope.user.displayName}</strong>
                                    <small>${sessionScope.user.username}</small>
                                </div>
                            </div>
                            <div class="dropdown-divider"></div>

                            <!-- Link: Hồ sơ cá nhân -->
                            <a href="${pageContext.request.contextPath}/profile"
                               class="dropdown-item ${param.activeMenu == 'profile' ? 'active' : ''}">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                    <circle cx="12" cy="7" r="4"/>
                                </svg>
                                Hồ sơ cá nhân
                            </a>

                            <!-- Link: Đơn của tôi -->
                            <a href="${pageContext.request.contextPath}/my-orders"
                               class="dropdown-item ${param.activeMenu == 'orders' ? 'active' : ''}">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                    <polyline points="14 2 14 8 20 8"/>
                                </svg>
                                Đơn của tôi
                            </a>

                            <!-- Link: Đổi mật khẩu -->
                            <a href="${pageContext.request.contextPath}/change-password" class="dropdown-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                </svg>
                                Đổi mật khẩu
                            </a>
                            <div class="dropdown-divider"></div>

                            <!-- Link: Đăng xuất -->
                            <a href="${pageContext.request.contextPath}/logout" class="dropdown-item dropdown-logout">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                                    <polyline points="16 17 21 12 16 7"/>
                                    <line x1="21" y1="12" x2="9" y2="12"/>
                                </svg>
                                Đăng xuất
                            </a>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- ⭐ NÚT HAMBURGER MENU (chỉ hiện trên mobile) -->
        <button class="hamburger-btn" onclick="toggleMobileMenu(event)" aria-label="Menu">
            <span></span>
            <span></span>
            <span></span>
        </button>
    </div>
</header>

<!-- ⭐ MOBILE MENU OVERLAY & DRAWER -->
<div class="mobile-menu-overlay" onclick="closeMobileMenu()"></div>
<div class="mobile-menu" id="mobileMenu">
    <a href="${pageContext.request.contextPath}/home" onclick="handleMobileNav(event, this)">Trang chủ</a>
    <a href="${pageContext.request.contextPath}/home#packages" onclick="handleMobileNav(event, this)">Bảng giá</a>
    <a href="${pageContext.request.contextPath}/home#contact" onclick="handleMobileNav(event, this)">Tư vấn</a>
    <a href="${pageContext.request.contextPath}/contact" onclick="handleMobileNav(event, this)">Liên hệ</a>
    <c:choose>
        <c:when test="${not empty sessionScope.user}">
            <a href="${pageContext.request.contextPath}/profile" onclick="handleMobileNav(event, this)">Hồ sơ cá nhân</a>
            <a href="${pageContext.request.contextPath}/my-orders" onclick="handleMobileNav(event, this)">Đơn của tôi</a>
            <a href="${pageContext.request.contextPath}/logout" onclick="handleMobileNav(event, this)">Đăng xuất</a>
        </c:when>
        <c:otherwise>
            <div class="mobile-menu-auth" style="margin-top: 10px; display: flex; gap: 8px;">
                <a href="${pageContext.request.contextPath}/login" style="flex: 1; text-align: center; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.25); border-radius: 8px; padding: 10px;">Đăng nhập</a>
                <a href="${pageContext.request.contextPath}/register" style="flex: 1; text-align: center; background: linear-gradient(135deg, #f37021, #ff8c42); color: white; border-radius: 8px; padding: 10px;">Đăng ký</a>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<!-- =========================================================
     SCRIPTS ĐIỀU KHIỂN HEADER (DROPDOWN & MOBILE MENU)
     ========================================================= -->
<script>
    if (typeof toggleUserMenu === 'undefined') {
        function toggleUserMenu(event) {
            if (event) event.stopPropagation();
            var dropdown = document.getElementById('userDropdown');
            var avatar = document.querySelector('.user-avatar');
            if (dropdown) {
                dropdown.classList.toggle('active');
                dropdown.classList.toggle('show');
            }
            if (avatar) avatar.classList.toggle('active');
        }

        document.addEventListener('click', function(e) {
            var dropdown = document.getElementById('userDropdown');
            var avatar = document.querySelector('.user-avatar');
            if (dropdown && !dropdown.contains(e.target) && (!avatar || !avatar.contains(e.target))) {
                dropdown.classList.remove('active');
                dropdown.classList.remove('show');
                if (avatar) avatar.classList.remove('active');
            }
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                var dropdown = document.getElementById('userDropdown');
                var avatar = document.querySelector('.user-avatar');
                if (dropdown) {
                    dropdown.classList.remove('active');
                    dropdown.classList.remove('show');
                }
                if (avatar) avatar.classList.remove('active');
            }
        });
    }

    if (typeof toggleMobileMenu === 'undefined') {
        function toggleMobileMenu(event) {
            if (event) event.stopPropagation();
            var menu = document.getElementById('mobileMenu');
            var overlay = document.querySelector('.mobile-menu-overlay');
            var btn = document.querySelector('.hamburger-btn');
            if (menu) menu.classList.toggle('active');
            if (overlay) overlay.classList.toggle('active');
            if (btn) btn.classList.toggle('active');

            if (menu && menu.classList.contains('active')) {
                document.body.style.overflow = 'hidden';
            } else {
                document.body.style.overflow = '';
            }
        }

        function closeMobileMenu() {
            var menu = document.getElementById('mobileMenu');
            var overlay = document.querySelector('.mobile-menu-overlay');
            var btn = document.querySelector('.hamburger-btn');
            if (menu) menu.classList.remove('active');
            if (overlay) overlay.classList.remove('active');
            if (btn) btn.classList.remove('active');
            document.body.style.overflow = '';
        }

        function handleMobileNav(event, el) {
            var href = el.getAttribute('href') || '';
            closeMobileMenu();

            // Nếu đang ở trang chủ và click vào anchor hash
            if (href.indexOf('#') !== -1) {
                var hash = href.substring(href.indexOf('#'));
                var target = document.querySelector(hash);
                if (target) {
                    event.preventDefault();
                    setTimeout(function() {
                        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        history.pushState(null, '', hash);
                        window.dispatchEvent(new HashChangeEvent('hashchange'));
                    }, 100);
                }
            }
        }
    }
</script>
