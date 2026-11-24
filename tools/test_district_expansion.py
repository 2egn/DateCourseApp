import json
import random
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from build_public_demo import ROOT
from build_district_expansion import stage, DEFAULT_POLICY
from place_data_quality import screen
from collect_busan_appeal_data import NearestTransitIndex, distance, manual_facilities, match_designations

class DistrictExpansionTest(unittest.TestCase):
    def row(self,pid,name='가게',lat='35.15',lng='129.11',address='도로 1'):
        return dict(place_id=pid,name=name,lat=lat,lon=lng,road_address=address,lot_address='',category='MEAL',district='수영구',source_reference_date='2026-09-30')

    def test_core_expansion_retains_published_places_facts_and_conservative_new_prices(self):
        root=ROOT/'data/places/expansion';base=root/'suyeong-2026-11-16';expanded=root/'busan-core-2026-11-23'
        old=json.loads((base/'places.json').read_text(encoding='utf-8'))['places']
        places=json.loads((expanded/'places.json').read_text(encoding='utf-8'))['places']
        self.assertEqual(old,places[:len(old)]);self.assertEqual(2040,len(places))
        self.assertEqual(len(places),len({p['recordId'] for p in places}))
        self.assertEqual(len(places),len({p['details']['externalIds']['public'] for p in places}))
        old_facts=json.loads((base/'appeal/demo_appeal_facts.json').read_text(encoding='utf-8'))['places']
        facts=json.loads((expanded/'appeal/demo_appeal_facts.json').read_text(encoding='utf-8'));index={p['placeId']:p for p in facts['places']}
        self.assertTrue(all(p==index[p['placeId']] for p in old_facts))
        self.assertEqual(2040,len(index))
        import hashlib
        self.assertEqual(hashlib.sha256((expanded/'places.json').read_bytes()).hexdigest(),facts['datasetSha256'])
        reports=json.loads((expanded/'stage-reports.json').read_text(encoding='utf-8'))['stages']
        quarantined=set(pid for report in reports for pid in report['quality']['quarantinedIds'])
        for place in places[len(old):]:
            self.assertNotIn(place['details']['externalIds']['public'],quarantined)
            self.assertEqual(0,place['rating'])
            price=place['priceEstimate'];self.assertNotIn(price['evidence'],['GOOGLE_PROVIDED','ADMIN_REVIEWED'])
            if price['evidence']=='SIMILAR_ESTIMATE':self.assertIn(place['details']['subcategory'],reports[0]['pricePolicy']['allowedSubtypes'])
            if price['evidence']=='UNKNOWN':self.assertIsNone(price['minimum']);self.assertIsNone(price['maximum'])

    def test_jung_gu_candidate_pack_preserves_active_baseline_and_unknown_prices(self):
        import hashlib
        from build_public_demo import read
        folder=ROOT/'data/places/expansion/jung-gu-2026-11-25'
        meta=json.loads((folder/'baseline.json').read_text(encoding='utf-8'))
        before_path=ROOT/meta['baselineDataset'];before_facts=ROOT/meta['baselineFacts']
        self.assertEqual(meta['baselineDatasetSha256'],hashlib.sha256(before_path.read_bytes()).hexdigest())
        self.assertEqual(meta['baselineFactsSha256'],hashlib.sha256(before_facts.read_bytes()).hexdigest())
        places=json.loads((folder/'places.json').read_text(encoding='utf-8'))['places']
        before=json.loads(before_path.read_text(encoding='utf-8'))['places']
        self.assertEqual(2138,len(places));self.assertTrue(before==places[:len(before)])
        self.assertEqual(len(places),len({p['recordId'] for p in places}))
        self.assertEqual(len(places),len({p['placeId'] for p in places}))
        facts=json.loads((folder/'appeal/demo_appeal_facts.json').read_text(encoding='utf-8'))
        self.assertEqual(hashlib.sha256((folder/'places.json').read_bytes()).hexdigest(),facts['datasetSha256'])
        index={p['placeId']:p for p in facts['places']}
        self.assertEqual(len(places),len(index))
        self.assertTrue(all(p==index[p['placeId']] for p in json.loads(before_facts.read_text(encoding='utf-8'))['places']))
        source={p['place_id']:p for p in read('busan_places.csv')}
        policy=json.loads((folder/'policy.json').read_text(encoding='utf-8'))
        report=json.loads((folder/'quality-report.json').read_text(encoding='utf-8'))
        for place in places[len(before):]:
            pid=place['details']['externalIds']['public']
            self.assertEqual('중구',source[pid]['district']);self.assertNotIn(pid,report['quality']['quarantinedIds'])
            self.assertEqual(0,place['rating']);price=place['priceEstimate']
            self.assertIn(price['evidence'],['UNKNOWN','SIMILAR_ESTIMATE','REFERENCE'])
            if price['evidence']=='UNKNOWN':self.assertIsNone(price['minimum']);self.assertIsNone(price['maximum'])
            if price['evidence']=='SIMILAR_ESTIMATE':
                self.assertIn(place['details']['subcategory'],policy['pricePolicy']['allowedSubtypes'])
                self.assertIn('해당 매장 가격 확인값이 아님',price['basis'])
        self.assertEqual(meta['datasetSha256'],hashlib.sha256((folder/'places.json').read_bytes()).hexdigest())
        self.assertEqual(meta['factsSha256'],hashlib.sha256((folder/'appeal/demo_appeal_facts.json').read_bytes()).hexdigest())

    def test_suspected_duplicates_are_quarantined_but_existing_records_are_retained(self):
        rows=[self.row('old'),self.row('new',lat='35.1501',address='도로 2'),self.row('other',name='다른 가게'),self.row('bad',name='좌표 없음',lat='')]
        report=screen(rows,['old'])
        self.assertEqual(['bad','new'],report['quarantinedIds'])
        self.assertTrue(next(r for r in report['reviews'] if r['publicId']=='old')['retainedExisting'])
        self.assertEqual(1,len(report['nearbySameNamePairs']))
        self.assertEqual('missing_or_invalid_coordinate',next(r for r in report['reviews'] if r['publicId']=='bad')['reasons'][0])
        rows=[self.row('A',name=' 가 게 ',address='도로 1'),self.row('B',address='도로1')]
        self.assertEqual([['A','B']],screen(rows)['exactNameAddressGroups'])
        with self.assertRaises(ValueError):screen([self.row('same'),self.row('same')])

    def test_old_unknown_and_future_reference_dates_are_held_out(self):
        rows=[dict(self.row(str(i),name=str(i)),source_reference_date=d) for i,d in enumerate(['2019-01-01','','2099-01-01'])]
        self.assertEqual(['0','1','2'],screen(rows)['quarantinedIds'])

    def test_transit_grid_matches_exhaustive_search_including_cell_edges_and_remote_points(self):
        rng=random.Random(122)
        rows=[{'lat':35+rng.random()*.4,'lng':128.8+rng.random()*.5,'id':i} for i in range(500)]
        rows.extend([dict(rows[0],id=500)])
        index=NearestTransitIndex(rows,'lat','lng')
        for lat,lng in [(35.0,128.8),(35.15999,129.11999),(35.6,129.6),(rows[0]['lat'],rows[0]['lng'])]+[(35+rng.random()*.4,128.8+rng.random()*.5) for _ in range(60)]:
            expected=min(rows,key=lambda r:distance(lat,lng,r['lat'],r['lng']))
            self.assertEqual(expected,index.nearest(lat,lng))

    def test_manual_dates_are_preserved_and_absent_facilities_are_optional(self):
        fact=dict(publicPlaceId='one',kind='FREE_ADMISSION',checkedAt='2026-11-14')
        self.assertEqual([],manual_facilities([], [fact]))
        place=dict(placeId='DEMO-one',name='시설',details={'externalIds':{'public':'one'}})
        result=manual_facilities([place],[fact])
        self.assertEqual('2026-11-14',result[0]['checkedAt'])
        self.assertEqual('DEMO-one',result[0]['placeId'])

    def test_designations_are_scoped_and_ambiguous_matches_remain_for_review(self):
        row=self.row('A');row['name']='식당';row['road_address']='부산광역시 수영구 수영로 1'
        source=dict(district='수영구',sourceUrl='https://example.test',referenceDate='2026-09-30',rows=[{'업소명':'식당','소재지(도로명)':'수영로 1'}])
        links,review=match_designations([row],[source]);self.assertEqual('A',links[0]['public_place_id']);self.assertFalse(review)
        links,review=match_designations([row,dict(row,place_id='B')],[source]);self.assertFalse(links);self.assertEqual(1,len(review))
        links,_=match_designations([dict(row,district='다른구')],[source]);self.assertFalse(links)

    def test_staged_district_is_repeatable_preserves_baseline_and_has_conservative_prices(self):
        original=(ROOT/'data/places/demo/seomyeon-public-estimates.json').read_bytes()
        with tempfile.TemporaryDirectory() as temp:
            target=Path(temp)
            with patch('urllib.request.urlopen',side_effect=AssertionError('No network')):
                report=stage(DEFAULT_POLICY,target)
                first=(target/'places.json').read_bytes()
                self.assertEqual(report,stage(DEFAULT_POLICY,target))
                self.assertEqual(first,(target/'places.json').read_bytes())
            rows=json.loads(first)['places'];old=json.loads(original)['places'];self.assertEqual(old,rows[:len(old)])
            self.assertEqual(480,report['added']);self.assertEqual(866,report['total'])
            self.assertFalse(set(report['quality']['quarantinedIds']) & {p['details']['externalIds']['public'] for p in rows})
            for p in rows[len(old):]:
                price=p['priceEstimate']
                self.assertIn('수영구',p['details']['address'])
                if price['evidence']=='SIMILAR_ESTIMATE':
                    self.assertIn(p['details']['subcategory'],report['pricePolicy']['allowedSubtypes'])
                    self.assertNotIn('일식',price['basis'])
                else:self.assertIsNone(price['minimum'])
        self.assertEqual(original,(ROOT/'data/places/demo/seomyeon-public-estimates.json').read_bytes())

if __name__=='__main__':unittest.main()
