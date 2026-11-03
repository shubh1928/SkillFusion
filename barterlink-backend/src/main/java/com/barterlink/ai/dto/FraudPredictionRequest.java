package com.barterlink.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudPredictionRequest {

    @JsonProperty("user_id")
    private UUID userId;

    private Map<String, Double> features;

}