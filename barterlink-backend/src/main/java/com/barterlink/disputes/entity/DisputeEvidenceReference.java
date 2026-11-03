package com.barterlink.disputes.entity;

import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.disputes.enums.DisputeEvidenceType;
import com.barterlink.negotiation.entity.NegotiationMessage;
import com.barterlink.sessions.entity.SessionProof;
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
        name = "dispute_evidence_references",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dispute_evidence_session_proof",
                        columnNames = {"dispute_id", "session_proof_id"}
                ),
                @UniqueConstraint(
                        name = "uk_dispute_evidence_message",
                        columnNames = {"dispute_id", "negotiation_message_id"}
                ),
                @UniqueConstraint(
                        name = "uk_dispute_evidence_agreement_version",
                        columnNames = {"dispute_id", "agreement_version_id"}
                )
        }
)
public class DisputeEvidenceReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "evidence_reference_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    private DisputeCase disputeCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 40)
    private DisputeEvidenceType evidenceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_proof_id")
    private SessionProof sessionProof;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negotiation_message_id")
    private NegotiationMessage negotiationMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id")
    private AgreementVersion agreementVersion;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private Instant attachedAt;

    @PrePersist
    protected void onCreate() {
        if (attachedAt == null) {
            attachedAt = Instant.now();
        }
    }
}