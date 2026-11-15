"""Collect public appeal facts without changing application datasets or databases.

Only public downloads are used. No map review scraping, paid APIs, or credentials.
Downloads are cached under build/appeal-data-fetch; published outputs retain source
dates and checksums. Run from any working directory with Python 3.10+.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import re
import shutil
import urllib.parse
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / 'build/appeal-data-fetch'
OUT = ROOT / 'data/places/appeal/busan-2026-11-14'
PUBLIC = ROOT / 'data/places/public/busan'
DATE = datetime.now(timezone(timedelta(hours=9))).date().isoformat()


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')


def fetch(url, path):
    if not path.exists():
        request = urllib.request.Request(url, headers={'User-Agent': 'DateCourseApp-public-data-research/1.0'})
        with urllib.request.urlopen(request, timeout=30) as response:
            data = response.read()
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return path.read_bytes()


def csv_rows(path):
    raw = path.read_bytes()
    for encoding in ('utf-8-sig', 'cp949'):
        try:
            return list(csv.DictReader(io.StringIO(raw.decode(encoding))))
        except UnicodeDecodeError:
            continue
    raise ValueError(f'Unsupported CSV encoding: {path.name}')


def write_csv(path, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        return
    with path.open('w', encoding='utf-8-sig', newline='') as output:
        writer = csv.DictWriter(output, fieldnames=list(rows[0]), lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def download_file(name, identifier):
    url = f'https://www.data.go.kr/data/{identifier}/fileData.do'
    page = fetch(url, CACHE / f'{name}.html').decode('utf-8')
    metadata = json.loads(re.search(r'<script[^>]+type="application/ld\+json"[^>]*>(.*?)</script>', page, re.S)[1])
    button = re.search(r"fn_fileDataDown\('([^']+)', '([^']+)'", page)
    query = urllib.parse.urlencode({'publicDataPk': identifier, 'publicDataDetailPk': button[2],
                                   'atchFileId': '', 'fileDetailSn': '1', 'publicDataTyCode': 'PR0051'})
    result = json.loads(fetch('https://www.data.go.kr/tcs/dss/selectFileDataDownload.do?' + query,
                              CACHE / f'{name}-download.json'))
    if result.get('status') is not True:
        raise RuntimeError(f'Public download not available: {name}')
    download = 'https://www.data.go.kr/cmm/cmm/fileDownload.do?' + urllib.parse.urlencode(
        {'atchFileId': result['atchFileId'], 'fileDetailSn': result['fileDetailSn']})
    payload = fetch(download, CACHE / f'{name}.csv')
    metadata.update(download_url=download, downloaded_sha256=hashlib.sha256(payload).hexdigest(),
                    downloaded_bytes=len(payload), retrieved_date=DATE)
    return metadata


def distance(a, b, c, d):
    a, b, c, d = map(math.radians, (float(a), float(b), float(c), float(d)))
    h = math.sin((c-a)/2)**2 + math.cos(a)*math.cos(c)*math.sin((d-b)/2)**2
    return 12742000 * math.asin(math.sqrt(min(1, h)))


def name_key(text):
    text = re.sub(r'\(주\)|주식회사', '', text)
    return re.sub(r'[^0-9a-z가-힣]', '', text.lower())


def road_key(text):
    match = re.search(r'([^\s,()]+(?:대로|로|길))\s*(\d+(?:-\d+)?)', text)
    return (match[1], match[2]) if match else None


def valid_point(lat, lng):
    try:
        return 34.8 <= float(lat) <= 35.6 and 128.6 <= float(lng) <= 129.6
    except (TypeError, ValueError):
        return False


def source_date(date):
    if re.fullmatch(r'\d{8}', date):
        return datetime.strptime(date, '%Y%m%d').date().isoformat()
    if re.fullmatch(r'\d{4,5}(?:\.0)?', date):
        return (datetime(1899, 12, 30) + timedelta(days=float(date))).date().isoformat()
    return date


def railway_rows():
    # The portal links to this public provider download; it is not a private API.
    import xml.etree.ElementTree as ET
    import zipfile
    url = 'https://data.kric.go.kr/rips/dataset/download.file?type=filedata&id=32&operation=1'
    payload = fetch(url, CACHE / 'rail.xlsx')
    archive = zipfile.ZipFile(io.BytesIO(payload))
    ns = {'m': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
    strings = [''.join(node.itertext()) for node in ET.fromstring(archive.read('xl/sharedStrings.xml')).findall('m:si', ns)]
    rows = []
    for row in ET.fromstring(archive.read('xl/worksheets/sheet1.xml')).findall('m:sheetData/m:row', ns):
        values = []
        for cell in row.findall('m:c', ns):
            column = 0
            for letter in ''.join(filter(str.isalpha, cell.attrib['r'])):
                column = column * 26 + ord(letter) - 64
            while len(values) < column:
                values.append('')
            value = cell.find('m:v', ns)
            text = '' if value is None else value.text or ''
            if cell.attrib.get('t') == 's':
                text = strings[int(text)]
            elif cell.attrib.get('t') == 'inlineStr':
                text = ''.join(cell.find('m:is', ns).itertext())
            values[column-1] = text
        rows.append(values)
    records = []
    for values in rows[1:]:
        row = dict(zip(rows[0], values))
        date = row.get('데이터기준일자', '')
        row['데이터기준일자'] = source_date(date)
        if '부산' in row.get('역사도로명주소', '') and valid_point(row.get('역위도'), row.get('역경도')):
            records.append(row)
    return records, {'name': '전국도시철도역사정보표준데이터', 'url': 'https://www.data.go.kr/data/15013205/standard.do',
                     'provider_url': 'https://data.kric.go.kr/rips/M_01_01/detail.do?id=32',
                     'download_url': url, 'retrieved_date': DATE, 'downloaded_bytes': len(payload),
                     'downloaded_sha256': hashlib.sha256(payload).hexdigest(), 'national_rows': len(rows)-1,
                     'license': '공공데이터포털 메타데이터: 이용허락범위 제한 없음',
                     'selection': '역사도로명주소에 부산 포함; 좌표 유효. 부산 전체 역사·출입구의 완전한 목록을 보장하지 않음.'}


def read_lf_bytes(path):
    data = path.read_bytes()
    if b'\r\n' in data:
        raise ValueError('Demo snapshot must use LF line endings before generating public facts.')
    return data


def load_inputs(offline):
    if offline:
        sources=[s for s in read_json(OUT/'sources.json') if s.get('identifier')!='3045247']
        rail_source=next(s for s in sources if '전국도시철도' in s.get('name',''))
        manifest=read_json(OUT/'checksums.json')
        for name,digest in manifest['outputs'].items():
            if name.startswith('raw/') and hashlib.sha256((OUT/name).read_bytes()).hexdigest()!=digest:
                raise ValueError('Cached public input has changed: '+name)
        buses=csv_rows(OUT/'raw/busan_bus_stops.csv')
        stations=csv_rows(OUT/'raw/busan_railway_stations.csv')
        models=csv_rows(OUT/'raw/busanjin_model_restaurants_cp949.csv')
        return sources,rail_source,buses,stations,models,read_json(OUT/'summary.json')['national_bus_rows']
    sources=[download_file('model','15055847'),download_file('bus','15067528')]
    stations,rail_source=railway_rows();sources.append(rail_source)
    bus_all=csv_rows(CACHE/'bus.csv')
    buses=[row for row in bus_all if row['도시명']=='부산광역시' and valid_point(row['위도'],row['경도'])]
    return sources,rail_source,buses,stations,csv_rows(CACHE/'model.csv'),len(bus_all)


def collect(offline=False):
    demo_path = ROOT / 'data/places/demo/seomyeon-public-estimates.json'
    demo_bytes = read_lf_bytes(demo_path)
    (OUT / 'raw').mkdir(parents=True, exist_ok=True)
    sources,rail_source,buses,stations,models,national_bus_rows=load_inputs(offline)
    write_csv(OUT / 'busanjin_model_restaurants.csv', [
        {'source_row': index, 'name': row['업소명'].strip(), 'road_address': row['소재지(도로명)'].strip(),
         'food_type': row['음식의유형'], 'main_menu': row['주된음식'], 'phone': row['전화번호'],
         'source_date': '2025-11-17', 'source_url': sources[0]['url']}
        for index, row in enumerate(models, 1)])
    if not offline:
        write_csv(OUT / 'raw/busan_bus_stops.csv', buses)
        write_csv(OUT / 'raw/busan_railway_stations.csv', stations)
        shutil.copyfile(CACHE / 'model.csv', OUT / 'raw/busanjin_model_restaurants_cp949.csv')
        shutil.copyfile(CACHE / 'rail.xlsx', OUT / 'raw/national_railway_original.xlsx')

    places = csv_rows(PUBLIC / 'busan_places.csv')
    by_id = {row['place_id']: row for row in places}
    lookup = defaultdict(list)
    for row in places:
        if row['category'] == 'MEAL' and row['district'] == '부산진구' and road_key(row['road_address']):
            lookup[(name_key(row['name']), road_key(row['road_address']))].append(row)
    links, review = [], []
    for number, row in enumerate(models, 1):
        candidates = lookup[(name_key(row['업소명']), road_key(row['소재지(도로명)']))]
        if len(candidates) != 1:
            review.append({'source_row': number, 'name': row['업소명'], 'address': row['소재지(도로명)'],
                           'candidate_ids': [p['place_id'] for p in candidates],
                           'reason': 'no_unique_exact_name_and_road_building_match'})
            continue
        p = candidates[0]
        links.append({'public_place_id': p['place_id'], 'place_name': p['name'], 'tag': 'MODEL_RESTAURANT',
                      'source_name': row['업소명'], 'source_address': row['소재지(도로명)'], 'source_row': number,
                      'basis': '부산진구 모범음식점 지정 목록 · 상호와 도로명 건물번호가 유일하게 일치',
                      'sourceUrl': sources[0]['url'], 'sourceDate': '2025-11-17', 'checkedAt': DATE})
    # Reuse existing strict designation matches; never promote unmatched or similar-name records.
    old_manifest = read_json(PUBLIC / 'manifest.json')
    good_source = next(s for s in old_manifest['sources'] if s['identifier'] == '3045247')
    sources.append({'reused_from': (PUBLIC/'manifest.json').relative_to(ROOT).as_posix(), **good_source})
    for shop in csv_rows(PUBLIC / 'busan_good_price_shops.csv'):
        pid = shop['linked_place_id']
        if pid in by_id:
            links.append({'public_place_id': pid, 'place_name': by_id[pid]['name'], 'tag': 'GOOD_PRICE',
                          'source_name': shop.get('name', ''), 'source_address': shop.get('address', ''), 'source_row': '',
                          'basis': '기존 상호·도로명 건물번호 유일 일치 연결 재사용',
                          'sourceUrl': good_source['url'], 'sourceDate': '2026-09-30', 'checkedAt': DATE})
    write_json(OUT / 'sources.json', sources)
    write_csv(OUT / 'designation_links.csv', links)
    write_json(OUT / 'designation_review_needed.json', review)

    demos = read_json(demo_path)['places']
    model_by_id = {r['public_place_id']: r for r in links if r['tag'] == 'MODEL_RESTAURANT'}
    facts, nearest = [], []
    for place in demos:
        pid = place['details']['externalIds']['public']
        tags = [dict(tag) for tag in place['details']['tags']]
        if pid in model_by_id:
            r = model_by_id[pid]
            tags.append({'tag': 'MODEL_RESTAURANT', 'basis': r['basis'], 'sourceUrl': r['sourceUrl'],
                         'checkedAt': r['sourceDate'], 'matchCheckedAt': DATE})
        station = min(stations, key=lambda r: distance(place['lat'],place['lng'],r['역위도'],r['역경도']))
        stop = min(buses, key=lambda r: distance(place['lat'],place['lng'],r['위도'],r['경도']))
        sd = distance(place['lat'],place['lng'],station['역위도'],station['역경도'])
        bd = distance(place['lat'],place['lng'],stop['위도'],stop['경도'])
        nearest.append({'place_id': place['placeId'], 'name': place['name'], 'nearest_station': station['역사명'],
                        'station_line': station['노선명'], 'station_distance_m': round(sd,1), 'station_source_date': station['데이터기준일자'],
                        'nearest_bus_stop': stop['정류장명'], 'bus_stop_id': stop['정류장번호'],
                        'bus_distance_m': round(bd,1), 'bus_source_date': stop['정보수집일'], 'distance_kind': 'straight_line'})
        for qualifies, tag, basis, url, reference in [
            (sd <= 500, 'NEAR_SUBWAY', f"{station['역사명']}역 역사 좌표 기준 직선 {sd:.0f}m · 출입구·도보 경로 아님",
             rail_source['url'], station['데이터기준일자']),
            (bd <= 200, 'NEAR_BUS', f"{stop['정류장명']} 정류장 기준 직선 {bd:.0f}m · 도보 경로 아님",
             sources[1]['url'], stop['정보수집일'])]:
            if qualifies:
                tags.append({'tag': tag, 'basis': basis, 'sourceUrl': url, 'checkedAt': reference, 'calculatedAt': DATE})
        facts.append({'placeId': place['placeId'], 'publicPlaceId': pid, 'name': place['name'], 'tags': tags})
    write_csv(OUT / 'demo_nearest_transit.csv', nearest)
    write_json(OUT / 'threshold_comparison.json', {
        'distance_kind': 'straight_line', 'demo_places': len(nearest),
        'station_within_m': {str(m): sum(float(p['station_distance_m']) <= m for p in nearest) for m in (100,200,300,500)},
        'bus_stop_within_m': {str(m): sum(float(p['bus_distance_m']) <= m for p in nearest) for m in (50,100,150,200)},
        'note': '거리 표시는 소수점 첫째 자리 반올림. 기본 태그 판정은 반올림 전 거리로 계산함.'})
    write_json(OUT / 'demo_appeal_facts.json', {'schemaVersion': 1, 'generatedAt': datetime.now(timezone.utc).isoformat(),
                'datasetSha256': hashlib.sha256(demo_bytes).hexdigest(), 'places': facts,
                'rules': {'NEAR_SUBWAY': '역사 중심 직선거리 500m 이하',
                          'NEAR_BUS': '정류장 좌표 직선거리 200m 이하'}})
    # Extra facility facts stay outside the application's current PlaceTag enum.
    standards = read_json(PUBLIC / 'raw/busan_standard_sources.json')
    facilities = []
    for place in demos:
        if place['category'] != 'CULTURE':
            continue
        for dataset, name_field, address_field in [('15012890','PARK_NM','RDNMADR'),('15017323','FCLTY_NM','RDNMADR')]:
            matches = [r for r in standards[dataset] if name_key(r[name_field]) == name_key(place['name'])
                       and valid_point(r.get('LATITUDE'),r.get('LONGITUDE'))
                       and distance(place['lat'],place['lng'],r['LATITUDE'],r['LONGITUDE']) <= 1]
            if not matches:
                continue
            latest = max(r['REFERENCE_DATE'] for r in matches)
            matches = [r for r in matches if r['REFERENCE_DATE'] == latest]
            for word, kind, label in [('화장실','RESTROOM','화장실 시설 정보'),('주차','PARKING','주차시설 · 요금 미확인')]:
                if all(word in (r.get('CNVNNC_FCLTY','') + r.get('FCLTY_INFO','')) for r in matches):
                    facilities.append({'placeId':place['placeId'], 'name':place['name'], 'kind':kind,'label':label,
                                       'basis':'원문 편익시설/부대시설 필드에 명시 · 상호와 좌표 1m 이내 일치',
                                       'sourceUrl':f'https://www.data.go.kr/data/{dataset}/standard.do',
                                       'sourceDate':latest,'checkedAt':DATE})
    museum = next(p for p in demos if p['name']=='증권박물관(부산관)')
    facilities.extend([
        {'placeId':museum['placeId'],'name':museum['name'],'kind':'FREE_ADMISSION','label':'무료 관람',
         'basis':'운영기관 관람 안내에서 관람료 무료 확인','sourceUrl':'https://bsmuseum.ksd.or.kr/contents.do?menuno=262',
         'sourceExcerpt':'관람료 무료','checkedAt':DATE},
        {'placeId':museum['placeId'],'name':museum['name'],'kind':'FREE_AUDIO_GUIDE','label':'오디오가이드 무료 대여',
         'basis':'운영기관 관람 안내에 무료 대여 명시', 'conditions':'관람 당일 신분증 보관 및 신청서 작성; 만 14세 이하는 보호자 동반',
         'sourceUrl':'https://bsmuseum.ksd.or.kr/contents.do?menuno=262','sourceExcerpt':'대여료 무료','checkedAt':DATE}])
    write_json(OUT / 'additional_facility_facts.json', {'note':'현행 PlaceTag에 자동 적용하지 않는 별도 확인 자료', 'facts':facilities})
    write_json(OUT / 'sources.json', sources)
    summary = {'demo_places': len(facts), 'busan_bus_stops': len(buses), 'national_bus_rows': national_bus_rows,
               'busan_railway_rows': len(stations), 'busanjin_model_rows': len(models),
               'model_unique_place_matches': len({r['public_place_id'] for r in links if r['tag']=='MODEL_RESTAURANT'}),
               'model_review_needed': len(review), 'tag_counts': dict(Counter(t['tag'] for p in facts for t in p['tags'])),
               'source_date_counts_rail': dict(Counter(r['데이터기준일자'] for r in stations)),
               'additional_facility_fact_count': len(facilities),
               'ui_or_database_changed': False, 'input_mode': 'offline_reuse' if offline else 'public_download', 'calculated_at': DATE}
    write_json(OUT / 'summary.json', summary)
    tracked_inputs = [demo_path, PUBLIC/'busan_good_price_shops.csv', PUBLIC/'busan_places.csv',
                      PUBLIC/'raw/busan_standard_sources.json']
    write_json(OUT / 'checksums.json', {
        'inputs': {p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked_inputs},
        'outputs': {p.relative_to(OUT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
                    for p in sorted(OUT.rglob('*')) if p.is_file() and p.name not in ('checksums.json','README.md','.gitattributes')}})
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline',action='store_true',help='Reuse checksum-verified published public inputs; no downloads')
    args=parser.parse_args();collect(args.offline)
