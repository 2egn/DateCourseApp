import {NOV14_ADDITIONS} from '../src/board-nov14.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {TaskBoard} from '../src/board-store.mjs';
import {TASK_COPY,COPY_VERSION} from '../src/board-copy.mjs';
import {AUDIT_TASKS,AUDIT_ADDITIONS,AUDIT_VERSION} from '../src/board-audit.mjs';
test('branch audit is atomic, version guarded, repeatable, and preserves unrelated task fields',async()=>{
 const {ctx,db}=store();db.exec('DELETE FROM tasks');db.prepare('DELETE FROM board_meta WHERE key=?').run(AUDIT_VERSION);
 for(const item of AUDIT_TASKS){
  const before={title:'기존 작업',description:'설명',acceptance:'확인',dependencies:'',status:'todo',priority:'high',...item.before};
  const conflict=item.id==='T-18071D4A';
  db.prepare('INSERT INTO tasks(id,title,description,acceptance,dependencies,status,area,priority,assignee,relatedNote,archived,version,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)').run(item.id,conflict?'다른 팀원의 수정':before.title,before.description,before.acceptance,before.dependencies,before.status,'backend',before.priority,'teammate','/목차.html',0,item.expectedVersion+(conflict?1:0),'created','updated','author');
 }
 const before=new Map(db.prepare('SELECT * FROM tasks').all().map(t=>[t.id,t]));const board=new TaskBoard(ctx);
 for(const item of AUDIT_TASKS){const row=db.prepare('SELECT * FROM tasks WHERE id=?').get(item.id),old=before.get(item.id);
  if(item.id==='T-18071D4A'){assert.deepEqual(row,old);continue;}
  for(const [key,value] of Object.entries(item.after))assert.equal(row[key],value,item.id+' '+key);
  for(const key of ['area','assignee','relatedNote','archived','createdAt'])assert.equal(row[key],old[key]);
  assert.equal(row.version,old.version+1);
 }
 assert.equal(db.prepare("SELECT status FROM tasks WHERE id='DB-02'").get().status,'todo');
 const report=JSON.parse(db.prepare('SELECT value FROM board_meta WHERE key=?').get(AUDIT_VERSION).value);assert.equal(report.changed.length,50);assert.deepEqual(report.skipped,['T-18071D4A']);assert.equal(report.added.length,4);
 const once=db.prepare('SELECT * FROM tasks ORDER BY id').all();new TaskBoard(ctx);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),once);
 const tasks=(await (await board.fetch(request())).json()).tasks;assert.equal(tasks.find(t=>t.id==='DB-02').parentId,'DB-01');assert.equal(tasks.find(t=>t.id==='T-31FAD2F2').parentId,'DATA-01');db.close();
});
function store(){const db=new DatabaseSync(':memory:');const ctx={storage:{sql:{exec(query,...params){const s=db.prepare(query);const rows=s.columns().length?s.all(...params):(s.run(...params),[]);return {toArray:()=>rows};}},transactionSync(fn){db.exec('BEGIN');try{const r=fn();db.exec('COMMIT');return r;}catch(e){db.exec('ROLLBACK');throw e;}}}};return {ctx,board:new TaskBoard(ctx),db};}
const request=(method='GET',body,path='/api/board',user='2egn')=>new Request('https://docs.test'+path,{method,headers:{'X-Board-User':user,'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)});
test('seed persists once and two instances share create/edit/archive changes',async()=>{const {ctx,board,db}=store();let r=await board.fetch(request());assert.equal((await r.json()).tasks.length,16+AUDIT_ADDITIONS.length+NOV14_ADDITIONS.length);r=await board.fetch(request('POST',{title:'SQL \' ; <script> text',area:'backend'},'/api/board/tasks'));assert.equal(r.status,201);const t=(await r.json()).task;const second=new TaskBoard(ctx);assert.equal((await (await second.fetch(request())).json()).tasks.length,17+AUDIT_ADDITIONS.length+NOV14_ADDITIONS.length);r=await second.fetch(request('PATCH',{version:1,status:'progress'},'/api/board/tasks/'+t.id,'teammate'));assert.equal(r.status,200);assert.equal((await r.json()).task.updatedBy,'teammate');r=await board.fetch(request('PATCH',{version:1,title:'stale'},'/api/board/tasks/'+t.id));assert.equal(r.status,409);for(const [version,archived] of [[2,true],[3,false]]){r=await board.fetch(request('PATCH',{version,archived},'/api/board/tasks/'+t.id));assert.equal(r.status,200);assert.equal(Boolean((await r.json()).task.archived),archived);}db.close();});
test('validates fields, local links, limits and caller',async()=>{const {board,db}=store();for(const body of [{title:''},{title:'x',status:'bad'},{title:'x',unknown:1},{title:'x',relatedNote:'//evil.test/a.html'},{title:'x',relatedNote:'/../a.html'},{title:'x',assignee:'<bad>'},{title:'x'.repeat(101)}])assert.equal((await board.fetch(request('POST',body,'/api/board/tasks'))).status,400);assert.equal((await board.fetch(request('POST',{title:'x'.repeat(9000)},'/api/board/tasks'))).status,413);assert.equal((await board.fetch(request('GET',undefined,'/api/board',''))).status,403);assert.equal((await board.fetch(request('PATCH',{version:1,status:'todo'},'/api/board/tasks/missing'))).status,404);db.close();});

test('wording migration preserves work state and skips concurrent edits; redeploy never reapplies it',()=>{
 const {ctx,db}=store();db.exec('DELETE FROM tasks');db.prepare('DELETE FROM board_meta WHERE key=?').run(COPY_VERSION);
 const statuses=['todo','progress','review','done'],areas=['frontend','backend','infra','docs','data'];
 const insert=db.prepare('INSERT INTO tasks(id,title,description,acceptance,dependencies,status,area,priority,assignee,relatedNote,archived,version,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)');
 for(const [i,item] of TASK_COPY.entries()){
  const changed=item.id==='DB-03';
  insert.run(item.id,changed?'팀원이 방금 바꾼 제목':item.before.title,item.before.description,item.before.acceptance,item.before.dependencies,statuses[i%4],areas[i%5],['high','normal','low'][i%3],'teammate','/목차.html',i%2,item.expectedVersion+(changed?1:0),'created-before','updated-before','original-author');
 }
 insert.run('CUSTOM','팀원이 만든 작업','새 설명','새 기준','', 'progress','backend','high','teammate','',0,4,'created-before','updated-before','original-author');
 const before=new Map(db.prepare('SELECT * FROM tasks').all().map(t=>[t.id,t]));new TaskBoard(ctx);
 for(const task of db.prepare('SELECT * FROM tasks').all()){
  const old=before.get(task.id),copy=TASK_COPY.find(t=>t.id===task.id);
  if(task.id==='DB-03'||task.id==='CUSTOM'){assert.deepEqual(task,old);continue;}
  for(const name of ['status','area','priority','assignee','relatedNote','archived','createdAt'])assert.equal(task[name],old[name],task.id+' '+name);
  for(const [name,value] of Object.entries(copy.after))assert.equal(task[name],value);
  assert.equal(task.version,old.version+1);
 }
 const report=JSON.parse(db.prepare('SELECT value FROM board_meta WHERE key=?').get(COPY_VERSION).value);assert.equal(report.changed.length,52);assert.deepEqual(report.skipped,['DB-03']);
 const once=db.prepare('SELECT * FROM tasks ORDER BY id').all();new TaskBoard(ctx);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),once);
 db.close();
});

test('beginner wording keeps field limits and dependency IDs valid, including fresh installations',async()=>{
 const byId=new Set(TASK_COPY.map(t=>t.id));assert.equal(byId.size,53);
 for(const item of TASK_COPY){
  for(const [name,limit] of Object.entries({title:100,description:2000,acceptance:1000,dependencies:200})){assert.ok(item.after[name].length<=limit,item.id+' '+name);if(name!=='dependencies')assert.ok(item.after[name].trim());}
  for(const id of item.after.dependencies.match(/\b(?:T-[A-F0-9]{8}|[A-Z]+-\d+)\b/g)||[])assert.ok(byId.has(id),id);
 }
 const {board,db}=store();const data=await (await board.fetch(request())).json();const task=data.tasks.find(t=>t.id==='DB-02');assert.equal(task.title,TASK_COPY.find(t=>t.id==='DB-02').after.title);assert.equal(task.version,2);db.close();
});
