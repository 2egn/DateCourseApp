import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
import {AUDIT_TASKS,AUDIT_ADDITIONS} from '../src/board-audit.mjs';
import {ASSIGNMENT_TASKS} from '../src/assignment-priorities.mjs';
import {STATUSES,AREAS,PRIORITIES} from '../src/board-store.mjs';
import {taskTree} from '../web/dashboard/task-tree.js';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const before=JSON.parse(fs.readFileSync('build/board-reorg-before.json','utf8'));
const tasks=before.map(t=>({...t,...AUDIT_TASKS.find(a=>a.id===t.id)?.after}));
for(const t of AUDIT_ADDITIONS)tasks.push({archived:false,version:1,assignee:'',updatedAt:new Date().toISOString(),updatedBy:'검사',...t});
for(const task of tasks)Object.assign(task,ASSIGNMENT_TASKS.find(item=>item.id===task.id)?.after);
const data={tasks,statuses:STATUSES,areas:AREAS,priorities:PRIORITIES};
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 const context=await browser.newContext({viewport:{width:1440,height:1000}}),errors=[];
 await context.route('https://hierarchy.test/**',async route=>{
  const r=route.request(),url=new URL(r.url());
  if(url.pathname==='/api/board')return route.fulfill({json:data});
  if(url.pathname.startsWith('/api/board/tasks/')){const task=tasks.find(t=>t.id===url.pathname.split('/').at(-1));Object.assign(task,JSON.parse(r.postData()),{version:task.version+1});return route.fulfill({json:{task}});}
  let file=path.resolve('docs/site/public','.'+decodeURI(url.pathname));if(fs.existsSync(file)&&fs.statSync(file).isDirectory())file=path.join(file,'index.html');
  if(!fs.existsSync(file))return route.fulfill({status:404,body:''});
  return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.css':'text/css','.js':'text/javascript','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});
 });
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto('https://hierarchy.test/dashboard/');await page.locator('.card').first().waitFor();
 assert.equal(await page.locator('[data-task-id]').count(),55);assert.ok(await page.locator('.card').count()<55);
 const ids=await page.locator('[data-task-id]').evaluateAll(els=>els.map(e=>e.dataset.taskId));assert.equal(new Set(ids).size,55);
 await page.locator('.board-guide > summary').click();await page.getByText('여러 저장을 전부 성공시키거나 전부 취소하는 한 묶음',{exact:true}).waitFor();await page.locator('.board-guide > summary').click();
 const node=id=>page.locator('[data-task-id="'+id+'"]');
 const preview=node('DB-01').locator(':scope > details > summary .subtask-preview-title');await preview.first().waitFor({state:'visible'});assert.match((await preview.allTextContents()).join(' '),/코스가 빠짐없이/);
 const details=node('DB-01').locator(':scope > details');await details.locator(':scope > summary').click();await preview.first().waitFor({state:'hidden'});await node('DB-02').locator(':scope > .subtask-heading').waitFor({state:'visible'});await details.locator(':scope > summary').click();await preview.first().waitFor({state:'visible'});
 fs.mkdirSync('build/ui-preview',{recursive:true});await node('DB-01').screenshot({path:'build/ui-preview/board-collapsed-preview.png'});
 for(const task of tasks.filter(t=>!t.archived)){const parent=taskTree(tasks.filter(t=>!t.archived)).parents.get(task.id);if(parent){const p=tasks.find(t=>t.id===parent);assert.equal(p.status,task.status);assert.equal(p.area,task.area);}}
 assert.equal(await node('DB-01').locator('[data-task-id="DB-02"]').count(),1);
 assert.equal(await node('DB-02').locator('[data-task-id="DB-03"]').count(),1);
 assert.equal(await node('DATA-01').locator('[data-task-id="T-31FAD2F2"]').count(),0);
 assert.equal(await node('TEST-01').locator('[data-task-id="T-D50D2A41"]').count(),0);
 assert.equal(taskTree(tasks).parents.has('TEST-01'),false);
 await page.locator('#search').fill('코스가 빠짐없이 한 번에 저장되게 만들기');await node('DB-02').getByRole('button',{name:'코스가 빠짐없이 한 번에 저장되게 만들기',exact:true}).click();
 assert.match(await page.locator('#dependency-preview').textContent(),/DB-01/);await page.locator('#close').click();
 await node('DB-02').locator(':scope > .subtask-controls > select').selectOption('progress');await page.waitForFunction(()=>document.querySelector('[data-task-id="DB-02"]')?.closest('.column')?.classList.contains('progress'));
 await page.reload();await page.locator('#search').fill('코스가 빠짐없이 한 번에 저장되게 만들기');await node('DB-02').getByRole('button',{name:'코스가 빠짐없이 한 번에 저장되게 만들기',exact:true}).waitFor();
 assert.equal(await node('DB-02').locator(':scope > .card-bottom > select').inputValue(),'progress');
 assert.equal(tasks.find(t=>t.id==='DB-01').status,'todo');assert.equal(tasks.find(t=>t.id==='DB-03').status,'todo');
 await page.locator('#search').fill('');await page.getByRole('button',{name:'저장·내부 처리',exact:true}).click();assert.ok(await node('DB-02').count());await page.getByRole('button',{name:'전체 영역',exact:true}).click();
 await page.locator('#priority').selectOption('high');assert.equal(await node('DATA-01').count(),0);assert.equal(await node('T-31FAD2F2').count(),0);assert.equal(await node('T-6E4E5509').count(),1);await page.locator('#priority').selectOption('');
 await page.evaluate(()=>document.querySelectorAll('details.subtasks').forEach(e=>e.open=true));
 fs.mkdirSync('build/ui-preview',{recursive:true});await page.screenshot({path:'build/ui-preview/board-hierarchy-desktop.png',fullPage:false});
 for(const width of [390,320]){await page.setViewportSize({width,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);}
 await node('DB-01').scrollIntoViewIfNeeded();await page.screenshot({path:'build/ui-preview/board-hierarchy-mobile.png',fullPage:false});
 await page.locator('#archived').check();assert.equal(await page.locator('[data-task-id]').count(),57);assert.equal(await node('PUB-01').locator('[data-task-id="PUB-02"]').count(),0);
 assert.deepEqual(errors,[]);console.log('Hierarchy UI passed: 57 unique tasks, chained/multiple prerequisites, search context, field filters, independent saved status, archive and 320px layout.');
}finally{await browser.close();}
