"""
Synthetic Dataset Generator
===========================

This module generates synthetic behavioural datasets for
training and evaluating the BarterLink Fraud Detection model.

Each generated record represents one user's behavioural
feature vector using the standardized feature definitions
declared in feature_schema.py.

Responsibilities
----------------
- Generate realistic user behaviour
- Simulate normal and suspicious users
- Produce training datasets
- Export datasets as CSV files

This module does NOT train the ML model.
Training is handled separately by train.py.
"""


from __future__ import annotations

import random
from pydantic import BaseModel
from pydantic import ConfigDict
from pydantic import Field
from enum import Enum
from pathlib import Path

import pandas as pd

from fraud.feature_schema import FEATURE_COLUMNS


# ==========================================================
# Dataset Configuration
# ==========================================================

# Ensures reproducible dataset generation.
RANDOM_SEED = 42

# Default number of users to generate.
DEFAULT_DATASET_SIZE = 10_000

# Percentage of suspicious users.
FRAUD_RATIO = 0.05

# Directory used to store generated datasets.
DATA_DIRECTORY = Path(__file__).parent / "data"

# Output CSV filename.
OUTPUT_FILE = DATA_DIRECTORY / "synthetic_users.csv"




INTEGER_FEATURES: frozenset[str] = frozenset(
    {
        # Trust
        "account_maturity",
        "service_experience",

        # Activity
        "request_count",
        "negotiation_count",

        # Collaboration
        "collaboration_count",
        "completed_collaboration_count",
        "session_count",

        # Financial
        "total_transactions",

        # Reputation
        "rating_count",
    }
)



# ==========================================================
# User Behaviour Profiles
# ==========================================================

class UserProfile(str, Enum):
    """
    Supported behavioural profiles used for synthetic
    dataset generation.
    """

    ESTABLISHED = "ESTABLISHED"
    CASUAL = "CASUAL"
    NEW = "NEW"
    SUSPICIOUS = "SUSPICIOUS"



class FeatureRange(BaseModel):
    """
    Represents the minimum and maximum values
    allowed for a behavioural feature.
    """

    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
    )

    minimum: float = Field(
        ...,
        description="Minimum value for the feature."
    )

    maximum: float = Field(
        ...,
        description="Maximum value for the feature."
    )



class BehaviourProfile(BaseModel):
    """
    Configuration describing a synthetic user profile.
    """

    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
    )

    profile: UserProfile = Field(
        ...,
        description="Behaviour profile identifier."
    )

    fraud: bool = Field(
        ...,
        description="Whether this profile represents fraudulent behaviour."
    )

    weight: float = Field(
        ...,
        gt=0.0,
        le=1.0,
        description="Relative probability of selecting this profile."
    )
    

    feature_ranges: dict[str, FeatureRange] = Field(
        ...,
        description="Feature generation ranges for this profile."
    )
    

    
# ==========================================================
# Profile Registry
# ==========================================================

PROFILE_REGISTRY: dict[UserProfile, BehaviourProfile] = {}


def create_profile(
    profile: UserProfile,
    fraud: bool,
    weight: float,
    feature_ranges: dict[str, tuple[float, float]],
) -> BehaviourProfile:
    """
    Creates a BehaviourProfile from simple (minimum, maximum) tuples.
    """

    return BehaviourProfile(
        profile=profile,
        fraud=fraud,
        weight=weight,
        feature_ranges={
            feature: FeatureRange(
                minimum=minimum,
                maximum=maximum,
            )
            for feature, (minimum, maximum) in feature_ranges.items()
        },
    )


    
ESTABLISHED_RANGES = {
    # =========================
    # TRUST
    # =========================
    "account_maturity": (500, 900),
    "profile_completeness": (0.90, 1.00),
    "service_experience": (80, 500),
    "reputation_index": (0.85, 1.00),
    "skill_reliability": (0.90, 1.00),

    # =========================
    # ACTIVITY
    # =========================
    "request_count": (80, 300),
    "recent_request_rate": (0.70, 1.00),
    "negotiation_count": (60, 250),
    "negotiation_conversion_rate": (0.80, 1.00),
    "negotiation_activity_rate": (0.70, 1.00),

    # =========================
    # COLLABORATION
    # =========================
    "collaboration_count": (50, 220),
    "completed_collaboration_count": (45, 210),
    "collaboration_completion_rate": (0.90, 1.00),
    "session_count": (60, 250),
    "completed_session_rate": (0.90, 1.00),

    # =========================
    # FINANCIAL
    # =========================
    "total_transactions": (50, 250),
    "refund_ratio": (0.00, 0.05),
    "escrow_lock_ratio": (0.85, 1.00),
    "token_flow_balance": (0.80, 1.20),
    "average_transaction_size": (20.0, 100.0),

    # =========================
    # REPUTATION
    # =========================
    "rating_count": (40, 200),
    "average_rating": (4.50, 5.00),
    "low_rating_ratio": (0.00, 0.05),
    "high_rating_ratio": (0.80, 1.00),
    "rating_consistency": (0.85, 1.00),

    # =========================
    # CONSISTENCY
    # =========================
    "activity_trust_alignment": (0.85, 1.00),
    "collaboration_reputation_alignment": (0.90, 1.00),
    "financial_activity_alignment": (0.85, 1.00),
    "profile_behavior_alignment": (0.90, 1.00),
    "overall_consistency_score": (0.90, 1.00),
}


CASUAL_RANGES = {
    # =========================
    # TRUST
    # =========================
    "account_maturity": (90, 500),
    "profile_completeness": (0.70, 0.95),
    "service_experience": (20, 120),
    "reputation_index": (0.65, 0.90),
    "skill_reliability": (0.70, 0.95),

    # =========================
    # ACTIVITY
    # =========================
    "request_count": (20, 120),
    "recent_request_rate": (0.40, 0.80),
    "negotiation_count": (15, 100),
    "negotiation_conversion_rate": (0.60, 0.85),
    "negotiation_activity_rate": (0.40, 0.80),

    # =========================
    # COLLABORATION
    # =========================
    "collaboration_count": (10, 90),
    "completed_collaboration_count": (8, 80),
    "collaboration_completion_rate": (0.70, 0.90),
    "session_count": (15, 100),
    "completed_session_rate": (0.75, 0.95),

    # =========================
    # FINANCIAL
    # =========================
    "total_transactions": (10, 90),
    "refund_ratio": (0.02, 0.15),
    "escrow_lock_ratio": (0.60, 0.90),
    "token_flow_balance": (0.70, 1.30),
    "average_transaction_size": (10.0, 70.0),

    # =========================
    # REPUTATION
    # =========================
    "rating_count": (10, 80),
    "average_rating": (3.80, 4.70),
    "low_rating_ratio": (0.02, 0.15),
    "high_rating_ratio": (0.50, 0.80),
    "rating_consistency": (0.70, 0.95),

    # =========================
    # CONSISTENCY
    # =========================
    "activity_trust_alignment": (0.65, 0.90),
    "collaboration_reputation_alignment": (0.70, 0.90),
    "financial_activity_alignment": (0.65, 0.90),
    "profile_behavior_alignment": (0.70, 0.90),
    "overall_consistency_score": (0.70, 0.90),
}


NEW_RANGES = {
    # =========================
    # TRUST
    # =========================
    "account_maturity": (1, 90),
    "profile_completeness": (0.40, 0.80),
    "service_experience": (0, 20),
    "reputation_index": (0.40, 0.75),
    "skill_reliability": (0.40, 0.80),

    # =========================
    # ACTIVITY
    # =========================
    "request_count": (0, 20),
    "recent_request_rate": (0.10, 0.50),
    "negotiation_count": (0, 15),
    "negotiation_conversion_rate": (0.30, 0.70),
    "negotiation_activity_rate": (0.10, 0.50),

    # =========================
    # COLLABORATION
    # =========================
    "collaboration_count": (0, 10),
    "completed_collaboration_count": (0, 8),
    "collaboration_completion_rate": (0.40, 0.80),
    "session_count": (0, 12),
    "completed_session_rate": (0.50, 0.85),

    # =========================
    # FINANCIAL
    # =========================
    "total_transactions": (0, 15),
    "refund_ratio": (0.00, 0.20),
    "escrow_lock_ratio": (0.40, 0.80),
    "token_flow_balance": (0.60, 1.40),
    "average_transaction_size": (5.0, 40.0),

    # =========================
    # REPUTATION
    # =========================
    "rating_count": (0, 10),
    "average_rating": (3.00, 4.50),
    "low_rating_ratio": (0.00, 0.25),
    "high_rating_ratio": (0.30, 0.70),
    "rating_consistency": (0.50, 0.85),

    # =========================
    # CONSISTENCY
    # =========================
    "activity_trust_alignment": (0.40, 0.80),
    "collaboration_reputation_alignment": (0.45, 0.80),
    "financial_activity_alignment": (0.40, 0.80),
    "profile_behavior_alignment": (0.45, 0.80),
    "overall_consistency_score": (0.45, 0.80),
}


SUSPICIOUS_RANGES = {
    # =========================
    # TRUST
    # =========================
    "account_maturity": (1, 30),
    "profile_completeness": (0.10, 0.60),
    "service_experience": (0, 10),
    "reputation_index": (0.10, 0.50),
    "skill_reliability": (0.10, 0.50),

    # =========================
    # ACTIVITY
    # =========================
    "request_count": (150, 600),
    "recent_request_rate": (0.90, 1.00),
    "negotiation_count": (100, 400),
    "negotiation_conversion_rate": (0.00, 0.30),
    "negotiation_activity_rate": (0.90, 1.00),

    # =========================
    # COLLABORATION
    # =========================
    "collaboration_count": (0, 20),
    "completed_collaboration_count": (0, 5),
    "collaboration_completion_rate": (0.00, 0.30),
    "session_count": (0, 15),
    "completed_session_rate": (0.00, 0.30),

    # =========================
    # FINANCIAL
    # =========================
    "total_transactions": (100, 500),
    "refund_ratio": (0.40, 1.00),
    "escrow_lock_ratio": (0.00, 0.20),
    "token_flow_balance": (2.00, 8.00),
    "average_transaction_size": (100.0, 500.0),

    # =========================
    # REPUTATION
    # =========================
    "rating_count": (0, 20),
    "average_rating": (1.00, 3.00),
    "low_rating_ratio": (0.40, 1.00),
    "high_rating_ratio": (0.00, 0.20),
    "rating_consistency": (0.00, 0.40),

    # =========================
    # CONSISTENCY
    # =========================
    "activity_trust_alignment": (0.00, 0.30),
    "collaboration_reputation_alignment": (0.00, 0.30),
    "financial_activity_alignment": (0.00, 0.30),
    "profile_behavior_alignment": (0.00, 0.30),
    "overall_consistency_score": (0.00, 0.30),
}


PROFILE_REGISTRY[UserProfile.ESTABLISHED] = create_profile(
    profile=UserProfile.ESTABLISHED,
    fraud=False,
    weight=0.45,
    feature_ranges=ESTABLISHED_RANGES,
)




PROFILE_REGISTRY[UserProfile.CASUAL] = create_profile(
    profile=UserProfile.CASUAL,
    fraud=False,
    weight=0.35,
    feature_ranges=CASUAL_RANGES,
)



PROFILE_REGISTRY[UserProfile.NEW] = create_profile(
    profile=UserProfile.NEW,
    fraud=False,
    weight=0.15,
    feature_ranges=NEW_RANGES,
)


PROFILE_REGISTRY[UserProfile.SUSPICIOUS] = create_profile(
    profile=UserProfile.SUSPICIOUS,
    fraud=True,
    weight=0.05,
    feature_ranges=SUSPICIOUS_RANGES,
)


def validate_profile_registry() -> None:
    """
    Validates every registered behaviour profile.

    Ensures that:
    - every profile contains all expected features,
    - no unknown features are present,
    - every feature range is valid.
    """

    expected_features = set(FEATURE_COLUMNS)

    for profile in PROFILE_REGISTRY.values():
        actual_features = set(profile.feature_ranges)

        missing = expected_features - actual_features
        if missing:
            raise ValueError(
                f"{profile.profile.value} profile is missing features: "
                f"{sorted(missing)}"
            )

        unexpected = actual_features - expected_features
        if unexpected:
            raise ValueError(
                f"{profile.profile.value} profile has unknown features: "
                f"{sorted(unexpected)}"
            )

        for feature_name, feature_range in profile.feature_ranges.items():
            if feature_range.minimum > feature_range.maximum:
                raise ValueError(
                    f"{profile.profile.value}.{feature_name}: "
                    f"minimum ({feature_range.minimum}) "
                    f"is greater than maximum ({feature_range.maximum})"
                )
                
                

def generate_feature_value(
    feature_name: str,
    feature_range: FeatureRange,
) -> float | int:
    """
    Generates a random value for a feature within its configured range.

    Integer features are generated using random.randint(),
    while continuous features use random.uniform().
    """

    if feature_name in INTEGER_FEATURES:
        return random.randint(
            int(feature_range.minimum),
            int(feature_range.maximum),
        )

    return random.uniform(
        feature_range.minimum,
        feature_range.maximum,
    )
    
    
def apply_collaboration_rules(
    user: dict[str, float | int],
    profile: BehaviourProfile,
) -> None:
    """
    Ensures collaboration-related features remain logically consistent.
    """

    total = int(user["collaboration_count"])

    completion_range = profile.feature_ranges[
        "collaboration_completion_rate"
    ]

    completion_rate = random.uniform(
        completion_range.minimum,
        completion_range.maximum,
    )

    completed = round(total * completion_rate)

    completed = min(completed, total)

    user["completed_collaboration_count"] = completed

    user["collaboration_completion_rate"] = (
        completed / total
        if total > 0
        else 0.0
    )
    
    
def apply_session_rules(
    user: dict[str, float | int],
    profile: BehaviourProfile,
) -> None:
    """
    Ensures session-related features remain logically consistent.
    """

    total = int(user["session_count"])

    completion_range = profile.feature_ranges[
        "completed_session_rate"
    ]

    completion_rate = random.uniform(
        completion_range.minimum,
        completion_range.maximum,
    )

    completed = round(total * completion_rate)

    completed = min(completed, total)

    user["completed_session_rate"] = (
        completed / total
        if total > 0
        else 0.0
    )
    
    
def apply_rating_rules(
    user: dict[str, float | int],
    profile: BehaviourProfile,
) -> None:
    """
    Ensures rating-related features remain logically consistent.
    """

    rating = float(user["average_rating"])

    low_range = profile.feature_ranges["low_rating_ratio"]
    high_range = profile.feature_ranges["high_rating_ratio"]

    # Determine where the average rating lies between 1.0 and 5.0
    rating_factor = (rating - 1.0) / 4.0
    rating_factor = max(0.0, min(1.0, rating_factor))

    # Better average rating → lower low-rating ratio
    low_ratio = (
        low_range.maximum
        - rating_factor * (low_range.maximum - low_range.minimum)
    )

    # Better average rating → higher high-rating ratio
    high_ratio = (
        high_range.minimum
        + rating_factor * (high_range.maximum - high_range.minimum)
    )

    # Add a little randomness so every user isn't identical
    low_ratio += random.uniform(-0.03, 0.03)
    high_ratio += random.uniform(-0.03, 0.03)

    # Clamp values back into the profile's configured ranges
    low_ratio = max(
        low_range.minimum,
        min(low_range.maximum, low_ratio),
    )

    high_ratio = max(
        high_range.minimum,
        min(high_range.maximum, high_ratio),
    )

    user["low_rating_ratio"] = low_ratio
    user["high_rating_ratio"] = high_ratio

    consistency = max(
        0.0,
        min(
            1.0,
            high_ratio - low_ratio + random.uniform(-0.05, 0.05),
        ),
    )

    user["rating_consistency"] = consistency
   
   

def apply_consistency_rules(
    user: dict[str, float | int],
    profile: BehaviourProfile,
) -> None:
    """
    Generates consistency-related features using the profile's
    configured ranges while adjusting them based on behaviour.
    """

    def clamp(value: float) -> float:
        return max(0.0, min(1.0, value))

    trust = (
        float(user["profile_completeness"])
        + float(user["reputation_index"])
        + float(user["skill_reliability"])
    ) / 3.0

    activity = (
        float(user["recent_request_rate"])
        + float(user["negotiation_activity_rate"])
    ) / 2.0

    collaboration = (
        float(user["collaboration_completion_rate"])
        + float(user["completed_session_rate"])
    ) / 2.0

    financial = (
        (1.0 - float(user["refund_ratio"]))
        + float(user["escrow_lock_ratio"])
    ) / 2.0

    reputation = (
        float(user["average_rating"]) / 5.0
        + float(user["rating_consistency"])
    ) / 2.0

    feature_dependencies = {
        "activity_trust_alignment": (activity + trust) / 2.0,
        "collaboration_reputation_alignment": (
            collaboration + reputation
        ) / 2.0,
        "financial_activity_alignment": (
            financial + activity
        ) / 2.0,
        "profile_behavior_alignment": (
            float(user["profile_completeness"])
            + collaboration
        ) / 2.0,
    }

    scores = {}

    for feature_name, behaviour_score in feature_dependencies.items():

        feature_range = profile.feature_ranges[feature_name]

        value = random.uniform(
            feature_range.minimum,
            feature_range.maximum,
        )

        value += (behaviour_score - 0.5) * 0.10

        value += random.uniform(-0.02, 0.02)

        scores[feature_name] = clamp(value)

    user.update(scores)

    overall = sum(scores.values()) / len(scores)

    overall += random.uniform(-0.02, 0.02)

    user["overall_consistency_score"] = clamp(overall)
    
    
    
def generate_user(
    profile: BehaviourProfile,
) -> dict[str, float | int | bool | str]:
    """
    Generates a single synthetic user from a behaviour profile.
    """

    user: dict[str, float | int | bool | str] = {}

    for feature_name, feature_range in profile.feature_ranges.items():
        user[feature_name] = generate_feature_value(
            feature_name,
            feature_range,
        )
        
    apply_collaboration_rules(
        user,
        profile,
    )
    
    apply_session_rules(
        user,
        profile,
    )
    
    apply_rating_rules(
        user,
        profile,
    )
    
    apply_consistency_rules(
        user,
        profile,
    )

    user["fraud"] = profile.fraud
    user["profile"] = profile.profile.value

    return user


def choose_profile() -> BehaviourProfile:
    """
    Randomly selects a behaviour profile using the configured weights.
    """

    return random.choices(
        population=list(PROFILE_REGISTRY.values()),
        weights=[
            profile.weight
            for profile in PROFILE_REGISTRY.values()
        ],
        k=1,
    )[0]
    

def generate_dataset(
    size: int = DEFAULT_DATASET_SIZE,
) -> pd.DataFrame:
    """
    Generates a synthetic dataset of users.
    """
    
    if size <= 0:
        raise ValueError("Dataset size must be greater than zero.")


    users = [
        generate_user(
            choose_profile(),
        )
        for _ in range(size)
    ]

    return pd.DataFrame(users)



def save_dataset(
    dataset: pd.DataFrame,
    output_file: Path = OUTPUT_FILE,
) -> None:
    """
    Saves the generated dataset to a CSV file.
    """

    output_file.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    dataset.to_csv(
        output_file,
        index=False,
    )
    
    
def main() -> None:
    """
    Generates and saves the synthetic fraud detection dataset.
    """

    random.seed(RANDOM_SEED)

    validate_profile_registry()

    dataset = generate_dataset()

    save_dataset(dataset)

    print(f"Generated {len(dataset)} synthetic users.")
    print(f"Dataset saved to: {OUTPUT_FILE}")
    
    
if __name__ == "__main__":
    main()
