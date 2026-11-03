package com.barterlink.rewards.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardClaimStatus;
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
@Table(
        name = "reward_claims",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reward_claim_key",
                        columnNames = "claim_key"
                ),
                @UniqueConstraint(
                        name = "uk_reward_source_recipient_campaign",
                        columnNames = {
                                "source_type",
                                "source_id",
                                "recipient_id",
                                "campaign"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_reward_claim_status_next_attempt",
                        columnList = "status,next_attempt_at"
                ),
                @Index(
                        name = "idx_reward_claim_recipient_campaign",
                        columnList = "recipient_id,campaign,status"
                )
        }
)
public class RewardClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "reward_claim_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RewardCampaign campaign;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    /**
     * A deterministic 32-byte claim key passed to the V2 contract.
     */
    @Column(name = "claim_key", nullable = false, length = 66)
    private String claimKey;

    @Column(name = "amount_tokens", nullable = false)
    private Integer amountTokens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardClaimStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "blockchain_transaction_id",
            unique = true
    )
    private BlockchainTransaction blockchainTransaction;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "processing_at")
    private Instant processingAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }

        if (status == null) {
            status = RewardClaimStatus.PENDING;
        }

        if (attemptCount == null) {
            attemptCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}