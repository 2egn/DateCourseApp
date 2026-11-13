// Quartz initializes the mobile explorer on navigation, but not on a resize.
// Keep the desktop expansion choice separately from the mobile overlay.
if(!window.__datecourseResponsiveNavigation){
 window.__datecourseResponsiveNavigation=true;
 const mobile=matchMedia('(max-width: 800px)'),desktopState=new WeakMap();
 function setExpanded(explorer,expanded){
  explorer.classList.toggle('collapsed',!expanded);
  explorer.setAttribute('aria-expanded',String(expanded));
  for(const toggle of explorer.querySelectorAll('.explorer-toggle'))toggle.setAttribute('aria-expanded',String(expanded));
  const content=explorer.querySelector('.explorer-content');
  if(content){content.setAttribute('aria-expanded',String(expanded));content.inert=!expanded;}
 }
 function closeMobile(){for(const explorer of document.querySelectorAll('.explorer'))setExpanded(explorer,false);document.documentElement.classList.remove('mobile-no-scroll');}
 mobile.addEventListener('change',event=>{
  for(const explorer of document.querySelectorAll('.explorer')){
   if(event.matches){desktopState.set(explorer,!explorer.classList.contains('collapsed'));setExpanded(explorer,false);}
   else setExpanded(explorer,desktopState.get(explorer)??true);
  }
  document.documentElement.classList.remove('mobile-no-scroll');
 });
 document.addEventListener('nav',()=>{if(mobile.matches)requestAnimationFrame(closeMobile);});
 document.addEventListener('click',event=>{
  const toggle=event.target.closest?.('.explorer-toggle');if(!toggle)return;
  const explorer=toggle.closest('.explorer');if(explorer)setExpanded(explorer,!explorer.classList.contains('collapsed'));
 });
 document.addEventListener('keydown',event=>{
  if(event.key==='Escape'&&mobile.matches&&document.querySelector('.explorer:not(.collapsed)')){
   closeMobile();document.querySelector('.explorer .mobile-explorer')?.focus();
  }
 });
 if(mobile.matches)requestAnimationFrame(closeMobile);
}
