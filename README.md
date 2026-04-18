<div align="center">

# 📬 AI Postman · AI 送报员

**你每天打开的第一封邮件，应该值得被打开。**

*每天一封，只送你真正需要知道的。*

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.java.com)
[![Python](https://img.shields.io/badge/Python-3.10+-blue.svg)](https://www.python.org)
[![React](https://img.shields.io/badge/React-18+-61DAFB.svg)](https://reactjs.org)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

</div>

---

## 💢 你是否也有这些困扰？

> - 每天被 100+ 条推送轰炸，刷完感觉什么都没学到
> - RSS 订阅源堆积如山，根本看不完，只会越积越多
> - 明明想关注某个领域，却总是被算法推送无关内容
> - 重要的行业动态往往被娱乐热点淹没，等发现已经晚了
> - 信息太多，反而焦虑；信息太少，又怕错过

**AI Postman 的存在，就是为了打破这道信息差。**

它不是又一个资讯 App，不是 RSS 阅读器，不是"今日热点"聚合。

它是一个**懂你的情报助理**——每天只回答你一个问题：**"今天，对我最重要的是什么？"**

---

## ✨ 核心亮点

### 🎯 目标导向，不是算法导向

AI Postman 理解你的职业背景、关注领域和当前目标，主动过滤 90% 的噪音，只保留真正相关的内容——不是平台想让你看的，而是你**真正需要**看的。

### 📧 每日一封 · 不打扰主义

没有 App，没有通知轰炸，没有无限滚动。只有**每天一封邮件**，在你设定的时间准点送达，读完即走。

### 🔢 四象限分区，内容即决策

每封邮件分为四个区块，不需要你来判断"这条重不重要"：

| 区块 | 含义 |
|------|------|
| 🔴 **Must Read** | 今天必须知道的，错过就晚了 |
| 🎯 **Focus Updates** | 你重点跟进的领域最新动态 |
| 📌 **Worth Saving** | 值得收藏备查的深度内容 |
| 🎲 **Surprise** | 受控的"意外收获"，拓展认知边界 |

### 🧠 越用越懂你

每次点击反馈（有用 / 跟进 / 一般），系统自动更新你的兴趣画像，下一封邮件就会更精准。这是一个真正**越用越好用**的正向闭环：

```
你的画像 → AI 排序 → 精选内容 → 邮件送达 → 你的反馈 → 画像进化
```

### 🤖 宁缺毋滥的质量哲学

> `DIGEST_FORCE_LLM_FINALIZE=true` 是默认配置。

当 LLM 服务异常时，系统选择**不发送**，而非发出充斥模板文案的低质量邮件。我们相信：一封真正有价值的邮件，比十封凑数的邮件更重要。

### 🔍 AI 自动发现信息源

输入一个主题关键词，AI 自动发现、评估并推荐优质 RSS 信息源，持续扩充你的信息网络，无需手动搜索订阅。

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────┐
│                  Frontend (React)                    │
│        用户画像 / 偏好设置 / 信息源管理              │
└─────────────────┬───────────────────────────────────┘
                  │ REST API
┌─────────────────▼───────────────────────────────────┐
│             Backend (Spring Boot)                    │
│  调度引擎 / 内容排序打分 / 编排协调 / 邮件发送       │
│           Quartz / PostgreSQL / Flyway               │
└─────────────────┬───────────────────────────────────┘
                  │ Internal API
┌─────────────────▼───────────────────────────────────┐
│              Worker (Python FastAPI)                 │
│   RSS 抓取 / 内容清洗去重 / LLM 摘要 / 源发现        │
│         LangChain / httpx / BeautifulSoup            │
└─────────────────────────────────────────────────────┘
```

**核心设计原则**：Java 端保持稳定、可调度、事务干净；Python 端承载所有慢的、易超时的、模型相关的风险。两端职责边界清晰，互不干扰。

调度节奏采用"昨晚算好，今早准点发"策略——Quartz 四个 Job 错峰执行，LLM 工作提前 8 小时在夜间完成，彻底规避早高峰 LLM 排队导致发送延迟的问题。

---

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Java 17+, Spring Boot 3, Spring Data JPA, Quartz Scheduler |
| **Worker** | Python 3.10+, FastAPI, LangChain, httpx |
| **前端** | React 18, Vite, Tailwind CSS, TypeScript |
| **数据库** | PostgreSQL 14+, Flyway（数据库版本管理） |
| **邮件服务** | SMTP（默认 QQ 邮箱，163 / Gmail / 企业邮箱均可） |
| **LLM** | 兼容 OpenAI 格式（OpenAI / 国产模型均可） |
| **部署** | 一键脚本（Ubuntu/Debian）/ Docker Compose |

---

## 🚀 快速开始

> **想要真正体验 AI Postman 的价值？**
> 本地跑一次看看效果没问题，但**每日邮件送达**的完整体验需要服务持续运行。
> 强烈建议部署到服务器（国内云服务器 2C2G 即可），一次部署，每天准时收信。
> [跳转至服务器持久部署 →](#️-服务器持久部署推荐)

---

### 选择你的部署方式

| 方式 | 适合场景 | 持续收信 | 难度 |
|------|----------|----------|------|
| **方式一**：一键脚本（Ubuntu 服务器） | 服务器部署、体验完整功能 | ✅ 支持 | ⭐⭐ |
| **方式二**：Docker Compose | 启动依赖服务（或含后端/Worker容器） | ⚠️ 需电脑常开 | ⭐ |
| **方式三**：手动本地开发 | 开发调试 | ⚠️ 需电脑常开 | ⭐⭐⭐ |

---

### 方式一：一键脚本部署（Ubuntu/Debian，推荐）

脚本会自动安装所有依赖（Java、Python、Node.js、PostgreSQL、Redis），创建项目级 Python 虚拟环境 `.venv`，并在启动时做健康检查。

```bash
# 1. 克隆项目
git clone https://github.com/your-username/ai-postman.git
cd ai-postman

# 2. 安装所有依赖（首次部署运行，约需 5-10 分钟）
bash install-deps.sh

# 3. 编辑 .env，填入 LLM / SMTP 配置（首次必须）
nano .env

# 4. 启动所有服务（默认会配置并启用 Nginx）
bash start-all.sh

# 本地模式（不自动配置 Nginx）也可用：
# bash start-local.sh


# 停止服务
bash stop-all.sh
```

> ⚠️ **注意**：
> - `install-deps.sh` 仅支持 Ubuntu/Debian，且需要 `sudo` 权限。
> - `start-all.sh` 默认启用 Nginx（80 端口）；如不希望自动配置 Nginx，可用 `ENABLE_NGINX=false bash start-all.sh`。
> - `.env` 已存在时不会被覆盖；仅在缺失时由 `.env.example` 生成。

---

### 方式二：Docker Compose（依赖/容器化调试）

> ⚠️ **说明**：
> - 当前 Compose 包含 `postgres` / `redis` / `worker` / `backend`，**不包含前端服务**。
> - `backend` 容器里的 LLM/SMTP 环境变量默认未补全，直接全容器启动时，邮件与摘要能力可能不可用。
> - 推荐把它当作“依赖启动器”，先启动数据库和 Redis，再本地跑后端/Worker。

```bash
# 1. 克隆项目
git clone https://github.com/your-username/ai-postman.git
cd ai-postman

# 2. 复制并配置环境变量
cp .env.example .env
# 编辑 .env，填入 LLM Key 和邮件配置（⚠️ 必须）

# 3. 推荐：仅启动数据库依赖
docker compose -f docker/docker-compose.yml up -d postgres redis

# 4. 再用方式三在本地启动后端和 Worker
# 如需全容器启动：docker compose -f docker/docker-compose.yml up -d
```

---

### 方式三：手动本地启动（开发调试）

```bash
# 1. 启动数据库依赖
docker compose -f docker/docker-compose.yml up -d postgres redis

# 2. 复制并配置环境变量
cp .env.example .env
# 编辑 .env，填入 LLM Key 和邮件配置（⚠️ 必须）

# 3. 安装 Worker 依赖（项目根 .venv）
python3 -m venv .venv
.venv/bin/pip install --upgrade pip
.venv/bin/pip install -r worker/requirements.txt

# 4. 分别启动各服务
bash scripts/run-backend.sh   # 后端（端口 8080）
bash scripts/run-worker.sh    # Worker（端口 8000）
cd frontend && npm install && npm run dev   # React 管理台（端口 5173，可选）
```

> 💡 各脚本会自动为本地地址设置 `NO_PROXY`，解决国内常见的"系统代理劫持本地数据库/Worker 连接"问题。

---

### 必要配置（.env）

无论哪种方式，以下两项是**必填**的：

```env
# 1. LLM 接口（支持任何 OpenAI 格式，国内模型也可）
LLM_BASE_URL=https://your-llm-provider.example.com/v1
LLM_API_BASE=https://your-llm-provider.example.com/v1
LLM_API_KEY=YOUR_LLM_API_KEY_HERE
LLM_MODEL=YOUR_MODEL_NAME

# 2. 邮件发送（默认 QQ 邮箱 SMTP，在 QQ 邮箱设置中开启 SMTP 并获取授权码）
SMTP_HOST=smtp.qq.com
SMTP_PORT=587
SMTP_USERNAME=your@qq.com
SMTP_PASSWORD=YOUR_SMTP_AUTH_CODE_HERE
```

其余配置项有合理默认值，完整说明见 `.env.example`。

---

## 📖 使用方法

`start-all.sh` 启动完成后，打开管理台即可开始使用：

```
http://127.0.0.1:3000
```

若服务器启用了默认 Nginx 反向代理，也可直接访问：

```
http://<你的服务器IP或域名>
```

### 首封邮件验收

当你已经完成 `.env` 里的 LLM 和 SMTP 配置，并且服务已经通过 `bash start-all.sh` 启动后，可以直接跑下面这条脚本做“从 0 到第一封邮件”的全链路验收：

```bash
EMAIL=your@example.com bash scripts/acceptance-first-email.sh
```

脚本会自动执行以下步骤：

- 检查 `/api/health/readiness` 就绪状态
- 创建或复用一个用户并写入默认偏好
- 确保默认信息源存在
- 触发抓取、轮询抓取任务完成
- 触发日报构建、轮询构建完成
- 先发送一封测试邮件，再发送当天日报

如果脚本失败，输出里会直接指出卡在哪一段。

### 发布前一键体检（推荐）

在准备推送到 GitHub 前，建议先执行：

```bash
bash scripts/release-check.sh
```

这条命令会检查：

- 仓库内是否存在 `*.pem / *.key / *.p12` 等敏感文件
- 核心脚本是否具备执行权限
- `.env.example` 是否包含关键配置项
- `backend` 与 `frontend` 是否可编译

如需额外执行 Worker 的 `pytest` 冒烟测试：

```bash
FULL_CHECK=true bash scripts/release-check.sh
```

> ⚠️ 不要把 SSH 私钥、`.env`、日志、运行时 pid 文件提交到仓库。当前 `.gitignore` 已覆盖这些高风险文件类型。

### 第一步：完善用户画像

进入**用户画像**页面，填写你的职业背景、关注领域、阅读习惯和每日收信时间。这是 AI 个性化筛选内容的依据，建议认真填写。

### 第二步：配置系统

进入**系统配置**页面，填入 LLM API Key 和 QQ 邮箱 SMTP 授权码（如果你在 `.env` 里已经填好，可以跳过这步）。

### 第三步：等待每日邮件

完成以上两步后，系统会按照你设定的时间每天自动发送日报，无需任何手动操作。

### 调度节奏

| 任务 | 频率 | 说明 |
|------|------|------|
| 内容抓取 | 每 2 小时 | 从所有订阅源异步拉取新内容 |
| 夜间预构建 | 每小时第 10 分 | 提前 8 小时构建草稿，错开高峰 |
| LLM 精修 | 每小时第 15 分 | 对候选内容生成个性化摘要 |
| 邮件发送 | 每小时第 20 分 | 按用户本地时区准点发送 |

### 第四步：通过反馈进化系统

收到邮件后，点击每条内容下方的反馈链接——`✅ 有用` / `🔔 跟进` / `👌 一般`，系统将自动调整下一封邮件的内容偏向，越用越准。

---

## 🖥️ 服务器持久部署（推荐）

> AI Postman 的核心价值在于**每天准时收信**。本地运行只是体验，服务器部署才是完整形态。

### 为什么需要服务器？

- 每日邮件送达依赖 Quartz 定时任务**持续运行**，电脑关机即中断
- 夜间预构建（凌晨处理 → 早上发送）需要 24 小时在线
- 服务器配置：**2 核 2GB 内存**的云服务器完全够用，国内各大云厂商约 30-60 元/月

### 推荐服务器配置

```
OS：Ubuntu 22.04 LTS
CPU：2 核
内存：2 GB（运行后约占用 1.5 GB）
存储：20 GB
开放端口：80（推荐，Nginx 统一入口）
可选开放：3000（直连前端）、8080（直连后端 API，仅调试时建议）
```

### 部署后设置为开机自启（systemd）

服务器重启后希望自动恢复运行，创建 systemd 服务：

```bash
# 创建服务文件（以后端为例）
sudo nano /etc/systemd/system/aipostman.service
```

```ini
[Unit]
Description=AI Postman Service
After=network.target postgresql.service redis-server.service

[Service]
Type=forking
User=ubuntu
WorkingDirectory=/home/ubuntu/ai-postman
ExecStart=/home/ubuntu/ai-postman/start-all.sh
ExecStop=/home/ubuntu/ai-postman/stop-all.sh
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 启用并启动
sudo systemctl daemon-reload
sudo systemctl enable aipostman
sudo systemctl start aipostman

# 查看状态
sudo systemctl status aipostman
```

### 查看运行日志

```bash
# 实时查看各服务日志
tail -f backend/backend.log    # 后端（调度、发送记录）
tail -f worker/worker.log      # Worker（抓取、LLM 处理记录）
tail -f frontend/frontend.log  # 前端
```

### 持续收信效果

部署成功并完成 Onboarding 后，系统将按以下节奏自动运行，无需任何人工干预：

```
每天凌晨（发送前 8 小时）  →  夜间预构建，AI 开始筛选和摘要
每天清晨（发送前约 5 分钟） →  LLM 精修，生成个性化摘要
每天你设定的时间            →  准时发送到你的邮箱 📬
每隔 2 小时                 →  持续抓取新内容，为第二天做准备
```

---

## 📁 项目结构

```
ai-postman/
├── backend/              # Spring Boot 后端
│   └── src/main/java/
│       ├── scheduler/    # Quartz 调度任务
│       ├── service/      # 核心业务逻辑（Digest / Ranking / Feedback）
│       ├── controller/   # REST API 接口层
│       └── client/       # Worker HTTP 客户端
│   └── resources/
│       ├── db/migration/ # Flyway 数据库迁移（V1-V10）
│       └── static/       # 内置轻量控制台（无前端依赖可跑通业务）
├── worker/               # Python FastAPI Worker
│   └── app/
│       ├── llm/          # LLM 客户端（含韧性设计：重试/降级/缓存）
│       ├── fetchers/     # RSS/Atom 抓取器
│       ├── enrichers/    # 编辑决策 & 内容 Finalize & LLM 摘要
│       ├── dedup/        # 内容去重
│       └── agents/       # 信息源自动发现 Agent
├── frontend/             # React 管理台（Vite + Tailwind CSS）
├── sql/                  # 数据库初始化脚本
├── docker/               # Docker Compose 配置
├── docs/                 # 架构与产品文档
└── scripts/              # 运维脚本（启动/停止/清理）
```

---

## 性能调参建议

模型响应速度与模型体量强相关。建议优先用"快稳模式"跑通全链路，再逐步调高质量：

```env
# 快稳模式（推荐先用这组配置）
LLM_ENRICH_ON_FETCH=false
LLM_EDITORIAL_ENABLED=true
WORKER_EDITORIAL_TIMEOUT_SECONDS=8
DIGEST_EDITORIAL_CANDIDATE_LIMIT=4
```

如果仍然偏慢，可以尝试：降低 `DIGEST_EDITORIAL_CANDIDATE_LIMIT`，或临时设置 `LLM_EDITORIAL_ENABLED=false`，或换用低延迟模型（如 `gpt-4o-mini`）。

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！无论是修复 Bug、改进文档，还是提出新功能想法。

### 如何贡献

1. **Fork** 本仓库并创建你的特性分支：`git checkout -b feature/amazing-feature`
2. 提交你的改动：`git commit -m 'feat: add some amazing feature'`
3. 推送到分支：`git push origin feature/amazing-feature`
4. 发起 **Pull Request**，描述你的改动和动机

### Commit 规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 规范：

| 前缀 | 含义 |
|------|------|
| `feat:` | 新功能 |
| `fix:` | Bug 修复 |
| `docs:` | 文档更新 |
| `refactor:` | 代码重构（不影响功能） |
| `perf:` | 性能优化 |
| `test:` | 测试相关 |
| `chore:` | 构建/依赖/脚本等杂项 |

### 贡献须知

- 新功能请附带必要的测试
- 涉及数据库结构变更，请添加对应的 Flyway migration 文件（按 V11、V12... 顺序命名）
- 敏感配置禁止硬编码，统一走环境变量
- PR 提交前请确认本地服务能正常启动，核心链路（Fetch → Build → Finalize → Send）可跑通

### 参与讨论

- 🐛 **Bug 反馈**：[提交 Issue](https://github.com/your-username/ai-postman/issues)
- 💡 **功能建议**：[发起 Discussion](https://github.com/your-username/ai-postman/discussions)
- 📖 **深度文档**：查阅 [docs/product.md](./docs/product.md) 与 [docs/architecture.md](./docs/architecture.md)

---

## 📄 开源协议

本项目基于 **MIT License** 开源，你可以自由地商业使用、修改代码、分发传播，唯一的要求是保留原始版权声明。

```
MIT License  Copyright (c) 2024 AI Postman Contributors
```

详见 [LICENSE](LICENSE) 文件。

---

## 🗺️ 路线图

- [ ] 支持更多内容源（微信公众号、播客字幕、YouTube 字幕）
- [ ] 多用户 SaaS 托管模式
- [ ] 移动端 Web 阅读界面
- [ ] 知识库集成（"Worth Saving" 内容自动存入 Notion / Obsidian）
- [ ] 更细粒度的反馈类型（打分、主题标签）
- [ ] 周报 / 月报汇总模式

---

<div align="center">

**打破信息差，让每个人都能平等地获取真正有价值的信息。**

如果这个项目对你有帮助，欢迎 ⭐ Star 支持！

</div>
