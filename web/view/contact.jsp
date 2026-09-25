<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Liên hệ - FPT Telecom</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
</head>
<body>

<!-- ============ TOP NAV ============ -->
<header class="top-nav">
    <div class="container">
        <a href="${pageContext.request.contextPath}/home" class="logo">
            <img src="${pageContext.request.contextPath}/assets/images/fpt-logo.jpg"
                 alt="FPT Telecom"
                 style="height: 55px; width: auto; object-fit: contain; display: block;">
        </a>
        <nav class="menu">
            <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
            <a href="${pageContext.request.contextPath}/home#packages">Bảng giá</a>
            <a href="${pageContext.request.contextPath}/home#contact">Tư vấn</a>
            <a href="${pageContext.request.contextPath}/contact" class="active">Liên hệ</a>
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
                    <strong>0932 079 469</strong>
                </div>
            </div>
            <c:choose>
                <c:when test="${empty sessionScope.user}">
                    <a href="${pageContext.request.contextPath}/login" class="btn-login-small">Đăng nhập</a>
                    <a href="${pageContext.request.contextPath}/register" class="btn-register-small">Đăng ký ngay</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/profile" class="btn-login-small">
                        ${sessionScope.user.displayName}
                    </a>
                    <a href="${pageContext.request.contextPath}/logout" class="btn-register-small">Đăng xuất</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>

<!-- ============ HERO CONTACT ============ -->
<div class="contact-hero">
    <div class="container">
        <div class="contact-hero-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
        </div>
        <h1>Liên Hệ Với Chúng Tôi</h1>
        <p>Chọn kênh liên hệ phù hợp — chúng tôi luôn sẵn sàng hỗ trợ</p>
    </div>
</div>

<!-- ============ SECTION LIÊN HỆ ============ -->
<section class="contact-info-section" id="lien-he">
    <div class="container">
        <div class="contact-info-grid">

            <!-- KHUNG 1: TƯ VẤN VIÊN -->
            <div class="contact-info-card">
                <div class="contact-info-icon icon-user">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                        <circle cx="12" cy="7" r="4"/>
                    </svg>
                </div>
                <div class="contact-info-content">
                    <div class="contact-info-label">TƯ VẤN VIÊN PHỤ TRÁCH</div>
                    <div class="contact-info-value">Bùi Nguyên Phúc</div>
                    <p class="contact-info-desc">
                        Nhân viên Kinh doanh &amp; CSKH — FPT Telecom.
                        Gọi trực tiếp để được tư vấn và lắp đặt nhanh nhất.
                    </p>
                </div>
            </div>

            <!-- KHUNG 2: SĐT / ZALO -->
            <div class="contact-info-card">
                <div class="contact-info-icon icon-phone">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                    </svg>
                </div>
                <div class="contact-info-content">
                    <div class="contact-info-label">SỐ ĐIỆN THOẠI / ZALO</div>
                    <div class="contact-info-value">
                        <a href="tel:0932079469">0932 079 469</a>
                    </div>
                    <p class="contact-info-desc">
                        Nhận cuộc gọi và tin nhắn Zalo trong giờ hành chính,
                        phản hồi ngoài giờ trong khả năng.
                    </p>
                </div>
            </div>

            <!-- KHUNG 3: ĐĂNG KÝ ONLINE -->
            <div class="contact-info-card">
                <div class="contact-info-icon icon-link">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
                        <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
                    </svg>
                </div>
                <div class="contact-info-content">
                    <div class="contact-info-label">ĐĂNG KÝ ONLINE</div>
                    <div class="contact-info-value">
                        <a href="https://fpt.vn/sale/PhucBN2-608" target="_blank">
                            fpt.vn/sale/PhucBN2-608 →
                        </a>
                    </div>
                    <p class="contact-info-desc">
                        Đăng ký qua link này để đơn hàng được gắn đúng về Phúc,
                        đảm bảo bạn được hỗ trợ xuyên suốt từ lắp đặt đến sau bán.
                    </p>
                </div>
            </div>

            <!-- KHUNG 4: HOTLINE KỸ THUẬT -->
            <div class="contact-info-card">
                <div class="contact-info-icon icon-support">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
                        <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
                    </svg>
                </div>
                <div class="contact-info-content">
                    <div class="contact-info-label">HOTLINE HỖ TRỢ KỸ THUẬT (CHUNG)</div>
                    <div class="contact-info-value">
                        <a href="tel:19006600">1900 6600</a>
                    </div>
                    <p class="contact-info-desc">
                        Dùng khi cần báo sự cố ngoài giờ hoặc không liên lạc được với tư vấn viên.
                    </p>
                </div>
            </div>

        </div>

        <!-- NÚT QUAY LẠI -->
        <div style="text-align: center; margin-top: 40px;">
            <a href="${pageContext.request.contextPath}/home"
               style="display: inline-flex; align-items: center; gap: 8px;
                      padding: 12px 28px; background: white; color: #f37021;
                      border: 2px solid #f37021; border-radius: 30px;
                      font-weight: 800; font-size: 14px; text-decoration: none;
                      transition: all 0.25s;">
                ← Quay lại trang chủ
            </a>
        </div>
    </div>
</section>

<!-- ============ FOOTER ============ -->
<footer class="footer">
    <div class="container">
        <div class="footer-bottom" style="padding: 30px 0;">
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

</body>
</html>