import test from 'node:test';import assert from 'node:assert/strict';import {DatabaseSync} from 'node:sqlite';
import {TaskBoard} from '../src/board-store.mjs';import {NOV14_VERSION,applyNov14,NOV14_ADDITIONS} from '../src/board-nov14.mjs';
test('current implementation sync keeps teammate ownership and dependencies, skips archives, and runs once',()=>{
 const db=new DatabaseSync(':memory:');
 const sql={exec(q,...args){const s=db.prepare(q);const rows=s.columns().length?s.all(...args):(s.run(...args),[]);return {toArray:()=>rows};}};
 const ctx={storage:{sql,transactionSync(fn){db.exec('BEGIN');try{const value=fn();db.exec('COMMIT');return value;}catch(e){db.exec('ROLLBACK');throw e;}}}};
 new TaskBoard(ctx);
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('DB-03').status,'done');
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('TIME-01').status,'todo');
 db.prepare('DELETE FROM board_meta WHERE key=?').run(NOV14_VERSION);
 db.prepare('UPDATE tasks SET assignee=?,dependencies=?,priority=?,description=? WHERE id=?').run('teammate','DB-01','low','팀원 메모','UI-01');
 db.prepare('UPDATE tasks SET archived=1,status=? WHERE id=?').run('progress','UI-02');
 applyNov14(sql);
 const task=db.prepare('SELECT * FROM tasks WHERE id=?').get('UI-01');
 assert.equal(task.assignee,'teammate');assert.equal(task.dependencies,'DB-01');assert.equal(task.priority,'low');assert.match(task.description,/팀원 메모/);assert.match(task.description,/ea2242b/);
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('UI-02').status,'progress');
 const before=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyNov14(sql);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),before);
 for(const row of db.prepare('SELECT * FROM tasks').all()){assert.ok(row.description.length<=2000);assert.ok(row.title.length<=100);}
 assert.equal(NOV14_ADDITIONS.find(t=>t[0]==='TIME-02')[7],'TIME-01');db.close();
});
