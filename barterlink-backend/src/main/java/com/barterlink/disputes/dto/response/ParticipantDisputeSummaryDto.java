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
public class ParticipantDisputeSummaryDto {

    private UUID disputeId;
    private UUID sessionId;
    private UUID agreementId;

    private String currentUserRole;

    private String claimantName;
    private String respondentName;

    private String requesterName;
    private String providerName;

    private String reason;

    private DisputeCaseStatus status;
    private Integer evidenceVersion;

    private Instant createdAt;
    private Instant panelAssignedAt;

    private DisputeDecisionOutcome decisionOutcome;
    private String decisionSummary;
    private Instant decisionDeclaredAt;

    /*
     * The panel has five seats.
     *
     * While voting is open, only the number of votes submitted
     * is exposed. The outcome split and rationales remain null.
     */
    private int panelSize;
    private int votesSubmitted;

    /*
     * Populated only after a decision has been declared.
     */
    private Long requesterVotes;
    private Long providerVotes;

    private List<AnonymousPanelVoteRationaleDto>
            anonymousVoteRationales;

    /*
     * Settlement details are calculated from the agreement,
     * dispute state, blockchain transaction tracking and
     * application wallet transaction records.
     */
    private String exchangeMode;
    private String releaseMode;

    private Integer tokenAmount;

    /*
     * Expected status values:
     *
     * AWAITING_DECISION
     * NOT_APPLICABLE
     * UNSUPPORTED
     * PENDING
     * FAILED
     * BLOCKCHAIN_CONFIRMED
     * CONFIRMED
     * UNVERIFIED
     */
    private String settlementStatus;

    private String settlementDirection;
    private String settlementRecipientName;
    private String settlementMessage;

    private String blockchainTransactionStatus;
    private String blockchainTransactionHash;
    private String blockchainTransactionUrl;
}