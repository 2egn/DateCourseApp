if(!window.__datecourseCompactDocs){
 window.__datecourseCompactDocs=true;
 const ancestorDetails=element=>{const result=[];for(let n=element?.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')result.push(n);return result.reverse();};
 function revealHash(){
  if(!location.hash)return;
  let id;try{id=decodeURIComponent(location.hash.slice(1));}catch{return;}
  const target=document.getElementById(id);if(!target)return;
  for(const details of ancestorDetails(target))details.open=true;
  requestAnimationFrame(()=>target.scrollIntoView({block:'start'}));
 }
 document.addEventListener('click',event=>{
  const button=event.target.closest?.('[data-doc-expand],[data-doc-collapse]');
  if(button){const open=button.hasAttribute('data-doc-expand');for(const box of button.closest('article').querySelectorAll('details'))box.open=open;return;}
  const link=event.target.closest?.('a[href]');if(link?.hash)requestAnimationFrame(revealHash);
 });
 document.addEventListener('focusin',event=>{if(event.target.closest?.('summary'))return;for(const details of ancestorDetails(event.target))details.open=true;});
 window.addEventListener('hashchange',revealHash);document.addEventListener('nav',revealHash);
 let printState=[];
 window.addEventListener('beforeprint',()=>{printState=[...document.querySelectorAll('article details')].map(d=>[d,d.open]);for(const [d]of printState)d.open=true;});
 window.addEventListener('afterprint',()=>{for(const [d,open]of printState)if(d.isConnected)d.open=open;printState=[];});
 revealHash();
}
