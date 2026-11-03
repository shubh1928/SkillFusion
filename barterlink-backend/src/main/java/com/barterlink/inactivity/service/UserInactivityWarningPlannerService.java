
package com.barterlink.inactivity.service;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.inactivity.dto.InactivityUserSnapshot;
import com.barterlink.inactivity.entity.UserInactivityWarning;
import com.barterlink.inactivity.entity.UserInactivityWarning.WarningStage;
import com.barterlink.inactivity.repository.UserInactivityWarningRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.barterlink.inactivity.entity.UserInactivityWarning.DeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class UserInactivityWarningPlannerService {

    private final UserRepository userRepository;
    private final UserInactivityWarningRepository warningRepository;
    private final UserInactivitySchedulePolicy schedulePolicy;
    private final TransactionTemplate transactionTemplate;

    public UserInactivityWarningPlannerService(
            UserRepository userRepository,
            UserInactivityWarningRepository warningRepository,
            UserInactivitySchedulePolicy schedulePolicy,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.warningRepository = warningRepository;
        this.schedulePolicy = schedulePolicy;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    /**
     * Checks for due inactivity warnings every five minutes.
     *
     * This method creates warning records only.
     * It does not send emails or notifications.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.inactivity.warning-planner-delay-ms:300000}",
            initialDelayString =
                    "${app.inactivity.warning-planner-initial-delay-ms:15000}"
    )
    public void planDueWarnings() {

        List<InactivityUserSnapshot> users;

        try {
            users = userRepository.findInactivitySnapshots();
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to load users for inactivity warning planning",
                    exception
            );
            return;
        }

        Instant now = Instant.now();

        for (InactivityUserSnapshot snapshot : users) {
            try {
                planWarningForUser(snapshot, now);
            } catch (RuntimeException exception) {
                log.error(
                        "Unable to plan inactivity warning for user {}",
                        snapshot.id(),
                        exception
                );
            }
        }
    }

    private void planWarningForUser(
            InactivityUserSnapshot snapshot,
            Instant now
    ) {
        Instant lastActiveAt = snapshot.lastActiveAt();

        if (lastActiveAt == null) {
            return;
        }

        Instant deadline =
                schedulePolicy.calculateDeletionDeadline(lastActiveAt);

        // Account-removal processing is a separate future step.
        // Do not create or send overdue warnings here.
        if (!now.isBefore(deadline)) {
            return;
        }

        WarningStage dueStage =
                schedulePolicy.findLatestDueStage(lastActiveAt, now);

        if (dueStage == null) {
            return;
        }

        try {
            Boolean created = transactionTemplate.execute(status -> {

                User currentUser = userRepository
                        .findById(snapshot.id())
                        .orElse(null);

                if (currentUser == null
                        || !Objects.equals(
                        currentUser.getLastActiveAt(),
                        lastActiveAt
                )) {
                    // Activity changed since the initial user scan.
                    return false;
                }


                List<UserInactivityWarning> cycleWarnings =
                        warningRepository.findByUserAndCycleStartedAtForUpdate(
                                currentUser.getId(),
                                lastActiveAt
                        );

                Instant latestDueTime =
                        schedulePolicy.calculateWarningTime(lastActiveAt, dueStage);

// Policy B: cancel earlier, unsent stages when a later stage is due.
// Preserve each channel that has already been successfully sent.
                for (UserInactivityWarning existingWarning : cycleWarnings) {

                    Instant existingWarningTime =
                            schedulePolicy.calculateWarningTime(
                                    lastActiveAt,
                                    existingWarning.getWarningStage()
                            );

                    if (existingWarningTime.isBefore(latestDueTime)) {

                        cancelUnsentChannels(existingWarning);
                    }
                }

                warningRepository.saveAll(cycleWarnings);

                boolean alreadyExists = cycleWarnings.stream()
                        .anyMatch(warning ->
                                warning.getWarningStage() == dueStage
                        );

                if (alreadyExists) {
                    return false;
                }


                UserInactivityWarning warning =
                        UserInactivityWarning.builder()
                                .user(currentUser)
                                .cycleStartedAt(lastActiveAt)
                                .warningStage(dueStage)
                                .build();

                // Flush here so a concurrent duplicate is detected
                // inside this transaction.
                warningRepository.saveAndFlush(warning);

                return true;
            });

            if (Boolean.TRUE.equals(created)) {
                log.info(
                        "Created {} inactivity warning for user {}",
                        dueStage,
                        snapshot.id()
                );
            }

        } catch (DataIntegrityViolationException exception) {
            // The unique database constraint protects against duplicate
            // user + inactivity-cycle + warning-stage records.
            log.debug(
                    "Warning record already exists or could not be inserted "
                            + "for user {} and stage {}",
                    snapshot.id(),
                    dueStage
            );
        }
    }

    private void cancelUnsentChannels(UserInactivityWarning warning) {

        if (warning.getEmailStatus() != DeliveryStatus.SENT) {
            warning.setEmailStatus(DeliveryStatus.CANCELLED);
            warning.setEmailProcessingStartedAt(null);
        }

        if (warning.getNotificationStatus() != DeliveryStatus.SENT) {
            warning.setNotificationStatus(DeliveryStatus.CANCELLED);
            warning.setNotificationProcessingStartedAt(null);
        }
    }


}
