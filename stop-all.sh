#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$SCRIPT_DIR/.runtime"

log() {
  printf '[stop-all] %s\n' "$*"
}

stop_pid_file() {
  local name="$1"
  local pid_file="$2"

  if [ -f "$pid_file" ]; then
    local pid
    pid="$(cat "$pid_file")"
    if [ -n "$pid" ] && kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" || true
      sleep 1
      kill -9 "$pid" >/dev/null 2>&1 || true
      log "$name 已停止 (pid: $pid)"
    fi
    rm -f "$pid_file"
  fi
}

stop_port() {
  local name="$1"
  local port="$2"
  if lsof -ti tcp:"$port" >/dev/null 2>&1; then
    lsof -ti tcp:"$port" | xargs -r kill || true
    sleep 1
    lsof -ti tcp:"$port" | xargs -r kill -9 || true
    log "$name 已停止 (port: $port)"
  else
    log "$name 未运行 (port: $port)"
  fi
}

mkdir -p "$RUNTIME_DIR"
stop_pid_file 'Worker' "$RUNTIME_DIR/worker.pid"
stop_pid_file 'Backend' "$RUNTIME_DIR/backend.pid"
stop_pid_file 'Frontend' "$RUNTIME_DIR/frontend.pid"
stop_port 'Worker' 8000
stop_port 'Backend' 8080
stop_port 'Frontend' 3000
