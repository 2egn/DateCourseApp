import test from 'node:test';import assert from 'node:assert/strict';
import {updateDocumentRevisions} from '../tools/document-revisions.mjs';
test('baseline highlights recent documents; identical rebuilds keep read versions stable',()=>{
 const state=updateDocumentRevisions(null,{'old.md':'old','recent.md':'recent'},new Set(['recent.md']));
 assert.equal(state.documents['old.md'].updated,false);assert.equal(state.documents['recent.md'].updated,true);
 assert.deepEqual(updateDocumentRevisions(state,{'old.md':'old','recent.md':'recent'}),state);
});
test('only changed/new documents advance; reversions and re-additions count as updates',()=>{
 let state=updateDocumentRevisions(null,{'a.md':'first\n','b.md':'same'});
 state=updateDocumentRevisions(state,{'a.md':'second\n','b.md':'same','c.md':'new'});
 assert.equal(state.documents['a.md'].revision,2);assert.equal(state.documents['b.md'].revision,1);assert.equal(state.documents['c.md'].updated,true);
 state=updateDocumentRevisions(state,{'a.md':'first\r\n','b.md':'same'});
 assert.equal(state.documents['a.md'].revision,3);assert.equal(state.documents['c.md'].present,false);
 state=updateDocumentRevisions(state,{'a.md':'first\n','b.md':'same','c.md':'new'});
 assert.equal(state.documents['a.md'].revision,3);assert.equal(state.documents['c.md'].revision,2);
});
test('invalid revision history fails instead of resetting everyone’s read markers',()=>{
 assert.throws(()=>updateDocumentRevisions({schemaVersion:2},{}));
 assert.throws(()=>updateDocumentRevisions({schemaVersion:1,documents:{'a.md':{revision:-1}}},{'a.md':'a'}));
});
