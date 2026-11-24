package com.datecourse.db;

import com.datecourse.page.review.InformationReviewController;

import com.datecourse.model.*;
import com.zaxxer.hikari.HikariDataSource;
import com.datecourse.model.UserSession;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InformationReportRepositoryTest {
    HikariDataSource pool; InformationReportRepository reports;
    UserSession owner=new UserSession(7,"사용자");
    Place original=new Place(1,"public:one","식당","MEAL",35,129,
        new PriceEstimate(6000,6000,PriceEstimate.Unit.PER_PERSON,"추정",PriceEstimate.Evidence.SIMILAR_ESTIMATE,"",""),0,PlaceDetails.EMPTY);
    @BeforeEach void setup() throws Exception {
        pool=DatabasePool.create(new DbSettings("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL","sa",""));
        try(var c=pool.getConnection()) {
            for(String f:List.of("02_create_tables.sql","07_saved_courses.sql","08_saved_course_price_estimates.sql"))
                try(var reader=Files.newBufferedReader(Path.of("data/sql",f))){RunScript.execute(c,reader);}
            try(var s=c.createStatement()){s.executeUpdate("INSERT INTO members(member_id,email,password_hash,name) VALUES (7,'demo@example.test','test','사용자')");}
        }
        com.datecourse.maintenance.DatabaseSetup.prepareReports(pool);
        com.datecourse.maintenance.DatabaseSetup.prepareCatalog(pool);
        com.datecourse.maintenance.DatabaseSetup.preparePreviews(pool);
        com.datecourse.maintenance.DatabaseSetup.prepareTransport(pool);
        reports=new InformationReportRepository(pool);
    }
    @AfterEach void close(){pool.close();}
    @Test void pendingReportsStayReachableBeyondThreeHundredNewerReports() throws Exception {
        long oldest=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","오래된 미검토","");
        for(int i=0;i<305;i++)reports.submit(owner,original,InformationReport.Field.PRICE,"9000","최근 제보","");
        var seen=new java.util.HashSet<Long>();Long cursor=null;int pages=0;
        do {
            var page=reports.reports(InformationReportRepository.ReportFilter.PENDING,cursor,50);
            assertTrue(page.items().size()<=50);
            for(var item:page.items()){assertEquals("PENDING",item.status());assertTrue(seen.add(item.id()));}
            cursor=page.nextBeforeId();pages++;
        } while(cursor!=null);
        assertEquals(306,seen.size());assertTrue(seen.contains(oldest));assertEquals(7,pages);
        reports.review(oldest,false,"","검토 후 반려");
        var rejected=reports.reports(InformationReportRepository.ReportFilter.REJECTED,null,50);
        assertEquals(List.of(oldest),rejected.items().stream().map(InformationReport::id).toList());
        assertNull(rejected.nextBeforeId());
        assertThrows(IllegalArgumentException.class,()->reports.reports(InformationReportRepository.ReportFilter.ALL,-1L,50));
    }

    @Test void reviewedIdentityCorrectionInvalidatesLinkAndRetainsOtherEvidence() throws Exception {
        var link=new ExternalPlaceLink(ExternalPlaceLink.Provider.GOOGLE_PLACES,"google_id",ExternalPlaceLink.Confidence.HIGH,
            ExternalPlaceLink.ReviewStatus.CONFIRMED,ExternalPlaceLink.EntityKind.VENUE,"name_phone","2026-11-14T00:00:00Z","동일성 검토");
        var fact=new PlaceAttributeFact(PlaceFeature.RESERVABLE,FactState.AVAILABLE,
            new SourceEvidence(SourceEvidence.Origin.OFFICIAL_SITE,"예약 안내","https://example.test","2026-11-14",""));
        var details=new PlaceDetails("이전 주소","한식","","","","","","",Map.of("google","google_id"),List.of(),List.of(fact),List.of(link));
        var place=new Place(original.recordId(),original.placeId(),original.name(),original.category(),35,129,original.priceEstimate(),0,details);
        long id=reports.submit(owner,place,InformationReport.Field.ADDRESS,"새 주소","주소 오류","https://example.test");
        assertTrue(reports.apply(List.of(place)).getFirst().details().externalLinks().getFirst().usable());
        reports.review(id,true,"새 주소","도로명 주소 확인");
        var updated=reports.apply(List.of(place)).getFirst();
        assertEquals("새 주소",updated.details().address());
        assertFalse(updated.details().externalLinks().getFirst().usable());
        assertEquals(FactState.AVAILABLE,updated.details().featureState(PlaceFeature.RESERVABLE));
        assertEquals(SourceEvidence.Origin.ADMIN_REVIEW,updated.details().evidenceFor(PlaceField.ADDRESS).origin());
    }
    @Test void reviewedPaidPriceRemovesFreeFactWithoutDroppingOtherMetadata() throws Exception {
        var evidence=new SourceEvidence(SourceEvidence.Origin.PUBLIC_DATA,"무료 입장","https://example.test","2026-11-14","");
        var fact=new PlaceAttributeFact(PlaceFeature.FREE_ADMISSION,FactState.AVAILABLE,evidence);
        var details=new PlaceDetails("","","","","","","","",Map.of(),
            List.of(new PlaceDetails.TagFact(PlaceTag.FREE_ADMISSION,"무료","https://example.test","2026-11-14")),List.of(fact),List.of(),Map.of(PlaceField.NAME,evidence));
        var place=new Place(1,"public:free","시설","CULTURE",35,129,PriceEstimate.exact(0),0,details);
        long id=reports.submit(owner,place,InformationReport.Field.PRICE,"1000","유료 전환","");
        reports.review(id,true,"1000","이용료 확인");
        var updated=reports.apply(List.of(place)).getFirst();
        assertFalse(updated.details().hasTag(PlaceTag.FREE_ADMISSION));
        assertEquals(FactState.UNKNOWN,updated.details().featureState(PlaceFeature.FREE_ADMISSION));
        assertEquals(evidence,updated.details().evidenceFor(PlaceField.NAME));
    }
    @Test void pendingDoesNotChangeSourceAndApprovalSurvivesNewRepository() throws Exception {
        long id=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","메뉴판 확인","https://example.test/menu");
        assertEquals(original,reports.apply(List.of(original)).getFirst());
        reports.review(id,true,"9500","1인 식사 기준 확인");
        Place updated=new InformationReportRepository(pool).apply(List.of(original)).getFirst();
        assertEquals(9500,updated.price());assertEquals(PriceEstimate.Evidence.ADMIN_REVIEWED,updated.priceEstimate().evidence());
        assertEquals(6000,original.price());assertEquals("APPROVED",reports.reports().getFirst().status());
        assertEquals("9500",reports.reports().getFirst().reviewedValue());
        assertThrows(IllegalArgumentException.class,()->reports.review(id,true,"10000","중복 승인"));
    }
    @Test void rejectionAndInvalidApprovalNeverChangePlace() throws Exception {
        long id=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","제보","" );
        assertThrows(IllegalArgumentException.class,()->reports.review(id,true,"-1","오류"));
        assertEquals("PENDING",reports.reports().getFirst().status());
        reports.review(id,false,"","확인 근거 부족");assertEquals(original,reports.apply(List.of(original)).getFirst());
        assertEquals("REJECTED",reports.reports().getFirst().status());
    }
    @Test void validatesMembershipAndInputAndReclassifiesReviewedSubtype() throws Exception {
        assertThrows(IllegalArgumentException.class,()->reports.submit(null,original,InformationReport.Field.PRICE,"0","제보",""));
        assertThrows(IllegalArgumentException.class,()->reports.submit(new UserSession(99,"없음"),original,InformationReport.Field.NAME,"수정","제보",""));
        assertThrows(IllegalArgumentException.class,()->reports.submit(owner,original,InformationReport.Field.PRICE,"-1","제보",""));
        assertThrows(IllegalArgumentException.class,()->reports.submit(owner,original,InformationReport.Field.NAME,"수정","제보","javascript:alert(1)"));
        long id=reports.submit(owner,original,InformationReport.Field.SUBCATEGORY,"일식 회/초밥","업종 오류","" );
        reports.review(id,true,"일식 회/초밥","확인");
        assertEquals("일식",com.datecourse.service.PlaceFacets.subtype(reports.apply(List.of(original)).getFirst()));
    }
    @Test void savedSnapshotRetainsEstimateAfterApprovalAndNewSaveKeepsReviewedEvidence() throws Exception {
        var saved=new SavedCourseRepository(pool);var plan=new RoutePlan(35,129,2,1000,"추정 코스",List.of(original));
        var first=saved.save(owner,plan);
        long id=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","메뉴판","" );reports.review(id,true,"9000","확인");
        assertEquals(plan,saved.findAll(owner).getFirst().plan());
        assertTrue(saved.findAll(owner).getFirst().plan().places().getFirst().priceEstimate().estimated());
        var updated=reports.apply(List.of(original));
        saved.save(owner,first.courseId(),new RoutePlan(35,129,2,1000,"수정",updated));
        assertEquals(PriceEstimate.Evidence.ADMIN_REVIEWED,saved.findAll(owner).getFirst().plan().places().getFirst().priceEstimate().evidence());
    }
    @Test void publicIdentitySharesCorrectionAcrossDatasetsWithoutChangingOtherPlaces() throws Exception {
        var details=new PlaceDetails("","카페","","","source","","","",Map.of("public","shared-source"));
        Place a=new Place(1,"old-demo","카페","CAFE",35,129,original.priceEstimate(),0,details);
        Place b=new Place(2,"expanded-demo","카페","CAFE",35,129,original.priceEstimate(),0,details);
        long id=reports.submit(owner,a,InformationReport.Field.NAME,"수정된 이름","동일 공공 ID","" );
        reports.review(id,true,"수정된 이름","확인");
        var rows=reports.apply(List.of(a,b,original));assertEquals("수정된 이름",rows.get(0).name());
        assertEquals("수정된 이름",rows.get(1).name());assertEquals(original,rows.get(2));
    }
    @Test void onlyVerifiedAdminLoginReceivesReviewCapability() throws Exception {
        char[] fixture="test-only-admin-password".toCharArray();
        try(var c=pool.getConnection();var s=c.createStatement()) {
            s.executeUpdate("CREATE TABLE app_admins(username VARCHAR(50) PRIMARY KEY,password_hash VARCHAR(255))");
        }
        try(var c=pool.getConnection();var s=c.prepareStatement("INSERT INTO app_admins VALUES (?,?)")) {
            s.setString(1,MemberRepository.ADMIN_USERNAME);s.setString(2,com.datecourse.auth.PasswordHash.create(fixture));s.executeUpdate();
        }
        var members=new MemberRepository(pool);
        assertThrows(IllegalArgumentException.class,()->members.login(MemberRepository.ADMIN_USERNAME,"wrong".toCharArray()));
        var admin=members.login(MemberRepository.ADMIN_USERNAME,fixture).adminSession();assertNotNull(admin);
        long id=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","확인","" );
        assertEquals(1,admin.informationReports().size());admin.reviewInformation(id,true,"9000","검토 완료");
        assertEquals(9000,reports.apply(List.of(original)).getFirst().price());
        try(var c=pool.getConnection();var s=c.prepareStatement("UPDATE members SET password_hash=? WHERE member_id=7")) {
            s.setString(1,com.datecourse.auth.PasswordHash.create(fixture));s.executeUpdate();
        }
        assertNull(members.login("demo@example.test",fixture).adminSession());
    }
    @Test void reviewContextShowsLatestCorrectionAndRejectsStaleApproval() throws Exception {
        long first=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","첫 제보","");
        long second=reports.submit(owner,original,InformationReport.Field.PRICE,"9500","다음 제보","");
        var stale=reports.reviewContext(second);assertNull(stale.correction());
        reports.review(first,true,"9000","확인 완료");
        assertThrows(IllegalArgumentException.class,()->reports.review(stale,true,"9500","이전 화면에서 승인"));
        assertEquals("PENDING",reports.reviewContext(second).report().status());
        assertEquals(9000,reports.apply(List.of(original)).getFirst().price());
        var fresh=reports.reviewContext(second);assertEquals("9000",fresh.correction().value());
        reports.review(fresh,true,"9500","최신 정정을 확인한 뒤 승인");
        assertEquals(9500,reports.apply(List.of(original)).getFirst().price());
        assertThrows(IllegalArgumentException.class,()->reports.review(fresh,true,"9500","중복 처리"));
    }
    @Test void administratorScreenApprovesPendingRowThroughItsActualButton() throws Exception {
        char[] fixture="test-only-admin-password".toCharArray();
        try(var c=pool.getConnection();var s=c.createStatement()) {s.executeUpdate("CREATE TABLE app_admins(username VARCHAR(50) PRIMARY KEY,password_hash VARCHAR(255))");}
        try(var c=pool.getConnection();var s=c.prepareStatement("INSERT INTO app_admins VALUES (?,?)")) {
            s.setString(1,MemberRepository.ADMIN_USERNAME);s.setString(2,com.datecourse.auth.PasswordHash.create(fixture));s.executeUpdate();
        }
        var session=new MemberRepository(pool).login(MemberRepository.ADMIN_USERNAME,fixture).adminSession();
        reports.submit(owner,original,InformationReport.Field.PRICE,"9000","메뉴판 확인","https://example.test/menu");
        var started=new java.util.concurrent.CountDownLatch(1);
        try{javafx.application.Platform.startup(()->{javafx.application.Platform.setImplicitExit(false);started.countDown();});}
        catch(IllegalStateException alreadyStarted){javafx.application.Platform.runLater(started::countDown);}
        assertTrue(started.await(10,java.util.concurrent.TimeUnit.SECONDS));
        var allowReview=new java.util.concurrent.atomic.AtomicBoolean(false);
        javafx.scene.Parent root=fx(()->{
            var loader=new javafx.fxml.FXMLLoader(getClass().getResource("/com/datecourse/page/review/information-review-view.fxml"));
            javafx.scene.Parent page=loader.load();new javafx.scene.Scene(page,1020,690);page.resize(1020,690);page.applyCss();page.layout();
            var controller=(com.datecourse.page.review.InformationReviewController)loader.getController();
            var confirm=controller.getClass().getDeclaredField("confirmation");confirm.setAccessible(true);
            confirm.set(controller,(java.util.function.BiPredicate<String,String>)(title,message)->allowReview.get());
            controller.configure(session);return page;
        });
        long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while(fx(()->root.lookup("#approveButton").isDisabled())&&System.nanoTime()<end)Thread.sleep(20);
        fx(()->{
            var list=(javafx.scene.control.ListView<?>)root.lookup("#reports");assertEquals(1,list.getItems().size());list.getSelectionModel().selectFirst();
            ((javafx.scene.control.TextField)root.lookup("#correctedValue")).setText("9500");
            ((javafx.scene.control.TextArea)root.lookup("#reviewNote")).setText("기준 메뉴를 재확인해 9,500원으로 수정 승인");
            root.resize(1020,690);root.applyCss();root.layout();
            var file=Path.of("build/ui-preview/information-review.png");Files.createDirectories(file.getParent());
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",file.toFile());
            var button=(javafx.scene.control.Button)root.lookup("#approveButton");assertFalse(button.isDisabled());button.fire();return null;
        });
        assertEquals("PENDING",reports.reports().getFirst().status());
        allowReview.set(true);
        fx(()->{((javafx.scene.control.Button)root.lookup("#approveButton")).fire();return null;});
        while(!"APPROVED".equals(reports.reports().getFirst().status())&&System.nanoTime()<end)Thread.sleep(20);
        assertEquals("APPROVED",reports.reports().getFirst().status());assertEquals(9500,reports.apply(List.of(original)).getFirst().price());
        // Wait for the follow-up list refresh before the test's pool is closed.
        while(fx(()->((javafx.scene.control.ListView<InformationReport>)root.lookup("#reports")).getItems().stream().anyMatch(r->r.status().equals("PENDING")))&&System.nanoTime()<end)Thread.sleep(20);
    }
    @Test void publicDatasetRehearsalBuildsSavesReportsAndReopensWithoutChangingOldSnapshot() throws Exception {
        var source=new com.datecourse.service.PublicDemoPlaceRepository().findAll();
        var search=new com.datecourse.service.NextPlaceService();
        var selected=new ArrayList<Place>();
        Place meal=search.candidates(source,selected,35.1578,129.06,2,2000,
            new NextPlaceCondition("식사",7000,2000,false,Set.of("한식"),Set.of(PlaceTag.GOOD_PRICE),30)).getFirst().place();
        selected.add(meal);
        Place cafe=search.candidates(source,selected,35.1578,129.06,2,2000,
            new NextPlaceCondition("카페",3000,2000,false,Set.of(),Set.of(),30)).getFirst().place();selected.add(cafe);
        Place culture=search.candidates(source,selected,35.1578,129.06,2,2000,
            new NextPlaceCondition("문화",0,2000,false,Set.of(),Set.of(PlaceTag.FREE_ADMISSION),30)).getFirst().place();selected.add(culture);
        var plan=new RoutePlan(35.1578,129.06,2,2000,"서면 시연 리허설",selected,
            Map.of(meal.recordId(),2000,cafe.recordId(),2000,culture.recordId(),2000));
        var validator=new com.datecourse.service.RouteRangeValidator();
        assertTrue(validator.outsideRange(plan).isEmpty());assertTrue(validator.legs(plan).stream().noneMatch(l->l.exceeded()));
        assertEquals(18800,plan.costEstimate().minimum());assertEquals(2,plan.costEstimate().estimatedPlaces());
        var courses=new SavedCourseRepository(pool);var saved=courses.save(owner,plan);
        assertEquals(plan,courses.findAll(owner).getFirst().plan());
        long reportId=reports.submit(owner,meal,InformationReport.Field.PRICE,"9500","리허설 가정: 기준 메뉴 변경","https://example.test/rehearsal");
        reports.review(reportId,true,"9500","테스트 DB에서만 승인하는 가상 정정");
        var corrected=reports.apply(source);
        var underSeven=search.matching(corrected,List.of(),35.1578,129.06,2,2000,
            new NextPlaceCondition("식사",7000,2000,false,Set.of("한식"),Set.of(),30));
        assertTrue(underSeven.stream().noneMatch(p->p.place().placeId().equals(meal.placeId())));
        var newMeal=search.matching(corrected,List.of(),35.1578,129.06,2,2000,
            new NextPlaceCondition("식사",9500,2000,false,Set.of("한식"),Set.of(),30)).stream()
            .map(p->p.place()).filter(p->p.placeId().equals(meal.placeId())).findFirst().orElseThrow();
        assertEquals(plan,courses.findAll(owner).getFirst().plan());
        var updated=new RoutePlan(plan.startLat(),plan.startLng(),plan.people(),plan.radius(),plan.title(),List.of(newMeal,cafe,culture),plan.arrivalRadii());
        assertEquals(23800,updated.costEstimate().minimum());
        courses.save(owner,saved.courseId(),updated);assertEquals(updated,courses.findAll(owner).getFirst().plan());
        Path output=Path.of("build/rehearsal/public-course.md");Files.createDirectories(output.getParent());
        Files.writeString(output,"# 공공자료 기반 자동 리허설\n\nH2 테스트 DB 전용. 실제 영업·실도로 검증 또는 실제 매장 가격 정정을 의미하지 않음.\n\n"
            +"식사: "+meal.name()+" · "+meal.priceText()+"\n\n카페: "+cafe.name()+" · "+cafe.priceText()
            +"\n\n문화: "+culture.name()+" · "+culture.priceText()+"\n\n원래 코스: "+plan.costEstimate().display()
            +"\n\n가상 승인 후 7,000원 조건에서 해당 식당 제외 확인. 기존 저장본 유지 확인.\n\n명시적 수정 저장 후: "+updated.costEstimate().display()+"\n");
    }
    @Test void administratorCanNavigatePendingPagesAndFilterReviewedRows() throws Exception {
        char[] fixture="test-only-admin-password".toCharArray();
        try(var c=pool.getConnection();var statement=c.createStatement()) {
            statement.executeUpdate("CREATE TABLE app_admins(username VARCHAR(50) PRIMARY KEY,password_hash VARCHAR(255))");
        }
        try(var c=pool.getConnection();var statement=c.prepareStatement("INSERT INTO app_admins VALUES (?,?)")) {
            statement.setString(1,MemberRepository.ADMIN_USERNAME);statement.setString(2,com.datecourse.auth.PasswordHash.create(fixture));statement.executeUpdate();
        }
        var session=new MemberRepository(pool).login(MemberRepository.ADMIN_USERNAME,fixture).adminSession();
        long oldest=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","첫 제보","");
        for(int i=0;i<54;i++)reports.submit(owner,original,InformationReport.Field.PRICE,"9000","추가 제보","");
        long rejected=reports.submit(owner,original,InformationReport.Field.PRICE,"9000","반려할 제보","");
        reports.review(rejected,false,"","확인 불가");
        var started=new java.util.concurrent.CountDownLatch(1);
        try{javafx.application.Platform.startup(()->{javafx.application.Platform.setImplicitExit(false);started.countDown();});}
        catch(IllegalStateException running){javafx.application.Platform.runLater(started::countDown);}
        assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS));
        var root=fx(()->{
            var loader=new javafx.fxml.FXMLLoader(getClass().getResource("/com/datecourse/page/review/information-review-view.fxml"));
            javafx.scene.Parent page=loader.load();new javafx.scene.Scene(page,1020,690);page.resize(1020,690);page.applyCss();page.layout();
            loader.<com.datecourse.page.review.InformationReviewController>getController().configure(session);return page;
        });
        awaitReportPage(root,"1페이지",50);
        fx(()->{assertFalse(root.lookup("#nextButton").isDisabled());
            ((javafx.scene.control.Button)root.lookup("#nextButton")).fire();return null;});
        awaitReportPage(root,"2페이지",5);
        fx(()->{
            var list=(javafx.scene.control.ListView<InformationReport>)root.lookup("#reports");
            assertTrue(list.getItems().stream().anyMatch(r->r.id()==oldest));
            assertTrue(root.lookup("#nextButton").isDisabled());
            ((javafx.scene.control.Button)root.lookup("#previousButton")).fire();return null;
        });
        awaitReportPage(root,"1페이지",50);
        fx(()->{((javafx.scene.control.ComboBox<InformationReportRepository.ReportFilter>)root.lookup("#filterBox"))
                .setValue(InformationReportRepository.ReportFilter.REJECTED);return null;});
        awaitReportPage(root,"1페이지",1);
        fx(()->{
            var list=(javafx.scene.control.ListView<InformationReport>)root.lookup("#reports");
            assertEquals(rejected,list.getItems().getFirst().id());assertTrue(root.lookup("#approveButton").isDisabled());
            root.applyCss();root.layout();var file=java.nio.file.Path.of("build/ui-preview/report-status-pages.png");
            java.nio.file.Files.createDirectories(file.getParent());
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",file.toFile());return null;
        });
    }
    private static void awaitReportPage(javafx.scene.Parent root,String page,int count)throws Exception {
        long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<end) {
            if(fx(()->((javafx.scene.control.Label)root.lookup("#status")).getText().endsWith(page)
                    && ((javafx.scene.control.ListView<?>)root.lookup("#reports")).getItems().size()==count
                    && !root.lookup("#filterBox").isDisabled()))return;
            Thread.sleep(20);
        }
        fail("제보 페이지 로드 시간 초과");
    }

    private static <T>T fx(java.util.concurrent.Callable<T> work) throws Exception {
        var task=new java.util.concurrent.FutureTask<T>(work);javafx.application.Platform.runLater(task);return task.get(10,java.util.concurrent.TimeUnit.SECONDS);
    }
}
