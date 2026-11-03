package com.barterlink.sessions.dto.response;

import com.barterlink.sessions.enums.SessionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.sessions.enums.SessionMode;

@Getter
@Setter
public class SessionResponseDto {

    private UUID sessionId;

    /**
     * Populated by the session-dispute endpoint when a persistent case is created.
     */
    private UUID disputeId;

    private UUID agreementId;

    private UUID negotiationId;

    private UUID requesterId;

    private String requesterName;

    private UUID providerId;

    private String providerName;

    private SessionStatus status;

    private ExchangeMode exchangeMode;

    /*
     * Session mode selection and mutual agreement.
     */
    private SessionMode requesterSelectedMode;

    private SessionMode providerSelectedMode;

    private SessionMode agreedSessionMode;

    private Instant requesterModeSelectedAt;

    private Instant providerModeSelectedAt;

    private Instant modeAgreedAt;

    /*
     * Both participants must click
     * Start Session before the session
     * becomes IN_PROGRESS.
     */
    private Boolean providerStarted;

    private Boolean requesterStarted;

    /*
     * Work completion confirmation.
     */
    private Boolean providerConfirmed;

    private Boolean requesterConfirmed;

    /*
     * Optional scheduling.
     */
    private Instant scheduledAt;

    private Instant scheduleAcceptedAt;

    /*
     * Session timestamps.
     */
    private Instant startedAt;

    /*
     * Individual work completion.
     */
    private Instant providerCompletedAt;

    private Instant requesterCompletedAt;

    /*
     * Final confirmations.
     */
    private Instant providerConfirmedAt;

    private Instant requesterConfirmedAt;

    /*
     * Session finished.
     */
    private Instant completedAt;

    private Instant cancelledAt;

    private Instant createdAt;
}