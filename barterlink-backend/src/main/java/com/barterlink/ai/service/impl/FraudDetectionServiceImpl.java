package com.barterlink.ai.service.impl;

import com.barterlink.ai.behaviour.service.BehaviourAnalyticsService;
import com.barterlink.ai.client.AIClient;
import com.barterlink.ai.dto.FraudPredictionRequest;
import com.barterlink.ai.dto.FraudPredictionResponse;
import com.barterlink.ai.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionServiceImpl
        implements FraudDetectionService {

    private final BehaviourAnalyticsService behaviourAnalyticsService;
    private final AIClient aiClient;

    @Override
    public FraudPredictionResponse detectFraud(
            UUID userId
    ) {

        Map<String, Double> features =
                behaviourAnalyticsService.calculateFeatures(userId);

        System.out.println("\n========== GENERATED FEATURES ==========");

        log.debug("Generated Features: {}", features);

        FraudPredictionRequest request =
                FraudPredictionRequest.builder()
                        .userId(userId)
                        .features(features)
                        .build();

        System.out.println("\n========== REQUEST ==========");
        log.debug("Prediction Request: {}", request);

        FraudPredictionResponse response =
                aiClient.predictFraud(request);

        System.out.println("\n========== RESPONSE ==========");
        log.debug("Prediction Response: {}", response);

        return response;
    }

}