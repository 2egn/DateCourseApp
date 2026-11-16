"""Build a retained complete snapshot and review report before district publication. No DB or network."""
import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from build_public_demo import ROOT, DEMO, build, load_policy, read
from place_data_quality import screen, CATEGORIES

DEFAULT_POLICY=ROOT/'data/places/expansion/suyeong-policy.json'

def write(path,value):
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')

def stage(policy_path, output, baseline=DEMO/'seomyeon-public-estimates.json'):
    policy=load_policy(policy_path)
    if not policy.get('district') or not policy.get('pricePolicy'):raise ValueError('District and conservative price policy required')
    source=read('busan_places.csv');by_id={p['place_id']:p for p in source}
    old=json.loads(baseline.read_text(encoding='utf-8'));previous={p['details']['externalIds']['public']:p for p in old['places']}
    if not previous:raise ValueError('A nonempty retained baseline is required')
    rows=[p for p in source if p['category'] in CATEGORIES and (p['district']==policy['district'] or p['place_id'] in previous)]
    review=screen(rows,previous,policy.get('earliestReferenceDate','2024-01-01'))
    output=Path(output);output.mkdir(parents=True,exist_ok=True)
    target=output/'places.json'
    if target.resolve()==baseline.resolve():raise ValueError('Stage output must not overwrite the baseline')
    # Always build from a declared baseline, never accidentally reuse a previous staged expansion.
    write(target,old)
    metadata=build(policy_path,target,review['quarantinedIds'])
    result=json.loads(target.read_text(encoding='utf-8'));current={p['details']['externalIds']['public']:p for p in result['places']}
    if any(current.get(pid)!=place for pid,place in previous.items()):raise ValueError('Existing place changed during expansion')
    additions=[p for pid,p in current.items() if pid not in previous]
    if any(by_id[p['details']['externalIds']['public']]['district']!=policy['district'] for p in additions):raise ValueError('Addition outside selected district')
    report={'schemaVersion':1,'district':policy['district'],'baselineSha256':hashlib.sha256(baseline.read_bytes()).hexdigest(),
            'datasetSha256':hashlib.sha256(target.read_bytes()).hexdigest(),
            'policySha256':hashlib.sha256(policy_path.read_bytes()).hexdigest(),'retained':len(previous),'added':len(additions),'total':len(current),
            'districtSourceRows':sum(p['district']==policy['district'] for p in rows),
            'addedCategories':dict(Counter(p['category'] for p in additions)),
            'addedPriceEvidence':dict(Counter(p['priceEstimate']['evidence'] for p in additions)),
            'pricePolicy':policy['pricePolicy'],'priceCohorts':metadata['cohorts'],
            'retainedPriceReviewIds':[pid for pid,p in previous.items() if p['priceEstimate']['evidence']=='SIMILAR_ESTIMATE' and p['details']['subcategory'] not in policy['pricePolicy']['allowedSubtypes']],
            'priceLimitation':'Inexpensive-designated-shop sample; not representative of ordinary venue prices. No fresh venue or provider verification.',
            'sourceMode':'existing_public_snapshots_offline','quality':review}
    write(output/'quality-report.json',report)
    print(json.dumps({k:report[k] for k in ['district','retained','added','total','addedCategories','addedPriceEvidence']},ensure_ascii=False))
    return report

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--policy',type=Path,default=DEFAULT_POLICY)
    parser.add_argument('--output',type=Path,default=ROOT/'build/district-expansion/suyeong')
    parser.add_argument('--baseline',type=Path,default=DEMO/'seomyeon-public-estimates.json')
    args=parser.parse_args();stage(args.policy,args.output,args.baseline)
