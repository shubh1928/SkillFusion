package com.barterlink.blockchain.transaction;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.transaction.annotation.Propagation;
import java.util.List;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BlockchainTransactionReconciliationService {


    private static final long REBROADCAST_INTERVAL_SECONDS = 60L;

    private static final Logger log =
            LoggerFactory.getLogger(
                    BlockchainTransactionReconciliationService.class
            );

    private final Web3j web3j;

    private final BlockchainTransactionRepository repository;

    private final BlockchainTransactionTrackingService trackingService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void reconcileSubmittedTransactions() {

        List<BlockchainTransaction> transactions =
                repository.findByStatusInAndTransactionHashIsNotNull(
                        List.of(
                                BlockchainTransactionStatus.REQUESTED,
                                BlockchainTransactionStatus.SUBMITTED
                        )
                );

        for (BlockchainTransaction transaction : transactions) {

            String transactionHash = transaction.getTransactionHash();

            if (transactionHash == null || transactionHash.isBlank()) {
                log.warn(
                        "Cannot reconcile blockchain transaction {} because its hash is missing.",
                        transaction.getId()
                );
                continue;
            }

            String normalizedHash = transactionHash.trim();

            try {
                var response = web3j.ethGetTransactionReceipt(
                        normalizedHash
                ).send();

                if (response.hasError()) {
                    log.warn(
                            "RPC error while reconciling blockchain transaction {}. "
                                    + "Existing status is preserved. Error: {}",
                            transaction.getId(),
                            response.getError().getMessage()
                    );
                    continue;
                }

                if (response.getTransactionReceipt().isEmpty()) {
                    log.info(
                            "No receipt yet for blockchain transaction {}: {}",
                            transaction.getId(),
                            normalizedHash
                    );

                    rebroadcastExactSignedTransactionIfDue(
                            transaction,
                            normalizedHash
                    );

                    continue;
                }

                TransactionReceipt receipt =
                        response.getTransactionReceipt().get();

                /*
                 * Do not assume the entity loaded at the beginning of this
                 * method reflects the latest database state. Reload through
                 * the tracking service, using its own transaction.
                 */
                BlockchainTransaction latest =
                        trackingService.getTransaction(transaction.getId());

                if (latest.getStatus()
                        == BlockchainTransactionStatus.REQUESTED) {

                    trackingService.markSubmitted(
                            latest.getId(),
                            normalizedHash
                    );
                }

                /*
                 * Reload after the state transition. NOT_SUPPORTED prevents
                 * this scheduled method from retaining an outer persistence
                 * context with the old REQUESTED state.
                 */
                latest = trackingService.getTransaction(transaction.getId());

                if (latest.getStatus()
                        == BlockchainTransactionStatus.CONFIRMED
                        || latest.getStatus()
                        == BlockchainTransactionStatus.FAILED) {
                    continue;
                }

                if (latest.getStatus()
                        != BlockchainTransactionStatus.SUBMITTED) {
                    continue;
                }

                String chainStatus = receipt.getStatus();

                if ("0x1".equalsIgnoreCase(chainStatus)) {

                    trackingService.markConfirmed(latest.getId());

                    log.info(
                            "Reconciled blockchain transaction {} as CONFIRMED. Hash: {}",
                            latest.getId(),
                            normalizedHash
                    );

                } else if ("0x0".equalsIgnoreCase(chainStatus)) {

                    trackingService.markFailed(
                            latest.getId(),
                            "Transaction was mined but failed on blockchain. "
                                    + "Transaction hash: " + normalizedHash
                    );

                    log.warn(
                            "Reconciled blockchain transaction {} as FAILED. Hash: {}",
                            latest.getId(),
                            normalizedHash
                    );

                } else {

                    /*
                     * An unknown status is not proof of transaction failure.
                     * Preserve SUBMITTED for another reconciliation attempt.
                     */
                    log.warn(
                            "Unknown blockchain receipt status for transaction {}: {}. "
                                    + "Existing status is preserved.",
                            latest.getId(),
                            chainStatus
                    );
                }

            } catch (Exception e) {
                log.warn(
                        "Unable to reconcile blockchain transaction {} with hash {}. "
                                + "No automatic resubmission will be attempted.",
                        transaction.getId(),
                        normalizedHash,
                        e
                );
            }
        }
    }

    /**
     * Rebroadcasts the exact previously signed transaction when its receipt
     * is still missing and the retry interval has elapsed.
     *
     * Never creates a new nonce, signs a new transaction, changes the
     * recipient, or changes the reward claim key.
     */
    private void rebroadcastExactSignedTransactionIfDue(
            BlockchainTransaction transaction,
            String expectedHash
    ) {
        String signedRawTransaction = transaction.getSignedRawTransaction();

        if (signedRawTransaction == null
                || signedRawTransaction.isBlank()) {
            log.warn(
                    "Cannot automatically rebroadcast transaction {} because "
                            + "its signed bytes are not stored. Hash: {}",
                    transaction.getId(),
                    expectedHash
            );
            return;
        }

        Instant lastAttempt = transaction.getLastBroadcastAttemptAt();

        if (lastAttempt != null
                && lastAttempt.isAfter(
                Instant.now().minusSeconds(
                        REBROADCAST_INTERVAL_SECONDS
                )
        )) {
            return;
        }

        String normalizedRaw = signedRawTransaction.trim();
        String normalizedHash = expectedHash.trim();

        final String calculatedHash;

        try {
            calculatedHash = Numeric.toHexString(
                    Hash.sha3(
                            Numeric.hexStringToByteArray(normalizedRaw)
                    )
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Stored signed transaction is invalid for blockchain transaction {}",
                    transaction.getId(),
                    exception
            );
            return;
        }

        if (!calculatedHash.equalsIgnoreCase(normalizedHash)) {
            log.error(
                    "Stored signed transaction hash mismatch for transaction {}. "
                            + "Expected {}, calculated {}. Rebroadcast blocked.",
                    transaction.getId(),
                    normalizedHash,
                    calculatedHash
            );
            return;
        }

        try {
            /*
             * Persist the attempt time first, so a repeated scheduler run
             * cannot immediately flood the RPC provider with rebroadcasts.
             */
            trackingService.recordBroadcastAttempt(
                    transaction.getId()
            );

            var response = web3j.ethSendRawTransaction(
                    normalizedRaw
            ).send();

            if (response.hasError()) {
                String errorMessage =
                        response.getError() == null
                                ? "Unknown RPC error"
                                : response.getError().getMessage();

                String normalizedError =
                        errorMessage.toLowerCase(Locale.ROOT);

                boolean alreadyKnown =
                        normalizedError.contains("already known")
                                || normalizedError.contains("known transaction");

                if (!alreadyKnown) {
                    log.warn(
                            "Rebroadcast of transaction {} returned an RPC error. "
                                    + "Existing transaction state is preserved. Error: {}",
                            transaction.getId(),
                            errorMessage
                    );
                    return;
                }
            } else {
                String returnedHash = response.getTransactionHash();

                if (returnedHash != null
                        && !returnedHash.isBlank()
                        && !returnedHash.equalsIgnoreCase(normalizedHash)) {
                    log.error(
                            "RPC returned a different hash while rebroadcasting "
                                    + "transaction {}. Expected {}, received {}.",
                            transaction.getId(),
                            normalizedHash,
                            returnedHash
                    );
                    return;
                }
            }

            BlockchainTransaction latest =
                    trackingService.getTransaction(transaction.getId());

            if (latest.getStatus()
                    == BlockchainTransactionStatus.REQUESTED) {
                trackingService.markSubmitted(
                        latest.getId(),
                        normalizedHash
                );
            }

            log.info(
                    "Rebroadcasted the same signed transaction for tracking record {}. Hash: {}",
                    transaction.getId(),
                    normalizedHash
            );

        } catch (Exception exception) {
            /*
             * A broadcast exception is not evidence that the transaction
             * failed. Preserve the saved hash and signed bytes for recovery.
             */
            log.warn(
                    "Unable to rebroadcast the saved transaction {}. "
                            + "Its existing state is preserved for another reconciliation cycle.",
                    transaction.getId(),
                    exception
            );
        }
    }

}