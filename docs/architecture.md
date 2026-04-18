# Architecture

## 技术栈

- Backend: Spring Boot, PostgreSQL, Redis, Quartz, Flyway
- Worker: Python, FastAPI, feedparser
- Delivery: Resend

## 系统边界

### Backend

- 用户与偏好管理
- 数据源管理
- 候选内容查询与打分
- 日报草稿生成
- 邮件发送
- 反馈记录与简单反馈修正
- 定时调度

### Worker

- 拉取 RSS/Atom/JSON feed
- 标准化内容
- 去重分组
- 生成摘要、相关性解释、行动建议

## 主链路

1. 定时拉取启用数据源
2. 原始条目入库
3. 标准化并去重
4. 对候选内容做增强
5. 按用户偏好评分排序
6. 满足阈值时生成日报草稿
7. 发送邮件
8. 记录反馈并用于后续排序修正
