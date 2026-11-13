import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';
import {extractMethods,enrichMethods} from './flow-methods.mjs';
const baseline=path.resolve('build/docs-before-readable'),output=path.resolve('build/docs-tools/quartz/team-content');
const notes=JSON.parse(fs.readFileSync(new URL('./reading-notes.json',import.meta.url),'utf8'));
const fences=text=>[...text.matchAll(/^```[^\n]*\n[\s\S]*?^```/gm)].map(m=>m[0]);
let total=0,reading=0,examples=0;
function visit(dir){for(const item of fs.readdirSync(dir,{withFileTypes:true})){
 const file=path.join(dir,item.name);if(item.isDirectory()){visit(file);continue;}if(!file.endsWith('.md'))continue;
 const relative=path.relative(baseline,file),before=fs.readFileSync(file,'utf8'),after=fs.readFileSync(path.join(output,relative),'utf8');total++;
 assert.deepEqual(fences(after),fences(before),'Executable excerpt/diagram changed: '+relative);
 for(const field of ['source','source_sha256','source_commit'])assert.equal(after.match(new RegExp('^'+field+': (.*)$','m'))?.[1],before.match(new RegExp('^'+field+': (.*)$','m'))?.[1],relative+' '+field);
 assert.match(after,/^readability_updated: 2026-11-13$/m,relative);
 const stem=path.basename(file,'.md');if(notes[stem]){
  reading++;assert.ok(after.includes(notes[stem].overview),relative);assert.match(after,/## 사용 장면과 읽는 순서/);
  if(!stem.endsWith('.js')){assert.equal((after.match(/^## 주요 메서드/gm)||[]).length,1,relative);assert.deepEqual(Object.keys(extractMethods(after)).sort(),Object.keys(extractMethods(before)).sort(),relative+' function identifiers');}
  assert.equal(enrichMethods(after,stem,{}),after,relative+' enrichment must be idempotent');
 }
 examples+=(after.match(/data-learning="/g)||[]).length;
}}
visit(baseline);assert.equal(total,86);assert.equal(reading,78);assert.equal(examples,17);
console.log(`Readable documents passed: ${total} pages, ${reading} authored walkthroughs, ${examples} visual examples; code/diagrams, method names and provenance preserved.`);
