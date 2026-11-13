// Local teaching examples only: no requests, credentials, persistence or app data.
if(!window.__datecourseLearningDemos){
 window.__datecourseLearningDemos=true;
 const mounted=new Map();let serial=0,scheduled=false;
 const el=(tag,cls,text)=>{const e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined)e.textContent=text;return e;};
 const button=(label,action)=>{const b=el('button','learning-button',label);b.type='button';b.addEventListener('click',action);return b;};
 const section=(root,title,caption)=>{root.replaceChildren(el('h3','learning-title',title),el('p','learning-caption',caption));const view=el('div','learning-view'),controls=el('div','learning-controls'),status=el('p','learning-status');status.setAttribute('aria-live','polite');root.append(view,controls,status);return {view,controls,status};};
 const lane=(label,items)=>{const group=el('div','learning-lane');group.append(el('strong','',label));const list=el('ol','learning-route');for(const value of items)list.append(el('li','',value));group.append(list);return group;};
 function draft(root){
  const {view,controls,status}=section(root,'편집본과 원래 코스는 언제 같아질까요?','설명용 코스예요. 버튼으로 편집을 열고 공원을 추가한 뒤 취소·완료를 비교해 보세요.');
  let main=['식당','카페'],copy=null,message='메인에는 확정 코스만 있어요. 편집을 열어 보세요.';
  const start=button('편집 열기',()=>{copy=[...main];message='begin(): 원래 장소 목록을 복사했어요. 메인 코스는 그대로예요.';render();});
  const add=button('편집본에 공원 추가',()=>{if(copy&&!copy.includes('공원'))copy.push('공원');message='편집본만 바뀌었어요. 완료 전까지 메인은 그대로예요.';render();});
  const cancel=button('편집 취소',()=>{copy=null;message='취소: 편집본을 버렸어요. 메인 코스는 바뀌지 않았어요.';render();});
  const finish=button('편집 완료',()=>{main=[...copy];copy=null;message='완료: 편집 결과를 메인에 반영했어요. DB에 영구 저장한 것은 아니에요.';render();});
  const reset=button('처음으로',()=>{main=['식당','카페'];copy=null;message='처음 예시로 돌아왔어요.';render();});controls.append(start,add,cancel,finish,reset);
  function render(){view.replaceChildren(lane('메인 화면의 확정본',main),lane('편집 화면의 복사본',copy||['편집 화면 닫힘']));start.disabled=!!copy;add.disabled=!copy||copy.includes('공원');cancel.disabled=finish.disabled=!copy;status.textContent=message;}
  render();
 }
 function ranges(root){
  const {view,controls,status}=section(root,'두 거리 범위를 모두 만족하는 장소','설명용 가상 위치와 직선 거리예요. 실제 지도·가게 정보는 아니에요. 이전 장소의 허용 거리를 바꿔 보세요.');
  const ns='http://www.w3.org/2000/svg',svg=document.createElementNS(ns,'svg');svg.classList.add('learning-range-chart');svg.setAttribute('role','img');svg.setAttribute('aria-label','출발지 중심 전체 범위와 이전 장소 중심 이동 범위, 그리고 후보 A·B·C·D');
  const legend=el('p','learning-legend','파란 실선: 출발지 전체 700m · 초록 점선: 이전 장소의 이동 범위');
  const list=el('ul','learning-range-results');view.append(svg,legend,list);
  const label=el('label','learning-range-label'),id='learning-radius-'+(++serial),output=el('output');output.htmlFor=id;
  const input=el('input');input.type='range';input.id=id;input.min='150';input.max='650';input.step='50';input.value='400';label.htmlFor=id;label.append('이전 장소에서 허용할 거리 ',output);controls.append(label,input);
  const points=[{id:'A',x:300,y:-70},{id:'B',x:-500,y:-70},{id:'C',x:800,y:-70},{id:'D',x:550,y:190}],previous={x:420,y:-70};
  let width=0;
  function shape(tag,attrs,text){const node=document.createElementNS(ns,tag);for(const [k,v]of Object.entries(attrs))node.setAttribute(k,v);if(text!==undefined)node.textContent=text;return node;}
  function render(){
   const radius=Number(input.value),w=Math.max(220,Math.floor(svg.getBoundingClientRect().width)),h=250,scale=Math.min((w-26)/1880,(h-26)/1520),left=(w-1880*scale)/2,x=value=>left+(value+760)*scale,y=value=>h/2-value*scale;
   width=w;svg.setAttribute('viewBox',`0 0 ${w} ${h}`);svg.replaceChildren();output.value=radius+'m';
   const clipId='learning-intersection-'+id;const defs=shape('defs',{}),clip=shape('clipPath',{id:clipId});clip.append(shape('circle',{cx:x(0),cy:y(0),r:700*scale}));defs.append(clip);svg.append(defs);
   svg.append(shape('circle',{cx:x(previous.x),cy:y(previous.y),r:radius*scale,class:'learning-overlap','clip-path':`url(#${clipId})`}));
   svg.append(shape('circle',{cx:x(0),cy:y(0),r:700*scale,class:'learning-global-circle'}),shape('circle',{cx:x(previous.x),cy:y(previous.y),r:radius*scale,class:'learning-previous-circle'}));
   svg.append(shape('circle',{cx:x(0),cy:y(0),r:4,class:'learning-origin'}),shape('text',{x:x(0)-6,y:y(0)-10,'text-anchor':'end',class:'learning-map-label'},'출발지'));
   svg.append(shape('rect',{x:x(previous.x)-4,y:y(previous.y)-4,width:8,height:8,class:'learning-previous'}),shape('text',{x:x(previous.x)+8,y:y(previous.y)+22,class:'learning-map-label'},'이전 장소'));
   list.replaceChildren();const eligible=[];
   for(const p of points){const global=Math.hypot(p.x,p.y)<=700,local=Math.hypot(p.x-previous.x,p.y-previous.y)<=radius,valid=global&&local;if(valid)eligible.push(p.id);
    const dot=shape('circle',{cx:x(p.x),cy:y(p.y),r:valid?6:4,class:valid?'learning-candidate':'learning-excluded'});dot.append(shape('title',{},`${p.id}: ${valid?'두 범위 통과':!global?'전체 범위 밖':'이전 장소 범위 밖'}`));svg.append(dot,shape('text',{x:x(p.x)+9,y:y(p.y)-9,class:'learning-map-label'},p.id));
    list.append(el('li',valid?'eligible':'',`${p.id} · ${valid?'두 범위 통과':!global?'전체 범위 밖':'이전 장소 범위 밖'}`));
   }
   status.textContent=`거리 조건 통과: ${eligible.join(', ')||'없음'}. 실제 추천에서는 활동·가격·이미 고른 장소도 추가로 확인해요.`;
  }
  input.addEventListener('input',render);render();const observer=new ResizeObserver(()=>{if(Math.floor(svg.getBoundingClientRect().width)!==width)render();});observer.observe(svg);return()=>observer.disconnect();
 }
 function asyncExample(root){
  const {view,controls,status}=section(root,'먼저 보낸 요청이 나중에 도착하면?','다음 단계를 눌러 선택과 응답 순서가 엇갈리는 모습을 확인해 보세요. 실제 서비스에는 요청하지 않아요.');
  const states=[
   {choice:'식당',a:'응답 기다리는 중',b:'아직 요청하지 않음',shown:'아직 없음',text:'식당을 선택하고 상세정보를 요청했어요.'},
   {choice:'카페',a:'응답 기다리는 중',b:'응답 기다리는 중',shown:'아직 없음',text:'응답 전에 카페를 선택했어요. 현재 선택은 이제 카페예요.'},
   {choice:'카페',a:'응답 기다리는 중',b:'도착 → 화면에 반영',shown:'카페 상세정보',text:'카페 응답이 먼저 도착했어요. 현재 선택과 같으므로 반영해요.'},
   {choice:'카페',a:'늦게 도착 → 무시',b:'이미 반영함',shown:'카페 상세정보',text:'이전 식당 응답이 늦게 왔어요. 지금 선택한 카페의 화면을 덮어쓰지 않아요.'}
  ];let step=0;
  const next=button('다음 단계',()=>{step=Math.min(step+1,states.length-1);render();}),reset=button('처음으로',()=>{step=0;render();});controls.append(next,reset);
  function render(){const state=states[step];view.replaceChildren(lane('현재 선택',[state.choice]));const requests=el('dl','learning-requests');for(const [name,value] of [['식당 요청',state.a],['카페 요청',state.b],['현재 화면',state.shown]]){const row=el('div');row.append(el('dt','',name),el('dd','',value));requests.append(row);}view.append(requests);next.disabled=step===states.length-1;status.textContent=`${step+1}/4 · ${state.text}`;}
  render();
 }
 function transaction(root){
  const replacing=root.dataset.context==='replace';
  const {view,controls,status}=section(root,'여러 변경을 함께 확정하거나 취소하기',replacing?'자료 교체의 원리를 단순화한 예시예요. 실제 DB는 바꾸지 않아요. 별도로 만드는 백업은 이 그림의 처리 묶음 밖에 있어요.':'코스 저장이 지켜야 할 규칙을 설명하는 예시예요. 현재 앱의 저장 구현 완료를 뜻하지 않고 실제 DB도 바꾸지 않아요.');
  const steps=replacing?['이전 자료 삭제 요청','새 자료 추가 요청']:['코스 기본 정보 저장 요청','장소 목록 저장 요청'];let mode='ready';
  const success=button('모두 성공한 경우',()=>{mode='success';render();}),failure=button('중간에 실패한 경우',()=>{mode='failure';render();}),reset=button('처음으로',()=>{mode='ready';render();});controls.append(success,failure,reset);
  function render(){
   const flow=el('ol','learning-transaction');for(const [i,label]of steps.entries()){const row=el('li','',label+' · '+(mode==='ready'?'대기':mode==='failure'&&i===1?'오류 발생':'처리'));flow.append(row);}flow.append(el('li','',mode==='ready'?'최종 결정 대기':mode==='success'?'commit · 전부 확정':'rollback · 이번 변경 전부 취소'));
   const result=el('p','learning-saved-result',replacing?(mode==='success'?'최종 저장소: 새 장소 자료':mode==='failure'?'최종 저장소: 교체 전 자료 그대로':'최종 저장소: 교체 전 자료'):(mode==='success'?'최종 저장소: 코스 정보와 장소 목록 모두 있음':mode==='failure'?'최종 저장소: 이번 코스·장소는 어느 쪽도 추가되지 않음':'최종 저장소: 이번 코스는 아직 없음'));
   view.replaceChildren(flow,result);success.setAttribute('aria-pressed',String(mode==='success'));failure.setAttribute('aria-pressed',String(mode==='failure'));
   status.textContent=mode==='ready'?'성공·실패 예시를 골라 최종 저장 결과를 비교해 보세요.':mode==='success'?'변경할 내용이 모두 성공하면 한 묶음으로 확정해요.': '묶음 안에서 실패하면 그 묶음의 변경을 취소해요. 일부만 남은 결과를 성공으로 처리하면 안 돼요.';
  }render();
 }
 function cost(root){
  const {view,controls,status}=section(root,'1인 가격과 전체 인원 금액 구분하기','설명용 가격이에요. 인원을 바꾸면 장소별 금액과 코스 합계가 함께 달라져요.');
  const id='learning-people-'+(++serial),label=el('label');label.htmlFor=id;label.textContent='인원 ';const input=el('input');input.id=id;input.type='number';input.min='1';input.max='6';input.step='1';input.value='2';label.append(input,'명');controls.append(label);
  const won=value=>value.toLocaleString('ko-KR')+'원';
  function render(){const people=Number(input.value);if(!Number.isInteger(people)||people<1||people>6){status.textContent='예시 인원은 1~6명 중 정수로 입력해 주세요.';return;}
   const list=el('ul','learning-cost-list');for(const [name,price]of [['식당',12000],['카페',6000]])list.append(el('li','',`${name} · 1인 ${won(price)} × ${people}명 = ${won(price*people)}`));view.replaceChildren(list,el('p','learning-cost-total','코스 합계 '+won(18000*people)));status.textContent='RoutePlan.totalCost(): 장소별 1인 가격에 인원을 곱한 뒤 모두 더해요. 예전 CoursePlace가 이미 전체 인원 금액을 담은 경우에는 인원을 다시 곱하면 안 돼요.';}
  input.addEventListener('input',render);render();
 }
 const demos={draft,ranges,async:asyncExample,transaction,cost};
 function bind(){
  scheduled=false;
  // DOM morphing may reuse the outer element but replace our controls with
  // the next document's fallback paragraph. That is a fresh example too.
  for(const [root,state]of mounted)if(!root.isConnected||state.type!==root.dataset.learning||!root.querySelector('.learning-view')){state.cleanup?.();mounted.delete(root);}
  for(const root of document.querySelectorAll('.learning-demo[data-learning]')){
   if(mounted.has(root))continue;const setup=demos[root.dataset.learning];
   if(setup){const state={type:root.dataset.learning,cleanup:null};mounted.set(root,state);state.cleanup=setup(root);}
  }
 }
 function schedule(){if(!scheduled){scheduled=true;requestAnimationFrame(bind);}}
 // Quartz can replace the body during navigation. Observe the stable HTML root
 // so local examples also bind when another page module fails before nav fires.
 document.addEventListener('nav',schedule);new MutationObserver(schedule).observe(document.documentElement,{childList:true,subtree:true});schedule();
}
