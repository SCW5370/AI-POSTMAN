CREATE TABLE digest_build_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    digest_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(500),
    digest_id BIGINT REFERENCES daily_digests(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_digest_build_tasks_status ON digest_build_tasks(status);
CREATE INDEX idx_digest_build_tasks_user_date ON digest_build_tasks(user_id, digest_date);
