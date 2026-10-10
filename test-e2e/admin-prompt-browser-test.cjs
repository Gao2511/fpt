'use strict';
const {chromium}=require('playwright-core');
const fs=require('node:fs');
const assert=require('node:assert/strict');
(async()=>{
 const source=fs.readFileSync('web/view/admin/settings.jsp','utf8');
 const begin=source.indexOf('<textarea name="ai_system_prompt"');
 const end=source.indexOf('<div class="prompt-footer-stats">',begin);
 const editor=source.slice(begin,end).replace(/<c:out[^>]*\/>/g,'');
 const tools=source.slice(source.indexOf('function loadFptPromptTemplate()'),source.indexOf('function resetForm()'));
 const browser=await chromium.launch({headless:true,executablePath:process.env.FPT_TEST_BROWSER_BIN||'C:/Program Files/Google/Chrome/Application/chrome.exe'});
 try{
  const page=await browser.newPage();
  await page.setContent('<form>'+editor+'</form><span id="promptCharCount"></span><span id="promptWordCount"></span>');
  await page.addScriptTag({content:tools});
  const custom='Xưng tôi, gọi khách là bạn.\n</textarea><script>unexpected()</script>';
  await page.locator('#ai_system_prompt').fill(custom);
  assert.equal(await page.evaluate(()=>new FormData(document.querySelector('form')).get('ai_system_prompt')),custom);
  assert.equal(await page.locator('#promptWordCount').textContent(),custom.trim().split(/\s+/).length+' từ');
  console.log('PASS admin editor accepts multiline custom text and submits it unchanged');
  await page.evaluate(()=>{document.getElementById('fptPromptTemplate').value='Mẫu FPT mới.\nTư vấn tiếng Việt.';loadFptPromptTemplate();});
  assert.equal(await page.locator('#ai_system_prompt').inputValue(),'Mẫu FPT mới.\nTư vấn tiếng Việt.');
  console.log('PASS template button replaces editor content');
  page.on('dialog',dialog=>dialog.accept());
  await page.evaluate(()=>clearPrompt());
  assert.equal(await page.locator('#ai_system_prompt').inputValue(),'');
  assert.equal(await page.locator('#promptWordCount').textContent(),'0 từ');
  console.log('PASS clear prompt remains editable and submits empty value');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
