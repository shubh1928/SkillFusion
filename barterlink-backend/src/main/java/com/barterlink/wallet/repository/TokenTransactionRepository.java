package com.barterlink.wallet.repository;

import com.barterlink.wallet.entity.TokenTransaction;
import com.barterlink.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import com.barterlink.wallet.enums.TransactionType;

import java.util.List;
import java.util.UUID;

public interface TokenTransactionRepository
        extends JpaRepository<TokenTransaction, UUID> {

    List<TokenTransaction> findByWallet(Wallet wallet);

    List<TokenTransaction> findByWalletId(UUID walletId);

    List<TokenTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    long countByWallet(
            Wallet wallet
    );

    long countByWalletAndTransactionType(
            Wallet wallet,
            TransactionType transactionType
    );

    List<TokenTransaction> findByWalletAndTransactionType(
            Wallet wallet,
            TransactionType transactionType
    );

    java.util.Optional<TokenTransaction> findByRewardClaim_Id(
            UUID rewardClaimId
    );

    boolean existsByRewardClaim_Id(UUID rewardClaimId);
}