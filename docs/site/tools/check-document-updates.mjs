import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 let updated=false,serveOld=false;const context=await browser.newContext({viewport:{width:1440,height:1000}}),errors=[];
 const key='/실행_및_테스트';
 const handler=async route=>{let file=path.resolve('docs/site/public','.'+decodeURI(new URL(route.request().url()).pathname));if(!fs.existsSync(file)&&!path.extname(file))file+='.html';if(!fs.existsSync(file))return route.fulfill({status:404,body:''});if(fs.statSync(file).isDirectory())file=path.join(file,'index.html');let body=fs.readFileSync(file);if(file.endsWith('.html')&&updated&&!serveOld){body=body.toString().replace(/(<script type="application\/json" data-document-revisions>)([\s\S]*?)(<\/script>)/,(_,start,json,end)=>{const data=JSON.parse(json);data[key]={...data[key],revision:data[key].revision+1,version:'updated-test-version',updated:true};return start+JSON.stringify(data)+end;});}return route.fulfill({body,contentType:({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});};
 await context.route('https://updates.test/**',handler);
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto('https://updates.test/');await page.waitForLoadState('networkidle');
 const link=p=>p.locator('article').getByRole('link',{name:'처음 실행하기와 테스트',exact:true});
 await link(page).locator('.document-unread-dot').waitFor();await link(page).hover();await page.waitForTimeout(300);assert.equal(await link(page).locator('.document-unread-dot').count(),1);
 await link(page).click();await page.getByRole('heading',{name:'처음 실행하기와 테스트',exact:true}).waitFor();await page.goto('https://updates.test/');await page.waitForLoadState('networkidle');assert.equal(await link(page).locator('.document-unread-dot').count(),0);
 // Same content / a normal rebuild must not make a read document unread again.
 await page.reload();await page.waitForLoadState('networkidle');assert.equal(await link(page).locator('.document-unread-dot').count(),0);
 const other=await context.newPage();await other.goto('https://updates.test/');await other.waitForLoadState('networkidle');assert.equal(await link(other).locator('.document-unread-dot').count(),0);
 updated=true;await page.reload();await page.waitForLoadState('networkidle');await link(page).locator('.document-unread-dot').waitFor();await other.reload();await other.waitForLoadState('networkidle');await link(other).locator('.document-unread-dot').waitFor();
 await page.bringToFront();await link(page).click();await page.getByRole('heading',{name:'처음 실행하기와 테스트',exact:true}).waitFor();await link(other).locator('.document-unread-dot').waitFor({state:'detached'});
 // An older tab/document must not overwrite the read revision of a newer one.
 serveOld=true;await page.goto('https://updates.test/실행_및_테스트.html');await page.waitForLoadState('networkidle');serveOld=false;await page.goto('https://updates.test/');await page.waitForLoadState('networkidle');assert.equal(await link(page).locator('.document-unread-dot').count(),0);
 const separate=await browser.newContext();await separate.route('https://updates.test/**',handler);const fresh=await separate.newPage();await fresh.goto('https://updates.test/');await fresh.waitForLoadState('networkidle');await link(fresh).locator('.document-unread-dot').waitFor();await fresh.screenshot({path:'build/ui-preview/document-update-dots.png'});assert.deepEqual(errors,[]);await separate.close();await context.close();console.log('Document dots passed: recent updates, hover is not read, read/reload persistence, new version, tab synchronization, stale tabs and separate browsers.');
}finally{await browser.close();}
