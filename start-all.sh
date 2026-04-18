#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE="$SCRIPT_DIR/.env"
VENV_DIR="$SCRIPT_DIR/.venv"
RUNTIME_DIR="$SCRIPT_DIR/.runtime"
BACKEND_LOG="$SCRIPT_DIR/backend/backend.log"
FRONTEND_LOG="$SCRIPT_DIR/frontend/frontend.log"
WORKER_LOG="$SCRIPT_DIR/worker/worker.log"
PUBLIC_HOST="${PUBLIC_HOST:-_}"
ENABLE_NGINX="${ENABLE_NGINX:-true}"

mkdir -p "$RUNTIME_DIR"

log() {
  printf '[start-all] %s\n' "$*"
}

fail() {
  printf '[start-all] ERROR: %s\n' "$*" >&2
  exit 1
}

load_env() {
  [ -f "$ENV_FILE" ] || fail "未找到 $ENV_FILE，请先运行 ./install-deps.sh"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
}

warn_for_placeholders() {
  local warnings=0
  for key in LLM_API_KEY SMTP_USERNAME SMTP_PASSWORD; do
    local value="${!key:-}"
    if [ -z "$value" ] || [[ "$value" == YOUR_* ]] || [[ "$value" == your@qq.com ]]; then
      log "警告: $key 尚未配置，相关能力将不可用"
      warnings=1
    fi
  done
  if [ "$warnings" -eq 1 ]; then
    log '提示：系统基础服务仍会启动，但摘要生成与邮件发送依赖真实外部配置'
  fi
}

ensure_command() {
  command -v "$1" >/dev/null 2>&1 || fail "缺少命令: $1"
}

ensure_service_running() {
  local service_name="$1"
  if systemctl list-unit-files "$service_name" >/dev/null 2>&1; then
    sudo systemctl enable --now "$service_name"
  else
    fail "系统缺少服务: $service_name"
  fi
}

setup_nginx() {
  [ "$ENABLE_NGINX" = 'true' ] || {
    log '已跳过 Nginx 配置'
    return 0
  }

  if ! command -v nginx >/dev/null 2>&1; then
    log '安装 Nginx'
    sudo apt-get update -y
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y nginx
  fi

  local nginx_config
  if [ -d /etc/nginx/sites-available ] && [ -d /etc/nginx/sites-enabled ]; then
    nginx_config=/etc/nginx/sites-available/ai-postman
  else
    nginx_config=/etc/nginx/conf.d/ai-postman.conf
  fi

  sudo tee "$nginx_config" >/dev/null <<NGINX
server {
    listen 80;
    server_name ${PUBLIC_HOST};

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINX

  if [ "$nginx_config" = /etc/nginx/sites-available/ai-postman ]; then
    sudo ln -sfn "$nginx_config" /etc/nginx/sites-enabled/ai-postman
    if [ -f /etc/nginx/sites-enabled/default ]; then
      sudo rm -f /etc/nginx/sites-enabled/default
    fi
  fi

  sudo nginx -t
  sudo systemctl enable --now nginx
  sudo systemctl reload nginx
}

stop_port_process() {
  local port="$1"
  if lsof -ti tcp:"$port" >/dev/null 2>&1; then
    lsof -ti tcp:"$port" | xargs -r kill
    sleep 1
    if lsof -ti tcp:"$port" >/dev/null 2>&1; then
      lsof -ti tcp:"$port" | xargs -r kill -9
    fi
  fi
}

ensure_frontend_build() {
  cd "$SCRIPT_DIR/frontend"
  if [ -d dist ] && [ ! -w dist ]; then
    log '检测到 frontend/dist 无写权限，尝试修复属主'
    sudo chown -R "$(id -u):$(id -g)" dist || fail 'frontend/dist 无写权限，请手动执行: sudo chown -R $(id -u):$(id -g) frontend/dist'
  fi
  if [ ! -x node_modules/.bin/vite ] || [ ! -x node_modules/.bin/tsc ]; then
    log '前端依赖缺失，重新安装'
    if [ -f package-lock.json ]; then
      npm ci
    else
      npm install
    fi
  fi
  npm run build
}

ensure_backend_build() {
  cd "$SCRIPT_DIR/backend"
  mvn -q -DskipTests package
}

ensure_worker_env() {
  [ -x "$VENV_DIR/bin/python" ] || fail "未找到 $VENV_DIR/bin/python，请先运行 ./install-deps.sh"
  if ! "$VENV_DIR/bin/python" -c 'import fastapi, uvicorn' >/dev/null 2>&1; then
    log '检测到 Worker 虚拟环境依赖不完整，正在补装'
    "$VENV_DIR/bin/pip" install -r "$SCRIPT_DIR/worker/requirements.txt"
  fi
}

start_worker() {
  stop_port_process 8000
  cd "$SCRIPT_DIR/worker"
  nohup "$VENV_DIR/bin/python" -m uvicorn app.main:app --host 127.0.0.1 --port 8000 >"$WORKER_LOG" 2>&1 &
  echo $! > "$RUNTIME_DIR/worker.pid"
}

start_backend() {
  stop_port_process 8080
  cd "$SCRIPT_DIR/backend"
  nohup java -jar target/backend-0.0.1-SNAPSHOT.jar >"$BACKEND_LOG" 2>&1 &
  echo $! > "$RUNTIME_DIR/backend.pid"
}

start_frontend() {
  stop_port_process 3000
  cd "$SCRIPT_DIR/frontend"
  nohup serve -s dist -l 3000 >"$FRONTEND_LOG" 2>&1 &
  echo $! > "$RUNTIME_DIR/frontend.pid"
}

wait_for_http() {
  local name="$1"
  local url="$2"
  local timeout_seconds="$3"
  local start_ts
  start_ts="$(date +%s)"

  while true; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "$name 已就绪: $url"
      return 0
    fi
    if [ $(( $(date +%s) - start_ts )) -ge "$timeout_seconds" ]; then
      log "$name 启动超时，最近日志："
      case "$name" in
        Worker) tail -n 30 "$WORKER_LOG" || true ;;
        Backend) tail -n 30 "$BACKEND_LOG" || true ;;
        Frontend) tail -n 30 "$FRONTEND_LOG" || true ;;
      esac
      return 1
    fi
    sleep 2
  done
}

seed_initial_data() {
  log '初始化默认源数据'
  bash "$SCRIPT_DIR/scripts/seed-high-quality-sources.sh" "http://127.0.0.1:8080"
}

trigger_fetch() {
  log '触发首次抓取任务'
  curl -fsS -X POST 'http://127.0.0.1:8080/api/admin/fetch-async' \
    -H 'Content-Type: application/json' \
    -d '{"sourceIds":[]}' >/dev/null || log '首次抓取触发失败，可在管理台稍后重试'
}

main() {
  log '=== AI Postman 一键服务启动脚本 ==='
  ensure_command curl
  ensure_command lsof
  ensure_command mvn
  ensure_command node
  ensure_command npm
  ensure_command java
  if ! command -v serve >/dev/null 2>&1; then
    log '未检测到 serve，正在安装'
    sudo npm install -g serve
  fi

  load_env
  warn_for_placeholders
  ensure_service_running postgresql
  ensure_service_running redis-server
  ensure_worker_env
  ensure_frontend_build
  ensure_backend_build
  setup_nginx

  start_worker
  wait_for_http 'Worker' 'http://127.0.0.1:8000/health' 45 || fail 'Worker 启动失败'

  start_backend
  wait_for_http 'Backend' 'http://127.0.0.1:8080/api/health' 90 || fail 'Backend 启动失败'

  start_frontend
  wait_for_http 'Frontend' 'http://127.0.0.1:3000' 45 || fail 'Frontend 启动失败'

  seed_initial_data
  trigger_fetch

  cat <<MSG
[start-all] 所有核心服务已启动。
[start-all] Frontend: http://127.0.0.1:3000
[start-all] Backend:  http://127.0.0.1:8080/api
[start-all] Worker:   http://127.0.0.1:8000/health
[start-all] Nginx:    http://127.0.0.1
[start-all] 日志文件:
  - $WORKER_LOG
  - $BACKEND_LOG
  - $FRONTEND_LOG
MSG
}

main "$@"
