from __future__ import annotations

from fraud.feature_schema import (
    FEATURE_BY_COLUMN,
)

from fraud.schemas import (
    FraudReason,
)

from dataclasses import dataclass


@dataclass(frozen=True)
class ExplanationRule:
    """
    Defines a single explanation rule.

    If the observed feature value satisfies the rule,
    a FraudReason will be generated.
    """

    feature: str
    threshold: float
    trigger_if_less_than: bool
    message: str
    

EXPLANATION_RULES: tuple[ExplanationRule, ...] = (

    ExplanationRule(
        feature="account_maturity",
        threshold=30.0,
        trigger_if_less_than=True,
        message="Account is relatively new and has limited behavioural history.",
    ),

    ExplanationRule(
        feature="profile_completeness",
        threshold=0.50,
        trigger_if_less_than=True,
        message="Profile completeness is low, reducing user trust.",
    ),

    ExplanationRule(
        feature="reputation_index",
        threshold=0.40,
        trigger_if_less_than=True,
        message="User reputation is below the expected level.",
    ),

    ExplanationRule(
        feature="skill_reliability",
        threshold=0.50,
        trigger_if_less_than=True,
        message="Skill reliability is lower than expected.",
    ),

    ExplanationRule(
        feature="average_rating",
        threshold=3.0,
        trigger_if_less_than=True,
        message="Average rating received from other users is low.",
    ),

    ExplanationRule(
        feature="low_rating_ratio",
        threshold=0.40,
        trigger_if_less_than=False,
        message="A high proportion of received ratings are low.",
    ),
    
    ExplanationRule(
        feature="activity_trust_alignment",
        threshold=0.50,
        trigger_if_less_than=True,
        message="User activity is inconsistent with the established trust profile.",
    ),
    
    ExplanationRule(
        feature="financial_activity_alignment",
        threshold=0.50,
        trigger_if_less_than=True,
        message="Financial transaction patterns are inconsistent with the user's marketplace activity.",
    ),
    
    ExplanationRule(
        feature="profile_behavior_alignment",
        threshold=0.50,
        trigger_if_less_than=True,
        message="Profile quality does not align well with collaboration behaviour.",
    ),
    
    ExplanationRule(
        feature="overall_consistency_score",
        threshold=0.60,
        trigger_if_less_than=True,
        message="Overall behavioural consistency is lower than expected.",
    ),
    
)


def generate_reasons(
    features: dict[str, float],
) -> list[FraudReason]:
    """
    Generates human-readable fraud reasons by evaluating
    the configured explanation rules against the supplied
    feature values.
    """

    reasons: list[FraudReason] = []

    for rule in EXPLANATION_RULES:

        value = features.get(rule.feature)

        if value is None:
            continue

        triggered = (
            value < rule.threshold
            if rule.trigger_if_less_than
            else value > rule.threshold
        )

        if not triggered:
            continue

        feature = FEATURE_BY_COLUMN[rule.feature]

        reasons.append(
            FraudReason(
                feature=feature.name,
                value=value,
                message=rule.message,
            )
        )

    return reasons





