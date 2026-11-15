import test from 'node:test';import assert from 'node:assert/strict';import {DatabaseSync} from 'node:sqlite';
import {TaskBoard} from '../src/board-store.mjs';import {applyNov16,NOV16_VERSION,NOV16_ADDITIONS} from '../src/board-nov16.mjs';
test('latest implementation sync is once-only and preserves human edits, ownership, archives and long descriptions',()=>{
 const db=new DatabaseSync(':memory:');const sql={exec(q,...args){const s=db.prepare(q),rows=s.columns().length?s.all(...args):(s.run(...args),[]);return{toArray:()=>rows};}};
 const ctx={storage:{sql,transactionSync(fn){db.exec('BEGIN');try{const result=fn();db.exec('COMMIT');return result;}catch(error){db.exec('ROLLBACK');throw error;}}}};
 new TaskBoard(ctx);assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('TIME-01').status,'done');
 db.prepare('DELETE FROM board_meta WHERE key=?').run(NOV16_VERSION);
 const template=db.prepare('SELECT * FROM tasks WHERE id=?').get('TIME-01');
 for(const id of ['T-D50D2A41','T-69444E49','T-67B4D1EA']) {
  if(db.prepare('SELECT id FROM tasks WHERE id=?').get(id))continue;
  const row={...template,id,title:id,version:1};const columns=Object.keys(row);
  db.prepare(`INSERT INTO tasks(${columns.join(',')}) VALUES(${columns.map(()=>'?').join(',')})`).run(...Object.values(row));
 }

 db.prepare('UPDATE tasks SET status=?,updatedBy=?,description=? WHERE id=?').run('progress','teammate','팀원 수정','TIME-01');
 db.prepare('UPDATE tasks SET status=?,archived=1,updatedBy=? WHERE id=?').run('progress','구현 동기화 (2egn 요청)','T-D50D2A41');
 db.prepare('UPDATE tasks SET status=?,assignee=?,priority=?,dependencies=?,updatedBy=?,description=? WHERE id=?').run('todo','teammate','high','DB-01','구현 동기화 (2egn 요청)','추가 메모','T-69444E49');
 db.prepare('UPDATE tasks SET updatedBy=?,description=? WHERE id=?').run('구현 동기화 (2egn 요청)','가'.repeat(2000),'T-67B4D1EA');
 applyNov16(sql);
 const human=db.prepare('SELECT * FROM tasks WHERE id=?').get('TIME-01');assert.equal(human.status,'progress');assert.equal(human.description,'팀원 수정');
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('T-D50D2A41').status,'progress');
 const changed=db.prepare('SELECT * FROM tasks WHERE id=?').get('T-69444E49');assert.equal(changed.status,'done');assert.equal(changed.assignee,'teammate');assert.equal(changed.priority,'high');assert.equal(changed.dependencies,'DB-01');assert.match(changed.description,/추가 메모/);assert.match(changed.description,/^두 원/);
 assert.equal(db.prepare('SELECT description FROM tasks WHERE id=?').get('T-67B4D1EA').description,'가'.repeat(2000));
 const before=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyNov16(sql);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),before);
 for(const [id]of NOV16_ADDITIONS)assert.ok(before.some(t=>t.id===id));
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('CHECK-01').status,'todo');
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('TIME-02').status,'todo');db.close();
});
