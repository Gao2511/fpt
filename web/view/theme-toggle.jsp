<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!-- ⭐ THEME TOGGLE — Nút chuyển Dark/Light mode -->
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/theme-toggle.css">

<button class="theme-toggle-btn" id="themeToggleBtn" aria-label="Đổi giao diện sáng/tối" title="Đổi giao diện">
    <!-- Icon Sun -->
    <svg class="icon-sun" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="5"/>
        <line x1="12" y1="1" x2="12" y2="3"/>
        <line x1="12" y1="21" x2="12" y2="23"/>
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
        <line x1="1" y1="12" x2="3" y2="12"/>
        <line x1="21" y1="12" x2="23" y2="12"/>
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
    </svg>

    <!-- Icon Moon -->
    <svg class="icon-moon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
    </svg>
</button>

<script>
    // ===== THEME TOGGLE =====
    (function() {
        var btn = document.getElementById('themeToggleBtn');
        if (!btn) return;

        var STORAGE_KEY = 'fpt-theme-mode';

        // ⭐ Đọc theme đã lưu (hoặc theo OS nếu chưa lưu)
        function getInitialTheme() {
            var saved = localStorage.getItem(STORAGE_KEY);
            if (saved) return saved;

            // Check OS preference
            if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
                return 'dark';
            }
            return 'light';
        }

        // ⭐ Áp dụng theme
        function applyTheme(theme) {
            if (theme === 'dark') {
                document.body.classList.add('dark-mode');
                btn.classList.add('dark-mode');
            } else {
                document.body.classList.remove('dark-mode');
                btn.classList.remove('dark-mode');
            }
            localStorage.setItem(STORAGE_KEY, theme);
        }

        // ⭐ Khởi tạo
        var currentTheme = getInitialTheme();
        applyTheme(currentTheme);

        // ⭐ Toggle khi click
        btn.addEventListener('click', function() {
            var newTheme = document.body.classList.contains('dark-mode') ? 'light' : 'dark';
            applyTheme(newTheme);
        });

        // ⭐ Lắng nghe thay đổi OS theme (chỉ khi user chưa chọn thủ công)
        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function(e) {
                if (!localStorage.getItem(STORAGE_KEY)) {
                    applyTheme(e.matches ? 'dark' : 'light');
                }
            });
        }
    })();
</script>