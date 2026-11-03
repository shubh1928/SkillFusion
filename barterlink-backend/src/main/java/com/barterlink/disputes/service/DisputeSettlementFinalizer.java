package com.barterlink.disputes.service;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeSettlementFinalizer {

    private final DisputeCaseRepository disputeCaseRepository;
    private final AgreementRepository agreementRepository;
    private final ServiceSessionRepository sessionRepository;
    private final WalletService walletService;

    @Transactional
    public void markSettlementPending(UUID disputeId) {
        DisputeCase disputeCase = lockCase(disputeId);

        if (disputeCase.getStatus() == DisputeCaseStatus.RESOLVED
                || disputeCase.getStatus()
                == DisputeCaseStatus.SETTLEMENT_PENDING) {
            return;
        }

        if (disputeCase.getStatus() != DisputeCaseStatus.DECIDED
                || disputeCase.getDecisionOutcome() == null) {
            throw new BusinessException(
                    "The dispute is not ready for settlement");
        }

        disputeCase.setStatus(DisputeCaseStatus.SETTLEMENT_PENDING);
        disputeCaseRepository.save(disputeCase);
    }

    /**
     * Atomically claims the one automatic token-settlement attempt.
     * The claim commits before the external blockchain call.
     */
    @Transactional
    public SettlementClaimResult claimSettlement(UUID disputeId) {
        DisputeCase disputeCase = lockCase(disputeId);

        if (disputeCase.getStatus() == DisputeCaseStatus.RESOLVED) {
            return SettlementClaimResult.ALREADY_RESOLVED;
        }

        if (disputeCase.getStatus()
                != DisputeCaseStatus.SETTLEMENT_PENDING
                || disputeCase.getDecisionOutcome() == null) {
            throw new BusinessException(
                    "The dispute is not waiting for settlement");
        }

        if (disputeCase.isSettlementAttemptClaimed()) {
            return SettlementClaimResult.ALREADY_CLAIMED;
        }

        disputeCase.setSettlementAttemptClaimed(true);
        disputeCaseRepository.save(disputeCase);

        return SettlementClaimResult.CLAIMED;
    }


    /**
     * Releases a settlement claim only when the caller has independently
     * verified that no release/refund attempt exists and the escrow remains
     * LOCKED on-chain.
     *
     * Never use this method for an uncertain blockchain submission.
     */
    @Transactional
    public boolean releaseClaimAfterSafePreSubmissionFailure(UUID disputeId) {

        DisputeCase disputeCase = lockCase(disputeId);

        if (disputeCase.getStatus()
                != DisputeCaseStatus.SETTLEMENT_PENDING
                || !disputeCase.isSettlementAttemptClaimed()
                || disputeCase.getDecisionOutcome() == null) {
            return false;
        }

        disputeCase.setSettlementAttemptClaimed(false);
        disputeCaseRepository.save(disputeCase);

        return true;
    }

    /**
     * Direct Barter has no token-escrow settlement.
     */
    @Transactional
    public void resolveDirectBarter(UUID disputeId) {
        DisputeCase disputeCase = lockCase(disputeId);

        if (disputeCase.getStatus() == DisputeCaseStatus.RESOLVED) {
            return;
        }

        requirePendingDispute(disputeCase);

        if (disputeCase.isSettlementAttemptClaimed()) {
            throw new BusinessException(
                    "A token settlement claim already exists");
        }

        if (disputeCase.getMilestone() != null) {
            throw new BusinessException(
                    "Milestone-specific disputes require their own workflow");
        }

        ServiceSession session = disputeCase.getSession();

        ServiceAgreement agreement =
                session == null ? null : session.getAgreement();

        requireDisputedPair(session, agreement);

        if (agreement.getExchangeMode() != ExchangeMode.DIRECT_BARTER) {
            throw new BusinessException(
                    "This settlement path is only for Direct Barter");
        }

        Instant now = Instant.now();

        boolean requesterFavored =
                disputeCase.getDecisionOutcome()
                        == DisputeDecisionOutcome.REQUESTER_FAVORED;

        if (requesterFavored) {
            agreement.setStatus(AgreementStatus.CANCELLED);
            session.setStatus(SessionStatus.CANCELLED);
            session.setCancelledAt(now);
        } else {
            agreement.setStatus(AgreementStatus.COMPLETED);
            agreement.setCompletedAt(now);

            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(now);
        }

        disputeCase.setStatus(DisputeCaseStatus.RESOLVED);

        agreementRepository.save(agreement);
        sessionRepository.save(session);
        disputeCaseRepository.save(disputeCase);
    }

    /**
     * Updates application balances only after the caller has verified
     * the escrow's final on-chain state and matching confirmed transaction.
     */
    @Transactional
    public void resolveTokenEscrow(
            UUID disputeId,
            Integer tokenAmount,
            boolean refundRequester) {

        DisputeCase disputeCase = lockCase(disputeId);

        if (disputeCase.getStatus() == DisputeCaseStatus.RESOLVED) {
            return;
        }

        requirePendingDispute(disputeCase);

        if (!disputeCase.isSettlementAttemptClaimed()) {
            throw new BusinessException(
                    "Token settlement has not been claimed");
        }

        if (disputeCase.getMilestone() != null) {
            throw new BusinessException(
                    "Milestone-specific disputes require their own workflow");
        }

        ServiceSession session = disputeCase.getSession();

        ServiceAgreement agreement =
                session == null ? null : session.getAgreement();

        requireDisputedPair(session, agreement);

        ExchangeMode exchangeMode = agreement.getExchangeMode();

        if ((exchangeMode != ExchangeMode.TOKEN_BASED
                && exchangeMode != ExchangeMode.HYBRID)
                || agreement.getReleaseMode()
                != AgreementReleaseMode.DIRECT_RELEASE) {
            throw new BusinessException(
                    "This settlement path only supports non-milestone token escrow");
        }

        if (tokenAmount == null
                || tokenAmount <= 0
                || !tokenAmount.equals(agreement.getAgreedTokenAmount())) {
            throw new BusinessException(
                    "Settlement amount does not match the agreement");
        }

        if (agreement.getEscrowLockedTokens() == null
                || !tokenAmount.equals(
                        agreement.getEscrowLockedTokens())) {
            throw new BusinessException(
                    "Application escrow lock amount does not match the settlement amount");
        }

        boolean requesterFavored =
                disputeCase.getDecisionOutcome()
                        == DisputeDecisionOutcome.REQUESTER_FAVORED;

        if (refundRequester != requesterFavored) {
            throw new BusinessException(
                    "Settlement action does not match the panel decision");
        }

        UUID requesterId = agreement.getRequester().getId();
        UUID providerId = agreement.getProvider().getId();

        String description = "Dispute " + disputeCase.getId()
                + " final panel settlement";

        Instant now = Instant.now();

        if (refundRequester) {
            walletService.refundTokens(
                    requesterId, tokenAmount, description);

            agreement.setStatus(AgreementStatus.REFUNDED);

            session.setStatus(SessionStatus.CANCELLED);
            session.setCancelledAt(now);
        } else {
            walletService.transferLockedTokens(
                    requesterId, providerId, tokenAmount, description);

            agreement.setStatus(AgreementStatus.COMPLETED);
            agreement.setCompletedAt(now);

            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(now);
        }

        agreement.setEscrowLockedTokens(0);
        disputeCase.setStatus(DisputeCaseStatus.RESOLVED);

        agreementRepository.save(agreement);
        sessionRepository.save(session);
        disputeCaseRepository.save(disputeCase);
    }

    private DisputeCase lockCase(UUID disputeId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID is required");
        }

        return disputeCaseRepository.findByIdForUpdate(disputeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Dispute case not found"));
    }

    private void requirePendingDispute(DisputeCase disputeCase) {
        if (disputeCase.getStatus()
                != DisputeCaseStatus.SETTLEMENT_PENDING) {
            throw new BusinessException(
                    "The dispute is not waiting for settlement");
        }

        if (disputeCase.getDecisionOutcome() == null) {
            throw new BusinessException(
                    "The dispute has no final panel decision");
        }
    }

    private void requireDisputedPair(
            ServiceSession session,
            ServiceAgreement agreement) {

        if (session == null
                || agreement == null
                || session.getStatus() != SessionStatus.DISPUTED
                || agreement.getStatus() != AgreementStatus.DISPUTED) {
            throw new BusinessException(
                    "The session and agreement must remain disputed during settlement");
        }
    }

    public enum SettlementClaimResult {
        CLAIMED,
        ALREADY_CLAIMED,
        ALREADY_RESOLVED
    }
}