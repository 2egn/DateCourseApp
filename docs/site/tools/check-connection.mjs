import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';import {createRequire} from 'node:module';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 const context=await browser.newContext({viewport:{width:1440,height:1000},permissions:['clipboard-read','clipboard-write']}),errors=[];
 let active=true,fail=false;
 await context.route('https://connection.test/**',async route=>{
  const url=new URL(route.request().url());
  if(url.pathname==='/api/agent/status')return route.fulfill({status:fail?401:200,json:fail?{error:'login_required'}:{login:'2egn',connections:active?[{name:'Codex',expires:Date.now()+1000}]:[]}});
  if(url.pathname==='/api/agent/revoke'){assert.equal(route.request().method(),'POST');active=false;return route.fulfill({json:{revoked:true}});}
  let file=path.resolve('docs/site/public','.'+decodeURI(url.pathname));if(fs.existsSync(file)&&fs.statSync(file).isDirectory())file=path.join(file,'index.html');
  if(!fs.existsSync(file))return route.fulfill({status:404,body:''});
  return route.fulfill({body:fs.readFileSync(file),contentType:({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json'})[path.extname(file)]||'application/octet-stream'});
 });
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto('https://connection.test/connect/');await page.getByText('2egn · 활성 인증 1개',{exact:true}).waitFor();
 assert.equal(await page.locator('.team-navigation [aria-current=page]').textContent(),'Codex 연결');
 assert.equal(await page.locator('.steps>li').count(),3);assert.equal(await page.locator('details').getAttribute('open'),null);
 for(const [name,expected] of [['서버 이름 복사','datecourseapp-team'],['서버 주소 복사','https://connection.test/mcp'],['확인 질문 복사',await page.locator('#check-prompt').textContent()]]){
  await page.getByRole('button',{name,exact:true}).click();assert.equal(await page.evaluate(()=>navigator.clipboard.readText()),expected);
 }
 await page.getByText('설정 메뉴가 다르거나 연결이 안 될 때',{exact:true}).click();await page.getByRole('button',{name:'서버 등록 명령 복사',exact:true}).click();assert.equal(await page.evaluate(()=>navigator.clipboard.readText()),'codex mcp add datecourseapp-team --url https://connection.test/mcp');
 await page.getByText('설정 메뉴가 다르거나 연결이 안 될 때',{exact:true}).click();
 fs.mkdirSync('build/ui-preview',{recursive:true});await page.screenshot({path:'build/ui-preview/codex-connect-desktop.png',fullPage:true});
 await page.getByRole('button',{name:'내 AI 연결 모두 해제',exact:true}).click();await page.getByRole('button',{name:'돌아가기',exact:true}).click();assert.equal(active,true);
 await page.getByRole('button',{name:'내 AI 연결 모두 해제',exact:true}).click();await page.getByRole('button',{name:'모두 해제',exact:true}).click();await page.getByText('2egn · 활성 인증 0개',{exact:true}).waitFor();assert.equal(active,false);assert.equal(await page.getByRole('button',{name:'내 AI 연결 모두 해제',exact:true}).isDisabled(),true);
 for(const width of [390,320]){await page.setViewportSize({width,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);}
 await page.screenshot({path:'build/ui-preview/codex-connect-mobile.png',fullPage:true});
 await page.emulateMedia({colorScheme:'dark'});await page.screenshot({path:'build/ui-preview/codex-connect-dark.png',fullPage:true});
 fail=true;await page.getByRole('button',{name:'새로고침',exact:true}).click();await page.getByText('로그인이 만료됐어요. 이 페이지를 새로고침해 주세요.',{exact:true}).waitFor();
 assert.deepEqual(errors,[]);console.log('Connection page passed: copy fields, instructions, CLI fallback, revoke/cancel, expired login, 320/390px and dark mode.');
}finally{await browser.close();}
