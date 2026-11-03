package com.barterlink.disputes.dto.response;

import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeVoteResponseDto {

    private UUID voteId;
    private UUID disputeId;
    private DisputeDecisionOutcome outcome;
    private String reason;
    private Integer evidenceVersion;
    private Instant createdAt;

    private DisputeCaseStatus caseStatus;
    private DisputeDecisionOutcome decisionOutcome;
    private String decisionSummary;
    private Instant decisionDeclaredAt;

    private long requesterVotes;
    private long providerVotes;
}