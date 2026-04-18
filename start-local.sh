#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

log() {
  printf '[start-local] %s\n' "$*"
}

fail() {
  printf '[start-local] ERROR: %s\n' "$*" >&2
  exit 1
}

if [ ! -d backend ] || [ ! -d frontend ] || [ ! -d worker ]; then
  fail '请在 ai-postman 项目根目录执行此脚本'
fi

if [ ! -f .env ]; then
  if [ -f .env.example ]; then
    cp .env.example .env
    log '未检测到 .env，已由 .env.example 生成，请先补全 LLM/SMTP 配置再重试'
  fi
  fail '缺少 .env，请先完成配置'
fi

log '本地模式启动（默认关闭 Nginx 配置）'
log '如需启用 Nginx，请改用: bash start-all.sh'
ENABLE_NGINX=false bash "$SCRIPT_DIR/start-all.sh"
