from typing import List, Dict, Set

def dedup_by_url(items: List[Dict]) -> List[Dict]:
    seen: Set[str] = set()
    output: List[Dict] = []
    for item in items:
        url = item.get("canonical_url") or item.get("url")
        if url in seen:
            continue
        seen.add(url)
        output.append(item)
    return output
