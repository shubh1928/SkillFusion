package com.barterlink.ai.behaviour.service;

import com.barterlink.ai.behaviour.calculator.ActivityFeatureCalculator;
import com.barterlink.ai.behaviour.calculator.CollaborationFeatureCalculator;
import com.barterlink.ai.behaviour.calculator.ConsistencyFeatureCalculator;
import com.barterlink.ai.behaviour.calculator.FinancialFeatureCalculator;
import com.barterlink.ai.behaviour.calculator.ReputationFeatureCalculator;
import com.barterlink.ai.behaviour.calculator.TrustFeatureCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BehaviourAnalyticsServiceImpl
        implements BehaviourAnalyticsService {

    private final TrustFeatureCalculator trustFeatureCalculator;

    private final ActivityFeatureCalculator activityFeatureCalculator;

    private final CollaborationFeatureCalculator collaborationFeatureCalculator;

    private final FinancialFeatureCalculator financialFeatureCalculator;

    private final ReputationFeatureCalculator reputationFeatureCalculator;

    private final ConsistencyFeatureCalculator consistencyFeatureCalculator;

    @Override
    public Map<String, Double> calculateFeatures(
            UUID userId
    ) {

        Map<String, Double> features = new HashMap<>();

        features.putAll(
                trustFeatureCalculator.calculate(userId)
        );

        features.putAll(
                activityFeatureCalculator.calculate(userId)
        );

        features.putAll(
                collaborationFeatureCalculator.calculate(userId)
        );

        features.putAll(
                reputationFeatureCalculator.calculate(userId)
        );

        features.putAll(
                financialFeatureCalculator.calculate(userId)
        );

        features.putAll(
                consistencyFeatureCalculator.calculate(features)
        );

        return features;

    }

}