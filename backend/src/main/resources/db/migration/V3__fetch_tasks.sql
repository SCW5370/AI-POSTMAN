CREATE TABLE fetch_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    source_ids_json TEXT,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(500),
    saved_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fetch_tasks_status ON fetch_tasks(status);
