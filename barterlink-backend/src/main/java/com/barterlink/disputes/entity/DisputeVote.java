package com.barterlink.disputes.entity;

import com.barterlink.disputes.enums.DisputeDecisionOutcome;
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
        name = "dispute_votes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dispute_vote_assignment_version",
                        columnNames = {
                                "dispute_id",
                                "panel_assignment_id",
                                "evidence_version"
                        }
                )
        }
)
public class DisputeVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "vote_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    private DisputeCase disputeCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panel_assignment_id", nullable = false)
    private DisputePanelAssignment panelAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private DisputeDecisionOutcome outcome;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence_version", nullable = false)
    private Integer evidenceVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}