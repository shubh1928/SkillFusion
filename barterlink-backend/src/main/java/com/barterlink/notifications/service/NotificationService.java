package com.barterlink.notifications.service;

import com.barterlink.notifications.dto.response.NotificationResponseDto;
import com.barterlink.notifications.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void createNotification(
            UUID recipientId,
            NotificationType type,
            String title,
            String message);

    List<NotificationResponseDto> getMyNotifications(
            UUID userId);

    List<NotificationResponseDto> getUnreadNotifications(
            UUID userId);

    NotificationResponseDto markAsRead(
            UUID notificationId,
            UUID userId);

    void markAllAsRead(
            UUID userId);

    long getUnreadCount(
            UUID userId);
}