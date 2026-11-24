"""가격 근거를 선택해 새 스냅샷을 만든다. 운영 DB를 직접 변경하지 않는다."""
import argparse
import copy
import csv
import hashlib
import json
import re
from datetime import datetime, timezone
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def normalized(value):
    return re.sub(r'\s+', '', value)


def read_json(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')


def public_id(place):
    return place['details']['externalIds']['public']


def choose_price(place, observations, menus):
    """장소·지역·메뉴·단위가 맞는 근거를 고른다. 현재 참고 가격은 보존한다."""
    current = place['priceEstimate']
    if current['evidence'] not in {'UNKNOWN', 'SIMILAR_ESTIMATE', 'DEMO_ASSUMPTION'}:
        return current, None
    region = '부산' if place['details']['address'].startswith('부산광역시') else ''
    menu_names = {normalized(m) for m in menus}
    choices = []
    for observation in observations:
        amount = observation.get('amount')
        kind = observation.get('kind')
        if type(amount) is not int or amount <= 0 or not observation.get('sourceUrl') or not observation.get('period'):
            continue
        unit = observation.get('unit')
        if unit not in {'PER_PERSON', 'PER_DRINK', 'ADULT_TICKET'}:
            continue
        menu = normalized(observation.get('menu', ''))
        if not menu:
            continue
        if kind == 'VENUE':
            # ID뿐 아니라 주소·매장명·메뉴·제공 단위가 모두 맞아야 한다.
            if (observation.get('publicPlaceId') != public_id(place)
                    or normalized(observation.get('name', '')) != normalized(place['name'])
                    or normalized(observation.get('address', '')) != normalized(place['details']['address'])
                    or menu not in menu_names or observation.get('servingVerified') is not True):
                continue
            priority = 0
        elif kind == 'REGIONAL':
            if (place['category'] != 'MEAL' or unit != 'PER_PERSON' or observation.get('region') != region
                    or observation.get('servingVerified') is not True):
                continue
            # 밀면을 냉면, 일반 한식을 비빔밥으로 확대 해석하지 않는다.
            if menu not in menu_names and menu not in normalized(place['name']):
                continue
            priority = 1
        elif kind == 'SURVEY':
            # 현재 조사에서는 커피 전문점의 커피만 1잔 가정을 허용한다.
            if (place['category'] != 'CAFE' or place['details']['subcategory'] != '카페'
                    or menu != '커피' or unit != 'PER_DRINK' or observation.get('region') != '전국'
                    or type(observation.get('sampleCount')) is not int or observation['sampleCount'] < 20
                    or not observation.get('assumption')):
                continue
            priority = 2
        else:
            continue
        choices.append((priority, observation))
    if not choices:
        return current, None
    # 같은 등급의 여러 메뉴를 평균 내지 않고 범위로 보존한다.
    priority = min(p for p, _ in choices)
    chosen = [o for p, o in choices if p == priority]
    # 서로 다른 단위를 하나의 가격 범위로 합치지 않는다.
    units = {o['unit'] for o in chosen}
    if len(units) != 1:
        return current, None
    basis = chosen[0]['label'] + ' · ' + ', '.join(o['menu'] for o in chosen)
    basis += ' · ' + ', '.join(sorted({o['period'] for o in chosen}))
    if priority:
        basis += ' · 매장 실값 아님; 대표 메뉴 기준 추정'
    if priority == 2:
        basis += ' · ' + chosen[0]['assumption']
    price = dict(minimum=min(o['amount'] for o in chosen), maximum=max(o['amount'] for o in chosen),
                 unit=chosen[0]['unit'], basis=basis,
                 evidence='REFERENCE' if priority == 0 else 'SIMILAR_ESTIMATE',
                 sourceUrl=chosen[0]['sourceUrl'], checkedAt=max(o['period'] for o in chosen))
    return price, chosen


def load_menus(directory):
    """공공 자료의 확정 연결만 사용하며 제공량 미확인은 실값으로 승격하지 않는다."""
    with (directory / 'busan_good_price_shops.csv').open(encoding='utf-8-sig', newline='') as source:
        shops = {s['price_shop_id']: s for s in csv.DictReader(source)
                 if s['match_method'] == 'unique_name_road_building' and s['linked_place_id']}
    result = defaultdict(list)
    with (directory / 'busan_menu_prices.csv').open(encoding='utf-8-sig', newline='') as source:
        for menu in csv.DictReader(source):
            shop = shops.get(menu['price_shop_id'])
            if shop:
                result[shop['linked_place_id']].append(menu['menu_name'])
    return result


def stage(dataset, facts, sources, menus, output, generated_at):
    """가격 외 장소 필드와 기존 특징 자료를 보존하고 변경 근거를 함께 기록한다."""
    inputs = {path.resolve() for path in (dataset, facts, sources)}
    outputs = {(output / name).resolve() for name in
               ('places.json', 'appeal/demo_appeal_facts.json', 'price-review.json')}
    if inputs & outputs:
        raise ValueError('기준본을 덮어쓸 수 없습니다.')
    old = read_json(dataset)
    new_facts = read_json(facts)
    dataset_hash = hashlib.sha256(dataset.read_bytes()).hexdigest()
    if new_facts.get('datasetSha256') != dataset_hash:
        raise ValueError('기준본과 특징 자료의 지문이 다릅니다.')
    updated = copy.deepcopy(old)
    observations = read_json(sources)['sources']
    changes = []
    selected = Counter()
    for before, after in zip(old['places'], updated['places']):
        price, chosen = choose_price(before, observations, menus.get(public_id(before), []))
        after['priceEstimate'] = price
        if price != before['priceEstimate']:
            selected[chosen[0]['kind']] += 1
            changes.append(dict(publicPlaceId=public_id(before), before=before['priceEstimate'], after=price,
                                observations=chosen))
    write_json(output / 'places.json', updated)
    new_facts['generatedAt'] = generated_at
    new_facts['datasetSha256'] = hashlib.sha256((output / 'places.json').read_bytes()).hexdigest()
    write_json(output / 'appeal/demo_appeal_facts.json', new_facts)
    report = dict(schemaVersion=1, generatedAt=generated_at, baseline=str(dataset.as_posix()),
                  baselineSha256=dataset_hash,
                  sourcesSha256=hashlib.sha256(sources.read_bytes()).hexdigest(), places=len(updated['places']),
                  changed=len(changes), selected=dict(selected),
                  evidenceCounts=dict(Counter(p['priceEstimate']['evidence'] for p in updated['places'])),
                  nonPriceFieldsPreserved=True, factsPreserved=True, changes=changes)
    write_json(output / 'price-review.json', report)
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dataset', type=Path, required=True)
    parser.add_argument('--facts', type=Path, required=True)
    parser.add_argument('--sources', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--generated-at', help='재현용 UTC 생성 시각. 생략하면 현재 UTC를 사용한다.')
    args = parser.parse_args()
    report = stage(args.dataset, args.facts, args.sources, load_menus(ROOT / 'data/places/public/busan'),
                   args.output, args.generated_at or datetime.now(timezone.utc).isoformat())
    print(json.dumps({k: v for k, v in report.items() if k != 'changes'}, ensure_ascii=False))


if __name__ == '__main__':
    main()
