import {dependencyIds,visibleTree} from './task-tree.js';
const $=s=>document.querySelector(s),el=(tag,cls,text)=>{const e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined)e.textContent=text;return e;};
const expanded=new Set();let hierarchy=null;
let data={tasks:[],statuses:{},areas:{},priorities:{}},area='',editing=null,busy=false,loading=false,dragged=null;
const stateHelp={todo:'아직 시작하지 않은 작업',progress:'지금 만들거나 고치고 있는 작업',review:'만든 기능이 제대로 되는지 확인하는 단계',done:'만들고 확인하는 과정까지 끝난 작업'};
const form=$('#form'),dialog=$('#editor');
const linkedQuery=new URL(location.href).searchParams.get('q');if(linkedQuery)$('#search').value=linkedQuery.slice(0,100);
async function api(path='',method='GET',body){const r=await fetch('/api/board'+path,{method,credentials:'same-origin',headers:body?{'Content-Type':'application/json'}:{},body:body?JSON.stringify(body):undefined});if(r.status===401){location.assign('/auth/login?next=/dashboard/');throw Error('다시 로그인해 주세요.');}let result;try{result=await r.json();}catch{throw Error(r.status===403?'팀원 권한을 확인해 주세요.':'저장소 응답을 확인하지 못했습니다. 잠시 후 새로고침해 주세요.');}if(!r.ok){const e=Error(result.error||'저장하지 못했습니다.');e.status=r.status;throw e;}return result;}
function options(select,values){select.replaceChildren(...Object.entries(values).map(([k,v])=>{const o=el('option','',v);o.value=k;return o;}));}
let notesLoading=false,notesLoaded=false;
async function loadNotes(){if(notesLoading||notesLoaded)return;notesLoading=true;try{const r=await fetch('/dashboard/notes.json');if(!r.ok)return;const notes=await r.json();const existing=new Set(Array.from(form.elements.relatedNote.options,o=>o.value));for(const n of notes)if(!existing.has(n.url)){const o=el('option','',n.title);o.value=n.url;form.elements.relatedNote.append(o);}notesLoaded=true;}catch{}finally{notesLoading=false;}}
function render(){const active=data.tasks.filter(t=>!t.archived),done=active.filter(t=>t.status==='done').length;$('#summary').replaceChildren(...[['전체 작업',active.length],['진행·확인',active.filter(t=>['progress','review'].includes(t.status)).length],['완료',done],['완료율',(active.length?Math.round(done/active.length*100):0)+'%']].map(([label,value])=>{const m=el('div','metric',label);m.append(el('b','',value));return m;}));
 $('#areas').replaceChildren(...Object.entries({'':'전체 영역',...data.areas}).map(([key,label])=>{const b=el('button',area===key?'active':'',label);b.setAttribute('aria-pressed',String(area===key));b.onclick=()=>{area=key;render();};return b;}));
 const q=$('#search').value.toLowerCase().trim(),priority=$('#priority').value,archived=$('#archived').checked;
 const rank={high:0,normal:1,low:2},available=data.tasks.filter(t=>!t.archived||archived).sort((a,b)=>rank[a.priority]-rank[b.priority]||a.id.localeCompare(b.id));
 const tasks=available.filter(t=>(!area||t.area===area)&&(!priority||t.priority===priority)&&[t.title,t.id,t.assignee,t.description,t.acceptance,t.dependencies].join(' ').toLowerCase().includes(q));
 hierarchy=visibleTree(available,tasks);hierarchy.matches=new Set(tasks.map(t=>t.id));hierarchy.filtered=!!(q||area||priority);
 const effectiveRank=t=>Math.min(t.status==='done'?3:rank[t.priority],...(hierarchy.children.get(t.id)||[]).filter(c=>hierarchy.ids.has(c.id)).map(effectiveRank));
 hierarchy.roots.sort((a,b)=>effectiveRank(a)-effectiveRank(b)||a.id.localeCompare(b.id));hierarchy.effectiveRank=effectiveRank;
 $('#board').replaceChildren(...Object.entries(data.statuses).map(([key,label])=>{const c=el('section','column '+key),h=el('h2');h.append(el('span','dot'),document.createTextNode(label),el('span','count',tasks.filter(t=>t.status===key).length));c.append(h,el('p','state-help',stateHelp[key]+' · 숫자는 하위 작업을 포함해요.'));const list=hierarchy.roots.filter(t=>t.status===key);c.append(...list.map(card));if(!list.length)c.append(el('div','empty',tasks.some(t=>t.status===key)?'이 상태의 작업은 상위 카드 안의 하위 목록에 있어요.':'이 상태의 작업이 없어요.'));c.ondragover=e=>{if(busy)return;e.preventDefault();c.classList.add('over');};c.ondragleave=()=>c.classList.remove('over');c.ondrop=async e=>{e.preventDefault();c.classList.remove('over');const task=data.tasks.find(t=>t.id===dragged);if(task&&task.status!==key)await quick(task,{status:key});};return c;}));
}
function dependencyList(value,interactive=true){
 const p=el('p','dependencies');p.append(el('strong','','먼저 할 일: '));let cursor=0;
 for(const match of value.matchAll(/\b(?:T-[A-F0-9]{8}|[A-Z]+-\d+)\b/g)){
  p.append(document.createTextNode(value.slice(cursor,match.index)));const task=data.tasks.find(t=>t.id===match[0]);
  if(task&&interactive){const button=el('button','dependency-link',task.id+' · '+task.title);button.type='button';button.onclick=()=>open(task);p.append(button);}
  else p.append(document.createTextNode(task?task.id+' · '+task.title:match[0]));cursor=match.index+match[0].length;
 }
 p.append(document.createTextNode(value.slice(cursor)));return p;
}
function card(t){
 const c=el('article','card');c.dataset.taskId=t.id;c.draggable=!t.archived&&!busy;
 c.ondragstart=e=>{dragged=t.id;e.dataTransfer.setData('text/plain',t.id);};
 c.ondragend=()=>{dragged=null;document.querySelectorAll('.over').forEach(e=>e.classList.remove('over'));};
 const tags=el('div','tags');tags.append(el('span','tag',data.areas[t.area]),el('span','tag '+t.priority,'중요도 '+data.priorities[t.priority]));
 if(t.archived)tags.append(el('span','tag','보관됨'));
 if(hierarchy.effectiveRank(t)===0&&(t.priority!=='high'||t.status==='done'))tags.append(el('span','tag high','중요도 높은 하위 작업 있음'));
 const h=el('h3'),button=el('button','card-title',t.title);button.onclick=()=>open(t);button.title='설명과 완료 확인 방법 보기·수정';h.append(button);c.append(tags,h);
 if(t.description)c.append(el('p','task-summary',t.description.split('\n')[0]));
 c.append(el('p','',t.assignee?'담당 @'+t.assignee:'담당자 미정'));
 if(t.dependencies)c.append(dependencyList(t.dependencies));
 if(t.relatedNote){const a=el('a','','코드 설명 읽기 ↗');a.href=t.relatedNote;c.append(a);}
 const bottom=el('div','card-bottom'),select=el('select');options(select,data.statuses);select.value=t.status;select.setAttribute('aria-label',t.title+' 상태');select.disabled=busy||!!t.archived;select.onchange=()=>quick(t,{status:select.value});
 bottom.append(el('small','',t.id),select);c.append(bottom,el('p','',`${t.updatedBy} · ${new Date(t.updatedAt).toLocaleDateString('ko-KR')}`));
 if(!hierarchy.matches.has(t.id))c.append(el('p','context-note','검색된 하위 작업의 상위 카드예요.'));
 if(hierarchy.warnings.has(t.id))c.append(el('p','dependency-warning','선행 작업이 서로를 기다리고 있어요. 연결을 확인해 주세요.'));
 const children=childList(t);if(children)c.append(children);return c;
}
function childList(t){
 const children=(hierarchy.children.get(t.id)||[]).filter(c=>hierarchy.ids.has(c.id));if(!children.length)return null;
 const all=[];function collect(task){for(const c of hierarchy.children.get(task.id)||[]){if(hierarchy.ids.has(c.id)){all.push(c);collect(c);}}}collect(t);
 const details=el('details','subtasks'),summary=el('summary','',`하위 작업 ${all.filter(t=>t.status==='done').length}/${all.length} 완료`);
 const preview=el('span','subtask-preview');for(const child of children)preview.append(el('span','subtask-preview-title',child.title));summary.append(preview);
 details.open=hierarchy.filtered||expanded.has(t.id);details.ontoggle=()=>{if(details.open)expanded.add(t.id);else expanded.delete(t.id);};details.append(summary);
 const list=el('ul','subtask-list');for(const child of children)list.append(subtask(child));details.append(list);return details;
}
function subtask(t){
 const row=el('li','subtask');row.dataset.taskId=t.id;row.draggable=false;
 const heading=el('div','subtask-heading'),button=el('button','subtask-title',t.title);button.onclick=()=>open(t);heading.append(button);row.append(heading);
 const tags=el('div','tags');tags.append(el('span','tag',data.areas[t.area]),el('span','tag '+t.priority,data.priorities[t.priority]));row.append(tags);
 if(t.description)row.append(el('p','task-summary',t.description.split('\n')[0]));
 const pending=dependencyIds(t).map(id=>data.tasks.find(t=>t.id===id)).filter(t=>t&&t.status!=='done');
 if(pending.length)row.append(el('p','blocked-note','먼저 완료: '+pending.map(t=>t.title).join(' · ')));
 if(!hierarchy.matches.has(t.id))row.append(el('p','context-note','하위 검색 결과를 보여주기 위한 상위 작업'));
 const controls=el('div','subtask-controls'),select=el('select');options(select,data.statuses);select.value=t.status;select.setAttribute('aria-label',t.title+' 상태');select.disabled=busy||!!t.archived;select.onchange=()=>quick(t,{status:select.value});controls.append(el('small','',t.id),select);row.append(controls);
 const children=childList(t);if(children)row.append(children);return row;
}
async function load(){if(busy||loading||dialog.open)return;loading=true;try{const latest=await api();if(!busy&&!dialog.open){data=latest;$('#message').textContent='최근 갱신 '+new Date().toLocaleTimeString('ko-KR');$('#add').disabled=false;render();}}catch(e){$('#message').textContent=e.message;}finally{loading=false;}}
async function quick(t,values){if(busy)return;busy=true;try{const {task}=await api('/tasks/'+t.id,'PATCH',{version:t.version,...values});data.tasks=data.tasks.map(x=>x.id===task.id?task:x);$('#message').textContent='변경 내용을 저장했어요.';}catch(e){$('#message').textContent=e.message;}finally{busy=false;render();}}
function open(task){if(busy)return;editing=task?{...task}:null;form.reset();for(const [field,values] of [['status',data.statuses],['area',data.areas],['priority',data.priorities]])options(form.elements[field],values);if(task?.relatedNote&&!Array.from(form.elements.relatedNote.options).some(o=>o.value===task.relatedNote)){const o=el('option','',task.relatedNote);o.value=task.relatedNote;form.elements.relatedNote.append(o);}for(const name of ['title','description','acceptance','dependencies','assignee','relatedNote','status','area','priority'])if(task)form.elements[name].value=task[name];if(!task){form.elements.status.value='todo';form.elements.priority.value='normal';}$('#dialog-title').textContent=task?'작업 설명·수정 · '+task.id:'작업 만들기';$('#edit-error').textContent='';$('#archive').hidden=!task;$('#archive').textContent=task?.archived?'목록에 다시 표시':'지금 목록에서 보관';const preview=$('#dependency-preview');preview.replaceChildren();if(task?.dependencies)preview.append(dependencyList(task.dependencies,false));dialog.showModal();loadNotes();}
async function save(values){if(busy)return;busy=true;$('#save').disabled=$('#archive').disabled=$('#close').disabled=true;try{const {task}=await api(editing?'/tasks/'+editing.id:'/tasks',editing?'PATCH':'POST',editing?{version:editing.version,...values}:values);data.tasks=[task,...data.tasks.filter(t=>t.id!==task.id)];$('#message').textContent='변경 내용을 저장했어요.';dialog.close();}catch(e){$('#edit-error').textContent=e.message+(e.status===409?' 입력은 유지됩니다. 창을 닫고 최신 작업을 다시 열어 비교해 주세요.':'');}finally{busy=false;$('#save').disabled=$('#archive').disabled=$('#close').disabled=false;if(!dialog.open)render();}}
form.onsubmit=e=>{e.preventDefault();save(Object.fromEntries(new FormData(form)));};$('#archive').onclick=()=>save({archived:!editing.archived});$('#close').onclick=()=>dialog.close();dialog.addEventListener('cancel',e=>{if(busy)e.preventDefault();});$('#add').onclick=()=>open(null);$('#add').disabled=true;$('#refresh').onclick=load;for(const name of ['search','priority','archived'])$('#'+name).addEventListener('input',render);
let initial=null;try{initial=JSON.parse($('#board-bootstrap')?.textContent||'null');}catch{}
if(initial&&Array.isArray(initial.tasks)&&initial.statuses&&initial.areas&&initial.priorities){data=initial;$('#add').disabled=false;$('#message').textContent='최근 갱신 '+new Date().toLocaleTimeString('ko-KR');render();}else await load();
$('#board-bootstrap')?.remove();
setInterval(()=>{if(document.visibilityState==='visible')load();},30000);
