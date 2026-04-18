-- 修改 digest_build_tasks 表的 message 字段类型为 TEXT，去除长度限制
ALTER TABLE digest_build_tasks ALTER COLUMN message TYPE TEXT;