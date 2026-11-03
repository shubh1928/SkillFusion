
package com.barterlink.agreements.controller;

import com.barterlink.agreements.enums.AgreementMilestoneStatus;
import com.barterlink.agreements.service.impl.MilestoneCompletionStateService;
import com.barterlink.agreements.service.impl.MilestoneSettlementCoordinator;
import com.barterlink.agreements.service.impl.SubsequentMilestoneFundingCoordinator;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.sessions.service.SessionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
public class AgreementMilestoneController {

    private final CurrentUserUtil currentUserUtil;
    private final MilestoneCompletionStateService completionStateService;
    private final MilestoneSettlementCoordinator settlementCoordinator;
    private final AgreementRepository agreementRepository;
    private final SubsequentMilestoneFundingCoordinator subsequentFundingCoordinator;
    private final SessionService sessionService;

    @PostMapping("/{milestoneId}/confirm-completion")
    public ApiResponse<MilestoneCompletionResponse> confirmCompletion(
            @PathVariable UUID milestoneId) {

        UUID userId = currentUserUtil.getCurrentUserId();

        MilestoneCompletionStateService.ConfirmationDecision decision =
                completionStateService.recordCompletion(
                        milestoneId,
                        userId);

        boolean settled =
                decision.status() == AgreementMilestoneStatus.RELEASED;

        AgreementMilestoneStatus responseStatus =
                decision.status();

        String message = decision.message();

        if (decision.releaseTriggered()
                && decision.status()
                == AgreementMilestoneStatus.RELEASE_PENDING
                && decision.requesterConfirmed()
                && decision.providerConfirmed()) {

            MilestoneSettlementCoordinator.SettlementResult result =
                    settlementCoordinator.settleConfirmedMilestone(
                            decision);

            settled = result.settled();
            message = result.message();

            if (settled) {
                responseStatus = AgreementMilestoneStatus.RELEASED;
            }
        }

        MilestoneCompletionResponse response =
                new MilestoneCompletionResponse(
                        decision.milestoneId(),
                        decision.milestoneNumber(),
                        responseStatus,
                        decision.requesterConfirmed(),
                        decision.providerConfirmed(),
                        settled,
                        message);

        return ApiResponse.success(message, response);
    }

    public record MilestoneCompletionResponse(
            UUID milestoneId,
            Integer milestoneNumber,
            AgreementMilestoneStatus status,
            boolean requesterConfirmed,
            boolean providerConfirmed,
            boolean settled,
            String message) {
    }

    @PostMapping("/agreements/{agreementId}/fund-next")
    public ApiResponse<SubsequentMilestoneFundingCoordinator.FundingResult>
    retryNextMilestoneFunding(
            @PathVariable UUID agreementId) {

        UUID userId = currentUserUtil.getCurrentUserId();

        ServiceAgreement agreement =
                agreementRepository.findById(agreementId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        if (!agreement.getRequester().getId().equals(userId)) {
            throw new BusinessException(
                    "Only the requester can retry funding from the requester's wallet");
        }

        SubsequentMilestoneFundingCoordinator.FundingResult result =
                subsequentFundingCoordinator.fundNextMilestone(
                        agreementId);

        String responseMessage = result.message();

        /*
         * Recovery path:
         * If no further milestone needs funding, retry final session completion.
         */
        if (result.allMilestonesReleased()) {

            boolean completed =
                    sessionService.completeMilestoneSessionIfFinal(
                            agreementId);

            if (!completed) {
                throw new BusinessException(
                        "All milestones appear to be released, but final session "
                                + "completion is still pending. "
                                + "Reconciliation is required.");
            }

            responseMessage =
                    "All milestones have been released. "
                            + "The agreement and session are completed.";
        }

        return ApiResponse.success(
                responseMessage,
                result);
    }
}
