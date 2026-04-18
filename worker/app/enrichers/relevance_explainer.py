def build_relevance_reason(item: dict) -> str:
    tags = item.get("tags") or []
    title = item.get("title_clean", "")
    if "copilot" in title.lower():
        return "这条内容和开发效率工具演进有关，适合关注工程工具链变化时优先看。"
    if "agent" in title.lower():
        return "这条内容和 AI Agent 能力演进有关，适合继续跟踪这一方向。"
    if tags:
        return f"这条内容和 {tags[0]} 方向相关，适合放进今天的重点观察列表。"
    return "这条内容与当前关注方向接近，适合作为今日候选。"
