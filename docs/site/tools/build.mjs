import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {spawnSync} from 'node:child_process';
import {extractMethods,enrichMethods,methodNames} from './flow-methods.mjs';
import {optimizeAssets} from './optimize-assets.mjs';
import {normalizeTableLinks} from './table-links.mjs';
import {updateDocumentRevisions} from './document-revisions.mjs';
const site=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),root=path.resolve(site,'../..');
const quartz=path.join(root,'build/docs-tools/quartz');
const source=process.env.DOCS_NOTES_DIR||path.join(process.env.USERPROFILE,'OneDrive/OneSyncFiles/SchoolStudies/Java/프로젝트/DateCourseApp');
if(!fs.existsSync(source))throw Error('DOCS_NOTES_DIR에 문서 원본 폴더를 지정해 주세요.');
const yaml=createRequire(path.join(quartz,'package.json'))('yaml');
const cfg=yaml.parse(fs.readFileSync(path.join(quartz,'quartz.config.default.yaml'),'utf8'));
Object.assign(cfg.configuration,{pageTitle:'DateCourseApp 팀 문서',analytics:null,locale:'ko-KR',baseUrl:'datecourseapp-team-docs.ate-ourse-pp.workers.dev',prefetch:'off'});
Object.assign(cfg.configuration.theme,{fontOrigin:'local',cdnCaching:false});
cfg.configuration.theme.typography={header:'Malgun Gothic',body:'Malgun Gothic',code:'Consolas'};
cfg.configuration.links.markdownLinkResolution='absolute';
cfg.configuration.contentIndex={enableSiteMap:false,enableRSS:false};
for(const p of cfg.plugins)if(['quartz-fonts','og-image','cname','latex','created-modified-date','encrypted-pages','graph','footer','tag-page','note-properties','page-title','content-meta'].some(n=>p.source==='@quartz-community/'+n))p.enabled=false;
fs.writeFileSync(path.join(quartz,'quartz.config.yaml'),yaml.stringify(cfg));
const output=path.resolve(quartz,'team-content');
if(path.dirname(output)!==path.resolve(quartz)||path.basename(output)!=='team-content')throw Error('문서 임시 경로 확인 실패');
fs.rmSync(output,{recursive:true,force:true});fs.mkdirSync(output,{recursive:true});
function copy(dir){for(const item of fs.readdirSync(dir,{withFileTypes:true})){
 const src=path.join(dir,item.name);if(item.isDirectory()){copy(src);continue;}if(!item.name.endsWith('.md'))continue;
 const relative=path.relative(source,src).replace(/^00_/,'');
 if(['팀원_전용_게시_계획.md','작업_대시보드.md'].includes(relative))continue;
 const dest=path.join(output,relative);fs.mkdirSync(path.dirname(dest),{recursive:true});
 let text=fs.readFileSync(src,'utf8').replaceAll('\r\n','\n').replaceAll('[[Java/프로젝트/DateCourseApp/','[[').replace(/\[\[00_/g,'[[').replace(/\|00_/g,'|');
 const title=path.basename(relative,'.md').replaceAll('_',' ');
 if(text.startsWith('---')){const end=text.indexOf('\n---',3);if(end<0)throw Error('잘못된 frontmatter: '+src);const meta=yaml.parse(text.slice(3,end))||{};meta.title=title;text='---\n'+yaml.stringify(meta)+'---'+text.slice(end+4);}
 else text='---\ntitle: '+JSON.stringify(title)+'\n---\n'+text;
 text=text.replace(/\n## 게시와 이후 작업\n[\s\S]*?(?=\n## |$)/g,'');
 if(['02_화면/ConditionController.md','02_화면/RouteEditorController.md','02_화면/PlannerMap.md','05_회원_DB/MembershipApp.md'].includes(relative.replaceAll(path.sep,'/'))){
  text=text.replace(/^(---\n[\s\S]*?\n---\n)/,'$1\n> **버전 안내:** 아래 설명·코드 발췌는 초기 작성본(`ce1cd24`까지의 흐름)이다. 이 클래스는 `d32bc76`에서 내장 서버·도보 경로·로딩 연결이 변경되었다. [[리뷰_및_구현_방향|최신 리뷰와 남은 문제]] 및 [[업데이트_내역|업데이트 내역]]을 함께 확인한다.\n');
 }
 if(relative==='전체_동작_흐름.md')text=text.replace(/^(---\n[\s\S]*?\n---\n)/,'$1\n> **기준 버전:** 이 전체 흐름은 초기 편집 구현 설명이다. `d32bc76`의 내장 지도 서버·TMAP 도보 기능과 다음 구현 순서는 [[리뷰_및_구현_방향|최신 리뷰]]와 [[업데이트_내역|업데이트 내역]]에서 확인한다.\n');
 if(relative==='문서_읽는법.md'){
  text=text.replace(/^- 별도 파일:.*$/m,'- 기본 안내: 실행·테스트, 전체 흐름, 목차, 읽는 법, 라이브러리 안내. JavaScript 지도 보충 노트도 함께 제공한다.');
  text=text.replace(/^- 기존 `프로젝트.md`.*\n/m,'');
  text=text.replace(/^- Java 메인 소스.*$/m,'- 초기 클래스 노트는 Java 메인 55개·테스트 13개 기준이다. d32bc76의 새 클래스 7개를 추가했으며, 기존 테스트 노트가 최신 테스트 전체를 설명한다는 뜻은 아니다. 기준 차이는 [[업데이트_내역|업데이트 내역]]에서 확인한다.');
 }
 if(relative==='목차.md')text=text.replace('## 추천 읽기 순서','처음 실행한다면 [[실행_및_테스트|처음 실행하기와 테스트]]부터 준비한다.\n\n## 추천 읽기 순서');
 if(relative==='목차.md')text+='\n## d32bc76 추가 클래스\n\n기존 클래스 노트의 기준과 변경 범위는 [[업데이트_내역|업데이트 내역]]에서 확인한다.\n\n| 노트 | 역할 |\n|---|---|\n| [[03_처리/MapPageServer|MapPageServer]] | 두 지도가 공유하는 내장 로컬 지도 서버 |\n| [[03_처리/TmapSettings|TmapSettings]] | 도보 서비스 키와 탐색 옵션 설정 |\n| [[03_처리/WalkingRouteService|WalkingRouteService]] | Java에서 도보 경로 요청·응답 처리 |\n| [[02_화면/TmapSettingsDialog|TmapSettingsDialog]] | 도보 탐색 방식 선택 창 |\n| [[04_데이터/WalkingRoute|WalkingRoute]] | 도보 구간·좌표·안내 결과 |\n| [[04_데이터/WalkingSearchOption|WalkingSearchOption]] | 지원하는 탐색 옵션 |\n| [[05_회원_DB/PlannerLoadingDialog|PlannerLoadingDialog]] | 로그인 후 화면 준비 진행 표시 |\n';
 fs.writeFileSync(dest,text);
}}
copy(source);
fs.cpSync(path.join(site,'content'),output,{recursive:true});
fs.mkdirSync(path.join(output,'downloads'),{recursive:true});
fs.copyFileSync(path.join(root,'data/sql/07_saved_courses.sql'),path.join(output,'downloads/07_saved_courses.sql'));
fs.copyFileSync(path.join(root,'data/sql/08_saved_course_price_estimates.sql'),path.join(output,'downloads/08_saved_course_price_estimates.sql'));
const markdownFiles=[];
function collectMarkdown(dir){for(const item of fs.readdirSync(dir,{withFileTypes:true})){const file=path.join(dir,item.name);if(item.isDirectory())collectMarkdown(file);else if(item.name.endsWith('.md')){const text=fs.readFileSync(file,'utf8');if(text.includes('\r\n'))fs.writeFileSync(file,text.replaceAll('\r\n','\n'));markdownFiles.push(file);}}}
collectMarkdown(output);
const all={},unique={};
for(const file of markdownFiles)for(const [name,description] of Object.entries(extractMethods(fs.readFileSync(file,'utf8')))){
 all[path.basename(file,'.md')+'.'+name]=description;
 unique[name]=!(name in unique)||unique[name]===description?description:null;
}
for(const [name,description] of Object.entries(unique))if(description)all[name]=description;
let enriched=0;
for(const file of markdownFiles){const before=fs.readFileSync(file,'utf8'),after=normalizeTableLinks(enrichMethods(before,path.basename(file,'.md'),all));if(before!==after)enriched++;fs.writeFileSync(file,after);}
console.log(`Updated diagram method tables in ${enriched} notes.`);
function removeDuplicateTitles(dir){for(const item of fs.readdirSync(dir,{withFileTypes:true})){
 const file=path.join(dir,item.name);if(item.isDirectory()){removeDuplicateTitles(file);continue;}if(!item.name.endsWith('.md'))continue;
 const text=fs.readFileSync(file,'utf8').replace(/^(---\n[\s\S]*?\n---\n)\s*# [^\n]+\n/,'$1');fs.writeFileSync(file,text);
}}
removeDuplicateTitles(output);
const revisionFile=path.join(site,'document-revisions.json');
const previousRevisions=fs.existsSync(revisionFile)?JSON.parse(fs.readFileSync(revisionFile,'utf8')):null;
const initialRecent=new Set(['목차.md','전체_동작_흐름.md','문서_읽는법.md','02_화면/ConditionController.md','02_화면/RouteEditorController.md','02_화면/PlannerMap.md','05_회원_DB/MembershipApp.md']);
const documentContents={};
for(const file of markdownFiles){const relative=path.relative(output,file).replaceAll(path.sep,'/');documentContents[relative]=fs.readFileSync(file,'utf8');if(fs.existsSync(path.join(site,'content',relative)))initialRecent.add(relative);}
const revisionState=updateDocumentRevisions(previousRevisions,documentContents,initialRecent);
const result=spawnSync(process.execPath,['quartz/bootstrap-cli.mjs','build','-d','team-content','-o',path.join(site,'public'),'--concurrency','1'],{cwd:quartz,stdio:'inherit'});
if(result.status!==0)process.exit(result.status||1);
const contentIndex=JSON.parse(fs.readFileSync(path.join(site,'public/static/contentIndex.json'),'utf8'));
const revisionIndex={};
for(const [slug,note] of Object.entries(contentIndex)){const state=revisionState.documents[note.filePath];if(state?.present)revisionIndex[slug==='index'?'/':'/'+slug]={version:state.revision+'-'+state.fingerprint.slice(0,16),revision:state.revision,updated:state.updated};}
const revisionJson=JSON.stringify(revisionIndex).replaceAll('<','\\u003c');
fs.cpSync(path.join(site,'web/dashboard'),path.join(site,'public/dashboard'),{recursive:true});
fs.mkdirSync(path.join(site,'public/downloads'),{recursive:true});
fs.copyFileSync(path.join(root,'data/sql/07_saved_courses.sql'),path.join(site,'public/downloads/07_saved_courses.sql'));
fs.copyFileSync(path.join(root,'data/sql/08_saved_course_price_estimates.sql'),path.join(site,'public/downloads/08_saved_course_price_estimates.sql'));
fs.cpSync(path.join(site,'web/connect'),path.join(site,'public/connect'),{recursive:true});
fs.copyFileSync(path.join(site,'web/navigation.css'),path.join(site,'public/navigation.css'));
for(const file of ['flow-tooltips.css','flow-tooltips.js','responsive-navigation.js','document-updates.js','document-updates.css','learning-demos.css','learning-demos.js'])fs.copyFileSync(path.join(site,'web',file),path.join(site,'public',file));
const navigation=active=>`<header class="team-navigation"><a class="team-brand" href="/" data-router-ignore>DateCourseApp</a><nav aria-label="사이트 메뉴"><a href="/" data-router-ignore${active==='docs'?' aria-current="page"':''}>문서</a><a href="/dashboard/" data-router-ignore${active==='board'?' aria-current="page"':''}>작업 보드</a><a href="/업데이트_내역.html" data-router-ignore${active==='updates'?' aria-current="page"':''}>업데이트</a><a href="/connect/" data-router-ignore${active==='connect'?' aria-current="page"':''}>Codex 연결</a></nav></header>`;
function addNavigation(dir){for(const item of fs.readdirSync(dir,{withFileTypes:true})){
 const file=path.join(dir,item.name);if(item.isDirectory()){addNavigation(file);continue;}if(!item.name.endsWith('.html'))continue;
 const section=path.relative(path.join(site,'public'),file).split(path.sep)[0];
 const active=section==='dashboard'?'board':section==='connect'?'connect':'docs';
 let html=fs.readFileSync(file,'utf8');html=html.replace('</head>','<link rel="stylesheet" href="/navigation.css"></head>');
 html=html.replace('</head>','<link rel="stylesheet" href="/document-updates.css"><script type="module" src="/document-updates.js" data-persist></script></head>');
 html=html.replace('</body>','<script type="application/json" data-document-revisions>'+revisionJson+'</script></body>');
 if(active==='docs'){
  html=html.replace('</head>','<link rel="stylesheet" href="/flow-tooltips.css"><script type="module" src="/flow-tooltips.js" data-persist></script><script type="module" src="/responsive-navigation.js" data-persist></script><link rel="stylesheet" href="/learning-demos.css"><script type="module" src="/learning-demos.js" data-persist></script></head>');
  const relative=path.relative(path.join(site,'public'),file).replace(/\.html$/,'.md');
  // Quartz normalizes Latin filenames to lowercase. Match the source without
  // changing the user's Obsidian filenames.
  const sourceFile=markdownFiles.find(f=>path.relative(output,f).toLowerCase()===relative.toLowerCase());
  if(sourceFile){const text=fs.readFileSync(sourceFile,'utf8'),summaries=extractMethods(text);
   if(text.includes('```mermaid')&&Object.keys(summaries).length){
    const unresolved=methodNames([...text.matchAll(/```mermaid\n([\s\S]*?)```/g)].map(m=>m[1]).join('\n')).filter(n=>!summaries[n]);
    if(unresolved.length)throw Error('흐름도 설명 누락: '+sourceFile+' '+unresolved.join(', '));
    html=html.replace('</body>','<script type="application/json" data-flow-methods>'+JSON.stringify(summaries).replaceAll('<','\\u003c')+'</script></body>');
   }
  }
 }
 html=html.replace(/<body\b[^>]*>/i,match=>match+navigation(path.basename(file)==='업데이트_내역.html'?'updates':active));fs.writeFileSync(file,html);
}}
addNavigation(path.join(site,'public'));
const index=JSON.parse(fs.readFileSync(path.join(site,'public/static/contentIndex.json'),'utf8'));
const notes=Object.entries(index).filter(([slug,n])=>slug!=='index'&&n.filePath&&fs.existsSync(path.join(output,n.filePath))).map(([slug,n])=>({title:n.title,url:'/'+slug+'.html'})).sort((a,b)=>a.title.localeCompare(b.title,'ko'));
fs.writeFileSync(path.join(site,'public/dashboard/notes.json'),JSON.stringify(notes));
// A build-time Markdown catalog for authenticated read-only agent tools.
// The Worker protects this asset like every other document; it is never public.
const agentDocs=Object.entries(index).filter(([,n])=>n.filePath&&Object.hasOwn(documentContents,n.filePath))
 .map(([slug,n])=>({id:slug,title:n.title,url:slug==='index'?'/':'/'+slug+'.html',
  version:revisionIndex[slug==='index'?'/':'/'+slug]?.version,
  markdown:documentContents[n.filePath].replace(/^---\n[\s\S]*?\n---\n/,'')}))
 .sort((a,b)=>a.title.localeCompare(b.title,'ko'));
fs.mkdirSync(path.join(site,'public/agent'),{recursive:true});
fs.writeFileSync(path.join(site,'public/agent/documents.json'),JSON.stringify(agentDocs));
optimizeAssets(path.join(site,'public'),quartz);
fs.writeFileSync(revisionFile,JSON.stringify(revisionState,null,2)+'\n');
console.log(`Built ${notes.length} notes and shared dashboard. Graph view disabled.`);
