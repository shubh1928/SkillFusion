package com.barterlink.ai.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAssessmentDto {

    private UUID userId;

    private Double riskScore;

    private Double trustScore;

    private String riskLevel;

    private Boolean fraud;

    private Instant evaluatedAt;

    private List<FraudReasonDto> reasons;

}