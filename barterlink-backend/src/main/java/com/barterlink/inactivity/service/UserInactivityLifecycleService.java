
package com.barterlink.inactivity.service;

import com.barterlink.auth.entities.Provider;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.RefreshTokenRepository;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.inactivity.dto.InactivityUserSnapshot;
import com.barterlink.inactivity.entity.RetainedUserIdentity;
import com.barterlink.inactivity.repository.RetainedUserIdentityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "app.inactivity.lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class UserInactivityLifecycleService {

    private static final String INACTIVE_USER_NAME = "Inactive user";

    private final UserRepository userRepository;
    private final RetainedUserIdentityRepository retainedIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserInactivitySchedulePolicy schedulePolicy;
    private final TransactionTemplate transactionTemplate;

    public UserInactivityLifecycleService(
            UserRepository userRepository,
            RetainedUserIdentityRepository retainedIdentityRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserInactivitySchedulePolicy schedulePolicy,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.retainedIdentityRepository = retainedIdentityRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.schedulePolicy = schedulePolicy;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    /**
     * Finds accounts whose three-calendar-month inactivity deadline
     * has passed. The scheduler remains disabled unless explicitly enabled
     * through app.inactivity.lifecycle.enabled=true.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.inactivity.lifecycle.deactivation-delay-ms:300000}",
            initialDelayString =
                    "${app.inactivity.lifecycle.initial-delay-ms:60000}"
    )
    public void deactivateDueAccounts() {
        List<InactivityUserSnapshot> snapshots;

        try {
            snapshots = userRepository.findInactivitySnapshots();
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to load accounts for inactivity deactivation",
                    exception
            );
            return;
        }

        Instant scanTime = Instant.now();

        for (InactivityUserSnapshot snapshot : snapshots) {
            if (snapshot.lastActiveAt() == null) {
                continue;
            }

            Instant deadline = schedulePolicy.calculateDeletionDeadline(
                    snapshot.lastActiveAt()
            );

            if (scanTime.isBefore(deadline)) {
                continue;
            }

            try {
                Boolean deactivated = transactionTemplate.execute(status ->
                        deactivateOne(snapshot, Instant.now())
                );

                if (Boolean.TRUE.equals(deactivated)) {
                    log.info(
                            "Deactivated inactive account {}",
                            snapshot.id()
                    );
                }
            } catch (RuntimeException exception) {
                log.error(
                        "Unable to deactivate inactive account {}",
                        snapshot.id(),
                        exception
                );
            }
        }
    }

    private boolean deactivateOne(
            InactivityUserSnapshot snapshot,
            Instant now
    ) {
        UUID userId = snapshot.id();

        User user = userRepository.findByIdForUpdate(userId)
                .orElse(null);

        if (user == null) {
            return false;
        }

        // Skip accounts already processed by the lifecycle.
        if (user.getInactivityDeactivatedAt() != null
                || user.getAnonymizedAt() != null) {
            return false;
        }

        // The initial scan may be stale if the account became active.
        if (!Objects.equals(
                user.getLastActiveAt(),
                snapshot.lastActiveAt()
        )) {
            return false;
        }

        Instant lastActiveAt = user.getLastActiveAt();

        if (lastActiveAt == null) {
            return false;
        }

        // Recalculate the deadline while holding the user row lock.
        Instant currentDeadline =
                schedulePolicy.calculateDeletionDeadline(lastActiveAt);

        if (now.isBefore(currentDeadline)) {
            return false;
        }

        // A pre-existing retained record with no lifecycle marker is
        // unexpected. Fail closed rather than create duplicate retention.
        if (retainedIdentityRepository.findByUser_Id(userId).isPresent()) {
            throw new IllegalStateException(
                    "Retained identity already exists for user " + userId
            );
        }

        // The one-month retention period begins when deactivation occurs.
        Instant retentionExpiresAt = now
                .atZone(ZoneOffset.UTC)
                .plusMonths(1)
                .toInstant();

        RetainedUserIdentity retainedIdentity =
                RetainedUserIdentity.builder()
                        .user(user)
                        .originalEmail(user.getEmail())
                        .originalName(user.getName())
                        .originalImage(user.getImage())
                        .originalProvider(user.getProvider())
                        .originalProviderId(user.getProviderId())
                        .originalBio(user.getBio())
                        .retainedAt(now)
                        .expiresAt(retentionExpiresAt)
                        .build();

        // Persist identity retention in the same transaction as deactivation.
        retainedIdentityRepository.saveAndFlush(retainedIdentity);

        // Immediately prevent access and record lifecycle timestamps.
        user.setEnable(false);
        user.setInactivityDeactivatedAt(now);
        user.setIdentityRetentionExpiresAt(retentionExpiresAt);

        // Release the original email and remove identity from the historical
        // row while retaining its stable ID and existing relationships.
        user.setEmail(null);
        user.setName(INACTIVE_USER_NAME);
        user.setImage(null);
        user.setPassword(null);
        user.setProviderId(null);
        user.setBio(null);
        user.setProvider(Provider.LOCAL);

        // Revoke outstanding refresh tokens within this transaction.
        refreshTokenRepository.revokeAllByUserId(userId);

        userRepository.save(user);

        return true;
    }


    /**
     * Purges retained identity records after the additional one-month
     * retention period has expired.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.inactivity.lifecycle.retention-delay-ms:300000}",
            initialDelayString =
                    "${app.inactivity.lifecycle.retention-initial-delay-ms:90000}"
    )
    public void purgeExpiredRetainedIdentities() {
        List<UUID> expiredUserIds;

        try {
            expiredUserIds =
                    retainedIdentityRepository.findExpiredUserIds(Instant.now());
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to load expired retained identities",
                    exception
            );
            return;
        }

        for (UUID userId : expiredUserIds) {
            try {
                Boolean anonymized = transactionTemplate.execute(status ->
                        anonymizeOne(userId, Instant.now())
                );

                if (Boolean.TRUE.equals(anonymized)) {
                    log.info(
                            "Purged retained identity for historical user {}",
                            userId
                    );
                }
            } catch (RuntimeException exception) {
                log.error(
                        "Unable to purge retained identity for user {}",
                        userId,
                        exception
                );
            }
        }
    }

    private boolean anonymizeOne(UUID userId, Instant now) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElse(null);

        if (user == null) {
            // Preserve unexpected cases for investigation.
            return false;
        }

        if (user.getInactivityDeactivatedAt() == null
                || user.getAnonymizedAt() != null) {
            return false;
        }

        Instant userRetentionExpiry =
                user.getIdentityRetentionExpiresAt();

        if (userRetentionExpiry == null
                || now.isBefore(userRetentionExpiry)) {
            return false;
        }

        RetainedUserIdentity retainedIdentity =
                retainedIdentityRepository.findByUserIdForUpdate(userId)
                        .orElse(null);

        if (retainedIdentity == null) {
            // Do not mark the account anonymized if its retained identity
            // is unexpectedly missing.
            return false;
        }

        Instant identityExpiry = retainedIdentity.getExpiresAt();

        if (identityExpiry == null
                || now.isBefore(identityExpiry)) {
            return false;
        }

        if (!Objects.equals(userRetentionExpiry, identityExpiry)) {
            throw new IllegalStateException(
                    "User and retained identity expiry timestamps do not match "
                            + "for user " + userId
            );
        }

        // Ensure direct identity fields remain cleared.
        user.setEmail(null);
        user.setName("Inactive user");
        user.setImage(null);
        user.setPassword(null);
        user.setProviderId(null);
        user.setBio(null);
        user.setProvider(Provider.LOCAL);

        // Mark the historical account as anonymized.
        user.setAnonymizedAt(now);

        // Delete only the private retained identity record.
        retainedIdentityRepository.delete(retainedIdentity);

        // Keep the original user ID and all existing history relationships.
        userRepository.save(user);

        return true;
    }


}
