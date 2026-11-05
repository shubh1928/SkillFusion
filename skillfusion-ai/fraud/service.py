from __future__ import annotations

from fraud.predictor import (
    PredictionResult,
    predict,
)

from fraud.schemas import (
    FraudPredictionRequest,
    FraudPredictionResponse,
    FraudReason,
    FraudRiskLevel,
)

from fraud.explanation_rules import (
    generate_reasons,
)

   

def normalize_risk_score(
    anomaly_score: float,
) -> float:
    """
    Converts the Isolation Forest anomaly score into a
    normalized fraud risk score between 0.0 and 1.0.

    Higher values indicate greater fraud risk.
    """

    MIN_SCORE = -0.30
    MAX_SCORE = 0.25

    risk_score = (
        MAX_SCORE - anomaly_score
    ) / (MAX_SCORE - MIN_SCORE)

    risk_score = max(
        0.0,
        min(1.0, risk_score),
    )

    return round(risk_score, 3)
  

def determine_risk_level(
    risk_score: float,
) -> FraudRiskLevel:
    """
    Determines the fraud risk level from the
    normalized risk score.

    Business thresholds:

    0.00 - 0.24  -> LOW
    0.25 - 0.74  -> MEDIUM
    0.75 - 1.00  -> HIGH
    """

    if risk_score >= 0.75:
        return FraudRiskLevel.HIGH

    if risk_score >= 0.25:
        return FraudRiskLevel.MEDIUM

    return FraudRiskLevel.LOW


def predict_fraud(
    request: FraudPredictionRequest,
) -> FraudPredictionResponse:
    """
    Executes the complete fraud prediction workflow.

    Workflow:

    1. Run the machine learning prediction.
    2. Calculate the risk score.
    3. Determine the risk level.
    4. Generate explanation reasons.
    5. Build the API response.
    """

    prediction = predict(request)

    risk_score = normalize_risk_score(
        prediction.anomaly_score
    )

    risk_level = determine_risk_level(risk_score)

    reasons = generate_reasons(
        request.features,
    )

    return FraudPredictionResponse(
        user_id=request.user_id,
        risk_score=risk_score,
        risk_level=risk_level,
        is_fraud=prediction.is_fraud,
        reasons=reasons,
    )
 
