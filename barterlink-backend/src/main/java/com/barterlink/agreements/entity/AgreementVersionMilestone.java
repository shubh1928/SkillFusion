package com.barterlink.agreements.entity;

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
        name = "agreement_version_milestones",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_version_milestone_number",
                        columnNames = {"agreement_version_id", "milestone_number"}
                ),
                @UniqueConstraint(
                        name = "uk_version_milestone_identity",
                        columnNames = {"agreement_version_id", "milestone_id"}
                )
        }
)
public class AgreementVersionMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "version_milestone_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion version;

    /*
     * Links this version's snapshot to the stable work-item identity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private AgreementMilestone milestone;

    @Column(name = "milestone_number", nullable = false)
    private Integer milestoneNumber;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "deliverable", columnDefinition = "TEXT")
    private String deliverable;

    @Column(name = "deadline")
    private Instant deadline;

    /*
     * Whole BLT token allocation for this milestone in this version.
     * The service will validate positivity and the exact total before
     * the version can be submitted for acceptance.
     */
    @Column(name = "token_allocation")
    private Integer tokenAllocation;
}