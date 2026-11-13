package kr.ac.university.localdb.db;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.datecourse.model.SavedCourse;
import com.datecourse.model.PriceEstimate;
import com.datecourse.model.PlaceDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import com.datecourse.service.RouteRangeValidator;
import kr.ac.university.localdb.UserSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Saves the course and its place snapshots in one transaction. Does not own the pool. */
public final class SavedCourseRepository {
    private final DataSource dataSource;
    private final RouteRangeValidator rangeValidator = new RouteRangeValidator();
    private static final ObjectMapper JSON = new ObjectMapper();

    public SavedCourseRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    /** One ordered query reads each course and its saved places, without the current place dataset. */
    public List<SavedCourse> findAll(UserSession session) throws SQLException {
        requireSession(session);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT c.course_id AS saved_id,c.title,c.start_lat,c.start_lng,c.people,c.radius_m,c.updated_at,p.* FROM saved_courses c
                     LEFT JOIN saved_course_places p ON p.course_id=c.course_id
                     WHERE c.member_id=? ORDER BY c.updated_at DESC,c.course_id DESC,p.visit_order
                     """)) {
            statement.setLong(1, session.memberId());
            try (ResultSet rows = statement.executeQuery()) {
                List<SavedCourse> result = new ArrayList<>();
                long current = -1;
                RoutePlan header = null;
                java.time.Instant updatedAt = null;
                List<Place> places = new ArrayList<>();
                Map<Long, Integer> radii = new LinkedHashMap<>();
                while (rows.next()) {
                    long id = rows.getLong("saved_id");
                    if (id != current) {
                        if (header != null) result.add(restored(current, session, header, places, radii, updatedAt));
                        current = id;
                        updatedAt = rows.getTimestamp("updated_at").toInstant();
                        header = new RoutePlan(rows.getDouble("start_lat"), rows.getDouble("start_lng"),
                                rows.getInt("people"), rows.getInt("radius_m"), rows.getString("title"), List.of());
                        places = new ArrayList<>();
                        radii = new LinkedHashMap<>();
                    }
                    if (rows.getObject("visit_order") != null) {
                        Place place = readPlaceSnapshot(rows);
                        places.add(place);
                        Integer radius = rows.getObject("arrival_radius_m", Integer.class);
                        if (radius != null) radii.put(place.recordId(), radius);
                    }
                }
                if (header != null) result.add(restored(current, session, header, places, radii, updatedAt));
                return List.copyOf(result);
            } catch (IllegalArgumentException failure) {
                throw new SQLException("저장한 코스 정보를 읽지 못했습니다.", failure);
            }
        }
    }

    private SavedCourse restored(long id, UserSession session, RoutePlan header,
                                 List<Place> places, Map<Long, Integer> radii, java.time.Instant updatedAt) {
        return new SavedCourse(id, session.memberId(), new RoutePlan(header.startLat(), header.startLng(),
                header.people(), header.radius(), header.title(), places, radii), updatedAt);
    }

    public void rename(UserSession session, long courseId, String title) throws SQLException {
        requireSession(session);
        text(title, 20, true, "코스 이름");
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE saved_courses SET title=?,updated_at=CURRENT_TIMESTAMP WHERE course_id=? AND member_id=?")) {
            statement.setString(1, title);
            statement.setLong(2, courseId);
            statement.setLong(3, session.memberId());
            // MySQL may return zero for an unchanged name; verify ownership with the same connection.
            if (statement.executeUpdate() == 0) {
                try (PreparedStatement check = connection.prepareStatement(
                        "SELECT course_id FROM saved_courses WHERE course_id=? AND member_id=?")) {
                    check.setLong(1, courseId);
                    check.setLong(2, session.memberId());
                    try (ResultSet row = check.executeQuery()) {
                        if (!row.next()) throw new SQLException("코스가 없거나 본인 소유가 아닙니다.");
                    }
                }
            }
        }
    }

    public void delete(UserSession session, long courseId) throws SQLException {
        requireSession(session);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM saved_courses WHERE course_id=? AND member_id=?")) {
            statement.setLong(1, courseId);
            statement.setLong(2, session.memberId());
            if (statement.executeUpdate() != 1) throw new SQLException("코스가 없거나 본인 소유가 아닙니다.");
        }
    }

    private void requireSession(UserSession session) {
        if (session == null || session.memberId() <= 0) throw new IllegalArgumentException("로그인한 회원 정보가 필요합니다.");
    }

    /** Creates a new saved course; the result is returned only after commit. */
    public SavedCourse save(UserSession session, RoutePlan plan) throws SQLException {
        return persist(session, null, plan);
    }

    /** Updates the same course ID. Missing/foreign courses never become new courses. */
    public SavedCourse save(UserSession session, long courseId, RoutePlan plan) throws SQLException {
        if (courseId <= 0) throw new IllegalArgumentException("저장 코스 번호를 확인해 주세요.");
        return persist(session, courseId, plan);
    }

    private SavedCourse persist(UserSession session, Long existingCourseId, RoutePlan plan) throws SQLException {
        validate(session, plan);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long courseId = existingCourseId == null
                        ? insertCourse(connection, session.memberId(), plan)
                        : updateCourse(connection, existingCourseId, session.memberId(), plan);
                replacePlaces(connection, courseId, plan);
                java.time.Instant updatedAt;
                try (PreparedStatement query = connection.prepareStatement("SELECT updated_at FROM saved_courses WHERE course_id=?")) {
                    query.setLong(1, courseId);
                    try (ResultSet row = query.executeQuery()) { row.next(); updatedAt = row.getTimestamp(1).toInstant(); }
                }
                SavedCourse result = new SavedCourse(courseId, session.memberId(), plan, updatedAt);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); }
                catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                throw failure;
            }
        }
    }

    private long insertCourse(Connection connection, long memberId, RoutePlan plan) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO saved_courses(member_id,title,start_lat,start_lng,people,radius_m)
                VALUES(?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, memberId);
            bindCourse(statement, plan, 2);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next() || keys.getLong(1) <= 0) throw new SQLException("저장 코스 번호를 받지 못했습니다.");
                return keys.getLong(1);
            }
        }
    }

    private long updateCourse(Connection connection, long courseId, long memberId, RoutePlan plan) throws SQLException {
        // A locked ownership read also handles drivers returning 0 for an unchanged UPDATE.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT course_id FROM saved_courses WHERE course_id=? AND member_id=? FOR UPDATE")) {
            statement.setLong(1, courseId);
            statement.setLong(2, memberId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("저장 코스가 없거나 본인 소유가 아닙니다.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE saved_courses SET title=?,start_lat=?,start_lng=?,people=?,radius_m=?,updated_at=CURRENT_TIMESTAMP
                WHERE course_id=? AND member_id=?
                """)) {
            bindCourse(statement, plan, 1);
            statement.setLong(6, courseId);
            statement.setLong(7, memberId);
            statement.executeUpdate();
        }
        return courseId;
    }

    private void bindCourse(PreparedStatement statement, RoutePlan plan, int first) throws SQLException {
        statement.setString(first, plan.title());
        statement.setDouble(first + 1, plan.startLat());
        statement.setDouble(first + 2, plan.startLng());
        statement.setInt(first + 3, plan.people());
        statement.setInt(first + 4, plan.radius());
    }

    private void replacePlaces(Connection connection, long courseId, RoutePlan plan) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM saved_course_places WHERE course_id=?")) {
            statement.setLong(1, courseId);
            statement.executeUpdate();
        }
        if (plan.places().isEmpty()) return;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO saved_course_places(course_id,visit_order,record_id_snapshot,place_id_snapshot,
                    name_snapshot,category_snapshot,lat_snapshot,lng_snapshot,unit_price_snapshot,rating_snapshot,arrival_radius_m,
                    price_estimate_snapshot,place_details_snapshot)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            int order = 1;
            for (Place place : plan.places()) {
                statement.setLong(1, courseId);
                statement.setInt(2, order++);
                statement.setLong(3, place.recordId());
                statement.setString(4, place.placeId());
                statement.setString(5, place.name());
                statement.setString(6, place.category());
                statement.setDouble(7, place.lat());
                statement.setDouble(8, place.lng());
                if (place.priceEstimate().exact()) statement.setInt(9, place.price());
                else statement.setNull(9, Types.INTEGER);
                statement.setDouble(10, place.rating());
                Integer radius = plan.arrivalRadii().get(place.recordId());
                if (radius == null) statement.setNull(11, Types.INTEGER);
                else statement.setInt(11, radius);
                statement.setString(12, snapshotJson(place.priceEstimate()));
                statement.setString(13, snapshotJson(place.details()));
                statement.addBatch();
            }
            for (int count : statement.executeBatch()) {
                if (count == Statement.EXECUTE_FAILED) throw new SQLException("방문 장소를 모두 저장하지 못했습니다.");
            }
        }
    }

    private void validate(UserSession session, RoutePlan plan) {
        if (session == null || session.memberId() <= 0) throw new IllegalArgumentException("로그인한 회원 정보가 필요합니다.");
        if (plan == null) throw new IllegalArgumentException("저장할 코스가 필요합니다.");
        text(plan.title(), 20, false, "코스 이름");
        coordinates(plan.startLat(), plan.startLng());
        if (plan.people() < 1 || plan.people() > 20 || plan.radius() < 200 || plan.radius() > 2000)
            throw new IllegalArgumentException("인원은 1~20명, 전체 반경은 200~2000m여야 합니다.");
        Set<Long> records = new HashSet<>();
        for (Place place : plan.places()) {
            if (place.recordId() <= 0 || !records.add(place.recordId()))
                throw new IllegalArgumentException("장소 번호가 잘못되었거나 같은 장소가 중복되어 있습니다.");
            text(place.placeId(), 255, false, "장소 ID");
            text(place.name(), 255, true, "장소 이름");
            text(place.category(), 100, true, "장소 분류");
            coordinates(place.lat(), place.lng());
            if (!Double.isFinite(place.rating()) || place.rating() < 0 || place.rating() > 5)
                throw new IllegalArgumentException("장소 가격·평점을 확인해 주세요.");
        }
        if (!records.containsAll(plan.arrivalRadii().keySet()))
            throw new IllegalArgumentException("코스에 없는 장소의 이동 반경이 남아 있습니다.");
        if (!rangeValidator.outsideRange(plan).isEmpty()
                || rangeValidator.legs(plan).stream().anyMatch(RouteRangeValidator.Leg::exceeded))
            throw new IllegalArgumentException("전체 범위 또는 장소 사이 이동 반경을 벗어난 코스는 저장할 수 없습니다.");
    }

    private void text(String value, int maxLength, boolean required, String label) {
        if (value == null || value.length() > maxLength || (required && value.isBlank()))
            throw new IllegalArgumentException(label + "을(를) 확인해 주세요. 최대 " + maxLength + "자입니다.");
    }

    private void coordinates(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180)
            throw new IllegalArgumentException("유효한 장소 좌표가 필요합니다.");
    }

    private static String snapshotJson(Object value) throws SQLException {
        try { return JSON.writeValueAsString(value); }
        catch (IOException failure) { throw new SQLException("장소 가격·출처를 저장할 형식으로 바꾸지 못했습니다.", failure); }
    }

    /** Both reads and round-trip tests restore the same price and provenance snapshot. */
    static Place readPlaceSnapshot(ResultSet row) throws SQLException {
        try {
            String priceJson = row.getString("price_estimate_snapshot");
            Integer legacy = row.getObject("unit_price_snapshot", Integer.class);
            PriceEstimate price = priceJson == null
                    ? legacy != null && legacy > 0 ? PriceEstimate.exact(legacy) : PriceEstimate.unknown()
                    : JSON.readValue(priceJson, PriceEstimate.class);
            String detailsJson = row.getString("place_details_snapshot");
            PlaceDetails details = detailsJson == null ? PlaceDetails.EMPTY : JSON.readValue(detailsJson, PlaceDetails.class);
            return new Place(row.getLong("record_id_snapshot"), row.getString("place_id_snapshot"),
                    row.getString("name_snapshot"), row.getString("category_snapshot"),
                    row.getDouble("lat_snapshot"), row.getDouble("lng_snapshot"), price,
                    row.getDouble("rating_snapshot"), details);
        } catch (IOException | IllegalArgumentException failure) {
            throw new SQLException("저장한 장소 가격·출처를 읽지 못했습니다.", failure);
        }
    }
}
