"""
Fraud Detection Feature Schema
==============================

This module defines the complete feature contract used by the
Fraud Detection Engine.

Every component of the ML pipeline must import feature
definitions from this file instead of hardcoding feature names.

Used By
-------
- dataset_generator.py
- dataset_validator.py
- train.py
- predict.py
- FastAPI endpoints

Author:
BarterLink AI Module
"""

from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, Field, ConfigDict


class FeatureCategory(str, Enum):
    """
    High-level behavioural dimensions used by the
    fraud detection model.
    """

    TRUST = "TRUST"
    ACTIVITY = "ACTIVITY"
    COLLABORATION = "COLLABORATION"
    FINANCIAL = "FINANCIAL"
    REPUTATION = "REPUTATION"
    CONSISTENCY = "CONSISTENCY"


class FeatureSourceType(str, Enum):
    """
    Describes how a feature is produced.
    """

    DIRECT = "DIRECT"
    DERIVED = "DERIVED"
    AGGREGATED = "AGGREGATED"


class FeatureDefinition(BaseModel):
    """
    Defines a single ML feature.

    This class represents metadata only.
    It does NOT calculate the feature.

    Feature values will be generated later by
    BehaviourAnalyticsService in Spring Boot.
    """

    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
    )

    id: str = Field(
        ...,
        description="Unique feature identifier (e.g. B001)"
    )

    name: str = Field(
        ...,
        description="Human-readable feature name."
    )

    column_name: str = Field(
        ...,
        description="Column name used in datasets."
    )

    category: FeatureCategory = Field(
        ...,
        description="Behaviour category."
    )

    source_type: FeatureSourceType = Field(
        ...,
        description="How this feature is produced."
    )

    description: str = Field(
        ...,
        description="Feature description."
    )

    default_value: float = Field(
        default=0.0,
        description="Default value when unavailable."
    )

    min_value: float | None = Field(
        default=None,
        description="Optional lower bound."
    )

    max_value: float | None = Field(
        default=None,
        description="Optional upper bound."
    )
    
    

def create_feature(
    *,
    feature_id: str,
    name: str,
    column_name: str,
    category: FeatureCategory,
    source_type: FeatureSourceType,
    description: str,
    default_value: float = 0.0,
    min_value: float | None = None,
    max_value: float | None = None,
) -> FeatureDefinition:
    """
    Factory function for creating immutable FeatureDefinition objects.
    """

    return FeatureDefinition(
        id=feature_id,
        name=name,
        column_name=column_name,
        category=category,
        source_type=source_type,
        description=description,
        default_value=default_value,
        min_value=min_value,
        max_value=max_value,
    )

# ==========================================================
# TRUST FEATURES
# ==========================================================

TRUST_FEATURES = [

    create_feature(
        feature_id="B001",
        name="Account Maturity",
        column_name="account_maturity",
        category=FeatureCategory.TRUST,
        source_type=FeatureSourceType.DERIVED,
        description="Number of days since the user's account was created.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B002",
        name="Profile Completeness",
        column_name="profile_completeness",
        category=FeatureCategory.TRUST,
        source_type=FeatureSourceType.DERIVED,
        description="Percentage indicating how complete the user's profile is.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B003",
        name="Service Experience",
        column_name="service_experience",
        category=FeatureCategory.TRUST,
        source_type=FeatureSourceType.DIRECT,
        description="Total number of successfully completed services.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B004",
        name="Reputation Index",
        column_name="reputation_index",
        category=FeatureCategory.TRUST,
        source_type=FeatureSourceType.DIRECT,
        description="Overall reputation score assigned to the user.",
        default_value=0.0,
        min_value=0.0,
    ),

    create_feature(
        feature_id="B005",
        name="Skill Reliability",
        column_name="skill_reliability",
        category=FeatureCategory.TRUST,
        source_type=FeatureSourceType.DIRECT,
        description="Reliability score based on the user's demonstrated skills.",
        default_value=0.0,
        min_value=0.0,
    ),
]



# ==========================================================
# ACTIVITY FEATURES
# ==========================================================

ACTIVITY_FEATURES = [

    create_feature(
        feature_id="B006",
        name="Request Count",
        column_name="request_count",
        category=FeatureCategory.ACTIVITY,
        source_type=FeatureSourceType.AGGREGATED,
        description="Total number of service requests created by the user.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B007",
        name="Recent Request Rate",
        column_name="recent_request_rate",
        category=FeatureCategory.ACTIVITY,
        source_type=FeatureSourceType.AGGREGATED,
        description="Ratio of recent service requests to overall request activity.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B008",
        name="Negotiation Count",
        column_name="negotiation_count",
        category=FeatureCategory.ACTIVITY,
        source_type=FeatureSourceType.AGGREGATED,
        description="Total number of negotiations initiated by the user.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B009",
        name="Negotiation Conversion Rate",
        column_name="negotiation_conversion_rate",
        category=FeatureCategory.ACTIVITY,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of successful negotiations to total negotiations initiated.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B010",
        name="Negotiation Activity Rate",
        column_name="negotiation_activity_rate",
        category=FeatureCategory.ACTIVITY,
        source_type=FeatureSourceType.AGGREGATED,
        description="Frequency of negotiation activity relative to the user's overall behaviour.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),
]


# ==========================================================
# COLLABORATION FEATURES
# ==========================================================

COLLABORATION_FEATURES = [

    create_feature(
        feature_id="B011",
        name="Collaboration Count",
        column_name="collaboration_count",
        category=FeatureCategory.COLLABORATION,
        source_type=FeatureSourceType.AGGREGATED,
        description="Total number of collaborations initiated by the user.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B012",
        name="Completed Collaboration Count",
        column_name="completed_collaboration_count",
        category=FeatureCategory.COLLABORATION,
        source_type=FeatureSourceType.AGGREGATED,
        description="Total number of successfully completed collaborations.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B013",
        name="Collaboration Completion Rate",
        column_name="collaboration_completion_rate",
        category=FeatureCategory.COLLABORATION,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of completed collaborations to total collaborations.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B014",
        name="Session Count",
        column_name="session_count",
        category=FeatureCategory.COLLABORATION,
        source_type=FeatureSourceType.AGGREGATED,
        description="Total number of service sessions associated with the user.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B015",
        name="Completed Session Rate",
        column_name="completed_session_rate",
        category=FeatureCategory.COLLABORATION,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of completed sessions to total service sessions.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),
]

# ==========================================================
# FINANCIAL FEATURES
# ==========================================================

FINANCIAL_FEATURES = [

    create_feature(
        feature_id="B016",
        name="Total Transactions",
        column_name="total_transactions",
        category=FeatureCategory.FINANCIAL,
        source_type=FeatureSourceType.AGGREGATED,
        description="Total number of wallet transactions performed by the user.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B017",
        name="Refund Ratio",
        column_name="refund_ratio",
        category=FeatureCategory.FINANCIAL,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of refunded transactions to total wallet transactions.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B018",
        name="Escrow Lock Ratio",
        column_name="escrow_lock_ratio",
        category=FeatureCategory.FINANCIAL,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of locked escrow transactions to total wallet transactions.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B019",
        name="Token Flow Balance",
        column_name="token_flow_balance",
        category=FeatureCategory.FINANCIAL,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of total tokens spent to total tokens earned.",
        default_value=0.0,
        min_value=0.0,
    ),

    create_feature(
        feature_id="B020",
        name="Average Transaction Size",
        column_name="average_transaction_size",
        category=FeatureCategory.FINANCIAL,
        source_type=FeatureSourceType.AGGREGATED,
        description="Average token amount across all wallet transactions.",
        default_value=0.0,
        min_value=0.0,
    ),
]

# ==========================================================
# REPUTATION FEATURES
# ==========================================================

REPUTATION_FEATURES = [

    create_feature(
        feature_id="B021",
        name="Rating Count",
        column_name="rating_count",
        category=FeatureCategory.REPUTATION,
        source_type=FeatureSourceType.AGGREGATED,
        description="Total number of ratings received by the user.",
        default_value=0.0,
        min_value=0,
    ),

    create_feature(
        feature_id="B022",
        name="Average Rating",
        column_name="average_rating",
        category=FeatureCategory.REPUTATION,
        source_type=FeatureSourceType.AGGREGATED,
        description="Average star rating received from other users.",
        default_value=0.0,
        min_value=0.0,
        max_value=5.0,
    ),

    create_feature(
        feature_id="B023",
        name="Low Rating Ratio",
        column_name="low_rating_ratio",
        category=FeatureCategory.REPUTATION,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of ratings that are less than or equal to 2 stars.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B024",
        name="High Rating Ratio",
        column_name="high_rating_ratio",
        category=FeatureCategory.REPUTATION,
        source_type=FeatureSourceType.DERIVED,
        description="Ratio of ratings that are greater than or equal to 4 stars.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B025",
        name="Rating Consistency",
        column_name="rating_consistency",
        category=FeatureCategory.REPUTATION,
        source_type=FeatureSourceType.DERIVED,
        description="Consistency score derived from the user's rating distribution.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),
]

# ==========================================================
# CONSISTENCY FEATURES
# ==========================================================

CONSISTENCY_FEATURES = [

    create_feature(
        feature_id="B026",
        name="Activity Trust Alignment",
        column_name="activity_trust_alignment",
        category=FeatureCategory.CONSISTENCY,
        source_type=FeatureSourceType.DERIVED,
        description="Alignment between the user's trust profile and request activity.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B027",
        name="Collaboration Reputation Alignment",
        column_name="collaboration_reputation_alignment",
        category=FeatureCategory.CONSISTENCY,
        source_type=FeatureSourceType.DERIVED,
        description="Alignment between collaboration behaviour and user reputation.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B028",
        name="Financial Activity Alignment",
        column_name="financial_activity_alignment",
        category=FeatureCategory.CONSISTENCY,
        source_type=FeatureSourceType.DERIVED,
        description="Alignment between financial transactions and platform activity.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B029",
        name="Profile Behavior Alignment",
        column_name="profile_behavior_alignment",
        category=FeatureCategory.CONSISTENCY,
        source_type=FeatureSourceType.DERIVED,
        description="Alignment between profile completeness and collaboration behaviour.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),

    create_feature(
        feature_id="B030",
        name="Overall Consistency Score",
        column_name="overall_consistency_score",
        category=FeatureCategory.CONSISTENCY,
        source_type=FeatureSourceType.DERIVED,
        description="Overall behavioural consistency score derived from all consistency metrics.",
        default_value=0.0,
        min_value=0.0,
        max_value=1.0,
    ),
]


# ==========================================================
# MASTER FEATURE REGISTRY
# ==========================================================

FEATURES: list[FeatureDefinition] = (
    TRUST_FEATURES
    + ACTIVITY_FEATURES
    + COLLABORATION_FEATURES
    + FINANCIAL_FEATURES
    + REPUTATION_FEATURES
    + CONSISTENCY_FEATURES
)



FEATURE_IDS: list[str] = [
    feature.id
    for feature in FEATURES
]

FEATURE_COLUMNS: list[str] = [
    feature.column_name
    for feature in FEATURES
]

FEATURE_COLUMN_SET = frozenset(FEATURE_COLUMNS)

FEATURE_COUNT: int = len(FEATURES)



FEATURE_BY_ID: dict[str, FeatureDefinition] = {
    feature.id: feature
    for feature in FEATURES
}

FEATURE_BY_COLUMN: dict[str, FeatureDefinition] = {
    feature.column_name: feature
    for feature in FEATURES
}



# ==========================================================
# HELPER FUNCTIONS
# ==========================================================

def get_feature(feature_id: str) -> FeatureDefinition:
    """
    Returns the feature definition for the given feature ID.

    Raises:
        KeyError: If the feature ID does not exist.
    """
    return FEATURE_BY_ID[feature_id]


def get_feature_by_column(column_name: str) -> FeatureDefinition:
    """
    Returns the feature definition for the given dataset column name.

    Raises:
        KeyError: If the column name does not exist.
    """
    return FEATURE_BY_COLUMN[column_name]


def get_features_by_category(
    category: FeatureCategory,
) -> list[FeatureDefinition]:
    """
    Returns all features belonging to the specified category.
    """
    return [
        feature
        for feature in FEATURES
        if feature.category == category
    ]
    
    

__all__ = [
    "FeatureCategory",
    "FeatureSourceType",
    "FeatureDefinition",
    "TRUST_FEATURES",
    "ACTIVITY_FEATURES",
    "COLLABORATION_FEATURES",
    "FINANCIAL_FEATURES",
    "REPUTATION_FEATURES",
    "CONSISTENCY_FEATURES",
    "FEATURES",
    "FEATURE_IDS",
    "FEATURE_COLUMNS",
    "FEATURE_COLUMN_SET",
    "FEATURE_COUNT",
    "FEATURE_BY_ID",
    "FEATURE_BY_COLUMN",
    "get_feature",
    "get_feature_by_column",
    "get_features_by_category",
]

    
    
# ==========================================================
# SCHEMA VALIDATION
# ==========================================================

if FEATURE_COUNT != 30:
    raise ValueError(
        f"Expected 30 fraud detection features, found {FEATURE_COUNT}."
    )

if len(FEATURE_IDS) != len(set(FEATURE_IDS)):
    raise ValueError("Duplicate feature IDs detected.")

if len(FEATURE_COLUMNS) != len(set(FEATURE_COLUMNS)):
    raise ValueError("Duplicate feature column names detected.")


