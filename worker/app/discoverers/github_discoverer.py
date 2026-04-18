from .base_discoverer import BaseDiscoverer
import requests
import time

class GitHubDiscoverer(BaseDiscoverer):
    """
    GitHub 源发现器，基于 GitHub topic/repo
    """
    
    def __init__(self):
        self.github_api_url = "https://api.github.com"
        self.headers = {
            "Accept": "application/vnd.github.v3+json"
        }
        # 可选：如果有 GitHub API token，可以添加到 headers 中以提高速率限制
        # self.headers["Authorization"] = f"token {your_token}"
    
    def discover(self, topic, batch_size=10):
        """
        发现与主题相关的 GitHub 源
        """
        discovered_sources = []
        
        # 1. 搜索相关的 GitHub 仓库
        try:
            # 构建搜索 URL
            search_url = f"{self.github_api_url}/search/repositories"
            params = {
                "q": topic,
                "sort": "stars",
                "order": "desc",
                "per_page": batch_size
            }
            
            response = requests.get(search_url, headers=self.headers, params=params, timeout=10)
            if response.status_code == 200:
                repositories = response.json().get("items", [])
                
                for repo in repositories:
                    if len(discovered_sources) >= batch_size:
                        break
                    
                    # 提取仓库的 RSS 源（releases 或 commits）
                    repo_name = repo["full_name"]
                    
                    # 添加 releases RSS
                    releases_rss = f"https://github.com/{repo_name}/releases.atom"
                    if self._validate_source(releases_rss):
                        confidence = self._calculate_confidence(topic, repo)
                        discovered_sources.append({
                            "url": releases_rss,
                            "source_type": "GITHUB_RELEASES",
                            "confidence": confidence,
                            "discovery_method": "github_search"
                        })
                    
                    # 添加 commits RSS（如果还有空间）
                    if len(discovered_sources) < batch_size:
                        commits_rss = f"https://github.com/{repo_name}/commits.atom"
                        if self._validate_source(commits_rss):
                            confidence = self._calculate_confidence(topic, repo) * 0.9  # commits 的置信度稍低
                            discovered_sources.append({
                                "url": commits_rss,
                                "source_type": "GITHUB_COMMITS",
                                "confidence": confidence,
                                "discovery_method": "github_search"
                            })
                    
                    time.sleep(0.2)  # 避免请求过快，同时控制总耗时
        except Exception as e:
            print(f"Error searching GitHub: {e}")
        
        # 2. 搜索相关的 GitHub 主题
        if len(discovered_sources) < batch_size:
            try:
                topic_url = f"{self.github_api_url}/search/topics"
                params = {
                    "q": topic,
                    "sort": "popular",
                    "per_page": 5
                }
                
                response = requests.get(topic_url, headers=self.headers, params=params, timeout=10)
                if response.status_code == 200:
                    topics = response.json().get("items", [])
                    
                    for topic_item in topics:
                        if len(discovered_sources) >= batch_size:
                            break
                        
                        topic_name = topic_item["name"]
                        # 获取该主题的热门仓库
                        repo_url = f"{self.github_api_url}/topics/{topic_name}/repos"
                        repo_params = {
                            "sort": "stars",
                            "per_page": 2
                        }
                        
                        repo_response = requests.get(repo_url, headers=self.headers, params=repo_params, timeout=10)
                        if repo_response.status_code == 200:
                            topic_repos = repo_response.json()
                            for repo in topic_repos:
                                if len(discovered_sources) >= batch_size:
                                    break
                                
                                repo_name = repo["full_name"]
                                releases_rss = f"https://github.com/{repo_name}/releases.atom"
                                if self._validate_source(releases_rss):
                                    confidence = self._calculate_confidence(topic, repo) * 0.85  # 主题相关的置信度稍低
                                    discovered_sources.append({
                                        "url": releases_rss,
                                        "source_type": "GITHUB_RELEASES",
                                        "confidence": confidence,
                                        "discovery_method": "github_topic"
                                    })
                        
                        time.sleep(0.2)  # 避免请求过快，同时控制总耗时
            except Exception as e:
                print(f"Error searching GitHub topics: {e}")
        
        return discovered_sources
    
    def _calculate_confidence(self, topic, repo):
        """
        计算 GitHub 仓库与主题的相关度
        """
        topic_lower = topic.lower()
        
        # 检查主题是否出现在仓库名称或描述中
        relevance_score = 0
        
        if topic_lower in repo.get("name", "").lower():
            relevance_score += 30
        if topic_lower in repo.get("description", "").lower():
            relevance_score += 25
        
        # 检查主题是否出现在仓库的 topics 中
        repo_topics = repo.get("topics", [])
        for repo_topic in repo_topics:
            if topic_lower in repo_topic.lower():
                relevance_score += 20
                break
        
        # 根据 stars 数量调整置信度
        stars = repo.get("stargazers_count", 0)
        if stars > 10000:
            relevance_score += 20
        elif stars > 1000:
            relevance_score += 15
        elif stars > 100:
            relevance_score += 10
        
        # 确保置信度在合理范围内
        return min(95.0, max(60.0, relevance_score))
