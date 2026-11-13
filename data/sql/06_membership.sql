-- Run against the database selected in your SQL client. Java imports use the database in db.url.
ALTER TABLE members MODIFY password_hash VARCHAR(255) NOT NULL;
CREATE TABLE IF NOT EXISTS app_admins (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
