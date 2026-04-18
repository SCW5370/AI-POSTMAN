from app.parsers.item_normalizer import canonicalize_url, normalize_title


def test_normalize_title() -> None:
    assert normalize_title(" Hello   World ") == "Hello World"


def test_canonicalize_url() -> None:
    assert canonicalize_url("https://example.com/path?a=1#b") == "https://example.com/path"
