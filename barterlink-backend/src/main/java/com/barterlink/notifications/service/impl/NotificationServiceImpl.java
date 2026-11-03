package com.barterlink.notifications.service.impl;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.notifications.dto.response.NotificationResponseDto;
import com.barterlink.notifications.entity.Notification;
import com.barterlink.notifications.enums.NotificationType;
import com.barterlink.notifications.repository.NotificationRepository;
import com.barterlink.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.websocket.constants.WebSocketDestinationConstants;
import com.barterlink.websocket.dto.NotificationMessageDto;
import com.barterlink.websocket.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    private final WebSocketService webSocketService;

    @Override
    public void createNotification(
            UUID recipientId,
            NotificationType type,
            String title,
            String message) {

        User recipient =
                userRepository.findById(recipientId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Recipient not found"));

        Notification notification =
                Notification.builder()
                        .recipient(recipient)
                        .type(type)
                        .title(title)
                        .message(message)
                        .build();

        Notification savedNotification =
                notificationRepository.save(notification);


        String destination =
                WebSocketDestinationConstants.NOTIFICATION_TOPIC
                        + recipient.getId();

        NotificationMessageDto messageDto =
                mapToWebSocketDto(savedNotification);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendWebSocketNotificationSafely(
                                    destination,
                                    messageDto
                            );
                        }
                    }
            );

        } else {
            sendWebSocketNotificationSafely(destination, messageDto);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getMyNotifications(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        return notificationRepository
                .findByRecipientAndIsReadOrderByCreatedAtDesc(
                        user,
                        false)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public NotificationResponseDto markAsRead(
            UUID notificationId,
            UUID userId) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Notification not found"));

        if (!notification.getRecipient()
                .getId()
                .equals(userId)) {

            throw new ResourceNotFoundException(
                    "Notification not found");
        }

        notification.setIsRead(true);

        return mapToResponse(
                notificationRepository.save(notification));
    }

    @Override
    public void markAllAsRead(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        List<Notification> notifications =
                notificationRepository
                        .findByRecipientAndIsReadOrderByCreatedAtDesc(
                                user,
                                false);

        notifications.forEach(notification ->
                notification.setIsRead(true));

        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        return notificationRepository.countByRecipientAndIsRead(
                user,
                false);
    }

    private NotificationResponseDto mapToResponse(
            Notification notification) {

        NotificationResponseDto dto =
                new NotificationResponseDto();

        dto.setNotificationId(
                notification.getId());

        dto.setType(
                notification.getType());

        dto.setTitle(
                notification.getTitle());

        dto.setMessage(
                notification.getMessage());

        dto.setIsRead(
                notification.getIsRead());

        dto.setCreatedAt(
                notification.getCreatedAt());

        return dto;
    }

    private NotificationMessageDto mapToWebSocketDto(
            Notification notification) {

        return NotificationMessageDto.builder()

                .notificationId(
                        notification.getId())

                .userId(
                        notification.getRecipient().getId())

                .title(
                        notification.getTitle())

                .message(
                        notification.getMessage())

                .notificationType(
                        notification.getType().name())

                .isRead(
                        notification.getIsRead())

                .createdAt(
                        notification.getCreatedAt())

                .build();
    }


    private void sendWebSocketNotificationSafely(
            String destination,
            NotificationMessageDto messageDto
    ) {
        try {
            webSocketService.sendNotification(destination, messageDto);
        } catch (RuntimeException exception) {
            log.warn(
                    "Notification {} was saved, but its WebSocket push failed",
                    messageDto.getNotificationId(),
                    exception
            );
        }
    }


}