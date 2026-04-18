#!/bin/bash

# 测试邮件发送功能

# 定义测试参数
DIGEST_ID=1
API_URL="http://localhost:8080/api/admin/digests/send"

# 发送请求
echo "测试邮件发送功能..."
echo "发送请求到: $API_URL"
echo "Digest ID: $DIGEST_ID"

curl -X POST "$API_URL/$DIGEST_ID" \
  -H "Content-Type: application/json" \
  -v

# 检查响应
echo "\n测试完成，查看响应结果和日志"