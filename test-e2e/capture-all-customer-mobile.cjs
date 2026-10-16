const { chromium } = require('playwright-core');
const fs = require('fs');
const path = require('path');

(async () => {
    const executablePath = process.env.FPT_TEST_BROWSER_BIN || [
        'C:/Program Files/Google/Chrome/Application/chrome.exe',
        'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
    ].find(candidate => fs.existsSync(candidate));

    if (!executablePath) throw new Error('No Chromium executable found.');

    const browser = await chromium.launch({ headless: true, executablePath });
    const context = await browser.newContext({
        viewport: { width: 375, height: 812 },
        userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1'
    });
    const page = await context.newPage();

    const failedRequests = [];
    const consoleErrors = [];

    page.on('response', resp => {
        if (resp.status() >= 400) {
            failedRequests.push({ url: resp.url(), status: resp.status() });
        }
    });

    page.on('console', msg => {
        if (msg.type() === 'error') {
            consoleErrors.push(msg.text());
        }
    });

    const outDir = path.join(__dirname, 'screenshots-mobile');
    if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

    console.log('Navigating to http://localhost:8084/fpt-sale/home...');
    await page.goto('http://localhost:8084/fpt-sale/home', { waitUntil: 'networkidle' });

    // Full page screenshot
    await page.screenshot({ path: path.join(outDir, '01-full-home.png'), fullPage: true });
    console.log('Captured 01-full-home.png (full page)');

    // Top / Hero section
    await page.screenshot({ path: path.join(outDir, '02-hero.png') });

    // Section by section
    const sections = [
        { name: 'packages', selector: '#packages' },
        { name: 'mesh', selector: '.mesh-section' },
        { name: 'features', selector: '.features' },
        { name: 'devices', selector: '.devices' },
        { name: 'contact', selector: '.contact-section' },
        { name: 'footer', selector: '.footer' }
    ];

    for (const sec of sections) {
        const el = await page.$(sec.selector);
        if (el) {
            await el.scrollIntoViewIfNeeded();
            await page.waitForTimeout(300);
            await page.screenshot({ path: path.join(outDir, `03-${sec.name}.png`) });
            console.log(`Captured 03-${sec.name}.png`);
        } else {
            console.log(`Element ${sec.selector} not found!`);
        }
    }

    // Open Drawer
    const hamburger = await page.$('.hamburger-btn');
    if (hamburger) {
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.waitForTimeout(200);
        await hamburger.click();
        await page.waitForTimeout(400);
        await page.screenshot({ path: path.join(outDir, '04-drawer.png') });
        console.log('Captured 04-drawer.png');
        await page.click('.mobile-menu-close');
        await page.waitForTimeout(400);
    }

    // Open Chatbot
    const chatBtn = await page.$('.ai-chat-button');
    if (chatBtn) {
        await chatBtn.click();
        await page.waitForTimeout(600);
        await page.screenshot({ path: path.join(outDir, '05-chatbot-open.png') });
        console.log('Captured 05-chatbot-open.png');
    }

    // Check broken images across the whole page
    const brokenImages = await page.evaluate(() => {
        const imgs = Array.from(document.querySelectorAll('img'));
        return imgs.map(img => ({
            src: img.src,
            alt: img.alt,
            naturalWidth: img.naturalWidth,
            naturalHeight: img.naturalHeight,
            complete: img.complete,
            displayedWidth: img.offsetWidth,
            displayedHeight: img.offsetHeight
        })).filter(img => img.naturalWidth === 0 || img.displayedWidth === 0);
    });

    console.log('Broken/Hidden images:', JSON.stringify(brokenImages, null, 2));
    console.log('Failed network requests:', JSON.stringify(failedRequests, null, 2));
    console.log('Console errors:', JSON.stringify(consoleErrors, null, 2));

    await browser.close();
})();
