package com.barterlink.rewards.repository;

import com.barterlink.rewards.entity.RewardClaim;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardClaimStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardClaimRepository
        extends JpaRepository<RewardClaim, UUID> {

    boolean existsByClaimKey(String claimKey);

    Optional<RewardClaim> findByClaimKey(String claimKey);

    long countByRecipient_IdAndCampaign(
            UUID recipientId,
            RewardCampaign campaign
    );

    long countByRecipient_IdAndCampaignAndStatusIn(
            UUID recipientId,
            RewardCampaign campaign,
            Collection<RewardClaimStatus> statuses
    );

    Optional<RewardClaim> findByRecipient_IdAndCampaignAndSourceTypeAndSourceId(
            UUID recipientId,
            RewardCampaign campaign,
            String sourceType,
            String sourceId
    );

    List<RewardClaim> findTop50ByRecipient_IdOrderByCreatedAtDesc(
            UUID recipientId
    );

    List<RewardClaim> findByStatusOrderByCreatedAtAsc(
            RewardClaimStatus status
    );

    List<RewardClaim> findByStatusInOrderByCreatedAtAsc(
            Collection<RewardClaimStatus> statuses
    );

    List<RewardClaim> findByStatusAndProcessingAtBefore(
            RewardClaimStatus status,
            Instant cutoff
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT claim
            FROM RewardClaim claim
            WHERE claim.id = :claimId
            """)
    Optional<RewardClaim> findByIdForUpdate(
            @Param("claimId") UUID claimId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardClaim>
    findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            RewardClaimStatus status,
            Instant now
    );
}