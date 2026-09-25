<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/fonts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/badge.css">
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />
<!-- ============ HEADER ============ -->
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
        <a href="${pageContext.request.contextPath}/admin/dashboard" class="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"/>
                <rect x="14" y="3" width="7" height="7"/>
                <rect x="14" y="14" width="7" height="7"/>
                <rect x="3" y="14" width="7" height="7"/>
            </svg>
            Dashboard
        </a>
        <a href="${pageContext.request.contextPath}/admin/customers">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            Khách hàng
        </a>
        <a href="${pageContext.request.contextPath}/admin/packages">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                <line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
            Gói cước
        </a>
        <a href="${pageContext.request.contextPath}/admin/users">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
            </svg>
            Tài khoản
        </a>
        <a href="${pageContext.request.contextPath}/admin/email-logs">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
            </svg>
            Thông báo
        </a>
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
            <a href="${pageContext.request.contextPath}/logout" class="logout-btn" title="Đăng xuất">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
            </a>
        </div>
    </div>
</header>

<!-- ============ MAIN ============ -->
<main class="admin-main">

    <!-- TIÊU ĐỀ -->
    <div class="page-title">
        <h1>Bảng Điều Khiển Quản Trị</h1>
        <p>Thống kê toàn diện hệ thống bán cước Internet FPT Telecom</p>
    </div>

    <!-- ============ 4 STAT CARDS ============ -->
    <div class="stats-grid">

        <!-- ⭐ Card 1: Khách hàng → /admin/customers — HOVER CAM -->
        <a href="${pageContext.request.contextPath}/admin/customers" class="stat-card stat-card-link stat-link-orange">
            <div class="stat-info">
                <div class="stat-label">TỔNG SỐ KHÁCH HÀNG</div>
                <div class="stat-value">${totalCustomers}</div>
                <div class="stat-note up">↑ ${countNew} khách mới</div>
            </div>
            <div class="stat-icon icon-orange">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                </svg>
            </div>
        </a>

        <!-- ⭐ Card 2: Gói FPT → /admin/packages — HOVER XANH DƯƠNG -->
        <a href="${pageContext.request.contextPath}/admin/packages" class="stat-card stat-card-link stat-link-blue">
            <div class="stat-info">
                <div class="stat-label">TỔNG SỐ GÓI FPT</div>
                <div class="stat-value">${totalPackages}</div>
                <div class="stat-note hot">
                    <span class="note-pill note-pill-hot">
                        <svg class="note-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
                        </svg>
                        ${countHot} HOT
                    </span>
                    <span class="note-pill note-pill-featured">
                        <svg class="note-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                        </svg>
                        ${countFeatured} NỔI BẬT
                    </span>
                </div>
            </div>
            <div class="stat-icon icon-blue">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="7" width="20" height="14" rx="2"/>
                    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                </svg>
            </div>
        </a>

        <!-- ⭐ Card 3: Tài khoản → /admin/users — HOVER XANH LÁ -->
        <a href="${pageContext.request.contextPath}/admin/users" class="stat-card stat-card-link stat-link-green">
            <div class="stat-info">
                <div class="stat-label">TÀI KHOẢN NGƯỜI DÙNG</div>
                <div class="stat-value">${totalUsers}</div>
                <div class="stat-note up">✓ ${countActiveUsers} đang hoạt động</div>
            </div>
            <div class="stat-icon icon-green">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="8.5" cy="7" r="4"/>
                    <line x1="20" y1="8" x2="20" y2="14"/>
                    <line x1="23" y1="11" x2="17" y2="11"/>
                </svg>
            </div>
        </a>

        <!-- ⭐ Card 4: Thông báo → /admin/email-logs — HOVER TÍM -->
        <a href="${pageContext.request.contextPath}/admin/email-logs" class="stat-card stat-card-link stat-link-purple">
            <div class="stat-info">
                <div class="stat-label">THÔNG BÁO</div>
                <div class="stat-value">${totalEmails}</div>
                <div class="stat-note down">↑ ${totalEmails} email đã gửi</div>
            </div>
            <div class="stat-icon icon-purple">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                    <polyline points="22,6 12,13 2,6"/>
                </svg>
            </div>
        </a>

    </div>

    <!-- ============ 2 CHARTS ============ -->
    <div class="charts-row">

        <!-- CHART 1: 7 NGÀY -->
        <div class="card chart-card">
            <div class="card-header">
                <h3>Khách hàng đăng ký 7 ngày gần nhất</h3>
                <span class="badge-orange">${totalCustomers} tổng khách</span>
            </div>
            <div class="chart-body">
                <canvas id="dailyChart"></canvas>
            </div>
        </div>

        <!-- CHART 2: DANH SÁCH GÓI CƯỚC -->
        <div class="card chart-card">
            <div class="card-header">
                <h3>Danh sách gói cước</h3>
                <span class="badge-orange">${totalPackages} gói</span>
            </div>
            <div class="package-summary">
                <div class="pkg-total">${totalPackages} <span>gói</span></div>
                <div class="pkg-desc">Tổng số gói cước Internet & FPT Play đang hoạt động</div>

                <!-- Danh sách gói -->
              <div class="pkg-list">
    <c:forEach items="${packages}" var="p" varStatus="loop">
        <div class="pkg-item">
            <span class="pkg-dot"
                  style="background: ${loop.index == 0 ? '#f37021' :
                                     loop.index == 1 ? '#1a4b8c' :
                                     loop.index == 2 ? '#0284c7' :
                                     loop.index == 3 ? '#16a34a' : '#7c3aed'};">
            </span>
            <span class="pkg-name">${p.name}</span>
            <span class="pkg-price">
                <fmt:formatNumber value="${p.price}" pattern="#,###"/>đ
            </span>
            <span class="pkg-badge-col">
                <c:choose>
                    <c:when test="${p.badgeType == 'hot'}">
                        <span class="pkg-badge pkg-badge-hot pkg-badge-sm">
                            <svg viewBox="0 0 24 24">
                            <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
                            </svg>
                            HOT
                        </span>
                    </c:when>
                    <c:when test="${p.badgeType == 'featured'}">
                        <span class="pkg-badge pkg-badge-featured pkg-badge-sm">
                            <svg viewBox="0 0 24 24">
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                            </svg>
                            NỔI BẬT
                        </span>
                    </c:when>
                    <c:otherwise>
                        <span class="pkg-count" style="color:#94a3b8;">Bình thường</span>
                    </c:otherwise>
                </c:choose>
            </span>
        </div>
    </c:forEach>
              </div>

                <div class="pkg-footer">
                    <a href="${pageContext.request.contextPath}/admin/packages" class="link-detail">
                        Chi tiết danh sách gói →
                    </a>
                </div>
            </div>
        </div>

    </div>

    <!-- ============ KHÁCH HÀNG GẦN NHẤT ============ -->
    <div class="card table-card">
        <div class="card-header">
            <div>
                <h3>Khách hàng đăng ký gần đây</h3>
                <p class="sub">5 khách hàng để lại thông tin mới nhất trên hệ thống</p>
            </div>
            <a href="${pageContext.request.contextPath}/admin/customers" class="link-detail">
                Xem tất cả danh sách →
            </a>
        </div>

        <c:choose>
            <c:when test="${not empty recentCustomers}">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Mã KH</th>
                            <th>Họ và tên</th>
                            <th>Số điện thoại</th>
                            <th>Gmail</th>
                            <th>Trạng thái</th>
                            <th>Ngày đăng ký</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${recentCustomers}" var="c">
                            <tr>
                                <td class="text-muted">#KH-${c.id}</td>
                                <td><strong>${c.fullName}</strong></td>
                                <td>${c.phone}</td>
                                <td class="text-muted">${c.email}</td>
                                <td>
                                    <span class="status-badge status-${c.status}">
                                        ${c.status}
                                    </span>
                                </td>
                                <td class="text-muted">
                                    <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>

            <c:otherwise>
                <div style="text-align:center; padding: 40px 20px; color:#94a3b8;">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                         style="width:48px;height:48px;opacity:0.3;margin:0 auto 12px;">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                        <circle cx="9" cy="7" r="4"/>
                    </svg>
                    <p>Chưa có khách hàng nào đăng ký</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

</main>

<!-- ============ CHART.JS ============ -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<script>
    // ===== CHART 1: 7 NGÀY =====
    const ctxDaily = document.getElementById('dailyChart').getContext('2d');

    const labels = [
        <c:forEach items="${chartLabels}" var="label" varStatus="loop">
            "${label}"<c:if test="${!loop.last}">,</c:if>
        </c:forEach>
    ];

    const dataValues = [
        <c:forEach items="${chartData}" var="val" varStatus="loop">
            ${val}<c:if test="${!loop.last}">,</c:if>
        </c:forEach>
    ];

    new Chart(ctxDaily, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Số khách hàng',
                data: dataValues,
                backgroundColor: dataValues.map((v, i) =>
                    i === dataValues.indexOf(Math.max(...dataValues)) ? '#f37021' : '#ffb26b'
                ),
                borderRadius: 8,
                borderSkipped: false,
                barThickness: 42
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#1a4b8c',
                    titleFont: { family: 'Be Vietnam Pro', size: 13, weight: 'bold' },
                    bodyFont: { family: 'Be Vietnam Pro', size: 13 },
                    padding: 12,
                    cornerRadius: 8,
                    displayColors: false,
                    callbacks: {
                        label: (ctx) => ctx.parsed.y + ' khách hàng'
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f0f0f0', drawBorder: false },
                    ticks: {
                        font: { family: 'Be Vietnam Pro', size: 12 },
                        color: '#94a3b8',
                        stepSize: 1
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        font: { family: 'Be Vietnam Pro', size: 12, weight: '600' },
                        color: '#64748b'
                    }
                }
            }
        }
    });
</script>

</body>
</html>