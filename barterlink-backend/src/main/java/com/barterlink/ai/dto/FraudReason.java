package com.barterlink.ai.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudReason {

    private String feature;

    private double value;

    private String message;

}