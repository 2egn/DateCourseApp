import test from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {handle,seal} from '../src/worker.mjs';
import {AgentAccess} from '../src/agent-store.mjs';
const origin='https://docs.example.test',callback='http://127.0.0.1:43125/callback';
const member=async()=>Response.json([{id:1390711615}]);
const outsider=async()=>Response.json([]);
const hash=async value=>Buffer.from(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value))).toString('base64url');
function setup(){
 const db=new DatabaseSync(':memory:');let alarm=null,assets=0,writes=0;
 const ctx={storage:{sql:{exec(q,...p){const s=db.prepare(q),rows=s.columns().length?s.all(...p):(s.run(...p),[]);return {toArray:()=>rows};}},transactionSync(fn){db.exec('BEGIN');try{const r=fn();db.exec('COMMIT');return r;}catch(e){db.exec('ROLLBACK');throw e;}},getAlarm:async()=>alarm,setAlarm:async value=>{alarm=value;}}};
 const object=new AgentAccess(ctx);
 const docs=[{id:'index',title:'시작',url:'/',version:'1-a',markdown:'시작 안내'},{id:'저장',title:'코스 저장',url:'/저장.html',version:'2-b',markdown:'개발 중 코스 저장 흐름'}];
 const env={SITE_ORIGIN:origin,GITHUB_CLIENT_ID:'test',GITHUB_CLIENT_SECRET:'test',SESSION_KEY:'ab'.repeat(32),
  AGENT_ACCESS:{idFromName:n=>n,get:()=>object},ASSETS:{fetch:async r=>{assets++;return r.url.endsWith('/agent/documents.json')?Response.json(docs):new Response('PRIVATE DOCS');}},
  BOARD:{idFromName:n=>n,get:()=>({fetch:async r=>{if(r.method!=='GET')writes++;return Response.json({tasks:[{id:'A',title:'저장 구현',priority:'high',status:'todo',area:'backend',archived:false},{id:'B',title:'보관',priority:'low',status:'done',archived:true}],statuses:{todo:'할 일'},areas:{backend:'백엔드'},priorities:{high:'높음'}});}})}};
 const request=(path,{method='GET',cookie,token,json,form,headers={}}={})=>new Request(origin+path,{method,headers:{...cookie?{Cookie:'__Host-dca_session='+cookie}:{},...token?{Authorization:'Bearer '+token}:{},...json!==undefined?{'Content-Type':'application/json'}:{},...form?{'Content-Type':'application/x-www-form-urlencoded'}:{},...headers},body:json!==undefined?JSON.stringify(json):form?new URLSearchParams(form):undefined});
 const run=(path,options,remote=member)=>handle(request(path,options),env,remote);
 const session=()=>seal({id:7,login:'teammate',token:'github-test-token',expires:Date.now()+3600000},env,'__Host-dca_session');
 const register=async()=>{const r=await run('/oauth/register',{method:'POST',json:{client_name:'Codex <script>',redirect_uris:[callback],token_endpoint_auth_method:'none'}});assert.equal(r.status,201);return r.json();};
 const authorize=async(client,change={})=>{
  const cookie=await session(),verifier='x'.repeat(64);
  const p={client_id:client.client_id,redirect_uri:callback,response_type:'code',resource:origin+'/mcp',scope:'team:read',state:'test-state',code_challenge:await hash(verifier),code_challenge_method:'S256',...change};
  const response=await run('/oauth/authorize?'+new URLSearchParams(p),{cookie});return {response,cookie,verifier,p};
 };
 const approve=async(client)=>{const a=await authorize(client);assert.equal(a.response.status,200);const html=await a.response.text();assert.doesNotMatch(html,/<script>/);const consent=html.match(/name="consent" value="([^"]+)"/)[1];const response=await run('/oauth/authorize',{method:'POST',cookie:a.cookie,headers:{Origin:origin},form:{consent,decision:'allow'}});assert.equal(response.status,302);const target=new URL(response.headers.get('Location'));assert.equal(target.searchParams.get('iss'),origin);assert.equal(target.searchParams.get('state'),'test-state');return {...a,consent,code:target.searchParams.get('code')};};
 const exchange=(client,a,change={})=>run('/oauth/token',{method:'POST',form:{grant_type:'authorization_code',code:a.code,client_id:client.client_id,redirect_uri:callback,resource:origin+'/mcp',code_verifier:a.verifier,...change}});
 const connected=async()=>{const client=await register(),a=await approve(client),r=await exchange(client,a);assert.equal(r.status,200);return {client,a,token:(await r.json()).access_token};};
 const rpc=(token,method,params)=>run('/mcp',{method:'POST',token,json:{jsonrpc:'2.0',id:1,method,params}});
 return {db,env,object,run,session,register,authorize,approve,exchange,connected,rpc,assets:()=>assets,writes:()=>writes};
}
test('only discovery is public; documents, MCP and browser APIs remain protected',async t=>{
 const s=setup();t.after(()=>s.db.close());
 for(const path of ['/.well-known/oauth-protected-resource/mcp','/.well-known/oauth-protected-resource','/.well-known/oauth-authorization-server']){const r=await s.run(path);assert.equal(r.status,200);assert.equal(r.headers.get('Cache-Control'),'private, no-store');assert.doesNotMatch(await r.text(),/PRIVATE DOCS|github-test-token/);}
 const mcp=await s.run('/mcp');assert.equal(mcp.status,401);assert.match(mcp.headers.get('WWW-Authenticate'),/resource_metadata/);
 assert.equal((await s.run('/api/agent/status')).status,401);
 assert.equal((await s.run('/agent/documents.json')).status,401);
 assert.equal((await s.run('/connect/')).status,302);assert.equal(s.assets(),0);
 const cookie=await s.session();assert.equal((await s.run('/mcp',{cookie})).status,401);
});
test('DCR accepts only loopback callbacks and authorization code public clients',async t=>{
 const s=setup();t.after(()=>s.db.close());
 for(const redirect of ['https://evil.test/callback','http://127.0.0.1.evil.test/callback','http://user@127.0.0.1/callback','http://127.0.0.1/callback#bad','http://localhost/other'])assert.equal((await s.run('/oauth/register',{method:'POST',json:{redirect_uris:[redirect]}})).status,400);
 assert.equal((await s.run('/oauth/register',{method:'POST',json:{redirect_uris:[callback],grant_types:['client_credentials']}})).status,400);
 const client=await s.register();assert.equal(typeof client.client_id,'string');assert.equal(client.token_endpoint_auth_method,'none');
 const native=await s.run('/oauth/register',{method:'POST',json:{client_name:'Codex',redirect_uris:['http://127.0.0.1:61134/callback/_960XD5mylHI'],grant_types:['authorization_code','refresh_token'],response_types:['code'],token_endpoint_auth_method:'none',application_type:'native',scope:'team:read'}});
 assert.equal(native.status,201);assert.deepEqual((await native.json()).grant_types,['authorization_code']);
});
test('authorize requires team session, exact callback, audience, scope, PKCE and explicit consent',async t=>{
 const s=setup();t.after(()=>s.db.close());const client=await s.register();
 assert.equal((await s.run('/oauth/authorize?client_id='+client.client_id)).status,302);
 assert.equal((await s.authorize(client,{redirect_uri:'http://127.0.0.1:99/callback'})).response.status,400);
 for(const change of [{scope:'team:write'},{resource:'https://evil.test/mcp'},{code_challenge_method:'plain'},{code_challenge:'bad'}]){const r=(await s.authorize(client,change)).response;assert.equal(r.status,302);assert.ok(new URL(r.headers.get('Location')).searchParams.get('error'));}
 const a=await s.authorize(client),html=await a.response.text(),consent=html.match(/name="consent" value="([^"]+)"/)[1];
 assert.ok(a.response.headers.get('Content-Security-Policy').includes("form-action 'self' "+callback+';'));
 assert.doesNotMatch(a.response.headers.get('Content-Security-Policy'),/\*|evil/);
 assert.equal((await s.run('/oauth/authorize',{method:'POST',cookie:a.cookie,headers:{Origin:'https://evil.test'},form:{consent,decision:'allow'}})).status,403);
 const r=await s.run('/oauth/authorize',{method:'POST',cookie:a.cookie,headers:{Origin:origin},form:{consent,decision:'deny'}});assert.equal(new URL(r.headers.get('Location')).searchParams.get('error'),'access_denied');
 const replay=await s.run('/oauth/authorize',{method:'POST',cookie:a.cookie,headers:{Origin:origin},form:{consent,decision:'allow'}});
 assert.equal(replay.status,400);assert.match(await replay.text(),/이미 제출한 승인 화면/);
 assert.equal((await s.run('/oauth/authorize?'+new URLSearchParams(a.p),{cookie:a.cookie},outsider)).status,403);
});
test('codes are single use, PKCE bound, expiring and never disclose GitHub credentials',async t=>{
 const s=setup();t.after(()=>s.db.close());const client=await s.register(),a=await s.approve(client);
 const results=await Promise.all([s.exchange(client,a),s.exchange(client,a)]);assert.deepEqual(results.map(r=>r.status).sort(),[200,400]);
 const data=await results.find(r=>r.status===200).json();assert.equal(data.token_type,'Bearer');assert.equal(data.scope,'team:read');assert.ok(data.expires_in<=28800);assert.doesNotMatch(JSON.stringify(data),/github-test-token/);
 const stored=s.db.prepare('SELECT key,value FROM agent_records').all();assert.ok(!stored.some(row=>row.key.includes(data.access_token)||row.value.includes('github-test-token')));
 for(const change of [{code_verifier:'z'.repeat(64)},{redirect_uri:'http://127.0.0.1:1/callback'},{resource:'https://evil.test/mcp'},{client_id:'different'}])assert.equal((await s.exchange(client,await s.approve(client),change)).status,400);
 const expired=await s.approve(client);s.db.exec("UPDATE agent_records SET expires=1 WHERE key LIKE 'code:%'");assert.equal((await s.exchange(client,expired)).status,400);
});
test('read-only MCP negotiates, searches, reads Markdown and filters real board state',async t=>{
 const s=setup();t.after(()=>s.db.close());const {token}=await s.connected();
 const init=await (await s.rpc(token,'initialize',{protocolVersion:'2025-06-18'})).json();assert.equal(init.result.protocolVersion,'2025-06-18');assert.equal(init.result.capabilities.tools.listChanged,false);
 const list=await (await s.rpc(token,'tools/list')).json();assert.equal(list.result.tools.length,5);assert.ok(list.result.tools.every(v=>v.annotations.readOnlyHint&&!v.annotations.destructiveHint));
 const call=async(name,args={})=>{const r=await s.rpc(token,'tools/call',{name,arguments:args});assert.equal(r.status,200);const data=await r.json();assert.ok(!data.result.isError);return JSON.parse(data.result.content[0].text);};
 assert.equal((await call('connection_status')).login,'teammate');
 assert.equal((await call('list_documents',{limit:1})).documents.length,1);
 assert.equal((await call('search_documents',{query:'저장'})).documents[0].id,'저장');
 assert.match((await call('read_document',{id:'저장'})).markdown,/개발 중/);
 assert.equal((await call('get_tasks',{priority:'high',status:'todo'})).tasks[0].id,'A');assert.equal((await call('get_tasks')).total,1);assert.equal((await call('get_tasks',{include_archived:true})).total,2);
 for(const params of [{name:'create_task',arguments:{title:'bad'}},{name:'get_tasks',arguments:{status:'bad'}},{name:'read_document',arguments:{id:'../../secrets'}},{name:'list_documents',arguments:{limit:100000}},{name:'list_documents',arguments:{offset:-1}}])assert.equal((await (await s.rpc(token,'tools/call',params)).json()).result.isError,true);
 assert.equal(s.writes(),0);
 assert.equal((await s.run('/api/board/tasks',{method:'POST',token,json:{title:'bad'}})).status,401);
 assert.equal((await s.rpc(token,'tasks/delete',{})).status,200);assert.equal((await (await s.rpc(token,'tasks/delete',{})).json()).error.code,-32601);
});
test('membership removal, credential failure, expiry and revocation invalidate access',async t=>{
 const s=setup();t.after(()=>s.db.close());const c=await s.connected();
 const req={method:'POST',token:c.token,json:{jsonrpc:'2.0',id:1,method:'tools/list'}};
 assert.equal((await s.run('/mcp',req,outsider)).status,403);
 assert.equal((await s.run('/mcp',req,async()=>new Response('',{status:401}))).status,401);
 assert.equal((await s.run('/mcp',req,async()=>new Response('',{status:500}))).status,503);
 const status=await (await s.run('/api/agent/status',{cookie:c.a.cookie})).json();assert.equal(status.connections.length,1);assert.doesNotMatch(JSON.stringify(status),/github-test-token|identity|epoch/);
 assert.equal((await s.run('/api/agent/revoke',{method:'POST',cookie:c.a.cookie,headers:{Origin:'https://evil.test'}})).status,403);
 const pending=await s.approve(c.client);
 assert.equal((await s.run('/api/agent/revoke',{method:'POST',cookie:c.a.cookie,headers:{Origin:origin}})).status,200);
 assert.equal((await s.rpc(c.token,'ping')).status,401);assert.equal((await s.exchange(c.client,pending)).status,400);
 const next=await s.connected();assert.equal((await s.rpc(next.token,'ping')).status,200);
 s.db.exec("UPDATE agent_records SET expires=1 WHERE key LIKE 'access:%'");assert.equal((await s.rpc(next.token,'ping')).status,401);
});
test('untrusted origins, oversized input and malformed RPC fail closed',async t=>{
 const s=setup();t.after(()=>s.db.close());const {token}=await s.connected();
 assert.equal((await s.run('/mcp',{method:'POST',token,headers:{Origin:'https://evil.test'},json:{}})).status,403);
 assert.equal((await s.run('/mcp',{token})).status,405);
 assert.equal((await s.run('/mcp',{method:'POST',token,json:{x:'x'.repeat(33000)}})).status,413);
 assert.equal((await s.run('/mcp',{method:'POST',token,json:[]})).status,400);
 assert.equal((await s.run('/mcp',{method:'POST',token,json:{jsonrpc:'2.0',method:'notifications/initialized'}})).status,202);
 for(let i=0;i<35;i++)await s.run('/oauth/register',{method:'POST',json:{redirect_uris:[callback]}});
 assert.equal((await s.run('/oauth/register',{method:'POST',json:{redirect_uris:[callback]}})).status,429);
});
