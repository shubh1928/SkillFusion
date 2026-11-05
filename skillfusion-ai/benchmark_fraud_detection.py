from pathlib import Path

import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
)

from fraud.feature_schema import FEATURE_COLUMNS


BASE_DIR = Path(__file__).resolve().parent
DATA_FILE = BASE_DIR / "fraud" / "data" / "synthetic_users.csv"
OUTPUT_DIR = BASE_DIR / "benchmark_output" / "fraud_detection"


def calculate_metrics(method, actual, predicted):
    tn, fp, fn, tp = confusion_matrix(
        actual,
        predicted,
        labels=[False, True],
    ).ravel()

    false_positive_rate = (
        fp / (fp + tn) * 100 if (fp + tn) else 0.0
    )

    return {
        "Method": method,
        "Test Profiles": len(actual),
        "TN": int(tn),
        "FP": int(fp),
        "FN": int(fn),
        "TP": int(tp),
        "Accuracy (%)": round(
            accuracy_score(actual, predicted) * 100, 2
        ),
        "Precision (%)": round(
            precision_score(
                actual, predicted, zero_division=0
            ) * 100, 2
        ),
        "Recall (%)": round(
            recall_score(
                actual, predicted, zero_division=0
            ) * 100, 2
        ),
        "F1-score (%)": round(
            f1_score(
                actual, predicted, zero_division=0
            ) * 100, 2
        ),
        "False Positive Rate (%)": round(
            false_positive_rate, 2
        ),
    }


def main():
    if not DATA_FILE.exists():
        raise FileNotFoundError(
            f"Synthetic dataset not found: {DATA_FILE}"
        )

    dataset = pd.read_csv(DATA_FILE)

    if "fraud" not in dataset.columns:
        raise ValueError("The dataset has no 'fraud' label.")

    missing = [
        col for col in FEATURE_COLUMNS
        if col not in dataset.columns
    ]

    if missing:
        raise ValueError(f"Missing feature columns: {missing}")

    X = dataset[FEATURE_COLUMNS]
    y = dataset["fraud"].astype(bool)

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y,
    )

    print("Dataset: synthetic behavioural profiles")
    print(f"Total profiles: {len(dataset)}")
    print(f"Training profiles: {len(X_train)}")
    print(f"Held-out test profiles: {len(X_test)}")
    print(f"Fraud-labelled test profiles: {int(y_test.sum())}")
    print(f"Normal test profiles: {int((~y_test).sum())}")

    # Proposed method: Skill Fusion Isolation Forest
    model = IsolationForest(
        n_estimators=200,
        contamination=0.05,
        max_samples="auto",
        bootstrap=False,
        random_state=42,
        n_jobs=-1,
    )

    model.fit(X_train)

    model_predictions = pd.Series(
        model.predict(X_test) == -1,
        index=y_test.index,
    )

    # Baseline: always classify profiles as non-fraudulent
    baseline_predictions = pd.Series(
        False,
        index=y_test.index,
    )

    results = [
        calculate_metrics(
            "Majority-class baseline",
            y_test,
            baseline_predictions,
        ),
        calculate_metrics(
            "Skill Fusion Isolation Forest",
            y_test,
            model_predictions,
        ),
    ]

    results_df = pd.DataFrame(results)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    results_df.to_csv(
        OUTPUT_DIR / "fraud_comparison_metrics.csv",
        index=False,
    )

    pd.DataFrame({
        "actual_fraud": y_test,
        "baseline_prediction": baseline_predictions,
        "isolation_forest_prediction": model_predictions,
    }).to_csv(
        OUTPUT_DIR / "held_out_predictions.csv",
        index=False,
    )

    print("\nHELD-OUT TEST COMPARISON")
    print(results_df.to_string(index=False))
    print(f"\nResults saved to: {OUTPUT_DIR}")


if __name__ == "__main__":
    main()