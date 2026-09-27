<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!-- ⭐ NAV MENU SLIDER — Hiệu ứng slider cho menu nav -->
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style/nav-slider.css">

<script>
    // ===== NAV MENU SLIDER =====
    (function() {
        function initSlider() {
            var menu = document.querySelector('.top-nav .menu');
            if (!menu) return;
            if (menu.querySelector('.nav-slider')) return;

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

            links.forEach(function(link) {
                link.addEventListener('mouseenter', function() {
                    moveSlider(this);
                });
            });

            menu.addEventListener('mouseleave', function() {
                var active = menu.querySelector('a.active');
                if (active) {
                    moveSlider(active);
                } else {
                    slider.style.width = '0px';
                    slider.style.left = '0px';
                }
            });

            // ⭐ Set active link — ƯU TIÊN HASH > PATH
            function setActiveLink() {
                var currentPath = window.location.pathname;
                var currentHash = window.location.hash;
                var contextPath = '${pageContext.request.contextPath}';
                var cleanPath = currentPath.replace(contextPath, '');
                var normalizedCurrent = cleanPath.replace(/\/$/, '') || '/';

                var bestMatch = null;

                // ⭐ LẦN 1: Tìm link match cả path + hash (ưu tiên cao nhất)
                if (currentHash) {
                    links.forEach(function(link) {
                        var href = link.getAttribute('href') || '';
                        var cleanHref = href.replace(contextPath, '');
                        var hrefParts = cleanHref.split('#');
                        var hrefPath = hrefParts[0];
                        var hrefHash = hrefParts[1] || '';
                        var normalizedHref = hrefPath.replace(/\/$/, '') || '/';

                        if (hrefHash && '#' + hrefHash === currentHash) {
                            // Nếu link chỉ có hash → match ngay
                            if (!hrefPath || hrefPath === '/') {
                                bestMatch = link;
                            }
                            // Nếu link có path → check thêm path
                            else if (normalizedHref === normalizedCurrent) {
                                bestMatch = link;
                            }
                        }
                    });
                }

                // ⭐ LẦN 2: Nếu không có hash match → tìm link match path (không hash)
                if (!bestMatch && !currentHash) {
                    links.forEach(function(link) {
                        var href = link.getAttribute('href') || '';
                        var cleanHref = href.replace(contextPath, '');
                        var hrefParts = cleanHref.split('#');
                        var hrefPath = hrefParts[0];
                        var hrefHash = hrefParts[1] || '';

                        if (!hrefHash && hrefPath && hrefPath !== '#') {
                            var normalizedHref = hrefPath.replace(/\/$/, '') || '/';
                            if (normalizedHref === normalizedCurrent) {
                                bestMatch = link;
                            }
                        }
                    });
                }

                // ⭐ Apply active
                links.forEach(function(link) { link.classList.remove('active'); });

                if (bestMatch) {
                    bestMatch.classList.add('active');
                    moveSlider(bestMatch);
                } else {
                    slider.style.width = '0px';
                    slider.style.left = '0px';
                }
            }

            setActiveLink();

            window.addEventListener('hashchange', setActiveLink);
            window.addEventListener('resize', function() {
                var active = menu.querySelector('a.active');
                if (active) {
                    moveSlider(active);
                }
            });

            if (document.fonts && document.fonts.ready) {
                document.fonts.ready.then(function() {
                    setActiveLink();
                });
            }
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initSlider);
        } else {
            initSlider();
        }
    })();
</script>