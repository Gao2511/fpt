<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu Hình AI - FPT Sale Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-package.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-settings.css">
</head>
<body>

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
        <a href="${pageContext.request.contextPath}/admin/settings" class="active">
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
<main class="settings-page">
    <div class="settings-container">

        <!-- HEADER -->
        <div class="settings-header">
            <h1>Cấu Hình AI Chatbot</h1>
            <p>Thay đổi API Key, Model và System Prompt của AI trực tiếp trên web</p>
        </div>

        <!-- FLASH MESSAGE -->
        <c:if test="${not empty message}">
            <div class="alert alert-${messageType}">
                <span>${message}</span>
                <button onclick="this.parentElement.remove()" class="alert-close">×</button>
            </div>
        </c:if>

        <!-- FORM -->
        <form method="post" action="${pageContext.request.contextPath}/admin/settings" id="settingsForm">
            <input type="hidden" name="action" value="updateAI">

            <div class="settings-grid">

                <!-- CỘT TRÁI -->
                <div class="settings-card">
                    <div class="card-header-dark">
                        <div class="card-icon">
                            <svg viewBox="0 0 24 24">
                                <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                            </svg>
                        </div>
                        <div>
                            <h3>Thông tin kết nối AI</h3>
                            <p>API Key và Model đang sử dụng</p>
                        </div>
                    </div>

                    <!-- API KEY -->
                    <div class="form-group-dark">
                        <label class="form-label-dark">
                            <svg viewBox="0 0 24 24">
                                <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                            </svg>
                            API Key <span class="req">*</span>
                        </label>
                        <div class="input-dark-wrap">
                            <input type="text" name="gemini_api_key" id="gemini_api_key"
                                   class="input-dark"
                                   value="${settingsMap['gemini_api_key']}"
                                   placeholder="Nhập API Key của bạn...">
                            <button type="button" onclick="toggleApiKey()" class="input-btn" title="Hiện/Ẩn">
                                <svg id="eyeIcon" viewBox="0 0 24 24">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                            </button>
                        </div>
                        <small class="hint-dark">
                            <svg viewBox="0 0 24 24">
                                <circle cx="12" cy="12" r="10"/>
                                <line x1="12" y1="16" x2="12" y2="12"/>
                                <line x1="12" y1="8" x2="12.01" y2="8"/>
                            </svg>
                            Key được lưu trong DB, có thể thay đổi bất cứ lúc nào
                        </small>
                    </div>

                    <!-- MODEL -->
                    <div class="form-group-dark">
                        <label class="form-label-dark">
                            <svg viewBox="0 0 24 24">
                                <rect x="2" y="7" width="20" height="14" rx="2"/>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                            </svg>
                            Model AI <span class="req">*</span>
                        </label>
                        <select name="gemini_model" id="gemini_model" class="select-dark">
                            <c:choose>
                                <c:when test="${not empty settingsMap['gemini_model']}">
                                    <option value="${settingsMap['gemini_model']}" selected>
                                        ${settingsMap['gemini_model']}
                                    </option>
                                </c:when>
                                <c:otherwise>
                                    <option value="">-- Chưa cấu hình model --</option>
                                </c:otherwise>
                            </c:choose>
                        </select>
                        <small class="hint-dark">
                            <svg viewBox="0 0 24 24">
                                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                            </svg>
                            Model càng mạnh → trả lời càng thông minh nhưng chậm hơn
                        </small>
                    </div>

                    <!-- CUSTOM MODEL -->
                    <div class="form-group-dark">
                        <label class="form-label-dark">
                            <svg viewBox="0 0 24 24">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                            </svg>
                            Đổi model khác
                        </label>
                        <input type="text" id="customModel"
                               class="input-dark"
                               style="padding-right:16px;"
                               placeholder="Nhập tên model muốn đổi..."
                               onkeypress="if(event.key==='Enter'){event.preventDefault();applyCustomModel(this.value);}">
                        <small class="hint-dark">
                            <svg viewBox="0 0 24 24">
                                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
                            </svg>
                            Nhập tên model rồi nhấn Enter để áp dụng vào dropdown trên
                        </small>
                    </div>
                </div>

                <!-- CỘT PHẢI -->
                <div class="settings-card">
                    <div class="card-header-dark">
                        <div class="card-icon">
                            <svg viewBox="0 0 24 24">
                                <rect x="3" y="11" width="18" height="10" rx="2"/>
                                <circle cx="12" cy="16" r="2"/>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                            </svg>
                        </div>
                        <div>
                            <h3>System Prompt</h3>
                            <p>Tính cách và kiến thức của AI</p>
                        </div>
                    </div>

                    <div class="form-group-dark">
                        <label class="form-label-dark">
                            <svg viewBox="0 0 24 24">
                                <path d="M12 20h9"/>
                                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                            </svg>
                            Hướng dẫn cho AI
                        </label>
                        <textarea name="gemini_system_prompt" id="gemini_system_prompt"
                                  class="textarea-dark"
                                  placeholder="Mô tả vai trò, giọng điệu, thông tin gói cước... cho AI">${settingsMap['gemini_system_prompt']}</textarea>
                        <small class="hint-dark">
                            <svg viewBox="0 0 24 24">
                                <circle cx="12" cy="12" r="10"/>
                                <line x1="12" y1="16" x2="12" y2="12"/>
                                <line x1="12" y1="8" x2="12.01" y2="8"/>
                            </svg>
                            Mô tả vai trò, giọng điệu, thông tin gói cước... cho AI
                        </small>
                    </div>
                </div>

            </div>

            <!-- NOTE CARD -->
            <div class="note-card">
                <label class="form-label-dark">
                    <svg viewBox="0 0 24 24">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                        <line x1="16" y1="13" x2="8" y2="13"/>
                        <line x1="16" y1="17" x2="8" y2="17"/>
                        <polyline points="10 9 9 9 8 9"/>
                    </svg>
                    Ghi chú thay đổi
                    <span style="color:#64748b;font-weight:400;font-style:italic;font-size:12px;">(không bắt buộc)</span>
                </label>
                <input type="text" name="change_note"
                       class="input-dark"
                       style="padding-right:16px;"
                       placeholder="VD: Đổi key do hết quota, chuyển sang key mới cho team...">
                <small class="hint-dark">
                    <svg viewBox="0 0 24 24">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="16" x2="12" y2="12"/>
                        <line x1="12" y1="8" x2="12.01" y2="8"/>
                    </svg>
                    Ghi chú sẽ được lưu vào lịch sử khi bạn đổi API Key
                </small>
            </div>

            <!-- ACTIONS -->
            <div class="actions-bar">
                <button type="button" class="btn-dark btn-dark-secondary" onclick="resetForm()">
                    <svg viewBox="0 0 24 24">
                        <polyline points="1 4 1 10 7 10"/>
                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
                    </svg>
                    Khôi phục
                </button>
                <button type="submit" class="btn-dark btn-dark-primary">
                    <svg viewBox="0 0 24 24">
                        <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                        <polyline points="17 21 17 13 7 13 7 21"/>
                        <polyline points="7 3 7 8 15 8"/>
                    </svg>
                    Lưu cấu hình
                </button>
            </div>
        </form>

        <!-- INFO BOTTOM -->
        <div class="info-bottom-dark">
            <svg viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
            </svg>
            <span>
                Cấu hình được áp dụng <strong>ngay lập tức</strong> cho chatbot AI.
                Không cần khởi động lại server.
            </span>
        </div>

        <!-- ============================================================ -->
        <!-- BẢNG LỊCH SỬ API KEY                                          -->
        <!-- ============================================================ -->
        <div class="history-card">
            <div class="history-header">
                <div class="history-header-left">
                    <div class="card-icon">
                        <svg viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="10"/>
                            <polyline points="12 6 12 12 16 14"/>
                        </svg>
                    </div>
                    <div>
                        <h3>Lịch sử thay đổi API Key</h3>
                        <p>Hiển thị ${apiKeyHistory.size()} / ${totalHistory} lần thay đổi gần nhất</p>
                    </div>
                </div>
                <c:if test="${totalHistory > 0}">
                    <a href="${pageContext.request.contextPath}/admin/settings?action=deleteAllHistory"
                       onclick="return confirm('Bạn có chắc muốn xóa TOÀN BỘ ${totalHistory} dòng lịch sử? Hành động này KHÔNG thể hoàn tác!');"
                       class="btn-clear-all-dark">
                        <svg viewBox="0 0 24 24">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                        </svg>
                        Xóa toàn bộ
                    </a>
                </c:if>
            </div>

            <c:choose>
                <c:when test="${empty apiKeyHistory}">
                    <div class="empty-state-dark">
                        <svg viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="10"/>
                            <polyline points="12 6 12 12 16 14"/>
                        </svg>
                        <p>Chưa có lịch sử thay đổi nào</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="history-table-wrap">
                        <table class="history-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>API Key</th>
                                    <th>Model</th>
                                    <th>Người đổi</th>
                                    <th>Ghi chú</th>
                                    <th>Thời gian</th>
                                    <th style="text-align:center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${apiKeyHistory}" var="h">
                                    <tr>
                                        <td class="history-id">#${h.id}</td>
                                        <td>
                                            <span class="api-key-badge">${h.apiKeyMasked}</span>
                                        </td>
                                        <td>
                                            <span class="model-badge">${h.model}</span>
                                        </td>
                                        <td>
                                            <span class="user-name">${h.changedByName}</span>
                                        </td>
                                        <td>
                                            <span class="note-text">${h.note}</span>
                                        </td>
                                        <td class="time-text">
                                            <fmt:formatDate value="${h.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                        </td>
                                        <td style="text-align:center;">
                                            <a href="${pageContext.request.contextPath}/admin/settings?action=deleteHistory&id=${h.id}"
                                               onclick="return confirm('Bạn có chắc muốn xóa dòng lịch sử #${h.id}?');"
                                               class="btn-delete-row" title="Xóa">
                                                <svg viewBox="0 0 24 24">
                                                    <polyline points="3 6 5 6 21 6"/>
                                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                                                </svg>
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</main>

<script>
    // ===== TOGGLE HIỆN/ẨN API KEY =====
    function toggleApiKey() {
        const input = document.getElementById('gemini_api_key');
        const icon = document.getElementById('eyeIcon');
        if (input.type === 'password') {
            input.type = 'text';
            icon.innerHTML = '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>';
        } else {
            input.type = 'password';
            icon.innerHTML = '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>';
        }
    }

    // ===== ÁP DỤNG CUSTOM MODEL VÀO DROPDOWN =====
    function applyCustomModel(value) {
        value = value.trim();
        if (!value) return;

        const select = document.getElementById('gemini_model');
        let exists = false;

        for (let i = 0; i < select.options.length; i++) {
            if (select.options[i].value === value) {
                select.selectedIndex = i;
                exists = true;
                break;
            }
        }

        if (!exists) {
            const newOption = document.createElement('option');
            newOption.value = value;
            newOption.text = value;
            select.add(newOption);
            select.value = value;
        }

        document.getElementById('customModel').value = '';
    }

    // ===== KHÔI PHỤC FORM =====
    function resetForm() {
        if (confirm('Bạn có chắc muốn khôi phục về giá trị đã lưu?')) {
            location.reload();
        }
    }

    // ===== ẨN API KEY MẶC ĐỊNH =====
    document.addEventListener('DOMContentLoaded', function() {
        const apiInput = document.getElementById('gemini_api_key');
        if (apiInput && apiInput.value) {
            apiInput.type = 'password';
        }
    });
</script>

</body>
</html>
