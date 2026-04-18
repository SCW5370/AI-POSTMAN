from typing import List, Dict
from app.enrichers.action_hint_generator import choose_action_hint
from app.enrichers.relevance_explainer import build_relevance_reason
from app.llm.llm_client import LlmClient
from app.config import LLM_ENRICH_MAX_ITEMS, LLM_ENRICH_ON_FETCH


LLM_CLIENT = LlmClient()


def short_summary(item: Dict) -> str:
    text = item.get("summary_clean") or item.get("content_clean") or item.get("title_clean", "")
    return text[:60].strip()


def enrich_items(items: List[Dict]) -> List[Dict]:
    # Keep fetch path responsive by default. LLM enrichment can be re-enabled via env.
    if not LLM_ENRICH_ON_FETCH:
        return [
            {
                **item,
                "short_summary": short_summary(item),
                "relevance_reason": build_relevance_reason(item),
                "action_hint": choose_action_hint(item),
            }
            for item in items
        ]

    ranked_for_llm = sorted(
        items,
        key=lambda item: (
            item.get("source_quality_score", 0),
            item.get("freshness_score", 0),
        ),
        reverse=True,
    )
    llm_item_keys = {
        (item.get("source_id"), item.get("url"))
        for item in ranked_for_llm[:LLM_ENRICH_MAX_ITEMS]
    }
    enriched: List[Dict] = []
    for index, item in enumerate(items, start=1):
        use_llm = (item.get("source_id"), item.get("url")) in llm_item_keys
        llm_summary = None
        llm_reason = None
        if use_llm:
            _title = item.get("title_clean", "")[:80]
            print(f"[worker] enriching item {index}/{len(items)} with llm: {_title}")
            llm_summary = LLM_CLIENT.summarize(item)
            llm_reason = LLM_CLIENT.relevance_reason(item)
        enriched.append(
            {
                **item,
                "short_summary": llm_summary or short_summary(item),
                "relevance_reason": llm_reason or build_relevance_reason(item),
                "action_hint": choose_action_hint(item),
            }
        )
    return enriched
