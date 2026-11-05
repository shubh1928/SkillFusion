from typing import List

from pydantic import BaseModel


class MarketplaceRequest(BaseModel):
    requestId: str
    title: str
    description: str


class RecommendationRequest(BaseModel):
    skills: List[str]
    requests: List[MarketplaceRequest]


class RecommendedRequest(BaseModel):
    requestId: str
    similarityScore: float


class RecommendationResponse(BaseModel):
    recommendations: List[RecommendedRequest]