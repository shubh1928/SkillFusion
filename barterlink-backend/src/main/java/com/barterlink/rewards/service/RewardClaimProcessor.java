package com.barterlink.rewards.service;

import com.barterlink.blockchain.token.BlockchainTokenService;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionRepository;
import com.barterlink.blockchain.transaction.BlockchainTransactionStatus;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import com.barterlink.rewards.enums.RewardClaimStatus;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardClaimProcessor {

    private final RewardClaimLifecycleService lifecycleService;
    private final BlockchainTokenService blockchainTokenService;
    private final BlockchainTransactionRepository
            blockchainTransactionRepository;
    private final WalletRepository walletRepository;

    public void process(UUID claimId) {
        RewardClaimLifecycleService.ClaimSnapshot claim =
                lifecycleService.getSnapshot(claimId);

        try {
            if (reconcileProcessedClaimKey(claim)) {
                return;
            }

            if (blockchainTokenService.isRewardCampaignPaused(
                    claim.campaign()
            )) {
                lifecycleService.deferBecauseCampaignPaused(claim.id());
                return;
            }

            List<BlockchainTransaction> previousAttempts =
                    new ArrayList<>(
                            blockchainTransactionRepository
                                    .findAllByRewardClaimKeyOrderByRequestedAtDesc(
                                            claim.claimKey()
                                    )
                    );

            if (!previousAttempts.isEmpty()) {

                Optional<BlockchainTransaction> mismatchedAttempt =
                        previousAttempts.stream()
                                .filter(attempt ->
                                        !isTransactionForClaim(
                                                attempt,
                                                claim.claimKey()
                                        )
                                )
                                .findFirst();

                if (mismatchedAttempt.isPresent()) {
                    lifecycleService.markReconciliationRequired(
                            claim.id(),
                            null,
                            "A previous transaction does not match this reward "
                                    + "claim's transaction type and claim key. "
                                    + "Automatic resubmission is blocked."
                    );
                    return;
                }

                Optional<BlockchainTransaction> confirmedAttempt =
                        previousAttempts.stream()
                                .filter(attempt ->
                                        attempt.getStatus()
                                                == BlockchainTransactionStatus.CONFIRMED
                                )
                                .findFirst();

                if (confirmedAttempt.isPresent()) {
                    BlockchainTransaction transaction =
                            confirmedAttempt.get();

                    if (transaction.getTransactionHash() == null
                            || transaction.getTransactionHash().isBlank()) {
                        lifecycleService.markReconciliationRequired(
                                claim.id(),
                                transaction.getId(),
                                "A previous reward transaction is marked "
                                        + "CONFIRMED but has no transaction hash. "
                                        + "Wallet settlement requires reconciliation."
                        );
                    } else {
                        confirmClaimAfterContractMapping(claim, transaction);
                    }

                    return;
                }

                Optional<BlockchainTransaction> activeAttempt =
                        previousAttempts.stream()
                                .filter(attempt ->
                                        attempt.getStatus()
                                                == BlockchainTransactionStatus.REQUESTED
                                                || attempt.getStatus()
                                                == BlockchainTransactionStatus.SUBMITTED
                                )
                                .findFirst();

                if (activeAttempt.isPresent()) {
                    BlockchainTransaction transaction =
                            activeAttempt.get();

                    if (transaction.getTransactionHash() == null
                            || transaction.getTransactionHash().isBlank()) {
                        lifecycleService.markReconciliationRequired(
                                claim.id(),
                                transaction.getId(),
                                "A previous reward transaction is still active "
                                        + "but has no recorded transaction hash. "
                                        + "A second mint is blocked."
                        );
                    } else {
                        lifecycleService.markSubmitted(
                                claim.id(),
                                transaction.getId()
                        );
                    }

                    return;
                }

                Optional<BlockchainTransaction> ambiguousFailure =
                        previousAttempts.stream()
                                .filter(attempt ->
                                        attempt.getStatus()
                                                == BlockchainTransactionStatus.FAILED
                                )
                                .filter(attempt ->
                                        !isDefinitivelyFailedAttempt(attempt)
                                )
                                .findFirst();

                if (ambiguousFailure.isPresent()) {
                    BlockchainTransaction transaction =
                            ambiguousFailure.get();

                    lifecycleService.markReconciliationRequired(
                            claim.id(),
                            transaction.getId(),
                            "A previous reward transaction is marked FAILED, "
                                    + "but its stored failure details do not "
                                    + "prove that another mint is safe. "
                                    + "Automatic resubmission is blocked."
                    );

                    return;
                }

                boolean allAttemptsFailedDefinitively =
                        previousAttempts.stream()
                                .allMatch(this::isDefinitivelyFailedAttempt);

                if (!allAttemptsFailedDefinitively) {
                    lifecycleService.markReconciliationRequired(
                            claim.id(),
                            null,
                            "Previous reward transaction attempts could not "
                                    + "all be classified as definitive failures. "
                                    + "Automatic resubmission is blocked."
                    );

                    return;
                }

                /*
                 * Reaching here means every prior attempt was classified
                 * as definitively failed. The existing claim key is retained.
                 */
            }

            Wallet wallet = walletRepository
                    .findByUserId(claim.recipientId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Reward recipient wallet was not found: "
                                    + claim.recipientId()
                    ));

            if (wallet.getBlockchainAddress() == null
                    || wallet.getBlockchainAddress().isBlank()) {
                throw new IllegalStateException(
                        "Reward recipient blockchain wallet is not provisioned"
                );
            }

            UUID blockchainTransactionId =
                    blockchainTokenService.submitCampaignReward(
                            wallet,
                            claim.campaign(),
                            claim.claimKey()
                    );

            lifecycleService.markSubmitted(
                    claim.id(),
                    blockchainTransactionId
            );

        } catch (RuntimeException exception) {
            log.warn(
                    "Reward claim processing attempt failed. claimId={}, campaign={}",
                    claim.id(),
                    claim.campaign(),
                    exception
            );

            recoverAfterProcessingError(claim, exception);
        }
    }

    public void synchronizeSubmittedClaims() {
        for (var claim : lifecycleService.findSubmittedSnapshots()) {
            try {
                if (reconcileProcessedClaimKey(claim)) {
                    continue;
                }

                if (claim.status()
                        == RewardClaimStatus.RECONCILIATION_REQUIRED) {

                    if (claim.blockchainTransactionId() == null) {
                        continue;
                    }

                    BlockchainTransaction transaction =
                            lifecycleService.getBlockchainTransaction(
                                    claim.blockchainTransactionId()
                            );

                    if (!isTransactionForClaim(
                            transaction,
                            claim.claimKey()
                    )) {
                        lifecycleService.markReconciliationRequired(
                                claim.id(),
                                null,
                                "The linked blockchain transaction does not match "
                                        + "the reward claim. Automatic recovery is blocked."
                        );
                        continue;
                    }

                    if (transaction.getStatus()
                            == BlockchainTransactionStatus.FAILED) {

                        if (isDefinitivelyFailedAttempt(transaction)) {
                            lifecycleService
                                    .retryAfterDefinitiveBlockchainFailure(
                                            claim.id(),
                                            transaction.getFailureReason()
                                    );
                        } else {
                            lifecycleService.markReconciliationRequired(
                                    claim.id(),
                                    transaction.getId(),
                                    "The failed transaction outcome remains "
                                            + "ambiguous. Automatic resubmission "
                                            + "is blocked."
                            );
                        }
                    }

                    /*
                     * Keep unresolved reconciliation claims out of the
                     * ordinary submission path.
                     */
                    continue;
                }

                if (claim.blockchainTransactionId() == null) {
                    continue;
                }

                BlockchainTransaction transaction =
                        lifecycleService.getBlockchainTransaction(
                                claim.blockchainTransactionId()
                        );

                if (transaction.getStatus()
                        == BlockchainTransactionStatus.CONFIRMED) {
                    confirmClaimAfterContractMapping(claim, transaction);

                } else if (transaction.getStatus()
                        == BlockchainTransactionStatus.FAILED) {

                    String failureReason =
                            transaction.getFailureReason() == null
                                    ? "Blockchain transaction failed"
                                    : transaction.getFailureReason();

                    if (isDefinitivelyFailedAttempt(transaction)) {
                        lifecycleService
                                .retryAfterDefinitiveBlockchainFailure(
                                        claim.id(),
                                        failureReason
                                );
                    } else {
                        lifecycleService.markReconciliationRequired(
                                claim.id(),
                                transaction.getId(),
                                "The transaction is marked FAILED, but its "
                                        + "failure does not prove that retrying "
                                        + "the mint is safe. "
                                        + failureReason
                        );
                    }
                }

            } catch (RuntimeException exception) {
                log.warn(
                        "Unable to synchronize reward claim {}",
                        claim.id(),
                        exception
                );
            }
        }
    }

    /**
     * Handles the case where the V2 contract reports that a claim key
     * has already been processed.
     *
     * A positive mapping alone is not used to credit the SQL wallet.
     * The processor first looks for a matching, confirmed blockchain
     * transaction with a saved hash. Otherwise, the claim remains in
     * reconciliation and is never blindly minted again.
     */
    private boolean reconcileProcessedClaimKey(
            RewardClaimLifecycleService.ClaimSnapshot claim
    ) {
        if (!blockchainTokenService.isRewardClaimProcessed(
                claim.claimKey()
        )) {
            return false;
        }

        List<BlockchainTransaction> attempts = new ArrayList<>(
                blockchainTransactionRepository
                        .findAllByRewardClaimKeyOrderByRequestedAtDesc(
                                claim.claimKey()
                        )
        );

        /*
         * A claim may have an explicit transaction association that is
         * missing from the claim-key query. Load and validate that row too.
         */
        if (claim.blockchainTransactionId() != null) {
            final BlockchainTransaction linked;

            try {
                linked = lifecycleService.getBlockchainTransaction(
                        claim.blockchainTransactionId()
                );
            } catch (RuntimeException exception) {
                lifecycleService.markReconciliationRequired(
                        claim.id(),
                        null,
                        "The contract reports the reward claim key as processed, "
                                + "but its linked blockchain transaction could not "
                                + "be loaded. Automatic wallet credit and resubmission "
                                + "are blocked."
                );
                return true;
            }

            if (!isTransactionForClaim(linked, claim.claimKey())) {
                lifecycleService.markReconciliationRequired(
                        claim.id(),
                        null,
                        "The linked blockchain transaction does not match this "
                                + "reward claim key and campaign-mint transaction type. "
                                + "Automatic wallet credit and resubmission are blocked."
                );
                return true;
            }

            boolean alreadyIncluded = attempts.stream()
                    .anyMatch(attempt ->
                            attempt.getId().equals(linked.getId())
                    );

            if (!alreadyIncluded) {
                attempts.add(linked);
            }
        }

        /*
         * Never use an unrelated transaction to settle this claim.
         */
        boolean hasMismatchedAttempt = attempts.stream()
                .anyMatch(attempt ->
                        !isTransactionForClaim(
                                attempt,
                                claim.claimKey()
                        )
                );

        if (hasMismatchedAttempt) {
            lifecycleService.markReconciliationRequired(
                    claim.id(),
                    null,
                    "A transaction-history mismatch was detected for this reward "
                            + "claim. Automatic wallet credit and resubmission are blocked."
            );
            return true;
        }

        /*
         * A matching CONFIRMED transaction with a saved hash can be
         * used for the normal idempotent SQL settlement.
         */
        Optional<BlockchainTransaction> confirmedAttempt =
                attempts.stream()
                        .filter(attempt ->
                                attempt.getStatus()
                                        == BlockchainTransactionStatus.CONFIRMED
                        )
                        .filter(attempt ->
                                attempt.getTransactionHash() != null
                                        && !attempt.getTransactionHash().isBlank()
                        )
                        .findFirst();

        if (confirmedAttempt.isPresent()) {
            lifecycleService.confirmClaim(
                    claim.id(),
                    confirmedAttempt.get().getId()
            );
            return true;
        }

        /*
         * No suitable confirmed transaction exists. Preserve the existing
         * transaction association where possible and wait for reconciliation.
         */
        UUID transactionId = claim.blockchainTransactionId();

        if (transactionId == null && !attempts.isEmpty()) {
            transactionId = attempts.get(0).getId();
        }

        lifecycleService.markReconciliationRequired(
                claim.id(),
                transactionId,
                "The V2 contract reports this reward claim key as processed, "
                        + "but no matching confirmed transaction with a saved "
                        + "hash is available. The claim is preserved for "
                        + "reconciliation; wallet credit and a second mint are blocked."
        );

        return true;
    }

    /**
     * Only these recorded outcomes establish a definitive failure:
     * - a failure during preparation before broadcast; or
     * - a mined transaction whose receipt proves execution failed.
     *
     * An unclassified FAILED state is treated as ambiguous.
     */
    private boolean isDefinitivelyFailedAttempt(
            BlockchainTransaction transaction
    ) {
        if (transaction.getStatus()
                != BlockchainTransactionStatus.FAILED) {
            return false;
        }

        String reason = transaction.getFailureReason();

        if (reason == null || reason.isBlank()) {
            return false;
        }

        String normalized = reason.toLowerCase(Locale.ROOT);

        if (normalized.contains("mined but failed on blockchain")) {
            return transaction.getTransactionHash() != null
                    && !transaction.getTransactionHash().isBlank();
        }

        return normalized.startsWith(
                "campaign reward transaction could not be prepared:"
        ) || normalized.startsWith(
                "campaign reward transaction could not be signed or tracked before broadcast:"
        );
    }

    /**
     * Examines recorded attempts after a processing exception.
     * A transaction which may have been broadcast is preserved for
     * reconciliation rather than treated as an ordinary retry.
     */
    private void recoverAfterProcessingError(
            RewardClaimLifecycleService.ClaimSnapshot claim,
            RuntimeException processingException
    ) {
        String reason =
                processingException.getMessage() == null
                        || processingException.getMessage().isBlank()
                        ? "Reward processing failed without a diagnostic message"
                        : processingException.getMessage();

        final List<BlockchainTransaction> attempts;

        try {
            attempts = new ArrayList<>(
                    blockchainTransactionRepository
                            .findAllByRewardClaimKeyOrderByRequestedAtDesc(
                                    claim.claimKey()
                            )
            );
        } catch (RuntimeException historyException) {
            log.error(
                    "Unable to read transaction history for reward claim {}. "
                            + "The claim will not be blindly resubmitted.",
                    claim.id(),
                    historyException
            );
            return;
        }

        boolean mismatch = attempts.stream()
                .anyMatch(attempt ->
                        !isTransactionForClaim(
                                attempt,
                                claim.claimKey()
                        )
                );

        if (mismatch) {
            lifecycleService.markReconciliationRequired(
                    claim.id(),
                    null,
                    reason + ". A transaction-history mismatch was found. "
                            + "Automatic resubmission is blocked."
            );
            return;
        }

        Optional<BlockchainTransaction> confirmed =
                attempts.stream()
                        .filter(attempt ->
                                attempt.getStatus()
                                        == BlockchainTransactionStatus.CONFIRMED
                        )
                        .findFirst();

        if (confirmed.isPresent()) {
            BlockchainTransaction transaction = confirmed.get();

            if (transaction.getTransactionHash() == null
                    || transaction.getTransactionHash().isBlank()) {
                lifecycleService.markReconciliationRequired(
                        claim.id(),
                        transaction.getId(),
                        reason + ". A confirmed transaction has no saved hash."
                );
                return;
            }

            try {
                confirmClaimAfterContractMapping(claim, transaction);
            } catch (RuntimeException settlementException) {
                lifecycleService.markReconciliationRequired(
                        claim.id(),
                        transaction.getId(),
                        "A blockchain transaction is confirmed, but SQL wallet "
                                + "settlement could not finish. Reconcile the same "
                                + "claim without submitting another mint."
                );
            }

            return;
        }

        Optional<BlockchainTransaction> active =
                attempts.stream()
                        .filter(attempt ->
                                attempt.getStatus()
                                        == BlockchainTransactionStatus.REQUESTED
                                        || attempt.getStatus()
                                        == BlockchainTransactionStatus.SUBMITTED
                        )
                        .findFirst();

        if (active.isPresent()) {
            BlockchainTransaction transaction = active.get();

            if (transaction.getTransactionHash() == null
                    || transaction.getTransactionHash().isBlank()) {
                lifecycleService.markReconciliationRequired(
                        claim.id(),
                        transaction.getId(),
                        reason + ". An active transaction has no saved hash. "
                                + "Automatic resubmission is blocked."
                );
            } else {
                lifecycleService.markSubmitted(
                        claim.id(),
                        transaction.getId()
                );
            }

            return;
        }

        boolean allAttemptsFailedDefinitively =
                attempts.stream()
                        .allMatch(this::isDefinitivelyFailedAttempt);

        if (attempts.isEmpty() || allAttemptsFailedDefinitively) {
            lifecycleService.retryAfterError(
                    claim.id(),
                    reason
            );
            return;
        }

        Optional<BlockchainTransaction> ambiguous =
                attempts.stream()
                        .filter(attempt ->
                                attempt.getStatus()
                                        == BlockchainTransactionStatus.FAILED
                        )
                        .findFirst();

        lifecycleService.markReconciliationRequired(
                claim.id(),
                ambiguous.map(BlockchainTransaction::getId)
                        .orElse(null),
                reason + ". At least one previous transaction attempt has "
                        + "an uncertain outcome. Automatic resubmission is blocked."
        );
    }

    /**
     * A database CONFIRMED status is not enough when the V2 contract
     * does not confirm that this claim key has already been processed.
     */
    private void confirmClaimAfterContractMapping(
            RewardClaimLifecycleService.ClaimSnapshot claim,
            BlockchainTransaction transaction
    ) {
        if (!isTransactionForClaim(transaction, claim.claimKey())) {
            lifecycleService.markReconciliationRequired(
                    claim.id(),
                    null,
                    "The transaction does not match the reward claim. "
                            + "Wallet credit is blocked pending reconciliation."
            );
            return;
        }

        final boolean processed;

        try {
            processed = blockchainTokenService.isRewardClaimProcessed(
                    claim.claimKey()
            );
        } catch (RuntimeException exception) {
            lifecycleService.markReconciliationRequired(
                    claim.id(),
                    transaction.getId(),
                    "Unable to verify the reward claim key against the V2 "
                            + "token contract. Wallet credit is blocked pending "
                            + "reconciliation."
            );
            return;
        }

        if (!processed) {
            lifecycleService.markReconciliationRequired(
                    claim.id(),
                    transaction.getId(),
                    "The database transaction is marked CONFIRMED, but the V2 "
                            + "contract does not report this claim key as processed. "
                            + "Wallet credit is blocked pending reconciliation."
            );
            return;
        }

        try {
            lifecycleService.confirmClaim(
                    claim.id(),
                    transaction.getId()
            );

        } catch (RuntimeException settlementException) {

            log.error(
                    "Confirmed blockchain reward could not be settled "
                            + "into the application wallet. claimId={}, "
                            + "transactionId={}. No new mint will be requested.",
                    claim.id(),
                    transaction.getId(),
                    settlementException
            );

            lifecycleService.markReconciliationRequired(
                    claim.id(),
                    transaction.getId(),
                    "The V2 contract reports this claim key as processed, "
                            + "but SQL wallet or ledger settlement failed. "
                            + "The existing transaction is preserved for "
                            + "reconciliation; another mint is blocked."
            );
        }
    }

    private boolean isTransactionForClaim(
            BlockchainTransaction transaction,
            String claimKey
    ) {
        return transaction.getTransactionType()
                == BlockchainTransactionType.MINT_CAMPAIGN_REWARD
                && transaction.getRewardClaimKey() != null
                && claimKey != null
                && transaction.getRewardClaimKey()
                .equalsIgnoreCase(claimKey);
    }

}