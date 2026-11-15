-- Optional derived image; structured course/place snapshots remain authoritative.
CREATE TABLE IF NOT EXISTS saved_course_previews (
    course_id BIGINT UNSIGNED PRIMARY KEY,
    content_hash CHAR(64) NOT NULL,
    image_png MEDIUMBLOB NOT NULL,
    CONSTRAINT fk_preview_course FOREIGN KEY (course_id) REFERENCES saved_courses(course_id) ON DELETE CASCADE
);
