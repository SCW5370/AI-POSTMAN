from typing import List, Optional, Dict
from pydantic import BaseModel, Field


class WorkerSource(BaseModel):
    id: int
    name: str
    url: str
    sourceType: str = Field(alias="sourceType")
    priority: int = 50
    language: str = "zh"


class FetchRequest(BaseModel):
    sources: List[WorkerSource]


class WorkerItem(BaseModel):
    sourceId: int
    externalId: Optional[str] = None
    title: str
    url: str
    author: Optional[str] = None
    publishedAt: Optional[str] = None
    summaryRaw: Optional[str] = None
    contentRaw: Optional[str] = None
    canonicalUrl: Optional[str] = None
    titleClean: str
    summaryClean: Optional[str] = None
    contentClean: Optional[str] = None
    tags: List[str] = []
    sourceQualityScore: float = 0
    freshnessScore: float = 0
    dedupGroupKey: Optional[str] = None
    shortSummary: Optional[str] = None
    relevanceReason: Optional[str] = None
    actionHint: Optional[str] = None


class FetchResponse(BaseModel):
    items: List[WorkerItem]


class EditorialPreference(BaseModel):
    goals: Optional[str] = None
    preferredTopics: List[str] = []
    blockedTopics: List[str] = []
    deliveryMode: str = "BALANCED"
    explorationRatio: float = 0.10
    maxItemsPerDigest: int = 5


class EditorialCandidate(BaseModel):
    normalizedItemId: int
    title: str
    summary: Optional[str] = None
    sourceName: str
    tags: List[str] = []
    ruleScore: float


class EditorialDecisionRequest(BaseModel):
    preference: EditorialPreference
    candidates: List[EditorialCandidate]


class EditorialDecisionItem(BaseModel):
    normalizedItemId: int
    section: str
    scoreAdjustment: float = 0
    reason: str


class EditorialDecisionResponse(BaseModel):
    decisions: List[EditorialDecisionItem]
    llmUsed: bool = False
    strategy: str = "heuristic"
    fallbackReason: Optional[str] = None


class FinalizeDigestItem(BaseModel):
    normalizedItemId: int
    title: str
    summary: Optional[str] = None
    content: Optional[str] = None
    sourceName: str
    section: str


class FinalizeDigestRequest(BaseModel):
    preference: EditorialPreference
    items: List[FinalizeDigestItem]
    userProfile: Dict = {}


class FinalizedDigestItem(BaseModel):
    normalizedItemId: int
    shortSummary: str
    relevanceReason: str
    actionHint: str


class FinalizeDigestResponse(BaseModel):
    items: List[FinalizedDigestItem]
    llmUsed: bool = False
    strategy: str = "heuristic"
    fallbackReason: Optional[str] = None
