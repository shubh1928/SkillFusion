package com.barterlink.sessions.entity;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.sessions.enums.SessionStatus;
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
@Table(name = "service_sessions")
public class ServiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id",
            nullable = false,
            updatable = false)
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

    @Builder.Default
    @Column(nullable = false)
    private Boolean providerConfirmed = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean requesterConfirmed = false;

    private Instant startedAt;

    private Instant providerConfirmedAt;

    private Instant requesterConfirmedAt;

    private Instant completedAt;

    @Column(nullable = false,
            updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (status == null) {
            status = SessionStatus.CREATED;
        }
    }
}