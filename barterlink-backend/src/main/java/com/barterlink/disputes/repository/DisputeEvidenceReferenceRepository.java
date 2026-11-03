package com.barterlink.disputes.repository;

import com.barterlink.disputes.entity.DisputeEvidenceReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeEvidenceReferenceRepository
        extends JpaRepository<DisputeEvidenceReference, UUID> {

    List<DisputeEvidenceReference>
    findByDisputeCase_IdOrderByAttachedAtAsc(UUID disputeId);

    boolean existsByDisputeCase_IdAndSessionProof_Id(
            UUID disputeId,
            UUID sessionProofId
    );

    boolean existsByDisputeCase_IdAndNegotiationMessage_Id(
            UUID disputeId,
            UUID negotiationMessageId
    );

    boolean existsByDisputeCase_IdAndAgreementVersion_Id(
            UUID disputeId,
            UUID agreementVersionId
    );
}