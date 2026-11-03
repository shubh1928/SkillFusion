package com.barterlink.ai.fraud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "user_fraud_reason")
public class UserFraudReason {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reason_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assessment_id",
            nullable = false
    )
    private UserFraudAssessment assessment;

    @Column(nullable = false, length = 100)
    private String feature;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false, length = 500)
    private String message;
}