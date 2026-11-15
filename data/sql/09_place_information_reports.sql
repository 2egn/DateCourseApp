-- Additive tables. Original places and saved course snapshots are not rewritten.
CREATE TABLE IF NOT EXISTS place_information_reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    place_id VARCHAR(200) NOT NULL,
    place_name VARCHAR(200) NOT NULL,
    field_name VARCHAR(30) NOT NULL,
    original_value VARCHAR(1000) NOT NULL,
    proposed_value VARCHAR(1000) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    review_note VARCHAR(1000) NOT NULL DEFAULT '',
    reviewed_value VARCHAR(1000) NOT NULL DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS place_information_overrides (
    place_id VARCHAR(200) NOT NULL,
    field_name VARCHAR(30) NOT NULL,
    corrected_value VARCHAR(1000) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (place_id, field_name)
);
