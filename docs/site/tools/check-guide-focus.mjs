import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});
try {
 const page=await browser.newPage({viewport:{width:1440,height:1080}}),errors=[],writes=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://guide.test/**',async route=>{const req=route.request();if(req.method()!=='GET')writes.push(req.url());let file=path.resolve('docs/site/public','.'+decodeURI(new URL(req.url()).pathname));if(fs.existsSync(file)&&fs.statSync(file).isDirectory())file=path.join(file,'index.html');if(!fs.existsSync(file)&&!path.extname(file))file+='.html';if(!fs.existsSync(file))return route.fulfill({status:404,body:''});return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.js':'text/javascript','.mjs':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});});

 await page.goto('https://guide.test/');await page.locator('#feature-title').waitFor();
 assert.equal(await page.locator('#example-section').getAttribute('open'),null);
 assert.equal(await page.locator('#example-section summary').innerText(),'조건·처리 예시');
 assert.doesNotMatch(await page.locator('body').innerText(),/이렇게 이해|어떤 순서로|무엇을, 왜/);
 await page.setViewportSize({width:1440,height:900});
 await page.evaluate(()=>{
  window.guideCalls={focus:0,scroll:0};const focus=HTMLElement.prototype.focus,scroll=Element.prototype.scrollIntoView;
  HTMLElement.prototype.focus=function(...args){window.guideCalls.focus++;return focus.apply(this,args);};
  Element.prototype.scrollIntoView=function(...args){window.guideCalls.scroll++;return scroll.apply(this,args);};
 });
 for(const [selector,id] of [['#journey','03-recommendation'],['#journey','07-saved-courses'],['#feature-menu','01-account']]){
  const button=page.locator(selector+' [data-feature="'+id+'"]');await button.focus();
  const before=await page.evaluate(()=>{scrollTo({top:40,behavior:"instant"});window.guideCalls={focus:0,scroll:0};return scrollY;});
  await page.keyboard.press('Enter');await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
  assert.equal(await button.evaluate(e=>e===document.activeElement),true,id+' active button retained');
  assert.deepEqual(await page.evaluate(()=>window.guideCalls),{focus:0,scroll:0},id+' no scripted focus/scroll');
  assert.ok(Math.abs(await page.evaluate(()=>scrollY)-before)<=1,id+' scroll position retained');
 }
 await page.locator('#code-section').evaluate(e=>e.open=true);await page.locator('.limits').evaluate(e=>e.open=true);await page.locator('#example-section').evaluate(e=>e.open=true);
 await page.locator('#journey [data-feature="03-recommendation"]').click();
 assert.equal(await page.locator('#code-section').getAttribute('open'),'');assert.equal(await page.locator('.limits').getAttribute('open'),'');assert.equal(await page.locator('#example-section').getAttribute('open'),'');
 const stage=page.locator('[data-stage="4"]');await stage.focus();await page.evaluate(()=>{window.originalStage=document.activeElement;window.guideCalls={focus:0,scroll:0};});
 await page.keyboard.press('Enter');assert.equal(await page.evaluate(()=>window.originalStage===document.activeElement),true);assert.deepEqual(await page.evaluate(()=>window.guideCalls),{focus:0,scroll:0});assert.equal(await page.locator('.candidate:not(.removed)').count(),3);
 await page.locator('#journey [data-feature="03-recommendation"]').click();assert.equal(await page.locator('.candidate:not(.removed)').count(),3,'reselecting active tab preserves state');
 const next=page.locator('#next-feature');await next.focus();await page.evaluate(()=>window.guideCalls={focus:0,scroll:0});await page.keyboard.press('Enter');assert.equal(await next.evaluate(e=>e===document.activeElement),true);assert.deepEqual(await page.evaluate(()=>window.guideCalls),{focus:0,scroll:0});
 assert.deepEqual(errors,[]);assert.deepEqual(writes,[]);console.log('Guide focus passed: tab/menu/next focus retained, no scripted scroll, stable filter buttons, disclosure state retained, compact labels.');
}finally{await browser.close();}
