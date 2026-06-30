package com.barterlink.wallet.service;


import com.barterlink.wallet.dto.response.TokenTransactionResponseDto;
import com.barterlink.wallet.dto.WalletResponseDto;
import com.barterlink.wallet.dto.WalletSummaryDto;

import java.util.List;
import java.util.UUID;

public interface WalletService {

    WalletResponseDto initializeWallet(UUID userId);

    WalletResponseDto getWallet(UUID userId);

    WalletSummaryDto getWalletSummary(UUID userId);

    List<TokenTransactionResponseDto> getTransactionHistory(UUID userId);

    void addBonusTokens(
            UUID userId,
            Integer amount,
            String description);

    void lockTokens(
            UUID userId,
            Integer amount,
            String description);

    void releaseTokens(
            UUID userId,
            Integer amount,
            String description);

    void refundTokens(
            UUID userId,
            Integer amount,
            String description);

    void spendTokens(
            UUID userId,
            Integer amount,
            String description);

    void transferLockedTokens(
            UUID requesterId,
            UUID providerId,
            Integer amount,
            String description);
}