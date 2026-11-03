package com.barterlink.disputes.service;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.auth.entities.User;
import com.barterlink.blockchain.escrow.BlockchainEscrowTransactionService;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionStatus;
import com.barterlink.blockchain.transaction.BlockchainTransactionTrackingService;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeSettlementService {

    private static final BigInteger ESCROW_LOCKED = BigInteger.ONE;
    private static final BigInteger ESCROW_RELEASED = BigInteger.valueOf(2);
    private static final BigInteger ESCROW_REFUNDED = BigInteger.valueOf(3);
    private static final BigInteger WEI_PER_BLT = BigInteger.TEN.pow(18);

    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputeSettlementFinalizer finalizer;
    private final BlockchainEscrowTransactionService escrowService;
    private final BlockchainTransactionTrackingService trackingService;
    private final WalletRepository walletRepository;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SettlementResult settle(UUID disputeId) {
        try {
            DisputeCase disputeCase = loadCase(disputeId);

            if (disputeCase.getStatus() == DisputeCaseStatus.RESOLVED) {
                return new SettlementResult(true, "Dispute is already resolved.");
            }

            if (disputeCase.getStatus() != DisputeCaseStatus.DECIDED
                    && disputeCase.getStatus()
                    != DisputeCaseStatus.SETTLEMENT_PENDING) {
                return pending("The dispute has no settleable final panel decision.");
            }

            if (disputeCase.getDecisionOutcome() == null) {
                return pending("The dispute decision outcome is missing.");
            }

            if (disputeCase.getStatus() == DisputeCaseStatus.DECIDED) {
                finalizer.markSettlementPending(disputeId);
            }

            disputeCase = loadCase(disputeId);

            if (disputeCase.getStatus() == DisputeCaseStatus.RESOLVED) {
                return new SettlementResult(true, "Dispute is already resolved.");
            }

            if (disputeCase.getStatus()
                    != DisputeCaseStatus.SETTLEMENT_PENDING) {
                return pending("The dispute is not waiting for settlement.");
            }

            ServiceSession session = disputeCase.getSession();

            ServiceAgreement agreement =
                    session == null ? null : session.getAgreement();

            if (session == null || agreement == null) {
                return pending(
                        "Session or agreement is missing; manual reconciliation is required.");
            }

            if (session.getStatus() != SessionStatus.DISPUTED
                    || agreement.getStatus() != AgreementStatus.DISPUTED) {
                return pending(
                        "The session and agreement are not both in DISPUTED state.");
            }

            if (disputeCase.getMilestone() != null) {
                return pending(
                        "Milestone-specific disputes require their own settlement workflow.");
            }

            if (agreement.getExchangeMode() == ExchangeMode.DIRECT_BARTER) {
                if (disputeCase.isSettlementAttemptClaimed()) {
                    return pending(
                            "A token settlement claim exists for this Direct Barter case.");
                }

                finalizer.resolveDirectBarter(disputeId);

                return new SettlementResult(
                        true,
                        "Direct Barter dispute resolved without a token transfer.");
            }

            if (agreement.getExchangeMode() != ExchangeMode.TOKEN_BASED
                    && agreement.getExchangeMode() != ExchangeMode.HYBRID) {
                return pending(
                        "The exchange mode is not supported by this settlement path.");
            }

            // Do not enable general dispute settlement for Milestone Release.
            if (agreement.getReleaseMode()
                    == AgreementReleaseMode.MILESTONE_RELEASE) {
                return pending(
                        "General dispute settlement is disabled for Milestone Release agreements.");
            }

            if (agreement.getReleaseMode()
                    != AgreementReleaseMode.DIRECT_RELEASE) {
                return pending(
                        "The agreement does not use a supported direct escrow release mode.");
            }

            String escrowId = agreement.getBlockchainEscrowId();
            Integer tokenAmount = agreement.getAgreedTokenAmount();

            if (escrowId == null || escrowId.isBlank()
                    || tokenAmount == null || tokenAmount <= 0) {
                return pending("Escrow ID or agreed token amount is missing.");
            }

            if (agreement.getEscrowLockedTokens() == null
                    || !tokenAmount.equals(agreement.getEscrowLockedTokens())) {
                return pending(
                        "Application escrow lock amount does not match the agreement; manual reconciliation is required.");
            }

            /*
             * A claimed case is verification-only. It can never return
             * to the new-submission path.
             */
            if (disputeCase.isSettlementAttemptClaimed()) {
                return reconcileClaimedSettlement(disputeId);
            }

            BlockchainEscrowTransactionService.EscrowDetails beforeAttempt =
                    escrowService.getEscrow(escrowId);

            verifyEscrowDetails(beforeAttempt, agreement, session, tokenAmount);

            if (!ESCROW_LOCKED.equals(beforeAttempt.status())) {
                return pending(
                        "Escrow is not LOCKED on-chain. No new transaction or wallet update was attempted; manual reconciliation is required.");
            }

            List<BlockchainTransaction> priorReleases =
                    trackingService.getTransactionsByEscrowIdAndType(
                            escrowId, BlockchainTransactionType.RELEASE);

            List<BlockchainTransaction> priorRefunds =
                    trackingService.getTransactionsByEscrowIdAndType(
                            escrowId, BlockchainTransactionType.REFUND);

            if (!priorReleases.isEmpty() || !priorRefunds.isEmpty()) {
                return pending(
                        "A release/refund attempt already exists for this escrow. No duplicate transaction was submitted.");
            }

            DisputeSettlementFinalizer.SettlementClaimResult claimResult =
                    finalizer.claimSettlement(disputeId);

            if (claimResult
                    == DisputeSettlementFinalizer.SettlementClaimResult.ALREADY_RESOLVED) {
                return new SettlementResult(true, "Dispute is already resolved.");
            }

            if (claimResult
                    == DisputeSettlementFinalizer.SettlementClaimResult.ALREADY_CLAIMED) {
                return reconcileClaimedSettlement(disputeId);
            }

            boolean refundRequester =
                    disputeCase.getDecisionOutcome()
                            == DisputeDecisionOutcome.REQUESTER_FAVORED;

            boolean claimReleasedForRetry = false;

            try {
                if (refundRequester) {
                    escrowService.sendRefund(escrowId);
                } else {
                    escrowService.sendRelease(escrowId);
                }
            } catch (RuntimeException submissionOrReceiptFailure) {

                log.warn(
                        "Dispute transaction returned an error. "
                                + "Checking whether a safe pre-submission recovery is possible. "
                                + "disputeId={}, escrowId={}",
                        disputeId,
                        escrowId,
                        submissionOrReceiptFailure
                );

                claimReleasedForRetry =
                        releaseClaimAfterVerifiedPreSubmissionFailure(
                                disputeId,
                                escrowId,
                                agreement,
                                session,
                                tokenAmount
                        );
            }

            if (claimReleasedForRetry) {
                return pending(
                        "No release/refund attempt was found, the original confirmed escrow lock "
                                + "was verified, and the escrow remains LOCKED. "
                                + "The claim was released for a safe retry."
                );
            }

            return reconcileClaimedSettlement(disputeId);

        } catch (RuntimeException exception) {
            log.error(
                    "Dispute settlement remains pending. No automatic duplicate transaction will be submitted. disputeId={}",
                    disputeId,
                    exception);

            return pending(
                    "Dispute settlement could not be verified or synchronized. It remains pending for reconciliation.");
        }
    }


    private boolean releaseClaimAfterVerifiedPreSubmissionFailure(
            UUID disputeId,
            String escrowId,
            ServiceAgreement agreement,
            ServiceSession session,
            Integer tokenAmount) {

        try {
            /*
             * A release or refund record means the operation may already
             * have reached the blockchain. Preserve the claim in that case.
             */
            List<BlockchainTransaction> releases =
                    trackingService.getTransactionsByEscrowIdAndType(
                            escrowId,
                            BlockchainTransactionType.RELEASE
                    );

            List<BlockchainTransaction> refunds =
                    trackingService.getTransactionsByEscrowIdAndType(
                            escrowId,
                            BlockchainTransactionType.REFUND
                    );

            if (!releases.isEmpty() || !refunds.isEmpty()) {
                log.warn(
                        "Keeping settlement claim because a release/refund record exists. "
                                + "disputeId={}, escrowId={}",
                        disputeId,
                        escrowId
                );
                return false;
            }

            /*
             * Verify that the original escrow lock has a confirmed,
             * correctly tracked transaction.
             */
            BlockchainTransaction lockTransaction =
                    trackingService.getTransactionByEscrowIdAndType(
                            escrowId,
                            BlockchainTransactionType.LOCK
                    );

            BigInteger expectedAmount =
                    BigInteger.valueOf(tokenAmount).multiply(WEI_PER_BLT);

            if (lockTransaction.getStatus()
                    != BlockchainTransactionStatus.CONFIRMED
                    || !expectedAmount.equals(lockTransaction.getAmount())
                    || lockTransaction.getTransactionHash() == null
                    || lockTransaction.getTransactionHash().isBlank()) {

                log.warn(
                        "Keeping settlement claim because the original escrow lock "
                                + "is not verifiably confirmed. disputeId={}, escrowId={}",
                        disputeId,
                        escrowId
                );
                return false;
            }

            /*
             * A missing receipt or RPC problem must never be interpreted as
             * proof that the escrow has not changed.
             */
            BlockchainEscrowTransactionService.EscrowDetails currentEscrow =
                    escrowService.getEscrow(escrowId);

            verifyEscrowDetails(
                    currentEscrow,
                    agreement,
                    session,
                    tokenAmount
            );

            if (!ESCROW_LOCKED.equals(currentEscrow.status())) {
                log.warn(
                        "Keeping settlement claim because the escrow is not LOCKED. "
                                + "disputeId={}, escrowId={}, onChainStatus={}",
                        disputeId,
                        escrowId,
                        currentEscrow.status()
                );
                return false;
            }

            return finalizer.releaseClaimAfterSafePreSubmissionFailure(
                    disputeId
            );

        } catch (RuntimeException verificationFailure) {
            /*
             * If any part of the verification fails, leave the claim set.
             * That prevents an automatic duplicate transaction.
             */
            log.warn(
                    "Could not verify a safe pre-submission recovery. "
                            + "Keeping settlement claim. disputeId={}, escrowId={}",
                    disputeId,
                    escrowId,
                    verificationFailure
            );

            return false;
        }
    }

    /**
     * Verifies a previous attempt. This method never submits a transaction.
     */
    private SettlementResult reconcileClaimedSettlement(UUID disputeId) {
        DisputeCase disputeCase = loadCase(disputeId);

        if (disputeCase.getStatus() == DisputeCaseStatus.RESOLVED) {
            return new SettlementResult(true, "Dispute is already resolved.");
        }

        if (disputeCase.getStatus() != DisputeCaseStatus.SETTLEMENT_PENDING
                || !disputeCase.isSettlementAttemptClaimed()) {
            return pending(
                    "There is no claimed token settlement to reconcile.");
        }

        if (disputeCase.getDecisionOutcome() == null) {
            return pending("The final panel decision is missing.");
        }

        ServiceSession session = disputeCase.getSession();

        ServiceAgreement agreement =
                session == null ? null : session.getAgreement();

        if (session == null || agreement == null
                || session.getStatus() != SessionStatus.DISPUTED
                || agreement.getStatus() != AgreementStatus.DISPUTED) {
            return pending(
                    "The session and agreement are not both disputed; manual reconciliation is required.");
        }

        if (disputeCase.getMilestone() != null
                || (agreement.getExchangeMode() != ExchangeMode.TOKEN_BASED
                && agreement.getExchangeMode() != ExchangeMode.HYBRID)
                || agreement.getReleaseMode()
                != AgreementReleaseMode.DIRECT_RELEASE) {
            return pending(
                    "This settlement is outside the supported non-milestone token escrow path.");
        }

        String escrowId = agreement.getBlockchainEscrowId();
        Integer tokenAmount = agreement.getAgreedTokenAmount();

        if (escrowId == null || escrowId.isBlank()
                || tokenAmount == null || tokenAmount <= 0
                || agreement.getEscrowLockedTokens() == null
                || !tokenAmount.equals(agreement.getEscrowLockedTokens())) {
            return pending(
                    "Escrow details or locked-token accounting do not match; manual reconciliation is required.");
        }

        boolean refundRequester =
                disputeCase.getDecisionOutcome()
                        == DisputeDecisionOutcome.REQUESTER_FAVORED;

        BlockchainEscrowTransactionService.EscrowDetails escrow =
                escrowService.getEscrow(escrowId);

        verifyEscrowDetails(escrow, agreement, session, tokenAmount);

        BigInteger expectedFinalState = refundRequester
                ? ESCROW_REFUNDED
                : ESCROW_RELEASED;

        if (!expectedFinalState.equals(escrow.status())) {
            return pending(
                    "The previously claimed transaction has not reached the expected final on-chain state. No new transaction was submitted.");
        }

        BlockchainTransactionType expectedType = refundRequester
                ? BlockchainTransactionType.REFUND
                : BlockchainTransactionType.RELEASE;

        BlockchainTransactionType oppositeType = refundRequester
                ? BlockchainTransactionType.RELEASE
                : BlockchainTransactionType.REFUND;

        List<BlockchainTransaction> expectedTransactions =
                trackingService.getTransactionsByEscrowIdAndType(
                        escrowId, expectedType);

        List<BlockchainTransaction> oppositeTransactions =
                trackingService.getTransactionsByEscrowIdAndType(
                        escrowId, oppositeType);

        if (!oppositeTransactions.isEmpty()) {
            return pending(
                    "An opposite release/refund transaction exists for this escrow. Manual reconciliation is required.");
        }

        BigInteger expectedAmount =
                BigInteger.valueOf(tokenAmount).multiply(WEI_PER_BLT);

        if (!hasConfirmedTrackedAttempt(
                expectedTransactions,
                escrowId,
                expectedType,
                expectedAmount)) {
            return pending(
                    "The final escrow state does not match exactly one confirmed tracking record. Wallet reconciliation was not attempted.");
        }

        finalizer.resolveTokenEscrow(
                disputeId, tokenAmount, refundRequester);

        return new SettlementResult(
                true,
                refundRequester
                        ? "Escrow refund verified and reconciled; agreement marked REFUNDED and session cancelled."
                        : "Escrow release verified and reconciled; agreement and session marked completed.");
    }

    private boolean hasConfirmedTrackedAttempt(
            List<BlockchainTransaction> transactions,
            String escrowId,
            BlockchainTransactionType expectedType,
            BigInteger expectedAmount) {

        if (transactions == null || transactions.size() != 1) {
            return false;
        }

        BlockchainTransaction transaction = transactions.get(0);

        return transaction.getStatus()
                    == BlockchainTransactionStatus.CONFIRMED
                && transaction.getTransactionType() == expectedType
                && escrowId.equalsIgnoreCase(transaction.getEscrowId())
                && expectedAmount.equals(transaction.getAmount())
                && transaction.getTransactionHash() != null
                && !transaction.getTransactionHash().isBlank();
    }

    private DisputeCase loadCase(UUID disputeId) {
        if (disputeId == null) {
            throw new IllegalArgumentException("Dispute ID is required");
        }

        return disputeCaseRepository.findForSettlementById(disputeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Dispute case not found"));
    }

    private void verifyEscrowDetails(
            BlockchainEscrowTransactionService.EscrowDetails escrow,
            ServiceAgreement agreement,
            ServiceSession session,
            Integer tokenAmount) {

        if (escrow == null
                || escrow.amount() == null
                || escrow.status() == null) {
            throw new IllegalStateException(
                    "Blockchain did not return complete escrow details");
        }

        BigInteger expectedAmount =
                BigInteger.valueOf(tokenAmount).multiply(WEI_PER_BLT);

        if (!expectedAmount.equals(escrow.amount())) {
            throw new IllegalStateException(
                    "On-chain escrow amount does not match the agreement token amount");
        }

        User requester = agreement.getRequester();
        User provider = agreement.getProvider();

        if (requester == null || provider == null) {
            throw new IllegalStateException(
                    "Agreement participants are missing");
        }

        Wallet requesterWallet =
                walletRepository.findByUserId(requester.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Requester wallet not found"));

        Wallet providerWallet =
                walletRepository.findByUserId(provider.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider wallet not found"));

        requireAddress(
                escrow.requester(),
                requesterWallet.getBlockchainAddress(),
                "requester");

        requireAddress(
                escrow.provider(),
                providerWallet.getBlockchainAddress(),
                "provider");

        if (!sameHex(
                escrow.agreementId(),
                toBytes32Uuid(agreement.getId()))) {
            throw new IllegalStateException(
                    "On-chain escrow agreement ID does not match");
        }

        if (!sameHex(
                escrow.sessionId(),
                toBytes32Uuid(session.getId()))) {
            throw new IllegalStateException(
                    "On-chain escrow session ID does not match");
        }
    }

    private void requireAddress(
            String actual,
            String expected,
            String participant) {

        if (actual == null
                || expected == null
                || expected.isBlank()
                || !actual.equalsIgnoreCase(expected)) {
            throw new IllegalStateException(
                    "On-chain escrow " + participant
                            + " address does not match the agreement wallet");
        }
    }

    private String toBytes32Uuid(UUID id) {
        if (id == null) {
            throw new IllegalStateException("Agreement/session ID is missing");
        }

        return "0x" + "0".repeat(32)
                + id.toString().replace("-", "");
    }

    private boolean sameHex(String actual, String expected) {
        return actual != null
                && expected != null
                && Numeric.cleanHexPrefix(actual)
                        .equalsIgnoreCase(Numeric.cleanHexPrefix(expected));
    }

    private SettlementResult pending(String message) {
        log.warn("Dispute settlement pending: {}", message);
        return new SettlementResult(false, message);
    }

    public record SettlementResult(boolean settled, String message) {
    }
}