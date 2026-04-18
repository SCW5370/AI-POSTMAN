# 源扩展系统 Phase 1 实现计划

## 1. 项目现状分析

### 1.1 现有结构
- **Monorepo 结构**：Spring Boot 后端 + Python worker
- **数据库**：PostgreSQL，使用 Flyway 进行迁移
- **现有 sources 表**：包含 id、name、url、source_type、category、enabled、priority、language、last_fetched_at 等字段
- **抓取链路**：Python worker 通过 feedparser 等工具抓取内容

### 1.2 现有问题
- 源是手工写死的，无法动态扩展
- 缺少源发现和审核机制
- 缺少源健康评估体系

## 2. Phase 1 目标

实现“半自动源扩展，稳定优先”，具体包括：

1. **新增 source_candidates 表**：用于存储候选源
2. **新增发现器服务**：支持按用户主题发现候选源
3. **新增 API 端点**：用于发现、查询、审批候选源
4. **新增源健康分**：评估源的质量和可靠性

## 3. 详细实现计划

### 3.1 数据库变更

#### 3.1.1 新增 source_candidates 表
```sql
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
```

#### 3.1.2 扩展 sources 表（添加健康分相关字段）
```sql
ALTER TABLE sources ADD COLUMN health_score NUMERIC(5, 2) NOT NULL DEFAULT 100;
ALTER TABLE sources ADD COLUMN success_rate NUMERIC(5, 2) NOT NULL DEFAULT 100;
ALTER TABLE sources ADD COLUMN avg_delay_ms INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sources ADD COLUMN deduplication_rate NUMERIC(5, 2) NOT NULL DEFAULT 0;
ALTER TABLE sources ADD COLUMN feedback_hit_rate NUMERIC(5, 2) NOT NULL DEFAULT 0;
ALTER TABLE sources ADD COLUMN last_health_check_at TIMESTAMP;
```

### 3.2 后端实现

#### 3.2.1 新增实体类
- `SourceCandidate.java`：对应 source_candidates 表
- 更新 `Source.java`：添加健康分相关字段

#### 3.2.2 新增仓库
- `SourceCandidateRepository.java`：处理候选源的 CRUD 操作

#### 3.2.3 新增服务
- `SourceDiscoveryService.java`：负责源发现逻辑
- `SourceHealthService.java`：负责源健康评估
- `SourceCandidateService.java`：处理候选源的审批和管理

#### 3.2.4 新增 API 端点
- `POST /api/sources/discover`：按 userId/topic 发现候选源
- `GET /api/sources/candidates`：获取候选源列表
- `POST /api/sources/candidates/{id}/approve`：审批候选源转正到 sources
- `POST /api/sources/candidates/{id}/reject`：拒绝候选源

### 3.3 Worker 实现

#### 3.3.1 新增发现器模块
- `discoverers/` 目录：包含不同类型的源发现器
  - `rss_discoverer.py`：基于 RSSHub / Feed 搜索
  - `github_discoverer.py`：基于 GitHub topic/repo
  - `media_rss_discoverer.py`：基于已验证的媒体 RSS 目录

#### 3.3.2 新增发现器服务
- `source_discoverer.py`：整合不同发现器，提供统一的发现接口

### 3.4 配置和部署

#### 3.4.1 新增配置项
- `SOURCE_DISCOVERY_BATCH_SIZE`：单次发现的源数量
- `SOURCE_DISCOVERY_TIMEOUT_SECONDS`：发现超时时间
- `SOURCE_HEALTH_CHECK_INTERVAL_HOURS`：健康检查间隔
- `SOURCE_HEALTH_MIN_SCORE`：源健康分最低阈值

#### 3.4.2 脚本更新
- 更新 `run-worker.sh`：确保包含新的发现器模块

## 4. 技术复用和依赖

### 4.1 现有技术复用
- **抓取层**：继续使用 feedparser + newspaper3k/readability-lxml
- **流程编排**：使用现有的 Spring Quartz + async task
- **数据库**：使用现有的 PostgreSQL

### 4.2 新增依赖
- **后端**：无新增依赖
- **Worker**：可能需要新增 `github-api` 包用于 GitHub 发现

## 5. 实现步骤

1. **数据库迁移**：创建 V4__source_candidates.sql 迁移文件
2. **后端实体和仓库**：实现 SourceCandidate 实体和仓库
3. **后端服务**：实现 SourceDiscoveryService、SourceHealthService、SourceCandidateService
4. **后端 API**：实现源发现和管理的 API 端点
5. **Worker 发现器**：实现不同类型的源发现器
6. **集成测试**：确保发现、审批、健康评估流程正常工作
7. **文档更新**：更新 README.md 和相关文档

## 6. 风险和应对措施

### 6.1 风险
- **发现器稳定性**：外部 API 可能不稳定
- **健康评估准确性**：需要足够的数据才能准确评估
- **性能问题**：大量源发现可能影响系统性能

### 6.2 应对措施
- **超时和重试**：为发现器添加超时和重试机制
- **渐进式部署**：先在小范围内测试，再全面部署
- **限流**：对发现 API 添加限流，避免滥用

## 7. 验收标准

1. **功能验收**：
   - 能够按主题发现候选源
   - 能够审批和拒绝候选源
   - 能够查看源健康分

2. **性能验收**：
   - 发现操作在 30 秒内完成
   - 审批操作在 1 秒内完成

3. **稳定性验收**：
   - 发现操作失败不影响现有抓取流程
   - 健康评估不影响系统性能

## 8. 后续扩展

Phase 1 完成后，可以考虑：

1. **Phase 2**：主题自动扩圈，基于用户反馈自动发现邻近主题的源
2. **Phase 3**：Agent 化搜索，使用 AI 自动发现和评估源

## 9. 实施时间估计

- **数据库迁移**：1 天
- **后端实现**：3-4 天
- **Worker 实现**：2-3 天
- **测试和调试**：2 天
- **文档更新**：1 天

总计：约 9-11 天，符合 1-2 周的时间预期。