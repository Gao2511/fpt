'use strict';
const {chromium} = require('playwright-core');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
    const executablePath = process.env.FPT_TEST_BROWSER_BIN || [
        'C:/Program Files/Google/Chrome/Application/chrome.exe',
        'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
    ].find(candidate => fs.existsSync(candidate));
    if (!executablePath) throw new Error('Set FPT_TEST_BROWSER_BIN to a Chromium browser executable.');
    const browser = await chromium.launch({headless: true, executablePath});
    try {
        const page = await browser.newPage({viewport: {width: 1100, height: 800}});
        const errors = []; page.on('pageerror', error => errors.push(error.message));
        const source = fs.readFileSync(path.join(__dirname, '../web/view/home.jsp'), 'utf8');
        const begin = source.indexOf('<div class="ai-chat-button"');
        const finish = source.indexOf('<script src="${pageContext.request.contextPath}/js/ai-chat.js"', begin);
        const widget = source.slice(begin, finish);
        const messages = [
            {reply: 'Gói 195K: 195.000đ/tháng, 300Mbps, modem WiFi 6.\nAnh/chị cần tư vấn thêm không?', action: 'none', state: {secret: 'INTERNAL_STATE'}},
            {reply: 'Anh/chị vui lòng kiểm tra thông tin và gửi biểu mẫu tư vấn.', action: 'open_registration', registrationPath: '/package-detail?id=3#registration'},
            {reply: {message: 'DO_NOT_DISPLAY_RAW_JSON'}, action: 'none'}
        ];
        let requests = 0;
        await page.route('https://chatbot.test/**', async route => {
            if (route.request().url().endsWith('/ai-chat')) {
                requests++; await route.fulfill({contentType: 'application/json', body: JSON.stringify(messages.shift())});
            } else await route.fulfill({contentType: 'text/html', body: '<!doctype html><html lang="vi"><meta charset="UTF-8"><body>' + widget + '</body></html>'});
        });
        await page.goto('https://chatbot.test/');
        await page.addStyleTag({path: path.join(__dirname, '../web/assets/style/ai-chat.css')});
        await page.addScriptTag({path: path.join(__dirname, '../web/js/ai-chat.js')});
        await page.evaluate(() => FptChat.init(''));
        await page.locator('#aiChatBtn').click();
        async function ask(question, count) {
            await page.locator('#aiChatInput').fill(question); await page.locator('#aiChatInput').press('Enter');
            await page.waitForFunction(expected => document.querySelectorAll('.ai-msg-bot').length === expected && !document.getElementById('aiChatInput').disabled, count);
        }
        await ask('Hiện tại có gói nào phù hợp với sinh viên không?', 2);
        assert.match(await page.locator('.ai-msg-bot').last().textContent(), /195\.000đ\/tháng/);
        assert.equal(await page.locator('#aiChatBody').textContent().then(t => t.includes('INTERNAL_STATE')), false);
        assert.equal(await page.locator('.ai-msg-bot').last().evaluate(el => getComputedStyle(el).whiteSpace), 'pre-wrap');
        console.log('PASS browser renders validated multiline Vietnamese text without envelope metadata');
        await ask('Tôi muốn đăng ký gói 220K.', 3);
        assert.equal(await page.locator('.ai-msg-bot a').getAttribute('href'), '/package-detail?id=3#registration');
        assert.equal(requests, 2); console.log('PASS browser offers a local registration link without submitting');
        await ask('Chào bạn!', 4);
        assert.match(await page.locator('.ai-msg-bot').last().textContent(), /chưa nhận được câu trả lời đầy đủ/);
        assert.equal(await page.locator('#aiChatBody').textContent().then(t => t.includes('DO_NOT_DISPLAY_RAW_JSON')), false);
        console.log('PASS browser handles malformed response without displaying raw JSON');
        await page.setViewportSize({width: 390, height: 844});
        const bounds = await page.locator('#aiChatWindow').boundingBox();
        assert(bounds.x >= 0 && bounds.x + bounds.width <= 391, 'Chat widget overflows mobile viewport');
        assert.equal(errors.length, 0); console.log('PASS browser chat fits mobile viewport');
        console.log('Chat browser: 4 passed (all network responses mocked).');
    } finally {await browser.close();}
})().catch(error => {console.error(error);process.exitCode = 1;});
