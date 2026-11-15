import {createRequire} from 'node:module';
import path from 'node:path';

// Existing Quartz parser; no additional browser framework or dependency.
export function compactDocument(html,quartz){
 const {parse,parseFragment,serialize}=createRequire(path.join(quartz,'package.json'))('parse5');
 const tree=parse(html),find=(node,test)=>test(node)?node:(node.childNodes||[]).map(n=>find(n,test)).find(Boolean);
 const article=find(tree,n=>n.tagName==='article');
 if(!article||article.attrs.some(a=>a.name==='data-compact-document'))return html;
 const text=node=>node.nodeName==='#text'?node.value:(node.childNodes||[]).map(text).join('');
 const children=(node,items)=>{node.childNodes=items;for(const child of items)child.parentNode=node;return node;};
 const element=markup=>parseFragment(markup).childNodes[0];
 const label=(value,limit=160)=>{
  const clean=value.replace(/\s+/g,' ').trim(),sentence=clean.match(/^.*?[.!?](?:\s|$)/)?.[0]?.trim()||clean;
  if(clean.length<=limit)return clean;
  return sentence.length>limit?sentence.slice(0,limit-1)+'…':sentence;
 };
 const plain=value=>({nodeName:'#text',value,parentNode:null});
 const body=find(article,n=>n!==article&&n.tagName==='div'&&n.attrs?.some(a=>a.name==='class'&&a.value.split(/\s+/).includes('markdown-rendered')))||article;
 const source=[...body.childNodes];
 const heading=source.findIndex(n=>n.tagName==='h2'&&/최신 변경|한눈에 보기|하는 일/.test(text(n)));
 const summary=heading>=0?source.slice(heading+1).find(n=>n.tagName==='p'):source.find(n=>n.tagName==='p');
 if(summary){const intro=element('<p class="doc-overview"></p>');children(intro,[plain(label(text(summary)))]);source.unshift(intro);}
 function disclosure(title,content,level){
  const box=element('<details class="doc-section"><summary></summary><div class="doc-section-body"></div></details>');
  if(level===3)box.attrs.push({name:'data-doc-level',value:'nested'});
  children(box.childNodes[0],[title]);children(box.childNodes[1],content);return box;
 }
 function sections(nodes,level){
  const output=[];let title=null,body=[];
  const flush=()=>{if(title)output.push(disclosure(title,level===2?sections(body,3):body,level));else output.push(...body);};
  for(const node of nodes){if(node.tagName==='h'+level){flush();title=node;body=[];}else body.push(node);}
  flush();return output;
 }
 const first=source.findIndex(n=>n.tagName==='h2');
 const prefix=first<0?source:source.slice(0,first),rest=first<0?[]:source.slice(first);
 const overview=prefix.filter(n=>n.attrs?.some(a=>a.name==='class'&&(/(?:^| )doc-overview(?: |$)|(?:^| )doc-quick-links(?: |$)/).test(a.value)));
 const context=prefix.filter(n=>!overview.includes(n)&&!(n===summary&&text(n).replace(/\s+/g,' ').trim().length<=160&&!find(n,c=>c.tagName==='a'))&&!(n.nodeName==='#text'&&!n.value.trim()));
 const content=sections(rest,2);
 if(context.length){const title=element('<span>문서 기준·배경</span>');content.push(disclosure(title,context,2));}
 if(!content.length)return html;
 const controls=element('<div class="doc-controls" aria-label="문서 표시"><span>필요한 설명만 펼쳐 보세요.</span><button type="button" data-doc-expand>모두 펼치기</button><button type="button" data-doc-collapse>모두 접기</button></div>');
 children(body,[...overview,controls,...content]);article.attrs.push({name:'data-compact-document',value:'true'});
 return serialize(tree);
}
