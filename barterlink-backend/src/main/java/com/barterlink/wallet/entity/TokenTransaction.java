package com.barterlink.wallet.entity;

import com.barterlink.wallet.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import com.barterlink.rewards.entity.RewardClaim;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "token_transactions")
public class TokenTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "wallet_id",
            nullable = false
    )
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer amount;

    @Column(length = 500)
    private String description;

    /**
     * Unique link for a reward ledger credit.
     * This is not used as a human-readable idempotency key.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_claim_id", unique = true)
    private RewardClaim rewardClaim;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}