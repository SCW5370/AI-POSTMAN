import os
from dotenv import load_dotenv
from app.agents.source_discovery_agent import SourceDiscoveryAgent

# 加载.env文件
load_dotenv()

print("Testing SourceDiscoveryAgent...")
print(f"LLM_MODEL: {os.getenv('LLM_MODEL')}")
print(f"LLM_API_KEY: {os.getenv('LLM_API_KEY')}")
print(f"LLM_API_BASE: {os.getenv('LLM_API_BASE')}")

try:
    # 初始化agent
    agent = SourceDiscoveryAgent()
    print("Agent initialized successfully!")
    
    # 测试agent运行
    topic = "人工智能最新发展"
    print(f"Running agent for topic: {topic}")
    result = agent.run(topic)
    print("Agent run completed successfully!")
    print("\nResult:")
    print(result)
    
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()