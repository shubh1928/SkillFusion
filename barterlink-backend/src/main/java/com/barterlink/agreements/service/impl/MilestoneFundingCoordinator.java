package com.barterlink.agreements.service.impl;

import com.barterlink.blockchain.escrow.BlockchainEscrowTransactionService;
import com.barterlink.blockchain.permit.BlockchainPermitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneFundingCoordinator {

    private final MilestoneFundingPreparationService preparationService;
    private final BlockchainEscrowTransactionService escrowTransactionService;
    private final BlockchainPermitService permitService;

    /*
     * This method is called after the acceptance transaction commits.
     * Do not place the database preparation and blockchain request
     * inside one long-running SQL transaction.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void fundPreparedMilestone(
            MilestoneFundingPreparationService.PreparedMilestoneFunding prepared) {

        BigInteger blockchainAmount =
                BigInteger.valueOf(prepared.tokenAllocation())
                        .multiply(BigInteger.TEN.pow(18));

        try {
            BlockchainEscrowTransactionService.EscrowDetails current =
                    escrowTransactionService.getEscrow(
                            prepared.escrowId());

            if (BigInteger.ONE.equals(current.status())) {

                /*
                 * The deterministic escrow is already LOCKED.
                 * Verify its actual details and complete the database
                 * activation; never submit a duplicate lock.
                 */
                verifyEscrow(current, prepared, blockchainAmount);

                preparationService.markFundedAndActivate(prepared);
                return;
            }

            if (!BigInteger.ZERO.equals(current.status())) {
                throw new IllegalStateException(
                        "The milestone escrow is not in the NONE state; "
                                + "automatic submission is blocked");
            }

            BigInteger permitDeadline =
                    BigInteger.valueOf(
                                    System.currentTimeMillis() / 1000)
                            .add(BigInteger.valueOf(3600));

            try {
                BlockchainPermitService.PermitSignature permit =
                        permitService.createEscrowPermit(
                                prepared.requesterId(),
                                blockchainAmount,
                                permitDeadline);

                escrowTransactionService.sendLockWithPermit(
                        prepared.escrowId(),
                        prepared.requesterAddress(),
                        prepared.providerAddress(),
                        blockchainAmount,
                        prepared.agreementIdBytes32(),
                        prepared.sessionIdBytes32(),
                        permit);

            } catch (RuntimeException submissionException) {

                /*
                 * A submission error or timeout does not always prove
                 * that no transaction reached the network. Inspect the
                 * chain below, but never retry automatically.
                 */
                log.warn(
                        "Initial milestone lock returned an error for "
                                + "agreement {} and escrow {}. "
                                + "Checking the actual on-chain state.",
                        prepared.agreementId(),
                        prepared.escrowId(),
                        submissionException);
            }

            BlockchainEscrowTransactionService.EscrowDetails confirmed =
                    escrowTransactionService.getEscrow(
                            prepared.escrowId());

            verifyEscrow(
                    confirmed,
                    prepared,
                    blockchainAmount);

            preparationService.markFundedAndActivate(prepared);

            log.info(
                    "Initial milestone funded and version activated. "
                            + "Agreement={}, milestone={}, allocation={}",
                    prepared.agreementId(),
                    prepared.milestoneId(),
                    prepared.tokenAllocation());

        } catch (RuntimeException exception) {

            /*
             * Fail closed. Keep the milestone in FUNDING_PENDING and
             * keep its allocation reserved. Do not automatically refund
             * or resubmit when the on-chain outcome is uncertain.
             */
            log.error(
                    "Initial milestone funding/activation remains unresolved. "
                            + "Agreement={}, milestone={}, escrow={}. "
                            + "The wallet allocation remains reserved. "
                            + "Manual reconciliation is required; "
                            + "no automatic retry was performed.",
                    prepared.agreementId(),
                    prepared.milestoneId(),
                    prepared.escrowId(),
                    exception);
        }
    }

    private void verifyEscrow(
            BlockchainEscrowTransactionService.EscrowDetails escrow,
            MilestoneFundingPreparationService.PreparedMilestoneFunding prepared,
            BigInteger expectedAmount) {

        if (escrow == null
                || !BigInteger.ONE.equals(escrow.status())) {
            throw new IllegalStateException(
                    "The on-chain milestone escrow is not LOCKED");
        }

        if (escrow.amount() == null
                || !expectedAmount.equals(escrow.amount())) {
            throw new IllegalStateException(
                    "The on-chain escrow amount does not match milestone 1");
        }

        if (escrow.requester() == null
                || !prepared.requesterAddress()
                .equalsIgnoreCase(escrow.requester())) {
            throw new IllegalStateException(
                    "The on-chain escrow requester does not match");
        }

        if (escrow.provider() == null
                || !prepared.providerAddress()
                .equalsIgnoreCase(escrow.provider())) {
            throw new IllegalStateException(
                    "The on-chain escrow provider does not match");
        }

        if (!sameHex(
                escrow.agreementId(),
                prepared.agreementIdBytes32())) {
            throw new IllegalStateException(
                    "The on-chain agreement identifier does not match");
        }

        if (!sameHex(
                escrow.sessionId(),
                prepared.sessionIdBytes32())) {
            throw new IllegalStateException(
                    "The on-chain session identifier does not match");
        }
    }

    private boolean sameHex(String actual, String expected) {

        return actual != null
                && expected != null
                && Numeric.cleanHexPrefix(actual)
                .equalsIgnoreCase(
                        Numeric.cleanHexPrefix(expected));
    }
}