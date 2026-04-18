from app.llm.llm_client import LlmClient
import json

class SourceEvaluator:
    def __init__(self):
        self.llm_client = LlmClient()
    
    def evaluate_candidate(self, url, topic, source_type, confidence):
        """
        使用 LLM 评估候选源是否应该被批准
        
        Args:
            url: 源的 URL
            topic: 主题
            source_type: 源类型
            confidence: 置信度
            
        Returns:
            dict: 包含评估结果和理由
        """
        prompt = f"""
        你是一个专业的信息源审核员，需要评估以下候选源是否应该被批准加入 AI Postman 系统：
        
        URL: {url}
        主题: {topic}
        源类型: {source_type}
        置信度: {confidence}
        
        审核标准：
        1. 相关性：源的内容是否与主题高度相关
        2. 可信度：源是否来自可靠的发布者
        3. 质量：源的内容质量是否高
        4. 更新频率：源是否定期更新
        5. 多样性：源是否能提供独特的视角
        
        请返回一个 JSON 对象，包含：
        - approve: 布尔值，表示是否批准
        - reason: 字符串，解释批准或拒绝的理由
        - score: 0-100 的分数，表示源的整体质量
        """
        
        response = self.llm_client._chat(
            prompt=prompt,
            max_tokens=200,
            json_mode=True
        )
        
        if not response:
            return {
                "approve": False,
                "reason": "LLM 评估失败",
                "score": 0
            }
        
        try:
            result = self.llm_client._parse_json_like(response)
            if not result:
                return {
                    "approve": False,
                    "reason": "LLM 返回格式错误",
                    "score": 0
                }
            
            # 确保返回的结果包含所有必要字段
            return {
                "approve": result.get("approve", False),
                "reason": result.get("reason", "无理由"),
                "score": result.get("score", 0)
            }
        except Exception as e:
            return {
                "approve": False,
                "reason": f"解析结果失败: {str(e)}",
                "score": 0
            }
