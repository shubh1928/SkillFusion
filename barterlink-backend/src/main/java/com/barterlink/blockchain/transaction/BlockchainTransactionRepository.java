package com.barterlink.blockchain.transaction;

import com.barterlink.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface BlockchainTransactionRepository
        extends JpaRepository<BlockchainTransaction, UUID> {

    List<BlockchainTransaction> findByWallet(
            Wallet wallet
    );

    List<BlockchainTransaction> findByWalletOrderByRequestedAtDesc(
            Wallet wallet
    );

    List<BlockchainTransaction> findByWalletAndStatus(
            Wallet wallet,
            BlockchainTransactionStatus status
    );

    Optional<BlockchainTransaction> findByTransactionHash(
            String transactionHash
    );

    Optional<BlockchainTransaction> findByEscrowId(
            String escrowId
    );

    Optional<BlockchainTransaction> findByEscrowIdAndTransactionType(
            String escrowId,
            BlockchainTransactionType transactionType
    );

    List<BlockchainTransaction> findAllByEscrowIdAndTransactionType(
            String escrowId,
            BlockchainTransactionType transactionType
    );

    List<BlockchainTransaction> findByStatus(
            BlockchainTransactionStatus status
    );

    long countByWalletAndStatusIn(
            Wallet wallet,
            Collection<BlockchainTransactionStatus> statuses
    );

    List<BlockchainTransaction> findByStatusInAndTransactionHashIsNotNull(
            Collection<BlockchainTransactionStatus> statuses
    );

    List<BlockchainTransaction>
    findAllByRewardClaimKeyOrderByRequestedAtDesc(
            String rewardClaimKey
    );

    Optional<BlockchainTransaction>
    findFirstByRewardClaimKeyOrderByRequestedAtDesc(
            String rewardClaimKey
    );

}