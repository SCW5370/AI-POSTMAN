# 测试配置加载顺序
import sys
import os

# 添加项目根目录到 Python 路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# 首先导入配置模块，确保 .env 文件被加载
from app.config import LLM_API_KEY, LLM_API_BASE, LLM_MODEL

print("Config loaded successfully!")
print(f"LLM_API_KEY: {LLM_API_KEY[:10]}..." if LLM_API_KEY else "LLM_API_KEY: (empty)")
print(f"LLM_API_BASE: {LLM_API_BASE}")
print(f"LLM_MODEL: {LLM_MODEL}")

# 测试 LLM 客户端初始化
from app.llm.llm_client import LlmClient

llm_client = LlmClient()
print(f"LLM client initialized: {llm_client is not None}")
print(f"LLM enabled: {llm_client.is_enabled()}")
print(f"LLM API key present: {bool(llm_client.api_key)}")
print(f"LLM model present: {bool(llm_client.model)}")
print(f"LLM base URL: {llm_client.base_url}")

print("Test completed!")
