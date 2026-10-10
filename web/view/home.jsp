<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- =========================================================================
     TRANG CHỦ FPT TELECOM - HOME.JSP
     =========================================================================
     MỤC LỤC / TABLE OF CONTENTS:
     01. META & STYLESHEETS        - Imports CSS, Fonts, Address Picker
     02. SHARED WIDGETS            - Runner Loader, Nav Slider, Theme Toggle
     03. TOP NAVIGATION HEADER     - Header dùng chung (/view/customer/customer-header.jsp)
     04. HERO BANNER & SLIDESHOW   - Banner công nghệ WiFi 6 & Neon Glow FX
     05. STATS COUNTER BAR         - Khối số liệu ấn tượng (Khách hàng, hạ tầng)
     06. PACKAGES SECTION (#packages) - Bảng giá Internet, FPT Play & Camera
     07. FEATURES & ADVANTAGES     - Ưu điểm vượt trội của mạng FPT
     08. CONSULTATION FORM (#contact) - Form đăng ký tư vấn lắp đặt & Address Picker
     09. FOOTER SECTION            - Chân trang, hotline hỗ trợ, bản quyền
     10. FLOATING AI CHAT WIDGET   - Khung chat tư vấn AI tự động
     11. PAGE SCRIPTS              - Slideshow controller, Smooth scrolling
     ========================================================================= --%>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>FPT Telecom - Internet, Truyền hình, Camera</title>
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/ai-chat.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/pkg-card.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/fonts.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/hero-stars.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/theme-toggle.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/performance-optimize.css">
        <script src="${pageContext.request.contextPath}/js/address-data.js"></script>
        <script src="${pageContext.request.contextPath}/js/address-picker.js" defer></script>
        <script src="${pageContext.request.contextPath}/js/tech-text.js"></script>
    </head>
    <body>
        <!-- ⭐ LOADING RUNNER -->
        <jsp:include page="/view/loading-runner.jsp" />
        <jsp:include page="/view/nav-slider.jsp" />
        <jsp:include page="/view/theme-toggle.jsp" />

        <!-- ============ TOP NAV ============ -->
        <jsp:include page="/view/customer/customer-header.jsp">
            <jsp:param name="isHomePage" value="true" />
            <jsp:param name="activeMenu" value="home" />
        </jsp:include>

        <!-- ============ HERO ============ -->
        <section class="hero">
            <!-- ⭐ BẦU TRỜI SAO — chỉ hiện khi dark mode -->
            <div class="hero-stars">
                <div class="stars stars-1"></div>
                <div class="stars stars-2"></div>
                <div class="stars stars-3"></div>
                <div class="meteor m1"></div>
                <div class="meteor m2"></div>
                <div class="meteor m3"></div>
                <div class="moon"></div>
            </div>

            <div class="container">
                <div class="hero-text">
                    <c:if test="${not empty sessionScope.user && sessionScope.user.role == 'customer'}">
                        <div class="welcome-banner">
                            <span class="welcome-line-1">Chào mừng trở lại,</span>
                            <span class="welcome-line-2">${sessionScope.user.fullName}!</span>
                        </div>
                    </c:if>

                    <!-- ⭐ TECH TEXT — Hiệu ứng chữ tương tác "FPT WIFI 6" -->
                    <div class="tech-text-wrap" id="techTextContainer"></div>

                    <!-- ⭐ TEXT EFFECT — Chữ vẽ nét rồi phát sáng neon cam -->
                    <div class="text-hover-wrap">
                        <svg class="text-hover-svg" viewBox="0 0 920 280" xmlns="http://www.w3.org/2000/svg">
                            <defs>
                                <linearGradient id="textGradientOrange" x1="0%" y1="0%" x2="100%" y2="0%">
                                    <stop offset="0%" stop-color="#ffa54f"/>
                                    <stop offset="50%" stop-color="#ff7a18"/>
                                    <stop offset="100%" stop-color="#ffa54f"/>
                                </linearGradient>
                            </defs>

                            <!-- ⭐ DÒNG 1: "TỐC ĐỘ CAO" -->
                            <text x="50%" y="75" text-anchor="middle" dominant-baseline="central"
                                  stroke-width="1.2" stroke="#ffffff" fill="none"
                                  font-family="'Be Vietnam Pro', sans-serif" font-size="105" font-weight="900"
                                  class="th-draw">
                                TỐC ĐỘ CAO
                            </text>
                            <text x="50%" y="75" text-anchor="middle" dominant-baseline="central"
                                  fill="url(#textGradientOrange)"
                                  stroke="#ffa54f"
                                  stroke-width="1"
                                  paint-order="stroke fill"
                                  font-family="'Be Vietnam Pro', sans-serif" font-size="105" font-weight="900"
                                  class="th-main">
                                TỐC ĐỘ CAO
                            </text>

                            <!-- ⭐ DÒNG 2: "HỖ TRỢ 24/7" (Tách xa dòng 1 tránh cấn dấu ngã/mũ) -->
                            <text x="50%" y="215" text-anchor="middle" dominant-baseline="central"
                                  stroke-width="1.2" stroke="#ffffff" fill="none"
                                  font-family="'Be Vietnam Pro', sans-serif" font-size="105" font-weight="900"
                                  class="th-draw">
                                HỖ TRỢ 24/7
                            </text>
                            <text x="50%" y="215" text-anchor="middle" dominant-baseline="central"
                                  fill="url(#textGradientOrange)"
                                  stroke="#ffa54f"
                                  stroke-width="1"
                                  paint-order="stroke fill"
                                  font-family="'Be Vietnam Pro', sans-serif" font-size="105" font-weight="900"
                                  class="th-main">
                                HỖ TRỢ 24/7
                            </text>
                        </svg>
                    </div>  
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
                        <img src="${pageContext.request.contextPath}/assets/images/Hero4.png"
                             alt="FPT Camera" class="hero-slide">
                        <img src="${pageContext.request.contextPath}/assets/images/Hero5.png"
                             alt="FPT Camera" class="hero-slide">
                        <img src="${pageContext.request.contextPath}/assets/images/Hero6.png"
                             alt="FPT Camera" class="hero-slide">
                        <img src="${pageContext.request.contextPath}/assets/images/Hero7.png"
                             alt="FPT Camera" class="hero-slide">

                        <%-- ⭐ NÚT CHUYỂN SLIDE --%>
                        <button class="hero-nav hero-nav-prev" onclick="prevSlide()" aria-label="Previous">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="15 18 9 12 15 6"/>
                            </svg>
                        </button>
                        <button class="hero-nav hero-nav-next" onclick="nextSlide()" aria-label="Next">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="9 18 15 12 9 6"/>
                            </svg>
                        </button>

                        <%-- ⭐ DOTS INDICATOR --%>
                        <div class="hero-dots">
                            <button class="dot-slide active" onclick="goToSlide(0)" aria-label="Slide 1"></button>
                            <button class="dot-slide" onclick="goToSlide(1)" aria-label="Slide 2"></button>
                            <button class="dot-slide" onclick="goToSlide(2)" aria-label="Slide 3"></button>
                            <button class="dot-slide" onclick="goToSlide(3)" aria-label="Slide 4"></button>
                            <button class="dot-slide" onclick="goToSlide(4)" aria-label="Slide 5"></button>
                            <button class="dot-slide" onclick="goToSlide(5)" aria-label="Slide 6"></button>
                            <button class="dot-slide" onclick="goToSlide(6)" aria-label="Slide 7"></button>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- ============ SECTION TITLE: PACKAGES ============ -->
        <div class="section-title">           
            <h2>${packages.size()} GÓI CƯỚC WIFI FPT TỐC ĐỘ CAO BÁN CHẠY NHẤT</h2>           
        </div>

        <!-- ============ GÓI CƯỚC — LOAD ĐỘNG TỪ DB ============ -->
        <div class="container" id="packages">
            <div class="packages">
                <c:forEach items="${packages}" var="pkg">
                    <div class="pkg-wrap">
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
                            <div class="pkg-speed-badge" style="text-align: center; margin: -2px 0 6px;">
                                <span style="display: inline-flex; align-items: center; gap: 5px; background: rgba(243, 112, 33, 0.1); color: #f37021; font-weight: 800; font-size: 13px; padding: 3px 12px; border-radius: 999px; border: 1px solid rgba(243, 112, 33, 0.25);">
                                    <svg style="width: 14px; height: 14px;" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                                    Tốc độ ${pkg.speedMbps} Mbps
                                </span>
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
                </div>
                </c:forEach>

                <c:if test="${empty packages}">
                    <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: #64748b;">
                        <p>Chưa có gói cước nào. Vui lòng thêm gói trong trang quản trị.</p>
                    </div>
                </c:if>
            </div>
        </div>

        
        <!-- ============ SECTION MESH WIFI ============ -->
        <div class="section-title" id="mesh-wifi" style="margin-top: 35px;">
            <h2>MỞ RỘNG PHỦ SÓNG TOÀN DIỆN VỚI MESH WIFI FPT</h2>
            <p style="color: #64748b; font-size: 14.5px; max-width: 680px; margin: 10px auto 0; text-align: center; line-height: 1.6;">
                Giải pháp mở rộng phủ sóng WiFi xuyên tầng cho nhà ống, nhà phố nhiều tầng — loại bỏ hoàn toàn góc chết sóng WiFi.
            </p>
        </div>

        <div class="container mesh-section-wrap">
            <div class="mesh-grid">
                <!-- CARD MESH F1 -->
                <div class="mesh-card-wrap">
                    <div class="mesh-card__border"></div>
                    <div class="mesh-card">
                        <div class="mesh-card-header">
                            <div>
                                <h3>Mesh WiFi F1</h3>
                                <div class="mesh-subtitle">Giải pháp phủ sóng cho nhà 2 tầng</div>
                            </div>
                            <span class="mesh-tag">Nhà 2 tầng</span>
                        </div>
                        <hr class="mesh-line" />

                        <div class="mesh-price-box">
                            <div class="mesh-monthly">
                                <span class="mesh-plus">+</span>
                                <span class="mesh-amount">10.000</span>
                                <span class="mesh-unit">đ/tháng</span>
                            </div>
                            <div class="mesh-note-fee">
                                Phí lắp đặt: <strong>500.000 VNĐ</strong>
                            </div>
                        </div>

                        <ul class="mesh-specs">
                            <li>
                                <span class="mesh-check">
                                    <svg class="mesh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                                </span>
                                <span><strong>Nhà phù hợp:</strong> Nhà 2 tầng</span>
                            </li>
                            <li>
                                <span class="mesh-check">
                                    <svg class="mesh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>
                                </span>
                                <span><strong>Thiết bị:</strong> 1 Modem WiFi 6 + 1 Access Point</span>
                            </li>
                            <li>
                                <span class="mesh-check">
                                    <svg class="mesh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                                </span>
                                <span class="mesh-note-inline"><em>Lưu ý: Tùy chọn mở rộng phủ sóng cộng thêm vào tiền cước gói Internet hàng tháng.</em></span>
                            </li>
                        </ul>

                        <button type="button" class="btn-mesh" onclick="selectMeshOption('Mesh WiFi F1 (Nhà 2 tầng)')">
                            Đăng ký tư vấn Mesh F1 →
                        </button>
                    </div>
                </div>

                <!-- CARD MESH F2 -->
                <div class="mesh-card-wrap mesh-featured-wrap">
                    <div class="mesh-card__border"></div>
                    <div class="mesh-card mesh-featured">
                        <div class="mesh-card-header">
                            <div>
                                <h3>Mesh WiFi F2</h3>
                                <div class="mesh-subtitle">Giải pháp phủ sóng cho nhà 3 tầng</div>
                            </div>
                            <span class="mesh-tag">Nhà 3 tầng</span>
                        </div>
                        <hr class="mesh-line" />

                        <div class="mesh-price-box">
                            <div class="mesh-monthly">
                                <span class="mesh-plus">+</span>
                                <span class="mesh-amount">20.000</span>
                                <span class="mesh-unit">đ/tháng</span>
                            </div>
                            <div class="mesh-note-fee">
                                Phí lắp đặt: <strong>700.000 VNĐ</strong>
                            </div>
                        </div>

                        <ul class="mesh-specs">
                            <li>
                                <span class="mesh-check">
                                    <svg class="mesh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                                </span>
                                <span><strong>Nhà phù hợp:</strong> Nhà 3 tầng</span>
                            </li>
                            <li>
                                <span class="mesh-check">
                                    <svg class="mesh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>
                                </span>
                                <span><strong>Thiết bị:</strong> 1 Modem WiFi 6 + 2 Access Point</span>
                            </li>
                            <li>
                                <span class="mesh-check">
                                    <svg class="mesh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                                </span>
                                <span class="mesh-note-inline"><em>Lưu ý: Tùy chọn mở rộng phủ sóng cộng thêm vào tiền cước gói Internet hàng tháng.</em></span>
                            </li>
                        </ul>

                        <button type="button" class="btn-mesh" onclick="selectMeshOption('Mesh WiFi F2 (Nhà 3 tầng)')">
                            Đăng ký tư vấn Mesh F2 →
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ FEATURES ============ -->
        <div class="section-title">
            <h2>Cam Kết Chất Lượng Dịch Vụ Hàng Đầu FPT</h2>            
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
                        <h4>Tài Khoản FPT Play Vip</h4>
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
                    <h2><span>Liên hệ tư vấn khảo sát và nhận ưu đãi có hạn </span></h2>                 

                    <form class="contact-form"
                          action="${pageContext.request.contextPath}/ContactServlet"
                          method="POST"
                          id="contactForm">
                        <input type="hidden" name="registration_token" value="${registrationToken}">

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

                        <!-- ⭐ ĐỊA CHỈ: CASCADING DROPDOWN -->
                        <div class="full">
                            <label style="font-size:13px;font-weight:700;color:#1e293b;text-transform:uppercase;margin-bottom:10px;display:block;">
                                Địa chỉ lắp đặt <span style="color:#f37021;">*</span>
                            </label>
                            <div id="addressPickerHome" data-address-picker="home"></div>
                        </div>
                        <div class="full">
                            <label>Ghi chú (không bắt buộc)</label>
                            <input type="text" name="user_note" id="user_note"
                                   placeholder="Ví dụ: nhà 3 tầng, dùng nhiều camera..."
                                   list="noteSuggestionsHome"
                                   autocomplete="off">
                            <datalist id="noteSuggestionsHome">
                                <option value="Thêm gói Mesh WiFi F1 (nhà 2 tầng - +10.000đ/tháng)">
                                <option value="Thêm gói Mesh WiFi F2 (nhà 3 tầng - +20.000đ/tháng)">
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
                        </div>
                        <label class="full"><input type="checkbox" name="registration_consent" value="yes" style="width:auto;padding:0;margin-right:6px;" required> Tôi đồng ý gửi thông tin để FPT liên hệ tư vấn gói cước.</label>
                        <button type="submit">Đăng ký tư vấn ngay →</button>
                    </form>
                </div>
            </div>
        </section>

        <!-- ============ FOOTER ============ -->
        <footer class="footer" id="contact-info">
            <div class="container">

                <div class="footer-top">

                    <!-- Cột 1: Logo + Mô tả -->
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
                            Công ty Cổ phần Viễn thông FPT — Nhà cung cấp dịch vụ Internet tốc độ cao.                           
                        </p>
                    </div>

                    <!-- Cột 2: Liên hệ -->
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
                                    94 Phạm Hùng, Quy Nhơn, Gia Lai.
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
                            <strong>0932 079 469</strong>
                        </div>
                    </div>

                    <div class="copyright">
                        <strong>Copyright © 2024 Cơ quan chủ quản: Công Ty Cổ Phần Viễn Thông FPT </strong>

                    </div>

                </div>

            </div>
        </footer>

        <!-- ============ SCRIPT CHUNG ============ -->
        <script>
            // ===== SCROLL REVEAL =====
            const revealElements = document.querySelectorAll(
                    '.section-title, .pkg, .mesh-card, .feature, .device, .contact-wrapper'
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
            document.querySelectorAll('.btn-pkg, .btn-mesh, .btn-device, .btn-hero-primary, .contact-form button').forEach(btn => {
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


            window.addEventListener('resize', function () {
                if (window.innerWidth > 900) closeMobileMenu();
            });

            document.addEventListener('keydown', function (e) {
                if (e.key === 'Escape') closeMobileMenu();
            });
        </script>

        <!-- ============ SCRIPT: LƯU FORM TƯ VẤN + CHUYỂN LOGIN ============ -->
        <script>
            (function () {
                var contactForm = document.getElementById('contactForm');
                if (!contactForm) return;

                var isLoggedIn = '${not empty sessionScope.user}' === 'true';
                var STORAGE_KEY = 'contactFormBackup';

                if (!isLoggedIn) {
                    contactForm.addEventListener('submit', function (e) {
                        e.preventDefault();

                        var finalAddr = document.querySelector('.addr-final[data-prefix="home"]');
                        var formData = {
                            user_name: document.getElementById('user_name').value,
                            user_phone: document.getElementById('user_phone').value,
                            user_email: document.getElementById('user_email').value,
                            user_address: finalAddr ? finalAddr.value : '',
                            user_note: document.getElementById('user_note').value
                        };

                        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(formData));

                        var returnUrl = window.location.origin
                                + window.location.pathname
                                + '#contact';

                        var loginUrl = '${pageContext.request.contextPath}/login'
                                + '?returnUrl=' + encodeURIComponent(returnUrl);

                        window.location.href = loginUrl;
                    });
                }

                var saved = sessionStorage.getItem(STORAGE_KEY);
                if (saved) {
                    try {
                        var data = JSON.parse(saved);
                        if (data.user_note)
                            document.getElementById('user_note').value = data.user_note;
                        if (isLoggedIn) {
                            setTimeout(function () {
                                var contactSection = document.getElementById('contact');
                                if (contactSection) {
                                    contactSection.scrollIntoView({behavior: 'smooth', block: 'start'});
                                }
                            }, 500);
                        }
                        sessionStorage.removeItem(STORAGE_KEY);
                    } catch (e) {
                        sessionStorage.removeItem(STORAGE_KEY);
                    }
                }
            })();

            // ============ VALIDATE FORM CONTACT ============
            document.getElementById('contactForm')?.addEventListener('submit', function (e) {
                var prefix = 'home';
                var mode = window['getAddressMode_' + prefix] ? window['getAddressMode_' + prefix]() : 'new';

                if (mode === 'manual') {
                    var manualInput = document.querySelector('.addr-manual-input[data-prefix="' + prefix + '"]');
                    if (!manualInput || manualInput.value.trim() === '') {
                        e.preventDefault();
                        alert('Vui lòng nhập địa chỉ lắp đặt đầy đủ!');
                        if (manualInput) manualInput.focus();
                        return false;
                    }
                    return true;
                }

                if (mode === 'old') {
                    var provOld = document.querySelector('.addr-province-old[data-prefix="' + prefix + '"]');
                    var distOld = document.querySelector('.addr-district-old[data-prefix="' + prefix + '"]');
                    var wardOld = document.querySelector('.addr-ward-old[data-prefix="' + prefix + '"]');
                    var detailOld = document.querySelector('.addr-detail-old[data-prefix="' + prefix + '"]');

                    if (!provOld || !provOld.value) {
                        e.preventDefault(); alert('Vui lòng chọn Tỉnh/Thành phố!'); if (provOld) provOld.focus(); return false;
                    }
                    if (!distOld || !distOld.value) {
                        e.preventDefault(); alert('Vui lòng chọn Quận/Huyện!'); if (distOld) distOld.focus(); return false;
                    }
                    if (!wardOld || !wardOld.value) {
                        e.preventDefault(); alert('Vui lòng chọn Phường/Xã!'); if (wardOld) wardOld.focus(); return false;
                    }
                    if (!detailOld || detailOld.value.trim() === '') {
                        e.preventDefault(); alert('Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường...)!'); if (detailOld) detailOld.focus(); return false;
                    }
                    return true;
                }

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
                    alert('Vui lòng chọn Tỉnh/Thành phố!');
                    if (provNew && provNew.value === 'custom' && customProvNew) customProvNew.focus();
                    else if (provNew) provNew.focus();
                    return false;
                }
                if (!wardVal) {
                    e.preventDefault();
                    alert('Vui lòng chọn Phường/Xã!');
                    if (wardNew && wardNew.value === 'custom' && customWardNew) customWardNew.focus();
                    else if (wardNew) wardNew.focus();
                    return false;
                }
                if (!detailNew || detailNew.value.trim() === '') {
                    e.preventDefault();
                    alert('Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường...)!');
                    if (detailNew) detailNew.focus();
                    return false;
                }
            });
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

        <script src="${pageContext.request.contextPath}/js/ai-chat.js"></script>
        <script>FptChat.init('${pageContext.request.contextPath}');</script>


        <script>
            // ===== HERO SLIDESHOW NÂNG CAO =====
            (function () {
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
                    resetTimer();
                }

                function prevSlide() {
                    showSlide((currentIndex - 1 + slides.length) % slides.length);
                    resetTimer();
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

                    // ⭐ Touch swipe cho mobile
                    let touchStartX = 0;
                    let touchEndX = 0;
                    slideshow.addEventListener('touchstart', function(e) {
                        touchStartX = e.changedTouches[0].screenX;
                    }, { passive: true });

                    slideshow.addEventListener('touchend', function(e) {
                        touchEndX = e.changedTouches[0].screenX;
                        const diff = touchEndX - touchStartX;
                        if (Math.abs(diff) > 40) {
                            if (diff < 0) {
                                nextSlide();
                            } else {
                                prevSlide();
                            }
                        }
                    }, { passive: true });
                }

                window.nextSlide = nextSlide;
                window.prevSlide = prevSlide;
                window.goToSlide = goToSlide;
            })();
        </script>
        <script>
            // ⭐ Scroll tới section mà KHÔNG redirect
            function scrollToSection(event, sectionId) {
                event.preventDefault();
                const target = document.getElementById(sectionId);
                if (target) {
                    target.scrollIntoView({ 
                        behavior: 'smooth', 
                        block: 'start' 
                    });
                }
                // Cập nhật URL hash mà KHÔNG reload
                history.pushState(null, '', '#' + sectionId);
                // ⭐ Trigger hashchange cho nav-slider cập nhật active
                window.dispatchEvent(new HashChangeEvent('hashchange'));
            }
        </script>
        <script>// ⭐ Về trang chủ — xóa hash cũ
function goHome(event) {
    event.preventDefault();
    // Xóa hash khỏi URL
    history.pushState(null, '', window.location.pathname);
    // Scroll lên đầu
    window.scrollTo({ top: 0, behavior: 'smooth' });
    // Trigger hashchange để nav-slider cập nhật
    window.dispatchEvent(new HashChangeEvent('hashchange'));
}
</script>
        <script>
            document.addEventListener('DOMContentLoaded', function () {
                const container = document.getElementById('techTextContainer');
                if (container && window.TechText) {
                    window.TechText.init(container, {
                        text: 'FPT WIFI 6',
                        fontFamily: "'Orbitron', 'Plus Jakarta Sans', sans-serif",
                        fontSize: 600,
                        fontWeight: 900,
                        letterSpacing: 0.04,
                        color: '#ffffff',
                        accentColor: '#ff9933',
                        reach: 195,
                        softness: 0.6,
                        specks: 18,
                        selection: true,
                        labels: true,
                        draggable: true,
                        sweep: true,
                        speed: 0.6
                    });
                }
            });
        </script>
    <script>
function selectMeshOption(meshName) {
    const contactSection = document.getElementById('contact');
    const noteInput = document.getElementById('user_note');
    if (noteInput) {
        if (!noteInput.value.includes(meshName)) {
            noteInput.value = noteInput.value ? noteInput.value + ' | Đăng ký kèm ' + meshName : 'Đăng ký kèm ' + meshName;
        }
    }
    if (contactSection) {
        contactSection.scrollIntoView({ behavior: 'smooth' });
        const nameInput = document.getElementById('user_name');
        if (nameInput) setTimeout(() => nameInput.focus(), 600);
    }
}
</script>
</body>
</html>
