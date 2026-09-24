<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${pkg.name} - Chi tiết gói cước FPT</title>
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/customer/package-detail.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/badge.css">
        <script src="${pageContext.request.contextPath}/js/address-data.js"></script>
        <script src="${pageContext.request.contextPath}/js/address-picker.js" defer></script>    
    </head>
    <body>

        <!-- TOP NAV -->
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
                    <a href="${pageContext.request.contextPath}/contact">Liên hệ</a>
                </nav>
                <div class="right">
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

                    <%-- AVATAR DROPDOWN --%>
                    <c:choose>
                        <c:when test="${empty sessionScope.user}">
                            <a href="${pageContext.request.contextPath}/login" class="btn-login-small">Đăng nhập</a>
                            <a href="${pageContext.request.contextPath}/register" class="btn-register-small">Đăng ký ngay</a>
                        </c:when>
                        <c:when test="${sessionScope.user.role == 'admin'}">
                            <a href="${pageContext.request.contextPath}/admin/dashboard" class="btn-login-small">
                                Trang quản trị
                            </a>
                            <a href="${pageContext.request.contextPath}/logout" class="btn-register-small">
                                Đăng xuất
                            </a>
                        </c:when>
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
                                        <line x1="16" y1="13" x2="8" y2="13"/>
                                        <line x1="16" y1="17" x2="8" y2="17"/>
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
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </header>

        <!-- BREADCRUMB -->
        <div class="breadcrumb-wrap">
            <div class="container">
                <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
                <span>›</span>
                <a href="${pageContext.request.contextPath}/home#packages">Bảng giá</a>
                <span>›</span>
                <strong>${pkg.name}</strong>
            </div>
        </div>

        <!-- MAIN -->
        <main class="detail-main">
            <div class="container">

                <!-- HERO GÓI CƯỚC -->
                <div class="pkg-hero">
                    <div class="pkg-hero-left">
                        <span class="pkg-hero-badge">${pkg.packageCode}</span>
                        <h1>${pkg.name}</h1>
                        <p class="pkg-hero-desc">${pkg.description}</p>

                        <div class="pkg-hero-price">
                            <span class="price-num">
                                <fmt:formatNumber value="${pkg.price}" pattern="#,###"/>
                            </span>
                            <span class="price-unit">đ/tháng</span>
                        </div>

                        <div class="pkg-hero-actions">
                            <a href="tel:0932079469" class="btn-primary-lg">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                                </svg>
                                Gọi tư vấn ngay
                            </a>
                            <a href="${pageContext.request.contextPath}/home#contact" class="btn-secondary-lg">
                                Đăng ký lắp đặt
                            </a>
                        </div>
                    </div>

                    <div class="pkg-hero-right">
                        <div class="info-badge">
                            <span class="info-label">Tốc độ</span>
                            <span class="info-value">${pkg.speedMbps} Mbps</span>
                        </div>

                        <%-- BADGE HOT --%>
                        <c:if test="${pkg.badgeType == 'hot'}">
                            <div class="info-badge hot">
                                <span class="info-label">Ưu đãi</span>
                                <span class="info-value" style="display:flex;align-items:center;">
                                    <span class="pkg-badge pkg-badge-hot">
                                        <svg viewBox="0 0 24 24">
                                        <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
                                        </svg>
                                        GÓI HOT
                                    </span>
                                </span>
                            </div>
                        </c:if>

                        <%-- BADGE NỔI BẬT --%>
                        <c:if test="${pkg.badgeType == 'featured'}">
                            <div class="info-badge featured">
                                <span class="info-label">Đề xuất</span>
                                <span class="info-value" style="display:flex;align-items:center;">
                                    <span class="pkg-badge pkg-badge-featured">
                                        <svg viewBox="0 0 24 24">
                                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                                        </svg>
                                        NỔI BẬT
                                    </span>
                                </span>
                            </div>
                        </c:if>
                    </div>
                </div>

                <!-- CHI TIẾT -->
                <div class="detail-grid">

                    <!-- BLOCK QUYỀN LỢI -->
                    <div class="detail-block">
                        <h2>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 12 20 22 4 22 4 12"/>
                            <rect x="2" y="7" width="20" height="5"/>
                            <line x1="12" y1="22" x2="12" y2="7"/>
                            <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"/>
                            <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"/>
                            </svg>
                            Quyền lợi khi đăng ký
                        </h2>
                        <ul class="benefit-list">
                            <c:choose>
                                <c:when test="${not empty pkg.longDescription}">
                                    <c:forEach items="${pkg.longDescriptionLines}" var="line">
                                        <li>${line}</li>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                    <li>Miễn phí modem WiFi 6 tốc độ cao</li>
                                    <li>Lắp đặt trong vòng 24 giờ</li>
                                    <li>Hỗ trợ kỹ thuật 24/7 qua tổng đài 1900 6600</li>
                                    <li>Không phí hòa mạng, không cọc thiết bị</li>
                                    <li>Tặng tháng cước theo chương trình khuyến mãi</li>
                                    </c:otherwise>
                                </c:choose>
                        </ul>
                    </div>

                    <!-- BLOCK CAM KẾT CHẤT LƯỢNG -->
                    <div class="detail-block">
                        <h2>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            <polyline points="9 12 11 14 15 10"/>
                            </svg>
                            Cam kết chất lượng dịch vụ
                        </h2>
                        <ul class="benefit-list">
                            <li>Đường truyền ổn định, không gián đoạn</li>
                            <li>Băng thông quốc tế đảm bảo theo cam kết</li>
                            <li>Kỹ thuật viên hỗ trợ tận nhà trong 24h</li>
                            <li>Hoàn tiền nếu không đạt tốc độ cam kết</li>
                        </ul>
                    </div>

                </div>

                <!-- FORM ĐĂNG KÝ NHANH -->
                <div class="detail-block">
                    <h2>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 20h9"/>
                        <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                        </svg>
                        Đăng ký gói "${pkg.name}"
                    </h2>

                    <%-- ⭐ GỢI Ý ĐĂNG NHẬP CHO KHÁCH CHƯA LOGIN --%>
                    <c:if test="${empty sessionScope.user}">
                        <div style="padding:12px 16px; background:#fff3e0; border:1px solid #f37021; border-radius:10px; margin-bottom:16px; font-size:13.5px; color:#92400e; display:flex; align-items:center; gap:8px;">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:18px; height:18px; flex-shrink:0;">
                            <circle cx="12" cy="12" r="10"/>
                            <line x1="12" y1="16" x2="12" y2="12"/>
                            <line x1="12" y1="8" x2="12.01" y2="8"/>
                            </svg>
                            <span>
                                <strong>Mẹo:</strong>
                                <a href="${pageContext.request.contextPath}/login?returnUrl=/package-detail?id=${pkg.id}"
                                   style="color:#f37021; font-weight:800; text-decoration:underline;">
                                    Đăng nhập
                                </a>
                                để tự động điền thông tin, đăng ký nhanh hơn!
                            </span>
                        </div>
                    </c:if>

                    <p style="color:#64748b;font-size:13.5px;margin-bottom:16px;">
                        Bạn có thể đăng ký cho chính mình hoặc cho người khác (bố mẹ, người thân, công ty...)
                    </p>

                    <form action="${pageContext.request.contextPath}/ContactServlet" method="POST" class="quick-contact-form">
                        <input type="hidden" name="user_package" value="${pkg.name}">

                        <div class="form-row">
                            <input type="text" name="user_name" id="user_name"
                                   placeholder="Họ tên người cần lắp đặt *"
                                   required
                                   value="${not empty sessionScope.user ? sessionScope.user.displayName : ''}">

                            <input type="tel" name="user_phone" id="user_phone"
                                   placeholder="SĐT người cần lắp đặt *"
                                   required
                                   value="${not empty sessionScope.user.phone ? sessionScope.user.phone : ''}">
                        </div>

                        <input type="email" name="user_email" id="user_email"
                               placeholder="Email (không bắt buộc)"
                               value="${not empty sessionScope.user.email ? sessionScope.user.email : ''}">

                        <%-- ⭐ ĐỊA CHỈ: CASCADING DROPDOWN --%>
                        <div id="addressPickerDetail" data-address-picker="detail" style="margin-bottom:12px;"></div>
                        <input type="text" name="user_note" id="user_note_detail"
                               placeholder="Ghi chú (không bắt buộc)"
                               list="noteSuggestionsDetail"
                               autocomplete="off">
                        <datalist id="noteSuggestionsDetail">
                            <option value="Nhà riêng, 1 tầng">
                            <option value="Nhà riêng, nhiều tầng">
                            <option value="Căn hộ chung cư">
                            <option value="Nhà mặt phố, kinh doanh">
                            <option value="Văn phòng công ty">
                            <option value="Quán cà phê / nhà hàng">
                            <option value="Cần lắp nhiều camera">
                            <option value="Cần lắp thêm FPT Play">
                            <option value="Cần lắp cho phòng trọ">
                            <option value="Lắp đặt ngoài giờ hành chính">
                        </datalist>
                        <button type="submit">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z"/>
                            <path d="M12 15l-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z"/>
                            <path d="M9 12H4s.55-3.03 2-4c1.62-1.08 5 0 5 0"/>
                            <path d="M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5"/>
                            </svg>
                            Đăng ký ngay - Nhận tư vấn trong 5 phút
                        </button>
                    </form>
                </div>

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
                                <span class="contact-icon icon-location">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
                                <span class="contact-icon icon-phone">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                                    </svg>
                                </span>
                                <div>
                                    <strong>Hotline đăng ký</strong>
                                    <span class="value">0932 079 469</span>
                                </div>
                            </li>
                            <li>
                                <span class="contact-icon icon-support">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
                                    <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
                                    </svg>
                                </span>
                                <div>
                                    <strong>Chăm sóc khách hàng</strong>
                                    <span class="value">1900 6600</span>
                                </div>
                            </li>
                            <li>
                                <span class="contact-icon icon-email">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
                            <strong>1900 6600</strong>
                        </div>
                    </div>

                    <div class="copyright">
                        <strong>Copyright © 2024 Cơ quan chủ quản: Công Ty Cổ Phần Viễn Thông FPT</strong>


                    </div>

                </div>

            </div>
        </footer>

        <!-- ============ SCRIPT: TOGGLE USER MENU ============ -->
        <script>
            function toggleUserMenu(event) {
                event.stopPropagation();
                const dropdown = document.getElementById('userDropdown');
                const avatar = document.querySelector('.user-avatar');
                if (dropdown && avatar) {
                    dropdown.classList.toggle('active');
                    avatar.classList.toggle('active');
                }
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

            document.addEventListener('keydown', function (event) {
                if (event.key === 'Escape') {
                    const dropdown = document.getElementById('userDropdown');
                    const avatar = document.querySelector('.user-avatar');
                    if (dropdown && avatar) {
                        dropdown.classList.remove('active');
                        avatar.classList.remove('active');
                    }
                }
            });
        </script>

        <!-- ============ SCRIPT: VALIDATE FORM ĐĂNG KÝ NHANH ============ -->
        <script>
            (function () {
                var quickForm = document.querySelector('.quick-contact-form');
                if (!quickForm) return;

                quickForm.addEventListener('submit', function (e) {
                    // ===== 1. VALIDATE HỌ TÊN =====
                    var nameInput = document.getElementById('user_name');
                    if (nameInput.value.trim() === '') {
                        e.preventDefault();
                        showError(nameInput, 'Vui lòng nhập họ tên người cần lắp đặt!');
                        return false;
                    }

                    // ===== 2. VALIDATE SĐT =====
                    var phoneInput = document.getElementById('user_phone');
                    var phone = phoneInput.value.trim().replace(/[\s.\-]/g, '');
                    if (!/^[0-9]{10,11}$/.test(phone)) {
                        e.preventDefault();
                        showError(phoneInput, 'Số điện thoại không hợp lệ! Vui lòng nhập 10-11 chữ số.');
                        return false;
                    }

                    // ===== 3. VALIDATE ĐỊA CHỈ =====
                    var prefix = 'detail';
                    var mode = window['getAddressMode_' + prefix] ? window['getAddressMode_' + prefix]() : 'new';

                    // --- Chế độ tự nhập tay ---
                    if (mode === 'manual') {
                        var manualInput = document.querySelector('.addr-manual-input[data-prefix="' + prefix + '"]');
                        if (!manualInput || manualInput.value.trim() === '') {
                            e.preventDefault();
                            showError(manualInput, 'Vui lòng nhập địa chỉ lắp đặt đầy đủ!');
                            return false;
                        }
                    }
                    // --- Chế độ cũ (3 cấp) ---
                    else if (mode === 'old') {
                        var provOld = document.querySelector('.addr-province-old[data-prefix="' + prefix + '"]');
                        var distOld = document.querySelector('.addr-district-old[data-prefix="' + prefix + '"]');
                        var wardOld = document.querySelector('.addr-ward-old[data-prefix="' + prefix + '"]');
                        var detailOld = document.querySelector('.addr-detail-old[data-prefix="' + prefix + '"]');

                        if (!provOld || !provOld.value) {
                            e.preventDefault();
                            showError(provOld, 'Vui lòng chọn Tỉnh/Thành phố!');
                            return false;
                        }
                        if (!distOld || !distOld.value) {
                            e.preventDefault();
                            showError(distOld, 'Vui lòng chọn Quận/Huyện!');
                            return false;
                        }
                        if (!wardOld || !wardOld.value) {
                            e.preventDefault();
                            showError(wardOld, 'Vui lòng chọn Phường/Xã!');
                            return false;
                        }
                        if (!detailOld || detailOld.value.trim() === '') {
                            e.preventDefault();
                            showError(detailOld, 'Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường...)!');
                            return false;
                        }
                    }
                    // --- Chế độ mới (2 cấp) - mặc định ---
                    else {
                        var provNew = document.querySelector('.addr-province-new[data-prefix="' + prefix + '"]');
                        var wardNew = document.querySelector('.addr-ward-new[data-prefix="' + prefix + '"]');
                        var detailNew = document.querySelector('.addr-detail[data-prefix="' + prefix + '"]');
                        var customProvNew = document.querySelector('.addr-custom-prov-new[data-prefix="' + prefix + '"]');
                        var customWardNew = document.querySelector('.addr-custom-ward-new[data-prefix="' + prefix + '"]');

                        var provVal = (provNew && provNew.value === 'custom')
                            ? (customProvNew ? customProvNew.value.trim() : '')
                            : (provNew ? provNew.value : '');
                        var wardVal = (wardNew && wardNew.value === 'custom')
                            ? (customWardNew ? customWardNew.value.trim() : '')
                            : (wardNew ? wardNew.value : '');

                        if (!provVal) {
                            e.preventDefault();
                            showError(provNew && provNew.value === 'custom' ? customProvNew : provNew, 'Vui lòng chọn hoặc nhập Tỉnh/Thành phố!');
                            return false;
                        }
                        if (!wardVal) {
                            e.preventDefault();
                            showError(wardNew && wardNew.value === 'custom' ? customWardNew : wardNew, 'Vui lòng chọn hoặc nhập Phường/Xã!');
                            return false;
                        }
                        if (!detailNew || detailNew.value.trim() === '') {
                            e.preventDefault();
                            showError(detailNew, 'Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường...)!');
                            return false;
                        }
                    }

                    // ===== 4. VALIDATE EMAIL (nếu có nhập) =====
                    var emailInput = document.getElementById('user_email');
                    var email = emailInput.value.trim();
                    if (email !== '') {
                        var emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
                        if (!emailRegex.test(email)) {
                            e.preventDefault();
                            showError(emailInput, 'Email không hợp lệ!');
                            return false;
                        }
                    }
                });

                // Hàm hiển thị lỗi
                function showError(input, message) {
                    alert(message);
                    if (!input) return;
                    input.focus();
                    input.scrollIntoView({behavior: 'smooth', block: 'center'});
                    input.style.borderColor = '#dc2626';
                    input.style.boxShadow = '0 0 0 4px rgba(220, 38, 38, 0.15)';
                    input.style.transition = 'all 0.3s';
                    input.addEventListener('input', function handler() {
                        input.style.borderColor = '';
                        input.style.boxShadow = '';
                        input.removeEventListener('input', handler);
                    });
                }
            })();
        </script>

    </body>
</html>