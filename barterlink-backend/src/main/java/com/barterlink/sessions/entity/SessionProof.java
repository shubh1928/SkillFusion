
package com.barterlink.sessions.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.sessions.enums.ProofType;
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
@Table(name = "session_proofs")
public class SessionProof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "proof_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            nullable = false
    )
    private ServiceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "uploaded_by",
            nullable = false
    )
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "proof_type",
            nullable = false
    )
    private ProofType proofType;

    /**
     * External URL for link-based proofs.
     * Null for proofs backed by uploaded files.
     */
    @Column(
            name = "proof_url",
            nullable = true,
            length = 2000
    )
    private String proofUrl;

    /**
     * Original filename supplied for an uploaded attachment,
     * sanitized by the backend before storage.
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * Validated media type of the uploaded attachment.
     */
    @Column(name = "content_type", length = 127)
    private String contentType;

    /**
     * Uploaded file size in bytes.
     * Null for link-based proofs.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * SHA-256 hash of the uploaded file bytes.
     */
    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
