package com.barterlink.sessions.dto.response;

import com.barterlink.sessions.enums.SessionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class SessionResponseDto {

    private UUID sessionId;

    private UUID agreementId;

    private UUID requesterId;

    private String requesterName;

    private UUID providerId;

    private String providerName;

    private SessionStatus status;

    private Boolean providerConfirmed;

    private Boolean requesterConfirmed;

    private Instant startedAt;

    private Instant providerConfirmedAt;

    private Instant requesterConfirmedAt;

    private Instant completedAt;

    private Instant createdAt;
}
