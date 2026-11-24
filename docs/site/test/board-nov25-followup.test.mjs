import test from 'node:test';import assert from 'node:assert/strict';import {DatabaseSync} from 'node:sqlite';
import {TaskBoard} from '../src/board-store.mjs';
import {applyNov25Followup,NOV25_FOLLOWUP_VERSION,NOV25_FOLLOWUP_ADDITIONS,IMPLEMENTATION_REFERENCE} from '../src/board-nov25-followup.mjs';
test('followup records distinguish feature work from applied data and preserve all human work',async()=>{
 const db=new DatabaseSync(':memory:');const sql={exec(q,...args){const s=db.prepare(q),rows=s.columns().length?s.all(...args):(s.run(...args),[]);return{toArray:()=>rows};}};
 const ctx={storage:{sql,transactionSync(fn){db.exec('BEGIN');try{const value=fn();db.exec('COMMIT');return value;}catch(error){db.exec('ROLLBACK');throw error;}}}};
 const board=new TaskBoard(ctx);const data=await(await board.fetch(new Request('https://board.test/api/board',{headers:{'X-Board-User':'fixture'}}))).json();
 assert.deepEqual(data.implementation,IMPLEMENTATION_REFERENCE);assert.equal(IMPLEMENTATION_REFERENCE.scope,'feature');
 assert.equal(data.tasks.find(t=>t.id==='KEYBOARD-POLISH-25').dependencies,'KEYBOARD-UX-25');
 assert.equal(data.tasks.find(t=>t.id==='KEYBOARD-POLISH-25').parentId,'');
 assert.equal(data.tasks.find(t=>t.id==='KEYBOARD-POLISH-25').status,'review');
 assert.match(data.tasks.find(t=>t.id==='JUNGGU-98-25').description,/운영 DB 반영 완료/);
 db.prepare('DELETE FROM board_meta WHERE key=?').run(NOV25_FOLLOWUP_VERSION);
 db.prepare('DELETE FROM tasks WHERE id=?').run('ROUTE-PREF-SYNC-25');
 db.prepare('UPDATE tasks SET description=?,status=?,assignee=?,priority=?,dependencies=?,archived=1,updatedBy=? WHERE id=?').run('팀원 수정','progress','teammate','high','DB-01','teammate','JUNGGU-98-25');
 const before=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyNov25Followup(sql);
 for(const row of before)assert.deepEqual(db.prepare('SELECT * FROM tasks WHERE id=?').get(row.id),row);
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('ROUTE-PREF-SYNC-25').status,'done');
 for(const id of ['KEYBOARD-UX-25','CHECK-01','TIME-02'])assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get(id).status,'todo');
 const after=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyNov25Followup(sql);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),after);
 for(const [id,title,,status,description,acceptance]of NOV25_FOLLOWUP_ADDITIONS){assert.ok(title.length<=100 && description.length<=2000 && acceptance.length<=1000);assert.ok(['done','review'].includes(status));assert.ok(after.some(t=>t.id===id));}
 db.close();
});
