USE university_project;
ALTER TABLE members MODIFY password_hash VARCHAR(255) NOT NULL;
CREATE TABLE IF NOT EXISTS app_admins (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
