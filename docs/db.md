# Database

MVP 使用 Flyway 初始化以下核心表：

- `users`
- `user_preferences`
- `sources`
- `raw_items`
- `normalized_items`
- `item_enrichments`
- `daily_digests`
- `daily_digest_items`
- `feedback_events`

后端 JPA 实体与这些表一一对应，初始化 SQL 见 `backend/src/main/resources/db/migration/V1__init.sql`。
