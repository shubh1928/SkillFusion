package com.barterlink.disputes.service;

import com.barterlink.blockchain.transaction.BlockchainTransactionReconciliationService;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeSettlementRecoveryScheduler {

    private final BlockchainTransactionReconciliationService
            transactionReconciliationService;

    private final DisputeCaseRepository disputeCaseRepository;

    private final DisputeSettlementService disputeSettlementService;

    @Scheduled(
            fixedDelayString =
                    "${barterlink.disputes.settlement-recovery-delay-ms:60000}"
    )
    public void recoverPendingDisputeSettlements() {

        /*
         * Reconcile recorded blockchain hashes first.
         * This service does not submit a new transaction.
         */
        try {
            transactionReconciliationService
                    .reconcileSubmittedTransactions();

        } catch (RuntimeException exception) {
            log.error(
                    "Blockchain transaction reconciliation failed during dispute recovery.",
                    exception);
        }

        List<DisputeCase> candidates = new ArrayList<>();

        try {
            candidates.addAll(
                    disputeCaseRepository.findAllByStatusOrderByCreatedAtAsc(
                            DisputeCaseStatus.DECIDED));

            candidates.addAll(
                    disputeCaseRepository.findAllByStatusOrderByCreatedAtAsc(
                            DisputeCaseStatus.SETTLEMENT_PENDING));

        } catch (RuntimeException exception) {
            log.error(
                    "Could not load disputes for settlement recovery.",
                    exception);
            return;
        }

        for (DisputeCase disputeCase : candidates) {
            try {
                /*
                 * The settlement service itself preserves the milestone
                 * restriction and returns unsupported cases as pending.
                 */
                DisputeSettlementService.SettlementResult result =
                        disputeSettlementService.settle(
                                disputeCase.getId());

                if (!result.settled()) {
                    log.warn(
                            "Dispute recovery remains pending. disputeId={}, reason={}",
                            disputeCase.getId(),
                            result.message());
                }

            } catch (RuntimeException exception) {
                log.error(
                        "Unexpected error recovering dispute settlement. disputeId={}",
                        disputeCase.getId(),
                        exception);
            }
        }
    }
}