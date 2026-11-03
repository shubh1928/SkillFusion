package com.barterlink.agreements.entity;

import com.barterlink.agreements.enums.AgreementMilestoneStatus;
import com.barterlink.negotiation.entity.Negotiation;
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
@Table(name = "agreement_milestones")
public class AgreementMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "milestone_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    /*
     * Stable milestone identity across all versions of this negotiation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negotiation_id", nullable = false)
    private Negotiation negotiation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AgreementMilestoneStatus status;

    /*
     * Only the current funded milestone should have tokens locked.
     */
    @Builder.Default
    @Column(name = "escrow_locked_tokens", nullable = false)
    private Integer escrowLockedTokens = 0;

    @Column(name = "blockchain_escrow_id", length = 66)
    private String blockchainEscrowId;

    /**
     * Timestamp when the requester confirmed this milestone as complete.
     * Null means the requester has not confirmed yet.
     */
    @Column(name = "requester_completion_confirmed_at")
    private Instant requesterCompletionConfirmedAt;

    /**
     * Timestamp when the provider confirmed this milestone as complete.
     * Null means the provider has not confirmed yet.
     */
    @Column(name = "provider_completion_confirmed_at")
    private Instant providerCompletionConfirmedAt;

    /**
     * Time when the first blockchain release attempt was claimed.
     * A non-null value prevents automatic duplicate submissions.
     */
    @Column(name = "release_attempt_started_at")
    private Instant releaseAttemptStartedAt;

    /**
     * Time when the first blockchain funding attempt was claimed.
     * A non-null value prevents automatic duplicate funding submissions.
     */
    @Column(name = "funding_attempt_started_at")
    private Instant fundingAttemptStartedAt;

    @Column(name = "funded_at")
    private Instant fundedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = AgreementMilestoneStatus.PLANNED;
        }

        if (escrowLockedTokens == null) {
            escrowLockedTokens = 0;
        }
    }
}