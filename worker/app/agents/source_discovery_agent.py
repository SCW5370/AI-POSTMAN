from langchain_community.chat_models import ChatOpenAI
from langchain_core.tools import Tool
import os
from app.discoverers import RSSDiscoverer, GitHubDiscoverer

class SourceDiscoveryAgent:
    def __init__(self):
        # 初始化发现器（只使用 RSS 发现器，禁用 GitHub 发现器）
        self.rss_discoverer = RSSDiscoverer()
        # self.github_discoverer = GitHubDiscoverer()  # 禁用 GitHub 发现器，避免外网访问
        
        # 创建工具（只保留 RSS 搜索工具）
        self.tools = [
            Tool(
                name="RSSSearch",
                func=self.search_rss,
                description="搜索与主题相关的 RSS 源，返回源的 URL、类型、置信度和发现方法"
            )
            # 禁用 GitHub 搜索工具，避免外网访问
        ]
    
    def search_rss(self, topic):
        """搜索 RSS 源"""
        sources = self.rss_discoverer.discover(topic, batch_size=5)
        return self.format_sources(sources)
    
    def search_github(self, topic):
        """搜索 GitHub 源"""
        sources = self.github_discoverer.discover(topic, batch_size=5)
        return self.format_sources(sources)
    
    def format_sources(self, sources):
        """格式化源列表为可读字符串"""
        if not sources:
            return "未找到相关源"
        
        formatted = []
        for i, source in enumerate(sources, 1):
            formatted.append(f"{i}. URL: {source['url']}")
            formatted.append(f"   类型: {source['source_type']}")
            formatted.append(f"   置信度: {source['confidence']}")
            formatted.append(f"   发现方法: {source['discovery_method']}")
            formatted.append("")
        
        return "\n".join(formatted)
    
    def run(self, topic):
        """运行 Agent 进行源发现"""
        llm_api_key = (os.getenv("LLM_API_KEY") or "").strip()
        llm_model = (os.getenv("LLM_MODEL") or "").strip()
        llm_base = (os.getenv("LLM_API_BASE") or os.getenv("LLM_BASE_URL") or "").strip()
        if not llm_api_key or not llm_model or not llm_base:
            raise ValueError("LLM source-discovery requires LLM_API_KEY / LLM_MODEL / LLM_API_BASE")

        llm = ChatOpenAI(
            model=llm_model,
            openai_api_key=llm_api_key,
            openai_api_base=llm_base,
            temperature=0.3
        )

        # 构建提示
        prompt = f"""
        你是一个专业的信息源发现助手，需要为用户找到与 "{topic}" 相关的高质量信息源。
        
        请执行以下步骤：
        1. 使用可用工具搜索与该主题相关的信息源
        2. 评估每个源的质量、相关性和可靠性
        3. 为用户提供一个经过筛选和排序的源列表
        4. 对每个推荐的源给出简要的推荐理由
        
        请确保推荐的源具有以下特点：
        - 与主题高度相关
        - 内容质量高
        - 更新频率合理
        - 来源可靠
        - 优先推荐国内源
        """
        
        # 搜索 RSS 源（只搜索国内源）
        rss_sources = self.search_rss(topic)
        
        # 构建完整的提示
        full_prompt = f"""
        {prompt}
        
        搜索结果：
        
        RSS 源：
        {rss_sources}
        
        请基于以上搜索结果，为用户提供一个经过筛选和排序的源列表，并对每个推荐的源给出简要的推荐理由。
        """
        
        # 调用 LLM 生成回复
        response = llm.invoke(full_prompt)
        return response.content

    def run_sources(self, topic, batch_size=10):
        """
        返回结构化候选源列表，供后端直接入库。
        """
        # 只使用 RSS 发现器，避免外网访问
        rss_sources = self.rss_discoverer.discover(topic, batch_size=max(1, batch_size))
        # 不使用 GitHub 发现器，避免外网访问
        # github_sources = self.github_discoverer.discover(topic, batch_size=max(1, batch_size))
        
        seen = set()
        deduped = []
        for source in rss_sources:
            url = (source or {}).get("url")
            if not url or url in seen:
                continue
            seen.add(url)
            item = dict(source)
            method = item.get("discovery_method") or "agent"
            item["discovery_method"] = f"agent_{method}"
            deduped.append(item)
        deduped.sort(key=lambda s: float(s.get("confidence", 0.0)), reverse=True)
        return deduped[:max(1, batch_size)]
