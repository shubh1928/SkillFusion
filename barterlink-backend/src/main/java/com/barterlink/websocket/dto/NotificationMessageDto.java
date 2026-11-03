package com.barterlink.websocket.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessageDto {

    private UUID notificationId;

    private UUID userId;

    private String title;

    private String message;

    private String notificationType;

    private Boolean isRead;

    private Instant createdAt;
}