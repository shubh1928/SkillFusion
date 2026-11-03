package com.barterlink.sessions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionRecordingReadinessResponseDto {

    private UUID sessionId;

    private Instant modeAgreedAt;

    private Instant requesterRecorderReadyAt;

    private Instant providerRecorderReadyAt;

    private boolean requesterRecorderReady;

    private boolean providerRecorderReady;

    private boolean currentUserRecorderReady;

    private boolean bothParticipantsConsented;

    private boolean bothParticipantsRecorderReady;
}