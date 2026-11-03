package com.barterlink.rewards.service;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.auth.util.ReferralCodeGenerator;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.rewards.entity.ReferralAttribution;
import com.barterlink.rewards.enums.ReferralAttributionStatus;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardEnqueueResult;
import com.barterlink.rewards.repository.ReferralAttributionRepository;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralAttributionService {

    private static final String REFERRAL_REWARD_SOURCE_TYPE =
            "REFERRAL_QUALIFIED";

    private final UserRepository userRepository;
    private final ReferralAttributionRepository referralAttributionRepository;
    private final ServiceSessionRepository serviceSessionRepository;
    private final RewardClaimQueueService rewardClaimQueueService;

    /**
     * Records referral attribution during local registration.
     * A missing code is allowed. An invalid supplied code is rejected.
     */
    @Transactional
    public void attributeLocalSignup(
            User referredUser,
            String rawReferrerCode
    ) {
        if (rawReferrerCode == null || rawReferrerCode.isBlank()) {
            return;
        }

        if (referredUser == null || referredUser.getId() == null) {
            throw new IllegalArgumentException(
                    "The new user must be persisted before referral attribution"
            );
        }

        String referrerCode =
                ReferralCodeGenerator.normalize(rawReferrerCode);

        User referrer = userRepository
                .findByReferralCodeIgnoreCase(referrerCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The supplied referral code does not exist"
                ));

        validateReferrerForLocalSignup(referrer, referredUser);

        if (referralAttributionRepository
                .existsByReferredUser_Id(referredUser.getId())) {
            throw new IllegalStateException(
                    "Referral attribution already exists for this account"
            );
        }

        Instant now = Instant.now();

        ReferralAttribution attribution =
                ReferralAttribution.builder()
                        .referrer(referrer)
                        .referredUser(referredUser)
                        .status(ReferralAttributionStatus.TRACKING)
                        .attributedAt(now)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        referralAttributionRepository.save(attribution);
    }

    /**
     * Records attribution only for a newly created OAuth account.
     * Invalid or stale referral context must not block Google/GitHub login.
     */
    @Transactional
    public void attributeOAuthSignup(
            User referredUser,
            String rawReferrerCode
    ) {
        if (rawReferrerCode == null || rawReferrerCode.isBlank()
                || referredUser == null || referredUser.getId() == null) {
            return;
        }

        final String referrerCode;

        try {
            referrerCode =
                    ReferralCodeGenerator.normalize(rawReferrerCode);
        } catch (IllegalArgumentException invalidCode) {
            log.warn(
                    "Ignoring an invalid referral code supplied with OAuth signup"
            );
            return;
        }

        Optional<User> optionalReferrer =
                userRepository.findByReferralCodeIgnoreCase(referrerCode);

        if (optionalReferrer.isEmpty()) {
            log.warn(
                    "Ignoring an unknown referral code supplied with OAuth signup"
            );
            return;
        }

        User referrer = optionalReferrer.get();

        if (!isEligibleReferrer(referrer)
                || referrer.getId().equals(referredUser.getId())
                || (referrer.getEmail() != null
                    && referrer.getEmail().equalsIgnoreCase(
                            referredUser.getEmail()
                    ))) {
            log.warn(
                    "Ignoring an ineligible or self-referral during OAuth signup"
            );
            return;
        }

        if (referralAttributionRepository
                .existsByReferredUser_Id(referredUser.getId())) {
            return;
        }

        Instant now = Instant.now();

        ReferralAttribution attribution =
                ReferralAttribution.builder()
                        .referrer(referrer)
                        .referredUser(referredUser)
                        .status(ReferralAttributionStatus.TRACKING)
                        .attributedAt(now)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        referralAttributionRepository.save(attribution);
    }

    /**
     * Returns the user's stable public code. A code is generated for existing
     * accounts that predate this feature when they first request it.
     */
    @Transactional
    public String getOrCreateReferralCode(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User was not found while retrieving referral code"
                ));

        if (user.getReferralCode() == null
                || user.getReferralCode().isBlank()) {

            String code;

            do {
                code = ReferralCodeGenerator.generate();
            } while (userRepository.existsByReferralCodeIgnoreCase(code));

            user.setReferralCode(code);
            userRepository.save(user);
        }

        return user.getReferralCode();
    }

    /**
     * Called after SessionServiceImpl saves a completed session.
     */
    @Transactional
    public void processCompletedDirectBarterSession(
            ServiceSession session
    ) {
        if (!isCompletedDirectBarter(session)) {
            return;
        }

        UUID requesterId =
                session.getAgreement().getRequester().getId();

        UUID providerId =
                session.getAgreement().getProvider().getId();

        List<UUID> participantIds = new ArrayList<>(2);
        participantIds.add(requesterId);

        if (!providerId.equals(requesterId)) {
            participantIds.add(providerId);
        }

        for (UUID participantId : participantIds) {
            referralAttributionRepository
                    .findByReferredUser_Id(participantId)
                    .ifPresent(attribution ->
                            evaluateAttribution(attribution.getId())
                    );
        }
    }

    /**
     * Retries qualified referrals that have not yet been queued. This is
     * called by the existing reward scheduler after it has been enabled.
     */
    @Transactional
    public void retryQualifiedReferrals() {
        List<ReferralAttribution> qualified =
                referralAttributionRepository
                        .findTop100ByStatusOrderByQualifiedAtAsc(
                                ReferralAttributionStatus.QUALIFIED
                        );

        for (ReferralAttribution attribution : qualified) {
            /*
             * Recheck current session eligibility before queuing.
             * Do not trust a previously saved QUALIFIED status alone.
             */
            evaluateAttribution(attribution.getId());
        }
    }

    /**
     * Ensures an anti-fraud referral review reserves its lifetime slot once
     * reward processing is enabled.
     *
     * REVIEW_REQUIRED claims are not processed by the automatic mint worker.
     */
    /**
     * Automatically rechecks referrals waiting for a qualifying session
     * with a counterparty other than the referrer.
     *
     * This method does not create review-only reward claims or require
     * administrator approval.
     */
    @Transactional
    public void retryWaitingReferrals() {
        List<ReferralAttribution> waiting =
                referralAttributionRepository
                        .findTop100ByStatusOrderByAttributedAtAsc(
                                ReferralAttributionStatus.REVIEW_REQUIRED
                        );

        for (ReferralAttribution attribution : waiting) {
            evaluateAttribution(attribution.getId());
        }
    }

    private void evaluateAttribution(UUID attributionId) {
        ReferralAttribution attribution =
                referralAttributionRepository.findByIdForUpdate(attributionId)
                        .orElse(null);

        if (attribution == null
                || attribution.getStatus()
                == ReferralAttributionStatus.REWARD_QUEUED
                || attribution.getStatus()
                == ReferralAttributionStatus.LIFETIME_CAP_REACHED) {
            return;
        }

        ReferralAttributionStatus currentStatus = attribution.getStatus();

        /*
         * TRACKING: the invitee is still accumulating qualifying sessions.
         * REVIEW_REQUIRED: two sessions may exist, but the required different
         * counterparty has not yet been recorded. This is an automatic waiting
         * state, not a request for administrator approval.
         */
        if (currentStatus != ReferralAttributionStatus.TRACKING
                && currentStatus != ReferralAttributionStatus.REVIEW_REQUIRED
                && currentStatus != ReferralAttributionStatus.QUALIFIED) {
            return;
        }

        UUID referredUserId = attribution.getReferredUser().getId();
        UUID referrerId = attribution.getReferrer().getId();

        long totalCompleted =
                serviceSessionRepository
                        .countCompletedDirectBarterSessionsForParticipantSince(
                                referredUserId,
                                attribution.getAttributedAt(),
                                SessionStatus.COMPLETED,
                                AgreementStatus.COMPLETED,
                                ExchangeMode.DIRECT_BARTER
                        );

        attribution.setQualifyingDirectBarterSessionCount(
                totalCompleted > Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : (int) totalCompleted
        );

        if (totalCompleted < 2) {
            /*
             * Fewer than two eligible sessions remain.
             * Clear session references that no longer establish qualification.
             */
            attribution.setQualifyingSessionIdOne(null);
            attribution.setQualifyingSessionIdTwo(null);
            attribution.setOtherCounterpartySessionId(null);

            /*
             * A previously qualified referral whose reward was not queued
             * must return to tracking if it no longer meets eligibility.
             */
            if (currentStatus == ReferralAttributionStatus.QUALIFIED
                    || currentStatus
                    == ReferralAttributionStatus.REVIEW_REQUIRED) {

                attribution.setStatus(
                        ReferralAttributionStatus.TRACKING
                );
                attribution.setQualifiedAt(null);
            }

            attribution.setReviewRequiredAt(null);
            attribution.setReviewReason(null);

            referralAttributionRepository.save(attribution);
            return;
        }

        List<UUID> qualifyingSessionIds =
                serviceSessionRepository
                        .findCompletedDirectBarterSessionIdsForParticipantSince(
                                referredUserId,
                                attribution.getAttributedAt(),
                                SessionStatus.COMPLETED,
                                AgreementStatus.COMPLETED,
                                ExchangeMode.DIRECT_BARTER
                        );

        /*
         * The count query and ID query must agree before qualification.
         * Do not queue a reward if two distinct session references
         * cannot be established.
         */
        if (qualifyingSessionIds.size() < 2) {
            /*
             * The session-ID query could not establish two distinct,
             * eligible Direct Barter sessions. Do not retain stale
             * qualification evidence.
             */
            attribution.setQualifyingDirectBarterSessionCount(
                    qualifyingSessionIds.size()
            );
            attribution.setQualifyingSessionIdOne(null);
            attribution.setQualifyingSessionIdTwo(null);
            attribution.setOtherCounterpartySessionId(null);

            /*
             * If qualification was not yet rewarded, return the attribution
             * to tracking. Clear stale qualification and waiting metadata.
             */
            if (attribution.getStatus() == ReferralAttributionStatus.QUALIFIED
                    || attribution.getStatus()
                    == ReferralAttributionStatus.REVIEW_REQUIRED) {
                attribution.setStatus(ReferralAttributionStatus.TRACKING);
                attribution.setQualifiedAt(null);
            }

            attribution.setReviewRequiredAt(null);
            attribution.setReviewReason(null);

            referralAttributionRepository.save(attribution);
            return;
        }

        List<UUID> otherCounterpartySessionIds =
                serviceSessionRepository
                        .findCompletedDirectBarterSessionIdsWithOtherCounterpartySince(
                                referredUserId,
                                referrerId,
                                attribution.getAttributedAt(),
                                SessionStatus.COMPLETED,
                                AgreementStatus.COMPLETED,
                                ExchangeMode.DIRECT_BARTER
                        );

        if (otherCounterpartySessionIds.isEmpty()) {
            /*
             * Two valid sessions exist, but both are only with the referrer.
             * Store the evidence and wait for another qualifying participant.
             */
            attribution.setQualifyingSessionIdOne(
                    qualifyingSessionIds.get(0)
            );
            attribution.setQualifyingSessionIdTwo(
                    qualifyingSessionIds.get(1)
            );
            attribution.setOtherCounterpartySessionId(null);

            if (attribution.getStatus()
                    != ReferralAttributionStatus.REVIEW_REQUIRED) {
                attribution.setStatus(
                        ReferralAttributionStatus.REVIEW_REQUIRED
                );
                attribution.setReviewRequiredAt(Instant.now());
            }

            /*
             * This attribution is waiting, not currently qualified.
             * Clear any qualification timestamp left by an earlier evaluation.
             */
            attribution.setQualifiedAt(null);

            attribution.setReviewReason(
                    "Waiting for a qualifying Direct Barter session with another participant."
            );

            referralAttributionRepository.save(attribution);
            return;
        }

        /*
         * Choose one session involving someone other than the referrer,
         * plus a second distinct eligible session.
         */
        UUID otherCounterpartySessionId =
                otherCounterpartySessionIds.get(0);

        UUID secondQualifyingSessionId =
                qualifyingSessionIds.stream()
                        .filter(id ->
                                !id.equals(otherCounterpartySessionId)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Two distinct qualifying referral sessions "
                                                + "could not be selected"
                                )
                        );

        attribution.setQualifyingSessionIdOne(
                otherCounterpartySessionId
        );
        attribution.setQualifyingSessionIdTwo(
                secondQualifyingSessionId
        );
        attribution.setOtherCounterpartySessionId(
                otherCounterpartySessionId
        );

        /*
         * All automatic qualification conditions are now satisfied.
         * Clear the temporary waiting state and continue through the normal
         * reward claim queue, which enforces the lifetime cap and idempotency.
         */
        attribution.setStatus(ReferralAttributionStatus.QUALIFIED);

        if (attribution.getQualifiedAt() == null) {
            attribution.setQualifiedAt(Instant.now());
        }

        attribution.setReviewRequiredAt(null);
        attribution.setReviewReason(null);

        referralAttributionRepository.save(attribution);

        attemptQueue(attribution);
    }

    private void attemptQueueById(UUID attributionId) {
        ReferralAttribution attribution =
                referralAttributionRepository.findByIdForUpdate(attributionId)
                        .orElse(null);

        if (attribution != null
                && attribution.getStatus()
                    == ReferralAttributionStatus.QUALIFIED) {
            attemptQueue(attribution);
        }
    }

    private void attemptQueue(ReferralAttribution attribution) {
        RewardEnqueueResult result =
                rewardClaimQueueService.enqueueForUser(
                        attribution.getReferrer().getId(),
                        RewardCampaign.REFERRAL_QUALIFIED,
                        REFERRAL_REWARD_SOURCE_TYPE,
                        attribution.getId().toString()
                );

        Instant now = Instant.now();

        switch (result) {
            case QUEUED, ALREADY_EXISTS -> {
                attribution.setStatus(
                        ReferralAttributionStatus.REWARD_QUEUED
                );

                if (attribution.getRewardQueuedAt() == null) {
                    attribution.setRewardQueuedAt(now);
                }

                referralAttributionRepository.save(attribution);
            }

            case LIFETIME_CAP_REACHED -> {
                attribution.setStatus(
                        ReferralAttributionStatus.LIFETIME_CAP_REACHED
                );

                attribution.setLifetimeCapReachedAt(now);
                referralAttributionRepository.save(attribution);
            }

            case FEATURE_DISABLED -> {
                // Retain QUALIFIED so the scheduler can try again later.
            }
        }
    }

    private boolean isCompletedDirectBarter(ServiceSession session) {
        if (session == null
                || session.getStatus() != SessionStatus.COMPLETED
                || session.getCompletedAt() == null
                || session.getAgreement() == null
                || session.getAgreement().getStatus()
                    != AgreementStatus.COMPLETED
                || session.getAgreement().getNegotiation() == null) {
            return false;
        }

        return session.getAgreement().getExchangeMode()
                    == ExchangeMode.DIRECT_BARTER
                && session.getAgreement().getNegotiation()
                    .getProposedExchangeMode()
                    == ExchangeMode.DIRECT_BARTER;
    }

    private void validateReferrerForLocalSignup(
            User referrer,
            User referredUser
    ) {
        if (!isEligibleReferrer(referrer)) {
            throw new IllegalArgumentException(
                    "The referral code does not belong to an eligible account"
            );
        }

        if (referrer.getId().equals(referredUser.getId())
                || (referrer.getEmail() != null
                    && referrer.getEmail().equalsIgnoreCase(
                            referredUser.getEmail()
                    ))) {
            throw new IllegalArgumentException(
                    "You cannot use your own referral code"
            );
        }
    }

    private boolean isEligibleReferrer(User user) {
        return user != null
                && user.isEnabled()
                && user.getInactivityDeactivatedAt() == null
                && user.getAnonymizedAt() == null;
    }
}