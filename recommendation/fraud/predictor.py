from __future__ import annotations

from pathlib import Path

import joblib
import pandas as pd

from pydantic import BaseModel
from pydantic import ConfigDict
from pydantic import Field

from sklearn.ensemble import IsolationForest

from fraud.feature_schema import FEATURE_COLUMNS
from fraud.schemas import (
    FraudPredictionRequest,
    FraudPredictionResponse,
    FraudReason,
    FraudRiskLevel,
)
from fraud.trainer import MODEL_FILE

class PredictionResult(BaseModel):
    """
    Represents the raw output produced by the
    Isolation Forest model.

    This is an internal model used only inside
    predictor.py.
    """

    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
    )

    is_fraud: bool = Field(
        ...,
        description="Whether the model classified the user as anomalous.",
    )

    anomaly_score: float = Field(
        ...,
        description="Raw anomaly score produced by the Isolation Forest model.",
    )

MODEL: IsolationForest | None = None

def load_model(
    model_file: Path = MODEL_FILE,
) -> IsolationForest:
    """
    Loads the trained Isolation Forest model.

    The model is loaded only once and reused for
    all future prediction requests.
    """

    global MODEL

    if MODEL is not None:
        return MODEL

    if not model_file.exists():
        raise FileNotFoundError(
            f"Model not found: {model_file}"
        )

    MODEL = joblib.load(model_file)

    return MODEL



def prepare_prediction_input(
    request: FraudPredictionRequest,
) -> pd.DataFrame:
    """
    Converts a prediction request into the feature matrix expected
    by the trained model.
    """

    feature_values = [
        request.features[feature]
        for feature in FEATURE_COLUMNS
    ]

    return pd.DataFrame(
        [feature_values],
        columns=FEATURE_COLUMNS,
    )
    

def predict_model(
    model: IsolationForest,
    features: pd.DataFrame,
) -> PredictionResult:
    """
    Executes the Isolation Forest model and returns
    the raw prediction result.

    This function performs no business logic.
    It only converts the model output into a
    PredictionResult object.
    """

    prediction = model.predict(features)[0]

    anomaly_score = model.decision_function(features)[0]

    return PredictionResult(
        is_fraud=(prediction == -1),
        anomaly_score=float(anomaly_score),
    )
    
    
  
    
def predict(
    request: FraudPredictionRequest,
) -> PredictionResult:
    """
    Generates a raw machine learning prediction
    for a fraud detection request.

    This function orchestrates the prediction
    pipeline by:

    1. Loading the trained model.
    2. Preparing the feature matrix.
    3. Executing the prediction.

    No business rules are applied here.
    """

    model = load_model()

    features = prepare_prediction_input(request)

    return predict_model(
        model=model,
        features=features,
    )