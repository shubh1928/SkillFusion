package com.barterlink.disputes.dto.response;

import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import com.barterlink.disputes.enums.DisputePanelAssignmentStatus;
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
public class AssignedDisputeSummaryDto {

    private UUID disputeId;
    private UUID sessionId;
    private UUID agreementId;

    private String claimantName;
    private String respondentName;
    private String reason;

    private DisputeCaseStatus caseStatus;
    private Integer evidenceVersion;

    private Integer panelSeatNumber;
    private DisputePanelAssignmentStatus assignmentStatus;

    private Instant createdAt;
    private Instant panelAssignedAt;

    private DisputeDecisionOutcome decisionOutcome;
    private String decisionSummary;
    private Instant decisionDeclaredAt;
}