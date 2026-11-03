package com.barterlink.rewards.service;

import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionRepository;
import com.barterlink.blockchain.transaction.BlockchainTransactionStatus;
import com.barterlink.rewards.entity.RewardClaim;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardClaimStatus;
import com.barterlink.rewards.repository.RewardClaimRepository;
import com.barterlink.wallet.entity.TokenTransaction;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.enums.TransactionType;
import com.barterlink.wallet.repository.TokenTransactionRepository;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.barterlink.blockchain.config.BlockchainProperties;

@Service
@RequiredArgsConstructor
public class RewardClaimLifecycleService {

    private static final int MAX_ATTEMPTS = 8;

    private final RewardClaimRepository rewardClaimRepository;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final WalletRepository walletRepository;
    private final TokenTransactionRepository tokenTransactionRepository;
    private final BlockchainProperties blockchainProperties;

    @Transactional
    public UUID claimNextPending() {
        Instant now = Instant.now();

        Optional<RewardClaim> optional =
                rewardClaimRepository
                        .findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                RewardClaimStatus.PENDING,
                                now
                        );

        if (optional.isEmpty()) {
            return null;
        }

        RewardClaim claim = optional.get();

        claim.setStatus(RewardClaimStatus.PROCESSING);
        claim.setProcessingAt(now);
        claim.setAttemptCount(claim.getAttemptCount() + 1);
        claim.setLastError(null);

        rewardClaimRepository.save(claim);

        return claim.getId();
    }

    @Transactional
    public void recoverStaleProcessingClaims() {
        Instant cutoff = Instant.now().minusSeconds(300);
        Instant now = Instant.now();

        for (RewardClaim claim :
                rewardClaimRepository.findByStatusAndProcessingAtBefore(
                        RewardClaimStatus.PROCESSING,
                        cutoff
                )) {

            claim.setStatus(RewardClaimStatus.PENDING);
            claim.setProcessingAt(null);
            claim.setNextAttemptAt(now);
            claim.setLastError(
                    "Recovered a reward claim left in PROCESSING "
                            + "after an interrupted attempt"
            );

            rewardClaimRepository.save(claim);
        }
    }

    @Transactional(readOnly = true)
    public List<ClaimSnapshot> findSubmittedSnapshots() {
        return rewardClaimRepository
                .findByStatusInOrderByCreatedAtAsc(
                        List.of(
                                RewardClaimStatus.SUBMITTED,
                                RewardClaimStatus.RECONCILIATION_REQUIRED
                        )
                )
                .stream()
                .map(claim -> new ClaimSnapshot(
                        claim.getId(),
                        claim.getRecipient().getId(),
                        claim.getCampaign(),
                        claim.getClaimKey(),
                        claim.getAmountTokens(),
                        claim.getStatus(),
                        claim.getBlockchainTransaction() == null
                                ? null
                                : claim.getBlockchainTransaction().getId(),
                        claim.getAttemptCount()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BlockchainTransaction getBlockchainTransaction(
            UUID transactionId
    ) {
        return blockchainTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Blockchain transaction was not found: "
                                + transactionId
                ));
    }

    @Transactional(readOnly = true)
    public ClaimSnapshot getSnapshot(UUID claimId) {
        RewardClaim claim = rewardClaimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reward claim was not found: " + claimId
                ));

        UUID transactionId =
                claim.getBlockchainTransaction() == null
                        ? null
                        : claim.getBlockchainTransaction().getId();

        return new ClaimSnapshot(
                claim.getId(),
                claim.getRecipient().getId(),
                claim.getCampaign(),
                claim.getClaimKey(),
                claim.getAmountTokens(),
                claim.getStatus(),
                transactionId,
                claim.getAttemptCount()
        );
    }

    @Transactional
    public void markSubmitted(
            UUID claimId,
            UUID blockchainTransactionId
    ) {
        RewardClaim claim = getClaimForUpdate(claimId);

        BlockchainTransaction transaction =
                blockchainTransactionRepository.findById(
                                blockchainTransactionId
                        )
                        .orElseThrow(() -> new IllegalStateException(
                                "Blockchain transaction was not found: "
                                        + blockchainTransactionId
                        ));

        if (claim.getStatus() == RewardClaimStatus.CONFIRMED) {
            return;
        }

        claim.setBlockchainTransaction(transaction);
        claim.setStatus(RewardClaimStatus.SUBMITTED);
        claim.setProcessingAt(null);
        claim.setLastError(null);

        rewardClaimRepository.save(claim);
    }

    @Transactional
    public void markReconciliationRequired(
            UUID claimId,
            UUID blockchainTransactionId,
            String reason
    ) {
        RewardClaim claim = getClaimForUpdate(claimId);

        if (claim.getStatus() == RewardClaimStatus.CONFIRMED) {
            return;
        }

        if (blockchainTransactionId != null) {
            BlockchainTransaction transaction =
                    blockchainTransactionRepository
                            .findById(blockchainTransactionId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Blockchain transaction was not found: "
                                            + blockchainTransactionId
                            ));

            claim.setBlockchainTransaction(transaction);
        }

        claim.setStatus(RewardClaimStatus.RECONCILIATION_REQUIRED);
        claim.setProcessingAt(null);

        String safeReason =
                reason == null || reason.isBlank()
                        ? "Transaction outcome requires reconciliation"
                        : reason.trim();

        if (safeReason.length() > 1000) {
            safeReason = safeReason.substring(0, 1000);
        }

        claim.setLastError(safeReason);
        rewardClaimRepository.save(claim);
    }

    @Transactional
    public void deferBecauseCampaignPaused(UUID claimId) {
        RewardClaim claim = getClaimForUpdate(claimId);

        if (claim.getStatus() == RewardClaimStatus.CONFIRMED) {
            return;
        }

        claim.setStatus(RewardClaimStatus.PENDING);
        claim.setProcessingAt(null);

        // An intentionally paused campaign is not a failed mint attempt.
        claim.setAttemptCount(
                Math.max(0, claim.getAttemptCount() - 1)
        );

        claim.setNextAttemptAt(Instant.now().plusSeconds(300));
        claim.setLastError(
                "Reward campaign is paused on the V2 token contract"
        );

        rewardClaimRepository.save(claim);
    }

    /**
     * Requeues a previously submitted or reconciliation-required claim
     * only after the processor has established that the linked attempt
     * failed definitively.
     *
     * The existing reward claim and claim key are preserved.
     */
    @Transactional
    public void retryAfterDefinitiveBlockchainFailure(
            UUID claimId,
            String reason
    ) {
        RewardClaim claim = getClaimForUpdate(claimId);

        if (claim.getStatus() == RewardClaimStatus.CONFIRMED
                || claim.getStatus() == RewardClaimStatus.REVIEW_REQUIRED) {
            return;
        }

        if (claim.getStatus() != RewardClaimStatus.SUBMITTED
                && claim.getStatus()
                != RewardClaimStatus.RECONCILIATION_REQUIRED) {
            return;
        }

        String safeReason =
                reason == null || reason.isBlank()
                        ? "Previous blockchain transaction failed definitively"
                        : reason.trim();

        if (safeReason.length() > 1000) {
            safeReason = safeReason.substring(0, 1000);
        }

        claim.setProcessingAt(null);
        claim.setLastError(safeReason);

        if (claim.getAttemptCount() >= MAX_ATTEMPTS) {
            claim.setStatus(RewardClaimStatus.FAILED);
        } else {
            long backoffSeconds = Math.min(
                    900L,
                    15L * (1L << Math.min(claim.getAttemptCount(), 6))
            );

            claim.setStatus(RewardClaimStatus.PENDING);
            claim.setNextAttemptAt(
                    Instant.now().plusSeconds(backoffSeconds)
            );
        }

        rewardClaimRepository.save(claim);
    }

    @Transactional
    public void retryAfterError(UUID claimId, String reason) {

        RewardClaim claim = getClaimForUpdate(claimId);


        if (claim.getStatus() == RewardClaimStatus.CONFIRMED
                || claim.getStatus() == RewardClaimStatus.RECONCILIATION_REQUIRED
                || claim.getStatus() == RewardClaimStatus.REVIEW_REQUIRED) {
            return;
        }

        String safeReason =
                reason == null || reason.isBlank()
                        ? "Unknown reward processing error"
                        : reason.trim();

        if (safeReason.length() > 1000) {
            safeReason = safeReason.substring(0, 1000);
        }

        claim.setProcessingAt(null);
        claim.setLastError(safeReason);

        if (claim.getAttemptCount() >= MAX_ATTEMPTS) {
            claim.setStatus(RewardClaimStatus.FAILED);
        } else {
            long backoffSeconds = Math.min(
                    900L,
                    15L * (1L << Math.min(claim.getAttemptCount(), 6))
            );

            claim.setStatus(RewardClaimStatus.PENDING);
            claim.setNextAttemptAt(
                    Instant.now().plusSeconds(backoffSeconds)
            );
        }

        rewardClaimRepository.save(claim);
    }

    /**
     * Call only when there is proof of a successful transaction receipt
     * or a positive on-chain rewardClaimKeyProcessed mapping.
     *
     * The claim-status change and wallet credit commit atomically.
     */
    /**
     * Confirms a reward only when its matching blockchain transaction
     * has a CONFIRMED status and a saved transaction hash.
     *
     * Wallet credit, ledger insertion, and claim confirmation are
     * committed within the same SQL transaction.
     */
    @Transactional
    public void confirmClaim(
            UUID claimId,
            UUID blockchainTransactionId
    ) {
        RewardClaim claim = getClaimForUpdate(claimId);

        if (claim.getStatus() == RewardClaimStatus.CONFIRMED) {
            return;
        }

        /*
         * A missing transaction ID is never accepted as proof of a mint.
         * In particular, do not clear an existing transaction association.
         */
        if (blockchainTransactionId == null) {
            throw new IllegalStateException(
                    "Cannot confirm a reward claim without a verified "
                            + "blockchain transaction"
            );
        }

        BlockchainTransaction transaction =
                blockchainTransactionRepository.findById(
                                blockchainTransactionId
                        )
                        .orElseThrow(() -> new IllegalStateException(
                                "Blockchain transaction was not found: "
                                        + blockchainTransactionId
                        ));

        if (transaction.getStatus()
                != BlockchainTransactionStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Reward cannot be credited before blockchain confirmation"
            );
        }

        if (transaction.getTransactionHash() == null
                || transaction.getTransactionHash().isBlank()) {
            throw new IllegalStateException(
                    "Reward cannot be credited without a saved blockchain transaction hash"
            );
        }

        if (transaction.getTransactionType()
                != com.barterlink.blockchain.transaction.BlockchainTransactionType
                .MINT_CAMPAIGN_REWARD
                || transaction.getRewardClaimKey() == null
                || claim.getClaimKey() == null
                || !transaction.getRewardClaimKey()
                .equalsIgnoreCase(claim.getClaimKey())) {
            throw new IllegalStateException(
                    "Blockchain transaction does not match this reward claim"
            );
        }

        /*
         * Verify that this confirmed transaction belongs to the
         * recipient of this claim.
         */
        if (transaction.getWallet() == null
                || transaction.getWallet().getUser() == null
                || !claim.getRecipient().getId().equals(
                transaction.getWallet().getUser().getId()
        )) {
            throw new IllegalStateException(
                    "Confirmed blockchain transaction does not belong "
                            + "to the reward claim recipient"
            );
        }

        /*
         * The SQL claim must use the fixed amount for its campaign.
         * The transaction record stores blockchain units (wei).
         */
        Integer claimedAmount = claim.getAmountTokens();

        if (claim.getCampaign() == null
                || claimedAmount == null
                || claimedAmount <= 0
                || claimedAmount != claim.getCampaign().getTokenAmount()) {
            throw new IllegalStateException(
                    "Reward claim amount does not match the fixed "
                            + "campaign reward amount"
            );
        }

        BigInteger expectedAmountWei =
                BigInteger.valueOf(claimedAmount)
                        .multiply(BigInteger.TEN.pow(18));

        if (transaction.getAmount() == null
                || !expectedAmountWei.equals(transaction.getAmount())) {
            throw new IllegalStateException(
                    "Confirmed blockchain transaction amount does not "
                            + "match the reward claim amount"
            );
        }

        /*
         * The Ethereum transaction destination must be the configured
         * V2 reward token contract, not the recipient wallet address.
         */
        String savedDestination = transaction.getToAddress();
        String configuredRewardToken =
                blockchainProperties.getTokenAddress();

        if (configuredRewardToken == null
                || configuredRewardToken.isBlank()
                || savedDestination == null
                || savedDestination.isBlank()
                || !savedDestination.trim().equalsIgnoreCase(
                configuredRewardToken.trim()
        )) {

            throw new IllegalStateException(
                    "Confirmed blockchain transaction destination does not "
                            + "match the configured active BLT token contract"
            );
        }

        claim.setBlockchainTransaction(transaction);

        /*
         * An existing ledger entry means the wallet must not be credited again.
         * This also permits a retry to finish claim-state reconciliation safely.
         */
        Optional<TokenTransaction> existingLedgerEntry =
                tokenTransactionRepository.findByRewardClaim_Id(claimId);

        if (existingLedgerEntry.isPresent()) {
            TokenTransaction ledgerEntry = existingLedgerEntry.get();

            /*
             * An existing ledger entry must match this claim's recipient
             * wallet, reward amount, transaction type and claim association.
             * Never create a second credit to correct a mismatched entry.
             */
            boolean matchingWallet =
                    ledgerEntry.getWallet() != null
                            && transaction.getWallet() != null
                            && ledgerEntry.getWallet().getId()
                            .equals(transaction.getWallet().getId());

            boolean matchingAmount =
                    ledgerEntry.getAmount() != null
                            && ledgerEntry.getAmount()
                            .equals(claim.getAmountTokens());

            boolean matchingTransactionType =
                    ledgerEntry.getTransactionType()
                            == TransactionType.REWARD;

            boolean matchingClaim =
                    ledgerEntry.getRewardClaim() != null
                            && claimId.equals(
                            ledgerEntry.getRewardClaim().getId()
                    );

            if (!matchingWallet
                    || !matchingAmount
                    || !matchingTransactionType
                    || !matchingClaim) {

                claim.setStatus(
                        RewardClaimStatus.RECONCILIATION_REQUIRED
                );
                claim.setProcessingAt(null);
                claim.setLastError(
                        "An existing reward ledger entry does not match "
                                + "the confirmed claim's wallet, amount, "
                                + "transaction type or claim association. "
                                + "No additional wallet credit was applied."
                );

                rewardClaimRepository.save(claim);
                return;
            }

            /*
             * The existing ledger entry matches this claim.
             * Do not increment the wallet balance a second time.
             */
            claim.setStatus(RewardClaimStatus.CONFIRMED);

            if (claim.getConfirmedAt() == null) {
                claim.setConfirmedAt(
                        ledgerEntry.getCreatedAt()
                );
            }

            claim.setProcessingAt(null);
            claim.setLastError(null);

            rewardClaimRepository.save(claim);
            return;
        }

        UUID recipientId = claim.getRecipient().getId();

        Wallet wallet = walletRepository.findByUserIdForUpdate(recipientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Recipient wallet was not found: " + recipientId
                ));

        BlockchainTransaction confirmedTransaction =
                claim.getBlockchainTransaction();

        if (confirmedTransaction == null
                || confirmedTransaction.getWallet() == null
                || !confirmedTransaction.getWallet().getId()
                .equals(wallet.getId())) {
            throw new IllegalStateException(
                    "The locked recipient wallet does not match the "
                            + "confirmed reward transaction"
            );
        }

        int amount = claim.getAmountTokens();

        if (amount <= 0) {
            throw new IllegalStateException(
                    "Reward claim amount must be greater than zero"
            );
        }

        wallet.setAvailableTokens(
                Math.addExact(wallet.getAvailableTokens(), amount)
        );

        walletRepository.save(wallet);

        TokenTransaction walletTransaction =
                TokenTransaction.builder()
                        .wallet(wallet)
                        .transactionType(TransactionType.REWARD)
                        .amount(amount)
                        .description(buildWalletDescription(claim))
                        .rewardClaim(claim)
                        .build();

        tokenTransactionRepository.save(walletTransaction);

        claim.setStatus(RewardClaimStatus.CONFIRMED);
        claim.setConfirmedAt(Instant.now());
        claim.setProcessingAt(null);
        claim.setLastError(null);

        rewardClaimRepository.save(claim);
    }

    private RewardClaim getClaimForUpdate(UUID claimId) {
        return rewardClaimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reward claim was not found: " + claimId
                ));
    }

    private String buildWalletDescription(RewardClaim claim) {
        return switch (claim.getCampaign()) {
            case DIRECT_BARTER_COMPLETION ->
                    "Direct Barter completion reward (source "
                            + claim.getSourceId() + ")";

            case DAILY_ACTIVITY_STREAK_30 ->
                    "30-day daily activity streak reward";

            case REFERRAL_QUALIFIED ->
                    "Qualified referral reward (source "
                            + claim.getSourceId() + ")";
        };
    }

    public record ClaimSnapshot(
            UUID id,
            UUID recipientId,
            RewardCampaign campaign,
            String claimKey,
            Integer amountTokens,
            RewardClaimStatus status,
            UUID blockchainTransactionId,
            Integer attemptCount
    ) {}
}