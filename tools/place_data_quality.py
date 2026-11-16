"""Deterministic review candidates; nearby names never imply an automatic identity merge."""
import math
import re
import unicodedata
from collections import defaultdict
from datetime import date
from build_public_demo import valid_point, dist

CATEGORIES={'MEAL','CAFE','CULTURE'}

def normalize(value):
    return re.sub(r'[^\w가-힣]','',unicodedata.normalize('NFKC',value or '').casefold())

def screen(rows, existing_ids=(), earliest_date='2024-01-01'):
    existing=set(existing_ids); reasons=defaultdict(set); by_id={}; names=defaultdict(list); addresses=defaultdict(list)
    for row in rows:
        pid=row['place_id']
        if pid in by_id:raise ValueError('Duplicate source ID: '+pid)
        by_id[pid]=row
        if row['category'] not in CATEGORIES:continue
        if not row['name'].strip():reasons[pid].add('missing_name')
        address=row['road_address'] or row['lot_address']
        if not address:reasons[pid].add('missing_address')
        if not valid_point(row) or not (34.8<=float(row['lat'])<=35.6 and 128.6<=float(row['lon'])<=129.6):
            reasons[pid].add('missing_or_invalid_coordinate')
        else:names[normalize(row['name'])].append(row)
        try:
            reference=date.fromisoformat(row['source_reference_date'])
            if reference<date.fromisoformat(earliest_date):reasons[pid].add('old_source_reference')
            if reference>date.today():reasons[pid].add('future_source_reference')
        except (ValueError,KeyError):reasons[pid].add('missing_or_invalid_source_date')
        if address:addresses[(normalize(row['name']),normalize(address))].append(row)
    exact=[]
    for group in addresses.values():
        if len(group)>1:
            exact.append(sorted(r['place_id'] for r in group))
            for row in group:reasons[row['place_id']].add('same_name_and_address')
    nearby=[]
    for group in names.values():
        cells=defaultdict(list)
        for row in group:
            y,x=math.floor(float(row['lat'])*1000),math.floor(float(row['lon'])*1000)
            for dy in (-1,0,1):
                for dx in (-1,0,1):
                    for other in cells[(y+dy,x+dx)]:
                        distance=dist(row,(float(other['lat']),float(other['lon'])))
                        if distance<=50:
                            nearby.append({'ids':sorted([row['place_id'],other['place_id']]),'meters':round(distance,1),'name':row['name']})
                            for candidate in (row,other):reasons[candidate['place_id']].add('nearby_same_name')
            cells[(y,x)].append(row)
    reviews=[{'publicId':pid,'name':by_id[pid]['name'],'address':by_id[pid]['road_address'] or by_id[pid]['lot_address'],
              'retainedExisting':pid in existing,'reasons':sorted(why)} for pid,why in sorted(reasons.items())]
    return {'reviews':reviews,'exactNameAddressGroups':sorted(exact),'nearbySameNamePairs':sorted(nearby,key=lambda p:p['ids']),
            'quarantinedIds':sorted(pid for pid in reasons if pid not in existing)}
