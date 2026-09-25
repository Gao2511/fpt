<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!-- ⭐ NAV MENU SLIDER — Hiệu ứng slider cho menu nav -->
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/nav-slider.css">

<script>
    // ===== NAV MENU SLIDER =====
    (function() {
        function initSlider() {
            var menu = document.querySelector('.top-nav .menu');
            if (!menu) return;

            // Nếu đã có slider → bỏ qua
            if (menu.querySelector('.nav-slider')) return;

            // ⭐ Tạo slider element
            var slider = document.createElement('div');
            slider.className = 'nav-slider';
            menu.insertBefore(slider, menu.firstChild);

            var links = menu.querySelectorAll('a');
            if (links.length === 0) return;

            function moveSlider(target) {
                if (!target) return;
                var menuRect = menu.getBoundingClientRect();
                var targetRect = target.getBoundingClientRect();
                var left = targetRect.left - menuRect.left;
                var width = targetRect.width;

                slider.style.left = left + 'px';
                slider.style.width = width + 'px';
            }

            // ⭐ Hover → slider di chuyển
            links.forEach(function(link) {
                link.addEventListener('mouseenter', function() {
                    moveSlider(this);
                });
            });

            // ⭐ Rời menu → slider quay về link active
            menu.addEventListener('mouseleave', function() {
                var active = menu.querySelector('a.active') || links[0];
                moveSlider(active);
            });

            // ⭐ Set active link dựa vào URL
            function setActiveLink() {
                var currentPath = window.location.pathname;
                var currentHash = window.location.hash;

                links.forEach(function(link) {
                    link.classList.remove('active');
                    var href = link.getAttribute('href') || '';

                    // Match theo hash (cho #packages, #contact)
                    if (href.indexOf('#') > -1) {
                        var hashPart = href.substring(href.indexOf('#'));
                        if (hashPart === currentHash) {
                            link.classList.add('active');
                        }
                    }

                    // Match theo path (cho /home, /contact, /my-orders)
                    var hrefPath = href.split('#')[0].split('?')[0];
                    if (hrefPath && hrefPath !== '#' && hrefPath !== '') {
                        var lastSegment = hrefPath.split('/').pop();
                        var currentSegment = currentPath.split('/').pop();

                        if (lastSegment === currentSegment
                            || (lastSegment === 'home' && (currentPath.endsWith('/') || currentPath.endsWith('/home')))
                            || (lastSegment === 'my-orders' && currentPath.indexOf('my-orders') > -1)
                            || (lastSegment === 'contact' && currentPath.indexOf('contact') > -1)
                            || (lastSegment === 'profile' && currentPath.indexOf('profile') > -1)) {
                            link.classList.add('active');
                        }
                    }
                });

                // Nếu không có active → chọn link đầu tiên
                if (!menu.querySelector('a.active') && links.length > 0) {
                    links[0].classList.add('active');
                }

                var active = menu.querySelector('a.active') || links[0];
                moveSlider(active);
            }

            // ⭐ Chạy khi load + khi hash đổi + resize
            setActiveLink();

            window.addEventListener('hashchange', setActiveLink);
            window.addEventListener('resize', function() {
                var active = menu.querySelector('a.active') || links[0];
                moveSlider(active);
            });

            // ⭐ Đợi font load xong mới tính lại vị trí
            if (document.fonts && document.fonts.ready) {
                document.fonts.ready.then(function() {
                    setActiveLink();
                });
            }
        }

        // Chạy khi DOM ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initSlider);
        } else {
            initSlider();
        }
    })();
</script>