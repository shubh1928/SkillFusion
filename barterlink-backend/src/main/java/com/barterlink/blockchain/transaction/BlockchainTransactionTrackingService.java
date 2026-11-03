    package com.barterlink.blockchain.transaction;

    import com.barterlink.wallet.entity.Wallet;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Propagation;
    import org.springframework.transaction.annotation.Transactional;
    import org.web3j.crypto.Hash;
    import org.web3j.utils.Numeric;
    import java.math.BigInteger;
    import java.time.Instant;
    import java.util.UUID;
    import java.util.List;

    @Service
    @RequiredArgsConstructor
    public class BlockchainTransactionTrackingService {

        private final BlockchainTransactionRepository repository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction createRequested(
                Wallet wallet,
                BlockchainTransactionType transactionType,
                BigInteger amount,
                String escrowId,
                String fromAddress,
                String toAddress
        ) {

            if (wallet == null) {
                throw new IllegalArgumentException(
                        "Wallet is required"
                );
            }

            if (transactionType == null) {
                throw new IllegalArgumentException(
                        "Blockchain transaction type is required"
                );
            }

            if (amount != null && amount.signum() < 0) {
                throw new IllegalArgumentException(
                        "Transaction amount cannot be negative"
                );
            }

            BlockchainTransaction transaction =
                    BlockchainTransaction.builder()
                            .wallet(wallet)
                            .transactionType(transactionType)
                            .status(
                                    BlockchainTransactionStatus.REQUESTED
                            )
                            .amount(amount)
                            .escrowId(escrowId)
                            .fromAddress(fromAddress)
                            .toAddress(toAddress)
                            .build();

            return repository.save(transaction);
        }


        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction createRequestedReward(
                Wallet wallet,
                BigInteger amount,
                String fromAddress,
                String toAddress,
                String rewardClaimKey
        ) {

            if (wallet == null) {
                throw new IllegalArgumentException(
                        "Wallet is required"
                );
            }

            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Reward transaction amount must be greater than zero"
                );
            }

            if (rewardClaimKey == null
                    || !rewardClaimKey.trim().matches(
                    "(?i)^0x[0-9a-f]{64}$"
            )) {
                throw new IllegalArgumentException(
                        "Reward claim key must be a valid 32-byte hexadecimal value"
                );
            }

            BlockchainTransaction transaction =
                    BlockchainTransaction.builder()
                            .wallet(wallet)
                            .transactionType(
                                    BlockchainTransactionType.MINT_CAMPAIGN_REWARD
                            )
                            .status(BlockchainTransactionStatus.REQUESTED)
                            .amount(amount)
                            .fromAddress(fromAddress)
                            .toAddress(toAddress)
                            .rewardClaimKey(rewardClaimKey.trim())
                            .build();

            return repository.save(transaction);
        }


        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction recordSignedTransactionHash(
                UUID transactionId,
                String transactionHash
        ) {

            requireTransactionHash(transactionHash);

            BlockchainTransaction transaction =
                    getTransaction(transactionId);

            if (transaction.getStatus()
                    != BlockchainTransactionStatus.REQUESTED) {

                throw new IllegalStateException(
                        "Only REQUESTED transactions can record a signed hash");
            }

            String normalizedHash = transactionHash.trim();
            String existingHash = transaction.getTransactionHash();

            if (existingHash != null
                    && !existingHash.isBlank()
                    && !existingHash.equalsIgnoreCase(normalizedHash)) {

                throw new IllegalStateException(
                        "A different transaction hash is already recorded");
            }

            transaction.setTransactionHash(normalizedHash);

            return repository.save(transaction);
        }





        /**
         * Persists the signed transaction hash and exact signed bytes together
         * before the transaction is broadcast.
         *
         * The saved hash is verified against the signed bytes before persistence.
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction recordSignedRewardTransaction(
                UUID transactionId,
                String transactionHash,
                String signedRawTransaction
        ) {
            requireTransactionHash(transactionHash);

            if (signedRawTransaction == null
                    || !signedRawTransaction.trim().matches(
                    "(?i)^0x(?:[0-9a-f]{2})+$"
            )) {
                throw new IllegalArgumentException(
                        "Signed raw transaction must be valid hexadecimal bytes"
                );
            }

            String normalizedHash = transactionHash.trim();
            String normalizedRawTransaction = signedRawTransaction.trim();

            String calculatedHash = Numeric.toHexString(
                    Hash.sha3(
                            Numeric.hexStringToByteArray(
                                    normalizedRawTransaction
                            )
                    )
            );

            if (!calculatedHash.equalsIgnoreCase(normalizedHash)) {
                throw new IllegalArgumentException(
                        "Transaction hash does not match the signed transaction bytes"
                );
            }

            BlockchainTransaction transaction = getTransaction(transactionId);

            if (transaction.getStatus()
                    != BlockchainTransactionStatus.REQUESTED) {
                throw new IllegalStateException(
                        "Only REQUESTED transactions can save signed reward transaction data"
                );
            }

            String existingHash = transaction.getTransactionHash();
            String existingRaw = transaction.getSignedRawTransaction();

            if (existingHash != null
                    && !existingHash.isBlank()
                    && !existingHash.equalsIgnoreCase(normalizedHash)) {
                throw new IllegalStateException(
                        "A different transaction hash is already recorded"
                );
            }

            if (existingRaw != null
                    && !existingRaw.isBlank()
                    && !existingRaw.equalsIgnoreCase(normalizedRawTransaction)) {
                throw new IllegalStateException(
                        "Different signed transaction bytes are already recorded"
                );
            }

            transaction.setTransactionHash(normalizedHash);
            transaction.setSignedRawTransaction(normalizedRawTransaction);
            transaction.setLastBroadcastAttemptAt(Instant.now());

            return repository.save(transaction);
        }

        /**
         * Records the time before rebroadcasting a previously signed transaction.
         * Only the same stored transaction may be retried.
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction recordBroadcastAttempt(
                UUID transactionId
        ) {
            BlockchainTransaction transaction = getTransaction(transactionId);

            if (transaction.getStatus()
                    != BlockchainTransactionStatus.REQUESTED
                    && transaction.getStatus()
                    != BlockchainTransactionStatus.SUBMITTED) {
                throw new IllegalStateException(
                        "Only REQUESTED or SUBMITTED transactions can be rebroadcast"
                );
            }

            if (transaction.getTransactionHash() == null
                    || transaction.getTransactionHash().isBlank()
                    || transaction.getSignedRawTransaction() == null
                    || transaction.getSignedRawTransaction().isBlank()) {
                throw new IllegalStateException(
                        "A saved transaction hash and signed transaction are required"
                );
            }

            transaction.setLastBroadcastAttemptAt(Instant.now());

            return repository.save(transaction);
        }


        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction markSubmitted(
                UUID transactionId,
                String transactionHash
        ) {

            requireTransactionHash(transactionHash);

            BlockchainTransaction transaction =
                    getTransaction(transactionId);

            if (transaction.getStatus()
                    != BlockchainTransactionStatus.REQUESTED) {

                throw new IllegalStateException(
                        "Only REQUESTED transactions can be marked SUBMITTED"
                );
            }

            String normalizedHash = transactionHash.trim();
            String existingHash = transaction.getTransactionHash();

            if (existingHash != null
                    && !existingHash.isBlank()
                    && !existingHash.equalsIgnoreCase(normalizedHash)) {

                throw new IllegalStateException(
                        "Submitted transaction hash does not match the recorded signed hash");
            }

            transaction.setTransactionHash(normalizedHash);

            transaction.setStatus(
                    BlockchainTransactionStatus.SUBMITTED
            );

            transaction.setSubmittedAt(
                    Instant.now()
            );

            return repository.save(transaction);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction markConfirmed(
                UUID transactionId
        ) {

            BlockchainTransaction transaction =
                    getTransaction(transactionId);

            if (transaction.getStatus()
                    != BlockchainTransactionStatus.SUBMITTED) {

                throw new IllegalStateException(
                        "Only SUBMITTED transactions can be marked CONFIRMED"
                );
            }

            if (transaction.getTransactionHash() == null
                    || transaction.getTransactionHash().isBlank()) {

                throw new IllegalStateException(
                        "A transaction hash is required before confirmation"
                );
            }

            transaction.setStatus(
                    BlockchainTransactionStatus.CONFIRMED
            );

            transaction.setConfirmedAt(
                    Instant.now()
            );

            return repository.save(transaction);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BlockchainTransaction markFailed(
                UUID transactionId,
                String failureReason
        ) {

            BlockchainTransaction transaction =
                    getTransaction(transactionId);

            BlockchainTransactionStatus currentStatus =
                    transaction.getStatus();

            if (currentStatus
                    != BlockchainTransactionStatus.REQUESTED
                    && currentStatus
                    != BlockchainTransactionStatus.SUBMITTED) {

                throw new IllegalStateException(
                        "Only REQUESTED or SUBMITTED transactions "
                                + "can be marked FAILED"
                );
            }

            String reason =
                    failureReason == null
                            ? "Unknown blockchain transaction failure"
                            : failureReason.trim();

            if (reason.isBlank()) {
                reason =
                        "Unknown blockchain transaction failure";
            }

            if (reason.length() > 1000) {
                reason = reason.substring(0, 1000);
            }

            transaction.setFailureReason(reason);

            transaction.setStatus(
                    BlockchainTransactionStatus.FAILED
            );

            transaction.setFailedAt(
                    Instant.now()
            );

            return repository.save(transaction);
        }

        @Transactional(readOnly = true)
        public BlockchainTransaction getTransaction(
                UUID transactionId
        ) {

            if (transactionId == null) {
                throw new IllegalArgumentException(
                        "Transaction ID is required"
                );
            }

            return repository.findById(transactionId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Blockchain transaction not found: "
                                            + transactionId
                            )
                    );
        }


        @Transactional(readOnly = true)
        public BlockchainTransaction getTransactionByEscrowIdAndType(
                String escrowId,
                BlockchainTransactionType transactionType
        ) {

            if (escrowId == null || escrowId.isBlank()) {
                throw new IllegalArgumentException(
                        "Escrow ID is required"
                );
            }

            if (transactionType == null) {
                throw new IllegalArgumentException(
                        "Blockchain transaction type is required"
                );
            }

            return repository
                    .findByEscrowIdAndTransactionType(
                            escrowId.trim(),
                            transactionType
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Blockchain transaction not found for escrow ID "
                                            + escrowId
                                            + " and type "
                                            + transactionType
                            )
                    );
        }

        private void requireTransactionHash(
                String transactionHash
        ) {

            if (transactionHash == null
                    || transactionHash.isBlank()) {

                throw new IllegalArgumentException(
                        "Transaction hash is required"
                );
            }

            String cleanHash =
                    transactionHash.trim();

            if (!cleanHash.matches(
                    "(?i)^0x[0-9a-f]{64}$"
            )) {

                throw new IllegalArgumentException(
                        "Transaction hash must be a valid 32-byte "
                                + "Ethereum transaction hash"
                );
            }
        }

        @Transactional(readOnly = true)
        public List<BlockchainTransaction> getTransactionsByEscrowIdAndType(
                String escrowId,
                BlockchainTransactionType transactionType) {

            if (escrowId == null || escrowId.isBlank()) {
                throw new IllegalArgumentException(
                        "Escrow ID is required");
            }

            if (transactionType == null) {
                throw new IllegalArgumentException(
                        "Blockchain transaction type is required");
            }

            return repository.findAllByEscrowIdAndTransactionType(
                    escrowId.trim(),
                    transactionType);
        }

    }