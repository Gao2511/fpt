const {chromium,request}=require('playwright-core');
const assert=require('node:assert/strict');
const {spawn}=require('node:child_process');
const crypto=require('node:crypto');
const fs=require('node:fs');
const path=require('node:path');
(async()=>{
 const password=crypto.randomBytes(24).toString('base64url'),salt=crypto.randomBytes(16);
 const hash='pbkdf2$210000$'+salt.toString('base64')+'$'+crypto.pbkdf2Sync(password,salt,210000,32,'sha256').toString('base64');
 const child=spawn('java',['-Dfile.encoding=UTF-8','-cp',process.env.FPT_CMS_TEST_CLASSPATH,'cms.CmsBrowserServer'],{env:{...process.env,DEMO_ADMIN_ENABLED:'true',DEMO_ADMIN_IDENTIFIER:'demo-presenter',DEMO_ADMIN_PASSWORD_HASH:hash},stdio:['ignore','pipe','pipe']});
 let browser,anonymous;let log='';child.stderr.on('data',b=>{log+=b.toString();});
 try{
  const port=await new Promise((resolve,reject)=>{let out='';const timer=setTimeout(()=>reject(new Error('Test app startup timeout: '+log.slice(-2000))),30000);child.stdout.on('data',b=>{out+=b.toString();const m=out.match(/FPT_CMS_TEST_PORT=(\d+)/);if(m){clearTimeout(timer);resolve(m[1]);}});child.once('exit',code=>{clearTimeout(timer);reject(new Error('Test server exited '+code+': '+log.slice(-2000)));});});
  const base='http://127.0.0.1:'+port;anonymous=await request.newContext();
  for(const url of ['/admin/packages','/%61dmin/packages','/MainController?action=AdminPackages','/main?action=AdminPackages','/view/admin/package-form.jsp','/%76iew/admin/package-form.jsp'])assert.equal((await anonymous.get(base+url,{maxRedirects:0})).status(),302,url);
  console.log('PASS HTTP unauthenticated routes, aliases and direct JSP');
  browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
  const demo=await browser.newContext();await demo.route('**/*',r=>r.request().url().startsWith(base)?r.continue():r.abort());const page=await demo.newPage();
  await page.goto(base+'/login');await page.locator('[name=identifier]').fill('demo-presenter');await page.locator('[name=password]').fill(password);await Promise.all([page.waitForURL('**/admin/dashboard'),page.locator('#btnLoginPrimary').click()]);
  assert.match(await page.locator('[data-demo-admin]').innerText(),/KHÔNG THAY ĐỔI DỮ LIỆU THẬT/);console.log('PASS provisioned demo authenticates through existing login');
  const initial=await (await anonymous.get(base+'/fixture-stats')).json();
  for(const width of [1440,768,375]){
   await page.setViewportSize({width,height:1000});
   for(const url of ['/admin/dashboard','/admin/packages','/admin/packages?action=edit&id=3','/admin/customers','/admin/customer-detail?id=1','/admin/content','/admin/business']){
    const response=await page.goto(base+url);assert.equal(response.status(),200,url);assert.equal(await page.locator('[data-demo-admin]').count(),1);
    if(url==='/admin/content'||url==='/admin/business') {assert.equal(await page.locator('.cms-sections > details').count(),url.endsWith('business')?4:8);assert.ok(await page.locator('#cms-form button[type=submit]').isDisabled());for(const input of await page.locator('[data-image-upload]').all())assert.ok(await input.isDisabled());} 
    assert.ok(!(await page.locator('body').innerText()).includes('DO_NOT_EXPOSE'));
   }
   console.log('PASS demo pages at '+width+'px');
   const output=path.join(process.env.TEMP||process.cwd(),'fpt-cms-qa');fs.mkdirSync(output,{recursive:true});await page.screenshot({path:path.join(output,'demo-'+width+'.png')});
  }
  for(const url of ['/admin/packages','/admin/content','/admin/business','/admin/settings','/admin/users','/admin/media','/admin/ai-playground','/ContactServlet','/profile','/register','/reset-password','/ai-chat','/MainController?action=AdminPackages'])assert.equal((await demo.request.post(base+url,{form:{action:'update',role:'admin'}})).status(),403,url);
  assert.equal((await demo.request.get(base+'/admin/packages?action=delete&id=3')).status(),403);
  for(const url of ['/assets/%75ploads/avatars/private.png','/%61dmin/settings','/admin/%75sers','/media?id=00000000-0000-0000-0000-000000000000'])assert.equal((await demo.request.get(base+url)).status(),403,url);
  assert.deepEqual(await (await anonymous.get(base+'/fixture-stats')).json(),initial);console.log('PASS demo direct requests produce zero writes and zero private DAO reads');
  await demo.close();const admin=await browser.newContext();await admin.route('**/*',r=>r.request().url().startsWith(base)?r.continue():r.abort());const owner=await admin.newPage();await owner.goto(base+'/fixture-login');await owner.goto(base+'/admin/content');
  await owner.locator('[name="hero.title"]').fill('Tiêu đề kiểm thử');await Promise.all([owner.waitForURL('**/admin/content'),owner.getByRole('button',{name:'Lưu và xuất bản',exact:true}).click()]);await owner.reload();assert.equal(await owner.locator('[name="hero.title"]').inputValue(),'Tiêu đề kiểm thử');
  await owner.goto(base+'/home');assert.match(await owner.locator('.th-main').first().textContent(),/Tiêu đề kiểm thử/);console.log('PASS CMS title saves, survives refresh and renders publicly');
  const normalPrices=await owner.locator('.pkg-body .price').allTextContents();
  await owner.goto(base+'/admin/business');await owner.locator('[name="business.phone"]').fill('0912345678');await owner.locator('.cms-sidebar a[href="#cms-fees"]').click();await owner.locator('[name="business.standardFee"]').fill('350000');await Promise.all([owner.waitForURL('**/admin/business'),owner.getByRole('button',{name:'Lưu và xuất bản',exact:true}).click()]);
  for(const width of [1440,768,375]){await owner.setViewportSize({width,height:1000});await owner.goto(base+'/home');assert.equal(await owner.locator('.pkg-body .standard-installation-fee').count(),6);for(const fee of await owner.locator('.pkg-body .standard-installation-fee').all()){assert.match(await fee.innerText(),/350\.000đ\/lần/);assert.ok(await fee.evaluate(el=>el.scrollWidth<=el.clientWidth+1&&el.getBoundingClientRect().bottom<=el.parentElement.querySelector('.btn-pkg').getBoundingClientRect().top));}assert.deepEqual(await owner.locator('.pkg-body .price').allTextContents(),normalPrices);assert.equal(await owner.locator('.hotline-box').getAttribute('href'),'tel:0912345678');assert.ok(await owner.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Homepage overflow '+width);}
  console.log('PASS shared contact/fee and unchanged monthly prices at 1440/768/375px');
  await owner.goto(base+'/admin/settings');const previousPrompt=await owner.locator('#ai_system_prompt').inputValue();const customPrompt='Tư vấn tự nhiên bằng tiếng Việt.\n</textarea><script>window.cmsPromptExecuted=true</script>';await owner.locator('#ai_system_prompt').fill(customPrompt);const settingsForm=await owner.locator('#settingsForm').evaluate(el=>Object.fromEntries(new FormData(el)));assert.equal((await admin.request.post(base+'/admin/settings',{form:{...settingsForm,ai_active_model:'fixture-model',ai_system_prompt:customPrompt},maxRedirects:0})).status(),302);await owner.goto(base+'/admin/settings');assert.equal(await owner.locator('#ai_system_prompt').inputValue(),customPrompt);assert.equal(await owner.evaluate(()=>window.cmsPromptExecuted),undefined);console.log('PASS real prompt editor saves multiline text unchanged without executing markup');
  const promptRevision=await owner.locator('[name=revisionId]').first().inputValue();const promptToken=await owner.locator('meta[name=admin-csrf]').getAttribute('content');assert.equal((await admin.request.post(base+'/admin/settings',{form:{action:'rollbackPrompt',revisionId:promptRevision,csrf_token:promptToken},maxRedirects:0})).status(),302);await owner.goto(base+'/admin/settings');assert.equal(await owner.locator('#ai_system_prompt').inputValue(),previousPrompt);console.log('PASS prompt rollback restores previous effective prompt through existing settings API');
  assert.equal((await admin.request.post(base+'/admin/content',{form:{'hero.title':'unauthorized token'}})).status(),403);console.log('PASS real admin writes require CSRF');
  await owner.goto(base+'/admin/content');const token=await owner.locator('meta[name=admin-csrf]').getAttribute('content');const png=await (await anonymous.get(base+'/fixture-image')).body();
  const uploaded=await admin.request.post(base+'/admin/media',{headers:{'X-CSRF-Token':token},multipart:{image:{name:'fixture.png',mimeType:'image/png',buffer:png}}});assert.equal(uploaded.status(),200);const mediaUrl=(await uploaded.text()).match(/\/media\?id=[a-f0-9-]{36}/)[0];assert.equal((await anonymous.get(base+mediaUrl)).status(),404);
  await owner.locator('.cms-sidebar a[href="#cms-images"]').click();
  const imageCard=owner.locator('[data-image-field]').filter({has:owner.locator('[name="hero.image1"]')});
  assert.equal(await imageCard.locator('input[type=text]').count(),0);
  await imageCard.locator('input[type=file]').setInputFiles({name:'banner.png',mimeType:'image/png',buffer:png});
  await owner.waitForFunction(()=>document.querySelector('[name="hero.image1"]').value.startsWith('/media?id='));
  const chosenUrl=await owner.locator('[name="hero.image1"]').inputValue();assert.equal((await anonymous.get(base+chosenUrl)).status(),404);
  assert.match(await imageCard.locator('img').getAttribute('src'),/^blob:/);
  await imageCard.locator('input[type=file]').setInputFiles({name:'bad.png',mimeType:'image/png',buffer:Buffer.from('not image')});
  await owner.getByText('Tải ảnh chưa thành công.',{exact:false}).waitFor();assert.equal(await owner.locator('[name="hero.image1"]').inputValue(),chosenUrl);
  console.log('PASS inline upload previews draft and failed upload preserves previous selection');
  await Promise.all([owner.waitForURL('**/admin/content'),owner.getByRole('button',{name:'Lưu và xuất bản',exact:true}).click()]);const publicImage=await anonymous.get(base+chosenUrl);assert.equal(publicImage.status(),200);assert.equal(publicImage.headers()['content-type'],'image/png');assert.equal(publicImage.headers()['x-content-type-options'],'nosniff');assert.ok((await publicImage.body()).length>0);assert.equal((await admin.request.post(base+'/admin/media',{headers:{'X-CSRF-Token':token},multipart:{image:{name:'spoof.png',mimeType:'image/png',buffer:Buffer.from('<script>spoof</script>')}}})).status(),400);console.log('PASS media upload persists, draft stays private, publish enables image and spoof is rejected');
  await owner.goto(base+'/admin/packages?action=edit&id=3');const price=await owner.locator('[name=price]').inputValue();const form=await owner.locator('[name=price]').evaluate(el=>Object.fromEntries(new FormData(el.form)));await admin.request.post(base+'/admin/packages',{form:{...form,price:'-1'}});await owner.goto(base+'/admin/packages?action=edit&id=3');assert.equal(await owner.locator('[name=price]').inputValue(),price);console.log('PASS package editor rejects negative price without altering stored package');
  for(const width of [1440,1024,768,375]){await owner.setViewportSize({width,height:1000});await owner.goto(base+'/admin/content');assert.ok(await owner.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Content editor overflow '+width);await owner.screenshot({path:path.join(process.env.TEMP||process.cwd(),'fpt-cms-qa','admin-'+width+'.png')});}
  await owner.goto(base+'/admin/business');assert.equal(await owner.locator('.header-nav a.active').innerText(),'Thông tin kinh doanh');assert.equal(await owner.locator('.cms-sections > details').count(),4);await owner.setViewportSize({width:1440,height:1000});await owner.screenshot({path:path.join(process.env.TEMP||process.cwd(),'fpt-cms-qa','business-1440.png')});
  for(const kbTab of ['dashboard','documents','upload','faqs','indexing','playground','rag-config','audit']){
   const kbRes=await owner.goto(base+'/admin/knowledge-base?tab='+kbTab);assert.equal(kbRes.status(),200,'KB tab '+kbTab);
   assert.ok(await owner.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'KB overflow '+kbTab);
  }
  for(const width of [1440,1024,768,375]){await owner.setViewportSize({width,height:1000});await owner.goto(base+'/admin/knowledge-base?tab=dashboard');assert.ok(await owner.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'KB dashboard overflow '+width);}
  console.log('PASS grouped content/business editors, 5-group sidebar, KB tabs and responsive layout at 1440/1024/768/375px');
  console.log('QA screenshots: '+path.join(process.env.TEMP||process.cwd(),'fpt-cms-qa'));
  await admin.close();
 }catch(error){console.error('Test server diagnostics: '+(log.match(/.*(?:Exception|Error|SEVERE|Caused by).*/g)||[]).slice(-12).join('\n'));throw error;}finally{if(browser)await browser.close();if(anonymous)await anonymous.dispose();child.kill();}
})().catch(e=>{console.error(e);process.exitCode=1;});
