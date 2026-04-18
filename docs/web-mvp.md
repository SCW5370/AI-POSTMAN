# Web MVP（第一阶段）

## 目标

在不引入新前端框架的前提下，把业务闭环放到一个可操作的网页上：

1. 首次 onboarding
2. 偏好与策略配置
3. 一键触发 fetch / build / send
4. 直接预览最终邮件 HTML
5. build 任务异步执行且状态持久化

## 页面入口

- `http://127.0.0.1:8080/`

页面文件：`backend/src/main/resources/static/index.html`

## 新增 API

### `POST /api/onboarding/setup`

一次完成：

- 创建用户（若邮箱已存在则复用）
- 保存偏好
- 可选注入默认数据源（GitHub Blog / InfoQ / HN）

请求示例：

```json
{
  "email": "demo@example.com",
  "displayName": "Demo User",
  "timezone": "Asia/Shanghai",
  "goals": "最近在准备后端实习，也关注AI Agent方向",
  "preferredTopics": ["后端开发", "AI Agent", "GitHub开源项目"],
  "blockedTopics": ["娱乐八卦"],
  "deliveryMode": "BALANCED",
  "deliveryTime": "08:00",
  "maxItemsPerDigest": 5,
  "explorationRatio": 0.1,
  "seedDefaultSources": true
}
```

### `POST /api/admin/digests/build-async`

提交异步构建任务，返回 `taskId`。

### `GET /api/admin/digests/build-async/{taskId}`

查询任务状态：`pending/running/success/failed`。

### `POST /api/admin/fetch-async`

提交异步抓取任务，返回 `taskId`。

### `GET /api/admin/fetch-async/{taskId}`

查询抓取任务状态：`pending/running/success/failed`，并返回 `savedCount`。

## 第一阶段验收清单

1. 页面可打开、可完成 onboarding。
2. 页面按钮可触发 fetch/build/send。
3. build 后可拿到 digestId 并打开 HTML 预览。
4. send 成功后邮箱可收到日报。
5. 相同邮箱重复初始化不会产生重复用户。
