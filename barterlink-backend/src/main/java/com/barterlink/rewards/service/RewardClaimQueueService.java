package com.barterlink.rewards.service;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.rewards.entity.RewardClaim;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardClaimStatus;
import com.barterlink.rewards.enums.RewardEnqueueResult;
import com.barterlink.rewards.repository.RewardClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardClaimQueueService {

    /*
     * Every persisted claim reserves its lifetime slot.
     *
     * A failed or held claim does not become a new reward merely because
     * its status changes. A definitive manual review rejection will need
     * a separate, audited resolution flow before a slot can be released.
     */
    private static final List<RewardClaimStatus> RESERVED_STATUSES =
            List.of(
                    RewardClaimStatus.PENDING,
                    RewardClaimStatus.PROCESSING,
                    RewardClaimStatus.SUBMITTED,
                    RewardClaimStatus.RECONCILIATION_REQUIRED,
                    RewardClaimStatus.REVIEW_REQUIRED,
                    RewardClaimStatus.CONFIRMED,
                    RewardClaimStatus.FAILED
            );

    private final UserRepository userRepository;
    private final RewardClaimRepository rewardClaimRepository;

    @Value("${barterlink.rewards.enabled:false}")
    private boolean rewardsEnabled;

    /**
     * Called by the trusted server-side Direct Barter completion flow.
     * The claims participate in the surrounding SQL transaction.
     */
    @Transactional
    public void enqueueDirectBarterCompletion(
            UUID sessionId,
            User requester,
            User provider
    ) {
        if (!rewardsEnabled) {
            return;
        }

        if (sessionId == null
                || requester == null
                || provider == null
                || requester.getId() == null
                || provider.getId() == null) {
            throw new IllegalArgumentException(
                    "Session and both participants are required"
            );
        }

        if (requester.getId().equals(provider.getId())) {
            throw new IllegalArgumentException(
                    "A Direct Barter reward requires two different participants"
            );
        }

        /*
         * Deterministic locking order prevents opposing concurrent sessions
         * from locking the same two user rows in opposite orders.
         */
        UUID firstId =
                requester.getId().compareTo(provider.getId()) < 0
                        ? requester.getId()
                        : provider.getId();

        UUID secondId =
                firstId.equals(requester.getId())
                        ? provider.getId()
                        : requester.getId();

        User firstLocked = userRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reward participant was not found: " + firstId
                ));

        User secondLocked = userRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reward participant was not found: " + secondId
                ));

        User requesterLocked =
                firstLocked.getId().equals(requester.getId())
                        ? firstLocked
                        : secondLocked;

        User providerLocked =
                firstLocked.getId().equals(provider.getId())
                        ? firstLocked
                        : secondLocked;

        // Each participant is checked and enqueued independently.
        enqueueForLockedUser(
                requesterLocked,
                RewardCampaign.DIRECT_BARTER_COMPLETION,
                "SESSION_COMPLETION",
                sessionId.toString(),
                RewardClaimStatus.PENDING
        );

        enqueueForLockedUser(
                providerLocked,
                RewardCampaign.DIRECT_BARTER_COMPLETION,
                "SESSION_COMPLETION",
                sessionId.toString(),
                RewardClaimStatus.PENDING
        );
    }

    @Transactional
    public RewardEnqueueResult enqueueForUser(
            UUID recipientId,
            RewardCampaign campaign,
            String sourceType,
            String sourceId
    ) {
        if (!rewardsEnabled) {
            return RewardEnqueueResult.FEATURE_DISABLED;
        }

        validateSource(campaign, sourceType, sourceId);

        User recipient = userRepository.findByIdForUpdate(recipientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reward recipient was not found: " + recipientId
                ));

        return enqueueForLockedUser(
                recipient,
                campaign,
                sourceType,
                sourceId,
                RewardClaimStatus.PENDING
        );
    }

    private RewardEnqueueResult enqueueForLockedUser(
            User recipient,
            RewardCampaign campaign,
            String sourceType,
            String sourceId,
            RewardClaimStatus initialStatus
    ) {
        validateSource(campaign, sourceType, sourceId);

        String claimKey = createClaimKey(
                campaign,
                sourceType,
                sourceId,
                recipient.getId()
        );

        if (rewardClaimRepository.existsByClaimKey(claimKey)) {
            return RewardEnqueueResult.ALREADY_EXISTS;
        }

        long reservedClaims =
                rewardClaimRepository
                        .countByRecipient_IdAndCampaignAndStatusIn(
                                recipient.getId(),
                                campaign,
                                RESERVED_STATUSES
                        );

        if (reservedClaims >= campaign.getLifetimeLimit()) {
            return RewardEnqueueResult.LIFETIME_CAP_REACHED;
        }

        Instant now = Instant.now();

        RewardClaim claim = RewardClaim.builder()
                .recipient(recipient)
                .campaign(campaign)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .claimKey(claimKey)
                .amountTokens(campaign.getTokenAmount())
                .status(initialStatus)
                .attemptCount(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        rewardClaimRepository.save(claim);

        return RewardEnqueueResult.QUEUED;
    }

    private void validateSource(
            RewardCampaign campaign,
            String sourceType,
            String sourceId
    ) {
        if (campaign == null
                || sourceType == null
                || sourceType.isBlank()
                || sourceId == null
                || sourceId.isBlank()) {
            throw new IllegalArgumentException(
                    "Reward campaign and source identifiers are required"
            );
        }

        if (sourceType.length() > 40 || sourceId.length() > 64) {
            throw new IllegalArgumentException(
                    "Reward source identifiers exceed supported lengths"
            );
        }
    }

    private String createClaimKey(
            RewardCampaign campaign,
            String sourceType,
            String sourceId,
            UUID recipientId
    ) {
        String canonical = campaign.name()
                + ":" + sourceType
                + ":" + sourceId
                + ":" + recipientId;

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));

            return "0x" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "SHA-256 is required by this Java runtime",
                    impossible
            );
        }
    }
}