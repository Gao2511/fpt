<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- ⭐ FALLBACK NẾU DATA NULL --%>
<c:if test="${empty pkg.name}">
    <c:set var="pkgName" value="Gói cước FPT"/>
</c:if>
<c:if test="${not empty pkg.name}">
    <c:set var="pkgName" value="${pkg.name}"/>
</c:if>

<c:if test="${empty pkg.description}">
    <c:set var="pkgDesc" value="Gói cước Internet tốc độ cao FPT Telecom"/>
</c:if>
<c:if test="${not empty pkg.description}">
    <c:set var="pkgDesc" value="${pkg.description}"/>
</c:if>

<c:if test="${empty pkg.packageCode}">
    <c:set var="pkgCode" value="FPT"/>
</c:if>
<c:if test="${not empty pkg.packageCode}">
    <c:set var="pkgCode" value="${pkg.packageCode}"/>
</c:if>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${pkgName} - Chi tiết gói cước FPT</title>
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/customer/package-detail.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/performance-optimize.css">
        <!-- ⭐ Font Inter + Poppins cho trang package-detail -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&family=Poppins:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">
        <script src="${pageContext.request.contextPath}/js/address-data.js"></script>
        <script src="${pageContext.request.contextPath}/js/address-picker.js" defer></script>
    </head>
    <body>
        <jsp:include page="/view/loading-runner.jsp" />
        <jsp:include page="/view/nav-slider.jsp" />
        <jsp:include page="/view/theme-toggle.jsp" />

        <!-- TOP NAV -->
        <!-- ============ TOP NAV (DÙNG CHUNG) ============ -->
<jsp:include page="/view/customer/customer-header.jsp">
    <jsp:param name="activeMenu" value="packages"/>
</jsp:include>

        <!-- BREADCRUMB -->
        <div class="breadcrumb-wrap">
            <div class="container">
                <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
                <span>›</span>
                <a href="${pageContext.request.contextPath}/home#packages">Bảng giá</a>
                <span>›</span>
                <strong>${pkgName}</strong>
            </div>
        </div>

        <!-- MAIN -->
        <main class="detail-main">
            <div class="container">

                <!-- ============ HERO GÓI — HIỆU ỨNG ÁNH SÁNG CAM ============ -->
                <div class="pkg-hero-glow">
                    <div class="pkg-hero-card">
                        <!-- RAY SÁNG CHÉO -->
                        <div class="ray"></div>

                        <!-- QUẦNG SÁNG GÓC PHẢI -->
                        <div class="glow-orb"></div>

                        <!-- KHUNG LINE TRẮNG + DOT CHẠY QUANH -->
                        <div class="frame">
                            <div class="dot"></div>
                        </div>
                        <div class="frame-corner"></div>

                        <!-- NỘI DUNG BÊN TRÁI -->
                        <div class="pkg-hero-content">
                            <%-- ⭐ BADGE — chỉ hiện khi có hot/featured --%>
                            <c:if test="${pkg.badgeType == 'hot'}">
                                <span class="pkg-hero-badge pkg-hero-badge-hot">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
                                    </svg>
                                    HOT
                                </span>
                            </c:if>

                            <c:if test="${pkg.badgeType == 'featured'}">
                                <span class="pkg-hero-badge pkg-hero-badge-featured">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                                    </svg>
                                    NỔI BẬT
                                </span>
                            </c:if>

                            <%-- ⭐ Gói thường → KHÔNG hiển thị gì cả --%>

                            <h1>${pkgName}</h1>
                            <p class="pkg-hero-desc">${pkgDesc}</p>

                            <div class="pkg-hero-price">
                                <span class="price-num">
                                    <c:choose>
                                        <c:when test="${not empty pkg.price and pkg.price > 0}">
                                            <fmt:formatNumber value="${pkg.price}" pattern="#,###"/>
                                        </c:when>
                                        <c:otherwise>0</c:otherwise>
                                    </c:choose>
                                </span>
                                <span class="price-unit">đ/tháng</span>
                            </div>

                            <div class="pkg-hero-actions">
                                <a href="tel:0932079469" class="btn-glow-primary">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                                    </svg>
                                    Gọi tư vấn ngay
                                </a>
                                <a href="${pageContext.request.contextPath}/home#contact" class="btn-glow-secondary">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M9 11l3 3L22 4"/>
                                        <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
                                    </svg>
                                    Đăng ký lắp đặt
                                </a>
                            </div>
                        </div>

                        <!-- BADGE TỐC ĐỘ BÊN PHẢI -->
                        <div class="pkg-hero-speed">
                            <span class="speed-label">Tốc độ</span>
                            <span class="speed-value">
                                <c:choose>
                                    <c:when test="${not empty pkg.speedMbps and pkg.speedMbps > 0}">
                                        ${pkg.speedMbps} Mbps
                                    </c:when>
                                    <c:otherwise>Đang cập nhật</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
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
                        Đăng ký gói "${pkgName}"
                    </h2>

                    <%-- GỢI Ý ĐĂNG NHẬP --%>
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

                    <form action="${pageContext.request.contextPath}/ContactServlet" method="POST" class="quick-contact-form" id="registration">
                        <input type="hidden" name="registration_token" value="${registrationToken}">
                        <input type="hidden" name="user_package_id" value="${pkg.id}">
                        <input type="hidden" name="user_package" value="${pkgName}">

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

                        <div id="addressPickerDetail" data-address-picker="detail" style="margin-bottom:12px;"></div>

                        <!-- TÙY CHỌN MỞ RỘNG MESH WIFI -->
                        <div style="background:#fff7ed; border:1px solid #fed7aa; border-radius:10px; padding:12px 14px; margin-bottom:14px;">
                            <div style="font-size:13px; font-weight:700; color:#c2410c; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                                <svg style="width:16px;height:16px;" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M5 12.55a11 11 0 0 1 14.08 0"/><path d="M1.42 9a16 16 0 0 1 21.16 0"/><path d="M8.53 16.11a6 6 0 0 1 6.95 0"/><line x1="12" y1="20" x2="12.01" y2="20"/></svg>
                                Tùy chọn mở rộng vùng phủ sóng Mesh WiFi (phí cộng thêm hàng tháng):
                            </div>
                            <div style="display:flex; flex-direction:column; gap:8px; font-size:13px; color:#374151;">
                                <label style="display:flex; align-items:center; gap:8px; cursor:pointer; font-weight:400; margin:0;">
                                    <input type="checkbox" id="optMeshF1" onchange="toggleMeshOption('Mesh WiFi F1 (Nhà 2 tầng, +100K/tháng)')" style="width:auto; margin:0;">
                                    <span><strong>Mesh WiFi F1</strong> — Nhà 2 tầng (1 Modem WiFi 6 + 1 AP, phí lắp 500k, <strong>+100.000đ/tháng</strong>)</span>
                                </label>
                                <label style="display:flex; align-items:center; gap:8px; cursor:pointer; font-weight:400; margin:0;">
                                    <input type="checkbox" id="optMeshF2" onchange="toggleMeshOption('Mesh WiFi F2 (Nhà 3 tầng, +200K/tháng)')" style="width:auto; margin:0;">
                                    <span><strong>Mesh WiFi F2</strong> — Nhà 3 tầng (1 Modem WiFi 6 + 2 AP, phí lắp 700k, <strong>+200.000đ/tháng</strong>)</span>
                                </label>
                            </div>
                        </div>
                        <input type="text" name="user_note" id="user_note_detail"
                               placeholder="Ghi chú (không bắt buộc)"
                               list="noteSuggestionsDetail"
                               autocomplete="off">
                        <datalist id="noteSuggestionsDetail">
                            <option value="Thêm gói Mesh WiFi F1 (nhà 2 tầng - thêm 100K/tháng)">
                            <option value="Thêm gói Mesh WiFi F2 (nhà 3 tầng - thêm 200K/tháng)">
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
                        <label style="display:block;margin:12px 0;"><input type="checkbox" name="registration_consent" value="yes" style="width:auto;padding:0;margin-right:6px;" required> Tôi đồng ý gửi thông tin để FPT liên hệ tư vấn gói cước.</label>
                        <button type="submit">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z"/>
                            <path d="M12 15l-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z"/>
                            <path d="M9 12H4s.55-3.03 2-4c1.62-1.08 5 0 5 0"/>
                            <path d="M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5"/>
                            </svg>
                            Đăng ký ngay - Nhận tư vấn 
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
                            Công ty Cổ phần Viễn thông FPT — Nhà cung cấp dịch vụ Internet tốc độ cao.             
                        </p>
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
                                    94 Phạm Hùng, Quy Nhơn, Gia Lai.
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
                            <strong>0932 079 469</strong>
                        </div>
                    </div>

                    <div class="copyright">
                        <strong>Copyright © 2024 Cơ quan chủ quản: Công Ty Cổ Phần Viễn Thông FPT</strong>
                    </div>

                </div>

            </div>
        </footer>

        <!-- SCRIPTS -->


        <script>
            (function () {
                var quickForm = document.querySelector('.quick-contact-form');
                if (!quickForm) return;

                quickForm.addEventListener('submit', function (e) {
                    var nameInput = document.getElementById('user_name');
                    if (nameInput.value.trim() === '') {
                        e.preventDefault();
                        showError(nameInput, 'Vui lòng nhập họ tên người cần lắp đặt!');
                        return false;
                    }

                    var phoneInput = document.getElementById('user_phone');
                    var phone = phoneInput.value.trim().replace(/[\s.\-]/g, '');
                    if (!/^[0-9]{10,11}$/.test(phone)) {
                        e.preventDefault();
                        showError(phoneInput, 'Số điện thoại không hợp lệ! Vui lòng nhập 10-11 chữ số.');
                        return false;
                    }

                    var emailInput = document.getElementById('user_email');
                    var email = emailInput.value.trim();
                    if (email !== '' && !/^[A-Za-z0-9+_.-]+@(.+)$/.test(email)) {
                        e.preventDefault();
                        showError(emailInput, 'Email không hợp lệ!');
                        return false;
                    }
                });

                function showError(input, message) {
                    alert(message);
                    if (!input) return;
                    input.focus();
                    input.scrollIntoView({behavior: 'smooth', block: 'center'});
                    input.style.borderColor = '#dc2626';
                    input.style.boxShadow = '0 0 0 4px rgba(220, 38, 38, 0.15)';
                    input.addEventListener('input', function handler() {
                        input.style.borderColor = '';
                        input.style.boxShadow = '';
                        input.removeEventListener('input', handler);
                    });
                }
            })();
        </script>

    <script>
function toggleMeshOption(meshLabel) {
    var note = document.getElementById('user_note_detail');
    if (!note) return;
    var checkbox = event.target;
    if (checkbox.checked) {
        if (!note.value.includes(meshLabel)) {
            note.value = note.value ? note.value + ' | Đăng ký kèm ' + meshLabel : 'Đăng ký kèm ' + meshLabel;
        }
    } else {
        note.value = note.value.replace(' | Đăng ký kèm ' + meshLabel, '').replace('Đăng ký kèm ' + meshLabel, '').trim();
    }
}
</script>
</body>
</html>
