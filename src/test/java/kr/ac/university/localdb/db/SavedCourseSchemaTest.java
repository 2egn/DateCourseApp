package kr.ac.university.localdb.db;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** DB-01 schema contract only; this is not the future application save repository. */
class SavedCourseSchemaTest {
    private Connection connection;

    @BeforeEach
    void createSchema() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        for (String file : List.of("02_create_tables.sql", "07_saved_courses.sql")) {
            try (var reader = Files.newBufferedReader(Path.of("data", "sql", file), StandardCharsets.UTF_8)) {
                RunScript.execute(connection, reader);
            }
        }
        execute("INSERT INTO members(member_id,email,password_hash,name) VALUES(7,'demo@example.test','test-only','Demo')");
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null) connection.close();
    }

    @Test
    void threePlaceSnapshotRestoresOrderMissingRadiusAndPerPersonPricesWithoutSourceData() throws SQLException {
        RoutePlan expected = examplePlan();
        insertCourse(101);
        // Deliberately insert in a different order from the intended visit order.
        insertPlace(101, 3, expected.places().get(2), 800);
        insertPlace(101, 1, expected.places().get(0), null);
        insertPlace(101, 2, expected.places().get(1), 500);

        List<Place> places = new ArrayList<>();
        Map<Long, Integer> radii = new HashMap<>();
        try (var statement = connection.prepareStatement(
                "SELECT * FROM saved_course_places WHERE course_id=? ORDER BY visit_order")) {
            statement.setLong(1, 101);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Place place = new Place(rows.getLong("record_id_snapshot"), rows.getString("place_id_snapshot"),
                            rows.getString("name_snapshot"), rows.getString("category_snapshot"),
                            rows.getDouble("lat_snapshot"), rows.getDouble("lng_snapshot"),
                            rows.getInt("unit_price_snapshot"), rows.getDouble("rating_snapshot"));
                    places.add(place);
                    Integer radius = rows.getObject("arrival_radius_m", Integer.class);
                    if (radius != null) radii.put(place.recordId(), radius);
                }
            }
        }
        try (var statement = connection.prepareStatement("SELECT * FROM saved_courses WHERE course_id=? AND member_id=?")) {
            statement.setLong(1, 101);
            statement.setLong(2, 7);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                RoutePlan actual = new RoutePlan(row.getDouble("start_lat"), row.getDouble("start_lng"),
                        row.getInt("people"), row.getInt("radius_m"), row.getString("title"), places, radii);
                assertEquals(expected, actual);
                assertEquals(46_000L, actual.totalCost());
                assertFalse(actual.arrivalRadii().containsKey(11L));
            }
        }
        assertEquals(0, count("SELECT COUNT(*) FROM data_records"), "Snapshot restoration must not require source rows");
    }

    @Test
    void schemaRejectsDuplicateOrderDuplicateRecordAndInvalidBasicValues() throws SQLException {
        insertCourse(101);
        insertPlace(101, 1, examplePlan().places().get(0), null);
        Place another = new Place(44, "CAFE_0044", "다른 카페", "CAFE", 35.158, 129.060, 5_000, 4.0);
        assertThrows(SQLException.class, () -> insertPlace(101, 1, another, 500));
        assertThrows(SQLException.class, () -> insertPlace(101, 2, examplePlan().places().get(0), 500));
        assertThrows(SQLException.class, () -> insertPlace(101, 2, another, 100));
        assertThrows(SQLException.class, () -> execute("UPDATE saved_course_places SET unit_price_snapshot=-1 WHERE course_id=101"));
        assertThrows(SQLException.class, () -> execute("UPDATE saved_courses SET people=0 WHERE course_id=101"));
        assertThrows(SQLException.class, () -> execute("UPDATE saved_courses SET radius_m=100 WHERE course_id=101"));
        assertThrows(SQLException.class, () -> execute("UPDATE saved_courses SET start_lat=91 WHERE course_id=101"));
        assertThrows(SQLException.class, () -> execute("UPDATE saved_courses SET member_id=999 WHERE course_id=101"));
    }

    @Test
    void priceMigrationPreservesExistingCourseAndMakesUnknownSnapshotsPossible() throws Exception {
        insertCourse(101);
        insertPlace(101, 1, examplePlan().places().get(0), null);
        try (var reader = Files.newBufferedReader(Path.of("data/sql/08_saved_course_price_estimates.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM saved_course_places WHERE course_id=101")) {
            assertTrue(rows.next());
            assertEquals(examplePlan().places().get(0).price(), rows.getInt("unit_price_snapshot"));
            assertNull(rows.getString("price_estimate_snapshot"));
            assertNull(rows.getString("place_details_snapshot"));
        }
        execute("UPDATE saved_course_places SET unit_price_snapshot=NULL WHERE course_id=101");
        assertEquals(1, count("SELECT COUNT(*) FROM saved_course_places WHERE unit_price_snapshot IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM saved_courses"));
    }

    @Test
    void emptyCourseAndSameIdUpdatesAreRepresentableAndSchemaCanBeAppliedAgain() throws Exception {
        insertCourse(101);
        execute("UPDATE saved_courses SET title='수정한 코스',people=3 WHERE course_id=101 AND member_id=7");
        try (var statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT course_id,title,people FROM saved_courses WHERE member_id=7")) {
            assertTrue(row.next());
            assertEquals(101, row.getLong("course_id"));
            assertEquals("수정한 코스", row.getString("title"));
            assertEquals(3, row.getInt("people"));
            assertFalse(row.next());
        }
        assertEquals(0, count("SELECT COUNT(*) FROM saved_course_places WHERE course_id=101"));
        try (var reader = Files.newBufferedReader(Path.of("data/sql/07_saved_courses.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
        assertEquals(1, count("SELECT COUNT(*) FROM saved_courses"));
    }

    @Test
    void deletingCourseRemovesItsPlacesButMemberCannotBeDeletedWhileCoursesRemain() throws SQLException {
        insertCourse(101);
        insertPlace(101, 1, examplePlan().places().get(0), null);
        assertThrows(SQLException.class, () -> execute("DELETE FROM members WHERE member_id=7"));
        execute("DELETE FROM saved_courses WHERE course_id=101 AND member_id=7");
        assertEquals(0, count("SELECT COUNT(*) FROM saved_course_places"));
        assertEquals(1, count("SELECT COUNT(*) FROM members"));
    }

    private RoutePlan examplePlan() {
        return new RoutePlan(35.1579, 129.0590, 2, 1000, "주말 코스", List.of(
                new Place(11, "MEAL_0011", "예시 식당", "MEAL", 35.1580, 129.0591, 10_000, 4.2),
                new Place(22, "CAFE_0022", "예시 카페", "CAFE", 35.1585, 129.0595, 5_000, 4.0),
                new Place(33, "CULTURE_0033", "예시 전시관", "CULTURE", 35.1590, 129.0600, 8_000, 4.5)),
                Map.of(22L, 500, 33L, 800));
    }

    private void insertCourse(long courseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO saved_courses(course_id,member_id,title,start_lat,start_lng,people,radius_m)
                VALUES(?,7,'주말 코스',35.1579,129.0590,2,1000)
                """)) {
            statement.setLong(1, courseId);
            statement.executeUpdate();
        }
    }

    private void insertPlace(long courseId, int order, Place place, Integer radius) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO saved_course_places(course_id,visit_order,record_id_snapshot,place_id_snapshot,
                    name_snapshot,category_snapshot,lat_snapshot,lng_snapshot,unit_price_snapshot,rating_snapshot,arrival_radius_m)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            statement.setLong(1, courseId);
            statement.setInt(2, order);
            statement.setLong(3, place.recordId());
            statement.setString(4, place.placeId());
            statement.setString(5, place.name());
            statement.setString(6, place.category());
            statement.setDouble(7, place.lat());
            statement.setDouble(8, place.lng());
            statement.setInt(9, place.price());
            statement.setDouble(10, place.rating());
            if (radius == null) statement.setNull(11, Types.INTEGER);
            else statement.setInt(11, radius);
            statement.executeUpdate();
        }
    }

    private void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.executeUpdate(sql); }
    }

    private int count(String sql) throws SQLException {
        try (var statement = connection.createStatement(); ResultSet row = statement.executeQuery(sql)) {
            assertTrue(row.next());
            return row.getInt(1);
        }
    }
}
