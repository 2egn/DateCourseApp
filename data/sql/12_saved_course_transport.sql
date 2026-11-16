-- User-selected mode only. Provider paths, times and credentials are never persisted here.
CREATE TABLE IF NOT EXISTS saved_course_transport (
    course_id BIGINT UNSIGNED PRIMARY KEY,
    travel_mode VARCHAR(20) NOT NULL,
    CONSTRAINT fk_transport_course FOREIGN KEY (course_id) REFERENCES saved_courses(course_id) ON DELETE CASCADE
);
