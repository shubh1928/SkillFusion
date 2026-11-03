from sentence_transformers import util

from recommendation.ai_engine import create_embedding

from recommendation.schemas import (
    RecommendationRequest,
    RecommendationResponse,
    RecommendedRequest
)

MIN_SIMILARITY = 0.25      # Ignore weak matches
MAX_RECOMMENDATIONS = 5    # Return at most 5 requests

def build_user_profile(
    request: RecommendationRequest
) -> str:

    return " ".join(request.skills)


def build_request_profile(request_item) -> str:

    text = []

    if request_item.title:
        text.append(request_item.title)

    if request_item.description:
        text.append(request_item.description)

    return " ".join(text)


def recommend_requests(
    request: RecommendationRequest
) -> RecommendationResponse:

    user_profile = build_user_profile(request)

    user_embedding = create_embedding(
        user_profile
    )

    recommendations = []

    for marketplace_request in request.requests:

        request_profile = build_request_profile(
            marketplace_request
        )

        request_embedding = create_embedding(
            request_profile
        )

        similarity = util.cos_sim(
            user_embedding,
            request_embedding
        ).item()

        if similarity >= MIN_SIMILARITY:

            recommendations.append(

                RecommendedRequest(
                    requestId=marketplace_request.requestId,
                    similarityScore=round(similarity, 4)
                )

            )

    recommendations.sort(

        key=lambda item: item.similarityScore,

        reverse=True

    )

    return RecommendationResponse(

    recommendations=recommendations[:MAX_RECOMMENDATIONS]

    )