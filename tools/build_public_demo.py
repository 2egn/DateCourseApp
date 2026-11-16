"""Build a separate public-place demonstration dataset. Never imports into MySQL.

Prices derived from public menu amounts assume a single-person serving. This is
explicitly an estimate, not confirmation of a venue's menu or current business.
"""
import argparse
import unicodedata
import re
import csv
import json
import math
import statistics
import hashlib
from collections import defaultdict, Counter, deque
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'data/places/public/busan'
DEMO=ROOT/'data/places/demo'
POLICY=DEMO/'selection-policy.json'
def read(name):
    with (DATA/name).open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))
def dist(p,center=(35.1578,129.06)):
    a,b,c,d=map(math.radians,(*center,float(p['lat']),float(p['lon'])))
    h=math.sin((c-a)/2)**2+math.cos(a)*math.cos(c)*math.sin((d-b)/2)**2
    return 12742000*math.asin(min(1,math.sqrt(h)))

def valid_point(p):
    try:
        lat,lng=float(p['lat']),float(p['lon'])
        return math.isfinite(lat) and math.isfinite(lng) and -90<=lat<=90 and -180<=lng<=180
    except (ValueError,TypeError,KeyError):
        return False


def identity_key(p):
    def clean(value):return re.sub(r'\s+','',unicodedata.normalize('NFKC',value).casefold())
    return clean(p['name']),clean(p['road_address'] or p['lot_address'])


def load_policy(path):
    policy=json.loads(path.read_text(encoding='utf-8'))
    center=policy.get('center',{})
    if policy.get('schemaVersion')!=1 or not valid_point({'lat':center.get('lat'),'lon':center.get('lng')}):
        raise ValueError('Invalid selection center or schema version')
    radius=policy.get('radiusMeters')
    if isinstance(radius,bool) or not isinstance(radius,(int,float)) or not math.isfinite(radius) or radius<=0:
        raise ValueError('Invalid selection radius')
    limits=policy.get('categoryLimits',{})
    if set(limits)!={'MEAL','CAFE','CULTURE'} or any(type(n)!=int or n<1 for n in limits.values()):
        raise ValueError('Positive category limits are required')
    for name in ['preferredPerCuisine','preferredDesignated']:
        if type(policy.get(name))!=int or policy[name]<0:raise ValueError('Invalid preference count')
    if 'district' in policy and (not isinstance(policy['district'],str) or not policy['district'].strip()):
        raise ValueError('A district must be a nonempty name')
    if 'pricePolicy' in policy:
        price=policy['pricePolicy']
        if type(price.get('minimumSamples'))!=int or price['minimumSamples']<1 or not isinstance(price.get('allowedSubtypes'),list) or not all(isinstance(v,str) and v for v in price['allowedSubtypes']):
            raise ValueError('Invalid price cohort policy')
    return policy


def select_places(near,previous_ids,checked_ids,blocked,good,group,policy):
    limits=policy['categoryLimits'];byid={p['place_id']:p for p in near}
    chosen={};seen=set();counts=Counter()
    def add(p,required=False):
        if p['place_id'] in chosen:return True
        key=identity_key(p)
        allowed=p['place_id'] not in blocked and key not in seen and counts[p['category']]<limits[p['category']]
        if not allowed:
            if required:raise ValueError('Existing place conflicts with selection policy: '+p['place_id'])
            return False
        seen.add(key);chosen[p['place_id']]=p;counts[p['category']]+=1;return True
    for pid in previous_ids:
        if pid not in byid:raise ValueError('Existing place is outside the available source: '+pid)
        add(byid[pid],True)
    for pid in checked_ids:
        if pid in byid:add(byid[pid])
    for kind in ['한식','중식','일식']:
        for p in [p for p in near if group(p)==kind][:policy['preferredPerCuisine']]:add(p)
    for p in [p for p in near if p['place_id'] in good][:policy['preferredDesignated']]:add(p)
    # Round-robin actual source subtypes, rather than filling one popular subtype first.
    pools=defaultdict(deque)
    for p in near:pools[(p['category'],p['industry_name'])].append(p)
    while pools:
        for key in list(pools):
            pool=pools[key]
            if counts[key[0]]>=limits[key[0]]:del pools[key];continue
            while pool:
                if add(pool.popleft()):break
            if not pool:del pools[key]
    return list(chosen.values())


def build(policy_path=POLICY,target=DEMO/'seomyeon-public-estimates.json',excluded_ids=()):
    policy=load_policy(policy_path);center=(policy['center']['lat'],policy['center']['lng'])
    previous=json.loads(target.read_text(encoding='utf-8'))['places'] if target.exists() else []
    previous_by_id={p['details']['externalIds']['public']:p for p in previous}

    manifest=json.loads((DATA/'manifest.json').read_text(encoding='utf-8'))
    for name in ['busan_places.csv','busan_good_price_shops.csv','busan_menu_prices.csv']:
        if hashlib.sha256((DATA/name).read_bytes()).hexdigest()!=manifest['sha256'][name]:
            raise ValueError('Public input checksum mismatch: '+name)
    places=read('busan_places.csv');shops=read('busan_good_price_shops.csv');menus=read('busan_menu_prices.csv')
    byid={p['place_id']:p for p in places}
    checked_snapshot=json.loads((ROOT/'data/places/demo/seomyeon-jeonpo-demo.json').read_text(encoding='utf-8'))
    checked={p['details']['externalIds']['public']:p['details'] for p in checked_snapshot['places']}
    checks={}
    documents=[json.loads(p.read_text(encoding='utf-8')) for p in (ROOT/'data/places/demo').glob('rehearsal-place-checks-*.json')]
    for document in sorted(documents,key=lambda d:d['checkedAt']):
        for row in document['places']:checks[row['publicId']]=dict(row,checkedAt=document['checkedAt'])
    shopids={s['price_shop_id']:s['linked_place_id'] for s in shops if s['linked_place_id']}
    good=set(shopids.values())
    # Narrow menu whitelist: no shared platters, weights, sets or hairdressing prices.
    allow={'한식':{'돼지국밥','순대국밥','소고기국밥','김치찌개','된장찌개','순두부찌개','비빔밥','물밀면','비빔밀면','칼국수','잔치국수'},
           '중식':{'짜장면','자장면','짬뽕','간짜장','볶음밥'},
           '일식':{'우동','돈까스','돈가스','모둠초밥(포장)','모듬초밥(점심)','초밥7+데마끼','냉모밀','새우튀김우동','오무라이스','카레라이스','모밀국수','유부초밥'},
           '카페':{'아메리카노','카페라떼'}}
    def group(p):
        s=p['industry_name']
        if p['category']=='CAFE':return '카페'
        if any(w in s for w in ['일식','초밥','돈가스']):return '일식'
        if any(w in s for w in ['중국','중식','마라']):return '중식'
        # Shared meat/seafood dishes do not inherit a single-bowl meal estimate.
        if any(w in s for w in ['백반','한식','국/','밀면','국수']):return '한식'
        return None
    sample=defaultdict(lambda:defaultdict(set))
    for m in menus:
        pid=shopids.get(m['price_shop_id']); p=byid.get(pid)
        if not p or not m['price_krw']:continue
        for kind,names in allow.items():
            if m['menu_name'].strip() in names and group(p)==kind and p['category']==('CAFE' if kind=='카페' else 'MEAL'):
                sample[kind][pid].add(int(m['price_krw']))
    estimates={}
    for kind,rows in sample.items():
        if len(rows)>=5:
            # Each unique venue contributes once regardless of menu count.
            amount=int(round(statistics.mean(statistics.median(v) for v in rows.values())/100)*100)
            estimates[kind]=(amount,len(rows))
    near=[p for p in places if valid_point(p) and p['category'] in policy['categoryLimits']
          and (p['place_id'] in previous_by_id or (p['district']==policy['district'] if policy.get('district') else dist(p,center)<=policy['radiusMeters']))]
    near.sort(key=lambda p:(dist(p,center),p['place_id']))
    selected=select_places(near,list(previous_by_id),list(checked),
        {pid for pid,row in checks.items() if row['decision']=='REVIEW_NEEDED'} | set(excluded_ids),good,group,policy)
    category_counts=Counter(p['category'] for p in selected)
    next_record_id=max((p['recordId'] for p in previous),default=2000000)+1
    out=[]
    for p in selected:
        if p['place_id'] in previous_by_id:
            out.append(previous_by_id[p['place_id']]);continue
        record_id=previous_by_id.get(p['place_id'],{}).get('recordId')
        if record_id is None:record_id=next_record_id;next_record_id+=1
        price=dict(minimum=None,maximum=None,unit='UNKNOWN',basis='확인 가능한 단위별 표본 부족',evidence='UNKNOWN',sourceUrl='',checkedAt='')
        g=group(p)
        pricing=policy.get('pricePolicy')
        if g in estimates and (not pricing or (estimates[g][1]>=pricing['minimumSamples'] and p['industry_name'] in pricing['allowedSubtypes'])):
            amount,n=estimates[g]
            price=dict(minimum=amount,maximum=amount,unit='PER_DRINK' if g=='카페' else 'PER_PERSON',
                evidence='SIMILAR_ESTIMATE',sourceUrl='https://www.data.go.kr/data/3045247/fileData.do',checkedAt='2026-09-30',
                basis=f'부산 착한가격업소 {g} 고유 장소 {n}곳 · 기준 메뉴를 장소별 중앙값으로 묶은 평균 · 1인분/1잔 가정. 일반 가게보다 낮을 수 있으며 해당 매장 가격 확인값이 아님.')
        elif p['price_basis']=='adult_admission' and p['price_krw']:
            amount=int(p['price_krw']);price=dict(minimum=amount,maximum=amount,unit='ADULT_TICKET',basis='공공자료 성인 기본 관람료 · 특별전/프로그램 제외',evidence='REFERENCE',sourceUrl=p['source_url'],checkedAt=p['source_reference_date'])
        tags=[]
        if p['place_id'] in good:tags.append(dict(tag='GOOD_PRICE',basis='공공 지정자료와 장소 ID 연결 · 지정 현황은 자료 기준일 참고',sourceUrl='https://www.data.go.kr/data/3045247/fileData.do',checkedAt='2026-09-30'))
        if price['minimum']==0 and price['evidence']=='REFERENCE':tags.append(dict(tag='FREE_ADMISSION',basis='성인 기본 관람료 0원 · 별도 프로그램 제외',sourceUrl=p['source_url'],checkedAt=p['source_reference_date']))
        identity=checked.get(p['place_id'],{})
        verification=checks.get(p['place_id'],{})
        external_ids={'public':p['place_id']}
        if identity.get('externalIds',{}).get('google'):external_ids['google']=identity['externalIds']['google']
        if verification.get('decision')=='MATCHED_AT_CHECK':
            external_ids['google']=verification['googlePlaceId']
            identity=dict(identity,identityCheckedAt=verification['checkedAt'])
        out.append(dict(recordId=record_id,placeId='DCA-DEMO-PUBLIC-'+p['place_id'],name=p['name']+(' '+p['branch'] if p['branch'] else ''),category=p['category'],lat=float(p['lat']),lng=float(p['lon']),priceEstimate=price,rating=0,
            details=dict(address=p['road_address'] or p['lot_address'],subcategory=p['industry_name'],phone=p['phone'],website=p['website'],sourceUrl=p['source_url'],sourceDate=p['source_reference_date'],identityCheckedAt=identity.get('identityCheckedAt',''),verificationUrl=identity.get('verificationUrl',''),externalIds=external_ids,tags=tags)))
    doc=dict(schemaVersion=1,title=(policy['district']+' 확대 공공자료 · 추정 가격') if policy.get('district') else '서면 공공자료 · 추정 가격',startLat=center[0],startLng=center[1],
        note='Real public-source place records. Only marked places have recorded identity checks; review-needed places are held out. Costs are not Google-verified. Similar-place estimates assume per-person portions; inexpensive-shop sample is biased. Original nine-place dataset is unchanged.',places=out)
    target.write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'places':len(out),'categories':dict(category_counts),'estimate_samples':estimates,'estimated':sum(p['priceEstimate']['evidence']=='SIMILAR_ESTIMATE' for p in out),'unknown':sum(p['priceEstimate']['minimum'] is None for p in out)},ensure_ascii=False))
    return {'cohorts':{kind:{'amount':amount,'sampleSize':n,'aggregation':'mean_of_per_place_menu_medians','source':'busan_good_price_shops','bias':'inexpensive_designated_shops','unit':'PER_DRINK' if kind=='카페' else 'PER_PERSON'} for kind,(amount,n) in estimates.items()},'places':len(out)}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--policy',type=Path,default=POLICY)
    parser.add_argument('--output',type=Path,default=DEMO/'seomyeon-public-estimates.json')
    args=parser.parse_args();build(args.policy,args.output)
