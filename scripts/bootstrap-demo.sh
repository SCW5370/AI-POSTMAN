#!/usr/bin/env bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${EMAIL:-3276532080@qq.com}"
DISPLAY_NAME="${DISPLAY_NAME:-Demo User}"
TIMEZONE="${TIMEZONE:-Asia/Shanghai}"
export NO_PROXY="${NO_PROXY:-localhost,127.0.0.1,::1,postgres,redis,worker,backend}"
export no_proxy="${no_proxy:-$NO_PROXY}"

echo "Creating demo user..."
USER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"displayName\":\"$DISPLAY_NAME\",\"timezone\":\"$TIMEZONE\"}")
echo "$USER_RESPONSE"

USER_ID=$(printf '%s' "$USER_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
if [ -z "$USER_ID" ]; then
  echo "Failed to extract user id"
  exit 1
fi

echo "Saving demo preference..."
curl -s -X POST "$BASE_URL/api/preferences/$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "goals":"最近在准备后端实习，也关注AI Agent方向",
    "preferredTopics":["后端开发","AI Agent","GitHub开源项目"],
    "blockedTopics":["娱乐八卦"],
    "deliveryMode":"BALANCED",
    "deliveryTime":"08:00",
    "maxItemsPerDigest":5,
    "explorationRatio":0.10
  }'
echo

echo "Seeding demo sources..."
while IFS= read -r payload; do
  curl -s -X POST "$BASE_URL/api/sources" \
    -H "Content-Type: application/json" \
    -d "$payload" >/dev/null
done <<'EOF'
{"name":"GitHub Blog","url":"https://github.blog/feed/","sourceType":"RSS","category":"tech","priority":90,"language":"en"}
{"name":"InfoQ","url":"https://feed.infoq.com/","sourceType":"RSS","category":"tech","priority":85,"language":"en"}
{"name":"Hacker News Frontpage","url":"https://hnrss.org/frontpage","sourceType":"RSS","category":"tech","priority":80,"language":"en"}
EOF

echo "Fetching content..."
curl -s -X POST "$BASE_URL/api/admin/fetch" -H "Content-Type: application/json" -d '{"sourceIds":[]}'
echo

TODAY=$(date +%F)
echo "Building digest for $TODAY ..."
curl -s -X POST "$BASE_URL/api/admin/digests/build" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER_ID,\"digestDate\":\"$TODAY\"}"
echo
