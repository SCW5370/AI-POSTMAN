import os
from dotenv import load_dotenv

# 加载.env文件
load_dotenv()

from typing import List, Dict
from fastapi import FastAPI
from pydantic import BaseModel

from app.fetchers.rss_fetcher import fetch_many_sources
from app.parsers.item_normalizer import normalize_items
from app.dedup.grouping import apply_dedup_grouping
from app.enrichers.editorial_decider import decide_sections
from app.enrichers.digest_finalizer import finalize_digest
from app.enrichers.summarizer import enrich_items
from app.enrichers.feedback_analyzer import FeedbackAnalyzer
from app.source_discoverer import SourceDiscoverer
from app.agents.source_discovery_agent import SourceDiscoveryAgent
from app.llm.source_evaluator import SourceEvaluator
from app.profile.profile_analyzer import ProfileAnalyzer
from app.llm.schemas import (
    EditorialDecisionRequest,
    EditorialDecisionResponse,
    FinalizeDigestRequest,
    FinalizeDigestResponse,
    FetchRequest,
    FetchResponse,
)

app = FastAPI(title="AI Postman Worker")
source_discoverer = SourceDiscoverer()
source_agent = SourceDiscoveryAgent()
source_evaluator = SourceEvaluator()
profile_analyzer = ProfileAnalyzer()
feedback_analyzer = FeedbackAnalyzer()

class DiscoverSourcesRequest(BaseModel):
    topic: str
    batch_size: int = 10

class DiscoveredSource(BaseModel):
    url: str
    source_type: str
    confidence: float
    discovery_method: str

class DiscoverSourcesResponse(BaseModel):
    sources: List[DiscoveredSource]

class DiscoverAgentRequest(BaseModel):
    topic: str
    batch_size: int = 10

class EvaluateCandidateRequest(BaseModel):
    url: str
    topic: str
    source_type: str
    confidence: float

class EvaluateCandidateResponse(BaseModel):
    approve: bool
    reason: str
    score: float

class AnalyzeChatRequest(BaseModel):
    chatHistory: str

class AnalyzeSurveyRequest(BaseModel):
    surveyData: Dict

class AnalyzeFeedbackRequest(BaseModel):
    feedbackData: Dict

class IntegrateFeedbackRequest(BaseModel):
    userProfile: Dict
    feedbackAnalysis: Dict


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}


@app.post("/api/fetch", response_model=FetchResponse)
def fetch(request: FetchRequest) -> FetchResponse:
    raw_items = fetch_many_sources([source.model_dump(by_alias=True) for source in request.sources])
    normalized = normalize_items(raw_items)
    deduped = apply_dedup_grouping(normalized)
    enriched = enrich_items(deduped)
    return FetchResponse(items=[
        {
            "sourceId": item["source_id"],
            "externalId": item.get("external_id"),
            "title": item["title"],
            "url": item["url"],
            "author": item.get("author"),
            "publishedAt": item.get("published_at"),
            "summaryRaw": item.get("summary_raw"),
            "contentRaw": item.get("content_raw"),
            "canonicalUrl": item.get("canonical_url"),
            "titleClean": item["title_clean"],
            "summaryClean": item.get("summary_clean"),
            "contentClean": item.get("content_clean"),
            "tags": item.get("tags", []),
            "sourceQualityScore": item.get("source_quality_score", 0),
            "freshnessScore": item.get("freshness_score", 0),
            "dedupGroupKey": item.get("dedup_group_key"),
            "shortSummary": item.get("short_summary"),
            "relevanceReason": item.get("relevance_reason"),
            "actionHint": item.get("action_hint"),
        }
        for item in enriched
    ])


@app.post("/api/editorial/decide", response_model=EditorialDecisionResponse)
def editorial_decide(request: EditorialDecisionRequest) -> EditorialDecisionResponse:
    return decide_sections(request)


@app.post("/api/editorial/finalize", response_model=FinalizeDigestResponse)
def editorial_finalize(request: FinalizeDigestRequest) -> FinalizeDigestResponse:
    return finalize_digest(request)


@app.post("/api/discover/sources")
async def discover_sources(request: DiscoverSourcesRequest):
    sources = await source_discoverer.discover_sources(request.topic, request.batch_size)
    # 转换为 WorkerClient 期望的格式
    return [
        {
            "id": None,
            "name": source["url"],  # 暂时使用 URL 作为名称
            "url": source["url"],
            "sourceType": source["source_type"],
            "priority": 50,  # 默认优先级
            "language": "zh",  # 默认语言
            "confidence": source["confidence"],
            "discoveryMethod": source["discovery_method"]
        }
        for source in sources
    ]


@app.post("/api/discover/agent")
async def discover_with_agent(request: DiscoverAgentRequest):
    """使用 Agent 进行智能源发现并返回结构化候选源。"""
    sources = source_agent.run_sources(request.topic, request.batch_size)
    return [
        {
            "id": None,
            "name": source["url"],
            "url": source["url"],
            "sourceType": source["source_type"],
            "priority": 55,
            "language": "zh",
            "confidence": source["confidence"],
            "discoveryMethod": source["discovery_method"]
        }
        for source in sources
    ]


@app.post("/api/discover/evaluate")
async def evaluate_candidate(request: EvaluateCandidateRequest):
    """使用 LLM 评估候选源"""
    result = source_evaluator.evaluate_candidate(
        request.url,
        request.topic,
        request.source_type,
        request.confidence
    )
    return result


@app.post("/api/profile/analyze-chat")
async def analyze_chat(request: AnalyzeChatRequest):
    """分析聊天内容，提取用户画像"""
    result = profile_analyzer.analyze_chat(request.chatHistory)
    return result


@app.post("/api/profile/analyze-survey")
async def analyze_survey(request: AnalyzeSurveyRequest):
    """分析问卷数据，提取用户画像"""
    result = profile_analyzer.analyze_survey(request.surveyData)
    return result


@app.post("/api/feedback/analyze")
async def analyze_feedback(request: AnalyzeFeedbackRequest):
    """分析用户反馈，提取有价值的信息"""
    result = feedback_analyzer.analyze_feedback(request.feedbackData)
    return result


@app.post("/api/feedback/integrate")
async def integrate_feedback(request: IntegrateFeedbackRequest):
    """将反馈分析结果整合到用户画像中"""
    result = feedback_analyzer.integrate_feedback(request.userProfile, request.feedbackAnalysis)
    return result
