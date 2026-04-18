class BaseDiscoverer:
    """
    基础源发现器类，定义通用接口
    """
    
    def discover(self, topic, batch_size=10):
        """
        发现与主题相关的源
        
        Args:
            topic: 主题
            batch_size: 最大返回数量
            
        Returns:
            list: 发现的源列表，每个元素包含 url、source_type、confidence、discovery_method
        """
        raise NotImplementedError("Subclasses must implement discover method")
    
    def _validate_source(self, url):
        """
        验证源是否有效
        
        Args:
            url: 源 URL
            
        Returns:
            bool: 是否有效
        """
        # 基础验证逻辑，子类可以重写
        if not url or not isinstance(url, str):
            return False
        return True
    
    def _calculate_confidence(self, topic, source):
        """
        计算源与主题的相关度
        
        Args:
            topic: 主题
            source: 源信息
            
        Returns:
            float: 置信度（0-100）
        """
        # 基础计算逻辑，子类可以重写
        return 70.0  # 默认置信度
