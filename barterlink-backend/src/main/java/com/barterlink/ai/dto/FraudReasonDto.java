package com.barterlink.ai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudReasonDto {

    private String feature;

    private Double value;

    private String message;
}