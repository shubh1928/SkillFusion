from __future__ import annotations

from pathlib import Path

import joblib
import pandas as pd

from sklearn.ensemble import IsolationForest

from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
)

from fraud.dataset_generator import OUTPUT_FILE
from fraud.feature_schema import FEATURE_COLUMNS


MODEL_DIRECTORY = Path(__file__).parent / "model"

MODEL_FILE = MODEL_DIRECTORY / "isolation_forest.joblib"

RESULTS_FILE = (
    MODEL_DIRECTORY /
    "evaluation_results.csv"
)

RANDOM_SEED = 42
CONTAMINATION = 0.05

def load_dataset(
    dataset_file: Path = OUTPUT_FILE,
) -> pd.DataFrame:
    """
    Loads the synthetic dataset from disk.

    Raises:
        FileNotFoundError:
            If the dataset file does not exist.
    """

    if not dataset_file.exists():
        raise FileNotFoundError(
            f"Dataset not found: {dataset_file}"
        )

    return pd.read_csv(dataset_file)


def prepare_features(
    dataset: pd.DataFrame,
) -> tuple[pd.DataFrame, pd.Series]:
    """
    Separates the feature matrix from the fraud labels.

    Raises:
        ValueError:
            If any required feature columns are missing.
    """

    missing_features = [
        feature
        for feature in FEATURE_COLUMNS
        if feature not in dataset.columns
    ]

    if missing_features:
        raise ValueError(
            f"Missing feature columns: {missing_features}"
        )

    features = dataset[FEATURE_COLUMNS]

    if features.isnull().any().any():
        raise ValueError(
            "Dataset contains missing feature values."
        )

    labels = dataset["fraud"]

    return features, labels


def train_model(
    features: pd.DataFrame,
) -> IsolationForest:
    """
    Trains an Isolation Forest model using the behavioural features.
    """

    model = IsolationForest(
        n_estimators=200,
        contamination=CONTAMINATION,
        max_samples="auto",
        bootstrap=False,
        random_state=RANDOM_SEED,
        n_jobs=-1,
    )

    model.fit(features)

    return model



def predict_fraud(
    model: IsolationForest,
    features: pd.DataFrame,
) -> pd.Series:
    """
    Predicts whether each user is fraudulent.

    Returns:
        A boolean Series where:
        - True  = Fraud
        - False = Normal
    """

    predictions = model.predict(features)

    return pd.Series(
        predictions == -1,
        index=features.index,
        name="predicted_fraud",
    )
    
    

def calculate_anomaly_scores(
    model: IsolationForest,
    features: pd.DataFrame,
) -> pd.Series:
    """
    Calculates anomaly scores for each user.

    Higher scores indicate more normal behaviour.
    Lower (negative) scores indicate more anomalous behaviour.
    """

    scores = model.decision_function(features)

    return pd.Series(
        scores,
        index=features.index,
        name="anomaly_score",
    )
    
    
def build_results_dataframe(
    labels: pd.Series,
    predictions: pd.Series,
    scores: pd.Series,
) -> pd.DataFrame:
    """
    Builds a DataFrame containing the model's predictions.
    """

    return pd.DataFrame(
        {
            "actual_fraud": labels,
            "predicted_fraud": predictions,
            "anomaly_score": scores,
        }
    )



def evaluate_model(
    labels: pd.Series,
    predictions: pd.Series,
) -> dict[str, object]:
    """
    Evaluates the trained model and returns its performance metrics.
    """

    return {
        "confusion_matrix": confusion_matrix(
            labels,
            predictions,
        ),
        "precision": precision_score(
            labels,
            predictions,
        ),
        "recall": recall_score(
            labels,
            predictions,
        ),
        "f1_score": f1_score(
            labels,
            predictions,
        ),
        "classification_report": classification_report(
            labels,
            predictions,
        ),
    }  
    

def save_model(
    model: IsolationForest,
    model_file: Path = MODEL_FILE,
) -> None:
    """
    Saves the trained Isolation Forest model to disk.
    """

    model_file.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    joblib.dump(
        model,
        model_file,
    )
    

def main() -> None:
    """
    Trains, evaluates, and saves the fraud detection model.
    """

    dataset = load_dataset()

    features, labels = prepare_features(dataset)

    model = train_model(features)

    predictions = predict_fraud(
        model,
        features,
    )

    scores = calculate_anomaly_scores(
        model,
        features,
    )

    metrics = evaluate_model(
        labels,
        predictions,
    )

    results = build_results_dataframe(
        labels,
        predictions,
        scores,
    )

    save_model(model)

    results.to_csv(
        RESULTS_FILE,
        index=False,
    )

    print("\n========== Model Evaluation ==========")
    print(f"Precision : {metrics['precision']:.3f}")
    print(f"Recall    : {metrics['recall']:.3f}")
    print(f"F1 Score  : {metrics['f1_score']:.3f}")

    print("\nConfusion Matrix")
    print(metrics["confusion_matrix"])

    print("\nClassification Report")
    print(metrics["classification_report"])

    print(f"\nModel saved to: {MODEL_FILE}")
    
    print(f"Evaluation results saved to: {RESULTS_FILE}")

    print("\nResults Preview")
    print(results.head())
    


if __name__ == "__main__":
    main()
    

