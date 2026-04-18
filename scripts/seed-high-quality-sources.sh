#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
EXISTING_JSON="$(curl -sS "${BASE_URL}/api/sources" || echo "")"

add_source() {
  local name="$1"
  local url="$2"
  local source_type="$3"
  local category="$4"
  local priority="$5"
  local language="$6"

  if printf '%s' "${EXISTING_JSON}" | grep -F "\"url\":\"${url}\"" >/dev/null 2>&1; then
    echo "skip existing: ${name}"
    return
  fi

  curl -sS -X POST "${BASE_URL}/api/sources" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\":\"${name}\",
      \"url\":\"${url}\",
      \"sourceType\":\"${source_type}\",
      \"category\":\"${category}\",
      \"priority\":${priority},
      \"language\":\"${language}\"
    }" >/dev/null
  echo "added: ${name}"
}

# 国内高质量AI和科技源
add_source "机器之心" "https://www.jiqizhixin.com/rss" "RSS" "ai" 95 "zh"
add_source "量子位" "https://www.qbitai.com/feed" "RSS" "ai" 92 "zh"
add_source "深度学习与计算机视觉" "https://zhuanlan.zhihu.com/rss/深度学习与计算机视觉" "RSS" "ai" 90 "zh"
add_source "AI前线" "https://www.aifront.net/feed" "RSS" "ai" 88 "zh"
add_source "新智元" "https://www.zhidx.com/rss.xml" "RSS" "ai" 86 "zh"
add_source "智东西" "https://www.zhidx.com/rss.xml" "RSS" "ai" 85 "zh"
add_source "极客公园" "https://www.geekpark.net/rss" "RSS" "tech" 84 "zh"
add_source "爱范儿" "https://www.ifanr.com/feed" "RSS" "tech" 83 "zh"
add_source "少数派" "https://sspai.com/feed" "RSS" "tech" 82 "zh"
add_source "36氪" "https://36kr.com/feed" "RSS" "tech" 81 "zh"
add_source "虎嗅网" "https://www.huxiu.com/rss/0.xml" "RSS" "tech" 80 "zh"
add_source "IT之家" "https://www.ithome.com/rss/" "RSS" "tech" 79 "zh"
add_source "中关村在线" "https://www.zol.com.cn/rss/allnews.xml" "RSS" "tech" 78 "zh"
add_source "CSDN" "https://www.csdn.net/rss" "RSS" "dev" 77 "zh"
add_source "掘金后端" "https://juejin.cn/rss/backend" "RSS" "dev" 76 "zh"
add_source "掘金AI" "https://juejin.cn/rss/ai" "RSS" "ai" 75 "zh"
add_source "SegmentFault 思否" "https://segmentfault.com/feeds/blogs" "RSS" "dev" 74 "zh"
add_source "开源中国" "https://www.oschina.net/blog/rss" "RSS" "dev" 73 "zh"
# 仅保留国内/中文源，避免引入境外不可达源

echo "Done. Seed requests sent to ${BASE_URL}/api/sources"
