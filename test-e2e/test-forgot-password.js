const { chromium } = require('playwright-core');

const BASE_URL = 'http://localhost:8084/fpt-sale';

async function runTest() {
    console.log('====================================================');
    console.log('🧪 BẮT ĐẦU TEST LUỒNG QUÊN MẬT KHẨU & OTP');
    console.log('====================================================\n');

    const browser = await chromium.launch({
        channel: 'chrome',
        headless: true
    });

    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 }
    });

    const page = await context.newPage();

    let passed = 0;
    let failed = 0;

    function assert(desc, condition, detail = '') {
        if (condition) {
            passed++;
            console.log(`  ✅ [PASS] ${desc}`);
        } else {
            failed++;
            console.log(`  ❌ [FAIL] ${desc} ${detail ? '--> ' + detail : ''}`);
        }
    }

    try {
        // -------------------------------------------------------------
        // SCENARIO 1: Chuyển từ Login sang Forgot Password KHI ĐÃ NHẬP EMAIL
        // -------------------------------------------------------------
        console.log('\n--- Scenario 1: Pre-fill email từ Login sang Forgot Password ---');
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
        
        await page.fill('#inputEmail', 'khachhang_test@fpt.vn');
        await page.click('#linkForgotPassword');
        await page.waitForTimeout(500);

        const currentUrl1 = page.url();
        assert('URL chứa tham số email pre-filled', currentUrl1.includes('email=khachhang_test%40fpt.vn') || currentUrl1.includes('email=khachhang_test@fpt.vn'), currentUrl1);
        
        const emailInputVal = await page.inputValue('#inputEmail');
        assert('Input Email trên trang Quên mật khẩu được điền sẵn', emailInputVal === 'khachhang_test@fpt.vn', emailInputVal);

        // -------------------------------------------------------------
        // SCENARIO 2: Chuyển từ Login sang Forgot Password KHI CHƯA NHẬP EMAIL
        // -------------------------------------------------------------
        console.log('\n--- Scenario 2: Chưa nhập email ở Login -> input trống ở Forgot Password ---');
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
        await page.click('#linkForgotPassword');
        await page.waitForTimeout(500);

        const currentUrl2 = page.url();
        assert('URL không chứa tham số email', !currentUrl2.includes('email='), currentUrl2);
        
        const emailInputValEmpty = await page.inputValue('#inputEmail');
        assert('Input Email trên trang Quên mật khẩu để trống', emailInputValEmpty === '', emailInputValEmpty);

        // -------------------------------------------------------------
        // SCENARIO 3: Nhập email không tồn tại -> Thông báo lỗi
        // -------------------------------------------------------------
        console.log('\n--- Scenario 3: Nhập email không tồn tại trong DB ---');
        await page.fill('#inputEmail', 'nonexistent_user_99999@fpt.vn');
        await page.click('#btnSubmitForgot');
        await page.waitForTimeout(1000);

        const alertText = await page.locator('.glass-alert-error, .glass-alert').first().innerText().catch(() => '');
        assert('Báo lỗi email không tồn tại trong hệ thống', alertText.includes('chưa được đăng ký') || alertText.includes('không tồn tại'), alertText);

        // -------------------------------------------------------------
        // SCENARIO 4: Kiểm tra form OTP bước 2 (Render và Validation)
        // -------------------------------------------------------------
        console.log('\n--- Scenario 4: Kiểm tra giao diện và validation mã OTP ---');
        // Test UI direct OTP step check via POST request
        const resOtp = await page.request.post(`${BASE_URL}/forgot-password`, {
            form: {
                action: 'verify_otp',
                email: 'test@fpt.vn',
                otp: '123'
            }
        });
        const resOtpBody = await resOtp.text();
        assert('Validation OTP không đúng hoặc hết hạn hoạt động chuẩn xác', resOtpBody.includes('Mã OTP không chính xác') || resOtpBody.includes('Xác thực OTP'));

        // -------------------------------------------------------------
        // SCENARIO 5: Kiểm tra Trang Reset Password với Token hợp lệ / không hợp lệ
        // -------------------------------------------------------------
        console.log('\n--- Scenario 5: Kiểm tra /reset-password ---');
        await page.goto(`${BASE_URL}/reset-password?token=invalid_token_xyz`, { waitUntil: 'domcontentloaded' });
        const resetErrorText = await page.locator('.glass-alert-error, .glass-alert').first().innerText().catch(() => '');
        assert('Từ chối token reset không hợp lệ hoặc hết hạn', resetErrorText.includes('không hợp lệ') || resetErrorText.includes('hết hạn'), resetErrorText);

        // -------------------------------------------------------------
        // SCENARIO 6: Kiểm tra Thông báo đăng nhập sau khi reset thành công (?reset=success)
        // -------------------------------------------------------------
        console.log('\n--- Scenario 6: Kiểm tra Alert sau khi reset thành công ở Login ---');
        await page.goto(`${BASE_URL}/login?reset=success`, { waitUntil: 'domcontentloaded' });
        const successAlert = await page.locator('.glass-alert-success').first().innerText().catch(() => '');
        assert('Trang login hiển thị thông báo Đặt lại mật khẩu thành công', successAlert.includes('Đặt lại mật khẩu thành công'), successAlert);

    } catch (e) {
        console.error('Lỗi khi chạy test:', e);
        failed++;
    } finally {
        await browser.close();
    }

    console.log('\n====================================================');
    console.log(`🏁 KẾT QUẢ: ${passed} PASS, ${failed} FAIL`);
    console.log('====================================================');
    process.exit(failed > 0 ? 1 : 0);
}

runTest();
