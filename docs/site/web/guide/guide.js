const data=JSON.parse(document.getElementById('guide-data').textContent);
const features=data.features,byId=new Map(features.map(f=>[f.id,f]));
const $=id=>document.getElementById(id);
const groups=[['use','앱 사용 흐름'],['common','함께 동작하는 기능'],['support','실행·자료·협업 기반']];
let currentId='',demoStage=0;
function el(tag,className,text){const n=document.createElement(tag);if(className)n.className=className;if(text!==undefined)n.textContent=text;return n;}
function button(label,id){const b=el('button','',label);b.type='button';b.dataset.feature=id;return b;}
for(const [key,title] of groups){
 $('feature-menu').append(el('p','menu-group',title));const group=el('div','menu-items');
 for(const f of features.filter(x=>x.group===key))group.append(button(f.short,f.id));$('feature-menu').append(group);
}
features.filter(f=>f.group==='use').forEach((f,i)=>{const b=button('',f.id);b.append(el('span','step-number',String(i+1).padStart(2,'0')),el('span','',f.short));$('journey').append(b);});
function documentHref(url){return location.protocol==='file:'&&url.startsWith('/')?'https://datecourseapp-team-docs.ate-ourse-pp.workers.dev'+url:url;}
// 선택한 기능의 설명·기술·코드를 같은 패널에 연결하고 현재 위치를 표시한다.
function render(id,{navigate=false}={}){
 const f=byId.get(id)||byId.get('02-planning');if(navigate&&f.id===currentId)return;currentId=f.id;const index=features.indexOf(f),use=features.filter(x=>x.group==='use');
 if(navigate&&location.hash!=='#'+f.id)history.pushState({feature:f.id},'','#'+f.id);
 document.title=f.short+' · DateCourseApp 구현 안내';
 for(const b of document.querySelectorAll('[data-feature]')){const selected=b.dataset.feature===f.id;if(selected)b.setAttribute('aria-current',b.closest('#journey')?'step':'true');else b.removeAttribute('aria-current');}
 $('feature-position').textContent=groups.find(x=>x[0]===f.group)[1]+(f.group==='use'?' / '+(use.indexOf(f)+1)+'단계 · 전체 '+use.length+'단계':'');
 $('feature-title').textContent=f.title;$('feature-summary').textContent=f.what;$('feature-input').textContent=f.input;$('feature-output').textContent=f.output;$('feature-focus').textContent=f.focus;
 $('feature-flow').replaceChildren(...f.steps.map((s,i)=>{const li=el('li');li.append(el('span','',String(i+1).padStart(2,'0')),el('p','',s));return li;}));
 $('feature-example').textContent=f.example;$('detail-link').href=documentHref(f.documentUrl);$('source-doc-link').href=documentHref(f.documentUrl);$('feature-limits').textContent=f.limits;
 $('feature-tech').replaceChildren(...f.technologies.map(t=>{const card=el('div','tech-card');card.append(el('h4','',t.tool),el('p','why',t.need),el('p','how',t.how));return card;}));
 $('feature-code').replaceChildren(...f.code.map(t=>{const card=el('div','code-card'),body=el('div');body.append(el('code','',t.label),el('p','',t.description));card.append(el('h4','',t.action),body);if(t.url){const a=el('a','','원본 코드 ↗');a.href=t.url;a.target='_blank';a.rel='noopener';card.append(a);}return card;}));
 if(!f.code.length){$('feature-code').append(el('p','hint','관련 서버·빌드 자원은 상세 문서에서 확인할 수 있습니다.'));}
 $('source-ref').textContent=data.sourceRef;
 const next=features[(index+1)%features.length];$('next-caption').textContent='다음 기능 · '+next.short;$('next-feature').dataset.next=next.id;
 const demo=$('recommendation-demo');demo.hidden=f.id!=='03-recommendation';if(!demo.hidden)renderDemo();
}
const places=[{id:'A',distance:200,price:8000,takeout:true},{id:'B',distance:300,price:12000,takeout:true},{id:'C',distance:700,price:6000,takeout:true},{id:'D',distance:400,price:null,takeout:true},{id:'E',distance:450,price:7000,takeout:true,selected:true},{id:'F',distance:350,price:9000,takeout:true},{id:'G',distance:250,price:5000,takeout:null},{id:'H',distance:450,price:9000,takeout:true}];
const stages=['처음 8곳','500m 이내','1만 원 이하','이미 고른 곳 제외','포장 가능 확인'];
function reason(p,stage){if(stage>=1&&p.distance>500)return '500m 밖';if(stage>=2&&p.price===null)return '가격 미확인';if(stage>=2&&p.price>10000)return '가격 초과';if(stage>=3&&p.selected)return '이미 선택';if(stage>=4&&p.takeout!==true)return '포장 미확인';return '';}
// 가상 카페에 조건을 순서대로 적용하는 이유를 보여주며 실제 검색은 하지 않는다.
function renderDemo(){
 const root=$('recommendation-demo');
 // 단계 버튼은 다시 만들지 않아 클릭·키보드 포커스를 그대로 유지한다.
 if(!root.querySelector('.filter-trail')){
  const trail=el('div','filter-trail');trail.setAttribute('aria-label','가상 카페의 조건 적용 단계');
  stages.forEach((name,i)=>{const b=el('button','',name);b.type='button';b.dataset.stage=i;trail.append(b);});
  root.append(trail,el('div','candidate-list'),el('p','filter-result'),el('p','demo-caption','가상 자료 8곳 · 전체 범위·활동·좌표·기본 중복 검사는 통과한 상태'));
  root.querySelector('.filter-result').setAttribute('aria-live','polite');
 }
 for(const b of root.querySelectorAll('[data-stage]'))b.setAttribute('aria-pressed',String(Number(b.dataset.stage)===demoStage));
 const cards=places.map(p=>{const why=reason(p,demoStage),c=el('div','candidate'+(why?' removed':''));c.append(el('strong','','카페 '+p.id),el('small','',p.distance+'m · '+(p.price===null?'가격 미확인':p.price.toLocaleString('ko-KR')+'원')),el('small','',why?'제외 · '+why:'조건 충족'));return c;});
 root.querySelector('.candidate-list').replaceChildren(...cards);
 const remaining=places.filter(p=>!reason(p,demoStage)).sort((a,b)=>a.distance-b.distance);
 root.querySelector('.filter-result').textContent=remaining.length+'곳'+(demoStage===4?' · 거리순 A → F → H · 표시 한도 5곳, 일치 결과 3곳':' 조건 충족');
}

document.addEventListener('click',event=>{
 const b=event.target.closest('[data-feature]');if(b){render(b.dataset.feature,{navigate:true});return;}
 const stage=event.target.closest('[data-stage]');if(stage){demoStage=Number(stage.dataset.stage);renderDemo();}
});
$('next-feature').addEventListener('click',()=>render($('next-feature').dataset.next,{navigate:true}));
$('code-shortcut').addEventListener('click',e=>{e.preventDefault();$('code-section').open=true;$('code-section').scrollIntoView({block:'start'});$('code-section').querySelector('summary').focus({preventScroll:true});});
window.addEventListener('popstate',()=>render(location.hash.slice(1)));
window.addEventListener('hashchange',()=>{if(byId.has(location.hash.slice(1))&&location.hash.slice(1)!==currentId)render(location.hash.slice(1));});
render(location.hash.slice(1));

if(location.protocol==='file:'){for(const a of document.querySelectorAll('a[href]'))if(a.getAttribute('href').startsWith('/'))a.href='https://datecourseapp-team-docs.ate-ourse-pp.workers.dev'+a.getAttribute('href');}
