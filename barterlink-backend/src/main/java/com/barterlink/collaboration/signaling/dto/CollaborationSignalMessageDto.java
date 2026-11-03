package com.barterlink.collaboration.signaling.dto;

import com.barterlink.collaboration.enums.SignalType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationSignalMessageDto {

    private UUID roomId;

    // NEW
    private UUID senderId;

    private UUID receiverId;

    private SignalType signalType;

    /*
     * SDP Offer / SDP Answer /
     * ICE Candidate
     */
    private String payload;

    @Builder.Default
    private Instant createdAt = Instant.now();
}