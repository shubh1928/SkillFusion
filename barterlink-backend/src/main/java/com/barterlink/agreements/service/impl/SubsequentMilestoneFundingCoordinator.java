
package com.barterlink.agreements.service.impl;

import com.barterlink.agreements.service.impl.MilestoneFundingPreparationService.PreparedMilestoneFunding;
import com.barterlink.blockchain.escrow.BlockchainEscrowTransactionService;
import com.barterlink.blockchain.permit.BlockchainPermitService;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionTrackingService;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubsequentMilestoneFundingCoordinator {

    private static final BigInteger ESCROW_NONE =
            BigInteger.ZERO;

    private static final BigInteger ESCROW_LOCKED =
            BigInteger.ONE;

    private static final BigInteger WEI_PER_BLT =
            BigInteger.TEN.pow(18);

    private final MilestoneFundingPreparationService preparationService;
    private final MilestoneCompletionStateService completionStateService;
    private final BlockchainEscrowTransactionService escrowService;
    private final BlockchainPermitService permitService;
    private final BlockchainTransactionTrackingService trackingService;

    /**
     * Prepare and fund the next milestone.
     *
     * This method does not keep a SQL transaction open while
     * waiting for blockchain operations.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FundingResult fundNextMilestone(UUID agreementId) {

        Optional<PreparedMilestoneFunding> optional =
                preparationService.prepareNextMilestoneFunding(
                        agreementId);

        if (optional.isEmpty()) {
            return new FundingResult(
                    false,
                    true,
                    "All milestones are released. "
                            + "Agreement and session completion must now be finalized.");
        }

        PreparedMilestoneFunding prepared = optional.get();

        try {
            BlockchainEscrowTransactionService.EscrowDetails current =
                    escrowService.getEscrow(prepared.escrowId());

            /*
             * If a previous lock transaction succeeded on-chain,
             * reconcile the database instead of submitting another lock.
             */
            if (ESCROW_LOCKED.equals(current.status())) {
                verifyEscrowDetails(current, prepared);

                completionStateService.claimFundingAttempt(
                        prepared.milestoneId());

                preparationService.markNextMilestoneFunded(prepared);

                return new FundingResult(
                        true,
                        false,
                        "The next milestone escrow was verified and funded.");
            }

            /*
             * Never automatically submit for any state other than NONE.
             */
            if (!ESCROW_NONE.equals(current.status())) {
                return pending(
                        "The next milestone escrow is not in the NONE state. "
                                + "Funding requires reconciliation.");
            }

            /*
             * Any previous LOCK tracking record blocks automatic resubmission.
             * A FAILED tracking status alone does not prove that the
             * transaction never reached the blockchain.
             */
            List<BlockchainTransaction> previousLocks =
                    trackingService.getTransactionsByEscrowIdAndType(
                            prepared.escrowId(),
                            BlockchainTransactionType.LOCK);

            if (!previousLocks.isEmpty()) {
                return pending(
                        "A previous funding transaction is recorded. "
                                + "Its outcome must be reconciled before retrying.");
            }

            /*
             * Claim the one permitted submission attempt.
             * The row-locked database method prevents concurrent requests
             * from both submitting a lock transaction.
             */
            boolean claimed =
                    completionStateService.claimFundingAttempt(
                            prepared.milestoneId());

            if (!claimed) {
                return pending(
                        "A funding attempt was already claimed. "
                                + "No duplicate transaction was submitted.");
            }

            BigInteger blockchainAmount =
                    BigInteger.valueOf(prepared.tokenAllocation())
                            .multiply(WEI_PER_BLT);

            BigInteger permitDeadline =
                    BigInteger.valueOf(Instant.now().getEpochSecond())
                            .add(BigInteger.valueOf(3600));

            /*
             * Submit the blockchain lock only after the SQL preparation
             * and funding-attempt claim have committed.
             */
            try {
                BlockchainPermitService.PermitSignature permit =
                        permitService.createEscrowPermit(
                                prepared.requesterId(),
                                blockchainAmount,
                                permitDeadline);

                escrowService.sendLockWithPermit(
                        prepared.escrowId(),
                        prepared.requesterAddress(),
                        prepared.providerAddress(),
                        blockchainAmount,
                        prepared.agreementIdBytes32(),
                        prepared.sessionIdBytes32(),
                        permit);

            } catch (RuntimeException submissionException) {

                log.warn(
                        "Later milestone funding returned an error. "
                                + "Agreement={}, milestone={}, escrow={}. "
                                + "Checking on-chain state; no automatic retry.",
                        prepared.agreementId(),
                        prepared.milestoneId(),
                        prepared.escrowId(),
                        submissionException);
            }

            /*
             * A successful method return is not enough by itself.
             * Independently inspect the escrow after the attempt.
             */
            BlockchainEscrowTransactionService.EscrowDetails confirmed =
                    escrowService.getEscrow(prepared.escrowId());

            if (!ESCROW_LOCKED.equals(confirmed.status())) {
                return pending(
                        "The next milestone funding is not yet confirmed "
                                + "on-chain. It remains pending for reconciliation.");
            }

            verifyEscrowDetails(confirmed, prepared);

            preparationService.markNextMilestoneFunded(prepared);

            return new FundingResult(
                    true,
                    false,
                    "The next milestone escrow was verified and funded.");

        } catch (RuntimeException exception) {

            log.error(
                    "Later milestone funding remains unresolved. "
                            + "Agreement={}, milestone={}, escrow={}. "
                            + "No automatic retry was performed.",
                    prepared.agreementId(),
                    prepared.milestoneId(),
                    prepared.escrowId(),
                    exception);

            return pending(
                    "The next milestone funding could not be verified. "
                            + "The existing preparation remains pending "
                            + "until its blockchain outcome is reconciled.");
        }
    }

    private void verifyEscrowDetails(
            BlockchainEscrowTransactionService.EscrowDetails escrow,
            PreparedMilestoneFunding prepared) {

        if (escrow == null
                || !ESCROW_LOCKED.equals(escrow.status())) {
            throw new IllegalStateException(
                    "The milestone escrow is not LOCKED on-chain");
        }

        BigInteger expectedAmount =
                BigInteger.valueOf(prepared.tokenAllocation())
                        .multiply(WEI_PER_BLT);

        if (escrow.amount() == null
                || !expectedAmount.equals(escrow.amount())) {
            throw new IllegalStateException(
                    "On-chain escrow amount does not match the milestone allocation");
        }

        if (escrow.requester() == null
                || !prepared.requesterAddress()
                .equalsIgnoreCase(escrow.requester())) {
            throw new IllegalStateException(
                    "On-chain escrow requester does not match");
        }

        if (escrow.provider() == null
                || !prepared.providerAddress()
                .equalsIgnoreCase(escrow.provider())) {
            throw new IllegalStateException(
                    "On-chain escrow provider does not match");
        }

        if (!sameHex(
                escrow.agreementId(),
                prepared.agreementIdBytes32())) {
            throw new IllegalStateException(
                    "On-chain agreement ID does not match");
        }

        if (!sameHex(
                escrow.sessionId(),
                prepared.sessionIdBytes32())) {
            throw new IllegalStateException(
                    "On-chain session ID does not match");
        }
    }

    private boolean sameHex(String actual, String expected) {
        return actual != null
                && expected != null
                && Numeric.cleanHexPrefix(actual)
                .equalsIgnoreCase(
                        Numeric.cleanHexPrefix(expected));
    }

    private FundingResult pending(String message) {
        return new FundingResult(false, false, message);
    }

    public record FundingResult(
            boolean milestoneFunded,
            boolean allMilestonesReleased,
            String message) {
    }
}
