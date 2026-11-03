package com.barterlink.rewards.repository;

import com.barterlink.rewards.entity.ReferralAttribution;
import com.barterlink.rewards.enums.ReferralAttributionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralAttributionRepository
        extends JpaRepository<ReferralAttribution, UUID> {

    Optional<ReferralAttribution> findByReferredUser_Id(
            UUID referredUserId
    );

    boolean existsByReferredUser_Id(UUID referredUserId);

    List<ReferralAttribution>
    findTop100ByStatusOrderByQualifiedAtAsc(
            ReferralAttributionStatus status
    );

    long countByReferrer_Id(UUID referrerId);

    long countByReferrer_IdAndStatus(
            UUID referrerId,
            ReferralAttributionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT attribution
            FROM ReferralAttribution attribution
            WHERE attribution.id = :attributionId
            """)
    Optional<ReferralAttribution> findByIdForUpdate(
            @Param("attributionId") UUID attributionId
    );

    List<ReferralAttribution> findByReferrer_IdOrderByAttributedAtDesc(
            UUID referrerId
    );

    List<ReferralAttribution>
    findTop100ByStatusOrderByAttributedAtAsc(
            ReferralAttributionStatus status
    );
}