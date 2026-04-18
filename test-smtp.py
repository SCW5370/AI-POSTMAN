#!/usr/bin/env python3
import requests
import json

# 测试邮件发送
url = "http://localhost:8080/api/admin/digests/send/1"

headers = {
    "Content-Type": "application/json"
}

response = requests.post(url, headers=headers)

print("Response status code:", response.status_code)
print("Response content:", response.text)
