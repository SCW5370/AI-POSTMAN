from typing import List
from collections import Counter
import re


def extract_tags(title: str, summary: str, limit: int = 5) -> List[str]:
    tokens = re.findall(r"[\u4e00-\u9fffA-Za-z0-9\-\+]{2,}", f"{title} {summary}")
    stopwords = {
        "today", "with", "from", "this", "that", "about", "github", "agent", "article",
        "url", "comments", "show", "ask", "news", "frontpage", "https", "http", "com",
        "www", "for", "into", "general", "availability"
    }
    counts = Counter(
        token for token in tokens
        if token.lower() not in stopwords
        and not token.lower().startswith("utm")
        and len(token) >= 3
    )
    return [word for word, _ in counts.most_common(limit)]
