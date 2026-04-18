def choose_action_hint(item: dict) -> str:
    freshness = item.get("freshness_score", 0)
    if freshness >= 20:
        return "READ_NOW"
    if len(item.get("tags") or []) >= 3:
        return "KEEP_TRACKING"
    return "SAVE_FOR_LATER"
