package com.barterlink.agreements.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.entity.Negotiation;
import jakarta.persistence.*;
import lombok.*;
import com.barterlink.agreements.enums.AgreementReleaseMode;

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
            name = "negotiation_id",
            nullable = false,
            unique = true
    )
    private Negotiation negotiation;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status;

    @Column(nullable = false, updatable = false)
    private Instant acceptedAt;

    private Instant completedAt;

    @Column(length = 255)
    private String finalOfferTitle;

    @Column(columnDefinition = "TEXT")
    private String finalOfferDescription;

    @Column(nullable = false, length = 255)
    private String finalRequestTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String finalRequestDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeMode exchangeMode;

    /*
     * Agreed method of releasing BLT.
     *
     * NULL is permitted for legacy agreements and Direct Barter,
     * which does not use token release modes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "release_mode", length = 30)
    private AgreementReleaseMode releaseMode;

    @Column(nullable = false)
    private Integer estimatedHours;

    @Column(nullable = false)
    private Integer agreedTokenAmount;

    @Column(nullable = false)
    private Integer escrowLockedTokens;

    @Column(name = "blockchain_escrow_id", length = 66)
    private String blockchainEscrowId;

    /*
     * The currently active agreement version.
     *
     * NULL is valid for legacy agreements until versioning is connected.
     * A pending amendment must never replace this reference.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_version_id", unique = true)
    private AgreementVersion activeVersion;

    @PrePersist
    protected void onCreate() {

        acceptedAt = Instant.now();

        if (status == null) {
            status = AgreementStatus.ACTIVE;
        }
    }
}