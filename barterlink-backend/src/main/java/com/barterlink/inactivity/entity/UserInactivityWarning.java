
package com.barterlink.inactivity.entity;

import com.barterlink.auth.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "user_inactivity_warnings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inactivity_warning_cycle_stage",
                        columnNames = {
                                "user_id",
                                "cycle_started_at",
                                "warning_stage"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_inactivity_email_delivery",
                        columnList = "email_status, created_at"
                ),
                @Index(
                        name = "idx_inactivity_notification_delivery",
                        columnList = "notification_status, created_at"
                )
        }
)
public class UserInactivityWarning {

    public enum WarningStage {
        FIRST,
        SECOND,
        FINAL
    }

    public enum DeliveryStatus {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "warning_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /**
     * The lastActiveAt value that identifies this inactivity cycle.
     * A qualifying user action starts a new cycle.
     */
    @Column(name = "cycle_started_at", nullable = false, updatable = false)
    private Instant cycleStartedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_stage", nullable = false, length = 20, updatable = false)
    private WarningStage warningStage;

    // Email delivery state

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    private DeliveryStatus emailStatus = DeliveryStatus.PENDING;

    @Builder.Default
    @Column(name = "email_attempts", nullable = false)
    private int emailAttempts = 0;

    @Column(name = "email_processing_started_at")
    private Instant emailProcessingStartedAt;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "email_last_error", columnDefinition = "TEXT")
    private String emailLastError;

    // In-app notification delivery state

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    private DeliveryStatus notificationStatus = DeliveryStatus.PENDING;

    @Builder.Default
    @Column(name = "notification_attempts", nullable = false)
    private int notificationAttempts = 0;

    @Column(name = "notification_processing_started_at")
    private Instant notificationProcessingStartedAt;

    @Column(name = "notification_sent_at")
    private Instant notificationSentAt;

    @Column(name = "notification_last_error", columnDefinition = "TEXT")
    private String notificationLastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (emailStatus == null) {
            emailStatus = DeliveryStatus.PENDING;
        }

        if (notificationStatus == null) {
            notificationStatus = DeliveryStatus.PENDING;
        }

        if (emailAttempts < 0) {
            emailAttempts = 0;
        }

        if (notificationAttempts < 0) {
            notificationAttempts = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
