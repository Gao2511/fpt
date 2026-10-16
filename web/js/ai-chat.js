(function (root) {
    'use strict';
    function validatedReply(data) {
        if (!data || typeof data.reply !== 'string' || !data.reply.trim() || data.reply.length > 16000 ||
            !['none', 'open_registration'].includes(data.action)) throw new Error('Invalid chat response');
        if (data.action === 'open_registration' &&
            (typeof data.registrationPath !== 'string' || !/^\/package-detail\?id=[1-9][0-9]*#registration$/.test(data.registrationPath)))
            throw new Error('Invalid registration action');
        return {text: data.reply, registrationPath: data.action === 'open_registration' ? data.registrationPath : null};
    }
    function appendReply(body, data, contextPath) {
        const reply = validatedReply(data);
        const bubble = body.ownerDocument.createElement('div');
        bubble.className = 'ai-msg ai-msg-bot';
        bubble.style.whiteSpace = 'pre-wrap';
        bubble.textContent = reply.text;
        if (reply.registrationPath) {
            const link = body.ownerDocument.createElement('a');
            link.href = contextPath + reply.registrationPath;
            link.textContent = 'Mở biểu mẫu đăng ký tư vấn →';
            link.style.display = 'inline-flex';
            bubble.appendChild(link);
        }
        body.appendChild(bubble);
        return bubble;
    }
    function init(contextPath) {
        let pending = false;
        function syncViewportHeight() {
            const win = root.document && root.document.getElementById ? root.document.getElementById('aiChatWindow') : null;
            if (!win || !win.style) return;
            const vvh = root.visualViewport && root.visualViewport.height ? Math.round(root.visualViewport.height) + 'px' : null;
            if (vvh && typeof win.style.setProperty === 'function') {
                win.style.setProperty('--ai-vvh', vvh);
            }
        }
        function syncBodyState(isOpen) {
            const docBody = root.document && root.document.body;
            if (!docBody) return;
            if (docBody.classList && typeof docBody.classList.toggle === 'function') {
                docBody.classList.toggle('ai-chat-open', !!isOpen);
            }
            const isMobile = typeof root.innerWidth === 'number' && root.innerWidth <= 640;
            if (docBody.style) {
                docBody.style.overflow = (isOpen && isMobile) ? 'hidden' : '';
            }
        }
        root.toggleAIChat = function () {
            const win = root.document.getElementById('aiChatWindow');
            if (!win) return;
            const isOpen = win.classList.toggle('active');
            syncViewportHeight();
            syncBodyState(isOpen);
            if (isOpen) {
                const body = root.document.getElementById('aiChatBody');
                if (body) body.scrollTop = body.scrollHeight;
                const input = root.document.getElementById('aiChatInput');
                if (input) root.setTimeout(() => input.focus(), 150);
            }
        };
        if (root.visualViewport && typeof root.visualViewport.addEventListener === 'function') {
            root.visualViewport.addEventListener('resize', syncViewportHeight);
        }
        if (root.document && typeof root.document.addEventListener === 'function') {
            root.document.addEventListener('keydown', function (e) {
                if (e && e.key === 'Escape') {
                    const win = root.document.getElementById('aiChatWindow');
                    if (win && win.classList && win.classList.contains('active')) {
                        win.classList.remove('active');
                        syncBodyState(false);
                    }
                }
            });
        }
        root.sendAIMessage = async function () {
            const input = root.document.getElementById('aiChatInput');
            const body = root.document.getElementById('aiChatBody');
            const message = input.value.trim();
            if (!message || pending) return;
            if (message.length > 2000) { appendReply(body, {reply: 'Vui lòng nhập câu hỏi tối đa 2000 ký tự.', action: 'none'}, contextPath); return; }
            pending = true; input.disabled = true;
            const user = root.document.createElement('div'); user.className = 'ai-msg ai-msg-user'; user.textContent = message; body.appendChild(user);
            input.value = '';
            const loading = root.document.createElement('div');
            loading.className = 'ai-msg-loading';
            loading.innerHTML = '<span></span><span></span><span></span> <span style="margin-left:4px;font-size:12.5px;color:#64748b">Đang tra cứu gói cước…</span>';
            body.appendChild(loading);
            body.scrollTop = body.scrollHeight;
            const controller = new AbortController();
            const timer = root.setTimeout(() => controller.abort(), 55000);
            try {
                const response = await root.fetch(contextPath + '/ai-chat', {
                    method: 'POST', credentials: 'same-origin', signal: controller.signal,
                    headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                    body: new URLSearchParams({message: message}).toString()
                });
                const data = await response.json();
                appendReply(body, data, contextPath);
            } catch (error) {
                const contact = body.dataset?.contact || '0932 079 469';
                appendReply(body, {reply: 'Em chưa nhận được câu trả lời đầy đủ lúc này. Anh/chị có thể thử lại hoặc liên hệ tư vấn viên qua điện thoại/Zalo ' + contact + '.', action: 'none'}, contextPath);
            } finally {
                root.clearTimeout(timer); loading.remove(); pending = false; input.disabled = false; input.focus(); body.scrollTop = body.scrollHeight;
            }
        };
    }
    const api = {validatedReply, appendReply, init};
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    else root.FptChat = api;
})(typeof window !== 'undefined' ? window : globalThis);
