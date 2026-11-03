package com.barterlink.notifications.dto.response;

import com.barterlink.notifications.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class NotificationResponseDto {

    private UUID notificationId;

    private NotificationType type;

    private String title;

    private String message;

    private Boolean isRead;

    private Instant createdAt;
}