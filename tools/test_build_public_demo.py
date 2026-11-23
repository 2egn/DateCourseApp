import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from build_public_demo import ROOT, POLICY, build, load_policy, select_places, valid_point

class PublicExpansionTest(unittest.TestCase):
    def test_spatial_sampling_keeps_existing_places_and_reaches_other_neighborhoods(self):
        rows=[dict(place_id=str(i),name='카페 '+str(i),road_address='도로 '+str(i),lot_address='',
                   category='CAFE',industry_name='카페',lat=35.15+i*.00001,lon=129.11) for i in range(5)]
        rows.append(dict(rows[0],place_id='far',name='다른 동네',road_address='먼 도로',lat=35.17))
        policy=load_policy(POLICY);policy.update(categoryLimits=dict(MEAL=1,CAFE=3,CULTURE=1),
            preferredPerCuisine=0,preferredDesignated=0,center=dict(lat=35.15,lng=129.11),spatialCellMeters=500)
        selected=select_places(rows,['0'],[],set(),set(),lambda p:None,policy)
        self.assertEqual('0',selected[0]['place_id'])
        self.assertEqual(3,len(selected))
        self.assertIn('far',[p['place_id'] for p in selected])
        self.assertEqual(selected,select_places(rows,['0'],[],set(),set(),lambda p:None,policy))
        blocked=select_places(rows,['0'],[],{'far'},set(),lambda p:None,policy)
        self.assertNotIn('far',[p['place_id'] for p in blocked])

    def test_spatial_policy_rejects_invalid_cell_sizes(self):
        policy=load_policy(POLICY)
        with tempfile.TemporaryDirectory() as directory:
            target=Path(directory)/'policy.json'
            for invalid in [True,0,-1,'500',float('nan'),float('inf')]:
                target.write_text(json.dumps(dict(policy,spatialCellMeters=invalid)),encoding='utf-8')
                with self.assertRaises(ValueError):load_policy(target)

    def test_expanded_snapshot_retains_original_ids_coordinates_and_prices(self):
        directory=ROOT/'data/places/demo'
        source=(directory/'seomyeon-public-estimates.json').read_bytes()
        self.assertNotIn(b'\r\n',source)
        rows=json.loads(source)['places']
        self.assertEqual(386,len(rows))
        expected={'MEAL':240,'CAFE':120,'CULTURE':26}
        for category,count in expected.items():self.assertEqual(count,sum(p['category']==category for p in rows))
        indexed={p['details']['externalIds']['public']:p for p in rows}
        self.assertEqual(len(rows),len(indexed));self.assertEqual(len(rows),len({p['recordId'] for p in rows}))
        baseline=json.loads((directory/'expansion-baseline-2026-11-16.json').read_text(encoding='utf-8'))
        self.assertEqual(127,len(baseline['places']))
        for old in baseline['places']:
            current=indexed[old['publicId']]
            for field in ['placeId','recordId','lat','lng']:self.assertEqual(old[field],current[field])
            digest=hashlib.sha256(json.dumps(current['priceEstimate'],sort_keys=True,ensure_ascii=False,separators=(',',':')).encode()).hexdigest()
            self.assertEqual(old['priceSha256'],digest)
        self.assertEqual(119,sum(p['priceEstimate']['evidence']=='UNKNOWN' for p in rows))
        self.assertTrue(all(p['priceEstimate']['evidence']!='GOOGLE_PROVIDED' for p in rows))

    def test_rebuilding_same_snapshot_is_byte_stable_and_offline(self):
        original=(ROOT/'data/places/demo/seomyeon-public-estimates.json').read_bytes()
        with tempfile.TemporaryDirectory() as directory:
            target=Path(directory)/'places.json';target.write_bytes(original)
            with patch('urllib.request.urlopen',side_effect=AssertionError('Network is not allowed')):
                build(POLICY,target)
            self.assertEqual(original,target.read_bytes())

    def test_policy_and_selection_protect_existing_rows_and_enforce_caps(self):
        policy=load_policy(POLICY);policy['categoryLimits']={'MEAL':2,'CAFE':1,'CULTURE':1}
        policy['preferredPerCuisine']=policy['preferredDesignated']=0
        def row(pid,name,address,kind='MEAL'):
            return {'place_id':pid,'name':name,'road_address':address,'lot_address':address,'category':kind,'industry_name':kind}
        old=row('old','기존','도로 1');duplicate=row('duplicate',' 기 존 ','도로 1')
        blocked=row('blocked','제외','도로 2');new=row('new','신규','도로 3');extra=row('extra','추가','도로 4')
        result=select_places([duplicate,blocked,new,old,extra],['old'],[],{'blocked'},set(),lambda p:None,policy)
        self.assertEqual(['old','new'],[r['place_id'] for r in result])
        with self.assertRaises(ValueError):select_places([old],['missing'],[],set(),set(),lambda p:None,policy)
        with self.assertRaises(ValueError):select_places([old],['old'],[],{'old'},set(),lambda p:None,policy)
        self.assertFalse(valid_point({'lat':'nan','lon':'129'}))
        self.assertFalse(valid_point({'lat':'35','lon':'181'}))
        with tempfile.TemporaryDirectory() as directory:
            invalid=Path(directory)/'policy.json';policy['categoryLimits']['MEAL']=-1
            invalid.write_text(json.dumps(policy),encoding='utf-8')
            with self.assertRaises(ValueError):load_policy(invalid)

if __name__=='__main__':unittest.main()
