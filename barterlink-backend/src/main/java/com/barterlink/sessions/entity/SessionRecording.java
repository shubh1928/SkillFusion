
package com.barterlink.sessions.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.sessions.enums.SessionRecordingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "session_recordings",
        indexes = {
                @Index(
                        name = "idx_session_recordings_session_created",
                        columnList = "session_id, created_at"
                ),
                @Index(
                        name = "idx_session_recordings_status_retention",
                        columnList = "status, retention_delete_at"
                )
        }
)
public class SessionRecording {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "recording_id",
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
            name = "created_by_user_id",
            nullable = false
    )
    private User createdBy;

    @Column(
            name = "google_drive_file_id",
            unique = true,
            length = 255
    )
    private String googleDriveFileId;

    @Column(
            name = "file_name",
            nullable = false,
            length = 255
    )
    private String fileName;

    @Column(
            name = "mime_type",
            nullable = false,
            length = 150
    )
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 32
    )
    @Builder.Default
    private SessionRecordingStatus status =
            SessionRecordingStatus.PENDING_UPLOAD;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(
            name = "sha256",
            length = 64
    )
    private String sha256;

    @Column(
            name = "consent_policy_version",
            nullable = false,
            length = 100
    )
    private String consentPolicyVersion;

    @Column(
            name = "mode_agreed_at",
            nullable = false
    )
    private Instant modeAgreedAt;

    @Column(
            name = "educational_reuse_permitted",
            nullable = false
    )
    @Builder.Default
    private boolean educationalReusePermitted = false;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "upload_started_at")
    private Instant uploadStartedAt;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "retention_delete_at")
    private Instant retentionDeleteAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = SessionRecordingStatus.PENDING_UPLOAD;
        }
    }
}
