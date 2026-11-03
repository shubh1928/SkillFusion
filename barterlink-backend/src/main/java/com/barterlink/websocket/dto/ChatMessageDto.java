package com.barterlink.websocket.dto;

import com.barterlink.negotiation.enums.NegotiationMessageType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private UUID negotiationId;

    private UUID messageId;

    private UUID senderId;

    private String senderName;

    private String senderImage;

    private NegotiationMessageType messageType;

    private String message;

    private String attachmentUrl;

    private Instant createdAt;

}