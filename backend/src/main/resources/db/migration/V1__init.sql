CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    delivery_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    goals TEXT,
    preferred_topics JSONB NOT NULL DEFAULT '[]',
    blocked_topics JSONB NOT NULL DEFAULT '[]',
    preferred_sources JSONB NOT NULL DEFAULT '[]',
    delivery_mode VARCHAR(32) NOT NULL DEFAULT 'balanced',
    delivery_time VARCHAR(16) NOT NULL DEFAULT '08:00',
    max_items_per_digest INT NOT NULL DEFAULT 5,
    exploration_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0.10,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL UNIQUE,
    source_type VARCHAR(32) NOT NULL,
    category VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 50,
    language VARCHAR(32) DEFAULT 'zh',
    last_fetched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE raw_items (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    external_id VARCHAR(255),
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    author VARCHAR(255),
    published_at TIMESTAMP,
    summary_raw TEXT,
    content_raw TEXT,
    fetched_at TIMESTAMP NOT NULL DEFAULT NOW(),
    raw_hash VARCHAR(128),
    UNIQUE (source_id, url)
);

CREATE TABLE normalized_items (
    id BIGSERIAL PRIMARY KEY,
    raw_item_id BIGINT NOT NULL UNIQUE REFERENCES raw_items(id) ON DELETE CASCADE,
    canonical_url TEXT,
    title_clean TEXT NOT NULL,
    summary_clean TEXT,
    content_clean TEXT,
    language VARCHAR(32),
    tags JSONB NOT NULL DEFAULT '[]',
    source_quality_score NUMERIC(6, 2) NOT NULL DEFAULT 0,
    freshness_score NUMERIC(6, 2) NOT NULL DEFAULT 0,
    dedup_group_key VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'ready',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE item_enrichments (
    id BIGSERIAL PRIMARY KEY,
    normalized_item_id BIGINT NOT NULL UNIQUE REFERENCES normalized_items(id) ON DELETE CASCADE,
    short_summary TEXT,
    relevance_reason TEXT,
    action_hint VARCHAR(64),
    llm_tags JSONB NOT NULL DEFAULT '[]',
    enrichment_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE daily_digests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    digest_date DATE NOT NULL,
    digest_type VARCHAR(32) NOT NULL DEFAULT 'daily',
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    subject TEXT,
    html_content TEXT,
    total_items INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, digest_date, digest_type)
);

CREATE TABLE daily_digest_items (
    id BIGSERIAL PRIMARY KEY,
    digest_id BIGINT NOT NULL REFERENCES daily_digests(id) ON DELETE CASCADE,
    normalized_item_id BIGINT NOT NULL REFERENCES normalized_items(id) ON DELETE CASCADE,
    section VARCHAR(64) NOT NULL,
    item_order INT NOT NULL,
    final_score NUMERIC(8, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE feedback_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    digest_id BIGINT NOT NULL REFERENCES daily_digests(id) ON DELETE CASCADE,
    digest_item_id BIGINT NOT NULL REFERENCES daily_digest_items(id) ON DELETE CASCADE,
    feedback_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
