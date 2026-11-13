const $=id=>document.getElementById(id),feedback=$('feedback');
if($('server-url')){
 $('server-url').textContent=location.origin+'/mcp';
 $('add-command').textContent='codex mcp add datecourseapp-team --url '+location.origin+'/mcp';
 document.querySelectorAll('[data-copy]').forEach(button=>button.addEventListener('click',async()=>{
  const value=$(button.dataset.copy).textContent;
  try{await navigator.clipboard.writeText(value);feedback.textContent='복사했어요.';}
  catch{feedback.textContent='자동 복사가 안 돼요. 내용을 드래그해서 복사해 주세요.';}
 }));
 async function status(){
  $('refresh-status').disabled=true;
  try{
   const r=await fetch('/api/agent/status');if(!r.ok)throw new Error(r.status===401?'로그인이 만료됐어요. 이 페이지를 새로고침해 주세요.':'연결 정보를 불러오지 못했어요. 잠시 후 다시 눌러 주세요.');
   const data=await r.json(),count=data.connections.length;
   $('connection-state').textContent=data.login+' · 활성 인증 '+count+'개';
   $('connection-detail').textContent=count?'인증은 유효해요. 실제 도구 연결은 위 확인 질문으로 확인하세요.':'아직 활성 인증이 없어요. Codex에서 인증을 진행해 주세요.';
   $('revoke').disabled=!count;
  }catch(e){$('connection-state').textContent=e.message;$('connection-detail').textContent='';$('revoke').disabled=true;}
  finally{$('refresh-status').disabled=false;}
 }
 $('refresh-status').addEventListener('click',status);
 $('revoke').addEventListener('click',()=>$('revoke-dialog').showModal());
 $('revoke-dialog').addEventListener('close',async()=>{
  if($('revoke-dialog').returnValue!=='confirm')return;
  $('revoke').disabled=true;
  try{const r=await fetch('/api/agent/revoke',{method:'POST'});if(!r.ok)throw new Error();feedback.textContent='내 AI 연결을 모두 해제했어요. 다시 쓰려면 Codex에서 인증해 주세요.';await status();}
  catch{feedback.textContent='해제하지 못했어요. 새로고침 후 다시 시도해 주세요.';await status();}
 });
 document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')status();});
 status();
}
