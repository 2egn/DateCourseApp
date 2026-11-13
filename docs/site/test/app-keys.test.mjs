import test from 'node:test';
import assert from 'node:assert/strict';
import {handle,seal} from '../src/worker.mjs';
const env={SITE_ORIGIN:'https://docs.example.test',GITHUB_CLIENT_ID:'id',GITHUB_CLIENT_SECRET:'secret',SESSION_KEY:'ab'.repeat(32),
 APP_ACCESS_KEY:'12'.repeat(32),TMAP_APP_KEY:'tmap-test-key',KAKAO_MAP_JAVASCRIPT_KEY:'kakao-test-key',GOOGLE_PLACES_API_KEY:'google-test-key'};
const request=(authorization,method='POST',query='')=>new Request(env.SITE_ORIGIN+'/api/app/keys'+query,{method,headers:authorization?{Authorization:authorization}:{}});
test('shared credential returns only the allowlisted provider keys without public caching',async()=>{
 const response=await handle(request('Bearer '+env.APP_ACCESS_KEY),env);
 assert.equal(response.status,200);
 assert.deepEqual(await response.json(),{version:1,keys:{tmap:env.TMAP_APP_KEY,kakaoJavascript:env.KAKAO_MAP_JAVASCRIPT_KEY,googlePlaces:env.GOOGLE_PLACES_API_KEY}});
 assert.equal(response.headers.get('Cache-Control'),'private, no-store');
 assert.equal(response.headers.get('Access-Control-Allow-Origin'),null);
});
test('missing, malformed and wrong keys fail closed without exposing any credentials',async()=>{
 for(const value of [undefined,'Bearer '+ '34'.repeat(32),'Bearer nope','Basic test']){
  const r=await handle(request(value),env);assert.equal(r.status,401);
  assert.deepEqual(await r.json(),{error:'unauthorized'});
 }
});
test('methods and query-string credentials are rejected',async()=>{
 assert.equal((await handle(request('Bearer '+env.APP_ACCESS_KEY,'GET'),env)).status,405);
 assert.equal((await handle(request('Bearer '+env.APP_ACCESS_KEY,'POST','?key=bad'),env)).status,400);
});
test('missing server configuration never returns a partial key set',async()=>{
 for(const change of [{APP_ACCESS_KEY:''},{TMAP_APP_KEY:''},{GOOGLE_PLACES_API_KEY:undefined}]){
  const r=await handle(request('Bearer '+env.APP_ACCESS_KEY),{...env,...change});assert.equal(r.status,503);
  assert.deepEqual(await r.json(),{error:'service_unavailable'});
 }
});
test('document session and MCP bearer alone cannot retrieve application secrets',async()=>{
 const session=await seal({id:1,token:'github',expires:Date.now()+10000},env,'__Host-dca_session');
 const r=await handle(new Request(env.SITE_ORIGIN+'/api/app/keys',{method:'POST',headers:{Cookie:'__Host-dca_session='+session}}),env);
 assert.equal(r.status,401);
 assert.equal((await handle(request('Bearer mcp-access-token'),env)).status,401);
 assert.equal((await handle(request('Bearer '+env.APP_ACCESS_KEY),{...env,SITE_ORIGIN:'https://other.example.test'})).status,403);
});

const dbEnv={...env,APP_DB_URL:'jdbc:mysql://example.invalid:3306/course?sslMode=REQUIRED',APP_DB_USER:'team',APP_DB_PASSWORD:'  p@ss:word=한글  '};
const dbRequest=authorization=>new Request(env.SITE_ORIGIN+'/api/app/database',{method:'POST',headers:authorization?{Authorization:authorization}:{}});
test('database route uses the same credential but returns no API keys or OAuth secrets',async()=>{
 const r=await handle(dbRequest('Bearer '+env.APP_ACCESS_KEY),dbEnv);
 assert.equal(r.status,200);
 assert.deepEqual(await r.json(),{version:1,database:{url:dbEnv.APP_DB_URL,user:dbEnv.APP_DB_USER,password:dbEnv.APP_DB_PASSWORD}});
 assert.equal(r.headers.get('Cache-Control'),'private, no-store');
 const api=await handle(request('Bearer '+env.APP_ACCESS_KEY),dbEnv);
 assert.equal((await api.json()).database,undefined);
});
test('database route denies missing and wrong credentials, independently of document login',async()=>{
 for(const credential of [undefined,'Bearer '+ '00'.repeat(32),'Bearer mcp-token']) {
  const r=await handle(dbRequest(credential),dbEnv);assert.equal(r.status,401);
  assert.deepEqual(await r.json(),{error:'unauthorized'});
 }
});
test('missing or invalid database configuration fails closed without breaking API-only requests',async()=>{
 for(const change of [{APP_DB_URL:''},{APP_DB_USER:''},{APP_DB_PASSWORD:''},{APP_DB_URL:'jdbc:other://bad'}]){
  const r=await handle(dbRequest('Bearer '+env.APP_ACCESS_KEY),{...dbEnv,...change});
  assert.equal(r.status,503);assert.deepEqual(await r.json(),{error:'service_unavailable'});
  assert.equal((await handle(request('Bearer '+env.APP_ACCESS_KEY),{...dbEnv,...change})).status,200);
 }
});
