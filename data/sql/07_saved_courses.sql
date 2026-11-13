-- DB-01 schema. DB-02 provides the save repository; UI-01 button wiring is pending.
-- Select the application's MySQL database, then run 02_create_tables.sql first.
-- Target: MySQL 8.0.16+ (enforced CHECK constraints). No existing data is changed.

CREATE TABLE IF NOT EXISTS saved_courses (
    course_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(20) NOT NULL DEFAULT '',
    start_lat DOUBLE NOT NULL,
    start_lng DOUBLE NOT NULL,
    people INT NOT NULL,
    radius_m INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id),
    KEY idx_saved_courses_member_updated (member_id, updated_at, course_id),
    CONSTRAINT fk_saved_courses_member FOREIGN KEY (member_id)
        REFERENCES members(member_id) ON DELETE RESTRICT,
    CONSTRAINT ck_saved_courses_lat CHECK (start_lat BETWEEN -90 AND 90),
    CONSTRAINT ck_saved_courses_lng CHECK (start_lng BETWEEN -180 AND 180),
    CONSTRAINT ck_saved_courses_people CHECK (people BETWEEN 1 AND 20),
    CONSTRAINT ck_saved_courses_radius CHECK (radius_m BETWEEN 200 AND 2000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS saved_course_places (
    course_id BIGINT UNSIGNED NOT NULL,
    visit_order INT NOT NULL,
    -- Snapshot identifiers, not foreign keys into the replaceable data_records table.
    record_id_snapshot BIGINT NOT NULL,
    place_id_snapshot VARCHAR(255) NOT NULL,
    name_snapshot VARCHAR(255) NOT NULL,
    category_snapshot VARCHAR(100) NOT NULL,
    lat_snapshot DOUBLE NOT NULL,
    lng_snapshot DOUBLE NOT NULL,
    unit_price_snapshot INT NOT NULL,
    rating_snapshot DOUBLE NOT NULL,
    -- NULL means this place had no recorded arrival limit. Do not invent a default.
    arrival_radius_m INT NULL,
    PRIMARY KEY (course_id, visit_order),
    UNIQUE KEY uk_saved_course_place_record (course_id, record_id_snapshot),
    CONSTRAINT fk_saved_course_places_course FOREIGN KEY (course_id)
        REFERENCES saved_courses(course_id) ON DELETE CASCADE,
    CONSTRAINT ck_saved_course_places_order CHECK (visit_order >= 1),
    CONSTRAINT ck_saved_course_places_record CHECK (record_id_snapshot >= 1),
    CONSTRAINT ck_saved_course_places_lat CHECK (lat_snapshot BETWEEN -90 AND 90),
    CONSTRAINT ck_saved_course_places_lng CHECK (lng_snapshot BETWEEN -180 AND 180),
    CONSTRAINT ck_saved_course_places_price CHECK (unit_price_snapshot >= 0),
    CONSTRAINT ck_saved_course_places_rating CHECK (rating_snapshot BETWEEN 0 AND 5),
    CONSTRAINT ck_saved_course_places_arrival CHECK
        (arrival_radius_m IS NULL OR arrival_radius_m BETWEEN 200 AND 2000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
