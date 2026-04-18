from __future__ import annotations
from dotenv import load_dotenv
from typing import Optional, Dict, Any, List

from app.llm.llm_client import LlmClient
from app.llm.schemas import FinalizeDigestRequest, FinalizeDigestResponse


def finalize_digest(request: FinalizeDigestRequest) -> FinalizeDigestResponse:
    # 每次请求刷新 .env，确保使用用户在前端最新保存的 LLM 配置
    load_dotenv(override=True)
    llm_client = LlmClient()

    if not llm_client.is_enabled():
        raise Exception("LLM finalization failed: llm_not_configured")
    try:
        result = llm_client.finalize_digest_items(request.model_dump())
        if result is not None and isinstance(result, dict) and "items" in result:
            sanitized_items = _sanitize_finalize_items(request, result.get("items", []))
            print("[worker] digest finalize strategy=llm used=true")
            return FinalizeDigestResponse(
                items=sanitized_items,
                llmUsed=True,
                strategy="llm",
                fallbackReason=None,
            )
        reason = llm_client.get_last_error() or "llm_empty_or_parse_failed"
        raise Exception(f"LLM finalization failed: {reason}")
    except Exception as ex:
        print(f"[worker] digest finalize exception: {ex}")
        raise


def heuristic_finalize(request: FinalizeDigestRequest, fallback_reason: Optional[str] = None) -> FinalizeDigestResponse:
    goal = (request.preference.goals or "").strip()
    goal_hint = goal[:20] + ("..." if len(goal) > 20 else "")
    items = []
    # Python 3.8 不支持 f-string 内嵌引号，提前赋值
    _goal = goal_hint or "当前主线"
    for item in request.items:
        summary = item.summary or item.content or item.title
        summary = summary.strip()
        if len(summary) > 160:
            summary = summary[:160] + "..."
        if item.section == "MUST_READ":
            reason = f"这条和你当前目标{_goal}最直接相关，建议优先读。"
            action = "read_now"
        elif item.section == "WORTH_SAVING":
            reason = f"这条更适合先收藏，作为{_goal}的后续资料。"
            action = "save_for_later"
        elif item.section == "SURPRISE":
            reason = "这条是邻近扩展信息，帮助你保持视野广度。"
            action = "keep_tracking"
        else:
            reason = f"这条可作为{_goal}的持续跟进。"
            action = "keep_tracking"

        items.append(
            {
                "normalizedItemId": item.normalizedItemId,
                "shortSummary": summary,
                "relevanceReason": reason,
                "actionHint": action,
            }
        )
    return FinalizeDigestResponse(
        items=items,
        llmUsed=False,
        strategy="heuristic",
        fallbackReason=fallback_reason,
    )


def _sanitize_finalize_items(request: FinalizeDigestRequest, raw_items: Any) -> List[Dict[str, Any]]:
    if not isinstance(raw_items, list):
        raw_items = []

    by_id: Dict[int, Dict[str, Any]] = {}
    for raw in raw_items:
        if not isinstance(raw, dict):
            continue
        try:
            item_id = int(raw.get("normalizedItemId"))
        except Exception:
            continue
        by_id[item_id] = raw

    normalized: List[Dict[str, Any]] = []
    for item in request.items:
        raw = by_id.get(item.normalizedItemId, {})
        summary = str(raw.get("shortSummary") or item.summary or item.content or item.title).strip()
        if len(summary) > 180:
            summary = summary[:180] + "..."

        reason = str(raw.get("relevanceReason") or "").strip()
        if not reason:
            reason = _default_reason(item.section)
        if len(reason) > 90:
            reason = reason[:90] + "..."

        action = str(raw.get("actionHint") or "").strip().lower()
        if action not in {"read_now", "save_for_later", "keep_tracking"}:
            action = _default_action(item.section)

        normalized.append({
            "normalizedItemId": item.normalizedItemId,
            "shortSummary": summary or item.title,
            "relevanceReason": reason,
            "actionHint": action,
        })
    return normalized


def _default_action(section: str) -> str:
    if section == "MUST_READ":
        return "read_now"
    if section == "WORTH_SAVING":
        return "save_for_later"
    return "keep_tracking"


def _default_reason(section: str) -> str:
    if section == "MUST_READ":
        return "这条信息时效性强、关联度高，建议优先阅读。"
    if section == "WORTH_SAVING":
        return "这条信息信息密度较高，建议收藏后再深入阅读。"
    if section == "SURPRISE":
        return "这条信息属于邻近扩展，有助于拓展视野。"
    return "这条信息与当前关注方向相关，建议持续跟进。"
