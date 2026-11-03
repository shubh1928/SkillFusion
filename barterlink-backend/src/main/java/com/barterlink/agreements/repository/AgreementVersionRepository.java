package com.barterlink.agreements.repository;

import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.negotiation.entity.Negotiation;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface AgreementVersionRepository
        extends JpaRepository<AgreementVersion, UUID> {

    List<AgreementVersion> findByNegotiation_IdOrderByVersionNumberDesc(
            UUID negotiationId);

    Optional<AgreementVersion> findByNegotiationAndVersionNumber(
            Negotiation negotiation,
            Integer versionNumber);

    List<AgreementVersion> findByNegotiation_IdAndStatusOrderByVersionNumberDesc(
            UUID negotiationId,
            AgreementVersionStatus status);

    Optional<AgreementVersion> findTopByNegotiation_IdOrderByVersionNumberDesc(
            UUID negotiationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select v
        from AgreementVersion v
        where v.id = :versionId
        """)
    Optional<AgreementVersion> findByIdForUpdate(
            @Param("versionId") UUID versionId
    );

}