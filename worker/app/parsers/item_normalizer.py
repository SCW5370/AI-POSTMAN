from __future__ import annotations

import hashlib
import re
from typing import List, Dict, Optional
from urllib.parse import urlparse, urlunparse

from app.parsers.html_cleaner import clean_html
from app.parsers.tag_extractor import extract_tags


def normalize_title(title: str) -> str:
    return re.sub(r"\s+", " ", clean_html(title)).strip()


def canonicalize_url(url: str) -> str:
    parsed = urlparse(url)
    return urlunparse((parsed.scheme, parsed.netloc, parsed.path, "", "", ""))


def raw_hash(title: str, url: str) -> str:
    return hashlib.sha256(f"{normalize_title(title)}::{canonicalize_url(url)}".encode()).hexdigest()


def freshness_score(published_at: Optional[str]) -> float:
    return 20.0 if published_at else 5.0


def normalize_items(items: List[Dict]) -> List[Dict]:
    normalized: List[Dict] = []
    for item in items:
        title_clean = normalize_title(item.get("title", ""))
        summary_clean = clean_html(item.get("summary_raw", ""))[:800]
        content_clean = clean_html(item.get("content_raw", ""))[:2000]
        canonical_url = canonicalize_url(item.get("url", ""))
        normalized.append(
            {
                **item,
                "canonical_url": canonical_url,
                "title_clean": title_clean,
                "summary_clean": summary_clean,
                "content_clean": content_clean,
                "tags": extract_tags(title_clean, summary_clean),
                "source_quality_score": float(item.get("source_priority", 50)),
                "freshness_score": freshness_score(item.get("published_at")),
                "dedup_group_key": raw_hash(title_clean, canonical_url),
            }
        )
    return normalized
