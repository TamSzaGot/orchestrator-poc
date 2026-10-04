CREATE TABLE IF NOT EXISTS jobs (
    id SERIAL PRIMARY KEY,
    submission_id VARCHAR(255) NOT NULL,
    partner_id VARCHAR(255) NOT NULL,
    requested_at VARCHAR(255) NOT NULL
);
