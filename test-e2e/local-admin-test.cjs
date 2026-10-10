'use strict';
// Explicitly writes and rolls back a CMS revision on the LOCAL database only.
let stage='setup';
const fs=require('node:fs');
const assert=require('node:assert/strict');
const {chromium}=require('./node_modules/playwright-core');
(async()=>{
 const config=JSON.parse(fs.readFileSync('.local/runtime.json','utf8').replace(/^\uFEFF/,''));
 assert.equal(config.DB_HOST,'127.0.0.1');assert.equal(config.DB_PORT,'15432');assert.equal(config.DB_NAME,'fpt_local');
 const credentials=fs.readFileSync('.local/admin-credentials.txt','utf8');
 const username=credentials.match(/^Username: (.+)$/m)[1].trim();
 const password=credentials.match(/^Password: (.+)$/m)[1].trim();
 const origin='http://localhost:8085';const root=origin+'/fpt-sale';
 const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'});
 try {
  const context=await browser.newContext();
  await context.route('**/*',r=>new URL(r.request().url()).origin===origin?r.continue():r.abort());
  const page=await context.newPage();
  stage='login page';await page.goto(root+'/login');
  await page.locator('[name=identifier]').fill(username);await page.locator('[name=password]').fill(password);
  stage='login submit';await Promise.all([page.waitForURL('**/admin/dashboard'),page.locator('#btnLoginSubmit').click()]);
  assert.match(await page.locator('body').innerText(),/ADMIN/);
  for(const path of ['customers','packages','users','settings','content','business','email-logs']) {
   stage='admin '+path;const response=await page.goto(root+'/admin/'+path);
   assert.equal(response.status(),200,path);assert.ok(!page.url().endsWith('/login'),path);
  }
  stage='CMS edit';await page.goto(root+'/admin/content');
  const original=await page.locator('[name="hero.title"]').inputValue();
  const marker='LOCAL ADMIN TEST';
  await page.locator('[name="hero.title"]').fill(marker);
  const originalImage=await page.locator('[name="hero.image1"]').inputValue();
  await page.locator('.cms-sidebar a[href="#cms-images"]').click();
  const imageCard=page.locator('[data-image-field]').filter({has:page.locator('[name="hero.image1"]')});
  const encoded=await page.evaluate(()=>{const canvas=document.createElement('canvas');canvas.width=20;canvas.height=10;const c=canvas.getContext('2d');c.fillStyle='#f37021';c.fillRect(0,0,20,10);return canvas.toDataURL('image/png').split(',')[1];});
  stage='inline image upload';await imageCard.locator('input[type=file]').setInputFiles({name:'local-fixture.png',mimeType:'image/png',buffer:Buffer.from(encoded,'base64')});
  await page.waitForFunction(()=>document.querySelector('[name="hero.image1"]').value.startsWith('/media?id='));
  const selectedImage=await page.locator('[name="hero.image1"]').inputValue();assert.notEqual(selectedImage,originalImage);
  assert.equal((await context.request.get(root+selectedImage)).status(),404);
  stage='CMS publication';
  await Promise.all([page.waitForNavigation(),page.locator('form[action$="/admin/content"] button[type=submit]').click()]);
  assert.equal(await page.locator('[name="hero.title"]').inputValue(),marker);
  const visitor=await browser.newContext();await visitor.route('**/*',r=>new URL(r.request().url()).origin===origin?r.continue():r.abort());
  const publicPage=await visitor.newPage();await publicPage.goto(root+'/home');
  assert.match(await publicPage.locator('body').innerText(),/LOCAL ADMIN TEST/);
  assert.equal((await visitor.request.get(root+selectedImage)).status(),200);
  await page.locator('.cms-history').evaluate(el=>el.open=true);
  const rollback=page.locator('form').filter({has:page.locator('input[name=action][value=rollback]')}).first();
  await Promise.all([page.waitForNavigation(),rollback.locator('button[type=submit]').click()]);
  assert.equal(await page.locator('[name="hero.title"]').inputValue(),original);
  assert.equal(await page.locator('[name="hero.image1"]').inputValue(),originalImage);
  assert.equal((await visitor.request.get(root+selectedImage)).status(),404);
  await publicPage.reload();assert.ok(!(await publicPage.locator('body').innerText()).includes(marker));
  fs.mkdirSync('.local/qa',{recursive:true});
  await page.setViewportSize({width:1440,height:1000});await page.goto(root+'/admin/content');await page.locator('.cms-sidebar a[href="#cms-images"]').click();await page.screenshot({path:'.local/qa/content-images.png'});
  await page.goto(root+'/admin/business');await page.screenshot({path:'.local/qa/business.png'});
  await visitor.close();await context.close();
  console.log('Local admin: login, 7 admin pages, persisted CMS text and inline image upload, public rendering and rollback passed.');
 } finally {await browser.close();}
})().catch(()=>{console.error('Local admin integration test failed at '+stage+'; inspect local Tomcat logs.');process.exitCode=1;});


