package com.barterlink.wallet.service.impl;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.constants.WalletConstants;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.wallet.dto.response.TokenTransactionResponseDto;
import com.barterlink.wallet.dto.WalletResponseDto;
import com.barterlink.wallet.dto.WalletSummaryDto;
import com.barterlink.wallet.entity.TokenTransaction;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.enums.TransactionType;
import com.barterlink.wallet.repository.TokenTransactionRepository;
import com.barterlink.wallet.repository.WalletRepository;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.blockchain.wallet.BlockchainWalletProvisioningService;
import com.barterlink.blockchain.token.BlockchainTokenService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TokenTransactionRepository tokenTransactionRepository;
    private final UserRepository userRepository;
    private final BlockchainWalletProvisioningService blockchainWalletProvisioningService;
    private final BlockchainTokenService blockchainTokenService;

    @Transactional
    @Override
    public WalletResponseDto initializeWallet(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (walletRepository.existsByUser(user)) {
            throw new BusinessException("Wallet already exists");
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .availableTokens(0)
                .lockedTokens(0)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        /*
         * Create the user's blockchain wallet and securely persist
         * the blockchain address, encrypted private key and IV.
         */
        blockchainWalletProvisioningService.provisionWallet(
                userId
        );

        WalletResponseDto dto = new WalletResponseDto();
        dto.setWalletId(savedWallet.getId());
        dto.setAvailableTokens(savedWallet.getAvailableTokens());
        dto.setLockedTokens(savedWallet.getLockedTokens());

        return dto;
    }

    @Transactional
    @Override
    public void grantSignupBonus(UUID userId) {

        Wallet wallet = getWalletEntity(userId);

        if (wallet.getBlockchainAddress() == null
                || wallet.getBlockchainAddress().isBlank()) {
            throw new BusinessException(
                    "Blockchain wallet is not provisioned");
        }

        blockchainTokenService.mintSignupBonus(wallet);

        addBonusTokens(
                userId,
                WalletConstants.SIGNUP_BONUS,
                "Signup Bonus");
    }

    @Override
    public WalletResponseDto getWallet(UUID userId) {

        Wallet wallet = getWalletEntity(userId);

        WalletResponseDto dto = new WalletResponseDto();
        dto.setWalletId(wallet.getId());
        dto.setAvailableTokens(wallet.getAvailableTokens());
        dto.setLockedTokens(wallet.getLockedTokens());

        return dto;
    }

    @Override
    public WalletSummaryDto getWalletSummary(UUID userId) {

        Wallet wallet = getWalletEntity(userId);

        WalletSummaryDto dto = new WalletSummaryDto();
        dto.setAvailableTokens(wallet.getAvailableTokens());
        dto.setLockedTokens(wallet.getLockedTokens());
        dto.setTotalTokens(
                wallet.getAvailableTokens()
                        + wallet.getLockedTokens());

        return dto;
    }

    @Override
    public List<TokenTransactionResponseDto> getTransactionHistory(UUID userId) {

        Wallet wallet = getWalletEntity(userId);

        return tokenTransactionRepository
                .findByWalletOrderByCreatedAtDesc(wallet)
                .stream()
                .map(transaction -> {

                    TokenTransactionResponseDto dto =
                            new TokenTransactionResponseDto();

                    dto.setTransactionId(transaction.getId());
                    dto.setTransactionType(
                            transaction.getTransactionType());
                    dto.setAmount(transaction.getAmount());
                    dto.setDescription(transaction.getDescription());
                    dto.setCreatedAt(transaction.getCreatedAt());

                    return dto;

                }).toList();
    }

    @Transactional
    @Override
    public void addBonusTokens(
            UUID userId,
            Integer amount,
            String description) {

        Wallet wallet = getWalletEntity(userId);

        wallet.setAvailableTokens(
                wallet.getAvailableTokens() + amount);

        walletRepository.save(wallet);

        createTransaction(
                wallet,
                TransactionType.BONUS,
                amount,
                description);
    }

    @Transactional
    @Override
    public void lockTokens(
            UUID userId,
            Integer amount,
            String description) {

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        if (wallet.getAvailableTokens() < amount) {
            throw new BusinessException(
                    "Insufficient wallet balance");
        }

        wallet.setAvailableTokens(
                wallet.getAvailableTokens() - amount);

        wallet.setLockedTokens(
                wallet.getLockedTokens() + amount);

        walletRepository.save(wallet);

        createTransaction(
                wallet,
                TransactionType.LOCKED,
                amount,
                description);
    }

    @Transactional
    @Override
    public void releaseTokens(
            UUID userId,
            Integer amount,
            String description) {

        Wallet wallet = getWalletEntity(userId);

        if (wallet.getLockedTokens() < amount) {
            throw new BusinessException(
                    "Insufficient locked balance");
        }

        wallet.setLockedTokens(
                wallet.getLockedTokens() - amount);

        wallet.setAvailableTokens(
                wallet.getAvailableTokens() + amount);

        walletRepository.save(wallet);

        createTransaction(
                wallet,
                TransactionType.RELEASED,
                amount,
                description);
    }

    @Transactional
    @Override
    public void refundTokens(
            UUID userId,
            Integer amount,
            String description) {

        if (userId == null) {
            throw new BusinessException(
                    "User ID is required for a refund");
        }

        if (amount == null || amount <= 0) {
            throw new BusinessException(
                    "Refund amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        if (wallet.getLockedTokens() == null
                || wallet.getLockedTokens() < amount) {
            throw new BusinessException(
                    "Insufficient locked balance");
        }

        wallet.setLockedTokens(
                wallet.getLockedTokens() - amount);

        wallet.setAvailableTokens(
                wallet.getAvailableTokens() + amount);

        walletRepository.save(wallet);

        createTransaction(
                wallet,
                TransactionType.REFUNDED,
                amount,
                description);
    }

    @Transactional
    @Override
    public void spendTokens(
            UUID userId,
            Integer amount,
            String description) {

        Wallet wallet = getWalletEntity(userId);

        wallet.setAvailableTokens(
                wallet.getAvailableTokens() + amount);

        walletRepository.save(wallet);

        createTransaction(
                wallet,
                TransactionType.EARNED,
                amount,
                description);
    }

    private Wallet getWalletEntity(UUID userId) {

        return walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wallet not found"));
    }

    private void createTransaction(
            Wallet wallet,
            TransactionType transactionType,
            Integer amount,
            String description) {

        TokenTransaction transaction =
                TokenTransaction.builder()
                        .wallet(wallet)
                        .transactionType(transactionType)
                        .amount(amount)
                        .description(description)
                        .build();

        tokenTransactionRepository.save(transaction);
    }


    @Transactional
    @Override
    public void transferLockedTokens(
            UUID requesterId,
            UUID providerId,
            Integer amount,
            String description) {

        if (requesterId == null || providerId == null) {
            throw new BusinessException(
                    "Requester and provider are required");
        }

        if (requesterId.equals(providerId)) {
            throw new BusinessException(
                    "Requester and provider must be different users");
        }

        if (amount == null || amount <= 0) {
            throw new BusinessException(
                    "Transfer amount must be greater than zero");
        }

        /*
         * Always acquire wallet locks in the same UUID order
         * to reduce the risk of deadlocks between concurrent transfers.
         */
        UUID firstUserId =
                requesterId.compareTo(providerId) < 0
                        ? requesterId
                        : providerId;

        UUID secondUserId =
                requesterId.equals(firstUserId)
                        ? providerId
                        : requesterId;

        Wallet firstWallet =
                walletRepository.findByUserIdForUpdate(firstUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Wallet not found"));

        Wallet secondWallet =
                walletRepository.findByUserIdForUpdate(secondUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Wallet not found"));

        Wallet requesterWallet =
                requesterId.equals(firstUserId)
                        ? firstWallet
                        : secondWallet;

        Wallet providerWallet =
                providerId.equals(firstUserId)
                        ? firstWallet
                        : secondWallet;

        if (requesterWallet.getLockedTokens() < amount) {
            throw new BusinessException(
                    "Insufficient locked balance");
        }

        requesterWallet.setLockedTokens(
                requesterWallet.getLockedTokens() - amount);

        providerWallet.setAvailableTokens(
                providerWallet.getAvailableTokens() + amount);

        walletRepository.save(requesterWallet);
        walletRepository.save(providerWallet);

        createTransaction(
                requesterWallet,
                TransactionType.RELEASED,
                amount,
                "Transferred to provider: " + description);

        createTransaction(
                providerWallet,
                TransactionType.EARNED,
                amount,
                description);
    }

}