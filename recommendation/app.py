from fastapi import FastAPI

from recommendation.ai_engine import model
from recommendation.matcher import recommend_requests

from recommendation.schemas import (
    RecommendationRequest,
    RecommendationResponse,
    RecommendedRequest
)

from fraud.api import router as fraud_router

app = FastAPI(
    title="BarterLink AI Recommendation Service",
    version="1.0.0",
    description="AI-powered semantic recommendation engine for BarterLink."
)

app.include_router(fraud_router)

@app.get("/")
def home():

    return {
        "message": "BarterLink AI Service is Running"
    }


@app.post(
    "/recommend",
    response_model=RecommendationResponse
)
def recommend(
    request: RecommendationRequest
):

    return recommend_requests(request)