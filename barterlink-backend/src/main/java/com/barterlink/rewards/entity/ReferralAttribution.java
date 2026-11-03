package com.barterlink.rewards.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.rewards.enums.ReferralAttributionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "referral_attributions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_referral_referred_user",
                        columnNames = "referred_user_id"
                ),
                @UniqueConstraint(
                        name = "uk_referral_referrer_referred_pair",
                        columnNames = {
                                "referrer_user_id",
                                "referred_user_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_referral_referrer_status",
                        columnList = "referrer_user_id,status"
                ),
                @Index(
                        name = "idx_referral_status_qualified_at",
                        columnList = "status,qualified_at"
                )
        }
)
public class ReferralAttribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "referral_attribution_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "referrer_user_id",
            nullable = false,
            updatable = false
    )
    private User referrer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "referred_user_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private User referredUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReferralAttributionStatus status =
            ReferralAttributionStatus.TRACKING;

    @Column(name = "attributed_at", nullable = false, updatable = false)
    private Instant attributedAt;

    @Column(name = "qualified_at")
    private Instant qualifiedAt;

    @Column(name = "reward_queued_at")
    private Instant rewardQueuedAt;

    @Column(name = "lifetime_cap_reached_at")
    private Instant lifetimeCapReachedAt;

    @Column(name = "review_required_at")
    private Instant reviewRequiredAt;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Column(name = "qualifying_direct_barter_session_count")
    private Integer qualifyingDirectBarterSessionCount;

    /**
     * Two distinct completed Direct Barter sessions used to evaluate
     * referral qualification.
     */
    @Column(name = "qualifying_session_id_one")
    private UUID qualifyingSessionIdOne;

    @Column(name = "qualifying_session_id_two")
    private UUID qualifyingSessionIdTwo;

    /**
     * Identifies which qualifying session involved a counterparty
     * other than the referrer. Null while the referral is waiting.
     */
    @Column(name = "other_counterparty_session_id")
    private UUID otherCounterpartySessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (attributedAt == null) {
            attributedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = ReferralAttributionStatus.TRACKING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}