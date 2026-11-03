"""
Fraud Detection API Schemas
===========================

This module defines the request and response models used by
the Fraud Detection API.

These schemas act as the contract between the Spring Boot
backend and the FastAPI fraud detection service.

Used By
-------
- FastAPI endpoints
- predict.py
- Spring Boot AI integration

Author:
BarterLink AI Module
"""


from __future__ import annotations

from enum import Enum

from pydantic import BaseModel
from pydantic import ConfigDict
from pydantic import Field
from pydantic import model_validator

from fraud.feature_schema import FEATURE_COLUMN_SET

from uuid import UUID


class FraudRiskLevel(str, Enum):
    """
    Risk levels assigned by the fraud detection model.
    """

    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    
    
class FraudPredictionRequest(BaseModel):
    """
    Request payload sent by the Spring Boot backend
    to the Fraud Detection API.
    """

    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
    )

    user_id: UUID = Field(
        ...,
        description="Unique identifier of the user being evaluated."
    )

    features: dict[str, float] = Field(
        ...,
        description="Dictionary containing all fraud detection feature values."
    )
    

    @model_validator(mode="after")
    def validate_features(self) -> "FraudPredictionRequest":
        """
        Ensures that the request contains exactly the expected
        fraud detection features.
        """

        received = set(self.features.keys())
        expected = FEATURE_COLUMN_SET

        missing = expected - received
        extra = received - expected

        if missing:
            raise ValueError(
                f"Missing feature(s): {sorted(missing)}"
            )

        if extra:
            raise ValueError(
                f"Unexpected feature(s): {sorted(extra)}"
            )

        return self
    
    
    
class FraudReason(BaseModel):
    """
    Explains one factor contributing to the fraud prediction.
    """

    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
    )

    feature: str = Field(
        ...,
        description="Feature responsible for the fraud indication."
    )

    value: float = Field(
        ...,
        description="Observed value of the feature."
    )

    message: str = Field(
        ...,
        description="Human-readable explanation of the detected behaviour."
    )
    
    
    
class FraudPredictionResponse(BaseModel):
    """
    Response returned by the Fraud Detection API after
    evaluating a user's behavioural features.
    """

    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
    )

    user_id: UUID = Field(
        ...,
        description="Unique identifier of the evaluated user."
    )

    risk_score: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Normalized fraud risk score between 0 and 1."
    )

    risk_level: FraudRiskLevel = Field(
        ...,
        description="Categorized fraud risk level."
    )

    is_fraud: bool = Field(
        ...,
        description="Whether the user is classified as fraudulent."
    )

    reasons: list[FraudReason] = Field(
        default_factory=list,
        description="Human-readable explanations supporting the prediction."
    )   



__all__ = [
    "FraudRiskLevel",
    "FraudPredictionRequest",
    "FraudReason",
    "FraudPredictionResponse",
]