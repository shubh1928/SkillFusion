package com.barterlink.ai.fraud.service;

import com.barterlink.ai.dto.FraudPredictionResponse;

import java.util.UUID;

public interface FraudAssessmentPersistenceService {

    void saveAssessment(
            UUID userId,
            FraudPredictionResponse response
    );

}