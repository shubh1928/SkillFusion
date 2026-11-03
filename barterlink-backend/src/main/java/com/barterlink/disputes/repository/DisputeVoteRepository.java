package com.barterlink.disputes.repository;

import com.barterlink.disputes.entity.DisputeVote;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface DisputeVoteRepository
        extends JpaRepository<DisputeVote, UUID> {

    List<DisputeVote>
    findByDisputeCase_IdAndEvidenceVersionOrderByCreatedAtAsc(
            UUID disputeId,
            Integer evidenceVersion
    );

    List<DisputeVote>
    findByDisputeCase_IdOrderByCreatedAtAsc(UUID disputeId);

    boolean existsByDisputeCase_IdAndPanelAssignment_IdAndEvidenceVersion(
            UUID disputeId,
            UUID panelAssignmentId,
            Integer evidenceVersion
    );

    long countByDisputeCase_IdAndEvidenceVersionAndOutcome(
            UUID disputeId,
            Integer evidenceVersion,
            DisputeDecisionOutcome outcome
    );

    Optional<DisputeVote>
    findFirstByDisputeCase_IdAndPanelAssignment_IdAndEvidenceVersionOrderByCreatedAtDesc(
            UUID disputeId,
            UUID panelAssignmentId,
            Integer evidenceVersion
    );
}