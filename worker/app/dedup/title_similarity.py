from difflib import SequenceMatcher


def similar(a: str, b: str, threshold: float = 0.92) -> bool:
    return SequenceMatcher(None, a.lower(), b.lower()).ratio() >= threshold
