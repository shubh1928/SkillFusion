package com.barterlink.disputes.repository;

import com.barterlink.disputes.entity.DisputeCase;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisputeCaseRepository
        extends JpaRepository<DisputeCase, UUID> {

    Optional<DisputeCase>
    findFirstBySession_IdAndStatusInOrderByCreatedAtDesc(
            UUID sessionId,
            Collection<DisputeCaseStatus> statuses
    );

    Optional<DisputeCase> findFirstBySession_IdOrderByCreatedAtDesc(
            UUID sessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT d
    FROM DisputeCase d
    WHERE d.id = :disputeId
    """)
    Optional<DisputeCase> findByIdForUpdate(
            @Param("disputeId") UUID disputeId
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.agreement",
            "milestone"
    })
    List<DisputeCase> findAllByStatusOrderByCreatedAtAsc(
            DisputeCaseStatus status
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.agreement",
            "session.agreement.requester",
            "session.agreement.provider",
            "milestone"
    })
    @Query("""
        SELECT d
        FROM DisputeCase d
        WHERE d.id = :disputeId
        """)
    Optional<DisputeCase> findForSettlementById(
            @Param("disputeId") UUID disputeId
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.agreement",
            "session.agreement.requester",
            "session.agreement.provider",
            "milestone"
    })
    List<DisputeCase> findAllByStatusAndSettlementAttemptClaimedFalseOrderByCreatedAtAsc(
            DisputeCaseStatus status
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.agreement",
            "session.agreement.requester",
            "session.agreement.provider",
            "milestone"
    })
    List<DisputeCase> findAllByStatusAndSettlementAttemptClaimedTrueOrderByCreatedAtAsc(
            DisputeCaseStatus status
    );

    @Query("""
        SELECT DISTINCT d
        FROM DisputeCase d
        JOIN FETCH d.session s
        JOIN FETCH s.agreement a
        JOIN FETCH a.requester
        JOIN FETCH a.provider
        JOIN FETCH d.claimant
        JOIN FETCH d.respondent
        WHERE d.claimant.id = :userId
           OR d.respondent.id = :userId
        ORDER BY d.createdAt DESC
        """)
    List<DisputeCase> findParticipantCases(
            @Param("userId") UUID userId);

}

