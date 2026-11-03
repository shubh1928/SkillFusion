package com.barterlink.ai.behaviour.calculator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConsistencyFeatureCalculatorImpl
        implements ConsistencyFeatureCalculator {

    @Override
    public Map<String, Double> calculate(
            Map<String, Double> features
    ) {

        Map<String, Double> consistencyFeatures = new HashMap<>();

        double profileCompleteness =
                getFeature(features, "profile_completeness");

        double serviceExperience =
                getFeature(features, "service_experience");

        double requestCount =
                getFeature(features, "request_count");

        double collaborationCount =
                getFeature(features, "collaboration_count");

        double averageRating =
                getFeature(features, "average_rating");

        double totalTransactions =
                getFeature(features, "total_transactions");

        double refundRatio =
                getFeature(features, "refund_ratio");

        double activityTrustAlignment = 0.0;

        if (profileCompleteness > 0) {

            activityTrustAlignment =
                    profileCompleteness *
                            Math.min(
                                    requestCount / (serviceExperience + 1),
                                    1.0
                            );
        }

        consistencyFeatures.put(
                "activity_trust_alignment",
                activityTrustAlignment
        );

        double collaborationReputationAlignment = 0.0;

        if (collaborationCount > 0) {

            collaborationReputationAlignment =
                    averageRating / 5.0;
        }

        consistencyFeatures.put(
                "collaboration_reputation_alignment",
                collaborationReputationAlignment
        );

        double financialActivityAlignment = 0.0;

        if (requestCount > 0) {

            financialActivityAlignment =
                    Math.min(
                            totalTransactions / requestCount,
                            1.0
                    );
        }

        consistencyFeatures.put(
                "financial_activity_alignment",
                financialActivityAlignment
        );

        double profileBehaviorAlignment = 0.0;

        if (profileCompleteness > 0) {

            profileBehaviorAlignment =
                    profileCompleteness *
                            Math.min(
                                    collaborationCount /
                                            (serviceExperience + 1),
                                    1.0
                            );
        }

        consistencyFeatures.put(
                "profile_behavior_alignment",
                profileBehaviorAlignment
        );

        double overallConsistencyScore =

                (activityTrustAlignment
                        + collaborationReputationAlignment
                        + financialActivityAlignment
                        + profileBehaviorAlignment
                        + (1.0 - refundRatio))

                        / 5.0;

        consistencyFeatures.put(
                "overall_consistency_score",
                overallConsistencyScore
        );

        return consistencyFeatures;

    }



    private double getFeature(
            Map<String, Double> features,
            String key
    ) {

        return features.getOrDefault(key, 0.0);
    }
}