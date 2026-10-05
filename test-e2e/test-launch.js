const { chromium } = require('playwright-core');

async function testLaunch() {
    try {
        const browser = await chromium.launch({
            channel: 'chrome',
            headless: true
        });
        const page = await browser.newPage();
        await page.goto('http://localhost:8084/fpt-sale/home', { timeout: 10000 });
        const title = await page.title();
        console.log('SUCCESS: Browser launched! Page title:', title);
        await browser.close();
    } catch (err) {
        console.error('Launch failed:', err);
    }
}

testLaunch();
