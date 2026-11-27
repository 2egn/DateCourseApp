import fs from 'node:fs';import path from 'node:path';

function clean(text){return text.replace(/\[([^\]]+)\]\([^)]+\)/g,'$1').replace(/\[\[([^\]|]+)\|([^\]]+)\]\]/g,'$2').replace(/\[\[([^\]]+)\]\]/g,'$1').replaceAll('`','').trim();}
function rows(section){const lines=section.split('\n'),start=lines.findIndex(l=>/^\s*\|/.test(l));if(start<0)return [];let end=start;while(end<lines.length&&/^\s*\|/.test(lines[end]))end++;return lines.slice(start+2,end).map(l=>l.trim().slice(1,-1).split('|').map(x=>x.trim()));}
function section(text,name){const start=text.indexOf('## '+name+'\n');if(start<0)throw Error('Missing feature section: '+name);const body=text.slice(start+name.length+4);const end=body.indexOf('\n## ');return (end<0?body:body.slice(0,end)).trim();}

// 기능 문서의 설명·예시·기술·코드 표를 그대로 사용해 서로 다른 요약본이 생기지 않게 한다.
export function readGuideFeature(markdown,layout,{root,sourceRef}){
 markdown=markdown.replaceAll("\r\n","\n");
 const what=markdown.match(/^\*\*(.+)\*\*$/m)?.[1];if(!what)throw Error('Missing feature summary: '+layout.id);
 const technologies=rows(section(markdown,'무엇을 사용했고 왜 필요한가')).map(r=>{if(r.length!==3)throw Error('Invalid technology table');return {tool:clean(r[0]),need:clean(r[1]),how:clean(r[2])};});
 const detail=markdown.match(/<details><summary>핵심 코드와 연결해서 보기<\/summary>([\s\S]*?)<\/details>/)?.[1]||'';
 const sourceUrl=relative=>{const file=relative.replace(/^\.\.\/\.\.\/\.\.\//,'');const full=path.resolve(root,file);if(!full.startsWith(path.resolve(root)+path.sep)||!fs.existsSync(full))throw Error('Missing guide source: '+file);return 'https://github.com/2egn/DateCourseApp/'+(fs.statSync(full).isDirectory()?'tree/':'blob/')+sourceRef+'/'+file;};
 const code=rows(detail).map(r=>{if(r.length!==3)throw Error('Invalid code table');const href=r[1].match(/\]\(([^)]+)\)/)?.[1];return {action:clean(r[0]),label:clean(r[1]),description:clean(r[2]),url:href?sourceUrl(href):null};});
 for(const match of detail.matchAll(/관련 자원: \[([^\]]+)\]\(([^)]+)\)/g))code.push({action:'관련 자원',label:match[1],description:'해당 기능에 사용되는 자원·설정·도구',url:sourceUrl(match[2])});
 return {...layout,title:clean(markdown.match(/^# (.+)$/m)?.[1]||layout.short),what:clean(what),example:clean(section(markdown,'예시로 이해하기')),technologies,code,limits:clean(section(markdown,'현재 구현의 범위').split('\n\n관련 설명:')[0]),documentUrl:'/기능별_구현/'+layout.id+'.html'};
}
export function buildGuideData(root){
 const dir=path.join(root,'docs/site/web/guide');const layout=JSON.parse(fs.readFileSync(path.join(dir,'layout.json'),'utf8').replace(/^\uFEFF/,''));
 const sourceRef=fs.readFileSync(path.join(root,'docs/implementation-reference/README.md'),'utf8').match(/소스 기준 `([^`]+)`/)?.[1];if(!sourceRef)throw Error('Missing guide source reference');
 if(layout.length!==12||new Set(layout.map(x=>x.id)).size!==12)throw Error('Expected twelve unique guide features');
 const features=layout.map(l=>{if(!/^[0-9]{2}-[a-z-]+$/.test(l.id)||!['use','common','support'].includes(l.group)||l.steps.length!==4)throw Error('Invalid guide layout');return readGuideFeature(fs.readFileSync(path.join(root,'docs/implementation-reference/features',l.id+'.md'),'utf8'),l,{root,sourceRef});});
 return {sourceRef,features};
}
export function writeImplementationGuide(root){
 const dir=path.join(root,'docs/site/web/guide'),publicDir=path.join(root,'docs/site/public');const data=buildGuideData(root);
 const text=fs.readFileSync(path.join(dir,'index.html'),'utf8').replace('/* GUIDE_CSS */',()=>fs.readFileSync(path.join(dir,'guide.css'),'utf8')).replace('/* GUIDE_DATA */',()=>JSON.stringify(data).replaceAll('<','\\u003c')).replace('/* GUIDE_JS */',()=>fs.readFileSync(path.join(dir,'guide.js'),'utf8'));
 fs.copyFileSync(path.join(publicDir,'index.html'),path.join(publicDir,'reference.html'));fs.writeFileSync(path.join(publicDir,'index.html'),text);
 const preview=path.join(root,'build/ui-preview/implementation-guide.html');fs.mkdirSync(path.dirname(preview),{recursive:true});fs.writeFileSync(preview,text);
 return data;
}
