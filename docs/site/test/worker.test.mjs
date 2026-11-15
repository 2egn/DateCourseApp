import test from 'node:test';
import assert from 'node:assert/strict';
import {handle, seal, unseal, isCollaborator} from '../src/worker.mjs';
import fs from 'node:fs';

const env = {SITE_ORIGIN: 'https://docs.example.test', GITHUB_CLIENT_ID: 'test-id',
  GITHUB_CLIENT_SECRET: 'test-secret', SESSION_KEY: 'ab'.repeat(32),
  ASSETS: {fetch: async () => new Response('PRIVATE DOCUMENT', {headers: {'Cache-Control': 'public, max-age=1000'}})}};
const member = async () => Response.json([{id:1390711615}]);
const outsider = async () => Response.json([{id:42}]);
const make = (path, token) => new Request(env.SITE_ORIGIN + path, token ? {headers:{Cookie:'__Host-dca_session='+token}} : {});
async function session(expires=Date.now()+10000) {return seal({id:103644239,token:'test-access-token',expires},env,'__Host-dca_session');}

test('board API requires membership and same-origin writes; actor is server controlled',async()=>{
 assert.equal((await handle(make('/api/board'),env)).status,401);
 const token=await session();assert.equal((await handle(make('/api/board',token),env,outsider)).status,403);
 let forwarded;const boardEnv={...env,BOARD:{idFromName:n=>n,get:()=>({fetch:async r=>{forwarded=r;return Response.json({tasks:[]});}})}};
 const write=origin=>new Request(env.SITE_ORIGIN+'/api/board/tasks',{method:'POST',headers:{Cookie:'__Host-dca_session='+token,Origin:origin,'X-Board-User':'spoofed','Content-Type':'application/json'},body:'{}'});
 assert.equal((await handle(write('https://evil.test'),boardEnv,member)).status,403);
 const response=await handle(write(env.SITE_ORIGIN),boardEnv,member);assert.equal(response.status,200);assert.equal(forwarded.headers.get('X-Board-User'),'GitHub #103644239');assert.equal(response.headers.get('Cache-Control'),'private, no-store');
 const old=await handle(make('/00_목차.html',token),env,member);assert.equal(old.status,302);assert.equal(decodeURI(old.headers.get('Location')),'/목차.html');
 for(const [path,target] of [['/팀원_전용_게시_계획.html','/'],['/00_작업_대시보드.html','/dashboard/']]){
  const retired=await handle(make(path,token),env,member);assert.equal(retired.status,302);assert.equal(retired.headers.get('Location'),target);
 }
});

test('documents redirect to login, while assets never start another OAuth attempt',async()=>{
  for (const path of ['/', '/02_화면/RouteEditorController.html','/static/contentIndex.json','/index.css','/image.png']) {
    const response=await handle(make(path),env);
    const document=path==='/' || path.endsWith('.html');
    assert.equal(response.status,document?302:401);
    if(document)assert.match(response.headers.get('Location'),/^\/auth\/login/);
    else assert.equal(response.headers.get('Location'),null);
    assert.doesNotMatch(await response.text(),/PRIVATE DOCUMENT/);
  }
});
test('a public reader or contributor without collaboration is denied',async()=>{
  assert.equal((await handle(make('/',await session()),env,outsider)).status,403);
});
test('member can read documents but nothing is publicly cacheable',async()=>{
  const response=await handle(make('/',await session()),env,member);
  assert.equal(response.status,200);assert.equal(await response.text(),'PRIVATE DOCUMENT');
  assert.equal(response.headers.get('Cache-Control'),'private, no-store');
});
test('removed membership is checked again with the same session',async()=>{
  const token=await session();assert.equal((await handle(make('/',token),env,member)).status,200);
  assert.equal((await handle(make('/',token),env,outsider)).status,403);
});

test('overlapping membership lookups share one call without caching settled permissions',async()=>{
 let finish,calls=0;const remote=async()=>{calls++;return new Promise(resolve=>{finish=resolve;});};
 const token=await session();const requests=Promise.all([handle(make('/',token),env,remote),handle(make('/index.css',token),env,remote)]);
 while(!finish)await new Promise(resolve=>setTimeout(resolve,1));
 finish(Response.json([{id:1390711615}]));const responses=await requests;assert.equal(calls,1);assert.ok(responses.every(r=>r.status===200));
 const next=handle(make('/',token),env,remote);while(calls<2)await new Promise(resolve=>setTimeout(resolve,1));finish(Response.json([]));assert.equal((await next).status,403);
});
test('GitHub API failure never exposes the assets',async()=>{
  const response=await handle(make('/',await session()),env,async()=>new Response('rate limit',{status:403}));
  assert.equal(response.status,503);assert.doesNotMatch(await response.text(),/PRIVATE DOCUMENT/);
});
test('expired and forged cookies fail closed',async()=>{
  assert.equal((await handle(make('/',await session(Date.now()-1)),env,member)).status,302);
  const token=await session();assert.equal((await handle(make('/',token+'x'),env,member)).status,302);
});
test('state and session encrypted payloads are not interchangeable',async()=>{
  assert.equal(await unseal(await session(),env,'__Host-dca_state'),null);
});
test('configuration missing and alternate host fail closed',async()=>{
  assert.equal((await handle(make('/'),{...env,SESSION_KEY:''})).status,503);
  assert.equal((await handle(new Request('https://preview.example.test/'),env)).status,403);
});
test('membership check handles pagination and never checks public visibility',async()=>{
  let calls=0;
  const allowed=await isCollaborator('test',async(url)=>{
    calls++;assert.match(url,/\/user\/repos\?affiliation=owner,collaborator/);
    return Response.json(calls===1?Array.from({length:100},(_,id)=>({id})): [{id:1390711615}]);
  });
  assert.equal(allowed,true);assert.equal(calls,2);
});
test('login uses state and PKCE without requesting repo write scopes',async()=>{
  const response=await handle(make('/auth/login?next=https://evil.test/'),env);
  const url=new URL(response.headers.get('Location'));
  assert.equal(url.origin,'https://github.com');assert.equal(url.searchParams.get('scope'),'');
  assert.equal(url.searchParams.get('code_challenge_method'),'S256');assert.equal(url.searchParams.get('code_challenge').length,43);
  const encrypted=response.headers.get('Set-Cookie').split(';')[0].split('=')[1];
  const state=await unseal(encrypted,env,'__Host-dca_state');assert.equal(state.next,'/');
});
test('callback rejects missing or mismatched state before token exchange',async()=>{
  let called=false;const remote=async()=>{called=true;return Response.json({});};
  assert.equal((await handle(make('/auth/callback?code=a&state=bad'),env,remote)).status,403);
  assert.equal(called,false);
});
test('logout requires a same-origin POST',async()=>{
  assert.equal((await handle(make('/auth/logout'),env)).status,403);
  const good=new Request(env.SITE_ORIGIN+'/auth/logout',{method:'POST',headers:{Origin:env.SITE_ORIGIN}});
  assert.equal((await handle(good,env)).status,302);
});

test('successful OAuth validates identity and membership then issues an encrypted session',async()=>{
  const state=await seal({nonce:'nonce',verifier:'verifier',next:'/00_목차',expires:Date.now()+10000},env,'__Host-dca_state');
  const request=new Request(env.SITE_ORIGIN+'/auth/callback?code=code&state=nonce',{headers:{Cookie:'__Host-dca_state='+state}});
  const calls=[];
  const remote=async(url,options)=>{
    calls.push(url);
    if(url==='https://github.com/login/oauth/access_token'){
      assert.equal(options.body.get('code_verifier'),'verifier');
      return Response.json({access_token:'only-test-token'});
    }
    if(url==='https://api.github.com/user')return Response.json({id:103644239});
    return member();
  };
  const response=await handle(request,env,remote);assert.equal(response.status,302);
  assert.equal(decodeURI(response.headers.get('Location')),'/00_목차');
  const encrypted=response.headers.get('Set-Cookie').match(/__Host-dca_session=([^;]+)/)[1];
  assert.doesNotMatch(encrypted,/only-test-token/);
  const payload=await unseal(encrypted,env,'__Host-dca_session');
  assert.equal(payload.id,103644239);assert.equal(payload.token,'only-test-token');
  assert.equal(calls.length,3);
});

test('deployment configuration gates every static asset and disables preview URLs',()=>{
  const config=JSON.parse(fs.readFileSync(new URL('../wrangler.jsonc',import.meta.url),'utf8'));
  assert.equal(config.assets.run_worker_first,true);assert.equal(config.preview_urls,false);
});

test('two concurrent login attempts retain independent cookies and both can finish',async()=>{
  const first=await handle(make('/auth/login?next=/one'),env);
  const second=await handle(make('/auth/login?next=/two'),env);
  const a=first.headers.get('Set-Cookie').split(';')[0];
  const b=second.headers.get('Set-Cookie').split(';')[0];
  assert.notEqual(a.split('=')[0],b.split('=')[0]);
  const remote=async(url)=> url.includes('/access_token')?Response.json({access_token:'test-token'}):
    url.endsWith('/user')?Response.json({id:103644239}):member();
  for(const [login,expected] of [[first,'/one'],[second,'/two']]){
    const nonce=new URL(login.headers.get('Location')).searchParams.get('state');
    const request=new Request(env.SITE_ORIGIN+'/auth/callback?code=test&state='+nonce,{headers:{Cookie:a+'; '+b}});
    const response=await handle(request,env,remote);
    assert.equal(response.status,302);assert.equal(response.headers.get('Location'),expected);
  }
});

test('missing cookie offers recovery but does not bypass state checks',async()=>{
  const response=await handle(make('/auth/callback?code=test&state='+('a'.repeat(43))),env);
  assert.equal(response.status,403);
  const body=await response.text();assert.match(body,/AUTH_COOKIE_MISSING/);assert.match(body,/href="\/auth\/login"/);
});

test('expired state and mismatched state are rejected before token exchange',async()=>{
  for(const [payload,reason] of [
    [{nonce:'a'.repeat(43),expires:Date.now()-1},'AUTH_STATE_EXPIRED'],
    [{nonce:'wrong',expires:Date.now()+10000},'AUTH_STATE_MISMATCH']
  ]){
    const nonce='a'.repeat(43);
    const value=await seal(payload,env,'__Host-dca_state');
    const request=new Request(env.SITE_ORIGIN+'/auth/callback?code=test&state='+nonce,
      {headers:{Cookie:'__Host-dca_state_'+nonce+'='+value}});
    const response=await handle(request,env,()=>{throw new Error('Must not exchange token');});
    assert.equal(response.status,403);assert.match(await response.text(),new RegExp(reason));
  }
});

test('favicon and background fetches do not redirect or set login cookies',async()=>{
  const icon=await handle(make('/favicon.ico'),env);
  assert.equal(icon.status,401);assert.equal(icon.headers.get('Set-Cookie'),null);
  const background=new Request(env.SITE_ORIGIN+'/',{headers:{'Sec-Fetch-Dest':'empty'}});
  const response=await handle(background,env);
  assert.equal(response.status,401);assert.equal(response.headers.get('Location'),null);
});

test('authorized board HTML includes a safe current snapshot without a second membership check',async()=>{
 const token=await session();let reads=0,checks=0;
 const payload={tasks:[{id:'T-1',title:'</script><img src=x onerror=alert(1)>'}],statuses:{todo:'할 일'},areas:{frontend:'프론트'},priorities:{normal:'보통'}};
 const boardEnv={...env,ASSETS:{fetch:async()=>new Response('<html><body>BOARD</body></html>',{headers:{'Content-Type':'text/html','Content-Length':'41',ETag:'old'}})},BOARD:{idFromName:n=>n,get:()=>({fetch:async r=>{reads++;assert.equal(r.headers.get('X-Board-User'),'GitHub #103644239');assert.equal(new URL(r.url).pathname,'/api/board');return Response.json(payload);}})}};
 const response=await handle(make('/dashboard/',token),boardEnv,async()=>{checks++;return member();});const html=await response.text();
 assert.equal(response.status,200);assert.equal(checks,1);assert.equal(reads,1);assert.equal(response.headers.get('Content-Length'),null);assert.equal(response.headers.get('ETag'),null);assert.equal(response.headers.get('Cache-Control'),'private, no-store');assert.doesNotMatch(html,/<img/);
 const text=html.match(/<script id="board-bootstrap" type="application\/json">([\s\S]*?)<\/script>/)[1];assert.deepEqual(JSON.parse(text),payload);
 reads=0;assert.equal((await handle(make('/dashboard/',token),boardEnv,outsider)).status,403);assert.equal(reads,0);
});

test('board snapshot failure keeps the normal HTML for client-side retry',async()=>{
 const boardEnv={...env,ASSETS:{fetch:async()=>new Response('<html><body>BOARD</body></html>',{headers:{'Content-Type':'text/html'}})},BOARD:{idFromName:n=>n,get:()=>({fetch:async()=>{throw Error('storage offline');}})}};
 const response=await handle(make('/dashboard/',await session()),boardEnv,member);assert.equal(response.status,200);assert.equal(await response.text(),'<html><body>BOARD</body></html>');
});
