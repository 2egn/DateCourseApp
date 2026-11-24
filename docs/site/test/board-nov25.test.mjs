import test from 'node:test';import assert from 'node:assert/strict';import {DatabaseSync} from 'node:sqlite';
import {TaskBoard} from '../src/board-store.mjs';
import {applyNov25,NOV25_VERSION,NOV25_ADDITIONS,IMPLEMENTATION_REFERENCE} from '../src/board-nov25.mjs';

test('Nov25 adds verified records once while preserving every existing task and unfinished checks',async()=>{
 const db=new DatabaseSync(':memory:');const sql={exec(q,...args){const s=db.prepare(q),rows=s.columns().length?s.all(...args):(s.run(...args),[]);return{toArray:()=>rows};}};
 const board=new TaskBoard({storage:{sql,transactionSync(fn){db.exec('BEGIN');try{const result=fn();db.exec('COMMIT');return result;}catch(error){db.exec('ROLLBACK');throw error;}}}});
 const response=await board.fetch(new Request('https://board.test/api/board',{headers:{'X-Board-User':'fixture'}}));
 assert.deepEqual((await response.json()).implementation,IMPLEMENTATION_REFERENCE);
 assert.equal(IMPLEMENTATION_REFERENCE.commit,'64d93e3');assert.equal(IMPLEMENTATION_REFERENCE.scope,'master');
 db.prepare('DELETE FROM board_meta WHERE key=?').run(NOV25_VERSION);
 db.prepare('DELETE FROM tasks WHERE id=?').run('JDK21-25');
 db.prepare('UPDATE tasks SET description=?,status=?,assignee=?,priority=?,dependencies=?,archived=1,updatedBy=? WHERE id=?').run('팀원 수정','progress','teammate','high','DB-01','teammate','INPUT-FIXES-25');
 const before=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyNov25(sql);
 for(const row of before)assert.deepEqual(db.prepare('SELECT * FROM tasks WHERE id=?').get(row.id),row);
 assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get('JDK21-25').status,'done');
 for(const id of ['KEYBOARD-UX-25','CHECK-01','TIME-02'])assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get(id).status,'todo');
 for(const id of ['MAP-BUFFER-25','UI-VISIBILITY-25'])assert.equal(db.prepare('SELECT status FROM tasks WHERE id=?').get(id).status,'review');
 const after=db.prepare('SELECT * FROM tasks ORDER BY id').all();applyNov25(sql);assert.deepEqual(db.prepare('SELECT * FROM tasks ORDER BY id').all(),after);
 for(const [id,title,,status,evidence,acceptance]of NOV25_ADDITIONS){assert.ok(title.length<=100 && evidence.length<1800 && acceptance.length<=1000);assert.ok(['done','review','todo'].includes(status));assert.ok(after.some(t=>t.id===id));}
 db.close();
});
