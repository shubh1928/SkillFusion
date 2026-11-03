package com.barterlink.wallet.entity;

import com.barterlink.auth.entities.User;
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
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    /*
     * Blockchain wallet information.
     *
     * These fields are nullable for now because existing wallet
     * records may already exist without blockchain information.
     * They will be populated when the blockchain wallet is created.
     */
    @Column(
            name = "blockchain_address",
            length = 42,
            unique = true
    )
    private String blockchainAddress;

    @Lob
    @Column(name = "encrypted_private_key")
    private String encryptedPrivateKey;

    @Column(
            name = "private_key_iv",
            length = 24
    )
    private String privateKeyIv;

    @Builder.Default
    @Column(nullable = false)
    private Integer availableTokens = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer lockedTokens = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}