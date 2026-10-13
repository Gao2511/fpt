<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!-- =========================================================
     ADMIN SIDEBAR & TOPBAR COMPONENT (DÙNG CHUNG CHO TẤT CẢ TRANG ADMIN)
     Tham số truyền vào: activeTab ('dashboard' | 'customers' | 'packages' | 'content' | 'business' | 'users' | 'email-logs' | 'settings' | 'knowledge-base')
     ========================================================= -->
<meta name="admin-csrf" content="<c:out value='${csrfToken}'/>">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css?v=5.8">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-cms.css?v=5.8">
<style id="admin-layout-core">
    :root { --admin-sidebar-expanded: 256px; --admin-sidebar-collapsed: 68px; --admin-sidebar-offset: 256px; --admin-topbar-height: 64px; }
    html, body.has-admin-sidebar { background-color: #0a1628; overflow-x: hidden; }
    body.has-admin-sidebar { --admin-sidebar-offset: var(--admin-sidebar-expanded); }
    body.has-admin-sidebar.sidebar-collapsed { --admin-sidebar-offset: var(--admin-sidebar-collapsed); }
    .admin-sidebar { position: fixed; top: 0; left: 0; bottom: 0; width: var(--admin-sidebar-offset, 256px); background: #0b1222; border-right: 1px solid rgba(148,163,184,0.14); z-index: 1000; display: flex; flex-direction: column; }
    .sidebar-brand { height: 64px; min-height: 64px; padding: 0 14px; display: flex; align-items: center; justify-content: space-between; gap: 8px; border-bottom: 1px solid rgba(255,255,255,0.08); }
    .admin-sidebar .admin-sidebar-nav.header-nav { flex: 1; overflow-y: auto; overflow-x: hidden; padding: 6px 10px 10px; display: flex; flex-direction: column; gap: 4px; width: 100%; order: 0; }
    .sidebar-group, .sidebar-group-items { display: flex; flex-direction: column; gap: 1px; }
    .sidebar-group:not(.is-open) .sidebar-group-items { display: none; }
    .sidebar-group-header-btn { width: 100%; display: flex; align-items: center; justify-content: space-between; background: transparent; border: none; color: #64748b; font-size: 10px; line-height: 1.2; font-weight: 800; text-transform: uppercase; letter-spacing: 0.08em; padding: 2px 10px; cursor: pointer; }
    .admin-sidebar .sidebar-link, .admin-sidebar .sidebar-group-toggle, .admin-sidebar .admin-sidebar-nav.header-nav a.sidebar-link { display: flex; align-items: center; gap: 10px; width: 100%; padding: 5.5px 10px; border-radius: 8px; font-size: 13px; line-height: 1.25; font-weight: 600; color: #cbd5e1; text-decoration: none; background: transparent; border: 1px solid transparent; box-sizing: border-box; }
    .admin-sidebar .sidebar-group-toggle { justify-content: space-between; cursor: pointer; }
    .admin-sidebar .sidebar-toggle-left { display: inline-flex; align-items: center; gap: 10px; min-width: 0; }
    .admin-sidebar .sidebar-link.active, .admin-sidebar .admin-sidebar-nav.header-nav a.sidebar-link.active { background: linear-gradient(135deg, #f37021, #ff8c42); color: #fff; font-weight: 700; }
    .admin-sidebar .sidebar-submenu { display: flex; flex-direction: column; gap: 1px; margin-left: 11px; padding-left: 9px; border-left: 1px solid rgba(148,163,184,0.18); max-height: 0; opacity: 0; overflow: hidden; pointer-events: none; }
    .admin-sidebar .sidebar-group-expandable.is-open .sidebar-submenu { max-height: 520px; opacity: 1; margin-top: 2px; pointer-events: auto; }
    .admin-sidebar .sidebar-sublink, .admin-sidebar .admin-sidebar-nav.header-nav a.sidebar-link.sidebar-sublink { padding: 4.5px 9px; font-size: 12.5px; line-height: 1.25; border-radius: 7px; gap: 8px; }
    .admin-sidebar svg { flex-shrink: 0; }
    .admin-header.admin-topbar { min-height: 64px !important; height: 64px !important; margin-left: var(--admin-sidebar-offset, 256px); width: calc(100% - var(--admin-sidebar-offset, 256px)); padding: 0 24px; display: flex; flex-wrap: nowrap !important; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 90; box-sizing: border-box; }
    .admin-topbar .header-left { display: flex; align-items: center; gap: 12px; flex: 1 1 auto; min-width: 0; overflow: hidden; }
    .admin-topbar .header-right { display: flex; align-items: center; gap: 12px; flex: 0 0 auto; white-space: nowrap; }
    body.has-admin-sidebar .admin-main, body.has-admin-sidebar .settings-page { margin-left: var(--admin-sidebar-offset, 256px); width: calc(100% - var(--admin-sidebar-offset, 256px)); max-width: none; box-sizing: border-box; }
    @media (max-width: 1024px) {
        body.has-admin-sidebar, body.has-admin-sidebar.sidebar-collapsed { --admin-sidebar-offset: 0px; }
        .admin-sidebar { width: 260px; transform: translateX(-105%); pointer-events: none; }
        body.sidebar-mobile-open .admin-sidebar { transform: translateX(0); pointer-events: auto; }
        .mobile-menu-toggle { display: inline-flex; }
        .admin-header.admin-topbar, body.has-admin-sidebar .admin-main, body.has-admin-sidebar .settings-page, .admin-demo-banner { margin-left: 0; width: 100%; }
    }
    @media (max-width: 768px) {
        .admin-header.admin-topbar { padding: 0 10px; gap: 8px; }
        .topbar-brand-pill, .topbar-sep, .header-view-site span, .user-text { display: none; }
        .admin-topbar .header-right { gap: 6px; }
        .admin-topbar .user-info { padding-left: 8px; gap: 6px; }
    }
    @media (max-width: 520px) { .admin-topbar .role-badge { display: none; } }
</style>
<script src="${pageContext.request.contextPath}/js/admin-security.js" defer></script>
<script>
    (function () {
        try {
            document.body.classList.add('has-admin-sidebar');
            if (window.localStorage && localStorage.getItem('fpt_admin_sidebar_collapsed') === '1') {
                document.body.classList.add('sidebar-collapsed');
            }
        } catch (e) {}
    })();
</script>

<c:set var="currentKbTab" value="${not empty requestScope.kbTab ? requestScope.kbTab : (not empty param.tab ? param.tab : 'dashboard')}"/>
<c:set var="isAiSectionActive" value="${param.activeTab == 'knowledge-base' || param.activeTab == 'settings'}"/>

<c:if test="${demoMode}">
    <div data-demo-admin role="status" class="admin-demo-banner">
        CHẾ ĐỘ DEMO — KHÔNG THAY ĐỔI DỮ LIỆU THẬT
    </div>
</c:if>

<!-- MOBILE BACKDROP -->
<div class="admin-sidebar-backdrop" id="adminSidebarBackdrop" aria-hidden="true"></div>

<!-- LEFT SIDEBAR NAVIGATION -->
<aside class="admin-sidebar" id="adminSidebar" aria-label="Điều hướng Quản trị Hệ thống">
    <!-- SIDEBAR BRAND HEADER -->
    <div class="sidebar-brand">
        <a href="${pageContext.request.contextPath}/admin/dashboard" class="logo" title="FPT Sale Manager — Trang tổng quan">
            <span class="logo-fpt">FPT</span>
            <div class="logo-text">
                <strong>Sale Manager</strong>
                <small>Hệ thống Quản trị FPT</small>
            </div>
        </a>
        <button type="button"
                class="sidebar-collapse-btn"
                id="adminSidebarCollapseBtn"
                aria-label="Thu gọn hoặc mở rộng thanh bên"
                aria-expanded="true"
                title="Thu gọn / Mở rộng menu">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <polyline points="15 18 9 12 15 6"/>
            </svg>
        </button>
    </div>

    <!-- SCROLLABLE NAVIGATION AREA -->
    <nav class="admin-sidebar-nav header-nav" aria-label="Menu chức năng Admin">

        <!-- =====================================================
             GROUP 1: TỔNG QUAN
             ===================================================== -->
        <div class="sidebar-group is-open" data-sidebar-group="overview">
            <div class="sidebar-group-label">Tổng quan</div>
            <div class="sidebar-group-items">
                <a href="${pageContext.request.contextPath}/admin/dashboard"
                   class="sidebar-link ${param.activeTab == 'dashboard' ? 'active' : ''}"
                   data-tooltip="Dashboard"
                   ${param.activeTab == 'dashboard' ? 'aria-current="page"' : ''}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <rect x="3" y="3" width="7" height="7" rx="1"/>
                        <rect x="14" y="3" width="7" height="7" rx="1"/>
                        <rect x="14" y="14" width="7" height="7" rx="1"/>
                        <rect x="3" y="14" width="7" height="7" rx="1"/>
                    </svg>
                    <span class="sidebar-label">Dashboard</span>
                </a>
            </div>
        </div>

        <!-- =====================================================
             GROUP 2: QUẢN LÝ WEBSITE
             ===================================================== -->
        <div class="sidebar-group is-open" data-sidebar-group="website">
            <button type="button" class="sidebar-group-label sidebar-group-header-btn" data-group-toggle="website" aria-expanded="true">
                <span>Quản lý Website</span>
                <svg class="sidebar-group-caret" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <polyline points="6 9 12 15 18 9"/>
                </svg>
            </button>
            <div class="sidebar-group-items">
                <!-- Nội dung trang -->
                <a href="${pageContext.request.contextPath}/admin/content"
                   class="sidebar-link ${param.activeTab == 'content' ? 'active' : ''}"
                   data-nav-item="content"
                   data-tooltip="Nội dung trang"
                   ${param.activeTab == 'content' ? 'aria-current="page"' : ''}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <rect x="3" y="3" width="18" height="18" rx="2"/>
                        <path d="M3 9h18M9 9v12"/>
                    </svg>
                    <span class="sidebar-label">Nội dung trang</span>
                </a>

                <!-- Gói cước / Sản phẩm -->
                <a href="${pageContext.request.contextPath}/admin/packages"
                   class="sidebar-link ${param.activeTab == 'packages' ? 'active' : ''}"
                   data-tooltip="Gói cước / Sản phẩm"
                   ${param.activeTab == 'packages' ? 'aria-current="page"' : ''}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                        <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                        <line x1="12" y1="22.08" x2="12" y2="12"/>
                    </svg>
                    <span class="sidebar-label">Gói cước / Sản phẩm</span>
                </a>

                <!-- Media / Hình ảnh (khu vực #cms-images trong /admin/content) -->
                <a href="${pageContext.request.contextPath}/admin/content#cms-images"
                   class="sidebar-link"
                   data-hash-target="#cms-images"
                   data-tooltip="Media / Hình ảnh">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <circle cx="8.5" cy="8.5" r="1.5"/>
                        <polyline points="21 15 16 10 5 21"/>
                    </svg>
                    <span class="sidebar-label">Media / Hình ảnh</span>
                </a>

                <!-- Thông tin kinh doanh -->
                <a href="${pageContext.request.contextPath}/admin/business"
                   class="sidebar-link ${param.activeTab == 'business' ? 'active' : ''}"
                   data-nav-item="business"
                   data-tooltip="Thông tin kinh doanh"
                   ${param.activeTab == 'business' ? 'aria-current="page"' : ''}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <rect x="3" y="7" width="18" height="14" rx="2"/>
                        <path d="M8 7V3h8v4M3 12h18M10 12v3h4v-3"/>
                    </svg>
                    <span class="sidebar-label">Thông tin kinh doanh</span>
                </a>
            </div>
        </div>

        <!-- =====================================================
             GROUP 3: KHÁCH HÀNG
             ===================================================== -->
        <div class="sidebar-group is-open" data-sidebar-group="customers">
            <button type="button" class="sidebar-group-label sidebar-group-header-btn" data-group-toggle="customers" aria-expanded="true">
                <span>Khách hàng</span>
                <svg class="sidebar-group-caret" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <polyline points="6 9 12 15 18 9"/>
                </svg>
            </button>
            <div class="sidebar-group-items">
                <!-- Danh sách khách hàng -->
                <a href="${pageContext.request.contextPath}/admin/customers"
                   class="sidebar-link ${param.activeTab == 'customers' ? 'active' : ''}"
                   data-tooltip="Danh sách khách hàng"
                   ${param.activeTab == 'customers' ? 'aria-current="page"' : ''}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                        <circle cx="9" cy="7" r="4"/>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                    </svg>
                    <span class="sidebar-label">Danh sách khách hàng</span>
                </a>

                <!-- Thông báo / Lịch sử Email -->
                <c:if test="${!demoMode}">
                    <a href="${pageContext.request.contextPath}/admin/email-logs"
                       class="sidebar-link ${param.activeTab == 'email-logs' ? 'active' : ''}"
                       data-tooltip="Thông báo / Lịch sử Email"
                       ${param.activeTab == 'email-logs' ? 'aria-current="page"' : ''}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                            <polyline points="22,6 12,13 2,6"/>
                        </svg>
                        <span class="sidebar-label">Thông báo / Lịch sử Email</span>
                    </a>
                </c:if>
            </div>
        </div>

        <!-- =====================================================
             GROUP 4: AI & CHATBOT (COLLAPSIBLE GROUP)
             ===================================================== -->
        <c:if test="${!demoMode}">
            <div class="sidebar-group sidebar-group-expandable ${isAiSectionActive ? 'is-open group-active' : ''}" id="sidebarGroupAi">
                <div class="sidebar-group-label">AI &amp; Chatbot</div>
                <a href="${pageContext.request.contextPath}/admin/settings#api-key"
                   class="sidebar-link ${param.activeTab == 'settings' ? 'active' : ''}"
                   id="adminAiSettingsLink"
                   data-tooltip="Cấu hình AI &amp; API key"
                   ${param.activeTab == 'settings' ? 'aria-current="page"' : ''}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <circle cx="8" cy="15" r="5"/><path d="M11.5 11.5 21 2m-3 3 3 3m-6 0 3 3"/>
                    </svg>
                    <span class="sidebar-label">Cấu hình AI &amp; API key</span>
                </a>
                <button type="button"
                        class="sidebar-group-toggle ${param.activeTab == 'knowledge-base' ? 'group-active' : ''}"
                        id="sidebarAiToggleBtn"
                        aria-expanded="${isAiSectionActive ? 'true' : 'false'}"
                        aria-controls="sidebarSubmenuAi"
                        data-tooltip="AI &amp; Chatbot">
                    <span class="sidebar-toggle-left">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M12 2a4 4 0 0 1 4 4v1h1a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3h-1v1a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-1H7a3 3 0 0 1-3-3v-6a3 3 0 0 1 3-3h1V6a4 4 0 0 1 4-4z"/>
                            <circle cx="9.5" cy="12.5" r="1"/>
                            <circle cx="14.5" cy="12.5" r="1"/>
                        </svg>
                        <span class="sidebar-label">Kho tri thức &amp; RAG</span>
                    </span>
                    <svg class="sidebar-chevron" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </button>

                <div class="sidebar-submenu" id="sidebarSubmenuAi" role="group" aria-label="Menu con AI và Chatbot">
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && currentKbTab == 'dashboard' ? 'active' : ''}" data-tooltip="Tổng quan AI" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=dashboard"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg><span class="sidebar-label">Tổng quan AI</span></a>
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && (currentKbTab == 'upload' || currentKbTab == 'imports') ? 'active' : ''}" data-tooltip="Tải lên &amp; Xuất bản" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=upload"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg><span class="sidebar-label">Tải lên &amp; Xuất bản</span></a>
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && currentKbTab == 'documents' ? 'active' : ''}" data-tooltip="Quản lý tài liệu" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg><span class="sidebar-label">Quản lý tài liệu</span></a>
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && currentKbTab == 'faqs' ? 'active' : ''}" data-tooltip="FAQ" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=faqs"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg><span class="sidebar-label">FAQ</span></a>
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && currentKbTab == 'indexing' ? 'active' : ''}" data-tooltip="Indexing" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=indexing"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg><span class="sidebar-label">Indexing</span></a>
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && currentKbTab == 'verify' ? 'active' : ''}" data-tooltip="Kiểm tra AI RAG" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=verify"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg><span class="sidebar-label">Kiểm tra AI RAG</span></a>
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && currentKbTab == 'playground' ? 'active' : ''}" data-tooltip="RAG Playground" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=playground"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><polygon points="5 3 19 12 5 21 5 3"/></svg><span class="sidebar-label">RAG Playground</span></a>
                    <a class="sidebar-link sidebar-sublink ${param.activeTab == 'knowledge-base' && (currentKbTab == 'advanced' || currentKbTab == 'rag-config' || currentKbTab == 'audit') ? 'active' : ''}" data-tooltip="Cài đặt nâng cao" href="${pageContext.request.contextPath}/admin/knowledge-base?tab=advanced"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg><span class="sidebar-label">Cài đặt nâng cao</span></a>
                </div>
            </div>
        </c:if>

        <!-- =====================================================
             GROUP 5: HỆ THỐNG
             ===================================================== -->
        <div class="sidebar-group is-open" data-sidebar-group="system">
            <button type="button" class="sidebar-group-label sidebar-group-header-btn" data-group-toggle="system" aria-expanded="true">
                <span>Hệ thống</span>
                <svg class="sidebar-group-caret" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <polyline points="6 9 12 15 18 9"/>
                </svg>
            </button>
            <div class="sidebar-group-items">
                <!-- Cài đặt chung -->
                <a href="${pageContext.request.contextPath}/admin/business#cms-contact"
                   class="sidebar-link"
                   data-hash-target="#cms-contact"
                   data-tooltip="Cài đặt chung">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/>
                        <circle cx="12" cy="12" r="3"/>
                    </svg>
                    <span class="sidebar-label">Cài đặt chung</span>
                </a>

                <!-- Quản lý tài khoản / phân quyền -->
                <c:if test="${!demoMode}">
                    <a href="${pageContext.request.contextPath}/admin/users"
                       class="sidebar-link ${param.activeTab == 'users' ? 'active' : ''}"
                       data-tooltip="Quản lý tài khoản"
                       ${param.activeTab == 'users' ? 'aria-current="page"' : ''}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                        <span class="sidebar-label">Quản lý tài khoản</span>
                    </a>
                </c:if>

                <!-- Đăng xuất -->
                <a href="${pageContext.request.contextPath}/logout"
                   class="sidebar-link sidebar-link-logout"
                   data-tooltip="Đăng xuất">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                        <polyline points="16 17 21 12 16 7"/>
                        <line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                    <span class="sidebar-label">Đăng xuất</span>
                </a>
            </div>
        </div>
    </nav>
    <div class="sidebar-build-meta" id="adminBuildMeta" title="Build: <%= utils.BuildInfo.getSummary() %>" style="padding:10px 16px; border-top:1px solid rgba(255,255,255,0.08); font-size:11px; color:#94a3b8; display:flex; align-items:center; justify-content:space-between; gap:6px; white-space:nowrap; overflow:hidden;">
        <span class="sidebar-label" style="overflow:hidden; text-overflow:ellipsis;">v<%= utils.BuildInfo.getVersion() %> &middot; <code><%= utils.BuildInfo.getShortCommitSha() %></code></span>
        <span class="sidebar-label" style="padding:1px 6px; border-radius:4px; background:rgba(243,112,33,0.18); color:#fdba74; font-weight:700; font-size:10px;"><%= utils.BuildInfo.getEnvironment() %></span>
    </div>
</aside>

<!-- TOP HEADER BAR -->
<header class="admin-header admin-topbar">
    <div class="header-left">
        <button type="button"
                class="mobile-menu-toggle"
                id="adminMobileMenuBtn"
                aria-label="Mở menu điều hướng"
                aria-controls="adminSidebar"
                aria-expanded="false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <line x1="3" y1="6" x2="21" y2="6"/>
                <line x1="3" y1="12" x2="21" y2="12"/>
                <line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
        </button>
        <div class="topbar-breadcrumb">
            <span class="topbar-brand-pill">FPT Gia Lai CMS</span>
            <span class="topbar-sep">/</span>
            <strong class="topbar-current-page">
                <c:choose>
                    <c:when test="${param.activeTab == 'dashboard'}">Tổng quan Dashboard</c:when>
                    <c:when test="${param.activeTab == 'customers'}">Quản lý Khách hàng</c:when>
                    <c:when test="${param.activeTab == 'packages'}">Gói cước &amp; Sản phẩm</c:when>
                    <c:when test="${param.activeTab == 'content'}">Nội dung Trang &amp; Media</c:when>
                    <c:when test="${param.activeTab == 'business'}">Thông tin Kinh doanh &amp; Cài đặt chung</c:when>
                    <c:when test="${param.activeTab == 'users'}">Quản lý Tài khoản</c:when>
                    <c:when test="${param.activeTab == 'email-logs'}">Thông báo &amp; Lịch sử Email</c:when>
                    <c:when test="${param.activeTab == 'settings'}">Cấu hình AI &amp; API key</c:when>
                    <c:when test="${param.activeTab == 'knowledge-base'}">AI Knowledge Base &amp; RAG</c:when>
                    <c:otherwise>Bảng điều khiển</c:otherwise>
                </c:choose>
            </strong>
        </div>
    </div>

    <div class="header-right">
        <a href="${pageContext.request.contextPath}/home" target="_blank" rel="noopener" class="header-view-site" title="Xem trang khách hàng">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
                <polyline points="15 3 21 3 21 9"/>
                <line x1="10" y1="14" x2="21" y2="3"/>
            </svg>
            <span>Xem Web</span>
        </a>
        <span class="role-badge">${demoMode ? 'Demo' : 'Admin'}</span>
        <div class="user-info">
            <c:choose>
                <c:when test="${not empty sessionScope.user.avatarUrl}">
                    <img src="${pageContext.request.contextPath}/${sessionScope.user.avatarUrl}" class="avatar" alt="Admin"
                         onerror="this.src='${pageContext.request.contextPath}/${sessionScope.user.defaultAvatar}'">
                </c:when>
                <c:otherwise>
                    <img src="${pageContext.request.contextPath}/${sessionScope.user.defaultAvatar}" class="avatar" alt="Admin">
                </c:otherwise>
            </c:choose>
            <div class="user-text">
                <strong><c:out value="${sessionScope.user.fullName != null ? sessionScope.user.fullName : sessionScope.user.username}"/></strong>
                <small><span class="online-dot"></span> Online</small>
            </div>
            <a href="${pageContext.request.contextPath}/logout" class="logout-btn" title="Đăng xuất khỏi hệ thống">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
            </a>
        </div>
    </div>
</header>

<script>
    (function () {
        var body = document.body;
        var sidebar = document.getElementById('adminSidebar');
        var collapseBtn = document.getElementById('adminSidebarCollapseBtn');
        var mobileBtn = document.getElementById('adminMobileMenuBtn');
        var backdrop = document.getElementById('adminSidebarBackdrop');
        var aiGroup = document.getElementById('sidebarGroupAi');
        var aiToggleBtn = document.getElementById('sidebarAiToggleBtn');
        var STORAGE_COLLAPSED = 'fpt_admin_sidebar_collapsed';
        var STORAGE_AI_OPEN = 'fpt_admin_ai_group_open';

        function syncCollapseAria() {
            var isCollapsed = body.classList.contains('sidebar-collapsed');
            if (collapseBtn) {
                collapseBtn.setAttribute('aria-expanded', isCollapsed ? 'false' : 'true');
            }
        }

        function setMobileDrawer(open) {
            if (open) {
                body.classList.add('sidebar-mobile-open');
                if (mobileBtn) mobileBtn.setAttribute('aria-expanded', 'true');
                if (backdrop) backdrop.setAttribute('aria-hidden', 'false');
            } else {
                body.classList.remove('sidebar-mobile-open');
                if (mobileBtn) mobileBtn.setAttribute('aria-expanded', 'false');
                if (backdrop) backdrop.setAttribute('aria-hidden', 'true');
            }
        }

        if (collapseBtn) {
            syncCollapseAria();
            collapseBtn.addEventListener('click', function () {
                if (window.innerWidth <= 1024) {
                    setMobileDrawer(false);
                    return;
                }
                var collapsed = body.classList.toggle('sidebar-collapsed');
                syncCollapseAria();
                try {
                    localStorage.setItem(STORAGE_COLLAPSED, collapsed ? '1' : '0');
                } catch (e) {}
            });
        }

        if (mobileBtn) {
            mobileBtn.addEventListener('click', function () {
                var open = !body.classList.contains('sidebar-mobile-open');
                setMobileDrawer(open);
            });
        }

        if (backdrop) {
            backdrop.addEventListener('click', function () {
                setMobileDrawer(false);
            });
        }

        if (aiGroup && aiToggleBtn) {
            var serverActive = aiGroup.classList.contains('group-active');
            try {
                var savedAi = localStorage.getItem(STORAGE_AI_OPEN);
                if (serverActive) {
                    aiGroup.classList.add('is-open');
                    aiToggleBtn.setAttribute('aria-expanded', 'true');
                } else if (savedAi === '1') {
                    aiGroup.classList.add('is-open');
                    aiToggleBtn.setAttribute('aria-expanded', 'true');
                } else {
                    aiGroup.classList.remove('is-open');
                    aiToggleBtn.setAttribute('aria-expanded', 'false');
                }
            } catch (e) {}

            aiToggleBtn.addEventListener('click', function () {
                if (window.innerWidth > 1024 && body.classList.contains('sidebar-collapsed')) {
                    body.classList.remove('sidebar-collapsed');
                    syncCollapseAria();
                    try { localStorage.setItem(STORAGE_COLLAPSED, '0'); } catch (e) {}
                    aiGroup.classList.add('is-open');
                    aiToggleBtn.setAttribute('aria-expanded', 'true');
                    return;
                }
                var open = aiGroup.classList.toggle('is-open');
                aiToggleBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
                try {
                    localStorage.setItem(STORAGE_AI_OPEN, open ? '1' : '0');
                } catch (e) {}
            });
        }

        // Support collapsible group headers for Website, Customers, and System groups
        if (sidebar) {
            var groupToggles = sidebar.querySelectorAll('[data-group-toggle]');
            for (var g = 0; g < groupToggles.length; g++) {
                (function (btn) {
                    btn.addEventListener('click', function () {
                        var groupEl = btn.closest('.sidebar-group');
                        if (!groupEl) return;
                        var isOpen = groupEl.classList.toggle('is-open');
                        btn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
                    });
                })(groupToggles[g]);
            }
        }

        // Close mobile drawer after clicking a link or pressing Escape
        if (sidebar) {
            var links = sidebar.querySelectorAll('a.sidebar-link');
            for (var i = 0; i < links.length; i++) {
                links[i].addEventListener('click', function () {
                    if (window.innerWidth <= 1024) {
                        setMobileDrawer(false);
                    }
                });
            }
        }

        document.addEventListener('keydown', function (ev) {
            if (ev.key === 'Escape' && body.classList.contains('sidebar-mobile-open')) {
                setMobileDrawer(false);
                if (mobileBtn) mobileBtn.focus();
            }
        });

        // Highlight hash-based secondary indicator without removing primary activeTab class
        function syncHashSubIndicator() {
            if (!sidebar) return;
            var h = window.location.hash;
            var hashLinks = sidebar.querySelectorAll('[data-hash-target]');
            for (var j = 0; j < hashLinks.length; j++) {
                var target = hashLinks[j].getAttribute('data-hash-target');
                if (h && h === target) {
                    hashLinks[j].classList.add('hash-active');
                } else {
                    hashLinks[j].classList.remove('hash-active');
                }
            }
        }
        window.addEventListener('hashchange', syncHashSubIndicator);
        syncHashSubIndicator();
    })();
</script>
