package com.barterlink.inactivity.service;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.blockchain.token.BlockchainTokenService;
import com.barterlink.blockchain.transaction.BlockchainTransactionRepository;
import com.barterlink.blockchain.transaction.BlockchainTransactionStatus;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import com.barterlink.marketplace.repository.ServiceRequestRepository;
import com.barterlink.negotiation.enums.NegotiationStatus;
import com.barterlink.negotiation.repository.NegotiationRepository;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AccountDeletionSafetyService {

    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final NegotiationRepository negotiationRepository;
    private final AgreementRepository agreementRepository;
    private final ServiceSessionRepository serviceSessionRepository;
    private final WalletRepository walletRepository;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final BlockchainTokenService blockchainTokenService;

    public AccountDeletionSafetyService(
            UserRepository userRepository,
            ServiceRequestRepository serviceRequestRepository,
            NegotiationRepository negotiationRepository,
            AgreementRepository agreementRepository,
            ServiceSessionRepository serviceSessionRepository,
            WalletRepository walletRepository,
            BlockchainTransactionRepository blockchainTransactionRepository,
            BlockchainTokenService blockchainTokenService) {

        this.userRepository = userRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.negotiationRepository = negotiationRepository;
        this.agreementRepository = agreementRepository;
        this.serviceSessionRepository = serviceSessionRepository;
        this.walletRepository = walletRepository;
        this.blockchainTransactionRepository = blockchainTransactionRepository;
        this.blockchainTokenService = blockchainTokenService;
    }

    /**
     * Read-only assessment. This method never deletes a user,
     * transfers tokens, or modifies business records.
     */
    @Transactional(readOnly = true)
    public DeletionSafetyReport assess(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + userId
                        )
                );

        Instant now = Instant.now();
        Instant lastActiveAt = user.getLastActiveAt();
        Instant deadline = null;
        List<String> blockers = new ArrayList<>();

        if (lastActiveAt == null) {
            blockers.add(
                    "LAST_ACTIVITY_MISSING: the inactivity deadline cannot be established."
            );
        } else {
            deadline = lastActiveAt
                    .atZone(ZoneOffset.UTC)
                    .plusMonths(3)
                    .toInstant();

            if (now.isBefore(deadline)) {
                blockers.add(
                        "INACTIVITY_DEADLINE_NOT_REACHED: deadline is "
                                + deadline + "."
                );
            }
        }

        checkServiceRequests(user, blockers);
        checkNegotiations(userId, blockers);
        checkAgreementsAndEscrows(user, userId, blockers);
        checkSessions(userId, blockers);
        checkWalletAndBlockchain(userId, blockers);

        boolean deadlineReached =
                deadline != null && !now.isBefore(deadline);

        boolean eligibleForDeletion =
                deadlineReached && blockers.isEmpty();

        return new DeletionSafetyReport(
                userId,
                lastActiveAt,
                deadline,
                deadlineReached,
                eligibleForDeletion,
                List.copyOf(blockers)
        );
    }

    private void checkServiceRequests(
            User user,
            List<String> blockers) {

        long count = serviceRequestRepository
                .countByRequesterAndStatusIn(
                        user,
                        List.of(
                                ServiceRequestStatus.OPEN,
                                ServiceRequestStatus.ACCEPTED,
                                ServiceRequestStatus.IN_PROGRESS,
                                ServiceRequestStatus.DISPUTED
                        )
                );

        addCountBlocker(
                blockers,
                count,
                "UNRESOLVED_SERVICE_REQUESTS"
        );
    }

    private void checkNegotiations(
            UUID userId,
            List<String> blockers) {

        long count = negotiationRepository.countUnresolvedForUser(
                userId,
                List.of(
                        NegotiationStatus.ACTIVE,
                        NegotiationStatus.SESSION_REQUESTED,
                        NegotiationStatus.ACCEPTED
                )
        );

        addCountBlocker(
                blockers,
                count,
                "UNRESOLVED_NEGOTIATIONS"
        );
    }

    private void checkAgreementsAndEscrows(
            User user,
            UUID userId,
            List<String> blockers) {

        long count = agreementRepository.countUnresolvedForUser(
                userId,
                List.of(
                        AgreementStatus.ACTIVE,
                        AgreementStatus.DISPUTED
                )
        );

        addCountBlocker(
                blockers,
                count,
                "UNRESOLVED_AGREEMENTS_OR_DISPUTES"
        );

        /*
         * A recorded escrow ID is not proof that funds have been released.
         * Until contract status values and finality are verified, keep these
         * accounts blocked rather than assuming the escrow is settled.
         */
        Map<UUID, ServiceAgreement> agreements = new LinkedHashMap<>();

        for (ServiceAgreement agreement :
                agreementRepository.findByRequester(user)) {
            agreements.put(agreement.getId(), agreement);
        }

        for (ServiceAgreement agreement :
                agreementRepository.findByProvider(user)) {
            agreements.put(agreement.getId(), agreement);
        }

        long escrowReviewCount = agreements.values().stream()
                .filter(agreement ->
                        agreement.getBlockchainEscrowId() != null
                                && !agreement.getBlockchainEscrowId().isBlank()
                )
                .count();

        addCountBlocker(
                blockers,
                escrowReviewCount,
                "ON_CHAIN_ESCROW_REQUIRES_VERIFICATION"
        );
    }

    private void checkSessions(
            UUID userId,
            List<String> blockers) {

        long count = serviceSessionRepository.countUnresolvedForUser(
                userId,
                List.of(
                        SessionStatus.CREATED,
                        SessionStatus.SCHEDULED,
                        SessionStatus.IN_PROGRESS,
                        SessionStatus.PROVIDER_CONFIRMED,
                        SessionStatus.DISPUTED
                )
        );

        addCountBlocker(
                blockers,
                count,
                "UNRESOLVED_SESSIONS"
        );
    }

    private void checkWalletAndBlockchain(
            UUID userId,
            List<String> blockers) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElse(null);

        if (wallet == null) {
            blockers.add(
                    "WALLET_MISSING: the account's wallet requires investigation."
            );
            return;
        }

        Integer availableTokens = wallet.getAvailableTokens();
        Integer lockedTokens = wallet.getLockedTokens();

        if (availableTokens == null || availableTokens != 0) {
            blockers.add(
                    "DATABASE_TOKEN_BALANCE_UNRESOLVED: available token balance "
                            + "is not exactly zero. No automatic recovery will occur."
            );
        }

        if (lockedTokens == null || lockedTokens != 0) {
            blockers.add(
                    "LOCKED_TOKENS_UNRESOLVED: locked token balance "
                            + "is not exactly zero."
            );
        }

        long pendingTransactions =
                blockchainTransactionRepository.countByWalletAndStatusIn(
                        wallet,
                        List.of(
                                BlockchainTransactionStatus.REQUESTED,
                                BlockchainTransactionStatus.SUBMITTED
                        )
                );

        addCountBlocker(
                blockers,
                pendingTransactions,
                "PENDING_BLOCKCHAIN_TRANSACTIONS"
        );

        String blockchainAddress = wallet.getBlockchainAddress();

        if (blockchainAddress == null || blockchainAddress.isBlank()) {
            blockers.add(
                    "ON_CHAIN_BALANCE_UNVERIFIED: the wallet has no blockchain address."
            );
            return;
        }

        try {
            BigInteger onChainBalance =
                    blockchainTokenService.getBalance(blockchainAddress);

            if (onChainBalance == null || onChainBalance.signum() < 0) {
                blockers.add(
                        "ON_CHAIN_BALANCE_UNVERIFIED: the returned balance is invalid."
                );
            } else if (onChainBalance.signum() > 0) {
                blockers.add(
                        "ON_CHAIN_BLT_BALANCE_NONZERO: tokens remain at the "
                                + "blockchain address; manual policy and reconciliation "
                                + "are required before deletion."
                );
            }

        } catch (RuntimeException exception) {
            log.warn(
                    "Could not verify on-chain balance for user {}.",
                    userId,
                    exception
            );

            blockers.add(
                    "ON_CHAIN_BALANCE_UNVERIFIED: the blockchain balance could "
                            + "not be verified. Deletion must remain blocked."
            );
        }
    }

    private void addCountBlocker(
            List<String> blockers,
            long count,
            String code) {

        if (count > 0) {
            blockers.add(code + ": " + count + " record(s) require resolution.");
        }
    }

    public record DeletionSafetyReport(
            UUID userId,
            Instant lastActiveAt,
            Instant inactivityDeadline,
            boolean deadlineReached,
            boolean eligibleForDeletion,
            List<String> blockers
    ) {
    }
}