package com.barterlink.agreements.repository;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.auth.entities.User;
import com.barterlink.negotiation.entity.Negotiation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementRepository
        extends JpaRepository<ServiceAgreement, UUID> {

    Optional<ServiceAgreement> findByNegotiation(
            Negotiation negotiation);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ServiceAgreement a WHERE a.id = :agreementId")
    Optional<ServiceAgreement> findByIdForFinalizationUpdate(
            @Param("agreementId") UUID agreementId);

    List<ServiceAgreement> findByRequester(
            User requester);

    List<ServiceAgreement> findByProvider(
            User provider);

    List<ServiceAgreement> findByStatus(
            AgreementStatus status);

    long countByRequester(
            User requester
    );

    long countByRequesterAndStatus(
            User requester,
            AgreementStatus status
    );

    @Query("""
        SELECT COUNT(a)
        FROM ServiceAgreement a
        WHERE (a.requester.id = :userId OR a.provider.id = :userId)
          AND a.status IN :statuses
        """)
    long countUnresolvedForUser(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<AgreementStatus> statuses
    );

}