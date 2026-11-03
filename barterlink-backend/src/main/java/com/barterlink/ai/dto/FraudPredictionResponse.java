package com.barterlink.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudPredictionResponse {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("risk_score")
    private Double riskScore;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("is_fraud")
    private Boolean fraud;

    private List<FraudReason> reasons;

}