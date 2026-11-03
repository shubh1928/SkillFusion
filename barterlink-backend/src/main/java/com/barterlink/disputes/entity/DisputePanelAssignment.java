package com.barterlink.disputes.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.disputes.enums.DisputePanelAssignmentStatus;

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
        name = "dispute_panel_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dispute_panelist",
                        columnNames = {
                                "dispute_id",
                                "panelist_id"
                        }
                )
        }
)
public class DisputePanelAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "assignment_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private DisputeCase disputeCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panelist_id", nullable = false)
    private User panelist;

    /*
     * The panel seat is numbered 1 through 5.
     * A replacement can occupy the same seat while the previous
     * assignment remains in the audit history.
     */
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private DisputePanelAssignmentStatus status =
            DisputePanelAssignmentStatus.ASSIGNED;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }

        if (status == null) {
            status = DisputePanelAssignmentStatus.ASSIGNED;
        }
    }
}