import os
from dotenv import load_dotenv

# 加载.env文件
load_dotenv()

# LLM 配置
LLM_API_BASE = os.getenv("LLM_API_BASE", os.getenv("LLM_BASE_URL", "")).strip()
LLM_API_KEY = os.getenv("LLM_API_KEY", "").strip()
LLM_MODEL = os.getenv("LLM_MODEL", "").strip()
LLM_TIMEOUT_SECONDS = float(os.getenv("LLM_TIMEOUT_SECONDS", "8"))
LLM_CONNECT_TIMEOUT_SECONDS = float(os.getenv("LLM_CONNECT_TIMEOUT_SECONDS", "3"))
LLM_EDITORIAL_TIMEOUT_SECONDS = float(os.getenv("LLM_EDITORIAL_TIMEOUT_SECONDS", str(LLM_TIMEOUT_SECONDS)))
LLM_FINALIZE_TIMEOUT_SECONDS = float(os.getenv("LLM_FINALIZE_TIMEOUT_SECONDS", str(max(LLM_TIMEOUT_SECONDS, 20))))
LLM_EDITORIAL_RETRY_COUNT = int(os.getenv("LLM_EDITORIAL_RETRY_COUNT", "1"))
LLM_FINALIZE_RETRY_COUNT = int(os.getenv("LLM_FINALIZE_RETRY_COUNT", "1"))
LLM_CACHE_ENABLED = os.getenv("LLM_CACHE_ENABLED", "true").lower() == "true"
LLM_CACHE_TTL_SECONDS = int(os.getenv("LLM_CACHE_TTL_SECONDS", "21600"))
LLM_TRUST_ENV = os.getenv("LLM_TRUST_ENV", "true").lower() == "true"
LLM_DEBUG = os.getenv("LLM_DEBUG", "true").lower() == "true"

# 其他配置
LLM_ENRICH_MAX_ITEMS = int(os.getenv("LLM_ENRICH_MAX_ITEMS", "6"))
LLM_ENRICH_ON_FETCH = os.getenv("LLM_ENRICH_ON_FETCH", "false").lower() == "true"
LLM_EDITORIAL_ENABLED = os.getenv("LLM_EDITORIAL_ENABLED", "true").lower() == "true"
