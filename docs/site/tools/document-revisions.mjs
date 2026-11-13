import {createHash} from 'node:crypto';

// Track document content, not generated HTML, CSS hashes or rebuild times.
export function updateDocumentRevisions(previous,documents,initialRecent=new Set()){
 if(previous&&previous.schemaVersion!==1)throw Error('Unsupported document revision history.');
 const firstBuild=!previous;
 const entries={...previous?.documents};
 for(const [file,content] of Object.entries(documents)){
  const fingerprint=createHash('sha256').update(content.replaceAll('\r\n','\n')).digest('hex');
  const old=entries[file];
  if(old&&(!Number.isSafeInteger(old.revision)||old.revision<1))throw Error('Invalid document revision: '+file);
  if(!old||old.fingerprint!==fingerprint||old.present===false){
   entries[file]={fingerprint,revision:(old?.revision||0)+1,updated:!firstBuild||initialRecent.has(file),present:true};
  }
 }
 for(const file of Object.keys(entries))if(!(file in documents))entries[file]={...entries[file],present:false};
 return {schemaVersion:1,documents:entries};
}
