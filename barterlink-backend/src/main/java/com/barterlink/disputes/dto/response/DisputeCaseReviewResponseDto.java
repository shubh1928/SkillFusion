package com.barterlink.disputes.dto.response;

import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeCaseReviewResponseDto {

    private UUID disputeId;
    private UUID sessionId;
    private UUID agreementId;

    private UUID claimantId;
    private String claimantName;

    private UUID respondentId;
    private String respondentName;

    private String reason;
    private DisputeCaseStatus status;

    private Integer evidenceVersion;
    private Instant createdAt;
    private Instant panelAssignedAt;

    private DisputeDecisionOutcome decisionOutcome;
    private String decisionSummary;
    private Instant decisionDeclaredAt;

    private Integer panelSeatNumber;

    // Voting status for the authenticated panelist
    // on the CURRENT evidence version.
    private boolean currentUserHasVoted;
    private DisputeDecisionOutcome currentUserVoteOutcome;
    private String currentUserVoteReason;
    private Instant currentUserVotedAt;

    private List<DisputeEvidenceItemDto> evidence;
}