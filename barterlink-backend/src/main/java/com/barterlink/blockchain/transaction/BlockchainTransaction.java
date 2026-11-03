package com.barterlink.blockchain.transaction;

import com.barterlink.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(
        name = "blockchain_transactions",
        indexes = {
                @Index(
                        name = "idx_blockchain_tx_wallet",
                        columnList = "wallet_id"
                ),
                @Index(
                        name = "idx_blockchain_tx_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_blockchain_tx_escrow",
                        columnList = "escrow_id"
                )
        }
)
public class BlockchainTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "transaction_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "wallet_id",
            nullable = false
    )
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_type",
            nullable = false,
            length = 30
    )
    private BlockchainTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private BlockchainTransactionStatus status;

    @Convert(converter = BigIntegerStringConverter.class)
    @Column(
            length = 78
    )
    private BigInteger amount;

    /**
     * Ethereum transaction hash.
     * May be populated while status is REQUESTED when the signed
     * transaction hash is persisted before broadcasting, allowing
     * reconciliation if the RPC submission result is uncertain.
     */
    @Column(
            name = "tx_hash",
            length = 66,
            unique = true
    )
    private String transactionHash;

    /**
     * Exact signed Ethereum transaction bytes for campaign reward recovery.
     * Used to rebroadcast the same transaction when its outcome is uncertain.
     */
    @Column(
            name = "signed_raw_transaction",
            length = 8192
    )
    private String signedRawTransaction;

    /**
     * Timestamp of the most recent initial or repeated broadcast attempt.
     */
    @Column(name = "last_broadcast_attempt_at")
    private Instant lastBroadcastAttemptAt;

    /*
     * Escrow ID associated with LOCK / RELEASE / REFUND.
     * Stored as a 32-byte hexadecimal string.
     */
    @Column(
            name = "escrow_id",
            length = 66
    )
    private String escrowId;

    /**
     * Deterministic campaign claim key. Multiple attempts may share the same
     * key; the V2 contract prevents the key from minting more than once.
     */
    @Column(
            name = "reward_claim_key",
            length = 66
    )
    private String rewardClaimKey;

    /*
     * Blockchain sender.
     * Usually the protocol signer for application transactions.
     */
    @Column(
            name = "from_address",
            length = 42
    )
    private String fromAddress;

    /*
     * Contract address receiving the transaction.
     */
    @Column(
            name = "to_address",
            length = 42
    )
    private String toAddress;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @Column(
            name = "requested_at",
            nullable = false,
            updatable = false
    )
    private Instant requestedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = Instant.now();
    }
}