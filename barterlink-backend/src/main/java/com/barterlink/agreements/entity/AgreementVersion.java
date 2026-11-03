package com.barterlink.agreements.entity;

import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.auth.entities.User;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.entity.Negotiation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "agreement_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_agreement_version_number",
                        columnNames = {"negotiation_id", "version_number"}
                )
        }
)
public class AgreementVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "agreement_version_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negotiation_id", nullable = false)
    private Negotiation negotiation;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AgreementVersionStatus status;

    /*
     * Immutable terms snapshot once submitted for acceptance.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_mode", nullable = false, length = 30)
    private ExchangeMode exchangeMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_mode", length = 30)
    private AgreementReleaseMode releaseMode;

    @Column(name = "offer_title", length = 255)
    private String offerTitle;

    @Column(name = "offer_description", columnDefinition = "TEXT")
    private String offerDescription;

    @Column(name = "request_title", length = 255)
    private String requestTitle;

    @Column(name = "request_description", columnDefinition = "TEXT")
    private String requestDescription;

    @Column(name = "agreed_terms", columnDefinition = "TEXT")
    private String agreedTerms;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    /*
     * All application-level BLT amounts are whole tokens.
     */
    @Column(name = "agreed_token_amount")
    private Integer agreedTokenAmount;

    /*
     * Overall agreement/session deadline.
     */
    @Column(name = "agreed_deadline")
    private Instant agreedDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "requester_accepted_at")
    private Instant requesterAcceptedAt;

    @Column(name = "provider_accepted_at")
    private Instant providerAcceptedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "activation_failure_reason", columnDefinition = "TEXT")
    private String activationFailureReason;

    /*
     * Version-specific milestone terms.
     * Deliberately no orphanRemoval: removing a child from this
     * collection must not silently delete version history.
     */
    @OneToMany(
            mappedBy = "version",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @OrderBy("milestoneNumber ASC")
    @Builder.Default
    private List<AgreementVersionMilestone> milestones =
            new ArrayList<>();

    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = AgreementVersionStatus.DRAFT;
        }
    }
}