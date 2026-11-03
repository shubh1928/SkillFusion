package com.barterlink.ai.fraud.service.impl;

import com.barterlink.ai.dto.FraudPredictionResponse;
import com.barterlink.ai.fraud.entity.UserFraudAssessment;
import com.barterlink.ai.fraud.repository.UserFraudAssessmentRepository;
import com.barterlink.ai.fraud.service.FraudAssessmentPersistenceService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.barterlink.ai.dto.FraudReason;
import com.barterlink.ai.fraud.entity.UserFraudReason;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudAssessmentPersistenceServiceImpl
        implements FraudAssessmentPersistenceService {

    private final UserRepository userRepository;

    private final UserFraudAssessmentRepository assessmentRepository;

    @Override
    @Transactional
    public void saveAssessment(
            UUID userId,
            FraudPredictionResponse response
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with the given id!"
                        ));

        UserFraudAssessment assessment =
                assessmentRepository.findByUser(user)
                        .orElseGet(UserFraudAssessment::new);

        assessment.setUser(user);

        assessment.setRiskScore(response.getRiskScore());

        assessment.setTrustScore(
                calculateTrustScore(response.getRiskScore())
        );

        assessment.setRiskLevel(response.getRiskLevel());

        assessment.setFraud(response.getFraud());

        assessment.setEvaluatedAt(Instant.now());

        List<UserFraudReason> reasonEntities = new ArrayList<>();

        if (response.getReasons() != null) {

            for (FraudReason reason : response.getReasons()) {

                UserFraudReason entity =
                        UserFraudReason.builder()
                                .assessment(assessment)
                                .feature(reason.getFeature())
                                .value(reason.getValue())
                                .message(reason.getMessage())
                                .build();

                reasonEntities.add(entity);
            }
        }

        assessment.getReasons().clear();
        assessment.getReasons().addAll(reasonEntities);

        assessmentRepository.save(assessment);
    }

    private Double calculateTrustScore(Double riskScore) {

        double trustScore = (1 - riskScore) * 100;

        return Math.round(trustScore * 100.0) / 100.0;
    }

}