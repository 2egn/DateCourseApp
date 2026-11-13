import fs from 'node:fs';import path from 'node:path';import {createRequire} from 'node:module';import assert from 'node:assert/strict';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});const report=[];
try{for(const [version,folder] of [['before','build/perf-before-site'],['after','docs/site/public']]){
 let requests=0,active=0;const queue=[];const context=await browser.newContext();
 await context.route('https://performance.test/**',async route=>{
  if(active>=6)await new Promise(resolve=>queue.push(resolve));active++;requests++;
  try{await new Promise(resolve=>setTimeout(resolve,80));let file=path.resolve(folder,'.'+decodeURI(new URL(route.request().url()).pathname));if(!fs.existsSync(file)&&!path.extname(file))file+='.html';if(!fs.existsSync(file))return route.fulfill({status:404,body:''});if(fs.statSync(file).isDirectory())file=path.join(file,'index.html');await route.fulfill({body:fs.readFileSync(file),headers:{'Cache-Control':'private, no-store'},contentType:({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});}finally{active--;queue.shift()?.();}
 });
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));const start=Date.now();await page.goto('https://performance.test/');await page.waitForLoadState('networkidle');const firstMs=Date.now()-start,firstRequests=requests;
 await page.locator('article a').filter({hasText:'문서 읽는 법',exact:true}).click();await page.waitForURL(/문서|%EB/);await page.waitForLoadState('networkidle');await page.locator('article').waitFor();const totalMs=Date.now()-start;assert.deepEqual(errors,[]);
 report.push({version,firstRequests,firstMs,nextRequests:requests-firstRequests,nextMs:totalMs-firstMs});await context.close();
 }assert.ok(report[1].firstRequests<report[0].firstRequests);assert.ok(report[1].nextRequests<report[0].nextRequests);console.log(JSON.stringify({simulation:'80ms per protected request, six concurrent downloads; not live-site timing',results:report},null,2));fs.writeFileSync('build/navigation-performance.json',JSON.stringify(report,null,2));
}finally{await browser.close();}
