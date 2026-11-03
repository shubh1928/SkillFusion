package com.barterlink.disputes.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.disputes.dto.request.CastDisputeVoteRequestDto;
import com.barterlink.disputes.dto.response.DisputeVoteResponseDto;
import com.barterlink.disputes.service.DisputeVotingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.barterlink.disputes.dto.response.DisputeCaseReviewResponseDto;
import com.barterlink.disputes.service.DisputeCaseReviewService;
import com.barterlink.disputes.dto.response.AssignedDisputeSummaryDto;
import java.util.List;
import java.util.UUID;
import com.barterlink.disputes.dto.response.ParticipantDisputeSummaryDto;
import com.barterlink.disputes.service.DisputeParticipantService;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeVotingService disputeVotingService;
    private final CurrentUserUtil currentUserUtil;
    private final DisputeCaseReviewService disputeCaseReviewService;
    private final DisputeParticipantService disputeParticipantService;

    @PostMapping("/{disputeId}/votes")
    public ApiResponse<DisputeVoteResponseDto> castVote(
            @PathVariable UUID disputeId,
            @Valid @RequestBody CastDisputeVoteRequestDto request) {

        DisputeVoteResponseDto response =
                disputeVotingService.castVote(
                        disputeId,
                        currentUserUtil.getCurrentUserId(),
                        request.getOutcome(),
                        request.getReason()
                );

        return ApiResponse.success(
                "Vote submitted successfully",
                response
        );
    }

    @GetMapping("/{disputeId}/review")
    public ApiResponse<DisputeCaseReviewResponseDto> getCaseReview(
            @PathVariable UUID disputeId) {

        DisputeCaseReviewResponseDto response =
                disputeCaseReviewService.getCaseReview(
                        disputeId,
                        currentUserUtil.getCurrentUserId()
                );

        return ApiResponse.success(
                "Dispute case review retrieved successfully",
                response
        );
    }

    @GetMapping("/assigned")
    public ApiResponse<List<AssignedDisputeSummaryDto>> getAssignedDisputes() {

        List<AssignedDisputeSummaryDto> response =
                disputeCaseReviewService.getAssignedDisputes(
                        currentUserUtil.getCurrentUserId()
                );

        return ApiResponse.success(
                "Assigned disputes retrieved successfully",
                response
        );
    }

    @GetMapping("/mine")
    public ApiResponse<List<ParticipantDisputeSummaryDto>> getMyDisputes() {

        List<ParticipantDisputeSummaryDto> response =
                disputeParticipantService.getMyDisputes(
                        currentUserUtil.getCurrentUserId());

        return ApiResponse.success(
                "Participant disputes retrieved successfully",
                response);
    }
}