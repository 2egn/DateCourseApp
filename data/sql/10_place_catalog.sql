-- Additive catalog tables. Existing members, source records, overrides and saved courses remain intact.
CREATE TABLE IF NOT EXISTS place_catalog_lock (
    lock_id INT PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS place_catalog_sources (
    source_key VARCHAR(100) PRIMARY KEY,
    generated_at VARCHAR(40) NOT NULL,
    fingerprint CHAR(64) NOT NULL
);
CREATE TABLE IF NOT EXISTS place_catalog_imports (
    import_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_key VARCHAR(100) NOT NULL,
    generated_at VARCHAR(40) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    inserted_count INT NOT NULL,
    updated_count INT NOT NULL,
    reactivated_count INT NOT NULL,
    deactivated_count INT NOT NULL,
    unchanged_count INT NOT NULL,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS place_catalog_entries (
    entry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_key VARCHAR(200) NOT NULL UNIQUE,
    source_key VARCHAR(100) NOT NULL,
    record_id BIGINT UNSIGNED NULL UNIQUE,
    payload LONGTEXT NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    active BOOLEAN NOT NULL,
    last_import_id BIGINT NOT NULL,
    FOREIGN KEY (record_id) REFERENCES data_records(record_id),
    FOREIGN KEY (last_import_id) REFERENCES place_catalog_imports(import_id)
);
CREATE TABLE IF NOT EXISTS place_catalog_revisions (
    revision_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_id BIGINT NOT NULL,
    place_key VARCHAR(200) NOT NULL,
    record_id BIGINT UNSIGNED NOT NULL,
    change_kind VARCHAR(20) NOT NULL,
    payload LONGTEXT NOT NULL,
    active BOOLEAN NOT NULL,
    FOREIGN KEY (import_id) REFERENCES place_catalog_imports(import_id)
);
