from typing import List, Dict
from app.dedup.title_similarity import similar
from app.dedup.url_dedup import dedup_by_url


def apply_dedup_grouping(items: List[Dict]) -> List[Dict]:
    items = dedup_by_url(items)
    output: List[Dict] = []
    for item in items:
        if any(similar(item["title_clean"], existing["title_clean"]) for existing in output):
            continue
        output.append(item)
    return output
