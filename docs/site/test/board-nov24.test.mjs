import test from 'node:test';import assert from 'node:assert/strict';import {DatabaseSync} from 'node:sqlite';
import {TaskBoard} from '../src/board-store.mjs';
import {IMPLEMENTATION_REFERENCE as LATEST_REFERENCE} from '../src/board-nov25.mjs';
import {applyNov24,NOV24_VERSION,NOV24_ADDITIONS,IMPLEMENTATION_REFERENCE} from '../src/board-nov24.mjs';

test('feature branch sync preserves human changes and incomplete work, and runs only once',async()=>{
 const db=new DatabaseSync(':memory:');const sql={exec(q,...args){const s=db.prepare(q),rows=s.columns().length?s.all(...args):(s.run(...args),[]);return{toArray:()=>rows};}};
 const ctx={storage:{sql,transactionSync(fn){db.exec('BEGIN');try{const result=fn();db.exec('COMMIT');return result;}catch(error){db.exec('ROLLBACK');throw error;}}}};
 const board=new TaskBoard(ctx);
 const response=await board.fetch(new Request('https://board.test/api/board',{headers:{'X-Board-User':'fixture'}}));
 assert.deepEqual((await response.json()).implementation,LATEST_REFERENCE);assert.equal(IMPLEMENTATION_REFERENCE.scope,'feature');
 const template=db.prepare('SELECT * FROM tasks WHERE id=?').get('TIME-01');
 for(const id of ['DATA-01','T-DD5B5813','T-6E4E5509','T-B5F06FB3']){
  if(db.prepare('SELECT id FROM tasks WHERE id=?').get(id))continue;
  const row={...template,id,title:id};const columns=Object.keys(row);
  db.prepare(`INSERT INTO tasks(${columns.join(',')}) VALUES(${columns.map(()=>'?').join(',')})`).run(...Object.values(row));
 }
 db.prepare('DELETE FROM board_meta WHERE key=?').run(NOV24_VERSION);
 db.prepare('UPDATE tasks SET status=?,updatedBy=?,description=? WHERE id=?').run('todo','teammate','팀원 수정','DATA-01');
 db.prepare('UPDATE tasks SET status=?,archived=1,updatedBy=? WHERE id=?').run('todo','구현 동기화 (2026-11-16)','T-DD5B5813');
 db.prepare('UPDATE tasks SET updatedBy=?,description=? WHERE id=?').run('구현 동기화 (2026-11-16)','가'.repeat(2000),'T-6E4E5509');
 db.prepare('UPDATE tasks SET status=?,assignee=?,priority=?,dependencies=?,updatedBy=?,description=? WHERE id=?').run('todo','teammate','high','DB-01','구현 동기화 (2026-11-16)','과거 기록','T-B5F06FB3');
 const ids=['DATA-01','T-DD5B5813','T-6E4E5509',NOV24_ADDITIONS[0][0]];
 db.prepare('UPDATE tasks SET description=?,updatedBy=? WHERE id=?').run('추가된 카드의 팀원 메모','teammate',ids[3]);
 const protectedRows=ids.map(id=>db.prepare('SELECT * FROM tasks WHERE id=?').get(id));
 applyNov24(sql);
 for(const row of protectedRows)assert.deepEqual(db.prepare('SELECT * FROM tasks WHERE id=?').get(row.id),row);
 const changed=db.prepare('SELECT * FROM tasks WHERE id=?').get('T-B5F06FB3');
 assert.equal(changed.status,'done');assert.equal(changed.assignee,'teammate');assert.equal(changed.priority,'high');assert.equal(changed.dependencies,'DB-01');assert.match(changed.description,/master 미반영/);assert.match(changed.description,/과거 기록/);
 const once=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyNov24(sql);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),once);
 for(const [id]of NOV24_ADDITIONS)assert.ok(once.some(t=>t.id===id));
 for(const id of ['CHECK-01','TIME-02'])assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get(id).status,'todo');
 db.close();
});
