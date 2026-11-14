"""Validate acquisition boundaries and the generated offline fact pack."""
import csv
import hashlib
import json
import unittest
from collect_busan_appeal_data import ROOT, OUT, distance, name_key, road_key, source_date, valid_point


class AppealDataTest(unittest.TestCase):
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
        self.assertEqual(127,len(places))
        for row in places:
            self.assertIn(row['bus_stop_id'],buses)
            self.assertIn(row['nearest_station'],stations)
            self.assertEqual('straight_line',row['distance_kind'])
            self.assertGreaterEqual(float(row['station_distance_m']),0)
            self.assertGreaterEqual(float(row['bus_distance_m']),0)


if __name__=='__main__':unittest.main()
