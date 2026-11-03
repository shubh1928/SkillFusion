package com.barterlink.sessions.entity;

import com.barterlink.auth.entities.User;
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
@Table(name = "session_recording_consents")
public class SessionRecordingConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "consent_id",
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
            name = "user_id",
            nullable = false
    )
    private User user;

    /*
     * Required permission to record the live session
     * for the purposes described in the recording policy.
     */
    @Column(
            name = "recording_consent",
            nullable = false
    )
    private boolean recordingConsent;

    /*
     * Separate, optional permission for educational
     * derivatives and their approved distribution.
     */
    @Column(
            name = "educational_reuse_consent",
            nullable = false
    )
    private boolean educationalReuseConsent;

    /*
     * Server-assigned version of the consent policy
     * presented to the participant.
     */
    @Column(
            name = "policy_version",
            nullable = false,
            length = 100
    )
    private String policyVersion;

    /*
     * Associates this consent with the specific
     * mutually agreed session-mode decision.
     * Consent from an earlier mode agreement must
     * not automatically authorize a later one.
     */
    @Column(
            name = "mode_agreed_at",
            nullable = false
    )
    private Instant modeAgreedAt;

    /*
     * Each submission is a new record so that a
     * later refusal or withdrawal does not erase
     * the earlier consent history.
     */
    @Column(
            name = "decided_at",
            nullable = false,
            updatable = false
    )
    private Instant decidedAt;

    @PrePersist
    protected void onCreate() {
        if (decidedAt == null) {
            decidedAt = Instant.now();
        }
    }
}