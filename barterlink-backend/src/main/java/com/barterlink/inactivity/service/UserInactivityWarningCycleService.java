package com.barterlink.inactivity.service;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.inactivity.entity.UserInactivityWarning;
import com.barterlink.inactivity.entity.UserInactivityWarning.WarningStage;
import com.barterlink.inactivity.repository.UserInactivityWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class UserInactivityWarningCycleService {

    private final UserRepository userRepository;
    private final UserInactivityWarningRepository warningRepository;

    /**
     * Creates warning records when their scheduled warning time is reached.
     * This method does not send email or in-app notifications.
     */
    @Transactional
    public int prepareDueWarnings() {

        Instant now = Instant.now();
        int createdCount = 0;

        for (User user : userRepository.findAll()) {

            Instant cycleStartedAt = user.getLastActiveAt();

            // Users without an activity timestamp cannot be processed safely.
            if (cycleStartedAt == null) {
                continue;
            }

            // Three calendar months, calculated consistently in UTC.
            Instant inactivityDeadline = cycleStartedAt
                    .atZone(ZoneOffset.UTC)
                    .plusMonths(3)
                    .toInstant();

            // Do not create late reminders once the deadline has passed.
            // Account deletion is handled separately after safety checks.
            if (!now.isBefore(inactivityDeadline)) {
                continue;
            }

            if (isDue(now, inactivityDeadline.minus(Duration.ofDays(30)))) {
                createdCount += createWarningIfMissing(
                        user,
                        cycleStartedAt,
                        WarningStage.FIRST
                );
            }

            if (isDue(now, inactivityDeadline.minus(Duration.ofDays(7)))) {
                createdCount += createWarningIfMissing(
                        user,
                        cycleStartedAt,
                        WarningStage.SECOND
                );
            }

            if (isDue(now, inactivityDeadline.minus(Duration.ofHours(24)))) {
                createdCount += createWarningIfMissing(
                        user,
                        cycleStartedAt,
                        WarningStage.FINAL
                );
            }
        }

        return createdCount;
    }

    private boolean isDue(Instant now, Instant warningTime) {
        return !now.isBefore(warningTime);
    }

    private int createWarningIfMissing(
            User user,
            Instant cycleStartedAt,
            WarningStage stage) {

        boolean alreadyExists = warningRepository
                .findByUser_IdAndCycleStartedAtAndWarningStage(
                        user.getId(),
                        cycleStartedAt,
                        stage
                )
                .isPresent();

        if (alreadyExists) {
            return 0;
        }

        UserInactivityWarning warning = UserInactivityWarning.builder()
                .user(user)
                .cycleStartedAt(cycleStartedAt)
                .warningStage(stage)
                .build();

        warningRepository.save(warning);

        return 1;
    }
}