"""Isolated feasibility probe. No application/DB writes or saved Google content."""
import argparse
import csv
import hashlib
import json
import math
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'data/places/public/busan'
OUTPUT = ROOT / 'data/places/pilot/seomyeon-jeonpo-2026-11-13'
REPORT = ROOT / 'build/place-data-pilot'
CENTER = (35.1578, 129.0600)
NEIGHBORHOODS = {'부전1동', '부전2동', '전포1동', '전포2동'}
# Deliberate small sample: two public-menu-linked meals, two cafes, one museum.
# These are availability probes, NOT the current app's recommended top five.
SAMPLE_IDS = [
    'SBIZ:MA0101202607A0151351', 'SBIZ:MA010120220813751543',
    'SBIZ:MA0106202201A2364418', 'SBIZ:MA0101202406A0012647',
    'STD:15017323:e33f9aadac190db685befd1a',
]
PROTECTED = [ROOT / 'data/places/current/integrated_places.csv',
             ROOT / 'data/sql/05_replace_integrated_data.sql',
             ROOT / 'src/main/java/kr/ac/university/localdb/db/PlaceRepository.java',
             *[SOURCE / n for n in ('busan_places.csv', 'busan_menu_prices.csv',
                                    'busan_good_price_shops.csv')]]


def rows(path):
    with path.open(encoding='utf-8-sig', newline='') as handle:
        return list(csv.DictReader(handle))


def fingerprints():
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in PROTECTED}


def distance(a, b):
    lat1, lon1, lat2, lon2 = map(math.radians, (*a, *b))
    h = math.sin((lat2-lat1)/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin((lon2-lon1)/2)**2
    return 6371000 * 2 * math.asin(min(1, math.sqrt(h)))


def in_region(row):
    try:
        point = (float(row['lat']), float(row['lon']))
        if not all(map(math.isfinite, point)) or not (33 < point[0] < 39 and 124 < point[1] < 132):
            return False
    except (ValueError, KeyError):
        return False
    if row['category'] in {'MEAL', 'CAFE'}:
        return row['district'] == '부산진구' and row['neighborhood'] in NEIGHBORHOODS
    return (row['category'] == 'CULTURE' and distance(CENTER, point) <= 2000
            and ('부산진구' in row['road_address'] or '부산진구' in row['lot_address']))


def subcategory(row):
    # Preserve original industry labels instead of inferring cuisine from the name.
    if row['category'] != 'CULTURE':
        return row['industry_name'] or '미분류'
    label = row['industry_name']
    for word, result in [('박물관', '박물관·미술관'), ('도서관', '도서관'), ('공원', '공원')]:
        if word in label:
            return result
    return label or '문화 세부 분류 미확인'


def public_price(row):
    value = row.get('price_krw', '')
    if row.get('price_basis') == 'adult_admission' and value.isdigit():
        amount = int(value)
        return {'price_min_krw': amount, 'price_max_krw': amount,
                'price_unit': 'ADULT_TICKET',
                'price_state': 'EXPLICIT_FREE' if amount == 0 else 'EXACT'}
    return {'price_min_krw': '', 'price_max_krw': '',
            'price_unit': 'UNKNOWN', 'price_state': 'UNKNOWN'}


def total_cost(prices, people):
    if not 1 <= people <= 20:
        raise ValueError('people must be 1..20')
    low = high = unknown = open_ended = 0
    for price in prices:
        minimum, maximum = price.get('min'), price.get('max')
        if price.get('unit') not in {'PER_PERSON', 'PER_DRINK', 'ADULT_TICKET'} or minimum is None:
            unknown += 1
            continue
        if minimum < 0 or maximum is not None and maximum < minimum:
            raise ValueError('invalid range')
        low += minimum * people
        if maximum is None:
            open_ended += 1
        else:
            high += maximum * people
    return {'known_min': low, 'known_max': None if open_ended else high,
            'unknown_places': unknown, 'open_ended_places': open_ended}


def within_budget(price, cap):
    return (price.get('unit') in {'PER_PERSON', 'PER_DRINK', 'ADULT_TICKET'}
            and price.get('max') is not None and 0 <= price.get('min', -1) <= price['max'] <= cap)


def review_sections(place_id, course_reviews, external):
    # Mirrors Review.course().coursePlaces(): course feedback is not a venue rating.
    internal = [r for r in course_reviews if place_id in r['place_ids']]
    return {'course_reviews_containing_place': internal, 'external_google_reviews': external}


def prepare():
    before = fingerprints()
    places = [r for r in rows(SOURCE / 'busan_places.csv') if in_region(r)]
    shops = rows(SOURCE / 'busan_good_price_shops.csv')
    menus = rows(SOURCE / 'busan_menu_prices.csv')
    shop_to_place = {r['price_shop_id']: r['linked_place_id'] for r in shops if r['linked_place_id']}
    menu_counts = Counter(shop_to_place.get(r['price_shop_id']) for r in menus)
    enriched = [dict(r, subcategory=subcategory(r), **public_price(r),
                     menu_rows_portion_unverified=menu_counts[r['place_id']]) for r in places]
    OUTPUT.mkdir(parents=True, exist_ok=True)
    # Exclusive create: a rerun cannot silently replace this test dataset.
    with (OUTPUT / 'places.csv').open('x', encoding='utf-8-sig', newline='') as handle:
        writer = csv.DictWriter(handle, fieldnames=list(enriched[0]))
        writer.writeheader()
        writer.writerows(enriched)
    manifest = {'created_utc': datetime.now(timezone.utc).isoformat(),
                'scope': 'MEAL/CAFE: four neighborhoods; CULTURE: Busanjingu within 2km of Seomyeon station',
                'counts': dict(Counter(r['category'] for r in enriched)),
                'price_states': dict(Counter(r['price_state'] for r in enriched)),
                'with_public_menu_rows': sum(r['menu_rows_portion_unverified'] > 0 for r in enriched),
                'source_sha256': before, 'google_content_stored': False,
                'app_connected': False, 'duplicate_policy': 'preserve source IDs; no automatic name merge',
                'source_files_unchanged': before == fingerprints()}
    with (OUTPUT / 'manifest.json').open('x', encoding='utf-8') as handle:
        json.dump(manifest, handle, ensure_ascii=False, indent=2)
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


def normalized_name(name):
    return re.sub(r'[^a-z0-9가-힣]', '', name.casefold()).removesuffix('점')


def load_google_key():
    values = {}
    for line in (ROOT / '.local/google-places.properties').read_text(encoding='utf-8-sig').splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            values[key.strip()] = value.strip()
    key = values.get('google.places.api.key', '')
    if not re.fullmatch(r'[A-Za-z0-9_-]{20,100}', key):
        raise ProbeError('KEY_MISSING_OR_INVALID_FORMAT')
    return key


def road_building(address):
    found = re.search(r'([가-힣A-Za-z0-9·]+(?:대로|로|길))\s+(\d+(?:-\d+)?)', address)
    return found.groups() if found else None


def match_status(local, remote):
    expected = normalized_name(local['name'] + local['branch'])
    actual = normalized_name(remote.get('displayName', {}).get('text', ''))
    location = remote.get('location', {})
    if not location or 'latitude' not in location or 'longitude' not in location:
        return 'NEEDS_REVIEW'
    meters = distance((float(local['lat']), float(local['lon'])),
                      (location['latitude'], location['longitude']))
    address = road_building(local['road_address'])
    if (expected and expected == actual and meters <= 200 and address
            and address == road_building(remote.get('formattedAddress', ''))):
        return 'NAME_ADDRESS_COORDS_AGREE'
    return 'NEEDS_REVIEW'


def money(value):
    return Decimal(value.get('units', '0')) + Decimal(value.get('nanos', 0)) / Decimal(1000000000)


def google_price_kind(place):
    value = place.get('priceRange', {})
    if value.get('startPrice'):
        low = money(value['startPrice'])
        high = money(value['endPrice']) if value.get('endPrice') else None
        if high is not None and high < low:
            return 'INVALID_RANGE'
        return 'RANGE' if high is not None else 'OPEN_ENDED'
    if place.get('priceLevel') not in (None, 'PRICE_LEVEL_UNSPECIFIED'):
        return 'LEVEL_ONLY'
    return 'UNKNOWN'


class ProbeError(Exception):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class GoogleProbe:
    def __init__(self, key, max_requests=10):
        if not 1 <= max_requests <= 80:
            raise ValueError('Request cap must be 1..80')
        self.key = key
        self.max_requests = max_requests
        self.calls = 0
        self.counts = Counter()
        self.opener = urllib.request.build_opener(NoRedirect)

    def request(self, method, suffix, mask, payload=None):
        if self.calls >= self.max_requests:
            raise ProbeError('REQUEST_CAP_REACHED')
        self.calls += 1
        self.counts['search' if payload is not None else 'details'] += 1
        request = urllib.request.Request('https://places.googleapis.com/v1/' + suffix,
            data=json.dumps(payload).encode() if payload is not None else None,
            headers={'Content-Type': 'application/json', 'X-Goog-Api-Key': self.key,
                     'X-Goog-FieldMask': mask}, method=method)
        try:
            with self.opener.open(request, timeout=25) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            # Never print the raw response, request headers, key, or review content.
            raise ProbeError('HTTP_' + str(error.code)) from None
        except (urllib.error.URLError, TimeoutError, OSError):
            raise ProbeError('NETWORK_ERROR') from None


def live():
    before = fingerprints()
    key = load_google_key()
    selected = {r['place_id']: r for r in rows(OUTPUT / 'places.csv')}
    sample = [selected[key] for key in SAMPLE_IDS]
    # Prevent accidental repeat billing across invocations, including partial runs.
    with (ROOT / '.local/place-data-pilot-live.lock').open('x', encoding='utf-8') as handle:
        handle.write(datetime.now(timezone.utc).isoformat())
    probe = GoogleProbe(key)
    observations = []
    error = None
    started = time.monotonic()
    for local in sample:
        try:
            result = probe.request('POST', 'places:searchText', 'places.id', {
                'textQuery': local['name'] + ' ' + local['branch'] + ' ' + local['road_address'],
                'languageCode': 'ko', 'regionCode': 'KR', 'pageSize': 1})
            if not result.get('places'):
                observations.append({'match': 'NOT_FOUND'})
                continue
            place_id = result['places'][0]['id']
            details = probe.request('GET', 'places/' + urllib.parse.quote(place_id, safe='')
                + '?languageCode=ko&regionCode=KR',
                'id,displayName,formattedAddress,location,businessStatus,primaryTypeDisplayName,'
                'types,priceLevel,priceRange,rating,userRatingCount,reviews,googleMapsUri,attributions')
            reviews = details.get('reviews', [])
            # Only capabilities/aggregate counts leave memory, never provider content.
            observations.append({'match': match_status(local, details),
                'price_kind': google_price_kind(details),
                'has_rating': 'rating' in details,
                'has_subcategory': bool(details.get('primaryTypeDisplayName')),
                'has_status': bool(details.get('businessStatus')),
                'review_count_returned': len(reviews),
                'preview_ready_count': sum(bool(r.get('text', {}).get('text')
                    and r.get('authorAttribution', {}).get('displayName')
                    and r.get('authorAttribution', {}).get('uri')) for r in reviews),
                'has_original_link': bool(details.get('googleMapsUri'))})
        except ProbeError as failure:
            error = str(failure)
            break  # no retries or repeated auth failures
    matched = [r for r in observations if r['match'] == 'NAME_ADDRESS_COORDS_AGREE']

    def coverage(items):
        return {'details_received': sum('price_kind' in r for r in items),
                'price_kinds': dict(Counter(r.get('price_kind', 'NOT_FETCHED') for r in items)),
                'with_rating': sum(r.get('has_rating', False) for r in items),
                'with_subcategory': sum(r.get('has_subcategory', False) for r in items),
                'with_status': sum(r.get('has_status', False) for r in items),
                'with_reviews': sum(r.get('review_count_returned', 0) > 0 for r in items),
                'reviews_received': sum(r.get('review_count_returned', 0) for r in items),
                'reviews_with_text_and_author_link': sum(r.get('preview_ready_count', 0) for r in items),
                'with_original_link': sum(r.get('has_original_link', False) for r in items)}
    report = {'tested_utc': datetime.now(timezone.utc).isoformat(), 'sample_size': len(sample),
              'request_attempts': dict(probe.counts), 'elapsed_seconds': round(time.monotonic()-started, 2),
              'match_counts': dict(Counter(r['match'] for r in observations)),
              'all_responses': coverage(observations), 'strictly_matched_only': coverage(matched),
              'error': error, 'source_files_unchanged': before == fingerprints(),
              'raw_google_content_stored': False, 'actual_billing_checked': False,
              'price_unit_note': 'Google venue range is NOT confirmed per-person course cost'}
    REPORT.mkdir(parents=True, exist_ok=True)
    with (REPORT / 'live-summary.json').open('x', encoding='utf-8') as handle:
        json.dump(report, handle, ensure_ascii=False, indent=2)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if error:
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['prepare', 'live'])
    args = parser.parse_args()
    try:
        prepare() if args.command == 'prepare' else live()
    except (ProbeError, FileExistsError) as error:
        print(str(error) if isinstance(error, ProbeError) else 'OUTPUT_OR_LIVE_LOCK_EXISTS: stopped without overwrite')
        raise SystemExit(1)
