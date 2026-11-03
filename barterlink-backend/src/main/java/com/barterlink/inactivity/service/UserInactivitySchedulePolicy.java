
package com.barterlink.inactivity.service;

import com.barterlink.inactivity.entity.UserInactivityWarning.WarningStage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

@Component
public class UserInactivitySchedulePolicy {

    public Instant calculateDeletionDeadline(Instant lastActiveAt) {
        Objects.requireNonNull(lastActiveAt, "lastActiveAt must not be null");

        return lastActiveAt
                .atZone(ZoneOffset.UTC)
                .plusMonths(3)
                .toInstant();
    }

    public Instant calculateWarningTime(
            Instant lastActiveAt,
            WarningStage stage
    ) {
        Objects.requireNonNull(stage, "Warning stage must not be null");

        ZonedDateTime deadline = calculateDeletionDeadline(lastActiveAt)
                .atZone(ZoneOffset.UTC);

        return switch (stage) {
            case FIRST -> deadline.minusDays(30).toInstant();
            case SECOND -> deadline.minusDays(7).toInstant();
            case FINAL -> deadline.minusHours(24).toInstant();
        };
    }



    public WarningStage findLatestDueStage(
            Instant lastActiveAt,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");

        Instant deadline = calculateDeletionDeadline(lastActiveAt);

        // Once the deadline has passed, no warning stage should be sent.
        if (!now.isBefore(deadline)) {
            return null;
        }

        WarningStage latestDueStage = null;

        for (WarningStage stage : WarningStage.values()) {
            Instant warningTime = calculateWarningTime(lastActiveAt, stage);

            if (!now.isBefore(warningTime)) {
                latestDueStage = stage;
            }
        }

        return latestDueStage;
    }


}
