import time
from typing import Any, Optional, Dict, OrderedDict

class LRUCache:
    """
    LRU (最近最少使用) 缓存实现
    支持缓存大小限制和自动清理过期项
    """
    
    def __init__(self, max_size: int = 1000, default_ttl: int = 3600):
        """
        初始化缓存
        
        Args:
            max_size: 缓存最大容量
            default_ttl: 默认过期时间（秒）
        """
        self.cache: OrderedDict[str, tuple[Any, float]] = OrderedDict()
        self.max_size = max_size
        self.default_ttl = default_ttl
    
    def get(self, key: str) -> Optional[Any]:
        """
        获取缓存值
        
        Args:
            key: 缓存键
            
        Returns:
            缓存值，如果不存在或已过期则返回 None
        """
        if key not in self.cache:
            return None
        
        # 检查是否过期
        value, expiry = self.cache[key]
        if time.time() > expiry:
            del self.cache[key]
            return None
        
        # 更新访问顺序（移到末尾表示最近使用）
        self.cache.move_to_end(key)
        return value
    
    def set(self, key: str, value: Any, ttl: Optional[int] = None) -> None:
        """
        设置缓存值
        
        Args:
            key: 缓存键
            value: 缓存值
            ttl: 过期时间（秒），如果不指定则使用默认值
        """
        # 清理过期项
        self._clean_expired()
        
        # 如果键已存在，先删除
        if key in self.cache:
            del self.cache[key]
        
        # 如果缓存已满，删除最久未使用的项
        if len(self.cache) >= self.max_size:
            self.cache.popitem(last=False)
        
        # 设置新值
        expiry = time.time() + (ttl or self.default_ttl)
        self.cache[key] = (value, expiry)
    
    def delete(self, key: str) -> None:
        """
        删除缓存值
        
        Args:
            key: 缓存键
        """
        if key in self.cache:
            del self.cache[key]
    
    def clear(self) -> None:
        """
        清空缓存
        """
        self.cache.clear()
    
    def size(self) -> int:
        """
        获取缓存大小
        
        Returns:
            缓存项数量
        """
        # 清理过期项
        self._clean_expired()
        return len(self.cache)
    
    def _clean_expired(self) -> None:
        """
        清理过期的缓存项
        """
        expired_keys = []
        current_time = time.time()
        
        for key, (_, expiry) in self.cache.items():
            if current_time > expiry:
                expired_keys.append(key)
        
        for key in expired_keys:
            del self.cache[key]
    
    def get_stats(self) -> Dict[str, int]:
        """
        获取缓存统计信息
        
        Returns:
            缓存统计信息
        """
        self._clean_expired()
        return {
            "size": len(self.cache),
            "max_size": self.max_size,
            "default_ttl": self.default_ttl
        }

# 创建全局缓存实例
cache = LRUCache(max_size=1000, default_ttl=3600)
