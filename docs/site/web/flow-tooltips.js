// One listener/observer survives Quartz's document navigation and SVG re-renders.
if(!window.__datecourseFlowTips){
 window.__datecourseFlowTips=true;
 let active=null,scheduled=false;
 const tip=document.createElement('div');tip.id='flow-function-tooltip';tip.className='flow-function-tooltip';tip.role='tooltip';tip.hidden=true;document.body.append(tip);
 function methods(){try{return JSON.parse(document.querySelector('script[data-flow-methods]')?.textContent||'{}');}catch{return {};}}
 const summaries=new WeakMap();
 function hide(){if(active)active.removeAttribute('aria-describedby');active=null;tip.hidden=true;}
 function bind(){scheduled=false;const table=methods();for(const node of document.querySelectorAll('code.mermaid svg g.node')){
  const text=node.querySelector('.nodeLabel')?.textContent||node.textContent;
  const names=[...new Set([...text.matchAll(/([A-Za-z_$][\w.$]*)\s*\(/g)].map(m=>m[1]))];
  const rows=names.filter(n=>table[n]).map(n=>({name:n,description:table[n]}));if(!rows.length)continue;
  summaries.set(node,rows);node.classList.add('flow-method-hint');node.setAttribute('tabindex','0');node.setAttribute('aria-label',rows.map(r=>r.name+'(): '+r.description).join(' '));
 }}
 function schedule(){if(!scheduled){scheduled=true;requestAnimationFrame(bind);}}
 function show(node,event){const rows=summaries.get(node);if(!rows)return;hide();active=node;if(!tip.isConnected)document.body.append(tip);tip.replaceChildren();for(const row of rows){const section=document.createElement('div'),name=document.createElement('strong'),desc=document.createElement('p');name.textContent=row.name+'()';desc.textContent=row.description;section.append(name,desc);tip.append(section);}tip.hidden=false;node.setAttribute('aria-describedby',tip.id);
  const box=node.getBoundingClientRect(),x=event?.clientX??box.left+box.width/2,y=event?.clientY??box.bottom;
  const width=tip.offsetWidth,height=tip.offsetHeight;tip.style.left=Math.max(12,Math.min(x+12,innerWidth-width-12))+'px';tip.style.top=Math.max(12,y+16+height<innerHeight?y+16:y-height-12)+'px';
 }
 document.addEventListener('pointerover',e=>{const n=e.target.closest?.('g.flow-method-hint');if(n&&n!==active)show(n,e);});
 document.addEventListener('pointermove',e=>{const n=e.target.closest?.('g.flow-method-hint');if(n&&tip.hidden)show(n,e);});
 document.addEventListener('pointerout',e=>{if(active&&document.activeElement!==active&&!active.contains(e.relatedTarget))hide();});
 document.addEventListener('focusin',e=>{const n=e.target.closest?.('g.flow-method-hint');if(n)show(n);});
 document.addEventListener('focusout',e=>{if(e.target===active)hide();});
 document.addEventListener('keydown',e=>{if(e.key==='Escape')hide();});
 const moved=()=>{if(active&&document.activeElement===active)show(active);else hide();};
 document.addEventListener('scroll',moved,true);window.addEventListener('resize',moved);
 document.addEventListener('nav',()=>{hide();schedule();});
 new MutationObserver(schedule).observe(document.body,{childList:true,subtree:true});schedule();
}
