import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';import {DatabaseSync} from 'node:sqlite';
import {TaskBoard} from '../src/board-store.mjs';
const db=new DatabaseSync(':memory:');const sql={exec(q,...args){const s=db.prepare(q),rows=s.columns().length?s.all(...args):(s.run(...args),[]);return{toArray:()=>rows};}};
const board=new TaskBoard({storage:{sql,transactionSync(fn){db.exec('BEGIN');try{const result=fn();db.exec('COMMIT');return result;}catch(error){db.exec('ROLLBACK');throw error;}}}});
const response=await board.fetch(new Request('https://board.test/api/board',{headers:{'X-Board-User':'fixture'}})),data=await response.json();db.close();
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 const page=await browser.newPage({viewport:{width:1440,height:1000}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://board.test/**',async route=>{
  const url=new URL(route.request().url());if(url.pathname==='/api/board')return route.fulfill({json:data});
  let file=path.resolve('docs/site/public','.'+decodeURI(url.pathname));if(!fs.existsSync(file))return route.fulfill({status:404,body:''});if(fs.statSync(file).isDirectory())file=path.join(file,'index.html');
  return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});
 });
 await page.goto('https://board.test/dashboard/');await page.waitForLoadState('networkidle');
 assert.match(await page.locator('#implementation-status').innerText(),/334705d/);
 const time=page.locator('[data-task-id="TIME-01"]');assert.equal(await time.locator('select').inputValue(),'done');
 assert.equal(data.tasks.find(t=>t.id==='CHECK-01').status,'todo');assert.equal(data.tasks.find(t=>t.id==='TIME-02').status,'todo');
 assert.ok(await page.locator('[data-task-id="MERGE-01"]').isVisible());await page.screenshot({path:'build/ui-preview/latest-dashboard-desktop.png'});
 await page.setViewportSize({width:390,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await page.screenshot({path:'build/ui-preview/latest-dashboard-mobile.png'});
 assert.deepEqual(errors,[]);console.log('Latest board passed: reference, implementation cards, remaining manual tasks, desktop/mobile.');
}finally{await browser.close();}
