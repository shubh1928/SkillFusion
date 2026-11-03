package com.barterlink.sessions.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class SessionRecordingConsentResponseDto {

    private UUID sessionId;

    /*
     * null = no decision submitted
     * true = consent given
     * false = consent declined
     */
    private Boolean requesterRecordingConsent;

    private Boolean providerRecordingConsent;

    /*
     * Separate optional educational-reuse decisions.
     */
    private Boolean requesterEducationalReuseConsent;

    private Boolean providerEducationalReuseConsent;

    private Instant requesterConsentAt;

    private Instant providerConsentAt;

    private String policyVersion;

    private Instant modeAgreedAt;

    /*
     * True only when both participants have explicitly
     * consented to recording for the current mode agreement.
     */
    private boolean bothParticipantsConsented;
}