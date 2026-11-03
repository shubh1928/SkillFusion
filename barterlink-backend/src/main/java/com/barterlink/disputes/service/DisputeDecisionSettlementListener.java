package com.barterlink.disputes.service;

import com.barterlink.disputes.event.DisputeDecisionDeclaredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeDecisionSettlementListener {

    private final DisputeSettlementService disputeSettlementService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecisionDeclared(DisputeDecisionDeclaredEvent event) {
        DisputeSettlementService.SettlementResult result =
                disputeSettlementService.settle(event.disputeId());

        if (!result.settled()) {
            log.warn(
                    "Dispute decision was committed but settlement remains pending. disputeId={}, reason={}",
                    event.disputeId(),
                    result.message());
        }
    }
}