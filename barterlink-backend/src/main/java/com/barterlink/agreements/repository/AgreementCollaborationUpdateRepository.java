
package com.barterlink.agreements.repository;

import com.barterlink.agreements.entity.AgreementCollaborationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementCollaborationUpdateRepository
        extends JpaRepository<AgreementCollaborationUpdate, UUID> {

    // Existing version-level collaboration queries

    List<AgreementCollaborationUpdate>
    findByVersion_IdOrderByCreatedAtAscIdAsc(UUID versionId);

    Optional<AgreementCollaborationUpdate>
    findByVersion_IdAndClientUpdateId(
            UUID versionId,
            String clientUpdateId
    );

    boolean existsByVersion_IdAndClientUpdateId(
            UUID versionId,
            String clientUpdateId
    );

    boolean existsByVersion_Id(UUID versionId);


    // New negotiation-level collaboration queries

    List<AgreementCollaborationUpdate>
    findByNegotiation_IdOrderByCreatedAtAscIdAsc(
            UUID negotiationId
    );

    Optional<AgreementCollaborationUpdate>
    findByNegotiation_IdAndClientUpdateId(
            UUID negotiationId,
            String clientUpdateId
    );
}
