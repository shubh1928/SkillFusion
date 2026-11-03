package com.barterlink.disputes.repository;

import com.barterlink.disputes.entity.DisputePanelAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import com.barterlink.disputes.enums.DisputePanelAssignmentStatus;

import java.util.List;
import java.util.UUID;
import java.util.Collection;

public interface DisputePanelAssignmentRepository
        extends JpaRepository<DisputePanelAssignment, UUID> {

    List<DisputePanelAssignment> findByDisputeCase_Id(
            UUID disputeId
    );

    boolean existsByDisputeCase_IdAndPanelist_Id(
            UUID disputeId,
            UUID panelistId
    );

    boolean existsByDisputeCase_IdAndPanelist_IdAndStatusIn(
            UUID disputeId,
            UUID panelistId,
            List<DisputePanelAssignmentStatus> statuses
    );

    List<DisputePanelAssignment>
    findByPanelist_IdAndStatusInOrderByAssignedAtDesc(
            UUID panelistId,
            Collection<DisputePanelAssignmentStatus> statuses
    );
}