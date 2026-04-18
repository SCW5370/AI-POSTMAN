#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/../worker"
if [ -f ../.env ]; then
  set -a
  source ../.env
  set +a
fi
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi
# 清除所有代理，防止 httpx/requests 通过代理访问本地服务
unset http_proxy https_proxy all_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY
unset socks_proxy SOCKS_PROXY socks5_proxy SOCKS5_PROXY
export NO_PROXY="localhost,127.0.0.1,::1,postgres,redis,worker,backend"
export no_proxy="$NO_PROXY"
if [ -x ../.venv/bin/python ]; then
  PYTHON_BIN="../.venv/bin/python"
else
  PYTHON_BIN="python3"
fi
"$PYTHON_BIN" -m uvicorn app.main:app --reload --port 8000
