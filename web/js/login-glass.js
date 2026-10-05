/* ==========================================================================
   FPT TELECOM — AUTHENTICATION CLIENT CONTROLLER (JS)
   Dùng chung cho: Login, Register, Forgot Password, Reset Password
   --------------------------------------------------------------------------
   MỤC LỤC / TABLE OF CONTENTS:
   1. SELECTOR HELPERS & COMMON MODAL
   2. PASSWORD TOGGLE VISIBILITY
   3. LOGIN FORM VALIDATION & SUBMISSION
   4. REGISTER FORM VALIDATION & STRENGTH METER
   5. FORGOT PASSWORD FORM VALIDATION
   6. SERVER MODAL AUTO-DISPATCHER
   ========================================================================== */

(function () {
    'use strict';

    // 1. SELECTOR HELPERS & COMMON MODAL
    const $ = (sel) => document.querySelector(sel);
    const $$ = (sel) => document.querySelectorAll(sel);

    const modal = $('#loginModal');
    const modalClose = $('#modalClose');
    const modalIcon = $('#modalIcon');
    const modalText = $('#modalText');

    const EYE_OPEN = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
    const EYE_CLOSED = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

    // Hiển thị Popup Modal kính mờ
    window.showGlassModal = function (type, message) {
        if (!modal) return;
        modal.classList.add('show');
        if (modalText) {
            modalText.textContent = message;
            modalText.classList.toggle('error', type === 'error');
        }
        if (modalIcon) {
            modalIcon.classList.toggle('success', type === 'success');
            if (type === 'loading') {
                modalIcon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spinner"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>';
            } else if (type === 'error') {
                modalIcon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>';
            } else if (type === 'success') {
                modalIcon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>';
            }
        }
        modalClose?.classList.toggle('hidden', type === 'loading');
    };

    modalClose?.addEventListener('click', () => {
        modal?.classList.remove('show');
    });

    modal?.addEventListener('click', (e) => {
        if (e.target === modal && !modalClose?.classList.contains('hidden')) {
            modal.classList.remove('show');
        }
    });

    // 2. PASSWORD TOGGLE VISIBILITY (Tất cả các nút toggle mật khẩu)
    $$('.glass-toggle-pass').forEach((btn) => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target');
            const targetInput = targetId ? $('#' + targetId) : btn.previousElementSibling;
            if (!targetInput) return;
            const isPass = targetInput.type === 'password';
            targetInput.type = isPass ? 'text' : 'password';
            btn.innerHTML = isPass ? EYE_CLOSED : EYE_OPEN;
        });
    });

    // 3. LOGIN FORM VALIDATION & SUBMISSION
    const loginForm = $('#loginForm');
    if (loginForm) {
        const inputAccount = $('#inputIdentifier') || $('#inputEmail');
        const inputPassword = $('#inputPassword');

        loginForm.addEventListener('submit', (e) => {
            const account = (inputAccount?.value || '').trim();
            const password = (inputPassword?.value || '').trim();

            if (account.length < 1) {
                e.preventDefault();
                window.showGlassModal('error', 'Vui lòng nhập tên đăng nhập, email hoặc SĐT!');
                return;
            }

            if (password.length < 1) {
                e.preventDefault();
                window.showGlassModal('error', 'Vui lòng nhập mật khẩu!');
                return;
            }

            window.showGlassModal('loading', 'Đang đăng nhập...');
        });

        // Tự động mang email sang trang Quên mật khẩu nếu người dùng đã nhập
        const linkForgot = $('#linkForgotPassword');
        if (linkForgot) {
            linkForgot.addEventListener('click', (e) => {
                const emailVal = (inputAccount?.value || '').trim();
                if (emailVal) {
                    e.preventDefault();
                    const href = linkForgot.getAttribute('href');
                    window.location.href = href + (href.includes('?') ? '&' : '?') + 'email=' + encodeURIComponent(emailVal);
                }
            });
        }
    }

    // 4. REGISTER FORM VALIDATION & STRENGTH METER
    const registerForm = $('#registerForm');
    if (registerForm) {
        const inputFullName = $('#inputFullName');
        const inputEmail = $('#inputEmail');
        const inputPhone = $('#inputPhone');
        const inputPassword = $('#inputPassword');
        const inputConfirmPassword = $('#inputConfirmPassword');
        const agreeCheckbox = $('#agreeCheckbox');

        // Thước đo độ mạnh mật khẩu khi nhập liệu
        if (inputPassword) {
            inputPassword.addEventListener('input', () => {
                const val = inputPassword.value;
                const statusEl = $('#pwdStatus');
                const bars = [$('#bar1'), $('#bar2'), $('#bar3'), $('#bar4')];
                const rule1 = $('#rule1');
                const rule2 = $('#rule2');
                const rule3 = $('#rule3');

                bars.forEach(b => { if (b) b.className = 'pwd-bar'; });
                rule1?.classList.remove('ok');
                rule2?.classList.remove('ok');
                rule3?.classList.remove('ok');

                let score = 0;
                if (val.length >= 8) { rule1?.classList.add('ok'); score++; }
                if (/[A-Z]/.test(val)) { rule2?.classList.add('ok'); score++; }
                if (/[0-9]/.test(val)) { rule3?.classList.add('ok'); score++; }
                if (/[^A-Za-z0-9]/.test(val)) { score++; }

                if (!statusEl) return;
                if (val.length === 0) {
                    statusEl.textContent = 'Chưa nhập';
                    statusEl.className = 'pwd-meter-status';
                } else if (score <= 2) {
                    statusEl.textContent = 'Yếu';
                    statusEl.className = 'pwd-meter-status weak';
                    bars[0]?.classList.add('active-weak');
                } else if (score === 3) {
                    statusEl.textContent = 'Trung bình';
                    statusEl.className = 'pwd-meter-status medium';
                    bars[0]?.classList.add('active-medium');
                    bars[1]?.classList.add('active-medium');
                    bars[2]?.classList.add('active-medium');
                } else {
                    statusEl.textContent = 'Mạnh';
                    statusEl.className = 'pwd-meter-status strong';
                    bars.forEach(b => b?.classList.add('active-strong'));
                }
            });
        }

        registerForm.addEventListener('submit', (e) => {
            const fullName = (inputFullName?.value || '').trim();
            const email = (inputEmail?.value || '').trim();
            const phone = (inputPhone?.value || '').trim();
            const password = inputPassword?.value || '';
            const confirm = inputConfirmPassword?.value || '';

            if (!fullName) {
                e.preventDefault();
                window.showGlassModal('error', 'Vui lòng nhập Họ và tên.');
                return;
            }

            if (!email || !/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(email)) {
                e.preventDefault();
                window.showGlassModal('error', 'Vui lòng nhập Email hợp lệ.');
                return;
            }

            const cleanPhone = phone.replace(/[\s.-]/g, '');
            if (!cleanPhone || !/^[0-9]{10,11}$/.test(cleanPhone)) {
                e.preventDefault();
                window.showGlassModal('error', 'Số điện thoại phải gồm 10-11 chữ số.');
                return;
            }

            if (password.length < 8) {
                e.preventDefault();
                window.showGlassModal('error', 'Mật khẩu phải có ít nhất 8 ký tự.');
                return;
            }

            if (password !== confirm) {
                e.preventDefault();
                window.showGlassModal('error', 'Mật khẩu xác nhận không khớp.');
                return;
            }

            if (agreeCheckbox && !agreeCheckbox.checked) {
                e.preventDefault();
                window.showGlassModal('error', 'Vui lòng đồng ý với Điều khoản dịch vụ & Chính sách của FPT.');
                return;
            }

            window.showGlassModal('loading', 'Đang tạo tài khoản...');
        });
    }

    // 5. FORGOT PASSWORD & OTP FORM VALIDATION
    const forgotForm = $('#forgotForm');
    if (forgotForm) {
        forgotForm.addEventListener('submit', (e) => {
            const emailInput = $('#inputEmail') || $('#inputIdentifier');
            const email = (emailInput?.value || '').trim();
            if (!email) {
                e.preventDefault();
                window.showGlassModal('error', 'Vui lòng nhập địa chỉ email của bạn.');
                return;
            }
            if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(email)) {
                e.preventDefault();
                window.showGlassModal('error', 'Địa chỉ email không đúng định dạng!');
                return;
            }
            window.showGlassModal('loading', 'Đang gửi mã OTP đến email của bạn...');
        });
    }

    const otpForm = $('#otpForm');
    if (otpForm) {
        otpForm.addEventListener('submit', (e) => {
            const otp = ($('#inputOtp')?.value || '').trim();
            if (otp.length !== 6) {
                e.preventDefault();
                window.showGlassModal('error', 'Vui lòng nhập đầy đủ 6 chữ số OTP.');
                return;
            }
            window.showGlassModal('loading', 'Đang xác thực mã OTP...');
        });
    }

    // 6. SERVER MODAL AUTO-DISPATCHER (Nếu cần trigger modal từ server attribute)
    const autoModalErr = document.querySelector('[data-server-modal-error]');
    if (autoModalErr && autoModalErr.textContent.trim()) {
        window.showGlassModal('error', autoModalErr.textContent.trim());
    }

})();