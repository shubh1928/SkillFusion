package com.barterlink.disputes.service;

import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.entity.DisputePanelAssignment;
import com.barterlink.disputes.entity.DisputeVote;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import com.barterlink.disputes.enums.DisputePanelAssignmentStatus;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.disputes.repository.DisputePanelAssignmentRepository;
import com.barterlink.disputes.repository.DisputeVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.disputes.dto.response.DisputeVoteResponseDto;
import com.barterlink.disputes.event.DisputeDecisionDeclaredEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeVotingService {

    private static final int PANEL_SIZE = 5;
    private static final int MAJORITY_REQUIRED = 3;
    private static final int MAX_REASON_LENGTH = 5000;

    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputePanelAssignmentRepository
            disputePanelAssignmentRepository;
    private final DisputeVoteRepository disputeVoteRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public DisputeVoteResponseDto castVote(
            UUID disputeId,
            UUID panelistId,
            DisputeDecisionOutcome outcome,
            String reason) {

        if (disputeId == null || panelistId == null) {
            throw new BusinessException(
                    "Dispute and panelist IDs are required");
        }

        if (outcome == null) {
            throw new BusinessException(
                    "A decision outcome is required");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(
                    "A reason for the vote is required");
        }

        String trimmedReason = reason.trim();

        if (trimmedReason.length() > MAX_REASON_LENGTH) {
            throw new BusinessException(
                    "The voting reason cannot exceed 5000 characters");
        }

        // Lock the case before checking status, evidence version and votes.
        DisputeCase disputeCase =
                disputeCaseRepository.findByIdForUpdate(disputeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Dispute case not found"));

        if (disputeCase.getStatus() != DisputeCaseStatus.EVIDENCE_OPEN
                && disputeCase.getStatus() != DisputeCaseStatus.VOTING) {

            throw new BusinessException(
                    "Voting is not open for this dispute");
        }

        UUID claimantId = disputeCase.getClaimant().getId();
        UUID respondentId = disputeCase.getRespondent().getId();

        if (panelistId.equals(claimantId)
                || panelistId.equals(respondentId)) {

            throw new BusinessException(
                    "Dispute participants cannot vote on their own case");
        }

        // Find this user's assignment to the case.
        DisputePanelAssignment assignment =
                disputePanelAssignmentRepository
                        .findByDisputeCase_Id(disputeId)
                        .stream()
                        .filter(item ->
                                item.getPanelist().getId().equals(panelistId))
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "You are not assigned to this dispute panel"));

        if (assignment.getStatus()
                != DisputePanelAssignmentStatus.ASSIGNED
                && assignment.getStatus()
                != DisputePanelAssignmentStatus.VOTED) {

            throw new BusinessException(
                    "Your panel assignment is not eligible to vote");
        }

        int currentEvidenceVersion =
                disputeCase.getEvidenceVersion() == null
                        ? 0
                        : disputeCase.getEvidenceVersion();

        // One vote per panelist per evidence version.
        boolean alreadyVoted =
                disputeVoteRepository
                        .existsByDisputeCase_IdAndPanelAssignment_IdAndEvidenceVersion(
                                disputeId,
                                assignment.getId(),
                                currentEvidenceVersion
                        );

        if (alreadyVoted) {
            throw new BusinessException(
                    "You have already voted on this evidence version");
        }

        Instant now = Instant.now();

        DisputeVote vote = DisputeVote.builder()
                .disputeCase(disputeCase)
                .panelAssignment(assignment)
                .outcome(outcome)
                .reason(trimmedReason)
                .evidenceVersion(currentEvidenceVersion)
                .createdAt(now)
                .build();

        DisputeVote savedVote =
                disputeVoteRepository.save(vote);

        // Ensure the vote is visible to the following tally queries.
        disputeVoteRepository.flush();

        assignment.setStatus(DisputePanelAssignmentStatus.VOTED);
        assignment.setStatusChangedAt(now);

        disputePanelAssignmentRepository.save(assignment);

        disputeCase.setStatus(DisputeCaseStatus.VOTING);

        long requesterVotes =
                disputeVoteRepository
                        .countByDisputeCase_IdAndEvidenceVersionAndOutcome(
                                disputeId,
                                currentEvidenceVersion,
                                DisputeDecisionOutcome.REQUESTER_FAVORED
                        );

        long providerVotes =
                disputeVoteRepository
                        .countByDisputeCase_IdAndEvidenceVersionAndOutcome(
                                disputeId,
                                currentEvidenceVersion,
                                DisputeDecisionOutcome.PROVIDER_FAVORED
                        );

        if (requesterVotes >= MAJORITY_REQUIRED) {

            declareDecision(
                    disputeCase,
                    DisputeDecisionOutcome.REQUESTER_FAVORED,
                    requesterVotes,
                    currentEvidenceVersion,
                    now
            );

        } else if (providerVotes >= MAJORITY_REQUIRED) {

            declareDecision(
                    disputeCase,
                    DisputeDecisionOutcome.PROVIDER_FAVORED,
                    providerVotes,
                    currentEvidenceVersion,
                    now
            );
        }

        disputeCaseRepository.save(disputeCase);

        if (disputeCase.getStatus() == DisputeCaseStatus.DECIDED) {
            applicationEventPublisher.publishEvent(
                    new DisputeDecisionDeclaredEvent(disputeCase.getId())
            );
        }

        return DisputeVoteResponseDto.builder()
                .voteId(savedVote.getId())
                .disputeId(disputeCase.getId())
                .outcome(savedVote.getOutcome())
                .reason(savedVote.getReason())
                .evidenceVersion(savedVote.getEvidenceVersion())
                .createdAt(savedVote.getCreatedAt())
                .caseStatus(disputeCase.getStatus())
                .decisionOutcome(disputeCase.getDecisionOutcome())
                .decisionSummary(disputeCase.getDecisionSummary())
                .decisionDeclaredAt(disputeCase.getDecisionDeclaredAt())
                .requesterVotes(requesterVotes)
                .providerVotes(providerVotes)
                .build();
    }

    private void declareDecision(
            DisputeCase disputeCase,
            DisputeDecisionOutcome outcome,
            long votesForOutcome,
            int evidenceVersion,
            Instant decisionTime) {

        disputeCase.setDecisionOutcome(outcome);

        disputeCase.setDecisionSummary(
                "Final panel decision: " + outcome
                        + ". Votes supporting this outcome: "
                        + votesForOutcome + " of " + PANEL_SIZE
                        + ". Decision based on evidence version "
                        + evidenceVersion + ". No appeal is available."
        );

        disputeCase.setDecisionDeclaredAt(decisionTime);
        disputeCase.setStatus(DisputeCaseStatus.DECIDED);
    }
}