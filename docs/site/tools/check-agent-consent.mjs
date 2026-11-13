// Exercise the actual browser form, not a hand-built OAuth POST.
import fs from 'node:fs';import path from 'node:path';import {createRequire} from 'node:module';import {DatabaseSync} from 'node:sqlite';import assert from 'node:assert/strict';
import {AgentAccess} from '../src/agent-store.mjs';import {handle,seal} from '../src/worker.mjs';
const {chromium}=createRequire(import.meta.url)(path.resolve(path.dirname(process.execPath),'../node_modules/playwright'));
const db=new DatabaseSync(':memory:');let alarm;
const ctx={storage:{sql:{exec(q,...p){const s=db.prepare(q),rows=s.columns().length?s.all(...p):(s.run(...p),[]);return {toArray:()=>rows};}},transactionSync(f){db.exec('BEGIN');try{const r=f();db.exec('COMMIT');return r;}catch(e){db.exec('ROLLBACK');throw e;}},getAlarm:async()=>alarm,setAlarm:async v=>{alarm=v;}}};
const object=new AgentAccess(ctx),origin='https://consent.test',callback='http://127.0.0.1:44888/callback/test';
const env={SITE_ORIGIN:origin,GITHUB_CLIENT_ID:'test',GITHUB_CLIENT_SECRET:'test',SESSION_KEY:'ab'.repeat(32),AGENT_ACCESS:{idFromName:n=>n,get:()=>object},ASSETS:{fetch:async r=>new Response(fs.readFileSync('docs/site/public'+new URL(r.url).pathname),{headers:{'Content-Type':'text/css'}})}};
const remote=async()=>Response.json([{id:1390711615}]);
const call=(pathname,init)=>handle(new Request(origin+pathname,init),env,remote);
const registered=await call('/oauth/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({client_name:'Browser consent test',redirect_uris:[callback]})});
const client=await registered.json(),verifier='x'.repeat(64),code_challenge=Buffer.from(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(verifier))).toString('base64url');
const params=new URLSearchParams({client_id:client.client_id,redirect_uri:callback,response_type:'code',resource:origin+'/mcp',scope:'team:read',state:'browser-state',code_challenge,code_challenge_method:'S256'});
const session=await seal({id:7,login:'test-team',token:'mock-github',expires:Date.now()+3600000},env,'__Host-dca_session');
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 const context=await browser.newContext(),logs=[];let postStatus,postError,callbackURL;
 await context.addCookies([{name:'__Host-dca_session',value:session,domain:'consent.test',path:'/',secure:true,httpOnly:true,sameSite:'Lax'}]);
 await context.route(origin+'/**',async route=>{
  const r=route.request(),response=await handle(new Request(r.url(),{method:r.method(),headers:await r.allHeaders(),body:r.method()==='POST'?r.postDataBuffer():undefined}),env,remote);
  const data=await response.arrayBuffer();if(r.method()==='POST'){postStatus=response.status;if(postStatus!==302)postError=new TextDecoder().decode(data);}
  await route.fulfill({status:response.status,headers:Object.fromEntries(response.headers),body:Buffer.from(data)});
 });
 await context.route(callback+'**',async route=>{callbackURL=new URL(route.request().url());await route.fulfill({contentType:'text/plain',body:'Connected'});});
 const page=await context.newPage();page.on('requestfailed',r=>logs.push(new URL(r.url()).pathname+': '+r.failure()?.errorText));page.on('console',m=>{if(m.type()==='error')logs.push(m.text().replace(/([?&](?:code|state)=)[^&\s]+/g,'$1[redacted]'));});
 await page.goto(origin+'/oauth/authorize?'+params);await page.getByRole('button',{name:'읽기 전용 연결 허용',exact:true}).click();
 await page.waitForTimeout(1500);
 assert.equal(postStatus,302,postError);assert.ok(callbackURL,logs.join('\n')+' current: '+page.url().split('?')[0]);assert.equal(callbackURL.searchParams.get('state'),'browser-state');assert.ok(callbackURL.searchParams.get('code'));
 console.log('Browser consent passed: form submission, signed consent validation and loopback callback navigation.');
}finally{await browser.close();db.close();}
