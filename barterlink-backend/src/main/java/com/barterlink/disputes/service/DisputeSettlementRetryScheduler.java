package com.barterlink.disputes.service;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeSettlementRetryScheduler {

    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputeSettlementService disputeSettlementService;

    @Scheduled(
            fixedDelayString =
                    "${barterlink.disputes.settlement-retry-delay-ms:300000}"
    )
    public void retryEligiblePendingSettlements() {

        try {
            List<DisputeCase> pendingCases =
                    disputeCaseRepository
                            .findAllByStatusAndSettlementAttemptClaimedFalseOrderByCreatedAtAsc(
                                    DisputeCaseStatus.SETTLEMENT_PENDING
                            );

            for (DisputeCase disputeCase : pendingCases) {

                if (!isEligibleForAutomaticRetry(disputeCase)) {
                    continue;
                }

                try {
                    DisputeSettlementService.SettlementResult result =
                            disputeSettlementService.settle(
                                    disputeCase.getId()
                            );

                    if (!result.settled()) {
                        log.debug(
                                "Dispute settlement remains pending. "
                                        + "disputeId={}, reason={}",
                                disputeCase.getId(),
                                result.message()
                        );
                    }

                } catch (RuntimeException exception) {
                    log.warn(
                            "Scheduled dispute settlement retry failed. "
                                    + "disputeId={}",
                            disputeCase.getId(),
                            exception
                    );
                }
            }

        } catch (RuntimeException exception) {
            log.error(
                    "Could not load pending disputes for safe settlement retry.",
                    exception
            );
        }
    }

    private boolean isEligibleForAutomaticRetry(DisputeCase disputeCase) {

        if (disputeCase == null
                || disputeCase.getStatus()
                != DisputeCaseStatus.SETTLEMENT_PENDING
                || disputeCase.isSettlementAttemptClaimed()
                || disputeCase.getDecisionOutcome() == null
                || disputeCase.getMilestone() != null) {
            return false;
        }

        ServiceSession session = disputeCase.getSession();

        ServiceAgreement agreement =
                session == null ? null : session.getAgreement();

        if (session == null
                || agreement == null
                || session.getStatus() != SessionStatus.DISPUTED
                || agreement.getStatus() != AgreementStatus.DISPUTED) {
            return false;
        }

        ExchangeMode exchangeMode = agreement.getExchangeMode();

        if (exchangeMode == ExchangeMode.DIRECT_BARTER) {
            return true;
        }

        if (exchangeMode != ExchangeMode.TOKEN_BASED
                && exchangeMode != ExchangeMode.HYBRID) {
            return false;
        }

        if (agreement.getReleaseMode()
                != AgreementReleaseMode.DIRECT_RELEASE) {
            return false;
        }

        Integer tokenAmount = agreement.getAgreedTokenAmount();

        return agreement.getBlockchainEscrowId() != null
                && !agreement.getBlockchainEscrowId().isBlank()
                && tokenAmount != null
                && tokenAmount > 0
                && agreement.getEscrowLockedTokens() != null
                && tokenAmount.equals(agreement.getEscrowLockedTokens());
    }

    @Scheduled(
            fixedDelayString =
                    "${barterlink.disputes.settlement-reconcile-delay-ms:300000}"
    )
    public void reconcileClaimedPendingSettlements() {

        try {
            List<DisputeCase> claimedCases =
                    disputeCaseRepository
                            .findAllByStatusAndSettlementAttemptClaimedTrueOrderByCreatedAtAsc(
                                    DisputeCaseStatus.SETTLEMENT_PENDING
                            );

            for (DisputeCase disputeCase : claimedCases) {

                if (disputeCase == null
                        || disputeCase.getMilestone() != null
                        || disputeCase.getDecisionOutcome() == null) {
                    continue;
                }

                ServiceSession session = disputeCase.getSession();

                ServiceAgreement agreement =
                        session == null ? null : session.getAgreement();

                if (session == null
                        || agreement == null
                        || session.getStatus() != SessionStatus.DISPUTED
                        || agreement.getStatus() != AgreementStatus.DISPUTED) {
                    continue;
                }

                ExchangeMode exchangeMode = agreement.getExchangeMode();

                if ((exchangeMode != ExchangeMode.TOKEN_BASED
                        && exchangeMode != ExchangeMode.HYBRID)
                        || agreement.getReleaseMode()
                        != AgreementReleaseMode.DIRECT_RELEASE) {
                    continue;
                }

                try {
                    DisputeSettlementService.SettlementResult result =
                            disputeSettlementService.settle(disputeCase.getId());

                    if (!result.settled()) {
                        log.debug(
                                "Claimed dispute remains pending reconciliation. "
                                        + "disputeId={}, reason={}",
                                disputeCase.getId(),
                                result.message()
                        );
                    }

                } catch (RuntimeException exception) {
                    log.warn(
                            "Could not reconcile claimed dispute settlement. "
                                    + "disputeId={}",
                            disputeCase.getId(),
                            exception
                    );
                }
            }

        } catch (RuntimeException exception) {
            log.error(
                    "Could not load claimed disputes for settlement reconciliation.",
                    exception
            );
        }
    }
}