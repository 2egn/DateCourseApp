-- Existing MySQL 8 databases: widen checks only; no course rows are rewritten.
-- Matches TravelMode.MAX_RADIUS (30km) and MAX_LEG_RADIUS (60km).
ALTER TABLE saved_courses
    DROP CHECK ck_saved_courses_radius,
    ADD CONSTRAINT ck_saved_courses_radius CHECK (radius_m BETWEEN 200 AND 30000);
ALTER TABLE saved_course_places
    DROP CHECK ck_saved_course_places_arrival,
    ADD CONSTRAINT ck_saved_course_places_arrival CHECK
        (arrival_radius_m IS NULL OR arrival_radius_m BETWEEN 200 AND 60000);
