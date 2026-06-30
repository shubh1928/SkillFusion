package com.barterlink.wallet.repository;


import com.barterlink.auth.entities.User;
import com.barterlink.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository
        extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUser(User user);

    Optional<Wallet> findByUserId(UUID userId);

    boolean existsByUser(User user);
}