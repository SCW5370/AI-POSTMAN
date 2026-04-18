from __future__ import annotations

from datetime import datetime, timezone
from concurrent.futures import ThreadPoolExecutor, as_completed
from time import perf_counter
from typing import List, Dict

import feedparser
import requests

from app.fetchers.base_fetcher import BaseFetcher


class RssFetcher(BaseFetcher):
    def fetch(self, source: Dict) -> List[Dict]:
        url = source.get("url")
        source_name = source.get("name") or url or "unknown"
        if not url:
            return []

        started = perf_counter()
        try:
            # Hard timeout per source to prevent one slow feed from blocking the whole batch.
            response = requests.get(
                url,
                timeout=(3, 8),
                headers={"User-Agent": "ai-postman-fetcher/1.0"},
            )
            response.raise_for_status()
            parsed = feedparser.parse(response.content)
        except Exception as ex:
            elapsed = perf_counter() - started
            print(f"[worker][fetch] skip source={source_name} url={url} elapsed={elapsed:.2f}s reason={ex.__class__.__name__}")
            return []

        items: List[Dict] = []
        for entry in parsed.entries:
            published = None
            if getattr(entry, "published_parsed", None):
                published = datetime(*entry.published_parsed[:6], tzinfo=timezone.utc).isoformat()
            items.append(
                {
                    "source_id": source["id"],
                    "external_id": getattr(entry, "id", None),
                    "title": getattr(entry, "title", ""),
                    "url": getattr(entry, "link", ""),
                    "author": getattr(entry, "author", None),
                    "published_at": published,
                    "summary_raw": getattr(entry, "summary", ""),
                    "content_raw": " ".join(part.value for part in getattr(entry, "content", [])) if getattr(entry, "content", None) else getattr(entry, "summary", ""),
                    "source_priority": source.get("priority", 50),
                    "language": source.get("language", "zh"),
                }
            )
        elapsed = perf_counter() - started
        print(f"[worker][fetch] source={source_name} url={url} items={len(items)} elapsed={elapsed:.2f}s")
        return items


def fetch_many_sources(sources: List[Dict]) -> List[Dict]:
    fetcher = RssFetcher()
    items: List[Dict] = []
    if not sources:
        return items

    max_workers = min(6, max(1, len(sources)))
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = [executor.submit(fetcher.fetch, source) for source in sources]
        for future in as_completed(futures):
            try:
                items.extend(future.result())
            except Exception as ex:
                print(f"[worker][fetch] unexpected worker fetch error: {ex.__class__.__name__}")
    return items
