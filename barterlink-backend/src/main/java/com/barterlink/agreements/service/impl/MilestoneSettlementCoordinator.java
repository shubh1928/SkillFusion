
package com.barterlink.agreements.service.impl;

import com.barterlink.blockchain.escrow.BlockchainEscrowTransactionService;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionStatus;
import com.barterlink.blockchain.transaction.BlockchainTransactionTrackingService;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Numeric;
import com.barterlink.sessions.service.SessionService;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneSettlementCoordinator {

    private static final BigInteger ESCROW_LOCKED =
            BigInteger.ONE;

    private static final BigInteger ESCROW_RELEASED =
            BigInteger.valueOf(2);

    private static final BigInteger ESCROW_REFUNDED =
            BigInteger.valueOf(3);

    private static final BigInteger WEI_PER_BLT =
            BigInteger.TEN.pow(18);

    private final MilestoneCompletionStateService stateService;
    private final BlockchainEscrowTransactionService escrowService;
    private final BlockchainTransactionTrackingService trackingService;
    private final SubsequentMilestoneFundingCoordinator subsequentFundingCoordinator;
    private final SessionService sessionService;

    /**
     * Coordinates one milestone release.
     *
     * Database transactions are deliberately not kept open
     * while waiting for blockchain confirmation.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SettlementResult settleConfirmedMilestone(
            MilestoneCompletionStateService.ConfirmationDecision decision) {

        validateDecision(decision);

        try {
            BlockchainEscrowTransactionService.EscrowDetails escrow =
                    escrowService.getEscrow(decision.escrowId());

            /*
             * The blockchain is authoritative. If it already shows RELEASED,
             * verify the escrow details and reconcile the SQL wallet mirror.
             */
            if (ESCROW_RELEASED.equals(escrow.status())) {
                verifyEscrowDetails(escrow, decision);

                return recordReleaseAndAdvance(decision);
            }

            /*
             * Never submit a release for a missing, refunded, or unknown
             * escrow state.
             */
            if (BigInteger.ZERO.equals(escrow.status())) {
                return pending(
                        "The milestone escrow was not found on-chain. "
                                + "Settlement requires reconciliation.");
            }

            if (ESCROW_REFUNDED.equals(escrow.status())) {
                return pending(
                        "The milestone escrow is refunded on-chain. "
                                + "Settlement requires reconciliation.");
            }

            if (!ESCROW_LOCKED.equals(escrow.status())) {
                return pending(
                        "The milestone escrow has an unexpected on-chain state. "
                                + "Settlement requires reconciliation.");
            }

            verifyEscrowDetails(escrow, decision);

            /*
             * Inspect every recorded RELEASE transaction for this escrow.
             * Any previous attempt blocks an automatic second submission.
             */
            List<BlockchainTransaction> releaseTransactions =
                    trackingService.getTransactionsByEscrowIdAndType(
                            decision.escrowId(),
                            BlockchainTransactionType.RELEASE);

            if (!releaseTransactions.isEmpty()) {
                return pending(
                        "A release transaction is already recorded. "
                                + "The outcome must be reconciled before another attempt.");
            }

            /*
             * Atomically claim the first release attempt in the database.
             * Concurrent callers cannot both claim it.
             */
            boolean claimed =
                    stateService.claimReleaseAttempt(
                            decision.milestoneId());

            if (!claimed) {
                /*
                 * Another request may have claimed the attempt while we
                 * were checking the chain. Re-read chain state, but do not
                 * submit another release here.
                 */
                BlockchainEscrowTransactionService.EscrowDetails latest =
                        escrowService.getEscrow(decision.escrowId());

                if (ESCROW_RELEASED.equals(latest.status())) {
                    verifyEscrowDetails(latest, decision);

                    return recordReleaseAndAdvance(decision);
                }

                return pending(
                        "A release attempt has already been claimed. "
                                + "No duplicate transaction was submitted.");
            }

            /*
             * Exactly one application request can reach this submission
             * path for the milestone. The blockchain service waits for a
             * receipt and records its transaction history.
             */
            try {
                escrowService.sendRelease(decision.escrowId());

            } catch (RuntimeException submissionFailure) {

                log.warn(
                        "Milestone release returned an error. Agreement={}, "
                                + "milestone={}, escrow={}. Checking on-chain state; "
                                + "no automatic retry will be performed.",
                        decision.agreementId(),
                        decision.milestoneId(),
                        decision.escrowId(),
                        submissionFailure);

                /*
                 * A timeout or RPC error does not establish that the
                 * transaction failed. A successful on-chain release can
                 * still be reconciled below.
                 */
            }

            /*
             * Verify the resulting state independently. A successful
             * method return alone is not used as proof of settlement.
             */
            BlockchainEscrowTransactionService.EscrowDetails afterAttempt =
                    escrowService.getEscrow(decision.escrowId());

            verifyEscrowDetails(afterAttempt, decision);

            if (!ESCROW_RELEASED.equals(afterAttempt.status())) {
                return pending(
                        "The release outcome is not yet confirmed on-chain. "
                                + "The milestone remains pending; no automatic retry was performed.");
            }

            return recordReleaseAndAdvance(decision);

        } catch (RuntimeException exception) {

            log.error(
                    "Milestone settlement remains unresolved. Agreement={}, "
                            + "milestone={}, escrow={}. No automatic retry was performed.",
                    decision.agreementId(),
                    decision.milestoneId(),
                    decision.escrowId(),
                    exception);

            /*
             * Preserve the pending state when verification or reconciliation
             * fails. A later request must check the chain and transaction
             * history again; it must not blindly submit another release.
             */
            return pending(
                    "Milestone settlement could not be verified. "
                            + "The escrow and transaction history require reconciliation.");
        }
    }


    /**
     * Records the verified release first, then attempts to fund the next
     * milestone outside that database settlement transaction.
     */
    private SettlementResult recordReleaseAndAdvance(
            MilestoneCompletionStateService.ConfirmationDecision decision) {

        /*
         * Record the verified blockchain release and synchronize the
         * application wallet before attempting the next stage.
         */
        stateService.markMilestoneReleasedAndTransfer(decision);

        SubsequentMilestoneFundingCoordinator.FundingResult funding;

        /*
         * First handle next-milestone funding separately.
         */
        try {
            funding =
                    subsequentFundingCoordinator.fundNextMilestone(
                            decision.agreementId());

        } catch (RuntimeException exception) {

            log.error(
                    "Milestone {} was released, but the next-stage "
                            + "funding check failed. Agreement={}.",
                    decision.milestoneNumber(),
                    decision.agreementId(),
                    exception);

            return new SettlementResult(
                    true,
                    "Milestone " + decision.milestoneNumber()
                            + " was released. The next stage could not be "
                            + "processed and requires a funding retry "
                            + "or reconciliation.");
        }

        if (funding.milestoneFunded()) {
            return new SettlementResult(
                    true,
                    "Milestone " + decision.milestoneNumber()
                            + " was released and the next milestone is funded.");
        }

        /*
         * If every milestone is already released, finalize the agreement
         * and session. This has its own error handling.
         */
        if (funding.allMilestonesReleased()) {

            try {
                boolean finalized =
                        sessionService.completeMilestoneSessionIfFinal(
                                decision.agreementId());

                if (finalized) {
                    return new SettlementResult(
                            true,
                            "All milestones have been released. "
                                    + "The agreement and session are completed.");
                }

                return new SettlementResult(
                        true,
                        "The last milestone was released, but final session "
                                + "completion remains pending. Retry finalization "
                                + "or perform reconciliation.");

            } catch (RuntimeException exception) {

                log.error(
                        "All milestones appear released, but final session "
                                + "completion failed. Agreement={}.",
                        decision.agreementId(),
                        exception);

                return new SettlementResult(
                        true,
                        "The last milestone release was confirmed, but final "
                                + "session completion failed. Use the requester "
                                + "funding-retry endpoint to retry finalization.");
            }
        }

        return new SettlementResult(
                true,
                "Milestone " + decision.milestoneNumber()
                        + " was released, but the next milestone is not funded yet. "
                        + funding.message());
    }

    private void validateDecision(
            MilestoneCompletionStateService.ConfirmationDecision decision) {

        if (decision == null
                || decision.milestoneId() == null
                || decision.agreementId() == null
                || decision.sessionId() == null
                || decision.escrowId() == null
                || decision.escrowId().isBlank()
                || decision.tokenAllocation() == null
                || decision.tokenAllocation() <= 0
                || decision.requesterId() == null
                || decision.providerId() == null
                || decision.requesterAddress() == null
                || decision.providerAddress() == null
                || decision.agreementIdBytes32() == null
                || decision.sessionIdBytes32() == null) {

            throw new IllegalArgumentException(
                    "Complete milestone settlement details are required");
        }

        if (!decision.requesterConfirmed()
                || !decision.providerConfirmed()) {
            throw new IllegalArgumentException(
                    "Both participants must confirm milestone completion");
        }
    }

    private void verifyEscrowDetails(
            BlockchainEscrowTransactionService.EscrowDetails escrow,
            MilestoneCompletionStateService.ConfirmationDecision decision) {

        if (escrow == null) {
            throw new IllegalStateException(
                    "Blockchain did not return escrow details");
        }

        BigInteger expectedAmount =
                BigInteger.valueOf(decision.tokenAllocation())
                        .multiply(WEI_PER_BLT);

        if (escrow.amount() == null
                || !expectedAmount.equals(escrow.amount())) {
            throw new IllegalStateException(
                    "On-chain escrow amount does not match the milestone allocation");
        }

        if (escrow.requester() == null
                || !escrow.requester().equalsIgnoreCase(
                decision.requesterAddress())) {
            throw new IllegalStateException(
                    "On-chain escrow requester does not match the agreement");
        }

        if (escrow.provider() == null
                || !escrow.provider().equalsIgnoreCase(
                decision.providerAddress())) {
            throw new IllegalStateException(
                    "On-chain escrow provider does not match the agreement");
        }

        if (!sameHex(
                escrow.agreementId(),
                decision.agreementIdBytes32())) {
            throw new IllegalStateException(
                    "On-chain escrow agreement ID does not match");
        }

        if (!sameHex(
                escrow.sessionId(),
                decision.sessionIdBytes32())) {
            throw new IllegalStateException(
                    "On-chain escrow session ID does not match");
        }
    }

    private boolean sameHex(String actual, String expected) {
        return actual != null
                && expected != null
                && Numeric.cleanHexPrefix(actual)
                .equalsIgnoreCase(
                        Numeric.cleanHexPrefix(expected));
    }

    private SettlementResult pending(String message) {
        return new SettlementResult(false, message);
    }

    public record SettlementResult(
            boolean settled,
            String message) {
    }
}
