import os
import json
import httpx

# 加载环境变量
api_key = os.getenv('LLM_API_KEY', '')
api_base = os.getenv('LLM_API_BASE', 'https://api.openai.com/v1')
model = os.getenv('LLM_MODEL', 'gpt-4o-mini')

print("Testing LLM connection...")
print(f"LLM_MODEL: {model}")
print(f"LLM_API_KEY: {api_key[:10]}..." if api_key else "LLM_API_KEY: (empty)")
print(f"LLM_API_BASE: {api_base}")

if not api_key or not model:
    print("Error: LLM_API_KEY or LLM_MODEL not set")
    exit(1)

try:
    # 构建请求
    url = f"{api_base.rstrip('/')}/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    data = {
        "model": model,
        "messages": [
            {"role": "user", "content": "你好，测试连接"}
        ],
        "temperature": 0.7,
        "max_tokens": 50
    }
    
    print(f"\nSending request to: {url}")
    print(f"Request data: {json.dumps(data, ensure_ascii=False)}")
    
    # 发送请求
    response = httpx.post(url, headers=headers, json=data, timeout=1