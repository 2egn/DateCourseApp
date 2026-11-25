import fs from 'node:fs';
import path from 'node:path';

// 이동표의 경로는 사이트 원본 안의 Markdown 파일만 가리켜야 한다.
export function validateDocumentMoves(moves){
 const targets=new Set();
 for(const [old,next] of Object.entries(moves)){
  for(const file of [old,next])if(!file.endsWith('.md')||path.posix.isAbsolute(file)||file.split('/').some(p=>p==='..'||p==='.'||!p)||file.includes('\\'))throw Error('Invalid document move path');
  if(old===next||targets.has(next)||Object.hasOwn(moves,next))throw Error('Conflicting document move');
  targets.add(next);
 }
 return moves;
}

// 외부 노트 복사본의 옛 파일을 지워 분류되지 않은 원본이 다시 게시되지 않게 한다.
export function removeMovedCopies(output,moves){
 validateDocumentMoves(moves);const root=path.resolve(output)+path.sep;
 for(const [old,next] of Object.entries(moves)){
  const stale=path.resolve(output,old),current=path.resolve(output,next);
  if(!stale.startsWith(root)||!current.startsWith(root)||!fs.statSync(current).isFile())throw Error('Moved document missing: '+next);
  if(fs.existsSync(stale)){if(!fs.statSync(stale).isFile())throw Error('Unexpected document directory');fs.unlinkSync(stale);}
 }
}

// OneDrive에 남은 이전 링크도 현재 게시 경로로 연결한다. 코드 블록은 그대로 둔다.
export function normalizeMovedLinks(text,moves){
 validateDocumentMoves(moves);const names=new Map(),leaves=new Map();
 for(const [old,next] of Object.entries(moves)){
  names.set(old.slice(0,-3),next.slice(0,-3));const leaf=path.posix.basename(old,'.md');leaves.set(leaf,leaves.has(leaf)?null:next.slice(0,-3));
 }
 for(const [leaf,next] of leaves)if(next)names.set(leaf,next);
 return text.split(/(```[\s\S]*?```)/).map((part,i)=>{
  if(i%2)return part;
  return part.replace(/\[\[([^\]]+)\]\]/g,(all,value)=>{
   const bar=value.indexOf('|'),dest=bar<0?value:value.slice(0,bar),label=bar<0?'':value.slice(bar),hash=dest.indexOf('#'),file=hash<0?dest:dest.slice(0,hash),anchor=hash<0?'':dest.slice(hash);
   return names.has(file)?'[['+names.get(file)+anchor+label+']]':all;
  }).replace(/href="\/([^"#?]+)\.html/g,(all,file)=>{
   const entry=Object.entries(moves).find(([old])=>old.slice(0,-3).toLowerCase()===file.toLowerCase());
   return entry?'href="/'+entry[1].slice(0,-3)+'.html':all;
  });
 }).join('');
}

// 문서 변경 차수는 이어받고 옛 항목은 부재 이력으로 남긴다.
export function carryMovedRevisions(previous,moves){
 if(!previous)return previous;validateDocumentMoves(moves);const documents={...previous.documents};
 for(const [old,next] of Object.entries(moves))if(documents[old]&&!documents[next])documents[next]={...documents[old]};
 return {...previous,documents};
}
