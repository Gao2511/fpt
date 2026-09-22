<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đơn Đăng Ký Của Tôi - FPT Telecom</title>
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/home.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/customer/my-orders.css">
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
                    <a href="${pageContext.request.contextPath}/home#packages">Bảng giá</a>
                    <a href="${pageContext.request.contextPath}/my-orders" class="active">Xem đơn</a>
                    <a href="${pageContext.request.contextPath}/home#contact-info">Liên hệ</a>
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
                        <c:when test="${empty sessionScope.user}">
                            <a href="${pageContext.request.contextPath}/login" class="btn-login-small">Đăng nhập</a>
                            <a href="${pageContext.request.contextPath}/register" class="btn-register-small">Đăng ký ngay</a>
                        </c:when>
                        <c:when test="${sessionScope.user.role == 'admin'}">
                            <a href="${pageContext.request.contextPath}/admin/dashboard" class="btn-login-small">Trang quản trị</a>
                            <a href="${pageContext.request.contextPath}/logout" class="btn-register-small">Đăng xuất</a>
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
                                    <a href="${pageContext.request.contextPath}/home" class="dropdown-item">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                                        <polyline points="9 22 9 12 15 12 15 22"/>
                                        </svg>
                                        Trang chủ
                                    </a>
                                    <a href="${pageContext.request.contextPath}/home#packages" class="dropdown-item">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <rect x="2" y="7" width="20" height="14" rx="2"/>
                                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                                        </svg>
                                        Bảng giá gói cước
                                    </a>
                                    <a href="${pageContext.request.contextPath}/my-orders" class="dropdown-item">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                        <polyline points="14 2 14 8 20 8"/>
                                        <line x1="16" y1="13" x2="8" y2="13"/>
                                        <line x1="16" y1="17" x2="8" y2="17"/>
                                        </svg>
                                        Xem đơn
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

        <!-- ============ MAIN ============ -->
        <main class="orders-container">
            <div class="orders-header">
                <h1>
                    <svg viewBox="0 0 24 24">
                    <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
                    <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
                    <line x1="9" y1="12" x2="15" y2="12"/>
                    <line x1="9" y1="16" x2="13" y2="16"/>
                    </svg>
                    Đơn Đăng Ký Của Tôi
                </h1>
                <p>Danh sách các đơn đăng ký tư vấn bạn đã gửi đến FPT Telecom</p>
                <span class="orders-count">${totalRecords} đơn đăng ký</span>
            </div>

            <c:choose>
                <%-- CÓ ĐƠN --%>
                <c:when test="${not empty orders}">
                    <c:forEach items="${orders}" var="o">
                        <div class="order-card">
                            <div class="order-header">
                                <span class="order-id">#KH-${o.id}</span>
                                <span class="order-status status-${fn:replace(o.status, ' ', '-')}">${o.status}</span>
                            </div>

                            <div class="order-body">
                                <div class="order-row">
                                    <span class="order-label">Họ tên:</span>
                                    <span class="order-value">${o.fullName}</span>
                                </div>
                                <div class="order-row">
                                    <span class="order-label">SĐT:</span>
                                    <span class="order-value">${o.phone}</span>
                                </div>
                                <div class="order-row">
                                    <span class="order-label">Email:</span>
                                    <span class="order-value">${not empty o.email ? o.email : '(Chưa cung cấp)'}</span>
                                </div>
                                <div class="order-row">
                                    <span class="order-label">Địa chỉ:</span>
                                    <span class="order-value">${not empty o.address ? o.address : '(Chưa cung cấp)'}</span>
                                </div>
                                <c:if test="${not empty o.note}">
                                    <div class="order-row order-row-full">
                                        <span class="order-label">Ghi chú:</span>
                                        <span class="order-value">${o.note}</span>
                                    </div>
                                </c:if>
                                <c:if test="${not empty o.consultantName}">
                                    <div class="order-row order-row-full">
                                        <span class="order-label">Tư vấn viên:</span>
                                        <span class="order-value">${o.consultantName}</span>
                                    </div>
                                </c:if>
                            </div>

                            <div class="order-footer">
                                <svg viewBox="0 0 24 24">
                                <circle cx="12" cy="12" r="10"/>
                                <polyline points="12 6 12 12 16 14"/>
                                </svg>
                                Ngày gửi: <fmt:formatDate value="${o.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                            </div>
                        </div>
                    </c:forEach>

                    <%-- ============================================================
                         PHÂN TRANG — HIỆN SỐ TRANG
                         ============================================================ --%>
                    <c:if test="${totalPages > 1}">
                        <div class="orders-pagination">

                            <%-- Nút "Trước" --%>
                            <c:choose>
                                <c:when test="${currentPage > 1}">
                                    <a href="?page=${currentPage - 1}" class="page-nav">‹ Trước</a>
                                </c:when>
                                <c:otherwise>
                                    <span class="page-nav disabled">‹ Trước</span>
                                </c:otherwise>
                            </c:choose>

                            <%-- Số trang --%>
                            <c:forEach begin="1" end="${totalPages}" var="i">
                                <c:choose>
                                    <%-- Trang hiện tại --%>
                                    <c:when test="${i == currentPage}">
                                        <span class="page-active">${i}</span>
                                    </c:when>

                                    <%-- 3 trang đầu, 3 trang cuối, hoặc gần trang hiện tại --%>
                                    <c:when test="${i <= 3 || i > totalPages - 3 || (i >= currentPage - 1 && i <= currentPage + 1)}">
                                        <a href="?page=${i}">${i}</a>
                                    </c:when>

                                    <%-- Dấu ... bên trái --%>
                                    <c:when test="${i == 4 && currentPage > 4}">
                                        <span class="page-dots">•••</span>
                                    </c:when>

                                    <%-- Dấu ... bên phải --%>
                                    <c:when test="${i == totalPages - 3 && currentPage < totalPages - 3}">
                                        <span class="page-dots">•••</span>
                                    </c:when>
                                </c:choose>
                            </c:forEach>

                            <%-- Nút "Sau" --%>
                            <c:choose>
                                <c:when test="${currentPage < totalPages}">
                                    <a href="?page=${currentPage + 1}" class="page-nav">Sau ›</a>
                                </c:when>
                                <c:otherwise>
                                    <span class="page-nav disabled">Sau ›</span>
                                </c:otherwise>
                            </c:choose>

                        </div>

                        <%-- Info: Trang X / Y --%>
                        <div class="orders-page-info-wrap">
                            <div class="orders-page-info">
                                Trang <strong>${currentPage}</strong> / ${totalPages}
                                — Hiển thị
                                <strong>${(currentPage - 1) * pageSize + 1}</strong>
                                đến
                                <strong>${currentPage * pageSize > totalRecords ? totalRecords : currentPage * pageSize}</strong>
                                trong tổng <strong>${totalRecords}</strong> đơn
                            </div>
                        </div>
                    </c:if>
                </c:when>

                <%-- KHÔNG CÓ ĐƠN — HÌNH MINH HỌA SVG --%>
                <c:otherwise>
                    <div class="empty-orders">
                        <svg class="empty-illustration" viewBox="0 0 240 240" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="120" cy="120" r="105" fill="#FFF4EC"/>
                            <circle cx="120" cy="120" r="105" stroke="#F37021" stroke-width="1.5" stroke-dasharray="4 8" opacity="0.4"/>
                            <ellipse cx="120" cy="192" rx="60" ry="8" fill="#F37021" opacity="0.12"/>
                            <path d="M60 90 L120 60 L180 90 L180 165 L120 195 L60 165 Z"
                                  fill="#FFFFFF" stroke="#F37021" stroke-width="2.5"
                                  stroke-linejoin="round"/>
                            <path d="M60 90 L120 120 L180 90"
                                  stroke="#F37021" stroke-width="2.5"
                                  stroke-linejoin="round" fill="none"/>
                            <path d="M120 120 L120 195"
                                  stroke="#F37021" stroke-width="2.5"
                                  stroke-linejoin="round"/>
                            <path d="M60 90 L85 78 L145 108 L120 120 Z"
                                  fill="#FFE0CC" stroke="#F37021" stroke-width="2.5"
                                  stroke-linejoin="round"/>
                            <path d="M180 90 L155 78 L95 108 L120 120 Z"
                                  fill="#FFD1B3" stroke="#F37021" stroke-width="2.5"
                                  stroke-linejoin="round"/>
                            <g transform="translate(120 55)">
                                <circle cx="0" cy="0" r="22" fill="#F37021"/>
                                <path d="M-6 -6 a6 6 0 1 1 6 8 v3"
                                      stroke="#FFFFFF" stroke-width="3"
                                      stroke-linecap="round" fill="none"/>
                                <circle cx="0" cy="10" r="2" fill="#FFFFFF"/>
                            </g>
                            <circle cx="55" cy="55" r="3" fill="#F37021" opacity="0.6"/>
                            <circle cx="195" cy="70" r="4" fill="#FF8C42" opacity="0.7"/>
                            <circle cx="200" cy="160" r="2.5" fill="#F37021" opacity="0.5"/>
                            <circle cx="42" cy="150" r="3.5" fill="#FF8C42" opacity="0.6"/>
                            <path d="M180 40 l4 8 l8 4 l-8 4 l-4 8 l-4 -8 l-8 -4 l8 -4 z"
                                  fill="#FFB380" opacity="0.8"/>
                            <path d="M55 190 l3 6 l6 3 l-6 3 l-3 6 l-3 -6 l-6 -3 l6 -3 z"
                                  fill="#FFB380" opacity="0.8"/>
                        </svg>

                        <h3>Chưa có đơn đăng ký nào</h3>
                        <p>Bạn chưa gửi đơn đăng ký tư vấn nào đến FPT Telecom</p>
                        <a href="${pageContext.request.contextPath}/home#contact" class="btn-cta">
                            <svg viewBox="0 0 24 24">
                            <path d="M12 20h9"/>
                            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                            </svg>
                            Đăng ký tư vấn ngay
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>
        </main>

        <!-- ============ FOOTER ============ -->
        <footer class="footer" id="contact-info">
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
                        <strong>Copyright © 2026 FPT Telecom.</strong><br>
                        Cơ quan chủ quản: Công Ty Cổ Phần Viễn Thông FPT.<br>
                        Tất cả các quyền được bảo lưu theo quy định pháp luật Việt Nam.
                    </div>
                </div>
            </div>
        </footer>

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

    </body>
</html>