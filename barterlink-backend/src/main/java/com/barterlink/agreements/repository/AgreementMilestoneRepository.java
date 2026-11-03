package com.barterlink.agreements.repository;

import com.barterlink.agreements.entity.AgreementMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementMilestoneRepository
        extends JpaRepository<AgreementMilestone, UUID> {

    List<AgreementMilestone> findByNegotiation_IdOrderByCreatedAtAsc(
            UUID negotiationId);

    Optional<AgreementMilestone> findByIdAndNegotiation_Id(
            UUID milestoneId,
            UUID negotiationId);

    /**
     * Loads a milestone under a database write lock.
     * Must be called from a transactional service method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM AgreementMilestone m
            WHERE m.id = :milestoneId
              AND m.negotiation.id = :negotiationId
            """)
    Optional<AgreementMilestone> findByIdAndNegotiationIdForUpdate(
            @Param("milestoneId") UUID milestoneId,
            @Param("negotiationId") UUID negotiationId);

}