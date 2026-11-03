package com.barterlink.negotiation.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.negotiation.dto.request.SendNegotiationMessageRequestDto;
import com.barterlink.negotiation.dto.request.UpdateNegotiationProposalRequestDto;
import com.barterlink.negotiation.dto.response.NegotiationMessageResponseDto;
import com.barterlink.negotiation.dto.response.NegotiationResponseDto;
import com.barterlink.negotiation.service.NegotiationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.barterlink.auth.services.UserActivityService;
import com.barterlink.negotiation.dto.request.SelectAgreementReleaseModeRequestDto;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import java.util.Objects;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/negotiations")
@RequiredArgsConstructor
public class NegotiationController {

    private final NegotiationService negotiationService;
    private final CurrentUserUtil currentUserUtil;
    private final UserActivityService userActivityService;

    @PostMapping("/interests/{interestId}")
    public ApiResponse<NegotiationResponseDto> openNegotiation(
            @PathVariable UUID interestId) {

        NegotiationResponseDto result =
                negotiationService.openNegotiation(interestId);

        userActivityService.recordCurrentUserActivity();

        return ApiResponse.success(
                "Negotiation opened successfully",
                result
        );
    }

    @GetMapping("/request/{requestId}/continue-chat")
    public ApiResponse<NegotiationResponseDto> continueChat(
            @PathVariable UUID requestId) {

        return ApiResponse.success(

                negotiationService.continueChat(

                        requestId,

                        currentUserUtil.getCurrentUserId()

                )

        );
    }

    @GetMapping("/{negotiationId}")
    public ApiResponse<NegotiationResponseDto> getNegotiation(
            @PathVariable UUID negotiationId) {

        return ApiResponse.success(
                negotiationService.getNegotiation(
                        negotiationId));
    }

    @GetMapping("/request/{requestId}")
    public ApiResponse<List<NegotiationResponseDto>> getRequestNegotiations(
            @PathVariable UUID requestId) {

        return ApiResponse.success(
                negotiationService.getRequestNegotiations(
                        requestId));
    }

    @GetMapping("/provider")
    public ApiResponse<List<NegotiationResponseDto>> getProviderNegotiations() {

        return ApiResponse.success(
                negotiationService.getProviderNegotiations(
                        currentUserUtil.getCurrentUserId()));
    }

    @GetMapping("/{negotiationId}/messages")
    public ApiResponse<List<NegotiationMessageResponseDto>> getMessages(
            @PathVariable UUID negotiationId) {

        return ApiResponse.success(
                negotiationService.getMessages(
                        negotiationId));
    }

    @PostMapping("/{negotiationId}/messages")
    public ApiResponse<NegotiationMessageResponseDto> sendMessage(
            @PathVariable UUID negotiationId,
            @Valid @RequestBody
            SendNegotiationMessageRequestDto request) {

        NegotiationMessageResponseDto result =
                negotiationService.sendMessage(
                        negotiationId,
                        currentUserUtil.getCurrentUserId(),
                        request
                );

        return ApiResponse.success(
                "Message sent successfully",
                result
        );
    }

    @PutMapping("/{negotiationId}/proposal")
    public ApiResponse<NegotiationResponseDto> updateProposal(
            @PathVariable UUID negotiationId,
            @Valid @RequestBody
            UpdateNegotiationProposalRequestDto request) {

        /*
         * Capture the persisted values before applying the requested update.
         * The service performs the actual validation and authorization.
         */
        NegotiationResponseDto previous =
                negotiationService.getNegotiation(negotiationId);

        boolean proposalChanged =
                !Objects.equals(
                        previous.getOfferTitle(),
                        request.getOfferTitle()
                )
                        || !Objects.equals(
                        previous.getOfferDescription(),
                        request.getOfferDescription()
                )
                        || !Objects.equals(
                        previous.getRequestTitle(),
                        request.getRequestTitle()
                )
                        || !Objects.equals(
                        previous.getRequestDescription(),
                        request.getRequestDescription()
                )
                        || !Objects.equals(
                        previous.getExchangeMode(),
                        request.getExchangeMode()
                )
                        || !Objects.equals(
                        previous.getEstimatedHours(),
                        request.getEstimatedHours()
                )
                        || !Objects.equals(
                        previous.getTokenAmount(),
                        request.getTokenAmount()
                );

        NegotiationResponseDto result =
                negotiationService.updateProposal(
                        negotiationId,
                        currentUserUtil.getCurrentUserId(),
                        request
                );

        if (!proposalChanged) {
            userActivityService.recordCurrentUserActivity();
        }

        return ApiResponse.success(
                "Proposal updated successfully",
                result
        );
    }

    @PutMapping("/{negotiationId}/release-mode")
    public ApiResponse<NegotiationResponseDto> selectAgreementReleaseMode(
            @PathVariable UUID negotiationId,
            @Valid @RequestBody
            SelectAgreementReleaseModeRequestDto request) {

        /*
         * Read the previously saved selection for this participant.
         */
        NegotiationResponseDto previous =
                negotiationService.getNegotiation(negotiationId);

        UUID currentUserId = currentUserUtil.getCurrentUserId();

        boolean isRequester =
                Objects.equals(
                        previous.getRequesterId(),
                        currentUserId
                );

        AgreementReleaseMode previousMode =
                isRequester
                        ? previous.getRequesterReleaseMode()
                        : previous.getProviderReleaseMode();

        Integer previousMilestoneCount =
                isRequester
                        ? previous.getRequesterMilestoneCount()
                        : previous.getProviderMilestoneCount();

        boolean releaseSelectionChanged =
                !Objects.equals(
                        previousMode,
                        request.getReleaseMode()
                )
                        || !Objects.equals(
                        previousMilestoneCount,
                        request.getMilestoneCount()
                );

        /*
         * The existing service validates permissions, exchange mode,
         * release-mode rules, milestone limits and agreement-version state.
         */
        NegotiationResponseDto result =
                negotiationService.selectAgreementReleaseMode(
                        negotiationId,
                        currentUserId,
                        request
                );

        if (!releaseSelectionChanged) {
            userActivityService.recordCurrentUserActivity();
        }

        return ApiResponse.success(
                "Release mode selection saved",
                result
        );
    }

    @PostMapping("/{negotiationId}/request-session")
    public ApiResponse<NegotiationResponseDto> requestSession(
            @PathVariable UUID negotiationId) {

        NegotiationResponseDto result =
                negotiationService.requestSession(
                        negotiationId,
                        currentUserUtil.getCurrentUserId()
                );

        return ApiResponse.success(
                "Session request sent successfully",
                result
        );
    }

        @PostMapping("/{negotiationId}/accept-session")
        public ApiResponse<NegotiationResponseDto> acceptSessionRequest(
                @PathVariable UUID negotiationId) {

            NegotiationResponseDto result =
                    negotiationService.acceptSessionRequest(
                            negotiationId,
                            currentUserUtil.getCurrentUserId()
                    );

            return ApiResponse.success(
                    "Session request accepted",
                    result
            );
        }

    @PostMapping("/{negotiationId}/reject-session")
    public ApiResponse<NegotiationResponseDto> rejectSessionRequest(
            @PathVariable UUID negotiationId) {

        NegotiationResponseDto result =
                negotiationService.rejectSessionRequest(
                        negotiationId,
                        currentUserUtil.getCurrentUserId()
                );

        userActivityService.recordCurrentUserActivity();

        return ApiResponse.success(
                "Session request rejected",
                result
        );
    }

    @DeleteMapping("/{negotiationId}")
    public ApiResponse<NegotiationResponseDto> cancelNegotiation(
            @PathVariable UUID negotiationId) {

        NegotiationResponseDto result =
                negotiationService.cancelNegotiation(
                        negotiationId,
                        currentUserUtil.getCurrentUserId()
                );

        userActivityService.recordCurrentUserActivity();

        return ApiResponse.success(
                "Negotiation cancelled successfully",
                result
        );
    }
}