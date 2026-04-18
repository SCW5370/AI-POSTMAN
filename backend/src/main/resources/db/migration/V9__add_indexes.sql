-- 为 daily_digests 表添加索引
CREATE INDEX IF NOT EXISTS idx_daily_digests_user_id ON daily_digests(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_digests_digest_date ON daily_digests(digest_date);
CREATE INDEX IF NOT EXISTS idx_daily_digests_status ON daily_digests(status);

-- 为 digest_build_tasks 表添加索引
CREATE INDEX IF NOT EXISTS idx_digest_build_tasks_user_id ON digest_build_tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_digest_build_tasks_status ON digest_build_tasks(status);

-- 为 source_candidates 表添加索引
CREATE INDEX IF NOT EXISTS idx_source_candidates_topic ON source_candidates(topic);
CREATE INDEX IF NOT EXISTS idx_source_candidates_status ON source_candidates(status);
CREATE INDEX IF NOT EXISTS idx_source_candidates_created_at ON source_candidates(created_at);
