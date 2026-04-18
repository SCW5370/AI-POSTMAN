#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
EMAIL="${EMAIL:-}"
DISPLAY_NAME="${DISPLAY_NAME:-AI Postman Acceptance}"
TIMEZONE="${TIMEZONE:-Asia/Shanghai}"
DELIVERY_TIME="${DELIVERY_TIME:-08:00}"
PREFERRED_TOPICS="${PREFERRED_TOPICS:-AI,LLM,Engineering}"
MAX_WAIT_FETCH="${MAX_WAIT_FETCH:-180}"
MAX_WAIT_BUILD="${MAX_WAIT_BUILD:-180}"
FORCE_LLM="${FORCE_LLM:-true}"
TOPICS_JSON="$(printf '%s' "$PREFERRED_TOPICS" | jq -R 'split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0))')"

log() {
  printf '[acceptance] %s\n' "$*"
}

fail() {
  printf '[acceptance] ERROR: %s\n' "$*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "缺少命令: $1"
}

json_post() {
  local url="$1"
  local body="$2"
  curl -fsS -X POST "$url" -H 'Content-Type: application/json' -d "$body"
}

poll_json_status() {
  local url="$1"
  local ok_status="$2"
  local timeout_seconds="$3"
  local started_at
  started_at="$(date +%s)"

  while true; do
    local payload status message
    payload="$(curl -fsS "$url")"
    status="$(printf '%s' "$payload" | jq -r '.data.status // empty')"
    message="$(printf '%s' "$payload" | jq -r '.data.message // empty')"
    log "轮询状态: ${status:-unknown} ${message:+- $message}"

    if [ "$status" = "$ok_status" ]; then
      printf '%s' "$payload"
      return 0
    fi
    if [ "$status" = 'failed' ]; then
      fail "任务失败: ${message:-unknown error}"
    fi
    if [ $(( $(date +%s) - started_at )) -ge "$timeout_seconds" ]; then
      fail "轮询超时: $url"
    fi
    sleep 3
  done
}

need_cmd curl
need_cmd jq

if [ -z "$EMAIL" ]; then
  if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    source ./.env
    set +a
    EMAIL="${SMTP_USERNAME:-}"
  fi
fi

[ -n "$EMAIL" ] || fail '请通过 EMAIL=your@example.com 提供验收邮箱，或在 .env 中设置 SMTP_USERNAME 作为默认值'

log '检查基础健康状态'
HEALTH_PAYLOAD="$(curl -fsS "$BASE_URL/api/health/readiness")"
READY="$(printf '%s' "$HEALTH_PAYLOAD" | jq -r '.data.ready')"
if [ "$READY" != 'true' ]; then
  printf '%s\n' "$HEALTH_PAYLOAD" | jq .
  fail '系统尚未达到首封邮件验收就绪状态，请先修复 readiness 中标红项'
fi

log '创建或复用用户并写入默认偏好'
ONBOARDING_PAYLOAD="$(json_post "$BASE_URL/api/onboarding/setup" "{
  \"email\": \"$EMAIL\",
  \"displayName\": \"$DISPLAY_NAME\",
  \"timezone\": \"$TIMEZONE\",
  \"goals\": \"Track $PREFERRED_TOPICS and ship one useful digest.\",
  \"preferredTopics\": $TOPICS_JSON,
  \"blockedTopics\": [],
  \"deliveryMode\": \"BALANCED\",
  \"deliveryTime\": \"$DELIVERY_TIME\",
  \"maxItemsPerDigest\": 5,
  \"explorationRatio\": 0.10,
  \"seedDefaultSources\": true
}")"
USER_ID="$(printf '%s' "$ONBOARDING_PAYLOAD" | jq -r '.data.user.id')"
[ -n "$USER_ID" ] && [ "$USER_ID" != 'null' ] || fail '创建用户失败'
log "用户 ID: $USER_ID"

SOURCE_COUNT="$(curl -fsS "$BASE_URL/api/sources" | jq -r '.data | length')"
if [ "$SOURCE_COUNT" -eq 0 ]; then
  log '后端没有信息源，执行本地种子脚本补种'
  "$PROJECT_DIR/scripts/seed-high-quality-sources.sh" "$BASE_URL"
fi

log '触发抓取任务'
FETCH_RESPONSE="$(json_post "$BASE_URL/api/admin/fetch-async" '{"sourceIds":[]}')"
FETCH_TASK_ID="$(printf '%s' "$FETCH_RESPONSE" | jq -r '.data.taskId')"
[ -n "$FETCH_TASK_ID" ] && [ "$FETCH_TASK_ID" != 'null' ] || fail '抓取任务创建失败'
poll_json_status "$BASE_URL/api/admin/fetch-async/$FETCH_TASK_ID" 'success' "$MAX_WAIT_FETCH" >/tmp/aipostman-fetch.json

log '触发日报构建任务'
TODAY="$(date +%F)"
BUILD_RESPONSE="$(json_post "$BASE_URL/api/admin/digests/build-async" "{\"userId\":$USER_ID,\"digestDate\":\"$TODAY\",\"forceLlm\":$FORCE_LLM}")"
BUILD_TASK_ID="$(printf '%s' "$BUILD_RESPONSE" | jq -r '.data.taskId')"
[ -n "$BUILD_TASK_ID" ] && [ "$BUILD_TASK_ID" != 'null' ] || fail '构建任务创建失败'
BUILD_DONE_PAYLOAD="$(poll_json_status "$BASE_URL/api/admin/digests/build-async/$BUILD_TASK_ID" 'success' "$MAX_WAIT_BUILD")"
DIGEST_ID="$(printf '%s' "$BUILD_DONE_PAYLOAD" | jq -r '.data.digest.id')"
TOTAL_ITEMS="$(printf '%s' "$BUILD_DONE_PAYLOAD" | jq -r '.data.digest.totalItems // 0')"
DIGEST_STATUS="$(printf '%s' "$BUILD_DONE_PAYLOAD" | jq -r '.data.digest.status // empty')"
[ -n "$DIGEST_ID" ] && [ "$DIGEST_ID" != 'null' ] || fail '日报构建成功但未返回 digestId'
if [ "$TOTAL_ITEMS" -le 0 ] || [ "$DIGEST_STATUS" = 'skipped' ]; then
  printf '%s\n' "$BUILD_DONE_PAYLOAD" | jq .
  fail '日报构建完成，但没有可发送内容；请检查信息源抓取质量、LLM 配置和发布阈值'
fi

log '发送测试邮件'
json_post "$BASE_URL/api/health/test-email" "{\"to\":\"$EMAIL\"}" >/tmp/aipostman-test-email.json

log '发送日报'
SEND_RESPONSE="$(curl -fsS -X POST "$BASE_URL/api/admin/digests/send/$DIGEST_ID" -H 'Content-Type: application/json')"
SEND_OK="$(printf '%s' "$SEND_RESPONSE" | jq -r '.success')"
[ "$SEND_OK" = 'true' ] || fail "日报发送失败: $(printf '%s' "$SEND_RESPONSE" | jq -r '.message // "unknown error"')"

log '验收通过，第一封邮件链路已跑通'
printf '%s\n' "$SEND_RESPONSE" | jq .
