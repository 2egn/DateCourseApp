import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
import {compactDocument} from './compact-docs.mjs';
const quartz=process.env.DOCS_QUARTZ_DIR||path.resolve('build/docs-tools/quartz');
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
// Build-time invariants: keep anchors, text, table/code markup, and do not nest twice.
const fixture='<html><head></head><body><article><p>짧은 요약.</p><h2 id="functions">주요 함수</h2><p>함수 설명.</p><h3 id="deep">세부 설명</h3><pre><code>danger &lt;script&gt;</code></pre><table><tr><td>정상</td></tr></table></article></body></html>';
const transformed=compactDocument(fixture,quartz);assert.equal(compactDocument(transformed,quartz),transformed);assert.match(transformed,/id="functions"/);assert.match(transformed,/id="deep"/);assert.match(transformed,/danger &lt;script&gt;/);assert.match(transformed,/<table>/);
const browser=await chromium.launch({channel:'chrome',headless:true});
try {
 const page=await browser.newPage({viewport:{width:1440,height:1000}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://compact.test/**',async route=>{
  let file=path.resolve('docs/site/public','.'+decodeURI(new URL(route.request().url()).pathname));if(!fs.existsSync(file)&&!path.extname(file))file+='.html';if(!fs.existsSync(file))return route.fulfill({status:404,body:''});if(fs.statSync(file).isDirectory())file=path.join(file,'index.html');
  return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});
 });
 await page.goto('https://compact.test/변경_요약.html');await page.waitForLoadState('networkidle');
 assert.equal(await page.locator('article details[open]').count(),0);assert.ok((await page.locator('.doc-overview').innerText()).length<170);
 const height=await page.locator('article').evaluate(e=>e.getBoundingClientRect().height);
 await page.locator('article .doc-section').first().locator(':scope>summary').click();
 assert.equal(await page.locator('article .doc-section').first().getAttribute('open'),'');
 const file=page.locator('article details.doc-item').filter({has:page.locator('summary').filter({hasText:'RouteEditorController'})}).first();
 await file.locator(':scope>summary').click();assert.equal(await file.getAttribute('open'),'');
 await file.locator('details>summary').first().click();assert.ok((await file.innerText()).includes('기존 구간 반경'));
 await page.screenshot({path:'build/ui-preview/compact-docs-desktop.png'});
 await page.getByRole('button',{name:'모두 펼치기',exact:true}).click();const fullHeight=await page.locator('article').evaluate(e=>e.getBoundingClientRect().height);assert.ok(fullHeight>height*2);
 await page.getByRole('button',{name:'모두 접기',exact:true}).click();assert.equal(await page.locator('article details[open]').count(),0);
 // Native keyboard interaction must open a collapsed section in one attempt.
 const first=page.locator('article .doc-section>summary').first();await first.focus();await page.keyboard.press('Enter');assert.equal(await page.locator('article .doc-section').first().getAttribute('open'),'');
 const id=await page.locator('article .doc-section h2').nth(1).getAttribute('id');await page.goto('https://compact.test/변경_요약.html#'+encodeURIComponent(id));
 await page.waitForFunction(()=>{const n=document.getElementById(decodeURIComponent(location.hash.slice(1)));return !!n?.closest('details')?.open;});
 await page.setViewportSize({width:390,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await page.screenshot({path:'build/ui-preview/compact-docs-mobile.png'});
 // Navigation starts compact again; deep links continue to reveal their path.
 await page.goto('https://compact.test/reference.html');await page.waitForLoadState('networkidle');await page.locator('.doc-quick-links').getByRole('link',{name:'파일·함수별 변경',exact:true}).click();
 await page.getByRole('heading',{name:'변경 파일·함수 요약',exact:true}).waitFor();assert.equal(await page.locator('article details[open]').count(),0);
 await page.evaluate(()=>window.dispatchEvent(new Event('beforeprint')));assert.equal(await page.locator('article details:not([open])').count(),0);await page.evaluate(()=>window.dispatchEvent(new Event('afterprint')));assert.equal(await page.locator('article details[open]').count(),0);
 assert.deepEqual(errors,[]);console.log(`Compact docs passed: hierarchy, one-click/keyboard, all/none, hashes, SPA, print, mobile. Initial height ${Math.round(height)}px vs ${Math.round(fullHeight)}px expanded.`);
}finally{await browser.close();}
