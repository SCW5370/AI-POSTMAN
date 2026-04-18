#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FULL_CHECK="${FULL_CHECK:-false}"

log() {
  printf '[release-check] %s\n' "$*"
}

fail() {
  printf '[release-check] ERROR: %s\n' "$*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "缺少命令: $1"
}

check_sensitive_files() {
  log '检查敏感文件'
  local found
  found="$(find "$PROJECT_DIR" -maxdepth 3 -type f \
    \( -name '*.pem' -o -name '*.key' -o -name '*.p12' -o -name '*.pfx' -o -name 'id_rsa' -o -name 'id_ed25519' \) \
    ! -path '*/.venv/*' ! -path '*/node_modules/*' | sed "s|$PROJECT_DIR/||")"
  if [ -n "$found" ]; then
    printf '%s\n' "$found"
    fail '检测到潜在敏感文件，请移出仓库目录后再发布'
  fi
}

check_required_scripts() {
  log '检查关键脚本可执行权限'
  local scripts=(
    "$PROJECT_DIR/install-deps.sh"
    "$PROJECT_DIR/start-all.sh"
    "$PROJECT_DIR/stop-all.sh"
    "$PROJECT_DIR/scripts/acceptance-first-email.sh"
    "$PROJECT_DIR/scripts/seed-high-quality-sources.sh"
  )
  local script
  for script in "${scripts[@]}"; do
    [ -f "$script" ] || fail "缺少脚本: $script"
    [ -x "$script" ] || fail "脚本不可执行: $script"
  done
}

check_env_template() {
  log '检查环境模板完整性'
  [ -f "$PROJECT_DIR/.env.example" ] || fail '缺少 .env.example'
  local key
  for key in LLM_API_KEY LLM_BASE_URL LLM_MODEL SMTP_HOST SMTP_PORT SMTP_USERNAME SMTP_PASSWORD DB_URL DB_USERNAME DB_PASSWORD WORKER_BASE_URL SERVER_PORT; do
    grep -q "^${key}=" "$PROJECT_DIR/.env.example" || fail ".env.example 缺少键: $key"
  done
}

build_backend() {
  log '后端编译检查'
  (cd "$PROJECT_DIR/backend" && mvn -q -DskipTests compile)
}

build_frontend() {
  log '前端构建检查'
  (cd "$PROJECT_DIR/frontend" && npm run -s build)
}

worker_smoke_test() {
  log 'Worker 轻量测试'
  if [ -x "$PROJECT_DIR/.venv/bin/python" ]; then
    if "$PROJECT_DIR/.venv/bin/python" -c 'import pytest' >/dev/null 2>&1; then
      (cd "$PROJECT_DIR/worker" && "$PROJECT_DIR/.venv/bin/python" -m pytest -q tests/test_normalizer.py)
    else
      log '跳过 Worker pytest：项目级 .venv 未安装 pytest（可先运行 install-deps.sh）'
    fi
  else
    log '跳过 Worker 测试：未检测到项目级 .venv'
  fi
}

main() {
  need_cmd find
  need_cmd grep
  need_cmd mvn
  need_cmd npm

  check_sensitive_files
  check_required_scripts
  check_env_template
  build_backend
  build_frontend
  if [ "$FULL_CHECK" = 'true' ]; then
    worker_smoke_test
  else
    log '跳过 Worker pytest（如需执行请使用 FULL_CHECK=true）'
  fi
  log '发布前检查通过'
}

main "$@"
