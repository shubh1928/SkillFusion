package com.barterlink.ai.behaviour.calculator;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.wallet.entity.TokenTransaction;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.enums.TransactionType;
import com.barterlink.wallet.repository.TokenTransactionRepository;
import com.barterlink.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FinancialFeatureCalculatorImpl
        implements FinancialFeatureCalculator {

    private final UserRepository userRepository;

    private final WalletRepository walletRepository;

    private final TokenTransactionRepository tokenTransactionRepository;

    @Override
    public Map<String, Double> calculate(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Wallet not found"));

        Map<String, Double> features = new HashMap<>();

        long totalTransactions =
                tokenTransactionRepository.countByWallet(wallet);

        long refundedTransactions =
                tokenTransactionRepository.countByWalletAndTransactionType(
                        wallet,
                        TransactionType.REFUNDED
                );

        long lockedTransactions =
                tokenTransactionRepository.countByWalletAndTransactionType(
                        wallet,
                        TransactionType.LOCKED
                );

        List<TokenTransaction> earnedTransactions =
                tokenTransactionRepository.findByWalletAndTransactionType(
                        wallet,
                        TransactionType.EARNED
                );

        List<TokenTransaction> spentTransactions =
                tokenTransactionRepository.findByWalletAndTransactionType(
                        wallet,
                        TransactionType.SPENT
                );

        double totalEarnedTokens = earnedTransactions.stream()
                .mapToInt(TokenTransaction::getAmount)
                .sum();

        double totalSpentTokens = spentTransactions.stream()
                .mapToInt(TokenTransaction::getAmount)
                .sum();

        double averageTransactionSize = 0.0;

        if (totalTransactions > 0) {

            averageTransactionSize =
                    (totalEarnedTokens + totalSpentTokens)
                            / totalTransactions;
        }

        features.put(
                "total_transactions",
                (double) totalTransactions
        );

        double refundRatio = 0.0;
        double escrowLockRatio = 0.0;
        double tokenFlowBalance = 0.0;

        if (totalTransactions > 0) {

            refundRatio =
                    (double) refundedTransactions / totalTransactions;

            escrowLockRatio =
                    (double) lockedTransactions / totalTransactions;
        }

        if (totalEarnedTokens > 0) {

            tokenFlowBalance =
                    totalSpentTokens / totalEarnedTokens;
        }

        features.put(
                "refund_ratio",
                refundRatio
        );

        features.put(
                "escrow_lock_ratio",
                escrowLockRatio
        );

        features.put(
                "token_flow_balance",
                tokenFlowBalance
        );

        features.put(
                "average_transaction_size",
                averageTransactionSize
        );

        return features;
    }

}