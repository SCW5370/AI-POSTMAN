import os
from dotenv import load_dotenv
from langchain_community.chat_models import ChatOpenAI

# 加载.env文件
load_dotenv()

print("Testing LLM connection...")
print(f"LLM_MODEL: {os.getenv('LLM_MODEL')}")
print(f"LLM_API_KEY: {os.getenv('LLM_API_KEY')}")
print(f"LLM_API_BASE: {os.getenv('LLM_API_BASE')}")

try:
    # 初始化LLM
    llm = ChatOpenAI(
        model=os.getenv("LLM_MODEL", "gpt-4o-mini"),
        openai_api_key=os.getenv("LLM_API_KEY"),
        openai_api_base=os.getenv("LLM_API_BASE"),
        temperature=0.3
    )
    print("LLM initialized successfully!")
    
    # 测试LLM调用
    test_prompt = "你好，我是AI Postman，测试LLM连接是否正常。"
    print(f"Testing LLM with prompt: {test_prompt}")
    response = llm.invoke(test_prompt)
    print("LLM call completed successfully!")
    print("\nResponse:")
    print(response.content)
    
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()