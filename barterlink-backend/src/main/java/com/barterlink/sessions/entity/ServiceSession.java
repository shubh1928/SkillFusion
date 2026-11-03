package com.barterlink.sessions.entity;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.auth.entities.User;
import com.barterlink.sessions.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import com.barterlink.sessions.enums.SessionMode;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "service_sessions")
public class ServiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "session_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "agreement_id",
            nullable = false,
            unique = true
    )
    private ServiceAgreement agreement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    /*
     * Session mode selection and mutual agreement.
     * Both participants must select the same mode before
     * agreedSessionMode is populated.
     */

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_selected_mode", length = 32)
    private SessionMode requesterSelectedMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_selected_mode", length = 32)
    private SessionMode providerSelectedMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreed_session_mode", length = 32)
    private SessionMode agreedSessionMode;

    @Column(name = "requester_mode_selected_at")
    private Instant requesterModeSelectedAt;

    @Column(name = "provider_mode_selected_at")
    private Instant providerModeSelectedAt;

    @Column(name = "mode_agreed_at")
    private Instant modeAgreedAt;

    /**
     * Recording preparation status for each participant.
     *
     * These timestamps are set only after the corresponding
     * participant reports successful recorder preparation.
     *
     * A null timestamp means that preparation has not been
     * confirmed. These fields alone do not authorize live media.
     */
    /**
     * Tracks when each participant explicitly requests
     * preparation for a live session.
     *
     * These timestamps do not mean that the session has started.
     * Both participants must request preparation before the room
     * can enter the PREPARING state.
     */
    @Column(name = "requester_live_preparation_requested_at")
    private Instant requesterLivePreparationRequestedAt;

    @Column(name = "provider_live_preparation_requested_at")
    private Instant providerLivePreparationRequestedAt;

    /**
     * Tracks when each participant reports recorder readiness.
     *
     * These timestamps are separate from preparation requests.
     * A preparation request alone does not establish recorder readiness.
     */
    @Column(name = "requester_recorder_ready_at")
    private Instant requesterRecorderReadyAt;

    @Column(name = "provider_recorder_ready_at")
    private Instant providerRecorderReadyAt;

    /*
     * Session start confirmation
     * Session starts only after BOTH participants click "Start Session".
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean providerStarted = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean requesterStarted = false;

    /*
     * Work completion confirmation.
     * Especially useful for Direct Barter where both participants complete work.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean providerConfirmed = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean requesterConfirmed = false;

    /*
     * Optional scheduling support.
     */
    private Instant scheduledAt;

    private Instant scheduleAcceptedAt;

    /*
     * Session lifecycle timestamps.
     */
    private Instant startedAt;

    /*
     * When each participant marks
     * "I have completed my work."
     */
    private Instant providerCompletedAt;

    private Instant requesterCompletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_by")
    private User scheduledBy;

    /*
     * When each participant confirms
     * the other participant's work.
     */
    private Instant providerConfirmedAt;

    private Instant requesterConfirmedAt;

    /*
     * Final session completion.
     */
    private Instant completedAt;

    private Instant cancelledAt;

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<SessionProof> proofs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (status == null) {
            status = SessionStatus.CREATED;
        }
    }
}