"""Validate acquisition boundaries and the generated offline fact pack."""
import csv
import hashlib
import json
import unittest
import tempfile
from unittest.mock import patch
from pathlib import Path
from collect_busan_appeal_data import ROOT, OUT, distance, name_key, road_key, source_date, valid_point, read_lf_bytes, load_inputs, retain_existing_facts


class AppealDataTest(unittest.TestCase):
    def test_expansion_preserves_existing_fact_dates_and_rejects_changed_places(self):
        place=dict(placeId='old',name='기존 장소',lat=35.15,details=dict(externalIds=dict(public='PUBLIC:old')))
        existing=dict(placeId='old',publicPlaceId='PUBLIC:old',tags=[dict(tag='NEAR_BUS',calculatedAt='2026-11-16')])
        fresh=[dict(existing,tags=[dict(tag='NEAR_BUS',calculatedAt='2026-11-23')]),dict(placeId='new',tags=[])]
        with tempfile.TemporaryDirectory() as directory:
            dataset=Path(directory)/'places.json';facts=Path(directory)/'facts.json'
            dataset.write_text(json.dumps(dict(places=[place]))+'\n',encoding='utf-8',newline='\n')
            prior=dict(schemaVersion=1,datasetSha256=hashlib.sha256(dataset.read_bytes()).hexdigest(),places=[existing])
            facts.write_text(json.dumps(prior),encoding='utf-8')
            self.assertEqual([existing,fresh[1]],retain_existing_facts([place],fresh,dataset,facts))
            with self.assertRaises(ValueError):retain_existing_facts([dict(place,lat=35.16)],fresh,dataset,facts)
            with self.assertRaises(ValueError):retain_existing_facts([],fresh,dataset,facts)
            facts.write_text(json.dumps(dict(prior,datasetSha256='wrong')),encoding='utf-8')
            with self.assertRaises(ValueError):retain_existing_facts([place],fresh,dataset,facts)

    def test_offline_inputs_reuse_verified_public_sources_without_network(self):
        with patch('urllib.request.urlopen',side_effect=AssertionError('Network is not allowed')):
            sources,rail,buses,stations,models,national=load_inputs(True)
        self.assertEqual(10046,len(buses));self.assertEqual(135,len(stations))
        self.assertEqual(43,len(models));self.assertEqual(227102,national)
        self.assertEqual('15055847',sources[0]['identifier'])
        self.assertTrue(rail['retrieved_date']<'2026-11-16')


    def test_demo_fingerprint_requires_lf_without_modifying_input(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'demo.json'
            path.write_bytes(b'{"places": []}\n')
            self.assertEqual(path.read_bytes(), read_lf_bytes(path))
            path.write_bytes(b'{"places": []}\r\n')
            before = path.read_bytes()
            with self.assertRaisesRegex(ValueError, 'LF'):
                read_lf_bytes(path)
            self.assertEqual(before, path.read_bytes())


    def test_matching_preserves_branches_and_building_numbers(self):
        self.assertEqual(name_key('(주)사미헌'), name_key('사미헌'))
        self.assertNotEqual(name_key('카페 서면점'), name_key('카페 전포점'))
        self.assertEqual(road_key('부산광역시 부산진구 가야대로 554, 1층'), road_key('가야대로 554 (가야동)'))
        self.assertNotEqual(road_key('서면로 39'), road_key('서면로 39-1'))

    def test_coordinates_and_mixed_spreadsheet_dates(self):
        self.assertEqual(0, distance(35,129,35,129))
        self.assertAlmostEqual(111.195, distance(35,129,35.001,129), places=2)
        self.assertFalse(valid_point('0','0'))
        self.assertFalse(valid_point('','129'))
        self.assertTrue(valid_point('35.16','129.06'))
        self.assertEqual('2026-06-30', source_date('20260630'))
        self.assertEqual('2026-06-18', source_date('46191'))
        self.assertEqual('2022-11-30', source_date('2022-11-30'))

    def test_pack_is_bound_to_current_demo_and_inputs_are_unchanged(self):
        manifest=json.loads((OUT/'checksums.json').read_text(encoding='utf-8'))
        for path, digest in manifest['inputs'].items():
            self.assertEqual(digest,hashlib.sha256((ROOT/path).read_bytes()).hexdigest(),path)
        for path, digest in manifest['outputs'].items():
            self.assertEqual(digest,hashlib.sha256((OUT/path).read_bytes()).hexdigest(),path)
        pack=json.loads((OUT/'demo_appeal_facts.json').read_text(encoding='utf-8'))
        self.assertEqual(manifest['inputs']['data/places/demo/seomyeon-public-estimates.json'],pack['datasetSha256'])
        self.assertEqual(len(pack['places']),len({p['placeId'] for p in pack['places']}))
        for place in pack['places']:
            self.assertEqual(len(place['tags']),len({t['tag'] for t in place['tags']}))
            for tag in place['tags']:
                self.assertTrue(tag['sourceUrl'].startswith('https://'))
                self.assertTrue(tag['checkedAt'])

    def test_transit_facts_reference_acquired_records_and_use_straight_line_units(self):
        def rows(path):
            with path.open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))
        buses={r['정류장번호'] for r in rows(OUT/'raw/busan_bus_stops.csv')}
        stations={r['역사명'] for r in rows(OUT/'raw/busan_railway_stations.csv')}
        places=rows(OUT/'demo_nearest_transit.csv')
        self.assertEqual(386,len(places))
        for row in places:
            self.assertIn(row['bus_stop_id'],buses)
            self.assertIn(row['nearest_station'],stations)
            self.assertEqual('straight_line',row['distance_kind'])
            self.assertGreaterEqual(float(row['station_distance_m']),0)
            self.assertGreaterEqual(float(row['bus_distance_m']),0)


if __name__=='__main__':unittest.main()
