package com.barterlink.ai.dto;

import lombok.*;
import java.util.List;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustAssessmentSummaryDto {

    private String riskLevel;

    private Double trustScore;

    private Double riskScore;

    private Instant evaluatedAt;

    private List<FraudReasonDto> reasons;
}