package com.barterlink.ai.service;

import com.barterlink.ai.dto.FraudAssessmentDto;

import java.util.UUID;

public interface FraudAssessmentService {

    FraudAssessmentDto getLatestAssessment(UUID userId);

}