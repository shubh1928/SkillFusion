
package com.barterlink.negotiation.entity;

import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.auth.entities.User;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.enums.NegotiationStatus;
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
        name = "negotiations",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "request_id",
                                "provider_id"
                        }
                )
        }
)
public class Negotiation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "negotiation_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NegotiationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 255)
    private String proposedOfferTitle;

    @Column(columnDefinition = "TEXT")
    private String proposedOfferDescription;

    @Column(length = 255)
    private String proposedRequestTitle;

    @Column(columnDefinition = "TEXT")
    private String proposedRequestDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeMode proposedExchangeMode;

    private Integer proposedEstimatedHours;

    private Integer proposedTokenAmount;

    private Instant acceptedAt;

    private Instant closedAt;

    /*
     * Release-mode choices are stored independently.
     * A choice is not an agreement until both participants
     * have selected compatible values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "requester_release_mode", length = 30)
    private AgreementReleaseMode requesterReleaseMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_release_mode", length = 30)
    private AgreementReleaseMode providerReleaseMode;

    /*
     * Each participant independently chooses the milestone count.
     * These fields are relevant only for MILESTONE_RELEASE.
     */
    @Column(name = "requester_milestone_count")
    private Integer requesterMilestoneCount;

    @Column(name = "provider_milestone_count")
    private Integer providerMilestoneCount;

    /*
     * New negotiations require the release-choice workflow.
     * Existing database rows remain NULL after nullable columns
     * are added, allowing us to distinguish legacy records.
     */
    @Column(name = "release_mode_selection_required")
    private Boolean releaseModeSelectionRequired;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = NegotiationStatus.ACTIVE;
        }

        if (releaseModeSelectionRequired == null) {
            releaseModeSelectionRequired = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
