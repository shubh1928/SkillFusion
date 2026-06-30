package com.barterlink.agreements.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.marketplace.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "service_agreements")
public class ServiceAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "agreement_id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "request_id",
            nullable = false,
            unique = true
    )
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "requester_id",
            nullable = false
    )
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "provider_id",
            nullable = false
    )
    private User provider;

    @Column(nullable = false)
    private Integer escrowLockedTokens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status;

    @Column(nullable = false, updatable = false)
    private Instant acceptedAt;

    private Instant completedAt;

    @PrePersist
    protected void onCreate() {

        acceptedAt = Instant.now();

        if (status == null) {
            status = AgreementStatus.ACTIVE;
        }
    }
}