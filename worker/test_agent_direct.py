import os
import sys
from dotenv import load_dotenv

sys.path.insert(0, '/Users/shichangwei/Desktop/goal/ai-postman/worker')

load_dotenv('/Users/shichangwei/Desktop/goal/ai-postman/worker/.env')

print("=" * 50)
print("Testing Agent Source Discovery")
print("=" * 50)
print(f"LLM_MODEL: {os.getenv('LLM_MODEL')}")
print(f"LLM_API_BASE: {os.getenv('LLM_API_BASE')}")
print()

try:
    from app.agents.source_discovery_agent import SourceDiscoveryAgent

    print("Initializing agent...")
    agent = SourceDiscoveryAgent()
    print("Agent initialized successfully!")
    print()

    print("Running source discovery for topic: 人工智能最新发展")
    print("-" * 50)
    result = agent.run("人工智能最新发展")
    print("Source discovery completed!")
    print()
    print("=" * 50)
    print("RESULT:")
    print("=" * 50)
    print(result)
    print("=" * 50)

except Exception as e:
    print(f"ERROR: {e}")
    import traceback
    traceback.print_exc()