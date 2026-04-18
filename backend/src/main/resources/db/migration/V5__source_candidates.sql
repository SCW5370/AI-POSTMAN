-- 新增 source_candidates 表
CREATE TABLE source_candidates (
    id BIGSERIAL PRIMARY KEY,
    query TEXT NOT NULL,
    topic TEXT,
    url TEXT NOT NULL UNIQUE,
    source_type VARCHAR(32) NOT NULL,
    confidence NUMERIC(5, 2) NOT NULL DEFAULT 0,
    discovery_method VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending', -- pending/approved/rejected
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);