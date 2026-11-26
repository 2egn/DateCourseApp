import test from 'node:test';import assert from 'node:assert/strict';import path from 'node:path';import {fileURLToPath} from 'node:url';
import {buildGuideData,readGuideFeature} from '../tools/implementation-guide.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..');
test('guide keeps all twelve feature documents, separates technology and code tables, and links actual sources',()=>{
 const data=buildGuideData(root);assert.equal(data.features.length,12);assert.equal(new Set(data.features.map(f=>f.id)).size,12);assert.equal(data.features.filter(f=>f.group==='use').length,6);
 for(const f of data.features){assert.equal(f.steps.length,4);assert.ok(f.technologies.length>=3&&f.technologies.length<=4,f.id);assert.ok(f.example.length>30);assert.ok(f.code.length>0,f.id);assert.ok(f.limits.length>20);assert.equal(f.documentUrl,'/기능별_구현/'+f.id+'.html');for(const c of f.code)assert.ok(!c.url||c.url.startsWith('https://github.com/2egn/DateCourseApp/'));assert.ok(!f.technologies.some(t=>t.tool==='동작'||/^[-:]+$/.test(t.tool)));}
 assert.equal(data.features.find(f=>f.id==='02-planning').technologies.length,3);assert.equal(data.features.find(f=>f.id==='02-planning').code.length,4);
 assert.equal(data.features.find(f=>f.id==='12-team-and-checks').code.length,6);
});
test('guide fails clearly on missing sections or sources instead of publishing an incomplete feature',()=>{
 assert.throws(()=>readGuideFeature('# Empty',{id:'empty'},{root,sourceRef:'a0d9436'}),/Missing feature summary/);
 const text='**summary**\n\n## 무엇을 사용했고 왜 필요한가\n\n| a | b | c |\n|---|---|---|\n| tool | need | how |\n\n<details><summary>핵심 코드와 연결해서 보기</summary>\n\n| a | b | c |\n|---|---|---|\n| run | [missing](../../../not-a-real-source.java) | action |\n</details>';
 assert.throws(()=>readGuideFeature(text,{id:'empty'},{root,sourceRef:'a0d9436'}),/Missing guide source/);
});
