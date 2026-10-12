(function () {
    'use strict';
    document.querySelectorAll('[data-kb-destructive]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm('Xóa vĩnh viễn tài liệu và các phiên bản? Thao tác này không thể khôi phục từ giao diện.')) event.preventDefault();
        });
    });
}());
