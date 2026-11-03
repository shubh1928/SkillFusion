package com.barterlink.ai.service.impl;

import com.barterlink.ai.dto.FraudPredictionResponse;
import com.barterlink.ai.fraud.service.FraudAssessmentPersistenceService;
import com.barterlink.ai.service.FraudDetectionService;
import com.barterlink.ai.service.TrustAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustAssessmentServiceImpl
        implements TrustAssessmentService {

    private final FraudDetectionService fraudDetectionService;

    private final FraudAssessmentPersistenceService fraudAssessmentPersistenceService;

    @Override
    public void refreshAssessment(UUID userId) {

        try {

            log.info("Refreshing trust assessment for user: {}", userId);

            FraudPredictionResponse response =
                    fraudDetectionService.detectFraud(userId);

            fraudAssessmentPersistenceService.saveAssessment(
                    userId,
                    response
            );

            log.info("Trust assessment refreshed successfully for user: {}", userId);

        } catch (Exception ex) {

            log.error(
                    "Failed to refresh trust assessment for user: {}",
                    userId,
                    ex
            );

        }
    }
}