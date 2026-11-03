package com.barterlink.ai.fraud.entity;

import com.barterlink.auth.entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_fraud_assessment")
public class UserFraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(nullable = false)
    private Double riskScore;

    @Column(nullable = false)
    private Double trustScore;

    @Column(nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false)
    private Boolean fraud;

    @Column(nullable = false)
    private Instant evaluatedAt;

    @OneToMany(
            mappedBy = "assessment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<UserFraudReason> reasons = new ArrayList<>();
}