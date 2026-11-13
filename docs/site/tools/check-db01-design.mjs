import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 const page=await browser.newPage({viewport:{width:1440,height:1000}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://design.test/**',async route=>{
  let file=path.resolve('docs/site/public','.'+decodeURI(new URL(route.request().url()).pathname));
  if(fs.existsSync(file)&&fs.statSync(file).isDirectory())file=path.join(file,'index.html');
  if(!fs.existsSync(file))return route.fulfill({status:404,body:''});
  return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.css':'text/css','.js':'text/javascript','.json':'application/json','.sql':'text/plain; charset=utf-8'})[path.extname(file)]||'application/octet-stream'});
 });
 await page.route('https://cdnjs.cloudflare.com/ajax/libs/mermaid/11.4.0/**',route=>route.fulfill({body:fs.readFileSync(path.resolve('build/docs-tools/quartz/node_modules/mermaid/dist',new URL(route.request().url()).pathname.split('/11.4.0/')[1])),contentType:'text/javascript'}));
 await page.goto('https://design.test/코스_저장_설계.html');await page.locator('code.mermaid svg').waitFor();
 assert.match(await page.locator('article').innerText(),/46,000/);
 const link=page.locator('article a[download]');assert.equal(new URL(await link.getAttribute('href'),page.url()).pathname,'/downloads/07_saved_courses.sql');
 const sql=await page.evaluate(async()=>{const r=await fetch('/downloads/07_saved_courses.sql');if(!r.ok)throw Error('SQL download failed');return r.text();});assert.equal(sql,fs.readFileSync('data/sql/07_saved_courses.sql','utf8'));
 fs.mkdirSync('build/ui-preview',{recursive:true});await page.screenshot({path:'build/ui-preview/db01-design-desktop.png'});
 await page.setViewportSize({width:390,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
 await page.goto('https://design.test/07_테스트/savedcourseschematest.html');const method=page.locator('code.mermaid g.flow-method-hint').filter({hasText:'createSchema()'});await method.waitFor();await method.scrollIntoViewIfNeeded();await method.focus();await page.locator('#flow-function-tooltip').waitFor({state:'visible'});assert.match(await page.locator('#flow-function-tooltip').innerText(),/메모리 DB/);
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);assert.deepEqual(errors,[]);
 await page.goto('https://design.test/05_회원_db/savedcourserepository.html');const persist=page.locator('code.mermaid g.flow-method-hint').filter({hasText:'persist()'});await persist.waitFor();await persist.scrollIntoViewIfNeeded();await persist.focus();await page.locator('#flow-function-tooltip').waitFor({state:'visible'});assert.match(await page.locator('#flow-function-tooltip').innerText(),/트랜잭션/);assert.match(await page.locator('article').innerText(),/오버로딩/);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
 await page.goto('https://design.test/07_테스트/savedcourserepositorytest.html');await page.locator('code.mermaid svg').waitFor();assert.match(await page.locator('article').innerText(),/되돌리는/);assert.deepEqual(errors,[]);
 console.log('DB-01/02 documentation passed: schema/save/test diagrams, exact SQL download, method tooltips and mobile layout.');
}finally{await browser.close();}
