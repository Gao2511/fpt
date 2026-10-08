<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu Hình AI Chatbot - FPT Telecom Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-package.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-settings.css?v=2.0">
</head>
<body>
<jsp:include page="/view/loading-runner.jsp" />

<!-- ============ HEADER (DÙNG CHUNG) ============ -->
<jsp:include page="/view/admin/admin-header.jsp">
    <jsp:param name="activeTab" value="settings"/>
</jsp:include>

<!-- ============ MAIN ============ -->
<main class="settings-page">
    <div class="settings-container">

        <!-- HEADER TRANG -->
        <div class="settings-header">
            <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px;">
                <div>
                    <h1>Trung Tâm Cấu Hình AI Chatbot</h1>
                    <p>Thiết lập tham số Model, API Key và System Prompt vận hành trợ lý ảo FPT Telecom</p>
                </div>
                <div class="header-status-pill">
                    <span class="status-indicator ${settingsMap['ai_chatbot_enabled'] == 'false' ? 'status-off' : 'status-on'}"></span>
                    <span>Chatbot: <strong>${settingsMap['ai_chatbot_enabled'] == 'false' ? 'Đang tạm dừng' : 'Đang hoạt động'}</strong></span>
                </div>
            </div>
        </div>

        <!-- FLASH MESSAGE -->
        <c:if test="${not empty message}">
            <div class="alert alert-${messageType}">
                <span>${message}</span>
                <button onclick="this.parentElement.remove()" class="alert-close">×</button>
            </div>
        </c:if>

        <!-- FORM CẤU HÌNH CHÍNH -->
        <form method="post" action="${pageContext.request.contextPath}/admin/settings" id="settingsForm">
            <input type="hidden" name="action" value="updateAI">

            <div class="settings-grid">

                <!-- CỘT 1: KẾT NỐI API & THAM SỐ MODEL -->
                <div class="settings-card">
                    <div class="card-header-dark">
                        <div class="card-icon">
                            <svg viewBox="0 0 24 24">
                                <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                            </svg>
                        </div>
                        <div>
                            <h3>Model AI &amp; Khóa API</h3>
                            <p>Chọn phiên bản Model Google Gemini &amp; khóa API kết nối</p>
                        </div>
                    </div>

                    <!-- 1. CHỌN MODEL AI -->
                    <div class="form-group-dark">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 8px;">
                            <label class="form-label-dark" for="modelPresetSelect" style="margin-bottom:0;">
                                <svg viewBox="0 0 24 24">
                                    <rect x="2" y="7" width="20" height="14" rx="2"/>
                                    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                                </svg>
                                Mô hình AI (Model) <span class="req">*</span>
                            </label>
                            <button type="button" class="btn-prompt-tool" onclick="fetchModelsFromApiKey(event)" title="Tự động truy vấn các model mà API Key của bạn hỗ trợ từ Google">
                                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
                                Đồng bộ từ API Key
                            </button>
                        </div>
                        <select id="modelPresetSelect" class="select-dark" onchange="handleModelPresetChange(this.value)">
                            <option value="gemini-1.5-flash" ${settingsMap['gemini_model'] == 'gemini-1.5-flash' || empty settingsMap['gemini_model'] ? 'selected' : ''}>
                                Gemini 1.5 Flash (Khuyên dùng: Siêu nhanh, thông minh, tối ưu)
                            </option>
                            <option value="gemini-2.0-flash" ${settingsMap['gemini_model'] == 'gemini-2.0-flash' ? 'selected' : ''}>
                                Gemini 2.0 Flash (Thế hệ mới nhất, phản hồi cực tốc)
                            </option>
                            <option value="gemini-1.5-pro" ${settingsMap['gemini_model'] == 'gemini-1.5-pro' ? 'selected' : ''}>
                                Gemini 1.5 Pro (Lý luận chuyên sâu, phân tích phức tạp)
                            </option>
                            <option value="gemini-1.0-pro" ${settingsMap['gemini_model'] == 'gemini-1.0-pro' ? 'selected' : ''}>
                                Gemini 1.0 Pro (Phiên bản cơ bản)
                            </option>
                            <option value="custom" ${settingsMap['gemini_model'] != 'gemini-1.5-flash' && settingsMap['gemini_model'] != 'gemini-2.0-flash' && settingsMap['gemini_model'] != 'gemini-1.5-pro' && settingsMap['gemini_model'] != 'gemini-1.0-pro' && not empty settingsMap['gemini_model'] ? 'selected' : ''}>
                                ✏️ Tùy chỉnh (Nhập model khác)...
                            </option>
                        </select>

                        <!-- Input Model thực tế gửi lên server -->
                        <div id="customModelWrap" style="margin-top: 10px; display: none;">
                            <input type="text" name="gemini_model" id="gemini_model"
                                   class="input-dark" style="padding-right:16px;"
                                   value="${not empty settingsMap['gemini_model'] ? settingsMap['gemini_model'] : 'gemini-1.5-flash'}"
                                   placeholder="Nhập mã định danh model (VD: gemini-1.5-flash)">
                        </div>
                        <small class="hint-dark">
                            <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                            Model được gọi qua endpoint chính thức của Google Gemini API v1beta.
                        </small>
                    </div>

                    <!-- 2. GEMINI API KEY -->
                    <div class="form-group-dark">
                        <label class="form-label-dark" for="gemini_api_key">
                            <svg viewBox="0 0 24 24">
                                <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                            </svg>
                            Google Gemini API Key <span class="req">*</span>
                        </label>
                        <div class="input-dark-wrap">
                            <input type="password" name="gemini_api_key" id="gemini_api_key"
                                   class="input-dark"
                                   value="${settingsMap['gemini_api_key']}"
                                   placeholder="Nhập khóa AIzaSy... của bạn">
                            <button type="button" onclick="toggleApiKey()" class="input-btn" title="Hiện/Ẩn">
                                <svg id="eyeIcon" viewBox="0 0 24 24">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                            </button>
                        </div>
                        
                        <!-- Nút Kiểm tra kết nối API trực tiếp -->
                        <div style="margin-top: 10px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                            <button type="button" id="btnTestConn" onclick="testConnection()" class="btn-test-conn">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                                </svg>
                                <span>Kiểm tra kết nối (Test Connection)</span>
                            </button>
                            <span id="testResultBadge" class="test-result-badge" style="display: none;"></span>
                        </div>
                    </div>

                    <!-- 3. THAM SỐ SINH CHỮ (TEMPERATURE & MAX TOKENS) -->
                    <div class="params-row" style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                        <div class="form-group-dark">
                            <label class="form-label-dark" for="gemini_temperature">
                                <svg viewBox="0 0 24 24"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>
                                Temperature: <span id="tempValueDisplay" style="color: #f37021; font-weight: 800;">${not empty settingsMap['gemini_temperature'] ? settingsMap['gemini_temperature'] : '0.7'}</span>
                            </label>
                            <input type="range" name="gemini_temperature" id="gemini_temperature"
                                   min="0.0" max="1.0" step="0.1"
                                   value="${not empty settingsMap['gemini_temperature'] ? settingsMap['gemini_temperature'] : '0.7'}"
                                   class="range-dark"
                                   oninput="document.getElementById('tempValueDisplay').textContent = this.value">
                            <div style="display:flex; justify-content:space-between; font-size:11px; color:#64748b; margin-top:4px;">
                                <span>0.0 (Chính xác)</span>
                                <span>1.0 (Sáng tạo)</span>
                            </div>
                        </div>

                        <div class="form-group-dark">
                            <label class="form-label-dark" for="gemini_max_tokens">
                                <svg viewBox="0 0 24 24"><polyline points="4 7 4 4 20 4 20 7"/><line x1="9" y1="20" x2="15" y2="20"/><line x1="12" y1="4" x2="12" y2="20"/></svg>
                                Max Tokens
                            </label>
                            <input type="number" name="gemini_max_tokens" id="gemini_max_tokens"
                                   min="100" max="2000" step="50"
                                   class="input-dark" style="padding-right:16px; font-family:inherit;"
                                   value="${not empty settingsMap['gemini_max_tokens'] ? settingsMap['gemini_max_tokens'] : '600'}">
                            <div style="font-size:11px; color:#64748b; margin-top:4px;">Độ dài câu trả lời (100 - 2000)</div>
                        </div>
                    </div>

                    <!-- 4. TRẠNG THÁI BẬT/TẮT CHATBOT -->
                    <div class="toggle-card">
                        <div class="toggle-info">
                            <strong>Kích hoạt Chatbot AI</strong>
                            <p>Bật hoặc tắt tính năng chatbot hỗ trợ tư vấn trên toàn hệ thống web</p>
                        </div>
                        <label class="switch-toggle">
                            <input type="checkbox" name="ai_chatbot_enabled" value="true"
                                   ${settingsMap['ai_chatbot_enabled'] != 'false' ? 'checked' : ''}>
                            <span class="switch-slider"></span>
                        </label>
                    </div>

                </div>

                <!-- CỘT 2: CHỈ DẪN HỆ THỐNG (SYSTEM PROMPT / PERSONA) -->
                <div class="settings-card">
                    <div class="card-header-dark" style="justify-content: space-between;">
                        <div style="display:flex; align-items:center; gap:12px;">
                            <div class="card-icon">
                                <svg viewBox="0 0 24 24">
                                    <path d="M12 20h9"/>
                                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                                </svg>
                            </div>
                            <div>
                                <h3>Chỉ Dẫn Hệ Thống (System Prompt)</h3>
                                <p>Định hình vai trò, tính cách và kiến thức của trợ lý ảo</p>
                            </div>
                        </div>
                        <div style="display:flex; gap:8px;">
                            <button type="button" class="btn-prompt-tool" onclick="loadFptPromptTemplate()" title="Nạp mẫu tư vấn chuẩn FPT">
                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                                Nạp mẫu FPT
                            </button>
                            <button type="button" class="btn-prompt-tool" onclick="clearPrompt()" title="Xóa trắng để tự viết">
                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
                                Xóa
                            </button>
                        </div>
                    </div>

                    <div class="form-group-dark">
                        <textarea name="gemini_system_prompt" id="gemini_system_prompt"
                                  class="textarea-dark"
                                  placeholder="Nhập vai trò và chỉ dẫn hoạt động của AI..."
                                  oninput="updatePromptStats()">${settingsMap['gemini_system_prompt']}</textarea>
                        
                        <div class="prompt-footer-stats">
                            <span id="promptCharCount">0 ký tự</span>
                            <span id="promptWordCount">0 từ</span>
                        </div>
                    </div>

                    <div class="prompt-guide-box">
                        <div class="guide-title">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                            Mẹo cấu hình AI hiệu quả:
                        </div>
                        <ul>
                            <li><strong>Vai trò:</strong> Xác định rõ "Bạn là nhân viên tư vấn FPT Telecom...".</li>
                            <li><strong>Phạm vi:</strong> Nêu rõ các gói cước chính (Giga, Sky, F-Game, FPT Play).</li>
                            <li><strong>Quy tắc an toàn:</strong> Yêu cầu AI không tiết lộ API Key, không bàn luận chính trị.</li>
                            <li><strong>Liên hệ:</strong> Cung cấp số Hotline 0932 079 469 để khách hàng liên hệ khi cần.</li>
                        </ul>
                    </div>
                </div>

            </div>

            <!-- THANH THAO TÁC (ACTION BAR) -->
            <div class="actions-bar">
                <button type="button" class="btn-dark btn-dark-secondary" onclick="resetForm()">
                    <svg viewBox="0 0 24 24">
                        <polyline points="1 4 1 10 7 10"/>
                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
                    </svg>
                    Khôi phục ban đầu
                </button>
                <button type="submit" class="btn-dark btn-dark-primary">
                    <svg viewBox="0 0 24 24">
                        <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                        <polyline points="17 21 17 13 7 13 7 21"/>
                        <polyline points="7 3 7 8 15 8"/>
                    </svg>
                    LƯU CẤU HÌNH AI
                </button>
            </div>
        </form>

        <!-- KHUNG THỬ NGHIỆM AI TRỰC TIẾP (LIVE AI PLAYGROUND) -->
        <div class="playground-card">
            <div class="playground-header">
                <div class="card-icon">
                    <svg viewBox="0 0 24 24">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                    </svg>
                </div>
                <div>
                    <h3>Thử Nghiệm Trò Chuyện Trực Tiếp (Live Playground)</h3>
                    <p>Kiểm tra ngay câu trả lời thực tế của AI dựa trên API Key và Model bạn vừa cấu hình</p>
                </div>
            </div>

            <div class="playground-chat-box">
                <div id="playgroundOutput" class="playground-output">
                    <div class="ai-msg-placeholder">Hãy đặt một câu hỏi thử nghiệm (VD: "Có gói cước nào dưới 200k không em?") để kiểm tra câu trả lời của AI.</div>
                </div>

                <div class="playground-input-row">
                    <input type="text" id="playgroundInput" class="input-dark" placeholder="Nhập câu hỏi thử nghiệm cho AI..."
                           onkeypress="if(event.key==='Enter'){event.preventDefault();sendPlaygroundMessage();}">
                    <button type="button" id="btnPlaygroundSend" onclick="sendPlaygroundMessage()" class="btn-dark btn-dark-primary">
                        <span>Gửi câu hỏi</span>
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                    </button>
                </div>
            </div>
        </div>

    </div>
</main>

<script>
    const CONTEXT_PATH = '${pageContext.request.contextPath}';

    // 1. TOGGLE HIỆN/ẨN API KEY
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

    // 2. XỬ LÝ CHỌN MODEL PRESET HOẶC CUSTOM
    function handleModelPresetChange(val) {
        const customWrap = document.getElementById('customModelWrap');
        const modelInput = document.getElementById('gemini_model');
        if (val === 'custom') {
            customWrap.style.display = 'block';
            modelInput.focus();
        } else {
            customWrap.style.display = 'none';
            modelInput.value = val;
        }
    }

    // 2.1. ĐỒNG BỘ DANH SÁCH MODEL TỪ CHÍNH API KEY
    function fetchModelsFromApiKey(event) {
        const apiKey = document.getElementById('gemini_api_key').value.trim();
        if (!apiKey) {
            alert('Vui lòng nhập API Key trước khi đồng bộ danh sách Model!');
            document.getElementById('gemini_api_key').focus();
            return;
        }

        const select = document.getElementById('modelPresetSelect');
        const modelInput = document.getElementById('gemini_model');
        const currentVal = modelInput.value;

        const btn = event.currentTarget;
        const oldHtml = btn.innerHTML;
        btn.innerHTML = '⏳ Đang đồng bộ...';
        btn.disabled = true;

        const params = new URLSearchParams();
        params.append('action', 'fetchModels');
        params.append('gemini_api_key', apiKey);

        fetch(CONTEXT_PATH + '/admin/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            btn.innerHTML = oldHtml;
            btn.disabled = false;
            if (data.success && data.models && data.models.length > 0) {
                select.innerHTML = '';
                let matched = false;
                data.models.forEach(m => {
                    const opt = document.createElement('option');
                    opt.value = m.id;
                    opt.textContent = m.displayName + ' (' + m.id + ')';
                    if (m.id === currentVal) {
                        opt.selected = true;
                        matched = true;
                    }
                    select.appendChild(opt);
                });

                const optCustom = document.createElement('option');
                optCustom.value = 'custom';
                optCustom.textContent = '✏️ Tùy chỉnh (Nhập model khác)...';
                select.appendChild(optCustom);

                if (!matched && currentVal) {
                    optCustom.selected = true;
                    document.getElementById('customModelWrap').style.display = 'block';
                } else {
                    document.getElementById('customModelWrap').style.display = 'none';
                    modelInput.value = select.value;
                }

                alert('✓ ' + data.message);
            } else {
                alert('✗ ' + data.message);
            }
        })
        .catch(err => {
            btn.innerHTML = oldHtml;
            btn.disabled = false;
            alert('✗ Lỗi khi kết nối lấy danh sách model: ' + err.message);
        });
    }

    // 3. KIỂM TRA KẾT NỐI GEMINI API TRỰC TIẾP (TEST CONNECTION)
    function testConnection() {
        const btn = document.getElementById('btnTestConn');
        const badge = document.getElementById('testResultBadge');
        const apiKey = document.getElementById('gemini_api_key').value.trim();
        const model = document.getElementById('gemini_model').value.trim();

        if (!apiKey) {
            badge.style.display = 'inline-block';
            badge.className = 'test-result-badge badge-error';
            badge.textContent = '✗ Vui lòng nhập API Key trước khi test!';
            return;
        }

        btn.disabled = true;
        btn.classList.add('loading');
        badge.style.display = 'inline-block';
        badge.className = 'test-result-badge badge-loading';
        badge.textContent = '⏳ Đang kiểm tra kết nối Google Gemini...';

        const params = new URLSearchParams();
        params.append('action', 'testConnection');
        params.append('gemini_api_key', apiKey);
        params.append('gemini_model', model);

        fetch(CONTEXT_PATH + '/admin/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            btn.disabled = false;
            btn.classList.remove('loading');
            if (data.success) {
                badge.className = 'test-result-badge badge-success';
                badge.textContent = '✓ ' + data.message;
            } else {
                badge.className = 'test-result-badge badge-error';
                badge.textContent = '✗ ' + data.message;
            }
        })
        .catch(err => {
            btn.disabled = false;
            btn.classList.remove('loading');
            badge.className = 'test-result-badge badge-error';
            badge.textContent = '✗ Lỗi kết nối máy chủ: ' + err.message;
        });
    }

    // 4. NẠP MẪU PROMPT CHUẨN FPT
    function loadFptPromptTemplate() {
        const template = 
`Bạn là trợ lý ảo AI thông minh và thân thiện của FPT Telecom.
Nhiệm vụ của bạn là tư vấn các dịch vụ viễn thông của FPT Telecom gồm: Gói cước Internet cáp quang, Truyền hình FPT Play, Camera an ninh FPT, FPT Smart Home.

=== NGUYÊN TẮC TƯ VẤN ===
- Xưng "em" và gọi khách hàng là "anh/chị".
- Giọng điệu nhiệt tình, lịch sự, ngắn gọn và dễ hiểu (khoảng 2-4 câu mỗi lần).
- Giới thiệu các gói cước phổ biến: Gói Giga 150Mbps, Gói Sky 1Gbps, Gói F-Game tối ưu độ trễ, Combo Internet + FPT Play.
- Cung cấp Hotline tư vấn lắp đặt: 0932 079 469 và Hotline kỹ thuật: 1900 6600.
- Tuyệt đối không tiết lộ mật khẩu, API key, hoặc thông tin nhạy cảm của hệ thống.
- Từ chối lịch sự nếu khách hàng hỏi về các chủ đề không liên quan đến dịch vụ FPT.`;

        document.getElementById('gemini_system_prompt').value = template;
        updatePromptStats();
    }

    function clearPrompt() {
        if (confirm('Bạn có muốn xóa toàn bộ nội dung hướng dẫn prompt?')) {
            document.getElementById('gemini_system_prompt').value = '';
            updatePromptStats();
        }
    }

    // 5. ĐẾM KÝ TỰ & TỪ
    function updatePromptStats() {
        const txt = document.getElementById('gemini_system_prompt').value || '';
        document.getElementById('promptCharCount').textContent = txt.length + ' ký tự';
        const words = txt.trim() ? txt.trim().split(/\\s+/).length : 0;
        document.getElementById('promptWordCount').textContent = words + ' từ';
    }

    // 6. THỬ NGHIỆM CHATBOT TRỰC TIẾP TRONG PLAYGROUND
    function sendPlaygroundMessage() {
        const input = document.getElementById('playgroundInput');
        const output = document.getElementById('playgroundOutput');
        const btn = document.getElementById('btnPlaygroundSend');
        const message = (input.value || '').trim();

        if (!message) return;

        // Render user message
        const userDiv = document.createElement('div');
        userDiv.className = 'play-msg play-msg-user';
        userDiv.textContent = message;
        output.appendChild(userDiv);
        input.value = '';

        // Render loading state
        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'play-msg play-msg-ai play-loading';
        loadingDiv.textContent = 'AI đang phản hồi...';
        output.appendChild(loadingDiv);
        output.scrollTop = output.scrollHeight;

        btn.disabled = true;

        const params = new URLSearchParams();
        params.append('message', message);

        fetch(CONTEXT_PATH + '/ai-chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            btn.disabled = false;
            loadingDiv.classList.remove('play-loading');
            loadingDiv.textContent = data.reply || 'Không có phản hồi từ AI.';
            output.scrollTop = output.scrollHeight;
        })
        .catch(err => {
            btn.disabled = false;
            loadingDiv.classList.remove('play-loading');
            loadingDiv.classList.add('play-error');
            loadingDiv.textContent = 'Lỗi kết nối: ' + err.message;
            output.scrollTop = output.scrollHeight;
        });
    }

    // 7. KHÔI PHỤC BAN ĐẦU
    function resetForm() {
        if (confirm('Bạn có chắc muốn khôi phục lại các giá trị như lúc mới tải trang?')) {
            location.reload();
        }
    }

    // INIT
    document.addEventListener('DOMContentLoaded', function() {
        const select = document.getElementById('modelPresetSelect');
        handleModelPresetChange(select.value);
        updatePromptStats();
    });
</script>

</body>
</html>
