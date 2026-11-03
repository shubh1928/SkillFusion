package com.barterlink.inactivity.service;

import com.barterlink.auth.entities.User;
import com.barterlink.inactivity.entity.UserInactivityWarning;
import com.barterlink.inactivity.entity.UserInactivityWarning.DeliveryStatus;
import com.barterlink.inactivity.entity.UserInactivityWarning.WarningStage;
import com.barterlink.inactivity.repository.UserInactivityWarningRepository;
import com.barterlink.notifications.enums.NotificationType;
import com.barterlink.notifications.service.EmailService;
import com.barterlink.notifications.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class UserInactivityWarningDeliveryService {
    private final UserInactivitySchedulePolicy schedulePolicy;
    private static final int MAX_ATTEMPTS = 5;


    private static final Duration PROCESSING_LEASE =
            Duration.ofMinutes(15);

    private static final List<DeliveryStatus> NON_PROCESSING_RETRYABLE_STATUSES =
            List.of(
                    DeliveryStatus.PENDING,
                    DeliveryStatus.FAILED
            );
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter
                    .ofPattern(
                            "dd MMM uuuu 'at' HH:mm 'UTC'",
                            Locale.ENGLISH
                    )
                    .withZone(ZoneOffset.UTC);

    private final UserInactivityWarningRepository warningRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;


    public UserInactivityWarningDeliveryService(
            UserInactivityWarningRepository warningRepository,
            EmailService emailService,
            NotificationService notificationService,
            PlatformTransactionManager transactionManager,
            UserInactivitySchedulePolicy schedulePolicy) {

        this.warningRepository = warningRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
        this.schedulePolicy = schedulePolicy;
    }


    /**
     * Processes warnings that are pending, failed, or potentially
     * abandoned in PROCESSING status.
     *
     * This method will be connected to the scheduler in a later step.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.inactivity.warning-delivery-delay-ms:60000}",
            initialDelayString =
                    "${app.inactivity.warning-delivery-initial-delay-ms:30000}"
    )
    public void processPendingWarnings() {


        Instant staleProcessingBefore =
                Instant.now().minus(PROCESSING_LEASE);

        List<UUID> warningIds = transactionTemplate.execute(
                transactionStatus ->
                        warningRepository.findEligibleWarningIds(
                                NON_PROCESSING_RETRYABLE_STATUSES,
                                DeliveryStatus.PROCESSING,
                                MAX_ATTEMPTS,
                                staleProcessingBefore
                        )
        );


        if (warningIds == null || warningIds.isEmpty()) {
            return;
        }

        for (UUID warningId : warningIds) {
            deliverOne(warningId, Channel.EMAIL);
            deliverOne(warningId, Channel.NOTIFICATION);
        }
    }

    private void deliverOne(UUID warningId, Channel channel) {

        DeliveryClaim claim = transactionTemplate.execute(
                transactionStatus -> claimDelivery(warningId, channel)
        );

        if (claim == null) {
            return;
        }

        String message = buildMessage(claim.stage(), claim.deadline());

        try {
            if (channel == Channel.EMAIL) {

                String email = claim.email();

                if (email == null || email.isBlank()) {
                    throw new IllegalStateException(
                            "The user's email address is missing."
                    );
                }

                emailService.sendEmail(
                        email,
                        buildTitle(claim.stage()),
                        "Hello " + displayName(claim.displayName())
                                + ",\n\n"
                                + message
                                + "\n\nBarterLink Team"
                );

            } else {

                notificationService.createNotification(
                        claim.userId(),
                        NotificationType.SYSTEM,
                        buildTitle(claim.stage()),
                        message
                );
            }

        } catch (RuntimeException exception) {

            log.warn(
                    "Inactivity {} delivery failed for warning {}",
                    channel,
                    warningId,
                    exception
            );

            try {
                finishDelivery(
                        warningId,
                        channel,
                        DeliveryStatus.FAILED,
                        describeError(exception)
                );
            } catch (RuntimeException stateException) {
                log.error(
                        "Could not save failed-delivery status for warning {}",
                        warningId,
                        stateException
                );
            }

            return;
        }

        try {
            finishDelivery(
                    warningId,
                    channel,
                    DeliveryStatus.SENT,
                    null
            );
        } catch (RuntimeException exception) {
            /*
             * The external service may already have accepted the message.
             * Do not incorrectly mark it FAILED here; a retry could duplicate it.
             */
            log.error(
                    "Delivery succeeded, but its SENT status could not be saved "
                            + "for warning {} and channel {}",
                    warningId,
                    channel,
                    exception
            );
        }
    }

    /**
     * Claims one delivery inside a short database transaction.
     * External email and notification calls happen after this transaction.
     */
    private DeliveryClaim claimDelivery(
            UUID warningId,
            Channel channel) {

        UserInactivityWarning warning =
                warningRepository.findByIdForUpdate(warningId)
                        .orElse(null);

        if (warning == null) {
            return null;
        }

        User user = warning.getUser();

        // A new activity timestamp means this warning belongs to an old cycle.
        if (!Objects.equals(
                warning.getCycleStartedAt(),
                user.getLastActiveAt())) {

            cancelObsoleteDeliveries(warning);
            warningRepository.save(warning);
            return null;
        }


        Instant now = Instant.now();

        Instant deadline = schedulePolicy.calculateDeletionDeadline(
                warning.getCycleStartedAt()
        );

// Never send an inactivity warning at or after the deadline.
        if (!now.isBefore(deadline)) {
            cancelObsoleteDeliveries(warning);
            warningRepository.save(warning);
            return null;
        }

// Use the central policy to determine the latest due stage.
        WarningStage latestDueStage =
                schedulePolicy.findLatestDueStage(
                        warning.getCycleStartedAt(),
                        now
                );

// No warning stage is due yet, so do not send anything.
        if (latestDueStage == null) {
            return null;
        }

// Policy B: allow only the latest due stage to be delivered.
// Preserve successfully sent channels; cancel unsent channels
// on an older warning record.
        if (warning.getWarningStage() != latestDueStage) {

            Instant thisWarningTime =
                    schedulePolicy.calculateWarningTime(
                            warning.getCycleStartedAt(),
                            warning.getWarningStage()
                    );

            Instant latestDueTime =
                    schedulePolicy.calculateWarningTime(
                            warning.getCycleStartedAt(),
                            latestDueStage
                    );

            if (thisWarningTime.isBefore(latestDueTime)) {
                cancelObsoleteDeliveries(warning);
                warningRepository.save(warning);
            }

            return null;
        }

        DeliveryStatus status = getStatus(warning, channel);

        if (status == DeliveryStatus.SENT
                || status == DeliveryStatus.CANCELLED) {
            return null;
        }

        Instant processingStartedAt =
                getProcessingStartedAt(warning, channel);


        // Another worker may currently be delivering this channel.
        if (status == DeliveryStatus.PROCESSING
                && processingStartedAt != null
                && processingStartedAt.isAfter(
                now.minus(PROCESSING_LEASE))) {
            return null;
        }

        int attempts = getAttempts(warning, channel);


        if (attempts >= MAX_ATTEMPTS) {

            String lastError = getLastError(warning, channel);

            boolean requiresUpdate =
                    status != DeliveryStatus.FAILED
                            || processingStartedAt != null
                            || lastError == null;

            // The channel has already exhausted its attempts and is
            // fully recorded as FAILED. Do not update it repeatedly.
            if (!requiresUpdate) {
                return null;
            }

            setStatus(warning, channel, DeliveryStatus.FAILED);
            setProcessingStartedAt(warning, channel, null);

            if (lastError == null) {
                setLastError(
                        warning,
                        channel,
                        "Maximum delivery attempts reached."
                );
            }

            warningRepository.save(warning);
            return null;
        }


        setStatus(warning, channel, DeliveryStatus.PROCESSING);
        setAttempts(warning, channel, attempts + 1);
        setProcessingStartedAt(warning, channel, now);
        setLastError(warning, channel, null);

        warningRepository.save(warning);


        return new DeliveryClaim(
                warning.getId(),
                user.getId(),
                user.getEmail(),
                user.getName(),
                warning.getWarningStage(),
                deadline
        );
    }

    private void finishDelivery(
            UUID warningId,
            Channel channel,
            DeliveryStatus finalStatus,
            String error) {

        transactionTemplate.executeWithoutResult(transactionStatus -> {

            UserInactivityWarning warning =
                    warningRepository.findByIdForUpdate(warningId)
                            .orElse(null);

            if (warning == null) {
                return;
            }

            // Do not complete a warning whose inactivity cycle has expired.
            if (!Objects.equals(
                    warning.getCycleStartedAt(),
                    warning.getUser().getLastActiveAt())) {

                cancelObsoleteDeliveries(warning);
                warningRepository.save(warning);
                return;
            }

            // Do not overwrite a cancellation or a changed state.
            if (getStatus(warning, channel)
                    != DeliveryStatus.PROCESSING) {
                return;
            }

            setStatus(warning, channel, finalStatus);
            setProcessingStartedAt(warning, channel, null);
            setLastError(warning, channel, error);

            if (finalStatus == DeliveryStatus.SENT) {
                setSentAt(warning, channel, Instant.now());
            }

            warningRepository.save(warning);
        });
    }

    private void cancelObsoleteDeliveries(
            UserInactivityWarning warning) {

        if (warning.getEmailStatus() != DeliveryStatus.SENT) {
            warning.setEmailStatus(DeliveryStatus.CANCELLED);
            warning.setEmailProcessingStartedAt(null);
        }

        if (warning.getNotificationStatus() != DeliveryStatus.SENT) {
            warning.setNotificationStatus(DeliveryStatus.CANCELLED);
            warning.setNotificationProcessingStartedAt(null);
        }
    }

    private DeliveryStatus getStatus(
            UserInactivityWarning warning,
            Channel channel) {

        return channel == Channel.EMAIL
                ? warning.getEmailStatus()
                : warning.getNotificationStatus();
    }

    private void setStatus(
            UserInactivityWarning warning,
            Channel channel,
            DeliveryStatus status) {

        if (channel == Channel.EMAIL) {
            warning.setEmailStatus(status);
        } else {
            warning.setNotificationStatus(status);
        }
    }

    private int getAttempts(
            UserInactivityWarning warning,
            Channel channel) {

        return channel == Channel.EMAIL
                ? warning.getEmailAttempts()
                : warning.getNotificationAttempts();
    }

    private void setAttempts(
            UserInactivityWarning warning,
            Channel channel,
            int attempts) {

        if (channel == Channel.EMAIL) {
            warning.setEmailAttempts(attempts);
        } else {
            warning.setNotificationAttempts(attempts);
        }
    }

    private Instant getProcessingStartedAt(
            UserInactivityWarning warning,
            Channel channel) {

        return channel == Channel.EMAIL
                ? warning.getEmailProcessingStartedAt()
                : warning.getNotificationProcessingStartedAt();
    }

    private void setProcessingStartedAt(
            UserInactivityWarning warning,
            Channel channel,
            Instant startedAt) {

        if (channel == Channel.EMAIL) {
            warning.setEmailProcessingStartedAt(startedAt);
        } else {
            warning.setNotificationProcessingStartedAt(startedAt);
        }
    }

    private void setSentAt(
            UserInactivityWarning warning,
            Channel channel,
            Instant sentAt) {

        if (channel == Channel.EMAIL) {
            warning.setEmailSentAt(sentAt);
        } else {
            warning.setNotificationSentAt(sentAt);
        }
    }

    private String getLastError(
            UserInactivityWarning warning,
            Channel channel) {

        return channel == Channel.EMAIL
                ? warning.getEmailLastError()
                : warning.getNotificationLastError();
    }

    private void setLastError(
            UserInactivityWarning warning,
            Channel channel,
            String error) {

        if (channel == Channel.EMAIL) {
            warning.setEmailLastError(error);
        } else {
            warning.setNotificationLastError(error);
        }
    }

    private String buildTitle(WarningStage stage) {

        return switch (stage) {
            case FIRST ->
                    "BarterLink inactivity reminder: 30 days";
            case SECOND ->
                    "BarterLink inactivity reminder: 7 days";
            case FINAL ->
                    "Final BarterLink inactivity reminder";
        };
    }

    private String buildMessage(
            WarningStage stage,
            Instant deadline) {

        String formattedDeadline =
                DEADLINE_FORMATTER.format(deadline);

        String timing = switch (stage) {
            case FIRST -> "This is your first inactivity reminder.";
            case SECOND -> "This is your 7-day inactivity reminder.";
            case FINAL -> "This is your final inactivity reminder.";
        };

        return timing
                + "\n\nThe inactivity deadline for your BarterLink account is "
                + formattedDeadline
                + "."
                + "\n\nIf no qualifying activity is recorded by this deadline, "
                + "your account may become eligible for deletion, subject to "
                + "safety checks."
                + "\n\nAccounts will not be deleted while unresolved exchanges, "
                + "escrow or token obligations, disputes, or other blocking "
                + "matters require the account to remain."
                + "\n\nSign in or perform a qualifying action on BarterLink "
                + "before the deadline to reset your inactivity period.";
    }

    private String displayName(String name) {
        return name == null || name.isBlank() ? "there" : name.trim();
    }

    private String describeError(RuntimeException exception) {
        String description = exception.getMessage();

        if (description == null || description.isBlank()) {
            description = exception.getClass().getSimpleName();
        }

        // Avoid storing an unbounded error message in the database.
        return description.length() > 4000
                ? description.substring(0, 4000)
                : description;
    }

    private enum Channel {
        EMAIL,
        NOTIFICATION
    }

    private record DeliveryClaim(
            UUID warningId,
            UUID userId,
            String email,
            String displayName,
            WarningStage stage,
            Instant deadline) {
    }
}