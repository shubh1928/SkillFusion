package com.barterlink.sessions.repository;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.marketplace.enums.ExchangeMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceSessionRepository
        extends JpaRepository<ServiceSession, UUID> {

    Optional<ServiceSession> findByAgreement(
            ServiceAgreement agreement);

    List<ServiceSession> findByStatus(
            SessionStatus status);

    List<ServiceSession> findByAgreementRequesterId(
            UUID requesterId);

    List<ServiceSession> findByAgreementProviderId(
            UUID providerId);

    long countByAgreementProviderIdAndStatus(
            UUID providerId,
            SessionStatus status);

    long countByAgreementRequesterIdAndStatus(
            UUID requesterId,
            SessionStatus status);

    long countByAgreementProviderId(
            UUID providerId);

    long countByAgreementRequesterId(
            UUID requesterId);

    @Query("""
    SELECT COUNT(DISTINCT s.id)
    FROM ServiceSession s
    JOIN s.agreement a
    JOIN a.negotiation n
    WHERE s.status = :completedStatus
      AND a.status = :agreementCompletedStatus
      AND s.completedAt IS NOT NULL
      AND s.completedAt > :attributedAt
      AND a.exchangeMode = :directBarter
      AND n.proposedExchangeMode = :directBarter
      AND (
            a.requester.id = :referredUserId
            OR a.provider.id = :referredUserId
      )
    """)
    long countCompletedDirectBarterSessionsForParticipantSince(
            @Param("referredUserId") UUID referredUserId,
            @Param("attributedAt") java.time.Instant attributedAt,
            @Param("completedStatus") SessionStatus completedStatus,
            @Param("agreementCompletedStatus")
            AgreementStatus agreementCompletedStatus,
            @Param("directBarter") ExchangeMode directBarter
    );

    @Query("""
    SELECT COUNT(DISTINCT s.id)
    FROM ServiceSession s
    JOIN s.agreement a
    JOIN a.negotiation n
    WHERE s.status = :completedStatus
      AND a.status = :agreementCompletedStatus
      AND s.completedAt IS NOT NULL
      AND s.completedAt > :attributedAt
      AND a.exchangeMode = :directBarter
      AND n.proposedExchangeMode = :directBarter
      AND (
            (
                a.requester.id = :referredUserId
                AND a.provider.id <> :referrerId
            )
            OR
            (
                a.provider.id = :referredUserId
                AND a.requester.id <> :referrerId
            )
      )
    """)
    long countCompletedDirectBarterSessionsWithOtherCounterpartySince(
            @Param("referredUserId") UUID referredUserId,
            @Param("referrerId") UUID referrerId,
            @Param("attributedAt") java.time.Instant attributedAt,
            @Param("completedStatus") SessionStatus completedStatus,
            @Param("agreementCompletedStatus")
            AgreementStatus agreementCompletedStatus,
            @Param("directBarter") ExchangeMode directBarter
    );

    @Query("""
        SELECT s.id
        FROM ServiceSession s
        JOIN s.agreement a
        JOIN a.negotiation n
        WHERE s.status = :completedStatus
          AND a.status = :agreementCompletedStatus
          AND s.completedAt IS NOT NULL
          AND s.completedAt > :attributedAt
          AND a.exchangeMode = :directBarter
          AND n.proposedExchangeMode = :directBarter
          AND (
                a.requester.id = :referredUserId
                OR a.provider.id = :referredUserId
          )
        ORDER BY s.completedAt ASC
        """)
    List<UUID> findCompletedDirectBarterSessionIdsForParticipantSince(
            @Param("referredUserId") UUID referredUserId,
            @Param("attributedAt") java.time.Instant attributedAt,
            @Param("completedStatus") SessionStatus completedStatus,
            @Param("agreementCompletedStatus")
            AgreementStatus agreementCompletedStatus,
            @Param("directBarter") ExchangeMode directBarter
    );

    @Query("""
        SELECT s.id
        FROM ServiceSession s
        JOIN s.agreement a
        JOIN a.negotiation n
        WHERE s.status = :completedStatus
          AND a.status = :agreementCompletedStatus
          AND s.completedAt IS NOT NULL
          AND s.completedAt > :attributedAt
          AND a.exchangeMode = :directBarter
          AND n.proposedExchangeMode = :directBarter
          AND (
                (
                    a.requester.id = :referredUserId
                    AND a.provider.id <> :referrerId
                )
                OR
                (
                    a.provider.id = :referredUserId
                    AND a.requester.id <> :referrerId
                )
          )
        ORDER BY s.completedAt ASC
        """)
    List<UUID> findCompletedDirectBarterSessionIdsWithOtherCounterpartySince(
            @Param("referredUserId") UUID referredUserId,
            @Param("referrerId") UUID referrerId,
            @Param("attributedAt") java.time.Instant attributedAt,
            @Param("completedStatus") SessionStatus completedStatus,
            @Param("agreementCompletedStatus")
            AgreementStatus agreementCompletedStatus,
            @Param("directBarter") ExchangeMode directBarter
    );


    @Query("""
        SELECT COUNT(s)
        FROM ServiceSession s
        JOIN s.agreement a
        WHERE (a.requester.id = :userId OR a.provider.id = :userId)
          AND s.status IN :statuses
        """)
    long countUnresolvedForUser(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<SessionStatus> statuses
    );

}