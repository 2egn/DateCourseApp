if(!window.__datecourseDocumentUpdates){
 window.__datecourseDocumentUpdates=true;
 const prefix='datecourseapp.document-read.v1:',memory=new Map();let pending=false;
 function pathOf(href){try{const u=new URL(href,location.href);if(u.origin!==location.origin)return null;let p=decodeURIComponent(u.pathname).replace(/\.(?:html|md)$/i,'').replace(/\/$/,'').toLowerCase();return p==='/index'||!p?'/':p;}catch{return null;}}
 function index(){try{return JSON.parse(document.querySelector('script[data-document-revisions]')?.textContent||'{}');}catch{return {};}}
 function read(path){try{const value=JSON.parse(localStorage.getItem(prefix+path)||'null');if(value&&Number.isSafeInteger(value.revision))return value;}catch{}return memory.get(path);}
 function unread(info,seen){return info.updated&&(!seen||seen.revision<info.revision||(seen.revision===info.revision&&seen.version!==info.version));}
 function markCurrent(){
  if(document.visibilityState!=='visible'||!document.querySelector('article'))return;
  const path=pathOf(location.href),info=index()[path];if(!info)return;
  const seen=read(path);if(seen&&seen.revision>info.revision)return;
  if(seen?.version===info.version)return;
  const value={revision:info.revision,version:info.version};memory.set(path,value);
  try{localStorage.setItem(prefix+path,JSON.stringify(value));}catch{}
 }
 function decorate(){
  pending=false;const versions=index(),states=new Map();
  for(const anchor of document.querySelectorAll('a[href]')){
   const path=pathOf(anchor.getAttribute('href')),info=versions[path];
   if(info&&!states.has(path))states.set(path,unread(info,read(path)));
   const show=!!info&&states.get(path),badge=anchor.querySelector(':scope > .document-unread-dot');
   if(show&&!badge){const dot=document.createElement('span');dot.className='document-unread-dot';dot.setAttribute('aria-hidden','true');dot.title='읽지 않은 업데이트';anchor.append(dot);anchor.setAttribute('data-document-unread','true');anchor.setAttribute('aria-description','읽지 않은 문서 업데이트가 있습니다.');}
   else if(!show&&badge){badge.remove();anchor.removeAttribute('data-document-unread');if(anchor.getAttribute('aria-description')==='읽지 않은 문서 업데이트가 있습니다.')anchor.removeAttribute('aria-description');}
  }
 }
 function schedule(){if(!pending){pending=true;requestAnimationFrame(decorate);}}
 function visit(){markCurrent();schedule();}
 document.addEventListener('nav',visit);
 document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')visit();});
 window.addEventListener('pageshow',visit);
 window.addEventListener('storage',event=>{if(!event.key||event.key.startsWith(prefix))schedule();});
 new MutationObserver(schedule).observe(document.body,{childList:true,subtree:true});
 visit();
}
