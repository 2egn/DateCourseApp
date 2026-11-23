import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from supplement_place_prices import choose_price, stage


class PriceSupplementTest(unittest.TestCase):
    def setUp(self):
        self.place = dict(name='부산칼국수', category='MEAL',
                          details=dict(address='부산광역시 부산진구 중앙대로 1', subcategory='국수/칼국수',
                                       externalIds=dict(public='SBIZ:1')),
                          priceEstimate=dict(minimum=None,maximum=None,unit='UNKNOWN',evidence='UNKNOWN'))
        self.regional = dict(kind='REGIONAL',region='부산',menu='칼국수',unit='PER_PERSON',amount=8071,
                             period='2026-10',sourceUrl='https://example.test',label='부산 지역별 외식비',
                             servingVerified=True)

    def test_regional_remains_estimated_and_unknown_does_not_become_free(self):
        price, _ = choose_price(self.place,[self.regional],[])
        self.assertEqual('SIMILAR_ESTIMATE',price['evidence'])
        self.assertEqual(8071,price['minimum'])
        self.assertEqual(self.place['priceEstimate'],choose_price(self.place,[],[])[0])

    def test_menu_region_unit_must_match(self):
        for field, value in [('region','서울'),('menu','냉면'),('unit','UNKNOWN'),('servingVerified',False),
                             ('amount',0),('amount',True),('sourceUrl',''),('servingVerified','false')]:
            source = self.regional | {field:value}
            self.assertIsNone(choose_price(self.place,[source],[])[1])
        self.place['name']='부산밀면'
        self.assertIsNone(choose_price(self.place,[self.regional | dict(menu='냉면')],[])[1])

    def test_venue_priority_requires_full_identity_and_known_serving(self):
        venue = self.regional | dict(kind='VENUE',publicPlaceId='SBIZ:1',name=self.place['name'],
                                      address=self.place['details']['address'],amount=6000,label='매장 공개 메뉴')
        price, _ = choose_price(self.place,[self.regional,venue],['칼국수'])
        self.assertEqual(6000,price['minimum'])
        self.assertEqual('REFERENCE',price['evidence'])
        for field,value in [('publicPlaceId','SBIZ:2'),('name','다른분점'),('address','다른주소'),
                            ('servingVerified',False)]:
            result,_=choose_price(self.place,[self.regional,venue | {field:value}],['칼국수'])
            self.assertEqual(8071,result['minimum'])
        self.assertIsNone(choose_price(self.place,[venue],[])[1])

    def test_existing_reference_is_not_overwritten(self):
        self.place['priceEstimate']=dict(minimum=0,maximum=0,unit='ADULT_TICKET',evidence='REFERENCE')
        self.assertEqual(self.place['priceEstimate'],choose_price(self.place,[self.regional],[])[0])

    def test_survey_requires_supported_cohort_and_marks_cup_assumption(self):
        self.place['category']='CAFE'
        self.place['details']['subcategory']='카페'
        source=self.regional | dict(kind='SURVEY',region='전국',menu='커피',unit='PER_DRINK',
                                    amount=4118,sampleCount=530,label='외식업체 조사',assumption='1잔 가정')
        price,_=choose_price(self.place,[source],[])
        self.assertEqual('SIMILAR_ESTIMATE',price['evidence'])
        self.assertIn('1잔 가정',price['basis'])
        self.assertIsNone(choose_price(self.place,[source | dict(sampleCount=2)],[])[1])
        self.place['details']['subcategory']='찻집'
        self.assertIsNone(choose_price(self.place,[source],[])[1])

    def test_multiple_menus_form_range_instead_of_average(self):
        second=self.regional | dict(menu='비빔밥',amount=9686)
        price,_=choose_price(self.place,[self.regional,second],['칼국수','비빔밥'])
        self.assertEqual((8071,9686),(price['minimum'],price['maximum']))

    def test_stage_preserves_identity_and_facts(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            data=dict(places=[self.place])
            facts=dict(places=[dict(placeId='x',tags=[])],datasetSha256='old',generatedAt='old')
            for name,value in [('data',data),('facts',facts),('sources',dict(sources=[self.regional]))]:
                (root/name).write_text(json.dumps(value),encoding='utf-8')
            facts['datasetSha256']=hashlib.sha256((root/'data').read_bytes()).hexdigest()
            (root/'facts').write_text(json.dumps(facts),encoding='utf-8')
            report=stage(root/'data',root/'facts',root/'sources',{},root/'out','2026-11-24T01:00:00Z')
            new=json.loads((root/'out/places.json').read_text(encoding='utf-8'))['places'][0]
            self.assertNotIn(b'\r\n',(root/'out/places.json').read_bytes())
            self.assertEqual(self.place['details'],new['details'])
            self.assertEqual(self.place['name'],new['name'])
            new_facts=json.loads((root/'out/appeal/demo_appeal_facts.json').read_text(encoding='utf-8'))
            self.assertEqual(facts['places'],new_facts['places'])
            self.assertEqual(1,report['changed'])
            self.assertEqual(None,self.place['priceEstimate']['minimum'])

    def test_wrong_fact_snapshot_stops_before_writing(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            (root/'data').write_text(json.dumps(dict(places=[self.place])),encoding='utf-8')
            (root/'facts').write_text(json.dumps(dict(datasetSha256='wrong')),encoding='utf-8')
            with self.assertRaises(ValueError):
                stage(root/'data',root/'facts',root/'sources',{},root/'out','2026-11-24T01:00:00Z')
            self.assertFalse((root/'out').exists())

    def test_output_cannot_overwrite_any_input(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            for name in ['places.json','appeal/demo_appeal_facts.json','price-review.json']:
                source=root/name
                source.parent.mkdir(parents=True,exist_ok=True)
                source.write_bytes(b'baseline must remain')
                with self.assertRaisesRegex(ValueError,'기준본'):
                    stage(root/'other-places.json',source,root/'sources.json',{},root,'2026-11-24T01:00:00Z')
                self.assertEqual(b'baseline must remain',source.read_bytes())


if __name__ == '__main__':
    unittest.main()
