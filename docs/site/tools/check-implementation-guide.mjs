import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});
try {
 const page=await browser.newPage({viewport:{width:1440,height:1080}}),errors=[],writes=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://guide.test/**',async route=>{const req=route.request();if(req.method()!=='GET')writes.push(req.url());let file=path.resolve('docs/site/public','.'+decodeURI(new URL(req.url()).pathname));if(fs.existsSync(file)&&fs.statSync(file).isDirectory())file=path.join(file,'index.html');if(!fs.existsSync(file)&&!path.extname(file))file+='.html';if(!fs.existsSync(file))return route.fulfill({status:404,body:''});return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.js':'text/javascript','.mjs':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});});
 await page.goto('https://guide.test/');await page.getByRole('heading',{name:'기본 조건 설정·코스 편집',exact:true}).waitFor();
 await page.screenshot({path:'build/ui-preview/guide-desktop.png',fullPage:true});
 const data=await page.locator('#guide-data').textContent();const features=JSON.parse(data).features;
 for(const f of features){await page.locator('#feature-menu [data-feature="'+f.id+'"]').click();await page.getByRole('heading',{name:f.title,exact:true}).waitFor();assert.equal(await page.locator('#feature-flow li').count(),4);assert.ok(await page.locator('#feature-tech .tech-card').count()>=3);assert.equal(await page.locator('#detail-link').getAttribute('href'),f.documentUrl);}
 await page.locator('#journey [data-feature="03-recommendation"]').click();await page.locator('#example-section').evaluate(e=>e.open=true);
 for(const [stage,n] of [8,7,5,4,3].entries()){await page.locator('[data-stage="'+stage+'"]').click();assert.equal(await page.locator('.candidate:not(.removed)').count(),n);}
 await page.locator('#code-shortcut').click();assert.equal(await page.locator('#code-section').getAttribute('open'),'');assert.ok((await page.locator('#feature-code').innerText()).includes('NextPlaceService'));
 await page.goBack();assert.equal(await page.locator('#feature-title').innerText(),features.at(-1).title);
 await page.goto('https://guide.test/#03-recommendation');await page.locator('#example-section').evaluate(e=>e.open=true);await page.locator('[data-stage="4"]').click();
 await page.screenshot({path:'build/ui-preview/guide-recommendation-desktop.png',fullPage:true});
 for(const width of [390,320]){await page.setViewportSize({width,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,width+'px overflow');for(const f of features){const menu=page.locator((f.group==='use'?'#journey':'#feature-menu')+' [data-feature="'+f.id+'"]');await menu.click();assert.equal(await page.locator('#feature-title').innerText(),f.title);}}
 await page.locator('#journey [data-feature="02-planning"]').click();await page.setViewportSize({width:390,height:844});await page.screenshot({path:'build/ui-preview/guide-mobile.png',fullPage:true});
 await page.goto('file:///'+path.resolve('build/ui-preview/implementation-guide.html').replaceAll('\\','/'));await page.getByRole('heading',{name:'기본 조건 설정·코스 편집',exact:true}).waitFor();await page.locator('#journey [data-feature="07-saved-courses"]').focus();await page.keyboard.press('Enter');assert.equal(await page.locator('#feature-title').innerText(),'코스 저장·목록 관리·이미지와 지도');assert.ok((await page.locator('#detail-link').getAttribute('href')).startsWith('https://datecourseapp-team-docs.ate-ourse-pp.workers.dev/'));
 assert.deepEqual(errors,[]);assert.deepEqual(writes,[]);console.log('Guide passed: 12 features, 4-step flows, filter example 8→3, source links, code disclosure, history, deep links, keyboard, 390/320px, standalone file; no writes.');
}finally{await browser.close();}
