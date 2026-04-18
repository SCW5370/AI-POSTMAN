from app.discoverers.base_discoverer import BaseDiscoverer
import requests
import json
import os

class SocialMediaDiscoverer(BaseDiscoverer):
    """
    社交媒体源发现器，从社交媒体平台获取内容
    """
    
    def __init__(self):
        self.api_keys = {
            "twitter": os.getenv("TWITTER_API_KEY", ""),
            "linkedin": os.getenv("LINKEDIN_API_KEY", "")
        }
    
    def discover(self, topic, batch_size=10):
        """
        从社交媒体平台发现与主题相关的源
        
        Args:
            topic: 主题
            batch_size: 最大返回数量
            
        Returns:
            list: 发现的源列表
        """
        sources = []
        
        # 从Twitter获取源
        if self.api_keys["twitter"]:
            twitter_sources = self._discover_from_twitter(topic, batch_size // 2)
            sources.extend(twitter_sources)
        
        # 从LinkedIn获取源
        if self.api_keys["linkedin"]:
            linkedin_sources = self._discover_from_linkedin(topic, batch_size // 2)
            sources.extend(linkedin_sources)
        
        # 从Reddit获取源（无需API密钥）
        reddit_sources = self._discover_from_reddit(topic, batch_size // 2)
        sources.extend(reddit_sources)
        
        # 去重并限制数量
        seen_urls = set()
        unique_sources = []
        for source in sources:
            if source["url"] not in seen_urls:
                seen_urls.add(source["url"])
                unique_sources.append(source)
        
        # 按置信度排序并限制数量
        unique_sources.sort(key=lambda x: x["confidence"], reverse=True)
        return unique_sources[:batch_size]
    
    def _discover_from_twitter(self, topic, batch_size):
        """
        从Twitter发现源
        
        Args:
            topic: 主题
            batch_size: 最大返回数量
            
        Returns:
            list: 发现的源列表
        """
        sources = []
        try:
            # 这里是模拟实现，实际使用时需要调用Twitter API
            # 示例：使用Twitter API搜索与主题相关的用户或话题
            # response = requests.get(f"https://api.twitter.com/2/tweets/search/recent", params={"query": topic}, headers={"Authorization": f"Bearer {self.api_keys['twitter']}"})
            # data = response.json()
            
            # 模拟返回数据
            mock_sources = [
                {"url": f"https://twitter.com/topic/{topic.replace(' ', '_')}", "source_type": "social_media", "confidence": 85.0, "discovery_method": "twitter"},
                {"url": f"https://twitter.com/{topic.replace(' ', '')}_news", "source_type": "social_media", "confidence": 80.0, "discovery_method": "twitter"},
                {"url": f"https://twitter.com/{topic}_community", "source_type": "social_media", "confidence": 75.0, "discovery_method": "twitter"}
            ]
            sources.extend(mock_sources[:batch_size])
        except Exception as e:
            print(f"从Twitter发现源失败: {e}")
        return sources
    
    def _discover_from_linkedin(self, topic, batch_size):
        """
        从LinkedIn发现源
        
        Args:
            topic: 主题
            batch_size: 最大返回数量
            
        Returns:
            list: 发现的源列表
        """
        sources = []
        try:
            # 这里是模拟实现，实际使用时需要调用LinkedIn API
            # 示例：使用LinkedIn API搜索与主题相关的公司或群组
            # response = requests.get(f"https://api.linkedin.com/v2/search", params={"q": topic}, headers={"Authorization": f"Bearer {self.api_keys['linkedin']}"})
            # data = response.json()
            
            # 模拟返回数据
            mock_sources = [
                {"url": f"https://linkedin.com/groups/{topic.replace(' ', '-')}-group", "source_type": "social_media", "confidence": 88.0, "discovery_method": "linkedin"},
                {"url": f"https://linkedin.com/company/{topic.replace(' ', '')}-inc", "source_type": "social_media", "confidence": 82.0, "discovery_method": "linkedin"}
            ]
            sources.extend(mock_sources[:batch_size])
        except Exception as e:
            print(f"从LinkedIn发现源失败: {e}")
        return sources
    
    def _discover_from_reddit(self, topic, batch_size):
        """
        从Reddit发现源
        
        Args:
            topic: 主题
            batch_size: 最大返回数量
            
        Returns:
            list: 发现的源列表
        """
        sources = []
        try:
            # 使用Reddit API搜索与主题相关的subreddit
            response = requests.get(f"https://www.reddit.com/api/search_subreddits.json", params={"q": topic, "limit": batch_size})
            if response.status_code == 200:
                data = response.json()
                for subreddit in data.get("data", {}).get("children", []):
                    subreddit_data = subreddit.get("data", {})
                    url = f"https://reddit.com{subreddit_data.get('url', '')}"
                    if self._validate_source(url):
                        confidence = self._calculate_confidence(topic, subreddit_data)
                        sources.append({
                            "url": url,
                            "source_type": "social_media",
                            "confidence": confidence,
                            "discovery_method": "reddit"
                        })
            else:
                # 模拟返回数据
                mock_sources = [
                    {"url": f"https://reddit.com/r/{topic.replace(' ', '')}", "source_type": "social_media", "confidence": 78.0, "discovery_method": "reddit"},
                    {"url": f"https://reddit.com/r/{topic}_news", "source_type": "social_media", "confidence": 72.0, "discovery_method": "reddit"}
                ]
                sources.extend(mock_sources[:batch_size])
        except Exception as e:
            print(f"从Reddit发现源失败: {e}")
            # 模拟返回数据
            mock_sources = [
                {"url": f"https://reddit.com/r/{topic.replace(' ', '')}", "source_type": "social_media", "confidence": 78.0, "discovery_method": "reddit"},
                {"url": f"https://reddit.com/r/{topic}_news", "source_type": "social_media", "confidence": 72.0, "discovery_method": "reddit"}
            ]
            sources.extend(mock_sources[:batch_size])
        return sources
    
    def _calculate_confidence(self, topic, source):
        """
        计算源与主题的相关度
        
        Args:
            topic: 主题
            source: 源信息
            
        Returns:
            float: 置信度（0-100）
        """
        # 基础置信度
        base_confidence = 70.0
        
        # 如果源信息中包含主题关键词，增加置信度
        if isinstance(source, dict):
            source_text = " ".join(str(value) for value in source.values()).lower()
            topic_words = topic.lower().split()
            for word in topic_words:
                if word in source_text:
                    base_confidence += 5.0
        
        # 限制置信度范围
        return min(base_confidence, 95.0)
