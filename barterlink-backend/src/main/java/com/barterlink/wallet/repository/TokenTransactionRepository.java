package com.barterlink.wallet.repository;

import com.barterlink.wallet.entity.TokenTransaction;
import com.barterlink.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TokenTransactionRepository
        extends JpaRepository<TokenTransaction, UUID> {

    List<TokenTransaction> findByWallet(Wallet wallet);

    List<TokenTransaction> findByWalletId(UUID walletId);

    List<TokenTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
}