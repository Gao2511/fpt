'use strict';
const {chromium}=require('playwright-core');
const {spawn}=require('node:child_process');
const path=require('node:path');
const fs=require('node:fs');
const assert=require('node:assert/strict');
(async()=>{
 const child=spawn('java',['-Dfile.encoding=UTF-8','-cp',process.env.KB57_TEST_CLASSPATH,'cms.KnowledgeViewServer',process.env.KB57_WEB,process.env.KB57_EVIDENCE],{env:{...process.env,KB_GEMINI_WORKER_ENABLED:'false',RAG_ENABLED:'false',RAG_EMBEDDING_CALLS_APPROVED:'false',DB_HOST:'127.0.0.1',DB_PORT:'1',DB_NAME:'offline_fixture',DB_USER:'FAKE_TEST',DB_PASSWORD:'FAKE_TEST'},stdio:['ignore','pipe','pipe']});
 let browser,errors='';child.stderr.on('data',b=>{errors+=b.toString();});
 try {
  const port=await new Promise((resolve,reject)=>{let out='';const timeout=setTimeout(()=>reject(new Error('Fixture start timeout: '+errors.slice(-1200))),30000);child.stdout.on('data',b=>{out+=b.toString();let m=out.match(/KB57_TEST_PORT=(\d+)/);if(m){clearTimeout(timeout);resolve(m[1]);}});child.on('exit',()=>{clearTimeout(timeout);reject(new Error('Fixture exited: '+errors.slice(-1200)));});});
  browser=await chromium.launch({headless:true,executablePath:process.env.KB57_CHROME});
  const page=await browser.newPage();const base='http://127.0.0.1:'+port;await page.route('**/*',r=>r.request().url().startsWith(base)?r.continue():r.abort());
  for(const width of [1440,375])for(const tab of ['upload','documents','verify','advanced']){
   await page.setViewportSize({width,height:980});const response=await page.goto(base+'/admin/knowledge-base?tab='+tab);assert.equal(response.status(),200,tab);
   fs.writeFileSync(path.join(process.env.KB57_EVIDENCE,tab+'-'+width+'.html'),await page.content());
   assert.equal(await page.locator('.kb-primary-nav > a').count(),4);
   assert.equal(await page.locator('.kb-primary-nav > a.active').count(),1);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'horizontal overflow '+width+' '+tab);
   assert.equal(await page.locator('script').filter({hasText:'danger'}).count(),0,'stored XSS');
   for(const form of await page.locator('form[method="post"]').all())assert.equal(await form.locator('[name="csrf_token"]').count(),1,'CSRF field');
   if(tab==='verify')assert.match(await page.locator('main').textContent(),/0 API calls/);
   await page.screenshot({path:path.join(process.env.KB57_EVIDENCE,tab+'-'+width+'.png'),fullPage:true});console.log('PASS KB57 browser '+tab+' '+width);
  }
  await page.goto(base+'/admin/knowledge-base?tab=documents&large=true');
  assert.equal(await page.locator('textarea[name="content"]').isDisabled(),true);
  const editor=page.locator('form').filter({has:page.locator('textarea[name="content"]')});
  assert.equal(await editor.locator('[name="preserveUploadedContent"]').inputValue(),'true');
  assert.equal(await editor.locator('[name="version"]').count(),1);
  console.log('PASS KB57 browser large-body metadata edit avoids oversized form submission');
  await page.setViewportSize({width:1440,height:980});
  await page.goto(base+'/admin/knowledge-base?tab=upload');
  await page.locator('#sidebarAiToggleBtn').click();
  assert.equal(await page.locator('#adminAiSettingsLink').isVisible(),true,'API key link remains visible when AI submenu is closed');
  await page.locator('#adminAiSettingsLink').click();
  assert.equal(new URL(page.url()).pathname,'/admin/settings');
  assert.equal(new URL(page.url()).hash,'#api-key');
  assert.equal(await page.locator('#adminAiSettingsLink').getAttribute('aria-current'),'page');
  assert.equal(await page.locator('#providerApiKeyInput').isVisible(),true);
  assert.equal(await page.locator('#providerApiKeyInput').getAttribute('type'),'password');
  assert.equal(await page.locator('#providerApiKeyInput').inputValue(),'********');
  await page.screenshot({path:path.join(process.env.KB57_EVIDENCE,'api-key-settings.png'),fullPage:true});
  console.log('PASS KB57 browser visible API key navigation opens masked key form');
  await page.goto(base+'/admin/knowledge-base?tab=documents&fileManaged=true');
  assert.equal(await page.locator('textarea[name="content"]').isDisabled(),true);
  assert.equal(await page.locator('form:has(input[name="action"][value="simplePublish"])').count(),0);
  assert.equal(await page.locator('form:has(input[name="action"][value="rollbackDocument"])').count(),0);
  assert.equal(await page.locator('form:has(input[name="action"][value="permanentDeleteDocument"])').count(),0);
  assert.equal(await page.locator('form:has(input[name="action"][value="archiveDocument"])').count()>0,true);
  const sync=page.locator('form:has(input[name="action"][value="syncRepositoryFiles"])');
  assert.equal(await sync.locator('[name="csrf_token"]').count(),1);
  await page.screenshot({path:path.join(process.env.KB57_EVIDENCE,'file-managed-read-only.png'),fullPage:true});
  console.log('PASS FILE-KB browser file-managed read-only controls and authenticated sync form');
  console.log('KB browser: 11/11 checks PASS; real API requests=0');
 }finally{if(browser)await browser.close();child.kill();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
