-- 新增 user_profiles 表
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interests JSONB,
    occupation VARCHAR(255),
    recent_activities JSONB,
    preferred_topics JSONB,
    confidence_score NUMERIC(5, 2) NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- 创建索引
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_confidence_score ON user_profiles(confidence_score DESC);
