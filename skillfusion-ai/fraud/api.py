from __future__ import annotations

from fastapi import APIRouter

from fraud.schemas import (
    FraudPredictionRequest,
    FraudPredictionResponse,
)

from fraud.service import predict_fraud

router = APIRouter(
    prefix="/fraud",
    tags=["Fraud Detection"],
)


@router.post(
    "/predict",
    response_model=FraudPredictionResponse,
)
def predict(
    request: FraudPredictionRequest,
) -> FraudPredictionResponse:
    """
    Predicts whether a user exhibits fraudulent behaviour.
    """

    return predict_fraud(request)