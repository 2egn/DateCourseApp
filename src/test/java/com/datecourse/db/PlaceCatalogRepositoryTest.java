package com.datecourse.db;

import com.datecourse.model.*;
import com.datecourse.demo.PublicPlaceImport;
import com.datecourse.service.PublicDemoPlaceRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.datecourse.model.UserSession;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaceCatalogRepositoryTest {
    HikariDataSource pool;
    PlaceCatalogRepository catalog;
    final Instant first=Instant.parse("2026-11-14T00:00:00Z");
    @BeforeEach void setup() throws Exception {
        pool=DatabasePool.create(new DbSettings("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        try(var c=pool.getConnection()) {
            for(String file:List.of("02_create_tables.sql","07_saved_courses.sql","08_saved_course_price_estimates.sql"))
                try(var reader=Files.newBufferedReader(Path.of("data/sql",file))){RunScript.execute(c,reader);}
            try(var s=c.createStatement()) {s.executeUpdate("INSERT INTO members(member_id,email,password_hash,name) VALUES(7,'demo@example.test','test','사용자')");}
        }
        catalog=new PlaceCatalogRepository(pool);
    }
    @AfterEach void close(){pool.close();}
    Place place(String key,String name,int price) {
        var details=new PlaceDetails("부산광역시 부산진구 서면로 1","한식","","","https://example.test","2026-11-14","","",Map.of("public",key));
        return new Place(2000001,"DCA-DEMO-"+key,name,"MEAL",35.1578,129.06,
                new PriceEstimate(price,price,PriceEstimate.Unit.PER_PERSON,"공공 메뉴",PriceEstimate.Evidence.REFERENCE,"https://example.test","2026-11-14"),0,details);
    }
    PlaceCatalogRepository.Batch batch(int generation,Place... places) {
        return new PlaceCatalogRepository.Batch("public-test",first.plusSeconds(generation),List.of(places));
    }
    int count(String table) throws Exception {
        try(var c=pool.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getInt(1);}
    }
    @Test void repeatedImportKeepsStableGlobalIdsAndNoDuplicateRevisions() throws Exception {
        var batch=batch(0,place("PUBLIC:A","가게",5000),place("PUBLIC:B","다른 가게",6000));
        assertEquals(2,catalog.synchronize(batch).inserted());
        var first=catalog.findAll();
        assertEquals(2,catalog.synchronize(batch).unchanged());
        assertEquals(first,new PlaceRepository(pool).findAll());
        assertEquals("PUBLIC:A",first.getFirst().placeId());
        assertTrue(first.getFirst().recordId()>0);
        assertEquals(2,count("place_catalog_entries"));assertEquals(2,count("data_records"));
        assertEquals(2,count("place_catalog_revisions"));assertEquals(2,count("place_catalog_imports"));
    }
    @Test void futureVersionFailsBeforeSchemaWritesButSmallClockSkewCanImport() throws Exception {
        var now=first.plusSeconds(10);
        var guarded=new PlaceCatalogRepository(pool,java.time.Clock.fixed(now,java.time.ZoneOffset.UTC));
        var place=place("PUBLIC:A","가게",5000);
        var future=new PlaceCatalogRepository.Batch("public-test",now.plusSeconds(7*60*60),List.of(place));
        assertThrows(IllegalArgumentException.class,()->guarded.synchronize(future));
        assertTrue(guarded.findImported().isEmpty());
        var slightSkew=new PlaceCatalogRepository.Batch("public-test",now.plusSeconds(60),List.of(place));
        assertEquals(1,guarded.synchronize(slightSkew).inserted());
        assertEquals(1,guarded.findAll().size());
    }
    @Test void publicUpdatePreservesApprovedCorrectionAndSavedSnapshot() throws Exception {
        catalog.synchronize(batch(0,place("PUBLIC:A","원래 가게",5000)));
        var before=catalog.findAll().getFirst();
        var saved=new SavedCourseRepository(pool);var session=new UserSession(7,"사용자");
        var snapshot=new RoutePlan(35.1578,129.06,2,1000,"기존 코스",List.of(before));
        saved.save(session,snapshot);
        var reports=new InformationReportRepository(pool);
        long report=reports.submit(session,before,InformationReport.Field.PRICE,"7000","메뉴 확인","https://example.test/menu");
        reports.review(report,true,"7000","관리자 메뉴 확인");
        assertEquals(7000,reports.apply(catalog.findAll()).getFirst().price());
        assertEquals(1,catalog.synchronize(batch(1,place("PUBLIC:A","원본 갱신 가게",9000))).updated());
        var raw=new PlaceRepository(pool).findAll().getFirst();
        var effective=reports.apply(List.of(raw)).getFirst();
        assertEquals(9000,raw.price());assertEquals(7000,effective.price());
        assertEquals("원본 갱신 가게",effective.name());assertEquals(before.recordId(),raw.recordId());
        assertEquals(PriceEstimate.Evidence.ADMIN_REVIEWED,effective.priceEstimate().evidence());
        assertEquals(snapshot,saved.findAll(session).getFirst().plan());
        assertEquals(2,count("place_catalog_revisions"));
    }
    @Test void regionalFragmentCannotDeactivateExistingPlacesInExpansionMode() throws Exception {
        var a=place("PUBLIC:A","서면 가게",5000);var b=place("PUBLIC:B","수영구 가게",6000);
        catalog.synchronize(batch(0,a));
        var old=catalog.findAll();
        assertThrows(IllegalArgumentException.class,()->catalog.synchronize(batch(1,b),PlaceCatalogRepository.ImportMode.EXPANSION));
        assertEquals(old,catalog.findAll());
        assertEquals(1,count("place_catalog_imports"));assertEquals(1,count("place_catalog_revisions"));
        var expanded=catalog.synchronize(batch(1,a,b),PlaceCatalogRepository.ImportMode.EXPANSION);
        assertEquals(1,expanded.inserted());assertEquals(1,expanded.unchanged());assertEquals(0,expanded.deactivated());
        assertEquals(old.getFirst().recordId(),catalog.findAll().getFirst().recordId());
        assertEquals(2,catalog.synchronize(batch(1,a,b),PlaceCatalogRepository.ImportMode.EXPANSION).unchanged());
    }
    @Test void missingPlacesDeactivateAndReappearWithSameRecordNumber() throws Exception {
        catalog.synchronize(batch(0,place("PUBLIC:A","가게",5000),place("PUBLIC:B","다른 가게",6000)));
        long id=catalog.findAll().get(1).recordId();
        assertEquals(1,catalog.synchronize(batch(1,place("PUBLIC:A","가게",5000))).deactivated());
        assertEquals(1,catalog.findAll().size());assertEquals(2,count("data_records"));
        assertEquals(1,catalog.synchronize(batch(2,place("PUBLIC:A","가게",5000),place("PUBLIC:B","돌아온 가게",7000))).reactivated());
        assertEquals(id,catalog.findAll().get(1).recordId());
    }
    @Test void priceOnlyUpdatePreservesIdsSavedSnapshotsAndAdministratorCorrections() throws Exception {
        var a=place("PUBLIC:A","가게",5000);
        catalog.synchronize(batch(0,a));
        var before=catalog.findAll().getFirst();
        var session=new UserSession(7,"사용자");
        var saved=new SavedCourseRepository(pool);
        var plan=new RoutePlan(35.1578,129.06,2,1000,"기존 코스",List.of(before));
        saved.save(session,plan);
        var reports=new InformationReportRepository(pool);
        long report=reports.submit(session,before,InformationReport.Field.PRICE,"7000","메뉴 확인","https://example.test");
        reports.review(report,true,"7000","검토 완료");
        var next=batch(1,place("PUBLIC:A","가게",8071));
        assertEquals(1,catalog.synchronize(next,PlaceCatalogRepository.ImportMode.PRICE_UPDATE).updated());
        assertEquals(before.recordId(),catalog.findAll().getFirst().recordId());
        assertEquals(plan,saved.findAll(session).getFirst().plan());
        assertEquals(7000,reports.apply(catalog.findAll()).getFirst().price());
        assertEquals(1,catalog.synchronize(next,PlaceCatalogRepository.ImportMode.PRICE_UPDATE).unchanged());
    }
    @Test void priceOnlyUpdateRejectsIdentityChangeAdditionAndRemovalBeforeWrites() throws Exception {
        var a=place("PUBLIC:A","가게",5000);var b=place("PUBLIC:B","다른 가게",6000);
        catalog.synchronize(batch(0,a,b));
        var before=catalog.findAll();
        for(var invalid:List.of(batch(1,a),batch(1,a,b,place("PUBLIC:C","추가",8000)),
                batch(1,place("PUBLIC:A","이름 변경",9000),b))) {
            assertThrows(IllegalArgumentException.class,()->catalog.synchronize(invalid,PlaceCatalogRepository.ImportMode.PRICE_UPDATE));
            assertEquals(before,catalog.findAll());
            assertEquals(1,count("place_catalog_imports"));
        }
    }
    @Test void staleOrConflictingGenerationNeverChangesCommittedData() throws Exception {
        catalog.synchronize(batch(2,place("PUBLIC:A","가게",5000)));
        assertThrows(IllegalArgumentException.class,()->catalog.synchronize(batch(1,place("PUBLIC:A","이전",1000))));
        assertThrows(IllegalArgumentException.class,()->catalog.synchronize(batch(2,place("PUBLIC:A","다른 내용",1000))));
        assertEquals("가게",catalog.findAll().getFirst().name());assertEquals(1,count("place_catalog_imports"));
    }
    @Test void lateOwnershipConflictRollsBackNewRowsAndHistory() throws Exception {
        catalog.synchronize(new PlaceCatalogRepository.Batch("other-source",first,List.of(place("PUBLIC:Z","다른 원본",5000))));
        var input=batch(1,place("PUBLIC:A","먼저 추가",6000),place("PUBLIC:Z","충돌",9000));
        assertThrows(IllegalArgumentException.class,()->catalog.synchronize(input));
        assertEquals(1,count("place_catalog_entries"));assertEquals(1,count("data_records"));
        assertEquals(1,count("place_catalog_imports"));assertEquals(1,count("place_catalog_revisions"));
        assertEquals("다른 원본",catalog.findAll().getFirst().name());
    }
    @Test void malformedDuplicateOrEmptySnapshotsFailBeforeWrites() throws Exception {
        var place=place("PUBLIC:A","가게",5000);
        assertThrows(IllegalArgumentException.class,()->new PlaceCatalogRepository.Batch("public-test",first,List.of()));
        assertThrows(IllegalArgumentException.class,()->catalog.synchronize(batch(0,place,place)));
        assertTrue(catalog.findImported().isEmpty());
        var d=place.details();var broken=new Place(1,place.placeId(),place.name(),place.category(),Double.NaN,129,place.priceEstimate(),0,d);
        assertThrows(IllegalArgumentException.class,()->catalog.synchronize(batch(0,broken)));
    }
    @Test void concurrentSameSnapshotDoesNotCreateDuplicates() throws Exception {
        catalog.initialize();
        var input=batch(0,place("PUBLIC:A","가게",5000));
        var start=new CountDownLatch(1);
        try(var workers=Executors.newFixedThreadPool(2)) {
            Callable<PlaceCatalogRepository.ImportResult> work=()->{start.await();return new PlaceCatalogRepository(pool).synchronize(input);};
            var a=workers.submit(work);var b=workers.submit(work);start.countDown();
            var one=a.get(20,TimeUnit.SECONDS);var two=b.get(20,TimeUnit.SECONDS);
            assertEquals(1,one.inserted()+two.inserted());assertEquals(1,one.unchanged()+two.unchanged());
        }
        assertEquals(1,count("place_catalog_entries"));assertEquals(1,count("data_records"));
    }
    @Test void sqlFailureAfterFirstRowRollsBackWholeBatch() throws Exception {
        catalog.initialize();
        try(var c=pool.getConnection();var statement=c.createStatement()) {
            statement.executeUpdate("ALTER TABLE place_catalog_entries ADD CONSTRAINT reject_z CHECK(place_key <> 'PUBLIC:Z')");
        }
        assertThrows(SQLException.class,()->catalog.synchronize(batch(0,place("PUBLIC:A","가게",5000),place("PUBLIC:Z","실패",6000))));
        assertEquals(0,count("data_records"));assertEquals(0,count("place_catalog_entries"));
        assertEquals(0,count("place_catalog_revisions"));assertEquals(0,count("place_catalog_imports"));
        assertTrue(catalog.findImported().isEmpty());
    }
    @Test void multipleWriteBatchesPreserveIdsAndRevisionKindsIncludingFinalRemainder() throws Exception {
        var original=java.util.stream.IntStream.range(0,405)
            .mapToObj(i->place(String.format("PUBLIC:%04d",i),"가게",5000)).toArray(Place[]::new);
        catalog.synchronize(batch(0,original));
        var before=catalog.findAll();
        var updated=java.util.stream.IntStream.range(0,405)
            .mapToObj(i->place(String.format("PUBLIC:%04d",i),"가게",6000)).toArray(Place[]::new);
        assertEquals(405,catalog.synchronize(batch(1,updated),PlaceCatalogRepository.ImportMode.PRICE_UPDATE).updated());
        assertEquals(200,catalog.synchronize(batch(2,Arrays.copyOf(updated,205))).deactivated());
        var reactivated=catalog.synchronize(batch(3,updated));
        assertEquals(200,reactivated.reactivated());assertEquals(205,reactivated.unchanged());
        assertEquals(405,catalog.synchronize(batch(3,updated)).unchanged());
        assertEquals(before.stream().map(Place::recordId).toList(),catalog.findAll().stream().map(Place::recordId).toList());
        assertTrue(catalog.findAll().stream().allMatch(p->p.price()==6000));
        assertEquals(1210,count("place_catalog_revisions"));
        try(var c=pool.getConnection();var s=c.createStatement();var rows=s.executeQuery(
                "SELECT change_kind,COUNT(*) FROM place_catalog_revisions GROUP BY change_kind")) {
            var kinds=new HashMap<String,Integer>();while(rows.next())kinds.put(rows.getString(1),rows.getInt(2));
            assertEquals(Map.of("INSERT",405,"UPDATE",405,"DEACTIVATE",200,"REACTIVATE",200),kinds);
        }
    }
    @Test void lateRevisionFailureRollsBackPreviouslyExecutedWriteBatches() throws Exception {
        var places=java.util.stream.IntStream.range(0,405)
            .mapToObj(i->place(String.format("PUBLIC:%04d",i),"가게",5000)).toArray(Place[]::new);
        catalog.synchronize(batch(0,places));
        var before=catalog.findAll();
        try(var c=pool.getConnection();var s=c.createStatement()) {
            s.executeUpdate("ALTER TABLE place_catalog_revisions ADD CONSTRAINT reject_late_update CHECK(change_kind <> 'UPDATE' OR place_key <> 'PUBLIC:0404')");
        }
        var updated=java.util.stream.IntStream.range(0,405)
            .mapToObj(i->place(String.format("PUBLIC:%04d",i),"가게",6000)).toArray(Place[]::new);
        assertThrows(SQLException.class,()->catalog.synchronize(batch(1,updated),PlaceCatalogRepository.ImportMode.PRICE_UPDATE));
        assertEquals(before,catalog.findAll());
        assertEquals(1,count("place_catalog_imports"));assertEquals(405,count("place_catalog_revisions"));
        assertEquals(405,catalog.synchronize(batch(0,places)).unchanged());
    }
    @Test void corruptionIsReportedInsteadOfFallingBackToLegacyData() throws Exception {
        catalog.synchronize(batch(0,place("PUBLIC:A","가게",5000)));
        try(var c=pool.getConnection();var statement=c.createStatement()) {
            statement.executeUpdate("UPDATE place_catalog_entries SET payload='{}'");
        }
        assertThrows(java.io.IOException.class,()->new PlaceRepository(pool).findAll());
    }
    @Test void importingReviewedOverlayAsPublicSourceIsRejected() throws Exception {
        catalog.synchronize(batch(0,place("PUBLIC:A","가게",5000)));
        var original=catalog.findAll().getFirst();
        var reports=new InformationReportRepository(pool);
        long id=reports.submit(new UserSession(7,"사용자"),original,InformationReport.Field.NAME,"수정된 이름","제보","");
        reports.review(id,true,"수정된 이름","검토");
        var overlay=reports.apply(List.of(original)).getFirst();
        assertThrows(IllegalArgumentException.class,()->catalog.synchronize(batch(1,overlay)));
        assertEquals("가게",catalog.findAll().getFirst().name());
    }
    @Test void expandedPublicPlacesRoundTripWithFactsAndUnreviewedLinks() throws Exception {
        var input=PublicPlaceImport.batch();
        assertEquals(386,catalog.synchronize(input).inserted());
        assertEquals(386,catalog.synchronize(input).unchanged());
        var places=new PlaceRepository(pool).findAll();
        assertEquals(386,places.size());
        assertEquals(368,places.stream().filter(p->p.details().hasTag(PlaceTag.NEAR_SUBWAY)).count());
        assertTrue(places.stream().flatMap(p->p.details().externalLinks().stream()).noneMatch(ExternalPlaceLink::usable));
        assertTrue(places.stream().allMatch(p->p.placeId().equals(p.details().externalIds().get("public"))));
        var original=new PublicDemoPlaceRepository().findAll();
        assertEquals(original.getFirst().priceEstimate(),places.stream().filter(p->p.placeId().equals(original.getFirst().details().externalIds().get("public"))).findFirst().orElseThrow().priceEstimate());
    }
    @Test void stagedDistrictExpansionLoadsThroughProductionModelAndRetainsExistingCatalog() throws Exception {
        var staged=PublicPlaceImport.batch(Path.of("data/places/expansion/suyeong-2026-11-16/places.json"),
                Path.of("data/places/expansion/suyeong-2026-11-16/appeal/demo_appeal_facts.json"));
        // A bundled baseline may be regenerated later; seed the prior version explicitly.
        var original=new PlaceCatalogRepository.Batch(staged.sourceKey(),staged.generatedAt().minusSeconds(1),PublicPlaceImport.batch().places());
        catalog.synchronize(original);
        var old=catalog.findAll();
        var result=catalog.synchronize(staged,PlaceCatalogRepository.ImportMode.EXPANSION);
        assertEquals(480,result.inserted());assertEquals(386,result.unchanged());assertEquals(0,result.deactivated());
        var all=catalog.findAll();assertEquals(866,all.size());
        for(var before:old)assertEquals(before,all.stream().filter(p->p.stableKey().equals(before.stableKey())).findFirst().orElseThrow());
        var condition=new NextPlaceCondition("식사",20000,2000,true,Set.of(),Set.of(),30);
        var service=new com.datecourse.service.NextPlaceService();
        var candidates=service.candidates(all,List.of(),35.1532,129.1189,2,2000,condition);
        assertEquals(30,candidates.size());
        assertTrue(candidates.stream().allMatch(p->p.place().details().address().contains("수영구")));
        assertEquals(866,catalog.synchronize(staged,PlaceCatalogRepository.ImportMode.EXPANSION).unchanged());
    }
    @Test void sourceFingerprintIgnoresInputOrderAndDemoRecordNumber() throws Exception {
        var a=place("PUBLIC:A","가게",5000);var b=place("PUBLIC:B","다른 가게",6000);
        var alternate=new Place(777,"another-display-id",a.name(),a.category(),a.lat(),a.lng(),a.priceEstimate(),a.rating(),a.details());
        assertEquals(PlaceCatalogRepository.prepare(batch(0,a,b)).fingerprint(),PlaceCatalogRepository.prepare(batch(0,b,alternate)).fingerprint());
    }
}
