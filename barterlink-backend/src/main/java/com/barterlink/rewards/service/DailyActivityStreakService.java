package com.barterlink.rewards.service;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.rewards.entity.RewardStreakDay;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardQualifyingActivityType;
import com.barterlink.rewards.repository.RewardStreakDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyActivityStreakService {

    private static final int REQUIRED_CONSECUTIVE_DAYS = 30;

    /*
     * Stable source identifiers are important. If the claim becomes
     * uncertain or remains pending, the same lifetime claim key must
     * be reused rather than creating another claim for a later date.
     */
    private static final String STREAK_SOURCE_TYPE = "STREAK_LIFETIME";
    private static final String STREAK_SOURCE_ID = "LIFETIME";

    private final UserRepository userRepository;
    private final RewardStreakDayRepository rewardStreakDayRepository;
    private final RewardClaimQueueService rewardClaimQueueService;

    /**
     * Call only after the authentication flow has successfully validated
     * the user's login credentials or completed OAuth authentication.
     *
     * Login alone does not qualify a day.
     */
    @Transactional
    public void recordSuccessfulLogin(
            UUID userId,
            Instant serverLoginTime
    ) {
        requireValidEvent(userId, serverLoginTime);

        User user = lockEligibleUser(userId);

        LocalDate utcDate = toUtcDate(serverLoginTime);

        RewardStreakDay day = findOrCreateDay(user, utcDate);

        if (!day.isLoginRecorded()) {
            day.setFirstLoginAt(serverLoginTime);
        }

        day.setLoginRecorded(true);
        day.setLastLoginAt(serverLoginTime);

        rewardStreakDayRepository.save(day);

        evaluateDayAndQueueReward(userId, utcDate, day);
    }

    /**
     * Record this only after an allowlisted business action has successfully
     * persisted or materially advanced a workflow.
     *
     * The caller must be trusted backend code. Never bind these values
     * directly from a client-supplied reward request.
     */
    @Transactional
    public void recordQualifyingActivity(
            UUID userId,
            RewardQualifyingActivityType activityType,
            String sourceId,
            Instant serverActivityTime
    ) {
        requireValidEvent(userId, serverActivityTime);

        if (activityType == null) {
            throw new IllegalArgumentException(
                    "Qualifying activity type is required"
            );
        }

        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException(
                    "Qualifying activity source is required"
            );
        }

        if (sourceId.length() > 100) {
            throw new IllegalArgumentException(
                    "Qualifying activity source exceeds the supported length"
            );
        }

        User user = lockEligibleUser(userId);

        LocalDate utcDate = toUtcDate(serverActivityTime);

        RewardStreakDay day = findOrCreateDay(user, utcDate);

        if (!day.isQualifyingActivityRecorded()) {
            day.setFirstQualifyingActivityType(activityType.name());
            day.setFirstQualifyingSourceId(sourceId);
            day.setFirstQualifyingActivityAt(serverActivityTime);
        }

        day.setQualifyingActivityRecorded(true);
        day.setLastQualifyingActivityType(activityType.name());
        day.setLastQualifyingSourceId(sourceId);
        day.setLastQualifyingActivityAt(serverActivityTime);

        rewardStreakDayRepository.save(day);

        evaluateDayAndQueueReward(userId, utcDate, day);
    }

    private User lockEligibleUser(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Streak user was not found: " + userId
                ));

        if (!user.isEnabled()
                || user.getInactivityDeactivatedAt() != null
                || user.getAnonymizedAt() != null) {

            throw new IllegalStateException(
                    "An inactive or anonymized account cannot advance a reward streak"
            );
        }

        return user;
    }

    private RewardStreakDay findOrCreateDay(
            User user,
            LocalDate utcDate
    ) {
        return rewardStreakDayRepository
                .findByUser_IdAndActivityDateUtc(
                        user.getId(),
                        utcDate
                )
                .orElseGet(() -> RewardStreakDay.builder()
                        .user(user)
                        .activityDateUtc(utcDate)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());
    }

    private void evaluateDayAndQueueReward(
            UUID userId,
            LocalDate eventDate,
            RewardStreakDay day
    ) {
        /*
         * A qualifying day requires both independent signals.
         */
        if (day.isLoginRecorded()
                && day.isQualifyingActivityRecorded()
                && !day.isDailyQualified()) {

            day.setDailyQualified(true);
            day.setDailyQualifiedAt(Instant.now());

            rewardStreakDayRepository.save(day);
        }

        /*
         * Do not calculate a streak from a day that is still missing
         * either the login or qualifying-activity signal.
         */
        if (!day.isDailyQualified()) {
            return;
        }

        int consecutiveDays =
                countConsecutiveQualifiedDays(userId, eventDate);

        if (consecutiveDays >= REQUIRED_CONSECUTIVE_DAYS) {
            /*
             * RewardClaimQueueService is the existing durable queue
             * from the previous batch. The stable source makes this
             * a single lifetime claim per user, not one claim per day.
             */
            rewardClaimQueueService.enqueueForUser(
                    userId,
                    RewardCampaign.DAILY_ACTIVITY_STREAK_30,
                    STREAK_SOURCE_TYPE,
                    STREAK_SOURCE_ID
            );
        }
    }

    private int countConsecutiveQualifiedDays(
            UUID userId,
            LocalDate endingDate
    ) {
        List<RewardStreakDay> qualifiedDays =
                rewardStreakDayRepository
                        .findByUser_IdAndActivityDateUtcLessThanEqualAndDailyQualifiedTrueOrderByActivityDateUtcDesc(
                                userId,
                                endingDate
                        );

        LocalDate expectedDate = endingDate;
        int consecutiveDays = 0;

        for (RewardStreakDay qualifiedDay : qualifiedDays) {
            LocalDate recordedDate = qualifiedDay.getActivityDateUtc();

            if (recordedDate.equals(expectedDate)) {
                consecutiveDays++;
                expectedDate = expectedDate.minusDays(1);
                continue;
            }

            /*
             * A gap breaks the sequence. Never fill a missing day or
             * grant grace days.
             */
            if (recordedDate.isBefore(expectedDate)) {
                break;
            }
        }

        return consecutiveDays;
    }

    private LocalDate toUtcDate(Instant serverTimestamp) {
        return serverTimestamp.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private void requireValidEvent(
            UUID userId,
            Instant serverTimestamp
    ) {
        if (userId == null || serverTimestamp == null) {
            throw new IllegalArgumentException(
                    "User and server event timestamp are required"
            );
        }
    }
}