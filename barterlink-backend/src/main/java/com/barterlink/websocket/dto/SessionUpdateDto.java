package com.barterlink.websocket.dto;

import com.barterlink.sessions.enums.SessionStatus;
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
public class SessionUpdateDto {

    private UUID sessionId;

    private UUID agreementId;

    private UUID requesterId;

    private UUID providerId;

    private SessionStatus status;

    private String event;

    private String message;

    private Instant eventTime;
}