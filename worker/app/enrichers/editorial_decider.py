from __future__ import annotations
import os
from typing import Optional

from dotenv import load_dotenv
from app.llm.llm_client import LlmClient
from app.llm.schemas import EditorialDecisionRequest, EditorialDecisionResponse


def decide_sections(request: EditorialDecisionRequest) -> EditorialDecisionResponse:
    # 每次请求刷新 .env，确保使用用户在前端最新保存的 LLM 配置
    load_dotenv(override=True)
    llm_editorial_enabled = os.getenv("LLM_EDITORIAL_ENABLED", "true").lower() == "true"
    llm_client = LlmClient()

    if llm_editorial_enabled and llm_client.is_enabled():
        decision = llm_client.editorial_decision(request.model_dump())
        if decision is not None:
            print("[worker] editorial strategy=llm used=true")
            return EditorialDecisionResponse(
                decisions=decision.get("decisions", []),
                llmUsed=True,
                strategy="llm",
                fallbackReason=None,
            )
        reason = llm_client.get_last_error() or "llm_empty_or_parse_failed"
        print(f"[worker] editorial strategy=heuristic used=false fallback={reason}")
        return heuristic_decision(request, fallback_reason=reason)
    if not llm_editorial_enabled:
        print("[worker] editorial strategy=heuristic used=false fallback=disabled_by_config")
        return heuristic_decision(request, fallback_reason="disabled_by_config")
    print("[worker] editorial strategy=heuristic used=false fallback=llm_not_configured")
    return heuristic_decision(request, fallback_reason="llm_not_configured")


def heuristic_decision(request: EditorialDecisionRequest, fallback_reason: Optional[str] = None) -> EditorialDecisionResponse:
    decisions = []
    max_items = request.preference.maxItemsPerDigest
    preferred_topics = [topic.lower() for topic in request.preference.preferredTopics]
    
    # 从用户偏好中提取更多信息
    user_profile = getattr(request.preference, 'userProfile', {})
    reading_habits = user_profile.get('readingHabits', {})
    preferred_length = reading_habits.get('preferredLength', '中等')
    preferred_time = reading_habits.get('preferredTime', '晚上')
    learning_style = user_profile.get('learningStyle', '视觉型')
    experience_level = user_profile.get('experienceLevel', '中级')
    content_format_preferences = user_profile.get('contentFormatPreferences', '简洁明了')

    # 计算每个候选内容的综合得分
    scored_candidates = []
    for item in request.candidates:
        _summary = item.summary or ""
        combined = f"{item.title} {_summary}".lower()
        preferred_hit = any(topic in combined for topic in preferred_topics if topic)
        
        # 基础得分
        base_score = item.ruleScore
        
        # 根据用户偏好调整得分
        score_adjustment = 0.0
        
        # 内容长度偏好
        content_length = len(combined)
        if preferred_length == '简短' and content_length < 500:
            score_adjustment += 2.0
        elif preferred_length == '详细' and content_length > 1000:
            score_adjustment += 2.0
        
        # 内容类型偏好
        if '图文并茂' in content_format_preferences and 'image' in item.tags:
            score_adjustment += 1.5
        
        # 经验水平匹配
        if experience_level == '初级' and 'beginner' in combined:
            score_adjustment += 2.0
        elif experience_level == '高级' and ('advanced' in combined or 'expert' in combined):
            score_adjustment += 2.0
        
        # 主题匹配
        if preferred_hit:
            score_adjustment += 3.0
        
        # 时效性调整（如果有发布时间）
        if hasattr(item, 'publishedAt') and item.publishedAt:
            # 假设 publishedAt 是 ISO 格式的字符串
            from datetime import datetime, timedelta
            try:
                published_date = datetime.fromisoformat(item.publishedAt.replace('Z', '+00:00'))
                days_since_published = (datetime.now().astimezone() - published_date).days
                if days_since_published <= 1:
                    score_adjustment += 2.0  # 最新内容
                elif days_since_published <= 7:
                    score_adjustment += 1.0  # 一周内的内容
            except:
                pass
        
        total_score = base_score + score_adjustment
        scored_candidates.append((item, total_score, score_adjustment))
    
    # 按综合得分排序
    sorted_candidates = sorted(scored_candidates, key=lambda x: x[1], reverse=True)[:max_items]
    
    for index, (item, total_score, score_adjustment) in enumerate(sorted_candidates):
        _summary = item.summary or ""
        combined = f"{item.title} {_summary}".lower()
        preferred_hit = any(topic in combined for topic in preferred_topics if topic)
        
        # 确定内容分类
        if index == 0:
            section = "MUST_READ"
            reason = "这条内容和你的当前关注最接近，适合作为今天首先阅读的一条。"
        elif preferred_hit and index < 3:
            section = "FOCUS_UPDATES"
            reason = "这条内容延续了你正在关注的方向，适合放进今天的持续跟进区。"
        elif preferred_hit:
            section = "WORTH_SAVING"
            reason = "这条内容与你的主线目标有关，适合先收藏下来，后续再展开。"
        elif 'tutorial' in combined or 'guide' in combined:
            section = "WORTH_SAVING"
            reason = "这条内容包含教程或指南，适合作为后续学习资料留存。"
        elif 'news' in combined or 'update' in combined:
            section = "FOCUS_UPDATES"
            reason = "这条内容属于最新动态，适合放进今天的持续跟进区。"
        else:
            section = "SURPRISE"
            reason = "这条内容不是核心主线，但和你的方向足够邻近，适合作为一点扩展。"

        decisions.append(
            {
                "normalizedItemId": item.normalizedItemId,
                "section": section,
                "scoreAdjustment": score_adjustment,
                "reason": reason,
            }
        )
    return EditorialDecisionResponse(
        decisions=decisions,
        llmUsed=False,
        strategy="enhanced_heuristic",
        fallbackReason=fallback_reason,
    )
