package com.barterlink.rewards.service;

import com.barterlink.blockchain.transaction.BlockchainTransactionReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "barterlink.rewards.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class RewardClaimScheduler {

    private static final int MAX_CLAIMS_PER_CYCLE = 20;

    private final BlockchainTransactionReconciliationService
            transactionReconciliationService;

    private final RewardClaimLifecycleService lifecycleService;

    private final RewardClaimProcessor rewardClaimProcessor;

    private final ReferralAttributionService referralAttributionService;

    @Scheduled(
            fixedDelayString =
                    "${barterlink.rewards.scheduler.fixed-delay-ms:15000}"
    )
    public void processRewards() {

        // Reconcile recorded hashes first. This does not resubmit transactions.
        try {
            transactionReconciliationService
                    .reconcileSubmittedTransactions();

        } catch (RuntimeException exception) {
            log.error(
                    "Could not reconcile blockchain transactions before reward processing",
                    exception
            );
        }

        try {
            lifecycleService.recoverStaleProcessingClaims();
            rewardClaimProcessor.synchronizeSubmittedClaims();
            referralAttributionService.retryQualifiedReferrals();
            referralAttributionService.retryWaitingReferrals();


        } catch (RuntimeException exception) {
            log.error(
                    "Could not recover or synchronize reward claims",
                    exception
            );
        }

        for (int i = 0; i < MAX_CLAIMS_PER_CYCLE; i++) {
            UUID claimId;

            try {
                claimId = lifecycleService.claimNextPending();

            } catch (RuntimeException exception) {
                log.error(
                        "Could not claim the next pending reward record",
                        exception
                );
                return;
            }

            if (claimId == null) {
                return;
            }

            rewardClaimProcessor.process(claimId);
        }
    }
}