from __future__ import annotations

from fraud.feature_schema import FEATURE_COLUMNS
from fraud.schemas import FraudPredictionRequest
from fraud.service import predict_fraud

from fraud.feature_schema import FEATURE_COLUMNS
from fraud.schemas import FraudPredictionRequest
from fraud.predictor import predict
from fraud.service import predict_fraud


def create_sample_request(
    *,
    user_id: int = 1,
    default_feature_value: float = 0.5,
) -> FraudPredictionRequest:
    """
    Creates a valid FraudPredictionRequest for manual testing.
    """

    features = {
        column: default_feature_value
        for column in FEATURE_COLUMNS
    }

    return FraudPredictionRequest(
        user_id=user_id,
        features=features,
    )
    

def main() -> None:
    """
    Executes a manual integration test of the fraud prediction service.
    """

    request = create_sample_request()

    print("\n=== Request ===")
    print(request.model_dump_json(indent=4))

    prediction = predict(request)

    print("\n=== Prediction Result ===")
    print(prediction.model_dump_json(indent=4))

    response = predict_fraud(request)

    print("\n=== Fraud Prediction Response ===")
    print(response.model_dump_json(indent=4))
    
    
if __name__ == "__main__":
    main()   
