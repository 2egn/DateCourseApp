import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 const context=await browser.newContext({viewport:{width:1440,height:1000}}),errors=[],writes=[];
 await context.route('https://learning.test/**',async route=>{
  const request=route.request(),url=new URL(request.url());if(request.method()!=='GET')writes.push(url.pathname);
  let file=path.resolve('docs/site/public','.'+decodeURI(url.pathname));if(!fs.existsSync(file)&&!path.extname(file))file+='.html';if(fs.existsSync(file)&&fs.statSync(file).isDirectory())file=path.join(file,'index.html');
  if(!fs.existsSync(file))return route.fulfill({status:404,body:''});
  return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});
 });
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 await context.route('https://cdnjs.cloudflare.com/ajax/libs/mermaid/11.4.0/**',async route=>{const suffix=new URL(route.request().url()).pathname.split('/11.4.0/')[1];return route.fulfill({body:fs.readFileSync(path.resolve('build/docs-tools/quartz/node_modules/mermaid/dist',suffix)),contentType:'text/javascript'});});
 const visit=async url=>{await page.goto('https://learning.test'+url);await page.locator('.learning-demo .learning-button, .learning-demo input').first().waitFor();};
 await visit('/04_데이터/routeplan.html');const draft=page.locator('[data-learning=draft]'),cost=page.locator('[data-learning=cost]');
 await draft.getByRole('button',{name:'편집 열기',exact:true}).click();await draft.getByRole('button',{name:'편집본에 공원 추가',exact:true}).click();assert.doesNotMatch(await draft.locator('.learning-lane').first().textContent(),/공원/);assert.match(await draft.locator('.learning-lane').nth(1).textContent(),/공원/);
 await draft.getByRole('button',{name:'편집 취소',exact:true}).click();assert.doesNotMatch(await draft.locator('.learning-lane').first().textContent(),/공원/);
 await draft.getByRole('button',{name:'편집 열기',exact:true}).click();await draft.getByRole('button',{name:'편집본에 공원 추가',exact:true}).click();await draft.getByRole('button',{name:'편집 완료',exact:true}).click();assert.match(await draft.locator('.learning-lane').first().textContent(),/공원/);assert.match(await draft.locator('.learning-status').textContent(),/DB에 영구 저장한 것은 아니/);
 assert.match(await cost.locator('.learning-cost-total').textContent(),/36,000/);await cost.getByRole('spinbutton').fill('3');assert.match(await cost.locator('.learning-cost-total').textContent(),/54,000/);
 await cost.getByRole('spinbutton').fill('0');assert.match(await cost.locator('.learning-status').textContent(),/1~6/);await cost.getByRole('spinbutton').fill('2');
 await draft.scrollIntoViewIfNeeded();fs.mkdirSync('build/ui-preview',{recursive:true});await page.screenshot({path:'build/ui-preview/readable-draft-and-cost.png'});
 await visit('/03_처리/nextplaceservice.html');let ranges=page.locator('[data-learning=ranges]');assert.match(await ranges.locator('.learning-status').textContent(),/A, D/);
 await ranges.getByRole('slider').fill('150');assert.match(await ranges.locator('.learning-status').textContent(),/통과: A\./);assert.equal(await ranges.locator('li.eligible').count(),1);
 await ranges.getByRole('slider').focus();await page.keyboard.press('ArrowRight');assert.equal(await ranges.getByRole('slider').inputValue(),'200');
 await ranges.getByRole('slider').fill('400');await ranges.scrollIntoViewIfNeeded();await page.screenshot({path:'build/ui-preview/readable-distance-intersection.png'});
 const article=page.locator('article');await article.getByRole('link',{name:'PlaceSearchService',exact:true}).last().click();await page.getByRole('heading',{name:'PlaceSearchService',exact:true}).waitFor();ranges=page.locator('[data-learning=ranges]');await ranges.getByRole('slider').waitFor({timeout:5000}).catch(async e=>{console.log(JSON.stringify({errors,widgets:await page.locator('[data-learning]').evaluateAll(nodes=>nodes.map(n=>({type:n.dataset.learning,html:n.innerHTML.slice(0,500),connected:n.isConnected}))),url:new URL(page.url()).pathname}));throw e;});assert.equal(await ranges.locator('svg').count(),1);await ranges.getByRole('slider').fill('150');assert.equal(await ranges.locator('li.eligible').count(),1);
 await visit('/02_화면/plannermap.html');const asyncDemo=page.locator('[data-learning=async]');for(let i=0;i<3;i++)await asyncDemo.getByRole('button',{name:'다음 단계',exact:true}).click();assert.match(await asyncDemo.locator('.learning-requests').textContent(),/늦게 도착 → 무시/);assert.match(await asyncDemo.locator('.learning-requests').textContent(),/카페 상세정보/);await asyncDemo.scrollIntoViewIfNeeded();await page.screenshot({path:'build/ui-preview/readable-stale-response.png'});
 await visit('/05_회원_db/replaceintegrateddata.html');const transaction=page.locator('[data-learning=transaction]');await transaction.getByRole('button',{name:'중간에 실패한 경우',exact:true}).click();assert.match(await transaction.locator('.learning-saved-result').textContent(),/교체 전 자료 그대로/);await transaction.getByRole('button',{name:'모두 성공한 경우',exact:true}).click();assert.match(await transaction.locator('.learning-saved-result').textContent(),/새 장소 자료/);
 for(const width of [390,320]){
  await page.setViewportSize({width,height:844});await visit('/03_처리/nextplaceservice.html');ranges=page.locator('[data-learning=ranges]');await ranges.scrollIntoViewIfNeeded();assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);const box=await ranges.locator('svg').boundingBox();assert.ok(box.width<=width&&box.width>=220);await ranges.getByRole('slider').fill('650');assert.equal(await ranges.locator('li.eligible').count(),2);
 }
 await page.screenshot({path:'build/ui-preview/readable-distance-mobile.png'});
 await page.evaluate(()=>{document.documentElement.setAttribute('saved-theme','dark');});await page.screenshot({path:'build/ui-preview/readable-distance-dark.png'});
 assert.deepEqual(errors,[]);assert.deepEqual(writes,[]);console.log('Learning examples passed: isolated edit/cancel/finish, cost calculation, range intersection, stale responses, commit/rollback, keyboard controls, SPA navigation and 320/390px layout; no data writes.');
}finally{await browser.close();}
