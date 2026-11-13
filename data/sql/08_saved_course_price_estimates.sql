-- Run once AFTER 07_saved_courses.sql in the selected application DB.
-- MySQL 8.0.16+. Existing courses and exact unit prices are retained.
-- New snapshots preserve unknown/range prices and public-source provenance.
-- These ALTERs are not executed automatically by the application.
ALTER TABLE saved_course_places MODIFY COLUMN unit_price_snapshot INT NULL;
ALTER TABLE saved_course_places ADD COLUMN price_estimate_snapshot LONGTEXT NULL;
ALTER TABLE saved_course_places ADD COLUMN place_details_snapshot LONGTEXT NULL;
