import os
from app.llm.llm_client import LlmClient

print("Testing LLM configuration...")
print(f"Current working directory: {os.getcwd()}")
print(f"LLM_API_KEY: {os.getenv('LLM_API_KEY', 'NOT SET')}")
print(f"LLM_API_BASE: {os.getenv('LLM_API_BASE', 'NOT SET')}")
print(f"LLM_MODEL: {os.getenv('LLM_MODEL', 'NOT SET')}")

# 测试 LLM 客户端初始化
llm_client = LlmClient()
print(f"LLM client initialized: {llm_client is not None}")
print(f"LLM enabled: {llm_client.is_enabled()}")
print(f"LLM API key present: {bool(llm_client.api_key)}")
print(f"LLM model present: {bool(llm_client.model)}")
print(f"LLM base URL: {llm_client.base_url}")

# 测试简单的 LLM 调用
try:
    test_prompt = "测试 LLM 连接"
    response = llm_client.summarize({"title_clean": "测试标题", "content_clean": "测试内容"})
    print(f"LLM response: {response}")
except Exception as e:
    print(f"LLM test error: {e}")
