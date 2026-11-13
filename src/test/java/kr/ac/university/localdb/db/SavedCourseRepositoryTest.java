package kr.ac.university.localdb.db;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.datecourse.model.SavedCourse;
import com.zaxxer.hikari.HikariDataSource;
import kr.ac.university.localdb.UserSession;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SavedCourseRepositoryTest {
    private HikariDataSource pool;
    private SavedCourseRepository repository;
    private final UserSession owner = new UserSession(7, "Demo");

    @BeforeEach
    void prepare() throws Exception {
        pool = DatabasePool.create(new DbSettings("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", ""));
        try (Connection connection = pool.getConnection()) {
            for (String file : List.of("02_create_tables.sql", "07_saved_courses.sql", "08_saved_course_price_estimates.sql")) {
                try (var reader = Files.newBufferedReader(Path.of("data", "sql", file), StandardCharsets.UTF_8)) {
                    RunScript.execute(connection, reader);
                }
            }
        }
        execute("INSERT INTO members(member_id,email,password_hash,name) VALUES"
                + "(7,'owner@example.test','test-only','Demo'),(8,'other@example.test','test-only','Other')");
        repository = new SavedCourseRepository(pool);
    }

    @AfterEach
    void closePool() {
        if (pool != null) pool.close();
    }

    @Test
    void createsSnapshotWithGeneratedIdOrderedPlacesAndMissingRadius() throws SQLException {
        try (MemberRepository members = new MemberRepository(pool)) {
            SavedCourse saved = members.savedCourses().save(owner, example());
            assertTrue(saved.courseId() > 0);
            assertEquals(owner.memberId(), saved.memberId());
            assertEquals(example(), saved.plan());
            assertEquals(example(), storedPlan(saved.courseId()));
            assertEquals(46_000, saved.plan().totalCost());
            assertEquals(1, count("saved_courses"));
            assertEquals(3, count("saved_course_places"));
            assertEquals(0, count("data_records"));
        }
    }

    @Test
    void updatesSameIdReplacesOrderAndKeepsOriginalCreationTime() throws SQLException {
        SavedCourse first = repository.save(owner, example());
        Timestamp created = createdAt(first.courseId());
        RoutePlan changed = new RoutePlan(example().startLat(), example().startLng(), 3, 1500, "수정한 코스",
                List.of(example().places().get(2), example().places().get(0)), Map.of(33L, 800, 11L, 500));
        SavedCourse updated = repository.save(owner, first.courseId(), changed);
        assertEquals(first.courseId(), updated.courseId());
        assertEquals(changed, storedPlan(updated.courseId()));
        assertEquals(created, createdAt(updated.courseId()));
        assertEquals(1, count("saved_courses"));
        assertEquals(2, count("saved_course_places"));
        // Identical updates must also succeed, independent of affected-row semantics.
        SavedCourse repeated = repository.save(owner, updated.courseId(), changed);
        assertEquals(updated.courseId(), repeated.courseId());
        assertEquals(updated.plan(), repeated.plan());
        assertNotNull(repeated.updatedAt());
        assertEquals(2, count("saved_course_places"));
    }

    @Test
    void savesAndResavesAnEmptyCourseWithoutInventingPlaces() throws SQLException {
        RoutePlan empty = new RoutePlan(35.1579, 129.059, 2, 1000, "", List.of(), Map.of());
        SavedCourse savedEmpty = repository.save(owner, empty);
        assertEquals(empty, storedPlan(savedEmpty.courseId()));
        SavedCourse full = repository.save(owner, example());
        repository.save(owner, full.courseId(), empty);
        assertEquals(empty, storedPlan(full.courseId()));
        assertEquals(2, count("saved_courses"));
        assertEquals(0, count("saved_course_places"));
    }

    @Test
    void rejectsForeignAndMissingCourseWithoutChangingOrCreatingAnything() throws SQLException {
        SavedCourse saved = repository.save(owner, example());
        assertThrows(SQLException.class, () -> repository.save(new UserSession(8, "Other"), saved.courseId(), example()));
        assertThrows(SQLException.class, () -> repository.save(owner, saved.courseId() + 1000, example()));
        assertEquals(example(), storedPlan(saved.courseId()));
        assertEquals(1, count("saved_courses"));
        assertEquals(3, count("saved_course_places"));
    }

    @Test
    void failedPlaceBatchRollsBackNewCourseAndPoolCanBeUsedAgain() throws SQLException {
        // A real DB failure after valid input reaches the place-insert stage.
        execute("ALTER TABLE saved_course_places ADD CONSTRAINT test_fail_price CHECK(unit_price_snapshot <> 5000)");
        assertThrows(SQLException.class, () -> repository.save(owner, example()));
        assertEquals(0, count("saved_courses"));
        assertEquals(0, count("saved_course_places"));
        try (Connection connection = pool.getConnection()) { assertTrue(connection.getAutoCommit()); }
        execute("ALTER TABLE saved_course_places DROP CONSTRAINT test_fail_price");
        SavedCourse recovered = repository.save(owner, example());
        assertEquals(example(), storedPlan(recovered.courseId()));
    }

    @Test
    void failedReplacementRestoresCourseMetadataAndOriginalPlaceRows() throws SQLException {
        SavedCourse first = repository.save(owner, example());
        Timestamp created = createdAt(first.courseId());
        execute("ALTER TABLE saved_course_places ADD CONSTRAINT test_fail_price CHECK(unit_price_snapshot <> 12345)");
        Place failing = new Place(44, "CAFE_0044", "실패 검사 장소", "CAFE", 35.158, 129.059, 12345, 4.0);
        RoutePlan changed = new RoutePlan(example().startLat(), example().startLng(), 3, 1500, "실패할 수정",
                List.of(example().places().get(0), failing), Map.of(44L, 500));
        assertThrows(SQLException.class, () -> repository.save(owner, first.courseId(), changed));
        assertEquals(example(), storedPlan(first.courseId()));
        assertEquals(created, createdAt(first.courseId()));
        assertEquals(1, count("saved_courses"));
        assertEquals(3, count("saved_course_places"));
    }

    @Test
    void invalidPlansAndRangesAreRejectedBeforeAnyCourseIsStored() throws SQLException {
        RoutePlan original = example();
        assertThrows(IllegalArgumentException.class, () -> repository.save(null, original));
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner, 0, original));
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner,
                new RoutePlan(original.startLat(), original.startLng(), 0, 1000, "", List.of())));
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner,
                new RoutePlan(Double.NaN, original.startLng(), 2, 1000, "", List.of())));
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner,
                new RoutePlan(original.startLat(), original.startLng(), 2, 1000, "이름".repeat(11), List.of())));
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner,
                new RoutePlan(original.startLat(), original.startLng(), 2, 1000, "", List.of(original.places().get(0), original.places().get(0)))));
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner,
                new RoutePlan(original.startLat(), original.startLng(), 2, 1000, "", original.places(), Map.of(999L, 500))));
        Place far = new Place(44, "far", "먼 장소", "CAFE", 35.2, 129.059, 5000, 4.0);
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner,
                new RoutePlan(original.startLat(), original.startLng(), 2, 1000, "", List.of(far))));
        Place legOutside = new Place(44, "leg", "구간 밖 장소", "CAFE", 35.161, 129.059, 5000, 4.0);
        assertThrows(IllegalArgumentException.class, () -> repository.save(owner,
                new RoutePlan(original.startLat(), original.startLng(), 2, 1000, "", List.of(legOutside), Map.of(44L, 200))));
        assertEquals(0, count("saved_courses"));
        assertEquals(0, count("saved_course_places"));
    }

    private RoutePlan example() {
        return new RoutePlan(35.1579, 129.059, 2, 1000, "주말 코스", List.of(
                new Place(11, "MEAL_0011", "예시 식당", "MEAL", 35.158, 129.0591, 10000, 4.2),
                new Place(22, "CAFE_0022", "예시 카페", "CAFE", 35.1585, 129.0595, 5000, 4.0),
                new Place(33, "CULTURE_0033", "예시 전시관", "CULTURE", 35.159, 129.060, 8000, 4.5)),
                Map.of(22L, 500, 33L, 800));
    }

    @Test
    void updatedTimestampIsReadFromDatabaseAndRefreshesAfterRename() throws Exception {
        SavedCourse saved = repository.save(owner, example());
        assertNotNull(saved.updatedAt());
        var fixed = java.time.Instant.parse("2020-01-01T00:00:00Z");
        try (var connection=pool.getConnection(); var query=connection.prepareStatement("UPDATE saved_courses SET updated_at=? WHERE course_id=?")) {
            query.setTimestamp(1,Timestamp.from(fixed));query.setLong(2,saved.courseId());query.executeUpdate();
        }
        assertEquals(fixed,repository.findAll(owner).get(0).updatedAt());
        repository.rename(owner,saved.courseId(),"최근 이름");
        assertTrue(repository.findAll(owner).get(0).updatedAt().isAfter(fixed));
    }

    @Test
    void readsOnlyOwnerCoursesIncludingEmptyAndKeepsSnapshotOrderOnUpdate() throws Exception {
        SavedCourse first = repository.save(owner, example());
        RoutePlan empty = new RoutePlan(35.1579, 129.059, 2, 1000, "빈 코스", List.of());
        SavedCourse second = repository.save(owner, empty);
        repository.save(new UserSession(8, "Other"), example());
        assertEquals(List.of(second, first), repository.findAll(owner));
        RoutePlan edited = new RoutePlan(example().startLat(), example().startLng(), 3, 1000,
                "수정", List.of(example().places().get(2), example().places().get(0)), Map.of(11L, 500));
        repository.save(owner, first.courseId(), edited);
        assertEquals(edited, repository.findAll(owner).stream()
                .filter(c -> c.courseId() == first.courseId()).findFirst().orElseThrow().plan());
        assertEquals(2, repository.findAll(owner).size());
        assertThrows(IllegalArgumentException.class, () -> repository.findAll(null));
    }

    @Test
    void renameAndDeleteRequireOwnershipAndDeletionRemovesPlaceSnapshots() throws Exception {
        SavedCourse saved = repository.save(owner, example());
        UserSession other = new UserSession(8, "Other");
        assertThrows(SQLException.class, () -> repository.rename(other, saved.courseId(), "다른 이름"));
        assertThrows(SQLException.class, () -> repository.delete(other, saved.courseId()));
        assertEquals(example(), repository.findAll(owner).get(0).plan());
        repository.rename(owner, saved.courseId(), "새 이름");
        repository.rename(owner, saved.courseId(), "새 이름");
        assertEquals("새 이름", repository.findAll(owner).get(0).plan().title());
        assertThrows(IllegalArgumentException.class, () -> repository.rename(owner, saved.courseId(), " "));
        repository.delete(owner, saved.courseId());
        assertTrue(repository.findAll(owner).isEmpty());
        assertEquals(0, count("saved_course_places"));
        assertThrows(SQLException.class, () -> repository.delete(owner, saved.courseId()));
    }

    @Test
    void unknownRangeFreeAndPublicProvenanceRoundTripWithoutZeroDefaults() throws Exception {
        var details = new com.datecourse.model.PlaceDetails("공개 원본 주소", "카페", "051-000-0000", "https://example.test/branch",
                "https://example.test/source", "2026-09-30", "2026-11-13", "https://example.test/verify", Map.of("public", "source:one"));
        Place unknown = new Place(101, "DCA:unknown", "가격 미확인", "CAFE", 35.158, 129.059,
                com.datecourse.model.PriceEstimate.unknown(), 0, details);
        Place range = new Place(102, "DCA:range", "설명용 범위", "MEAL", 35.1581, 129.059,
                new com.datecourse.model.PriceEstimate(8000, 12000, com.datecourse.model.PriceEstimate.Unit.PER_PERSON, "설명용"), 0, details);
        Place free = new Place(103, "DCA:free", "근거 있는 무료", "CULTURE", 35.1582, 129.059,
                new com.datecourse.model.PriceEstimate(0, 0, com.datecourse.model.PriceEstimate.Unit.ADULT_TICKET, "무료 관람"), 0, details);
        RoutePlan plan = new RoutePlan(35.1579, 129.059, 2, 1000, "범위·미확인", List.of(unknown, range, free));
        SavedCourse saved = repository.save(owner, plan);
        assertEquals(plan, storedPlan(saved.courseId()));
        assertEquals(plan, repository.findAll(owner).get(0).plan());
        assertEquals("확인된 비용 16,000원~24,000원 + 가격 미확인 1곳", storedPlan(saved.courseId()).costEstimate().display());
        try (var connection = pool.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT unit_price_snapshot FROM saved_course_places ORDER BY visit_order")) {
            assertTrue(rows.next()); assertNull(rows.getObject(1));
            assertTrue(rows.next()); assertNull(rows.getObject(1));
            assertTrue(rows.next()); assertEquals(0, rows.getInt(1));
        }
    }

    @Test
    void oldUnverifiedZeroSnapshotDoesNotRestoreAsConfirmedFree() throws Exception {
        SavedCourse saved = repository.save(owner, example());
        execute("UPDATE saved_course_places SET unit_price_snapshot=0,price_estimate_snapshot=NULL,place_details_snapshot=NULL WHERE visit_order=1");
        assertFalse(storedPlan(saved.courseId()).places().get(0).priceEstimate().usablePerPerson());
    }

    /** Test-only SQL reads; the application query API belongs to DB-03. */
    private RoutePlan storedPlan(long courseId) throws SQLException {
        try (Connection connection = pool.getConnection()) {
            List<Place> places = new ArrayList<>();
            Map<Long, Integer> radii = new HashMap<>();
            try (var statement = connection.prepareStatement("SELECT * FROM saved_course_places WHERE course_id=? ORDER BY visit_order")) {
                statement.setLong(1, courseId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        Place place = SavedCourseRepository.readPlaceSnapshot(rows);
                        places.add(place);
                        Integer radius = rows.getObject("arrival_radius_m", Integer.class);
                        if (radius != null) radii.put(place.recordId(), radius);
                    }
                }
            }
            try (var statement = connection.prepareStatement("SELECT * FROM saved_courses WHERE course_id=? AND member_id=?")) {
                statement.setLong(1, courseId);
                statement.setLong(2, owner.memberId());
                try (ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    return new RoutePlan(row.getDouble("start_lat"), row.getDouble("start_lng"), row.getInt("people"),
                            row.getInt("radius_m"), row.getString("title"), places, radii);
                }
            }
        }
    }

    private Timestamp createdAt(long courseId) throws SQLException {
        try (Connection connection = pool.getConnection();
             var statement = connection.prepareStatement("SELECT created_at FROM saved_courses WHERE course_id=?")) {
            statement.setLong(1, courseId);
            try (ResultSet row = statement.executeQuery()) { assertTrue(row.next()); return row.getTimestamp(1); }
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = pool.getConnection(); var statement = connection.createStatement()) { statement.executeUpdate(sql); }
    }

    private int count(String table) throws SQLException {
        try (Connection connection = pool.getConnection(); var statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(row.next());return row.getInt(1);
        }
    }
}
