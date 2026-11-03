
package com.barterlink.agreements.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.negotiation.entity.Negotiation;
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
        name = "agreement_collaboration_updates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_agreement_collaboration_update",
                        columnNames = {
                                "agreement_version_id",
                                "client_update_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_agreement_negotiation_collaboration_update",
                        columnNames = {
                                "negotiation_id",
                                "client_update_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_agreement_collaboration_version_created",
                        columnList = "agreement_version_id, created_at"
                ),
                @Index(
                        name = "idx_agreement_collaboration_negotiation_created",
                        columnList = "negotiation_id, created_at"
                )
        }
)
public class AgreementCollaborationUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "update_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    /*
     * Used for collaboration after an AgreementVersion exists.
     * Nullable for the initial negotiation-level shared document.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "agreement_version_id",
            nullable = true,
            updatable = false
    )
    private AgreementVersion version;

    /*
     * Used for collaboration inside the Current Deal form
     * before an AgreementVersion has been created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "negotiation_id",
            nullable = true,
            updatable = false
    )
    private Negotiation negotiation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            updatable = false
    )
    private User sender;

    @Column(
            name = "client_update_id",
            nullable = false,
            length = 36,
            updatable = false
    )
    private String clientUpdateId;

    @Lob
    @Column(
            name = "update_payload",
            nullable = false,
            columnDefinition = "LONGBLOB"
    )
    private byte[] updatePayload;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        /*
         * An update must belong to exactly one document:
         * either an agreement version or a negotiation.
         */
        if ((version == null) == (negotiation == null)) {
            throw new IllegalStateException(
                    "Collaboration update must belong to exactly one agreement document"
            );
        }
    }
}
