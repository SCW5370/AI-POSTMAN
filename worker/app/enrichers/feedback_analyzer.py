from app.llm.llm_client import LlmClient
import json

class FeedbackAnalyzer:
    """
    反馈分析器，用于分析用户反馈并提取有价值的信息
    """
    
    def __init__(self):
        self.llm_client = LlmClient()
    
    def analyze_feedback(self, feedback_data):
        """
        分析用户反馈，提取有价值的信息
        
        Args:
            feedback_data: 反馈数据，包含用户对内容的评价、评分等
            
        Returns:
            dict: 分析结果，包含反馈类型、情感倾向、关键词等
        """
        prompt = f"""
        你是一个专业的反馈分析专家，需要从以下用户反馈中提取有价值的信息：
        
        反馈数据：
        {json.dumps(feedback_data, ensure_ascii=False)}
        
        请提取以下信息：
        1. 反馈类型（feedbackType）：如内容质量、相关性、格式、时效性等
        2. 情感倾向（sentiment）：正面、负面、中性
        3. 关键词（keywords）：反馈中提到的重要关键词
        4. 改进建议（improvementSuggestions）：用户可能的改进期望
        5. 内容偏好（contentPreferences）：用户表现出的内容偏好
        6. 置信度分数（confidenceScore）：0-100的分数，表示分析结果的可信度
        
        请返回一个JSON对象，包含以上字段。
        示例输出格式：
        {
            "feedbackType": "内容质量",
            "sentiment": "正面",
            "keywords": ["AI技术", "深度学习", "实用性"],
            "improvementSuggestions": ["增加更多实例", "提供代码示例"],
            "contentPreferences": {"技术深度": "中等", "实例数量": "多", "格式": "图文并茂"},
            "confidenceScore": 90
        }
        """
        
        response = self.llm_client._chat(
            prompt=prompt,
            max_tokens=500,
            json_mode=True
        )
        
        if not response:
            return {
                "feedbackType": "",
                "sentiment": "中性",
                "keywords": [],
                "improvementSuggestions": [],
                "contentPreferences": {},
                "confidenceScore": 0
            }
        
        try:
            result = self.llm_client._parse_json_like(response)
            if not result:
                return {
                    "feedbackType": "",
                    "sentiment": "中性",
                    "keywords": [],
                    "improvementSuggestions": [],
                    "contentPreferences": {},
                    "confidenceScore": 0
                }
            
            return result
        except Exception as e:
            return {
                "feedbackType": "",
                "sentiment": "中性",
                "keywords": [],
                "improvementSuggestions": [],
                "contentPreferences": {},
                "confidenceScore": 0
            }
    
    def integrate_feedback(self, user_profile, feedback_analysis):
        """
        将反馈分析结果整合到用户画像中
        
        Args:
            user_profile: 用户画像数据
            feedback_analysis: 反馈分析结果
            
        Returns:
            dict: 更新后的用户画像
        """
        updated_profile = user_profile.copy()
        
        # 更新用户的内容偏好
        if feedback_analysis.get("contentPreferences"):
            if "contentFormatPreferences" not in updated_profile:
                updated_profile["contentFormatPreferences"] = ""
            
            # 整合内容格式偏好
            if feedback_analysis["contentPreferences"].get("格式"):
                updated_profile["contentFormatPreferences"] = feedback_analysis["contentPreferences"]["格式"]
        
        # 更新用户的偏好话题
        if feedback_analysis.get("keywords"):
            if "preferredTopics" not in updated_profile:
                updated_profile["preferredTopics"] = {}
            
            # 增加关键词的权重
            for keyword in feedback_analysis["keywords"]:
                if keyword in updated_profile["preferredTopics"]:
                    updated_profile["preferredTopics"][keyword] = min(updated_profile["preferredTopics"][keyword] + 10, 100)
                else:
                    updated_profile["preferredTopics"][keyword] = 80
        
        # 更新用户的阅读习惯
        if feedback_analysis.get("feedbackType") == "内容长度":
            if "readingHabits" not in updated_profile:
                updated_profile["readingHabits"] = {}
            
            if feedback_analysis.get("sentiment") == "负面":
                updated_profile["readingHabits"]["preferredLength"] = "简短"
            elif feedback_analysis.get("sentiment") == "正面":
                updated_profile["readingHabits"]["preferredLength"] = "详细"
        
        return updated_profile
