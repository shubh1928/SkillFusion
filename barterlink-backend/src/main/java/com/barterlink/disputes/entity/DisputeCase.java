package com.barterlink.disputes.entity;

import com.barterlink.agreements.entity.AgreementMilestone;
import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.auth.entities.User;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import com.barterlink.sessions.entity.ServiceSession;

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
@Table(name = "dispute_cases")
public class DisputeCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "dispute_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    /*
     * The session associated with this dispute.
     * A milestone-specific dispute also belongs to its session.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ServiceSession session;

    /*
     * Null for a general session dispute.
     * Set for a milestone-specific dispute.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private AgreementMilestone milestone;

    /*
     * Active agreement version when the dispute was opened.
     * Null can be valid for legacy agreements.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_at_opening_id")
    private AgreementVersion agreementVersionAtOpening;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimant_id", nullable = false)
    private User claimant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_id", nullable = false)
    private User respondent;

    @Column(
            name = "reason",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    @Builder.Default
    private DisputeCaseStatus status =
            DisputeCaseStatus.PANEL_ASSIGNMENT_PENDING;

    /*
     * Increment when new evidence is added.
     * Votes will later be associated with the evidence version reviewed.
     */
    @Builder.Default
    @Column(name = "evidence_version", nullable = false)
    private Integer evidenceVersion = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "panel_assigned_at")
    private Instant panelAssignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_outcome", length = 30)
    private DisputeDecisionOutcome decisionOutcome;

    @Column(name = "decision_summary", columnDefinition = "TEXT")
    private String decisionSummary;

    @Column(name = "decision_declared_at")
    private Instant decisionDeclaredAt;

    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    /**
     * Prevents duplicate automatic blockchain settlement attempts.
     * Once claimed, the case must be reconciled instead of resubmitted.
     */
    @Column(
            name = "settlement_attempt_claimed",
            nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE"
    )
    private boolean settlementAttemptClaimed;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = DisputeCaseStatus.PANEL_ASSIGNMENT_PENDING;
        }

        if (evidenceVersion == null) {
            evidenceVersion = 0;
        }
    }
}