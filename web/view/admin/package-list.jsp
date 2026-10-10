<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
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
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />
<!-- ============ HEADER (DÙNG CHUNG) ============ -->
<jsp:include page="/view/admin/admin-header.jsp">
    <jsp:param name="activeTab" value="packages"/>
</jsp:include>

<main class="admin-main page-package-list">

    <div class="page-header-flex">
        <div>
            <h1>Quản Lý Gói Cước &amp; Mesh WiFi</h1>
            <p>Quản lý bảng giá gói cước Internet cáp quang, phí hòa mạng và cấu hình bộ mở rộng sóng Mesh WiFi F1 / F2</p>
        </div>
        <div class="header-action-group">
            <a href="#mesh-settings-section" class="btn-mesh-jump">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M5 12.55a11 11 0 0 1 14.08 0"/>
                    <path d="M1.42 9a16 16 0 0 1 21.16 0"/>
                    <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
                    <line x1="12" y1="20" x2="12.01" y2="20"/>
                </svg>
                Cấu hình Mesh WiFi
            </a>
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

    <!-- 4 THẺ TỔNG QUAN GÓI CƯỚC & MESH WIFI -->
    <div class="pkg-kpi-grid">
        <div class="pkg-kpi-card">
            <div class="pkg-kpi-icon kpi-blue">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                    <line x1="12" y1="22.08" x2="12" y2="12"/>
                </svg>
            </div>
            <div class="pkg-kpi-body">
                <span class="pkg-kpi-label">GÓI CƯỚC INTERNET</span>
                <div class="pkg-kpi-val">${totalAll} <small>gói (${not empty totalActive ? totalActive : totalAll} đang bán)</small></div>
                <span class="pkg-kpi-sub">Phí lắp chuẩn: <strong><fmt:formatNumber value="${not empty cmsValues['business.standardFee'] ? cmsValues['business.standardFee'] : 300000}" pattern="#,###"/>đ</strong></span>
            </div>
        </div>

        <div class="pkg-kpi-card">
            <div class="pkg-kpi-icon kpi-orange">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
                </svg>
            </div>
            <div class="pkg-kpi-body">
                <span class="pkg-kpi-label">PHÂN LOẠI NỔI BẬT</span>
                <div class="pkg-kpi-val">${totalHot} <small>HOT</small> • ${not empty totalFeatured ? totalFeatured : 0} <small>Nổi bật</small></div>
                <span class="pkg-kpi-sub">Kết quả bộ lọc hiện tại: <strong>${totalRecords} gói</strong></span>
            </div>
        </div>

        <a href="#mesh-settings-section" class="pkg-kpi-card pkg-kpi-link">
            <div class="pkg-kpi-icon kpi-green">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M5 12.55a11 11 0 0 1 14.08 0"/>
                    <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
                    <line x1="12" y1="20" x2="12.01" y2="20"/>
                </svg>
            </div>
            <div class="pkg-kpi-body">
                <span class="pkg-kpi-label">MESH WIFI F1 • NHÀ 2 TẦNG</span>
                <div class="pkg-kpi-val">+<fmt:formatNumber value="${not empty cmsValues['business.meshF1Monthly'] ? cmsValues['business.meshF1Monthly'] : 10000}" pattern="#,###"/>đ<small>/tháng</small></div>
                <span class="pkg-kpi-sub">Phí lắp: <strong><fmt:formatNumber value="${not empty cmsValues['business.meshF1Fee'] ? cmsValues['business.meshF1Fee'] : 500000}" pattern="#,###"/> VNĐ</strong></span>
            </div>
        </a>

        <a href="#mesh-settings-section" class="pkg-kpi-card pkg-kpi-link">
            <div class="pkg-kpi-icon kpi-purple">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1.42 9a16 16 0 0 1 21.16 0"/>
                    <path d="M5 12.55a11 11 0 0 1 14.08 0"/>
                    <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
                    <line x1="12" y1="20" x2="12.01" y2="20"/>
                </svg>
            </div>
            <div class="pkg-kpi-body">
                <span class="pkg-kpi-label">MESH WIFI F2 • NHÀ 3 TẦNG</span>
                <div class="pkg-kpi-val">+<fmt:formatNumber value="${not empty cmsValues['business.meshF2Monthly'] ? cmsValues['business.meshF2Monthly'] : 20000}" pattern="#,###"/>đ<small>/tháng</small></div>
                <span class="pkg-kpi-sub">Phí lắp: <strong><fmt:formatNumber value="${not empty cmsValues['business.meshF2Fee'] ? cmsValues['business.meshF2Fee'] : 700000}" pattern="#,###"/> VNĐ</strong></span>
            </div>
        </a>
    </div>

    <!-- FILTER -->
    <form method="get" action="${pageContext.request.contextPath}/admin/packages" class="filter-bar">
        <div class="filter-row">
            <div class="filter-search">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input type="text" name="keyword" placeholder="Tìm theo tên gói, mã gói, mô tả thiết bị..."
                       value="${fn:escapeXml(keyword)}">
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

    <!-- TABLE DANH SÁCH GÓI CƯỚC -->
    <div class="table-wrapper">
        <div class="table-header">
            <div class="table-title">
                Danh sách gói cước Internet FPT
                <span class="table-badge">Trang ${currentPage} / ${totalPages}</span>
            </div>
        </div>

        <table class="customer-table">
            <thead>
                <tr>
                    <th style="width:70px;">ID / TT</th>
                    <th style="width:100px;">MÃ GÓI</th>
                    <th style="width:170px;">TÊN GÓI &amp; TRẠNG THÁI</th>
                    <th style="width:125px;">GIÁ / THÁNG</th>
                    <th style="width:105px;">TỐC ĐỘ</th>
                    <th style="width:125px;">PHÍ LẮP ĐẶT</th>
                    <th>THIẾT BỊ &amp; MÔ TẢ</th>
                    <th style="width:240px;">PHÂN LOẠI NHÃN</th>
                    <th style="width:95px;">THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${packages}" var="p">
                    <tr>
                        <td class="col-id">
                            <div>#${p.id}</div>
                            <small class="pkg-order-chip" title="Thứ tự hiển thị">TT: ${p.displayOrder}</small>
                        </td>
                        <td>
                            <span class="pkg-code">${fn:escapeXml(p.packageCode)}</span>
                        </td>
                        <td>
                            <div class="customer-info">
                                <strong>${fn:escapeXml(p.name)}</strong>
                                <span class="pkg-sale-status ${p.active ? 'is-active' : 'is-hidden'}">
                                    <span class="status-mini-dot"></span>
                                    ${p.active ? 'Đang bán' : 'Ngừng hiển thị'}
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
                        <td>
                            <div class="pkg-fee-cell">
                                <strong><fmt:formatNumber value="${p.standardInstallationFee > 0 ? p.standardInstallationFee : 300000}" pattern="#,###"/>đ</strong>
                                <small>${p.installationFeeInherited ? 'Mặc định' : 'Phí riêng'}</small>
                            </div>
                        </td>
                        <td class="col-desc" title="${fn:escapeXml(p.description)}">${fn:escapeXml(p.description)}</td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/packages"
                                  class="badge-form"><input type="hidden" name="csrf_token" value="<c:out value='${csrfToken}'/>">
                                <input type="hidden" name="action" value="updateBadge">
                                <input type="hidden" name="id" value="${p.id}">

                                <div class="badge-pill-group">
                                    <!-- Nút Thường -->
                                    <label class="badge-pill pill-normal ${empty p.badgeType ? 'active' : ''}" title="Đặt về gói bình thường">
                                        <input type="radio" name="badgeType" value=""
                                               ${empty p.badgeType ? 'checked' : ''}
                                               onchange="this.form.submit()">
                                        Thường
                                    </label>

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
                               class="action-btn btn-edit" title="Sửa chi tiết gói">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                                </svg>
                            </a>
                            <a href="${pageContext.request.contextPath}/admin/packages?action=delete&id=${p.id}"
                               class="action-btn btn-delete" title="Xóa gói"
                               onclick="return confirm('Xác nhận xóa gói cước này?');">
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
                        <td colspan="9" class="empty-row">
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
                <strong>${totalRecords == 0 ? 0 : (currentPage - 1) * pageSize + 1} - ${currentPage * pageSize > totalRecords ? totalRecords : currentPage * pageSize}</strong>
                trên tổng số <strong>${totalRecords}</strong> gói cước
            </div>

            <div class="pagination">
                <c:if test="${currentPage > 1}">
                    <a href="?page=${currentPage - 1}&keyword=${fn:escapeXml(keyword)}&sortBy=${fn:escapeXml(sortBy)}" class="page-btn">
                        ‹ Trước
                    </a>
                </c:if>

                <c:forEach begin="1" end="${totalPages}" var="i">
                    <c:choose>
                        <c:when test="${i == currentPage}">
                            <span class="page-btn page-active">${i}</span>
                        </c:when>
                        <c:otherwise>
                            <a href="?page=${i}&keyword=${fn:escapeXml(keyword)}&sortBy=${fn:escapeXml(sortBy)}" class="page-btn">${i}</a>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>

                <c:if test="${currentPage < totalPages}">
                    <a href="?page=${currentPage + 1}&keyword=${fn:escapeXml(keyword)}&sortBy=${fn:escapeXml(sortBy)}" class="page-btn">
                        Sau ›
                    </a>
                </c:if>
            </div>
        </div>
    </div>

    <!-- =========================================================
         CẤU HÌNH MESH WIFI (F1 & F2) & PHÍ HÒA MẠNG CHUẨN
         (NẰM CHUNG TRỰC TIẾP VỚI TRANG QUẢN LÝ GÓI CƯỚC)
         ========================================================= -->
    <section id="mesh-settings-section" class="mesh-admin-section">
        <form method="post" action="${pageContext.request.contextPath}/admin/packages" class="mesh-admin-form">
            <input type="hidden" name="csrf_token" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="action" value="updateMesh">

            <div class="mesh-section-header">
                <div class="mesh-header-left">
                    <div class="mesh-header-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M5 12.55a11 11 0 0 1 14.08 0"/>
                            <path d="M1.42 9a16 16 0 0 1 21.16 0"/>
                            <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
                            <line x1="12" y1="20" x2="12.01" y2="20"/>
                        </svg>
                    </div>
                    <div>
                        <div class="mesh-header-title-row">
                            <h2>Cấu Hình Mở Rộng Mesh WiFi (F1 &amp; F2) &amp; Phí Hòa Mạng</h2>
                            <span class="mesh-visibility-badge ${cmsValues['business.meshVisible'] == 'false' ? 'is-off' : 'is-on'}">
                                ${cmsValues['business.meshVisible'] == 'false' ? 'Đang ẩn trên Web' : 'Đang hiển thị trên Web'}
                            </span>
                        </div>
                        <p>Chỉnh sửa trực tiếp chi phí hàng tháng, phí lắp đặt và thiết bị Mesh WiFi F1/F2. Đồng bộ tự động với giao diện khách hàng và Chatbot AI.</p>
                    </div>
                </div>
                <button type="submit" class="btn-add-customer" ${demoMode ? 'disabled' : ''}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                        <polyline points="17 21 17 13 7 13 7 21"/>
                        <polyline points="7 3 7 8 15 8"/>
                    </svg>
                    Lưu cấu hình Mesh &amp; Phí
                </button>
            </div>

            <!-- HÀNG 1: CÀI ĐẶT CHUNG -->
            <div class="mesh-general-grid">
                <div class="mesh-field">
                    <label for="meshVisibleSelect">Hiển thị khu vực Mesh WiFi</label>
                    <select id="meshVisibleSelect" name="business.meshVisible" ${demoMode ? 'disabled' : ''}>
                        <option value="true" ${cmsValues['business.meshVisible'] != 'false' ? 'selected' : ''}>✓ Hiển thị trên trang chủ</option>
                        <option value="false" ${cmsValues['business.meshVisible'] == 'false' ? 'selected' : ''}>✕ Ẩn khỏi trang chủ</option>
                    </select>
                    <small>Bật hoặc tắt khối giới thiệu Mesh WiFi F1/F2 dưới bảng giá</small>
                </div>

                <div class="mesh-field">
                    <label for="standardFeeInput">Phí lắp đặt Internet chuẩn (VNĐ/lần)</label>
                    <input type="number" id="standardFeeInput" name="business.standardFee"
                           min="0" max="100000000" step="1000" required
                           value="${fn:escapeXml(not empty cmsValues['business.standardFee'] ? cmsValues['business.standardFee'] : '300000')}"
                           ${demoMode ? 'disabled' : ''}>
                    <small>Áp dụng cho tất cả gói Internet chọn "Dùng phí chuẩn"</small>
                </div>

                <div class="mesh-field">
                    <label for="meshTitleInput">Tiêu đề khu vực Mesh WiFi trên Web</label>
                    <input type="text" id="meshTitleInput" name="business.meshTitle" maxlength="200" required
                           value="${fn:escapeXml(not empty cmsValues['business.meshTitle'] ? cmsValues['business.meshTitle'] : 'MỞ RỘNG PHỦ SÓNG TOÀN DIỆN VỚI MESH WIFI FPT')}"
                           ${demoMode ? 'disabled' : ''}>
                    <small>Tiêu đề hiển thị phía trên 2 thẻ Mesh WiFi F1 &amp; F2</small>
                </div>
            </div>

            <!-- HÀNG 2: 2 CARD CẤU HÌNH MESH F1 & MESH F2 -->
            <div class="mesh-cards-grid">

                <!-- CARD MESH F1 -->
                <div class="mesh-config-card">
                    <div class="mesh-card-top">
                        <div>
                            <span class="mesh-floor-pill">NHÀ 2 TẦNG</span>
                            <h3>Mesh WiFi F1</h3>
                        </div>
                        <div class="mesh-live-price">
                            <strong>+<fmt:formatNumber value="${not empty cmsValues['business.meshF1Monthly'] ? cmsValues['business.meshF1Monthly'] : 10000}" pattern="#,###"/>đ<small>/tháng</small></strong>
                            <span>Phí lắp: <fmt:formatNumber value="${not empty cmsValues['business.meshF1Fee'] ? cmsValues['business.meshF1Fee'] : 500000}" pattern="#,###"/> VNĐ</span>
                        </div>
                    </div>

                    <div class="mesh-card-fields">
                        <div class="mesh-field">
                            <label for="meshF1Monthly">Phí cộng thêm hàng tháng (VNĐ/tháng)</label>
                            <input type="number" id="meshF1Monthly" name="business.meshF1Monthly"
                                   min="0" max="100000000" step="1000" required
                                   value="${fn:escapeXml(not empty cmsValues['business.meshF1Monthly'] ? cmsValues['business.meshF1Monthly'] : '10000')}"
                                   ${demoMode ? 'disabled' : ''}>
                            <small>Phụ thu cộng thêm vào gói Internet hàng tháng</small>
                        </div>

                        <div class="mesh-field">
                            <label for="meshF1Fee">Phí lắp đặt (VNĐ)</label>
                            <input type="number" id="meshF1Fee" name="business.meshF1Fee"
                                   min="0" max="100000000" step="1000" required
                                   value="${fn:escapeXml(not empty cmsValues['business.meshF1Fee'] ? cmsValues['business.meshF1Fee'] : '500000')}"
                                   ${demoMode ? 'disabled' : ''}>
                            <small>Khoản thu 1 lần khi triển khai lắp đặt Mesh F1</small>
                        </div>

                        <div class="mesh-field mesh-field-full">
                            <label for="meshF1Equipment">Thiết bị đi kèm</label>
                            <input type="text" id="meshF1Equipment" name="business.meshF1Equipment"
                                   maxlength="500" required
                                   value="${fn:escapeXml(not empty cmsValues['business.meshF1Equipment'] ? cmsValues['business.meshF1Equipment'] : '1 Modem WiFi 6 + 1 Access Point')}"
                                   ${demoMode ? 'disabled' : ''}>
                            <small>Hiển thị trong thẻ giới thiệu Mesh F1 và câu trả lời của AI</small>
                        </div>
                    </div>
                </div>

                <!-- CARD MESH F2 -->
                <div class="mesh-config-card mesh-config-card-f2">
                    <div class="mesh-card-top">
                        <div>
                            <span class="mesh-floor-pill pill-f2">NHÀ 3 TẦNG</span>
                            <h3>Mesh WiFi F2</h3>
                        </div>
                        <div class="mesh-live-price">
                            <strong>+<fmt:formatNumber value="${not empty cmsValues['business.meshF2Monthly'] ? cmsValues['business.meshF2Monthly'] : 20000}" pattern="#,###"/>đ<small>/tháng</small></strong>
                            <span>Phí lắp: <fmt:formatNumber value="${not empty cmsValues['business.meshF2Fee'] ? cmsValues['business.meshF2Fee'] : 700000}" pattern="#,###"/> VNĐ</span>
                        </div>
                    </div>

                    <div class="mesh-card-fields">
                        <div class="mesh-field">
                            <label for="meshF2Monthly">Phí cộng thêm hàng tháng (VNĐ/tháng)</label>
                            <input type="number" id="meshF2Monthly" name="business.meshF2Monthly"
                                   min="0" max="100000000" step="1000" required
                                   value="${fn:escapeXml(not empty cmsValues['business.meshF2Monthly'] ? cmsValues['business.meshF2Monthly'] : '20000')}"
                                   ${demoMode ? 'disabled' : ''}>
                            <small>Phụ thu cộng thêm vào gói Internet hàng tháng</small>
                        </div>

                        <div class="mesh-field">
                            <label for="meshF2Fee">Phí lắp đặt (VNĐ)</label>
                            <input type="number" id="meshF2Fee" name="business.meshF2Fee"
                                   min="0" max="100000000" step="1000" required
                                   value="${fn:escapeXml(not empty cmsValues['business.meshF2Fee'] ? cmsValues['business.meshF2Fee'] : '700000')}"
                                   ${demoMode ? 'disabled' : ''}>
                            <small>Khoản thu 1 lần khi triển khai lắp đặt Mesh F2</small>
                        </div>

                        <div class="mesh-field mesh-field-full">
                            <label for="meshF2Equipment">Thiết bị đi kèm</label>
                            <input type="text" id="meshF2Equipment" name="business.meshF2Equipment"
                                   maxlength="500" required
                                   value="${fn:escapeXml(not empty cmsValues['business.meshF2Equipment'] ? cmsValues['business.meshF2Equipment'] : '1 Modem WiFi 6 + 2 Access Point')}"
                                   ${demoMode ? 'disabled' : ''}>
                            <small>Hiển thị trong thẻ giới thiệu Mesh F2 và câu trả lời của AI</small>
                        </div>
                    </div>
                </div>

            </div>
        </form>
    </section>

</main>

</body>
</html>