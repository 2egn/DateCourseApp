import test from 'node:test';
import assert from 'node:assert/strict';
import {taskTree,visibleTree} from '../web/dashboard/task-tree.js';
const task=(id,dependencies='',status='todo',area='backend')=>({id,dependencies,status,area});
test('dependency chains have one home, and multiple prerequisites retain a deterministic parent',()=>{
 const tasks=[task('DB-01'),task('DB-02','DB-01'),task('UI-01','DB-02'),task('DB-03','DB-02'),task('UI-02','DB-03'),task('TEST-01','UI-01, UI-02')];
 const tree=taskTree(tasks);assert.deepEqual(tree.roots.map(t=>t.id),['DB-01']);assert.equal(tree.parents.get('TEST-01'),'UI-02');
 const seen=[];function visit(t){seen.push(t.id);for(const child of tree.children.get(t.id))visit(child);}tree.roots.forEach(visit);assert.equal(new Set(seen).size,tasks.length);assert.equal(seen.length,tasks.length);
});
test('search keeps the ancestor path without adding unrelated siblings; missing/archived parents cannot hide children',()=>{
 const tasks=[task('DB-01','','done'),task('DB-02','DB-01'),task('UI-01','DB-02'),task('UI-02','DB-02')];const tree=visibleTree(tasks,[tasks[2]]);
 assert.deepEqual([...tree.ids],['UI-01','DB-02']);assert.equal(tree.roots[0].status,'todo');assert.equal(taskTree([tasks[1]]).roots[0].id,'DB-02');
});
test('different status or area keeps tasks independent, including mixed prerequisites and later state changes',()=>{
 const tasks=[task('DB-01'),task('DB-02','DB-01'),task('UI-01','DB-02','todo','frontend'),task('TEST-01','DB-01, UI-01')];
 assert.equal(taskTree(tasks).parents.get('DB-02'),'DB-01');
 assert.equal(taskTree(tasks).parents.has('UI-01'),false);assert.equal(taskTree(tasks).parents.has('TEST-01'),false);
 tasks[1].status='progress';const tree=taskTree(tasks);assert.equal(tree.parents.has('DB-02'),false);
 assert.equal(visibleTree(tasks,[tasks[1]]).ids.size,1);assert.equal(tasks[1].dependencies,'DB-01');
});
test('cycles and self-dependencies remain visible instead of recursing forever',()=>{
 for(const tasks of [[task('DB-01','DB-01')],[task('DB-01','DB-02'),task('DB-02','DB-01'),task('UI-01','DB-01')]]){
  const tree=taskTree(tasks);assert.equal(tree.roots.length,tasks.length);assert.ok(tree.warnings.size);assert.equal(visibleTree(tasks,tasks).ids.size,tasks.length);
 }
});
