<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Hồ sơ cá nhân - FPT Telecom</title>
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/customer/profile.css">
    </head>
    <body>
<jsp:include page="/view/loading-runner.jsp" />
<jsp:include page="/view/nav-slider.jsp" />
 <jsp:include page="/view/theme-toggle.jsp" />
        <!-- ============ TOP NAV ============ -->
        <header class="top-nav">
            <div class="container">
                <a href="${pageContext.request.contextPath}/home" class="logo">
                    <img src="${pageContext.request.contextPath}/assets/images/fpt-logo.jpg"
                         alt="FPT Telecom"
                         style="height: 50px; width: auto; object-fit: contain; display: block;">
                </a>
                <nav class="menu">
                    <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
                    <a href="${pageContext.request.contextPath}/home#packages">Bảng giá</a>
                    <a href="${pageContext.request.contextPath}/my-orders">Xem đơn</a>
                </nav>
                <div class="right">
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
                            <a href="${pageContext.request.contextPath}/profile" class="dropdown-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                <circle cx="12" cy="7" r="4"/>
                                </svg>
                                Hồ sơ cá nhân
                            </a>
                            <a href="${pageContext.request.contextPath}/my-orders" class="dropdown-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                <polyline points="14 2 14 8 20 8"/>
                                </svg>
                                Đơn của tôi
                            </a>
                            <a href="${pageContext.request.contextPath}/change-password" class="dropdown-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                </svg>
                                Đổi mật khẩu
                            </a>
                            <div class="dropdown-divider"></div>
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
                </div>
            </div>
        </header>

        <!-- ============ MAIN ============ -->
        <main class="profile-container">

            <!-- FLASH MESSAGE -->
            <c:if test="${not empty message}">
                <div class="alert alert-${messageType}">
                    <span>${message}</span>
                    <button onclick="this.parentElement.remove()" class="alert-close">×</button>
                </div>
            </c:if>

            <!-- ============ AVATAR CARD ============ -->
            <div class="avatar-card" style="display:flex !important; align-items:center !important; gap:28px !important; flex-wrap:wrap !important;">

                <div class="snow-layer" id="snow1"></div>
                <div class="snow-layer" id="snow2"></div>
                <div class="snow-layer" id="snow3"></div>

                <!-- AVATAR -->
                <div style="position:relative !important; width:120px !important; height:120px !important; min-width:120px !important; min-height:120px !important; max-width:120px !important; max-height:120px !important; flex-shrink:0 !important; overflow:visible !important;">

                    <div id="avatarPreview"
                         style="width:120px !important; height:120px !important; min-width:120px !important; min-height:120px !important; max-width:120px !important; max-height:120px !important; border-radius:50% !important; overflow:hidden !important; display:block !important; background:linear-gradient(135deg,#f37021,#ff8c42); border:4px solid white; box-shadow:0 8px 24px rgba(243,112,33,0.35); box-sizing:border-box !important; position:relative !important;">

                        <c:choose>
                            <c:when test="${not empty sessionScope.user.avatarUrl}">
                                <img src="${pageContext.request.contextPath}/${sessionScope.user.avatarUrl}"
                                     alt="Avatar"
                                     style="position:absolute !important; top:0 !important; left:0 !important; width:120px !important; height:120px !important; min-width:120px !important; min-height:120px !important; max-width:120px !important; max-height:120px !important; object-fit:cover !important; object-position:center !important; border-radius:50% !important; display:block !important; margin:0 !important; padding:0 !important;">
                            </c:when>
                            <c:otherwise>
                                <div style="width:120px !important; height:120px !important; display:flex !important; align-items:center !important; justify-content:center !important; font-size:48px !important; font-weight:900 !important; color:white !important; text-transform:uppercase !important; line-height:1 !important;">
                                    ${sessionScope.user.initial}
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <label for="avatarInput" title="Đổi ảnh đại diện"
                           style="position:absolute !important; bottom:4px !important; right:4px !important; width:36px !important; height:36px !important; border-radius:50% !important; background:white !important; color:#f37021 !important; border:2px solid #f37021 !important; display:flex !important; align-items:center !important; justify-content:center !important; cursor:pointer !important; box-shadow:0 4px 12px rgba(0,0,0,0.15) !important; z-index:10 !important;">
                        <svg viewBox="0 0 24 24" style="width:16px !important; height:16px !important; stroke:currentColor !important; fill:none !important; stroke-width:2.5 !important;">
                        <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                        <circle cx="12" cy="13" r="4"/>
                        </svg>
                    </label>
                </div>

                <!-- THÔNG TIN NGƯỜI DÙNG -->
                <div class="avatar-info" style="flex:1 !important; min-width:200px !important;">

                    <h2 class="avatar-display-name">${sessionScope.user.displayName}</h2>

                    <span class="role-badge role-${sessionScope.user.role}">
                        <c:choose>
                            <c:when test="${sessionScope.user.role == 'admin'}">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 21 12 17.77 5.82 21 7 14.14l-5-4.87 6.91-1.01L12 2z"/>
                                </svg>
                                Quản trị viên
                            </c:when>
                            <c:when test="${sessionScope.user.role == 'staff'}">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="2" y="7" width="20" height="14" rx="2"/>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                                </svg>
                                Nhân viên
                            </c:when>
                            <c:otherwise>
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                <circle cx="12" cy="7" r="4"/>
                                </svg>
                                Khách hàng
                            </c:otherwise>
                        </c:choose>
                    </span>
                </div>
            </div>

            <!-- ============ FORM UPDATE ============ -->
            <form method="post" action="${pageContext.request.contextPath}/profile"
                  enctype="multipart/form-data" id="profileForm">

                <input type="file" id="avatarInput" name="avatar"
                       accept="image/*" style="display:none;"
                       onchange="previewAvatar(this)">

                <div class="profile-card">
                    <div class="profile-card-header">
                        <div class="icon-circle">
                            <svg viewBox="0 0 24 24">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                            </svg>
                        </div>
                        <div>
                            <h3>Thông tin cá nhân</h3>
                            <p>Tất cả các trường đều không bắt buộc</p>
                        </div>
                    </div>

                    <div class="form-group">
                        <label>Họ và tên <span class="optional-tag">(không bắt buộc)</span></label>
                        <input type="text" name="fullName"
                               value="${sessionScope.user.fullName}"
                               placeholder="VD: Nguyễn Văn An">
                    </div>

                    <div class="form-group">
                        <label>Số điện thoại <span class="optional-tag">(không bắt buộc)</span></label>
                        <input type="tel" name="phone"
                               value="${sessionScope.user.phone}"
                               placeholder="VD: 0912 345 678">
                    </div>

                    <div class="form-group">
                        <label>Email <span class="optional-tag">(không bắt buộc)</span></label>
                        <input type="email" name="email"
                               value="${sessionScope.user.email}"
                               placeholder="VD: email@example.com">
                    </div>

                    <div class="form-actions">
                        <a href="${pageContext.request.contextPath}/home" class="btn btn-secondary">
                            Hủy
                        </a>
                        <button type="submit" class="btn btn-primary">
                            <svg viewBox="0 0 24 24">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                            <polyline points="17 21 17 13 7 13 7 21"/>
                            <polyline points="7 3 7 8 15 8"/>
                            </svg>
                            Lưu thay đổi
                        </button>
                    </div>
                </div>
            </form>

            <!-- ============ QUICK ACTIONS ============ -->
            <div class="quick-actions">

                <a href="${pageContext.request.contextPath}/change-password" class="quick-action-btn">
                    <div class="qa-icon">
                        <svg viewBox="0 0 24 24">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                    </div>
                    <div class="qa-info">
                        <strong>Đổi mật khẩu</strong>
                        <small>Bảo mật tài khoản của bạn</small>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/my-orders" class="quick-action-btn">
                    <div class="qa-icon">
                        <svg viewBox="0 0 24 24">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                        <line x1="16" y1="13" x2="8" y2="13"/>
                        <line x1="16" y1="17" x2="8" y2="17"/>
                        </svg>
                    </div>
                    <div class="qa-info">
                        <strong>Đơn đăng ký của tôi</strong>
                        <small>Xem lịch sử đăng ký dịch vụ</small>
                    </div>
                </a>

            </div>

        </main>

        <!-- ============ FOOTER ============ -->
        <footer class="footer" id="contact-info">
            <div class="container">

                <div class="footer-top">

                    <div>
                        <div class="brand-footer">
                            <img src="${pageContext.request.contextPath}/assets/images/fpt-logo.jpg"
                                 alt="FPT Telecom"
                                 style="height: 55px; width: auto; object-fit: contain; margin-right: 12px;">
                            <div class="brand-text">
                                <strong>FPT Telecom</strong>
                                <small>Hệ sinh thái số FPT</small>
                            </div>
                        </div>
                        <p class="company-desc">
                            Công ty Cổ phần Viễn thông FPT — Nhà cung cấp dịch vụ Internet tốc độ cao,
                            Truyền hình tương tác FPT Play và giải pháp Camera AI thông minh hàng đầu Việt Nam.
                            Đồng hành cùng hàng triệu hộ gia đình và doanh nghiệp trên toàn quốc.
                        </p>

                        <div class="socials">
                            <a href="#" title="Facebook">
                                <svg viewBox="0 0 24 24">
                                <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"/>
                                </svg>
                            </a>
                            <a href="#" title="YouTube">
                                <svg viewBox="0 0 24 24">
                                <path d="M22.54 6.42a2.78 2.78 0 0 0-1.94-2C18.88 4 12 4 12 4s-6.88 0-8.6.46a2.78 2.78 0 0 0-1.94 2A29 29 0 0 0 1 11.75a29 29 0 0 0 .46 5.33A2.78 2.78 0 0 0 3.4 19c1.72.46 8.6.46 8.6.46s6.88 0 8.6-.46a2.78 2.78 0 0 0 1.94-2 29 29 0 0 0 .46-5.25 29 29 0 0 0-.46-5.33z"/>
                                <polygon points="9.75 15.02 15.5 11.75 9.75 8.48 9.75 15.02"/>
                                </svg>
                            </a>
                            <a href="#" title="Zalo">
                                <svg viewBox="0 0 24 24">
                                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
                                </svg>
                            </a>
                            <a href="#" title="TikTok">
                                <svg viewBox="0 0 24 24">
                                <path d="M9 12a4 4 0 1 0 4 4V4a5 5 0 0 0 5 5"/>
                                </svg>
                            </a>
                        </div>
                    </div>

                    <div>
                        <h4>Dịch vụ</h4>
                        <ul>
                            <li><a href="${pageContext.request.contextPath}/home#packages">Internet Cáp Quang</a></li>
                            <li><a href="${pageContext.request.contextPath}/home#packages">Combo Internet + FPT Play</a></li>
                            <li><a href="${pageContext.request.contextPath}/home#packages">FPT Play Box 4K</a></li>
                            <li><a href="${pageContext.request.contextPath}/home#packages">Camera AI Thông Minh</a></li>
                            <li><a href="${pageContext.request.contextPath}/home#packages">Gói Doanh Nghiệp</a></li>
                        </ul>
                    </div>

                    <div>
                        <h4>Hỗ trợ khách hàng</h4>
                        <ul>
                            <li><a href="#">Hướng dẫn thanh toán</a></li>
                            <li><a href="#">Tra cứu hóa đơn</a></li>
                            <li><a href="#">Báo sự cố kỹ thuật</a></li>
                            <li><a href="#">Câu hỏi thường gặp</a></li>
                            <li><a href="#">Chính sách bảo mật</a></li>
                        </ul>
                    </div>

                    <div>
                        <h4>Liên hệ với chúng tôi</h4>
                        <ul class="contact-list">
                            <li>
                                <span class="contact-icon">
                                    <svg viewBox="0 0 24 24">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                                    <circle cx="12" cy="10" r="3"/>
                                    </svg>
                                </span>
                                <div>
                                    <strong>Trụ sở chính</strong>
                                    94 Phạm Hùng, TP Quy Nhơn, Bình Định
                                </div>
                            </li>
                            <li>
                                <span class="contact-icon">
                                    <svg viewBox="0 0 24 24">
                                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                                    </svg>
                                </span>
                                <div>
                                    <strong>Hotline đăng ký</strong>
                                    <span class="value">0932 079 469</span>
                                </div>
                            </li>
                            <li>
                                <span class="contact-icon">
                                    <svg viewBox="0 0 24 24">
                                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                                    </svg>
                                </span>
                                <div>
                                    <strong>Chăm sóc khách hàng</strong>
                                    <span class="value">1900 6600</span>
                                </div>
                            </li>
                            <li>
                                <span class="contact-icon">
                                    <svg viewBox="0 0 24 24">
                                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                    <polyline points="22,6 12,13 2,6"/>
                                    </svg>
                                </span>
                                <div>
                                    <strong>Email</strong>
                                    <a href="mailto:PhucBN2@fpt.com">PhucBN2@fpt.com</a>
                                </div>
                            </li>
                        </ul>
                    </div>

                </div>

                <div class="footer-bottom">

                    <div class="hotline-box-footer">
                        <div class="phone-icon">
                            <svg viewBox="0 0 24 24">
                            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                            </svg>
                        </div>
                        <div class="phone-info">
                            <small>Hotline hỗ trợ 24/7</small>
                            <strong>0932 079 469</strong>
                        </div>
                    </div>

                    <div class="copyright">
                        <strong>Copyright © 2024 Cơ quan chủ quản: Công Ty Cổ Phần Viễn Thông FPT</strong>
                        
                       
                    </div>

                </div>

            </div>
        </footer>

        <script>
            function previewAvatar(input) {
                if (input.files && input.files[0]) {
                    const file = input.files[0];

                    if (file.size > 5 * 1024 * 1024) {
                        alert('File quá lớn! Vui lòng chọn ảnh nhỏ hơn 5MB.');
                        input.value = '';
                        return;
                    }

                    if (!file.type.startsWith('image/')) {
                        alert('Vui lòng chọn file ảnh!');
                        input.value = '';
                        return;
                    }

                    const reader = new FileReader();
                    reader.onload = function (e) {
                        const preview = document.getElementById('avatarPreview');
                        preview.innerHTML = '<img src="' + e.target.result + '" alt="Avatar">';
                    };
                    reader.readAsDataURL(file);
                }
            }

            function toggleUserMenu(event) {
                event.stopPropagation();
                document.getElementById('userDropdown').classList.toggle('active');
                document.querySelector('.user-avatar').classList.toggle('active');
            }

            document.addEventListener('click', function (event) {
                const dropdown = document.getElementById('userDropdown');
                const avatar = document.querySelector('.user-avatar');
                if (dropdown && avatar
                        && !avatar.contains(event.target)
                        && !dropdown.contains(event.target)) {
                    dropdown.classList.remove('active');
                    avatar.classList.remove('active');
                }
            });

            // ⭐ KHÔNG CÓ VALIDATE NÀO CẢ — submit luôn thành công
        </script>

    </body>
</html>