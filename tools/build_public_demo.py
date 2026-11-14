"""Build a separate public-place demonstration dataset. Never imports into MySQL.

Prices derived from public menu amounts assume a single-person serving. This is
explicitly an estimate, not confirmation of a venue's menu or current business.
"""
import csv
import json
import math
import statistics
from collections import defaultdict, Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'data/places/public/busan'
def read(name):
    with (DATA/name).open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))
def dist(p):
    a,b,c,d=map(math.radians,(35.1578,129.06,float(p['lat']),float(p['lon'])))
    h=math.sin((c-a)/2)**2+math.cos(a)*math.cos(c)*math.sin((d-b)/2)**2
    return 12742000*math.asin(min(1,math.sqrt(h)))

def build():
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
    near=[p for p in places if p['lat'] and p['lon'] and p['category'] in {'MEAL','CAFE','CULTURE'} and dist(p)<=2000]
    near.sort(key=lambda p:(dist(p),p['place_id']))
    chosen={};seen=set();category_counts=Counter()
    def add(p):
        if checks.get(p['place_id'],{}).get('decision')=='REVIEW_NEEDED':return
        key=(p['name'],p['road_address'])
        if key in seen:return
        seen.add(key);chosen[p['place_id']]=p;category_counts[p['category']]+=1
    for pid in checked:
        if pid in byid and byid[pid]['lat'] and byid[pid]['lon'] and dist(byid[pid])<=2000:add(byid[pid])
    # Cover multiple cuisine facets and designated shops, then fill nearby pools.
    for g in ['한식','중식','일식']:
        for p in [p for p in near if group(p)==g][:15]:add(p)
    for p in [p for p in near if p['place_id'] in good][:20]:add(p)
    for p in near:
        if category_counts[p['category']]<({'MEAL':60,'CAFE':35,'CULTURE':30}[p['category']]):add(p)
    out=[]
    for index,p in enumerate(chosen.values(),1):
        price=dict(minimum=None,maximum=None,unit='UNKNOWN',basis='확인 가능한 단위별 표본 부족',evidence='UNKNOWN',sourceUrl='',checkedAt='')
        g=group(p)
        if g in estimates:
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
        out.append(dict(recordId=2000000+index,placeId='DCA-DEMO-PUBLIC-'+p['place_id'],name=p['name']+(' '+p['branch'] if p['branch'] else ''),category=p['category'],lat=float(p['lat']),lng=float(p['lon']),priceEstimate=price,rating=0,
            details=dict(address=p['road_address'] or p['lot_address'],subcategory=p['industry_name'],phone=p['phone'],website=p['website'],sourceUrl=p['source_url'],sourceDate=p['source_reference_date'],identityCheckedAt=identity.get('identityCheckedAt',''),verificationUrl=identity.get('verificationUrl',''),externalIds=external_ids,tags=tags)))
    doc=dict(schemaVersion=1,title='서면 공공자료 · 추정 가격',startLat=35.1578,startLng=129.06,
        note='Real public-source place records. Only marked places have recorded identity checks; review-needed places are held out. Costs are not Google-verified. Similar-place estimates assume per-person portions; inexpensive-shop sample is biased. Original nine-place dataset is unchanged.',places=out)
    target=ROOT/'data/places/demo/seomyeon-public-estimates.json'
    target.write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'places':len(out),'categories':dict(category_counts),'estimate_samples':estimates,'estimated':sum(p['priceEstimate']['evidence']=='SIMILAR_ESTIMATE' for p in out),'unknown':sum(p['priceEstimate']['minimum'] is None for p in out)},ensure_ascii=False))
if __name__=='__main__':build()
