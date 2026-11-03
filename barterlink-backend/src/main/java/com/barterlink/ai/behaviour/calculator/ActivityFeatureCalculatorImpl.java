package com.barterlink.ai.behaviour.calculator;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.marketplace.repository.ServiceRequestRepository;
import com.barterlink.negotiation.repository.NegotiationRepository;
import com.barterlink.negotiation.enums.NegotiationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActivityFeatureCalculatorImpl
        implements ActivityFeatureCalculator {

    private final UserRepository userRepository;

    private final ServiceRequestRepository serviceRequestRepository;

    private final NegotiationRepository negotiationRepository;

    @Override
    public Map<String, Double> calculate(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Map<String, Double> features = new HashMap<>();

        Instant thirtyDaysAgo =
                Instant.now().minus(30, ChronoUnit.DAYS);

        long requestCount =
                serviceRequestRepository.countByRequester(user);

        long recentRequestCount =
                serviceRequestRepository.countByRequesterAndCreatedAtAfter(
                        user,
                        thirtyDaysAgo
                );

        long negotiationCount =
                negotiationRepository.countByRequester(user);

        long acceptedNegotiationCount =
                negotiationRepository.countByRequesterAndStatus(
                        user,
                        NegotiationStatus.ACCEPTED
                );

        long recentNegotiationCount =
                negotiationRepository.countByRequesterAndCreatedAtAfter(
                        user,
                        thirtyDaysAgo
                );

        features.put(
                "request_count",
                (double) requestCount
        );

        features.put(
                "recent_request_rate",
                (double) recentRequestCount
        );

        features.put(
                "negotiation_count",
                (double) negotiationCount
        );

        double negotiationConversionRate = 0.0;

        if (negotiationCount > 0) {

            negotiationConversionRate =
                    (double) acceptedNegotiationCount / negotiationCount;
        }

        features.put(
                "negotiation_conversion_rate",
                negotiationConversionRate
        );

        features.put(
                "negotiation_activity_rate",
                (double) recentNegotiationCount
        );



        return features;
    }


}