package com.barterlink.negotiation.dto.response;

import com.barterlink.negotiation.enums.NegotiationMessageType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class NegotiationMessageResponseDto {

    private UUID messageId;

    private UUID senderId;

    private String senderName;

    private String senderImage;

    private NegotiationMessageType messageType;

    private String message;

    private Instant createdAt;
}