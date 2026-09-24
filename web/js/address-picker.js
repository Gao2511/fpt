/**
 * FPT Address Picker - Phiên bản 2 CHẾ ĐỘ
 * ============================================
 * - Chế độ MỚI (mặc định): Tỉnh/Thành phố → Phường/Xã (dùng PROVINCE_DATA)
 * - Chế độ CŨ: Tỉnh/Thành phố → Quận/Huyện → Phường/Xã (dùng API open-api.vn)
 *
 * Dữ liệu: biến toàn cục PROVINCE_DATA (file address-data.js)
 * Tương thích: JSP thuần (NetBeans Web Project)
 */
(function () {
    'use strict';

    var API_OLD = 'https://provinces.open-api.vn/api';

    // ===== Kiểm tra data mới =====
    function isNewDataReady() {
        return typeof PROVINCE_DATA !== 'undefined'
            && Array.isArray(PROVINCE_DATA)
            && PROVINCE_DATA.length > 0;
    }

    // ===== Render HTML =====
    window.renderAddressPicker = function (containerId, prefix) {
        var container = document.getElementById(containerId);
        if (!container) return;

        container.innerHTML =
            '<!-- Thanh chuyển chế độ -->' +
            '<div class="addr-mode-bar">' +
                '<button type="button" class="addr-mode-btn" data-prefix="' + prefix + '">' +
                    '<span class="addr-mode-btn-text">Dùng địa chỉ cũ</span>' +
                '</button>' +
                '<button type="button" class="addr-toggle-btn" data-prefix="' + prefix + '">' +
                    '<span class="addr-toggle-text">Không tìm thấy nơi của bạn? Bấm để tự nhập tay</span>' +
                '</button>' +
            '</div>' +

            '<!-- CHẾ ĐỘ 1: CHỌN 2 CẤP MỚI -->' +
            '<div class="addr-cascade-wrap addr-new-mode" data-prefix="' + prefix + '">' +
                '<div class="addr-row addr-row-2">' +
                    '<div class="addr-col">' +
                        '<label>Tỉnh/Thành phố <span style="color:#f37021;">*</span></label>' +
                        '<select class="addr-province-new" data-prefix="' + prefix + '">' +
                            '<option value="">-- Chọn Tỉnh/Thành phố --</option>' +
                        '</select>' +
                        '<input type="text" class="addr-custom-input addr-custom-prov-new" data-prefix="' + prefix + '" ' +
                               'placeholder="Gõ tên Tỉnh/Thành của bạn..." style="display:none;" autocomplete="off">' +
                    '</div>' +
                    '<div class="addr-col">' +
                        '<label>Phường/Xã <span style="color:#f37021;">*</span></label>' +
                        '<select class="addr-ward-new" data-prefix="' + prefix + '" disabled>' +
                            '<option value="">-- Chọn Phường/Xã --</option>' +
                        '</select>' +
                        '<input type="text" class="addr-custom-input addr-custom-ward-new" data-prefix="' + prefix + '" ' +
                               'placeholder="Gõ tên Phường/Xã của bạn..." style="display:none;" autocomplete="off">' +
                    '</div>' +
                '</div>' +

                '<div class="addr-detail-wrap">' +
                    '<label>Địa chỉ chi tiết <span style="color:#9ca3af;font-weight:400;font-style:italic;">(số nhà, tên đường, thôn/xóm...)</span> <span style="color:#f37021;">*</span></label>' +
                    '<input type="text" class="addr-detail" data-prefix="' + prefix + '" ' +
                           'placeholder="Ví dụ: 123 Lê Lợi, Thôn 2..." autocomplete="off">' +
                '</div>' +
            '</div>' +

            '<!-- CHẾ ĐỘ 2: CHỌN 3 CẤP CŨ (ẩn mặc định) -->' +
            '<div class="addr-cascade-wrap addr-old-mode" data-prefix="' + prefix + '" style="display:none;">' +
                '<div class="addr-row addr-row-3">' +
                    '<div class="addr-col">' +
                        '<label>Tỉnh/Thành phố <span style="color:#f37021;">*</span></label>' +
                        '<select class="addr-province-old" data-prefix="' + prefix + '">' +
                            '<option value="">-- Chọn Tỉnh/Thành --</option>' +
                        '</select>' +
                    '</div>' +
                    '<div class="addr-col">' +
                        '<label>Quận/Huyện <span style="color:#f37021;">*</span></label>' +
                        '<select class="addr-district-old" data-prefix="' + prefix + '" disabled>' +
                            '<option value="">-- Chọn Quận/Huyện --</option>' +
                        '</select>' +
                    '</div>' +
                    '<div class="addr-col">' +
                        '<label>Phường/Xã <span style="color:#f37021;">*</span></label>' +
                        '<select class="addr-ward-old" data-prefix="' + prefix + '" disabled>' +
                            '<option value="">-- Chọn Phường/Xã --</option>' +
                        '</select>' +
                    '</div>' +
                '</div>' +

                '<div class="addr-detail-wrap">' +
                    '<label>Địa chỉ chi tiết <span style="color:#9ca3af;font-weight:400;font-style:italic;">(số nhà, tên đường, thôn/xóm...)</span> <span style="color:#f37021;">*</span></label>' +
                    '<input type="text" class="addr-detail-old" data-prefix="' + prefix + '" ' +
                           'placeholder="Ví dụ: 123 Lê Lợi, Thôn 2..." autocomplete="off">' +
                '</div>' +
            '</div>' +

            '<!-- CHẾ ĐỘ 3: TỰ NHẬP TAY TOÀN BỘ -->' +
            '<div class="addr-manual-wrap" data-prefix="' + prefix + '" style="display:none;">' +
                '<label style="display:block;font-size:12px;font-weight:700;color:#4a5568;margin-bottom:6px;text-transform:uppercase;">' +
                    'Địa chỉ lắp đặt đầy đủ <span style="color:#f37021;">*</span>' +
                '</label>' +
                '<textarea class="addr-manual-input" data-prefix="' + prefix + '" rows="2" ' +
                          'placeholder="Ví dụ: Số 25 Đường 10, P. An Lợi Đông, Q. 2, TP. HCM"></textarea>' +
                '<small style="color:#64748b;font-size:12px;margin-top:4px;display:block;">Nhập số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành...</small>' +
            '</div>' +

            '<!-- Preview -->' +
            '<div class="addr-preview" data-prefix="' + prefix + '" style="display:none;">' +
                '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" ' +
                     'style="width:16px;height:16px;flex-shrink:0;">' +
                    '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>' +
                    '<circle cx="12" cy="10" r="3"/>' +
                '</svg>' +
                '<span class="addr-preview-text"></span>' +
            '</div>' +

            '<!-- Hidden input duy nhất gửi server -->' +
            '<input type="hidden" name="user_address" class="addr-final" data-prefix="' + prefix + '">';

        initAddressPicker(prefix);
    };

    // ===== Logic chính =====
    function initAddressPicker(prefix) {
        var modeBtn      = document.querySelector('.addr-mode-btn[data-prefix="' + prefix + '"]');
        var modeBtnText  = modeBtn ? modeBtn.querySelector('.addr-mode-btn-text') : null;
        var toggleBtn    = document.querySelector('.addr-toggle-btn[data-prefix="' + prefix + '"]');
        var toggleText   = toggleBtn ? toggleBtn.querySelector('.addr-toggle-text') : null;

        var newWrap   = document.querySelector('.addr-new-mode[data-prefix="' + prefix + '"]');
        var oldWrap   = document.querySelector('.addr-old-mode[data-prefix="' + prefix + '"]');
        var manualWrap= document.querySelector('.addr-manual-wrap[data-prefix="' + prefix + '"]');

        // Chế độ mới
        var provNew   = document.querySelector('.addr-province-new[data-prefix="' + prefix + '"]');
        var wardNew   = document.querySelector('.addr-ward-new[data-prefix="' + prefix + '"]');
        var detailNew = document.querySelector('.addr-detail[data-prefix="' + prefix + '"]');
        var customProvNew = document.querySelector('.addr-custom-prov-new[data-prefix="' + prefix + '"]');
        var customWardNew = document.querySelector('.addr-custom-ward-new[data-prefix="' + prefix + '"]');

        // Chế độ cũ
        var provOld   = document.querySelector('.addr-province-old[data-prefix="' + prefix + '"]');
        var distOld   = document.querySelector('.addr-district-old[data-prefix="' + prefix + '"]');
        var wardOld   = document.querySelector('.addr-ward-old[data-prefix="' + prefix + '"]');
        var detailOld = document.querySelector('.addr-detail-old[data-prefix="' + prefix + '"]');

        // Manual
        var manualInput = document.querySelector('.addr-manual-input[data-prefix="' + prefix + '"]');

        // Preview
        var preview     = document.querySelector('.addr-preview[data-prefix="' + prefix + '"]');
        var previewText = preview ? preview.querySelector('.addr-preview-text') : null;
        var finalInput  = document.querySelector('.addr-final[data-prefix="' + prefix + '"]');

        if (!provNew) return;

        // Trạng thái: 'new' | 'old' | 'manual'
        var currentMode = 'new';

        // ==========================================================
        // CHẾ ĐỘ MỚI (2 cấp) — dùng PROVINCE_DATA
        // ==========================================================
        function buildProvinceNew() {
            if (!isNewDataReady()) {
                provNew.innerHTML = '<option value="">-- Lỗi: Chưa nạp address-data.js --</option>';
                return;
            }
            provNew.innerHTML = '<option value="">-- Chọn Tỉnh/Thành phố --</option>';

            var sorted = PROVINCE_DATA.slice().sort(function (a, b) {
                return a.name.localeCompare(b.name, 'vi');
            });

            sorted.forEach(function (p) {
                var opt = document.createElement('option');
                opt.value = p.code;
                opt.textContent = p.name;
                opt.dataset.name = p.name;
                provNew.appendChild(opt);
            });

            var optOther = document.createElement('option');
            optOther.value = 'custom';
            optOther.textContent = 'Khác (Tự gõ Tỉnh/Thành)...';
            optOther.dataset.name = 'Khác';
            provNew.appendChild(optOther);
        }

        buildProvinceNew();

        provNew.addEventListener('change', function () {
            var code = this.value;

            if (code === 'custom') {
                customProvNew.style.display = 'block';
                customProvNew.focus();
                wardNew.innerHTML = '<option value="custom">Khác (Tự gõ Phường/Xã)...</option>';
                wardNew.disabled = false;
                customWardNew.style.display = 'block';
                updatePreview();
                return;
            }
            customProvNew.style.display = 'none';

            wardNew.innerHTML = '<option value="">-- Đang tải Phường/Xã... --</option>';
            wardNew.disabled = true;
            customWardNew.style.display = 'none';

            if (!code) {
                wardNew.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
                updatePreview();
                return;
            }

            var prov = PROVINCE_DATA.find(function (p) {
                return String(p.code) === String(code);
            });

            if (prov && prov.wards && prov.wards.length > 0) {
                fillWardNew(prov.wards);
            } else {
                wardNew.innerHTML = '<option value="custom">Khác (Tự gõ Phường/Xã)...</option>';
                wardNew.disabled = false;
                customWardNew.style.display = 'block';
            }
            updatePreview();
        });

        function fillWardNew(wards) {
            wardNew.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            var sorted = wards.slice().sort(function (a, b) {
                return a.name.localeCompare(b.name, 'vi');
            });
            sorted.forEach(function (w) {
                var opt = document.createElement('option');
                opt.value = w.code;
                opt.textContent = w.name;
                opt.dataset.name = w.name;
                wardNew.appendChild(opt);
            });
            var optOther = document.createElement('option');
            optOther.value = 'custom';
            optOther.textContent = 'Khác (Tự gõ Phường/Xã)...';
            optOther.dataset.name = 'Khác';
            wardNew.appendChild(optOther);
            wardNew.disabled = false;
        }

        wardNew.addEventListener('change', updatePreview);
        customProvNew.addEventListener('input', updatePreview);
        customWardNew.addEventListener('input', updatePreview);
        detailNew.addEventListener('input', updatePreview);

        // ==========================================================
        // CHẾ ĐỘ CŨ (3 cấp) — gọi API provinces.open-api.vn
        // ==========================================================
        var oldProvincesLoaded = false;

        function loadOldProvinces() {
            if (oldProvincesLoaded) return;
            oldProvincesLoaded = true;

            provOld.innerHTML = '<option value="">-- Đang tải Tỉnh/Thành... --</option>';
            fetch(API_OLD + '/p/')
                .then(function (r) {
                    if (!r.ok) throw new Error('API error: ' + r.status);
                    return r.json();
                })
                .then(function (data) {
                    provOld.innerHTML = '<option value="">-- Chọn Tỉnh/Thành --</option>';
                    data.forEach(function (p) {
                        var opt = document.createElement('option');
                        opt.value = p.code;
                        opt.textContent = p.name;
                        opt.dataset.name = p.name;
                        provOld.appendChild(opt);
                    });
                })
                .catch(function (err) {
                    console.error('Lỗi tải tỉnh cũ:', err);
                    provOld.innerHTML = '<option value="">-- Lỗi tải dữ liệu --</option>';
                    oldProvincesLoaded = false;
                });
        }

        provOld.addEventListener('change', function () {
            var code = this.value;
            distOld.innerHTML = '<option value="">-- Đang tải Quận/Huyện... --</option>';
            distOld.disabled = true;
            wardOld.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            wardOld.disabled = true;

            if (!code) {
                distOld.innerHTML = '<option value="">-- Chọn Quận/Huyện --</option>';
                updatePreview();
                return;
            }

            fetch(API_OLD + '/p/' + code + '?depth=2')
                .then(function (r) { return r.json(); })
                .then(function (data) {
                    distOld.innerHTML = '<option value="">-- Chọn Quận/Huyện --</option>';
                    (data.districts || []).forEach(function (d) {
                        var opt = document.createElement('option');
                        opt.value = d.code;
                        opt.textContent = d.name;
                        opt.dataset.name = d.name;
                        distOld.appendChild(opt);
                    });
                    distOld.disabled = false;
                })
                .catch(function (err) {
                    console.error('Lỗi tải quận:', err);
                    distOld.innerHTML = '<option value="">-- Lỗi tải dữ liệu --</option>';
                });
            updatePreview();
        });

        distOld.addEventListener('change', function () {
            var code = this.value;
            wardOld.innerHTML = '<option value="">-- Đang tải Phường/Xã... --</option>';
            wardOld.disabled = true;

            if (!code) {
                wardOld.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
                updatePreview();
                return;
            }

            fetch(API_OLD + '/d/' + code + '?depth=2')
                .then(function (r) { return r.json(); })
                .then(function (data) {
                    wardOld.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
                    (data.wards || []).forEach(function (w) {
                        var opt = document.createElement('option');
                        opt.value = w.code;
                        opt.textContent = w.name;
                        opt.dataset.name = w.name;
                        wardOld.appendChild(opt);
                    });
                    wardOld.disabled = false;
                })
                .catch(function (err) {
                    console.error('Lỗi tải phường:', err);
                    wardOld.innerHTML = '<option value="">-- Lỗi tải dữ liệu --</option>';
                });
            updatePreview();
        });

        wardOld.addEventListener('change', updatePreview);
        detailOld.addEventListener('input', updatePreview);

        // ==========================================================
        // NÚT CHUYỂN CHẾ ĐỘ MỚI ↔ CŨ
        // ==========================================================
        modeBtn.addEventListener('click', function () {
            // Reset các chế độ về trạng thái chưa chọn
            if (currentMode === 'new') {
                // Chuyển sang cũ
                newWrap.style.display = 'none';
                manualWrap.style.display = 'none';
                oldWrap.style.display = 'block';
                modeBtnText.textContent = 'Dùng địa chỉ mới';
                currentMode = 'old';
                loadOldProvinces();
            } else {
                // Chuyển sang mới
                oldWrap.style.display = 'none';
                manualWrap.style.display = 'none';
                newWrap.style.display = 'block';
                modeBtnText.textContent = 'Dùng địa chỉ cũ';
                currentMode = 'new';
            }

            // Xóa preview và hidden input để tránh nhầm lẫn
            if (finalInput) finalInput.value = '';
            if (preview) preview.style.display = 'none';

            // Reset nút toggle nếu đang ở chế độ manual
            if (toggleText) toggleText.textContent = 'Không tìm thấy nơi của bạn? Bấm để tự nhập tay';

            updatePreview();
        });

        // ==========================================================
        // NÚT TOGGLE TỰ NHẬP TAY
        // ==========================================================
        toggleBtn.addEventListener('click', function () {
            var isManual = manualWrap.style.display !== 'none';

            if (isManual) {
                // Quay lại chế độ trước đó (new hoặc old)
                manualWrap.style.display = 'none';
                if (currentMode === 'new') newWrap.style.display = 'block';
                else oldWrap.style.display = 'block';
                toggleText.textContent = 'Không tìm thấy nơi của bạn? Bấm để tự nhập tay';
            } else {
                // Sang manual
                newWrap.style.display = 'none';
                oldWrap.style.display = 'none';
                manualWrap.style.display = 'block';
                toggleText.textContent = 'Quay lại chọn theo danh sách';
                if (manualInput) {
                    if (!manualInput.value && finalInput && finalInput.value) {
                        manualInput.value = finalInput.value;
                    }
                    manualInput.focus();
                }
            }
            updatePreview();
        });

        manualInput.addEventListener('input', updatePreview);

        // ==========================================================
        // CẬP NHẬT PREVIEW + HIDDEN INPUT
        // ==========================================================
        function updatePreview() {
            var isManual = manualWrap && manualWrap.style.display !== 'none';

            if (isManual) {
                var v = manualInput ? manualInput.value.trim() : '';
                if (!v) {
                    preview.style.display = 'none';
                    finalInput.value = '';
                    return;
                }
                preview.style.display = 'flex';
                previewText.textContent = v;
                finalInput.value = v;
                return;
            }

            // Chế độ cũ (3 cấp)
            if (currentMode === 'old') {
                var p = getSelectedText(provOld);
                var d = getSelectedText(distOld);
                var w = getSelectedText(wardOld);
                var dt = detailOld ? detailOld.value.trim() : '';
                var parts = [dt, w, d, p].filter(Boolean);
                if (parts.length === 0) {
                    preview.style.display = 'none';
                    finalInput.value = '';
                    return;
                }
                var full = parts.join(', ');
                preview.style.display = 'flex';
                previewText.textContent = full;
                finalInput.value = full;
                return;
            }

            // Chế độ mới (2 cấp)
            var pn = (provNew.value === 'custom')
                ? (customProvNew ? customProvNew.value.trim() : '')
                : getSelectedText(provNew);
            var wn = (wardNew.value === 'custom')
                ? (customWardNew ? customWardNew.value.trim() : '')
                : getSelectedText(wardNew);
            var dtn = detailNew ? detailNew.value.trim() : '';
            var partsn = [dtn, wn, pn].filter(Boolean);
            if (partsn.length === 0) {
                preview.style.display = 'none';
                finalInput.value = '';
                return;
            }
            var fulln = partsn.join(', ');
            preview.style.display = 'flex';
            previewText.textContent = fulln;
            finalInput.value = fulln;
        }

        function getSelectedText(sel) {
            if (!sel || !sel.selectedIndex || sel.selectedIndex < 0) return '';
            var opt = sel.options[sel.selectedIndex];
            return (opt && opt.dataset && opt.dataset.name) || opt.textContent || '';
        }

        // ==========================================================
        // API cho validate từ bên ngoài
        // ==========================================================
        window['getAddressMode_' + prefix] = function () {
            if (manualWrap.style.display !== 'none') return 'manual';
            return currentMode;
        };
    }

    // ===== Auto init =====
    function initAll() {
        var pickers = document.querySelectorAll('[data-address-picker]');
        for (var i = 0; i < pickers.length; i++) {
            var el = pickers[i];
            if (!el.getAttribute('data-initialized')) {
                el.setAttribute('data-initialized', 'true');
                renderAddressPicker(el.id, el.dataset.addressPicker);
            }
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }
})();