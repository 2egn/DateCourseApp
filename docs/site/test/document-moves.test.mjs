import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import os from 'node:os';import path from 'node:path';
import {validateDocumentMoves,removeMovedCopies,normalizeMovedLinks,carryMovedRevisions} from '../tools/document-moves.mjs';
import {updateDocumentRevisions} from '../tools/document-revisions.mjs';
const moves={'old.md':'topic/old.md'};
test('moved notes remove stale copies, preserve the target and reject escaping or ambiguous paths',()=>{
 const dir=fs.mkdtempSync(path.join(os.tmpdir(),'datecourse-doc-moves-'));
 try{fs.mkdirSync(path.join(dir,'topic'));fs.writeFileSync(path.join(dir,'old.md'),'obsolete');fs.writeFileSync(path.join(dir,'topic/old.md'),'current');removeMovedCopies(dir,moves);assert.equal(fs.existsSync(path.join(dir,'old.md')),false);assert.equal(fs.readFileSync(path.join(dir,'topic/old.md'),'utf8'),'current');removeMovedCopies(dir,moves);
 for(const invalid of [{'../a.md':'b.md'},{'a.md':'/b.md'},{'a.md':'b.md','b.md':'c.md'},{'a.md':'same.md','b.md':'same.md'}])assert.throws(()=>validateDocumentMoves(invalid));
 assert.throws(()=>removeMovedCopies(dir,{'missing.md':'missing-target.md'}));
 }finally{fs.rmSync(dir,{recursive:true,force:true});}
});
test('legacy links move with anchors and labels while quoted code and ambiguous short names stay unchanged',()=>{
 const source='[[old#part|Label]] <a href="/old.html#part">x</a>\n```js\n[[old]]\n```';
 assert.equal(normalizeMovedLinks(source,moves),'[[topic/old#part|Label]] <a href="/topic/old.html#part">x</a>\n```js\n[[old]]\n```');
 const ambiguous={'a/x.md':'archive/a/x.md','b/x.md':'archive/b/x.md'};assert.equal(normalizeMovedLinks('[[x]] [[a/x]]',ambiguous),'[[x]] [[archive/a/x]]');
});
test('document revision counts continue across moves and remain stable on the next build',()=>{
 const before={schemaVersion:1,documents:{'old.md':{fingerprint:'old',revision:12,updated:true,present:true}}};
 const carried=carryMovedRevisions(before,moves);assert.equal(carried.documents['topic/old.md'].revision,12);assert.equal(before.documents['topic/old.md'],undefined);
 const next=updateDocumentRevisions(carried,{'topic/old.md':'new'});assert.equal(next.documents['topic/old.md'].revision,13);assert.equal(next.documents['old.md'].present,false);assert.deepEqual(updateDocumentRevisions(next,{'topic/old.md':'new'}),next);
});
