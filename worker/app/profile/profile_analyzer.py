from app.llm.llm_client import LlmClient
from app.utils import cache
import json
import hashlib

class ProfileAnalyzer:
    def __init__(self):
        self.llm_client = LlmClient()
    
    def analyze_chat(self, chat_history):
        """
        分析聊天历史，提取用户画像信息
        
        Args:
            chat_history: 聊天历史字符串
            
        Returns:
            dict: 用户画像信息
        """
        # 生成缓存键
        cache_key = f"profile_chat_{hashlib.md5(chat_history.encode()).hexdigest()}"
        
        # 检查缓存
        cached_result = cache.get(cache_key)
        if cached_result:
            return cached_result
        
        prompt = (
            "你是一个专业的用户画像分析师，需要从以下聊天历史中提取用户的详细画像信息：\n\n"
            f"聊天历史：\n{chat_history}\n\n"
            "请提取以下信息：\n"
            "1. 兴趣爱好（interests）：用户提到的兴趣、爱好、喜欢的活动等\n"
            "2. 职业（occupation）：用户的职业或工作领域\n"
            "3. 最近活动（recentActivities）：用户最近在做的事情、关注的话题等\n"
            "4. 偏好话题（preferredTopics）：用户表现出兴趣的话题领域\n"
            "5. 学习风格（learningStyle）：用户的学习方式偏好，如视觉型、听觉型、动手型等\n"
            "6. 阅读习惯（readingHabits）：用户的阅读偏好，如阅读时间、阅读长度、喜欢的内容类型等\n"
            "7. 时间可用性（timeAvailability）：用户的时间安排，如工作日时间、周末时间等\n"
            "8. 技术技能（technicalSkills）：用户掌握的技术技能及其熟练程度\n"
            "9. 经验水平（experienceLevel）：用户的专业经验水平，如初级、中级、高级、专家\n"
            "10. 行业焦点（industryFocus）：用户关注的行业领域\n"
            "11. 编码习惯（codingHabits）：用户的编码风格和习惯\n"
            "12. 工具偏好（toolPreferences）：用户偏好使用的工具和技术栈\n"
            "13. 内容格式偏好（contentFormatPreferences）：用户喜欢的内容呈现形式\n"
            "14. 学习目标（learningGoals）：用户的学习和职业目标\n"
            "15. 置信度分数（confidenceScore）：0-100 的分数，表示画像的可信度\n\n"
            "请只返回 JSON 对象，键名使用上述英文字段名。"
        )
        
        try:
            response = self.llm_client._chat(
                prompt=prompt,
                max_tokens=500,
                json_mode=True
            )
        except Exception:
            response = None
        
        if not response:
            return {
                "interests": {},
                "occupation": "",
                "recentActivities": {},
                "preferredTopics": {},
                "learningStyle": "",
                "readingHabits": {},
                "timeAvailability": {},
                "technicalSkills": {},
                "experienceLevel": "",
                "industryFocus": "",
                "codingHabits": "",
                "toolPreferences": {},
                "contentFormatPreferences": "",
                "learningGoals": "",
                "confidenceScore": 0
            }
        
        try:
            result = self.llm_client._parse_json_like(response)
            if not result:
                return {
                    "interests": {},
                    "occupation": "",
                    "recentActivities": {},
                    "preferredTopics": {},
                    "learningStyle": "",
                    "readingHabits": {},
                    "timeAvailability": {},
                    "technicalSkills": {},
                    "experienceLevel": "",
                    "industryFocus": "",
                    "codingHabits": "",
                    "toolPreferences": {},
                    "contentFormatPreferences": "",
                    "learningGoals": "",
                    "confidenceScore": 0
                }
            
            # 保存到缓存
            cache.set(cache_key, result, ttl=86400)  # 缓存一天
            return result
        except Exception as e:
            return {
                "interests": {},
                "occupation": "",
                "recentActivities": {},
                "preferredTopics": {},
                "learningStyle": "",
                "readingHabits": {},
                "timeAvailability": {},
                "technicalSkills": {},
                "experienceLevel": "",
                "industryFocus": "",
                "codingHabits": "",
                "toolPreferences": {},
                "contentFormatPreferences": "",
                "learningGoals": "",
                "confidenceScore": 0
            }
    
    def analyze_survey(self, survey_data):
        """
        分析问卷数据，提取用户画像信息
        
        Args:
            survey_data: 问卷数据字典
            
        Returns:
            dict: 用户画像信息
        """
        # 生成缓存键
        cache_key = f"profile_survey_{hashlib.md5(json.dumps(survey_data, sort_keys=True).encode()).hexdigest()}"
        
        # 检查缓存
        cached_result = cache.get(cache_key)
        if cached_result:
            return cached_result
        
        prompt = (
            "你是一个专业的用户画像分析师，需要从以下问卷数据中提取用户的详细画像信息：\n\n"
            f"问卷数据：\n{json.dumps(survey_data, ensure_ascii=False)}\n\n"
            "请提取以下信息：\n"
            "1. 兴趣爱好（interests）：用户提到的兴趣、爱好、喜欢的活动等\n"
            "2. 职业（occupation）：用户的职业或工作领域\n"
            "3. 最近活动（recentActivities）：用户最近在做的事情、关注的话题等\n"
            "4. 偏好话题（preferredTopics）：用户表现出兴趣的话题领域\n"
            "5. 学习风格（learningStyle）：用户的学习方式偏好，如视觉型、听觉型、动手型等\n"
            "6. 阅读习惯（readingHabits）：用户的阅读偏好，如阅读时间、阅读长度、喜欢的内容类型等\n"
            "7. 时间可用性（timeAvailability）：用户的时间安排，如工作日时间、周末时间等\n"
            "8. 技术技能（technicalSkills）：用户掌握的技术技能及其熟练程度\n"
            "9. 经验水平（experienceLevel）：用户的专业经验水平，如初级、中级、高级、专家\n"
            "10. 行业焦点（industryFocus）：用户关注的行业领域\n"
            "11. 编码习惯（codingHabits）：用户的编码风格和习惯\n"
            "12. 工具偏好（toolPreferences）：用户偏好使用的工具和技术栈\n"
            "13. 内容格式偏好（contentFormatPreferences）：用户喜欢的内容呈现形式\n"
            "14. 学习目标（learningGoals）：用户的学习和职业目标\n"
            "15. 置信度分数（confidenceScore）：0-100 的分数，表示画像的可信度\n\n"
            "请只返回 JSON 对象，键名使用上述英文字段名。"
        )
        
        try:
            response = self.llm_client._chat(
                prompt=prompt,
                max_tokens=500,
                json_mode=True
            )
        except Exception:
            response = None
        
        if not response:
            return {
                "interests": {},
                "occupation": "",
                "recentActivities": {},
                "preferredTopics": {},
                "learningStyle": "",
                "readingHabits": {},
                "timeAvailability": {},
                "technicalSkills": {},
                "experienceLevel": "",
                "industryFocus": "",
                "codingHabits": "",
                "toolPreferences": {},
                "contentFormatPreferences": "",
                "learningGoals": "",
                "confidenceScore": 0
            }
        
        try:
            result = self.llm_client._parse_json_like(response)
            if not result:
                return {
                    "interests": {},
                    "occupation": "",
                    "recentActivities": {},
                    "preferredTopics": {},
                    "learningStyle": "",
                    "readingHabits": {},
                    "timeAvailability": {},
                    "technicalSkills": {},
                    "experienceLevel": "",
                    "industryFocus": "",
                    "codingHabits": "",
                    "toolPreferences": {},
                    "contentFormatPreferences": "",
                    "learningGoals": "",
                    "confidenceScore": 0
                }
            
            # 保存到缓存
            cache.set(cache_key, result, ttl=86400)  # 缓存一天
            return result
        except Exception as e:
            return {
                "interests": {},
                "occupation": "",
                "recentActivities": {},
                "preferredTopics": {},
                "learningStyle": "",
                "readingHabits": {},
                "timeAvailability": {},
                "technicalSkills": {},
                "experienceLevel": "",
                "industryFocus": "",
                "codingHabits": "",
                "toolPreferences": {},
                "contentFormatPreferences": "",
                "learningGoals": "",
                "confidenceScore": 0
            }
