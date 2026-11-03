package com.barterlink.blockchain.wallet;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.blockchain.config.BlockchainProperties;
import com.barterlink.blockchain.escrow.BlockchainEscrowTransactionService;
import com.barterlink.blockchain.token.BlockchainTokenService;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionRepository;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.wallet.dto.response.BlockchainTransactionResponseDto;
import com.barterlink.wallet.dto.response.BlockchainWalletResponseDto;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockchainWalletQueryService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final AgreementRepository agreementRepository;

    private final BlockchainTokenService blockchainTokenService;
    private final BlockchainEscrowTransactionService blockchainEscrowTransactionService;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final BlockchainProperties blockchainProperties;


    @Transactional(readOnly = true)
    public BlockchainWalletResponseDto getBlockchainWallet(
            UUID userId) {

        Wallet wallet =
                walletRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Wallet not found"));

        String blockchainAddress =
                wallet.getBlockchainAddress();

        if (blockchainAddress == null
                || blockchainAddress.isBlank()) {

            throw new BusinessException(
                    "Blockchain wallet is not provisioned");
        }

        BigDecimal onChainBalance =
                toBlT(
                        blockchainTokenService.getBalance(
                                blockchainAddress));

        BigDecimal lockedInEscrow =
                getLockedInEscrow(
                        userId,
                        blockchainAddress);

        BlockchainWalletResponseDto dto =
                new BlockchainWalletResponseDto();

        dto.setWalletId(wallet.getId());

        dto.setBlockchainAddress(
                blockchainAddress);

        dto.setChainId(
                blockchainProperties.getChainId());

        dto.setTokenContractAddress(
                blockchainProperties.getTokenAddress());

        dto.setEscrowContractAddress(
                blockchainProperties.getEscrowAddress());

        dto.setOnChainBalance(
                onChainBalance);

        dto.setLockedInEscrow(
                lockedInEscrow);

        dto.setTotalBalance(
                onChainBalance.add(lockedInEscrow));

        return dto;
    }


    @Transactional(readOnly = true)
    public List<BlockchainTransactionResponseDto>
    getBlockchainTransactionHistory(UUID userId) {

        Wallet wallet =
                walletRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Wallet not found"));

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        /*
         * Start with transactions directly associated with
         * the user's wallet.
         *
         * This includes the signup bonus and any future
         * wallet-associated blockchain operations.
         */
        Map<UUID, BlockchainTransaction> transactions =
                new LinkedHashMap<>();

        for (BlockchainTransaction transaction :
                blockchainTransactionRepository
                        .findByWalletOrderByRequestedAtDesc(wallet)) {

            transactions.put(
                    transaction.getId(),
                    transaction);
        }

        /*
         * Escrow transactions are also relevant to a user
         * when they participate as requester or provider.
         *
         * Some RELEASE / REFUND tracking records are associated
         * with the requester's wallet, so we also resolve
         * transactions through the user's escrow IDs.
         */
        Set<String> escrowIds =
                new LinkedHashSet<>();

        collectEscrowIds(
                agreementRepository.findByRequester(user),
                escrowIds);

        collectEscrowIds(
                agreementRepository.findByProvider(user),
                escrowIds);

        for (String escrowId : escrowIds) {

            addEscrowTransaction(
                    transactions,
                    escrowId,
                    BlockchainTransactionType.LOCK);

            addEscrowTransaction(
                    transactions,
                    escrowId,
                    BlockchainTransactionType.RELEASE);

            addEscrowTransaction(
                    transactions,
                    escrowId,
                    BlockchainTransactionType.REFUND);
        }

        return transactions.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                BlockchainTransaction::getRequestedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder())))
                .map(this::mapTransaction)
                .toList();
    }


    private void collectEscrowIds(
            List<ServiceAgreement> agreements,
            Set<String> escrowIds) {

        for (ServiceAgreement agreement : agreements) {

            String escrowId =
                    agreement.getBlockchainEscrowId();

            if (escrowId != null
                    && !escrowId.isBlank()) {

                escrowIds.add(
                        escrowId.trim());
            }
        }
    }


    private void addEscrowTransaction(
            Map<UUID, BlockchainTransaction> transactions,
            String escrowId,
            BlockchainTransactionType transactionType) {

        List<BlockchainTransaction> escrowTransactions =
                blockchainTransactionRepository
                        .findAllByEscrowIdAndTransactionType(
                                escrowId,
                                transactionType);

        for (BlockchainTransaction transaction : escrowTransactions) {

            transactions.putIfAbsent(
                    transaction.getId(),
                    transaction);
        }
    }


    private BigDecimal getLockedInEscrow(
            UUID userId,
            String blockchainAddress) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        BigInteger totalLockedBaseUnits =
                BigInteger.ZERO;

        /*
         * Only requester agreements can contain the user's
         * tokens locked into escrow.
         */
        List<ServiceAgreement> agreements =
                agreementRepository.findByRequester(user);

        for (ServiceAgreement agreement : agreements) {

            String escrowId =
                    agreement.getBlockchainEscrowId();

            if (escrowId == null
                    || escrowId.isBlank()) {
                continue;
            }

            BlockchainEscrowTransactionService.EscrowDetails escrow =
                    blockchainEscrowTransactionService
                            .getEscrow(escrowId);

            /*
             * Solidity EscrowStatus:
             *
             * NONE     = 0
             * LOCKED   = 1
             * RELEASED = 2
             * REFUNDED = 3
             */
            if (!BigInteger.ONE.equals(
                    escrow.status())) {
                continue;
            }

            if (!blockchainAddress.equalsIgnoreCase(
                    escrow.requester())) {
                continue;
            }

            totalLockedBaseUnits =
                    totalLockedBaseUnits.add(
                            escrow.amount());
        }

        return toBlT(
                totalLockedBaseUnits);
    }


    private BigDecimal toBlT(
            BigInteger baseUnits) {

        return new BigDecimal(baseUnits)
                .divide(
                        new BigDecimal(
                                BigInteger.TEN.pow(18)),
                        18,
                        java.math.RoundingMode.UNNECESSARY)
                .stripTrailingZeros();
    }


    private BlockchainTransactionResponseDto
    mapTransaction(
            BlockchainTransaction transaction) {

        BlockchainTransactionResponseDto dto =
                new BlockchainTransactionResponseDto();

        dto.setTransactionId(
                transaction.getId());

        dto.setTransactionType(
                transaction.getTransactionType());

        dto.setStatus(
                transaction.getStatus());

        dto.setAmount(
                transaction.getAmount() == null
                        ? null
                        : toBlT(
                        transaction.getAmount()));

        dto.setTransactionHash(
                transaction.getTransactionHash());

        dto.setEscrowId(
                transaction.getEscrowId());

        dto.setFromAddress(
                transaction.getFromAddress());

        dto.setToAddress(
                transaction.getToAddress());

        dto.setFailureReason(
                transaction.getFailureReason());

        dto.setRequestedAt(
                transaction.getRequestedAt());

        dto.setSubmittedAt(
                transaction.getSubmittedAt());

        dto.setConfirmedAt(
                transaction.getConfirmedAt());

        dto.setFailedAt(
                transaction.getFailedAt());

        return dto;
    }
}