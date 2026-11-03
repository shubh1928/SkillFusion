package com.barterlink.ai.service.impl;

import com.barterlink.ai.dto.FraudAssessmentDto;
import com.barterlink.ai.fraud.entity.UserFraudAssessment;
import com.barterlink.ai.fraud.repository.UserFraudAssessmentRepository;
import com.barterlink.ai.service.FraudAssessmentService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.barterlink.ai.dto.FraudReasonDto;
import com.barterlink.ai.fraud.entity.UserFraudReason;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudAssessmentServiceImpl
        implements FraudAssessmentService {

    private final UserRepository userRepository;

    private final UserFraudAssessmentRepository assessmentRepository;

    @Override
    public FraudAssessmentDto getLatestAssessment(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with the given id!"
                        ));

        UserFraudAssessment assessment =
                assessmentRepository.findByUser(user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Fraud assessment not found for this user!"
                                ));

        List<FraudReasonDto> reasons =
                assessment.getReasons()
                        .stream()
                        .map(reason -> FraudReasonDto.builder()
                                .feature(reason.getFeature())
                                .value(reason.getValue())
                                .message(reason.getMessage())
                                .build())
                        .toList();

        return FraudAssessmentDto.builder()
                .userId(user.getId())
                .riskScore(assessment.getRiskScore())
                .trustScore(assessment.getTrustScore())
                .riskLevel(assessment.getRiskLevel())
                .fraud(assessment.getFraud())
                .evaluatedAt(assessment.getEvaluatedAt())
                .reasons(reasons)
                .build();
    }
}