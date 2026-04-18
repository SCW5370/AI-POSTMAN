from .base_discoverer import BaseDiscoverer
import feedparser
import requests
from bs4 import BeautifulSoup
import time

class RSSDiscoverer(BaseDiscoverer):
    """
    RSS 源发现器，基于 RSSHub 和 Feed 搜索
    """
    
    def __init__(self):
        self.rsshub_endpoints = [
            "https://rsshub.app/",
            "https://rsshub.rssforever.com/"
        ]
        self.media_rss_directory = [
            # 国内技术媒体
            "https://www.infoq.cn/feed",
            "https://www.oschina.net/blog/rss",
            "https://www.csdn.net/rss",
            "https://www.jiqizhixin.com/rss",
            "https://www.qbitai.com/feed",
            "https://sspai.com/feed",
            "https://www.ifanr.com/feed",
            "https://36kr.com/feed",
            "https://www.huxiu.com/rss/0.xml",
            "https://www.ithome.com/rss/",
            "https://www.zol.com.cn/rss/allnews.xml",
            "https://juejin.cn/rss/backend",
            "https://juejin.cn/rss/ai",
            "https://segmentfault.com/feeds/blogs",
        ]
    
    def discover(self, topic, batch_size=10):
        """
        发现与主题相关的 RSS 源
        """
        discovered_sources = []
        
        # 1. 从媒体 RSS 目录中筛选
        for rss_url in self.media_rss_directory:
            if len(discovered_sources) >= batch_size:
                break
            
            try:
                # 验证 RSS 源是否有效
                feed = self._safe_parse_feed(rss_url, timeout_seconds=6)
                if feed is None:
                    continue
                if feed.bozo == 0 and feed.entries:
                    confidence = self._calculate_confidence(topic, rss_url)
                    if confidence > 50:
                        discovered_sources.append({
                            "url": rss_url,
                            "source_type": "RSS",
                            "confidence": confidence,
                            "discovery_method": "media_directory"
                        })
            except Exception as e:
                print(f"Error checking RSS {rss_url}: {e}")
            time.sleep(0.2)  # 避免请求过快，同时控制总耗时
        
        # 2. 尝试通过 RSSHub 发现
        for endpoint in self.rsshub_endpoints:
            if len(discovered_sources) >= batch_size:
                break
            
            try:
                # 构建 RSSHub URL（这里只是示例，实际需要根据 RSSHub 的规则构建）
                # 注意：RSSHub 的具体路径需要根据实际情况调整
                rsshub_url = f"{endpoint}search/baidu/{topic}"
                feed = self._safe_parse_feed(rsshub_url, timeout_seconds=6)
                if feed is None:
                    continue
                if feed.bozo == 0 and feed.entries:
                    # 从结果中提取可能的 RSS 源
                    for entry in feed.entries[:5]:  # 最多取前 5 个
                        if len(discovered_sources) >= batch_size:
                            break
                        
                        # 尝试从页面中提取 RSS 链接
                        rss_link = self._extract_rss_link(entry.link)
                        if rss_link and self._validate_source(rss_link):
                            confidence = self._calculate_confidence(topic, rss_link)
                            if confidence > 60:
                                discovered_sources.append({
                                    "url": rss_link,
                                    "source_type": "RSS",
                                    "confidence": confidence,
                                    "discovery_method": "rsshub"
                                })
            except Exception as e:
                print(f"Error using RSSHub {endpoint}: {e}")
            time.sleep(0.3)  # 避免请求过快，同时控制总耗时
        
        return discovered_sources
    
    def _extract_rss_link(self, url):
        """
        从网页中提取 RSS 链接
        """
        try:
            response = requests.get(url, timeout=5)
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # 查找 link 标签中的 RSS 链接
            for link in soup.find_all('link', rel='alternate'):
                if link.get('type') in ['application/rss+xml', 'application/atom+xml']:
                    rss_url = link.get('href')
                    # 处理相对 URL
                    if not rss_url.startswith('http'):
                        from urllib.parse import urljoin
                        rss_url = urljoin(url, rss_url)
                    return rss_url
        except Exception as e:
            print(f"Error extracting RSS link from {url}: {e}")
        return None

    def _safe_parse_feed(self, url, timeout_seconds=6):
        """
        先用 requests 设置明确超时，再交给 feedparser 解析，避免 feedparser 直连时长时间阻塞。
        """
        try:
            response = requests.get(
                url,
                timeout=(3, timeout_seconds),
                headers={"User-Agent": "ai-postman-discoverer/1.0"},
            )
            if response.status_code >= 400:
                return None
            return feedparser.parse(response.content)
        except Exception as e:
            print(f"Error fetching feed {url}: {e}")
            return None
    
    def _calculate_confidence(self, topic, source):
        """
        计算 RSS 源与主题的相关度
        """
        # 简单的关键词匹配
        topic_lower = topic.lower()
        source_lower = source.lower()
        
        # 检查主题关键词是否出现在源 URL 中
        if topic_lower in source_lower:
            return 85.0
        
        # 检查是否是已知的媒体源
        if source in self.media_rss_directory:
            return 75.0
        
        return 60.0
