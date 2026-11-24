package com.datecourse.db;

import com.datecourse.demo.PublicPlaceImport;
import com.datecourse.model.Place;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpansionAuditTest {
    HikariDataSource pool;
    PlaceCatalogRepository catalog;
    PlaceCatalogRepository.Batch baseline, expanded;
    @BeforeEach void setup() throws Exception {
        pool=DatabasePool.create(new DbSettings("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        try(var c=pool.getConnection()) {
            for(String file:List.of("02_create_tables.sql","07_saved_courses.sql","08_saved_course_price_estimates.sql"))
                try(var reader=Files.newBufferedReader(Path.of("data/sql",file))){RunScript.execute(c,reader);}
            try(var s=c.createStatement()){s.execute("CREATE TABLE protected_example(note VARCHAR(30))");s.execute("INSERT INTO protected_example VALUES('must remain')");}
        }
        com.datecourse.maintenance.DatabaseSetup.prepareReports(pool);
        com.datecourse.maintenance.DatabaseSetup.prepareCatalog(pool);
        com.datecourse.maintenance.DatabaseSetup.preparePreviews(pool);
        com.datecourse.maintenance.DatabaseSetup.prepareTransport(pool);
        new InformationReportRepository(pool).initialize();
        baseline=PublicPlaceImport.batch(Path.of("data/places/expansion/suyeong-2026-11-16/places.json"),Path.of("data/places/expansion/suyeong-2026-11-16/appeal/demo_appeal_facts.json"));
        expanded=PublicPlaceImport.batch(Path.of("data/places/expansion/busan-core-2026-11-23/places.json"),Path.of("data/places/expansion/busan-core-2026-11-23/appeal/demo_appeal_facts.json"));
        catalog=new PlaceCatalogRepository(pool);catalog.synchronize(baseline);
    }
    @AfterEach void close(){pool.close();}
    @Test void completeExpansionPreservesExistingIdsAndProtectedTables() throws Exception {
        var before=catalog.findAll();
        var preflight=ExpansionAudit.verify(pool,baseline,expanded,false);
        assertEquals(false,preflight.get("applied"));assertEquals(before,catalog.findAll());
        var result=ExpansionAudit.verify(pool,baseline,expanded,true);
        assertEquals(2040,result.get("activeAfter"));assertEquals(2040,result.get("effectiveAfter"));
        assertEquals(0,result.get("payloadMismatches"));assertEquals(true,result.get("existingRecordIdsPreserved"));
        assertTrue(((List<?>)result.get("unchangedTables")).stream().anyMatch(t->t.toString().equalsIgnoreCase("protected_example")));
        assertEquals(1174,((PlaceCatalogRepository.ImportResult)result.get("result")).inserted());
        assertEquals(2040,((PlaceCatalogRepository.ImportResult)result.get("repeat")).unchanged());
        var all=catalog.findAll();for(var old:before)assertEquals(old,all.stream().filter(p->p.stableKey().equals(old.stableKey())).findFirst().orElseThrow());
    }
    @Test void changedOrMissingBaselineStopsBeforeAnyWrite() throws Exception {
        var first=expanded.places().getFirst();var list=new ArrayList<>(expanded.places());
        list.set(0,new Place(first.recordId(),first.placeId(),first.name()+" changed",first.category(),first.lat(),first.lng(),first.priceEstimate(),first.rating(),first.details()));
        var changed=new PlaceCatalogRepository.Batch(expanded.sourceKey(),expanded.generatedAt(),list);
        assertThrows(IllegalStateException.class,()->ExpansionAudit.verify(pool,baseline,changed,true));
        list.removeFirst();var missing=new PlaceCatalogRepository.Batch(expanded.sourceKey(),expanded.generatedAt(),list);
        assertThrows(IllegalStateException.class,()->ExpansionAudit.verify(pool,baseline,missing,true));
        assertEquals(866,catalog.findAll().size());
    }
    @Test void priceAuditUpdatesOnlyPricesAndKeepsAllExistingRecordIds() throws Exception {
        var list=new ArrayList<>(baseline.places());var p=list.getFirst();
        list.set(0,new Place(p.recordId(),p.placeId(),p.name(),p.category(),p.lat(),p.lng(),
            new com.datecourse.model.PriceEstimate(8071,8071,com.datecourse.model.PriceEstimate.Unit.PER_PERSON,
                "부산 지역별 외식비 · 칼국수",com.datecourse.model.PriceEstimate.Evidence.SIMILAR_ESTIMATE,
                "https://example.test","2026-10"),p.rating(),p.details()));
        var changed=new PlaceCatalogRepository.Batch(baseline.sourceKey(),baseline.generatedAt().plusSeconds(1),list);
        var result=ExpansionAudit.verify(pool,baseline,changed,true,true);
        assertEquals(true,result.get("nonPriceFieldsPreserved"));
        assertEquals(1,((PlaceCatalogRepository.ImportResult)result.get("result")).updated());
        assertEquals(866,result.get("activeAfter"));
        assertEquals(866,((PlaceCatalogRepository.ImportResult)result.get("repeat")).unchanged());
    }
}
