package com.barterlink.rewards.entity;

import com.barterlink.auth.entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "reward_streak_days",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reward_streak_user_utc_date",
                        columnNames = {
                                "user_id",
                                "activity_date_utc"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_reward_streak_user_date",
                        columnList = "user_id,activity_date_utc"
                ),
                @Index(
                        name = "idx_reward_streak_user_qualified_date",
                        columnList = "user_id,daily_qualified,activity_date_utc"
                )
        }
)
public class RewardStreakDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "reward_streak_day_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Server-calculated UTC calendar date.
     */
    @Column(name = "activity_date_utc", nullable = false)
    private LocalDate activityDateUtc;

    @Builder.Default
    @Column(name = "login_recorded", nullable = false)
    private boolean loginRecorded = false;

    @Column(name = "first_login_at")
    private Instant firstLoginAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Builder.Default
    @Column(
            name = "qualifying_activity_recorded",
            nullable = false
    )
    private boolean qualifyingActivityRecorded = false;

    @Column(name = "first_activity_type", length = 50)
    private String firstQualifyingActivityType;

    @Column(name = "first_activity_source_id", length = 100)
    private String firstQualifyingSourceId;

    @Column(name = "first_activity_at")
    private Instant firstQualifyingActivityAt;

    @Column(name = "last_activity_type", length = 50)
    private String lastQualifyingActivityType;

    @Column(name = "last_activity_source_id", length = 100)
    private String lastQualifyingSourceId;

    @Column(name = "last_activity_at")
    private Instant lastQualifyingActivityAt;

    /**
     * True only after both a successful login and an allowed,
     * successful activity have been recorded for this UTC date.
     */
    @Builder.Default
    @Column(name = "daily_qualified", nullable = false)
    private boolean dailyQualified = false;

    @Column(name = "daily_qualified_at")
    private Instant dailyQualifiedAt;

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

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}