from app.discoverers import RSSDiscoverer, GitHubDiscoverer, SocialMediaDiscoverer
import asyncio

class SourceDiscoverer:
    """
    源发现服务，整合所有发现器
    """
    
    def __init__(self):
        self.discoverers = [
            RSSDiscoverer(),
            GitHubDiscoverer(),
            SocialMediaDiscoverer()
        ]
    
    async def discover_sources(self, topic, batch_size=10):
        """
        异步发现与主题相关的源
        
        Args:
            topic: 主题
            batch_size: 最大返回数量
            
        Returns:
            list: 发现的源列表
        """
        from app.utils import cache
        import hashlib
        
        # 生成缓存键
        cache_key = f"discover_sources_{hashlib.md5(topic.encode()).hexdigest()}_{batch_size}"
        
        # 检查缓存
        cached_result = cache.get(cache_key)
        if cached_result:
            return cached_result
        
        # 并行执行所有发现器
        tasks = []
        for discoverer in self.discoverers:
            task = asyncio.to_thread(discoverer.discover, topic, batch_size)
            tasks.append(task)
        
        # 收集所有结果
        results = await asyncio.gather(*tasks)
        
        # 合并结果并去重
        all_sources = []
        seen_urls = set()
        
        for sources in results:
            for source in sources:
                if source['url'] not in seen_urls:
                    seen_urls.add(source['url'])
                    all_sources.append(source)
        
        # 按置信度排序并限制数量
        all_sources.sort(key=lambda x: x['confidence'], reverse=True)
        result = all_sources[:batch_size]
        
        # 保存到缓存
        cache.set(cache_key, result, ttl=3600)  # 缓存一小时
        
        return result
    
    def discover_sources_sync(self, topic, batch_size=10):
        """
        同步发现与主题相关的源
        
        Args:
            topic: 主题
            batch_size: 最大返回数量
            
        Returns:
            list: 发现的源列表
        """
        return asyncio.run(self.discover_sources(topic, batch_size))
