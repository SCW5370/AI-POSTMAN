-- 为 user_preferences 表添加 delivery_frequency 列
ALTER TABLE user_preferences ADD COLUMN delivery_frequency VARCHAR(20) NOT NULL DEFAULT 'weekly';

-- 创建索引
CREATE INDEX idx_user_preferences_delivery_frequency ON user_preferences(delivery_frequency);
