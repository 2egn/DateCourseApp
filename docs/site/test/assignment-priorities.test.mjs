import test from 'node:test';import assert from 'node:assert/strict';import {DatabaseSync} from 'node:sqlite';
import {ASSIGNMENT_TASKS,ASSIGNMENT_VERSION,applyAssignmentPriorities} from '../src/assignment-priorities.mjs';
test('assignment priorities cover all 45 tasks, respect text limits, and preserve concurrent changes and states',()=>{
 assert.equal(ASSIGNMENT_TASKS.length,45);assert.equal(new Set(ASSIGNMENT_TASKS.map(t=>t.id)).size,45);
 const db=new DatabaseSync(':memory:');db.exec("CREATE TABLE tasks(id TEXT PRIMARY KEY,priority TEXT,description TEXT,version INTEGER,status TEXT,archived INTEGER,assignee TEXT,dependencies TEXT,updatedAt TEXT,updatedBy TEXT);CREATE TABLE board_meta(key TEXT PRIMARY KEY,value TEXT)");
 const sql={exec(q,...args){const s=db.prepare(q),rows=s.columns().length?s.all(...args):(s.run(...args),[]);return {toArray:()=>rows};}};
 for(const [i,item] of ASSIGNMENT_TASKS.entries()){
  assert.ok(item.after.description.length<=2000,item.id);assert.ok(['high','normal','low'].includes(item.after.priority));
  db.prepare('INSERT INTO tasks VALUES(?,?,?,?,?,?,?,?,?,?)').run(item.id,item.before.priority,item.before.description,item.expectedVersion+(i===0?1:0),i===1?'progress':'todo',i===2?1:0,'teammate','keep-links','old-time','old-author');
 }
 db.prepare('UPDATE tasks SET description=? WHERE id=?').run('팀원이 바꾼 설명',ASSIGNMENT_TASKS[3].id);
 const before=new Map(db.prepare('SELECT * FROM tasks').all().map(t=>[t.id,t]));applyAssignmentPriorities(sql);
 for(const [i,item] of ASSIGNMENT_TASKS.entries()){
  const t=db.prepare('SELECT * FROM tasks WHERE id=?').get(item.id),old=before.get(item.id);
  if(i<4){assert.deepEqual(t,old);continue;}
  assert.equal(t.priority,item.after.priority);assert.equal(t.description,item.after.description);assert.equal(t.version,old.version+1);
  for(const key of ['status','archived','assignee','dependencies'])assert.equal(t[key],old[key]);
 }
 const report=JSON.parse(db.prepare('SELECT value FROM board_meta WHERE key=?').get(ASSIGNMENT_VERSION).value);assert.equal(report.changed.length,41);assert.equal(report.skipped.length,4);
 db.prepare('UPDATE tasks SET priority=? WHERE id=?').run('low','DB-02');const once=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyAssignmentPriorities(sql);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),once);db.close();
});
