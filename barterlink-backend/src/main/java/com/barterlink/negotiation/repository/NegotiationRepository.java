package com.barterlink.negotiation.repository;

import com.barterlink.auth.entities.User;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.enums.NegotiationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NegotiationRepository
        extends JpaRepository<Negotiation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Negotiation n WHERE n.id = :negotiationId")
    Optional<Negotiation> findByIdForUpdate(
            @Param("negotiationId") UUID negotiationId
    );

    boolean existsByServiceRequestAndProvider(
            ServiceRequest serviceRequest,
            User provider);

    Optional<Negotiation> findByServiceRequestAndProvider(
            ServiceRequest serviceRequest,
            User provider);

    List<Negotiation> findByServiceRequest(
            ServiceRequest serviceRequest);

    List<Negotiation> findByRequester(
            User requester);

    List<Negotiation> findByProvider(
            User provider);

    List<Negotiation> findByStatus(
            NegotiationStatus status);

    List<Negotiation> findByServiceRequestAndStatus(
            ServiceRequest serviceRequest,
            NegotiationStatus status);

    long countByRequester(
            User requester
    );

    long countByRequesterAndStatus(
            User requester,
            NegotiationStatus status
    );

    long countByRequesterAndCreatedAtAfter(
            User requester,
            Instant createdAt
    );

    @Query("""
        SELECT COUNT(n)
        FROM Negotiation n
        WHERE (n.requester.id = :userId OR n.provider.id = :userId)
          AND n.status IN :statuses
        """)
    long countUnresolvedForUser(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<NegotiationStatus> statuses
    );

}