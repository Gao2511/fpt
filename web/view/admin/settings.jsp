<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu Hình AI Đa Nhà Cung Cấp - FPT Telecom Manager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="shortcut icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-package.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-settings.css?v=3.0">
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
                    <h1>Trung Tâm Cấu Hình AI Đa Nhà Cung Cấp</h1>
                    <p>Quản lý linh hoạt Google Gemini, OpenAI, DeepSeek, Claude, OpenRouter, Groq &amp; Custom models</p>
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
            <!-- Provider đang chọn kích hoạt -->
            <input type="hidden" name="ai_active_provider" id="activeProviderInput" 
                   value="${not empty settingsMap['ai_active_provider'] ? settingsMap['ai_active_provider'] : 'gemini'}">

            <div class="settings-grid">

                <!-- CỘT 1: NHÀ CUNG CẤP, KEY & MODEL -->
                <div class="settings-card">
                    <div class="card-header-dark">
                        <div class="card-icon">
                            <svg viewBox="0 0 24 24">
                                <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                            </svg>
                        </div>
                        <div>
                            <h3>Nhà Cung Cấp AI &amp; Khóa API</h3>
                            <p>Chọn nền tảng AI vận hành chính cho hệ thống</p>
                        </div>
                    </div>

                    <!-- 1. BỘ CHỌN NHÀ CUNG CẤP AI (MULTI-PROVIDER SELECTOR) -->
                    <div class="provider-selector-wrap">
                        <label class="form-label-dark" style="margin-bottom: 6px;">
                            <svg viewBox="0 0 24 24"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>
                            Chọn Nền Tảng AI (Provider) <span class="req">*</span>
                        </label>
                        <div class="provider-grid">
                            <c:set var="curProv" value="${not empty settingsMap['ai_active_provider'] ? settingsMap['ai_active_provider'] : 'gemini'}" />
                            
                            <div class="provider-card-btn ${curProv == 'gemini' ? 'active' : ''}" onclick="selectProvider('gemini')">
                                <div class="p-icon" style="color: #60a5fa;">✨</div>
                                <div class="p-name">Google Gemini</div>
                                <div class="p-badge ${keyStatus['gemini'] ? 'p-badge-connected' : 'p-badge-none'}">
                                    ${keyStatus['gemini'] ? 'Đã có key' : 'Chưa có key'}
                                </div>
                            </div>

                            <div class="provider-card-btn ${curProv == 'openai' ? 'active' : ''}" onclick="selectProvider('openai')">
                                <div class="p-icon" style="color: #10a37f;">⚡</div>
                                <div class="p-name">OpenAI (GPT)</div>
                                <div class="p-badge ${keyStatus['openai'] ? 'p-badge-connected' : 'p-badge-none'}">
                                    ${keyStatus['openai'] ? 'Đã có key' : 'Chưa có key'}
                                </div>
                            </div>

                            <div class="provider-card-btn ${curProv == 'deepseek' ? 'active' : ''}" onclick="selectProvider('deepseek')">
                                <div class="p-icon" style="color: #3b82f6;">🐳</div>
                                <div class="p-name">DeepSeek</div>
                                <div class="p-badge ${keyStatus['deepseek'] ? 'p-badge-connected' : 'p-badge-none'}">
                                    ${keyStatus['deepseek'] ? 'Đã có key' : 'Chưa có key'}
                                </div>
                            </div>

                            <div class="provider-card-btn ${curProv == 'claude' ? 'active' : ''}" onclick="selectProvider('claude')">
                                <div class="p-icon" style="color: #d97706;">🎭</div>
                                <div class="p-name">Anthropic Claude</div>
                                <div class="p-badge ${keyStatus['claude'] ? 'p-badge-connected' : 'p-badge-none'}">
                                    ${keyStatus['claude'] ? 'Đã có key' : 'Chưa có key'}
                                </div>
                            </div>

                            <div class="provider-card-btn ${curProv == 'openrouter' ? 'active' : ''}" onclick="selectProvider('openrouter')">
                                <div class="p-icon" style="color: #a855f7;">🌐</div>
                                <div class="p-name">OpenRouter</div>
                                <div class="p-badge ${keyStatus['openrouter'] ? 'p-badge-connected' : 'p-badge-none'}">
                                    ${keyStatus['openrouter'] ? 'Đã có key' : 'Chưa có key'}
                                </div>
                            </div>

                            <div class="provider-card-btn ${curProv == 'groq' ? 'active' : ''}" onclick="selectProvider('groq')">
                                <div class="p-icon" style="color: #f97316;">🚀</div>
                                <div class="p-name">Groq (LPU Speed)</div>
                                <div class="p-badge ${keyStatus['groq'] ? 'p-badge-connected' : 'p-badge-none'}">
                                    ${keyStatus['groq'] ? 'Đã có key' : 'Chưa có key'}
                                </div>
                            </div>

                            <div class="provider-card-btn ${curProv == 'custom' ? 'active' : ''}" onclick="selectProvider('custom')">
                                <div class="p-icon" style="color: #e2e8f0;">⚙️</div>
                                <div class="p-name">Custom OpenAI / Ollama</div>
                                <div class="p-badge ${keyStatus['custom'] ? 'p-badge-connected' : 'p-badge-none'}">
                                    ${keyStatus['custom'] ? 'Đã có key' : 'Chưa có key'}
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 2. Ô BASE URL (HIỆN CHO CUSTOM VÀ OPENAI-COMPATIBLE KHI CẦN) -->
                    <div class="form-group-dark" id="baseUrlGroup" style="display: none;">
                        <label class="form-label-dark" for="providerBaseUrlInput">
                            <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                            Base URL (API Endpoint)
                        </label>
                        <input type="text" name="current_base_url" id="providerBaseUrlInput"
                               class="input-dark" style="padding-right:16px;"
                               value="${baseUrls[curProv]}"
                               placeholder="VD: http://localhost:11434/v1 hoặc https://api.together.xyz/v1">
                        <small class="hint-dark">
                            Đường dẫn gốc tương thích OpenAI. Có SSRF protection chặn truy cập trái phép mạng nội bộ trừ khi cho phép local.
                        </small>
                    </div>

                    <!-- 3. API KEY THEO TỪNG HÃNG -->
                    <div class="form-group-dark">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                            <label class="form-label-dark" for="providerApiKeyInput" style="margin-bottom: 0;">
                                <svg viewBox="0 0 24 24">
                                    <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                                </svg>
                                <span id="apiKeyLabelText">Khóa API Key</span> <span class="req">*</span>
                            </label>
                            <span id="keyStatusBadge" style="font-size: 11px; color: #94a3b8;"></span>
                        </div>
                        <div class="input-dark-wrap">
                            <input type="password" name="current_api_key" id="providerApiKeyInput"
                                   class="input-dark"
                                   value="${maskedKeys[curProv]}"
                                   placeholder="Nhập API Key...">
                            <button type="button" onclick="toggleApiKeyVisibility()" class="input-btn" title="Hiện/Ẩn">
                                <svg id="eyeIcon" viewBox="0 0 24 24">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                            </button>
                        </div>
                        <small class="hint-dark" style="color: #64748b;">
                            🔒 Khóa được mã hóa AES-256-GCM khi lưu. Nếu không muốn đổi key, vui lòng giữ nguyên chuỗi đã che mờ.
                        </small>
                    </div>

                    <!-- 4. CHỌN MÔ HÌNH (MODEL SELECTOR) -->
                    <div class="form-group-dark">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; flex-wrap: wrap; gap: 8px;">
                            <label class="form-label-dark" for="modelPresetSelect" style="margin-bottom:0;">
                                <svg viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
                                Mô hình AI (Model) <span class="req">*</span>
                            </label>
                            <div style="display: flex; gap: 8px;">
                                <button type="button" class="btn-prompt-tool" onclick="syncModelsFromProvider(event)" title="Lấy danh sách các model đang hoạt động thật từ API của hãng">
                                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
                                    Đồng bộ từ API Key
                                </button>
                            </div>
                        </div>

                        <!-- Ô tìm kiếm model nhanh -->
                        <div class="model-search-wrap">
                            <svg class="model-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                            <input type="text" id="modelSearchInput" class="model-search-input"
                                   placeholder="Gõ để lọc nhanh model..." oninput="filterModelDropdown(this.value)">
                        </div>

                        <select id="modelPresetSelect" class="select-dark" onchange="handleModelSelectionChange(this.value)">
                            <!-- Nạp động qua JS theo provider -->
                        </select>

                        <!-- Input Model thực tế gửi lên server -->
                        <div id="customModelWrap" style="margin-top: 10px; display: none;">
                            <input type="text" name="ai_active_model" id="activeModelInput"
                                   class="input-dark" style="padding-right:16px;"
                                   value="${not empty settingsMap['ai_active_model'] ? settingsMap['ai_active_model'] : (not empty settingsMap['gemini_model'] ? settingsMap['gemini_model'] : 'gemini-1.5-flash')}"
                                   placeholder="Nhập tên mã định danh model (VD: gpt-4o, claude-3-5-haiku...)">
                        </div>

                        <!-- Nút Kiểm tra kết nối API trực tiếp -->
                        <div style="margin-top: 12px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                            <button type="button" id="btnTestConn" onclick="testConnection()" class="btn-test-conn">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                                </svg>
                                <span>Kiểm tra kết nối (Test Connection)</span>
                            </button>
                            <span id="testResultBadge" class="test-result-badge" style="display: none;"></span>
                        </div>
                    </div>

                    <!-- 5. MÔ HÌNH DỰ PHÒNG (FALLBACK MODEL) -->
                    <div class="fallback-settings-card">
                        <div class="fallback-title">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                            Mô hình Dự Phòng Tự Động (Fallback Resilience)
                        </div>
                        <div class="fallback-desc">
                            Khi model chính gặp sự cố (quá tải 503, hết quota 429, timeout), hệ thống tự động chuyển ngay sang model dự phòng này để chatbot khách không bị đứt đoạn.
                        </div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                            <div>
                                <label class="form-label-dark" style="font-size: 11.5px; margin-bottom: 4px;">Hãng dự phòng:</label>
                                <select name="ai_fallback_provider" id="fallbackProviderSelect" class="select-dark" style="padding: 8px 12px; font-size: 13px;">
                                    <option value="" ${empty settingsMap['ai_fallback_provider'] ? 'selected' : ''}>-- Không dùng (Tắt) --</option>
                                    <option value="gemini" ${settingsMap['ai_fallback_provider'] == 'gemini' ? 'selected' : ''}>Google Gemini</option>
                                    <option value="openai" ${settingsMap['ai_fallback_provider'] == 'openai' ? 'selected' : ''}>OpenAI</option>
                                    <option value="deepseek" ${settingsMap['ai_fallback_provider'] == 'deepseek' ? 'selected' : ''}>DeepSeek</option>
                                    <option value="claude" ${settingsMap['ai_fallback_provider'] == 'claude' ? 'selected' : ''}>Claude</option>
                                    <option value="groq" ${settingsMap['ai_fallback_provider'] == 'groq' ? 'selected' : ''}>Groq</option>
                                    <option value="openrouter" ${settingsMap['ai_fallback_provider'] == 'openrouter' ? 'selected' : ''}>OpenRouter</option>
                                    <option value="custom" ${settingsMap['ai_fallback_provider'] == 'custom' ? 'selected' : ''}>Custom</option>
                                </select>
                            </div>
                            <div>
                                <label class="form-label-dark" style="font-size: 11.5px; margin-bottom: 4px;">Model dự phòng:</label>
                                <input type="text" name="ai_fallback_model" id="fallbackModelInput"
                                       class="input-dark" style="padding: 8px 12px; font-size: 13px;"
                                       value="${settingsMap['ai_fallback_model']}"
                                       placeholder="VD: gemini-1.5-flash-8b hoặc gpt-4o-mini">
                            </div>
                        </div>
                    </div>

                    <!-- 6. THAM SỐ SINH CHỮ (TEMPERATURE & MAX TOKENS) -->
                    <div class="params-row" style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 18px;">
                        <div class="form-group-dark">
                            <label class="form-label-dark" for="ai_temperature">
                                <svg viewBox="0 0 24 24"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>
                                Temperature: <span id="tempValueDisplay" style="color: #f37021; font-weight: 800;">${not empty settingsMap['ai_temperature'] ? settingsMap['ai_temperature'] : (not empty settingsMap['gemini_temperature'] ? settingsMap['gemini_temperature'] : '0.7')}</span>
                            </label>
                            <input type="range" name="ai_temperature" id="ai_temperature"
                                    min="0.0" max="1.0" step="0.1"
                                    value="${not empty settingsMap['ai_temperature'] ? settingsMap['ai_temperature'] : (not empty settingsMap['gemini_temperature'] ? settingsMap['gemini_temperature'] : '0.7')}"
                                    class="range-dark"
                                    oninput="document.getElementById('tempValueDisplay').textContent = this.value">
                            <div style="display:flex; justify-content:space-between; font-size:11px; color:#64748b; margin-top:4px;">
                                <span>0.0 (Chính xác)</span>
                                <span>1.0 (Sáng tạo)</span>
                            </div>
                        </div>

                        <div class="form-group-dark">
                            <label class="form-label-dark" for="ai_max_tokens">
                                <svg viewBox="0 0 24 24"><polyline points="4 7 4 4 20 4 20 7"/><line x1="9" y1="20" x2="15" y2="20"/><line x1="12" y1="4" x2="12" y2="20"/></svg>
                                Max Output Tokens
                            </label>
                            <input type="number" name="ai_max_tokens" id="ai_max_tokens"
                                    min="100" max="4000" step="50"
                                    class="input-dark" style="padding-right:16px; font-family:inherit;"
                                    value="${not empty settingsMap['ai_max_tokens'] ? settingsMap['ai_max_tokens'] : (not empty settingsMap['gemini_max_tokens'] ? settingsMap['gemini_max_tokens'] : '600')}">
                            <div style="font-size:11px; color:#64748b; margin-top:4px;">Giới hạn token câu trả lời (100 - 4000)</div>
                        </div>
                    </div>

                    <!-- 7. TRẠNG THÁI BẬT/TẮT CHATBOT -->
                    <div class="toggle-card">
                        <div class="toggle-info">
                            <strong>Kích hoạt Chatbot AI Toàn Hệ Thống</strong>
                            <p>Bật hoặc tạm dừng tính năng tư vấn tự động trên website fpt.gialai.vn</p>
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
                                <p>Áp dụng thống nhất cho toàn bộ các hãng AI</p>
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
                        <textarea name="ai_system_prompt" id="ai_system_prompt"
                                  class="textarea-dark"
                                  style="min-height: 380px;"
                                  placeholder="Nhập vai trò, persona và hướng dẫn hoạt động cho AI..."
                                  oninput="updatePromptStats()">${not empty settingsMap['ai_system_prompt'] ? settingsMap['ai_system_prompt'] : settingsMap['gemini_system_prompt']}</textarea>
                        
                        <div class="prompt-footer-stats">
                            <span id="promptCharCount">0 ký tự</span>
                            <span id="promptWordCount">0 từ</span>
                        </div>
                    </div>

                    <div class="prompt-guide-box">
                        <div class="guide-title">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                            Kiến trúc Prompt Adapter thống nhất:
                        </div>
                        <ul>
                            <li><strong>Google Gemini:</strong> Hệ thống tự đóng gói vào cấu trúc <code>system_instruction</code> tách biệt.</li>
                            <li><strong>OpenAI / DeepSeek / Groq:</strong> Hệ thống tự đặt vào tin nhắn đầu tiên có role <code>system</code>.</li>
                            <li><strong>Anthropic Claude:</strong> Hệ thống tự đưa vào trường <code>system</code> ở root payload theo chuẩn Messages API.</li>
                            <li><strong>Bảo mật:</strong> Tự động lọc chặn các câu hỏi yêu cầu rò rỉ prompt hoặc key.</li>
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
                <div style="flex: 1;">
                    <h3>Thử Nghiệm Trò Chuyện Trực Tiếp (Live Playground)</h3>
                    <p>Kiểm tra câu trả lời thực tế theo Provider &amp; Model đang chọn trên màn hình (kể cả bản nháp chưa lưu)</p>
                </div>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <button type="button" class="btn-prompt-tool" onclick="clearPlaygroundChat()" title="Xóa đoạn chat thử nghiệm">
                        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
                        Xóa lịch sử test
                    </button>
                </div>
            </div>

            <div class="playground-chat-box">
                <div id="playgroundOutput" class="playground-output">
                    <div class="ai-msg-placeholder">Hãy đặt một câu hỏi thử nghiệm (VD: "Tư vấn cho anh gói cước 1Gbps để xem phim và đá banh") để kiểm tra phản hồi từ AI.</div>
                </div>

                <div class="playground-input-row">
                    <input type="text" id="playgroundInput" class="input-dark" placeholder="Nhập câu hỏi thử nghiệm cho model đang chọn..."
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

    // Dữ liệu presets được nạp sẵn
    const PRESETS = {
        gemini: [
            { id: 'gemini-1.5-flash', name: 'Gemini 1.5 Flash (Khuyên dùng: Siêu nhanh, thông minh, tối ưu chi phí)' },
            { id: 'gemini-2.0-flash', name: 'Gemini 2.0 Flash (Thế hệ mới nhất, phản hồi cực tốc)' },
            { id: 'gemini-1.5-flash-8b', name: 'Gemini 1.5 Flash-8B (Siêu nhẹ, dung lượng cao, ít nghẽn tải)' },
            { id: 'gemini-1.5-pro', name: 'Gemini 1.5 Pro (Lý luận chuyên sâu, ngữ cảnh 2M tokens)' },
            { id: 'gemini-1.0-pro', name: 'Gemini 1.0 Pro (Bản tiền nhiệm cơ bản)' }
        ],
        openai: [
            { id: 'gpt-4o-mini', name: 'GPT-4o Mini (Khuyên dùng: Siêu thông minh, phản hồi cực nhanh, rẻ)' },
            { id: 'gpt-4o', name: 'GPT-4o (Flagship đa thể thức thông minh nhất của OpenAI)' },
            { id: 'gpt-4-turbo', name: 'GPT-4 Turbo (Lý luận cao cấp)' },
            { id: 'gpt-3.5-turbo', name: 'GPT-3.5 Turbo (Cơ bản)' }
        ],
        deepseek: [
            { id: 'deepseek-chat', name: 'DeepSeek-V3 (Khuyên dùng: Cực kỳ thông minh, tự nhiên, chi phí siêu rẻ)' },
            { id: 'deepseek-reasoner', name: 'DeepSeek-R1 (Suy luận logic chuỗi tư duy CoT chuyên sâu)' }
        ],
        claude: [
            { id: 'claude-3-5-haiku-20241022', name: 'Claude 3.5 Haiku (Khuyên dùng: Phản hồi tức thì, văn phong xuất sắc)' },
            { id: 'claude-3-5-sonnet-20241022', name: 'Claude 3.5 Sonnet (Mô hình lý luận đối thoại đỉnh cao)' },
            { id: 'claude-3-opus-20240229', name: 'Claude 3 Opus (Phân tích chuyên sâu phức tạp)' }
        ],
        openrouter: [
            { id: 'deepseek/deepseek-chat', name: 'DeepSeek V3 (qua OpenRouter)' },
            { id: 'google/gemini-2.0-flash-001', name: 'Gemini 2.0 Flash (qua OpenRouter)' },
            { id: 'meta-llama/llama-3.3-70b-instruct', name: 'Llama 3.3 70B (qua OpenRouter)' }
        ],
        groq: [
            { id: 'llama-3.3-70b-versatile', name: 'Llama 3.3 70B Versatile (Groq LPU Speed)' },
            { id: 'llama-3.1-8b-instant', name: 'Llama 3.1 8B Instant (>700 tokens/s, cực tốc)' },
            { id: 'mixtral-8x7b-32768', name: 'Mixtral 8x7B (Groq)' }
        ],
        custom: [
            { id: 'default', name: 'Default Model (Tùy chỉnh server)' }
        ]
    };

    let currentProvider = '${not empty settingsMap["ai_active_provider"] ? settingsMap["ai_active_provider"] : "gemini"}';
    let currentModelList = [];

    // 1. CHỌN NHÀ CUNG CẤP AI
    function selectProvider(providerId) {
        currentProvider = providerId;
        document.getElementById('activeProviderInput').value = providerId;

        // Cập nhật giao diện card buttons
        document.querySelectorAll('.provider-card-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        const activeCard = Array.from(document.querySelectorAll('.provider-card-btn')).find(b => 
            b.getAttribute('onclick') && b.getAttribute('onclick').includes("'" + providerId + "'")
        );
        if (activeCard) activeCard.classList.add('active');

        // Hiện/ẩn Base URL
        const baseGroup = document.getElementById('baseUrlGroup');
        if (providerId === 'custom' || providerId === 'openrouter') {
            baseGroup.style.display = 'block';
        } else {
            baseGroup.style.display = 'none';
        }

        // Tải cấu hình của provider được chọn từ server qua AJAX
        fetchProviderData(providerId);
    }

    function fetchProviderData(providerId) {
        const params = new URLSearchParams();
        params.append('action', 'getProviderData');
        params.append('provider', providerId);

        fetch(CONTEXT_PATH + '/admin/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                document.getElementById('providerApiKeyInput').value = data.maskedKey || '';
                if (data.baseUrl) {
                    document.getElementById('providerBaseUrlInput').value = data.baseUrl;
                }
                document.getElementById('apiKeyLabelText').textContent = 'Khóa API Key của ' + data.displayName;
                
                const list = (data.presetModels && data.presetModels.length > 0) ? data.presetModels : (PRESETS[providerId] || []);
                renderModelDropdown(list, document.getElementById('activeModelInput').value);
            }
        })
        .catch(err => {
            // Dùng fallback presets local
            const list = PRESETS[providerId] || [];
            renderModelDropdown(list, document.getElementById('activeModelInput').value);
        });
    }

    // 2. RENDER VÀ FILTER MODEL DROPDOWN
    function renderModelDropdown(models, selectedValue) {
        currentModelList = models || [];
        const select = document.getElementById('modelPresetSelect');
        select.innerHTML = '';

        let matched = false;
        currentModelList.forEach(m => {
            const opt = document.createElement('option');
            opt.value = m.id;
            opt.textContent = (m.displayName || m.name || m.id) + ' (' + m.id + ')';
            if (m.id === selectedValue) {
                opt.selected = true;
                matched = true;
            }
            select.appendChild(opt);
        });

        const optCustom = document.createElement('option');
        optCustom.value = 'custom_input';
        optCustom.textContent = '✏️ Tùy chỉnh (Nhập model khác)...';
        if (!matched && selectedValue) {
            optCustom.selected = true;
        }
        select.appendChild(optCustom);

        handleModelSelectionChange(select.value);
    }

    function filterModelDropdown(query) {
        const q = (query || '').toLowerCase().trim();
        const select = document.getElementById('modelPresetSelect');
        const currentSelected = select.value;

        select.innerHTML = '';
        currentModelList.forEach(m => {
            const name = (m.displayName || m.name || m.id).toLowerCase();
            const id = m.id.toLowerCase();
            if (!q || name.includes(q) || id.includes(q)) {
                const opt = document.createElement('option');
                opt.value = m.id;
                opt.textContent = (m.displayName || m.name || m.id) + ' (' + m.id + ')';
                if (m.id === currentSelected) opt.selected = true;
                select.appendChild(opt);
            }
        });

        const optCustom = document.createElement('option');
        optCustom.value = 'custom_input';
        optCustom.textContent = '✏️ Tùy chỉnh (Nhập model khác)...';
        if (currentSelected === 'custom_input') optCustom.selected = true;
        select.appendChild(optCustom);
    }

    function handleModelSelectionChange(val) {
        const customWrap = document.getElementById('customModelWrap');
        const modelInput = document.getElementById('activeModelInput');
        if (val === 'custom_input') {
            customWrap.style.display = 'block';
            modelInput.focus();
        } else {
            customWrap.style.display = 'none';
            modelInput.value = val;
        }
    }

    // 3. ĐỒNG BỘ DANH SÁCH MODEL TỪ API CỦA HÃNG
    function syncModelsFromProvider(event) {
        const apiKey = document.getElementById('providerApiKeyInput').value.trim();
        const baseUrl = document.getElementById('providerBaseUrlInput').value.trim();
        const btn = event.currentTarget;
        const oldHtml = btn.innerHTML;

        btn.innerHTML = '⏳ Đang đồng bộ...';
        btn.disabled = true;

        const params = new URLSearchParams();
        params.append('action', 'fetchModels');
        params.append('provider', currentProvider);
        params.append('apiKey', apiKey);
        params.append('baseUrl', baseUrl);

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
                renderModelDropdown(data.models, document.getElementById('activeModelInput').value);
                alert('✓ ' + data.message);
            } else {
                alert('✗ ' + (data.message || 'Không thể đồng bộ danh sách model.'));
            }
        })
        .catch(err => {
            btn.innerHTML = oldHtml;
            btn.disabled = false;
            alert('✗ Lỗi kết nối khi đồng bộ: ' + err.message);
        });
    }

    // 4. KIỂM TRA KẾT NỐI (TEST CONNECTION)
    function testConnection() {
        const btn = document.getElementById('btnTestConn');
        const badge = document.getElementById('testResultBadge');
        const apiKey = document.getElementById('providerApiKeyInput').value.trim();
        const model = document.getElementById('activeModelInput').value.trim();
        const baseUrl = document.getElementById('providerBaseUrlInput').value.trim();

        btn.disabled = true;
        btn.classList.add('loading');
        badge.style.display = 'inline-block';
        badge.className = 'test-result-badge badge-loading';
        badge.textContent = '⏳ Đang kiểm tra kết nối tới ' + currentProvider + '...';

        const params = new URLSearchParams();
        params.append('action', 'testConnection');
        params.append('provider', currentProvider);
        params.append('apiKey', apiKey);
        params.append('model', model);
        params.append('baseUrl', baseUrl);

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

    // 5. THỬ NGHIỆM AI TRONG LIVE PLAYGROUND
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

        // Render AI message holder
        const aiDiv = document.createElement('div');
        aiDiv.className = 'play-msg play-msg-ai play-loading';
        aiDiv.textContent = 'AI đang phản hồi...';
        output.appendChild(aiDiv);
        output.scrollTop = output.scrollHeight;

        btn.disabled = true;

        const apiKey = document.getElementById('providerApiKeyInput').value.trim();
        const model = document.getElementById('activeModelInput').value.trim();
        const baseUrl = document.getElementById('providerBaseUrlInput').value.trim();
        const prompt = document.getElementById('ai_system_prompt').value;
        const temp = document.getElementById('ai_temperature').value;
        const maxTokens = document.getElementById('ai_max_tokens').value;

        const params = new URLSearchParams();
        params.append('message', message);
        params.append('provider', currentProvider);
        params.append('model', model);
        params.append('apiKey', apiKey);
        params.append('baseUrl', baseUrl);
        params.append('systemPrompt', prompt);
        params.append('temperature', temp);
        params.append('maxTokens', maxTokens);

        fetch(CONTEXT_PATH + '/admin/ai-playground', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            btn.disabled = false;
            aiDiv.classList.remove('play-loading');

            if (data.success) {
                aiDiv.textContent = data.reply || '';

                // Thêm meta tags thống kê
                const metaRow = document.createElement('div');
                metaRow.className = 'play-meta-row';
                metaRow.innerHTML = 
                    '<span class="play-tag play-tag-provider">🏷️ ' + (data.provider || currentProvider) + ' / ' + (data.model || model) + '</span>' +
                    '<span class="play-tag play-tag-latency">⚡ ' + (data.latencyMs || 0) + 'ms</span>' +
                    '<span class="play-tag play-tag-tokens">🪙 ' + (data.totalTokens ? data.totalTokens + ' tokens' : 'Tokens N/A') + '</span>';
                aiDiv.appendChild(metaRow);
            } else {
                aiDiv.classList.add('play-error');
                aiDiv.textContent = '✗ ' + (data.message || 'Lỗi xử lý');
            }
            output.scrollTop = output.scrollHeight;
        })
        .catch(err => {
            btn.disabled = false;
            aiDiv.classList.remove('play-loading');
            aiDiv.classList.add('play-error');
            aiDiv.textContent = '✗ Lỗi kết nối: ' + err.message;
            output.scrollTop = output.scrollHeight;
        });
    }

    function clearPlaygroundChat() {
        const output = document.getElementById('playgroundOutput');
        output.innerHTML = '<div class="ai-msg-placeholder">Hãy đặt một câu hỏi thử nghiệm để kiểm tra phản hồi từ AI.</div>';
    }

    // 6. TOGGLE PASSWORD KEY
    function toggleApiKeyVisibility() {
        const input = document.getElementById('providerApiKeyInput');
        const icon = document.getElementById('eyeIcon');
        if (input.type === 'password') {
            input.type = 'text';
            icon.innerHTML = '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>';
        } else {
            input.type = 'password';
            icon.innerHTML = '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>';
        }
    }

    // 7. PROMPT TOOLS & STATS
    function loadFptPromptTemplate() {
        const template = 
`Bạn là trợ lý ảo AI thông minh và thân thiện của FPT Telecom Gia Lai.
Nhiệm vụ của bạn là tư vấn các dịch vụ viễn thông của FPT Telecom gồm: Gói cước Internet cáp quang, Truyền hình FPT Play, Camera an ninh FPT, FPT Smart Home.

=== NGUYÊN TẮC TƯ VẤN ===
- Xưng "em" và gọi khách hàng là "anh/chị".
- Giọng điệu nhiệt tình, lịch sự, ngắn gọn và dễ hiểu (khoảng 2-4 câu mỗi lần).
- Giới thiệu các gói cước phổ biến: Gói Giga 150Mbps, Gói Sky 1Gbps, Gói F-Game tối ưu độ trễ, Combo Internet + FPT Play.
- Cung cấp Hotline tư vấn lắp đặt: 0932 079 469 và Hotline kỹ thuật: 1900 6600.
- Tuyệt đối không tiết lộ mật khẩu, API key, hoặc thông tin nhạy cảm của hệ thống.
- Từ chối lịch sự nếu khách hàng hỏi về các chủ đề không liên quan đến dịch vụ FPT.`;

        document.getElementById('ai_system_prompt').value = template;
        updatePromptStats();
    }

    function clearPrompt() {
        if (confirm('Bạn có muốn xóa toàn bộ nội dung hướng dẫn prompt?')) {
            document.getElementById('ai_system_prompt').value = '';
            updatePromptStats();
        }
    }

    function updatePromptStats() {
        const txt = document.getElementById('ai_system_prompt').value || '';
        document.getElementById('promptCharCount').textContent = txt.length + ' ký tự';
        const words = txt.trim() ? txt.trim().split(/\\s+/).length : 0;
        document.getElementById('promptWordCount').textContent = words + ' từ';
    }

    function resetForm() {
        if (confirm('Bạn có chắc muốn khôi phục lại các giá trị như lúc mới tải trang?')) {
            location.reload();
        }
    }

    // INIT
    document.addEventListener('DOMContentLoaded', function() {
        const savedProvider = '${not empty settingsMap["ai_active_provider"] ? settingsMap["ai_active_provider"] : "gemini"}';
        const savedModel = '${not empty settingsMap["ai_active_model"] ? settingsMap["ai_active_model"] : (not empty settingsMap["gemini_model"] ? settingsMap["gemini_model"] : "gemini-1.5-flash")}';
        
        const list = PRESETS[savedProvider] || PRESETS['gemini'];
        renderModelDropdown(list, savedModel);
        updatePromptStats();

        if (savedProvider === 'custom' || savedProvider === 'openrouter') {
            document.getElementById('baseUrlGroup').style.display = 'block';
        }
    });
</script>

</body>
</html>
