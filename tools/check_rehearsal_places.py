"""Bounded identity check. Provider payloads and credentials remain in memory.

One text-search request per selected source place, at most four requests. A lock
prevents accidental repeat calls. Only IDs and locally computed verdicts persist.
"""
import json
import sys
import argparse
import re
from pathlib import Path
from datetime import datetime, timezone

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/place-data-pilot'))
import pilot

NAMES=('대모골','카페이떼','파노라믹커피부산','부산포민속박물관')
def address_matches(local_address, remote_address):
    district=re.search(r'부산(?:광역시)?\s+([가-힣]+[구군])',local_address)
    road=pilot.road_building(local_address)
    return bool('부산' in remote_address and district and district.group(1) in remote_address
                and road and road==pilot.road_building(remote_address))

def main():
    parser=argparse.ArgumentParser();options=parser.add_mutually_exclusive_group()
    options.add_argument('--alternatives',action='store_true');options.add_argument('--district-recheck',action='store_true');args=parser.parse_args()
    batch='district-recheck' if args.district_recheck else 'culture-alternatives' if args.alternatives else 'primary'
    names=('증권박물관(부산관)','한국거래소자본시장역사박물관') if args.district_recheck else ('증권박물관(부산관)','한국거래소자본시장역사박물관','부산여대차박물관') if args.alternatives else NAMES
    output=ROOT/'archive/legacy-place-demo/data/places/demo'/('rehearsal-place-checks-2026-11-14.json' if batch=='primary' else f'rehearsal-place-checks-{batch}-2026-11-14.json')
    if output.exists():raise FileExistsError()
    rows=json.loads((ROOT/'archive/legacy-place-demo/data/places/demo/seomyeon-public-estimates.json').read_text(encoding='utf-8'))['places']
    sample=[next(p for p in rows if p['name']==name) for name in names]
    key=pilot.load_google_key()
    checked=datetime.now(timezone.utc).isoformat()
    with (ROOT/f'.local/rehearsal-identity-{batch}-2026-11-14.lock').open('x',encoding='utf-8') as f:f.write(checked)
    probe=pilot.GoogleProbe(key,max_requests=len(sample))
    outcomes=[]
    for local in sample:
        result=probe.request('POST','places:searchText',
            'places.id,places.displayName,places.formattedAddress,places.location,places.businessStatus,places.types',
            dict(textQuery=local['name']+' '+local['details']['address'],languageCode='ko',regionCode='KR',pageSize=1))
        matches=result.get('places',[]);reasons=[];provider_id=None
        if not matches:reasons.append('NOT_FOUND')
        else:
            remote=matches[0];provider_id=remote['id']
            if pilot.normalized_name(local['name'])!=pilot.normalized_name(remote.get('displayName',{}).get('text','')):reasons.append('NAME_DIFFERS')
            address=remote.get('formattedAddress','')
            local_address=local['details']['address']
            if not address_matches(local_address,address):reasons.append('ADDRESS_DIFFERS')
            point=remote.get('location',{})
            if 'latitude' not in point or 'longitude' not in point or pilot.distance((local['lat'],local['lng']),(point['latitude'],point['longitude']))>200:reasons.append('LOCATION_NOT_CONFIRMED')
            if remote.get('businessStatus')!='OPERATIONAL':reasons.append('OPERATION_NOT_CONFIRMED')
            kinds=set(remote.get('types',[]))
            expected={'MEAL':{'restaurant','food'},'CAFE':{'cafe','coffee_shop'},'CULTURE':{'museum','art_gallery'}}[local['category']]
            if not kinds.intersection(expected):reasons.append('TYPE_NOT_CONFIRMED')
        outcomes.append(dict(publicId=local['details']['externalIds']['public'],name=local['name'],googlePlaceId=provider_id,
            decision='MATCHED_AT_CHECK' if not reasons else 'REVIEW_NEEDED',reasons=reasons))
    doc=dict(checkedAt=checked,requests=probe.calls,rule='normalized_name_road_building_source_district_within_200m_operational_type',
        note='Identity only. Does not validate prices, future operation, walking routes or other places.',places=outcomes)
    with output.open('x',encoding='utf-8') as f:json.dump(doc,f,ensure_ascii=False,indent=2);f.write('\n')
    print(json.dumps(dict(requests=probe.calls,outcomes=[dict(name=r['name'],decision=r['decision'],reasons=r['reasons']) for r in outcomes]),ensure_ascii=False))

if __name__=='__main__':
    try:main()
    except FileExistsError:print('Already checked or locked; no repeat requests made.');sys.exit(1)
    except Exception:print('Identity check could not finish. Credentials and provider responses were not logged.');sys.exit(1)
