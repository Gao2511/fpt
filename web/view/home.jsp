<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>FPT Telecom - Internet, Truyền hình, Camera</title>
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/badge.css">
    </head>
    <body>

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
                    <a href="#packages">Bảng giá</a>
                    <a href="#contact">Tư vấn</a>
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
                    <c:choose>
                        <%-- Chưa đăng nhập --%>
                        <c:when test="${empty sessionScope.user}">
                            <a href="${pageContext.request.contextPath}/login" class="btn-login-small">Đăng nhập</a>
                            <a href="${pageContext.request.contextPath}/register" class="btn-register-small">Đăng ký ngay</a>
                        </c:when>

                        <%-- Admin --%>
                        <c:when test="${sessionScope.user.role == 'admin'}">
                            <a href="${pageContext.request.contextPath}/admin/dashboard" class="btn-login-small">
                                Trang quản trị
                            </a>
                            <a href="${pageContext.request.contextPath}/logout" class="btn-register-small">
                                Đăng xuất
                            </a>
                        </c:when>

                        <%-- Customer --%>
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

                                <%-- Dropdown menu --%>
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

        <!-- ============ HERO ============ -->
        <section class="hero">
            <div class="container">
                <div class="hero-text">
                    <c:if test="${not empty sessionScope.user && sessionScope.user.role == 'customer'}">
                        <div class="welcome-banner">
                            <span class="welcome-line-1">Chào mừng trở lại,</span>
                            <span class="welcome-line-2">${sessionScope.user.fullName}!</span>
                        </div>
                    </c:if>
                    <span class="badge-top">WiFi 6 - Hiện đại - Tốc độ cao</span>
                    <h1>BẢNG GIÁ INTERNET FPT<br><span class="highlight">CÁP QUANG TỐC ĐỘ CAO</span></h1>
                    <p class="desc">
                        Trang bị Modem Wifi 6 hiện đại giúp kết nối mạng ổn định,
                        Tốc độ cao đáp ứng nhu cầu làm việc, giải trí và học tập trực tuyến.
                    </p>
                    <div class="badges">
                        <span>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 6 9 17 4 12"/>
                            </svg>
                            WiFi 6 hiện đại
                        </span>
                        <span>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 6 9 17 4 12"/>
                            </svg>
                            Lắp đặt nhanh
                        </span>
                        <span>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 6 9 17 4 12"/>
                            </svg>
                            Hỗ trợ 24/7
                        </span>
                    </div>
                    <div class="cta-buttons">
                        <a href="#packages" class="btn-hero-primary">
                            Xem bảng giá ngay →
                        </a>
                        <a href="#contact" class="btn-hero-secondary">
                            <svg style="width:16px;height:16px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;" viewBox="0 0 24 24">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                            </svg>
                            Tư vấn miễn phí
                        </a>
                    </div>
                </div>
                <div class="hero-image">
                    <div class="hero-slideshow">
                        <img src="${pageContext.request.contextPath}/assets/images/Hero1.png" 
                             alt="FPT Internet" class="hero-slide active">
                        <img src="${pageContext.request.contextPath}/assets/images/Hero2.png" 
                             alt="FPT WiFi 6" class="hero-slide">
                        <img src="${pageContext.request.contextPath}/assets/images/Hero3.png" 
                             alt="FPT Camera" class="hero-slide">
                    </div>
                    <div class="floating-tag">
                        <span class="dot"></span> WiFi 6 hiện đại
                    </div>
                </div>
            </div>
        </section>

        <!-- ============ SECTION TITLE: PACKAGES ============ -->
        <div class="section-title">
            <span class="small-label">Bảng cước áp dụng cả năm</span>
            <h2>${packages.size()} Gói Cước Internet FPT Đột Phá Bạn Chạy Nhất</h2>
            <p>Cam kết đường truyền Internet ổn định, không lo gián đoạn trong suốt quá trình sử dụng.</p>
        </div>

        <!-- ============ GÓI CƯỚC — LOAD ĐỘNG TỪ DB ============ -->
        <div class="container" id="packages">
            <div class="packages">
                <c:forEach items="${packages}" var="pkg">
                    <div class="pkg ${pkg.badgeType == 'hot' ? 'featured' : ''}">

                        <%-- RIBBON HOT (SVG) --%>
                        <c:if test="${pkg.badgeType == 'hot'}">
                            <div class="pkg-ribbon pkg-ribbon-hot">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
                                </svg>
                                HOT
                            </div>
                        </c:if>

                        <%-- RIBBON NỔI BẬT (SVG) --%>
                        <c:if test="${pkg.badgeType == 'featured'}">
                            <div class="pkg-ribbon pkg-ribbon-featured">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                                </svg>
                                NỔI BẬT
                            </div>
                        </c:if>

                        <div class="pkg-header">
                            ${pkg.name}
                            <small>${pkg.description}</small>
                        </div>

                        <div class="pkg-body">
                            <div class="price">
                                <fmt:formatNumber value="${pkg.price}" pattern="#,###"/>
                                <span class="unit">đ/tháng</span>
                            </div>
                            <ul>
                                <c:choose>
                                    <c:when test="${not empty pkg.longDescription}">
                                        <c:forEach items="${pkg.longDescriptionLines}" var="line">
                                            <li>${line}</li>
                                            </c:forEach>
                                        </c:when>
                                        <c:otherwise>
                                        <li>WiFi 6 tốc độ cao ${pkg.speedMbps}Mbps</li>
                                        <li>Miễn phí modem WiFi 6</li>
                                        <li>Hỗ trợ kỹ thuật 24/7</li>
                                        <li>Lắp đặt trong 24 giờ</li>
                                        </c:otherwise>
                                    </c:choose>
                            </ul>
                            <a href="${pageContext.request.contextPath}/package-detail?id=${pkg.id}"
                               class="btn-pkg">Mua ngay →</a>
                        </div>
                    </div>
                </c:forEach>

                <c:if test="${empty packages}">
                    <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: #64748b;">
                        <p>Chưa có gói cước nào. Vui lòng thêm gói trong trang quản trị.</p>
                    </div>
                </c:if>
            </div>
        </div>

        <!-- ============ FEATURES ============ -->
        <div class="section-title">
            <h2>Cam Kết Chất Lượng Dịch Vụ Hàng Đầu FPT</h2>
            <p>Đảm bảo sự ổn định và an toàn trong suốt quá trình sử dụng dịch vụ của bạn.</p>
        </div>

        <div class="container">
            <div class="features">
                <div class="feature">
                    <div class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M5 12.55a11 11 0 0 1 14.08 0"/>
                        <path d="M1.42 9a16 16 0 0 1 21.16 0"/>
                        <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
                        <line x1="12" y1="20" x2="12.01" y2="20"/>
                        </svg>
                    </div>
                    <h4>CÔNG NGHỆ WIFI 6</h4>
                    <p>Kết nối không giới hạn, cho phép nhiều thiết bị cùng lúc mà không bị giật lag.</p>
                </div>
                <div class="feature">
                    <div class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z"/>
                        <path d="M12 15l-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z"/>
                        <path d="M9 12H4s.55-3.03 2-4c1.62-1.08 5 0 5 0"/>
                        <path d="M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5"/>
                        </svg>
                    </div>
                    <h4>TỐC ĐỘ CAO 1GBPS</h4>
                    <p>Đường truyền siêu tốc, đáp ứng mọi nhu cầu giải trí, làm việc và học tập online.</p>
                </div>
                <div class="feature">
                    <div class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
                        </svg>
                    </div>
                    <h4>LẮP ĐẶT TRONG 24H</h4>
                    <p>Kỹ thuật viên có mặt nhanh chóng, lắp đặt và kích hoạt dịch vụ trong ngày.</p>
                </div>
                <div class="feature">
                    <div class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
                        <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
                        </svg>
                    </div>
                    <h4>HỖ TRỢ 24/7 TẬN TÂM</h4>
                    <p>Tổng đài hỗ trợ hoạt động 24/7, giải đáp mọi thắc mắc của bạn mọi lúc mọi nơi.</p>
                </div>
            </div>
        </div>

        <!-- ============ DEVICES ============ -->
        <div class="section-title">
            <h2>Thiết Bị Đi Kèm Chính Hãng FPT</h2>
        </div>

        <div class="container">
            <div class="devices">

                <div class="device">
                    <div class="img-wrap">
                        <img src="${pageContext.request.contextPath}/assets/images/Wifi_6.png" 
                             alt="Modem WiFi 6">
                    </div>
                    <div class="info">
                        <h4>Modem WiFi 6 </h4>
                        <p>Modem WiFi 6 tốc độ cao, hỗ trợ nhiều thiết bị cùng lúc không giật lag.</p>
                    </div>
                </div>

                <div class="device">
                    <div class="img-wrap">
                        <img src="${pageContext.request.contextPath}/assets/images/FPT-Play-Box.png" 
                             alt="FPT Play Box">
                    </div>
                    <div class="info">
                        <h4>FPT Play Box 4K Voice</h4>
                        <p>Đầu thu FPT Play 4K, điều khiển bằng giọng nói, xem hàng trăm kênh truyền hình.</p>
                    </div>
                </div>

                <div class="device">
                    <div class="img-wrap">
                        <img src="${pageContext.request.contextPath}/assets/images/Camera-FPT-Play.png" 
                             alt="Camera AI">
                    </div>
                    <div class="info">
                        <h4>Camera AI Giám Sát 24/7</h4>
                        <p>Camera AI thông minh, giám sát an ninh 24/7, xem mọi lúc mọi nơi qua điện thoại.</p>
                    </div>
                </div>

                <div class="device">
                    <div class="img-wrap">
                        <img src="${pageContext.request.contextPath}/assets/images/Ngoai_Hang_Anh.png" 
                             alt="Box Ngoại Hạng Anh">
                    </div>
                    <div class="info">
                        <h4>Box Ngoại Hạng Anh</h4>
                        <p>Xem trọn vẹn Ngoại hạng Anh và nhiều giải đấu hấp dẫn khác với chất lượng 4K.</p>
                    </div>
                </div>

            </div>
        </div>
                             

        <!-- ============ CONTACT FORM ============ -->
        <section class="contact-section" id="contact">
            <div class="container">
                <div class="contact-wrapper">
                    <div class="icon-large">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                        </svg>
                    </div>
                    <h2>Liên Hệ Tư Vấn Khảo Sát &<br><span>Nhận Khuyến Mãi Lớn Nhất</span></h2>
                    <p>Chuyên viên của chúng tôi sẽ liên hệ với bạn sớm nhất có thể, tư vấn gói cước phù hợp và nhanh chóng lắp đặt.</p>

                    <form class="contact-form"
                          action="${pageContext.request.contextPath}/ContactServlet"
                          method="POST"
                          id="contactForm">

                        <div>
                            <label>Họ tên người cần lắp đặt <span style="color:#f37021;">*</span></label>
                            <input type="text" name="user_name" id="user_name"
                                   placeholder="Nguyễn Văn A" required
                                   value="${not empty sessionScope.user ? sessionScope.user.displayName : ''}">
                        </div>
                        <div>
                            <label>SĐT người cần lắp đặt <span style="color:#f37021;">*</span></label>
                            <input type="tel" name="user_phone" id="user_phone"
                                   placeholder="0912 345 678" required
                                   value="${not empty sessionScope.user.phone ? sessionScope.user.phone : ''}">
                        </div>

                        <!-- EMAIL KHÁCH HÀNG - KHÔNG BẮT BUỘC -->
                        <div class="full">
                            <label>Email <span style="color:#9ca3af;font-weight:400;font-style:italic;">(không bắt buộc)</span></label>
                            <input type="email" name="user_email" id="user_email"
                                   placeholder="khachhang@gmail.com"
                                   value="${not empty sessionScope.user.email ? sessionScope.user.email : ''}">
                        </div>

                        <!-- ⭐ ĐỊA CHỈ: LUÔN ĐỂ TRỐNG -->
                        <div class="full">
                            <label>Địa chỉ lắp đặt</label>
                            <input type="text" name="user_address" id="user_address"
                                   placeholder="Số nhà, đường, phường, quận..."
                                   value="">
                        </div>
                        <div class="full">
                            <label>Ghi chú (không bắt buộc)</label>
                            <textarea name="user_note" id="user_note"
                                      placeholder="Ví dụ: nhà 3 tầng, dùng nhiều camera..."></textarea>
                        </div>
                        <button type="submit">Đăng ký tư vấn ngay →</button>
                    </form>
                </div>
            </div>
        </section>

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

                        <div class="certifications">
                            <div class="cert-badge">
                                <svg viewBox="0 0 24 24">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                                </svg>
                                Bộ Công Thương
                            </div>
                            <div class="cert-badge">
                                <svg viewBox="0 0 24 24">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                                <polyline points="22 4 12 14.01 9 11.01"/>
                                </svg>
                                ISO 27001
                            </div>
                            <div class="cert-badge">
                                <svg viewBox="0 0 24 24">
                                <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/>
                                <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/>
                                <path d="M4 22h16"/>
                                <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/>
                                <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/>
                                <path d="M18 2H6v7a6 6 0 0 0 12 0V2z"/>
                                </svg>
                                Top 10 ISP
                            </div>
                        </div>

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
                                    <a href="mailto:hieulv20@fpt.com">PhucBN2@fpt.com</a>
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
                        <strong>Copyright © 2026 FPT Telecom.</strong><br>
                        Cơ quan chủ quản: Công Ty Cổ Phần Viễn Thông FPT.<br>
                        Tất cả các quyền được bảo lưu theo quy định pháp luật Việt Nam.
                    </div>

                </div>

            </div>
        </footer>

        <!-- ============ SCRIPT CHUNG ============ -->
        <script>
            // ===== SCROLL REVEAL =====
            const revealElements = document.querySelectorAll(
                    '.section-title, .pkg, .feature, .device, .contact-wrapper'
                    );
            revealElements.forEach(el => el.classList.add('reveal'));

            const observer = new IntersectionObserver((entries) => {
                entries.forEach((entry, index) => {
                    if (entry.isIntersecting) {
                        setTimeout(() => {
                            entry.target.classList.add('active');
                        }, index * 80);
                        observer.unobserve(entry.target);
                    }
                });
            }, {
                threshold: 0.1,
                rootMargin: '0px 0px -50px 0px'
            });

            revealElements.forEach(el => observer.observe(el));

            // ===== NAVBAR SCROLL EFFECT =====
            const topNav = document.querySelector('.top-nav');
            window.addEventListener('scroll', () => {
                if (window.scrollY > 50) {
                    topNav.style.boxShadow = '0 4px 30px rgba(0,0,0,0.1)';
                    topNav.style.padding = '8px 0';
                } else {
                    topNav.style.boxShadow = '0 2px 20px rgba(0,0,0,0.06)';
                    topNav.style.padding = '12px 0';
                }
            });

            // ===== CLICK RIPPLE EFFECT CHO NÚT =====
            document.querySelectorAll('.btn-pkg, .btn-device, .btn-hero-primary, .contact-form button').forEach(btn => {
                btn.addEventListener('click', function (e) {
                    const rect = this.getBoundingClientRect();
                    const x = e.clientX - rect.left;
                    const y = e.clientY - rect.top;

                    const ripple = document.createElement('span');
                    ripple.style.cssText = `
                        position: absolute;
                        width: 0;
                        height: 0;
                        border-radius: 50%;
                        background: rgba(255,255,255,0.5);
                        transform: translate(-50%, -50%);
                        left: ${x}px;
                        top: ${y}px;
                        pointer-events: none;
                        animation: rippleAnim 0.6s ease-out;
                    `;
                    this.style.position = 'relative';
                    this.style.overflow = 'hidden';
                    this.appendChild(ripple);

                    setTimeout(() => ripple.remove(), 600);
                });
            });

            const style = document.createElement('style');
            style.textContent = `
                @keyframes rippleAnim {
                    to { width: 300px; height: 300px; opacity: 0; }
                }
            `;
            document.head.appendChild(style);
        </script>

        <!-- ============ SCRIPT: LƯU FORM TƯ VẤN + CHUYỂN LOGIN ============ -->
        <script>
            (function () {
                var contactForm = document.getElementById('contactForm');
                if (!contactForm) {
                    console.log('❌ Không tìm thấy form contact');
                    return;
                }

                var isLoggedIn = '${not empty sessionScope.user}' === 'true';
                var STORAGE_KEY = 'contactFormBackup';

                console.log('✅ Form contact đã load. isLoggedIn =', isLoggedIn);

                // ===== BƯỚC 1: Chặn submit khi CHƯA đăng nhập =====
                if (!isLoggedIn) {
                    contactForm.addEventListener('submit', function (e) {
                        e.preventDefault();
                        console.log('🔒 Chưa login → Lưu form và chuyển sang login');

                        var formData = {
                            user_name: document.getElementById('user_name').value,
                            user_phone: document.getElementById('user_phone').value,
                            user_email: document.getElementById('user_email').value,
                            user_address: document.getElementById('user_address').value,
                            user_note: document.getElementById('user_note').value
                        };

                        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(formData));
                        console.log('💾 Đã lưu form:', formData);

                        var returnUrl = window.location.origin
                                + window.location.pathname
                                + '#contact';

                        var loginUrl = '${pageContext.request.contextPath}/login'
                                + '?returnUrl=' + encodeURIComponent(returnUrl);

                        console.log('➡️ Chuyển đến:', loginUrl);
                        window.location.href = loginUrl;
                    });
                } else {
                    console.log('👤 Đã login → Form submit bình thường');
                }

                // ===== BƯỚC 2: Khôi phục dữ liệu form khi quay lại =====
                var saved = sessionStorage.getItem(STORAGE_KEY);
                if (saved) {
                    try {
                        var data = JSON.parse(saved);
                        console.log('🔄 Khôi phục form:', data);

                        // ⭐ Chỉ khôi phục địa chỉ + ghi chú (không khôi phục user info)
                        if (data.user_address)
                            document.getElementById('user_address').value = data.user_address;
                        if (data.user_note)
                            document.getElementById('user_note').value = data.user_note;

                        if (isLoggedIn) {
                            console.log('✅ Đã login + có dữ liệu → Scroll xuống form');
                            setTimeout(function () {
                                var contactSection = document.getElementById('contact');
                                if (contactSection) {
                                    contactSection.scrollIntoView({behavior: 'smooth', block: 'start'});
                                }
                            }, 500);
                        }
                    } catch (e) {
                        console.error('❌ Lỗi khôi phục form:', e);
                        sessionStorage.removeItem(STORAGE_KEY);
                    }
                }
            })();
        </script>

        <!-- ============ AI CHAT WIDGET ============ -->
        <div class="ai-chat-button" id="aiChatBtn" onclick="toggleAIChat()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
        </div>

        <div class="ai-chat-window" id="aiChatWindow">
            <div class="ai-chat-header">
                <div class="ai-chat-header-info">
                    <div class="ai-chat-avatar">AI</div>
                    <div>
                        <div class="ai-chat-title">Tư vấn viên AI</div>
                        <div class="ai-chat-status">Đang hoạt động</div>
                    </div>
                </div>
                <button class="ai-chat-close" onclick="toggleAIChat()">×</button>
            </div>

            <div class="ai-chat-body" id="aiChatBody">
                <div class="ai-msg ai-msg-bot">
                    Xin chào anh/chị! Em là tư vấn viên AI của FPT Telecom.
                    Anh/chị cần tư vấn gói cước nào ạ? 😊
                </div>
            </div>

            <div class="ai-chat-footer">
                <input type="text" id="aiChatInput" placeholder="Nhập câu hỏi..."
                       onkeypress="if (event.key === 'Enter')
                                   sendAIMessage()">
                <button onclick="sendAIMessage()">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="22" y1="2" x2="11" y2="13"/>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                    </svg>
                </button>
            </div>
        </div>

        <style>
            .ai-chat-button {
                position: fixed;
                bottom: 25px;
                right: 25px;
                width: 60px;
                height: 60px;
                background: linear-gradient(135deg, #f37021, #ff8c42);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
                box-shadow: 0 8px 25px rgba(243, 112, 33, 0.4);
                z-index: 9999;
                transition: all 0.3s;
                animation: chatPulse 2s infinite;
            }
            .ai-chat-button:hover {
                transform: scale(1.1);
                box-shadow: 0 12px 35px rgba(243, 112, 33, 0.6);
            }
            .ai-chat-button svg {
                width: 26px;
                height: 26px;
                stroke: white;
            }
            @keyframes chatPulse {
                0%, 100% {
                    box-shadow: 0 8px 25px rgba(243, 112, 33, 0.4), 0 0 0 0 rgba(243, 112, 33, 0.7);
                }
                50%      {
                    box-shadow: 0 8px 25px rgba(243, 112, 33, 0.4), 0 0 0 15px rgba(243, 112, 33, 0);
                }
            }

            .ai-chat-window {
                position: fixed;
                bottom: 100px;
                right: 25px;
                width: 380px;
                height: 550px;
                background: white;
                border-radius: 20px;
                box-shadow: 0 20px 60px rgba(0,0,0,0.2);
                display: none;
                flex-direction: column;
                z-index: 9998;
                overflow: hidden;
                animation: chatSlideIn 0.3s ease-out;
            }
            .ai-chat-window.active {
                display: flex;
            }
            @keyframes chatSlideIn {
                from {
                    opacity: 0;
                    transform: translateY(30px);
                }
                to   {
                    opacity: 1;
                    transform: translateY(0);
                }
            }

            .ai-chat-header {
                background: linear-gradient(135deg, #f37021, #ff8c42);
                color: white;
                padding: 16px 20px;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            .ai-chat-header-info {
                display: flex;
                align-items: center;
                gap: 12px;
            }
            .ai-chat-avatar {
                width: 42px;
                height: 42px;
                background: rgba(255,255,255,0.2);
                border: 2px solid rgba(255,255,255,0.4);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: 800;
                font-size: 14px;
            }
            .ai-chat-title {
                font-weight: 800;
                font-size: 15px;
            }
            .ai-chat-status {
                font-size: 11.5px;
                opacity: 0.9;
                display: flex;
                align-items: center;
                gap: 5px;
            }
            .ai-chat-status::before {
                content: "";
                width: 7px;
                height: 7px;
                background: #4ade80;
                border-radius: 50%;
                box-shadow: 0 0 8px #4ade80;
            }
            .ai-chat-close {
                background: rgba(255,255,255,0.2);
                border: none;
                color: white;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                font-size: 20px;
                cursor: pointer;
                transition: 0.2s;
            }
            .ai-chat-close:hover {
                background: rgba(255,255,255,0.35);
            }

            .ai-chat-body {
                flex: 1;
                padding: 18px;
                overflow-y: auto;
                background: #f8fafc;
                display: flex;
                flex-direction: column;
                gap: 12px;
            }
            .ai-msg {
                max-width: 80%;
                padding: 11px 15px;
                border-radius: 16px;
                font-size: 13.5px;
                line-height: 1.55;
                word-wrap: break-word;
            }
            .ai-msg-bot {
                background: white;
                color: #1a1a1a;
                border-bottom-left-radius: 4px;
                align-self: flex-start;
                box-shadow: 0 2px 8px rgba(0,0,0,0.06);
            }
            .ai-msg-user {
                background: linear-gradient(135deg, #f37021, #ff8c42);
                color: white;
                border-bottom-right-radius: 4px;
                align-self: flex-end;
            }
            .ai-msg-loading {
                align-self: flex-start;
                background: white;
                padding: 14px 18px;
                border-radius: 16px;
                border-bottom-left-radius: 4px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.06);
                display: flex;
                gap: 5px;
            }
            .ai-msg-loading span {
                width: 7px;
                height: 7px;
                background: #f37021;
                border-radius: 50%;
                animation: typing 1.4s infinite;
            }
            .ai-msg-loading span:nth-child(2) {
                animation-delay: 0.2s;
            }
            .ai-msg-loading span:nth-child(3) {
                animation-delay: 0.4s;
            }
            @keyframes typing {
                0%, 60%, 100% {
                    transform: translateY(0);
                    opacity: 0.5;
                }
                30%           {
                    transform: translateY(-6px);
                    opacity: 1;
                }
            }

            .ai-chat-footer {
                padding: 12px 15px;
                background: white;
                border-top: 1px solid #e5e7eb;
                display: flex;
                gap: 8px;
            }
            .ai-chat-footer input {
                flex: 1;
                padding: 11px 15px;
                border: 1.5px solid #e5e7eb;
                border-radius: 25px;
                font-family: inherit;
                font-size: 13.5px;
                outline: none;
                transition: 0.2s;
            }
            .ai-chat-footer input:focus {
                border-color: #f37021;
                box-shadow: 0 0 0 3px rgba(243, 112, 33, 0.1);
            }
            .ai-chat-footer button {
                width: 42px;
                height: 42px;
                background: linear-gradient(135deg, #f37021, #ff8c42);
                border: none;
                border-radius: 50%;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: 0.2s;
            }
            .ai-chat-footer button:hover {
                transform: scale(1.08);
            }
            .ai-chat-footer button svg {
                width: 18px;
                height: 18px;
                stroke: white;
            }

            @media (max-width: 480px) {
                .ai-chat-window {
                    width: calc(100vw - 20px);
                    right: 10px;
                    height: 70vh;
                }
            }
        </style>

        <script>
            function toggleAIChat() {
                document.getElementById('aiChatWindow').classList.toggle('active');
            }

            function sendAIMessage() {
                const input = document.getElementById('aiChatInput');
                const body = document.getElementById('aiChatBody');
                const message = input.value.trim();
                if (!message)
                    return;

                body.innerHTML += '<div class="ai-msg ai-msg-user">' + escapeHtml(message) + '</div>';
                input.value = '';
                body.scrollTop = body.scrollHeight;

                const loadingId = 'loading-' + Date.now();
                body.innerHTML += '<div class="ai-msg-loading" id="' + loadingId + '"><span></span><span></span><span></span></div>';
                body.scrollTop = body.scrollHeight;

                fetch('${pageContext.request.contextPath}/ai-chat', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: 'message=' + encodeURIComponent(message)
                })
                        .then(res => res.json())
                        .then(data => {
                            document.getElementById(loadingId).remove();
                            body.innerHTML += '<div class="ai-msg ai-msg-bot">' + escapeHtml(data.reply).replace(/\n/g, '<br>') + '</div>';
                            body.scrollTop = body.scrollHeight;
                        })
                        .catch(err => {
                            document.getElementById(loadingId).remove();
                            body.innerHTML += '<div class="ai-msg ai-msg-bot">Xin lỗi, có lỗi xảy ra. Vui lòng thử lại.</div>';
                        });
            }

            function escapeHtml(text) {
                const div = document.createElement('div');
                div.textContent = text;
                return div.innerHTML;
            }
        </script>

        <script>
            function toggleUserMenu(event) {
                event.stopPropagation();
                const dropdown = document.getElementById('userDropdown');
                const avatar = document.querySelector('.user-avatar');

                dropdown.classList.toggle('active');
                avatar.classList.toggle('active');
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
        <script>
    // ===== HERO SLIDESHOW NÂNG CAO =====
    (function() {
        const slides = document.querySelectorAll('.hero-slide');
        const dots = document.querySelectorAll('.dot-slide');
        if (slides.length === 0) return;

        let currentIndex = 0;
        let autoTimer;
        const INTERVAL = 4000;

        function showSlide(index) {
            slides.forEach(s => s.classList.remove('active'));
            dots.forEach(d => d.classList.remove('active'));

            slides[index].classList.add('active');
            if (dots[index]) dots[index].classList.add('active');

            currentIndex = index;
        }

        function nextSlide() {
            showSlide((currentIndex + 1) % slides.length);
        }

        function prevSlide() {
            showSlide((currentIndex - 1 + slides.length) % slides.length);
        }

        function goToSlide(index) {
            showSlide(index);
            resetTimer();
        }

        function resetTimer() {
            clearInterval(autoTimer);
            autoTimer = setInterval(nextSlide, INTERVAL);
        }

        resetTimer();

        const slideshow = document.querySelector('.hero-slideshow');
        if (slideshow) {
            slideshow.addEventListener('mouseenter', () => clearInterval(autoTimer));
            slideshow.addEventListener('mouseleave', resetTimer);
        }

        window.nextSlide = nextSlide;
        window.prevSlide = prevSlide;
        window.goToSlide = goToSlide;
    })();
</script>
<script>
    document.getElementById('contactForm')?.addEventListener('submit', function(e) {
        const address = document.getElementById('user_address').value.trim();
        if (address === '') {
            e.preventDefault();
            alert('Vui lòng nhập địa chỉ lắp đặt!');
            document.getElementById('user_address').focus();
            return false;
        }
    });
</script>
    </body>
</html>