package com.barterlink.collaboration.dto.response;

import com.barterlink.collaboration.enums.CollaborationRoomStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CollaborationRoomResponseDto {

    private UUID roomId;

    private UUID sessionId;

    private CollaborationRoomStatus status;

    private Instant createdAt;

    private Instant activatedAt;

    private Instant closedAt;

}