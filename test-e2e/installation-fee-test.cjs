const { chromium } = require('playwright-core');
const assert = require('node:assert/strict');

// Read-only responsive check against the user's local Tomcat. No form submissions.
(async () => {
  const browser = await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
  try {
    const page = await browser.newPage();
    const base = process.env.FPT_TEST_BASE_URL || 'http://localhost:8084/fpt-sale';
    for (const width of [1440,768,375]) {
      await page.setViewportSize({width,height:1000});
      const response = await page.goto(base+'/home',{waitUntil:'networkidle'});
      assert.equal(response.status(),200);
      const fees = page.locator('.pkg-body .standard-installation-fee');
      assert.equal(await fees.count(),6);
      for (const fee of await fees.all()) {
        assert.match(await fee.innerText(),/300\.000đ\/lần/);
        const fits = await fee.evaluate(el => {
          const card=el.closest('.pkg-body'), button=card.querySelector('.btn-pkg');
          const f=el.getBoundingClientRect(), c=card.getBoundingClientRect(), b=button.getBoundingClientRect();
          return el.scrollWidth<=el.clientWidth+1 && el.scrollHeight<=el.clientHeight+1 && f.left>=c.left && f.right<=c.right+1 && f.bottom<=b.top+1;
        });
        assert.ok(fits,'Fee clipped or overlaps purchase button at '+width);
      }
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth<=innerWidth+1),'Horizontal overflow at '+width);
      const links = await page.locator('.pkg-body .btn-pkg').evaluateAll(els=>els.map(e=>e.href));
      for (const link of links) {
        const detail = await page.goto(link,{waitUntil:'networkidle'});
        assert.equal(detail.status(),200);
        const fee = page.locator('.standard-installation-fee');
        assert.equal(await fee.count(),1);
        assert.match(await fee.innerText(),/300\.000đ\/lần/);
        assert.ok(await fee.evaluate(el=>el.scrollWidth<=el.clientWidth+1 && el.scrollHeight<=el.clientHeight+1),'Detail fee clipped');
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth<=innerWidth+1),'Detail horizontal overflow at '+width);
        assert.equal(await page.locator('input[name="registration_consent"]').count(),1);
      }
      console.log('PASS installation fee home + six details at '+width+'px');
    }
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
