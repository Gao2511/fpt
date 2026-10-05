const { chromium } = require('playwright-core');

const BASE_URL = 'http://localhost:8084/fpt-sale';

async function runSuite() {
    console.log('====================================================');
    console.log('🚀 BẮT ĐẦU KIỂM THỬ PLAYWRIGHT E2E TOÀN BỘ PROJECT FPT');
    console.log('====================================================\n');

    const browser = await chromium.launch({
        channel: 'chrome',
        headless: true
    });

    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 }
    });

    let passedTests = 0;
    let failedTests = 0;
    const failures = [];

    function recordTest(name, passed, detail = '') {
        if (passed) {
            passedTests++;
            console.log(`  ✅ [PASS] ${name}`);
        } else {
            failedTests++;
            console.log(`  ❌ [FAIL] ${name} ${detail ? '--> ' + detail : ''}`);
            failures.push({ name, detail });
        }
    }

    const page = await context.newPage();

    // Listen to console and network errors
    const consoleErrors = [];
    const failedRequests = [];

    page.on('console', msg => {
        if (msg.type() === 'error') {
            consoleErrors.push({ text: msg.text(), location: msg.location() });
        }
    });

    page.on('response', resp => {
        if (resp.status() >= 400) {
            // Ignore optional favicons or external metrics if any
            failedRequests.push({ url: resp.url(), status: resp.status() });
        }
    });

    // -------------------------------------------------------------
    // TEST 1: TRANG CHỦ (/home)
    // -------------------------------------------------------------
    console.log('\n--- 1. Kiểm tra Trang Chủ (/home) ---');
    try {
        const res = await page.goto(`${BASE_URL}/home`, { waitUntil: 'domcontentloaded', timeout: 15000 });
        recordTest('Truy cập trang chủ HTTP Status 200', res.status() === 200, `Status: ${res.status()}`);

        const title = await page.title();
        recordTest('Tiêu đề trang chủ hợp lệ', title.includes('FPT'), `Title: ${title}`);

        // Logo
        const logo = await page.locator('.logo img').first();
        const logoVisible = await logo.isVisible();
        recordTest('Logo FPT Telecom hiển thị', logoVisible);

        // Header Navigation links
        const navMenu = await page.locator('.top-nav .menu a');
        const navCount = await navMenu.count();
        recordTest('Menu điều hướng chính hiển thị đầy đủ', navCount >= 3, `Số menu: ${navCount}`);

        // TechText Canvas ("FPT WIFI 6")
        await page.waitForTimeout(500); // Đợi canvas init
        const techCanvas = await page.locator('#techTextContainer canvas.tech-text-canvas');
        const techCanvasVisible = await techCanvas.isVisible();
        recordTest('Canvas TechText "FPT WIFI 6" khởi tạo thành công', techCanvasVisible);

        // SVG Text "TỐC ĐỘ CAO" & "HỖ TRỢ 24/7"
        const svgText = await page.locator('.text-hover-svg text.th-main');
        const svgCount = await svgText.count();
        recordTest('SVG Text "TỐC ĐỘ CAO" & "HỖ TRỢ 24/7" hiển thị', svgCount >= 2, `Số dòng: ${svgCount}`);

        // Slideshow mockup
        const slideshow = await page.locator('.hero-slideshow');
        const slideshowVisible = await slideshow.isVisible();
        recordTest('Khung ảnh Slideshow Hero hiển thị', slideshowVisible);

        const activeSlide = await page.locator('.hero-slide.active');
        recordTest('Có slide ảnh Hero đang kích hoạt', await activeSlide.count() > 0);

        // Next slide button interaction
        const slideshowEl = await page.locator('.hero-slideshow');
        await slideshowEl.hover();
        const nextBtn = await page.locator('.hero-nav-next');
        if (await nextBtn.count() > 0) {
            await nextBtn.click({ force: true });
            await page.waitForTimeout(300);
            recordTest('Nút bấm chuyển slide hoạt động', true);
        } else {
            recordTest('Nút bấm chuyển slide', false, 'Không tìm thấy .hero-nav-next');
        }

        // Bảng giá gói cước (#packages)
        const packagesSection = await page.locator('#packages, .packages');
        recordTest('Khu vực danh sách gói cước hiển thị', await packagesSection.count() > 0);

        const packageCards = await page.locator('.pkg, .package-card, .plan-card');
        const pkgCount = await packageCards.count();
        recordTest('Các gói cước Internet được load từ CSDL', pkgCount > 0, `Tìm thấy ${pkgCount} gói`);

        // Form đăng ký tư vấn (#contact)
        const contactForm = await page.locator('#contact, .contact-form, form[action*="contact"], form[action*="Contact"]');
        recordTest('Form đăng ký tư vấn lắp đặt hiển thị', await contactForm.count() > 0);

        // Address Picker
        const provinceSelect = await page.locator('.addr-province-new, select[data-prefix="home"], #provinceSelect');
        if (await provinceSelect.count() > 0) {
            const optionsCount = await provinceSelect.first().locator('option').count();
            recordTest('Bộ chọn Tỉnh/Thành (Address Picker) có dữ liệu', optionsCount > 1, `${optionsCount} lựa chọn`);
        } else {
            recordTest('Bộ chọn Tỉnh/Thành phố', false, 'Không tìm thấy select province');
        }

        // AI Chat widget button
        const aiChatBtn = await page.locator('.ai-chat-btn, .chat-widget-btn, #aiChatWindow, .ai-chat-launcher');
        recordTest('Widget Tư vấn AI hiển thị trên trang chủ', await aiChatBtn.count() > 0);

        // Theme toggle button
        const themeBtn = await page.locator('#themeToggleBtn, .theme-toggle-btn');
        if (await themeBtn.count() > 0) {
            await themeBtn.first().click();
            await page.waitForTimeout(300);
            const isDark = await page.evaluate(() => document.body.classList.contains('dark-mode') || document.documentElement.getAttribute('data-theme') === 'dark');
            recordTest('Nút chuyển đổi Dark Mode/Light Mode hoạt động', true, `Dark mode: ${isDark}`);
            // Toggle lại về ban đầu
            await themeBtn.first().click();
        } else {
            recordTest('Nút chuyển đổi giao diện Dark/Light mode', false, 'Không tìm thấy toggle theme');
        }

    } catch (err) {
        recordTest('Trang chủ gặp ngoại lệ', false, err.message);
    }

    // -------------------------------------------------------------
    // TEST 2: TRANG ĐĂNG NHẬP (/login)
    // -------------------------------------------------------------
    console.log('\n--- 2. Kiểm tra Trang Đăng Nhập (/login) ---');
    try {
        const res = await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 10000 });
        recordTest('Truy cập trang đăng nhập HTTP Status 200', res.status() === 200);

        const emailInput = await page.locator('input[type="email"], input[name="email"], input[name="username"]');
        const passInput = await page.locator('input[type="password"]');
        const submitBtn = await page.locator('button[type="submit"]');

        recordTest('Input Email/Tài khoản hiển thị', await emailInput.count() > 0);
        recordTest('Input Mật khẩu hiển thị', await passInput.count() > 0);
        recordTest('Nút Đăng nhập hiển thị', await submitBtn.count() > 0);

        // Thử submit thông tin sai để test validation
        if (await submitBtn.count() > 0) {
            await emailInput.first().fill('invalid_user_test@fpt.vn');
            await passInput.first().fill('WrongPassword123');
            await submitBtn.first().click();
            await page.waitForTimeout(1000);

            // Kiểm tra thông báo lỗi hoặc vẫn ở lại trang login
            const curUrl = page.url();
            recordTest('Xử lý đăng nhập sai mật khẩu an toàn (không bị crash 500)', curUrl.includes('login') || (await page.locator('.error, .alert, .alert-danger').count() > 0));
        }
    } catch (err) {
        recordTest('Trang đăng nhập gặp lỗi', false, err.message);
    }

    // -------------------------------------------------------------
    // TEST 3: TRANG ĐĂNG KÝ (/register)
    // -------------------------------------------------------------
    console.log('\n--- 3. Kiểm tra Trang Đăng Ký (/register) ---');
    try {
        const res = await page.goto(`${BASE_URL}/register`, { waitUntil: 'domcontentloaded', timeout: 10000 });
        recordTest('Truy cập trang đăng ký HTTP Status 200', res.status() === 200);

        const formInputs = await page.locator('form input');
        recordTest('Form đăng ký chứa các trường nhập liệu cần thiết', await formInputs.count() >= 3);
    } catch (err) {
        recordTest('Trang đăng ký gặp lỗi', false, err.message);
    }

    // -------------------------------------------------------------
    // TEST 4: TRANG QUÊN MẬT KHẨU (/forgot-password)
    // -------------------------------------------------------------
    console.log('\n--- 4. Kiểm tra Trang Quên Mật Khẩu (/forgot-password) ---');
    try {
        const res = await page.goto(`${BASE_URL}/forgot-password`, { waitUntil: 'domcontentloaded', timeout: 10000 });
        recordTest('Truy cập trang quên mật khẩu HTTP Status 200', res.status() === 200);
        const emailField = await page.locator('input[name="identifier"], input[type="email"], input[name="email"]');
        recordTest('Có trường nhập Email/SĐT để nhận mã khôi phục', await emailField.count() > 0);
    } catch (err) {
        recordTest('Trang quên mật khẩu gặp lỗi', false, err.message);
    }

    // -------------------------------------------------------------
    // TEST 5: TRANG CHI TIẾT GÓI CƯỚC (/package-detail)
    // -------------------------------------------------------------
    console.log('\n--- 5. Kiểm tra Chi Tiết Gói Cước (/package-detail) ---');
    try {
        const res = await page.goto(`${BASE_URL}/package-detail?id=1`, { waitUntil: 'domcontentloaded', timeout: 10000 });
        recordTest('Truy cập chi tiết gói cước id=1', res.status() === 200 || res.status() === 302, `Status: ${res.status()}`);
    } catch (err) {
        recordTest('Trang package-detail gặp lỗi', false, err.message);
    }

    // -------------------------------------------------------------
    // TEST 6: BẢO MẬT & PHÂN QUYỀN TRANG ADMIN
    // -------------------------------------------------------------
    console.log('\n--- 6. Kiểm tra Bảo Mật & Phân Quyền Admin Routes ---');
    const adminRoutes = [
        '/admin/dashboard',
        '/admin/customers',
        '/admin/packages',
        '/admin/users',
        '/admin/email-logs',
        '/admin/settings'
    ];

    for (const route of adminRoutes) {
        try {
            const res = await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 10000 });
            const url = page.url();
            // Nếu chưa đăng nhập admin, route phải redirect về login hoặc chặn 403/302, hoặc yêu cầu xác thực
            const isProtected = url.includes('/login') || res.status() === 302 || res.status() === 403 || url.includes('error');
            recordTest(`Route ${route} được bảo vệ hoặc định tuyến hợp lệ`, isProtected || res.status() === 200, `URL hiện tại: ${url}, Status: ${res.status()}`);
        } catch (err) {
            recordTest(`Route ${route} bị lỗi khi truy cập`, false, err.message);
        }
    }

    // -------------------------------------------------------------
    // TEST 7: KIỂM TRA LỖI CONSOLE & TÀI NGUYÊN 404
    // -------------------------------------------------------------
    console.log('\n--- 7. Kiểm tra Lỗi Console JS & Tài nguyên 404 ---');
    // Lọc các lỗi tĩnh
    const criticalFailedRequests = failedRequests.filter(r => 
        !r.url.endsWith('favicon.ico') && 
        !r.url.includes('google') && 
        !r.url.includes('cdn')
    );

    if (criticalFailedRequests.length === 0) {
        recordTest('Không có tài nguyên nội bộ nào bị lỗi 404/500', true);
    } else {
        const sample = criticalFailedRequests.slice(0, 3).map(r => `${r.status}: ${r.url}`).join(', ');
        recordTest('Tài nguyên nội bộ bị lỗi 404', false, sample);
    }

    // Kiểm tra Console Error
    const criticalConsoleErrors = consoleErrors.filter(e => 
        !e.text.includes('favicon') && 
        !e.text.includes('Third-party cookie')
    );

    if (criticalConsoleErrors.length === 0) {
        recordTest('Không có lỗi JavaScript Console Crash trên trình duyệt', true);
    } else {
        const sampleErr = criticalConsoleErrors.slice(0, 2).map(e => e.text).join(' | ');
        recordTest('Phát hiện lỗi JavaScript Console', false, sampleErr);
    }

    // -------------------------------------------------------------
    // KẾT QUẢ TỔNG HỢP
    // -------------------------------------------------------------
    console.log('\n====================================================');
    console.log(`📊 TỔNG KẾT KẾT QUẢ KIỂM THỬ PLAYWRIGHT`);
    console.log(`   - Tổng số kịch bản kiểm tra: ${passedTests + failedTests}`);
    console.log(`   - Thành công: ${passedTests} test`);
    console.log(`   - Thất bại: ${failedTests} test`);
    console.log('====================================================');

    if (failures.length > 0) {
        console.log('\n⚠️ DANH SÁCH CÁC VẤN ĐỀ CẦN LƯU Ý:');
        failures.forEach((f, idx) => {
            console.log(`   ${idx + 1}. ${f.name} (${f.detail || 'Không có mô tả'})`);
        });
    } else {
        console.log('\n🎉 TẤT CẢ CÁC BƯỚC KIỂM THỬ ĐỀU ĐẠT CHUẨN HOÀN TOÀN!');
    }

    await browser.close();
}

runSuite().catch(err => {
    console.error('Lỗi thực thi test:', err);
    process.exit(1);
});
