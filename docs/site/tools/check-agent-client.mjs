// Independent MCP SDK interoperability check; no real accounts or credentials.
import {DatabaseSync} from 'node:sqlite';import fs from 'node:fs';import path from 'node:path';import {pathToFileURL} from 'node:url';import assert from 'node:assert/strict';
import {AgentAccess} from '../src/agent-store.mjs';import {handle,seal} from '../src/worker.mjs';
const sdk=path.resolve('build/docs-tools/mcp-check/node_modules/@modelcontextprotocol/sdk/dist/esm/client');
const {Client}=await import(pathToFileURL(path.join(sdk,'index.js'))),{StreamableHTTPClientTransport,UnauthorizedError}=await import(pathToFileURL(path.join(sdk,'streamableHttp.js')));
const db=new DatabaseSync(':memory:');let alarm;
const ctx={storage:{sql:{exec(q,...p){const s=db.prepare(q),rows=s.columns().length?s.all(...p):(s.run(...p),[]);return {toArray:()=>rows};}},transactionSync(f){db.exec('BEGIN');try{const r=f();db.exec('COMMIT');return r;}catch(e){db.exec('ROLLBACK');throw e;}},getAlarm:async()=>alarm,setAlarm:async v=>{alarm=v;}}};
const object=new AgentAccess(ctx),origin='https://sdk.test';
const env={SITE_ORIGIN:origin,GITHUB_CLIENT_ID:'test',GITHUB_CLIENT_SECRET:'test',SESSION_KEY:'ab'.repeat(32),AGENT_ACCESS:{idFromName:n=>n,get:()=>object},ASSETS:{fetch:async()=>Response.json(JSON.parse(fs.readFileSync('docs/site/public/agent/documents.json','utf8')))}};
const remote=async()=>Response.json([{id:1390711615}]);
const fetchFn=async(input,init)=>handle(new Request(input,init),env,remote);
let clientInfo,tokens,verifier,authorization;
const provider={redirectUrl:'http://127.0.0.1:44995/callback/_960XD5mylHI',clientMetadata:{client_name:'SDK integration check',redirect_uris:['http://127.0.0.1:44995/callback/_960XD5mylHI'],grant_types:['authorization_code','refresh_token'],response_types:['code'],token_endpoint_auth_method:'none'},
 state:()=> 'sdk-authorization-state',clientInformation:()=>clientInfo,saveClientInformation:v=>{clientInfo=v;},tokens:()=>tokens,saveTokens:v=>{tokens=v;},saveCodeVerifier:v=>{verifier=v;},codeVerifier:()=>verifier,redirectToAuthorization:v=>{authorization=v;}};
let client;
try{
 let transport=new StreamableHTTPClientTransport(new URL(origin+'/mcp'),{authProvider:provider,fetch:fetchFn});
 client=new Client({name:'sdk-check',version:'1.0.0'});
 await assert.rejects(()=>client.connect(transport),UnauthorizedError);assert.ok(authorization);assert.ok(clientInfo);
 const cookie=await seal({id:7,login:'sdk-team',token:'mock-github',expires:Date.now()+3600000},env,'__Host-dca_session');
 const page=await handle(new Request(authorization,{headers:{Cookie:'__Host-dca_session='+cookie}}),env,remote);assert.equal(page.status,200);
 const consent=(await page.text()).match(/name="consent" value="([^"]+)"/)[1];
 const approved=await handle(new Request(origin+'/oauth/authorize',{method:'POST',headers:{Cookie:'__Host-dca_session='+cookie,Origin:origin,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({consent,decision:'allow'})}),env,remote);assert.equal(approved.status,302);
 const callback=new URL(approved.headers.get('Location'));assert.equal(callback.searchParams.get('state'),'sdk-authorization-state');assert.equal(callback.searchParams.get('iss'),origin);
 await transport.finishAuth(callback.searchParams.get('code'));assert.ok(tokens?.access_token);
 transport=new StreamableHTTPClientTransport(new URL(origin+'/mcp'),{authProvider:provider,fetch:fetchFn});client=new Client({name:'sdk-check',version:'1.0.0'});await client.connect(transport);
 assert.equal((await client.listTools()).tools.length,5);
 const status=JSON.parse((await client.callTool({name:'connection_status',arguments:{}})).content[0].text);assert.equal(status.login,'sdk-team');
 const search=JSON.parse((await client.callTool({name:'search_documents',arguments:{query:'RouteEditorController',limit:2}})).content[0].text);assert.ok(search.documents.length);
 const doc=JSON.parse((await client.callTool({name:'read_document',arguments:{id:search.documents[0].id}})).content[0].text);assert.ok(doc.markdown&&doc.version&&doc.url.startsWith(origin));
 const blocked=await client.callTool({name:'delete_document',arguments:{id:doc.id}});assert.equal(blocked.isError,true);
 console.log('MCP SDK passed: metadata discovery, dynamic registration, PKCE/consent, token exchange, initialize, tools/list, search/read actual built Markdown and write rejection.');
}finally{await client?.close();db.close();}
