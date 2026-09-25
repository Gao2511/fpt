<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản Lý Gói Cước - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-package.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/badge.css">
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />
<!-- HEADER -->
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
        <a href="${pageContext.request.contextPath}/admin/dashboard">
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
        <a href="${pageContext.request.contextPath}/admin/packages" class="active">
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

<main class="admin-main page-package-list">

    <div class="page-header-flex">
        <div>
            <h1>Quản Lý Gói Cước</h1>
            <p>Thêm, sửa, xóa và quản lý danh sách gói cước Internet FPT</p>
        </div>
        <div style="display:flex; gap:10px;">
            <a href="${pageContext.request.contextPath}/admin/packages?action=add" class="btn-add-customer">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="12" y1="5" x2="12" y2="19"/>
                    <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                Thêm gói cước mới
            </a>
        </div>
    </div>

    <!-- FLASH -->
    <c:if test="${not empty message}">
        <div class="alert alert-${messageType}">
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" class="alert-close">×</button>
        </div>
    </c:if>

    <!-- STATS -->
    <div class="quick-stats">
        <div class="qs-item">
            <span class="qs-label">Tổng số gói:</span>
            <strong class="qs-value">${totalAll}</strong>
        </div>
        <div class="qs-divider"></div>
        <div class="qs-item">
            <span class="qs-dot dot-signed"></span>
            <span class="qs-label">Gói HOT:</span>
            <strong class="qs-value">${totalHot}</strong>
        </div>
        <div class="qs-divider"></div>
        <div class="qs-item">
            <span class="qs-label">Kết quả lọc:</span>
            <strong class="qs-value">${totalRecords}</strong>
        </div>
    </div>

    <!-- FILTER -->
    <form method="get" action="${pageContext.request.contextPath}/admin/packages" class="filter-bar">
        <div class="filter-row">
            <div class="filter-search">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input type="text" name="keyword" placeholder="Tìm theo tên gói, mã gói, mô tả..."
                       value="${keyword}">
            </div>

            <select name="sortBy" class="filter-select">
                <option value="">Sắp xếp mặc định</option>
                <option value="price_asc" ${sortBy == 'price_asc' ? 'selected' : ''}>Giá tăng dần</option>
                <option value="price_desc" ${sortBy == 'price_desc' ? 'selected' : ''}>Giá giảm dần</option>
                <option value="name" ${sortBy == 'name' ? 'selected' : ''}>Tên A → Z</option>
                <option value="speed" ${sortBy == 'speed' ? 'selected' : ''}>Tốc độ cao nhất</option>
            </select>

            <button type="submit" class="btn-filter">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
                </svg>
                Lọc
            </button>

            <a href="${pageContext.request.contextPath}/admin/packages" class="btn-reset" title="Xóa bộ lọc">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="1 4 1 10 7 10"/>
                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
                </svg>
            </a>
        </div>
    </form>

    <!-- TABLE -->
    <div class="table-wrapper">
        <div class="table-header">
            <div class="table-title">
                Danh sách gói cước
                <span class="table-badge">Trang ${currentPage} / ${totalPages}</span>
            </div>
        </div>

        <table class="customer-table">
            <thead>
                <tr>
                    <th style="width:60px;">ID</th>
                    <th style="width:110px;">MÃ GÓI</th>
                    <th>TÊN GÓI</th>
                    <th style="width:120px;">GIÁ / THÁNG</th>
                    <th style="width:100px;">TỐC ĐỘ</th>
                    <th>MÔ TẢ</th>
                    <th style="width:230px;">PHÂN LOẠI</th>
                    <th style="width:130px;">THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${packages}" var="p">
                    <tr>
                        <td class="col-id">#${p.id}</td>
                        <td>
                            <span class="pkg-code">${p.packageCode}</span>
                        </td>
                        <td>
                            <div class="customer-info">
                                <strong>${p.name}</strong>
                                <span class="phone">
                                    <fmt:formatDate value="${p.createdAt}" pattern="dd/MM/yyyy"/>
                                </span>
                            </div>
                        </td>
                        <td>
                            <span class="pkg-price-tag">
                                <fmt:formatNumber value="${p.price}" pattern="#,###"/>đ
                            </span>
                        </td>
                        <td>
                            <span class="pkg-speed">${p.speedMbps} Mbps</span>
                        </td>
                        <td class="col-desc">${p.description}</td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/packages"
                                  class="badge-form">
                                <input type="hidden" name="action" value="updateBadge">
                                <input type="hidden" name="id" value="${p.id}">

                                <div class="badge-pill-group">
                                    <!-- Nút HOT -->
                                    <label class="badge-pill pill-hot ${p.badgeType == 'hot' ? 'active' : ''}">
                                        <input type="radio" name="badgeType" value="hot"
                                               ${p.badgeType == 'hot' ? 'checked' : ''}
                                               onchange="this.form.submit()">
                                        <svg viewBox="0 0 24 24">
                                            <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
                                        </svg>
                                        HOT
                                    </label>

                                    <!-- Nút NỔI BẬT -->
                                    <label class="badge-pill pill-featured ${p.badgeType == 'featured' ? 'active' : ''}">
                                        <input type="radio" name="badgeType" value="featured"
                                               ${p.badgeType == 'featured' ? 'checked' : ''}
                                               onchange="this.form.submit()">
                                        <svg viewBox="0 0 24 24">
                                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                                        </svg>
                                        NỔI BẬT
                                    </label>

                                    
                                </div>
                            </form>
                        </td>
                        <td class="col-actions">
                            <a href="${pageContext.request.contextPath}/admin/packages?action=edit&id=${p.id}"
                               class="action-btn btn-edit" title="Sửa">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                                </svg>
                            </a>
                            <a href="${pageContext.request.contextPath}/admin/packages?action=delete&id=${p.id}"
                               class="action-btn btn-delete" title="Xóa"
                               onclick="return confirm('Bạn có chắc muốn xóa gói cước ${p.name}?');">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="3 6 5 6 21 6"/>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                                </svg>
                            </a>
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${empty packages}">
                    <tr>
                        <td colspan="8" class="empty-row">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:48px;height:48px;opacity:0.3;">
                                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                            </svg>
                            <p>Không tìm thấy gói cước nào</p>
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <!-- PAGINATION -->
        <div class="table-footer">
            <div class="footer-info">
                Hiển thị
                <strong>${(currentPage - 1) * pageSize + 1} - ${currentPage * pageSize > totalRecords ? totalRecords : currentPage * pageSize}</strong>
                trên tổng số <strong>${totalRecords}</strong> gói cước
            </div>

            <div class="pagination">
                <c:if test="${currentPage > 1}">
                    <a href="?page=${currentPage - 1}&keyword=${keyword}&sortBy=${sortBy}" class="page-btn">
                        ‹ Trước
                    </a>
                </c:if>

                <c:forEach begin="1" end="${totalPages}" var="i">
                    <c:choose>
                        <c:when test="${i == currentPage}">
                            <span class="page-btn page-active">${i}</span>
                        </c:when>
                        <c:otherwise>
                            <a href="?page=${i}&keyword=${keyword}&sortBy=${sortBy}" class="page-btn">${i}</a>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>

                <c:if test="${currentPage < totalPages}">
                    <a href="?page=${currentPage + 1}&keyword=${keyword}&sortBy=${sortBy}" class="page-btn">
                        Sau ›
                    </a>
                </c:if>
            </div>
        </div>
    </div>

</main>

</body>
</html>