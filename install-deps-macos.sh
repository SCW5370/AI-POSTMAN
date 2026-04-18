#!/bin/bash

# 依赖环境安装脚本（macOS）
# 依赖 Homebrew，适用于 macOS 12 Monterey 及以上（Intel 与 Apple Silicon 均支持）

echo "=== AI Postman 依赖环境安装脚本（macOS）==="

# 检查脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "脚本所在目录：$SCRIPT_DIR"
cd "$SCRIPT_DIR"
echo "当前工作目录：$(pwd)"

# 检查项目目录结构
echo "检查项目目录结构..."
if [ ! -d "frontend" ] || [ ! -d "backend" ] || [ ! -d "worker" ] || [ ! -f ".env.example" ]; then
    echo "错误：项目目录结构不完整，缺少必要的目录或文件！"
    echo "请确保 frontend、backend 和 worker 目录存在，以及 .env.example 文件存在。"
    exit 1
fi

echo "1. 检查 / 安装 Homebrew"
if ! command -v brew &>/dev/null; then
    echo "未检测到 Homebrew，正在安装..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    # Apple Silicon 路径处理
    if [ -f "/opt/homebrew/bin/brew" ]; then
        echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
        eval "$(/opt/homebrew/bin/brew shellenv)"
    fi
else
    echo "Homebrew 已安装：$(brew --version | head -1)"
fi
brew update

echo "2. 安装必要的系统工具"
brew install curl wget git unzip

echo "3. 安装 PostgreSQL 15"
brew install postgresql@15
brew services start postgresql@15
# 将 pg 命令加入 PATH（brew 安装的 pg 不在默认 PATH）
PG_BIN="$(brew --prefix postgresql@15)/bin"
export PATH="$PG_BIN:$PATH"
echo "export PATH=\"$PG_BIN:\$PATH\"" >> ~/.zprofile

# 等待 PostgreSQL 启动
sleep 3

# 创建数据库和用户（macOS 下当前用户已有 pg superuser 权限）
createdb aipostman 2>/dev/null || true
psql postgres -c "CREATE USER aipostman WITH PASSWORD 'aipostman';" 2>/dev/null || true
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE aipostman TO aipostman;" 2>/dev/null || true
psql postgres -c "ALTER USER aipostman WITH SUPERUSER;" 2>/dev/null || true

echo "4. 安装 Redis"
brew install redis
brew services start redis

echo "5. 安装 Java 17"
brew install openjdk@17
# 将 Java 17 加入 PATH
JAVA_BIN="$(brew --prefix openjdk@17)/bin"
export PATH="$JAVA_BIN:$PATH"
echo "export PATH=\"$JAVA_BIN:\$PATH\"" >> ~/.zprofile
sudo ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk 2>/dev/null || true

echo "6. 安装 Maven"
brew install maven

echo "7. 安装 Node.js 18"
brew install node@18
NODE_BIN="$(brew --prefix node@18)/bin"
export PATH="$NODE_BIN:$PATH"
echo "export PATH=\"$NODE_BIN:\$PATH\"" >> ~/.zprofile
# 安装 serve（用于托管前端静态文件）
npm install -g serve

echo "8. 安装 Python 3"
brew install python@3.11
PYTHON_BIN="$(brew --prefix python@3.11)/bin"
export PATH="$PYTHON_BIN:$PATH"
echo "export PATH=\"$PYTHON_BIN:\$PATH\"" >> ~/.zprofile

if ! command -v python3 &>/dev/null; then
    echo "错误：Python3 安装失败！"
    exit 1
fi
echo "Python 安装成功：$(python3 --version)"

echo "9. 安装项目依赖"
cd "$SCRIPT_DIR"

# 前端依赖
if [ -d "frontend" ]; then
    echo "安装前端依赖..."
    cd frontend
    npm install
    npm run build
    cd ..
else
    echo "警告：frontend 目录不存在"
fi

# 后端依赖
if [ -d "backend" ]; then
    echo "安装后端依赖..."
    cd backend
    mvn clean package -DskipTests
    cd ..
else
    echo "警告：backend 目录不存在"
fi

# Worker 依赖
if [ -d "worker" ]; then
    echo "安装 Worker 服务依赖..."
    cd worker
    python3 -m venv .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    deactivate
    cd ..
else
    echo "警告：worker 目录不存在"
fi

echo "10. 配置环境变量"
cat > "$SCRIPT_DIR/.env" << EOF
APP_PUBLIC_BASE_URL=http://127.0.0.1:8080
DB_URL=jdbc:postgresql://127.0.0.1:5432/aipostman
DB_USERNAME=aipostman
DB_PASSWORD=aipostman
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
WORKER_BASE_URL=http://127.0.0.1:8000
WORKER_FETCH_TIMEOUT_SECONDS=25
WORKER_EDITORIAL_TIMEOUT_SECONDS=8
WORKER_FINALIZE_TIMEOUT_SECONDS=70
WORKER_FETCH_BATCH_SIZE=4
WORKER_FETCH_MAX_RETRIES=2
WORKER_FETCH_RETRY_BACKOFF_MS=1200
SERVER_PORT=8080
# ⚠️  请替换为你自己的 LLM 配置（支持任何 OpenAI 格式接口）
LLM_BASE_URL=
LLM_API_BASE=
LLM_API_KEY=YOUR_LLM_API_KEY_HERE
LLM_MODEL=
LLM_TRUST_ENV=false
LLM_DEBUG=false
LLM_TIMEOUT_SECONDS=8
LLM_CONNECT_TIMEOUT_SECONDS=3
LLM_EDITORIAL_TIMEOUT_SECONDS=25
LLM_FINALIZE_TIMEOUT_SECONDS=45
LLM_EDITORIAL_RETRY_COUNT=2
LLM_FINALIZE_RETRY_COUNT=2
LLM_CACHE_ENABLED=true
LLM_CACHE_TTL_SECONDS=21600
LLM_CACHE_MAX_ENTRIES=2000
LLM_ENRICH_MAX_ITEMS=6
LLM_ENRICH_ON_FETCH=false
LLM_EDITORIAL_ENABLED=true
# ⚠️  请替换为你自己的邮件 SMTP 配置
# QQ/163 邮箱需在设置中开启 SMTP 并获取授权码，Gmail 需生成应用专用密码
SMTP_HOST=smtp.qq.com
SMTP_PORT=587
SMTP_USERNAME=your@qq.com
SMTP_PASSWORD=YOUR_SMTP_AUTH_CODE_HERE
DIGEST_HIGH_SCORE_THRESHOLD=60
DIGEST_MEDIUM_SCORE_THRESHOLD=35
DIGEST_MEDIUM_SCORE_MIN_COUNT=1
DIGEST_MIN_ITEM_SCORE=20
DIGEST_MAX_CANDIDATE_POOL=50
DIGEST_EDITORIAL_CANDIDATE_LIMIT=4
DIGEST_FORCE_LLM_FINALIZE=true
DIGEST_ENSURE_LLM_BEFORE_SEND=true
DIGEST_EXCLUDE_RECENT_SENT_DAYS=0
SCHEDULER_PREBUILD_LEAD_HOURS=8
EOF

echo ""
echo "⚠️  重要：请编辑 .env 文件，填入你自己的 LLM API Key 和邮件服务配置："
echo "   nano $SCRIPT_DIR/.env"
echo ""
echo "💡 提示：新终端中 PATH 才会完全生效。如遇命令找不到，请执行："
echo "   source ~/.zprofile"
echo ""
echo "=== 依赖环境安装完成！==="
echo "PostgreSQL 和 Redis 已通过 brew services 后台运行。"
echo "运行 ./start-all.sh 启动所有服务。"
echo ""
echo "⚠️  注意：macOS 不支持 systemd，服务器持久运行请使用 Ubuntu 服务器部署。"
echo "   macOS 适合本地体验和开发调试，电脑关机后服务即停止。"
