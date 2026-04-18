#!/bin/bash

# 依赖环境安装脚本（CentOS 7 / CentOS Stream 8/9 / RHEL）
# 确保在空白服务器上安装所有必要的依赖

echo "=== AI Postman 依赖环境安装脚本（CentOS/RHEL）==="

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

# 检测包管理器：CentOS 8+/Stream 用 dnf，CentOS 7 用 yum
if command -v dnf &>/dev/null; then
    PKG="dnf"
else
    PKG="yum"
fi
echo "检测到包管理器：$PKG"

echo "1. 更新系统包管理器"
sudo $PKG update -y

echo "2. 安装必要的系统工具"
sudo $PKG install -y curl wget git unzip htop

echo "3. 安装 PostgreSQL 15"
# 使用 PostgreSQL 官方 repo，避免系统自带的版本过旧
if [ "$PKG" = "dnf" ]; then
    # CentOS Stream 8/9
    sudo $PKG install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm 2>/dev/null \
      || sudo $PKG install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm 2>/dev/null
    sudo $PKG -qy module disable postgresql 2>/dev/null || true
    sudo $PKG install -y postgresql15-server postgresql15
    sudo /usr/pgsql-15/bin/postgresql-15-setup initdb
    sudo systemctl start postgresql-15
    sudo systemctl enable postgresql-15
    PG_SERVICE="postgresql-15"
else
    # CentOS 7
    sudo yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm 2>/dev/null || true
    sudo yum install -y postgresql15-server postgresql15
    sudo /usr/pgsql-15/bin/postgresql-15-setup initdb
    sudo systemctl start postgresql-15
    sudo systemctl enable postgresql-15
    PG_SERVICE="postgresql-15"
fi

# 创建数据库和用户
sudo -u postgres /usr/pgsql-15/bin/psql -c "CREATE DATABASE aipostman;" 2>/dev/null || true
sudo -u postgres /usr/pgsql-15/bin/psql -c "CREATE USER aipostman WITH PASSWORD 'aipostman';" 2>/dev/null || true
sudo -u postgres /usr/pgsql-15/bin/psql -c "GRANT ALL PRIVILEGES ON DATABASE aipostman TO aipostman;" 2>/dev/null || true
sudo -u postgres /usr/pgsql-15/bin/psql -c "ALTER USER aipostman WITH SUPERUSER;" 2>/dev/null || true

echo "4. 安装 Redis"
# CentOS 需要 epel-release 才能安装 Redis
sudo $PKG install -y epel-release 2>/dev/null || true
sudo $PKG install -y redis
sudo systemctl start redis
sudo systemctl enable redis

echo "5. 安装 Java 17"
sudo $PKG install -y java-17-openjdk java-17-openjdk-devel
# 设置 JAVA_HOME
JAVA_HOME_PATH=$(dirname $(dirname $(readlink -f $(which java))))
echo "export JAVA_HOME=$JAVA_HOME_PATH" >> ~/.bashrc
export JAVA_HOME="$JAVA_HOME_PATH"

echo "6. 安装 Maven"
sudo $PKG install -y maven 2>/dev/null || true
if ! command -v mvn &>/dev/null; then
    echo "包管理器安装 Maven 失败，从官方源下载..."
    cd /tmp
    wget -q https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
    tar -xzf apache-maven-3.9.9-bin.tar.gz
    sudo mv -f apache-maven-3.9.9 /opt/maven
    echo 'export M2_HOME=/opt/maven' >> ~/.bashrc
    echo 'export PATH=$M2_HOME/bin:$PATH' >> ~/.bashrc
    # source ~/.bashrc 在非交互 shell 下无效，直接 export
    export M2_HOME=/opt/maven
    export PATH=$M2_HOME/bin:$PATH
    cd "$SCRIPT_DIR"
fi

echo "7. 安装 Node.js 18"
# 使用 NodeSource 官方脚本
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo $PKG install -y nodejs
# 安装 serve（用于托管前端静态文件）
sudo npm install -g serve

echo "8. 安装 Python 3"
sudo $PKG install -y python3 python3-venv python3-devel 2>/dev/null
# CentOS 7 可能没有 python3-venv，改用 virtualenv
if ! python3 -m venv --help &>/dev/null; then
    sudo $PKG install -y python3-virtualenv 2>/dev/null || pip3 install virtualenv
fi
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
    cd "$SCRIPT_DIR"
else
    echo "警告：frontend 目录不存在"
fi

# 后端依赖
if [ -d "backend" ]; then
    echo "安装后端依赖..."
    cd backend
    mvn clean package -DskipTests
    cd "$SCRIPT_DIR"
else
    echo "警告：backend 目录不存在"
fi

# Worker 依赖
if [ -d "worker" ]; then
    echo "安装 Worker 服务依赖..."
    cd worker
    python3 -m venv .venv
    # shellcheck source=/dev/null
    source .venv/bin/activate
    pip install -r requirements.txt
    deactivate
    cd "$SCRIPT_DIR"
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
echo "=== 依赖环境安装完成！==="
echo "请确保 PostgreSQL 和 Redis 服务已启动，然后运行 ./start-all.sh 启动所有服务。"
