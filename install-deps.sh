#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PROJECT_NAME="AI Postman"
VENV_DIR="$SCRIPT_DIR/.venv"
ENV_FILE="$SCRIPT_DIR/.env"
ENV_EXAMPLE_FILE="$SCRIPT_DIR/.env.example"
NODE_MAJOR_REQUIRED=18

log() {
  printf '[install-deps] %s\n' "$*"
}

fail() {
  printf '[install-deps] ERROR: %s\n' "$*" >&2
  exit 1
}

require_path() {
  local path="$1"
  [ -e "$path" ] || fail "缺少必要路径: $path"
}

ensure_ubuntu_or_debian() {
  [ -f /etc/os-release ] || fail '当前系统缺少 /etc/os-release，无法识别发行版'
  # shellcheck disable=SC1091
  source /etc/os-release
  case "${ID:-}" in
    ubuntu|debian) ;;
    *)
      fail "install-deps.sh 当前仅支持 Ubuntu/Debian，检测到系统: ${PRETTY_NAME:-unknown}"
      ;;
  esac
}

ensure_sudo() {
  command -v sudo >/dev/null 2>&1 || fail '需要 sudo，但系统未安装 sudo'
  sudo -v
}

install_base_packages() {
  log '更新 apt 索引'
  sudo apt-get update -y

  log '安装系统基础依赖'
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
    ca-certificates curl wget git unzip htop lsof jq gnupg software-properties-common \
    build-essential pkg-config python3 python3-venv python3-dev python3-pip \
    postgresql postgresql-contrib redis-server openjdk-17-jdk maven
}

ensure_nodejs() {
  local install_node='false'
  if command -v node >/dev/null 2>&1; then
    local current_major
    current_major="$(node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0)"
    if [ "$current_major" -ge "$NODE_MAJOR_REQUIRED" ]; then
      log "检测到 Node.js $(node --version)，满足要求"
      return 0
    fi
  fi

  log "安装 Node.js ${NODE_MAJOR_REQUIRED}"
  curl -fsSL "https://deb.nodesource.com/setup_${NODE_MAJOR_REQUIRED}.x" | sudo -E bash -
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs
}

ensure_service_enabled() {
  local service_name="$1"
  log "启动并启用服务: ${service_name}"
  sudo systemctl enable --now "$service_name"
}

ensure_database() {
  log '初始化 PostgreSQL 数据库和用户'
  sudo -u postgres psql <<'SQL'
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'aipostman') THEN
    CREATE ROLE aipostman LOGIN PASSWORD 'aipostman';
  END IF;
END
$$;
SQL

  if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='aipostman'" | grep -q 1; then
    sudo -u postgres createdb -O aipostman aipostman
  fi

  sudo -u postgres psql -c "ALTER ROLE aipostman WITH SUPERUSER;" >/dev/null
  sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE aipostman TO aipostman;" >/dev/null
}

ensure_python_venv() {
  log '创建并更新 Python 虚拟环境'
  python3 -m venv "$VENV_DIR"
  "$VENV_DIR/bin/pip" install --upgrade pip setuptools wheel
  "$VENV_DIR/bin/pip" install -r "$SCRIPT_DIR/worker/requirements.txt"
}

install_frontend_deps() {
  log '安装前端依赖'
  cd "$SCRIPT_DIR/frontend"
  if [ -f package-lock.json ]; then
    npm ci
  else
    npm install
  fi
  npm run build
}

build_backend() {
  log '构建后端'
  cd "$SCRIPT_DIR/backend"
  mvn -q -DskipTests package
}

ensure_global_tools() {
  log '安装前端静态文件服务 serve'
  sudo npm install -g serve
}

ensure_env_file() {
  if [ ! -f "$ENV_FILE" ]; then
    log '检测到 .env 不存在，使用 .env.example 生成初始配置'
    cp "$ENV_EXAMPLE_FILE" "$ENV_FILE"
  else
    log '.env 已存在，保留现有配置'
  fi

  if grep -Eq 'YOUR_LLM_API_KEY_HERE|YOUR_SMTP_AUTH_CODE_HERE|your@qq.com' "$ENV_FILE"; then
    cat <<MSG
[install-deps] 请在 $ENV_FILE 中补全以下外部服务配置，否则“生成摘要 / 发送邮件”等功能不会完整可用：
  - LLM_API_KEY / LLM_BASE_URL / LLM_MODEL
  - SMTP_HOST / SMTP_PORT / SMTP_USERNAME / SMTP_PASSWORD
MSG
  fi
}

main() {
  log "=== ${PROJECT_NAME} 依赖环境安装脚本 ==="
  require_path "$SCRIPT_DIR/frontend"
  require_path "$SCRIPT_DIR/backend"
  require_path "$SCRIPT_DIR/worker"
  require_path "$ENV_EXAMPLE_FILE"

  ensure_ubuntu_or_debian
  ensure_sudo
  install_base_packages
  ensure_nodejs
  ensure_service_enabled postgresql
  ensure_service_enabled redis-server
  ensure_database
  ensure_python_venv
  ensure_global_tools
  install_frontend_deps
  build_backend
  ensure_env_file

  cat <<MSG
[install-deps] 安装完成。
[install-deps] Python 虚拟环境: $VENV_DIR
[install-deps] 下一步：
  1. 编辑 $ENV_FILE，填入 LLM 和 SMTP 配置
  2. 运行 ./start-all.sh 启动所有服务
MSG
}

main "$@"
