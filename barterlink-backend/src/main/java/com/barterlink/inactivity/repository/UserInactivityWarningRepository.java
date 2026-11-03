
package com.barterlink.inactivity.repository;

import com.barterlink.inactivity.entity.UserInactivityWarning;
import com.barterlink.inactivity.entity.UserInactivityWarning.DeliveryStatus;
import com.barterlink.inactivity.entity.UserInactivityWarning.WarningStage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface UserInactivityWarningRepository
        extends JpaRepository<UserInactivityWarning, UUID> {

    List<UserInactivityWarning> findByUser_IdAndCycleStartedAt(
            UUID userId,
            Instant cycleStartedAt
    );

    Optional<UserInactivityWarning> findByUser_IdAndCycleStartedAtAndWarningStage(
            UUID userId,
            Instant cycleStartedAt,
            WarningStage warningStage
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM UserInactivityWarning w
            WHERE w.id = :warningId
            """)
    Optional<UserInactivityWarning> findByIdForUpdate(
            @Param("warningId") UUID warningId
    );

    List<UserInactivityWarning>
    findByEmailStatusOrNotificationStatus(
            DeliveryStatus emailStatus,
            DeliveryStatus notificationStatus
    );

    List<UserInactivityWarning> findByEmailStatusInOrNotificationStatusIn(
            Collection<DeliveryStatus> emailStatuses,
            Collection<DeliveryStatus> notificationStatuses
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w
        FROM UserInactivityWarning w
        WHERE w.user.id = :userId
          AND w.cycleStartedAt = :cycleStartedAt
        """)
    List<UserInactivityWarning> findByUserAndCycleStartedAtForUpdate(
            @Param("userId") UUID userId,
            @Param("cycleStartedAt") Instant cycleStartedAt
    );


    @Query("""
        SELECT w.id
        FROM UserInactivityWarning w
        WHERE
            (
                (
                    w.emailStatus IN (:nonProcessingStatuses)
                    AND w.emailAttempts < :maxAttempts
                )
                OR
                (
                    w.emailStatus = :processingStatus
                    AND (
                        w.emailProcessingStartedAt IS NULL
                        OR w.emailProcessingStartedAt <= :staleProcessingBefore
                    )
                )
            )
            OR
            (
                (
                    w.notificationStatus IN (:nonProcessingStatuses)
                    AND w.notificationAttempts < :maxAttempts
                )
                OR
                (
                    w.notificationStatus = :processingStatus
                    AND (
                        w.notificationProcessingStartedAt IS NULL
                        OR w.notificationProcessingStartedAt <= :staleProcessingBefore
                    )
                )
            )
        """)
    List<UUID> findEligibleWarningIds(
            @Param("nonProcessingStatuses")
            Collection<DeliveryStatus> nonProcessingStatuses,
            @Param("processingStatus")
            DeliveryStatus processingStatus,
            @Param("maxAttempts")
            int maxAttempts,
            @Param("staleProcessingBefore")
            Instant staleProcessingBefore
    );


}
