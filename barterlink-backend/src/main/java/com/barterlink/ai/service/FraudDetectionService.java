package com.barterlink.ai.service;

import com.barterlink.ai.dto.FraudPredictionResponse;

import java.util.UUID;

public interface FraudDetectionService {

    FraudPredictionResponse detectFraud(
            UUID userId
    );

}