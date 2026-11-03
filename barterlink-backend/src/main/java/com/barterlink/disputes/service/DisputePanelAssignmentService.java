package com.barterlink.disputes.service;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.entity.DisputePanelAssignment;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputePanelAssignmentStatus;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.disputes.repository.DisputePanelAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputePanelAssignmentService {

    private static final int PANEL_SIZE = 5;

    private final UserRepository userRepository;
    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputePanelAssignmentRepository
            disputePanelAssignmentRepository;

    @Transactional
    public List<DisputePanelAssignment> assignRandomPanel(
            DisputeCase disputeCase) {

        if (disputeCase == null || disputeCase.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved dispute case is required.");
        }

        if (disputeCase.getClaimant() == null
                || disputeCase.getRespondent() == null) {
            throw new IllegalStateException(
                    "Both dispute participants must be identified.");
        }

        if (disputeCase.getStatus()
                != DisputeCaseStatus.PANEL_ASSIGNMENT_PENDING) {
            throw new IllegalStateException(
                    "The dispute is not awaiting panel assignment.");
        }

        List<DisputePanelAssignment> existingAssignments =
                disputePanelAssignmentRepository
                        .findByDisputeCase_Id(disputeCase.getId());

        if (!existingAssignments.isEmpty()) {
            throw new IllegalStateException(
                    "Panel assignments already exist for this dispute.");
        }

        UUID claimantId = disputeCase.getClaimant().getId();
        UUID respondentId = disputeCase.getRespondent().getId();

        List<UUID> excludedUserIds =
                List.of(claimantId, respondentId);

        List<User> selectedPanelists =
                userRepository.findRandomEligiblePanelists(
                        excludedUserIds,
                        PageRequest.of(0, PANEL_SIZE)
                );

        if (selectedPanelists.size() != PANEL_SIZE) {
            throw new IllegalStateException(
                    "Five eligible panel members are not available. "
                            + "The dispute panel was not created.");
        }

        Instant assignedAt = Instant.now();

        List<DisputePanelAssignment> assignments =
                new java.util.ArrayList<>();

        for (int i = 0; i < selectedPanelists.size(); i++) {
            DisputePanelAssignment assignment =
                    DisputePanelAssignment.builder()
                            .disputeCase(disputeCase)
                            .panelist(selectedPanelists.get(i))
                            .seatNumber(i + 1)
                            .status(
                                    DisputePanelAssignmentStatus.ASSIGNED)
                            .assignedAt(assignedAt)
                            .statusChangedAt(assignedAt)
                            .build();

            assignments.add(assignment);
        }

        List<DisputePanelAssignment> savedAssignments =
                disputePanelAssignmentRepository.saveAll(assignments);

        disputeCase.setPanelAssignedAt(assignedAt);
        disputeCase.setStatus(DisputeCaseStatus.EVIDENCE_OPEN);

        disputeCaseRepository.save(disputeCase);

        return savedAssignments;
    }
}