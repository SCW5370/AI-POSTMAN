from typing import Optional
from bs4 import BeautifulSoup, MarkupResemblesLocatorWarning
import warnings


def clean_html(value: Optional[str]) -> str:
    if not value:
        return ""
    if "<" not in value and ">" not in value:
        return " ".join(value.split())
    with warnings.catch_warnings():
        warnings.simplefilter("ignore", MarkupResemblesLocatorWarning)
        return " ".join(BeautifulSoup(value, "html.parser").stripped_strings)
